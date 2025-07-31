//SavedListScreen.kt
package com.zoqo.lottocombinationfinder.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.zoqo.lottocombinationfinder.components.LottoResult
import com.zoqo.lottocombinationfinder.data.SavedCombinationsManager
import kotlinx.coroutines.launch

@Composable
fun SavedListScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val savedItems by SavedCombinationsManager.getSavedCombinations(context)
        .collectAsState(initial = emptyList())

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {



        if (savedItems.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("No saved combinations")
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(savedItems) { item ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {

                            // ✅ Prikaz kombinacije pomoću LottoResult
                            LottoResult("Combination: ${item.combination}", restartAnimationKey = 0L, modifier = Modifier.weight(1f))
                            Spacer(modifier = Modifier.width(8.dp)) // ➜ dodat razmak
                            // Ikonica za brisanje
                            IconButton(onClick = {
                                scope.launch {
                                    SavedCombinationsManager.deleteCombination(
                                        context,
                                        item.combination
                                    )
                                }
                            }) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete",
                                    tint = MaterialTheme.colorScheme.error // ili Color.Red
                                )
                            }
                        }
                    }
                }

            }
        }
    }
}



