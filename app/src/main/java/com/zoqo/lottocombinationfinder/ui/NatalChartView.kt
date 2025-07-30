package com.zoqo.lottocombinationfinder.ui

import androidx.compose.animation.animateColor
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import kotlin.math.*
import androidx.compose.animation.core.*
import androidx.compose.ui.res.stringResource
import com.zoqo.lottocombinationfinder.R
import com.zoqo.lottocombinationfinder.data.PlanetData
/**
 * Podaci o planeti sa retrogradnošću
 */


private val zodiac = listOf("♈", "♉", "♊", "♋", "♌", "♍", "♎", "♏", "♐", "♑", "♒", "♓")

@Composable
fun NatalChartView(
    data: AstroInputData,
    planets: List<PlanetData>
) {
    var scale by remember { mutableStateOf(1f) }
    var translation by remember { mutableStateOf(Offset.Zero) }
    var selected by remember { mutableStateOf<String?>(null) }
    val planetCenters = remember { mutableStateListOf<Pair<String, Offset>>() }
    var debugTap by remember { mutableStateOf<Offset?>(null) }
    val legendText = stringResource(R.string.retrograde_planet)
    val pulseAnim = rememberInfiniteTransition()
    val pulseRadius by pulseAnim.animateFloat(
        initialValue = 0f,
        targetValue = 20f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )
    val colorAnim = rememberInfiniteTransition()
    val pulseColor by colorAnim.animateColor(
        initialValue = Color(0xFFFFEB3B),
        targetValue = Color(0xFFFF0000),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    val gestureMod = Modifier
        .pointerInput(Unit) {
            detectTransformGestures { _, pan, zoom, _ ->
                scale = (scale * zoom).coerceIn(0.5f, 4f)
                translation += pan
            }
        }
        .pointerInput(scale, translation) {
            detectTapGestures { tap ->
                val canvasSize = this.size
                val pivot = Offset(canvasSize.width / 2f, canvasSize.height / 2f)
                val raw = ((tap - pivot) / scale + pivot) - translation

                debugTap = raw

                val threshold = 50f / scale
                val hit = planetCenters.firstOrNull { (_, pos) ->
                    (pos - raw).getDistance() <= threshold
                }
                selected = hit?.first
            }
        }

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .then(gestureMod)
    ) {
        planetCenters.clear()

        // Apply scale and translation transform for drawing
        withTransform({
            scale(scale, scale, pivot = center)
            translate(translation.x, translation.y)
        }) {
            val R = size.minDimension / 2.5f
            val C = center
            val labelR = R + 30f

            drawCircle(Color.DarkGray, R, C)

            repeat(12) { i ->
                val a = Math.toRadians(i * 30.0 - 90)
                val vx = cos(a).toFloat()
                val vy = sin(a).toFloat()
                drawLine(Color.LightGray, C, C + Offset(vx, vy) * R, 2f)

                // Zodiac sign
                drawIntoCanvas { cv ->
                    cv.nativeCanvas.drawText(
                        zodiac[i],
                        C.x + labelR * vx,
                        C.y + labelR * vy + 10,
                        android.graphics.Paint().apply {
                            color = android.graphics.Color.WHITE
                            textAlign = android.graphics.Paint.Align.CENTER
                            textSize = 32f
                            isAntiAlias = true
                        }
                    )
                }

                // House number
                val mid = Math.toRadians(i * 30.0 + 15.0 - 90)
                drawIntoCanvas { cv ->
                    cv.nativeCanvas.drawText(
                        "${i + 1}",
                        C.x + (R - 42) * cos(mid).toFloat(),
                        C.y + (R - 42) * sin(mid).toFloat() + 8,
                        android.graphics.Paint().apply {
                            color = android.graphics.Color.LTGRAY
                            textAlign = android.graphics.Paint.Align.CENTER
                            textSize = 22f
                            isAntiAlias = true
                        }
                    )
                }
            }

            // Planets with retrograde mark
            planets.forEach { planet ->
                val ang = Math.toRadians(planet.longitude - 90)
                val pos = Offset(
                    C.x + (R - 78) * cos(ang).toFloat(),
                    C.y + (R - 78) * sin(ang).toFloat()
                )
                planetCenters += planet.symbol to pos

                //  tekst koji uključuje ℞ i menja boju
                val display = if (planet.retrograde) "${planet.symbol}" else planet.symbol
                val paint = android.graphics.Paint().apply {
                    color = if (planet.retrograde)
                        android.graphics.Color.CYAN        // retrogradne označi plavkasto
                    else
                        android.graphics.Color.YELLOW
                    textAlign = android.graphics.Paint.Align.CENTER
                    textSize = 28f
                    isAntiAlias = true
                }
                drawIntoCanvas { cv ->
                    cv.nativeCanvas.drawText(display, pos.x, pos.y + 10, paint)
                }
            }

            // Pulsirajući efekat oko selektovane planete
            selected?.let { sym ->
                val pos = planetCenters.firstOrNull { it.first == sym }?.second
                if (pos != null) {
                    drawCircle(
                        color = pulseColor.copy(alpha = 0.4f),
                        radius = 28f + pulseRadius,
                        center = pos,
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3f)
                    )
                }
            }

            // Aspects
            val asp = listOf(0.0, 60.0, 90.0, 120.0, 180.0)
            val orb = 6.0
            for (i in planets.indices) for (j in i + 1 until planets.size) {
                val l1 = planets[i].longitude
                val l2 = planets[j].longitude
                val d = min(abs((l1 - l2 + 360) % 360), 360 - abs((l1 - l2 + 360) % 360))
                if (asp.any { abs(d - it) <= orb }) {
                    drawLine(
                        Color.Gray.copy(alpha = 0.35f),
                        planetCenters[i].second,
                        planetCenters[j].second,
                        strokeWidth = 1.4f
                    )
                }
            }
            // --- Legenda za retrogradnost ---
            val legendCircleRadius = 10f
            val legendMargin = 16f

// Pozicija legende (donji desni ugao canvasa)
            val legendX = legendMargin * 2
            val legendY = size.height - legendMargin * 2


// Mali cyan krug
            drawCircle(
                color = Color.Cyan,
                radius = legendCircleRadius,
                center = Offset(legendX, legendY)
            )

// Tekst pored kruga


            drawIntoCanvas { cv ->
                cv.nativeCanvas.drawText(
                    legendText,
                    legendX + 20f,               // razmak od kruga
                    legendY + 5f,                // mala vertikalna korekcija
                    android.graphics.Paint().apply {
                        color = android.graphics.Color.WHITE
                        textAlign = android.graphics.Paint.Align.LEFT
                        textSize = 26f
                        isAntiAlias = true
                    }
                )
            }

        }
    }

    selected?.let { sym ->
        PlanetInfoDialog(sym) { selected = null }
    }
}
