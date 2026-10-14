package com.example.expensetracker

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import androidx.work.testing.TestListenableWorkerBuilder
import com.example.expensetracker.data.ExpenseDao
import com.example.expensetracker.data.notifications.DailyReminderWorker
import com.example.expensetracker.data.notifications.NotificationHelper
import io.mockk.every
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
class DailyReminderWorkerTest {
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
    fun `doWork shows reminder if no expenses today`() = runTest {
        // Given
        every { expenseDao.getTotalForDate(any(), any()) } returns 0f
        
        val worker = TestListenableWorkerBuilder<DailyReminderWorker>(context)
            .setWorkerFactory(TestWorkerFactory(expenseDao, notifier))
            .build()

        // When
        val result = worker.doWork()

        // Then
        assertEquals(ListenableWorker.Result.success(), result)
        verify { notifier.showDailyReminder() }
    }

    @Test
    fun `doWork does NOT show reminder if user already logged expense`() = runTest {
        // Given
        every { expenseDao.getTotalForDate(any(), any()) } returns 100f
        
        val worker = TestListenableWorkerBuilder<DailyReminderWorker>(context)
            .setWorkerFactory(TestWorkerFactory(expenseDao, notifier))
            .build()

        // When
        val result = worker.doWork()

        // Then
        assertEquals(ListenableWorker.Result.success(), result)
        verify(exactly = 0) { notifier.showDailyReminder() }
    }
}

class TestWorkerFactory(
    private val expenseDao: ExpenseDao,
    private val notifier: NotificationHelper
) : WorkerFactory() {
    override fun createWorker(
        appContext: Context,
        workerClassName: String,
        workerParameters: WorkerParameters
    ): ListenableWorker? {
        return when (workerClassName) {
            DailyReminderWorker::class.java.name ->
                DailyReminderWorker(
                    appContext, 
                    workerParameters, 
                    expenseDao, 
                    notifier, 
                    reschedule = {} // No-op for testing to avoid WorkManager init
                )
            else -> null
        }
    }
}
