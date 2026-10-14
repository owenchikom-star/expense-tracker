package com.example.expensetracker.data
import android.content.Context

class AppContainer(private val context: Context) {
    val currencyManager: CurrencyManager by lazy {
        CurrencyManager(context, context.getSharedPreferences("expense_prefs", Context.MODE_PRIVATE))
    }

    val expenseRepository: ExpenseRepository by lazy {
        RoomExpenseRepository(
            ExpenseDatabase.getDatabase(context).expenseDao(),
            ExpenseDatabase.getDatabase(context).categoryBudgetDao(),
            context
        )
    }

    val categoryBudgetRepository: CategoryBudgetRepository by lazy {
        RoomCategoryBudgetRepository(
            ExpenseDatabase.getDatabase(context).categoryBudgetDao(),
            ExpenseDatabase.getDatabase(context).expenseDao(),
            context
        )
    }
}
