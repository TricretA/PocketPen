package helium314.keyboard.ai

import android.os.Handler
import android.os.Looper
import android.widget.Toast
import helium314.keyboard.keyboard.KeyboardSwitcher
import helium314.keyboard.latin.LatinIME
import helium314.keyboard.latin.R

object AiBridge {
    class Session(
        val sourceText: String,
        val hasSelection: Boolean,
        val beforeLength: Int,
        val afterLength: Int,
    )

    private const val RESUME_DELAY_MS = 150L
    private const val MAX_ATTEMPTS = 20
    private val handler = Handler(Looper.getMainLooper())

    fun requestInstruction(ime: LatinIME, session: Session) {
        if (!AiInstructionMode.start(ime, session))
            Toast.makeText(ime, R.string.ai_tell_unavailable, Toast.LENGTH_LONG).show()
    }

    fun run(ime: LatinIME, session: Session, instruction: String) {
        attempt(ime, session, instruction, 0)
    }

    private fun attempt(service: LatinIME, session: Session, instruction: String, count: Int) {
        handler.postDelayed({
            val token = KeyboardSwitcher.getInstance().mainKeyboardView?.windowToken
            if (token != null && service.isInputViewShown) {
                service.showOptionsDialog(AiPanel(service).createResult(token, session, instruction))
            } else if (count < MAX_ATTEMPTS) {
                attempt(service, session, instruction, count + 1)
            }
        }, RESUME_DELAY_MS)
    }
}
