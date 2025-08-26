package com.zoqo.lottocombinationfinder.components

import android.content.Context
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin


@Composable
fun LottoBallEarthSpin(
    number: Int,
    animationKey: Long,
    modifier: Modifier = Modifier,
    isBonus: Boolean = false,
    ballSize: Dp = 48.dp,
    padding: Dp = 4.dp,
    durationMs: Int = 800,
    orbitRadiusFrac: Float = 0.58f
) {
    val base = if (isBonus) Color(0xFFE53935) else Color.White
    val textColor = if (isBonus) Color.White else Color.Black
    val density = LocalDensity.current
// jačina senčenja (ivica)
    val shadeStrength = if (isBonus) 0.22f else 0.32f  // bele tamnije po ivici

// highlight jačina
    val highlightAlpha = if (isBonus) 0.95f else 1.0f  // bele jače svetle

    // 0f → 1f JEDNOM
    val t = remember { Animatable(0f) }
    LaunchedEffect(animationKey) {
        t.snapTo(0f)
        t.animateTo(1f, tween(durationMs, easing = FastOutSlowInEasing))
    }

    Box(
        modifier = modifier
            .size(ballSize)
            .padding(padding),
        contentAlignment = Alignment.Center
    ) {
        // podna senka
        Canvas(Modifier.matchParentSize()) {
            val r = size.minDimension / 2f
            drawOval(
                color = Color.Black.copy(alpha = 0.20f),
                topLeft = Offset(center.x - r * 0.7f, center.y + r * 0.55f),
                size = Size(r * 1.4f, r * 0.55f)
            )
        }

        // sve unutra je klipovano u krug
        Box(
            modifier = Modifier
                .matchParentSize()
                .clip(CircleShape)
        ) {
            // 3D sfera (statična)
            Canvas(Modifier.matchParentSize()) {
                val r = size.minDimension / 2f
                val c = center
                val light = Offset(c.x - r * 0.35f, c.y - r * 0.35f)
                fun shade(col: Color, k: Float) = androidx.compose.ui.graphics.lerp(col, Color.Black, k)
                fun tint (col: Color, k: Float) = androidx.compose.ui.graphics.lerp(col, Color.White, k)

                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(tint(base, 0.30f), base, shade(base, shadeStrength)),
                        center = light, radius = r * 1.25f
                    ),
                    radius = r * 0.98f, center = c
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color.Transparent, Color(0f, 0f, 0f, 0.18f)),
                        center = c, radius = r * 1.10f
                    ),
                    radius = r * 1.00f, center = c, blendMode = BlendMode.Multiply
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color.White.copy(alpha = highlightAlpha),
                            Color.White.copy(alpha = 0.35f),
                            Color.Transparent
                        ),
                        center = light, radius = r * 0.55f
                    ),
                    radius = r * 0.55f, center = light
                )
                drawArc(
                    brush = Brush.linearGradient(
                        colors = listOf(Color.White.copy(alpha = 0.18f), Color.Transparent),
                        start = Offset(c.x - r * 0.8f, c.y - r * 1.2f),
                        end   = Offset(c.x + r * 0.6f, c.y - r * 0.2f)
                    ),
                    startAngle = -100f, sweepAngle = 160f, useCenter = false,
                    topLeft = Offset(c.x - r, c.y - r),
                    size = Size(r * 2, r * 2)
                )
            }

            // KRETNJA PO EKVATORU – koristimo REALNU unutrašnju veličinu (posle paddinga)
            BoxWithConstraints(Modifier.matchParentSize()) {
                val sizePx = with(density) { min(maxWidth, maxHeight).toPx() }
                val rPx = sizePx / 2f
                val orbitR = rPx * orbitRadiusFrac

                // θ: 180° (iza) → 0° (ispred)
                val thetaDeg = androidx.compose.ui.util.lerp(180f, 0f, t.value)
                val thetaRad = Math.toRadians(thetaDeg.toDouble()).toFloat()

                val x = orbitR * sin(thetaRad)   // EKVATOR → y = 0
                val z = orbitR * cos(thetaRad)

                // foreshortening → na kraju ide ka 1×
                val edge = 1f - abs(sin(thetaRad))
                val scaleX = androidx.compose.ui.util.lerp(0.78f + 0.22f * edge, 1f, t.value)
                val scaleY = androidx.compose.ui.util.lerp(0.92f + 0.08f * edge, 1f, t.value)

                val frontAlpha = (z / orbitR).coerceIn(0f, 1f)
                val alpha = androidx.compose.ui.util.lerp(frontAlpha, 1f, t.value)

                // ⬅️ KLJUČ: translaciju primenjujemo na PARENT veličine lopte,
                // a Text centriramo unutar njega — zato ostaje savršeno centriran na kraju.
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .graphicsLayer {
                            translationX = x
                            translationY = 0f        // EKVATOR
                            cameraDistance = with(density) { 16.dp.toPx() }
                            this.scaleX = scaleX
                            this.scaleY = scaleY
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = number.toString(),
                        // opcioni tweak: lineHeight = fontSize da baselina ne “vuče” gore
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.Bold,
                            lineHeight = MaterialTheme.typography.bodyLarge.fontSize
                        ),
                        color = textColor.copy(alpha = alpha)
                    )
                }
            }
        }
    }
}





@Composable
fun LottoBallAnimateResult(
    number: Int,
    animationKey: Long,
    modifier: Modifier = Modifier,
    isBonus: Boolean = false
) {
    val base = if (isBonus) Color(0xFFE53935) else Color.White
    val textColor = if (isBonus) Color.White else Color.Black
// jačina senčenja (ivica)
    val shadeStrength = if (isBonus) 0.22f else 0.32f  // bele tamnije po ivici

// highlight jačina
    val highlightAlpha = if (isBonus) 0.95f else 1.0f  // bele jače svetle
    // suptilna puls animacija
    val infinite = rememberInfiniteTransition(label = "pulse")
    val scale by infinite.animateFloat(
        initialValue = 1f, targetValue = 1.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(700, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Box(
        modifier = modifier
            .size(48.dp)
            .padding(4.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
        contentAlignment = Alignment.Center
    ) {
        // Sjena na podlozi (mekana elipsa)
        Canvas(
            modifier = Modifier
                .matchParentSize()
        ) {
            val r = size.minDimension / 2f
            val cx = center.x
            val cy = center.y
            drawOval(
                color = Color.Black.copy(alpha = 0.20f),
                topLeft = Offset(cx - r * 0.7f, cy + r * 0.55f),
                size = Size(r * 1.4f, r * 0.55f)
            )
        }

        // Sfera sa slojevima gradijenata
        Canvas(
            modifier = Modifier
                .matchParentSize()
        ) {
            val r = size.minDimension / 2f
            val c = center

            // izvor svetla (gore-levo)
            val light = Offset(c.x - r * 0.35f, c.y - r * 0.35f)

            // helper za “tamniju”/“svetliju” nijansu
            fun shade(color: Color, t: Float) = androidx.compose.ui.graphics.lerp(color, Color.Black, t)
            fun tint(color: Color, t: Float)  = androidx.compose.ui.graphics.lerp(color, Color.White, t)

            // 1) osnovno popunjavanje – radijalni gradijent offset-ovan ka svetlu
            val body = Brush.radialGradient(
                colors = listOf(
                    tint(base, 0.30f),     // na mestu svetla malo svetlije
                    base,
                    shade(base, shadeStrength)     // rub blago tamniji
                ),
                center = light,
                radius = r * 1.25f
            )
            drawCircle(brush = body, radius = r * 0.98f, center = c)

            // 2) inner-vignette (mekana unutrašnja senka, bez “oštre ivice”)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.Transparent,
                        Color(0f, 0f, 0f, 0.18f)
                    ),
                    center = c,
                    radius = r * 1.10f
                ),
                radius = r * 1.00f,
                center = c,
                blendMode = BlendMode.Multiply
            )

            // 3) specular highlight (meka “fleka” sjaja)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White.copy(alpha = highlightAlpha),
                        Color.White.copy(alpha = 0.35f),
                        Color.Transparent
                    ),
                    center = light,
                    radius = r * 0.55f
                ),
                radius = r * 0.55f,
                center = light
            )

            // 4) glossy “crescent” – vrlo suptilna traka preko gornje polovine
            drawArc(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.18f),
                        Color.Transparent
                    ),
                    start = Offset(c.x - r * 0.8f, c.y - r * 1.2f),
                    end   = Offset(c.x + r * 0.6f, c.y - r * 0.2f)
                ),
                startAngle = -100f,
                sweepAngle = 160f,
                useCenter = false,
                topLeft = Offset(c.x - r, c.y - r),
                size = Size(r * 2, r * 2)
            )
        }

        // broj u centru
        Text(
            text = number.toString(),
            style = MaterialTheme.typography.bodyLarge.copy(
                fontWeight = FontWeight.Bold
            ),
            color = textColor
        )
    }
}







fun getTotalNumbersFromPrefs(context: Context): Int {
    val prefs = context.getSharedPreferences("astro_prefs", Context.MODE_PRIVATE)
    return prefs.getInt("totalNumbers", 39) // 39 default ako nije setovano
}