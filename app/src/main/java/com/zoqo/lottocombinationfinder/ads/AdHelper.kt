// AdHelper.kt
package com.zoqo.lottocombinationfinder.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.gms.ads.*
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import java.time.LocalDate
import kotlin.coroutines.resume

object AdHelper {
    private const val TAG = "AdHelper"

    //  Centralizovani adUnitId-ovi :
    private const val STARTUP_INTERSTITIAL_ID = "ca-app-pub-2115174563501851/7277688974"
    private const val EXIT_INTERSTITIAL_ID    = "ca-app-pub-2115174563501851/7277688974"
    private const val REWARDED_ID             = "ca-app-pub-2115174563501851/3530015653"
    private const val GENERIC_INTERSTITIAL_ID = "ca-app-pub-2115174563501851/8782342339"


    // 👉 Test ad unit IDs (Google official test values)
//    private const val STARTUP_INTERSTITIAL_ID = "ca-app-pub-3940256099942544/1033173712"
//    private const val EXIT_INTERSTITIAL_ID    = "ca-app-pub-3940256099942544/1033173712"
//    private const val REWARDED_ID             = "ca-app-pub-3940256099942544/5224354917"
//    private const val GENERIC_INTERSTITIAL_ID = "ca-app-pub-3940256099942544/1033173712"


    // SharedPrefs ključevi za dnevni limit start-up oglasa
    private const val PREFS_NAME = "ad_prefs"
    private const val KEY_AD_SHOWN_COUNT = "startup_ad_count"
    private const val KEY_AD_SHOWN_DATE  = "startup_ad_date"

    // Procese-level zaštita od duplog prikaza u istoj sesiji
    private var hasShownStartupThisProcess = false
    private var appStartTimestampMs = System.currentTimeMillis()

    var rewardedAd: RewardedAd? = null
        private set
    private var isRewardLoading = false

    var exitInterstitialAd: InterstitialAd? = null
        private set
    private var isExitLoading = false

//    fun initMobileAds(context: Context) {
//        MobileAds.initialize(context) {}
//    }
    fun markAppStart() {
        appStartTimestampMs = System.currentTimeMillis()
    }

    // Izračun veličine za anchored adaptive banner prema širini ekrana
    private fun getAdaptiveAdSize(activity: Activity): AdSize {
        val display = activity.windowManager.defaultDisplay
        val outMetrics = android.util.DisplayMetrics()
        display.getMetrics(outMetrics)

        val density = outMetrics.density
        val adWidthPixels = outMetrics.widthPixels.toFloat()
        val adWidth = (adWidthPixels / density).toInt()

        return AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(activity, adWidth)
    }

    fun canShowStartupAd(context: Context, probability: Double = 0.3): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val lastShownDate = prefs.getString(KEY_AD_SHOWN_DATE, null)
        val today = LocalDate.now().toString()

        val randomChance = Math.random() < probability
        val notShownToday = lastShownDate != today

        // Prikaži oglas samo ako je prošlo barem 24h ILI slučajna šansa uspe
        val shouldShow = randomChance || notShownToday

        if (shouldShow) {
            prefs.edit().putString(KEY_AD_SHOWN_DATE, today).apply()
        }
        return shouldShow
    }
// AdHelper.kt (dodaci)
object AdsConfig {
    var enabled = true
    var baseProbability = 0.18
    var maxProbability  = 0.35
    var minIntervalMs   = 120_000L
    var graceAfterStartupMs = 120_000L
    var sessionCap = 3
    var minActionsBetweenAds = 3
    var rampPerMinute = 0.03

    /**
     * Primeni nove vrednosti iz Firebase Remote Config-a
     * uz sigurnosne granice (da pogrešan RC ne "pokvari" aplikaciju).
     */
    fun applySafe(
        enabled: Boolean? = null,
        baseP: Double? = null,
        maxP: Double? = null,
        minIntMs: Long? = null,
        graceMs: Long? = null,
        cap: Int? = null,
        minActions: Int? = null,
        ramp: Double? = null
    ) {
        enabled?.let { this.enabled = it }

        baseP?.let { this.baseProbability = it.coerceIn(0.0, 1.0) }
        maxP?.let  { this.maxProbability  = it.coerceIn(0.0, 1.0) }
        if (this.maxProbability < this.baseProbability) {
            this.maxProbability = this.baseProbability
        }

        minIntMs?.let { this.minIntervalMs = it.coerceAtLeast(30_000L) }      // ≥ 30s
        graceMs?.let  { this.graceAfterStartupMs = it.coerceAtLeast(0L) }
        cap?.let      { this.sessionCap = it.coerceIn(0, 10) }                // 0–10
        minActions?.let { this.minActionsBetweenAds = it.coerceIn(0, 10) }
        ramp?.let     { this.rampPerMinute = it.coerceIn(0.0, 0.5) }          // do +50%/min
    }
}

    //private var lastAdTimestampMs = 0L
    private var lastStartupShownAtMs = 0L
    private var sessionAdCount = 0
    private var navActionsSinceAd = 0
    private var lastNoAdDecisionAtMs = 0L

    fun markStartupShownNow() { // pozovi kad prikažeš startup interstitial
        lastStartupShownAtMs = System.currentTimeMillis()
    }

    fun onNavAction() { // pozovi na svaki klik u meniju
        navActionsSinceAd++
    }

    /** Linearno rampovanje verovatnoće dok se ne prikaže oglas. */
    private fun currentProbability(): Double {
        val now = System.currentTimeMillis()
        val sinceNoAd = (now - lastNoAdDecisionAtMs).coerceAtLeast(0L)
        val minutes = sinceNoAd / 60_000.0
        val p = AdsConfig.baseProbability + minutes * AdsConfig.rampPerMinute
        return p.coerceIn(AdsConfig.baseProbability, AdsConfig.maxProbability)
    }

    fun shouldShowAdWithPolicy(): Boolean {
        val now = System.currentTimeMillis()

        // grace posle startup interstitiala
        if (now - lastStartupShownAtMs < AdsConfig.graceAfterStartupMs) return false

        // minimalan razmak i minimalan broj akcija
        if (now - lastAdTimestampMs < AdsConfig.minIntervalMs) return false
        if (navActionsSinceAd < AdsConfig.minActionsBetweenAds) return false

        // sesijski limit
        if (sessionAdCount >= AdsConfig.sessionCap) return false

        // verovatnoća (sa rampom)
        val p = currentProbability()
        val ok = Math.random() < p
        if (!ok) {
            lastNoAdDecisionAtMs = now
        }
        return ok
    }

    fun consumeAdDecision() { // pozovi kada se interstitial zaista prikaže
        lastAdTimestampMs = System.currentTimeMillis()
        sessionAdCount++
        navActionsSinceAd = 0
        lastNoAdDecisionAtMs = lastAdTimestampMs
    }



    private var genericInterstitialAd: InterstitialAd? = null
    private var isGenericLoading = false
    private var isShowingFullscreenAd = false

    // cooldown da ne “secka” korisnika prečesto (npr. 60s)
    private var lastAdTimestampMs: Long = 0

    /** Jednostavna odluka: verovatnoća + minimalni razmak između dva prikaza. */
//    fun shouldShowAd(probability: Double = 0.25, minIntervalMs: Long = 60_000): Boolean {
//        val now = System.currentTimeMillis()
//        val intervalOk = (now - lastAdTimestampMs) >= minIntervalMs
//        val chanceOk = Math.random() < probability
//        return intervalOk && chanceOk && !isShowingFullscreenAd
//    }

    /** Preload “generičkog” interstitial-a za navigaciju. Zovi iz Activity-ja nakon starta. */
    fun preloadGenericInterstitial(activity: Activity) {
        if (isGenericLoading || genericInterstitialAd != null) return
        isGenericLoading = true
        InterstitialAd.load(
            activity,
            GENERIC_INTERSTITIAL_ID,
            AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    genericInterstitialAd = ad
                    isGenericLoading = false
                }
                override fun onAdFailedToLoad(error: LoadAdError) {
                    genericInterstitialAd = null
                    isGenericLoading = false
                    Log.e(TAG, "Generic interstitial failed: ${error.message}")
                }
            }
        )
    }

    /**
     * Pokušaj da prikažeš interstitial ZA NAVIGACIJU.
     * Ako postoji keširan oglas i shouldShowAd() kaže “da” → prikaži; po zatvaranju pozovi onContinue().
     * Ako nema oglasa ili ne treba sada → odmah pozovi onContinue().
     */
    fun maybeShowNavInterstitial(
        activity: Activity,
        onContinue: () -> Unit
    ) {
        val canShow = shouldShowAdWithPolicy()
        val ad = genericInterstitialAd

        if (!canShow || ad == null) {
            // nema oglasa ili ne želimo sada — odmah nastavi
            onContinue()
            // probaj da preloadaš za sledeći put
            preloadGenericInterstitial(activity)
            return
        }

        isShowingFullscreenAd = true
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdShowedFullScreenContent() {
                // označi kao potrošen; pripremi novi
                genericInterstitialAd = null
                consumeAdDecision()
            }
            override fun onAdDismissedFullScreenContent() {
                isShowingFullscreenAd = false
                onContinue()
                preloadGenericInterstitial(activity)
            }
            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                isShowingFullscreenAd = false
                onContinue()
                preloadGenericInterstitial(activity)
            }
        }
        ad.show(activity)
    }


    private fun incrementStartupAdCount(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val today = LocalDate.now().toString()
        val storedDate = prefs.getString(KEY_AD_SHOWN_DATE, null)
        val editor = prefs.edit()
        if (storedDate == today) {
            editor.putInt(KEY_AD_SHOWN_COUNT, prefs.getInt(KEY_AD_SHOWN_COUNT, 0) + 1)
        } else {
            editor.putString(KEY_AD_SHOWN_DATE, today)
            editor.putInt(KEY_AD_SHOWN_COUNT, 1)
        }
        editor.apply()
    }

    /** Preloada sve što koristimo kasnije (rewarded + exit interstitial). */
    fun preload(activity: Activity) {
        loadRewardedAd(activity)
        loadExitInterstitialAd(activity)
    }

    /** Učitava i kešira Rewarded. */
    fun loadRewardedAd(activity: Activity, onLoaded: (() -> Unit)? = null) {
        if (isRewardLoading) return
        isRewardLoading = true
        RewardedAd.load(
            activity,
            REWARDED_ID,
            AdRequest.Builder().build(),
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    rewardedAd = ad
                    isRewardLoading = false
                    Log.d(TAG, "Rewarded loaded")
                    onLoaded?.invoke()
                }
                override fun onAdFailedToLoad(error: LoadAdError) {
                    rewardedAd = null
                    isRewardLoading = false
                    Log.e(TAG, "Rewarded failed: ${error.message}")
                }
            }
        )
    }

    /** Prikaži Rewarded ako je spreman; u suprotnom pozovi fallback odmah. */
    fun showRewardedOrFallback(
        activity: Activity,
        onReward: () -> Unit,
        onShown: (() -> Unit)? = null,
        onClosed: (() -> Unit)? = { loadRewardedAd(activity) },
        onFallback: (() -> Unit)? = null
    ) {
        val ad = rewardedAd
        if (ad == null) {
            Log.d(TAG, "Rewarded not ready, fallback")
            onFallback?.invoke()
            onReward() // i dalje dodeljujemo nagradu
            return
        }
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdShowedFullScreenContent() {
                onShown?.invoke()
            }
            override fun onAdDismissedFullScreenContent() {
                rewardedAd = null
                onClosed?.invoke()
            }
            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                Log.e(TAG, "Rewarded failed to show: ${adError.message}")
                rewardedAd = null
                onFallback?.invoke()
                onReward()
                onClosed?.invoke()
            }
        }
        ad.show(activity) {
            // User earned reward
            onReward()
        }
    }

    /** Exit interstitial preload. */
    fun loadExitInterstitialAd(activity: Activity) {
        if (isExitLoading) return
        isExitLoading = true
        InterstitialAd.load(
            activity,
            EXIT_INTERSTITIAL_ID,
            AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    exitInterstitialAd = ad
                    isExitLoading = false
                }
                override fun onAdFailedToLoad(error: LoadAdError) {
                    exitInterstitialAd = null
                    isExitLoading = false
                    Log.e(TAG, "Exit interstitial failed: ${error.message}")
                }
            }
        )
    }

    /** Prikaži exit interstitial ili uradi fallback akciju (npr. finish()). */
    private var lastAnyAdTimestampMs = 0L
    private const val EXIT_MIN_SESSION_MS = 30_000L   // 30s minimum sesija
    private const val EXIT_COOLDOWN_MS = 90_000L      // 90s od bilo kog drugog oglasa

    fun showExitOr(activity: Activity, fallback: () -> Unit) {
        val now = System.currentTimeMillis()
        val sessionDuration = now - appStartTimestampMs   // zapamtiš kad je startovao app

        val recentlyShown = now - lastAnyAdTimestampMs < EXIT_COOLDOWN_MS
        val tooShortSession = sessionDuration < EXIT_MIN_SESSION_MS

        if (recentlyShown || tooShortSession) {
            fallback()
            return
        }

        val ad = exitInterstitialAd
        if (ad == null) {
            fallback()
            return
        }

        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdShowedFullScreenContent() {
                lastAnyAdTimestampMs = now
            }
            override fun onAdDismissedFullScreenContent() {
                fallback()
            }
            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                fallback()
            }
        }
        ad.show(activity)
    }


    /**
     * Pokuša da prikaže start-up interstitial uz timeout.
     * Vraća true ako je oglas prikazan, false ako nije (timeout, fail, dnevni limit, već prikazan u procesu…)
     */
    suspend fun tryShowStartupInterstitial(
        activity: Activity,
        timeoutMs: Long = 5_000
    ): Boolean {
        if (hasShownStartupThisProcess) return false
        if (!canShowStartupAd(activity)) return false

        val shown = withTimeoutOrNull(timeoutMs) {
            suspendCancellableCoroutine<Boolean> { cont ->
                InterstitialAd.load(
                    activity,
                    STARTUP_INTERSTITIAL_ID,
                    AdRequest.Builder().build(),
                    object : InterstitialAdLoadCallback() {
                        override fun onAdLoaded(ad: InterstitialAd) {
                            ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                                override fun onAdDismissedFullScreenContent() {
                                    if (cont.isActive) cont.resume(true)
                                }
                                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                                    if (cont.isActive) cont.resume(false)
                                }
                            }
                            hasShownStartupThisProcess = true
                            incrementStartupAdCount(activity)
                            ad.show(activity)
                            hasShownStartupThisProcess = true
                            incrementStartupAdCount(activity)
                            markStartupShownNow() // 🔸 startuje grace period

                        }
                        override fun onAdFailedToLoad(error: LoadAdError) {
                            if (cont.isActive) cont.resume(false)
                        }
                    }
                )
            }
        } ?: false

        return shown
    }
}
