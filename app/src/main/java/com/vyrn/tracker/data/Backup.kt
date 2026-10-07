package com.vyrn.tracker.data

import android.content.Context
import android.net.Uri
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.withTransaction
import com.google.gson.Gson
import com.vyrn.tracker.notify.Reminders
import com.vyrn.tracker.widget.refreshWidget
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.time.LocalDate

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

    @Query("DELETE FROM habit_logs") suspend fun wipeHabitLogs()
    @Query("DELETE FROM habits") suspend fun wipeHabits()
    @Query("DELETE FROM routine_step_logs") suspend fun wipeRoutineStepLogs()
    @Query("DELETE FROM routine_steps") suspend fun wipeRoutineSteps()
    @Query("DELETE FROM routines") suspend fun wipeRoutines()
    @Query("DELETE FROM tasks") suspend fun wipeTasks()
    @Query("DELETE FROM transactions") suspend fun wipeTransactions()
    @Query("DELETE FROM budgets") suspend fun wipeBudgets()
    @Query("DELETE FROM goals") suspend fun wipeGoals()
    @Query("DELETE FROM recurring") suspend fun wipeRecurring()
    @Query("DELETE FROM debts") suspend fun wipeDebts()
    @Query("DELETE FROM categories") suspend fun wipeCategories()
    @Query("DELETE FROM accounts") suspend fun wipeAccounts()

    // Riparazione di righe orfane (riferimenti a elementi che non esistono più)
    @Query("DELETE FROM habit_logs WHERE habitId NOT IN (SELECT id FROM habits)") suspend fun orphanHabitLogs(): Int
    @Query("DELETE FROM routine_steps WHERE routineId NOT IN (SELECT id FROM routines)") suspend fun orphanSteps(): Int
    @Query("DELETE FROM routine_step_logs WHERE stepId NOT IN (SELECT id FROM routine_steps)") suspend fun orphanStepLogs(): Int
    @Query("DELETE FROM transactions WHERE accountId NOT IN (SELECT id FROM accounts)") suspend fun orphanTxAccount(): Int
    @Query("DELETE FROM transactions WHERE type = 2 AND (toAccountId IS NULL OR toAccountId NOT IN (SELECT id FROM accounts))")
    suspend fun orphanTransfers(): Int
    @Query("UPDATE transactions SET categoryId = NULL WHERE categoryId IS NOT NULL AND categoryId NOT IN (SELECT id FROM categories)")
    suspend fun orphanTxCategory(): Int
    @Query("DELETE FROM budgets WHERE categoryId NOT IN (SELECT id FROM categories)") suspend fun orphanBudgets(): Int
    @Query("DELETE FROM recurring WHERE accountId NOT IN (SELECT id FROM accounts)") suspend fun orphanRecurring(): Int

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

    /** Istantanea completa dei dati in JSON. */
    suspend fun snapshotJson(ctx: Context): String {
        val dao = AppDatabase.get(ctx).backupDao()
        return Gson().toJson(
            Snapshot(
                FORMAT, dao.habits(), dao.habitLogs(), dao.routines(), dao.routineSteps(), dao.routineStepLogs(),
                dao.tasks(), dao.accounts(), dao.categories(), dao.transactions(), dao.budgets(), dao.goals(),
                dao.recurring(), dao.debts(),
            ),
        )
    }

    suspend fun exportTo(ctx: Context, uri: Uri): Boolean = withContext(Dispatchers.IO) {
        try {
            val json = snapshotJson(ctx)
            ctx.contentResolver.openOutputStream(uri)?.use { it.write(json.toByteArray(Charsets.UTF_8)) } != null
        } catch (e: Exception) {
            false
        }
    }

    suspend fun importFrom(ctx: Context, uri: Uri): Boolean = withContext(Dispatchers.IO) {
        try {
            val text = ctx.contentResolver.openInputStream(uri)?.use { it.readBytes().toString(Charsets.UTF_8) }
                ?: return@withContext false
            restoreFromText(ctx, text)
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Sostituisce TUTTI i dati attuali con quelli del backup, in un'unica transazione: se qualcosa va storto
     * i dati attuali restano intatti. Restituisce false se il file non è valido.
     */
    suspend fun restoreFromText(ctx: Context, json: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val s = Gson().fromJson(json.removePrefix("\uFEFF"), Snapshot::class.java) ?: return@withContext false
            if (s.format != FORMAT) return@withContext false
            val db = AppDatabase.get(ctx)
            val dao = db.backupDao()
            db.withTransaction {
                dao.wipeHabitLogs(); dao.wipeHabits()
                dao.wipeRoutineStepLogs(); dao.wipeRoutineSteps(); dao.wipeRoutines()
                dao.wipeTasks(); dao.wipeTransactions(); dao.wipeBudgets(); dao.wipeGoals()
                dao.wipeRecurring(); dao.wipeDebts(); dao.wipeCategories(); dao.wipeAccounts()
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
            }
            Reminders.rescheduleAll(ctx)
            refreshWidget(ctx)
            true
        } catch (e: Exception) {
            false
        }
    }
}

/** Controlli di integrità: elimina righe che puntano a elementi inesistenti. */
object Maintenance {
    suspend fun repairOrphans(db: AppDatabase): Int {
        val dao = db.backupDao()
        return db.withTransaction {
            dao.orphanHabitLogs() + dao.orphanSteps() + dao.orphanStepLogs() + dao.orphanTxAccount() +
                dao.orphanTransfers() + dao.orphanTxCategory() + dao.orphanBudgets() + dao.orphanRecurring()
        }
    }
}

/** Backup automatico giornaliero nella memoria privata dell'app (ultimi 7 giorni). */
object AutoBackup {
    private const val KEEP = 7
    private const val PREFS = "vyrn_prefs"

    private fun dir(ctx: Context) = File(ctx.filesDir, "backups")

    fun list(ctx: Context): List<File> =
        dir(ctx).listFiles { f -> f.name.startsWith("vyrn-auto-") && f.name.endsWith(".json") }
            ?.sortedByDescending { it.name }.orEmpty()

    /** Crea il backup di oggi se non esiste ancora; elimina i più vecchi. */
    suspend fun runIfDue(ctx: Context) = withContext(Dispatchers.IO) {
        try {
            val today = LocalDate.now()
            val prefs = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            if (prefs.getLong("auto_backup_day", -1L) == today.toEpochDay()) return@withContext
            val d = dir(ctx).also { it.mkdirs() }
            File(d, "vyrn-auto-$today.json").writeText(Backup.snapshotJson(ctx), Charsets.UTF_8)
            prefs.edit().putLong("auto_backup_day", today.toEpochDay()).apply()
            list(ctx).drop(KEEP).forEach { it.delete() }
        } catch (_: Exception) {
            // Il backup automatico è un'aggiunta: un errore non deve bloccare l'app.
        }
    }
}
