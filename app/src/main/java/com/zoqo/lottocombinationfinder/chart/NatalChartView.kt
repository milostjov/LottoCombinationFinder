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


private val zodiac = listOf("♈", "♉", "♊", "♋", "♌", "♍", "♎", "♏", "♐", "♑", "♒", "♓")
private const val TEXT_SCALE = 1.50f   // <<< Povećava sve tekstove (1.0 = default)
val manualYOffset = -10f * TEXT_SCALE   // ⬅️ ručno podizanje kruga
@Composable
fun NatalChartView(
    planets: List<PlanetData>
) {
    var scale by remember { mutableStateOf(1f) }
    var translation by remember { mutableStateOf(Offset.Zero) }
    var selected by remember { mutableStateOf<String?>(null) }
    val planetCenters = remember { mutableStateListOf<Pair<String, Offset>>() }
    var debugTap by remember { mutableStateOf<Offset?>(null) }
    val legendText = stringResource(R.string.retrograde_planet)

    // Pulsiranje za sve planete
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
                scale = (scale * zoom).coerceIn(0.5f, 4f)
                translation += pan
            }
        }
        .pointerInput(scale, translation) {
            detectTapGestures(
                onDoubleTap = {
                    // ✅ Reset zoom i pozicije
                    scale = 1f
                    translation = Offset.Zero
                },
                onTap = { tap ->
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
            )
        }


    val zodiacRotation = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        zodiacRotation.snapTo(355f)
        zodiacRotation.animateTo(
            targetValue = 360f,
            animationSpec = tween(
                durationMillis = 500,
                easing = LinearEasing
            )
        )
    }





    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .then(gestureMod)
    ) {
        planetCenters.clear()

        withTransform({
            scale(scale, scale, pivot = center)
            translate(translation.x, translation.y)
        }) {
            val R = size.minDimension / 2.5f
            val C = center
            val labelR = R + 30f

            drawCircle(Color.DarkGray, R, C)

            //zodiak znakovi
            repeat(12) { i ->
                val a = Math.toRadians(i * 30.0 - 90 - zodiacRotation.value)
                val vx = cos(a).toFloat()
                val vy = sin(a).toFloat()
                drawLine(Color.LightGray, C, C + Offset(vx, vy) * R, 2f)

                drawIntoCanvas { cv ->
                    cv.nativeCanvas.drawText(
                        zodiac[i],
                        C.x + labelR * vx,
                        C.y + labelR * vy + 10,
                        android.graphics.Paint().apply {
                            color = android.graphics.Color.WHITE
                            textAlign = android.graphics.Paint.Align.CENTER
                            textSize = 27f * TEXT_SCALE
                            isAntiAlias = true
                        }
                    )
                }

                //brojevi 1..12
                val mid = Math.toRadians(i * 30.0 + 15.0 - 90 - zodiacRotation.value)
                drawIntoCanvas { cv ->
                    cv.nativeCanvas.drawText(
                        "${i + 1}",
                        C.x + (R - 42) * cos(mid).toFloat(),
                        C.y + (R - 42) * sin(mid).toFloat() + 8,
                        android.graphics.Paint().apply {
                            color = android.graphics.Color.LTGRAY
                            textAlign = android.graphics.Paint.Align.CENTER
                            textSize = 22f * TEXT_SCALE
                            isAntiAlias = true
                        }
                    )
                }
            }

            // Animirane planete
            planets.forEach { planet ->
                val ang = Math.toRadians(planet.longitude - 90 + zodiacRotation.value)
                val pos = Offset(
                    C.x + (R - 78) * cos(ang).toFloat(),
                    C.y + (R - 78) * sin(ang).toFloat()
                )
                planetCenters += planet.symbol to pos

                val PLANET_TEXT_SIZE = 32f * TEXT_SCALE
                val paint = android.graphics.Paint().apply {
                    color = if (planet.retrograde)
                        android.graphics.Color.CYAN
                    else
                        android.graphics.Color.YELLOW
                    textAlign = android.graphics.Paint.Align.CENTER
                    textSize = PLANET_TEXT_SIZE
                    isAntiAlias = true
                }

                // Pravi centar teksta
                val fm = paint.fontMetrics
                val textHeight = fm.descent - fm.ascent
                val textCenterOffset = (textHeight / 2f) - fm.descent

                // ✅ CIRCLE TAČNO U CENTRU SIMBOLA
                drawCircle(
                    color = if (planet.retrograde) Color.Cyan.copy(alpha = 0.3f) else Color.Yellow.copy(alpha = 0.3f),
                    radius = (20f * TEXT_SCALE) + (planetPulse * TEXT_SCALE),
                    center = Offset(pos.x, pos.y + textCenterOffset + manualYOffset),
                    style = Stroke(width = 3f * TEXT_SCALE)
                )

                // Iscrtavanje simbola
                drawIntoCanvas { cv ->
                    cv.nativeCanvas.drawText(planet.symbol, pos.x, pos.y + textCenterOffset, paint)
                }
            }



            // Aspekti
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

            // Legenda retrogradnosti
            val legendCircleRadius = 10f
            val legendMargin = 16f
            val legendX = legendMargin * 2
            val legendY = size.height - legendMargin * 2

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

    selected?.let { sym ->
        PlanetInfoDialog(sym) { selected = null }
    }
}
