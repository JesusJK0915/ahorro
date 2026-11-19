package com.jesusjk0915.ahorro

import android.app.Application
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import org.json.JSONArray
import org.json.JSONObject
import java.security.MessageDigest
import java.util.UUID

private const val PREFS_DATA = "ahorro_data_v2"
private const val KEY_STATE = "state"
private const val KEY_WELCOME = "welcome_seen"

fun lockHash(value: String): String {
    return try {
        val bytes = MessageDigest.getInstance("SHA-256")
            .digest(("ahorro-lock-v1|" + value).toByteArray(Charsets.UTF_8))
        bytes.joinToString("") { "%02x".format(it) }
    } catch (e: Exception) {
        value
    }
}

class Store(private val context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_DATA, Context.MODE_PRIVATE)

    fun welcomeSeen(): Boolean = prefs.getBoolean(KEY_WELCOME, false)

    fun markWelcomeSeen() {
        prefs.edit().putBoolean(KEY_WELCOME, true).apply()
    }

    fun load(): AppState = try {
        val raw = prefs.getString(KEY_STATE, null)
        if (raw.isNullOrEmpty()) AppState() else parseState(raw)
    } catch (e: Exception) {
        AppState()
    }

    /** Convierte un respaldo JSON en estado; devuelve null si no es válido. */
    fun restoreJson(raw: String): AppState? = try {
        val root = JSONObject(raw)
        if (!root.has("goals") && !root.has("moves")) null else parseState(raw)
    } catch (e: Exception) {
        null
    }

    private fun parseState(raw: String): AppState {
        return try {
            val root = JSONObject(raw)
            val goals = mutableListOf<Goal>()
            val gArr = root.optJSONArray("goals") ?: JSONArray()
            for (i in 0 until gArr.length()) {
                val o = gArr.getJSONObject(i)
                goals.add(
                    Goal(
                        id = o.optString("id", newId()),
                        name = o.optString("name", "Meta"),
                        target = o.optLong("target", 0L),
                        saved = o.optLong("saved", 0L),
                        tint = o.optInt("tint", 0),
                        ts = o.optLong("ts", System.currentTimeMillis()),
                        deadline = o.optLong("deadline", 0L),
                    )
                )
            }
            val moves = mutableListOf<Move>()
            val mArr = root.optJSONArray("moves") ?: JSONArray()
            for (i in 0 until mArr.length()) {
                val o = mArr.getJSONObject(i)
                moves.add(
                    Move(
                        id = o.optString("id", newId()),
                        goalId = o.optString("goalId", ""),
                        name = o.optString("name", "Aporte"),
                        amount = o.optLong("amount", 0L),
                        withdraw = o.optBoolean("withdraw", false),
                        ts = o.optLong("ts", System.currentTimeMillis()),
                        tint = o.optInt("tint", 0),
                    )
                )
            }
            val so = root.optJSONObject("settings") ?: JSONObject()
            val settings = Settings(
                dynamicColor = so.optBoolean("dynamicColor", true),
                glass = so.optBoolean("glass", false),
                glassTransparency = so.optInt("glassTransparency", 60),
                glassStyle = so.optString("glassStyle", "transparent"),
                hideDockLabels = so.optBoolean("hideDockLabels", false),
                security = so.optString("security", "none"),
                pinHash = so.optString("pinHash", ""),
                patternHash = so.optString("patternHash", ""),
                statsResetAt = so.optLong("statsResetAt", 0L),
                featuredGoalId = so.optString("featuredGoalId", ""),
                hideAmounts = so.optBoolean("hideAmounts", false),
                lockOnLeave = so.optBoolean("lockOnLeave", false),
            )
            AppState(goals = goals, moves = moves, settings = settings)
        } catch (e: Exception) {
            AppState()
        }
    }

    fun save(state: AppState) {
        try {
            prefs.edit().putString(KEY_STATE, backupJson(state)).apply()
            AhorrosWidget.refresh(context)
        } catch (e: Exception) {
            // Nunca debe romper la app por no poder guardar.
        }
    }

    /** Serializa metas, movimientos y ajustes para el respaldo local JSON. */
    fun backupJson(state: AppState): String {
        val root = JSONObject()
            val ga = JSONArray()
            state.goals.forEach { g ->
                ga.put(
                    JSONObject()
                        .put("id", g.id).put("name", g.name).put("target", g.target)
                        .put("saved", g.saved).put("tint", g.tint).put("ts", g.ts)
                        .put("deadline", g.deadline)
                )
            }
            val ma = JSONArray()
            state.moves.forEach { m ->
                ma.put(
                    JSONObject()
                        .put("id", m.id).put("goalId", m.goalId).put("name", m.name)
                        .put("amount", m.amount).put("withdraw", m.withdraw)
                        .put("ts", m.ts).put("tint", m.tint)
                )
            }
            val st = state.settings
            root.put("goals", ga).put("moves", ma)
            root.put(
                "settings",
                JSONObject()
                    .put("dynamicColor", st.dynamicColor)
                    .put("glass", st.glass)
                    .put("glassTransparency", st.glassTransparency)
                    .put("glassStyle", st.glassStyle)
                    .put("hideDockLabels", st.hideDockLabels)
                    .put("security", st.security)
                    .put("pinHash", st.pinHash)
                    .put("patternHash", st.patternHash)
                    .put("statsResetAt", st.statsResetAt)
                    .put("featuredGoalId", st.featuredGoalId)
                    .put("hideAmounts", st.hideAmounts)
                    .put("lockOnLeave", st.lockOnLeave)
            )
        return root.toString()
    }
}

fun newId(): String = UUID.randomUUID().toString().replace("-", "").take(10)

class AppViewModel(app: Application) : AndroidViewModel(app) {
    private val store = Store(app)

    var ui by mutableStateOf(store.load())
        private set

    private var backupMoves by mutableStateOf<List<Move>?>(null)
    val canUndoStatsReset: Boolean get() = backupMoves != null

    private var lastDeletedGoal by mutableStateOf<Goal?>(null)
    private var lastDeletedMove by mutableStateOf<Move?>(null)
    val canUndoDelete: Boolean get() = lastDeletedGoal != null || lastDeletedMove != null

    /** Fuerza una recomposición para actualizar filtros de fecha. */
    fun refresh() {
        ui = ui.copy()
    }

    fun welcomeSeen(): Boolean = store.welcomeSeen()

    fun markWelcomeSeen() = store.markWelcomeSeen()

    private fun commit(next: AppState) {
        ui = next
        store.save(next)
    }

    fun addGoal(name: String, target: Long, deadline: Long = 0L) {
        val clean = name.trim().take(42)
        if (clean.isEmpty() || target <= 0) return
        val g = Goal(
            id = newId(),
            name = clean,
            target = target,
            saved = 0,
            tint = ui.goals.size % TINTS.size,
            ts = System.currentTimeMillis(),
            deadline = deadline,
        )
        commit(ui.copy(goals = listOf(g) + ui.goals))
    }

    fun editGoal(id: String, name: String, target: Long, deadline: Long = 0L) {
        val clean = name.trim().take(42)
        if (clean.isEmpty() || target <= 0) return
        commit(
            ui.copy(
                goals = ui.goals.map {
                    if (it.id == id) it.copy(name = clean, target = target, deadline = deadline) else it
                },
                moves = ui.moves.map { if (it.goalId == id) it.copy(name = clean) else it },
            )
        )
    }

    fun deleteGoal(id: String) {
        val goal = ui.goals.firstOrNull { it.id == id } ?: return
        lastDeletedGoal = goal
        lastDeletedMove = null
        commit(
            ui.copy(
                goals = ui.goals.filterNot { it.id == id },
                settings = ui.settings.copy(
                    featuredGoalId = if (ui.settings.featuredGoalId == id) "" else ui.settings.featuredGoalId,
                ),
            )
        )
    }

    /** Restaura la última meta o el último movimiento borrado. */
    fun undoDelete() {
        lastDeletedGoal?.let { g ->
            lastDeletedGoal = null
            commit(ui.copy(goals = (listOf(g) + ui.goals).distinctBy { it.id }))
            return
        }
        lastDeletedMove?.let { m ->
            lastDeletedMove = null
            val sign = if (m.withdraw) -1L else 1L
            commit(
                ui.copy(
                    goals = ui.goals.map { g ->
                        if (g.id == m.goalId) g.copy(saved = maxOf(0, g.saved + sign * m.amount)) else g
                    },
                    moves = (listOf(m) + ui.moves).distinctBy { it.id }.take(200),
                )
            )
        }
    }

    fun deposit(id: String, amount: Long) {
        if (amount <= 0) return
        val g = ui.goals.firstOrNull { it.id == id } ?: return
        val m = Move(
            id = newId(), goalId = id, name = g.name, amount = amount,
            withdraw = false, ts = System.currentTimeMillis(), tint = g.tint,
        )
        commit(
            ui.copy(
                goals = ui.goals.map { if (it.id == id) it.copy(saved = it.saved + amount) else it },
                moves = (listOf(m) + ui.moves).take(200),
            )
        )
    }

    fun withdraw(id: String, amount: Long) {
        if (amount <= 0) return
        val g = ui.goals.firstOrNull { it.id == id } ?: return
        val value = minOf(amount, g.saved)
        if (value <= 0) return
        val m = Move(
            id = newId(), goalId = id, name = g.name, amount = value,
            withdraw = true, ts = System.currentTimeMillis(), tint = g.tint,
        )
        commit(
            ui.copy(
                goals = ui.goals.map { if (it.id == id) it.copy(saved = maxOf(0, it.saved - value)) else it },
                moves = (listOf(m) + ui.moves).take(200),
            )
        )
    }

    fun editMove(id: String, amount: Long) {
        if (amount <= 0) return
        val m = ui.moves.firstOrNull { it.id == id } ?: return
        val old = m.amount
        val sign = if (m.withdraw) -1L else 1L
        commit(
            ui.copy(
                goals = ui.goals.map { g ->
                    if (g.id == m.goalId) {
                        g.copy(saved = maxOf(0, g.saved + sign * (amount - old)))
                    } else g
                },
                moves = ui.moves.map { if (it.id == id) it.copy(amount = amount) else it },
            )
        )
    }

    fun deleteMove(id: String) {
        val m = ui.moves.firstOrNull { it.id == id } ?: return
        lastDeletedMove = m
        lastDeletedGoal = null
        val sign = if (m.withdraw) 1L else -1L
        commit(
            ui.copy(
                goals = ui.goals.map { g ->
                    if (g.id == m.goalId) {
                        g.copy(saved = maxOf(0, g.saved + sign * m.amount))
                    } else g
                },
                moves = ui.moves.filterNot { it.id == id },
            )
        )
    }

    fun setDynamicColor(v: Boolean) = commit(ui.copy(settings = ui.settings.copy(dynamicColor = v)))
    fun setGlass(v: Boolean) = commit(ui.copy(settings = ui.settings.copy(glass = v)))
    fun setGlassTransparency(v: Int) =
        commit(ui.copy(settings = ui.settings.copy(glassTransparency = v.coerceIn(0, 100))))
    fun setGlassStyle(v: String) = commit(ui.copy(settings = ui.settings.copy(glassStyle = v)))
    fun setHideDockLabels(v: Boolean) = commit(ui.copy(settings = ui.settings.copy(hideDockLabels = v)))
    fun setFeaturedGoal(id: String) = commit(ui.copy(settings = ui.settings.copy(featuredGoalId = id)))
    fun setHideAmounts(v: Boolean) = commit(ui.copy(settings = ui.settings.copy(hideAmounts = v)))
    fun setLockOnLeave(v: Boolean) = commit(ui.copy(settings = ui.settings.copy(lockOnLeave = v)))

    /** Respaldo local: JSON con todo el estado, sin red. */
    fun backupJson(): String = store.backupJson(ui)

    /** Restaura un respaldo reemplazando metas, movimientos y ajustes. */
    fun restoreJson(raw: String): Boolean {
        val next = store.restoreJson(raw) ?: return false
        lastDeletedGoal = null
        lastDeletedMove = null
        backupMoves = null
        commit(next)
        return true
    }
    fun setSecurity(method: String) = commit(ui.copy(settings = ui.settings.copy(security = method)))
    fun setPin(pin: String) = commit(ui.copy(settings = ui.settings.copy(pinHash = lockHash(pin))))
    fun setPattern(seq: List<Int>) = commit(
        ui.copy(settings = ui.settings.copy(patternHash = lockHash(seq.joinToString("-"))))
    )

    /** Borra solo el historial; metas y ahorro se conservan. */
    fun resetStats() {
        backupMoves = ui.moves
        commit(
            ui.copy(
                moves = emptyList(),
                settings = ui.settings.copy(statsResetAt = System.currentTimeMillis()),
            )
        )
    }

    fun undoStatsReset() {
        val moves = backupMoves ?: return
        commit(ui.copy(moves = moves))
        backupMoves = null
    }

    fun checkPin(pin: String): Boolean =
        ui.settings.pinHash.isNotEmpty() && ui.settings.pinHash == lockHash(pin)

    fun checkPattern(seq: List<Int>): Boolean =
        ui.settings.patternHash.isNotEmpty() && ui.settings.patternHash == lockHash(seq.joinToString("-"))
}
