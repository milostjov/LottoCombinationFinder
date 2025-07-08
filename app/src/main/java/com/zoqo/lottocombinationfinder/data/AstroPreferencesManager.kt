// AstroPreferencesManager.kt

package com.zoqo.lottocombinationfinder.data

import android.content.Context
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

    // dodatni astrološki parametri
    private val TIMEZONE_KEY = stringPreferencesKey("timezone_id")                    // npr. Europe/Belgrade
    private val ZODIAC_TYPE_KEY = stringPreferencesKey("zodiac_type")                  // tropical/sideral
    private val AYANAMSA_KEY = stringPreferencesKey("ayanamsa")                        // Lahiri itd.
    private val EPHEMERIS_TYPE_KEY = stringPreferencesKey("ephemeris_type")            // swisseph/jpl/moshier
    private val EXTRA_BODIES_KEY = stringPreferencesKey("extra_bodies")                // npr. "Ceres,Chiron,Eris"
    private val ORBS_KEY = doublePreferencesKey("orb_degree")                          // npr. 6.0
    private val DAY_NIGHT_KEY = stringPreferencesKey("day_night_mode")                 // diurnal/nocturnal

    // postojeći loto ključevi
    private val TOTAL_NUMBERS_KEY = stringPreferencesKey("total_numbers")
    private val NUMBERS_TO_CHOOSE_KEY = stringPreferencesKey("numbers_to_choose")
    private val RANK_INPUT_KEY = stringPreferencesKey("rank_input")

    suspend fun save(context: Context, data: AstroInputData) {
        context.astroDataStore.edit { prefs ->
            prefs[DATE_KEY] = data.date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            prefs[HOUR_KEY] = data.hour
            prefs[MINUTE_KEY] = data.minute
            prefs[LAT_KEY] = data.latitude
            prefs[LON_KEY] = data.longitude
            prefs[HOUSE_KEY] = data.houseSystem.name
            prefs[CITY_NAME_KEY] = data.cityName

            // dodatni parametri ako želiš da proširiš AstroInputData model:
            data.timeZoneId?.let { prefs[TIMEZONE_KEY] = it }
            data.zodiacType?.let { prefs[ZODIAC_TYPE_KEY] = it }
            data.ayanamsa?.let { prefs[AYANAMSA_KEY] = it }
            data.ephemerisType?.let { prefs[EPHEMERIS_TYPE_KEY] = it }
            data.extraBodies?.let { prefs[EXTRA_BODIES_KEY] = it.joinToString(",") }
            data.orb?.let { prefs[ORBS_KEY] = it }
            data.dayNightMode?.let { prefs[DAY_NIGHT_KEY] = it }
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

            // dodatna polja
            val timeZoneId = prefs[TIMEZONE_KEY]
            val zodiacType = prefs[ZODIAC_TYPE_KEY]
            val ayanamsa = prefs[AYANAMSA_KEY]
            val ephemerisType = prefs[EPHEMERIS_TYPE_KEY]
            val extraBodies = prefs[EXTRA_BODIES_KEY]?.split(",")?.filter { it.isNotBlank() }
            val orb = prefs[ORBS_KEY]
            val dayNightMode = prefs[DAY_NIGHT_KEY]

            AstroInputData(
                date = date,
                hour = hour,
                minute = minute,
                latitude = lat,
                longitude = lon,
                houseSystem = house,
                cityName = cityName,
                timeZoneId = timeZoneId,
                zodiacType = zodiacType,
                ayanamsa = ayanamsa,
                ephemerisType = ephemerisType,
                extraBodies = extraBodies,
                orb = orb,
                dayNightMode = dayNightMode
            )
        }
    }

    suspend fun saveLottoSettings(context: Context, total: String, choose: String, rank: String) {
        context.astroDataStore.edit { prefs ->
            prefs[TOTAL_NUMBERS_KEY] = total
            prefs[NUMBERS_TO_CHOOSE_KEY] = choose
            prefs[RANK_INPUT_KEY] = rank
        }
    }

    fun loadLottoSettings(context: Context): Flow<Triple<String, String, String>> {
        return context.astroDataStore.data.map { prefs ->
            val total = prefs[TOTAL_NUMBERS_KEY] ?: "39"
            val choose = prefs[NUMBERS_TO_CHOOSE_KEY] ?: "7"
            val rank = prefs[RANK_INPUT_KEY] ?: "123456"
            Triple(total, choose, rank)
        }
    }
}
