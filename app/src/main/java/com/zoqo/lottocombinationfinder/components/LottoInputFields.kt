package com.zoqo.lottocombinationfinder.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.zoqo.lottocombinationfinder.R

@Composable
fun LottoInputFields(
    totalNumbers: TextFieldValue,
    onTotalNumbersChange: (TextFieldValue) -> Unit,
    numbersToChoose: TextFieldValue,
    onNumbersToChooseChange: (TextFieldValue) -> Unit,
    rankInput: TextFieldValue,
    onRankInputChange: (TextFieldValue) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = totalNumbers,
            onValueChange = onTotalNumbersChange,
            label = { Text(stringResource(R.string.total_numbers)) },
            keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = numbersToChoose,
            onValueChange = onNumbersToChooseChange,
            label = { Text(stringResource(R.string.numbers_to_choose)) },
            keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = rankInput,
            onValueChange = onRankInputChange,
            label = { Text(stringResource(R.string.enter_rank_hint)) },
            keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )
    }


}
