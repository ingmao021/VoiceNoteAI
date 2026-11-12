package com.voicenoteai.wear.data.reminder

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import com.voicenoteai.wear.R
import com.voicenoteai.wear.domain.repository.AlertMode
import com.voicenoteai.wear.presentation.MainActivity

/** Publica la notificación del recordatorio cuando salta la alarma. */
class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val title = intent.getStringExtra(EXTRA_TITLE)?.takeIf { it.isNotBlank() } ?: return
        val id = intent.getStringExtra(EXTRA_ID).orEmpty()
        if (context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) return

        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        val channelId = channelFor(context, manager, PreferencesUserSettings.currentAlertMode(context))
        val open = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = Notification.Builder(context, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(context.getString(R.string.reminder_title))
            .setContentText(title)
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        manager.notify(id.hashCode(), notification)
    }

    /**
     * Un canal por modo: Android no deja cambiar sonido ni vibración de un canal ya creado.
     * `task_reminders` conserva el id original (sonido y vibración).
     */
    private fun channelFor(context: Context, manager: NotificationManager, mode: AlertMode): String {
        val (id, importance) = when (mode) {
            AlertMode.SOUND_AND_VIBRATE -> "task_reminders" to NotificationManager.IMPORTANCE_HIGH
            AlertMode.VIBRATE -> "task_reminders_vibrate" to NotificationManager.IMPORTANCE_HIGH
            AlertMode.SILENT -> "task_reminders_silent" to NotificationManager.IMPORTANCE_LOW
        }
        val channel = NotificationChannel(id, context.getString(R.string.reminder_channel), importance)
        when (mode) {
            AlertMode.SOUND_AND_VIBRATE -> channel.enableVibration(true)
            AlertMode.VIBRATE -> {
                channel.setSound(null, null)
                channel.enableVibration(true)
            }
            AlertMode.SILENT -> {
                channel.setSound(null, null)
                channel.enableVibration(false)
            }
        }
        manager.createNotificationChannel(channel)
        return id
    }

    companion object {
        const val EXTRA_TITLE = "title"
        const val EXTRA_ID = "id"
    }
}
