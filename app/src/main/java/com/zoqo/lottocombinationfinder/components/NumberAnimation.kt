package com.zoqo.lottocombinationfinder.components

import androidx.compose.runtime.*
import kotlinx.coroutines.delay

@Composable
fun AnimatedRankDisplay(
    targetNumber: String,
    onAnimationEnd: (() -> Unit)? = null
): String {
    var animatedText by remember { mutableStateOf("") }

    LaunchedEffect(targetNumber) {
        animatedText = ""
        for (char in targetNumber) {
            animatedText += char
            delay(150) // brzina ispisivanja cifara
        }
        onAnimationEnd?.invoke()
    }

    return animatedText
}
