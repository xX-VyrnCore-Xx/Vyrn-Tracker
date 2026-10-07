package com.vyrn.tracker

import com.vyrn.tracker.data.Account
import com.vyrn.tracker.data.FinTx
import com.vyrn.tracker.data.TxType
import com.vyrn.tracker.data.advanceDay
import com.vyrn.tracker.data.centsToInput
import com.vyrn.tracker.data.computeBalances
import com.vyrn.tracker.data.fmtTime
import com.vyrn.tracker.data.parseCents
import com.vyrn.tracker.data.weekdaysText
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class LogicTest {
    @Test
    fun parseCentsAcceptsCommaAndDot() {
        assertEquals(1250L, parseCents("12,50"))
        assertEquals(1250L, parseCents("12.5"))
        assertEquals(-300L, parseCents("-3"))
        assertEquals(0L, parseCents("0"))
        assertNull(parseCents("abc"))
        assertNull(parseCents(""))
    }

    @Test
    fun centsToInputUsesComma() {
        assertEquals("12,50", centsToInput(1250))
        assertEquals("0,05", centsToInput(5))
    }

    @Test
    fun fmtTimePadsAndHandlesNone() {
        assertEquals("01:05", fmtTime(65))
        assertEquals("--:--", fmtTime(-1))
    }

    @Test
    fun weekdaysTextDescribesMask() {
        assertEquals("Ogni giorno", weekdaysText(127))
        assertEquals("Nessun giorno", weekdaysText(0))
        assertEquals("Lun Mer", weekdaysText(5))
    }

    @Test
    fun monthlyRecurrenceClampsToEndOfMonth() {
        val jan31 = LocalDate.of(2026, 1, 31).toEpochDay()
        val feb = advanceDay(jan31, 1, 31)
        assertEquals(LocalDate.of(2026, 2, 28).toEpochDay(), feb)
        assertEquals(LocalDate.of(2026, 3, 31).toEpochDay(), advanceDay(feb, 1, 31))
    }

    @Test
    fun weeklyAndYearlyRecurrence() {
        val d = LocalDate.of(2026, 10, 7).toEpochDay()
        assertEquals(LocalDate.of(2026, 10, 14).toEpochDay(), advanceDay(d, 0, 7))
        assertEquals(LocalDate.of(2027, 10, 7).toEpochDay(), advanceDay(d, 2, 7))
    }

    @Test
    fun balancesIncludeIncomeExpenseAndTransfers() {
        val a = Account(id = 1, name = "A", initialCents = 1000)
        val b = Account(id = 2, name = "B")
        val day = LocalDate.of(2026, 10, 7).toEpochDay()
        val txs = listOf(
            FinTx(accountId = 1, type = TxType.INCOME, amountCents = 500, day = day),
            FinTx(accountId = 1, type = TxType.EXPENSE, amountCents = 200, day = day),
            FinTx(accountId = 1, type = TxType.TRANSFER, amountCents = 300, toAccountId = 2, day = day),
        )
        val balances = computeBalances(listOf(a, b), txs)
        assertEquals(1000L, balances[1L])
        assertEquals(300L, balances[2L])
    }
}
