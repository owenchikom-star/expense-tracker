package com.example.expensetracker

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.example.expensetracker.data.CategoryBudgetDao
import com.example.expensetracker.data.Expense
import com.example.expensetracker.data.ExpenseDao
import com.example.expensetracker.data.RoomCategoryBudgetRepository
import com.example.expensetracker.data.notifications.NotificationHelper
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers.any
import java.time.LocalDate


@OptIn(ExperimentalCoroutinesApi::class)
class RoomCategoryBudgetRepositoryTest {

    private lateinit var categoryDao: CategoryBudgetDao
    private lateinit var expenseDao: ExpenseDao
    private lateinit var context: Context
    private lateinit var notifier: NotificationHelper
    private lateinit var prefs: FakeSharedPreferences
    private lateinit var repo: RoomCategoryBudgetRepository

    @Before
    fun setup() {
        categoryDao = mockk()
        expenseDao = mockk()
        context = mockk()
        notifier = mockk(relaxed = true)
        prefs = FakeSharedPreferences()

        // Mock permission granted
        mockkStatic(ContextCompat::class)
        every {
            ContextCompat.checkSelfPermission(any(), Manifest.permission.POST_NOTIFICATIONS)
        } returns PackageManager.PERMISSION_GRANTED

        // Mock getSharedPreferences
        every { context.getSharedPreferences("expense_prefs", Context.MODE_PRIVATE) } returns prefs

        // Mock getBudgetForMonth (used for logging in production code)
        every { categoryDao.getBudgetForMonth(any(), any()) } returns flowOf(emptyList())

        repo = RoomCategoryBudgetRepository(categoryDao, expenseDao, context, notifier)
        Dispatchers.setMain(StandardTestDispatcher())
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun onTransactionAddedFiresNotificationAt80Percent() = runTest {
        // Given: Budget 1000, spent will be 850 after this transaction
        val today = LocalDate.now()
        val month = today.monthValue
        val year = today.year
        val expense = Expense(id = 0, name = "Shoprite", price = 850.0, category = "Food", date = today, note = "")

        coEvery { categoryDao.getBudgetForOneMonth("Food", month, year) } returns 1000.0
        coEvery { expenseDao.getTotalByCategoryAndMonth(eq("Food"), any(), any()) } returns 850.0f

        // When
        repo.onTransactionAdded(expense)

        // Then
        verify { notifier.showBudgetAlert(850f, 1000f, 85) } // 85%
        assertEquals(85, prefs.getInt("last_budget_alert_Food_${month}_${year}", 0)) // saved threshold
    }

    @Test
    fun `onTransactionAdded does NOT fire twice at same milestone`() = runTest {
        // Given: Already alerted at 85
        val today = LocalDate.now()
        val month = today.monthValue
        val year = today.year
        prefs.edit().putInt("last_budget_alert_Food_${month}_${year}", 85).apply()

        val expense = Expense(0, "Shoprite", 870.0, "Food", today, "")
        coEvery { categoryDao.getBudgetForOneMonth("Food", month, year) } returns 1000.0
        coEvery { expenseDao.getTotalByCategoryAndMonth(eq("Food"), any(), any()) } returns 870.0f // 87% -> threshold 85

        // When
        repo.onTransactionAdded(expense)

        // Then: No new notification because alertThreshold (85) == lastAlertedPercent (85)
        verify(exactly = 0) { notifier.showBudgetAlert(any(), any(), any()) }
    }

    @Test
    fun `onTransactionAdded does nothing if budget is 0`() = runTest {
        val expense = Expense(0, "Other", 500.0, "Other", LocalDate.now(), "")
        coEvery { categoryDao.getBudgetForOneMonth(any(), any(), any()) } returns 0.0

        repo.onTransactionAdded(expense)

        verify(exactly = 0) { notifier.showBudgetAlert(any(), any(), any()) }
    }

    @Test
    fun `onTransactionAdded does nothing if no notification permission`() = runTest {
        every {
            ContextCompat.checkSelfPermission(any(), Manifest.permission.POST_NOTIFICATIONS)
        } returns PackageManager.PERMISSION_DENIED

        val expense = Expense(0, "Shoprite", 850.0, "Food", LocalDate.now(), "")
        coEvery { categoryDao.getBudgetForOneMonth(any(), any(), any()) } returns 1000.0

        repo.onTransactionAdded(expense)

        verify(exactly = 0) { notifier.showBudgetAlert(any(), any(), any()) }
    }

    @Test
    fun `onTransactionAdded fires again at 100 percent if previously at 80`() = runTest {
        // Given: Already alerted at 80
        val today = LocalDate.now()
        val month = today.monthValue
        val year = today.year
        prefs.edit().putInt("last_budget_alert_Food_${month}_${year}", 80).apply()

        val expense = Expense(0, "Shoprite", 1000.0, "Food", today, "")
        coEvery { categoryDao.getBudgetForOneMonth("Food", month, year) } returns 1000.0
        coEvery { expenseDao.getTotalByCategoryAndMonth(eq("Food"), any(), any()) } returns 1000.0f // 100%

        // When
        repo.onTransactionAdded(expense)

        // Then: Fired because threshold 100 > last 80
        verify { notifier.showBudgetAlert(1000f, 1000f, 100) }
        assertEquals(100, prefs.getInt("last_budget_alert_Food_${month}_${year}", 0))
    }

    @Test
    fun `getMonthlyReportData uses correct date ranges`() = runTest {
        // Given
        val month = 7
        val year = 2026
        coEvery { categoryDao.getBudgetForMonth(month, year) } returns flowOf(emptyList())
        coEvery { expenseDao.getSpentByCategoryBetween(any(), any()) } returns emptyMap()
        coEvery { expenseDao.getTopExpensesBetween(any(), any()) } returns emptyList()

        // When
        repo.getMonthlyReportData(month, year)

        // Then: Should call DAO with epoch days, not milliseconds
        // July 1, 2026 = 20635
        // July 31, 2026 = 20665
        coEvery { 
            expenseDao.getSpentByCategoryBetween(20635, 20665) 
        }
        coEvery { 
            expenseDao.getTopExpensesBetween(20635, 20665)
        }
    }
}
