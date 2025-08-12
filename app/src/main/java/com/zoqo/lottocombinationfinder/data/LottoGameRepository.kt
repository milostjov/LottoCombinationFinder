package com.zoqo.lottocombinationfinder.data

import android.util.Log
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.remoteConfigSettings
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

data class LottoGame(
    val name: String = "",
    val country: String = "",
    val totalNumbers: Int = 0,
    val numbersToChoose: Int = 0
)

class LottoGameRepository {

    private val remoteConfig: FirebaseRemoteConfig = FirebaseRemoteConfig.getInstance()

    init {
        val configSettings = remoteConfigSettings {
            minimumFetchIntervalInSeconds = 3600 // cache 1h minimumFetchIntervalInSeconds = 259200 // 3 dana
        }
        remoteConfig.setConfigSettingsAsync(configSettings)
        remoteConfig.setDefaultsAsync(
            mapOf("lotto_games" to "[]")
        )
    }

    fun fetchLottoGames(onResult: (List<LottoGame>) -> Unit) {
        remoteConfig.fetch(0) // 0 sec cache → za testiranje  // remoteConfig.fetchAndActivate().addOnCompleteListener { task ->
            .addOnCompleteListener { fetchTask ->
                val info = remoteConfig.info
                Log.d("RC", "lastFetchStatus=${info.lastFetchStatus}, lastFetchTime=${info.fetchTimeMillis}")

                if (!fetchTask.isSuccessful) {
                    Log.e("RC", "Fetch failed", fetchTask.exception)
                }

                remoteConfig.activate().addOnCompleteListener {
                    val keys = remoteConfig.getKeysByPrefix("")
                    Log.d("RC", "keys=$keys")

                    val json = remoteConfig.getString("lotto_games")
                    Log.d("RC", "lotto_games=$json")

                    val listType = object : TypeToken<List<LottoGame>>() {}.type
                    val games: List<LottoGame> = Gson().fromJson(json, listType)

                    onResult(games)
                }
            }
    }
}
