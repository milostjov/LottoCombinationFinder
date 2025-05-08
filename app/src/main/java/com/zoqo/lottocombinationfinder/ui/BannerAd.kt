package com.zoqo.lottocombinationfinder.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView


    @Composable
    fun BannerAdView() {
        AndroidView(
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            factory = { context ->
                AdView(context).apply {
                    setAdSize(AdSize.BANNER) // ✅ koristi setAdSize, ne direktno `adSize =`
                    adUnitId = "ca-app-pub-3940256099942544/6300978111" // test banner ad unit ID
                    loadAd(AdRequest.Builder().build())
                }
            }
        )

    }
