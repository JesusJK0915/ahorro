package com.jesusjk0915.ahorro

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.padding(vertical = 8.dp),
    )
}

@Composable
fun AmountField(label: String, value: String, onChange: (String) -> Unit) {
    val digits = value.filter { it.isDigit() }
    val shown = try {
        if (digits.isEmpty()) "" else grouped(digits.toLong())
    } catch (e: NumberFormatException) {
        digits
    }
    OutlinedTextField(
        value = shown,
        onValueChange = { text -> onChange(text.filter { it.isDigit() }) },
        label = { Text(label) },
        prefix = { Text("\$  ") },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
fun ConfirmDialog(
    title: String,
    text: String,
    confirmLabel: String = "Eliminar",
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(text) },
        confirmButton = { Button(onClick = onConfirm) { Text(confirmLabel) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )
}

@Composable
fun PinPad(length: Int = 4, onComplete: (String) -> Unit) {
    var value by remember { mutableStateOf("") }
    val primary = MaterialTheme.colorScheme.primary
    val onPrimary = MaterialTheme.colorScheme.onPrimary

    fun handle(key: String) {
        if (key == "⌫") {
            value = if (value.length > 1) value.dropLast(1) else ""
            return
        }
        if (value.length < length) {
            val next = value + key
            value = next
            if (next.length == length) {
                onComplete(next)
                value = ""
            }
        }
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            repeat(length) { i ->
                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .clip(CircleShape)
                        .background(
                            if (i < value.length) primary
                            else MaterialTheme.colorScheme.surfaceVariant
                        ),
                )
            }
        }
        Spacer(Modifier.height(22.dp))
        for (row in 0..2) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                for (col in 0..2) {
                    val n = row * 3 + col + 1
                    PinKey(n.toString(), primary, onPrimary) { handle(it) }
                }
            }
            Spacer(Modifier.height(12.dp))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.size(64.dp))
            PinKey("0", primary, onPrimary) { handle(it) }
            PinKey("⌫", primary, onPrimary) { handle(it) }
        }
    }
}

@Composable
private fun PinKey(label: String, bg: Color, fg: Color, onClick: (String) -> Unit) {
    Box(
        modifier = Modifier
            .size(64.dp)
            .clip(CircleShape)
            .background(bg)
            .clickable { onClick(label) },
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = fg, style = MaterialTheme.typography.titleLarge)
    }
}

/** Patrón de 3x3 por toque: se marcan los puntos en orden y se confirma. */
@Composable
fun PatternPad(onComplete: (List<Int>) -> Unit) {
    var seq by remember { mutableStateOf(listOf<Int>()) }
    val primary = MaterialTheme.colorScheme.primary
    val outline = MaterialTheme.colorScheme.outline

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(216.dp)) {
            Canvas(Modifier.matchParentSize()) {
                val cell = 72f * density
                val half = 48f * density
                for (i in 1 until seq.size) {
                    val a = seq[i - 1]
                    val b = seq[i]
                    drawLine(
                        color = primary,
                        start = Offset((a % 3) * cell + half, (a / 3) * cell + half),
                        end = Offset((b % 3) * cell + half, (b / 3) * cell + half),
                        strokeWidth = 8f,
                        cap = StrokeCap.Round,
                    )
                }
            }
            repeat(9) { i ->
                val on = seq.contains(i)
                val xDp = ((i % 3) * 72 + 24).dp
                val yDp = ((i / 3) * 72 + 24).dp
                Box(
                    modifier = Modifier
                        .offset(x = xDp, y = yDp)
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(if (on) primary else MaterialTheme.colorScheme.surfaceVariant)
                        .border(2.dp, if (on) primary else outline, CircleShape)
                        .clickable {
                            seq = if (on) seq - i else seq + i
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    if (on) Text("${seq.indexOf(i) + 1}", color = MaterialTheme.colorScheme.onPrimary)
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        Button(
            onClick = { if (seq.size >= 4) onComplete(seq) },
            enabled = seq.size >= 4,
        ) {
            Text("Confirmar patrón (${seq.size})")
        }
        TextButton(onClick = { seq = emptyList() }) { Text("Limpiar") }
    }
}

@Composable
fun InfoLine(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = Modifier.padding(horizontal = 16.dp),
    )
}
