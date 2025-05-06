package com.zoqo.lottocombinationfinder

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.zoqo.lottocombinationfinder.components.*
import com.zoqo.lottocombinationfinder.ui.theme.LottoCombinationFinderTheme
import com.zoqo.lottocombinationfinder.utils.calculateTotalCombinations
import com.zoqo.lottocombinationfinder.utils.findCombination
import java.math.BigInteger

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LottoCombinationFinderTheme {
                LottoApp()
            }
        }
    }
}

@Composable
fun LottoApp() {
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
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
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
                        val total = totalNumbers.text.toIntOrNull()
                        val choose = numbersToChoose.text.toIntOrNull()
                        val rank = rankInput.text.toBigIntegerOrNull()

                        if (total == null || choose == null || total <= 0 || choose <= 0 || total < choose) {
                            resultText = "Invalid input for total numbers or numbers to choose."
                        } else if (rank == null || rank <= BigInteger.ZERO || rank > calculateTotalCombinations(total, choose)) {
                            resultText = "Invalid rank. Enter a number between 1 and ${calculateTotalCombinations(total, choose)}"
                        } else {
                            val combination = findCombination(rank.toInt(), total, choose)
                            resultText = "Combination: ${combination.joinToString(", ")}"
                        }
                    }
                )

                LottoResult(resultText)
            }
        }
    )
}
