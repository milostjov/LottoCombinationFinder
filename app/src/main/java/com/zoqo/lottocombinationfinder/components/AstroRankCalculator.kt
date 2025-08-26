//AstroRankCalculator.kt
package com.zoqo.lottocombinationfinder.components

import swisseph.SweConst
import swisseph.SweDate
import swisseph.SwissEph
import java.math.BigDecimal
import java.math.BigInteger
import java.math.MathContext
import java.math.RoundingMode
import java.time.LocalDate

object AstroRankCalculator {

    private val swe = SwissEph()

    init {
        swe.swe_set_ephe_path(".")
    }

    private val planetMap = mapOf(
        "Sun" to SweConst.SE_SUN,
        "Moon" to SweConst.SE_MOON,
        "Mercury" to SweConst.SE_MERCURY,
        "Venus" to SweConst.SE_VENUS,
        "Mars" to SweConst.SE_MARS,
        "Jupiter" to SweConst.SE_JUPITER,
        "Saturn" to SweConst.SE_SATURN,
        "Uranus" to SweConst.SE_URANUS,
        "Neptune" to SweConst.SE_NEPTUNE,
        "Pluto" to SweConst.SE_PLUTO
    )

    // Max i min udaljenosti planeta od Zemlje (u AU), aproksimacije
    private val planetDistanceRange = mapOf(
        "Sun" to (0.983 to 1.017),
        "Moon" to (0.0024 to 0.0027),
        "Mercury" to (0.3 to 1.4),
        "Venus" to (0.26 to 1.73),
        "Mars" to (0.37 to 2.68),
        "Jupiter" to (4.2 to 6.2),
        "Saturn" to (8.0 to 11.0),
        "Uranus" to (18.2 to 20.2),
        "Neptune" to (28.8 to 30.4),
        "Pluto" to (29.7 to 49.3)
    )



    fun calculateRankFromPlanetDistance(
        date: LocalDate,
        hour: Int,
        minute: Int,
        totalNumbers: Int,
        numbersToChoose: Int,
        planetName: String,
        ticketIndex: Int = 0,     // NEW: indeks tiketa (0,1,2…)
        extraSalt: Long = 0L      // NEW: opciono, ako želiš dodatni seed
    ): BigInteger {
        val timeDecimal = toDecimalTime(hour, minute)

        val jd = SweDate().apply {
            setDate(date.year, date.monthValue, date.dayOfMonth, timeDecimal)
        }.julDay



        val planetId = planetMap[planetName] ?: SweConst.SE_SUN
        val iflag = SweConst.SEFLG_SWIEPH or SweConst.SEFLG_TRUEPOS

        val position = DoubleArray(6)
        val errorMsg = StringBuffer()

        val result = swe.swe_calc(jd, planetId, iflag, position, errorMsg)
        if (result == SweConst.ERR) {
            println("Swiss Ephemeris error: $errorMsg")
            return BigInteger.ONE
        }

        val distanceAU = BigDecimal(position[2], MathContext.DECIMAL64)
        val (minDist, maxDist) = planetDistanceRange[planetName] ?: (1.0 to 1.0)

        val min = BigDecimal(minDist, MathContext.DECIMAL64)
        val max = BigDecimal(maxDist, MathContext.DECIMAL64)
        val range = max.subtract(min)
        val normalized = distanceAU.subtract(min)
            .divide(range, 16, RoundingMode.HALF_UP)
            .coerceIn(BigDecimal.ZERO, BigDecimal.ONE)

        val normalizedClamped = if (numbersToChoose > 1) {
            normalized.coerceIn(BigDecimal("0.05"), BigDecimal("0.95"))
        } else {
            normalized // bez ograničenja ako se bira samo 2 broja
        }


        val totalCombinations = calculateTotalCombinations(totalNumbers, numbersToChoose)
        val totalBigDecimal = BigDecimal(totalCombinations)

        val rankDecimal = normalizedClamped.multiply(totalBigDecimal)

        // Dodaj pseudo-random varijaciju radi UX efekta
        val timeInMinutes = date.toEpochDay() * 1440 + hour * 60 + minute

// Za svaku planetu, definiši "trajanje ciklusa" (u minutima) — veće za sporije planete
        val pseudoCycleMinutes = mapOf(
            "Sun" to 1445,         // 1 dan - 1440
            "Moon" to 60,          // referenca (27.3 dana)
            "Mercury" to 1940,     // 88 dana
            "Venus" to 4960,       // 225 dana
            "Mars" to 12300,       // 687 dana
            "Jupiter" to 67000,    // 11.9 godina
            "Saturn" to 166000,    // 29.5 godina
            "Uranus" to 471000,    // 84 godina
            "Neptune" to 925000,   // 165 godina
            "Pluto" to 1390000     // 248 godina
        )


        val cycle = pseudoCycleMinutes[planetName] ?: 14400
        val variationAmplitude = totalBigDecimal.multiply(BigDecimal("0.005")) // 0.5% odstupanja

        // NEW: fazni pomak po tiketu (90° korak), plus opciona so.
        val phaseByTicket = ticketIndex * (Math.PI / 2) // 0, 90°, 180°, 270°...
        val phaseBySalt = ((extraSalt % 360 + 360) % 360) * (Math.PI / 180.0)

        val angle = 2 * Math.PI * (timeInMinutes % cycle) / cycle
        val variation = Math.sin(angle).toBigDecimal().multiply(variationAmplitude)

        val rankWithVariation = rankDecimal.add(variation).setScale(0, RoundingMode.HALF_UP)
        val rankFinal = rankWithVariation.toBigInteger().plus(BigInteger.ONE).coerceIn(BigInteger.ONE, totalCombinations)

        return rankFinal

    }

    private fun toDecimalTime(hour: Int, minute: Int): Double {
        return hour / 24.0 + minute / 1440.0
    }

    private fun BigDecimal.coerceIn(min: BigDecimal, max: BigDecimal): BigDecimal {
        return when {
            this < min -> min
            this > max -> max
            else -> this
        }
    }
}
