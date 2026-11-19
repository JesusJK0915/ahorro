package com.jesusjk0915.ahorro

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.item
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@Composable
fun DropdownField(label: String, options: List<String>, selected: String, onSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = Modifier.fillMaxWidth()) {
        Column {
            Text(
                label.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp, bottom = 4.dp),
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainer)
                    .clickable { expanded = true }
                    .padding(horizontal = 14.dp, vertical = 13.dp),
            ) {
                Text(
                    selected,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
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
    val hide = state.settings.hideAmounts
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
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Historial",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
            )
            Text(
                if (moves.isEmpty()) "" else "${moves.size} mov.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
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
            val groups = moves.groupBy { dayKey(it.ts) }.entries.sortedByDescending { it.key }
            LazyColumn(verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)) {
                groups.forEach { (day, ofDay) ->
                    item(key = "header_$day") { DayHeader(day, ofDay, hide) }
                    items(ofDay, key = { "move_" + it.id }) { m ->
                        MoveRow(m, hide, onMoveClick)
                    }
                }
            }
        }
        Spacer(Modifier.height(96.dp))
    }
}

/** Encabezado por día con subtotal del grupo. */
@Composable
private fun DayHeader(day: String, ofDay: List<Move>, hide: Boolean) {
    val ts = ofDay.firstOrNull()?.ts ?: 0L
    val subtotal = ofDay.sumOf { if (it.withdraw) -it.amount else it.amount }
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            fmtDay(ts),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.weight(1f),
        )
        Text(
            "${ofDay.size} mov. · ${money(subtotal, hide)}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun MoveRow(m: Move, hide: Boolean = false, onClick: (Move) -> Unit) {
    val color = if (m.withdraw) MaterialTheme.colorScheme.error else tintAt(m.tint)
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { shown = true }
    val bounce = spring<Float>(
        dampingRatio = Spring.DampingRatioLowBouncy,
        stiffness = Spring.StiffnessMediumLow,
    )
    val enter by animateFloatAsState(if (shown) 1f else 0f, bounce, label = "moveEnter")
    val rise by animateFloatAsState(if (shown) 0f else 14f, bounce, label = "moveRise")
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick(m) }
            .graphicsLayer {
                alpha = enter.coerceIn(0f, 1f)
                translationY = rise
                val s = 0.96f + 0.04f * enter
                scaleX = s
                scaleY = s
            },
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 4.dp,
            hoveredElevation = 8.dp,
            focusedElevation = 8.dp,
        ),
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .width(3.dp)
                    .height(34.dp)
                    .background(color, RoundedCornerShape(50)),
            )
            Spacer(Modifier.width(10.dp))
            Box(
                Modifier.size(40.dp).background(
                    Brush.linearGradient(
                        listOf(color.copy(alpha = 0.24f), color.copy(alpha = 0.08f)),
                    ),
                    RoundedCornerShape(14.dp),
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
                "${if (m.withdraw) "-" else "+"}${money(m.amount, hide)}",
                style = MaterialTheme.typography.titleSmall,
                color = color,
            )
            Spacer(Modifier.width(6.dp))
            Box(
                Modifier
                    .size(28.dp)
                    .background(
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f),
                        RoundedCornerShape(10.dp),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Default.MoreHoriz,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}
