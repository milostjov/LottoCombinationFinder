package com.zoqo.lottocombinationfinder.ui

import android.app.TimePickerDialog
import android.content.Context
import android.text.format.DateFormat.is24HourFormat
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.zoqo.lottocombinationfinder.R
import com.zoqo.lottocombinationfinder.components.CitySearchField
import com.zoqo.lottocombinationfinder.data.AstroPreferencesManager
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.sql.Date
import java.text.DateFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import com.zoqo.lottocombinationfinder.ui.BannerAdView
import android.location.Geocoder
import androidx.compose.runtime.rememberUpdatedState
import org.maplibre.android.geometry.LatLng
import java.util.Locale


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AstroUserInputScreen(
    onConfirm: (AstroInputData) -> Unit,
    onOpenMap: (Double, Double) -> Unit,
    initialLat: Double,
    initialLon: Double,
    //pickedLocation: LatLng?
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var birthDate by remember { mutableStateOf(LocalDate.now()) }
    var birthHour by remember { mutableStateOf(12) }
    var birthMinute by remember { mutableStateOf(0) }

    var latitude  by remember { mutableStateOf(initialLat) }
    var longitude by remember { mutableStateOf(initialLon) }

    LaunchedEffect(initialLat, initialLon) {
        latitude  = initialLat
        longitude = initialLon
    }



    var selectedHouseSystem by remember { mutableStateOf(AstroHouseSystem.PLACIDUS) }
    val datePickerState = rememberDatePickerState()
    var showDatePicker by remember { mutableStateOf(false) }
    val formattedDate = remember(birthDate) {
        DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date.valueOf(birthDate.toString()))
    }

    val formattedTime = remember(birthHour, birthMinute) {
        String.format("%02d:%02d", birthHour, birthMinute)
    }
    var showTimePicker by remember { mutableStateOf(false) }
    var expanded by remember { mutableStateOf(false) }
    val selectedText = remember(selectedHouseSystem) { selectedHouseSystem.toString() }
    var cityName by remember { mutableStateOf("") }

    LaunchedEffect(latitude, longitude) {
        val name = getCityName(context, latitude, longitude)
        if (!name.isNullOrBlank()) {
            cityName = name
        }
    }



    // Load preferences
    LaunchedEffect(Unit) {
        AstroPreferencesManager.load(context).collectLatest { data ->
            birthDate = data.date
            birthHour = data.hour
            birthMinute = data.minute
            latitude = data.latitude
            longitude = data.longitude
            selectedHouseSystem = data.houseSystem
            cityName = data.cityName

            // reverse geocoding ako cityName nije sačuvan
            if (cityName.isBlank()) {
                getCityName(context, data.latitude, data.longitude)?.let {
                    cityName = it
                }}
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            BannerAdView()
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(stringResource(R.string.enter_birth_date))

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

            Text(stringResource(R.string.enter_birth_time))

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
                        .clickable { showTimePicker = true }
                )
            }

            if (showTimePicker) {
                TimePickerDialog(
                    context,
                    { _, hourOfDay, minute ->
                        birthHour = hourOfDay
                        birthMinute = minute
                        showTimePicker = false
                    },
                    birthHour,
                    birthMinute,
                    is24HourFormat(context)
                ).show()
            }

            Text(stringResource(R.string.enter_birth_city))

            CitySearchField(
                value = cityName,
                onValueChange = { cityName = it },
                onCitySelected = {
                    latitude = it.lat
                    longitude = it.lon

                    scope.launch {
                        getCityName(context, it.lat, it.lon)?.let { name ->
                            cityName = name
                        }
                    }
                }
            )



            Button(
                onClick = {
                    onOpenMap(latitude, longitude)

                    scope.launch {
                        getCityName(context, latitude, longitude)?.let {
                            cityName = it
                        }
                    }
                },
                modifier = Modifier.padding(vertical = 8.dp)
            ) {
                Text(stringResource(R.string.select_precisely_on_map))
            }


            Text(
                text = "Lat: %.5f, Lon: %.5f".format(latitude, longitude),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(start = 8.dp)
            )
            Text(
                text = if (cityName.isNotBlank()) "City: $cityName" else "City not set",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(start = 8.dp)
            )


            Text(stringResource(R.string.select_house_system))

            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = selectedText,
                    onValueChange = {},
                    label = { Text(stringResource(R.string.select_house_system)) },
                    modifier = Modifier.fillMaxWidth(),
                    readOnly = true,
                    enabled = true,
                    trailingIcon = {
                        Icon(Icons.Filled.ArrowDropDown, contentDescription = null)
                    }
                )
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clickable { expanded = true }
                )
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    AstroHouseSystem.values().forEach {
                        DropdownMenuItem(
                            text = { Text(it.toString()) },
                            onClick = {
                                selectedHouseSystem = it
                                expanded = false
                            }
                        )
                    }
                }
            }

            Button(onClick = {
                val input = AstroInputData(
                    birthDate, birthHour, birthMinute,
                    latitude, longitude, selectedHouseSystem, cityName
                )
                scope.launch {
                    AstroPreferencesManager.save(context, input)
                }
                onConfirm(input)
            }) {
                Text(stringResource(R.string.confirm))
            }
        }
    }
}



@Composable
fun NumberInput(label: String, value: Int, range: IntRange, onValueChange: (Int) -> Unit) {
    OutlinedTextField(
        value = value.toString(),
        onValueChange = {
            val num = it.toIntOrNull()
            if (num != null && num in range) onValueChange(num)
        },
        label = { Text(label) },
        modifier = Modifier.width(100.dp)
    )
}



// Data holders

data class AstroInputData(
    val date: LocalDate,
    val hour: Int,
    val minute: Int,
    val latitude: Double,
    val longitude: Double,
    val houseSystem: AstroHouseSystem,
    val cityName: String
)

enum class AstroHouseSystem(val code: Char) {
    PLACIDUS('P'),
    KOCH('K'),
    REGIOMONTANUS('R'),
    CAMPANUS('C'),
    EQUAL('E'),
    WHOLE_SIGN('W'),
    TOPIC('T'),
    SOLAR('V');

    override fun toString(): String = name.capitalize()
}

fun getCityName(context: Context, lat: Double, lon: Double): String? {
    return try {
        val geocoder = Geocoder(context, Locale.getDefault())
        val addressList = geocoder.getFromLocation(lat, lon, 1)
        addressList?.firstOrNull()?.locality
    } catch (e: Exception) {
        null
    }
}