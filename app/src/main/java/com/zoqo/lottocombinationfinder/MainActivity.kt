package com.zoqo.lottocombinationfinder

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.zoqo.lottocombinationfinder.components.*
import com.zoqo.lottocombinationfinder.ui.theme.LottoCombinationFinderTheme
import com.zoqo.lottocombinationfinder.utils.calculateTotalCombinations
import com.zoqo.lottocombinationfinder.utils.findCombination
import java.math.BigInteger
import com.google.android.gms.ads.MobileAds
import com.zoqo.lottocombinationfinder.ads.AppOpenAdManager
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardItem
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.google.android.gms.ads.FullScreenContentCallback
import android.util.Log
import android.widget.Toast
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.LoadAdError


class MainActivity : ComponentActivity() {
    private var rewardedAd: RewardedAd? = null
    private var isAdLoading = false
    private val TAG = "RewardedAd"
    private var exitInterstitialAd: InterstitialAd? = null
    private var isExitAdLoading = false


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("MainActivity", "onCreate STARTED")
        Toast.makeText(this, "MainActivity started", Toast.LENGTH_SHORT).show()
        try {
            MobileAds.initialize(this) {}
            Log.d("MainActivity", "MobileAds initialized.")
        } catch (e: Exception) {
            Log.e("MainActivity", "MobileAds init failed: ${e.message}")
        }

        try {
            loadRewardedAd()
            Log.d("MainActivity", "Rewarded ad loading initiated.")
        } catch (e: Exception) {
            Log.e("MainActivity", "RewardedAd loading failed: ${e.message}")
        }

        setContent {
            LottoCombinationFinderTheme {
                LottoApp(
                    showRewardedAd = { callback ->
                        try {
                            rewardedAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
                                override fun onAdDismissedFullScreenContent() {
                                    loadRewardedAd()
                                }
                            }

                            rewardedAd?.show(this@MainActivity) {
                                callback()
                            } ?: Log.d(TAG, "Reklama nije spremna.")
                        } catch (e: Exception) {
                            Log.e(TAG, "Greška u prikazu reklame: ${e.message}")
                        }
                    }
                )
            }
        }
    }


    private fun loadExitInterstitialAd() {
        if (isExitAdLoading) return
        isExitAdLoading = true

        val adRequest = AdRequest.Builder().build()
        InterstitialAd.load(
            this,
            "ca-app-pub-3940256099942544/1033173712", // test ad unit ID
            adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    exitInterstitialAd = ad
                    isExitAdLoading = false
                }

                override fun onAdFailedToLoad(adError: LoadAdError) {
                    exitInterstitialAd = null
                    isExitAdLoading = false
                }
            }
        )
    }

    override fun onBackPressed() {
        if (exitInterstitialAd != null) {
            exitInterstitialAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    finish() // napusti app nakon prikaza reklame
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    finish() // ako reklama ne može da se prikaže
                }
            }
            exitInterstitialAd?.show(this)
        } else {
            super.onBackPressed()
        }
    }


    private fun loadRewardedAd() {
        if (isAdLoading) return
        isAdLoading = true

        val adRequest = AdRequest.Builder().build()
        RewardedAd.load(this,
            "ca-app-pub-3940256099942544/5224354917", // test ad unit ID
            adRequest,
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    rewardedAd = ad
                    isAdLoading = false
                    Log.d(TAG, "Rewarded ad loaded.")
                }

                override fun onAdFailedToLoad(loadAdError: com.google.android.gms.ads.LoadAdError) {
                    rewardedAd = null
                    isAdLoading = false
                    Log.e(TAG, "Failed to load rewarded ad: ${loadAdError.message}")
                }
            })
    }
}





@Composable
fun LottoApp(showRewardedAd: ((onReward: () -> Unit) -> Unit)) {
    var rankInput by remember { mutableStateOf(TextFieldValue("")) }
    var resultText by remember { mutableStateOf("") }
    var totalNumbers by remember { mutableStateOf(TextFieldValue("39")) }
    var numbersToChoose by remember { mutableStateOf(TextFieldValue("7")) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        content = { innerPadding ->
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                LottoInputFields(
                    totalNumbers = totalNumbers,
                    onTotalNumbersChange = { totalNumbers = it },
                    numbersToChoose = numbersToChoose,
                    onNumbersToChooseChange = { numbersToChoose = it },
                    rankInput = rankInput,
                    onRankInputChange = { rankInput = it }
                )

                GenerateButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        showRewardedAd {
                            val total = totalNumbers.text.toIntOrNull()
                            val choose = numbersToChoose.text.toIntOrNull()
                            val rank = rankInput.text.toBigIntegerOrNull()

                            resultText = when {
                                total == null || choose == null || total <= 0 || choose <= 0 || total < choose ->
                                    "Invalid input for total numbers or numbers to choose."
                                rank == null || rank <= BigInteger.ZERO || rank > calculateTotalCombinations(total, choose) ->
                                    "Invalid rank. Enter a number between 1 and ${calculateTotalCombinations(total, choose)}"
                                else -> {
                                    val combination = findCombination(rank.toInt(), total, choose)
                                    "Combination: ${combination.joinToString(", ")}"
                                }
                            }
                        }
                    }
                )

                LottoResult(resultText)
            }
        }
    )
}

