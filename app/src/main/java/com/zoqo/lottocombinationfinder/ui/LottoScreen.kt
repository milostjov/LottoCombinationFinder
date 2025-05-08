// LottoScreen.kt
package com.zoqo.lottocombinationfinder.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.zoqo.lottocombinationfinder.components.GenerateButton
import com.zoqo.lottocombinationfinder.components.LottoInputFields
import com.zoqo.lottocombinationfinder.components.LottoResult
import com.zoqo.lottocombinationfinder.utils.calculateTotalCombinations
import com.zoqo.lottocombinationfinder.utils.findCombination
import java.math.BigInteger

@Composable
fun LottoApp(showRewardedAd: ((onReward: () -> Unit) -> Unit)) {
    var rankInput by remember { mutableStateOf(TextFieldValue("")) }
    var resultText by remember { mutableStateOf("") }
    var totalNumbers by remember { mutableStateOf(TextFieldValue("39")) }
    var numbersToChoose by remember { mutableStateOf(TextFieldValue("7")) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        content = { innerPadding ->
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .padding(16.dp)
                    .fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    LottoInputFields(
                        totalNumbers = totalNumbers,
                        onTotalNumbersChange = { totalNumbers = it },
                        numbersToChoose = numbersToChoose,
                        onNumbersToChooseChange = { numbersToChoose = it },
                        rankInput = rankInput,
                        onRankInputChange = { rankInput = it }
                    )

                    GenerateButton(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            showRewardedAd {
                                val total = totalNumbers.text.toIntOrNull()
                                val choose = numbersToChoose.text.toIntOrNull()
                                val rank = rankInput.text.toBigIntegerOrNull()

                                resultText = when {
                                    total == null || choose == null || total <= 0 || choose <= 0 || total < choose ->
                                        "Invalid input for total numbers or numbers to choose."
                                    rank == null || rank <= BigInteger.ZERO || rank > calculateTotalCombinations(total, choose) ->
                                        "Invalid rank. Enter a number between 1 and ${calculateTotalCombinations(total, choose)}"
                                    else -> {
                                        val combination = findCombination(rank.toInt(), total, choose)
                                        "Combination: ${combination.joinToString(", ")}"
                                    }
                                }
                            }
                        }
                    )

                    LottoResult(resultText)
                }

                BannerAdView()
            }
        }
    )
}
