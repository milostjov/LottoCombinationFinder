package com.zoqo.lottocombinationfinder.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val AppShapes = Shapes(
    small = RoundedCornerShape(4.dp),
    medium = RoundedCornerShape(8.dp),
    large = RoundedCornerShape(12.dp)
)

private val LuxDarkColorScheme = darkColorScheme(
    primary = Gold,
    secondary = DarkGold,
    tertiary = DarkGold,
    background = DeepBackground,
    surface = SurfaceDark,
    onPrimary = Color.Black,
    onSecondary = Color.Black,
    onTertiary = Color.Black,
    onBackground = TextPrimary,
    onSurface = TextPrimary
)

@Composable
fun LottoCombinationFinderTheme(
    // Forcira tamnu temu jer je luksuzni stil
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = LuxDarkColorScheme,
        typography = Typography,
        shapes = AppShapes,
        content = content
    )
}
