package com.example.moneylover

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.lifecycle.lifecycleScope
import com.example.moneylover.data.database.AppDatabase
import com.example.moneylover.data.repository.ReminderRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            // Reschedule all reminders after boot
            rescheduleAllReminders(context)
        }
    }

    private fun rescheduleAllReminders(context: Context) {
        val database = AppDatabase.getDatabase(context)
        val repository = ReminderRepository(database.reminderDao())

        // Use pending result to keep broadcast alive
        val pendingResult = goAsync()

        kotlinx.coroutines.GlobalScope.launch {
            try {
                val reminders = repository.getEnabledReminders().first()
                reminders.forEach { reminder ->
                    ReminderScheduler.scheduleReminder(context, reminder)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
