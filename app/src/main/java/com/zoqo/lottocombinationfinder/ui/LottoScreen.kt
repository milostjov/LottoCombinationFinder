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
import androidx.compose.material3.OutlinedTextField
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zoqo.lottocombinationfinder.R
import com.zoqo.lottocombinationfinder.components.AstroRankCalculator
import com.zoqo.lottocombinationfinder.components.LottoInputFields
import com.zoqo.lottocombinationfinder.components.LottoResult2
import com.zoqo.lottocombinationfinder.components.calculateTotalCombinations
import com.zoqo.lottocombinationfinder.components.findCombination
import com.zoqo.lottocombinationfinder.data.AstroPreferencesManager
import com.zoqo.lottocombinationfinder.data.GameType
import com.zoqo.lottocombinationfinder.data.SavedCombinationsManager
import com.zoqo.lottocombinationfinder.viewmodel.CountryUi
import com.zoqo.lottocombinationfinder.viewmodel.LottoGamesViewModel

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
    val scope = rememberCoroutineScope()

    // UI state
    var resultText by rememberSaveable { mutableStateOf("") }
    var totalNumbers by rememberSaveable(stateSaver = TextFieldValue.Saver) { mutableStateOf(TextFieldValue()) }

    // Nova polja
    var bonusNumbersField by rememberSaveable(stateSaver = TextFieldValue.Saver) { mutableStateOf(TextFieldValue("")) }
    var bonusPoolField   by rememberSaveable(stateSaver = TextFieldValue.Saver) { mutableStateOf(TextFieldValue("")) }
    var drawsField       by rememberSaveable(stateSaver = TextFieldValue.Saver) { mutableStateOf(TextFieldValue("")) }

    var numbersToChoose by rememberSaveable(stateSaver = TextFieldValue.Saver) { mutableStateOf(TextFieldValue()) }
    var rankInput by rememberSaveable(stateSaver = TextFieldValue.Saver) { mutableStateOf(TextFieldValue()) }
    var selectedGame by rememberSaveable { mutableStateOf("") }
    // čuvamo CODE (npr. "us")
    var selectedCountry by rememberSaveable { mutableStateOf("") }

    // ViewModel
    val viewModel: LottoGamesViewModel = viewModel()
    val games by viewModel.lottoGames.collectAsState()
    val countriesUi: List<CountryUi> by viewModel.countriesDisplay.collectAsState()

    // prethodno sačuvani izbor (country code, game name)
    val selection by remember { AstroPreferencesManager.loadLottoSelection(context) }.collectAsState(initial = "" to "")

    // lotto settings (brojevi)
    val lottoSettings by remember { AstroPreferencesManager.loadLottoSettings(context) }.collectAsState(initial = null)

    var initialized by rememberSaveable { mutableStateOf(false) }

    // Inicijalni izbor kada stignu podaci
    LaunchedEffect(games, countriesUi) {
        if (games.isNotEmpty()) {
            // pokušaj da vratiš prethodni izbor
            if (selectedCountry.isEmpty() && selection.first.isNotEmpty()) {
                selectedCountry = selection.first
            }
            if (selectedGame.isEmpty() && selection.second.isNotEmpty()) {
                if (games.any { it.name == selection.second }) {
                    selectedGame = selection.second
                }
            }
            // fallback ako i dalje nema
            if (selectedCountry.isEmpty()) {
                selectedCountry = games.first().country
            }
            if (selectedGame.isEmpty()) {
                selectedGame = games.firstOrNull { it.country == selectedCountry }?.name
                    ?: games.first().name
            }
        }
    }

    // Popuni total/choose kad se promeni igra
    LaunchedEffect(selectedGame, games) {
        val selected = games.find { it.name == selectedGame }
        if (selected != null) {
            totalNumbers     = TextFieldValue(selected.mainPool.toString())
            numbersToChoose  = TextFieldValue(selected.mainNumbers.toString())

            // samo popuni tekstualna polja; UI će ih prikazati uslovno
            bonusNumbersField = TextFieldValue(selected.bonusNumbers.takeIf { it > 0 }?.toString() ?: "")
            bonusPoolField    = TextFieldValue(selected.bonusPool.takeIf { it > 0 }?.toString() ?: "")
            drawsField        = TextFieldValue(selected.draws.takeIf { it > 0 }?.toString() ?: "")
        }
    }



    // Učitaj ranije sačuvane brojeve na startu
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

    // Auto‑čuvanje brojeva + recalculacija rank-a
    LaunchedEffect(initialized) {
        if (initialized) {
            snapshotFlow { Triple(totalNumbers.text, numbersToChoose.text, rankInput.text) }
                .collectLatest { (total, choose, _) ->
                    AstroPreferencesManager.saveLottoSettings(
                        context = context.applicationContext,
                        total = total,
                        choose = choose
                    )

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

                        val updatedData = astroInput.copy(rank = newRank.toString())
                        AstroPreferencesManager.save(context = context.applicationContext, data = updatedData)
                        rankInput = TextFieldValue(newRank.toString())
                    }
                }
        }
    }

    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // prikazni label za izabranu zemlju
            val selectedCountryLabel =
                countriesUi.firstOrNull { it.code == selectedCountry }?.display
                    ?: selectedCountry.uppercase()

            // igre filtrirane po CODE-u zemlje
            val gameNamesForSelectedCountry = games
                .filter { selectedCountry.isEmpty() || it.country == selectedCountry }
                .map { it.name }

            LottoInputFields(
                commonLottoGames = gameNamesForSelectedCountry,
                selectedGame = selectedGame,
                onGameSelected = { game ->
                    selectedGame = game
                    scope.launch {
                        AstroPreferencesManager.saveLottoSelection(
                            context = context.applicationContext,
                            country = selectedCountry, // CODE
                            game = selectedGame
                        )
                    }
                },

                // šaljemo CountryUi (display + flagUrl + code),
                // a u polju prikazujemo selectedCountryLabel
                countries = countriesUi,
                selectedCountryLabel = selectedCountryLabel,
                onCountrySelected = { chosen ->
                    selectedCountry = chosen.code
                    selectedGame = games.firstOrNull { it.country == chosen.code }?.name ?: ""
                    scope.launch {
                        AstroPreferencesManager.saveLottoSelection(
                            context = context.applicationContext,
                            country = selectedCountry,
                            game = selectedGame
                        )
                    }
                },

                totalNumbers = totalNumbers,
                onTotalNumbersChange = { totalNumbers = it },
                numbersToChoose = numbersToChoose,
                onNumbersToChooseChange = { numbersToChoose = it }
            )


            val selectedCfg = games.firstOrNull { it.name == selectedGame }

            when (selectedCfg?.classType) {
                // BONUS igre (npr. Powerball, Mega Millions, EuroMillions)
                GameType.BONUS -> {
                    OutlinedTextField(
                        value = bonusNumbersField,
                        onValueChange = { bonusNumbersField = it },
                        label = { Text("Bonus numbers (count)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = bonusPoolField,
                        onValueChange = { bonusPoolField = it },
                        label = { Text("Bonus pool (max)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // MULTIDRAW igre (npr. Laki 6 — izvuče se više brojeva nego što igrač bira)
                GameType.MULTIDRAW -> {
                    OutlinedTextField(
                        value = drawsField,
                        onValueChange = { drawsField = it },
                        label = { Text("Draws (numbers drawn)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // CARD (tombola) i CLASSIC (6/49...) nemaju dodatnih polja ovde
                GameType.CARD, GameType.CLASSIC, null -> { /* no-op */ }
            }


            var showRewardDialog by remember { mutableStateOf(false) }

            LottoResult2(resultText, restartAnimationKey = restartAnimationKey)
            
            GenerateButton(
                modifier = Modifier.fillMaxWidth(),
                onClick = { showRewardDialog = true }
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
                                    //val lottoSettings = AstroPreferencesManager.loadLottoSettings(context).first()
                                    val astroSettings = AstroPreferencesManager.load(context).first()

                                    val total = totalNumbers.text.toIntOrNull()
                                    val choose = numbersToChoose.text.toIntOrNull()


                                    resultText = when {
                                        total == null || choose == null || total <= 0 || choose <= 0 || total < choose ->
                                            context.getString(R.string.error_invalid_total_choose)

                                        else -> {
                                            val selectedCfg = games.firstOrNull { it.name == selectedGame }
                                            val planet = astroSettings.extraBodies?.firstOrNull() ?: "Mars"

                                            // MAIN rank (deterministički)
                                            val rankMain = AstroRankCalculator.calculateRankFromPlanetDistance(
                                                date = astroSettings.date,
                                                hour = astroSettings.hour,
                                                minute = astroSettings.minute,
                                                totalNumbers = total,
                                                numbersToChoose = choose,
                                                planetName = planet,
                                                ticketIndex = 0           // ostavi 0 za kompatibilnost
                                            )
                                            val mainCombo = findCombination(rankMain.toInt(), total, choose)

// BONUS (ako postoji): izračunaj poseban rank na bonus skupu
                                            val bonusCombo: List<Int> = if (selectedCfg != null &&
                                                selectedCfg.classType == GameType.BONUS &&
                                                selectedCfg.bonusNumbers > 0 && selectedCfg.bonusPool > 0
                                            ) {
                                                val rankBonus = AstroRankCalculator.calculateRankFromPlanetDistance(
                                                    date = astroSettings.date,
                                                    hour = astroSettings.hour,
                                                    minute = astroSettings.minute,
                                                    totalNumbers = selectedCfg.bonusPool,
                                                    numbersToChoose = selectedCfg.bonusNumbers,
                                                    planetName = planet,
                                                    ticketIndex = 100       // fazni pomak da bude nezavisno od main
                                                )

                                                val rawBonus = findCombination(
                                                    rankBonus.toInt(),
                                                    selectedCfg.bonusPool,
                                                    selectedCfg.bonusNumbers
                                                )

                                                // 👇 OVDE ispravljamo duplikate
                                                adjustBonusNumbers(mainCombo, rawBonus, selectedCfg.bonusPool)
                                            } else emptyList()

                                            val merged = formatTicket(mainCombo, bonusCombo)

                                            // Snimi kao JEDAN tiket
                                            SavedCombinationsManager.saveCombination(
                                                context = context,
                                                combination = merged,
                                                date = astroSettings.date,
                                                hour = astroSettings.hour,
                                                minute = astroSettings.minute,
                                                totalNumbers = total,
                                                numbersToChoose = choose,
                                                planetName = planet,
                                                gameName = selectedGame               // NEW
                                            )

                                            merged
                                        }
                                    }
                                }
                            }
                        }) { Text(stringResource(R.string.watch_ad_confirm)) }
                    },
                    dismissButton = {
                        TextButton(onClick = { showRewardDialog = false }) {
                            Text(stringResource(R.string.cancel))
                        }
                    }
                )
            }


        }
    }
}

private fun formatTicket(main: List<Int>, bonus: List<Int> = emptyList()): String {
    return if (bonus.isEmpty()) {
        "Combination: " + main.joinToString(", ")
    } else {
        "Combination: " + main.joinToString(", ") + " + " + bonus.joinToString(", ")
    }
}
/**
 * Adjusts bonus numbers so they don't collide with main numbers (or each other),
 * using your rule: <= half -> +1, > half -> -1. Wraps inside 1..bonusPool.
 */
fun adjustBonusNumbers(
    main: List<Int>,
    rawBonus: List<Int>,
    bonusPool: Int
): List<Int> {
    require(bonusPool >= 1) { "bonusPool must be >= 1" }

    val mainSet = main.toHashSet()
    val taken   = mainSet.toMutableSet() // zauzeto (glavni + već prihvaćeni bonusi)
    val result  = mutableListOf<Int>()

    val half = bonusPool / 2.0

    for (orig in rawBonus) {
        var x = orig.coerceIn(1, bonusPool)

        // odredi smer: <= half => +1; > half => -1
        val step = if (x <= half) 1 else -1

        var attempts = 0
        while (x in taken && attempts < bonusPool) {
            x += step
            // wrap okolo
            if (x > bonusPool) x = 1
            if (x < 1)         x = bonusPool
            attempts++
        }

        result += x
        taken  += x
    }
    return result
}
