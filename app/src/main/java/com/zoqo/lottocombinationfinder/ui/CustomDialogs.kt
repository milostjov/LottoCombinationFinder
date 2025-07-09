//CustomDialogs.kt
package com.zoqo.lottocombinationfinder.ui

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SignalWifiOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.viewinterop.AndroidView
import com.zoqo.lottocombinationfinder.R
import android.widget.NumberPicker
import android.widget.TextView
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.toArgb

import androidx.annotation.ColorInt
import android.os.Build

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
        title = { Text(text = "Select Time") },
        text = {
            // dva točka (hour • minute) u jednom redu
            Row(
                horizontalArrangement = Arrangement.spacedBy(24.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // ────── SAT ──────
                val colorInt = MaterialTheme.colorScheme.onSurface.toArgb()


                AndroidView(
                    factory = { context ->
                        NumberPicker(context).apply {
                            minValue = 0
                            maxValue = 23
                            value = hour
                            setFormatter { "%02d".format(it) }
                            setOnValueChangedListener { _, _, newVal -> hour = newVal }

                            applyTextColor(colorInt)   // ← prosleđuješ Int, nikad Color
                        }
                    },
                    update = {
                        it.value = hour
                        it.applyTextColor(colorInt)
                    },
                    modifier = Modifier.weight(1f)
                )


                Text(text = ":", style = MaterialTheme.typography.headlineMedium)

                // ───── MINUT ─────


                AndroidView(
                    factory = { context ->
                        NumberPicker(context).apply {
                            minValue = 0
                            maxValue = 59
                            value     = minute
                            setFormatter { "%02d".format(it) }
                            setOnValueChangedListener { _, _, newVal -> minute = newVal }

                            applyTextColor(colorInt)   // ← prosleđuješ Int, nikad Color
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
            }) { Text("OK") }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) { Text("Cancel") }
        }
    )
}


//@ColorInt
private fun NumberPicker.applyTextColor(@ColorInt color: Int) {
    // 1) Text boja svih child-TextView-ova (radi svuda)
    for (i in 0 until childCount) {
        (getChildAt(i) as? TextView)?.setTextColor(color)
    }

    // 2) API ≥ 29: postoji setTextColor na samom pickeru
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        try {
            val method = NumberPicker::class.java.getMethod("setTextColor", Int::class.javaPrimitiveType)
            method.invoke(this, color)
        } catch (_: Exception) { /* best-effort */ }
    }
    // 3) Ne diramo mSelectorWheelPaint refleksijom → nema non-SDK upozorenja
}



fun Context.hasInternetConnection(): Boolean {
    val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val network = connectivityManager.activeNetwork ?: return false
    val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
    return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
}
