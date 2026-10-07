package com.vyrn.tracker.data

import android.content.Context
import android.net.Uri
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.google.gson.Gson
import com.vyrn.tracker.notify.Reminders
import com.vyrn.tracker.widget.refreshWidget
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Dao
interface BackupDao {
    @Query("SELECT * FROM habits") suspend fun habits(): List<Habit>
    @Query("SELECT * FROM habit_logs") suspend fun habitLogs(): List<HabitLog>
    @Query("SELECT * FROM routines") suspend fun routines(): List<Routine>
    @Query("SELECT * FROM routine_steps") suspend fun routineSteps(): List<RoutineStep>
    @Query("SELECT * FROM routine_step_logs") suspend fun routineStepLogs(): List<RoutineStepLog>
    @Query("SELECT * FROM tasks") suspend fun tasks(): List<Task>
    @Query("SELECT * FROM accounts") suspend fun accounts(): List<Account>
    @Query("SELECT * FROM categories") suspend fun categories(): List<Category>
    @Query("SELECT * FROM transactions") suspend fun transactions(): List<FinTx>
    @Query("SELECT * FROM budgets") suspend fun budgets(): List<Budget>
    @Query("SELECT * FROM goals") suspend fun goals(): List<Goal>
    @Query("SELECT * FROM recurring") suspend fun recurring(): List<Recurring>
    @Query("SELECT * FROM debts") suspend fun debts(): List<Debt>

    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun putHabits(l: List<Habit>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun putHabitLogs(l: List<HabitLog>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun putRoutines(l: List<Routine>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun putRoutineSteps(l: List<RoutineStep>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun putRoutineStepLogs(l: List<RoutineStepLog>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun putTasks(l: List<Task>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun putAccounts(l: List<Account>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun putCategories(l: List<Category>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun putTransactions(l: List<FinTx>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun putBudgets(l: List<Budget>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun putGoals(l: List<Goal>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun putRecurring(l: List<Recurring>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun putDebts(l: List<Debt>)
}

/** Backup completo in JSON: si salva dove vuoi (anche su Google Drive tramite il selettore file). */
object Backup {
    private const val FORMAT = 1

    private data class Snapshot(
        val format: Int,
        val habits: List<Habit>?,
        val habitLogs: List<HabitLog>?,
        val routines: List<Routine>?,
        val routineSteps: List<RoutineStep>?,
        val routineStepLogs: List<RoutineStepLog>?,
        val tasks: List<Task>?,
        val accounts: List<Account>?,
        val categories: List<Category>?,
        val transactions: List<FinTx>?,
        val budgets: List<Budget>?,
        val goals: List<Goal>?,
        val recurring: List<Recurring>?,
        val debts: List<Debt>?,
    )

    suspend fun exportTo(ctx: Context, uri: Uri): Boolean = withContext(Dispatchers.IO) {
        try {
            val dao = AppDatabase.get(ctx).backupDao()
            val snapshot = Snapshot(
                FORMAT, dao.habits(), dao.habitLogs(), dao.routines(), dao.routineSteps(), dao.routineStepLogs(),
                dao.tasks(), dao.accounts(), dao.categories(), dao.transactions(), dao.budgets(), dao.goals(),
                dao.recurring(), dao.debts(),
            )
            val json = Gson().toJson(snapshot)
            ctx.contentResolver.openOutputStream(uri)?.use { it.write(json.toByteArray(Charsets.UTF_8)) } != null
        } catch (e: Exception) {
            false
        }
    }

    /** Sostituisce TUTTI i dati attuali con quelli del backup. Restituisce false se il file non è valido. */
    suspend fun importFrom(ctx: Context, uri: Uri): Boolean = withContext(Dispatchers.IO) {
        try {
            val text = ctx.contentResolver.openInputStream(uri)?.use { it.readBytes().toString(Charsets.UTF_8) }
                ?: return@withContext false
            val s = Gson().fromJson(text.removePrefix("﻿"), Snapshot::class.java) ?: return@withContext false
            if (s.format != FORMAT) return@withContext false
            val db = AppDatabase.get(ctx)
            db.clearAllTables()
            val dao = db.backupDao()
            dao.putHabits(s.habits.orEmpty())
            dao.putHabitLogs(s.habitLogs.orEmpty())
            dao.putRoutines(s.routines.orEmpty())
            dao.putRoutineSteps(s.routineSteps.orEmpty())
            dao.putRoutineStepLogs(s.routineStepLogs.orEmpty())
            dao.putTasks(s.tasks.orEmpty())
            dao.putAccounts(s.accounts.orEmpty())
            dao.putCategories(s.categories.orEmpty())
            dao.putTransactions(s.transactions.orEmpty())
            dao.putBudgets(s.budgets.orEmpty())
            dao.putGoals(s.goals.orEmpty())
            dao.putRecurring(s.recurring.orEmpty())
            dao.putDebts(s.debts.orEmpty())
            Reminders.rescheduleAll(ctx)
            refreshWidget(ctx)
            true
        } catch (e: Exception) {
            false
        }
    }
}
