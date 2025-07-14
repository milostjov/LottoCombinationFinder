//CustomDialogs.kt
package com.zoqo.lottocombinationfinder.ui

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.widget.NumberPicker
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SignalWifiOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.zoqo.lottocombinationfinder.R

@Composable
fun NoInternetDialog(onRetry: () -> Unit) {
    AlertDialog(
        onDismissRequest = {},
        title = {
            Text(text = stringResource(R.string.no_internet_title))
        },
        text = {
            Text(text = stringResource(R.string.no_internet_message))
        },
        confirmButton = {
            TextButton(onClick = onRetry) {
                Text(text = stringResource(R.string.retry))
            }
        },
        icon = {
            Icon(
                imageVector = Icons.Default.SignalWifiOff,
                contentDescription = null
            )
        }
    )
}

@Composable
fun CustomTimePickerDialog(
    initialHour: Int,
    initialMinute: Int,
    is24Hour: Boolean = true,
    onDismissRequest: () -> Unit,
    onTimeSelected: (hour: Int, minute: Int) -> Unit
) {
    var hour by remember { mutableStateOf(initialHour) }
    var minute by remember { mutableStateOf(initialMinute) }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = {Text(stringResource(R.string.select_time)) },
        text = {

            Row(
                horizontalArrangement = Arrangement.spacedBy(24.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {

                val colorInt = MaterialTheme.colorScheme.onSurface.toArgb()


                AndroidView(
                    factory = { context ->
                        NumberPicker(context).apply {
                            minValue = 0
                            maxValue = 23
                            value = hour
                            setFormatter { "%02d".format(it) }
                            setOnValueChangedListener { _, _, newVal -> hour = newVal }

                            applyTextColor(colorInt)
                        }
                    },
                    update = {
                        it.value = hour
                        it.applyTextColor(colorInt)
                    },
                    modifier = Modifier.weight(1f)
                )


                Text(text = ":", style = MaterialTheme.typography.headlineMedium)

                AndroidView(
                    factory = { context ->
                        NumberPicker(context).apply {
                            minValue = 0
                            maxValue = 59
                            value     = minute
                            setFormatter { "%02d".format(it) }
                            setOnValueChangedListener { _, _, newVal -> minute = newVal }

                            applyTextColor(colorInt)
                        }
                    },
                    update = {
                        it.value = minute
                        it.applyTextColor(colorInt)
                    },
                    modifier = Modifier.weight(1f)
                )


            }
        },
        confirmButton = {
            TextButton(onClick = {
                onTimeSelected(hour, minute)
                onDismissRequest()
            }) { Text(stringResource(R.string.ok)) }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) { Text(stringResource(R.string.cancel)) }
        }
    )
}



private fun NumberPicker.applyTextColor(@ColorInt color: Int) {
    for (i in 0 until childCount) {
        (getChildAt(i) as? TextView)?.setTextColor(color)
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        try {
            val method = NumberPicker::class.java.getMethod("setTextColor", Int::class.javaPrimitiveType)
            method.invoke(this, color)
        } catch (_: Exception) { /* best-effort */ }
    }

}

@Composable

fun PlanetInfoDialog(
    planetSymbol: String,
    onDismiss: () -> Unit
) {
    val name = getPlanetName(planetSymbol)
    val description = getPlanetDescription(planetSymbol)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = name) },
        text = { Text(text = description) },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.ok))
            }
        }
    )
}


@Composable
fun getPlanetName(symbol: String): String = when (symbol) {
    "☉" -> stringResource(R.string.planet_sun)
    "☽" -> stringResource(R.string.planet_moon)
    "☿" -> stringResource(R.string.planet_mercury)
    "♀" -> stringResource(R.string.planet_venus)
    "♂" -> stringResource(R.string.planet_mars)
    "♃" -> stringResource(R.string.planet_jupiter)
    "♄" -> stringResource(R.string.planet_saturn)
    "♅" -> stringResource(R.string.planet_uranus)
    "♆" -> stringResource(R.string.planet_neptune)
    "♇" -> stringResource(R.string.planet_pluto)
    else -> stringResource(R.string.planet_unknown)
}

@Composable
fun getPlanetDescription(symbol: String): String = when (symbol) {
    "☉" -> stringResource(R.string.desc_sun)
    "☽" -> stringResource(R.string.desc_moon)
    "☿" -> stringResource(R.string.desc_mercury)
    "♀" -> stringResource(R.string.desc_venus)
    "♂" -> stringResource(R.string.desc_mars)
    "♃" -> stringResource(R.string.desc_jupiter)
    "♄" -> stringResource(R.string.desc_saturn)
    "♅" -> stringResource(R.string.desc_uranus)
    "♆" -> stringResource(R.string.desc_neptune)
    "♇" -> stringResource(R.string.desc_pluto)
    else -> stringResource(R.string.desc_unknown)
}

fun Context.hasInternetConnection(): Boolean {
    val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val network = connectivityManager.activeNetwork ?: return false
    val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
    return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
}
