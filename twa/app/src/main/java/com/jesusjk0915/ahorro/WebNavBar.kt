package com.jesusjk0915.ahorro

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Barra flotante tipo píldora de la versión web: cristal líquido y un
 * indicador que se desliza suavemente entre las tres pestañas.
 */
@Composable
fun WebNavBar(settings: Settings, tab: Int, onTab: (Int) -> Unit) {
    val cs = MaterialTheme.colorScheme
    val density = LocalDensity.current
    var pillWidth by remember { mutableStateOf(0.dp) }

    val inner = (pillWidth - 14.dp).coerceAtLeast(0.dp)
    val itemW = inner / 3f
    val target = itemW * tab.toFloat()
    val indicatorX by animateDpAsState(
        targetValue = target,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "navIndicator",
    )

    val items = listOf(
        Triple("Inicio", Icons.Default.Savings, 0),
        Triple("Historial", Icons.Default.History, 1),
        Triple("Ajustes", Icons.Default.Settings, 2),
    )

    Box(
        modifier = Modifier.fillMaxWidth().padding(bottom = 14.dp),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Box(
            modifier = Modifier
                .padding(horizontal = 14.dp)
                .height(58.dp)
                .onSizeChanged { pillWidth = with(density) { it.width.toDp() } }
                .liquidGlass(settings, shape = RoundedCornerShape(50)),
            contentAlignment = Alignment.CenterStart,
        ) {
            if (itemW > 0.dp) {
                Box(
                    modifier = Modifier
                        .offset(x = indicatorX, y = 7.dp)
                        .size(width = itemW, height = 44.dp)
                        .background(cs.primaryContainer, RoundedCornerShape(50)),
                )
            }
            Row(modifier = Modifier.fillMaxSize()) {
                items.forEach { (label, icon, index) ->
                    val active = tab == index
                    val tint = if (active) cs.onPrimaryContainer else cs.onSurfaceVariant
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onTab(index) },
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp), tint = tint)
                        Text(label, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = tint)
                    }
                }
            }
        }
    }
}
