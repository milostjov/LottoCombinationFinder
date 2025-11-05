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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
    planet: com.zoqo.lottocombinationfinder.data.PlanetData,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "${planet.symbol} ${planet.name}", style = MaterialTheme.typography.titleLarge)
                if (planet.retrograde) {
                    Text(
                        text = stringResource(R.string.retrograde_planet),
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        },
        text = {
            Column {

                // 1) Znak i stepen (iz dužine)
                val (signIdx, degInSign) = lonToSignParts(planet.longitude)
                val zodiac = listOf("♈","♉","♊","♋","♌","♍","♎","♏","♐","♑","♒","♓")
                val zodiacNames = listOf(
                    stringResource(R.string.sign_aries),
                    stringResource(R.string.sign_taurus),
                    stringResource(R.string.sign_gemini),
                    stringResource(R.string.sign_cancer),
                    stringResource(R.string.sign_leo),
                    stringResource(R.string.sign_virgo),
                    stringResource(R.string.sign_libra),
                    stringResource(R.string.sign_scorpio),
                    stringResource(R.string.sign_sagittarius),
                    stringResource(R.string.sign_capricorn),
                    stringResource(R.string.sign_aquarius),
                    stringResource(R.string.sign_pisces)
                )

                InfoRow(label = stringResource(R.string.label_sign_degree),
                    value = "${zodiac[signIdx]} ${zodiacNames[signIdx]} — ${formatDMS(degInSign)}")

                // 2) Apsolutna ekliptička dužina
                InfoRow(stringResource(R.string.label_ecl_lon), "${formatDMS(planet.longitude)}")

                // 3) Ekliptička širina (ako ima)
                planet.latitude?.let {
                    InfoRow(stringResource(R.string.label_ecl_lat), formatDMS(it))
                }

                // 4) Brzina po dužini
                planet.speedLonDegPerDay?.let {
                    val tag = if (it < 0) "R" else "D"
                    InfoRow(stringResource(R.string.label_speed_lon), "${round(it, 5)} ($tag)")

                }

                // 5) Distanca (AU)
                planet.distanceAu?.let {
                    InfoRow(stringResource(R.string.label_distance), "${round(it, 6)} AU")
                }

                // 6) Kuća
                planet.house?.let {
                    InfoRow(stringResource(R.string.label_house), it.toString())
                }

                // 7) Ekvatorijalne koordinate
                planet.rightAscension?.let {
                    InfoRow(stringResource(R.string.label_ra), formatHMS(it))
                }
                planet.declination?.let {
                    InfoRow(stringResource(R.string.label_dec), formatDMS(it))
                }

                // 8) Lokalni horizont
                planet.altitude?.let {
                    InfoRow(stringResource(R.string.label_alt), formatDMS(it))
                }
                planet.azimuth?.let {
                    InfoRow(stringResource(R.string.label_az), formatDMS(it))
                }

                // 9) Kratak opis (postojeći opis po simbolu)
                Spacer(Modifier.fillMaxWidth().let { Modifier })
                Text(
                    text = getPlanetDescription(planet.symbol),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.ok)) }
        }
    )
}

/** Reusable UI helper */
@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}

/** Helpers za formatiranje i znak/stepen */
private fun lonToSignParts(lon: Double): Pair<Int, Double> {
    val norm = ((lon % 360.0) + 360.0) % 360.0
    val signIdx = (norm / 30.0).toInt()
    val degInSign = norm - signIdx * 30.0
    return signIdx to degInSign
}

private fun formatDMS(deg: Double): String {
    val sign = if (deg < 0) "-" else ""
    var d = kotlin.math.abs(deg)
    val D = kotlin.math.floor(d).toInt()
    d = (d - D) * 60
    val M = kotlin.math.floor(d).toInt()
    val S = ((d - M) * 60)
    return "%s%02d° %02d' %05.2f\"".format(sign, D, M, S)
}

private fun formatHMS(hoursOrDeg: Double): String {
    // Ako RA već dobijaš u satima iz SE, prosledi kao je; ako je u stepenima, konvertuj pre poziva.
    val sign = if (hoursOrDeg < 0) "-" else ""
    var h = kotlin.math.abs(hoursOrDeg)
    val H = kotlin.math.floor(h).toInt()
    h = (h - H) * 60
    val M = kotlin.math.floor(h).toInt()
    val S = ((h - M) * 60)
    return "%s%02dh %02dm %05.2fs".format(sign, H, M, S)
}

private fun round(value: Double, digits: Int): String = "%.${digits}f".format(value)
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
    "☊" -> stringResource(R.string.planet_northnode)
    "☋" -> stringResource(R.string.planet_southnode)
    "⚸" -> stringResource(R.string.planet_lilith)
    "⚷" -> stringResource(R.string.planet_chiron)
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
    "☊" -> stringResource(R.string.desc_northnode)
    "☋" -> stringResource(R.string.desc_southnode)
    "⚸" -> stringResource(R.string.desc_lilith)
    "⚷" -> stringResource(R.string.desc_chiron)
    else -> stringResource(R.string.desc_unknown)
}


fun Context.hasInternetConnection(): Boolean {
    val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val network = connectivityManager.activeNetwork ?: return false
    val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
    return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
}
@Composable
fun RankInfoDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(R.string.rank_info_title)) },
        text = { Text(text = stringResource(R.string.rank_info_text)) },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.ok))
            }
        }
    )
}
