package com.zoqo.lottocombinationfinder.components

import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.zoqo.lottocombinationfinder.R

@Composable
fun GenerateButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = modifier
    ) {
        Text(stringResource(R.string.find_combination))
    }
}
