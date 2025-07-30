package com.zoqo.lottocombinationfinder.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.accompanist.flowlayout.FlowRow

@Composable
fun LottoResult(
    resultText: String,
    restartAnimationKey: Long,
    modifier: Modifier = Modifier
) {
    if (!resultText.startsWith("Combination:")) {
        Text(
            text = resultText,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(top = 16.dp)
        )
        return
    }

    // Bez remember – jeftino je da se ponovo izračuna
    val numbers = resultText
        .removePrefix("Combination:")
        .split(",")
        .mapNotNull { it.trim().toIntOrNull() }

    FlowRow(
        mainAxisSpacing = 8.dp,
        crossAxisSpacing = 8.dp,
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 16.dp)
    ) {
        numbers.forEach { number ->
            /**
             * `key()` forsira Compose da POTPUNO odbaci stari čvor
             * i kreira novi kad se `restartAnimationKey` promeni.
             */
            key(restartAnimationKey, number) {
                LottoBallAnimated(
                    number = number,
                    animationKey = restartAnimationKey
                )
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
    if (!resultText.startsWith("Combination:")) {
        Text(
            text = resultText,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(top = 16.dp)
        )
        return
    }

    // Bez remember – jeftino je da se ponovo izračuna
    val numbers = resultText
        .removePrefix("Combination:")
        .split(",")
        .mapNotNull { it.trim().toIntOrNull() }

    FlowRow(
        mainAxisSpacing = 8.dp,
        crossAxisSpacing = 8.dp,
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 16.dp)
    ) {
        numbers.forEach { number ->
            /**
             * `key()` forsira Compose da POTPUNO odbaci stari čvor
             * i kreira novi kad se `restartAnimationKey` promeni.
             */
            key(restartAnimationKey, number) {
                LottoBallAnimated2(
                    number = number,
                    animationKey = restartAnimationKey
                )
            }
        }
    }
}

