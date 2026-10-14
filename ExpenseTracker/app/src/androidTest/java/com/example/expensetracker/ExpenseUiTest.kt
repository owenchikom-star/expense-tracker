package com.example.expensetracker

import android.content.Context
import com.example.expensetracker.data.CurrencyManager
import com.example.expensetracker.data.Expense
import com.example.expensetracker.ui.ui.ExpenseItem
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate

class ExpenseItemUiTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val currencyManager = CurrencyManager(context, context.getSharedPreferences("test_prefs", Context.MODE_PRIVATE))

    @Test
    fun showsReceiptThumbnailWhenReceiptImagePathIsNotNull() {
        // Given
        val expenseWithImage = Expense(
            id = 1,
            name = "Pick n Pay",
            price = 129.99,
            category = "Food",
            date = LocalDate.now(),
            note = "",
            receiptImagePath = "/data/data/com.example/files/receipts/test.jpg"
        )

        // When
        composeTestRule.setContent {
            ExpenseItem(
                expense = expenseWithImage,
                currencyManager = currencyManager,
                onClick = {},
                onEdit = {}
            )
        }

        // Then
        // 1. Name and price should show
        composeTestRule.onNodeWithText("Pick n Pay", substring = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("129", substring = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("99", substring = true).assertIsDisplayed()

        // 2. Receipt icon/thumbnail should show. We tagged it in the composable
        composeTestRule.onNodeWithTag("ReceiptThumbnail", useUnmergedTree = true).assertExists()
    }

    @Test
    fun doesNotShowReceiptThumbnailWhenReceiptImagePathIsNull() {
        // Given
        val expenseNoImage = Expense(
            id = 2,
            name = "Uber",
            price = 45.0,
            category = "Transport",
            date = LocalDate.now(),
            note = "",
            receiptImagePath = null
        )

        // When
        composeTestRule.setContent {
            ExpenseItem(
                expense = expenseNoImage,
                currencyManager = currencyManager,
                onClick = {},
                onEdit = {}
            )
        }

        // Then
        composeTestRule.onNodeWithText("Uber", substring = true).assertIsDisplayed()
        composeTestRule.onNodeWithTag("ReceiptThumbnail").assertDoesNotExist()
    }

    @Test
    fun clickOnItemTriggersOnClick() {
        var clicked = false
        val expense = Expense(
            id = 1,
            name = "Test",
            price = 10.0,
            category = "Other",
            date = LocalDate.now(),
            note = ""
        )

        composeTestRule.setContent {
            ExpenseItem(
                expense = expense,
                currencyManager = currencyManager,
                onClick = { clicked = true },
                onEdit = {}
            )
        }

        composeTestRule.onNodeWithText("Test", substring = true).performClick()
        assert(clicked)
    }
}