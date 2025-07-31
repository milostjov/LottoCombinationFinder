package com.zoqo.lottocombinationfinder.components

import android.content.Context
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp



@Composable
fun LottoBallAnimated(
    number: Int,
    animationKey: Long,
    modifier: Modifier = Modifier
) {
    /* ---------- 1. boja loptice ---------- */
    val context       = LocalContext.current
    val totalNumbers  = remember { getTotalNumbersFromPrefs(context) }
    val ratio         = (number.toFloat() / totalNumbers.toFloat()).coerceIn(0f, 1f)
    val ballColor     = lerp(Color(0xFFA8E6CF), Color(0xFFFF8B94), ratio)

    /* ---------- 2. animacija rotacije ---------- */
    /*
     * KREIRAJ NOVI Animatable svaki put kad animationKey stigne.
     * Time nema „sećanja“ na prethodni okret.
     */
    val rotation = remember(animationKey) { Animatable(0f) }

    LaunchedEffect(animationKey) {
        rotation.snapTo(0f)
        rotation.animateTo(
            targetValue   = 180f,
            animationSpec = tween(800, easing = LinearOutSlowInEasing)
        )
    }

    val showNumber by remember { derivedStateOf { rotation.value > 90f } }

    /* ---------- 3. UI ---------- */
    Box(
        modifier = modifier
            .size(48.dp)
            .padding(4.dp)
            .graphicsLayer {
                rotationY     = rotation.value
                cameraDistance = 16 * density  // 3-D efekat
            }
            .clip(CircleShape)
            .background(ballColor),
        contentAlignment = Alignment.Center
    ) {
        if (showNumber) {
            Text(
                text  = number.toString(),
                style = MaterialTheme.typography.bodyLarge,
                color = Color.Black,
                modifier = Modifier.graphicsLayer { rotationY = 180f } // ispravka ogledala
            )
        }
    }
}

@Composable
fun LottoBallAnimated2(
    number: Int,
    animationKey: Long,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val totalNumbers = remember { getTotalNumbersFromPrefs(context) }

    // 🎨 Boja loptice
    val ratio = (number.toFloat() / totalNumbers.toFloat()).coerceIn(0f, 1f)
    val ballColor = lerp(Color(0xFFA8E6CF), Color(0xFFFF8B94), ratio)

    // 🌟 Pulsirajuća animacija (ponavlja se svakih 5 sekundi)
    val scaleAnim = remember { Animatable(1f) }
    LaunchedEffect(animationKey) {
        while (true) {
            scaleAnim.animateTo(
                targetValue = 1.2f,
                animationSpec = tween(500, easing = LinearOutSlowInEasing)
            )
            scaleAnim.animateTo(
                targetValue = 1f,
                animationSpec = tween(500, easing = LinearOutSlowInEasing)
            )
            kotlinx.coroutines.delay(1000) // ⏳ pauza 5 sekundi
        }
    }

    // 🟢 UI prikaz loptice
    Box(
        modifier = modifier
            .size(48.dp)
            .padding(4.dp)
            .graphicsLayer {
                scaleX = scaleAnim.value
                scaleY = scaleAnim.value
            }
            .clip(CircleShape)
            .background(ballColor),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = number.toString(),
            style = MaterialTheme.typography.bodyLarge,
            color = Color.Black
        )
    }
}


fun getTotalNumbersFromPrefs(context: Context): Int {
    val prefs = context.getSharedPreferences("astro_prefs", Context.MODE_PRIVATE)
    return prefs.getInt("totalNumbers", 39) // 39 default ako nije setovano
}