package com.zoqo.lottocombinationfinder.chart

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import com.zoqo.lottocombinationfinder.R
import com.zoqo.lottocombinationfinder.data.PlanetData
import com.zoqo.lottocombinationfinder.ui.PlanetInfoDialog
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape
import kotlin.math.max


private val zodiac = listOf("♈","♉","♊","♋","♌","♍","♎","♏","♐","♑","♒","♓")
private const val TEXT_SCALE = 1.50f
val manualYOffset = -10f * TEXT_SCALE
private const val MIN_SCALE = 0.5f
private const val MAX_SCALE = 4f
private const val SCALE_STEP = 0.2f


// NEW: pomoćna struktura za postavljanje
private data class PlacedPlanet(
    val planet: PlanetData,
    val pos: Offset,
    val ring: Int,           // 0,1,2… radi male radijalne “prstenove”
    val angleDeg: Double     // prilagođen ugao (za aspekte/linije)
)

// NEW: minimalno rastojanje po dužini da uopšte smatramo preklapanjem
private const val MIN_SEP_DEG = 4.5

// NEW: raspoređivanje u klastere i vraćanje prilagođenih pozicija
private fun layoutPlanets(
    planets: List<PlanetData>,
    center: Offset,
    baseRadius: Float,          // R - 78 iz tvog koda
    zodiacRotation: Float
): List<PlacedPlanet> {
    if (planets.isEmpty()) return emptyList()

    // 1) Sortiraj po dužini [0..360)
    val sorted = planets.sortedBy { ((it.longitude % 360.0) + 360.0) % 360.0 }

    // 2) Napravi klastere gde su susedi bliži od MIN_SEP_DEG (cikličan krug)
    val clusters = mutableListOf<MutableList<PlanetData>>()
    var current = mutableListOf<PlanetData>()
    for (i in sorted.indices) {
        if (i == 0) {
            current.add(sorted[i])
        } else {
            val prev = sorted[i - 1].longitude
            val cur = sorted[i].longitude
            val diff = angularDiff(prev, cur)
            if (diff <= MIN_SEP_DEG) {
                current.add(sorted[i])
            } else {
                clusters.add(current)
                current = mutableListOf(sorted[i])
            }
        }
    }
    clusters.add(current)
    // spoj prvog i poslednjeg ako su blizu (zbog 360 wrap-a)
    if (clusters.isNotEmpty()) {
        val firstCluster = clusters.first()
        val lastCluster = clusters.last()
        if (firstCluster.isNotEmpty() && lastCluster.isNotEmpty()) {
            val firstAngle = firstCluster.first().longitude
            val lastAngle = lastCluster.last().longitude
            if (angularDiff(lastAngle, firstAngle) <= MIN_SEP_DEG) {
                // spoji u jedan kružni klaster
                val merged = (lastCluster + firstCluster).toMutableList()
                clusters.removeAt(clusters.lastIndex)
                clusters[0] = merged
            }
        }
    } else {
        // ako je bio samo jedan current
        clusters.add(current)
    }

    // Spoji prvi i poslednji klaster ako su blizu (wrap oko 360°)
    if (clusters.size > 1) {
        val firstCluster = clusters.first()
        val lastCluster = clusters.last()
        val firstAngle = firstCluster.first().longitude
        val lastAngle = lastCluster.last().longitude
        if (angularDiff(lastAngle, firstAngle) <= MIN_SEP_DEG) {
            val merged = (lastCluster + firstCluster).toMutableList()
            clusters.removeAt(clusters.lastIndex)
            clusters[0] = merged
        }
    }

    // 3) Za svaki klaster rasporedi planete simetrično oko srednje dužine
    val placed = mutableListOf<PlacedPlanet>()
    val angleSpreadPerItem = 2.0  // u stepenima: razmak po simbolu levo/desno
    val radialStep = 24f          // px: prsten pomaže da krugovi ne seku tekst
    clusters.forEach { cluster ->
        val n = cluster.size
        if (n == 1) {
            // bez pomeranja
            val p = cluster[0]
            val adjAngle = normalizeDeg(p.longitude) // bez offseta
            val pos = toPos(center, baseRadius, adjAngle - 90 + zodiacRotation)
            placed += PlacedPlanet(p, pos, ring = 0, angleDeg = adjAngle)
        } else {
            // srednji ugao (po proseku, uz wrap-aware prosečno računanje)
            val mean = circularMean(cluster.map { normalizeDeg(it.longitude) })
            // raspored: indeksi 0..n-1 oko sredine -> [-m..+m]
            val mid = (n - 1) / 2.0
            cluster.forEachIndexed { idx, p ->
                val k = idx - mid // npr. za 3: [-1, 0, +1], za 4: [-1.5,-0.5,0.5,1.5]
                val angleOffset = k * angleSpreadPerItem
                val ring = if (idx % 2 == 0) 0 else 1 // 0,1,0,1…
                val adjAngle = normalizeDeg(mean + angleOffset)
                val pos = toPos(center, baseRadius + ring * radialStep, adjAngle - 90 + zodiacRotation)
                placed += PlacedPlanet(p, pos, ring = ring, angleDeg = adjAngle)
            }
        }
    }

    return placed
}

// NEW: pomoćne matematičke funkcije
private fun normalizeDeg(d: Double): Double {
    var x = d % 360.0
    if (x < 0) x += 360.0
    return x
}
private fun angularDiff(a: Double, b: Double): Double {
    val d = abs(normalizeDeg(a) - normalizeDeg(b))
    return min(d, 360.0 - d)
}
private fun circularMean(anglesDeg: List<Double>): Double {
    val anglesRad = anglesDeg.map { Math.toRadians(it) }
    val x = anglesRad.sumOf { kotlin.math.cos(it) }
    val y = anglesRad.sumOf { kotlin.math.sin(it) }
    return normalizeDeg(Math.toDegrees(kotlin.math.atan2(y, x)))
}
private fun toPos(center: Offset, radius: Float, angleDegFromRightMinus90PlusRot: Double): Offset {
    val a = Math.toRadians(angleDegFromRightMinus90PlusRot)
    return Offset(
        x = center.x + radius * cos(a).toFloat(),
        y = center.y + radius * sin(a).toFloat()
    )
}

@Composable
fun NatalChartView(
    planets: List<PlanetData>
) {
    var scale by remember { mutableStateOf(1f) }
    var translation by remember { mutableStateOf(Offset.Zero) }
    var selected by remember { mutableStateOf<PlanetData?>(null) }

    // CHANGED: sada držimo ceo objekat sa pozicijom (ne samo par)
    val placedPlanets = remember { mutableStateListOf<PlacedPlanet>() }

    var debugTap by remember { mutableStateOf<Offset?>(null) }
    val legendText = stringResource(R.string.retrograde_planet)

    val infiniteTransition = rememberInfiniteTransition()
    val planetPulse by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 12f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    val gestureMod = Modifier
        .pointerInput(Unit) {
            detectTransformGestures { _, pan, zoom, _ ->
                scale = (scale * zoom).coerceIn(MIN_SCALE, MAX_SCALE)
                translation += pan
            }
        }
        .pointerInput(scale, translation) {
            detectTapGestures(
                onDoubleTap = {
                    scale = 1f
                    translation = Offset.Zero
                },
                onTap = { tap ->
                    val canvasSize = this.size
                    val pivot = Offset(canvasSize.width / 2f, canvasSize.height / 2f)
                    val raw = ((tap - pivot) / scale + pivot) - translation
                    debugTap = raw

                    // CHANGED: hit test nad prilagođenim pozicijama
                    val threshold = 36f / scale
                    val hit = placedPlanets.minByOrNull { (it.pos - raw).getDistance() }?.takeIf {
                        (it.pos - raw).getDistance() <= threshold
                    }
                    selected = hit?.planet
                }
            )
        }

    val zodiacRotation = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        zodiacRotation.snapTo(355f)
        zodiacRotation.animateTo(360f, tween(500, easing = LinearEasing))
    }


    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f) // zadržavamo kvadrat
    ) {
    Canvas(
        modifier = Modifier
            .matchParentSize()
            .then(gestureMod)
    ) {

        placedPlanets.clear()


        withTransform({
            scale(scale, scale, pivot = center)
            translate(translation.x, translation.y)
        }) {
            val R = size.minDimension * 0.42f      // umesto /2.5f  → veći krug
            val C = center + Offset(0f, 100f)   //  100 px naniže
            val labelR = R + 30 //+ 30f

            drawCircle(Color.DarkGray, R, C)

// zodiak znakovi
            repeat(12) { i ->
                val a = Math.toRadians(i * 30.0 - 90 - zodiacRotation.value)
                val vx = cos(a).toFloat()
                val vy = sin(a).toFloat()

                val px = C.x + labelR * vx
                val py = C.y + labelR * vy

                //drawLine(Color(0xFF90CAF9), C, C + Offset(vx, vy) * R, 3f)  // svetloplave linije

                drawLine(Color.LightGray, C, C + Offset(vx, vy) * R, 2f)

                drawIntoCanvas { cv ->
                    cv.nativeCanvas.drawText(
                        zodiac[i],
                        px,
                        py + 10,
                        android.graphics.Paint().apply {
                            color = android.graphics.Color.WHITE
                            textAlign = android.graphics.Paint.Align.CENTER
                            textSize = 27f * TEXT_SCALE
                            isAntiAlias = true
                        }
                    )
                }
            }






            // NEW: izračunaj prilagođene pozicije (nema preklapanja)
            val baseRadius = max(32f, R - 78f)     // NE dozvoli negativan poluprečnik
            val placed = layoutPlanets(
                planets = planets,
                center = C,
                baseRadius = baseRadius,
                zodiacRotation = zodiacRotation.value
            )
            placedPlanets.addAll(placed)

            // CHANGED: aspekti crtani prema prilagođenim pozicijama (jasnije)
            val asp = listOf(0.0, 60.0, 90.0, 120.0, 180.0)
            val orb = 6.0
            for (i in placed.indices) for (j in i + 1 until placed.size) {
                val l1 = placed[i].angleDeg
                val l2 = placed[j].angleDeg
                val d = min(abs((l1 - l2 + 360) % 360), 360 - abs((l1 - l2 + 360) % 360))
                if (asp.any { abs(d - it) <= orb }) {
                    drawLine(
                        Color.Gray.copy(alpha = 0.35f),
                        placed[i].pos,
                        placed[j].pos,
                        strokeWidth = 1.4f
                    )
                }
            }

            // CHANGED: iscrtavanje planeta – koristi placedPlanets
            placed.forEach { item ->
                val planet = item.planet
                val pos = item.pos

                val PLANET_TEXT_SIZE = 32f * TEXT_SCALE

                // ivica (outline) oko teksta – da se bolje vidi preko linija
                val strokePaint = android.graphics.Paint().apply {
                    color = android.graphics.Color.BLACK
                    textAlign = android.graphics.Paint.Align.CENTER
                    textSize = PLANET_TEXT_SIZE
                    isAntiAlias = true
                    style = android.graphics.Paint.Style.STROKE
                    strokeWidth = 3f
                }
                val fillPaint = android.graphics.Paint().apply {
                    color = if (planet.retrograde) android.graphics.Color.CYAN else android.graphics.Color.YELLOW
                    textAlign = android.graphics.Paint.Align.CENTER
                    textSize = PLANET_TEXT_SIZE
                    isAntiAlias = true
                }

                val fm = fillPaint.fontMetrics
                val textHeight = fm.descent - fm.ascent
                val textCenterOffset = (textHeight / 2f) - fm.descent

                // pulsirajući prsten precizno centriran na simbol
                drawCircle(
                    color = if (planet.retrograde) Color.Cyan.copy(alpha = 0.3f) else Color.Yellow.copy(alpha = 0.3f),
                    radius = (20f * TEXT_SCALE) + (planetPulse * TEXT_SCALE),
                    center = Offset(pos.x, pos.y + textCenterOffset + manualYOffset),
                    style = Stroke(width = 3f * TEXT_SCALE)
                )

                // tekst sa ivicom + popuna
                drawIntoCanvas { cv ->
                    cv.nativeCanvas.drawText(planet.symbol, pos.x, pos.y + textCenterOffset, strokePaint)
                    cv.nativeCanvas.drawText(planet.symbol, pos.x, pos.y + textCenterOffset, fillPaint)
                }
            }

            // legenda retrogradnosti
            val legendCircleRadius = 10f
            val legendMargin = 16f
            val legendX = legendMargin * 2
            val legendY = size.height - legendMargin * 2 + 150f

            drawCircle(
                color = Color.Cyan,
                radius = legendCircleRadius,
                center = Offset(legendX, legendY)
            )

            drawIntoCanvas { cv ->
                cv.nativeCanvas.drawText(
                    legendText,
                    legendX + 20f,
                    legendY + 5f,
                    android.graphics.Paint().apply {
                        color = android.graphics.Color.WHITE
                        textAlign = android.graphics.Paint.Align.LEFT
                        textSize = 26f * TEXT_SCALE
                        isAntiAlias = true
                    }
                )
            }
        }
    }
        // --- Zoom toolbar (overlay gore) ---
        Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 0.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(Color(0x55000000)) // poluprovidna tamna pozadina
                .padding(horizontal = 4.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = {
                    scale = (scale + SCALE_STEP).coerceIn(MIN_SCALE, MAX_SCALE)
                }
            ) {
                Icon(
                    imageVector = Icons.Filled.ZoomIn,
                    contentDescription = "Zoom in",
                    tint = Color.White
                )
            }
            IconButton(
                onClick = {
                    scale = (scale - SCALE_STEP).coerceIn(MIN_SCALE, MAX_SCALE)
                }
            ) {
                Icon(
                    imageVector = Icons.Filled.ZoomOut,
                    contentDescription = "Zoom out",
                    tint = Color.White
                )
            }
            IconButton(
                onClick = {
                    // Reset na 100% i centriraj
                    scale = 1f
                    translation = Offset.Zero
                }
            ) {
                Icon(
                    imageVector = Icons.Filled.CenterFocusStrong,
                    contentDescription = "Reset zoom (100%)",
                    tint = Color.White
                )
            }
        }
    }

    selected?.let { p ->
        PlanetInfoDialog(planet = p) { selected = null }
    }
}
