package com.jesusjk0915.ahorro

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat

object NotificationHelper {
    private const val CHANNEL_ID = "ahorro_notifications"
    private const val CHANNEL_NAME = "Ahorros"
    private const val CHANNEL_DESC = "Notificaciones de la app Ahorros"
    private const val REQ_CODE_GOAL = 1001
    private const val REQ_CODE_REMINDER = 1002

    /** Inicializa el canal de notificaciones (una sola vez al arrancar la app). */
    fun init(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply { description = CHANNEL_DESC }
            manager.createNotificationChannel(channel)
        }
    }

    /** Notifica que una meta se completó al 100%. */
    fun notifyGoalCompleted(context: Context, goalName: String, saved: Long, target: Long) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val intent = android.content.Intent(context, MainActivity::class.java)
        intent.flags = android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP or android.content.Intent.FLAG_ACTIVITY_SINGLE_TOP
        val pending = android.app.PendingIntent.getActivity(
            context, REQ_CODE_GOAL, intent,
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
        )
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("¡Meta completada! 🎉")
            .setContentText("Llegaste al 100% de \"$goalName\" ($saved de $target)")
            .setAutoCancel(true)
            .setContentIntent(pending)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(androidx.core.app.NotificationCompat.DEFAULT_ALL)
        manager.notify(REQ_CODE_GOAL, builder.build())
    }

    /** Notificación de recordatorio diario. */
    fun notifyDailyReminder(context: Context) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val intent = android.content.Intent(context, MainActivity::class.java)
        intent.flags = android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP or android.content.Intent.FLAG_ACTIVITY_SINGLE_TOP
        val pending = android.app.PendingIntent.getActivity(
            context, REQ_CODE_REMINDER, intent,
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
        )
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("¿Hiciste un aporte hoy?")
            .setContentText("Registra tus ahorros y sigue avanzando hacia tus metas.")
            .setAutoCancel(true)
            .setContentIntent(pending)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setDefaults(androidx.core.app.NotificationCompat.DEFAULT_SOUND)
        manager.notify(REQ_CODE_REMINDER, builder.build())
    }

    /** Cancela el recordatorio diario (ej. al desactivar notificaciones). */
    fun cancelReminder(context: Context) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.cancel(REQ_CODE_REMINDER)
    }
}
