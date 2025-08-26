// LottoInputFields.kt
package com.zoqo.lottocombinationfinder.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
//import androidx.compose.ui.text.input.KeyboardOptions
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.zoqo.lottocombinationfinder.R
import com.zoqo.lottocombinationfinder.viewmodel.LottoGamesViewModel
import com.zoqo.lottocombinationfinder.viewmodel.CountryUi
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LottoInputFields(
    commonLottoGames: List<String>,
    selectedGame: String,
    onGameSelected: (String) -> Unit,

    countries: List<CountryUi>,      // ⬅⬅ promena
    selectedCountryLabel: String,    // prikaz u polju (npr. "USA"/"SAD")
    onCountrySelected: (CountryUi) -> Unit, // ⬅⬅ vrati ceo objekat

    totalNumbers: TextFieldValue,
    onTotalNumbersChange: (TextFieldValue) -> Unit,
    numbersToChoose: TextFieldValue,
    onNumbersToChooseChange: (TextFieldValue) -> Unit
) {
    var expandedGame by remember { mutableStateOf(false) }
    var expandedCountry by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {

        // 📌 Dropdown za izbor države
        ExposedDropdownMenuBox(
            expanded = expandedCountry,
            onExpandedChange = { expandedCountry = !expandedCountry }
        ) {
            OutlinedTextField(
                value = selectedCountryLabel,
                onValueChange = {},
                label = { Text("Choose a Country") },
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth(),
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedCountry) }
            )
            ExposedDropdownMenu(
                expanded = expandedCountry,
                onDismissRequest = { expandedCountry = false }
            ) {
                countries.forEach { item ->
                    DropdownMenuItem(
                        text = {
                            androidx.compose.foundation.layout.Row(
                                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                            ) {
                                if (!item.flagUrl.isNullOrBlank()) {
                                    coil.compose.AsyncImage(
                                        model = coil.request.ImageRequest.Builder(LocalContext.current)
                                            .data(item.flagUrl)
                                            .decoderFactory(coil.decode.SvgDecoder.Factory()) // SVG podrška
                                            .build(),
                                        contentDescription = null,
                                        modifier = Modifier
                                            .size(20.dp)
                                            .clip(androidx.compose.foundation.shape.RoundedCornerShape(3.dp))
                                    )
                                    androidx.compose.foundation.layout.Spacer(Modifier.size(8.dp))
                                }
                                Text(item.display)
                            }
                        },
                        onClick = {
                            onCountrySelected(item)
                            expandedCountry = false
                        }
                    )
                }
            }
        }

        // 📌 Dropdown za izbor igre
        ExposedDropdownMenuBox(
            expanded = expandedGame,
            onExpandedChange = { expandedGame = !expandedGame }
        ) {
            OutlinedTextField(
                value = selectedGame,
                onValueChange = {},
                label = { Text("Choose a Lottery Game") },
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth(),
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedGame) }
            )
            ExposedDropdownMenu(
                expanded = expandedGame,
                onDismissRequest = { expandedGame = false }
            ) {
                commonLottoGames.forEach { game ->
                    DropdownMenuItem(
                        text = { Text(game) },
                        onClick = {
                            onGameSelected(game)
                            expandedGame = false
                        }
                    )
                }
            }
        }

        // 📌 Polje: total numbers
        OutlinedTextField(
            value = totalNumbers,
            onValueChange = onTotalNumbersChange,
            label = { Text(stringResource(R.string.total_numbers)) },
            keyboardOptions = KeyboardOptions.Default.copy(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )

        // 📌 Polje: numbers to choose
        OutlinedTextField(
            value = numbersToChoose,
            onValueChange = onNumbersToChooseChange,
            label = { Text(stringResource(R.string.numbers_to_choose)) },
            keyboardOptions = KeyboardOptions.Default.copy(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )
    }
}
