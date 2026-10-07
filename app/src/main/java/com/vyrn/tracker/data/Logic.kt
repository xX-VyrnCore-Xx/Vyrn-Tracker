package com.vyrn.tracker.data

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

// ---------- Formattazione ----------

private val itLocale: Locale = Locale.ITALY

fun formatMoney(cents: Long): String = NumberFormat.getCurrencyInstance(itLocale).format(cents / 100.0)

/** Interpreta "12,50" / "12.50" / "-3" come centesimi. Restituisce null se non valido. */
fun parseCents(text: String): Long? {
    val t = text.trim().replace(',', '.')
    val bd = t.toBigDecimalOrNull() ?: return null
    return bd.multiply(BigDecimal(100)).setScale(0, RoundingMode.HALF_UP).toLong()
}

fun centsToInput(cents: Long): String = String.format(Locale.US, "%.2f", cents / 100.0).replace('.', ',')

fun formatNumber(v: Double): String =
    if (v == Math.floor(v) && !v.isInfinite()) v.toLong().toString() else String.format(itLocale, "%.1f", v)

fun parseDoubleIt(text: String): Double? = text.trim().replace(',', '.').toDoubleOrNull()

fun fmtTime(minutes: Int): String =
    if (minutes < 0) "--:--" else String.format(Locale.US, "%02d:%02d", minutes / 60, minutes % 60)

private val dayFormatter = DateTimeFormatter.ofPattern("EEE d MMM", itLocale)
private val dayLongFormatter = DateTimeFormatter.ofPattern("EEEE d MMMM yyyy", itLocale)
private val monthFormatter = DateTimeFormatter.ofPattern("LLLL yyyy", itLocale)
private val monthShortFormatter = DateTimeFormatter.ofPattern("MMM", itLocale)

fun fmtDay(epochDay: Long): String = LocalDate.ofEpochDay(epochDay).format(dayFormatter)
fun fmtDayLong(epochDay: Long): String = LocalDate.ofEpochDay(epochDay).format(dayLongFormatter)
fun fmtMonth(ym: YearMonth): String = ym.format(monthFormatter).replaceFirstChar { it.uppercase() }
fun fmtMonthShort(ym: YearMonth): String = ym.format(monthShortFormatter).replaceFirstChar { it.uppercase() }

val WEEKDAY_SHORT = listOf("Lun", "Mar", "Mer", "Gio", "Ven", "Sab", "Dom")
val WEEKDAY_LETTER = listOf("L", "M", "M", "G", "V", "S", "D")

fun weekdaysText(mask: Int): String =
    if (mask == 127) "Ogni giorno"
    else if (mask == 0) "Nessun giorno"
    else WEEKDAY_SHORT.filterIndexed { i, _ -> (mask shr i) and 1 == 1 }.joinToString(" ")

fun isWeekdayOn(mask: Int, date: LocalDate): Boolean = ((mask shr (date.dayOfWeek.value - 1)) and 1) == 1

// ---------- Abitudini ----------

object HabitLogic {
    fun isScheduled(h: Habit, d: LocalDate): Boolean =
        if (h.freqType == 1) isWeekdayOn(h.weekdaysMask, d) else true

    /** Per le abitudini "da evitare" (kind 2) il giorno è riuscito se NON c'è una ricaduta registrata. */
    fun isDone(h: Habit, log: HabitLog?): Boolean =
        if (h.kind == 2) log == null || log.value < 1.0 else log != null && log.value >= h.target

    fun doneDays(h: Habit, logs: Map<Long, HabitLog>): Set<Long> {
        if (h.kind == 2) {
            val end = LocalDate.now().toEpochDay()
            val start = maxOf(h.createdDay, end - 3650)
            if (end < start) return emptySet()
            return (start..end).filter { d -> (logs[d]?.value ?: 0.0) < 1.0 }.toSet()
        }
        return logs.filterValues { it.value >= h.target }.keys
    }

    private fun weekStart(d: LocalDate): LocalDate = d.minusDays((d.dayOfWeek.value - 1).toLong())

    /** Completamenti nella settimana (lun-dom) che contiene [d]. */
    fun weekCount(done: Set<Long>, d: LocalDate): Int {
        val ws = weekStart(d)
        return (0..6).count { ws.plusDays(it.toLong()).toEpochDay() in done }
    }

    /** Serie attuale: giorni consecutivi (o settimane per "X volte a settimana"). */
    fun streak(h: Habit, done: Set<Long>, today: LocalDate): Int {
        if (done.isEmpty()) return 0
        if (h.freqType == 2) {
            val n = h.timesPerWeek.coerceAtLeast(1)
            var count = 0
            var ws = weekStart(today)
            if (weekCount(done, ws) >= n) count++
            ws = ws.minusWeeks(1)
            var guard = 0
            while (guard++ < 520) {
                if (weekCount(done, ws) >= n) {
                    count++
                    ws = ws.minusWeeks(1)
                } else break
            }
            return count
        }
        var d = today
        if (!(isScheduled(h, today) && today.toEpochDay() in done)) d = today.minusDays(1)
        var count = 0
        var guard = 0
        while (guard++ < 4000) {
            if (!isScheduled(h, d)) {
                d = d.minusDays(1)
                continue
            }
            if (d.toEpochDay() in done) {
                count++
                d = d.minusDays(1)
            } else break
        }
        return count
    }

    fun streakUnit(h: Habit): String = when {
        h.kind == 2 -> "gg liberi"
        h.freqType == 2 -> "sett."
        else -> "gg"
    }

    fun frequencyText(h: Habit): String = if (h.kind == 2) "Da evitare" else when (h.freqType) {
        1 -> weekdaysText(h.weekdaysMask)
        2 -> "${h.timesPerWeek} volte a settimana"
        else -> "Ogni giorno"
    }

    fun step(h: Habit): Double = when {
        h.target >= 1000 -> 500.0
        h.target >= 100 -> 50.0
        h.target >= 20 -> 5.0
        else -> 1.0
    }
}

// ---------- Finanza ----------

fun computeBalances(accounts: List<Account>, txs: List<FinTx>): Map<Long, Long> {
    val m = accounts.associate { it.id to it.initialCents }.toMutableMap()
    for (t in txs) {
        when (t.type) {
            TxType.INCOME -> m[t.accountId] = (m[t.accountId] ?: 0L) + t.amountCents
            TxType.EXPENSE -> m[t.accountId] = (m[t.accountId] ?: 0L) - t.amountCents
            else -> {
                m[t.accountId] = (m[t.accountId] ?: 0L) - t.amountCents
                t.toAccountId?.let { to -> m[to] = (m[to] ?: 0L) + t.amountCents }
            }
        }
    }
    return m
}

fun monthRange(ym: YearMonth): LongRange = ym.atDay(1).toEpochDay()..ym.atEndOfMonth().toEpochDay()

fun periodText(period: Int): String = when (period) {
    0 -> "Settimanale"
    1 -> "Mensile"
    else -> "Annuale"
}

fun advanceDay(day: Long, period: Int, anchor: Int): Long {
    val d = LocalDate.ofEpochDay(day)
    return when (period) {
        0 -> d.plusWeeks(1).toEpochDay()
        1 -> {
            val ym = YearMonth.from(d).plusMonths(1)
            ym.atDay(minOf(anchor.coerceAtLeast(1), ym.lengthOfMonth())).toEpochDay()
        }
        else -> d.plusYears(1).toEpochDay()
    }
}

/** Genera i movimenti delle spese/entrate ricorrenti scadute fino a oggi. */
suspend fun processRecurring(db: AppDatabase) {
    val dao = db.financeDao()
    val today = LocalDate.now().toEpochDay()
    val accountIds = dao.getAccounts().map { it.id }.toSet()
    for (r in dao.getActiveRecurring()) {
        if (r.accountId !in accountIds) continue
        var next = r.nextDay
        var guard = 0
        while (next <= today && guard++ < 400) {
            dao.insertTx(
                FinTx(
                    accountId = r.accountId,
                    categoryId = r.categoryId,
                    type = r.type,
                    amountCents = r.amountCents,
                    day = next,
                    note = r.name,
                    recurringId = r.id,
                ),
            )
            next = advanceDay(next, r.period, r.anchor)
        }
        if (next != r.nextDay) dao.updateRecurring(r.copy(nextDay = next))
    }
}

/** Crea conto e categorie predefinite al primo avvio. */
suspend fun seedDefaults(db: AppDatabase) {
    val dao = db.financeDao()
    if (dao.getAccounts().isEmpty()) dao.insertAccount(Account(name = "Contanti", icon = "💵", colorIdx = 2))
    if (dao.getCategories().isEmpty()) {
        val expense = listOf(
            "🛒" to "Spesa", "🍽️" to "Ristoranti", "🚗" to "Trasporti", "🏠" to "Casa", "💡" to "Bollette",
            "🎉" to "Svago", "👕" to "Shopping", "💊" to "Salute", "📚" to "Istruzione", "✈️" to "Viaggi",
            "📦" to "Altro",
        )
        expense.forEachIndexed { i, (icon, name) ->
            dao.insertCategory(Category(name = name, icon = icon, colorIdx = i, isIncome = false))
        }
        val income = listOf("💼" to "Stipendio", "🎁" to "Regalo", "📈" to "Investimenti", "💰" to "Altre entrate")
        income.forEachIndexed { i, (icon, name) ->
            dao.insertCategory(Category(name = name, icon = icon, colorIdx = i + 2, isIncome = true))
        }
    }
}
