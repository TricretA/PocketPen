package helium314.keyboard.ai

import android.content.Context
import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.io.File
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

object AiKeyStore {
    private const val KEYSTORE = "AndroidKeyStore"
    private const val ALIAS = "keyboard_ai_keys"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val IV_SIZE = 12
    private const val TAG_BITS = 128
    private const val PLAIN_PREFIX = "plain:"

    private fun file(context: Context, provider: AiProvider): File {
        val dir = File(context.noBackupFilesDir, "ai_keys")
        dir.mkdirs()
        return File(dir, provider.id)
    }

    fun save(context: Context, provider: AiProvider, apiKey: String): Boolean {
        val trimmed = apiKey.trim()
        val target = file(context, provider)
        if (trimmed.isEmpty()) return target.delete()
        return runCatching { target.writeText(encrypt(trimmed)) }.isSuccess
    }

    fun load(context: Context, provider: AiProvider): String? {
        val source = file(context, provider)
        if (!source.exists()) return null
        return runCatching { decrypt(source.readText()) }.getOrNull()?.takeIf { it.isNotEmpty() }
    }

    fun hasKey(context: Context, provider: AiProvider) = load(context, provider) != null

    fun clear(context: Context, provider: AiProvider) {
        file(context, provider).delete()
    }

    private fun secretKey(): SecretKey? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return null
        val keyStore = KeyStore.getInstance(KEYSTORE)
        keyStore.load(null)
        val existing = keyStore.getKey(ALIAS, null)
        if (existing is SecretKey) return existing
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE)
        generator.init(
            KeyGenParameterSpec.Builder(ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()
        )
        return generator.generateKey()
    }

    private fun encrypt(plain: String): String {
        val bytes = plain.toByteArray(Charsets.UTF_8)
        val key = secretKey() ?: return PLAIN_PREFIX + Base64.encodeToString(bytes, Base64.NO_WRAP)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, key)
        val encrypted = cipher.doFinal(bytes)
        return Base64.encodeToString(cipher.iv + encrypted, Base64.NO_WRAP)
    }

    private fun decrypt(stored: String): String {
        if (stored.startsWith(PLAIN_PREFIX))
            return String(Base64.decode(stored.removePrefix(PLAIN_PREFIX), Base64.NO_WRAP), Charsets.UTF_8)
        val key = secretKey() ?: error("keystore unavailable")
        val data = Base64.decode(stored, Base64.NO_WRAP)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(TAG_BITS, data, 0, IV_SIZE))
        return String(cipher.doFinal(data, IV_SIZE, data.size - IV_SIZE), Charsets.UTF_8)
    }
}
