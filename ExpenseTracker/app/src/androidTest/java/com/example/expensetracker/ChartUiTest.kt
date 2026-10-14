package com.example.expensetracker

import android.content.Context
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Rule
import org.junit.Test
import com.example.expensetracker.data.BarData
import com.example.expensetracker.data.CurrencyManager
import com.example.expensetracker.ui.ui.BarChart
import com.example.expensetracker.ui.ui.SpendingPieChart

class ChartsUiTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val currencyManager = CurrencyManager(context, context.getSharedPreferences("test_prefs", Context.MODE_PRIVATE))

    private val fakePieData = listOf(
        "Food" to 1200.0,
        "Transport" to 500.0,
        "Shopping" to 300.0
    )

    private val fakeBarData = listOf(
        BarData("Food", 1200f),
        BarData("Transport", 500f),
        BarData("Shopping", 300f)
    )

    @Test
    fun barChartDisplaysAllCategoriesWithCorrectLabels() {
        composeTestRule.setContent {
            BarChart(data = fakeBarData, currencyManager = currencyManager)
        }

        // BarChart takes first 3 chars of label
        composeTestRule.onNodeWithText("Foo").assertIsDisplayed()
        composeTestRule.onNodeWithText("Tra").assertIsDisplayed()
        composeTestRule.onNodeWithText("Sho").assertIsDisplayed()
    }

    @Test
    fun pieChartDisplaysSegmentsAndShowsTotalOnCenter() {
        composeTestRule.setContent {
            SpendingPieChart(data = fakePieData, currencyManager = currencyManager)
        }

        // SpendingPieChart shows total. We'll search for the numeric part to be safe.
        composeTestRule.onNodeWithText("Total").assertIsDisplayed()
        composeTestRule.onNodeWithText("2000", substring = true).assertIsDisplayed()
        
        // Check labels in the list below the chart
        composeTestRule.onNodeWithText("Food").assertIsDisplayed()
        composeTestRule.onNodeWithText("1200", substring = true).assertIsDisplayed()
    }

    @Test
    fun barChartShowsEmptyStateWhenNoData() {
        composeTestRule.setContent {
            BarChart(data = emptyList(), currencyManager = currencyManager)
        }
        composeTestRule.onNodeWithText("No spending in this range").assertIsDisplayed()
    }

    @Test
    fun pieChartShowsEmptyStateWhenNoData() {
        composeTestRule.setContent {
            SpendingPieChart(data = emptyList(), currencyManager = currencyManager)
        }
        composeTestRule.onNodeWithText("No data to display!!").assertIsDisplayed()
    }
}
