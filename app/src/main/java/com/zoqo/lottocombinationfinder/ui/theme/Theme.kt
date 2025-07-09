package com.zoqo.lottocombinationfinder.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

// Custom gold-dark color scheme
//private val Gold = Color(0xFFFFD700)
//private val DarkGold = Color(0xFFB8860B)
//private val DeepBackground = Color(0xFF121212)
//private val SurfaceDark = Color(0xFF1E1E1E)
//private val TextPrimary = Color(0xFFFFFFFF)
//private val TextSecondary = Color(0xFFCCCCCC)
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
    darkTheme: Boolean = true, // Forcira tamnu temu jer je luksuzni stil
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = LuxDarkColorScheme,
        typography = Typography,
        shapes = AppShapes,
        content = content
    )
}
