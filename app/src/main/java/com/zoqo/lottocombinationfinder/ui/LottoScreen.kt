// LottoScreen.kt
package com.zoqo.lottocombinationfinder.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.zoqo.lottocombinationfinder.R
import com.zoqo.lottocombinationfinder.components.AstroRankCalculator
import com.zoqo.lottocombinationfinder.components.GenerateButton
import com.zoqo.lottocombinationfinder.components.LottoInputFields
import com.zoqo.lottocombinationfinder.components.LottoResult
import com.zoqo.lottocombinationfinder.data.AstroPreferencesManager
import com.zoqo.lottocombinationfinder.utils.calculateTotalCombinations
import com.zoqo.lottocombinationfinder.utils.findCombination
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.math.BigInteger





@Composable
fun LottoApp(
    showRewardedAd: ((onReward: () -> Unit) -> Unit),
    onAstroSettingsClick: () -> Unit
) {
    val context = LocalContext.current
    var resultText by rememberSaveable { mutableStateOf("") }

    var totalNumbers by rememberSaveable(stateSaver = TextFieldValue.Saver) { mutableStateOf(TextFieldValue()) }
    var numbersToChoose by rememberSaveable(stateSaver = TextFieldValue.Saver) { mutableStateOf(TextFieldValue()) }
    var rankInput by rememberSaveable(stateSaver = TextFieldValue.Saver) { mutableStateOf(TextFieldValue()) }

    val lottoSettings by remember {
        AstroPreferencesManager.loadLottoSettings(context)
    }.collectAsState(initial = null)

    var initialized by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(lottoSettings) {
        val value = lottoSettings
        if (!initialized && value != null) {
            val (total, choose, rank) = value
            totalNumbers = TextFieldValue(total)
            numbersToChoose = TextFieldValue(choose)
            if (rankInput.text.isBlank()) {
                rankInput = TextFieldValue(rank)
            }
            initialized = true
        }

    }

    LaunchedEffect(initialized) {
        if (initialized) {
            snapshotFlow {
                Triple(totalNumbers.text, numbersToChoose.text, rankInput.text)
            }.collectLatest { (total, choose, rank) ->
                AstroPreferencesManager.saveLottoSettings(
                    context = context.applicationContext,
                    total = total,
                    choose = choose,
                    rank = rank
                )
            }
        }
    }

    Scaffold(
//        bottomBar = {
//            BannerAdView() // fiksiran pri dnu
//        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            LottoInputFields(
                totalNumbers = totalNumbers,
                onTotalNumbersChange = { totalNumbers = it },
                numbersToChoose = numbersToChoose,
                onNumbersToChooseChange = { numbersToChoose = it },
                rankInput = rankInput,
                onRankInputChange = { rankInput = it }
            )

            //Text(stringResource(R.string.or_generate_rank_num))
            val coroutineScope = rememberCoroutineScope()

            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    coroutineScope.launch {
                        // ➊ Uzmemo SAČUVANE astro podatke (datum, sat, minut) – samo jedan emit
                        val astroInput = AstroPreferencesManager
                            .load(context)
                            .first()

                        // ➋ Uzmemo SAČUVANE lotto postavke (total / choose) – samo jedan emit
                        val (totalStr, chooseStr, _) = AstroPreferencesManager
                            .loadLottoSettings(context)
                            .first()

                        val totalNumbers = totalStr.toIntOrNull() ?: 39   // fallback
                        val numbersToChoose = chooseStr.toIntOrNull() ?: 7 // fallback

                        // ➌ Izračunamo rank
                        val rank = AstroRankCalculator.calculateRankFromSunSign(
                            birthDate = astroInput.date,
                            birthHour = astroInput.hour,
                            birthMinute = astroInput.minute,
                            totalNumbers = totalNumbers,
                            numbersToChoose = numbersToChoose
                        )

                        // ➍ Upis u polje – i dalje ga korisnik može menjati
                        rankInput = TextFieldValue(rank.toString())
                    }
                }
            ) {
                Text(stringResource(R.string.or_generate_rank_num))
            }




            GenerateButton(
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    showRewardedAd {
                        val total = totalNumbers.text.toIntOrNull()
                        val choose = numbersToChoose.text.toIntOrNull()
                        val rank = rankInput.text.toBigIntegerOrNull()

                        resultText = when {
                            total == null || choose == null || total <= 0 || choose <= 0 || total < choose ->
                                context.getString(R.string.error_invalid_total_choose)

                            rank == null || rank <= BigInteger.ZERO || rank > calculateTotalCombinations(total, choose) ->
                                context.getString(R.string.error_invalid_rank, calculateTotalCombinations(total, choose))

                            else -> {
                                val combination = findCombination(rank.toInt(), total, choose)
                                context.getString(
                                    R.string.result_combination,
                                    combination.joinToString(", ")
                                )
                            }
                        }
                    }
                }
            )

            LottoResult(resultText)
        }
    }
}






