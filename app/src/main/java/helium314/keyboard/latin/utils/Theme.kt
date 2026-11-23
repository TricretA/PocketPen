package helium314.keyboard.latin.utils

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import helium314.keyboard.latin.R

object PP {
    val Ink = Color(0xFF12141B)
    val Signal = Color(0xFFF04A0C)
    val Paper = Color(0xFFF7F4ED)
    val Graphite = Color(0xFF3B404C)
    val Mist = Color(0xFFE3DFD5)
    val InkRaised = Color(0xFF1D2029)
    val InkLine = Color(0xFF2B2F3B)
    val InkMuted = Color(0xFF9AA0AE)
    val SignalSoft = Color(0xFFFFE3D6)
    val White = Color(0xFFFFFFFF)
}

val Poppins = FontFamily(
    Font(R.font.poppins_regular, FontWeight.Normal),
    Font(R.font.poppins_medium, FontWeight.Medium),
    Font(R.font.poppins_semibold, FontWeight.SemiBold),
    Font(R.font.poppins_bold, FontWeight.Bold),
)

private fun TextStyle.poppins(weight: FontWeight? = null) = copy(fontFamily = Poppins, fontWeight = weight ?: fontWeight)

private fun pocketTypography(): Typography {
    val base = Typography()
    return Typography(
        displayLarge = base.displayLarge.poppins(FontWeight.Bold),
        displayMedium = base.displayMedium.poppins(FontWeight.Bold),
        displaySmall = base.displaySmall.poppins(FontWeight.Bold),
        headlineLarge = base.headlineLarge.poppins(FontWeight.SemiBold),
        headlineMedium = base.headlineMedium.poppins(FontWeight.SemiBold),
        headlineSmall = base.headlineSmall.poppins(FontWeight.SemiBold),
        titleLarge = base.titleLarge.poppins(FontWeight.SemiBold),
        titleMedium = base.titleMedium.poppins(FontWeight.SemiBold),
        titleSmall = base.titleSmall.poppins(FontWeight.Medium),
        bodyLarge = base.bodyLarge.poppins(FontWeight.Normal),
        bodyMedium = base.bodyMedium.poppins(FontWeight.Normal),
        bodySmall = base.bodySmall.poppins(FontWeight.Normal),
        labelLarge = base.labelLarge.poppins(FontWeight.Medium),
        labelMedium = base.labelMedium.poppins(FontWeight.Medium),
        labelSmall = base.labelSmall.poppins(FontWeight.Medium),
    )
}

private val lightScheme = lightColorScheme(
    primary = PP.Signal,
    onPrimary = PP.Ink,
    primaryContainer = PP.SignalSoft,
    onPrimaryContainer = PP.Ink,
    secondary = PP.Graphite,
    onSecondary = PP.Paper,
    secondaryContainer = PP.Mist,
    onSecondaryContainer = PP.Ink,
    tertiary = PP.Graphite,
    background = PP.Paper,
    onBackground = PP.Ink,
    surface = PP.Paper,
    onSurface = PP.Ink,
    surfaceVariant = PP.Mist,
    onSurfaceVariant = PP.Graphite,
    surfaceContainerLowest = PP.White,
    surfaceContainerLow = PP.White,
    surfaceContainer = PP.White,
    surfaceContainerHigh = PP.White,
    surfaceContainerHighest = PP.Mist,
    outline = PP.Graphite,
    outlineVariant = PP.Mist,
    inverseSurface = PP.Ink,
    inverseOnSurface = PP.Paper,
)

private val darkScheme = darkColorScheme(
    primary = PP.Signal,
    onPrimary = PP.Ink,
    primaryContainer = Color(0xFF4A1C08),
    onPrimaryContainer = PP.Paper,
    secondary = PP.InkMuted,
    onSecondary = PP.Ink,
    secondaryContainer = PP.InkLine,
    onSecondaryContainer = PP.Paper,
    tertiary = PP.InkMuted,
    background = PP.Ink,
    onBackground = PP.Paper,
    surface = PP.Ink,
    onSurface = PP.Paper,
    surfaceVariant = PP.InkRaised,
    onSurfaceVariant = PP.InkMuted,
    surfaceContainerLowest = PP.Ink,
    surfaceContainerLow = PP.InkRaised,
    surfaceContainer = PP.InkRaised,
    surfaceContainerHigh = PP.InkRaised,
    surfaceContainerHighest = PP.InkLine,
    outline = PP.InkMuted,
    outlineVariant = PP.InkLine,
    inverseSurface = PP.Paper,
    inverseOnSurface = PP.Ink,
)

private val pocketShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(36.dp),
)

@Composable
fun Theme(dark: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (dark) darkScheme else lightScheme,
        typography = pocketTypography(),
        shapes = pocketShapes,
        content = content
    )
}

const val previewDark = true
