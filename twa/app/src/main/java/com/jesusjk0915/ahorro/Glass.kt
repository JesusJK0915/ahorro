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
 * Cristal líquido estilo iOS: borde luminoso, sombra suave y fondo translúcido real.
 * El fondo usa glassTransparency para que se vean los colores de atrás
 * (por ejemplo, si pasa por una pared roja, se refleja el rojo).
 * En "opaco" el borde y la sombra se atenúan.
 */
@Composable
fun Modifier.liquidGlass(
    settings: Settings,
    shape: Shape = RoundedCornerShape(20.dp),
): Modifier {
    if (!settings.glass) return this

    val cs = MaterialTheme.colorScheme
    val opaque = settings.glassStyle == "opaque"

    // Fondo translúcido real: 0 = más sólido, 100 = más transparente.
    val glassAlpha = 0.35f - (settings.glassTransparency / 100f) * 0.25f

    return this
        .background(
            Color.Black.copy(alpha = glassAlpha),
            shape,
        )
        .shadow(
            elevation = if (opaque) 3.dp else 10.dp,
            shape = shape,
            ambientColor = Color.Black.copy(alpha = 0.42f),
            spotColor = cs.primary.copy(alpha = 0.28f),
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

/**
 * Vidrio líquido estilo iOS para el dock flotante: sin relleno negro,
 * solo un tinte blanco muy tenue, reflejo superior, borde luminoso y
 * sombra para que parezca suspendido sobre el contenido.
 */
@Composable
fun Modifier.iosDockGlass(settings: Settings): Modifier {
    val cs = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(50)

    // Cristal desactivado: dock flotante normal, como antes del vidrio líquido.
    if (!settings.glass) {
        return this
            .shadow(
                elevation = 10.dp,
                shape = shape,
                ambientColor = Color.Black.copy(alpha = 0.45f),
                spotColor = cs.primary.copy(alpha = 0.18f),
            )
            .background(cs.surfaceContainerHigh, shape)
            .border(
                width = 1.dp,
                color = cs.outlineVariant.copy(alpha = 0.35f),
                shape = shape,
            )
    }

    val opaque = settings.glassStyle == "opaque"

    return this
        .shadow(
            elevation = if (opaque) 6.dp else 18.dp,
            shape = shape,
            ambientColor = Color.Black.copy(alpha = 0.55f),
            spotColor = cs.primary.copy(alpha = 0.35f),
        )
        .background(
            Color.White.copy(alpha = if (opaque) 0.04f else 0.06f),
            shape,
        )
        .background(
            Brush.verticalGradient(
                colors = listOf(
                    Color.White.copy(alpha = if (opaque) 0.12f else 0.22f),
                    Color.White.copy(alpha = 0.05f),
                    Color.Transparent,
                    cs.primary.copy(alpha = 0.08f),
                ),
            ),
            shape,
        )
        .border(
            width = 2.dp,
            brush = Brush.linearGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.70f),
                    cs.primary.copy(alpha = 0.28f),
                    Color.White.copy(alpha = 0.16f),
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
