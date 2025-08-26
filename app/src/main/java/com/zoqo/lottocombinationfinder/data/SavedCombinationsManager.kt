// SavedCombinationsManager.kt
package com.zoqo.lottocombinationfinder.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate

private val Context.dataStore by preferencesDataStore(name = "saved_combinations")

object SavedCombinationsManager {

    private val KEY_SAVED_LIST = stringPreferencesKey("saved_combinations_list")

    data class SavedCombination(
        val combination: String,
        val date: LocalDate?,
        val hour: Int?,
        val minute: Int?,
        val totalNumbers: Int,
        val numbersToChoose: Int,
        val planetName: String,
        val gameName: String = ""          // NEW
    )

    // List -> JSON
    private fun List<SavedCombination>.toJson(): String {
        val jsonArray = JSONArray()
        for (it in this) {
            val obj = JSONObject()
            obj.put("combination", it.combination)
            obj.put("date", it.date?.toString())
            obj.put("hour", it.hour)
            obj.put("minute", it.minute)
            obj.put("totalNumbers", it.totalNumbers)
            obj.put("numbersToChoose", it.numbersToChoose)
            obj.put("planetName", it.planetName)
            obj.put("gameName", it.gameName)    // NEW
            jsonArray.put(obj)
        }
        return jsonArray.toString()
    }

    // JSON -> List (back-compat: gameName je opcionalan)
    private fun String.toSavedList(): MutableList<SavedCombination> {
        val list = mutableListOf<SavedCombination>()
        val jsonArray = JSONArray(this)
        for (i in 0 until jsonArray.length()) {
            val obj = jsonArray.getJSONObject(i)
            list.add(
                SavedCombination(
                    combination = obj.getString("combination"),
                    date = obj.optString("date", null)?.let { LocalDate.parse(it) },
                    hour = obj.optInt("hour"),
                    minute = obj.optInt("minute"),
                    totalNumbers = obj.optInt("totalNumbers"),
                    numbersToChoose = obj.optInt("numbersToChoose"),
                    planetName = obj.optString("planetName"),
                    gameName = obj.optString("gameName", "") // NEW
                )
            )
        }
        return list
    }

    /**
     * Sačuvaj novu kombinaciju (dodaje na vrh liste)
     */
    suspend fun saveCombination(
        context: Context,
        combination: String,      // očekujemo "Combination: a, b, c + x, y" ili bez bonusa
        date: LocalDate?,
        hour: Int?,
        minute: Int?,
        totalNumbers: Int,
        numbersToChoose: Int,
        planetName: String,
        gameName: String          // NEW
    ) {
        context.dataStore.edit { prefs ->
            val currentList = prefs[KEY_SAVED_LIST]?.toSavedList() ?: mutableListOf()

            // Normalizuj i sačuvaj ceo tiket (ne seckati po zarezima!)
            val raw = combination.removePrefix("Combination:").trim()
            val parts = raw.split("+").map { it.trim() }

            fun parseNums(s: String?): List<Int> =
                if (s.isNullOrBlank()) emptyList()
                else Regex("""\d+""").findAll(s).map { it.value.toInt() }.toList()

            val main = parseNums(parts.getOrNull(0))
            val bonus = parseNums(parts.getOrNull(1))

            val normalized = buildString {
                append("Combination: ")
                append(main.joinToString(", "))
                if (bonus.isNotEmpty()) {
                    append(" + ")
                    append(bonus.joinToString(", "))
                }
            }

            currentList.add(
                0,
                SavedCombination(
                    combination = normalized,
                    date = date,
                    hour = hour,
                    minute = minute,
                    totalNumbers = totalNumbers,
                    numbersToChoose = numbersToChoose,
                    planetName = planetName,
                    gameName = gameName          // NEW
                )
            )

            prefs[KEY_SAVED_LIST] = currentList.take(50).toJson()
        }
    }

    fun getSavedCombinations(context: Context): Flow<List<SavedCombination>> =
        context.dataStore.data.map { prefs ->
            prefs[KEY_SAVED_LIST]?.toSavedList() ?: emptyList()
        }

    suspend fun clearAll(context: Context) {
        context.dataStore.edit { prefs -> prefs.remove(KEY_SAVED_LIST) }
    }

    suspend fun deleteCombination(context: Context, combination: String) {
        context.dataStore.edit { prefs ->
            val currentList = prefs[KEY_SAVED_LIST]?.toSavedList() ?: mutableListOf()
            val newList = currentList.filterNot { it.combination == combination }
            prefs[KEY_SAVED_LIST] = newList.toJson()
        }
    }
}
