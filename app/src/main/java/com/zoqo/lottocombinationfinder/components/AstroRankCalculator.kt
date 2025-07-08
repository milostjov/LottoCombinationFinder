//AstroRankCalculator.kt
package com.zoqo.lottocombinationfinder.components


import com.zoqo.lottocombinationfinder.utils.calculateTotalCombinations
import swisseph.SwissEph
import swisseph.SweDate
import swisseph.SweConst
import java.math.BigInteger
import java.time.LocalDate

object AstroRankCalculator {

    private val swe = SwissEph()

    init {
        // Ako koristiš ephemeris fajlove iz assets, ovo postavi pravilno
        swe.swe_set_ephe_path(".")
    }

    fun calculateRankFromSunSign(
        birthDate: LocalDate,
        birthHour: Int,
        birthMinute: Int,
        totalNumbers: Int,
        numbersToChoose: Int
    ): BigInteger {
        val timeDecimal = toDecimalTime(birthHour, birthMinute)

        val date = SweDate()
        date.setDate(
            birthDate.year,
            birthDate.monthValue,
            birthDate.dayOfMonth,
            timeDecimal
        )
        val jd = date.julDay

        val position = DoubleArray(6)
        val errorMsg = StringBuffer()

        val iflag = SweConst.SEFLG_SWIEPH
        val planet = SweConst.SE_SUN

        val result = swe.swe_calc(jd, planet, iflag, position, errorMsg)

        if (result == SweConst.ERR) {
            println("Swiss Ephemeris error: $errorMsg")
            return BigInteger.ONE
        }

        val longitude = position[0]
        val normalizedPos = (longitude % 360.0) / 360.0

        val totalCombinations = calculateTotalCombinations(totalNumbers, numbersToChoose)
        val rankDecimal = normalizedPos * totalCombinations.toDouble()

        val rankBig = rankDecimal.toBigDecimal().toBigInteger().plus(BigInteger.ONE)

        return rankBig.coerceIn(BigInteger.ONE, totalCombinations)
    }
}


    private fun toDecimalTime(hour: Int, minute: Int): Double {
        return hour / 24.0 + minute / 1440.0
    }





