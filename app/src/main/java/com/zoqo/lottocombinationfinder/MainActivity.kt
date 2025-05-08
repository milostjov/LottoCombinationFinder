package com.zoqo.lottocombinationfinder

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.AdError
import com.zoqo.lottocombinationfinder.ads.AdHelper
import com.zoqo.lottocombinationfinder.ui.LottoApp
import com.zoqo.lottocombinationfinder.ui.theme.LottoCombinationFinderTheme

class MainActivity : ComponentActivity() {

    private val TAG = "MainActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        MobileAds.initialize(this) {}

        // Prikaz interstitial reklame odmah po pokretanju (test ad unit ID)
        InterstitialAd.load(
            this,
            "ca-app-pub-3940256099942544/1033173712",
            AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    ad.show(this@MainActivity)
                }

                override fun onAdFailedToLoad(adError: LoadAdError) {
                    Log.e(TAG, "Initial interstitial ad failed to load: ${adError.message}")
                }
            }
        )

        // Učitavanje ostalih oglasa
        AdHelper.loadRewardedAd(this)
        AdHelper.loadExitInterstitialAd(this)

        // UI
        setContent {
            LottoCombinationFinderTheme {
                LottoApp(
                    showRewardedAd = { onReward ->
                        val ad = AdHelper.rewardedAd
                        if (ad != null) {
                            ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                                override fun onAdDismissedFullScreenContent() {
                                    AdHelper.loadRewardedAd(this@MainActivity)
                                }

                                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                                    Log.e(TAG, "Failed to show rewarded ad: ${adError.message}")
                                    onReward()
                                }
                            }
                            ad.show(this@MainActivity) { onReward() }
                        } else {
                            Log.d(TAG, "Rewarded ad not ready.")
                            onReward()
                        }
                    }
                )
            }
        }
    }

    override fun onBackPressed() {
        val ad = AdHelper.exitInterstitialAd
        if (ad != null) {
            ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    finish()
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    finish()
                }
            }
            ad.show(this)
        } else {
            super.onBackPressed()
        }
    }
}
