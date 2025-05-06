package com.zoqo.lottocombinationfinder.ads

import android.app.Activity
import android.app.Application
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.util.Log
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.appopen.AppOpenAd
import com.google.android.gms.ads.appopen.AppOpenAd.AppOpenAdLoadCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds

class AppOpenAdManager(private val application: Application) : Application.ActivityLifecycleCallbacks {

    private var appOpenAd: AppOpenAd? = null
    private var isShowingAd = false
    private var currentActivity: Activity? = null

    init {
        application.registerActivityLifecycleCallbacks(this)
        MobileAds.initialize(application)
        loadAd()
    }

    private fun loadAd() {
        val adRequest = AdRequest.Builder().build()
        AppOpenAd.load(
            application,
            "ca-app-pub-3940256099942544/3419835294", // TEST ad unit ID
            adRequest,
            object : AppOpenAd.AppOpenAdLoadCallback() {
                override fun onAdLoaded(ad: AppOpenAd) {
                    appOpenAd = ad
                    Log.d("AppOpenAdManager", "Ad loaded")
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    Log.e("AppOpenAdManager", "Ad failed to load: ${loadAdError.message}")
                }
            }
        )




    }

    fun showAdIfAvailable() {
        if (appOpenAd != null && !isShowingAd && currentActivity != null) {
            appOpenAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    isShowingAd = false
                    appOpenAd = null
                    loadAd() // reload after dismiss
                }

                override fun onAdFailedToShowFullScreenContent(adError: com.google.android.gms.ads.AdError) {
                    isShowingAd = false
                    Log.e("AppOpenAdManager", "Ad failed to show: ${adError.message}")
                }

                override fun onAdShowedFullScreenContent() {
                    isShowingAd = true
                }
            }

            appOpenAd?.show(currentActivity!!)
        } else {
            Log.d("AppOpenAdManager", "Ad not ready or already showing.")
        }
    }

    // ActivityLifecycleCallbacks
    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
    override fun onActivityStarted(activity: Activity) {
        currentActivity = activity
        showAdIfAvailable()
    }

    override fun onActivityResumed(activity: Activity) {
        currentActivity = activity
    }

    override fun onActivityPaused(activity: Activity) {}
    override fun onActivityStopped(activity: Activity) {
        if (currentActivity === activity) currentActivity = null
    }

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
    override fun onActivityDestroyed(activity: Activity) {}
}
