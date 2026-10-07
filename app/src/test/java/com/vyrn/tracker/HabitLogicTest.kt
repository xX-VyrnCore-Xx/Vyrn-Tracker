package com.vyrn.tracker

import com.vyrn.tracker.data.Habit
import com.vyrn.tracker.data.HabitLog
import com.vyrn.tracker.data.HabitLogic
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class HabitLogicTest {
    // Mercoledì 7 ottobre 2026
    private val today = LocalDate.of(2026, 10, 7)

    private fun habit(freq: Int = 0, mask: Int = 127, times: Int = 3, kind: Int = 0, created: Long = today.minusDays(100).toEpochDay()) =
        Habit(id = 1, name = "x", freqType = freq, weekdaysMask = mask, timesPerWeek = times, kind = kind, createdDay = created)

    private fun days(vararg offsets: Int): Set<Long> = offsets.map { today.minusDays(it.toLong()).toEpochDay() }.toSet()

    @Test
    fun streakCountsConsecutiveDays() {
        assertEquals(3, HabitLogic.streak(habit(), days(0, 1, 2), today))
    }

    @Test
    fun streakSurvivesWhenTodayIsNotDoneYet() {
        assertEquals(2, HabitLogic.streak(habit(), days(1, 2), today))
    }

    @Test
    fun streakBreaksOnGap() {
        assertEquals(1, HabitLogic.streak(habit(), days(0, 2, 3), today))
    }

    @Test
    fun streakIsZeroWithoutCompletions() {
        assertEquals(0, HabitLogic.streak(habit(), emptySet(), today))
    }

    @Test
    fun streakSkipsUnscheduledDays() {
        // Solo lunedì (bit 0) e mercoledì (bit 2)
        assertEquals(4, HabitLogic.streak(habit(freq = 1, mask = 5), days(0, 2, 7, 9), today))
    }

    @Test
    fun weeklyHabitCountsWeeksThatReachTheTarget() {
        // 2 volte a settimana: questa settimana (mer + lun) e la precedente (mer + lun)
        assertEquals(2, HabitLogic.streak(habit(freq = 2, times = 2), days(0, 2, 7, 9), today))
    }

    @Test
    fun bestStreakFindsLongestRun() {
        assertEquals(5, HabitLogic.bestStreak(habit(), days(0, 1, 2, 10, 11, 12, 13, 14), today))
    }

    @Test
    fun isScheduledFollowsWeekdayMask() {
        val monAndWed = habit(freq = 1, mask = 5)
        assertTrue(HabitLogic.isScheduled(monAndWed, today)) // mercoledì
        assertFalse(HabitLogic.isScheduled(monAndWed, today.plusDays(1))) // giovedì
        assertTrue(HabitLogic.isScheduled(habit(), today.plusDays(1)))
    }

    @Test
    fun quantityHabitIsDoneOnlyAtTarget() {
        val water = Habit(id = 2, name = "Acqua", kind = 1, target = 8.0)
        assertFalse(HabitLogic.isDone(water, HabitLog(2, 1, 7.0)))
        assertTrue(HabitLogic.isDone(water, HabitLog(2, 1, 8.0)))
        assertFalse(HabitLogic.isDone(water, null))
    }

    @Test
    fun avoidHabitIsDoneUnlessASlipIsLogged() {
        val h = habit(kind = 2)
        assertTrue(HabitLogic.isDone(h, null))
        assertTrue(HabitLogic.isDone(h, HabitLog(1, 1, 0.0, "nota")))
        assertFalse(HabitLogic.isDone(h, HabitLog(1, 1, 1.0)))
    }

    @Test
    fun avoidHabitCountsCleanDaysSinceLastSlip() {
        val now = LocalDate.now()
        val h = habit(kind = 2, created = now.minusDays(3).toEpochDay())
        val slip = now.minusDays(1).toEpochDay()
        val done = HabitLogic.doneDays(h, mapOf(slip to HabitLog(1, slip, 1.0)))
        assertEquals(3, done.size)
        assertFalse(slip in done)
        assertEquals(1, HabitLogic.streak(h, done, now))
    }
}
