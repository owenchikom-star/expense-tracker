package com.example.expensetracker

import android.content.Context
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.example.expensetracker.data.BudgetSummary
import com.example.expensetracker.data.CurrencyManager
import com.example.expensetracker.ui.ui.BudgetCard
import com.example.expensetracker.ui.ui.BudgetSection
import org.junit.Rule
import org.junit.Test

class BudgetScreenUiTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val currencyManager = CurrencyManager(context, context.getSharedPreferences("test_prefs", Context.MODE_PRIVATE))

    @Test
    fun clickingCopyLastMonthBudgetsTriggersCallback() {
        var copyCalled = false
        val summary = BudgetSummary(1000.0, 500.0, 500.0, 0)
        
        composeTestRule.setContent {
            BudgetSection(
                categories = listOf("Food", "Transport"),
                budgets = emptyList(),
                spentMap = emptyMap(),
                budgetSummary = summary,
                currencyManager = currencyManager,
                onBudgetSet = { _, _ -> },
                onCopyLastMonth = { copyCalled = true },
                hasLastMonthBudgets = true,
                hasCurrentMonthBudgets = false
            )
        }

        // When
        composeTestRule.onNodeWithText("Copy last month budgets").performClick()
        
        // The button shows a confirmation dialog
        composeTestRule.onNodeWithText("Copy").performClick()

        // Then
        assert(copyCalled)
    }

    @Test
    fun budgetCardShowsProgressBarAndOverBudgetMessage() {
        composeTestRule.setContent {
            BudgetCard(
                category = "Food",
                budget = 100.0,
                spent = 150.0,
                currencyManager = currencyManager,
                onBudgetSet = {}
            )
        }

        // Then
        composeTestRule.onNodeWithText("Food").assertIsDisplayed()
        composeTestRule.onNodeWithText("150", substring = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("100", substring = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("Over budget", substring = true).assertIsDisplayed()
        composeTestRule.onNodeWithTag("BudgetProgressBar").assertIsDisplayed()
    }
}
