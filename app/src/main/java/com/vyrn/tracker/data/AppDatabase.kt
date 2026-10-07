package com.vyrn.tracker.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import java.io.File

private const val DB_NAME = "vyrn.db"
private const val DB_VERSION = 4

/** v2: sfida (serie da raggiungere) sulle abitudini. */
private val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE habits ADD COLUMN goalDays INTEGER NOT NULL DEFAULT 0")
    }
}

/** v3: indici per rendere veloci le query per data, conto, categoria e relazioni. */
private val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_habit_logs_day` ON `habit_logs` (`day`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_routine_steps_routineId` ON `routine_steps` (`routineId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_routine_step_logs_day` ON `routine_step_logs` (`day`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_tasks_dueDay` ON `tasks` (`dueDay`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_transactions_day` ON `transactions` (`day`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_transactions_accountId` ON `transactions` (`accountId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_transactions_categoryId` ON `transactions` (`categoryId`)")
    }
}

/** v4: foto della ricevuta sui movimenti. */
private val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE transactions ADD COLUMN receipt TEXT")
    }
}

@Database(
    entities = [
        Habit::class, HabitLog::class, Routine::class, RoutineStep::class, RoutineStepLog::class, Task::class,
        Account::class, Category::class, FinTx::class, Budget::class, Goal::class, Recurring::class, Debt::class,
    ],
    version = DB_VERSION,
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
                instance ?: run {
                    val app = context.applicationContext
                    copyBeforeMigration(app)
                    Room.databaseBuilder(app, AppDatabase::class.java, DB_NAME)
                        .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                        .build()
                }.also { instance = it }
            }

        /**
         * Rete di sicurezza: se il database sul telefono è di una versione precedente, ne tiene una copia
         * (`vyrn.db.v<N>.bak`) prima che Room lo aggiorni, così un aggiornamento difettoso non perde i dati.
         */
        private fun copyBeforeMigration(context: Context) {
            try {
                val file = context.getDatabasePath(DB_NAME)
                if (!file.exists()) return
                val version = SQLiteDatabase.openDatabase(file.path, null, SQLiteDatabase.OPEN_READWRITE).use { it.version }
                if (version in 1 until DB_VERSION) {
                    // La chiusura dell'ultima connessione svuota il file WAL nel file principale.
                    file.copyTo(File(file.parentFile, "$DB_NAME.v$version.bak"), overwrite = true)
                }
            } catch (_: Exception) {
                // Nessuna copia possibile: Room procede comunque con la migrazione.
            }
        }
    }
}
