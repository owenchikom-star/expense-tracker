package com.example.expensetracker.data.notifications

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.expensetracker.data.ExpenseDao
import com.example.expensetracker.data.ExpenseDatabase
import java.util.Calendar
import java.util.TimeZone
import java.util.concurrent.TimeUnit

class DailyReminderWorker @JvmOverloads constructor(
    appContext: Context,
    params: WorkerParameters,
    private val expenseDao: ExpenseDao? = null,
    private val notificationHelper: NotificationHelper? = null,
    private val reschedule: (Context) -> Unit = ::scheduleDailyReminder
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val dao = expenseDao ?: ExpenseDatabase.getDatabase(applicationContext).expenseDao()
        val notifier = notificationHelper ?: NotificationHelper(applicationContext)

        val spentToday = dao.getTotalForDate(todayStartMillis(), System.currentTimeMillis())

        if (spentToday == 0f) { // Only notify if nothing logged
            notifier.showDailyReminder()
        }

        reschedule(applicationContext) // 👈 Reschedule for tomorrow
        return Result.success()
    }

    private fun todayStartMillis(): Long {
        val cal = Calendar.getInstance(TimeZone.getTimeZone("Africa/Johannesburg")).apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }
}

fun scheduleDailyReminder(context: Context, workManager: WorkManager? = null) {
    val wm = workManager ?: WorkManager.getInstance(context)
    val now = Calendar.getInstance(TimeZone.getTimeZone("Africa/Johannesburg"))
    val target = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 20) // 8PM
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        if (before(now)) add(Calendar.DAY_OF_YEAR, 1) // If it's past 8PM, do tomorrow
    }
    val delay = target.timeInMillis - now.timeInMillis

    val workRequest = OneTimeWorkRequestBuilder<DailyReminderWorker>()
        .setInitialDelay(delay, TimeUnit.MILLISECONDS)
        .addTag("daily_8pm")
        .build()

    wm.enqueueUniqueWork("daily_8pm_work", ExistingWorkPolicy.REPLACE, workRequest)
}



