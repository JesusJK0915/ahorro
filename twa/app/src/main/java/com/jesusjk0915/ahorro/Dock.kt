package com.jesusjk0915.ahorro

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp

/**
 * Dock flotante estilo Liquid Glass: al deslizar el dedo el vidrio se inclina
 * y el reflejo sigue el movimiento, luego vuelve suave a su posición.
 */
@Composable
fun GlassDock(
    settings: Settings,
    tab: Int,
    onTab: (Int) -> Unit,
) {
    var tiltX by remember { mutableStateOf(0f) }
    var tiltY by remember { mutableStateOf(0f) }
    var glowX by remember { mutableStateOf(0.5f) }

    val animX by animateFloatAsState(tiltX, spring(stiffness = Spring.StiffnessMediumLow))
    val animY by animateFloatAsState(tiltY, spring(stiffness = Spring.StiffnessMediumLow))
    val animGlow by animateFloatAsState(glowX, spring(stiffness = Spring.StiffnessMediumLow))

    val cs = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(28.dp)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 8.dp)
            .graphicsLayer {
                rotationX = animX
                rotationY = animY
                cameraDistance = 14f * density
            }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDrag = { change, amount ->
                        change.consume()
                        val w = size.width.coerceAtLeast(1)
                        tiltY = (tiltY + amount.x * 0.10f).coerceIn(-12f, 12f)
                        tiltX = (tiltX - amount.y * 0.10f).coerceIn(-10f, 10f)
                        glowX = (glowX + amount.x / w).coerceIn(0f, 1f)
                    },
                    onDragEnd = {
                        tiltX = 0f
                        tiltY = 0f
                        glowX = 0.5f
                    },
                    onDragCancel = {
                        tiltX = 0f
                        tiltY = 0f
                        glowX = 0.5f
                    },
                )
            }
            .liquidGlass(settings, shape)
            .drawBehind {
                if (settings.glass) {
                    val cx = animGlow * size.width
                    val cy = size.height * 0.35f
                    val r = size.width * 0.42f
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(Color.White.copy(alpha = 0.22f), Color.Transparent),
                            center = Offset(cx, cy),
                            radius = r,
                        ),
                        radius = r,
                        center = Offset(cx, cy),
                    )
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        NavigationBar(
            containerColor = if (settings.glass) Color.Transparent else cs.surfaceContainer,
            contentColor = cs.onSurface,
            tonalElevation = if (settings.glass) 0.dp else 3.dp,
        ) {
            val indicator = if (settings.glass) {
                cs.primary.copy(alpha = 0.26f)
            } else {
                cs.secondaryContainer
            }
            val colors = NavigationBarItemDefaults.colors(indicatorColor = indicator)
            NavigationBarItem(
                selected = tab == 0,
                onClick = { onTab(0) },
                icon = { Icon(Icons.Default.Home, contentDescription = null) },
                label = { Text("Inicio") },
                colors = colors,
            )
            NavigationBarItem(
                selected = tab == 1,
                onClick = { onTab(1) },
                icon = { Icon(Icons.Default.History, contentDescription = null) },
                label = { Text("Historial") },
                colors = colors,
            )
            NavigationBarItem(
                selected = tab == 2,
                onClick = { onTab(2) },
                icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                label = { Text("Ajustes") },
                colors = colors,
            )
        }
    }
}
