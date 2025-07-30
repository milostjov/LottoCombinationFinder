package com.zoqo.lottocombinationfinder.astro

import com.zoqo.lottocombinationfinder.ui.AstroInputData
import swisseph.SwissEph
import swisseph.SweConst
import swisseph.SweDate
import java.time.ZoneId
import com.zoqo.lottocombinationfinder.data.PlanetData



object AstroCalculator {

    private val swe = SwissEph()

    init {
        // postavi putanju do ephemeris fajlova ako ih koristiš
        swe.swe_set_ephe_path(".")
    }

    private val planetSymbols = mapOf(
        SweConst.SE_SUN to "☉",
        SweConst.SE_MOON to "☽",
        SweConst.SE_MERCURY to "☿",
        SweConst.SE_VENUS to "♀",
        SweConst.SE_MARS to "♂",
        SweConst.SE_JUPITER to "♃",
        SweConst.SE_SATURN to "♄",
        SweConst.SE_URANUS to "♅",
        SweConst.SE_NEPTUNE to "♆",
        SweConst.SE_PLUTO to "♇"
    )

    fun getPlanetPositions(data: AstroInputData): List<PlanetData> {

        val zone = ZoneId.systemDefault()
        val localDateTime = data.date.atTime(data.hour, data.minute)
        val utc = localDateTime.atZone(zone).withZoneSameInstant(ZoneId.of("UTC"))

        val decimalTime = utc.hour / 24.0 + utc.minute / 1440.0

        val date = SweDate()
        date.setDate(
            utc.year,
            utc.monthValue,
            utc.dayOfMonth,
            decimalTime
        )

        val jd = date.julDay
        val flags = SweConst.SEFLG_SWIEPH or SweConst.SEFLG_SPEED

        val result = mutableListOf<PlanetData>()

        for ((planetId, symbol) in planetSymbols) {
            val xx = DoubleArray(6)
            val serr = StringBuffer()

            val ret = swe.swe_calc(jd, planetId, flags, xx, serr)
            if (ret == SweConst.ERR) {
                println("Swiss Ephemeris error: $serr")
                continue
            }

            val longitude = xx[0] % 360.0
            val speed = xx[3] // < 0 znači retrogradno kretanje

            result.add(
                PlanetData(
                    symbol = symbol,
                    longitude = longitude,
                    retrograde = speed < 0
                )
            )

        }

        return result
    }
}
