// AstroPreferencesManager.kt

package com.zoqo.lottocombinationfinder.data

import android.content.Context
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.zoqo.lottocombinationfinder.ui.AstroInputData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

val Context.astroDataStore by preferencesDataStore(name = "astro_prefs")

object AstroPreferencesManager {

    private val DATE_KEY = longPreferencesKey("birth_date_millis")
    private val HOUR_KEY = intPreferencesKey("birth_hour")
    private val MINUTE_KEY = intPreferencesKey("birth_minute")
    // dodatni astrološki parametri

    private val EXTRA_BODIES_KEY = stringPreferencesKey("extra_bodies")                // npr. "Ceres,Chiron,Eris"


    // postojeći loto ključevi
    private val TOTAL_NUMBERS_KEY = stringPreferencesKey("total_numbers")
    private val NUMBERS_TO_CHOOSE_KEY = stringPreferencesKey("numbers_to_choose")
    private val RANK_INPUT_KEY = stringPreferencesKey("rank_input")

    suspend fun save(context: Context, data: AstroInputData) {
        context.astroDataStore.edit { prefs ->
            prefs[DATE_KEY] = data.date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            prefs[HOUR_KEY] = data.hour
            prefs[MINUTE_KEY] = data.minute
            data.extraBodies?.let { prefs[EXTRA_BODIES_KEY] = it.joinToString(",") }
            prefs[RANK_INPUT_KEY] = data.rank
        }
    }

    fun load(context: Context): Flow<AstroInputData> {
        return context.astroDataStore.data.map { prefs ->
            val date = prefs[DATE_KEY]?.let {
                Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate()
            } ?: LocalDate.now()

            val hour = prefs[HOUR_KEY] ?: 12
            val minute = prefs[MINUTE_KEY] ?: 0

            // dodatna polja
            val extraBodies = prefs[EXTRA_BODIES_KEY]
                ?.split(",")
                ?.map { it.trim() }
                ?.filter { it.isNotEmpty() }
                ?: listOf("Mercury", "Chiron")
            val rank = prefs[RANK_INPUT_KEY] ?: "123456"

            AstroInputData(
                date = date,
                hour = hour,
                minute = minute,
                extraBodies = extraBodies,
                rank = rank
            )
        }
    }

    suspend fun saveLottoSettings(context: Context, total: String, choose: String) {
        context.astroDataStore.edit { prefs ->
            prefs[TOTAL_NUMBERS_KEY] = total
            prefs[NUMBERS_TO_CHOOSE_KEY] = choose

        }
    }

    fun loadLottoSettings(context: Context): Flow<Pair<String, String>> {
        return context.astroDataStore.data.map { prefs ->
            val total = prefs[TOTAL_NUMBERS_KEY] ?: "69"
            val choose = prefs[NUMBERS_TO_CHOOSE_KEY] ?: "5"
            Pair(total, choose)
        }
    }

}
