package com.vyrn.tracker

import android.app.Application
import com.vyrn.tracker.data.AppDatabase
import com.vyrn.tracker.data.AutoBackup
import com.vyrn.tracker.data.Maintenance
import com.vyrn.tracker.data.processRecurring
import com.vyrn.tracker.data.seedDefaults
import com.vyrn.tracker.notify.Reminders
import com.vyrn.tracker.widget.refreshWidget
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class VyrnApplication : Application() {
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        Reminders.ensureChannel(this)
        appScope.launch {
            val db = AppDatabase.get(this@VyrnApplication)
            Maintenance.repairOrphans(db)
            seedDefaults(db)
            processRecurring(db)
            Reminders.rescheduleAll(this@VyrnApplication)
            refreshWidget(this@VyrnApplication)
            AutoBackup.runIfDue(this@VyrnApplication)
        }
    }
}
