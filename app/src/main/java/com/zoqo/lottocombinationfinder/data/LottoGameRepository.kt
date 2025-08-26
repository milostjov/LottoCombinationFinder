// LottoGameRepository.kt
package com.zoqo.lottocombinationfinder.data

import android.util.Log
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.remoteConfigSettings
import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.google.gson.reflect.TypeToken

enum class GameType { CLASSIC, BONUS, MULTIDRAW, CARD }

data class CardStructure(
    val rows: Int,
    val cols: Int,
    val numbersPerCard: Int,
    val numberPool: Int
)

/** Novi, generalizovani model igre (sa klasom igre). */
data class LottoGame(
    val name: String = "",
    val country: String = "",
    val classType: GameType = GameType.CLASSIC,

    // glavni skup
    val mainNumbers: Int = 0,   // koliko se bira
    val mainPool: Int = 0,      // raspon (n)

    // bonus skup (opciono)
    val bonusNumbers: Int = 0,
    val bonusPool: Int = 0,

    // za igre tipa "Laki 6" (više izvučenih nego što igrač bira)
    val draws: Int = 0,

    // za tombolu/bingo kartice
    val cardStructure: CardStructure? = null
)

/** Ostaje isto. */
data class CountryDisplay(
    val name: Map<String, String> = emptyMap(),
    val flagUrl: String? = null
)

class LottoGameRepository {

    private val rc by lazy { FirebaseRemoteConfig.getInstance() }
    private val gson = Gson()

    init {
        val cfg = remoteConfigSettings {
            minimumFetchIntervalInSeconds = 259_200 // ~3 dana
        }
        rc.setConfigSettingsAsync(cfg)
        rc.setDefaultsAsync(
            mapOf(
                "lotto_games" to "[]",
                "lotto_countries" to "{}"
            )
        )
    }

    fun fetchAll(onResult: (List<LottoGame>, Map<String, CountryDisplay>) -> Unit) {
        rc.fetchAndActivate().addOnCompleteListener {
            val games = safeParseGames(rc.getString("lotto_games"))
            val countries = safeParseCountries(rc.getString("lotto_countries"))
            onResult(games, countries)
        }
    }

//    /**
//     * Parser koji je:
//     * 1) Primarno usklađen sa novim JSON formatom (sa classType, main bonus*/draws/cardStructure).
//     2) Unazad kompatibilan sa starim formatom (totalNumbers + numbersToChoose).
//    **/
    private fun safeParseGames(json: String): List<LottoGame> {
    return try {
            if (json.isBlank()) return emptyList()

            val root = JsonParser.parseString(json)
            if (!root.isJsonArray) {
                Log.e("RC", "lotto_games not an array: $json")
                return emptyList()
            }

            val arr = root.asJsonArray
            val out = mutableListOf<LottoGame>()

            for (el: JsonElement in arr) {
                if (!el.isJsonObject) continue
                val obj = el.asJsonObject

                // Ako vec ima classType → direktno deserijalizuj u novi model.
                val hasClassType = obj.has("classType")

                // Ako ima cardStructure → CARD
                val hasCardStructure = obj.has("cardStructure") && obj["cardStructure"].isJsonObject

                // Ako je legacy (nema classType), probaj da mapiraš.
                val isLegacy = !hasClassType && (obj.has("totalNumbers") || obj.has("numbersToChoose"))

                val game: LottoGame = when {
                    hasClassType || hasCardStructure -> {
                        // Novi format
                        gson.fromJson(obj, LottoGame::class.java)
                    }
                    isLegacy -> {
                        // Legacy → mapiraj na novi model
                        legacyToNew(obj)
                    }
                    else -> {
                        // Ako je "polu-novi" (ima npr. bonusNumbers bez classType) — odredi classType heuristikom.
                        heuristicToNew(obj)
                    }
                }

                out.add(game)
            }

            out
        } catch (t: Throwable) {
            Log.e("RC", "Parse lotto_games failed: $json", t)
            emptyList()
        }
    }

    /**
     * Mapiranje starog formata:
     * { name, country, totalNumbers, numbersToChoose }
     * → CLASSIC sa mainPool = totalNumbers, mainNumbers = numbersToChoose
     */
    private fun legacyToNew(obj: JsonObject): LottoGame {
        val name = obj.optString("name")
        val country = obj.optString("country")
        val total = obj.optInt("totalNumbers")
        val choose = obj.optInt("numbersToChoose")

        // Ako slučajno postoje i bonus polja u starom JSON-u, respektuj ih.
        val bonusNumbers = obj.optInt("bonusNumbers")
        val bonusPool = obj.optInt("bonusPool")
        val draws = obj.optInt("draws")

        val inferredType = when {
            draws > 0 -> GameType.MULTIDRAW
            bonusNumbers > 0 && bonusPool > 0 -> GameType.BONUS
            else -> GameType.CLASSIC
        }

        return LottoGame(
            name = name,
            country = country,
            classType = inferredType,
            mainNumbers = choose,
            mainPool = total,
            bonusNumbers = bonusNumbers,
            bonusPool = bonusPool,
            draws = draws,
            cardStructure = null
        )
    }

    /**
     * Ako JSON nema classType ali sadrži polja iz novog modela,
     * pokušaj da ih pretvoriš u LottoGame i odredi classType heuristički.
     */
    private fun heuristicToNew(obj: JsonObject): LottoGame {
        val name = obj.optString("name")
        val country = obj.optString("country")

        val mainNumbers = obj.optInt("mainNumbers", fallback = obj.optInt("numbersToChoose"))
        val mainPool = obj.optInt("mainPool", fallback = obj.optInt("totalNumbers"))
        val bonusNumbers = obj.optInt("bonusNumbers")
        val bonusPool = obj.optInt("bonusPool")
        val draws = obj.optInt("draws")

        val inferredType = when {
            obj.has("cardStructure") -> GameType.CARD
            draws > 0 -> GameType.MULTIDRAW
            bonusNumbers > 0 && bonusPool > 0 -> GameType.BONUS
            else -> GameType.CLASSIC
        }

        val card = if (obj.has("cardStructure") && obj["cardStructure"].isJsonObject) {
            gson.fromJson(obj["cardStructure"], CardStructure::class.java)
        } else null

        return LottoGame(
            name = name,
            country = country,
            classType = inferredType,
            mainNumbers = mainNumbers,
            mainPool = mainPool,
            bonusNumbers = bonusNumbers,
            bonusPool = bonusPool,
            draws = draws,
            cardStructure = card
        )
    }

    private fun safeParseCountries(json: String): Map<String, CountryDisplay> {
        return try {
            val mapType = object : TypeToken<Map<String, CountryDisplay>>() {}.type
            gson.fromJson<Map<String, CountryDisplay>>(json, mapType) ?: emptyMap()
        } catch (t: Throwable) {
            Log.w("RC", "lotto_countries not a Map, trying List<Map<...>>; json=$json")
            try {
                val listType = object : TypeToken<List<Map<String, CountryDisplay>>>() {}.type
                val outer = gson.fromJson<List<Map<String, CountryDisplay>>>(json, listType)
                outer.firstOrNull() ?: emptyMap()
            } catch (t2: Throwable) {
                Log.e("RC", "Parse lotto_countries failed", t2)
                emptyMap()
            }
        }
    }
}

/** ——— Helpers ——— */

private fun JsonObject.optString(key: String, fallback: String = ""): String =
    if (this.has(key) && this[key].isJsonPrimitive) this[key].asString ?: fallback else fallback

private fun JsonObject.optInt(key: String, fallback: Int = 0): Int =
    if (this.has(key) && this[key].isJsonPrimitive) this[key].asInt else fallback

/**
 * Omogućava fallback: optInt("mainNumbers", fallback = optInt("numbersToChoose"))
 */

