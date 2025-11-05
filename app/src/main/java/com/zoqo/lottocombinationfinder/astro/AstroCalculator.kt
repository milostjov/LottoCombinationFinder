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
        SweConst.SE_PLUTO to "♇",
        // moderni dodaci
        SweConst.SE_MEAN_NODE to "☊", // Severni čvor (mean)
        SweConst.SE_MEAN_APOG to "⚸", // Lilith (Black Moon) - mean apogee
        SweConst.SE_CHIRON    to "⚷"  // Chiron
    )
    private val planetNames = mapOf(
        SweConst.SE_SUN     to "Sun",
        SweConst.SE_MOON    to "Moon",
        SweConst.SE_MERCURY to "Mercury",
        SweConst.SE_VENUS   to "Venus",
        SweConst.SE_MARS    to "Mars",
        SweConst.SE_JUPITER to "Jupiter",
        SweConst.SE_SATURN  to "Saturn",
        SweConst.SE_URANUS  to "Uranus",
        SweConst.SE_NEPTUNE to "Neptune",
        SweConst.SE_PLUTO   to "Pluto",
        SweConst.SE_MEAN_NODE to "North Node",
        SweConst.SE_MEAN_APOG to "Lilith (Black Moon)",
        SweConst.SE_CHIRON    to "Chiron"
    )


    fun getPlanetPositions(data: AstroInputData): List<PlanetData> {
        val zone = ZoneId.systemDefault()
        val localDateTime = data.date.atTime(data.hour, data.minute)
        val utc = localDateTime.atZone(zone).withZoneSameInstant(ZoneId.of("UTC"))
        val decimalTime = utc.hour / 24.0 + utc.minute / 1440.0

        val date = SweDate().apply { setDate(utc.year, utc.monthValue, utc.dayOfMonth, decimalTime) }
        val jd = date.julDay

        // SWIEPH + SPEED → dobijamo i brzine (xx[3..5])
        val flags = SweConst.SEFLG_SWIEPH or SweConst.SEFLG_SPEED

        val result = mutableListOf<PlanetData>()

        var northNodeLon: Double? = null
        var northNodeSpeed: Double? = null

        for ((planetId, symbol) in planetSymbols) {
            val xx = DoubleArray(6) // 0:lon,1:lat,2:dist,3..5: speed
            val serr = StringBuffer()

            val ret = swe.swe_calc(jd, planetId, flags, xx, serr)
            if (ret == SweConst.ERR) {
                println("Swiss Ephemeris error: $serr")
                continue
            }

            val lon = normalize360(xx[0])
            val lat = xx[1]
            val distAu = xx[2]
            val speedLon = xx[3]
            val isRetro = speedLon < 0

            val (raHours, decDeg) = computeEquatorial(jd, planetId) ?: (null to null)
            // zapamti severni čvor za kasnije (☋)
            if (planetId == SweConst.SE_MEAN_NODE) {
                northNodeLon = lon
                northNodeSpeed = speedLon
            }

            result.add(
                PlanetData(
                    id = planetId,
                    name = planetNames[planetId] ?: "Body $planetId",
                    symbol = symbol,
                    longitude = lon,
                    retrograde = isRetro,
                    latitude = lat,
                    speedLonDegPerDay = speedLon,
                    distanceAu = distAu,
                    rightAscension = raHours,   // u satima (npr. 14.123 h)
                    declination   = decDeg      // u stepenima (npr. -12.34°)
                )
            )
        }

        // Dodaj JUŽNI čvor (☋) kao 180° suprotnu tačku severnog
        if (northNodeLon != null && northNodeSpeed != null) {
            val southLon = normalize360(northNodeLon!! + 180.0)
            val southSpeed = northNodeSpeed!! // ista brzina, suprotan položaj
            val southRetro = southSpeed < 0

            // koristimo "fiktivni" ID koji se neće sudariti sa SweConst
            val SOUTH_NODE_ID = -1001

            result.add(
                PlanetData(
                    id = SOUTH_NODE_ID,
                    name = "South Node",
                    symbol = "☋",
                    longitude = southLon,
                    retrograde = southRetro,
                    latitude = 0.0,               // standardno se uzima 0 za prikaz
                    speedLonDegPerDay = southSpeed,
                    distanceAu = null             // nije relevantno
                )
            )
        }

        return result
    }

    private fun normalize360(d: Double): Double {
        var x = d % 360.0
        if (x < 0) x += 360.0
        return if (kotlin.math.abs(x) < 1e-12) 0.0 else x
    }


    // Geocentrične ekvatorijalne koordinate (RA u satima, Dec u stepenima)
    private fun computeEquatorial(
        jd: Double,
        planetId: Int
    ): Pair<Double, Double>? {
        val xx = DoubleArray(6)
        val serr = StringBuffer()

        val flags = SweConst.SEFLG_SWIEPH or SweConst.SEFLG_SPEED or SweConst.SEFLG_EQUATORIAL
        val ret = swe.swe_calc(jd, planetId, flags, xx, serr)
        if (ret == SweConst.ERR) {
            println("Swiss Ephemeris (equatorial) error: $serr")
            return null
        }
        val raHours = xx[0]   // RA u satima
        val decDeg  = xx[1]   // Dec u stepenima
        return raHours to decDeg
    }

}
