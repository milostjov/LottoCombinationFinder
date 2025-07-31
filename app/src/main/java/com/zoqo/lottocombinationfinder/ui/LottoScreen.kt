// LottoScreen.kt
package com.zoqo.lottocombinationfinder.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.zoqo.lottocombinationfinder.components.LottoInputFields
import com.zoqo.lottocombinationfinder.components.LottoResult2
import com.zoqo.lottocombinationfinder.components.calculateTotalCombinations
import com.zoqo.lottocombinationfinder.components.findCombination
import com.zoqo.lottocombinationfinder.data.AstroPreferencesManager
import com.zoqo.lottocombinationfinder.data.SavedCombinationsManager
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.math.BigInteger


@Suppress("NAME_SHADOWING")
@Composable
fun LottoApp(
    showRewardedAd: ((onReward: () -> Unit) -> Unit),
    restartAnimationKey: Long
) {
    val context = LocalContext.current
    var resultText by rememberSaveable { mutableStateOf("") }
//    var selectedPlanet by rememberSaveable { mutableStateOf("Jupiter") } // podrazumevana vrednost

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
            val (total, choose) = value
            totalNumbers = TextFieldValue(total)
            numbersToChoose = TextFieldValue(choose)
            val astroInput = AstroPreferencesManager.load(context).first()
            val savedRank = astroInput.rank.takeIf { it.isNotBlank() } ?: ""
            rankInput = TextFieldValue(savedRank)
            initialized = true
        }

    }

//    LaunchedEffect(initialized) {
//        if (initialized) {
//            snapshotFlow {
//                Triple(totalNumbers.text, numbersToChoose.text, rankInput.text)
//            }.collectLatest { (total, choose) ->
//                AstroPreferencesManager.saveLottoSettings(
//                    context = context.applicationContext,
//                    total = total,
//                    choose = choose
//
//                )
//            }
//        }
//    }

    LaunchedEffect(initialized) {
        if (initialized) {
            snapshotFlow {
                Triple(totalNumbers.text, numbersToChoose.text, rankInput.text)
            }.collectLatest { (total, choose, _) ->
                // Sačuvaj lotto podešavanja
                AstroPreferencesManager.saveLottoSettings(
                    context = context.applicationContext,
                    total = total,
                    choose = choose
                )

                // Ako su brojevi validni, automatski izračunaj i sačuvaj rank
                val totalInt = total.toIntOrNull()
                val chooseInt = choose.toIntOrNull()

                if (totalInt != null && chooseInt != null && totalInt > 0 && chooseInt > 0 && totalInt >= chooseInt) {
                    val astroInput = AstroPreferencesManager.load(context).first()
                    val planetName = astroInput.extraBodies?.firstOrNull() ?: "Mars"

                    val newRank = AstroRankCalculator.calculateRankFromPlanetDistance(
                        date = astroInput.date,
                        hour = astroInput.hour,
                        minute = astroInput.minute,
                        totalNumbers = totalInt,
                        numbersToChoose = chooseInt,
                        planetName = planetName
                    )

                    // Čuvamo rank odmah u DataStore
                    val updatedData = astroInput.copy(rank = newRank.toString())

                    AstroPreferencesManager.save(
                        context = context.applicationContext,
                        data = updatedData
                    )


                    // Ažuriramo polje na ekranu
                    rankInput = TextFieldValue(newRank.toString())
                }
            }
        }
    }


    Scaffold(

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

            )



            var showRewardDialog by remember { mutableStateOf(false) }

            GenerateButton(
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    showRewardDialog = true
                }
            )

            val coroutineScope = rememberCoroutineScope()
            if (showRewardDialog) {
                AlertDialog(
                    onDismissRequest = { showRewardDialog = false },
                    title = { Text(stringResource(R.string.watch_ad_title)) },
                    text = { Text(stringResource(R.string.watch_ad_description)) },
                    confirmButton = {
                        TextButton(onClick = {
                            showRewardDialog = false
                            showRewardedAd {

                                coroutineScope.launch {
                                    // Učitavamo lotto i astro podešavanja direktno iz DataStore-a
                                    val lottoSettings = AstroPreferencesManager.loadLottoSettings(context).first()
                                    val astroSettings = AstroPreferencesManager.load(context).first()

                                    val total = lottoSettings.first.toIntOrNull()
                                    val choose = lottoSettings.second.toIntOrNull()
                                    val rank = astroSettings.rank.toBigIntegerOrNull()

                                    resultText = when {
                                        total == null || choose == null || total <= 0 || choose <= 0 || total < choose ->
                                            context.getString(R.string.error_invalid_total_choose)

                                        rank == null || rank <= BigInteger.ZERO || rank > calculateTotalCombinations(total, choose) ->
                                            context.getString(R.string.error_invalid_rank, calculateTotalCombinations(total, choose))

                                        else -> {
                                            val combination = findCombination(rank.toInt(), total, choose)
                                            val result = context.getString(
                                                R.string.result_combination,
                                                combination.joinToString(", ")
                                            )

                                            // Sačuvaj rezultat
                                            SavedCombinationsManager.saveCombination(
                                                context = context,
                                                combination = result,
                                                date = astroSettings.date,
                                                hour = astroSettings.hour,
                                                minute = astroSettings.minute,
                                                totalNumbers = total,
                                                numbersToChoose = choose,
                                                planetName = astroSettings.extraBodies?.firstOrNull() ?: "Mars"
                                            )

                                            result
                                        }

                                    }
                                }
                            }
                        }) {
                            Text(stringResource(R.string.watch_ad_confirm))
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showRewardDialog = false }) {
                            Text(stringResource(R.string.cancel))
                        }
                    }
                )
            }



            LottoResult2(resultText, restartAnimationKey = restartAnimationKey)
        }
    }



}






