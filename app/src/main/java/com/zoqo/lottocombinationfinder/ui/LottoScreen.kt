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
import androidx.compose.material3.CircularProgressIndicator
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
import com.zoqo.lottocombinationfinder.components.findCombination
import com.zoqo.lottocombinationfinder.data.AstroPreferencesManager
import com.zoqo.lottocombinationfinder.data.GameType
import com.zoqo.lottocombinationfinder.data.SavedCombinationsManager
import com.zoqo.lottocombinationfinder.viewmodel.CountryUi
import com.zoqo.lottocombinationfinder.viewmodel.LottoGamesViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@Suppress("NAME_SHADOWING")
@Composable
fun LottoApp(
    showRewardedAd: ((onReward: () -> Unit) -> Unit),
    restartAnimationKey: Long
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // ViewModel state
    val viewModel: LottoGamesViewModel = viewModel()
    val games by viewModel.lottoGames.collectAsState()
    val countriesUi: List<CountryUi> by viewModel.countriesDisplay.collectAsState()

    // UI state
    var isLoaded by remember { mutableStateOf(false) }
    var resultText by rememberSaveable { mutableStateOf("") }

    var selectedGame by rememberSaveable { mutableStateOf("") }
    var selectedCountry by rememberSaveable { mutableStateOf("") } // code (npr. "us")

    var totalNumbers by rememberSaveable(stateSaver = TextFieldValue.Saver) { mutableStateOf(TextFieldValue()) }
    var numbersToChoose by rememberSaveable(stateSaver = TextFieldValue.Saver) { mutableStateOf(TextFieldValue()) }

    // dodatna polja (popunjavaju se po potrebi na osnovu igre)
    var bonusNumbersField by rememberSaveable(stateSaver = TextFieldValue.Saver) { mutableStateOf(TextFieldValue("")) }
    var bonusPoolField   by rememberSaveable(stateSaver = TextFieldValue.Saver) { mutableStateOf(TextFieldValue("")) }
    var drawsField       by rememberSaveable(stateSaver = TextFieldValue.Saver) { mutableStateOf(TextFieldValue("")) }

    // ---------- INITIAL LOAD (single gate) ----------
    LaunchedEffect(Unit) {
        // sačekaj da igre stignu (ako dolaze asinh.)
        while (games.isEmpty()) delay(30)

        // učitaj prethodni izbor (country, game) i lotto settings (total/choose) JEDNOM
        val (savedCountry, savedGame) = AstroPreferencesManager.loadLottoSelection(context).first()
        val savedSettings = AstroPreferencesManager.loadLottoSettings(context).first() // Pair<String,String>?

        // postavi zemlju (validiraj)
        val initialCountry = when {
            savedCountry.isNotBlank() && games.any { it.country == savedCountry } -> savedCountry
            else -> games.first().country
        }
        selectedCountry = initialCountry

        // postavi igru (validiraj; pokušaj savedGame u toj zemlji)
        val initialGame = when {
            savedGame.isNotBlank() && games.any { it.name == savedGame } -> savedGame
            else -> games.firstOrNull { it.country == initialCountry }?.name ?: games.first().name
        }
        selectedGame = initialGame

        // popuni polja iz selektovane igre (glavni/bonus/draws)
        fun applyGameConfig(name: String) {
            games.find { it.name == name }?.let { g ->
                totalNumbers    = TextFieldValue(g.mainPool.toString())
                numbersToChoose = TextFieldValue(g.mainNumbers.toString())
                bonusNumbersField = TextFieldValue(g.bonusNumbers.takeIf { it > 0 }?.toString() ?: "")
                bonusPoolField    = TextFieldValue(g.bonusPool.takeIf { it > 0 }?.toString() ?: "")
                drawsField        = TextFieldValue(g.draws.takeIf { it > 0 }?.toString() ?: "")
            }
        }
        applyGameConfig(initialGame)

        // prepiši eventualno sačuvane total/choose vrednosti
        savedSettings?.let { (t, c) ->
            if (t.isNotBlank()) totalNumbers = TextFieldValue(t)
            if (c.isNotBlank()) numbersToChoose = TextFieldValue(c)
        }

        isLoaded = true
    }

    // ---------- UI GATE ----------
    if (!isLoaded) {
        Scaffold { innerPadding ->
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize(),
                verticalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator(modifier = Modifier.padding(24.dp))
            }
        }
        return
    }

    fun onGameSelected(name: String) {
        if (selectedGame == name) return
        selectedGame = name

        games.find { it.name == name }?.let { g ->
            totalNumbers    = TextFieldValue(g.mainPool.toString())
            numbersToChoose = TextFieldValue(g.mainNumbers.toString())
            bonusNumbersField = TextFieldValue(g.bonusNumbers.takeIf { it > 0 }?.toString() ?: "")
            bonusPoolField    = TextFieldValue(g.bonusPool.takeIf { it > 0 }?.toString() ?: "")
            drawsField        = TextFieldValue(g.draws.takeIf { it > 0 }?.toString() ?: "")
        }

        scope.launch {
            AstroPreferencesManager.saveLottoSelection(
                context = context.applicationContext,
                country = selectedCountry,
                game = selectedGame
            )
        }
    }

    // ---------- EVENT HANDLERS (umesto LaunchedEffect reakcija) ----------
    fun onCountrySelected(new: CountryUi) {
        if (selectedCountry == new.code) return
        selectedCountry = new.code
        // prva igra za zemlju
        val fallback = games.firstOrNull { it.country == new.code }?.name ?: return
        onGameSelected(fallback)
        // zapamti izbor
        scope.launch {
            AstroPreferencesManager.saveLottoSelection(
                context = context.applicationContext,
                country = selectedCountry,
                game = selectedGame
            )
        }
    }



    // ---------- AUTO-SAVE lotto settings + recalculation (debounce & idempotent) ----------
    LaunchedEffect(isLoaded) {
        if (!isLoaded) return@LaunchedEffect

        var lastSaved: Pair<String, String>? = null

        snapshotFlow { totalNumbers.text to numbersToChoose.text }
            .distinctUntilChanged()
            .collectLatest { (total, choose) ->
                val ti = total.toIntOrNull()
                val ci = choose.toIntOrNull()
                if (ti == null || ci == null || ti <= 0 || ci <= 0 || ti < ci) return@collectLatest

                // izbegni dupli zapis istih vrednosti
                if (lastSaved == (total to choose)) return@collectLatest

                AstroPreferencesManager.saveLottoSettings(
                    context = context.applicationContext,
                    total = total,
                    choose = choose
                )
                lastSaved = total to choose

                // opcioni proračun rank-a i zapis (bez “ping-ponga”)
                val astroInput = AstroPreferencesManager.load(context).first()
                val planet = astroInput.extraBodies?.firstOrNull() ?: "Mars"
                val newRank = AstroRankCalculator.calculateRankFromPlanetDistance(
                    date = astroInput.date,
                    hour = astroInput.hour,
                    minute = astroInput.minute,
                    totalNumbers = ti,
                    numbersToChoose = ci,
                    planetName = planet
                )
                AstroPreferencesManager.save(
                    context = context.applicationContext,
                    data = astroInput.copy(rank = newRank.toString())
                )
            }
    }

    // ---------- UI ----------
    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // label za prikaz zemlje
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
                onGameSelected = ::onGameSelected,

                countries = countriesUi,
                selectedCountryLabel = selectedCountryLabel,
                onCountrySelected = ::onCountrySelected,

                totalNumbers = totalNumbers,
                onTotalNumbersChange = { totalNumbers = it },
                numbersToChoose = numbersToChoose,
                onNumbersToChooseChange = { numbersToChoose = it }
            )

            val selectedCfg = games.firstOrNull { it.name == selectedGame }

            when (selectedCfg?.classType) {
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
                GameType.MULTIDRAW -> {
                    OutlinedTextField(
                        value = drawsField,
                        onValueChange = { drawsField = it },
                        label = { Text("Draws (numbers drawn)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
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
                                    val astroSettings = AstroPreferencesManager.load(context).first()
                                    val total = totalNumbers.text.toIntOrNull()
                                    val choose = numbersToChoose.text.toIntOrNull()

                                    resultText = when {
                                        total == null || choose == null || total <= 0 || choose <= 0 || total < choose ->
                                            context.getString(R.string.error_invalid_total_choose)

                                        else -> {
                                            val currentCfg = games.firstOrNull { it.name == selectedGame }
                                            val planet = astroSettings.extraBodies?.firstOrNull() ?: "Mars"

                                            // MAIN
                                            val rankMain = AstroRankCalculator.calculateRankFromPlanetDistance(
                                                date = astroSettings.date,
                                                hour = astroSettings.hour,
                                                minute = astroSettings.minute,
                                                totalNumbers = total,
                                                numbersToChoose = choose,
                                                planetName = planet,
                                                ticketIndex = 0
                                            )
                                            val mainCombo = findCombination(rankMain.toInt(), total, choose)

                                            // BONUS (ako postoji)
                                            val bonusCombo: List<Int> =
                                                if (currentCfg != null &&
                                                    currentCfg.classType == GameType.BONUS &&
                                                    currentCfg.bonusNumbers > 0 && currentCfg.bonusPool > 0
                                                ) {
                                                    val rankBonus = AstroRankCalculator.calculateRankFromPlanetDistance(
                                                        date = astroSettings.date,
                                                        hour = astroSettings.hour,
                                                        minute = astroSettings.minute,
                                                        totalNumbers = currentCfg.bonusPool,
                                                        numbersToChoose = currentCfg.bonusNumbers,
                                                        planetName = planet,
                                                        ticketIndex = 100
                                                    )

                                                    val rawBonus = findCombination(
                                                        rankBonus.toInt(),
                                                        currentCfg.bonusPool,
                                                        currentCfg.bonusNumbers
                                                    )
                                                    adjustBonusNumbers(mainCombo, rawBonus, currentCfg.bonusPool)
                                                } else emptyList()

                                            val merged = formatTicket(mainCombo, bonusCombo)

                                            SavedCombinationsManager.saveCombination(
                                                context = context,
                                                combination = merged,
                                                date = astroSettings.date,
                                                hour = astroSettings.hour,
                                                minute = astroSettings.minute,
                                                totalNumbers = total,
                                                numbersToChoose = choose,
                                                planetName = planet,
                                                gameName = selectedGame
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
    val taken   = mainSet.toMutableSet()
    val result  = mutableListOf<Int>()

    val half = bonusPool / 2.0

    for (orig in rawBonus) {
        var x = orig.coerceIn(1, bonusPool)
        val step = if (x <= half) 1 else -1

        var attempts = 0
        while (x in taken && attempts < bonusPool) {
            x += step
            if (x > bonusPool) x = 1
            if (x < 1)         x = bonusPool
            attempts++
        }

        result += x
        taken  += x
    }
    return result
}
