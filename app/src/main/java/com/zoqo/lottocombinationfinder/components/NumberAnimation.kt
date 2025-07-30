//NumberAnimation.kt
package com.zoqo.lottocombinationfinder.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import kotlin.random.Random

@Composable
fun AnimatedRankDisplay(
    targetNumber: Int,
    spinSpeed: Long = 50L,     // brzina rotiranja cifara
    stopDelay: Long = 150L,    // vreme između zaustavljanja svake cifre
    onAnimationEnd: (() -> Unit)? = null
): String {
    var displayedText by remember { mutableStateOf("") }
    val targetStr = targetNumber.toString()
    val length = targetStr.length

    LaunchedEffect(targetNumber) {
        val chars = MutableList(length) { '0' }
        var stopIndex = -1
        displayedText = chars.joinToString("")

        // dok ne zaustavimo sve cifre
        while (stopIndex < length - 1) {
            for (i in 0 until length) {
                // ako cifra još nije zaustavljena, nastavlja da se vrti
                if (i > stopIndex) {
                    chars[i] = Random.nextInt(0, 9).digitToChar()
                }
            }
            displayedText = chars.joinToString("")
            delay(spinSpeed)

            // povremeno (na stopDelay) zaustavljamo sledeću cifru
            if ((0..2).random() == 1 || stopIndex == -1) {
                // dodaj malo "nasumičnosti" za prirodniji efekat
                if (System.currentTimeMillis() % stopDelay < spinSpeed) {
                    stopIndex++
                    chars[stopIndex] = targetStr[stopIndex]
                    displayedText = chars.joinToString("")
                }
            }
        }

        // osiguramo da su sve cifre na pravoj vrednosti
        for (i in 0 until length) chars[i] = targetStr[i]
        displayedText = chars.joinToString("")

        onAnimationEnd?.invoke()
    }

    return displayedText
}



