package com.zoqo.lottocombinationfinder.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp

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
        TextField(
            value = totalNumbers,
            onValueChange = onTotalNumbersChange,
            label = { Text("Total numbers") },
            modifier = Modifier.fillMaxWidth()
        )
        TextField(
            value = numbersToChoose,
            onValueChange = onNumbersToChooseChange,
            label = { Text("Numbers to choose") },
            modifier = Modifier.fillMaxWidth()
        )
        TextField(
            value = rankInput,
            onValueChange = onRankInputChange,
            label = { Text("Enter rank (e.g. your date of birth)") },
            modifier = Modifier.fillMaxWidth()
        )
    }
}
