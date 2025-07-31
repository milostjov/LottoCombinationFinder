//MainActivity.kt
package com.zoqo.lottocombinationfinder

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.OnUserEarnedRewardListener
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.zoqo.lottocombinationfinder.ads.AdHelper
import com.zoqo.lottocombinationfinder.ads.BannerAdView
import com.zoqo.lottocombinationfinder.data.AstroPreferencesManager
import com.zoqo.lottocombinationfinder.ui.AstroInputData
import com.zoqo.lottocombinationfinder.ui.AstroUserInputScreen
import com.zoqo.lottocombinationfinder.ui.LottoApp
import com.zoqo.lottocombinationfinder.chart.NatalChartScreen
import com.zoqo.lottocombinationfinder.ui.NoInternetDialog
import com.zoqo.lottocombinationfinder.ui.SavedListScreen
import com.zoqo.lottocombinationfinder.ui.hasInternetConnection
import com.zoqo.lottocombinationfinder.ui.theme.LottoCombinationFinderTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

class MainActivity : ComponentActivity() {

    private val TAG = "MainActivity"
    private var hasShownStartupAd = false
    private var lastBackPressTime: Long = 0
    object UiConstants {
        val ICON_SIZE: Dp = 36.dp
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!hasInternetConnection()) {
            setContent {
                LottoCombinationFinderTheme {
                    NoInternetDialog(onRetry = { recreate() })
                }
            }
            return
        }

        hasShownStartupAd = savedInstanceState?.getBoolean("shown_ad") ?: false

        initMobileAds()
        preloadOtherAds()

        if (!hasShownStartupAd && AdHelper.canShowStartupAd(this)) {
            setSplashUI()
            loadAdWithTimeout()
        } else {
            showMainUI()
        }

    }


    private fun initMobileAds() {
        MobileAds.initialize(this) {}
    }

    private fun setSplashUI() {
        setContent {
            Box(modifier = Modifier.fillMaxSize()) {
                Image(
                    painter = painterResource(R.drawable.splash_gold),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(modifier = Modifier.height(32.dp))
                    CircularProgressIndicator(color = Color.White)
                }
            }
        }
    }
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    private fun loadAdWithTimeout() {
        val scope = CoroutineScope(Dispatchers.Main)
        scope.launch {
            withTimeoutOrNull(5000) {
                suspendCancellableCoroutine { continuation ->
                    InterstitialAd.load(
                        this@MainActivity,
                        "ca-app-pub-3940256099942544/1033173712",
                        AdRequest.Builder().build(),
                        object : InterstitialAdLoadCallback() {
                            override fun onAdLoaded(ad: InterstitialAd) {
                                hasShownStartupAd = true
                                AdHelper.incrementStartupAdCount(this@MainActivity)

                                ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                                    override fun onAdDismissedFullScreenContent() {
                                        continuation.resume(Unit) {}
                                    }
                                    override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                                        continuation.resume(Unit) {}
                                    }
                                }

                                ad.show(this@MainActivity)
                            }

                            override fun onAdFailedToLoad(adError: LoadAdError) {
                                continuation.resume(Unit) {}
                            }
                        }
                    )
                }
            }
            showMainUI()
        }
    }

    private fun preloadOtherAds() {
        AdHelper.loadRewardedAd(this)
        AdHelper.loadExitInterstitialAd(this)
    }

    // Removed login and credits dialog from the UI for simplified app
    @OptIn(ExperimentalMaterial3Api::class)
    private fun showMainUI() {
        setContent {
            LottoCombinationFinderTheme {
                val navController = rememberNavController()
                val currentBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = currentBackStackEntry?.destination?.route
                val context = LocalContext.current
                val restartAnimationTrigger = remember { mutableStateOf(0L) }
                val astroData by AstroPreferencesManager.load(context).collectAsState(
                    initial = AstroInputData(
                        date = LocalDate.now(),
                        hour = 12,
                        minute = 0,
                        extraBodies = emptyList()
                    )
                )
                val formattedTitle = remember(astroData) {
                    val dateTime = astroData.date.atTime(astroData.hour, astroData.minute)

                    val dateFormatter = DateTimeFormatter
                        .ofPattern("MMM d, yyyy", Locale.ENGLISH)



                    "${context.getString(R.string.natal_chart)} for ${dateFormatter.format(dateTime)} "
                }




                BackHandler {
                    if (currentDestination == "lotto") {
                        val currentTime = System.currentTimeMillis()
                        if (currentTime - lastBackPressTime < 3000) {
                            val ad = AdHelper.exitInterstitialAd
                            if (ad != null) {
                                ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                                    override fun onAdDismissedFullScreenContent() {
                                        this@MainActivity.finish()
                                    }
                                    override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                                        this@MainActivity.finish()
                                    }
                                }
                                ad.show(this@MainActivity)
                            } else {
                                finish()
                            }
                        } else {
                            lastBackPressTime = currentTime
                            Toast.makeText(this@MainActivity, getString(R.string.press_back_again_to_exit), Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        navController.popBackStack()
                    }
                }

                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = {
                                Text(
                                    when (currentDestination) {
                                        "lotto" -> stringResource(R.string.app_name)
                                        "astro_input" -> stringResource(R.string.astro_setings)
                                        "saved_list" -> stringResource(R.string.saved_combinations)
                                        "natal_chart" ->  formattedTitle
                                        else -> ""
                                    }
                                )
                            },
                            navigationIcon = {
                                if (currentDestination != "lotto") {
                                    IconButton(onClick = { navController.popBackStack() }) {
                                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back_prefix))
                                    }
                                }
                            }
                        )
                    },
                    bottomBar = {
                        Column {
                            BannerAdView()
                            BottomAppBar {
                                IconButton(onClick = {
                                    navController.navigate("lotto") {
                                        popUpTo(navController.graph.startDestinationId) { inclusive = true }
                                        launchSingleTop = true
                                    }
                                }, modifier = Modifier.padding(horizontal = 16.dp)) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_home),
                                        tint = MaterialTheme.colorScheme.primary,
                                        contentDescription = stringResource(R.string.home),
                                        modifier = Modifier.size(UiConstants.ICON_SIZE)
                                    )
                                }
                                IconButton(onClick = { navController.navigate("astro_input") }, modifier = Modifier.padding(horizontal = 16.dp)) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_tools),
                                        tint = MaterialTheme.colorScheme.primary,
                                        contentDescription = stringResource(R.string.astro_setings),
                                        modifier = Modifier.size(UiConstants.ICON_SIZE)
                                    )
                                }


                                IconButton(onClick = { navController.navigate("saved_list") }, modifier = Modifier.padding(horizontal = 16.dp)) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_list), // tvoja ikonica
                                        contentDescription = "Saved",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(UiConstants.ICON_SIZE)
                                    )
                                }

                                IconButton(onClick = { navController.navigate("natal_chart") }, modifier = Modifier.padding(horizontal = 16.dp)) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_astro),
                                        contentDescription = stringResource(R.string.natal_chart),
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(UiConstants.ICON_SIZE)
                                    )
                                }



                            }
                        }
                    }
                ) { innerPadding ->
                    NavHost(navController = navController, startDestination = "lotto", modifier = Modifier.padding(innerPadding)) {
                        composable("lotto") {
                            LottoApp(
                                showRewardedAd = { onReward ->
                                    val ad = AdHelper.rewardedAd
                                    if (ad != null) {
                                        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                                            override fun onAdDismissedFullScreenContent() {
                                                Log.d(TAG, "Rewarded ad dismissed")
                                                AdHelper.loadRewardedAd(this@MainActivity)
                                            }
                                            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                                                Log.e(TAG, "Rewarded ad failed: ${adError.message}")
                                                onReward()
                                            }
                                            override fun onAdShowedFullScreenContent() {
                                                Log.d(TAG, "Rewarded ad showed")
                                            }
                                        }
                                        ad.show(this@MainActivity, OnUserEarnedRewardListener {
                                            Log.d(TAG, "User earned reward")
                                            restartAnimationTrigger.value = System.currentTimeMillis() // 🔥 reset animacije
                                            onReward()
                                        })
                                    } else {
                                        Log.d(TAG, "Rewarded ad not ready")
                                        onReward()
                                    }
                                }
,
                                restartAnimationKey = restartAnimationTrigger.value   // 👈 DODATO
                            )
                        }
                        composable("astro_input") {
                            AstroUserInputScreen(
                                onConfirm = { navController.popBackStack() }
                            )
                        }
                        composable("saved_list") {
                            SavedListScreen()
                        }


                        composable("natal_chart") {
                            NatalChartScreen()
                        }
                    }
                }
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean("shown_ad", hasShownStartupAd)
    }
}
