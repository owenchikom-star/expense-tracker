package com.example.expensetracker

import android.content.Context
import android.content.Intent
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import com.example.expensetracker.data.notifications.BootReceiver
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Before
import org.junit.Test

class BootReceiverTest {

    private lateinit var context: Context
    private lateinit var workManager: WorkManager

    @Before
    fun setup() {
        context = mockk(relaxed = true)
        workManager = mockk(relaxed = true)
    }

    @Test
    fun `onReceive with BOOT_COMPLETED schedules workers`() {
        // Given
        val receiver = BootReceiver(workManager)
        val intent = mockk<Intent>()
        every { intent.action } returns Intent.ACTION_BOOT_COMPLETED

        // When
        receiver.onReceive(context, intent)

        // Then
        verify(exactly = 1) {
            workManager.enqueueUniqueWork(
                "daily_8pm_work",
                ExistingWorkPolicy.REPLACE,
                any<OneTimeWorkRequest>()
            )
        }
        verify(exactly = 1) {
            workManager.enqueueUniqueWork(
                "weekly_sunday_work",
                ExistingWorkPolicy.REPLACE,
                any<OneTimeWorkRequest>()
            )
        }
    }

    @Test
    fun `onReceive with other action does nothing`() {
        // Given
        val receiver = BootReceiver(workManager)
        val intent = mockk<Intent>()
        every { intent.action } returns "SOME_OTHER_ACTION"

        // When
        receiver.onReceive(context, intent)

        // Then
        verify(exactly = 0) {
            workManager.enqueueUniqueWork(any(), any(), any<OneTimeWorkRequest>())
        }
    }
}
