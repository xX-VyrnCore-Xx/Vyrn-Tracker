package com.vyrn.tracker.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/** v2: sfida (serie da raggiungere) sulle abitudini. */
private val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE habits ADD COLUMN goalDays INTEGER NOT NULL DEFAULT 0")
    }
}

@Database(
    entities = [
        Habit::class, HabitLog::class, Routine::class, RoutineStep::class, RoutineStepLog::class, Task::class,
        Account::class, Category::class, FinTx::class, Budget::class, Goal::class, Recurring::class, Debt::class,
    ],
    version = 2,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun habitDao(): HabitDao
    abstract fun routineDao(): RoutineDao
    abstract fun taskDao(): TaskDao
    abstract fun financeDao(): FinanceDao
    abstract fun backupDao(): BackupDao

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

        fun get(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, "vyrn.db")
                    .build()
                    .also { instance = it }
            }
    }
}
