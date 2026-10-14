package com.example.expensetracker

import app.cash.turbine.test
import com.example.expensetracker.data.CategoryBudget
import com.example.expensetracker.data.Expense
import com.example.expensetracker.data.ParsedReceipt
import com.example.expensetracker.ui.ui.ExpenseViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class ExpenseViewModelTest {

    private lateinit var fakeExpenseRepo: FakeExpenseRepository
    private lateinit var fakeBudgetRepo: FakeCategoryBudgetRepository
    private lateinit var viewModel: ExpenseViewModel
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        fakeExpenseRepo = FakeExpenseRepository()
        fakeBudgetRepo = FakeCategoryBudgetRepository()
        val mockContext = io.mockk.mockk<android.content.Context>(relaxed = true)
        val mockPrefs = io.mockk.mockk<android.content.SharedPreferences>(relaxed = true)
        val currencyManager = com.example.expensetracker.data.CurrencyManager(mockContext, mockPrefs)
        viewModel = ExpenseViewModel(fakeExpenseRepo, fakeBudgetRepo, currencyManager, mockContext)
        runTest { testDispatcher.scheduler.advanceUntilIdle() }
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `fillFromReceipt sets image path and parsed data in state`() = runTest {
        val parsed = ParsedReceipt(
            storeName = "Pick n Pay",
            amount = 129.99,
            category = "Food",
            date = LocalDate.now()
        )
        val imagePath = "/data/receipts/1.jpg"

        viewModel.fillFromReceipt(parsed, imagePath)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.expensesUiState.value
        assertEquals("Pick n Pay", state.name)
        assertEquals("129.99", state.price)
        assertEquals("Food", state.category)
        assertEquals(imagePath, state.receiptImagePath)
    }

    @Test
    fun `saveExpense keeps old image path when editing and no new image provided`() = runTest {
        // 1. Insert expense with image
        val original = Expense(id = 1, name = "Old", price = 50.0, category = "Other",
            date = LocalDate.now(), note = "", receiptImagePath = "/old/path.jpg")
        fakeExpenseRepo.upsert(original)

        // 2. Start editing
        viewModel.onExpenseClick(original)
        viewModel.updateExpenseName("New Name") // change only name
        // don't call updateImagePath

        // 3. Save
        viewModel.saveExpense()
        testDispatcher.scheduler.advanceUntilIdle()

        val saved = fakeExpenseRepo.getAllExpenses().first().first()
        assertEquals("New Name", saved.name)
        assertEquals("/old/path.jpg", saved.receiptImagePath) // <-- KEY: didn't get wiped
    }

    @Test
    fun `saveExpense replaces image path when new image provided`() = runTest {
        val original = Expense(
            id = 1, name = "Old", price = 50.0, category = "Other",
            date = LocalDate.now(), note = "", receiptImagePath = "/old/path.jpg"
        )
        fakeExpenseRepo.upsert(original)

        viewModel.onExpenseClick(original)
        viewModel.updateImagePath("/new/path.jpg") // user picked new scan

        viewModel.saveExpense()
        testDispatcher.scheduler.advanceUntilIdle()

        val saved = fakeExpenseRepo.getAllExpenses().first().first()
        assertEquals("/new/path.jpg", saved.receiptImagePath) // <-- replaced
    }

    @Test
    fun `saveExpense creates new expense with image path`() = runTest {
        viewModel.updateExpenseName("Shoprite")
        viewModel.updateExpensePrice("200")
        viewModel.updateImagePath("/scan/new.jpg")

        viewModel.saveExpense()
        testDispatcher.scheduler.advanceUntilIdle()

        val all = fakeExpenseRepo.getAllExpenses().first()
        assertEquals(1, all.size)
        assertEquals("/scan/new.jpg", all[0].receiptImagePath)
        assertEquals("", viewModel.expensesUiState.value.price) // form reset
    }

    @Test
    fun `cancelEdit resets ui state including image path`() = runTest {
        viewModel.updateImagePath("/temp.jpg")
        viewModel.cancelEdit()
        testDispatcher.scheduler.advanceUntilIdle()

        assertNull(viewModel.expensesUiState.value.receiptImagePath)
    }

    @Test
    fun `full scan flow - fillFromReceipt then saveExpense persists with image`() = runTest {
        // Given: user scans a receipt
        val parsed = ParsedReceipt(
            storeName = "Checkers",
            amount = 345.67,
            category = "Food",
            date = LocalDate.of(2026, 7, 17)
        )
        val imagePath = "/storage/receipts/scan_001.jpg"

        // When 1: Fill form from scanner
        viewModel.fillFromReceipt(parsed, imagePath)
        testDispatcher.scheduler.advanceUntilIdle()

        // Assert 1: UI State is filled
        viewModel.expensesUiState.test {
            val state = awaitItem()
            assertEquals("Checkers", state.name)
            assertEquals("345.67", state.price)
            assertEquals("Food", state.category)
            assertEquals(imagePath, state.receiptImagePath)
            cancelAndConsumeRemainingEvents()
        }

        // When 2: User hits Save
        viewModel.saveExpense()
        testDispatcher.scheduler.advanceUntilIdle()

        // Assert 2: Repo has the new expense
        val savedExpenses = fakeExpenseRepo.getAllExpenses().first()
        assertEquals(1, savedExpenses.size)

        val saved = savedExpenses[0]
        assertEquals("Checkers", saved.name)
        assertEquals(345.67, saved.price, 0.001)
        assertEquals(imagePath, saved.receiptImagePath) // <-- KEY CHECK

        // Assert 3: Form was reset after save
        val resetState = viewModel.expensesUiState.value
        assertEquals("", resetState.name)
        assertNull(resetState.receiptImagePath)
    }

    @Test
    fun `edit flow - change name only, image path stays`() = runTest {
        // Given: existing expense with image
        val existing = Expense(
            id = 0, name = "Old Shop", price = 100.0, category = "Shopping",
            date = LocalDate.now(), note = "", receiptImagePath = "/old/scan.jpg"
        )
        fakeExpenseRepo.upsert(existing)
        testDispatcher.scheduler.advanceUntilIdle()
        val inserted = fakeExpenseRepo.getAllExpenses().first().first()

        // When: open edit sheet and change only name
        viewModel.onExpenseClick(inserted)
        viewModel.updateExpenseName("New Shop Name")
        viewModel.saveExpense()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then: name changed, image kept
        val updated = fakeExpenseRepo.getAllExpenses().first().first()
        assertEquals("New Shop Name", updated.name)
        assertEquals("/old/scan.jpg", updated.receiptImagePath)
    }

    @Test
    fun `searchExpenseList filters by name and category`() = runTest {
        // Given
        fakeExpenseRepo.upsert(Expense(id = 1, name = "Uber Ride", price = 50.0, category = "Transport", date = LocalDate.now(), note = ""))
        fakeExpenseRepo.upsert(Expense(id = 2, name = "Shoprite Food", price = 200.0, category = "Food", date = LocalDate.now(), note = ""))
        testDispatcher.scheduler.advanceUntilIdle()

        // When
        viewModel.updateSearchQuery("shop")
        viewModel.updateCategoryFilter("Food")
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        viewModel.searchExpenseList.test {
            var results = awaitItem()
            if (results.isEmpty()) {
                results = awaitItem()
            }
            assertEquals(1, results.size)
            assertEquals("Shoprite Food", results[0].name)
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `undoDelete restores expense including receiptImagePath`() = runTest {
        // Given: expense with image in DB
        val expense = Expense(
            id = 0,
            name = "Takealot",
            price = 899.0,
            category = "Shopping",
            date = LocalDate.now(),
            note = "Laptop",
            receiptImagePath = "/receipts/takealot_01.jpg"
        )
        fakeExpenseRepo.upsert(expense)
        testDispatcher.scheduler.advanceUntilIdle()

        val inserted = fakeExpenseRepo.getAllExpenses().first().first()
        assertEquals(1, fakeExpenseRepo.getAllExpenses().first().size)

        // When 1: Delete it
        viewModel.deleteExpense(inserted)
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(0, fakeExpenseRepo.getAllExpenses().first().size)

        // When 2: Undo delete
        viewModel.undoDelete()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then: Expense is back with same data including image
        val restoredList = fakeExpenseRepo.getAllExpenses().first()
        assertEquals(1, restoredList.size)

        val restored = restoredList[0]
        assertEquals("Takealot", restored.name)
        assertEquals(899.0, restored.price, 0.001)
        assertEquals("/receipts/takealot_01.jpg", restored.receiptImagePath) // <-- KEY CHECK
        assertEquals(1L, restored.id) // id should be 1 because it's the first item in the fake repo
    }

    @Test
    fun `undoDelete does nothing if nothing was deleted`() = runTest {
        // Given: empty DB, no delete happened

        // When
        viewModel.undoDelete()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then: DB still empty, no crash
        assertEquals(0, fakeExpenseRepo.getAllExpenses().first().size)
    }


    @Test
    fun `budgetSummary calculates totalBudget totalSpent and remaining correctly`() = runTest {
        // Given: 2 budgets for this month
        val today = LocalDate.now()
        fakeBudgetRepo.upsertBudget(CategoryBudget(0, "Food", 1000.0, today.monthValue, today.year))
        fakeBudgetRepo.upsertBudget(CategoryBudget(0, "Transport", 500.0, today.monthValue, today.year))

        // And 2 expenses this month
        fakeExpenseRepo.upsert(Expense(id = 1, name = "Shoprite", price = 300.00, category = "Food", date = today, note = "" ))
        fakeExpenseRepo.upsert(Expense(id = 2, name = "Uber", price = 150.0, category = "Transport", date = today, note = ""))
        testDispatcher.scheduler.advanceUntilIdle()

        // When/Then
        viewModel.budgetSummary.test {
            var summary = awaitItem()
            if (summary.totalBudget == 0.0) {
                summary = awaitItem()
            }
            assertEquals(1500.0, summary.totalBudget, 0.001) // 1000 + 500
            assertEquals(450.0, summary.totalSpent, 0.001) // 300 + 150
            assertEquals(1050.0, summary.remaining, 0.001) // 1500 - 450
            assertEquals(0, summary.overBudgetCategories)
        }
    }

    @Test
    fun `budgetSummary counts overBudget categories`() = runTest {
        val today = LocalDate.now()
        fakeBudgetRepo.upsertBudget(CategoryBudget(0, "Food", 100.0, today.monthValue, today.year))
        fakeExpenseRepo.upsert(Expense(id = 3, name = "Steak", price = 250.0, category = "Food", date = today, note = ""))
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.budgetSummary.test {
            var summary = awaitItem()
            if (summary.totalSpent == 0.0) {
                summary = awaitItem()
            }
            assertEquals(250.0, summary.totalSpent, 0.001)
            assertEquals(1, summary.overBudgetCategories) // 250 > 100
        }
    }

    @Test
    fun `saveExpense calls onTransactionAdded on repository`() = runTest {
        // Given
        viewModel.updateExpenseName("Petrol")
        viewModel.updateExpensePrice("400")
        viewModel.updateCategory("Transport")

        // When
        viewModel.saveExpense()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        assertEquals(1, fakeBudgetRepo.onTransactionAddedCalls.size)
        val calledWith = fakeBudgetRepo.onTransactionAddedCalls[0]
        assertEquals("Petrol", calledWith.name)
        assertEquals(400.0, calledWith.price, 0.001)
        assertEquals("Transport", calledWith.category)
    }

    @Test
    fun `spentPerCategory only includes current month`() = runTest {
        val today = LocalDate.now()
        val lastMonth = today.minusMonths(1)

        fakeExpenseRepo.upsert(Expense(id = 4, name = "This Month", price = 100.0, category = "Food", date = today, note = ""))
        fakeExpenseRepo.upsert(Expense(id = 5, name = "Last Month", price = 500.0, category = "Food", date = lastMonth, note = ""))
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.spentPerCategory.test {
            var spent = awaitItem()
            if (spent.isEmpty()) {
                spent = awaitItem()
            }
            assertEquals(100.0, spent["Food"]) // should ignore last month
            assertNull(spent["Transport"])
        }
    }

    @Test
    fun `copyLastMonthBudgets copies all budgets from previous month`() = runTest {
        // Given: Budgets for June 2027
        val june = LocalDate.of(2027, 6, 15)
        fakeBudgetRepo.upsertBudget(CategoryBudget(0, "Food", 2000.0, 6, 2027))
        fakeBudgetRepo.upsertBudget(CategoryBudget(0, "Transport", 800.0, 6, 2027))
        fakeBudgetRepo.upsertBudget(CategoryBudget(0, "Shopping", 500.0, 6, 2027))
        testDispatcher.scheduler.advanceUntilIdle()

        // When: Copy to July 2027
        viewModel.copyLastMonthBudgets(targetMonth = 7, targetYear = 2027)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then: July has same 3 categories with same amounts
        val julyBudgets = fakeBudgetRepo.getBudgetForMonth(7, 2027).first()
        assertEquals(3, julyBudgets.size)

        assertEquals(2000.0, julyBudgets.find { it.category == "Food" }?.monthlyBudget ?: 0.0, 0.001)
        assertEquals(800.0, julyBudgets.find { it.category == "Transport" }?.monthlyBudget ?: 0.0, 0.001)
        assertEquals(500.0, julyBudgets.find { it.category == "Shopping" }?.monthlyBudget ?: 0.0, 0.001)

        // And June budgets are still there
        val juneBudgets = fakeBudgetRepo.getBudgetForMonth(6, 2027).first()
        assertEquals(3, juneBudgets.size)
    }

    @Test
    fun `copyLastMonthBudgets overwrites existing budget for same category and month`() = runTest {
        // Given: July 2027 already has Food = 1000
        fakeBudgetRepo.upsertBudget(CategoryBudget(0, "Food", 1000.0, 7, 2027))
        // And June 2027 has Food = 2000
        fakeBudgetRepo.upsertBudget(CategoryBudget(0, "Food", 2000.0, 6, 2027))
        testDispatcher.scheduler.advanceUntilIdle()

        // When: Copy June to July 2027
        viewModel.copyLastMonthBudgets(targetMonth = 7, targetYear = 2027)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then: July Food is now 2000, not 1000. Upsert worked
        val julyFood = fakeBudgetRepo.getBudgetForMonth(7, 2027).first().first()
        assertEquals(2000.0, julyFood.monthlyBudget, 0.001)
    }

    @Test
    fun `copyLastMonthBudgets does nothing if last month has no budgets`() = runTest {
        // Given: No budgets in June 2027
        testDispatcher.scheduler.advanceUntilIdle()

        // When: Copy June to July 2027
        viewModel.copyLastMonthBudgets(targetMonth = 7, targetYear = 2027)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then: July is still empty
        val julyBudgets = fakeBudgetRepo.getBudgetForMonth(7, 2027).first()
        assertEquals(0, julyBudgets.size)
    }

    @Test
    fun `deleteExpense does not call onTransactionAdded`() = runTest {
        // Given
        val expense = Expense(id = 0, name =  "Cancel", price = 99.0, category = "Other", date = LocalDate.now(), note = "", receiptImagePath = "/img.jpg")
        fakeExpenseRepo.upsert(expense)
        testDispatcher.scheduler.advanceUntilIdle()
        val inserted = fakeExpenseRepo.getAllExpenses().first().first()

        val callsBefore = fakeBudgetRepo.onTransactionAddedCalls.size

        // When
        viewModel.deleteExpense(inserted)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        assertEquals(0, fakeExpenseRepo.getAllExpenses().first().size)
        assertEquals(callsBefore, fakeBudgetRepo.onTransactionAddedCalls.size) // no new calls
    }

    @Test
    fun `deleteExpense sets pending delete and undo restores`() = runTest {
        // Given
        val expense = Expense(id = 0, name = "Test", price = 50.0, category = "Food", date = LocalDate.now(), note = "", receiptImagePath = "/img.jpg")
        fakeExpenseRepo.upsert(expense)
        testDispatcher.scheduler.advanceUntilIdle()
        val inserted = fakeExpenseRepo.getAllExpenses().first().first()

        // When
        viewModel.deleteExpense(inserted)
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(0, fakeExpenseRepo.getAllExpenses().first().size)

        viewModel.undoDelete()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        val restored = fakeExpenseRepo.getAllExpenses().first().first()
        assertEquals("Test", restored.name)
        assertEquals("/img.jpg", restored.receiptImagePath)
    }

    @Test
    fun `chartData is sorted by spent descending and excludes 0`() = runTest {
        val today = LocalDate.now()
        // Given: 3 categories
        fakeExpenseRepo.upsert(Expense(id = 1, name = "Uber", price = 500.0, category = "Transport", date = today, note = ""))
        fakeExpenseRepo.upsert(Expense(id = 2, name = "Shoprite", price = 1200.0, category = "Food", date = today, note = ""))
        fakeExpenseRepo.upsert(Expense(id = 3, name = "Netflix", price = 99.0, category = "Subscriptions", date = today, note = ""))
        testDispatcher.scheduler.advanceUntilIdle()

        // When/Then: Sorted Food > Transport > Subscriptions. No 0s
        viewModel.chartData.test {
            var chartData = awaitItem()
            if (chartData.isEmpty()) {
                chartData = awaitItem()
            }
            assertEquals(3, chartData.size)
            assertEquals("Food", chartData[0].first)
            assertEquals(1200.0, chartData[0].second, 0.001)
            assertEquals("Transport", chartData[1].first)
            assertEquals("Subscriptions", chartData[2].first)
        }
    }

    @Test
    fun `chartData only includes current month`() = runTest {
        val today = LocalDate.now()
        val lastMonth = today.minusMonths(1)
        fakeExpenseRepo.upsert(Expense(id = 4, name = "This", price = 100.0, category = "Food", date = today, note = ""))
        fakeExpenseRepo.upsert(Expense(id = 5, name = "Last", price = 9999.0, category = "Food", date = lastMonth, note = ""))
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.chartData.test {
            var chartData = awaitItem()
            if (chartData.isEmpty()) {
                chartData = awaitItem()
            }
            assertEquals(1, chartData.size)
            assertEquals(100.0, chartData[0].second, 0.001) // not 10099
        }
    }

}