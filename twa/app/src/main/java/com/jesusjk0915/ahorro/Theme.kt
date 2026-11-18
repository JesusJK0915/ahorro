package com.jesusjk0915.ahorro

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

/** Material You del fondo de pantalla, siempre en esquema oscuro. */
@Composable
fun AhorrosTheme(dynamic: Boolean, content: @Composable () -> Unit) {
    val context = LocalContext.current
    val colors = if (dynamic) {
        try {
            dynamicDarkColorScheme(context)
        } catch (e: Exception) {
            darkColorScheme()
        }
    } else {
        darkColorScheme()
    }
    val shapes = Shapes(
        small = RoundedCornerShape(12.dp),
        medium = RoundedCornerShape(20.dp),
        large = RoundedCornerShape(28.dp),
    )
    MaterialTheme(colorScheme = colors, shapes = shapes, content = content)
}
