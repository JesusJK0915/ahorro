package com.jesusjk0915.ahorro

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Icono "anillo" de Metas, igual al SVG de la web (dos círculos concéntricos). */
@Composable
private fun TargetIcon(tint: Color, iconSize: Dp = 20.dp) {
    Canvas(Modifier.size(iconSize)) {
        val s = size.minDimension / 24f
        val stroke = 2f * s
        drawCircle(color = tint, radius = 8.5f * s, style = Stroke(stroke))
        drawCircle(color = tint, radius = 3.5f * s, style = Stroke(stroke))
    }
}

/** Icono de Historial, igual al SVG de la web (tres líneas, la última más corta). */
@Composable
private fun ListIcon(tint: Color, iconSize: Dp = 20.dp) {
    Canvas(Modifier.size(iconSize)) {
        val s = size.minDimension / 24f
        val stroke = 2f * s
        drawLine(tint, Offset(4f * s, 7f * s), Offset(20f * s, 7f * s), stroke)
        drawLine(tint, Offset(4f * s, 12f * s), Offset(20f * s, 12f * s), stroke)
        drawLine(tint, Offset(4f * s, 17f * s), Offset(14f * s, 17f * s), stroke)
    }
}

/**
 * Barra de la versión web con movimiento Material 3 Expressive:
 * spring con rebote, entrada desde abajo, escala del ícono activo, brillo
 * líquido que recorre la píldora y feedback al presionar.
 */
@Composable
fun WebNavBar(settings: Settings, tab: Int, onTab: (Int) -> Unit) {
    val cs = MaterialTheme.colorScheme
    val density = LocalDensity.current

    // Curvas de la web: --ease-spring (.34, 1.32, .64, 1) y --ease-bounce (.2, .9, .2, 1.35).
    val bouncy = spring<Float>(
        dampingRatio = Spring.DampingRatioLowBouncy,
        stiffness = Spring.StiffnessMediumLow,
    )
    val bouncyDp = spring<Dp>(
        dampingRatio = Spring.DampingRatioLowBouncy,
        stiffness = Spring.StiffnessMediumLow,
    )

    var entered by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { entered = true }
    val enter by animateFloatAsState(
        targetValue = if (entered) 1f else 0f,
        animationSpec = bouncy,
        label = "navEnter",
    )
    val enterOffset = with(density) { 24.dp.toPx() }

    // Brillo líquido: recorre la píldora cada pocos segundos (liquidSheen de la web).
    val sheenTransition = rememberInfiniteTransition(label = "sheen")
    val sheen by sheenTransition.animateFloat(
        initialValue = -1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(7000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "sheenPos",
    )

    // Medimos cada botón como hacía placeIndicator(): left = offsetLeft, width = offsetWidth.
    val xs = remember { mutableStateListOf(0.dp, 0.dp, 0.dp, 0.dp) }
    val ws = remember { mutableStateListOf(0.dp, 0.dp, 0.dp, 0.dp) }
    val hs = remember { mutableStateListOf(0.dp, 0.dp, 0.dp, 0.dp) }

    val indicatorX by animateDpAsState(
        targetValue = xs[tab],
        animationSpec = bouncyDp,
        label = "indicatorX",
    )
    val indicatorW by animateDpAsState(
        targetValue = ws[tab],
        animationSpec = bouncyDp,
        label = "indicatorW",
    )

    val labels = listOf("Metas", "Historial", "Logros", "Ajustes")
    val iconSize = if (settings.hideDockLabels) 28.dp else 20.dp
    // El difuminado sigue el slider de transparencia del cristal líquido.
    val glassBlur = (6 + settings.glassTransparency * 18 / 100).dp
    val fogAlpha = 0.10f + settings.glassTransparency / 100f * 0.22f

    Box(
        modifier = Modifier.fillMaxWidth().padding(bottom = 22.dp),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Box(
            modifier = Modifier
                .padding(horizontal = 20.dp)
                .widthIn(max = 560.dp)
                .iosDockGlass(settings)
                .padding(10.dp)
                .clip(RoundedCornerShape(50))
                .drawBehind {
                    val x = sheen * size.width
                    if (settings.glass) {
                        // Cristal líquido activo: reflejos del vidrio y lente.
                        // Barrido ancho y difuso, como el reflejo del vidrio.
                        drawRect(
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.White.copy(alpha = 0.16f),
                                    Color.Transparent,
                                ),
                                start = Offset(x - size.width * 0.45f, 0f),
                                end = Offset(x + size.width * 0.45f, size.height),
                            ),
                        )
                        // Hilo de luz fino que se desliza por encima.
                        drawRect(
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.White.copy(alpha = 0.45f),
                                    Color.Transparent,
                                ),
                                start = Offset(x - size.width * 0.16f, 0f),
                                end = Offset(x + size.width * 0.16f, size.height),
                            ),
                        )
                        // Reflejo superior del borde.
                        drawLine(
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.White.copy(alpha = 0.55f),
                                    Color.Transparent,
                                ),
                            ),
                            start = Offset(size.width * 0.15f, 1f),
                            end = Offset(size.width * 0.85f, 1f),
                            strokeWidth = 1.5f,
                        )
                        // Efecto lente (--gx del CSS): reflejo radial que sigue
                        // a la pestaña activa.
                        val activeX = with(density) { xs[tab].toPx() + ws[tab].toPx() / 2f }
                        drawRect(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.26f),
                                    Color.Transparent,
                                ),
                                center = Offset(activeX, size.height * 0.4f),
                                radius = with(density) { 100.dp.toPx() },
                            ),
                        )
                    } else {
                        // Cristal apagado: dock flotante simple, igual que antes.
                        drawRect(
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.White.copy(alpha = 0.28f),
                                    Color.Transparent,
                                ),
                                start = Offset(x - size.width * 0.3f, 0f),
                                end = Offset(x + size.width * 0.3f, size.height),
                            ),
                        )
                    }
                }
                .graphicsLayer {
                    alpha = enter
                    translationY = (1f - enter) * enterOffset
                    val s = 0.94f + 0.06f * enter
                    scaleX = s
                    scaleY = s
                },
        ) {
            // Niebla del cristal: difumina los colores que atraviesan el dock.
            // Usa blur de Compose (seguro; no captura el View root).
            if (settings.glass && hs[tab] > 0.dp) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(hs[tab])
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    Color.White.copy(alpha = fogAlpha),
                                    cs.primary.copy(alpha = fogAlpha * 0.9f),
                                    cs.tertiary.copy(alpha = fogAlpha * 0.65f),
                                    Color.Transparent,
                                ),
                            ),
                            RoundedCornerShape(50),
                        )
                        .blur(glassBlur)
                        .clip(RoundedCornerShape(50)),
                )
            }

            if (ws[tab] > 0.dp && hs[tab] > 0.dp) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .offset(x = indicatorX)
                        .width(indicatorW)
                        .height(hs[tab])
                        .background(cs.primaryContainer, RoundedCornerShape(50)),
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(0.dp),
            ) {
                labels.forEachIndexed { index, label ->
                    val active = tab == index
                    val tint = if (active) cs.onPrimaryContainer else cs.onSurfaceVariant
                    val interaction = remember { MutableInteractionSource() }
                    val pressed by interaction.collectIsPressedAsState()
                    val pressScale by animateFloatAsState(
                        targetValue = if (pressed) 0.96f else 1f,
                        animationSpec = bouncy,
                        label = "press$index",
                    )
                    val iconScale by animateFloatAsState(
                        targetValue = if (active) 1.08f else 1f,
                        animationSpec = bouncy,
                        label = "icon$index",
                    )

                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .clickable(
                                interactionSource = interaction,
                                indication = null,
                                onClick = { onTab(index) },
                            )
                            .padding(horizontal = 10.dp, vertical = 13.dp)
                            .graphicsLayer {
                                scaleX = pressScale
                                scaleY = pressScale
                            }
                            .onGloballyPositioned { c ->
                                val nx = with(density) { c.positionInParent().x.toDp() }
                                val nw = with(density) { c.size.width.toDp() }
                                val nh = with(density) { c.size.height.toDp() }
                                if (xs[index] != nx) xs[index] = nx
                                if (ws[index] != nw) ws[index] = nw
                                if (hs[index] != nh) hs[index] = nh
                            },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = if (settings.hideDockLabels) {
                            Arrangement.Center
                        } else {
                            Arrangement.spacedBy(8.dp)
                        },
                    ) {
                        Box(
                            modifier = Modifier.graphicsLayer {
                                scaleX = iconScale
                                scaleY = iconScale
                            },
                            contentAlignment = Alignment.Center,
                        ) {
                            when (index) {
                                0 -> TargetIcon(tint, iconSize)
                                1 -> ListIcon(tint, iconSize)
                                2 -> Icon(
                                    Icons.Default.EmojiEvents,
                                    contentDescription = null,
                                    modifier = Modifier.size(iconSize),
                                    tint = tint,
                                )
                                else -> Icon(
                                    Icons.Default.Settings,
                                    contentDescription = null,
                                    modifier = Modifier.size(iconSize),
                                    tint = tint,
                                )
                            }
                        }
                        if (!settings.hideDockLabels) {
                            Text(
                                label,
                                color = tint,
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                            )
                        }
                    }
                }
            }
        }
    }
}
