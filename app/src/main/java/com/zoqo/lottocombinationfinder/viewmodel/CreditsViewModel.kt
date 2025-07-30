package com.zoqo.lottocombinationfinder.viewmodel

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class CreditsViewModel : ViewModel() {
    private val _credits = MutableStateFlow(1)
    val credits: StateFlow<Int> = _credits

    fun increment(amount: Int = 1) {
        _credits.value += amount
    }

    fun decrement(amount: Int = 1) {
        _credits.value = (_credits.value - amount).coerceAtLeast(0)
    }

    fun set(value: Int) {
        _credits.value = value
    }
}
