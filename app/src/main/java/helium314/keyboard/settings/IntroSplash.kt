package helium314.keyboard.settings

import android.net.Uri
import android.view.Gravity
import android.widget.FrameLayout
import android.widget.VideoView
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import helium314.keyboard.latin.R
import helium314.keyboard.latin.utils.PP

@Composable
fun IntroSplash(onFinished: () -> Unit) {
    val context = LocalContext.current
    val uri = remember { Uri.parse("android.resource://${context.packageName}/${R.raw.pp_intro}") }
    Box(
        Modifier
            .fillMaxSize()
            .background(PP.Ink)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onFinished() }
    ) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                FrameLayout(ctx).apply {
                    val video = VideoView(ctx)
                    addView(video, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT, Gravity.CENTER))
                    video.setOnCompletionListener { onFinished() }
                    video.setOnErrorListener { _, _, _ -> onFinished(); true }
                    video.setVideoURI(uri)
                    video.start()
                }
            }
        )
    }
}
