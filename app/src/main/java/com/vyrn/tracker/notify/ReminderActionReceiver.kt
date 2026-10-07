package com.vyrn.tracker.notify

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import com.vyrn.tracker.data.AppDatabase
import com.vyrn.tracker.data.HabitLog
import com.vyrn.tracker.data.RoutineStepLog
import com.vyrn.tracker.widget.refreshWidget
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate

/** Gestisce i pulsanti "Fatto" e "Tra 1 ora" delle notifiche, senza aprire l'app. */
class ReminderActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val kind = intent.getIntExtra(Reminders.EXTRA_KIND, 0)
        val id = intent.getLongExtra(Reminders.EXTRA_ID, 0L)
        val title = intent.getStringExtra(Reminders.EXTRA_TITLE).orEmpty()
        val action = intent.action
        val app = context.applicationContext
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                when (action) {
                    Reminders.ACTION_DONE -> markDone(app, kind, id)
                    Reminders.ACTION_SNOOZE -> Reminders.snooze(app, kind, id, title)
                }
                NotificationManagerCompat.from(app).cancel(Reminders.requestCode(kind, id))
                refreshWidget(app)
            } finally {
                pending.finish()
            }
        }
    }

    private suspend fun markDone(ctx: Context, kind: Int, id: Long) {
        val db = AppDatabase.get(ctx)
        val day = LocalDate.now().toEpochDay()
        when (kind) {
            Reminders.KIND_HABIT -> {
                val dao = db.habitDao()
                val habit = dao.getHabits().firstOrNull { it.id == id } ?: return
                if (habit.kind == 2) return // "da evitare": nessuna azione da segnare
                val note = dao.getLog(id, day)?.note.orEmpty()
                dao.upsertLog(HabitLog(id, day, habit.target, note))
            }
            Reminders.KIND_ROUTINE -> {
                val dao = db.routineDao()
                dao.getSteps(id).forEach { dao.insertStepLog(RoutineStepLog(it.id, day)) }
            }
            Reminders.KIND_TASK -> {
                val dao = db.taskDao()
                val task = dao.getTasks().firstOrNull { it.id == id } ?: return
                dao.update(task.copy(done = true))
                Reminders.cancel(ctx, Reminders.KIND_TASK, id)
            }
        }
    }
}
