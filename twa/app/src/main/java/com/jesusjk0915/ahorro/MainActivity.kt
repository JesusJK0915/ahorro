package com.jesusjk0915.ahorro

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageInstaller
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.os.Vibrator
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

private sealed interface SheetState {
    object NewGoal : SheetState
    data class EditGoal(val goal: Goal) : SheetState
    data class GoalOptions(val goal: Goal) : SheetState
    data class Deposit(val goal: Goal) : SheetState
    data class Withdraw(val goal: Goal) : SheetState
    data class MoveOptions(val move: Move) : SheetState
    data class EditMove(val move: Move) : SheetState
}

private data class ConfirmState(val title: String, val text: String, val action: () -> Unit)

class MainActivity : FragmentActivity() {
    private val vm: AppViewModel by viewModels()
    private var downloadReceiver: BroadcastReceiver? = null

    /** Evita que el prompt del sistema dispare un segundo bloqueo al volver del background. */
    private var authInProgress = false

    override fun attachBaseContext(newBase: Context) {
        // Fuente del sistema limitada a un máximo del 110%.
        val scale = newBase.resources.configuration.fontScale
        val config = Configuration(newBase.resources.configuration)
        config.fontScale = minOf(scale, 1.10f)
        super.attachBaseContext(newBase.createConfigurationContext(config))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        registerDownloadReceiver()
        handleInstallIntent(intent)

        val deviceLockAvailable = BiometricManager.from(this).canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_WEAK or
                BiometricManager.Authenticators.DEVICE_CREDENTIAL
        ) == BiometricManager.BIOMETRIC_SUCCESS

        val lockedState = mutableStateOf(vm.ui.settings.security != "none")

        setContent {
            val locked by lockedState
            AhorrosTheme(dynamic = vm.ui.settings.dynamicColor) {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    if (locked) {
                        LockScreen(
                            method = vm.ui.settings.security,
                            deviceAvailable = deviceLockAvailable,
                            hasPin = vm.ui.settings.pinHash.isNotEmpty(),
                            hasPattern = vm.ui.settings.patternHash.isNotEmpty(),
                            onDeviceAuth = {
                                deviceAuth { ok ->
                                    if (ok) lockedState.value = false
                                    else toast("No se pudo autenticar")
                                }
                            },
                            onCheckPin = { vm.checkPin(it) },
                            onCheckPattern = { vm.checkPattern(it) },
                            onSuccess = { lockedState.value = false },
                            onToast = { toast(it) },
                        )
                    } else {
                        AppContent(
                            vm = vm,
                            onToast = { toast(it) },
                            onTestDevice = { onOk ->
                                deviceAuth { ok ->
                                    if (ok) onOk() else toast("No se pudo autenticar")
                                }
                            },
                            onLockNow = { lockedState.value = true },
                            deviceLockAvailable = deviceLockAvailable,
                        )
                    }
                }
            }
        }
    }

    private fun toast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        buzz()
    }

    private fun buzz(ms: Long = 15) {
        try {
            val v = getSystemService(Vibrator::class.java)
            v?.vibrate(ms)
        } catch (e: Exception) {
            // Algunos dispositivos no tienen vibrador.
        }
    }

    private fun deviceAuth(onResult: (Boolean) -> Unit) {
        val authenticators = BiometricManager.Authenticators.BIOMETRIC_WEAK or
            BiometricManager.Authenticators.DEVICE_CREDENTIAL
        if (BiometricManager.from(this).canAuthenticate(authenticators) != BiometricManager.BIOMETRIC_SUCCESS) {
            onResult(false)
            return
        }
        authInProgress = true
        try {
            val info = BiometricPrompt.PromptInfo.Builder()
                .setTitle("Autenticación")
                .setSubtitle("Desbloquea tu celular para continuar")
                .setAllowedAuthenticators(authenticators)
                .build()
            val prompt = BiometricPrompt(
                this,
                ContextCompat.getMainExecutor(this),
                object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                        authInProgress = false
                        onResult(true)
                    }

                    override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                        authInProgress = false
                        onResult(false)
                    }
                },
            )
            prompt.authenticate(info)
        } catch (e: Exception) {
            authInProgress = false
            onResult(false)
        }
    }

    private fun handleInstallIntent(intent: Intent?) {
        if (intent?.action != "ahorro.INSTALL_RESULT") return
        val status = intent.getIntExtra(
            PackageInstaller.EXTRA_STATUS,
            PackageInstaller.STATUS_FAILURE,
        )
        if (status == PackageInstaller.STATUS_SUCCESS) {
            toast("Actualización instalada ✅")
        } else {
            toast("No se pudo instalar la actualización")
        }
    }

    private fun registerDownloadReceiver() {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                Installer.handle(context, intent)
            }
        }
        val filter = IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(receiver, filter, Context.RECEIVER_EXPORTED)
        } else {
            registerReceiver(receiver, filter)
        }
        downloadReceiver = receiver
    }

    override fun onDestroy() {
        downloadReceiver?.let {
            try {
                unregisterReceiver(it)
            } catch (e: Exception) {
            }
        }
        downloadReceiver = null
        super.onDestroy()
    }
}

@Composable
private fun AppContent(
    vm: AppViewModel,
    onToast: (String) -> Unit,
    onTestDevice: (onOk: () -> Unit) -> Unit,
    onLockNow: () -> Unit,
    deviceLockAvailable: Boolean,
) {
    var tab by remember { mutableIntStateOf(0) }
    var sheet by remember { mutableStateOf<SheetState?>(null) }
    var confirm by remember { mutableStateOf<ConfirmState?>(null) }

    Scaffold(
        bottomBar = {
            val dockSettings = vm.ui.settings
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 8.dp)
                    .liquidGlass(dockSettings, shape = RoundedCornerShape(28.dp)),
            ) {
                NavigationBar(
                    containerColor = if (dockSettings.glass) {
                        Color.Transparent
                    } else {
                        MaterialTheme.colorScheme.surfaceContainer
                    },
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    tonalElevation = if (dockSettings.glass) 0.dp else 3.dp,
                ) {
                    NavigationBarItem(
                        selected = tab == 0,
                        onClick = { tab = 0 },
                        icon = { Icon(Icons.Default.Home, contentDescription = null) },
                        label = { Text("Inicio") },
                    )
                    NavigationBarItem(
                        selected = tab == 1,
                        onClick = { tab = 1 },
                        icon = { Icon(Icons.Default.History, contentDescription = null) },
                        label = { Text("Historial") },
                    )
                    NavigationBarItem(
                        selected = tab == 2,
                        onClick = { tab = 2 },
                        icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                        label = { Text("Ajustes") },
                    )
                }
            }
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            when (tab) {
                0 -> HomeScreen(
                    vm = vm,
                    onNewGoal = { sheet = SheetState.NewGoal },
                    onGoalClick = { sheet = SheetState.GoalOptions(it) },
                    onSettings = { tab = 2 },
                )
                1 -> HistoryScreen(vm = vm, onMoveClick = { sheet = SheetState.MoveOptions(it) })
                2 -> SettingsScreen(
                    vm = vm,
                    deviceLockAvailable = deviceLockAvailable,
                    onTestDevice = onTestDevice,
                    onLockNow = onLockNow,
                    onToast = onToast,
                )
            }
        }
    }

    when (val s = sheet) {
        is SheetState.NewGoal -> GoalFormSheet(
            goal = null,
            onSave = { name, target -> vm.addGoal(name, target); sheet = null; onToast("Meta creada") },
            onDismiss = { sheet = null },
        )
        is SheetState.EditGoal -> GoalFormSheet(
            goal = s.goal,
            onSave = { name, target -> vm.editGoal(s.goal.id, name, target); sheet = null; onToast("Meta actualizada") },
            onDismiss = { sheet = null },
        )
        is SheetState.GoalOptions -> GoalOptionsSheet(
            goal = s.goal,
            onDismiss = { sheet = null },
            onDeposit = { sheet = SheetState.Deposit(s.goal) },
            onWithdraw = { sheet = SheetState.Withdraw(s.goal) },
            onEdit = { sheet = SheetState.EditGoal(s.goal) },
            onDelete = {
                confirm = ConfirmState(
                    "¿Eliminar \"${s.goal.name}\"?",
                    "Se borrará la meta. Los movimientos quedan en el historial.",
                ) { vm.deleteGoal(s.goal.id); sheet = null; onToast("Meta eliminada") }
            },
        )
        is SheetState.Deposit -> AmountSheet(
            title = "Añadir dinero",
            subtitle = s.goal.name,
            confirmLabel = "Añadir",
            onConfirm = { amount -> vm.deposit(s.goal.id, amount); sheet = null; onToast("Aporte registrado") },
            onDismiss = { sheet = null },
        )
        is SheetState.Withdraw -> AmountSheet(
            title = "Retirar dinero",
            subtitle = s.goal.name,
            confirmLabel = "Retirar",
            onConfirm = { amount -> vm.withdraw(s.goal.id, amount); sheet = null; onToast("Retiro registrado") },
            onDismiss = { sheet = null },
        )
        is SheetState.MoveOptions -> MoveOptionsSheet(
            move = s.move,
            onDismiss = { sheet = null },
            onEdit = { sheet = SheetState.EditMove(s.move) },
            onDelete = {
                confirm = ConfirmState(
                    "¿Eliminar este movimiento?",
                    "El ahorro de la meta se ajustará automáticamente.",
                ) { vm.deleteMove(s.move.id); sheet = null; onToast("Movimiento eliminado") }
            },
        )
        is SheetState.EditMove -> AmountSheet(
            title = "Editar movimiento",
            subtitle = "${s.move.name} · ${fmtDate(s.move.ts)}",
            confirmLabel = "Guardar",
            onConfirm = { amount -> vm.editMove(s.move.id, amount); sheet = null; onToast("Movimiento actualizado") },
            onDismiss = { sheet = null },
        )
        null -> Unit
    }

    confirm?.let { c ->
        ConfirmDialog(
            title = c.title,
            text = c.text,
            onConfirm = { c.action(); confirm = null },
            onDismiss = { confirm = null },
        )
    }
}
