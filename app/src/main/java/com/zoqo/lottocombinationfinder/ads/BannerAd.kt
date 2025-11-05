// BannerAd.kt
package com.zoqo.lottocombinationfinder.ads

import android.app.Activity
import android.util.DisplayMetrics
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView

@Composable
fun BannerAdView(
     adUnitId: String = "ca-app-pub-2115174563501851/5721064729"// real
    //  adUnitId: String = "ca-app-pub-3940256099942544/6300978111" // test
) {
    val context = LocalContext.current
    val activity = remember(context) { context as? Activity }

    val adView = remember {
        AdView(context).apply {
            this.adUnitId = adUnitId

            // adaptive širina (fallback na BANNER ako Activity iz nekog razloga nije dostupan)
            activity?.let { act ->
                val metrics = DisplayMetrics()
                @Suppress("DEPRECATION")
                act.windowManager.defaultDisplay.getMetrics(metrics)
                val adWidth = (metrics.widthPixels / metrics.density).toInt()
                setAdSize(AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(act, adWidth))
            } ?: setAdSize(AdSize.BANNER)

            loadAd(AdRequest.Builder().build())
        }
    }

    val lifecycleOwner: LifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE  -> adView.pause()
                Lifecycle.Event.ON_RESUME -> adView.resume()
                // može i bez ON_DESTROY jer imamo onDispose, ali nije štetno:
                Lifecycle.Event.ON_DESTROY -> adView.destroy()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            adView.destroy()
        }
    }

    AndroidView(
        factory = { adView },
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
    )
}
