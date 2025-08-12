package com.zoqo.lottocombinationfinder.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zoqo.lottocombinationfinder.data.LottoGame
import com.zoqo.lottocombinationfinder.data.LottoGameRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class LottoGamesViewModel : ViewModel() {

    private val repository = LottoGameRepository()

    private val _lottoGames = MutableStateFlow<List<LottoGame>>(emptyList())
    val lottoGames: StateFlow<List<LottoGame>> = _lottoGames

    init {
        fetchGames()
    }

    private fun fetchGames() {
        repository.fetchLottoGames { games ->
            viewModelScope.launch {
                _lottoGames.emit(games)
            }
        }
    }
}
