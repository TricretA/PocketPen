package helium314.keyboard.settings.ui

import android.net.Uri
import android.view.Gravity
import android.widget.FrameLayout
import android.widget.VideoView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import helium314.keyboard.latin.R
import helium314.keyboard.latin.utils.PP

@Composable
fun AboutLogo(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val uri = remember { Uri.parse("android.resource://${context.packageName}/${R.raw.pp_about}") }
    var failed by remember { mutableStateOf(false) }
    Box(
        modifier
            .size(240.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(PP.Ink),
        contentAlignment = Alignment.Center
    ) {
        if (failed) {
            Image(painterResource(R.drawable.pp_mark), contentDescription = null, modifier = Modifier.size(120.dp))
        } else {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    FrameLayout(ctx).apply {
                        val video = VideoView(ctx)
                        addView(video, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT, Gravity.CENTER))
                        video.setOnPreparedListener {
                            it.isLooping = true
                            it.setVolume(0f, 0f)
                        }
                        video.setOnErrorListener { _, _, _ ->
                            failed = true
                            true
                        }
                        video.setVideoURI(uri)
                        video.start()
                    }
                }
            )
        }
    }
}
