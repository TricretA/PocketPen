package helium314.keyboard.ai

import helium314.keyboard.latin.R

enum class AiApiStyle { ANTHROPIC, OPENAI }

enum class AiProvider(
    val id: String,
    val label: String,
    val baseUrl: String,
    val style: AiApiStyle,
    val defaultModel: String,
    val logo: Int,
    val brandColor: Int,
    val suggestedModels: List<String>,
) {
    CLAUDE(
        "claude", "Claude", "https://api.anthropic.com/v1", AiApiStyle.ANTHROPIC, "claude-haiku-5-5",
        R.drawable.ic_logo_claude, 0xFFD97757.toInt(),
        listOf("claude-haiku-5-5", "claude-sonnet-5-5", "claude-opus-5-5", "claude-fable-5-2")
    ),
    OPENAI(
        "openai", "ChatGPT", "https://api.openai.com/v1", AiApiStyle.OPENAI, "gpt-5-mini",
        R.drawable.ic_logo_openai, 0xFF10A37F.toInt(),
        listOf("gpt-5-mini")
    ),
    OPENROUTER(
        "openrouter", "OpenRouter", "https://openrouter.ai/api/v1", AiApiStyle.OPENAI, "openai/gpt-5-mini",
        R.drawable.ic_logo_openrouter, 0xFF6467F2.toInt(),
        listOf("openai/gpt-5-mini")
    ),
    GROQ(
        "groq", "Groq", "https://api.groq.com/openai/v1", AiApiStyle.OPENAI, "llama-3.3-70b-versatile",
        R.drawable.ic_logo_groq, 0xFFF55036.toInt(),
        listOf("llama-3.3-70b-versatile")
    ),
    NVIDIA(
        "nvidia", "NVIDIA", "https://integrate.api.nvidia.com/v1", AiApiStyle.OPENAI, "google/gemma-4-31b-it",
        R.drawable.ic_logo_nvidia, 0xFF76B900.toInt(),
        listOf("google/gemma-4-31b-it")
    );

    companion object {
        fun fromId(id: String?) = entries.firstOrNull { it.id == id } ?: CLAUDE
    }
}
