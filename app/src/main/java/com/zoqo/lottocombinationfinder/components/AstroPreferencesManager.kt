package com.zoqo.lottocombinationfinder.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.zoqo.lottocombinationfinder.ui.AstroHouseSystem
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
    private val LAT_KEY = doublePreferencesKey("latitude")
    private val LON_KEY = doublePreferencesKey("longitude")
    private val HOUSE_KEY = stringPreferencesKey("house_system")
    private val CITY_NAME_KEY = stringPreferencesKey("city_name")

    suspend fun save(context: Context, data: AstroInputData) {
        context.astroDataStore.edit { prefs ->
            prefs[DATE_KEY] = data.date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            prefs[HOUR_KEY] = data.hour
            prefs[MINUTE_KEY] = data.minute
            prefs[LAT_KEY] = data.latitude
            prefs[LON_KEY] = data.longitude
            prefs[HOUSE_KEY] = data.houseSystem.name
            prefs[CITY_NAME_KEY] = data.cityName

        }
    }

    fun load(context: Context): Flow<AstroInputData> {
        return context.astroDataStore.data.map { prefs ->
            val date = prefs[DATE_KEY]?.let {
                Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate()
            } ?: LocalDate.now()

            val hour = prefs[HOUR_KEY] ?: 12
            val minute = prefs[MINUTE_KEY] ?: 0
            val lat = prefs[LAT_KEY] ?: 44.0
            val lon = prefs[LON_KEY] ?: 20.0
            val cityName = prefs[CITY_NAME_KEY] ?: ""

            val house = prefs[HOUSE_KEY]?.let {
                AstroHouseSystem.valueOf(it)
            } ?: AstroHouseSystem.PLACIDUS

            AstroInputData(date, hour, minute, lat, lon, house, cityName)
        }
    }
}
