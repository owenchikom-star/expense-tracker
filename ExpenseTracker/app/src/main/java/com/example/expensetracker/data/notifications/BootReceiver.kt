package com.example.expensetracker.data.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.WorkManager

class BootReceiver(private val workManager: WorkManager? = null) : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            scheduleDailyReminder(context, workManager)
            scheduleWeeklySummary(context, false, workManager)
        }
    }
}
