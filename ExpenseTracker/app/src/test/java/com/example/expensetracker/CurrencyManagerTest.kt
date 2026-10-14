package com.example.expensetracker

import android.content.Context
import android.content.SharedPreferences
import com.example.expensetracker.data.CurrencyManager
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.Locale

class CurrencyManagerTest {

    private lateinit var context: Context
    private lateinit var prefs: SharedPreferences
    private lateinit var currencyManager: CurrencyManager

    @Before
    fun setup() {
        context = mockk(relaxed = true)
        prefs = mockk(relaxed = true)
        currencyManager = CurrencyManager(context, prefs)
    }

    @Test
    fun `format uses saved currency code`() {
        // Given
        every { prefs.getString("app_currency_code", null) } returns "USD"
        
        // When
        val result = currencyManager.format(1234.56)
        
        // Then
        // Depending on the default locale, the exact formatting might differ, 
        // but it should contain the symbol or code.
        // For USD, it usually starts with $ or contains USD.
        assertTrue(result.contains("$") || result.contains("USD"))
        assertTrue(result.contains("1,234.56") || result.contains("1.234,56"))
    }
}
