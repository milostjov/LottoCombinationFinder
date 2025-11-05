package com.zoqo.lottocombinationfinder.astro

import android.content.Context
import android.util.Log
import com.zoqo.lottocombinationfinder.R
import com.zoqo.lottocombinationfinder.data.PlanetData
import swisseph.SweConst

data class RetrogradeAdvice(
    val symbol: String,
    val isRetrograde: Boolean,
    val warning: String?,          // npr. "Mercury is retrograde"
    val introspectionHint: String? // npr. "Review communications..."
)

/** Učitavanje tema i hintova iz strings.xml */
private fun getPlanetThemes(context: Context): Map<Int, Pair<String, String>> = mapOf(
    SweConst.SE_MERCURY to (
            context.getString(R.string.planet_mercury_theme) to
                    context.getString(R.string.planet_mercury_hint)
            ),
    SweConst.SE_VENUS to (
            context.getString(R.string.planet_venus_theme) to
                    context.getString(R.string.planet_venus_hint)
            ),
    SweConst.SE_MARS to (
            context.getString(R.string.planet_mars_theme) to
                    context.getString(R.string.planet_mars_hint)
            ),
    SweConst.SE_JUPITER to (
            context.getString(R.string.planet_jupiter_theme) to
                    context.getString(R.string.planet_jupiter_hint)
            ),
    SweConst.SE_SATURN to (
            context.getString(R.string.planet_saturn_theme) to
                    context.getString(R.string.planet_saturn_hint)
            ),
    SweConst.SE_URANUS to (
            context.getString(R.string.planet_uranus_theme) to
                    context.getString(R.string.planet_uranus_hint)
            ),
    SweConst.SE_NEPTUNE to (
            context.getString(R.string.planet_neptune_theme) to
                    context.getString(R.string.planet_neptune_hint)
            ),
    SweConst.SE_PLUTO to (
            context.getString(R.string.planet_pluto_theme) to
                    context.getString(R.string.planet_pluto_hint)
            ),
    SweConst.SE_SUN to (
            context.getString(R.string.planet_sun_theme) to
                    context.getString(R.string.planet_sun_hint)
            ),
    SweConst.SE_MOON to (
            context.getString(R.string.planet_moon_theme) to
                    context.getString(R.string.planet_moon_hint)
            )
)

/** Heuristika: izvodi SE_* ID iz simbola (čistije je dodati planetId u PlanetData). */
private fun planetIdFromSymbol(symbol: String): Int = when (symbol) {
    "☉" -> SweConst.SE_SUN
    "☽" -> SweConst.SE_MOON
    "☿" -> SweConst.SE_MERCURY
    "♀" -> SweConst.SE_VENUS
    "♂" -> SweConst.SE_MARS
    "♃" -> SweConst.SE_JUPITER
    "♄" -> SweConst.SE_SATURN
    "♅" -> SweConst.SE_URANUS
    "♆" -> SweConst.SE_NEPTUNE
    "♇" -> SweConst.SE_PLUTO
    else -> SweConst.SE_SUN
}

object AstroInterpreter {

    /**
     * Ulaz: lista PlanetData iz kalkulatora.
     * Izlaz: lista saveta sa upozorenjem i “introspekcija” hintom iz strings.xml.
     */
    fun buildRetrogradeAdvisories(
        context: Context,
        planets: List<PlanetData>
    ): List<RetrogradeAdvice> {
        val themes = getPlanetThemes(context)
        return planets.map { p ->
            val pid = planetIdFromSymbol(p.symbol)
            val theme = themes[pid]
            if (p.retrograde) {
                Log.d("Retrograde", "Symbol: ${p.symbol}")
                val test = context.getString(R.string.retro_warning, p.symbol)
                Log.d("Retrograde", "Warning string: $test")

                RetrogradeAdvice(
                    symbol = p.symbol,
                    isRetrograde = true,
                    warning = context.getString(R.string.retro_warning, p.symbol),
                    introspectionHint = theme?.second
                        ?: context.getString(R.string.retro_generic_hint)
                )

            } else {
                RetrogradeAdvice(
                    symbol = p.symbol,
                    isRetrograde = false,
                    warning = null,
                    introspectionHint = null
                )
            }
        }
    }

    /** Kratki badge tekst za UI (iz strings.xml). */
    fun shortBadgeText(context: Context, advice: RetrogradeAdvice): String? =
        if (advice.isRetrograde) context.getString(R.string.retro_badge) else null

    /** Duži tooltip (tema + hint), sve iz strings.xml. */
    fun longTooltip(context: Context, advice: RetrogradeAdvice): String? {
        if (!advice.isRetrograde) return null
        val pid = planetIdFromSymbol(advice.symbol)
        val themes = getPlanetThemes(context)
        val (area, hint) = themes[pid]
            ?: (context.getString(R.string.retro_theme_default) to
                    (advice.introspectionHint ?: context.getString(R.string.retro_fallback_hint)))
        return context.getString(R.string.retro_theme_prefix, area, hint)
    }
}
