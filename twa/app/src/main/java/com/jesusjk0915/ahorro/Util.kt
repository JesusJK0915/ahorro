package com.jesusjk0915.ahorro

import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private val ES = Locale("es", "CO")

fun money(value: Long, hidden: Boolean = false): String {
    if (hidden) return "••••"
    val nf = NumberFormat.getCurrencyInstance(ES)
    nf.maximumFractionDigits = 0
    nf.minimumFractionDigits = 0
    return nf.format(value)
}

fun grouped(value: Long): String = NumberFormat.getNumberInstance(ES).format(value)

private fun dayStamp(ts: Long): String = SimpleDateFormat("yyyyMMdd", ES).format(Date(ts))

fun fmtDate(ts: Long): String {
    val today = System.currentTimeMillis()
    val yesterday = today - 86_400_000L
    val time = SimpleDateFormat("h:mm a", ES).format(Date(ts)).replace(" a. m.", " AM").replace(" p. m.", " PM")
    return when (dayStamp(ts)) {
        dayStamp(today) -> "Hoy · $time"
        dayStamp(yesterday) -> "Ayer · $time"
        else -> SimpleDateFormat("d MMM", ES).format(Date(ts)) + " · $time"
    }
}

fun dayKey(ts: Long): String = SimpleDateFormat("yyyy-MM-dd", ES).format(Date(ts))
fun monthKey(ts: Long): String = SimpleDateFormat("yyyy-MM", ES).format(Date(ts))
fun csvDate(ts: Long): String = SimpleDateFormat("yyyy-MM-dd HH:mm", ES).format(Date(ts))
fun fmtDay(ts: Long): String = SimpleDateFormat("d MMM yyyy", ES).format(Date(ts))

/** Clave de mes relativa al actual: 0 = este mes, -1 = anterior, 1 = siguiente. */
fun monthKeyForOffset(offset: Int): String {
    val cal = Calendar.getInstance(ES)
    cal.add(Calendar.MONTH, offset)
    return monthKey(cal.timeInMillis)
}

/** Días enteros entre hoy y la fecha límite (negativo si ya venció). */
fun daysUntil(ts: Long): Int {
    if (ts <= 0L) return -1
    val cal = Calendar.getInstance(ES)
    cal.set(Calendar.HOUR_OF_DAY, 0)
    cal.set(Calendar.MINUTE, 0)
    cal.set(Calendar.SECOND, 0)
    cal.set(Calendar.MILLISECOND, 0)
    val today = cal.timeInMillis
    cal.timeInMillis = ts
    cal.set(Calendar.HOUR_OF_DAY, 0)
    cal.set(Calendar.MINUTE, 0)
    cal.set(Calendar.SECOND, 0)
    cal.set(Calendar.MILLISECOND, 0)
    return ((cal.timeInMillis - today) / 86_400_000L).toInt()
}

/** Cuánto hay que ahorrar al día para llegar a la meta en la fecha límite. */
fun perDayNeeded(goal: Goal): Long {
    if (goal.deadline <= 0L || goal.saved >= goal.target) return 0L
    val missing = goal.target - goal.saved
    val days = daysUntil(goal.deadline)
    if (days <= 0) return missing
    return (missing + days - 1) / days
}

/** Días consecutivos con al menos un aporte (hoy o, si aún no aportó, ayer). */
fun savingsStreak(moves: List<Move>): Int {
    val days = moves.filter { !it.withdraw }.map { dayKey(it.ts) }.toHashSet()
    val cal = Calendar.getInstance(ES)
    if (!days.contains(dayKey(cal.timeInMillis))) cal.add(Calendar.DAY_OF_YEAR, -1)
    var streak = 0
    while (days.contains(dayKey(cal.timeInMillis))) {
        streak++
        cal.add(Calendar.DAY_OF_YEAR, -1)
    }
    return streak
}

data class DayBucket(val key: String, val label: String, val net: Long)

/** Últimos 7 días terminando hoy, con el neto de movimientos de cada día. */
fun last7(moves: List<Move>): List<DayBucket> {
    val cal = Calendar.getInstance(ES)
    cal.add(Calendar.DAY_OF_YEAR, -6)
    val fmtLabel = SimpleDateFormat("EEE", ES)
    return (0 until 7).map { _ ->
        val ts = cal.timeInMillis
        val key = dayKey(ts)
        val label = fmtLabel.format(Date(ts)).replace(".", "").take(3)
        val net = moves.filter { dayKey(it.ts) == key }
            .sumOf { if (it.withdraw) -it.amount else it.amount }
        cal.add(Calendar.DAY_OF_YEAR, 1)
        DayBucket(key, label.replaceFirstChar { it.uppercase() }, net)
    }
}

data class CalDay(val day: Int, val amount: Long)

/** Devuelve (nombre del mes, espacios iniciales, días con aportes). */
fun monthCalendar(moves: List<Move>, offset: Int = 0): Triple<String, Int, List<CalDay>> {
    val cal = Calendar.getInstance(ES)
    cal.add(Calendar.MONTH, offset)
    val year = cal.get(Calendar.YEAR)
    val month = cal.get(Calendar.MONTH)
    val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)

    val first = Calendar.getInstance(ES)
    first.set(year, month, 1)
    val offset = (first.get(Calendar.DAY_OF_WEEK) - Calendar.MONDAY + 7) % 7

    val perDay = mutableMapOf<Int, Long>()
    moves.forEach { m ->
        if (m.withdraw) return@forEach
        val d = Calendar.getInstance(ES)
        d.timeInMillis = m.ts
        if (d.get(Calendar.YEAR) == year && d.get(Calendar.MONTH) == month) {
            val n = d.get(Calendar.DAY_OF_MONTH)
            perDay[n] = (perDay[n] ?: 0L) + m.amount
        }
    }
    val name = SimpleDateFormat("MMM yyyy", ES).format(Date(first.timeInMillis))
        .replaceFirstChar { it.uppercase() }
    return Triple(name, offset, (1..daysInMonth).map { CalDay(it, perDay[it] ?: 0L) })
}
