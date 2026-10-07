package com.vyrn.tracker.data

import androidx.room.Entity
import androidx.room.PrimaryKey

// ---------- Routine ----------

/** kind: 0 = sì/no, 1 = quantitativa. freqType: 0 = ogni giorno, 1 = giorni specifici, 2 = X volte a settimana. */
@Entity(tableName = "habits")
data class Habit(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val icon: String = "✅",
    val colorIdx: Int = 0,
    val kind: Int = 0,
    val target: Double = 1.0,
    val unit: String = "",
    val freqType: Int = 0,
    val weekdaysMask: Int = 127,
    val timesPerWeek: Int = 3,
    val reminderMinutes: Int = -1,
    val archived: Boolean = false,
    val createdDay: Long = 0,
)

@Entity(tableName = "habit_logs", primaryKeys = ["habitId", "day"])
data class HabitLog(
    val habitId: Long,
    val day: Long,
    val value: Double,
    val note: String = "",
)

/** period: 0 = mattina, 1 = pomeriggio, 2 = sera. */
@Entity(tableName = "routines")
data class Routine(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val icon: String = "🌅",
    val colorIdx: Int = 1,
    val period: Int = 0,
    val weekdaysMask: Int = 127,
    val reminderMinutes: Int = -1,
)

@Entity(tableName = "routine_steps")
data class RoutineStep(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val routineId: Long,
    val title: String,
    val durationMin: Int = 0,
    val position: Int = 0,
)

@Entity(tableName = "routine_step_logs", primaryKeys = ["stepId", "day"])
data class RoutineStepLog(
    val stepId: Long,
    val day: Long,
)

/** priority: 0 = bassa, 1 = media, 2 = alta. dueDay = -1 se senza scadenza. */
@Entity(tableName = "tasks")
data class Task(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val notes: String = "",
    val dueDay: Long = -1,
    val dueMinutes: Int = -1,
    val priority: Int = 1,
    val done: Boolean = false,
    val reminder: Boolean = false,
)

// ---------- Finanza ----------

object TxType {
    const val INCOME = 0
    const val EXPENSE = 1
    const val TRANSFER = 2
}

@Entity(tableName = "accounts")
data class Account(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val icon: String = "💳",
    val colorIdx: Int = 0,
    val initialCents: Long = 0,
)

@Entity(tableName = "categories")
data class Category(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val icon: String = "🏷️",
    val colorIdx: Int = 0,
    val isIncome: Boolean = false,
)

@Entity(tableName = "transactions")
data class FinTx(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val accountId: Long,
    val categoryId: Long? = null,
    val type: Int,
    val amountCents: Long,
    val toAccountId: Long? = null,
    val day: Long,
    val note: String = "",
    val recurringId: Long? = null,
)

@Entity(tableName = "budgets")
data class Budget(
    @PrimaryKey val categoryId: Long,
    val limitCents: Long,
)

@Entity(tableName = "goals")
data class Goal(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val icon: String = "🎯",
    val colorIdx: Int = 0,
    val targetCents: Long,
    val savedCents: Long = 0,
    val deadlineDay: Long = -1,
)

/** type: 0 = entrata, 1 = uscita. period: 0 = settimanale, 1 = mensile, 2 = annuale. */
@Entity(tableName = "recurring")
data class Recurring(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val type: Int = 1,
    val amountCents: Long,
    val accountId: Long,
    val categoryId: Long? = null,
    val period: Int = 1,
    val nextDay: Long,
    val anchor: Int = 1,
    val active: Boolean = true,
)

/** direction: 0 = qualcuno mi deve dei soldi, 1 = devo dei soldi io. */
@Entity(tableName = "debts")
data class Debt(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val person: String,
    val totalCents: Long,
    val paidCents: Long = 0,
    val direction: Int = 0,
    val note: String = "",
    val dueDay: Long = -1,
)
