//SavedCombinationsManager.kt
package com.zoqo.lottocombinationfinder.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
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
        val planetName: String
    )

    // 🔹 Pretvori listu u JSON string za čuvanje
    private fun List<SavedCombination>.toJson(): String {
        val jsonArray = JSONArray()
        this.forEach {
            val obj = JSONObject()
            obj.put("combination", it.combination)
            obj.put("date", it.date?.toString())
            obj.put("hour", it.hour)
            obj.put("minute", it.minute)
            obj.put("totalNumbers", it.totalNumbers)
            obj.put("numbersToChoose", it.numbersToChoose)
            obj.put("planetName", it.planetName)
            jsonArray.put(obj)
        }
        return jsonArray.toString()
    }

    // 🔹 Pretvori JSON nazad u listu
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
                    planetName = obj.optString("planetName")
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
        combination: String,
        date: LocalDate?,
        hour: Int?,
        minute: Int?,
        totalNumbers: Int,
        numbersToChoose: Int,
        planetName: String
    ) {
        context.dataStore.edit { prefs ->
            val currentList = prefs[KEY_SAVED_LIST]?.toSavedList() ?: mutableListOf()
            val cleanedCombination = combination
                .removePrefix("Combination:")
                .split(",")
                .mapNotNull { it.trim().toIntOrNull() }
                .joinToString(",")
            // dodaj novu na početak liste
            currentList.add(
                0,
                SavedCombination(
                    cleanedCombination, date, hour, minute,
                    totalNumbers, numbersToChoose, planetName
                )
            )

            // ograniči na poslednjih 50
            val trimmed = currentList.take(50)
            prefs[KEY_SAVED_LIST] = trimmed.toJson()
        }
    }

    /**
     * Vrati flow svih sačuvanih kombinacija
     */
    fun getSavedCombinations(context: Context): Flow<List<SavedCombination>> {
        return context.dataStore.data.map { prefs ->
            prefs[KEY_SAVED_LIST]?.toSavedList() ?: emptyList()
        }
    }

    /**
     * Obrisi sve sacuvane kombinacije
     */
    suspend fun clearAll(context: Context) {
        context.dataStore.edit { prefs ->
            prefs.remove(KEY_SAVED_LIST)
        }
    }

    suspend fun deleteCombination(context: Context, combination: String) {
        context.dataStore.edit { prefs ->
            val currentList = prefs[KEY_SAVED_LIST]?.toSavedList() ?: mutableListOf()
            val newList = currentList.filterNot { it.combination == combination }
            prefs[KEY_SAVED_LIST] = newList.toJson()
        }
    }

}
