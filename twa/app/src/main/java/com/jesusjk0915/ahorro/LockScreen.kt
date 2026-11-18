package com.jesusjk0915.ahorro

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun LockScreen(
    method: String,
    deviceAvailable: Boolean,
    hasPin: Boolean,
    hasPattern: Boolean,
    onDeviceAuth: () -> Unit,
    onCheckPin: (String) -> Boolean,
    onCheckPattern: (List<Int>) -> Boolean,
    onSuccess: () -> Unit,
    onToast: (String) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(48.dp))
        Icon(
            Icons.Default.Lock,
            contentDescription = null,
            modifier = Modifier.size(56.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.height(18.dp))
        Text(
            "Autenticación",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            "Desbloquea tu app de Ahorros",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(32.dp))

        when {
            method == "device" && deviceAvailable -> {
                Icon(
                    Icons.Default.Fingerprint,
                    contentDescription = null,
                    modifier = Modifier.size(72.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.height(24.dp))
                Button(onClick = onDeviceAuth) { Text("Autenticación") }
                Spacer(Modifier.height(10.dp))
                InfoLine("Usa tu huella, PIN, patrón o contraseña del celular.")
            }

            method == "device" && !deviceAvailable && hasPin -> {
                InfoLine("Tu celular no tiene bloqueo. Usa el PIN de la app.")
                Spacer(Modifier.height(18.dp))
                PinPad { value ->
                    if (onCheckPin(value)) onSuccess() else onToast("PIN incorrecto")
                }
            }

            method == "device" && !deviceAvailable && hasPattern -> {
                InfoLine("Tu celular no tiene bloqueo. Usa el patrón de la app.")
                Spacer(Modifier.height(18.dp))
                PatternPad { seq ->
                    if (onCheckPattern(seq)) onSuccess() else onToast("Patrón incorrecto")
                }
            }

            method == "device" && !deviceAvailable -> {
                InfoLine("Tu celular no tiene bloqueo y aún no configuraste un PIN o patrón de la app.")
            }

            method == "pin" -> {
                PinPad { value ->
                    if (onCheckPin(value)) onSuccess() else onToast("PIN incorrecto")
                }
            }

            method == "pattern" -> {
                PatternPad { seq ->
                    if (onCheckPattern(seq)) onSuccess() else onToast("Patrón incorrecto")
                }
            }

            else -> {
                Button(onClick = onSuccess) { Text("Entrar") }
            }
        }
    }
}
