package helium314.keyboard.clipboardplus

import android.content.ContentUris
import android.content.Context
import android.database.ContentObserver
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore

class ScreenshotWatcher(
    private val context: Context,
    private val onScreenshot: (Uri, String) -> Unit,
) : ContentObserver(Handler(Looper.getMainLooper())) {
    private var lastId = -1L
    private var running = false

    fun start() {
        if (running) return
        lastId = newestId()
        context.contentResolver.registerContentObserver(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, true, this)
        running = true
    }

    fun stop() {
        if (!running) return
        context.contentResolver.unregisterContentObserver(this)
        running = false
    }

    override fun onChange(selfChange: Boolean, uri: Uri?) {
        check()
    }

    private fun newestId(): Long = try {
        context.contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            arrayOf(MediaStore.Images.Media._ID),
            null,
            null,
            "${MediaStore.Images.Media._ID} DESC"
        )?.use { if (it.moveToFirst()) it.getLong(0) else 0L } ?: 0L
    } catch (e: Exception) {
        0L
    }

    private fun check() {
        val modern = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
        val projection = if (modern) arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.DATE_ADDED,
            MediaStore.Images.Media.MIME_TYPE,
            MediaStore.MediaColumns.RELATIVE_PATH,
            MediaStore.MediaColumns.IS_PENDING,
        ) else arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.DATE_ADDED,
            MediaStore.Images.Media.MIME_TYPE,
            MediaStore.Images.Media.DATA,
        )
        try {
            context.contentResolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                projection,
                null,
                null,
                "${MediaStore.Images.Media._ID} DESC"
            )?.use { cursor ->
                val candidates = mutableListOf<Triple<Long, Uri, String>>()
                var newest = lastId
                var rows = 0
                while (cursor.moveToNext() && rows < MAX_ROWS) {
                    rows++
                    val id = cursor.getLong(0)
                    if (id <= lastId) break
                    if (id > newest) newest = id
                    val name = cursor.getString(1).orEmpty()
                    val added = cursor.getLong(2)
                    val mime = cursor.getString(3) ?: "image/png"
                    val path = cursor.getString(4).orEmpty()
                    if (modern && cursor.getInt(5) != 0) return
                    val looksLikeScreenshot = name.contains("screenshot", true) || path.contains("screenshot", true)
                    val fresh = System.currentTimeMillis() / 1000 - added < FRESH_SECONDS
                    if (looksLikeScreenshot && fresh)
                        candidates.add(Triple(id, ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id), mime))
                }
                lastId = newest
                candidates.maxByOrNull { it.first }?.let { onScreenshot(it.second, it.third) }
            }
        } catch (e: Exception) {
            lastId = newestId()
        }
    }

    companion object {
        private const val MAX_ROWS = 6
        private const val FRESH_SECONDS = 30L
    }
}
