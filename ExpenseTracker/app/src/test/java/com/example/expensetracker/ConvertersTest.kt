package com.example.expensetracker

import com.example.expensetracker.data.Converters
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class ConvertersTest {

    private val converters = Converters()

    @Test
    fun fromLocalDate_convertsToEpochDay() {
        val date = LocalDate.of(2026, 7, 16)
        val epochDay = converters.fromLocalDate(date)
        assertEquals(20650L, epochDay)
    }

    @Test
    fun toLocalDate_convertsFromEpochDay() {
        val epochDay = 20650L
        val date = converters.toLocalDate(epochDay)
        assertEquals(LocalDate.of(2026, 7, 16), date)
    }

    @Test
    fun roundTrip_returnsOriginalDate() {
        val originalDate = LocalDate.now()
        val epochDay = converters.fromLocalDate(originalDate)
        val resultDate = converters.toLocalDate(epochDay!!)
        assertEquals(originalDate, resultDate)
    }
}
