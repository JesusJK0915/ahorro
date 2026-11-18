package com.jesusjk0915.ahorro

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.Canvas
import kotlin.math.abs

private fun pct(g: Goal): Float =
    if (g.target > 0) ((g.saved.toFloat() / g.target.toFloat()) * 100f).coerceIn(0f, 100f) else 0f

@Composable
private fun ProgressRing(value: Float, color: Color, size: Int = 92) {
    val track = MaterialTheme.colorScheme.surfaceVariant
    Canvas(Modifier.size(size.dp)) {
        val stroke = 10f * density
        drawArc(color = track, startAngle = 0f, sweepAngle = 360f, useCenter = false, style = Stroke(stroke))
        drawArc(
            color = color, startAngle = -90f, sweepAngle = 3.6f * value,
            useCenter = false, style = Stroke(stroke, cap = StrokeCap.Round),
        )
    }
}

@Composable
private fun ProgressLine(value: Float, color: Color) {
    val track = MaterialTheme.colorScheme.surfaceVariant
    Canvas(Modifier.fillMaxWidth().height(12.dp)) {
        val radius = CornerRadius(6f * density)
        drawRoundRect(color = track, size = Size(size.width, size.height), cornerRadius = radius)
        val w = size.width * (value / 100f)
        if (w > 0f) {
            drawRoundRect(color = color, size = Size(w, size.height), cornerRadius = radius)
        }
    }
}

@Composable
fun HomeScreen(
    vm: AppViewModel,
    onNewGoal: () -> Unit,
    onGoalClick: (Goal) -> Unit,
    onSettings: () -> Unit,
) {
    val state = vm.ui
    val totalSaved = state.goals.sumOf { it.saved }
    val totalTarget = state.goals.sumOf { it.target }
    val globalPct = if (totalTarget > 0) (totalSaved.toFloat() / totalTarget * 100f).coerceIn(0f, 100f) else 0f

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp, vertical = 14.dp),
    ) {
        Text("Ahorros", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text(
            "Tu dinero, bajo control",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(18.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        "Total ahorrado",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                    Text(
                        money(totalSaved),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                    Text(
                        "de ${money(totalTarget)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
                if (state.settings.ringMode) {
                    ProgressRing(globalPct, MaterialTheme.colorScheme.primary)
                }
            }
            if (!state.settings.ringMode) {
                Box(Modifier.padding(start = 18.dp, end = 18.dp, bottom = 18.dp)) {
                    ProgressLine(globalPct, MaterialTheme.colorScheme.primary)
                }
            }
        }

        Spacer(Modifier.height(18.dp))
        StatsCard(vm = vm)
        Spacer(Modifier.height(18.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Mis metas", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
            Text(
                "${state.goals.size}",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(8.dp))

        if (state.goals.isEmpty()) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Icon(
                        Icons.Default.Savings,
                        contentDescription = null,
                        modifier = Modifier.size(46.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(Modifier.height(12.dp))
                    Text("Aún no tienes metas", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Crea tu primera meta y empieza a guardar dinero hoy.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = onNewGoal) { Text("Crear mi primera meta") }
                    Spacer(Modifier.height(6.dp))
                    OutlinedButton(onClick = onSettings) { Text("Explorar la app") }
                }
            }
        } else {
            state.goals.forEach { g ->
                GoalCard(g = g, ring = state.settings.ringMode, onClick = { onGoalClick(g) })
                Spacer(Modifier.height(10.dp))
            }
            Spacer(Modifier.height(6.dp))
            Button(onClick = onNewGoal, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Nueva meta")
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun GoalCard(g: Goal, ring: Boolean, onClick: () -> Unit) {
    val color = tintAt(g.tint)
    val p = pct(g)
    Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            if (ring) {
                Box(contentAlignment = Alignment.Center) {
                    ProgressRing(p, color, 56)
                    Text("${p.toInt()}%", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.width(14.dp))
            }
            Column(Modifier.weight(1f)) {
                Text(g.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(
                    "${money(g.saved)} de ${money(g.target)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (!ring) {
                    Spacer(Modifier.height(8.dp))
                    ProgressLine(p, color)
                }
            }
            if (ring) {
                Text("${p.toInt()}%", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = color)
            }
        }
    }
}

@Composable
private fun StatsCard(vm: AppViewModel) {
    val moves = vm.ui.moves
    val key = monthKey(System.currentTimeMillis())
    val inMes = moves.filter { !it.withdraw && monthKey(it.ts) == key }
    val outMes = moves.filter { it.withdraw && monthKey(it.ts) == key }
    val buckets = last7(moves)
    val calendar = monthCalendar(moves)
    val maxNet = buckets.maxOf { abs(it.net) }.coerceAtLeast(1)

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Estadísticas", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                Text(
                    "Este mes",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatBox("Ahorrado", money(inMes.sumOf { it.amount }), Modifier.weight(1f))
                StatBox("Retirado", money(outMes.sumOf { it.amount }), Modifier.weight(1f))
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatBox("Aportes", "${inMes.size}", Modifier.weight(1f))
                StatBox("Retiros", "${outMes.size}", Modifier.weight(1f))
            }
            Spacer(Modifier.height(18.dp))
            Text("Últimos 7 días", style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                buckets.forEach { b ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        val h = (abs(b.net).toFloat() / maxNet.toFloat() * 60f).dp.coerceAtLeast(3.dp)
                        Box(Modifier.height(64.dp), contentAlignment = Alignment.BottomCenter) {
                            Box(
                                Modifier
                                    .width(16.dp)
                                    .height(h)
                                    .background(
                                        if (b.net < 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                                        RoundedCornerShape(6.dp),
                                    )
                            )
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(b.label, style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            Spacer(Modifier.height(18.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Aportes del mes", style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
                Text(calendar.first, style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.height(8.dp))
            val week = listOf("L", "M", "M", "J", "V", "S", "D")
            Row(Modifier.fillMaxWidth()) {
                week.forEach {
                    Text(it, style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                }
            }
            Spacer(Modifier.height(4.dp))
            val cells: List<CalDay?> = List(calendar.second) { null } + calendar.third
            for (chunk in cells.chunked(7)) {
                Row(Modifier.fillMaxWidth()) {
                    chunk.forEach { day ->
                        Box(Modifier.weight(1f).padding(2.dp), contentAlignment = Alignment.Center) {
                            if (day != null) {
                                val maxDay = calendar.third.maxOf { it.amount }.coerceAtLeast(1)
                                val level = if (day.amount <= 0) 0 else
                                    ((day.amount.toFloat() / maxDay.toFloat()) * 4).toInt().coerceIn(1, 4)
                                val bg = when (level) {
                                    0 -> MaterialTheme.colorScheme.surfaceVariant
                                    1 -> MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)
                                    2 -> MaterialTheme.colorScheme.primary.copy(alpha = 0.42f)
                                    3 -> MaterialTheme.colorScheme.primary.copy(alpha = 0.68f)
                                    else -> MaterialTheme.colorScheme.primary
                                }
                                Box(
                                    Modifier.size(30.dp).background(bg, RoundedCornerShape(8.dp)),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text("${day.day}", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatBox(label: String, value: String, modifier: Modifier) {
    Card(modifier = modifier) {
        Column(Modifier.padding(12.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        }
    }
}
