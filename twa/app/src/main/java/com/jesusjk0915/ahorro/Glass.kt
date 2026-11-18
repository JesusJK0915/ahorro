package com.jesusjk0915.ahorro

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

/**
 * Cristal líquido estilo iOS: reflejo diagonal, borde luminoso, sombra suave
 * y transparencia ajustable. En "opaco" el vidrio es esmerilado casi sólido.
 */
@Composable
fun Modifier.liquidGlass(
    settings: Settings,
    shape: Shape = RoundedCornerShape(20.dp),
): Modifier {
    if (!settings.glass) return this

    val cs = MaterialTheme.colorScheme
    val opaque = settings.glassStyle == "opaque"
    val t = settings.glassTransparency.coerceIn(0, 100) / 100f
    // 0% => casi sólido; 100% => muy translúcido.
    val alpha = if (opaque) 1f else 1f - (t * 0.62f)
    val glow = if (opaque) 0.06f else 0.16f
    val base = if (opaque) cs.surfaceContainerHigh else cs.surfaceContainer

    return this
        .shadow(
            elevation = if (opaque) 3.dp else 10.dp,
            shape = shape,
            ambientColor = Color.Black.copy(alpha = 0.42f),
            spotColor = cs.primary.copy(alpha = 0.28f),
        )
        .background(
            brush = Brush.linearGradient(
                colors = listOf(
                    cs.primary.copy(alpha = glow),
                    base.copy(alpha = alpha),
                    cs.tertiary.copy(alpha = glow * 0.8f),
                ),
            ),
            shape = shape,
        )
        .border(
            width = 1.dp,
            brush = Brush.linearGradient(
                colors = listOf(
                    Color.White.copy(alpha = if (opaque) 0.20f else 0.34f),
                    cs.primary.copy(alpha = 0.10f),
                    Color.White.copy(alpha = 0.06f),
                ),
            ),
            shape = shape,
        )
}

/** Color de contenedor para Card: transparente cuando el cristal está activo. */
@Composable
fun glassCardColor(settings: Settings, fallback: Color): Color =
    if (settings.glass) Color.Transparent else fallback

/** Atajo para los Cards que antes usaban surfaceContainerLow. */
@Composable
fun lowCardColors(settings: Settings) = CardDefaults.cardColors(
    containerColor = glassCardColor(settings, MaterialTheme.colorScheme.surfaceContainerLow)
)
