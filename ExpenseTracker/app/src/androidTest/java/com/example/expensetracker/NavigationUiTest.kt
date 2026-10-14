package com.example.expensetracker

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.example.expensetracker.theme.ExpenseTrackerTheme
import com.example.expensetracker.ui.ui.ExpenseApp
import org.junit.Rule
import org.junit.Test

class NavigationUiTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun appStartsOnDashboardTab() {
        composeTestRule.setContent {
            ExpenseTrackerTheme {
                ExpenseApp()
            }
        }

        // Dashboard title should be visible
        composeTestRule.onNodeWithText("Spending").assertIsDisplayed()
    }

    @Test
    fun canSwitchToTransactionsTab() {
        composeTestRule.setContent {
            ExpenseTrackerTheme {
                ExpenseApp()
            }
        }

        // Click on Transactions tab
        composeTestRule.onNodeWithText("Transactions").performClick()

        // Search bar or some text from the transactions screen should be visible
        composeTestRule.onNodeWithText("Search expense…").assertIsDisplayed()
    }

    @Test
    fun clickingFABOpensAddExpenseSheet() {
        composeTestRule.setContent {
            ExpenseTrackerTheme {
                ExpenseApp()
            }
        }

        // Switch to Transactions tab where FAB is located
        composeTestRule.onNodeWithText("Transactions").performClick()

        // Find FAB by content description
        composeTestRule.onNodeWithContentDescription("add expense", ignoreCase = true).performClick()

        // "Add Expense" title in the sheet should be visible
        composeTestRule.onNodeWithText("Add Expense").assertIsDisplayed()
    }
}
