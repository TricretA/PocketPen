package helium314.keyboard.clipboardplus

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import helium314.keyboard.latin.R
import helium314.keyboard.latin.utils.prefs

object ScreenshotAccess {
    private const val KEY = "clipboard_screenshots"

    fun permission(): String =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) Manifest.permission.READ_MEDIA_IMAGES
        else Manifest.permission.READ_EXTERNAL_STORAGE

    fun hasPermission(context: Context) =
        ContextCompat.checkSelfPermission(context, permission()) == PackageManager.PERMISSION_GRANTED

    fun enabled(context: Context) = context.prefs().getBoolean(KEY, true)

    fun setEnabled(context: Context, enabled: Boolean) = context.prefs().edit { putBoolean(KEY, enabled) }

    fun active(context: Context) = enabled(context) && hasPermission(context)

    fun requestPermission(context: Context) {
        val intent = Intent(context, MediaPermissionActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }
}

class MediaPermissionActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (ScreenshotAccess.hasPermission(this)) {
            finish()
            return
        }
        ActivityCompat.requestPermissions(this, arrayOf(ScreenshotAccess.permission()), REQUEST_CODE)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        val granted = grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED
        if (granted) ScreenshotAccess.setEnabled(this, true)
        Toast.makeText(this, if (granted) R.string.clipboard_screenshots_allowed else R.string.clipboard_screenshots_denied, Toast.LENGTH_LONG).show()
        finish()
    }

    companion object {
        private const val REQUEST_CODE = 52
    }
}
