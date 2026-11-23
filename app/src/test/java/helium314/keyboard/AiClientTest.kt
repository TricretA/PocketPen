package helium314.keyboard

import helium314.keyboard.ai.AiClient
import helium314.keyboard.ai.AiException
import helium314.keyboard.ai.AiProvider
import org.json.JSONObject
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

@RunWith(RobolectricTestRunner::class)
class AiClientTest {
    @Test fun openAiStyleResponseIsParsedAndThinkBlocksRemoved() {
        val json = JSONObject("""{"choices":[{"message":{"content":"<think>plan</think>Hello there"}}]}""")
        assertEquals("Hello there", AiClient.parseCompletion(AiProvider.NVIDIA, json))
    }

    @Test fun nullContentIsAnErrorNotTheWordNull() {
        val json = JSONObject("""{"choices":[{"message":{"content":null}}]}""")
        assertFailsWith<AiException> { AiClient.parseCompletion(AiProvider.OPENAI, json) }
    }

    @Test fun anthropicTextPartsAreJoinedAndOtherPartsIgnored() {
        val json = JSONObject("""{"content":[{"type":"thinking","thinking":"x"},{"type":"text","text":"Hi "},{"type":"text","text":"there"}]}""")
        assertEquals("Hi there", AiClient.parseCompletion(AiProvider.CLAUDE, json))
    }

    @Test fun modelListDropsNonChatModelsAndSorts() {
        val json = JSONObject("""{"data":[{"id":"z/chat-b"},{"id":"nvidia/nv-embedqa-1b"},{"id":"a/chat-a"},{"id":"openai/whisper-1"}]}""")
        assertEquals(listOf("a/chat-a", "z/chat-b"), AiClient.parseModels(json))
    }

    @Test fun errorMessagesAreReadableForCommonShapes() {
        assertContains(AiClient.errorMessage(401, ""), "rejected")
        assertEquals("HTTP 400: bad model", AiClient.errorMessage(400, """{"type":"error","error":{"type":"x","message":"bad model"}}"""))
        assertEquals("HTTP 404: not found", AiClient.errorMessage(404, """{"detail":"not found"}"""))
        assertEquals("HTTP 500: plain text", AiClient.errorMessage(500, "plain text"))
    }

    @Test fun everyProviderUsesHttpsAndHasADefaultModel() {
        AiProvider.entries.forEach {
            assert(it.baseUrl.startsWith("https://")) { it.id }
            assert(it.defaultModel.isNotBlank()) { it.id }
        }
    }
}
