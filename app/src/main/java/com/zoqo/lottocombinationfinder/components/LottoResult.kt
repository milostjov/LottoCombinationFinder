package com.zoqo.lottocombinationfinder.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.google.accompanist.flowlayout.FlowRow

@Composable
fun LottoResult(
    resultText: String,
    restartAnimationKey: Long,
    modifier: Modifier = Modifier
) {
    // Uskladi sa veličinom tvojih kuglica
    val BallSize = 48.dp
    val BallSpacing = 8.dp

    val payload = resultText.removePrefix("Combination:").trim()
    val parts = payload.split("+").map { it.trim() }

    fun parseNums(s: String?): List<Int> =
        if (s.isNullOrBlank()) emptyList()
        else Regex("""\d+""").findAll(s).map { it.value.toInt() }.toList()

    val mainNumbers  = parseNums(parts.getOrNull(0))
    val bonusNumbers = parseNums(parts.getOrNull(1))

    if (mainNumbers.isEmpty() && bonusNumbers.isEmpty()) {
        Text(
            text = resultText,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(top = 16.dp)
        )
        return
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // RED 1: glavne kugle (LottoBallAnimated)
        FlowRow(
            mainAxisSpacing = BallSpacing,
            crossAxisSpacing = BallSpacing,
            crossAxisAlignment = com.google.accompanist.flowlayout.FlowCrossAxisAlignment.Center
        ) {
            mainNumbers.forEach { n ->
                Box(Modifier.size(BallSize), contentAlignment = Alignment.Center) {
                    androidx.compose.runtime.key("main-$restartAnimationKey-$n") {

                        LottoBallEarthSpin(
                            number = n,
                            animationKey = restartAnimationKey
                        )
                    }
                }
            }
        }

        // RED 2: "+" ispod 1. kugle, bonus kugle počinju ispod 2. kugle
        if (bonusNumbers.isNotEmpty()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // pozicija 1: plus badge
//                Box(Modifier.size(BallSize), contentAlignment = Alignment.Center) {
//                    PlusBadge(animationKey = restartAnimationKey, modifier = Modifier.size(24.dp))
//                }
//
//                // razmak između kolona identičan kao u gornjem redu
//                Spacer(Modifier.width(BallSpacing))

                // bonus kugle (LottoBallAnimated ili druga animacija po želji)
                FlowRow(
                    mainAxisSpacing = BallSpacing,
                    crossAxisSpacing = BallSpacing,
                    crossAxisAlignment = com.google.accompanist.flowlayout.FlowCrossAxisAlignment.Center
                ) {
                    bonusNumbers.forEach { n ->
                        Box(Modifier.size(BallSize), contentAlignment = Alignment.Center) {
                            androidx.compose.runtime.key("bonus-$restartAnimationKey-$n") {
                                LottoBallEarthSpin(
                                    number = n,
                                    animationKey = restartAnimationKey,
                                    isBonus = true
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}



@Composable
fun LottoResult2(
    resultText: String,
    restartAnimationKey: Long,
    modifier: Modifier = Modifier
) {
    // Konstante za poravnanje – uskladi sa veličinom tvojih loptica
    val BallSize = 48.dp
    val BallSpacing = 8.dp

    val payload = resultText.removePrefix("Combination:").trim()
    val parts = payload.split("+").map { it.trim() }

    fun parseNums(s: String?): List<Int> =
        if (s.isNullOrBlank()) emptyList()
        else Regex("""\d+""").findAll(s).map { it.value.toInt() }.toList()

    val mainNumbers  = parseNums(parts.getOrNull(0))
    val bonusNumbers = parseNums(parts.getOrNull(1))

    if (mainNumbers.isEmpty() && bonusNumbers.isEmpty()) {
        Text(
            text = resultText,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(top = 16.dp)
        )
        return
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // RED 1: Glavne kugle
        FlowRow(
            mainAxisSpacing = BallSpacing,
            crossAxisSpacing = BallSpacing,
            crossAxisAlignment = com.google.accompanist.flowlayout.FlowCrossAxisAlignment.Center,
        ) {
            mainNumbers.forEach { n ->
                // Svaka kugla ima istu širinu/visinu -> stabilna mreža
                Box(Modifier.size(BallSize), contentAlignment = Alignment.Center) {
                    androidx.compose.runtime.key("main2-$restartAnimationKey-$n") {
                        LottoBallAnimateResult(
                            number = n,
                            animationKey = restartAnimationKey
                        )
                    }
                }
            }
        }

// RED 2: "+" poravnat ispod 1. kugle, prva bonus kugla ispod 2. kugle
        if (bonusNumbers.isNotEmpty()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Plus ispod 1. glavne kugle
//                Box(Modifier.size(BallSize), contentAlignment = Alignment.Center) {
//                    PlusBadge(animationKey = restartAnimationKey, modifier = Modifier.size(24.dp))
//                }
//
//                Spacer(Modifier.width(BallSpacing))

                // BONUS kugle – KORISTI LottoBallAnimated2 za kontinuirani puls
                FlowRow(
                    mainAxisSpacing = BallSpacing,
                    crossAxisSpacing = BallSpacing,
                    crossAxisAlignment = com.google.accompanist.flowlayout.FlowCrossAxisAlignment.Center,
                ) {
                    bonusNumbers.forEach { n ->
                        Box(Modifier.size(BallSize), contentAlignment = Alignment.Center) {
                            androidx.compose.runtime.key("bonus2-$restartAnimationKey-$n") {
                                LottoBallAnimateResult(   // ⬅️ ovde je ključna izmena
                                    number = n,
                                    animationKey = restartAnimationKey,
                                            isBonus = true
                                )
                            }
                        }
                    }
                }
            }
        }

    }
}


@Composable
fun PlusBadge(
    animationKey: Long,
    modifier: Modifier = Modifier
) {
    val scale = remember { androidx.compose.animation.core.Animatable(0.8f) }
    LaunchedEffect(animationKey) {              // re-animira kada se promeni ključ
        scale.snapTo(0.8f)
        scale.animateTo(
            1f,
            animationSpec = androidx.compose.animation.core.tween(
                durationMillis = 250,
                easing = androidx.compose.animation.core.LinearOutSlowInEasing
            )
        )
    }

    Surface(
        modifier = modifier
            .size(28.dp)
            .graphicsLayer(scaleX = scale.value, scaleY = scale.value),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.tertiaryContainer,
        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
        tonalElevation = 2.dp,
        shadowElevation = 4.dp
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = Icons.Filled.Add,
                contentDescription = "Bonus separator",
                modifier = Modifier.size(18.dp)
            )
        }
    }
}



