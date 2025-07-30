//AdHelper
package com.zoqo.lottocombinationfinder.ads

import android.app.Activity
import android.util.Log
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import android.content.Context
import java.time.LocalDate

object AdHelper {
    var rewardedAd: RewardedAd? = null
    var isAdLoading = false
    var exitInterstitialAd: InterstitialAd? = null
    var isExitAdLoading = false
    private const val TAG = "AdHelper"

    private const val PREFS_NAME = "ad_prefs"
    private const val KEY_LAST_STARTUP_AD_DAY = "last_startup_ad_day"
    private const val KEY_AD_SHOWN_COUNT = "startup_ad_count"
    private const val KEY_AD_SHOWN_DATE = "startup_ad_date"


    fun hasStartupAdBeenShownToday(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val today = LocalDate.now().toString()
        return prefs.getString(KEY_LAST_STARTUP_AD_DAY, null) == today
    }

    fun markStartupAdAsShownToday(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_LAST_STARTUP_AD_DAY, LocalDate.now().toString()).apply()
    }
    fun canShowStartupAd(context: Context, maxPerDay: Int = 3): Boolean {  // max 3x dnevno
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val today = LocalDate.now().toString()
        val storedDate = prefs.getString(KEY_AD_SHOWN_DATE, null)
        val count = prefs.getInt(KEY_AD_SHOWN_COUNT, 0)

        return if (storedDate == today) {
            count < maxPerDay
        } else {
            true // novi dan, može od nule
        }
    }
    fun incrementStartupAdCount(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val today = LocalDate.now().toString()
        val storedDate = prefs.getString(KEY_AD_SHOWN_DATE, null)

        val editor = prefs.edit()
        if (storedDate == today) {
            val currentCount = prefs.getInt(KEY_AD_SHOWN_COUNT, 0)
            editor.putInt(KEY_AD_SHOWN_COUNT, currentCount + 1)
        } else {
            editor.putString(KEY_AD_SHOWN_DATE, today)
            editor.putInt(KEY_AD_SHOWN_COUNT, 1)
        }
        editor.apply()
    }

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
    fun loadStartupInterstitialAdOncePerDay(
        activity: Activity,
        onFinished: () -> Unit
    ) {
        if (hasStartupAdBeenShownToday(activity)) {
            onFinished()
            return
        }

        InterstitialAd.load(
            activity,
            "ca-app-pub-3940256099942544/1033173712",
            AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    markStartupAdAsShownToday(activity)
                    ad.fullScreenContentCallback = object : com.google.android.gms.ads.FullScreenContentCallback() {
                        override fun onAdDismissedFullScreenContent() {
                            onFinished()
                        }

                        override fun onAdFailedToShowFullScreenContent(error: com.google.android.gms.ads.AdError) {
                            onFinished()
                        }
                    }
                    ad.show(activity)
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    onFinished()
                }
            }
        )
    }

}
