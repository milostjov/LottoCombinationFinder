//MainActivity.kt
package com.zoqo.lottocombinationfinder

import android.os.Bundle
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
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.zoqo.lottocombinationfinder.ads.AdHelper
import com.zoqo.lottocombinationfinder.ads.AdsMediationSetup
import com.zoqo.lottocombinationfinder.ads.BannerAdView
import com.zoqo.lottocombinationfinder.chart.NatalChartScreen
import com.zoqo.lottocombinationfinder.data.AstroPreferencesManager
import com.zoqo.lottocombinationfinder.ui.AstroInputData
import com.zoqo.lottocombinationfinder.ui.AstroUserInputScreen
import com.zoqo.lottocombinationfinder.ui.LottoApp
import com.zoqo.lottocombinationfinder.ui.NoInternetDialog
import com.zoqo.lottocombinationfinder.ui.SavedListScreen
import com.zoqo.lottocombinationfinder.ui.hasInternetConnection
import com.zoqo.lottocombinationfinder.ui.theme.LottoCombinationFinderTheme
import kotlinx.coroutines.launch
import androidx.compose.material3.NavigationBar as M3NavigationBar
import androidx.compose.material3.NavigationBarItem as M3NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults as M3NavigationBarItemDefaults


class MainActivity : ComponentActivity() {

    data class NavItem(val route: String, val iconRes: Int, val label: String)

    private val TAG = "MainActivity"
    private var hasShownStartupAd = false
    private var lastBackPressTime: Long = 0
    object UiConstants {
        val ICON_SIZE: Dp = 36.dp
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AdHelper.markAppStart()
        AdsMediationSetup.initMobileAds(this)
        AdsMediationSetup.obtainConsent(this) {
            // tek sada učitavaš/preloadaš oglase
            AdHelper.preload(this)
            AdHelper.preloadGenericInterstitial(this)
        }
        if (!hasInternetConnection()) {
            setContent {
                LottoCombinationFinderTheme {
                    NoInternetDialog(onRetry = { recreate() })
                }
            }
            return
        }

        // 1) Inicijalizacija AdMob-a
        AdsMediationSetup.initMobileAds(this)

        // 2) Splash UI dok probamo start-up oglas
        setSplashUI()

        // 3) Pokušaj start-up interstitial-a sa timeout-om, pa prikaži glavni UI
        //    + Preload ostalih oglasa
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
            AdHelper.preload(this@MainActivity)
            AdHelper.preloadGenericInterstitial(this@MainActivity)
            // ako prikaže oglas super, ako ne — samo nastavi
            AdHelper.tryShowStartupInterstitial(this@MainActivity, timeoutMs = 5_000)
            showMainUI()
        }
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
                        date = java.time.LocalDate.now(),
                        hour = 12,
                        minute = 0,
                        extraBodies = emptyList()
                    )
                )

                val formattedTitle = remember(astroData) {
                    val dateTime = astroData.date.atTime(astroData.hour, astroData.minute)
                    val dateFormatter = java.time.format.DateTimeFormatter
                        .ofPattern("MMM d, yyyy", java.util.Locale.ENGLISH)
                    "${context.getString(R.string.natal_chart)} for ${dateFormatter.format(dateTime)} "
                }

                BackHandler {
                    if (currentDestination == "lotto") {
                        val now = System.currentTimeMillis()
                        if (now - lastBackPressTime < 3000) {
                            AdHelper.showExitOr(this@MainActivity) { finish() }
                        } else {
                            lastBackPressTime = now
                            Toast.makeText(
                                this@MainActivity,
                                getString(R.string.press_back_again_to_exit),
                                Toast.LENGTH_SHORT
                            ).show()
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
                                        "natal_chart" -> formattedTitle
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
                        Column {
                            // Banner ostaje kao i do sada
                            BannerAdView()

                            val items = listOf(
                                NavItem("lotto", R.drawable.ic_home, stringResource(R.string.home)),
                                NavItem("astro_input", R.drawable.ic_tools, stringResource(R.string.astro_setings)),
                                NavItem("saved_list", R.drawable.ic_list, stringResource(R.string.saved_combinations)),
                                NavItem("natal_chart", R.drawable.ic_astro, stringResource(R.string.natal_chart))
                            )
                            val currentRoute = currentDestination

                            M3NavigationBar(modifier = Modifier.height(56.dp), tonalElevation = 0.dp) {
                                items.forEach { item ->
                                    val selected = currentRoute == item.route
                                    M3NavigationBarItem(
                                        selected = selected,
                                        onClick = {
                                            // prvo definiši lambda funkciju
                                            val proceedNavigation: () -> Unit = {
                                                if (item.route == "lotto") {
                                                    val popped = navController.popBackStack(route = "lotto", inclusive = false)
                                                    if (!popped && currentRoute != "lotto") {
                                                        navController.navigate("lotto") {
                                                            popUpTo(navController.graph.startDestinationId) { saveState = true }
                                                            launchSingleTop = true
                                                            restoreState = true
                                                        }
                                                    }
                                                } else if (currentRoute != item.route) {
                                                    navController.navigate(item.route) {
                                                        launchSingleTop = true
                                                    }
                                                }
                                            }

                                            //  zatim pozovi AdHelper, prosleđujući lambda-u
                                            AdHelper.onNavAction()
                                            AdHelper.maybeShowNavInterstitial(
                                                activity = this@MainActivity,
                                                onContinue = proceedNavigation
                                            )
                                        }
                                        ,
                                        icon = {
                                            Icon(
                                                painter = painterResource(id = item.iconRes),
                                                contentDescription = item.label,
                                                modifier = Modifier.size(UiConstants.ICON_SIZE)
                                            )
                                        },
                                        label = null,
                                        alwaysShowLabel = false,
                                        colors = M3NavigationBarItemDefaults.colors(
                                            indicatorColor = Color.Transparent,
                                            selectedIconColor = MaterialTheme.colorScheme.primary,
                                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    )
                                }
                            }
                        }
                    }
                ) { innerPadding ->
                    androidx.navigation.compose.NavHost(
                        navController = navController,
                        startDestination = "lotto",
                        modifier = Modifier.padding(innerPadding)
                    ) {
                        composable("lotto") {
                            LottoApp(
                                showRewardedAd = { onReward ->
                                    AdHelper.showRewardedOrFallback(
                                        activity = this@MainActivity,
                                        onReward = {
                                            // reset animacije
                                            restartAnimationTrigger.value = System.currentTimeMillis()
                                            onReward()
                                        },
                                        onShown = { /* opcionalno logovanje */ },
                                        onClosed = { AdHelper.loadRewardedAd(this@MainActivity) },
                                        onFallback = { /* opcionalno logovanje */ }
                                    )
                                },
                                restartAnimationKey = restartAnimationTrigger.value
                            )
                        }
                        composable("astro_input") { AstroUserInputScreen(onConfirm = { navController.popBackStack() }) }
                        composable("saved_list") { SavedListScreen() }
                        composable("natal_chart") { NatalChartScreen() }
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
