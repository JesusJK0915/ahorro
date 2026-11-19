package com.jesusjk0915.ahorro

import android.content.Context
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import java.io.File

@Composable
fun SwitchRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onChange(!checked) }
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    vm: AppViewModel,
    deviceLockAvailable: Boolean,
    onTestDevice: (onOk: () -> Unit) -> Unit,
    onLockNow: () -> Unit,
    onToast: (String) -> Unit,
) {
    val context = LocalContext.current
    val settings = vm.ui.settings
    var pinSetup by remember { mutableStateOf(false) }
    var firstPin by remember { mutableStateOf("") }
    var patternSetup by remember { mutableStateOf(false) }
    var firstPattern by remember { mutableStateOf<List<Int>>(emptyList()) }

    var checking by remember { mutableStateOf(false) }
    var update by remember { mutableStateOf<UpdateInfo?>(null) }
    var updateError by remember { mutableStateOf(false) }
    var pendingRestore by remember { mutableStateOf<String?>(null) }

    val restoreLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        try {
            val text = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
            if (text != null) pendingRestore = text else onToast("No se pudo leer el respaldo")
        } catch (e: Exception) {
            onToast("No se pudo leer el respaldo")
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp, vertical = 14.dp),
    ) {
        Text("Ajustes", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(16.dp))

        Text("Apariencia", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        ) {
            Column(Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                SwitchRow("Colores del fondo de pantalla", settings.dynamicColor) { vm.setDynamicColor(it) }
                SwitchRow("Cristal líquido", settings.glass) { vm.setGlass(it) }
                if (settings.glass) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Estilo",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = settings.glassStyle == "transparent",
                            onClick = { vm.setGlassStyle("transparent") },
                            label = { Text("Liquido Transparente") },
                        )
                        FilterChip(
                            selected = settings.glassStyle == "opaque",
                            onClick = { vm.setGlassStyle("opaque") },
                            label = { Text("Liquido Opaco") },
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "Transparencia: ${settings.glassTransparency}%",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Slider(
                        value = settings.glassTransparency.toFloat(),
                        onValueChange = { vm.setGlassTransparency(it.toInt()) },
                        valueRange = 0f..100f,
                        enabled = settings.glassStyle == "transparent",
                        modifier = Modifier.fillMaxWidth(),
                    )
                    if (settings.glassStyle == "opaque") {
                        Text(
                            "Liquido Opaco deja el vidrio esmerilado.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                }
                SwitchRow("Ocultar montos", settings.hideAmounts) { vm.setHideAmounts(it) }
                SwitchRow("Quitar nombres de el dock", settings.hideDockLabels) {
                    vm.setHideDockLabels(it)
                }
            }
        }

        Spacer(Modifier.height(18.dp))
        Text("Datos", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        ) {
            Column(Modifier.padding(16.dp)) {
                Text("Estadísticas e historial", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Text(
                    "Borra los movimientos. Tus metas y el ahorro se conservan.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(12.dp))
                OutlinedButton(
                    onClick = {
                        vm.resetStats()
                        onToast("Estadísticas reiniciadas")
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Reiniciar estadísticas")
                }
                if (vm.canUndoStatsReset) {
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = {
                            vm.undoStatsReset()
                            onToast("Estadísticas restauradas")
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Deshacer reinicio")
                    }
                }
                if (settings.statsResetAt > 0L) {
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "Último reinicio: ${fmtDate(settings.statsResetAt)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (vm.canUndoDelete) {
                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = {
                            vm.undoDelete()
                            onToast("Eliminación deshecha")
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Deshacer última eliminación")
                    }
                }
                Spacer(Modifier.height(16.dp))
                Text("Historial en CSV", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Text(
                    "Comparte tus movimientos con otra app. No se sube a ningún servidor.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(10.dp))
                OutlinedButton(
                    onClick = { exportCsv(context, vm.ui.moves, onToast) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Exportar y compartir CSV")
                }
                Spacer(Modifier.height(16.dp))
                Text("Respaldo completo", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Text(
                    "Guarda metas, movimientos y ajustes en un JSON. No se sube a ningún servidor.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(10.dp))
                OutlinedButton(
                    onClick = { exportBackup(context, vm.backupJson(), onToast) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Exportar respaldo JSON")
                }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = { restoreLauncher.launch(arrayOf("application/json", "text/plain", "*/*")) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Restaurar respaldo")
                }
            }
        }

        Spacer(Modifier.height(18.dp))
        Text("Seguridad", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        ) {
            Column(Modifier.padding(16.dp)) {
                SecurityOption("Sin bloqueo", settings.security == "none") { vm.setSecurity("none") }
                SecurityOption("Celular (huella, PIN o patrón)", settings.security == "device") {
                    if (deviceLockAvailable) {
                        onTestDevice {
                            vm.setSecurity("device")
                            onToast("Bloqueo del celular activado")
                        }
                    } else {
                        onToast("Tu celular no tiene bloqueo. Configura un PIN de la app.")
                        pinSetup = true
                    }
                }
                SecurityOption("PIN de la app", settings.security == "pin") {
                    if (settings.pinHash.isEmpty()) pinSetup = true else vm.setSecurity("pin")
                }
                SecurityOption("Patrón de la app", settings.security == "pattern") {
                    if (settings.patternHash.isEmpty()) patternSetup = true else vm.setSecurity("pattern")
                }
                if (settings.security != "none") {
                    SwitchRow("Bloquear al salir de la app", settings.lockOnLeave) {
                        vm.setLockOnLeave(it)
                    }
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(onClick = onLockNow, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.Lock, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Bloquear ahora")
                    }
                }
            }
        }

        Spacer(Modifier.height(18.dp))
        Text("Actualizaciones", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        ) {
            Column(Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.SystemUpdate, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Versión actual", style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(CURRENT_VERSION, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = {
                        checking = true
                        updateError = false
                        UpdateManager.check { info ->
                            checking = false
                            if (info == null) updateError = true else update = info
                        }
                    },
                    enabled = !checking,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(if (checking) "Buscando…" else "Buscar actualizaciones")
                }
                if (updateError) {
                    Spacer(Modifier.height(8.dp))
                    Text("No se pudo consultar. Revisa tu conexión.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error)
                }
                update?.let { info ->
                    Spacer(Modifier.height(10.dp))
                    Text(
                        if (UpdateManager.isNewer(info.version))
                            "Nueva versión ${info.version} disponible"
                        else "Ya tienes la última versión (${info.version})",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    if (info.notes.isNotBlank()) {
                        Text(info.notes, style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (UpdateManager.isNewer(info.version) && info.url.isNotBlank()) {
                        Spacer(Modifier.height(8.dp))
                        Button(onClick = { UpdateManager.startDownload(context, info.url) },
                            modifier = Modifier.fillMaxWidth()) {
                            Text("Descargar e instalar")
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(96.dp))
    }

    if (pinSetup) {
        AlertDialog(
            onDismissRequest = { pinSetup = false; firstPin = "" },
            title = { Text(if (firstPin.isEmpty()) "Crear PIN" else "Confirmar PIN") },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Text(if (firstPin.isEmpty()) "Elige un PIN de 4 dígitos" else "Vuelve a escribirlo")
                    Spacer(Modifier.height(14.dp))
                    PinPad(4) { value ->
                        if (firstPin.isEmpty()) {
                            firstPin = value
                        } else if (value == firstPin) {
                            vm.setPin(value)
                            vm.setSecurity("pin")
                            pinSetup = false
                            firstPin = ""
                            onToast("PIN configurado")
                        } else {
                            firstPin = ""
                            onToast("Los PIN no coinciden, intenta de nuevo")
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { pinSetup = false; firstPin = "" }) { Text("Cancelar") } },
        )
    }

    if (patternSetup) {
        AlertDialog(
            onDismissRequest = { patternSetup = false; firstPattern = emptyList() },
            title = { Text(if (firstPattern.isEmpty()) "Crear patrón" else "Confirmar patrón") },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Text("Marca al menos 4 puntos")
                    Spacer(Modifier.height(10.dp))
                    PatternPad { seq ->
                        if (firstPattern.isEmpty()) {
                            firstPattern = seq
                        } else if (seq == firstPattern) {
                            vm.setPattern(seq)
                            vm.setSecurity("pattern")
                            patternSetup = false
                            firstPattern = emptyList()
                            onToast("Patrón configurado")
                        } else {
                            firstPattern = emptyList()
                            onToast("Los patrones no coinciden, intenta de nuevo")
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { patternSetup = false; firstPattern = emptyList() }) { Text("Cancelar") } },
        )
    }

    pendingRestore?.let { raw ->
        AlertDialog(
            onDismissRequest = { pendingRestore = null },
            title = { Text("Restaurar respaldo") },
            text = {
                Text("Se reemplazarán tus metas, movimientos y ajustes actuales. Esta acción no se puede deshacer.")
            },
            confirmButton = {
                Button(onClick = {
                    val ok = vm.restoreJson(raw)
                    pendingRestore = null
                    onToast(if (ok) "Respaldo restaurado" else "El respaldo no es válido")
                }) { Text("Restaurar") }
            },
            dismissButton = { TextButton(onClick = { pendingRestore = null }) { Text("Cancelar") } },
        )
    }
}

@Composable
private fun SecurityOption(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        if (selected) {
            Icon(Icons.Default.Check, contentDescription = "Seleccionado", tint = MaterialTheme.colorScheme.primary)
        }
    }
}

/** Genera el CSV en el almacenamiento privado y lo comparte con FileProvider (sin red). */
private fun exportCsv(context: Context, moves: List<Move>, onToast: (String) -> Unit) {
    try {
        val sb = StringBuilder("fecha;tipo;meta;monto\n")
        moves.sortedBy { it.ts }.forEach { m ->
            val name = m.name.replace("\"", "\"\"").replace(";", ",")
            sb.append(csvDate(m.ts)).append(';')
                .append(if (m.withdraw) "retiro" else "aporte").append(';')
                .append('"').append(name).append('"').append(';')
                .append(m.amount).append('\n')
        }
        val file = File(context.filesDir, "ahorro_movimientos.csv")
        file.writeText(sb.toString(), Charsets.UTF_8)
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(send, "Compartir historial"))
    } catch (e: Exception) {
        onToast("No se pudo exportar el historial")
    }
}

/** JSON completo en almacenamiento privado y compartido con FileProvider (sin red). */
private fun exportBackup(context: Context, json: String, onToast: (String) -> Unit) {
    try {
        val file = File(context.filesDir, "ahorro_respaldo.json")
        file.writeText(json, Charsets.UTF_8)
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "application/json"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(send, "Compartir respaldo"))
    } catch (e: Exception) {
        onToast("No se pudo exportar el respaldo")
    }
}
