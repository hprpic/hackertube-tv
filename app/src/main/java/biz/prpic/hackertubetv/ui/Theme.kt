package biz.prpic.hackertubetv.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.tv.material3.LocalContentColor
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.darkColorScheme

/** The site's dark palette. Every color in the app comes from here. */
object AppColors {
    val Background = Color(0xFF0F0F0F)
    val Text = Color(0xFFF1F1F1)
    val Secondary = Color(0xFFAAAAAA)
    val Accent = Color(0xFFFF6600)

    /** The frame around the focused video. */
    val FocusFrame = Color.White

    /** Behind a video image while it loads, and behind the current menu item. */
    val Raised = Secondary.copy(alpha = 0.16f)

    /** Behind the duration badge, so it stays readable on a bright image. */
    val Badge = Background.copy(alpha = 0.8f)
}

private val colorScheme = darkColorScheme(
    primary = AppColors.Accent,
    onPrimary = AppColors.Text,
    background = AppColors.Background,
    onBackground = AppColors.Text,
    surface = AppColors.Background,
    onSurface = AppColors.Text,
    onSurfaceVariant = AppColors.Secondary,
)

/** Dark theme only; text defaults to the site's light grey. */
@Composable
fun HackerTubeTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = colorScheme) {
        CompositionLocalProvider(LocalContentColor provides AppColors.Text, content = content)
    }
}
