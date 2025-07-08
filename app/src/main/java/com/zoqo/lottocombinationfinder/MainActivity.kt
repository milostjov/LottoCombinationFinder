//MainActivity.kt
package com.zoqo.lottocombinationfinder

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.google.android.gms.ads.*
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.zoqo.lottocombinationfinder.ads.AdHelper
import com.zoqo.lottocombinationfinder.ui.AstroUserInputScreen
import com.zoqo.lottocombinationfinder.ui.LottoApp
import com.zoqo.lottocombinationfinder.ui.theme.LottoCombinationFinderTheme
import org.maplibre.android.MapLibre
import org.maplibre.android.WellKnownTileServer

import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.zoqo.lottocombinationfinder.components.MapLibrePickerScreen
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.ui.Alignment
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource

class MainActivity : ComponentActivity() {

    private val TAG = "MainActivity"
    private var hasShownStartupAd = false



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        hasShownStartupAd = savedInstanceState?.getBoolean("shown_ad") ?: false

        initMapLibre()
        initMobileAds()
        preloadOtherAds()

        if (!hasShownStartupAd) {
            // prikaz privremenog splash ekrana
            setSplashUI()
            loadAdWithTimeout()
        } else {
            showMainUI()
        }
    }





    private fun initMapLibre() {
        MapLibre.getInstance(
            applicationContext,
            null, // API ključ ako koristiš MapLibre tiles
            WellKnownTileServer.MapLibre
        )
    }

    private fun initMobileAds() {
        MobileAds.initialize(this) {}
    }

    private fun setSplashUI() {
        setContent {
            Box(modifier = Modifier.fillMaxSize()) {
                // Slika kao pozadina
                Image(
                    painter = painterResource(R.drawable.splash_gold),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                // Sadržaj preko slike
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




    private fun loadAdWithTimeout() {
        val scope = CoroutineScope(Dispatchers.Main)

        scope.launch {
            val adShown = withTimeoutOrNull(5000) { // 5 sekundi timeout
                suspendCancellableCoroutine { continuation ->
                    InterstitialAd.load(
                        this@MainActivity,
                        "ca-app-pub-3940256099942544/1033173712",
                        AdRequest.Builder().build(),
                        object : InterstitialAdLoadCallback() {
                            override fun onAdLoaded(ad: InterstitialAd) {
                                hasShownStartupAd = true
                                ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                                    override fun onAdDismissedFullScreenContent() {
                                        continuation.resume(Unit) {} // reklama gotova
                                    }

                                    override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                                        continuation.resume(Unit) {}
                                    }
                                }
                                ad.show(this@MainActivity)
                            }

                            override fun onAdFailedToLoad(adError: LoadAdError) {
                                continuation.resume(Unit) {} // nije uspela
                            }
                        }
                    )
                }
            }

            // bilo da je reklama prikazana ili ne — nastavi na glavni UI
            showMainUI()
        }
    }



    private fun preloadOtherAds() {
        AdHelper.loadRewardedAd(this)
        AdHelper.loadExitInterstitialAd(this)
    }

    @OptIn(ExperimentalMaterial3Api::class)
    private fun showMainUI() {
        setContent {
            LottoCombinationFinderTheme {
                val navController = rememberNavController()

                val currentBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = currentBackStackEntry?.destination?.route

                BackHandler {
                    if (currentDestination == "lotto") {
                        val ad = AdHelper.exitInterstitialAd
                        if (ad != null) {
                            ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                                override fun onAdDismissedFullScreenContent() {
                                    (this@MainActivity).finish()
                                }

                                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                                    (this@MainActivity).finish()
                                }
                            }
                            ad.show(this@MainActivity)
                        } else {
                            finish()
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
                                        "map_picker/{lat}/{lon}" -> stringResource(R.string.select_precisely_on_map)
                                        else -> ""
                                    }
                                )
                            },
                            navigationIcon = {
                                if (currentDestination != "lotto") {
                                    IconButton(onClick = { navController.popBackStack() }) {
                                        Icon(
                                            Icons.AutoMirrored.Filled.ArrowBack,
                                            contentDescription = stringResource(R.string.back_prefix)
                                        )
                                    }
                                }
                            }
                        )
                    },



                    bottomBar = {
                        BottomAppBar {
                            IconButton(
                                onClick = {
                                    navController.navigate("lotto") {
                                        popUpTo(navController.graph.startDestinationId) {
                                            inclusive = true
                                        }
                                        launchSingleTop = true
                                    }
                                },
                                modifier = Modifier.padding(horizontal = 16.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Home,
                                    contentDescription = stringResource(R.string.home)
                                )
                            }

                            IconButton(
                                onClick = { navController.navigate("astro_input") },
                                modifier = Modifier.padding(horizontal = 16.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Settings,
                                    contentDescription = stringResource(R.string.astro_setings)
                                )
                            }
                        }
                    }

                )
                { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = "lotto",
                        modifier = Modifier.padding(innerPadding)
                    ) {
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
                                            onReward()
                                        })

                                    } else {
                                        Log.d(TAG, "Rewarded ad not ready")
                                        onReward()
                                    }
                                },
                                onAstroSettingsClick = {
                                    navController.navigate("astro_input")
                                }
                            )
                        }

                        composable("astro_input") { backStackEntry ->
                            var latitude by rememberSaveable { mutableStateOf(40.7128) }
                            var longitude by rememberSaveable { mutableStateOf(-74.0060) }

                            val pickedLat by backStackEntry.savedStateHandle
                                .getStateFlow("picked_lat", latitude)
                                .collectAsState()

                            val pickedLon by backStackEntry.savedStateHandle
                                .getStateFlow("picked_lon", longitude)
                                .collectAsState()

                            LaunchedEffect(pickedLat, pickedLon) {
                                if (pickedLat != latitude || pickedLon != longitude) {
                                    latitude = pickedLat
                                    longitude = pickedLon
                                    backStackEntry.savedStateHandle.remove<Double>("picked_lat")
                                    backStackEntry.savedStateHandle.remove<Double>("picked_lon")
                                }
                            }

                            AstroUserInputScreen(
                                onConfirm = { navController.popBackStack() },
                                onOpenMap = { lat, lon ->
                                    navController.navigate("map_picker/$lat/$lon")
                                },
                                initialLat = latitude,
                                initialLon = longitude
                            )
                        }

                        composable(
                            "map_picker/{lat}/{lon}",
                            arguments = listOf(
                                navArgument("lat") { type = NavType.FloatType },
                                navArgument("lon") { type = NavType.FloatType }
                            )
                        ) { backStackEntry ->
                            val lat = backStackEntry.arguments?.getFloat("lat")?.toDouble() ?: 40.7128
                            val lon = backStackEntry.arguments?.getFloat("lon")?.toDouble() ?: -74.0060

                            MapLibrePickerScreen(
                                initialLat = lat,
                                initialLon = lon,
                                onLocationPicked = { newLat, newLon ->
                                    navController.previousBackStackEntry
                                        ?.savedStateHandle?.apply {
                                            set("picked_lat", newLat)
                                            set("picked_lon", newLon)
                                        }
                                    navController.popBackStack()
                                },
                                onCancel = {
                                    navController.popBackStack()
                                }
                            )
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
