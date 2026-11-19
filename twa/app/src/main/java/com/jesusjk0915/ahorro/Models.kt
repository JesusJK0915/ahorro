package com.jesusjk0915.ahorro

/** Modelo local: todo vive en el celular, sin red para las operaciones normales. */
data class Goal(
    val id: String,
    val name: String,
    val target: Long,
    val saved: Long,
    val tint: Int,
    val ts: Long,
)

data class Move(
    val id: String,
    val goalId: String,
    val name: String,
    val amount: Long,
    val withdraw: Boolean,
    val ts: Long,
    val tint: Int,
)

data class Settings(
    val dynamicColor: Boolean = true,
    val glass: Boolean = false,
    val glassTransparency: Int = 60, // 0 = casi sólido, 100 = muy translúcido
    val glassStyle: String = "transparent", // transparent | opaque
    val hideDockLabels: Boolean = false, // true = solo iconos en el dock
    val security: String = "none", // none | device | pin | pattern
    val pinHash: String = "",
    val patternHash: String = "",
    val statsResetAt: Long = 0L,
    val featuredGoalId: String = "", // meta fijada arriba y en el widget
    val hideAmounts: Boolean = false, // privacidad rápida en la pantalla
)

data class AppState(
    val goals: List<Goal> = emptyList(),
    val moves: List<Move> = emptyList(),
    val settings: Settings = Settings(),
)

val TINTS = intArrayOf(
    0xFF00696B.toInt(),
    0xFF8A5A00.toInt(),
    0xFF7A4E9C.toInt(),
    0xFFA03A2E.toInt(),
    0xFF00658E.toInt(),
)

fun tintAt(i: Int): androidx.compose.ui.graphics.Color =
    androidx.compose.ui.graphics.Color(TINTS[Math.floorMod(i, TINTS.size)])
