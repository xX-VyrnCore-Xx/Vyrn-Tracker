package com.vyrn.tracker.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface HabitDao {
    @Query("SELECT * FROM habits WHERE archived = 0 ORDER BY id")
    fun observeHabits(): Flow<List<Habit>>

    @Query("SELECT * FROM habit_logs")
    fun observeLogs(): Flow<List<HabitLog>>

    @Query("SELECT * FROM habits WHERE archived = 0")
    suspend fun getHabits(): List<Habit>

    @Query("SELECT * FROM habit_logs WHERE day = :day")
    suspend fun getLogsForDay(day: Long): List<HabitLog>

    @Query("SELECT * FROM habit_logs WHERE habitId = :habitId AND day = :day")
    suspend fun getLog(habitId: Long, day: Long): HabitLog?

    @Insert
    suspend fun insert(habit: Habit): Long

    @Update
    suspend fun update(habit: Habit)

    @Query("DELETE FROM habits WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM habit_logs WHERE habitId = :id")
    suspend fun deleteLogs(id: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertLog(log: HabitLog)

    @Query("DELETE FROM habit_logs WHERE habitId = :habitId AND day = :day")
    suspend fun deleteLog(habitId: Long, day: Long)
}

@Dao
interface RoutineDao {
    @Query("SELECT * FROM routines ORDER BY period, id")
    fun observeRoutines(): Flow<List<Routine>>

    @Query("SELECT * FROM routine_steps ORDER BY routineId, position")
    fun observeSteps(): Flow<List<RoutineStep>>

    @Query("SELECT * FROM routine_step_logs")
    fun observeStepLogs(): Flow<List<RoutineStepLog>>

    @Query("SELECT * FROM routines")
    suspend fun getRoutines(): List<Routine>

    @Insert
    suspend fun insertRoutine(routine: Routine): Long

    @Update
    suspend fun updateRoutine(routine: Routine)

    @Query("DELETE FROM routines WHERE id = :id")
    suspend fun deleteRoutine(id: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSteps(steps: List<RoutineStep>)

    @Query("DELETE FROM routine_steps WHERE routineId = :routineId AND id NOT IN (:keepIds)")
    suspend fun deleteStepsNotIn(routineId: Long, keepIds: List<Long>)

    @Query("DELETE FROM routine_steps WHERE routineId = :routineId")
    suspend fun deleteSteps(routineId: Long)

    @Query("DELETE FROM routine_step_logs WHERE stepId IN (SELECT id FROM routine_steps WHERE routineId = :routineId)")
    suspend fun deleteStepLogsForRoutine(routineId: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStepLog(log: RoutineStepLog)

    @Query("DELETE FROM routine_step_logs WHERE stepId = :stepId AND day = :day")
    suspend fun deleteStepLog(stepId: Long, day: Long)
}

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks ORDER BY id DESC")
    fun observeTasks(): Flow<List<Task>>

    @Query("SELECT * FROM tasks")
    suspend fun getTasks(): List<Task>

    @Insert
    suspend fun insert(task: Task): Long

    @Update
    suspend fun update(task: Task)

    @Query("DELETE FROM tasks WHERE id = :id")
    suspend fun delete(id: Long)
}

@Dao
interface FinanceDao {
    // Conti
    @Query("SELECT * FROM accounts ORDER BY id")
    fun observeAccounts(): Flow<List<Account>>

    @Query("SELECT * FROM accounts ORDER BY id")
    suspend fun getAccounts(): List<Account>

    @Insert
    suspend fun insertAccount(account: Account): Long

    @Update
    suspend fun updateAccount(account: Account)

    @Query("DELETE FROM accounts WHERE id = :id")
    suspend fun deleteAccount(id: Long)

    @Query("DELETE FROM transactions WHERE accountId = :id OR toAccountId = :id")
    suspend fun deleteTxForAccount(id: Long)

    @Query("DELETE FROM recurring WHERE accountId = :id")
    suspend fun deleteRecurringForAccount(id: Long)

    // Categorie
    @Query("SELECT * FROM categories ORDER BY isIncome, id")
    fun observeCategories(): Flow<List<Category>>

    @Query("SELECT * FROM categories ORDER BY isIncome, id")
    suspend fun getCategories(): List<Category>

    @Insert
    suspend fun insertCategory(category: Category): Long

    @Update
    suspend fun updateCategory(category: Category)

    @Query("DELETE FROM categories WHERE id = :id")
    suspend fun deleteCategory(id: Long)

    @Query("UPDATE transactions SET categoryId = NULL WHERE categoryId = :id")
    suspend fun clearCategoryInTx(id: Long)

    @Query("UPDATE recurring SET categoryId = NULL WHERE categoryId = :id")
    suspend fun clearCategoryInRecurring(id: Long)

    @Query("DELETE FROM budgets WHERE categoryId = :id")
    suspend fun deleteBudget(id: Long)

    // Movimenti
    @Query("SELECT * FROM transactions ORDER BY day DESC, id DESC")
    fun observeTx(): Flow<List<FinTx>>

    @Query("SELECT * FROM transactions")
    suspend fun getAllTx(): List<FinTx>

    @Insert
    suspend fun insertTx(tx: FinTx): Long

    @Insert
    suspend fun insertAllTx(list: List<FinTx>)

    @Update
    suspend fun updateTx(tx: FinTx)

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteTx(id: Long)

    // Budget
    @Query("SELECT * FROM budgets")
    fun observeBudgets(): Flow<List<Budget>>

    @Query("SELECT * FROM budgets")
    suspend fun getBudgets(): List<Budget>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertBudget(budget: Budget)

    // Obiettivi
    @Query("SELECT * FROM goals ORDER BY id")
    fun observeGoals(): Flow<List<Goal>>

    @Insert
    suspend fun insertGoal(goal: Goal): Long

    @Update
    suspend fun updateGoal(goal: Goal)

    @Query("DELETE FROM goals WHERE id = :id")
    suspend fun deleteGoal(id: Long)

    // Ricorrenti
    @Query("SELECT * FROM recurring ORDER BY id")
    fun observeRecurring(): Flow<List<Recurring>>

    @Query("SELECT * FROM recurring WHERE active = 1")
    suspend fun getActiveRecurring(): List<Recurring>

    @Insert
    suspend fun insertRecurring(r: Recurring): Long

    @Update
    suspend fun updateRecurring(r: Recurring)

    @Query("DELETE FROM recurring WHERE id = :id")
    suspend fun deleteRecurring(id: Long)

    // Debiti
    @Query("SELECT * FROM debts ORDER BY id DESC")
    fun observeDebts(): Flow<List<Debt>>

    @Insert
    suspend fun insertDebt(d: Debt): Long

    @Update
    suspend fun updateDebt(d: Debt)

    @Query("DELETE FROM debts WHERE id = :id")
    suspend fun deleteDebt(id: Long)
}
