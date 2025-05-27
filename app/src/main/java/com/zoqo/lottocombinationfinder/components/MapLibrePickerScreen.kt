package com.zoqo.lottocombinationfinder.components

import android.view.ViewGroup
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.zoqo.lottocombinationfinder.R
import org.maplibre.android.MapLibre
import org.maplibre.android.annotations.MarkerOptions
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapView
import androidx.compose.ui.viewinterop.AndroidView

@Composable
fun MapLibrePickerScreen(
    initialLat: Double,
    initialLon: Double,
    onLocationPicked: (Double, Double) -> Unit,
    onCancel: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = rememberUpdatedState(LocalLifecycleOwner.current)

    var selectedLat by remember { mutableStateOf(initialLat) }
    var selectedLon by remember { mutableStateOf(initialLon) }
    var mapView: MapView? = null

    // MapLibre init
    DisposableEffect(Unit) {
        MapLibre.getInstance(context)
        onDispose {}
    }

    // Lifecycle observer
    DisposableEffect(Unit) {
        val lifecycle = lifecycleOwner.value.lifecycle
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> mapView?.onStart()
                Lifecycle.Event.ON_RESUME -> mapView?.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView?.onPause()
                Lifecycle.Event.ON_STOP -> mapView?.onStop()
                Lifecycle.Event.ON_DESTROY -> mapView?.onDestroy()
                else -> {}
            }
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }

    Column(Modifier.fillMaxSize()) {
        AndroidView(
            modifier = Modifier.weight(1f),
            factory = { ctx ->
                MapView(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )

                    getMapAsync { mapboxMap ->
                        val pos = LatLng(initialLat, initialLon)
                        mapboxMap.setStyle("https://demotiles.maplibre.org/style.json") {
                            mapboxMap.moveCamera(CameraUpdateFactory.newLatLngZoom(pos, 6.0))
                            mapboxMap.addMarker(MarkerOptions().position(pos))

                            mapboxMap.addOnMapClickListener { point ->
                                mapboxMap.clear()
                                mapboxMap.addMarker(MarkerOptions().position(point))
                                selectedLat = point.latitude
                                selectedLon = point.longitude
                                true
                            }
                        }
                    }

                    onCreate(null)
                    mapView = this
                }
            }
        )

        Text(
            text = "Selected: %.5f, %.5f".format(selectedLat, selectedLon),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 4.dp)
        )

        Row(
            Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Button(onClick = onCancel) {
                Text(stringResource(R.string.cancel))
            }
            Button(onClick = {
                onLocationPicked(selectedLat, selectedLon)
            }) {
                Text(stringResource(R.string.save_location))
            }
        }
    }
}
