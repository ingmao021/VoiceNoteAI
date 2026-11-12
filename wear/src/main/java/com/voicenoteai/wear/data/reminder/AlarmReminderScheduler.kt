package com.voicenoteai.wear.data.reminder

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.voicenoteai.wear.domain.model.HistoryTask
import com.voicenoteai.wear.domain.repository.ReminderScheduler

/**
 * [ReminderScheduler] sobre AlarmManager. Usa una alarma inexacta que despierta el reloj
 * (`setAndAllowWhileIdle`): no pide el permiso de alarmas exactas y un aviso de «en una hora»
 * no necesita precisión al minuto.
 */
class AlarmReminderScheduler(context: Context) : ReminderScheduler {
    private val context = context.applicationContext

    override fun schedule(task: HistoryTask, minutes: Long) {
        val alarms = context.getSystemService(AlarmManager::class.java) ?: return
        val intent = Intent(context, ReminderReceiver::class.java)
            .putExtra(ReminderReceiver.EXTRA_TITLE, task.title)
            .putExtra(ReminderReceiver.EXTRA_ID, task.id)
        val pending = PendingIntent.getBroadcast(
            context,
            task.id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val at = System.currentTimeMillis() + minutes * 60_000L
        alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pending)
    }
}
