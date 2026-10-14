package com.example.expensetracker.data.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.expensetracker.MainActivity
import com.example.expensetracker.R

class NotificationHelper(private val context: Context) {
    companion object {
        const val CHANNEL_DAILY = "expense_daily"
        const val CHANNEL_BUDGET = "expense_budget"
        const val DAILY_ID = 1001
        const val WEEKLY_ID = 1003
        const val BUDGET_ID = 1002
    }

    init {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_DAILY, "Daily Reminder", NotificationManager.IMPORTANCE_DEFAULT
            ).apply { description = "Reminds you to log spending at 8PM" }
            context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)

            val budgetChannel = NotificationChannel(
                CHANNEL_BUDGET, "Budget Alert", NotificationManager.IMPORTANCE_HIGH
            ).apply { description = "Alerts you when you've gone over your budget" }
            context.getSystemService(NotificationManager::class.java).createNotificationChannel(budgetChannel)
        }
    }

    fun showDailyReminder() {
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)!= PackageManager.PERMISSION_GRANTED) return

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("open_tab", "transactions") // 👈 Jump to log screen
        }
        val pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE)

        val notification = NotificationCompat.Builder(context, CHANNEL_DAILY)
            .setSmallIcon(R.drawable.outline_notifications_24) // 👈 Add any drawable
            .setContentTitle("Log your spending 💸")
            .setContentText("You haven't added expenses today")
            .setStyle(NotificationCompat.BigTextStyle().bigText("It's 8PM. Don't forget to track today's expenses."))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).notify(DAILY_ID, notification)
    }

    fun showWeeklySummary(total: Float) {
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)!= PackageManager.PERMISSION_GRANTED) return

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("open_tab", "stats") // 👈 Jump to your BarChart screen
        }
        val pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE)

        val notification = NotificationCompat.Builder(context, CHANNEL_DAILY)
            .setSmallIcon(R.drawable.outline_notifications_24)
            .setContentTitle("Weekly Summary 📊")
            .setContentText("You spent R%.2f this week".format(total))
            .setStyle(NotificationCompat.BigTextStyle().bigText("Sunday check-in: R%.2f total from Mon-Sun. Tap to see breakdown.".format(total)))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).notify(WEEKLY_ID, notification) // 👈 Different ID
    }


    fun showBudgetAlert(spent: Float, budget: Float, percent: Int) {
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)!= PackageManager.PERMISSION_GRANTED) return

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("open_tab", "budget") // 👈 Jump to budget screen
        }
        val pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE)

        val notification = NotificationCompat.Builder(context, CHANNEL_BUDGET)
            .setSmallIcon(R.drawable.outline_warning_24) // 👈 Use a warning icon
            .setContentTitle("Budget Alert ⚠️")
            .setContentText("%d%% used: R%.2f / R%.2f".format(percent, spent, budget))
            .setStyle(NotificationCompat.BigTextStyle().bigText("You've used %d%% of your R%.2f monthly budget. Slow down.".format(percent, budget)))
            .setPriority(NotificationCompat.PRIORITY_HIGH) // 👈 High so it pops
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).notify(BUDGET_ID, notification) // Same ID = updates existing alert
    }


}