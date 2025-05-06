package com.zoqo.lottocombinationfinder.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.accompanist.flowlayout.FlowRow

@Composable
fun LottoResult(resultText: String) {
    if (resultText.startsWith("Combination:")) {
        val numbers = resultText
            .removePrefix("Combination:")
            .split(",")
            .mapNotNull { it.trim().toIntOrNull() }

        FlowRow(
            mainAxisSpacing = 8.dp,
            crossAxisSpacing = 8.dp,
            modifier = Modifier
                .padding(top = 16.dp)
                .fillMaxWidth()
        ) {
            numbers.forEach { LottoBall(it) }
        }
    } else {
        Text(
            text = resultText,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(top = 16.dp)
        )
    }
}
