package com.zoqo.lottocombinationfinder.components

import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.zoqo.lottocombinationfinder.R
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONArray
import java.net.URLEncoder
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
data class CitySuggestion(val displayName: String, val lat: Double, val lon: Double)

@Composable
fun CitySearchField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String = stringResource(R.string.enter_city),
    onCitySelected: (CitySuggestion) -> Unit
) {
    var internalText by remember { mutableStateOf(value) }
    var suggestions by remember { mutableStateOf(listOf<CitySuggestion>()) }
    var expanded by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    var debounceJob by remember { mutableStateOf<Job?>(null) }

    // Ako se spolja promeni value, ažuriraj lokalni tekst
    LaunchedEffect(value) {
        if (value != internalText) {
            internalText = value
        }
    }

    Column(modifier = modifier) {
        OutlinedTextField(
            value = internalText,
            onValueChange = {
                internalText = it
                onValueChange(it)

                if (it.length >= 2) {
                    debounceJob?.cancel()
                    debounceJob = coroutineScope.launch {
                        delay(300)
                        suggestions = searchCities(it)
                        expanded = suggestions.isNotEmpty()
                    }
                } else {
                    suggestions = emptyList()
                    expanded = false
                }
            },
            label = { Text(label) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = "Location Icon"
                )
            },
            modifier = Modifier.fillMaxWidth()
        )

        if (expanded) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp)
            ) {
                Column {
                    suggestions.forEach { suggestion ->
                        Text(
                            text = suggestion.displayName,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    internalText = suggestion.displayName
                                    onValueChange(suggestion.displayName)
                                    expanded = false
                                    onCitySelected(suggestion)
                                }
                                .padding(8.dp)
                        )
                    }
                }
            }
        }
    }
}


private suspend fun searchCities(query: String): List<CitySuggestion> {
    return withContext(Dispatchers.IO) {
        try {
            val encoded = URLEncoder.encode(query, "UTF-8")
            val url = "https://nominatim.openstreetmap.org/search?q=$encoded&format=json&limit=5"
            val jsonText = URL(url).readText()
            val jsonArray = JSONArray(jsonText)

            List(jsonArray.length()) { i ->
                val obj = jsonArray.getJSONObject(i)
                CitySuggestion(
                    displayName = obj.getString("display_name"),
                    lat = obj.getDouble("lat"),
                    lon = obj.getDouble("lon")
                )
            }
        } catch (e: Exception) {
            Log.e("CitySearch", "Error: ${e.message}")
            emptyList()
        }
    }
}
