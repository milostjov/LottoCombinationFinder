package com.zoqo.lottocombinationfinder.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun WelcomeScreen(
    onLoginClicked: () -> Unit,
    onContinueWithoutLogin: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp)
    ) {
        Text("Dobrodošao u Astro AI", style = MaterialTheme.typography.headlineMedium)

        Spacer(modifier = Modifier.height(24.dp))

        Button(onClick = onLoginClicked) {
            Text("Uloguj se sa Google nalogom")
        }

        Spacer(modifier = Modifier.height(16.dp))

        TextButton(onClick = onContinueWithoutLogin) {
            Text("Nastavi bez naloga")
        }
    }
}
