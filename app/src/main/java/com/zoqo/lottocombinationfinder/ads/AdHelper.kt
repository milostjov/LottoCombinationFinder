package com.zoqo.lottocombinationfinder.ads

import android.app.Activity
import android.util.Log
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback

object AdHelper {
    var rewardedAd: RewardedAd? = null
    var isAdLoading = false
    var exitInterstitialAd: InterstitialAd? = null
    var isExitAdLoading = false
    private const val TAG = "AdHelper"

    fun loadRewardedAd(activity: Activity, onLoaded: (() -> Unit)? = null) {
        if (isAdLoading) return
        isAdLoading = true

        val adRequest = AdRequest.Builder().build()
        RewardedAd.load(
            activity,
            "ca-app-pub-3940256099942544/5224354917", // test ad unit
            adRequest,
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    rewardedAd = ad
                    isAdLoading = false
                    Log.d(TAG, "Rewarded ad loaded.")
                    onLoaded?.invoke()
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    rewardedAd = null
                    isAdLoading = false
                    Log.e(TAG, "Rewarded ad load failed: ${error.message}")
                }
            }
        )
    }

    fun loadExitInterstitialAd(activity: Activity) {
        if (isExitAdLoading) return
        isExitAdLoading = true

        val adRequest = AdRequest.Builder().build()
        InterstitialAd.load(
            activity,
            "ca-app-pub-3940256099942544/1033173712", // test ad unit
            adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    exitInterstitialAd = ad
                    isExitAdLoading = false
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    exitInterstitialAd = null
                    isExitAdLoading = false
                }
            }
        )
    }
}
