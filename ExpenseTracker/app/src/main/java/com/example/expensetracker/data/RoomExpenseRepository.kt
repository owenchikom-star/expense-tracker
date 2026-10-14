package com.example.expensetracker.data

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.preference.PreferenceManager
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.expensetracker.data.notifications.NotificationHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.util.TimeZone
import androidx.core.content.edit
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.ZoneId

class RoomExpenseRepository(private val expenseDao: ExpenseDao, private val categoryBudgetDao: CategoryBudgetDao, private val context: Context) : ExpenseRepository {
    override suspend fun insert(expense: Expense) = expenseDao.insert(expense)
    override suspend fun upsert(expense: Expense) = expenseDao.upsert(expense)
    override fun getAllExpenses(): Flow<List<Expense>> = expenseDao.getAllExpenses()
    override suspend fun delete(expense: Expense) = expenseDao.delete(expense)
    override suspend fun update(expense: Expense) = expenseDao.update(expense)
    override fun getExpensesByDateRange(startDate: Long, endDate: Long): Flow<List<Expense>> = expenseDao.getExpensesByDateRange(startDate, endDate)
    override fun getExpensesByCategory(category: String): Flow<List<Expense>> = expenseDao.getExpensesByCategory(category)
    override fun getTotalBetweenDates(startDate: Long, endDate: Long): Flow<Double?> = expenseDao.getTotalBetweenDates(startDate, endDate)
    override fun getFilteredExpenses(category: String, startMs: Long?, endMs: Long?): Flow<List<Expense>> = expenseDao.getFilteredExpenses(category, startMs, endMs)
    override fun getTotalForDate(startOfDay: Long, endOfDay: Long): Float = expenseDao.getTotalForDate(startOfDay, endOfDay)
    override suspend fun getTotalForWeek(startOfWeek: Long, endOfWeek: Long): Float = expenseDao.getTotalForWeek(startOfWeek, endOfWeek)
    override suspend fun getTotalByCategoryAndMonth(category: String, start: Long, end: Long): Float = expenseDao.getTotalByCategoryAndMonth(category, start, end)

    }


class RoomCategoryBudgetRepository(private val categoryBudgetDao: CategoryBudgetDao,private val expenseDao: ExpenseDao, private val context: Context, private val notifier: NotificationHelper = NotificationHelper(context)) : CategoryBudgetRepository {
    override fun getBudgetForMonth(month: Int, year: Int): Flow<List<CategoryBudget>> = categoryBudgetDao.getBudgetForMonth(month, year)
    override suspend fun upsertBudget(budget: CategoryBudget) = categoryBudgetDao.upsertBudget(budget)
    override suspend fun deleteBudget(category: String, month: Int, year: Int) = categoryBudgetDao.deleteBudget(category, month, year)
    override suspend fun getBudgetForOneMonth(category: String, month: Int, year: Int): Double? = categoryBudgetDao.getBudgetForOneMonth(category, month, year)
    override suspend fun insertBudget(budget: CategoryBudget) = categoryBudgetDao.insertBudget(budget)

    override suspend fun copyLastMonthBudgets(targetMonth: Int, targetYear: Int) {
        val lastMonth = LocalDate.of(targetYear, targetMonth, 1).minusMonths(1)
        val lastBudgets = getBudgetForMonth(lastMonth.monthValue, lastMonth.year).first()

        lastBudgets.forEach { budget ->
            val newBudget = budget.copy(
                id = 0,
                month = targetMonth,
                year = targetYear
            )
            upsertBudget(newBudget)
        }
    }

    override suspend fun getMonthlyReportData(month: Int, year: Int): MonthlyReport {
        val budgets = categoryBudgetDao.getBudgetForMonth(month, year).first()
        val start = LocalDate.of(year, month, 1)
        val end = start.withDayOfMonth(start.lengthOfMonth())

        val startDate = start.toEpochDay()
        val endDate = end.toEpochDay()

        val spentMap = expenseDao.getSpentByCategoryBetween(startDate, endDate)
        val topExpenses = expenseDao.getTopExpensesBetween(startDate, endDate)
        val totalSpent = spentMap.values.sum()
        val totalBudget = budgets.sumOf { it.monthlyBudget }

        return MonthlyReport(
            month, year, totalSpent, totalBudget,
            spentMap, budgets.associate { it.category to it.monthlyBudget },
            topExpenses
        )
    }


    override suspend fun onTransactionAdded(expense: Expense) = withContext(Dispatchers.IO) {
        val category = expense.category
        if (category.isBlank()) return@withContext

        val tz = TimeZone.getTimeZone("Africa/Johannesburg")
        val now = Calendar.getInstance(tz)
        val month = now.get(Calendar.MONTH) + 1 // Calendar.MONTH is 0-11
        val year = now.get(Calendar.YEAR)

        // 👇 READ FROM ROOM NOW
        Log.d("BUDGET", "Checking category: '$category' for $month/$year")
        val categoryBudgetDouble = (categoryBudgetDao.getBudgetForOneMonth(category, month, year) ?: 0.0).toFloat()
        val allBudgets = categoryBudgetDao.getBudgetForMonth(month, year).first()
        Log.d("BUDGET", "All budgets in DB for $month/$year: $allBudgets")
        val categoryBudget = categoryBudgetDouble.toFloat()

        Log.d("BUDGET", "1. Checking $category with budget $categoryBudget")
        if (categoryBudget <= 0f) return@withContext

        if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return@withContext

        val startOfMonth = now.apply { set(Calendar.DAY_OF_MONTH, 1); set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0) }.timeInMillis
        val endOfMonth = now.apply { set(Calendar.DAY_OF_MONTH, now.getActualMaximum(Calendar.DAY_OF_MONTH)); set(Calendar.HOUR_OF_DAY, 23); set(Calendar.MINUTE, 59); set(Calendar.SECOND, 59); set(Calendar.MILLISECOND, 999) }.timeInMillis

        val prefs = context.getSharedPreferences("expense_prefs", Context.MODE_PRIVATE) // only for lastAlert
        val lastAlertKey = "last_budget_alert_${category}_${month}_${year}"
        val lastAlertedPercent = prefs.getInt(lastAlertKey, 0)

        val spentThisMonthInCat = expenseDao.getTotalByCategoryAndMonth(category, startOfMonth, endOfMonth)
        Log.d("BUDGET", "2. DAO returned: $spentThisMonthInCat")

        val percent = ((spentThisMonthInCat / categoryBudget) * 100).coerceAtMost(100f).toInt()
        Log.d("BUDGET", "3. Cat:$category Spent:$spentThisMonthInCat Budget:$categoryBudget Percent:$percent Last:$lastAlertedPercent")

        val alertThreshold = (percent/5)*5
        if (alertThreshold >= 80 && lastAlertedPercent < alertThreshold) {
            withContext(Dispatchers.Main) {
                notifier.showBudgetAlert(spentThisMonthInCat.toFloat(), categoryBudget, percent)
            }
            prefs.edit { putInt(lastAlertKey, alertThreshold) }
            Log.d("BUDGET", "4. ALERT FIRED for $category at $percent%")
        }
    }

    // Keep this for your UI
    suspend fun setBudget(category: String, amount: Double) {
        categoryBudgetDao.upsertBudget(CategoryBudget(0, category, amount, LocalDate.now().monthValue, LocalDate.now().year))
    }
}




