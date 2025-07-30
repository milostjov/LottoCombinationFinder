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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Button
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.zoqo.lottocombinationfinder.R
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

    ////
    // u AstroUserInputScreen
    var selectedPlanet by remember { mutableStateOf("Mars") } // podrazumevana planeta
    var extraBodies by remember { mutableStateOf(emptyList<String>()) }
    var extraBodiesText by remember { mutableStateOf("") }

    val datePickerState = rememberDatePickerState()
    var showDatePicker by remember { mutableStateOf(false) }
    val formattedDate = remember(birthDate) {
        DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date.valueOf(birthDate.toString()))
    }

    var formattedTime = remember(birthHour, birthMinute) {
        String.format("%02d:%02d", birthHour, birthMinute)
    }
    var rankInput by rememberSaveable { mutableStateOf("") }

    var showRankDialog by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()


    // Load preferences
    LaunchedEffect(Unit) {
        AstroPreferencesManager.load(context).collectLatest { data ->
            birthDate = data.date
            birthHour = data.hour
            birthMinute = data.minute
            rankInput = data.rank
            selectedPlanet = data.extraBodies?.firstOrNull() ?: "Mars"

            extraBodies = data.extraBodies ?: emptyList()
            extraBodiesText = extraBodies.joinToString(", ")

        }
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
                    formattedTime = String.format("%02d:%02d", hour, minute) // osveži prikaz
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


// Dodaj proveru validnosti
        var totalNumbers by remember { mutableStateOf(39) }
        var numbersToChoose by remember { mutableStateOf(7) }
        val context = LocalContext.current

        LaunchedEffect(Unit) {
            val (totalStr, chooseStr) = AstroPreferencesManager
                .loadLottoSettings(context)
                .first()
            totalNumbers = totalStr.toIntOrNull() ?: 39
            numbersToChoose = chooseStr.toIntOrNull() ?: 7
        }

        val maxRank = calculateTotalCombinations(totalNumbers, numbersToChoose)

        val isRankInvalid = rankInput.isNotEmpty() &&
                (rankInput.toBigIntegerOrNull() == null ||
                        rankInput.toBigInteger() <= BigInteger.ZERO ||
                        rankInput.toBigInteger() > maxRank)


        OutlinedTextField(
            value = rankInput,
            onValueChange = { newValue ->
                rankInput = newValue
                savePreferences(context, scope, birthDate, birthHour, birthMinute, selectedPlanet, newValue)
            },
            label = { Text(stringResource(R.string.enter_rank_hint)) },
            keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
            isError = isRankInvalid,   // <-- Ovde dodajemo
            supportingText = {
                if (isRankInvalid) {
                    Text(
                        text = context.getString(R.string.error_invalid_rank_range, maxRank.toLong()),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            modifier = Modifier.fillMaxWidth(),
            trailingIcon = {
                IconButton(onClick = { showRankDialog = true }) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_info),
                        contentDescription = "Info",
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        )



        if (showRankDialog) {
            RankInfoDialog(onDismiss = { showRankDialog = false })
        }

        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                coroutineScope.launch {
                    val astroInput = AstroPreferencesManager
                        .load(context)
                        .first()

                    val (totalStr, chooseStr) = AstroPreferencesManager
                        .loadLottoSettings(context)
                        .first()

                    val totalNumbers = totalStr.toIntOrNull() ?: 39
                    val numbersToChoose = chooseStr.toIntOrNull() ?: 7

                    val rank = AstroRankCalculator.calculateRankFromPlanetDistance(
                        date = astroInput.date,
                        hour = astroInput.hour,
                        minute = astroInput.minute,
                        totalNumbers = totalNumbers,
                        numbersToChoose = numbersToChoose,
                        planetName = astroInput.extraBodies?.firstOrNull() ?: "Mars"
                    )

                    rankInput = rank.toString()

                    // ✅ Sačuvaj automatski generisan rank
                    savePreferences(
                        context = context,
                        scope = this,
                        date = astroInput.date,
                        hour = astroInput.hour,
                        minute = astroInput.minute,
                        planet = astroInput.extraBodies?.firstOrNull() ?: "Mars",
                        rank = rank.toString()
                    )
                }
            }
        ) {
            Text(stringResource(R.string.generate_rank_num))
        }


        Button(
            onClick = {
                coroutineScope.launch {

                    val (totalStr, chooseStr) = AstroPreferencesManager
                        .loadLottoSettings(context)
                        .first()

                    val totalNumbers = totalStr.toIntOrNull() ?: 39
                    val numbersToChoose = chooseStr.toIntOrNull() ?: 7

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

fun isInputValid(
    birthDate: LocalDate?,
    birthHour: Int?,
    birthMinute: Int?,
    selectedPlanet: String?,
    rankInput: String?
): Boolean {
    return birthDate != null &&
            birthHour != null &&
            birthMinute != null &&
            !selectedPlanet.isNullOrEmpty() &&
            !rankInput.isNullOrEmpty() &&
            rankInput.all { it.isDigit() }
}

fun calculateMaxCombinations(n: Int, k: Int): Long {
    fun factorial(x: Int): Long = if (x <= 1) 1 else x * factorial(x - 1)
    return factorial(n) / (factorial(k) * factorial(n - k))
}
