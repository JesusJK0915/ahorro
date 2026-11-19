package com.jesusjk0915.ahorro

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import kotlin.math.sin
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Canvas
import java.util.Calendar
import kotlin.math.abs

private fun pct(g: Goal): Float =
    if (g.target > 0) ((g.saved.toFloat() / g.target.toFloat()) * 100f).coerceIn(0f, 100f) else 0f

/**
 * Ola del hero, igual a la versión web: un rastro continuo y un tramo lleno
 * cuyo ancho sube o baja con el porcentaje (spring, como el CSS de la web).
 * Se dibuja la curva completa hasta el ancho exacto: sin recortes que parpadeen.
 */
@Composable
private fun WaveProgress(value: Float, color: Color) {
    val infinite = rememberInfiniteTransition(label = "wave")
    val phase by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "wavePhase",
    )
    val fill by animateFloatAsState(
        targetValue = value,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMediumLow,
        ),
        label = "waveFill",
    )

    Canvas(Modifier.fillMaxWidth().height(18.dp)) {
        val center = size.height / 2f
        val amp = size.height * 0.17f
        val period = 44f * density
        val shift = phase * period
        val stroke = Stroke(width = size.height * 0.58f, cap = StrokeCap.Round)
        val step = 2f * density

        fun yAt(x: Float): Float = center + sin((x + shift) / period * 6.2831853f) * amp

        fun wave(endX: Float): Path {
            val path = Path()
            var x = 0f
            path.moveTo(x, yAt(x))
            while (x < endX) {
                x = (x + step).coerceAtMost(endX)
                path.lineTo(x, yAt(x))
            }
            return path
        }

        drawPath(wave(size.width), color = color.copy(alpha = 0.20f), style = stroke)
        val w = size.width * (fill / 100f)
        if (w > 0.5f) {
            drawPath(wave(w), color = color, style = stroke)
        }
    }
}

@Composable
fun HomeScreen(
    vm: AppViewModel,
    onNewGoal: () -> Unit,
    onGoalClick: (Goal) -> Unit,
) {
    val state = vm.ui
    val settings = state.settings
    val goals = state.goals.sortedByDescending { it.id == settings.featuredGoalId }
    val totalSaved = state.goals.sumOf { it.saved }
    val totalTarget = state.goals.sumOf { it.target }
    val globalPct = if (totalTarget > 0) (totalSaved.toFloat() / totalTarget * 100f).coerceIn(0f, 100f) else 0f
    val animatedPct by animateFloatAsState(
        targetValue = globalPct,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMediumLow,
        ),
        label = "heroPct",
    )
    val onHero = MaterialTheme.colorScheme.onPrimaryContainer
    val heroGlowAnim = rememberInfiniteTransition(label = "heroGlow")
    val heroGlow by heroGlowAnim.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.18f,
        animationSpec = infiniteRepeatable(tween(6500), RepeatMode.Reverse),
        label = "heroGlowPulse",
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp, vertical = 14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .background(
                        Brush.linearGradient(
                            listOf(
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.colorScheme.primaryContainer,
                            ),
                        ),
                        RoundedCornerShape(13.dp),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Default.Savings,
                    contentDescription = null,
                    modifier = Modifier.size(21.dp),
                    tint = MaterialTheme.colorScheme.onPrimary,
                )
            }
            Spacer(Modifier.width(10.dp))
            Text(
                "Ahorros",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
            )
        }
        Spacer(Modifier.height(14.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(32.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.linearGradient(
                            listOf(
                                MaterialTheme.colorScheme.primaryContainer,
                                MaterialTheme.colorScheme.primary,
                            ),
                        ),
                        RoundedCornerShape(32.dp),
                    ),
            ) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 60.dp, y = (-100).dp)
                        .size(240.dp)
                        .background(
                            Brush.radialGradient(
                                listOf(Color.White.copy(alpha = 0.28f), Color.Transparent),
                            ),
                        )
                        .graphicsLayer { scaleX = heroGlow; scaleY = heroGlow },
                )
                Column(modifier = Modifier.fillMaxWidth().padding(22.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            "Ahorro total",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = onHero.copy(alpha = 0.78f),
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { vm.setHideAmounts(!settings.hideAmounts) }) {
                                Icon(
                                    if (settings.hideAmounts) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = if (settings.hideAmounts) "Mostrar montos" else "Ocultar montos",
                                    tint = onHero,
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .background(Color.Black.copy(alpha = 0.28f), RoundedCornerShape(50)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    "${animatedPct.toInt()}%",
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = onHero,
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(
                        money(totalSaved, settings.hideAmounts),
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = onHero,
                    )
                    Spacer(Modifier.height(14.dp))
                    WaveProgress(globalPct, onHero)
                    Spacer(Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            "${state.goals.size} metas",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = onHero.copy(alpha = 0.8f),
                        )
                        Text(
                            "de ${money(totalTarget, settings.hideAmounts)}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = onHero.copy(alpha = 0.8f),
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(18.dp))
        StatsCard(vm = vm, hide = settings.hideAmounts)
        Spacer(Modifier.height(18.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Mis metas",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onNewGoal) {
                Text("Nueva meta", fontWeight = FontWeight.Bold)
            }
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
                    Text("Sin metas todavía", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Crea tu primera meta y mira cómo crece el progreso.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = onNewGoal) { Text("Crear mi primera meta") }
                }
            }
        } else {
            goals.forEach { g ->
                GoalCard(
                    g = g,
                    hide = settings.hideAmounts,
                    featured = g.id == settings.featuredGoalId,
                    onClick = { onGoalClick(g) },
                )
                Spacer(Modifier.height(10.dp))
            }
        }
        Spacer(Modifier.height(96.dp))
    }
}

@Composable
private fun GoalCard(g: Goal, hide: Boolean = false, featured: Boolean = false, onClick: () -> Unit) {
    val color = tintAt(g.tint)
    val p = pct(g)
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { shown = true }
    val bounce = spring<Float>(
        dampingRatio = Spring.DampingRatioLowBouncy,
        stiffness = Spring.StiffnessMediumLow,
    )
    val enter by animateFloatAsState(if (shown) 1f else 0f, bounce, label = "goalEnter")
    val rise by animateDpAsState(
        targetValue = if (shown) 0.dp else 16.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMediumLow,
        ),
        label = "goalRise",
    )
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .offset(y = rise)
            .graphicsLayer {
                alpha = enter.coerceIn(0f, 1f)
                val s = 0.94f + 0.06f * enter
                scaleX = s
                scaleY = s
            },
        elevation = CardDefaults.cardElevation(
            defaultElevation = 6.dp,
            hoveredElevation = 10.dp,
            focusedElevation = 10.dp,
        ),
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        g.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    if (featured) {
                        Spacer(Modifier.width(6.dp))
                        Text("★", style = MaterialTheme.typography.titleMedium, color = color)
                    }
                }
                Text(
                    "${money(g.saved, hide)} de ${money(g.target, hide)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(10.dp))
                WaveProgress(p, color)
            }
            Spacer(Modifier.width(12.dp))
            Text(
                "${p.toInt()}%",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = color,
            )
        }
    }
}

@Composable
private fun StatsCard(vm: AppViewModel, hide: Boolean = false) {
    val moves = vm.ui.moves
    val completed = vm.ui.goals.count { it.target > 0 && it.saved >= it.target }
    val streak = savingsStreak(moves)
    var selectedDay by remember { mutableStateOf<Int?>(null) }
    val key = monthKey(System.currentTimeMillis())
    val inMes = moves.filter { !it.withdraw && monthKey(it.ts) == key }
    val outMes = moves.filter { it.withdraw && monthKey(it.ts) == key }
    val buckets = last7(moves)
    val calendar = monthCalendar(moves)
    val maxNet = buckets.maxOf { abs(it.net) }.coerceAtLeast(1)
    val today = Calendar.getInstance().get(Calendar.DAY_OF_MONTH)

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 6.dp,
            hoveredElevation = 10.dp,
            focusedElevation = 10.dp,
        ),
    ) {
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
                StatBox("Ahorrado", money(inMes.sumOf { it.amount }, hide), Modifier.weight(1f))
                StatBox("Retirado", money(outMes.sumOf { it.amount }, hide), Modifier.weight(1f))
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatBox("Aportes", "${inMes.size}", Modifier.weight(1f))
                StatBox("Retiros", "${outMes.size}", Modifier.weight(1f))
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatBox("Metas cumplidas", "$completed", Modifier.weight(1f))
                StatBox("Racha", "$streak días", Modifier.weight(1f))
            }
            Spacer(Modifier.height(18.dp))
            Text("Últimos 7 días", style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                buckets.forEachIndexed { idx, b ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.clickable {
                            selectedDay = if (selectedDay == idx) null else idx
                        },
                    ) {
                        val targetH = (abs(b.net).toFloat() / maxNet.toFloat() * 60f).dp.coerceAtLeast(3.dp)
                        val h by animateDpAsState(targetH, spring(
                            dampingRatio = Spring.DampingRatioLowBouncy,
                            stiffness = Spring.StiffnessMediumLow,
                        ), label = "bar${b.label}")
                        val barColor = if (b.net < 0) MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.primary
                        Box(
                            Modifier
                                .height(64.dp)
                                .graphicsLayer {
                                    alpha = if (selectedDay == null || selectedDay == idx) 1f else 0.4f
                                },
                            contentAlignment = Alignment.BottomCenter,
                        ) {
                            Box(
                                Modifier
                                    .width(16.dp)
                                    .height(h)
                                    .shadow(
                                        4.dp,
                                        RoundedCornerShape(8.dp),
                                        ambientColor = Color.Black.copy(alpha = 0.35f),
                                        spotColor = barColor.copy(alpha = 0.4f),
                                    )
                                    .background(
                                        Brush.verticalGradient(
                                            listOf(barColor, barColor.copy(alpha = 0.45f)),
                                        ),
                                        RoundedCornerShape(8.dp),
                                    )
                            )
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(b.label, style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = if (selectedDay == idx) FontWeight.ExtraBold else FontWeight.Normal)
                    }
                }
            }
            val chosen = selectedDay?.let { buckets.getOrNull(it) }
            Spacer(Modifier.height(6.dp))
            Text(
                if (chosen == null) "Toca una barra para ver el detalle del día"
                else "${chosen.label}: ${money(chosen.net, hide)}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
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
                                val cellShape = RoundedCornerShape(9.dp)
                                Box(
                                    Modifier
                                        .size(30.dp)
                                        .background(bg, cellShape)
                                        .then(
                                            if (day.day == today) {
                                                Modifier.border(
                                                    1.5.dp,
                                                    MaterialTheme.colorScheme.primary,
                                                    cellShape,
                                                )
                                            } else Modifier,
                                        ),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        "${day.day}",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = if (day.day == today) FontWeight.ExtraBold else FontWeight.Normal,
                                    )
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
    Card(
        modifier = modifier,
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
    ) {
        Column(Modifier.padding(12.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        }
    }
}
