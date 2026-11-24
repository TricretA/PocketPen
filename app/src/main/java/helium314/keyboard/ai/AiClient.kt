package helium314.keyboard.ai

import android.content.Context
import helium314.keyboard.latin.R
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

open class AiException(message: String) : Exception(message)

class AiNetworkException(message: String) : AiException(message)

object AiClient {
    private const val ANTHROPIC_VERSION = "2023-06-01"
    private const val ANTHROPIC_MAX_TOKENS = 4096
    private const val CONNECT_TIMEOUT_MS = 15_000
    private const val READ_TIMEOUT_MS = 90_000
    private val thinkBlock = Regex("<think>[\\s\\S]*?</think>")
    private val nonChatModel = Regex("embed|whisper|tts|dall-e|moderation|image|audio|realtime|transcribe|guard|safety|rerank|clip|vlm-embed|detector|veo|imagen|aqa|live")

    fun complete(provider: AiProvider, apiKey: String, model: String, system: String, user: String): String {
        val body = JSONObject().put("model", model)
        val path: String
        if (provider.style == AiApiStyle.ANTHROPIC) {
            body.put("max_tokens", ANTHROPIC_MAX_TOKENS)
            body.put("system", system)
            body.put("messages", JSONArray().put(message("user", user)))
            path = "/messages"
        } else {
            body.put("messages", JSONArray().put(message("system", system)).put(message("user", user)))
            path = "/chat/completions"
        }
        return parseCompletion(provider, request(provider, apiKey, "POST", path, body.toString()))
    }

    fun listModels(provider: AiProvider, apiKey: String): List<String> {
        val path = if (provider.style == AiApiStyle.ANTHROPIC) "/models?limit=1000" else "/models"
        return parseModels(request(provider, apiKey, "GET", path, null))
    }

    fun parseCompletion(provider: AiProvider, response: JSONObject): String {
        val raw = if (provider.style == AiApiStyle.ANTHROPIC) {
            val parts = response.optJSONArray("content") ?: JSONArray()
            (0 until parts.length())
                .mapNotNull { parts.optJSONObject(it) }
                .filter { it.optString("type") == "text" }
                .joinToString("") { it.optString("text") }
        } else {
            val message = response.optJSONArray("choices")?.optJSONObject(0)?.optJSONObject("message")
            if (message == null || message.isNull("content")) "" else message.optString("content")
        }
        val cleaned = cleanOutput(raw)
        if (cleaned.isEmpty()) throw AiException("The model returned an empty response")
        return cleaned
    }

    fun parseModels(response: JSONObject): List<String> {
        val data = response.optJSONArray("data") ?: return emptyList()
        return (0 until data.length())
            .mapNotNull { data.optJSONObject(it)?.optString("id")?.removePrefix("models/")?.takeIf { id -> id.isNotEmpty() } }
            .filterNot { nonChatModel.containsMatchIn(it.lowercase()) }
            .sorted()
    }

    fun cleanOutput(raw: String): String = raw.replace(thinkBlock, "").trim()

    fun errorMessage(code: Int, body: String): String {
        if (code == 401 || code == 403) return "The API key was rejected (HTTP $code)"
        val detail = runCatching {
            val json = JSONObject(body)
            val error = json.opt("error")
            when {
                error is JSONObject -> error.optString("message")
                error is String -> error
                json.has("detail") -> json.optString("detail")
                else -> json.optString("message")
            }
        }.getOrNull()?.takeIf { it.isNotBlank() } ?: body.take(200)
        return "HTTP $code: $detail"
    }

    private fun message(role: String, content: String) = JSONObject().put("role", role).put("content", content)

    private fun request(provider: AiProvider, apiKey: String, method: String, path: String, body: String?): JSONObject {
        val connection = URL(provider.baseUrl + path).openConnection() as HttpURLConnection
        try {
            connection.requestMethod = method
            connection.connectTimeout = CONNECT_TIMEOUT_MS
            connection.readTimeout = READ_TIMEOUT_MS
            connection.setRequestProperty("Accept", "application/json")
            if (provider.style == AiApiStyle.ANTHROPIC) {
                connection.setRequestProperty("x-api-key", apiKey)
                connection.setRequestProperty("anthropic-version", ANTHROPIC_VERSION)
            } else {
                connection.setRequestProperty("Authorization", "Bearer $apiKey")
            }
            if (body != null) {
                connection.setRequestProperty("Content-Type", "application/json")
                connection.doOutput = true
                connection.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
            }
            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val text = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
            if (code !in 200..299) throw AiException(errorMessage(code, text))
            return JSONObject(text)
        } catch (e: AiException) {
            throw e
        } catch (e: IOException) {
            throw AiNetworkException("Network error: ${e.message ?: e.javaClass.simpleName}")
        } catch (e: JSONException) {
            throw AiException("Unexpected response from ${provider.label}")
        } finally {
            connection.disconnect()
        }
    }
}

object AiRunner {
    fun run(context: Context, action: AiAction, text: String, customInstruction: String? = null): String =
        runWith(context, AiSettings.provider(context), action, text, customInstruction)

    fun runWith(context: Context, provider: AiProvider, action: AiAction, text: String, customInstruction: String? = null): String {
        val apiKey = AiKeyStore.load(context, provider)
            ?: throw AiException(context.getString(R.string.ai_error_no_key, provider.label))
        val instruction = customInstruction?.takeIf { it.isNotBlank() } ?: AiPrompts.instruction(context, action)
        if (instruction.isBlank()) throw AiException(context.getString(R.string.ai_error_no_custom_prompt))
        return AiClient.complete(
            provider,
            apiKey,
            AiSettings.model(context, provider),
            AiPrompts.SYSTEM,
            AiPrompts.userMessage(instruction, text)
        )
    }
}
