package helium314.keyboard.ai

enum class AiApiStyle { ANTHROPIC, OPENAI }

enum class AiProvider(
    val id: String,
    val label: String,
    val baseUrl: String,
    val style: AiApiStyle,
    val defaultModel: String,
) {
    CLAUDE("claude", "Claude", "https://api.anthropic.com/v1", AiApiStyle.ANTHROPIC, "claude-haiku-5-5"),
    OPENAI("openai", "OpenAI", "https://api.openai.com/v1", AiApiStyle.OPENAI, "gpt-5-mini"),
    OPENROUTER("openrouter", "OpenRouter", "https://openrouter.ai/api/v1", AiApiStyle.OPENAI, "openai/gpt-5-mini"),
    GROQ("groq", "Groq", "https://api.groq.com/openai/v1", AiApiStyle.OPENAI, "llama-3.3-70b-versatile"),
    NVIDIA("nvidia", "NVIDIA", "https://integrate.api.nvidia.com/v1", AiApiStyle.OPENAI, "google/gemma-4-31b-it");

    companion object {
        fun fromId(id: String?) = entries.firstOrNull { it.id == id } ?: CLAUDE
    }
}
