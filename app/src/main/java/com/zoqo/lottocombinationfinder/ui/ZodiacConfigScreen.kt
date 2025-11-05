//ZodiacConfigScreen.kt
package com.zoqo.lottocombinationfinder.ui


import android.content.Context
import android.widget.Toast
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.zoqo.lottocombinationfinder.R
import com.zoqo.lottocombinationfinder.components.AnimatedRankDisplay
import com.zoqo.lottocombinationfinder.components.AstroRankCalculator
import com.zoqo.lottocombinationfinder.components.calculateTotalCombinations
import com.zoqo.lottocombinationfinder.data.AstroPreferencesManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.math.BigInteger
import java.sql.Date
import java.text.DateFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale
import com.zoqo.lottocombinationfinder.astro.AstroCalculator
import com.zoqo.lottocombinationfinder.astro.AstroInterpreter
import com.zoqo.lottocombinationfinder.astro.RetrogradeAdvice
import com.zoqo.lottocombinationfinder.ui.AstroInputData
import swisseph.SweConst
private val PLANET_KEY_TO_ID = mapOf(
    "Sun" to SweConst.SE_SUN,
    "Moon" to SweConst.SE_MOON,
    "Mercury" to SweConst.SE_MERCURY,
    "Venus" to SweConst.SE_VENUS,
    "Mars" to SweConst.SE_MARS,
    "Jupiter" to SweConst.SE_JUPITER,
    "Saturn" to SweConst.SE_SATURN,
    "Uranus" to SweConst.SE_URANUS,
    "Neptune" to SweConst.SE_NEPTUNE,
    "Pluto" to SweConst.SE_PLUTO
)

private fun symbolFor(planetId: Int): String = when (planetId) {
    SweConst.SE_SUN -> "☉"
    SweConst.SE_MOON -> "☽"
    SweConst.SE_MERCURY -> "☿"
    SweConst.SE_VENUS -> "♀"
    SweConst.SE_MARS -> "♂"
    SweConst.SE_JUPITER -> "♃"
    SweConst.SE_SATURN -> "♄"
    SweConst.SE_URANUS -> "♅"
    SweConst.SE_NEPTUNE -> "♆"
    SweConst.SE_PLUTO -> "♇"
    else -> "?"
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AstroUserInputScreen(
    onConfirm: (AstroInputData) -> Unit,

    ) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()


    var birthDate by remember { mutableStateOf(LocalDate.now()) }
    var birthHour by remember { mutableStateOf(12) }
    var birthMinute by remember { mutableStateOf(0) }

    var inputData by remember {
        mutableStateOf(
            AstroInputData(
                date = java.time.LocalDate.now(),
                hour = 12,
                minute = 0
            )
        )
    }
    // u AstroUserInputScreen
    var selectedPlanet by remember { mutableStateOf("Mars") } // podrazumevana planeta

    // izvedeni ID iz string ključa – koristi se za retro upozorenje
    val selectedPlanetId by remember(selectedPlanet) {
        mutableStateOf(PLANET_KEY_TO_ID[selectedPlanet] ?: SweConst.SE_MARS)
    }

    val datePickerState = rememberDatePickerState()
    var showDatePicker by remember { mutableStateOf(false) }
    val formattedDate = remember(birthDate) {
        DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date.valueOf(birthDate.toString()))
    }

    var formattedTime = remember(birthHour, birthMinute) {
        String.format(Locale.getDefault(), "%02d:%02d", birthHour, birthMinute)
    }
    var rankInput by rememberSaveable { mutableStateOf("") }

    var showRankDialog by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()


    // Load preferences
    var isLoaded by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        AstroPreferencesManager.load(context).first().let { data ->
            birthDate = data.date
            birthHour = data.hour
            birthMinute = data.minute
            rankInput = data.rank
            selectedPlanet = data.extraBodies?.firstOrNull() ?: "Mars"
            isLoaded = true
        }
    }

    if (!isLoaded) {
        // prikaži skeleton/loader umesto inputa
        CircularProgressIndicator(modifier = Modifier.padding(24.dp))
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {


        Box(modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = formattedDate,
                onValueChange = {},
                label = { Text(stringResource(R.string.birth_date)) },
                modifier = Modifier.fillMaxWidth(),
                readOnly = true,
                enabled = true,
                trailingIcon = {
                    Icon(Icons.Filled.DateRange, contentDescription = null)
                }
            )
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clickable { showDatePicker = true }
            )
        }

        if (showDatePicker) {
            DatePickerDialog(
                onDismissRequest = { showDatePicker = false },
                confirmButton = {
                    TextButton(onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            birthDate = Instant.ofEpochMilli(millis)
                                .atZone(ZoneId.systemDefault()).toLocalDate()
                            inputData = inputData.copy(date = birthDate)
                            savePreferences(context, scope, birthDate, birthHour, birthMinute, selectedPlanet, rankInput)

                        }
                        showDatePicker = false
                    }) { Text(stringResource(R.string.ok)) }
                },
                dismissButton = {
                    TextButton(onClick = { showDatePicker = false }) { Text(stringResource(R.string.cancel)) }
                }
            ) {
                DatePicker(state = datePickerState)
            }
        }


        var showTimePicker by remember { mutableStateOf(false) }


        Box(modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = formattedTime,
                onValueChange = {},
                label = { Text(stringResource(R.string.birth_time)) },
                modifier = Modifier.fillMaxWidth(),
                readOnly = true,
                enabled = true,
                trailingIcon = {
                    Icon(Icons.Filled.Schedule, contentDescription = null)
                }
            )
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clickable { showTimePicker = true }   // => radi
            )
        }

// 3️⃣  Dijalog ostaje identičan
        if (showTimePicker) {
            CustomTimePickerDialog(
                initialHour = birthHour,
                initialMinute = birthMinute,
                is24Hour = true,
                onDismissRequest = { showTimePicker = false },
                onTimeSelected = { hour, minute ->
                    birthHour = hour
                    birthMinute = minute
                    formattedTime = String.format(Locale.getDefault(), "%02d:%02d", hour, minute) // osveži prikaz
                    inputData = inputData.copy(hour = birthHour, minute = birthMinute)
                    savePreferences(context, scope, birthDate, birthHour, birthMinute, selectedPlanet, rankInput)

                }
            )
        }




        LuckyPlanetPicker(
            selectedPlanetKey = selectedPlanet,
            onPlanetSelected = {
                selectedPlanet = it
                savePreferences(context, scope, birthDate, birthHour, birthMinute, selectedPlanet, rankInput)

            }

        )
        RetrogradeInlineWarning(
            inputData = inputData,
            planetId = selectedPlanetId
        )


// Dodaj proveru validnosti
        var totalNumbers by remember { mutableStateOf(39) }
        var numbersToChoose by remember { mutableStateOf(7) }
        //val context = LocalContext.current

        LaunchedEffect(Unit) {
            val (totalStr, chooseStr) = AstroPreferencesManager
                .loadLottoSettings(context)
                .first()
            totalNumbers = totalStr.toIntOrNull() ?: 39
            numbersToChoose = chooseStr.toIntOrNull() ?: 7
        }



        RankDisplayField(rankInput = rankInput)





        if (showRankDialog) {
            RankInfoDialog(onDismiss = { showRankDialog = false })
        }



        // Pratimo promene relevantnih polja i automatski ažuriramo rank
        LaunchedEffect(birthDate, birthHour, birthMinute, selectedPlanet) {
            coroutineScope.launch {
                //val (totalStr, chooseStr) = AstroPreferencesManager.loadLottoSettings(context).first()
               // val totalNumbers = totalStr.toIntOrNull() ?: 39
               // val numbersToChoose = chooseStr.toIntOrNull() ?: 7

                val rank = AstroRankCalculator.calculateRankFromPlanetDistance(
                    date = birthDate,
                    hour = birthHour,
                    minute = birthMinute,
                    totalNumbers = totalNumbers,
                    numbersToChoose = numbersToChoose,
                    planetName = selectedPlanet
                )

                rankInput = rank.toString()
                savePreferences(context, scope, birthDate, birthHour, birthMinute, selectedPlanet, rank.toString())
            }
        }





        Button(
            onClick = {
                coroutineScope.launch {

//                    val (totalStr, chooseStr) = AstroPreferencesManager
//                        .loadLottoSettings(context)
//                        .first()

                   // val totalNumbers = totalStr.toIntOrNull() ?: 39
                   // val numbersToChoose = chooseStr.toIntOrNull() ?: 7

                    val rank = rankInput.toBigIntegerOrNull()
                    val maxRank = calculateTotalCombinations(totalNumbers, numbersToChoose)

                    if (rank != null && rank > BigInteger.ZERO && rank <= maxRank) {
                        onConfirm(
                            AstroInputData(
                                birthDate,
                                birthHour,
                                birthMinute,
                                listOf(selectedPlanet),
                                rankInput
                            )
                        )
                    } else {
                        Toast.makeText(
                            context,
                            context.getString(R.string.error_invalid_rank, maxRank.toLong()),
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            Text(stringResource(R.string.ok))
        }





        Spacer(modifier = Modifier.height(16.dp))

    }
}

@Composable
fun RankDisplayField(rankInput: String) {
    val context = LocalContext.current
    var totalNumbers by remember { mutableStateOf(49) }
    var numbersToChoose by remember { mutableStateOf(6) }
    var showRankDialog by remember { mutableStateOf(false) }

    // ✅ Učitavanje vrednosti iz memorije
    LaunchedEffect(Unit) {
        val (savedTotal, savedChoose) = loadSavedValues(context)
        totalNumbers = savedTotal
        numbersToChoose = savedChoose
    }

    val animatedRank = AnimatedRankDisplay(
        targetNumber = rankInput.toIntOrNull() ?: 0
    )

    // ✅ Pastelne boje
    val pastelGreen = Color(0xFFA8E6CF)
    val pastelRed = Color(0xFFFF8B94)

    // ✅ Izračunavanje procenta za gradijent boje
    val rankValue = try {
        BigInteger(rankInput)
    } catch (e: NumberFormatException) {
        BigInteger.ZERO
    }

    val maxRank = BigInteger.valueOf(
        calculateTotalCombinations(totalNumbers, numbersToChoose).toLong()
    )

    val percentage = if (maxRank > BigInteger.ZERO) {
        (rankValue.toFloat() / maxRank.toFloat()).coerceIn(0f, 1f)
    } else 0f

// Ako je manji rank bolji (bliže 1 = najbolja kombinacija)
    val adjustedPercentage = 1f - percentage

    val rankColor = lerp(pastelRed, pastelGreen, adjustedPercentage)


    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // ✅ Naslov sa info ikonicom
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = stringResource(R.string.enter_rank_hint),
                style = MaterialTheme.typography.titleMedium
            )
            IconButton(onClick = { showRankDialog = true }) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_info),
                    contentDescription = "Info",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .width(20.dp)
                        .height(20.dp)
                )
            }
        }

        // ✅ Prikaz animiranog ranga sa PASTEL gradijent bojom
        Text(
            text = animatedRank,
            style = MaterialTheme.typography.displayLarge.copy(color = rankColor),
            modifier = Modifier.padding(top = 8.dp)
        )
    }

    // ✅ Alert dialog sa detaljnim opisom
    if (showRankDialog) {
        AlertDialog(
            onDismissRequest = { showRankDialog = false },
            confirmButton = {
                TextButton(onClick = { showRankDialog = false }) {
                    Text("OK")
                }
            },
            title = { Text(stringResource(R.string.enter_rank_hint)) },
            text = { Text(stringResource(R.string.rank_info_text)) }
        )
    }
}


@Composable
fun RetrogradeInlineWarning(
    inputData: AstroInputData,
    planetId: Int
) {
    val context = LocalContext.current
    var warning by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(inputData, planetId) {
        val planets = AstroCalculator.getPlanetPositions(inputData)
        val advisories = AstroInterpreter.buildRetrogradeAdvisories(context, planets)

        val symbol = symbolFor(planetId)
        val adv = advisories.find { it.symbol == symbol }

        warning = if (adv?.isRetrograde == true) {
            // koristi string iz resources
            AstroInterpreter.shortBadgeText(context, adv)
        } else null
    }

    warning?.let { msg ->
        Spacer(Modifier.height(8.dp))
        Text(
            text = msg,
            color = Color.Red, // 🔴 crveno upozorenje
            style = MaterialTheme.typography.bodySmall
        )
    }
}





// Data holders

data class AstroInputData(
    val date: LocalDate,
    val hour: Int,
    val minute: Int,
    val extraBodies: List<String>? = null,     // npr. ["Ceres","Chiron"],
    val rank: String = ""

)

@Composable
fun LuckyPlanetPicker(
    selectedPlanetKey: String,
    onPlanetSelected: (String) -> Unit
) {
    val allPlanets = listOf(
        "Sun" to stringResource(R.string.planet_sun),
        "Moon" to stringResource(R.string.planet_moon),
        "Mercury" to stringResource(R.string.planet_mercury),
        "Venus" to stringResource(R.string.planet_venus),
        "Mars" to stringResource(R.string.planet_mars),
        "Jupiter" to stringResource(R.string.planet_jupiter),
        "Saturn" to stringResource(R.string.planet_saturn),
        "Uranus" to stringResource(R.string.planet_uranus),
        "Neptune" to stringResource(R.string.planet_neptune),
        "Pluto" to stringResource(R.string.planet_pluto)
    )

    var expanded by remember { mutableStateOf(false) }
    val selectedLabel = allPlanets.find { it.first == selectedPlanetKey }?.second ?: ""

    Box(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = selectedLabel,
            onValueChange = {},
            label = { Text(stringResource(R.string.select_lucky_planet)) },
            readOnly = true,
            trailingIcon = {
                Icon(
                    painter = painterResource(id = R.drawable.ic_saturn_light),
                    contentDescription = "Saturn",
                            //tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp) // standardna veličina ikona
                )
            },
            modifier = Modifier.fillMaxWidth()
        )


        // Klik bilo gde na tekstualno polje otvara meni
        Box(
            modifier = Modifier
                .matchParentSize()
                .clickable { expanded = true }
        )

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            allPlanets.forEach { (key, label) ->
                DropdownMenuItem(
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Image(
                                painter = painterResource(id = getPlanetIconRes(key)),
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(label)
                        }
                    },
                    onClick = {
                        onPlanetSelected(key)
                        expanded = false
                    }
                )
            }
        }
    }
}

@DrawableRes
fun getPlanetIconRes(key: String): Int {
    return when (key.lowercase()) {
        "sun" -> R.drawable.ic_sun
        "moon" -> R.drawable.ic_moon
        "mercury" -> R.drawable.ic_mercury
        "venus" -> R.drawable.ic_venus
        "mars" -> R.drawable.ic_mars
        "jupiter" -> R.drawable.ic_jupiter
        "saturn" -> R.drawable.ic_saturn_png
        "uranus" -> R.drawable.ic_uranus
        "neptune" -> R.drawable.ic_neptune
        "pluto" -> R.drawable.ic_pluto
        else -> R.drawable.ic_saturn_light
    }
}

fun savePreferences(
    context: Context,
    scope: CoroutineScope,   // ✅ ostaje taj „dugi“ scope
    date: LocalDate,
    hour: Int,
    minute: Int,
    planet: String,
    rank: String
) {
    val input = AstroInputData(
        date, hour, minute,
        extraBodies = listOf(planet),
        rank = rank
    )
    scope.launch {
        AstroPreferencesManager.save(context, input)   // sada se sigurno izvrši
    }
}

fun loadSavedValues(context: Context): Pair<Int, Int> {
    val sharedPreferences = context.getSharedPreferences("lotto_prefs", Context.MODE_PRIVATE)
    val totalNumbers = sharedPreferences.getInt("totalNumbers", 49) // default 49
    val numbersToChoose = sharedPreferences.getInt("numbersToChoose", 6) // default 6
    return totalNumbers to numbersToChoose
}
