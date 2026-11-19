package com.jesusjk0915.ahorro

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

private data class Achievement(
    val icon: String,
    val title: String,
    val detail: String,
    val unlocked: Boolean,
)

/** Pantalla de logros: insignias que se desbloquean con el uso real de la app. */
@Composable
fun AchievementsScreen(vm: AppViewModel) {
    val goals = vm.ui.goals
    val moves = vm.ui.moves
    val completed = goals.count { it.target > 0 && it.saved >= it.target }
    val streak = savingsStreak(moves)
    val deposits = moves.count { !it.withdraw }
    val totalSaved = goals.sumOf { it.saved }

    val badges = listOf(
        Achievement("🌱", "Primer paso", "Haz tu primer aporte", deposits >= 1),
        Achievement("💰", "Ahorrador", "Ahorra 100.000 en total", totalSaved >= 100_000L),
        Achievement("🏆", "Meta cumplida", "Completa una meta", completed >= 1),
        Achievement("🔥", "Racha de 3", "Aporta 3 días seguidos", streak >= 3),
        Achievement("⭐", "Racha de 7", "Aporta 7 días seguidos", streak >= 7),
        Achievement("🎯", "Cinco metas", "Crea 5 metas", goals.size >= 5),
    )
    val unlocked = badges.count { it.unlocked }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp, vertical = 14.dp),
    ) {
        Text("Logros", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(6.dp))
        Text(
            "$unlocked de ${badges.size} insignias desbloqueadas",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(18.dp))

        for (chunk in badges.chunked(2)) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                chunk.forEach { badge ->
                    BadgeCard(badge, Modifier.weight(1f))
                }
            }
            Spacer(Modifier.height(10.dp))
        }
        Spacer(Modifier.height(96.dp))
    }
}

@Composable
private fun BadgeCard(badge: Achievement, modifier: Modifier) {
    Card(
        modifier = modifier.graphicsLayer { alpha = if (badge.unlocked) 1f else 0.5f },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(badge.icon, style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(6.dp))
            Text(
                badge.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                badge.detail,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                if (badge.unlocked) "Desbloqueada" else "Bloqueada",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = if (badge.unlocked) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
