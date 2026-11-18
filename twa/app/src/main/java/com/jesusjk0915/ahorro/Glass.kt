package com.jesusjk0915.ahorro

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
 * Cristal líquido estilo iOS: borde luminoso y sombra suave, SIN relleno.
 * Al no pintar fondo, lo que hay detrás se ve tal cual y el efecto es real.
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

    return this
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

/** Color de contenedor para Card: transparente cuando el cristal está activo. */
@Composable
fun glassCardColor(settings: Settings, fallback: Color): Color =
    if (settings.glass) Color.Transparent else fallback

/** Atajo para los Cards que antes usaban surfaceContainerLow. */
@Composable
fun lowCardColors(settings: Settings) = CardDefaults.cardColors(
    containerColor = glassCardColor(settings, MaterialTheme.colorScheme.surfaceContainerLow)
)
