package com.example.expensetracker

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import androidx.work.testing.TestListenableWorkerBuilder
import com.example.expensetracker.data.ExpenseDao
import com.example.expensetracker.data.notifications.NotificationHelper
import com.example.expensetracker.data.notifications.WeeklySummaryWorker
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class WeeklyReminderWorkerTest {
    private lateinit var expenseDao: ExpenseDao
    private lateinit var notifier: NotificationHelper
    private lateinit var context: Context

    @Before
    fun setup() {
        expenseDao = mockk()
        notifier = mockk(relaxed = true)
        context = ApplicationProvider.getApplicationContext()
        Dispatchers.setMain(StandardTestDispatcher())
    }

    @Test
    fun `doWork shows summary if expenses were logged this week`() = runTest {
        // Given: R500 spent this week
        coEvery { expenseDao.getTotalForWeek(any(), any()) } returns 500f
        
        val worker = TestListenableWorkerBuilder<WeeklySummaryWorker>(context)
            .setWorkerFactory(TestWeeklyWorkerFactory(expenseDao, notifier))
            .build()

        // When
        val result = worker.doWork()

        // Then
        assertEquals(ListenableWorker.Result.success(), result)
        verify { notifier.showWeeklySummary(500f) }
    }

    @Test
    fun `doWork does NOT show summary if no expenses this week`() = runTest {
        // Given: 0 spent this week
        coEvery { expenseDao.getTotalForWeek(any(), any()) } returns 0f
        
        val worker = TestListenableWorkerBuilder<WeeklySummaryWorker>(context)
            .setWorkerFactory(TestWeeklyWorkerFactory(expenseDao, notifier))
            .build()

        // When
        val result = worker.doWork()

        // Then
        assertEquals(ListenableWorker.Result.success(), result)
        verify(exactly = 0) { notifier.showWeeklySummary(any()) }
    }
}

class TestWeeklyWorkerFactory(
    private val expenseDao: ExpenseDao,
    private val notifier: NotificationHelper
) : WorkerFactory() {
    override fun createWorker(
        appContext: Context,
        workerClassName: String,
        workerParameters: WorkerParameters
    ): ListenableWorker? {
        return when (workerClassName) {
            WeeklySummaryWorker::class.java.name ->
                WeeklySummaryWorker(
                    appContext, 
                    workerParameters, 
                    expenseDao, 
                    notifier, 
                    reschedule = {} // No-op to avoid WorkManager init in tests
                )
            else -> null
        }
    }
}
