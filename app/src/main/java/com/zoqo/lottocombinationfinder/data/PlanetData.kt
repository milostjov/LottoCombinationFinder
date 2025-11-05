package com.zoqo.lottocombinationfinder.data

// data/PlanetData.kt
data class PlanetData(
    val id: Int,                          // SweConst ID
    val name: String,                     // “Sun”, “Moon”, …
    val symbol: String,                   // “☉”, “☽”, …
    val longitude: Double,
    val retrograde: Boolean,
    val latitude: Double? = null,
    val speedLonDegPerDay: Double? = null,
    val distanceAu: Double? = null,
    val house: Int? = null,
    val rightAscension: Double? = null,
    val declination: Double? = null,
    val altitude: Double? = null,
    val azimuth: Double? = null
)