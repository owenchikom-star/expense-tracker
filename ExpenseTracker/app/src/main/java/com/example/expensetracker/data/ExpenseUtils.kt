package com.example.expensetracker.data

import android.annotation.SuppressLint
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDate

@Entity(tableName = "expenses")
data class Expense  (
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val price: Double,
    val category: String ,
    val date: LocalDate,
    val note : String ,
    val timeStamp: Long = System.currentTimeMillis(),
    val receiptImagePath: String? = null,
    val receiptText: String? = null,
)

@Entity(tableName = "categories_budget",
    indices = [Index(value = ["category", "month", "year"], unique = true)])
data class CategoryBudget(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val category: String,
    val monthlyBudget: Double,
    val month: Int,
    val year: Int,
)

data class BudgetSummary(
        val totalBudget: Double,
        val totalSpent: Double,
        val remaining: Double,
        val overBudgetCategories: Int,
    )

enum class DateFilter {
    ALL,
    THIS_WEEK,
    THIS_MONTH,
    LAST_3_MONTHS,
}

data class BarData(
    val label: String,
    val value: Float,
    val subLabel: String = ""
)

data class DashboardUiState(
    val expenses: List<Expense> = emptyList(),
    val chartData: List<BarData> = emptyList(),
    val selectedRange: TimeRange = TimeRange.WEEK,
    val isLoading: Boolean = false,
)

enum class TimeRange {
    DAY,
    WEEK,
    MONTH
}

data class ParsedReceipt(
    val amount: Double,
    val date: LocalDate,
    val category: String,
    val storeName: String
)

data class MonthlyReport(
    val month: Int,
    val year: Int,
    val totalSpent: Double,
    val totalBudget: Double,
    val spentByCategory: Map<String, Double>,
    val budgetByCategory: Map<String, Double>,
    val topExpenses: List<Expense>,
)

enum class NavigationType {
    BOTTOM_NAVIGATION,
    NAVIGATION_RAIL,
    PERMANENT_NAVIGATION_DRAWER,
}

enum class ContentType {
    SINGLE_PANE,
    DUAL_PANE,
}
