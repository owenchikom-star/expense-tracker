package com.example.expensetracker.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface ExpenseRepository {
    suspend fun insert(expense: Expense)
    suspend fun upsert(expense: Expense)
    fun getAllExpenses(): Flow<List<Expense>>
    suspend fun delete(expense: Expense)
    suspend fun update(expense: Expense)
    fun getExpensesByDateRange(startDate: Long, endDate: Long): Flow<List<Expense>>
    fun getExpensesByCategory(category: String): Flow<List<Expense>>
    fun getTotalBetweenDates(startDate: Long, endDate: Long): Flow<Double?>
    fun getFilteredExpenses(category: String, startMs: Long?, endMs: Long?): Flow<List<Expense>>
    fun getTotalForDate(startOfDay: Long, endOfDay: Long): Float
    suspend fun getTotalForWeek(startOfWeek: Long, endOfWeek: Long): Float
    suspend fun getTotalByCategoryAndMonth(category: String, start: Long, end: Long): Float
}

interface CategoryBudgetRepository {
    fun getBudgetForMonth(month: Int, year: Int): Flow<List<CategoryBudget>>
    suspend fun upsertBudget(budget: CategoryBudget)
    suspend fun insertBudget(budget: CategoryBudget)
    suspend fun deleteBudget(category: String, month: Int, year: Int)
    suspend fun getBudgetForOneMonth(category: String, month: Int, year: Int): Double?
    suspend fun onTransactionAdded(expense: Expense)
    suspend fun copyLastMonthBudgets(targetMonth: Int, targetYear: Int)
    suspend fun getMonthlyReportData(month: Int, year: Int): MonthlyReport
}
