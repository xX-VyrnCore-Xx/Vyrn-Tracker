package com.vyrn.tracker

import com.vyrn.tracker.notify.Reminders
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId

class RemindersTest {
    private fun millis(dt: LocalDateTime) = dt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

    // Mercoledì 7 ottobre 2026, ore 10:00
    private val now = LocalDateTime.of(2026, 10, 7, 10, 0)

    @Test
    fun nextTriggerIsLaterTodayWhenScheduled() {
        // lunedì + mercoledì, ore 11:00
        assertEquals(millis(LocalDateTime.of(2026, 10, 7, 11, 0)), Reminders.nextTrigger(5, 11 * 60, now))
    }

    @Test
    fun nextTriggerSkipsToNextScheduledDay() {
        // 09:00 di oggi è già passato: il prossimo è lunedì 12
        assertEquals(millis(LocalDateTime.of(2026, 10, 12, 9, 0)), Reminders.nextTrigger(5, 9 * 60, now))
    }

    @Test
    fun nextTriggerIsNoneWithoutDays() {
        assertEquals(-1L, Reminders.nextTrigger(0, 9 * 60, now))
    }
}
