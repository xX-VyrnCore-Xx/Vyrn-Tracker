package com.vyrn.tracker

import android.app.Application
import android.content.Context
import android.os.SystemClock
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.vyrn.tracker.notify.Reminders
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate

/** Timer di concentrazione (stile pomodoro). Lo stato sopravvive al cambio di scheda. */
class FocusViewModel(app: Application) : AndroidViewModel(app) {
    private val ctx: Context = app.applicationContext
    private val prefs = ctx.getSharedPreferences("vyrn_prefs", Context.MODE_PRIVATE)

    val totalSeconds = MutableStateFlow(25 * 60)
    val remaining = MutableStateFlow(25 * 60)
    val running = MutableStateFlow(false)
    val sessionsToday = MutableStateFlow(loadSessions())

    private var job: Job? = null
    private var endAt = 0L

    private fun today(): Long = LocalDate.now().toEpochDay()

    private fun loadSessions(): Int =
        if (prefs.getLong("focus_day", -1L) == today()) prefs.getInt("focus_count", 0) else 0

    fun setMinutes(minutes: Int) {
        if (running.value) return
        totalSeconds.value = minutes * 60
        remaining.value = minutes * 60
    }

    fun start() {
        if (running.value || remaining.value <= 0) return
        running.value = true
        endAt = SystemClock.elapsedRealtime() + remaining.value * 1000L
        job = viewModelScope.launch {
            while (true) {
                val left = ((endAt - SystemClock.elapsedRealtime() + 999) / 1000).toInt()
                if (left <= 0) {
                    finish()
                    break
                }
                remaining.value = left
                delay(250)
            }
        }
    }

    fun pause() {
        job?.cancel()
        running.value = false
    }

    fun reset() {
        job?.cancel()
        running.value = false
        remaining.value = totalSeconds.value
    }

    private fun finish() {
        running.value = false
        remaining.value = totalSeconds.value
        val count = loadSessions() + 1
        prefs.edit().putLong("focus_day", today()).putInt("focus_count", count).apply()
        sessionsToday.value = count
        Reminders.notify(ctx, 77_000, "Focus", "Sessione completata! Fai una pausa.")
    }
}
