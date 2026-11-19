package com.jesusjk0915.ahorro

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.matchParentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp

/** Ajustes de cristal disponibles en cualquier composable sin pasarlos a mano. */
val LocalGlassSettings = compositionLocalOf { Settings() }

/**
 * Capa de cristal líquido con blur REAL de Compose.
 *
 * No captura el View root (eso crasheaba antes con "Recording currently in
 * progress"): difumina capas de color dibujadas por la propia app, logrando el
 * aspecto esmerilado de iOS de forma segura. El radio y el tinte siguen el
 * slider de transparencia; [lens] es el punto donde está el dedo.
 */
@Composable
fun GlassLayer(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(24.dp),
    alpha: Float = 1f,
    lens: Offset? = null,
) {
    val settings = LocalGlassSettings.current
    if (!settings.glass) return

    val cs = MaterialTheme.colorScheme
    val t = settings.glassTransparency / 100f
    val radius = (10 + t * 22).dp
    val dense = LocalDensity.current

    Box(modifier.clip(shape)) {
        // Base oscura: da cuerpo al vidrio y mantiene legible el texto encima.
        Box(
            Modifier
                .matchParentSize()
                .background(Color.Black.copy(alpha = 0.32f * alpha)),
        )
        // Niebla de color: dos manchas desenfocadas que simulan lo que atraviesa el vidrio.
        Box(
            Modifier
                .matchParentSize()
                .background(
                    Brush.radialGradient(
                        listOf(cs.primary.copy(alpha = 0.55f * alpha), Color.Transparent),
                    ),
                )
                .blur(radius),
        )
        Box(
            Modifier
                .matchParentSize()
                .background(
                    Brush.radialGradient(
                        listOf(cs.tertiary.copy(alpha = 0.42f * alpha), Color.Transparent),
                    ),
                )
                .blur(radius * 1.15f),
        )
        if (lens != null) {
            // Lente: reflejo radial que sigue el dedo al deslizar sobre el dock.
            Box(
                Modifier
                    .matchParentSize()
                    .background(
                        Brush.radialGradient(
                            listOf(Color.White.copy(alpha = 0.42f * alpha), Color.Transparent),
                            center = lens,
                            radius = with(dense) { 110.dp.toPx() },
                        ),
                    )
                    .blur(radius * 0.45f),
            )
        }
        // Reflejo superior e inferior del vidrio.
        Box(
            Modifier
                .matchParentSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.White.copy(alpha = (0.22f + (1f - t) * 0.12f) * alpha),
                            Color.White.copy(alpha = 0.03f * alpha),
                            Color.Transparent,
                            cs.primary.copy(alpha = 0.12f * alpha),
                        ),
                    ),
                ),
        )
        // Borde especular: destello blanco arriba, tinte Material You abajo.
        Box(
            Modifier
                .matchParentSize()
                .border(
                    width = 1.5.dp,
                    brush = Brush.linearGradient(
                        listOf(
                            Color.White.copy(alpha = 0.78f * alpha),
                            cs.primary.copy(alpha = 0.30f * alpha),
                            Color.White.copy(alpha = 0.12f * alpha),
                            cs.tertiary.copy(alpha = 0.28f * alpha),
                        ),
                    ),
                    shape = shape,
                ),
        )
    }
}

/** Panel listo para usar: fondo de cristal (o sólido si está apagado) + contenido. */
@Composable
fun GlassPanel(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(24.dp),
    content: @Composable BoxScope.() -> Unit,
) {
    val settings = LocalGlassSettings.current
    Box(modifier.clip(shape)) {
        if (settings.glass) {
            GlassLayer(Modifier.matchParentSize(), shape)
        } else {
            Box(
                Modifier
                    .matchParentSize()
                    .background(MaterialTheme.colorScheme.surfaceContainerLow, shape),
            )
        }
        content()
    }
}
