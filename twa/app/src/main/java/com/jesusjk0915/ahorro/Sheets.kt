package com.jesusjk0915.ahorro

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BaseSheet(title: String, subtitle: String? = null, onDismiss: () -> Unit, content: @Composable () -> Unit) {
    // Entrada elástica del contenido dentro del sheet (rebote tipo iOS).
    val anim = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        anim.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioLowBouncy,
                stiffness = Spring.StiffnessMediumLow,
            ),
        )
    }
    val p = anim.value

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .graphicsLayer {
                    alpha = p.coerceIn(0f, 1f)
                    translationY = (1f - p) * 18f
                    val s = 0.94f + 0.06f * p
                    scaleX = s
                    scaleY = s
                },
        ) {
            Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            if (subtitle != null) {
                Spacer(Modifier.height(4.dp))
                Text(subtitle, style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.height(18.dp))
            content()
            Spacer(Modifier.height(16.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalOptionsSheet(
    goal: Goal,
    onDismiss: () -> Unit,
    onDeposit: () -> Unit,
    onWithdraw: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    BaseSheet(goal.name, "${money(goal.saved)} de ${money(goal.target)}", onDismiss) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(onClick = onDeposit, modifier = Modifier.fillMaxWidth()) { Text("Añadir dinero") }
            OutlinedButton(onClick = onWithdraw, modifier = Modifier.fillMaxWidth()) { Text("Retirar dinero") }
            OutlinedButton(onClick = onEdit, modifier = Modifier.fillMaxWidth()) { Text("Editar meta") }
            OutlinedButton(
                onClick = onDelete,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
            ) { Text("Eliminar meta") }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AmountSheet(
    title: String,
    subtitle: String,
    confirmLabel: String,
    onConfirm: (Long) -> Unit,
    onDismiss: () -> Unit,
) {
    var amount by remember { mutableStateOf("") }
    BaseSheet(title, subtitle, onDismiss) {
        AmountField("Monto", amount) { amount = it }
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text("Cancelar") }
            Button(
                onClick = { onConfirm(amount.toLongOrNull() ?: 0L) },
                enabled = (amount.toLongOrNull() ?: 0L) > 0,
                modifier = Modifier.weight(1f),
            ) { Text(confirmLabel) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalFormSheet(
    goal: Goal?,
    onSave: (String, Long) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by remember { mutableStateOf(goal?.name ?: "") }
    var target by remember { mutableStateOf(if (goal == null) "" else goal.target.toString()) }
    BaseSheet(
        title = if (goal == null) "Nueva meta" else "Editar meta",
        subtitle = if (goal == null) "¿Qué quieres ahorrar?" else "Cambia el nombre o el monto objetivo.",
        onDismiss = onDismiss,
    ) {
        androidx.compose.material3.OutlinedTextField(
            value = name,
            onValueChange = { name = it.take(42) },
            label = { Text("Nombre") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(12.dp))
        AmountField("Monto objetivo", target) { target = it }
        Spacer(Modifier.height(14.dp))
        Text("Montos rápidos", style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(500000L, 1000000L, 5000000L).forEach { v ->
                OutlinedButton(onClick = { target = v.toString() }, modifier = Modifier.weight(1f)) {
                    Text(grouped(v), style = MaterialTheme.typography.labelSmall)
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text("Cancelar") }
            Button(
                onClick = { onSave(name, target.toLongOrNull() ?: 0L) },
                enabled = name.isNotBlank() && (target.toLongOrNull() ?: 0L) > 0,
                modifier = Modifier.weight(1f),
            ) { Text(if (goal == null) "Crear meta" else "Guardar cambios") }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoveOptionsSheet(
    move: Move,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
) {
    BaseSheet(
        title = if (move.withdraw) "Retiro" else "Aporte",
        subtitle = "${money(move.amount)} · ${fmtDate(move.ts)}",
        onDismiss = onDismiss,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(onClick = onEdit, modifier = Modifier.fillMaxWidth()) { Text("Editar monto") }
            OutlinedButton(
                onClick = onDelete,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
            ) { Text("Eliminar movimiento") }
        }
    }
}
