package com.vyrn.tracker

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.vyrn.tracker.data.AppDatabase
import com.vyrn.tracker.data.Habit
import com.vyrn.tracker.data.HabitLog
import com.vyrn.tracker.data.Routine
import com.vyrn.tracker.data.RoutineStep
import com.vyrn.tracker.data.RoutineStepLog
import com.vyrn.tracker.data.Task
import com.vyrn.tracker.notify.Reminders
import com.vyrn.tracker.widget.refreshWidget
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

class RoutineViewModel(app: Application) : AndroidViewModel(app) {
    private val ctx: Context = app.applicationContext
    private val db = AppDatabase.get(ctx)
    private val hd = db.habitDao()
    private val rd = db.routineDao()
    private val td = db.taskDao()

    private fun <T> Flow<T>.state(init: T): StateFlow<T> =
        stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), init)

    val habits: StateFlow<List<Habit>> = hd.observeHabits().state(emptyList())
    val habitLogs: StateFlow<List<HabitLog>> = hd.observeLogs().state(emptyList())
    val routines: StateFlow<List<Routine>> = rd.observeRoutines().state(emptyList())
    val steps: StateFlow<List<RoutineStep>> = rd.observeSteps().state(emptyList())
    val stepLogs: StateFlow<List<RoutineStepLog>> = rd.observeStepLogs().state(emptyList())
    val tasks: StateFlow<List<Task>> = td.observeTasks().state(emptyList())

    /** Giorno selezionato nella schermata delle abitudini (epoch day). */
    val selectedDay = MutableStateFlow(LocalDate.now().toEpochDay())

    private fun io(block: suspend () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            block()
            refreshWidget(ctx)
        }
    }

    // ---------- Abitudini ----------

    fun saveHabit(h: Habit) = io {
        val id = if (h.id == 0L) hd.insert(h.copy(createdDay = LocalDate.now().toEpochDay())) else {
            hd.update(h)
            h.id
        }
        Reminders.syncHabit(ctx, h.copy(id = id))
    }

    fun deleteHabit(h: Habit) = io {
        hd.deleteLogs(h.id)
        hd.delete(h.id)
        Reminders.cancel(ctx, Reminders.KIND_HABIT, h.id)
    }

    fun setHabitValue(h: Habit, day: Long, value: Double) = io {
        val existing = hd.getLog(h.id, day)
        val note = existing?.note.orEmpty()
        if (value <= 0.0 && note.isBlank()) hd.deleteLog(h.id, day)
        else hd.upsertLog(HabitLog(h.id, day, value.coerceAtLeast(0.0), note))
    }

    fun toggleHabit(h: Habit, day: Long, currentlyDone: Boolean) {
        // Abitudine "da evitare": il tocco segna o annulla una ricaduta.
        if (h.kind == 2) setHabitValue(h, day, if (currentlyDone) 1.0 else 0.0)
        else setHabitValue(h, day, if (currentlyDone) 0.0 else h.target)
    }

    fun setHabitNote(h: Habit, day: Long, note: String) = io {
        val existing = hd.getLog(h.id, day)
        val value = existing?.value ?: 0.0
        if (value <= 0.0 && note.isBlank()) hd.deleteLog(h.id, day)
        else hd.upsertLog(HabitLog(h.id, day, value, note.trim()))
    }

    // ---------- Routine a step ----------

    fun saveRoutine(r: Routine, steps: List<RoutineStep>) = io {
        val id = if (r.id == 0L) rd.insertRoutine(r) else {
            rd.updateRoutine(r)
            r.id
        }
        rd.deleteStepsNotIn(id, steps.filter { it.id != 0L }.map { it.id })
        rd.upsertSteps(steps.mapIndexed { i, s -> s.copy(routineId = id, position = i) })
        Reminders.syncRoutine(ctx, r.copy(id = id))
    }

    fun deleteRoutine(r: Routine) = io {
        rd.deleteStepLogsForRoutine(r.id)
        rd.deleteSteps(r.id)
        rd.deleteRoutine(r.id)
        Reminders.cancel(ctx, Reminders.KIND_ROUTINE, r.id)
    }

    fun toggleStep(stepId: Long, day: Long, done: Boolean) = io {
        if (done) rd.deleteStepLog(stepId, day) else rd.insertStepLog(RoutineStepLog(stepId, day))
    }

    // ---------- Task ----------

    fun saveTask(t: Task) = io {
        val id = if (t.id == 0L) td.insert(t) else {
            td.update(t)
            t.id
        }
        Reminders.syncTask(ctx, t.copy(id = id))
    }

    fun toggleTask(t: Task) = io {
        val updated = t.copy(done = !t.done)
        td.update(updated)
        Reminders.syncTask(ctx, updated)
    }

    fun deleteTask(t: Task) = io {
        td.delete(t.id)
        Reminders.cancel(ctx, Reminders.KIND_TASK, t.id)
    }
}
