package com.example.expensetracker.ui.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.expensetracker.R
import com.example.expensetracker.data.BudgetSummary
import com.example.expensetracker.data.CategoryBudget
import com.example.expensetracker.data.CurrencyManager

@Composable
fun BudgetSection(
    categories: List<String>,
    budgets: List<CategoryBudget>,
    spentMap: Map<String, Double>,
    budgetSummary: BudgetSummary,
    currencyManager: CurrencyManager,
    onBudgetSet: (String, Double) -> Unit,
    onCopyLastMonth: () -> Unit,
    hasLastMonthBudgets: Boolean,
    hasCurrentMonthBudgets: Boolean,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            stringResource(R.string.budget_overview),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        BudgetSummaryCard(summary = budgetSummary,
            currencyManager = currencyManager,
            onCopyLastMonth = { onCopyLastMonth() },
            hasLastMonthBudgets = hasLastMonthBudgets,
            hasCurrentMonthBudgets = hasCurrentMonthBudgets
        )

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(vertical = 4.dp)
        ) {
            items(categories) { category ->
                BudgetCard(
                    category = category,
                    budget = budgets.find { it.category == category }?.monthlyBudget?: 0.0,
                    spent = spentMap[category]?: 0.0,
                    currencyManager = currencyManager,
                    onBudgetSet = { amount -> onBudgetSet(category, amount) },
                    modifier = Modifier.width(180.dp) // fixed width cards
                )
            }
        }
    }
}