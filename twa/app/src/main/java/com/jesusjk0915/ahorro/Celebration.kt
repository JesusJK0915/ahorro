package com.jesusjk0915.ahorro

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin

/**
 * Celebración ligera al completar una meta: confeti + tarjeta con rebote.
 * Se cierra sola o al tocarla.
 */
@Composable
fun GoalCelebration(name: String, onDone: () -> Unit) {
    val anim = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        anim.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioLowBouncy,
                stiffness = Spring.StiffnessMediumLow,
            ),
        )
        delay(850)
        onDone()
    }
    val p = anim.value
    val cs = MaterialTheme.colorScheme
    val colors = listOf(cs.primary, cs.tertiary, cs.secondary, Color.White)

    Box(
        modifier = Modifier.fillMaxSize().clickable { onDone() },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val t = p.coerceIn(0f, 1f)
            repeat(40) { i ->
                val angle = i * 2.399963f
                val radius = (70f + (i % 7) * 28f) * density * t
                val x = size.width / 2f + cos(angle) * radius
                val y = size.height * 0.40f + sin(angle) * radius + 300f * density * t * t
                drawCircle(
                    color = colors[i % colors.size].copy(alpha = 0.85f),
                    radius = (3f + (i % 3)) * density,
                    center = Offset(x, y),
                )
            }
        }

        Card(
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = cs.surfaceContainerHigh),
            modifier = Modifier.graphicsLayer {
                alpha = p.coerceIn(0f, 1f)
                val s = 0.86f + 0.14f * p.coerceIn(0f, 1.15f)
                scaleX = s
                scaleY = s
            },
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 28.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    "¡Meta cumplida!",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = cs.primary,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "Lograste tu objetivo de ahorro.",
                    style = MaterialTheme.typography.bodySmall,
                    color = cs.onSurfaceVariant,
                    fontSize = 13.sp,
                )
            }
        }
    }
}
