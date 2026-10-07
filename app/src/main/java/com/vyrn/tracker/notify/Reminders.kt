package com.vyrn.tracker.notify

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.vyrn.tracker.MainActivity
import com.vyrn.tracker.R
import com.vyrn.tracker.data.AppDatabase
import com.vyrn.tracker.data.Habit
import com.vyrn.tracker.data.Routine
import com.vyrn.tracker.data.Task
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

object Reminders {
    const val CHANNEL_ID = "vyrn_reminders"
    const val KIND_HABIT = 1
    const val KIND_ROUTINE = 2
    const val KIND_TASK = 3
    const val KIND_BUDGET = 4

    const val ACTION_DONE = "com.vyrn.tracker.action.REMINDER_DONE"
    const val ACTION_SNOOZE = "com.vyrn.tracker.action.REMINDER_SNOOZE"

    const val EXTRA_KIND = "kind"
    const val EXTRA_ID = "id"
    const val EXTRA_TITLE = "title"
    const val EXTRA_MASK = "mask"
    const val EXTRA_MINUTES = "minutes"

    fun ensureChannel(ctx: Context) {
        if (Build.VERSION.SDK_INT >= 26) {
            val nm = ctx.getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "Promemoria", NotificationManager.IMPORTANCE_DEFAULT),
            )
        }
    }

    internal fun requestCode(kind: Int, id: Long): Int = kind * 100_000 + (id % 100_000).toInt()

    private fun pending(ctx: Context, kind: Int, id: Long, title: String, mask: Int, minutes: Int, flags: Int): PendingIntent? {
        val intent = Intent(ctx, ReminderReceiver::class.java)
            .putExtra(EXTRA_KIND, kind)
            .putExtra(EXTRA_ID, id)
            .putExtra(EXTRA_TITLE, title)
            .putExtra(EXTRA_MASK, mask)
            .putExtra(EXTRA_MINUTES, minutes)
        return PendingIntent.getBroadcast(ctx, requestCode(kind, id), intent, flags or PendingIntent.FLAG_IMMUTABLE)
    }

    fun nextTrigger(mask: Int, minutes: Int, now: LocalDateTime = LocalDateTime.now()): Long {
        for (add in 0..7) {
            val d = now.toLocalDate().plusDays(add.toLong())
            if (((mask shr (d.dayOfWeek.value - 1)) and 1) == 0) continue
            val dt = d.atTime(minutes / 60, minutes % 60)
            if (dt.isAfter(now)) return dt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        }
        return -1L
    }

    fun scheduleRepeating(ctx: Context, kind: Int, id: Long, title: String, mask: Int, minutes: Int) {
        val at = nextTrigger(mask, minutes)
        if (at < 0) return
        val pi = pending(ctx, kind, id, title, mask, minutes, PendingIntent.FLAG_UPDATE_CURRENT) ?: return
        ctx.getSystemService(AlarmManager::class.java).setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
    }

    fun scheduleOnce(ctx: Context, kind: Int, id: Long, title: String, atMillis: Long) {
        if (atMillis <= System.currentTimeMillis()) return
        val pi = pending(ctx, kind, id, title, 127, -1, PendingIntent.FLAG_UPDATE_CURRENT) ?: return
        ctx.getSystemService(AlarmManager::class.java).setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, atMillis, pi)
    }

    fun cancel(ctx: Context, kind: Int, id: Long) {
        val pi = pending(ctx, kind, id, "", 127, -1, PendingIntent.FLAG_NO_CREATE) ?: return
        ctx.getSystemService(AlarmManager::class.java).cancel(pi)
        pi.cancel()
        snoozePending(ctx, kind, id, "", PendingIntent.FLAG_NO_CREATE)?.let {
            ctx.getSystemService(AlarmManager::class.java).cancel(it)
            it.cancel()
        }
    }

    private fun snoozePending(ctx: Context, kind: Int, id: Long, title: String, flags: Int): PendingIntent? {
        val intent = Intent(ctx, ReminderReceiver::class.java)
            .setAction("snooze")
            .putExtra(EXTRA_KIND, kind)
            .putExtra(EXTRA_ID, id)
            .putExtra(EXTRA_TITLE, title)
            .putExtra(EXTRA_MASK, 127)
            .putExtra(EXTRA_MINUTES, -1)
        val code = (kind + 10) * 100_000 + (id % 100_000).toInt()
        return PendingIntent.getBroadcast(ctx, code, intent, flags or PendingIntent.FLAG_IMMUTABLE)
    }

    /** Posticipa il promemoria (di default di un'ora) senza toccare quello ricorrente. */
    fun snooze(ctx: Context, kind: Int, id: Long, title: String, minutes: Int = 60) {
        val pi = snoozePending(ctx, kind, id, title, PendingIntent.FLAG_UPDATE_CURRENT) ?: return
        ctx.getSystemService(AlarmManager::class.java)
            .setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + minutes * 60_000L, pi)
    }

    fun syncHabit(ctx: Context, h: Habit) {
        if (h.reminderMinutes >= 0 && !h.archived) {
            scheduleRepeating(ctx, KIND_HABIT, h.id, h.name, if (h.freqType == 1) h.weekdaysMask else 127, h.reminderMinutes)
        } else cancel(ctx, KIND_HABIT, h.id)
    }

    fun syncRoutine(ctx: Context, r: Routine) {
        if (r.reminderMinutes >= 0) {
            scheduleRepeating(ctx, KIND_ROUTINE, r.id, r.name, r.weekdaysMask, r.reminderMinutes)
        } else cancel(ctx, KIND_ROUTINE, r.id)
    }

    fun syncTask(ctx: Context, t: Task) {
        if (t.reminder && !t.done && t.dueDay >= 0) {
            val minutes = if (t.dueMinutes >= 0) t.dueMinutes else 9 * 60
            val at = LocalDate.ofEpochDay(t.dueDay).atTime(minutes / 60, minutes % 60)
                .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
            scheduleOnce(ctx, KIND_TASK, t.id, t.title, at)
        } else cancel(ctx, KIND_TASK, t.id)
    }

    suspend fun rescheduleAll(ctx: Context) {
        val db = AppDatabase.get(ctx)
        db.habitDao().getHabits().forEach { syncHabit(ctx, it) }
        db.routineDao().getRoutines().forEach { syncRoutine(ctx, it) }
        db.taskDao().getTasks().forEach { syncTask(ctx, it) }
    }

    /** Notifica di promemoria con i pulsanti "Fatto" e "Tra 1 ora". */
    @SuppressLint("MissingPermission")
    fun notifyReminder(ctx: Context, kind: Int, id: Long, title: String, head: String, text: String) {
        val nm = NotificationManagerCompat.from(ctx)
        if (!nm.areNotificationsEnabled()) return
        ensureChannel(ctx)
        val open = PendingIntent.getActivity(
            ctx, 0, Intent(ctx, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val nid = requestCode(kind, id)
        fun action(act: String, label: String, n: Int): NotificationCompat.Action {
            val intent = Intent(ctx, ReminderActionReceiver::class.java)
                .setAction(act)
                .putExtra(EXTRA_KIND, kind)
                .putExtra(EXTRA_ID, id)
                .putExtra(EXTRA_TITLE, title)
            val pi = PendingIntent.getBroadcast(
                ctx, nid * 2 + n, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            return NotificationCompat.Action.Builder(R.drawable.ic_notification, label, pi).build()
        }
        val n = NotificationCompat.Builder(ctx, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(head)
            .setContentText(text)
            .setAutoCancel(true)
            .setContentIntent(open)
            .addAction(action(ACTION_DONE, "Fatto", 0))
            .addAction(action(ACTION_SNOOZE, "Tra 1 ora", 1))
            .build()
        nm.notify(nid, n)
    }

    @SuppressLint("MissingPermission")
    fun notify(ctx: Context, notificationId: Int, title: String, text: String) {
        val nm = NotificationManagerCompat.from(ctx)
        if (!nm.areNotificationsEnabled()) return
        ensureChannel(ctx)
        val open = PendingIntent.getActivity(
            ctx, 0, Intent(ctx, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val n = NotificationCompat.Builder(ctx, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(text)
            .setAutoCancel(true)
            .setContentIntent(open)
            .build()
        nm.notify(notificationId, n)
    }
}

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val kind = intent.getIntExtra(Reminders.EXTRA_KIND, 0)
        val id = intent.getLongExtra(Reminders.EXTRA_ID, 0L)
        val title = intent.getStringExtra(Reminders.EXTRA_TITLE).orEmpty()
        val mask = intent.getIntExtra(Reminders.EXTRA_MASK, 127)
        val minutes = intent.getIntExtra(Reminders.EXTRA_MINUTES, -1)
        val (head, text) = when (kind) {
            Reminders.KIND_HABIT -> "Abitudine" to "È ora di: $title"
            Reminders.KIND_ROUTINE -> "Routine" to "Inizia la routine: $title"
            else -> "Scadenza" to title
        }
        Reminders.notifyReminder(context, kind, id, title, head, text)
        if (kind != Reminders.KIND_TASK && minutes >= 0) {
            Reminders.scheduleRepeating(context, kind, id, title, mask, minutes)
        }
    }
}
