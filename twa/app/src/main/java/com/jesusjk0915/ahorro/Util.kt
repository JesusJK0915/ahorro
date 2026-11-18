package com.jesusjk0915.ahorro

import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private val ES = Locale("es", "CO")

fun money(value: Long): String {
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
fun monthCalendar(moves: List<Move>): Triple<String, Int, List<CalDay>> {
    val cal = Calendar.getInstance(ES)
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
