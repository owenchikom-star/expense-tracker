package com.example.expensetracker.data

import android.content.Context
import android.content.SharedPreferences
import java.util.Currency
import java.util.Locale
import androidx.core.content.edit
import java.text.NumberFormat

class CurrencyManager(private val context: Context, private val prefs: SharedPreferences) {

    private val KEY_CURRENCY = "app_currency_code"

    fun getCurrencyCode(): String {
        return prefs.getString(KEY_CURRENCY, null)
            ?: Currency.getInstance(Locale.getDefault()).currencyCode // Auto-detect
    }

    fun setCurrencyCode(code: String) {
        prefs.edit { putString(KEY_CURRENCY, code) }
    }

    fun format(amount: Double, specificCode: String? = null): String {
        val code = specificCode ?: getCurrencyCode()
        val currency = Currency.getInstance(code)
        val locale = Locale.getAvailableLocales().firstOrNull {
            try {
                Currency.getInstance(it).currencyCode == code
            } catch (e: Exception) {
                false
            }
        } ?: Locale.getDefault()

        val formatter = NumberFormat.getCurrencyInstance(locale)
        formatter.currency = currency
        return formatter.format(amount)
    }

    fun getAllCurrencies(): List<CurrencyItem> {
        return Currency.getAvailableCurrencies()
            .map {
                val locale = Locale.getAvailableLocales().firstOrNull { l ->
                    try {
                        Currency.getInstance(l).currencyCode == it.currencyCode
                    } catch (e: Exception) {
                        false
                    }
                }
                CurrencyItem(it.currencyCode, it.displayName, it.symbol, locale)
            }
            .sortedBy { it.displayName }
    }
}

data class CurrencyItem(
    val code: String,
    val displayName: String,
    val symbol: String,
    val locale: Locale?,
)