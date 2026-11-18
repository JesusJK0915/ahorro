package com.jesusjk0915.ahorro

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

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
    MaterialTheme(colorScheme = colors, content = content)
}
