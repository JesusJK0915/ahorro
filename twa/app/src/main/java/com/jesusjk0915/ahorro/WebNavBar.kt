package com.jesusjk0915.ahorro

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Icono "anillo" de Metas, igual al SVG de la web (dos círculos concéntricos). */
@Composable
private fun TargetIcon(tint: Color) {
    Canvas(Modifier.size(20.dp)) {
        val s = 20f / 24f * density
        val stroke = 2f * s
        drawCircle(color = tint, radius = 8.5f * s, style = Stroke(stroke))
        drawCircle(color = tint, radius = 3.5f * s, style = Stroke(stroke))
    }
}

/** Icono de Historial, igual al SVG de la web (tres líneas, la última más corta). */
@Composable
private fun ListIcon(tint: Color) {
    Canvas(Modifier.size(20.dp)) {
        val s = 20f / 24f * density
        val stroke = 2f * s
        drawLine(tint, Offset(4f * s, 7f * s), Offset(20f * s, 7f * s), stroke)
        drawLine(tint, Offset(4f * s, 12f * s), Offset(20f * s, 12f * s), stroke)
        drawLine(tint, Offset(4f * s, 17f * s), Offset(14f * s, 17f * s), stroke)
    }
}

/**
 * Réplica exacta de la barra de la versión web:
 * píldora con 7dp de padding, 4dp de gap, ícono + texto en fila y un indicador
 * medido del botón activo (placeIndicator() en app.js).
 */
@Composable
fun WebNavBar(settings: Settings, tab: Int, onTab: (Int) -> Unit) {
    val cs = MaterialTheme.colorScheme
    val density = LocalDensity.current

    // Medimos cada botón como hacía placeIndicator(): left = offsetLeft, width = offsetWidth.
    val xs = remember { mutableStateListOf(0.dp, 0.dp, 0.dp) }
    val ws = remember { mutableStateListOf(0.dp, 0.dp, 0.dp) }
    val hs = remember { mutableStateListOf(0.dp, 0.dp, 0.dp) }

    val indicatorX by animateDpAsState(
        targetValue = xs[tab],
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "indicatorX",
    )
    val indicatorW by animateDpAsState(
        targetValue = ws[tab],
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "indicatorW",
    )

    val labels = listOf("Metas", "Historial", "Ajustes")

    Box(
        modifier = Modifier.fillMaxWidth().padding(bottom = 14.dp),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Box(
            modifier = Modifier
                .padding(horizontal = 14.dp)
                .widthIn(max = 492.dp)
                .liquidGlass(settings, shape = RoundedCornerShape(50))
                .padding(7.dp),
        ) {
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
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                labels.forEachIndexed { index, label ->
                    val active = tab == index
                    val tint = if (active) cs.onPrimaryContainer else cs.onSurfaceVariant
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onTab(index) }
                            .padding(horizontal = 10.dp, vertical = 13.dp)
                            .onGloballyPositioned { c ->
                                val nx = with(density) { c.positionInParent().x.toDp() }
                                val nw = with(density) { c.size.width.toDp() }
                                val nh = with(density) { c.size.height.toDp() }
                                if (xs[index] != nx) xs[index] = nx
                                if (ws[index] != nw) ws[index] = nw
                                if (hs[index] != nh) hs[index] = nh
                            },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        when (index) {
                            0 -> TargetIcon(tint)
                            1 -> ListIcon(tint)
                            else -> Icon(
                                Icons.Default.Settings,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = tint,
                            )
                        }
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
