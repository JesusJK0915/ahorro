package com.jesusjk0915.ahorro

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.Vibrator
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.graphicsLayer
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Ícono de billetera del APK 1.0.0 (mismo trazo que el SVG de la bienvenida vieja). */
@Composable
private fun WalletArt(tint: Color) {
    Canvas(Modifier.size(54.dp)) {
        val s = size.minDimension / 24f
        val st = 1.6f * s
        drawRoundRect(
            color = tint,
            topLeft = Offset(3f * s, 6f * s),
            size = Size(18f * s, 12f * s),
            cornerRadius = CornerRadius(2.5f * s),
            style = Stroke(st),
        )
        drawLine(tint, Offset(16f * s, 12.5f * s), Offset(18f * s, 12.5f * s), st)
        listOf(12f, 6f, 18f).forEach { x ->
            drawLine(tint, Offset(x * s, 3f * s), Offset(x * s, 6f * s), st)
        }
    }
}

/**
 * Pantalla de bienvenida del APK 1.0.0: tarjeta centrada, ícono de billetera,
 * "Tu dinero, con rumbo" y el botón "Empezar a ahorrar". Animación de entrada
 * y salida de 450 ms (welcomeIn / welcomeOut de la versión web vieja).
 */
@Composable
fun WelcomeScreen(onDismiss: () -> Unit) {
    val context = LocalContext.current
    var entered by remember { mutableStateOf(false) }
    var leaving by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { entered = true }

    val appear by animateFloatAsState(
        targetValue = if (leaving) 0f else if (entered) 1f else 0f,
        animationSpec = tween(450),
        label = "welcomeAppear",
    )
    val scale = if (leaving) 0.97f + 0.03f * appear else 1.03f - 0.03f * appear

    fun start() {
        if (leaving) return
        leaving = true
        try {
            val v = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            v?.vibrate(20)
        } catch (_: Exception) {
        }
        Handler(Looper.getMainLooper()).postDelayed(Runnable { onDismiss() }, 450)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {},
            )
            .graphicsLayer {
                alpha = appear
                scaleX = scale
                scaleY = scale
            },
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 420.dp)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .size(108.dp)
                    .shadow(
                        elevation = 10.dp,
                        shape = RoundedCornerShape(32.dp),
                        spotColor = Color.Black.copy(alpha = 0.12f),
                    )
                    .clip(RoundedCornerShape(32.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                WalletArt(MaterialTheme.colorScheme.onPrimaryContainer)
            }
            Spacer(Modifier.height(24.dp))
            Text(
                "Tu dinero, con rumbo",
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = (-0.56).sp,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(12.dp))
            Text(
                "Crea metas, registra aportes y mira cómo crece tu ahorro. Todo se guarda en tu celular, sin cuentas ni internet.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 15.sp,
                lineHeight = 24.sp,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(28.dp))
            Button(onClick = { start() }, modifier = Modifier.fillMaxWidth()) {
                Text("Empezar a ahorrar")
            }
        }
    }
}
