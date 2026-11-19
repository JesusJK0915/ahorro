package com.jesusjk0915.ahorro

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import org.json.JSONObject

/**
 * Widget de inicio: total ahorrado, meta destacada y racha.
 * Lee los mismos datos de la app (sin red, sin servicios remotos).
 */
class AhorrosWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        for (id in ids) update(context, manager, id)
    }

    companion object {
        private data class Info(
            val total: Long = 0L,
            val goalName: String = "Sin metas todavía",
            val pct: Int = 0,
            val streak: Int = 0,
            val hidden: Boolean = false,
        )

        /** Llamado desde Store.save() para que el widget quede al día al instante. */
        fun refresh(context: Context) {
            try {
                val manager = AppWidgetManager.getInstance(context)
                val ids = manager.getAppWidgetIds(ComponentName(context, AhorrosWidget::class.java))
                for (id in ids) update(context, manager, id)
            } catch (e: Exception) {
                // Si no hay widget o el sistema no responde, la app sigue igual.
            }
        }

        private fun read(context: Context): Info {
            val raw = context.getSharedPreferences("ahorro_data_v2", Context.MODE_PRIVATE)
                .getString("state", null) ?: return Info()
            return try {
                val root = JSONObject(raw)
                val so = root.optJSONObject("settings") ?: JSONObject()
                val hidden = so.optBoolean("hideAmounts", false)
                val featuredId = so.optString("featuredGoalId", "")

                var total = 0L
                var name = "Sin metas todavía"
                var pct = 0
                val ga = root.optJSONArray("goals")
                if (ga != null && ga.length() > 0) {
                    var chosen = ga.getJSONObject(0)
                    for (i in 0 until ga.length()) {
                        val g = ga.getJSONObject(i)
                        total += g.optLong("saved", 0L)
                        if (g.optString("id", "") == featuredId) chosen = g
                    }
                    val saved = chosen.optLong("saved", 0L)
                    val target = chosen.optLong("target", 0L)
                    pct = if (target > 0) ((saved.toFloat() / target.toFloat()) * 100f).toInt().coerceIn(0, 100) else 0
                    name = chosen.optString("name", "Meta")
                }

                val moves = mutableListOf<Move>()
                val ma = root.optJSONArray("moves")
                if (ma != null) {
                    for (i in 0 until ma.length()) {
                        val m = ma.getJSONObject(i)
                        moves.add(
                            Move(
                                id = m.optString("id", ""),
                                goalId = m.optString("goalId", ""),
                                name = m.optString("name", ""),
                                amount = m.optLong("amount", 0L),
                                withdraw = m.optBoolean("withdraw", false),
                                ts = m.optLong("ts", 0L),
                                tint = m.optInt("tint", 0),
                            )
                        )
                    }
                }
                Info(total, name, pct, savingsStreak(moves), hidden)
            } catch (e: Exception) {
                Info()
            }
        }

        private fun update(context: Context, manager: AppWidgetManager, id: Int) {
            val info = read(context)
            val views = RemoteViews(context.packageName, R.layout.widget_ahorros)
            views.setTextViewText(R.id.widget_total, money(info.total, info.hidden))
            views.setTextViewText(R.id.widget_goal, info.goalName)
            views.setTextViewText(R.id.widget_pct, "${info.pct}% · racha ${info.streak} d")
            val open = PendingIntent.getActivity(
                context,
                0,
                Intent(context, MainActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            views.setOnClickPendingIntent(R.id.widget_root, open)
            manager.updateAppWidget(id, views)
        }
    }
}
