package helium314.keyboard.voice

import helium314.keyboard.ai.AiClient
import helium314.keyboard.ai.AiException
import helium314.keyboard.ai.AiNetworkException
import org.json.JSONException
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

object GroqSpeech {
    private const val ENDPOINT = "https://api.groq.com/openai/v1/audio/transcriptions"
    private const val CONNECT_TIMEOUT_MS = 15_000
    private const val READ_TIMEOUT_MS = 120_000
    private const val LINE = "\r\n"

    fun transcribe(apiKey: String, audio: File, model: String, language: String, prompt: String): String {
        val boundary = "----pocketpen" + System.currentTimeMillis()
        val head = StringBuilder()
        fun field(name: String, value: String) {
            head.append("--").append(boundary).append(LINE)
            head.append("Content-Disposition: form-data; name=\"").append(name).append("\"").append(LINE).append(LINE)
            head.append(value).append(LINE)
        }
        field("model", model)
        field("response_format", "json")
        field("temperature", "0")
        if (language.isNotBlank()) field("language", language)
        if (prompt.isNotBlank()) field("prompt", prompt)
        head.append("--").append(boundary).append(LINE)
        head.append("Content-Disposition: form-data; name=\"file\"; filename=\"voice.m4a\"").append(LINE)
        head.append("Content-Type: audio/mp4").append(LINE).append(LINE)
        val headBytes = head.toString().toByteArray(Charsets.UTF_8)
        val tailBytes = (LINE + "--" + boundary + "--" + LINE).toByteArray(Charsets.UTF_8)

        val connection = URL(ENDPOINT).openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "POST"
            connection.connectTimeout = CONNECT_TIMEOUT_MS
            connection.readTimeout = READ_TIMEOUT_MS
            connection.doOutput = true
            connection.setRequestProperty("Authorization", "Bearer $apiKey")
            connection.setRequestProperty("Accept", "application/json")
            connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
            connection.setFixedLengthStreamingMode(headBytes.size.toLong() + audio.length() + tailBytes.size.toLong())
            connection.outputStream.use { out ->
                out.write(headBytes)
                FileInputStream(audio).use { input ->
                    val buffer = ByteArray(8192)
                    while (true) {
                        val read = input.read(buffer)
                        if (read < 0) break
                        out.write(buffer, 0, read)
                    }
                }
                out.write(tailBytes)
            }
            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val body = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
            if (code !in 200..299) throw AiException(AiClient.errorMessage(code, body))
            return parseTranscript(body)
        } catch (e: AiException) {
            throw e
        } catch (e: IOException) {
            throw AiNetworkException("Network error: ${e.message ?: e.javaClass.simpleName}")
        } finally {
            connection.disconnect()
        }
    }

    fun parseTranscript(body: String): String {
        val text = try {
            val json = JSONObject(body)
            if (json.isNull("text")) "" else json.optString("text")
        } catch (e: JSONException) {
            body
        }
        val cleaned = text.trim()
        if (cleaned.isEmpty()) throw AiException("No speech was recognized")
        return cleaned
    }
}
