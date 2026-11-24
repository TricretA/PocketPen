package helium314.keyboard.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import androidx.core.content.edit
import helium314.keyboard.latin.BuildConfig
import helium314.keyboard.latin.utils.prefs
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

data class UpdateInfo(val version: String, val apkUrl: String, val notes: String)

object UpdateChecker {
    private const val API = "https://api.github.com/repos/TricretA/pocketpen/releases/latest"
    const val PREF_AUTO = "pp_update_auto"
    private const val PREF_LAST = "pp_update_last"
    private const val INTERVAL_MS = 12L * 60 * 60 * 1000

    val supported get() = !BuildConfig.DEBUG

    fun autoEnabled(context: Context) = context.prefs().getBoolean(PREF_AUTO, true)

    fun setAuto(context: Context, enabled: Boolean) = context.prefs().edit { putBoolean(PREF_AUTO, enabled) }

    fun isDue(context: Context): Boolean {
        if (!supported || !autoEnabled(context)) return false
        return System.currentTimeMillis() - context.prefs().getLong(PREF_LAST, 0L) > INTERVAL_MS
    }

    fun markChecked(context: Context) = context.prefs().edit { putLong(PREF_LAST, System.currentTimeMillis()) }

    fun fetch(): UpdateInfo? {
        val connection = URL(API).openConnection() as HttpURLConnection
        try {
            connection.connectTimeout = 15_000
            connection.readTimeout = 20_000
            connection.setRequestProperty("Accept", "application/vnd.github+json")
            connection.setRequestProperty("User-Agent", "Pocketpen")
            if (connection.responseCode !in 200..299) return null
            val json = JSONObject(connection.inputStream.bufferedReader().use { it.readText() })
            val tag = json.optString("tag_name")
            if (tag.isBlank() || !isNewer(tag, BuildConfig.VERSION_NAME)) return null
            val assets = json.optJSONArray("assets") ?: return null
            for (i in 0 until assets.length()) {
                val asset = assets.getJSONObject(i)
                if (asset.optString("name").endsWith(".apk"))
                    return UpdateInfo(tag.removePrefix("v"), asset.optString("browser_download_url"), json.optString("body"))
            }
            return null
        } finally {
            connection.disconnect()
        }
    }

    fun isNewer(remote: String, local: String): Boolean {
        fun parts(version: String) = version.removePrefix("v").split(Regex("[^0-9]+")).filter { it.isNotEmpty() }.map { it.toInt() }
        val a = parts(remote)
        val b = parts(local)
        for (i in 0 until maxOf(a.size, b.size)) {
            val x = a.getOrElse(i) { 0 }
            val y = b.getOrElse(i) { 0 }
            if (x != y) return x > y
        }
        return false
    }

    fun download(context: Context, info: UpdateInfo, onProgress: (Int) -> Unit): File {
        val directory = File(context.filesDir, "updates").apply { mkdirs() }
        val target = File(directory, "Pocketpen-update.apk")
        val connection = URL(info.apkUrl).openConnection() as HttpURLConnection
        try {
            connection.connectTimeout = 15_000
            connection.readTimeout = 30_000
            connection.instanceFollowRedirects = true
            connection.setRequestProperty("User-Agent", "Pocketpen")
            val total = connection.contentLengthLong
            var done = 0L
            connection.inputStream.use { input ->
                target.outputStream().use { output ->
                    val buffer = ByteArray(16 * 1024)
                    while (true) {
                        val read = input.read(buffer)
                        if (read < 0) break
                        output.write(buffer, 0, read)
                        done += read
                        if (total > 0) onProgress((done * 100 / total).toInt())
                    }
                }
            }
        } finally {
            connection.disconnect()
        }
        return target
    }

    fun install(context: Context, file: File): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !context.packageManager.canRequestPackageInstalls()) {
            val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:" + context.packageName))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            return false
        }
        val uri = FileProvider.getUriForFile(context, context.packageName + ".updates", file)
        val intent = Intent(Intent.ACTION_VIEW)
            .setDataAndType(uri, "application/vnd.android.package-archive")
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
        return true
    }
}
