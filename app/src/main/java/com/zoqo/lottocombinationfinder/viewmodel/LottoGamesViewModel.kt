// LottoGamesViewModel.kt
package com.zoqo.lottocombinationfinder.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zoqo.lottocombinationfinder.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Locale

data class CountryUi(
    val code: String,
    val display: String,
    val flagUrl: String?
)

class LottoGamesViewModel : ViewModel() {

    private val repo = LottoGameRepository()

    private val _lottoGames = MutableStateFlow<List<LottoGame>>(emptyList())
    val lottoGames: StateFlow<List<LottoGame>> = _lottoGames

    private val _countriesMap = MutableStateFlow<Map<String, CountryDisplay>>(emptyMap())
    val countriesMap: StateFlow<Map<String, CountryDisplay>> = _countriesMap

    init {
        viewModelScope.launch {
            runCatching {
                repo.fetchAll { games, countries ->
                    viewModelScope.launch {
                        _lottoGames.emit(games)
                        _countriesMap.emit(countries)
                    }
                }
            }.onFailure { e ->
                Log.e("VM", "fetchAll failed", e)
            }
        }
    }

    private fun localizedName(code: String, dict: Map<String, CountryDisplay>, locale: Locale): String {
        val entry = dict[code.lowercase()]
        val lang = locale.language.lowercase()
        val region = locale.country.lowercase()
        val tag = if (region.isNotEmpty()) "$lang-$region" else lang
        return entry?.name?.get(tag)
            ?: entry?.name?.get(lang)
            ?: entry?.name?.get("en")
            ?: code.uppercase()
    }

    val countriesDisplay: StateFlow<List<CountryUi>> =
        combine(lottoGames, countriesMap) { games, dict ->
            val locale = Locale.getDefault()
            games.map { it.country }
                .distinct()
                .map { code ->
                    CountryUi(
                        code = code,
                        display = localizedName(code, dict, locale),
                        flagUrl = dict[code.lowercase()]?.flagUrl
                    )
                }
                .sortedBy { it.display.lowercase() }
        }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
}
