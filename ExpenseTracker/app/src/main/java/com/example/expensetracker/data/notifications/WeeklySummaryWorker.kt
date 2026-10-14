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

class WeeklySummaryWorker @JvmOverloads constructor(
    appContext: Context,
    params: WorkerParameters,
    private val expenseDao: ExpenseDao? = null,
    private val notificationHelper: NotificationHelper? = null,
    private val reschedule: (Context) -> Unit = ::scheduleWeeklySummary
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val dao = expenseDao ?: ExpenseDatabase.getDatabase(applicationContext).expenseDao()
        val notifier = notificationHelper ?: NotificationHelper(applicationContext)

        val tz = TimeZone.getTimeZone("Africa/Johannesburg")
        val now = Calendar.getInstance(tz)

        // Sunday 23:59:59 = endOfWeek
        val endOfWeek = now.apply {
            set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }.timeInMillis

        // Monday 00:00:00 = startOfWeek. 7 days total
        val startOfWeek = Calendar.getInstance(tz).apply {
            timeInMillis = endOfWeek
            add(Calendar.DAY_OF_YEAR, -6) // Go back 6 days = Mon->Sun = 7 days
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val totalSpent = dao.getTotalForWeek(startOfWeek, endOfWeek)

        if (totalSpent > 0f) { // Only notify if they spent something
            notifier.showWeeklySummary(totalSpent)
        }

        reschedule(applicationContext) // Reschedule for next Sunday
        return Result.success()
    }
}

fun scheduleWeeklySummary(context: Context, forTesting: Boolean = false, workManager: WorkManager? = null) {
    val wm = workManager ?: WorkManager.getInstance(context)
    val delay = if (forTesting) {
        1L * 60 * 1000 // 👈 1 min test
    } else {
        val tz = TimeZone.getTimeZone("Africa/Johannesburg")
        val now = Calendar.getInstance(tz)
        val target = Calendar.getInstance(tz).apply {
            set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY) // Next Sunday
            set(Calendar.HOUR_OF_DAY, 19) // 7PM
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            if (before(now)) add(Calendar.WEEK_OF_YEAR, 1) // If Sunday 7PM passed, do next week
        }
        target.timeInMillis - now.timeInMillis
    }

    val workRequest = OneTimeWorkRequestBuilder<WeeklySummaryWorker>()
        .setInitialDelay(delay, TimeUnit.MILLISECONDS)
        .addTag("weekly_sunday")
        .build()

    wm.enqueueUniqueWork("weekly_sunday_work", ExistingWorkPolicy.REPLACE, workRequest)
}
