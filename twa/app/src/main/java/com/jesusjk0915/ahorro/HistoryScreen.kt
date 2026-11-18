package com.jesusjk0915.ahorro

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@Composable
fun DropdownField(label: String, options: List<String>, selected: String, onSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = Modifier.fillMaxWidth()) {
        OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
            Text(
                "$label: $selected",
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.fillMaxWidth(),
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onSelect(option)
                        expanded = false
                    },
                )
            }
        }
    }
}

@Composable
fun HistoryScreen(vm: AppViewModel, onMoveClick: (Move) -> Unit) {
    val state = vm.ui
    var goalLabel by remember { mutableStateOf("Todas las metas") }
    var period by remember { mutableStateOf("Todo el tiempo") }

    val goalLabels = listOf("Todas las metas") + state.goals.map { it.name }
    if (goalLabel != "Todas las metas" && state.goals.none { it.name == goalLabel }) {
        goalLabel = "Todas las metas"
    }
    val goalId = state.goals.firstOrNull { it.name == goalLabel }?.id

    val periods = listOf("Todo el tiempo", "Últimos 7 días", "Últimos 30 días", "Este mes")
    val now = System.currentTimeMillis()
    val month = monthKey(now)
    val moves = state.moves.filter { m ->
        if (goalId != null && m.goalId != goalId) return@filter false
        when (period) {
            "Últimos 7 días" -> now - m.ts <= 7 * 86_400_000L
            "Últimos 30 días" -> now - m.ts <= 30 * 86_400_000L
            "Este mes" -> monthKey(m.ts) == month
            else -> true
        }
    }.take(80)

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 18.dp, vertical = 14.dp)) {
        Text("Historial", style = MaterialTheme.typography.headlineMedium)
        Text(
            "Todos tus aportes y retiros",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(16.dp))

        DropdownField("Meta", goalLabels, goalLabel) { goalLabel = it }
        Spacer(Modifier.height(8.dp))
        DropdownField("Periodo", periods, period) { period = it }
        Spacer(Modifier.height(12.dp))

        if (moves.isEmpty()) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Icon(
                        Icons.Default.History,
                        contentDescription = null,
                        modifier = Modifier.size(44.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        if (state.moves.isEmpty()) "Aún no hay movimientos" else "Sin movimientos con este filtro",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        if (state.moves.isEmpty()) "Cada aporte que hagas aparecerá aquí."
                        else "Prueba con otra meta o con otro periodo.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        } else {
            Text(
                "${moves.size} movimientos",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(6.dp))
            LazyColumn(verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)) {
                items(moves, key = { it.id }) { m ->
                    MoveRow(m, onMoveClick)
                }
            }
        }
        Spacer(Modifier.height(20.dp))
    }
}

@Composable
private fun MoveRow(m: Move, onClick: (Move) -> Unit) {
    val color = if (m.withdraw) MaterialTheme.colorScheme.error else tintAt(m.tint)
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick(m) },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(40.dp).androidx.compose.foundation.background(
                    color.copy(alpha = 0.15f),
                    androidx.compose.foundation.shape.RoundedCornerShape(14.dp),
                ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    if (m.withdraw) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(20.dp),
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(m.name, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    fmtDate(m.ts),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                "${if (m.withdraw) "-" else "+"}${money(m.amount)}",
                style = MaterialTheme.typography.titleSmall,
                color = color,
            )
            Spacer(Modifier.width(6.dp))
            Icon(
                Icons.Default.MoreHoriz,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}
