package com.zoqo.lottocombinationfinder.chart

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.zoqo.lottocombinationfinder.data.AstroPreferencesManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

import com.zoqo.lottocombinationfinder.astro.AstroCalculator

@Composable
fun NatalChartScreen() {
    val context = LocalContext.current
    val data = remember {
        runBlocking {
            AstroPreferencesManager.load(context).first()
        }
    }

    val planetPositions = remember {
        AstroCalculator.getPlanetPositions(data)
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        //Text("Natalna karta za ${data.cityName}", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(16.dp))
        NatalChartView(planets = planetPositions)
    }
}



