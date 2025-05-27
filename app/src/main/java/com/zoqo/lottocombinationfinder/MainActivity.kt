package com.zoqo.lottocombinationfinder

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import com.google.android.gms.ads.*
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
//import org.maplibre.android.maps.MapView
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
import org.maplibre.android.geometry.LatLng

class MainActivity : ComponentActivity() {

    private val TAG = "MainActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Inicijalizacija MapLibre-a
        MapLibre.getInstance(
            applicationContext,
            null, // API ključ ako koristiš MapLibre tiles
            WellKnownTileServer.MapLibre
        )

        MobileAds.initialize(this) {}

        // Učitavanje početne interstitial reklame
        InterstitialAd.load(
            this,
            "ca-app-pub-3940256099942544/1033173712",
            AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    ad.show(this@MainActivity)
                }

                override fun onAdFailedToLoad(adError: LoadAdError) {
                    Log.e(TAG, "Interstitial ad failed to load: ${adError.message}")
                }
            }
        )

        AdHelper.loadRewardedAd(this)
        AdHelper.loadExitInterstitialAd(this)

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


                NavHost(navController, startDestination = "lotto") {
                    composable("lotto") {
                        LottoApp(
                            showRewardedAd = { onReward ->
                                val ad = AdHelper.rewardedAd
                                if (ad != null) {
                                    ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                                        override fun onAdDismissedFullScreenContent() {
                                            AdHelper.loadRewardedAd(this@MainActivity)
                                        }

                                        override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                                            Log.e(TAG, "Rewarded ad failed: ${adError.message}")
                                            onReward()
                                        }
                                    }
                                    ad.show(this@MainActivity) { onReward() }
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


                        // ⬇ Stream koji se automatski ažurira ako neko upiše u savedStateHandle
                        val pickedLat by backStackEntry.savedStateHandle
                            .getStateFlow("picked_lat", latitude)
                            .collectAsState()

                        val pickedLon by backStackEntry.savedStateHandle
                            .getStateFlow("picked_lon", longitude)
                            .collectAsState()

                        // ⬇ Kada se novi podaci detektuju — upiši ih i očisti da se ne vrti ponovo
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
                        val lat = backStackEntry.arguments?.getFloat("lat")?.toDouble() ?: 44.0
                        val lon = backStackEntry.arguments?.getFloat("lon")?.toDouble() ?: 20.0

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
