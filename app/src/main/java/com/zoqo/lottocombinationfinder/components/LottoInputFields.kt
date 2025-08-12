package com.zoqo.lottocombinationfinder.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.zoqo.lottocombinationfinder.R


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LottoInputFields(
    commonLottoGames: List<String>,
    selectedGame: String,
    onGameSelected: (String) -> Unit,
    countries: List<String>,
    selectedCountry: String,
    onCountrySelected: (String) -> Unit,
    totalNumbers: TextFieldValue,
    onTotalNumbersChange: (TextFieldValue) -> Unit,
    numbersToChoose: TextFieldValue,
    onNumbersToChooseChange: (TextFieldValue) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var expandedCountry by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {



        // 📌 Dropdown za izbor države
        ExposedDropdownMenuBox(
            expanded = expandedCountry,
            onExpandedChange = { expandedCountry = !expandedCountry }
        ) {
            OutlinedTextField(
                value = selectedCountry,
                onValueChange = {},
                label = { Text("Choose a Country") },
                modifier = Modifier
                    .menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true)
                    .fillMaxWidth(),
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedCountry) }
            )
            ExposedDropdownMenu(
                expanded = expandedCountry,
                onDismissRequest = { expandedCountry = false }
            ) {
                countries.forEach { country ->
                    DropdownMenuItem(
                        text = { Text(country) },
                        onClick = {
                            onCountrySelected(country)
                            expandedCountry = false
                        }
                    )
                }
            }
        }

        // 📌 Dropdown za izbor igre
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded }
        ) {
            OutlinedTextField(
                value = selectedGame,
                onValueChange = {},
                label = { Text("Choose a Lottery Game") },
                modifier = Modifier
                    .menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true)
                    .fillMaxWidth(),
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) }
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                commonLottoGames.forEach { game ->
                    DropdownMenuItem(
                        text = { Text(game) },
                        onClick = {
                            onGameSelected(game)
                            expanded = false
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
            keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )

        // 📌 Polje: numbers to choose
        OutlinedTextField(
            value = numbersToChoose,
            onValueChange = onNumbersToChooseChange,
            label = { Text(stringResource(R.string.numbers_to_choose)) },
            keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )
    }
}
