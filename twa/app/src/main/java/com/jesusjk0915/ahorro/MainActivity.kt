package com.jesusjk0915.ahorro

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.unit.dp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.core.content.ContextCompat
import kotlin.math.abs
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

private fun shortcutAction(intent: Intent?): String? = when (intent?.action) {
    "com.jesusjk0915.ahorro.NEW_GOAL" -> "new_goal"
    "com.jesusjk0915.ahorro.DEPOSIT" -> "deposit"
    else -> null
}

/**
 * Gesto de la versión web: deslizar horizontal para cambiar de apartado.
 * Sólo si el gesto es claramente horizontal (55px y 1.4x el movimiento vertical)
 * y no hay ninguna hoja o diálogo abierto. Deslizar a la izquierda avanza.
 */
private fun Modifier.swipeBetweenTabs(
    enabled: Boolean,
    tab: Int,
    onTab: (Int) -> Unit,
): Modifier = pointerInput(enabled, tab) {
    if (!enabled) return@pointerInput
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = true, pass = PointerEventPass.Main)
        while (true) {
            val event = awaitPointerEvent(PointerEventPass.Main)
            if (event.changes.none { it.pressed }) break
            val change = event.changes.firstOrNull { it.id == down.id } ?: continue
            // Un control hijo (por ejemplo el slider de transparencia) ya tomó el gesto.
            if (change.isConsumed) break
            val dx = change.position.x - down.position.x
            val dy = change.position.y - down.position.y
            if (abs(dx) >= 55f && abs(dx) >= abs(dy) * 1.4f) {
                val next = tab + if (dx > 0f) -1 else 1
                if (next in 0..3) onTab(next)
                break
            }
        }
    }
}

class MainActivity : FragmentActivity() {
    private val vm: AppViewModel by viewModels()
    private var downloadReceiver: BroadcastReceiver? = null
    private var shortcut by mutableStateOf<String?>(null)
    private val lockedState = mutableStateOf(false)

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
        shortcut = shortcutAction(intent)

        val deviceLockAvailable = BiometricManager.from(this).canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_WEAK or
                BiometricManager.Authenticators.DEVICE_CREDENTIAL
        ) == BiometricManager.BIOMETRIC_SUCCESS

        lockedState.value = vm.ui.settings.security != "none"
        val welcomeState = mutableStateOf(!vm.welcomeSeen())

        setContent {
            val locked by lockedState
            AhorrosTheme(dynamic = vm.ui.settings.dynamicColor) {
                Surface(
                    modifier = Modifier.fillMaxSize().statusBarsPadding(),
                    color = MaterialTheme.colorScheme.background,
                ) {
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
                            shortcut = shortcut,
                            onShortcutConsumed = { shortcut = null },
                        )
                    }
                    if (!locked && welcomeState.value) {
                        WelcomeScreen(
                            onDismiss = {
                                vm.markWelcomeSeen()
                                welcomeState.value = false
                            },
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

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleInstallIntent(intent)
        shortcut = shortcutAction(intent)
    }

    override fun onStop() {
        super.onStop()
        // Bloqueo automático al salir de la app (opción de Seguridad).
        if (!authInProgress && vm.ui.settings.security != "none" && vm.ui.settings.lockOnLeave) {
            lockedState.value = true
        }
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
    shortcut: String?,
    onShortcutConsumed: () -> Unit,
) {
    var tab by remember { mutableIntStateOf(0) }
    var sheet by remember { mutableStateOf<SheetState?>(null) }
    var confirm by remember { mutableStateOf<ConfirmState?>(null) }
    var celebrate by remember { mutableStateOf<String?>(null) }
    val cs = MaterialTheme.colorScheme
    val haptic = LocalHapticFeedback.current

    // Atajos del ícono: abren directamente la hoja correspondiente.
    LaunchedEffect(shortcut) {
        val action = shortcut ?: return@LaunchedEffect
        sheet = when (action) {
            "new_goal" -> SheetState.NewGoal
            "deposit" -> vm.ui.goals.firstOrNull()?.let { SheetState.Deposit(it) } ?: SheetState.NewGoal
            else -> null
        }
        onShortcutConsumed()
    }

    // Entrada elástica al abrir la app (rebote tipo iOS, sin AnimatedContent).
    val enterAnim = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        enterAnim.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioLowBouncy,
                stiffness = Spring.StiffnessMediumLow,
            ),
        )
    }
    val enter = enterAnim.value

    // Cambio de pestaña con dirección y rebote suave.
    val tabAnim = remember { Animatable(1f) }
    var previousTab by remember { mutableIntStateOf(tab) }
    var slideDir by remember { mutableStateOf(1f) }
    LaunchedEffect(tab) {
        if (tab == previousTab) return@LaunchedEffect
        slideDir = if (tab > previousTab) 1f else -1f
        previousTab = tab
        tabAnim.snapTo(0f)
        tabAnim.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioLowBouncy,
                stiffness = Spring.StiffnessMediumLow,
            ),
        )
    }
    val tabProgress = tabAnim.value

    // Fondo ambiental animado: manchas de Material You bien difuminadas.
    // Le dan profundidad al cristal y algo de color que atravesar al blur.
    val ambient = rememberInfiniteTransition(label = "ambient")
    val pulseA by ambient.animateFloat(
        initialValue = 0.86f,
        targetValue = 1.14f,
        animationSpec = infiniteRepeatable(tween(9000), RepeatMode.Reverse),
        label = "ambientA",
    )
    val pulseB by ambient.animateFloat(
        initialValue = 1.12f,
        targetValue = 0.88f,
        animationSpec = infiniteRepeatable(tween(11000), RepeatMode.Reverse),
        label = "ambientB",
    )

    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .offset(x = (-130).dp, y = (-150).dp)
                .size(340.dp)
                .background(
                    Brush.radialGradient(listOf(cs.primary.copy(alpha = 0.16f), Color.Transparent)),
                )
                .blur(70.dp)
                .graphicsLayer { scaleX = pulseA; scaleY = pulseA },
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(x = 140.dp, y = 120.dp)
                .size(320.dp)
                .background(
                    Brush.radialGradient(listOf(cs.tertiary.copy(alpha = 0.14f), Color.Transparent)),
                )
                .blur(70.dp)
                .graphicsLayer { scaleX = pulseB; scaleY = pulseB },
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(260.dp)
                .background(
                    Brush.verticalGradient(listOf(Color.Transparent, cs.primary.copy(alpha = 0.18f))),
                )
                .blur(60.dp)
                .graphicsLayer { alpha = pulseA },
        )
        Column(
            Modifier
                .fillMaxSize()
                .swipeBetweenTabs(
                    enabled = sheet == null && confirm == null,
                    tab = tab,
                    onTab = { tab = it },
                )
                .graphicsLayer {
                    alpha = enter.coerceIn(0f, 1f) * (0.45f + 0.55f * tabProgress.coerceIn(0f, 1f))
                    translationX = (1f - tabProgress) * slideDir * 28f
                    translationY = (1f - enter) * 26f + (1f - tabProgress) * 6f
                    val scale = (0.96f + 0.04f * enter) * (0.985f + 0.015f * tabProgress)
                    scaleX = scale
                    scaleY = scale
                },
        ) {
            when (tab) {
                0 -> HomeScreen(
                    vm = vm,
                    onNewGoal = { sheet = SheetState.NewGoal },
                    onGoalClick = { sheet = SheetState.GoalOptions(it) },
                )
                1 -> HistoryScreen(vm = vm, onMoveClick = { sheet = SheetState.MoveOptions(it) })
                2 -> AchievementsScreen(vm = vm)
                3 -> SettingsScreen(
                    vm = vm,
                    deviceLockAvailable = deviceLockAvailable,
                    onTestDevice = onTestDevice,
                    onLockNow = onLockNow,
                    onToast = onToast,
                )
            }
        }
        Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth()) {
            WebNavBar(settings = vm.ui.settings, tab = tab, onTab = { tab = it })
        }
        celebrate?.let { name ->
            GoalCelebration(name = name) { celebrate = null }
        }
        if (vm.canUndoDelete && celebrate == null) {
            Box(Modifier.align(Alignment.BottomCenter).padding(bottom = 92.dp)) {
                Button(
                    onClick = { vm.undoDelete(); onToast("Eliminación deshecha") },
                ) {
                    Text("Deshacer eliminación")
                }
            }
        }
    }

    when (val s = sheet) {
        is SheetState.NewGoal -> GoalFormSheet(
            goal = null,
            onSave = { name, target, deadline ->
                vm.addGoal(name, target, deadline); sheet = null; onToast("Meta creada")
            },
            onDismiss = { sheet = null },
        )
        is SheetState.EditGoal -> GoalFormSheet(
            goal = s.goal,
            onSave = { name, target, deadline ->
                vm.editGoal(s.goal.id, name, target, deadline); sheet = null; onToast("Meta actualizada")
            },
            onDismiss = { sheet = null },
        )
        is SheetState.GoalOptions -> GoalOptionsSheet(
            goal = s.goal,
            featured = vm.ui.settings.featuredGoalId == s.goal.id,
            hide = vm.ui.settings.hideAmounts,
            onToggleFeatured = {
                val already = vm.ui.settings.featuredGoalId == s.goal.id
                vm.setFeaturedGoal(if (already) "" else s.goal.id)
                sheet = null
                onToast(if (already) "Meta destacada quitada" else "Meta destacada")
            },
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
            quickFrom = (s.goal.target - s.goal.saved).coerceAtLeast(0L),
            onConfirm = { amount ->
                val before = s.goal
                vm.deposit(before.id, amount)
                sheet = null
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                val after = vm.ui.goals.firstOrNull { it.id == before.id }
                if (before.saved < before.target && after != null && after.saved >= after.target) {
                    celebrate = after.name
                    onToast("¡Meta cumplida!")
                } else {
                    onToast("Aporte registrado")
                }
            },
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
            hide = vm.ui.settings.hideAmounts,
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
