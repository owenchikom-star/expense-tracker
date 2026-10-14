package com.example.expensetracker

import com.example.expensetracker.data.ReceiptParser
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class ReceiptParserTest {

    @Test
    fun `parse extracts amount with English TOTAL keyword`() {
        val text = """
            CHECKERS STORE
            VAT NO: 123456789
            ITEMS:
            MILK R25.00
            BREAD R15.00
            TOTAL R40.00
            CASH R50.00
        """.trimIndent()

        val result = ReceiptParser.parse(text)
        assertEquals(40.0, result.amount, 0.001)
    }

    @Test
    fun `parse extracts amount with German GESAMT keyword`() {
        val text = """
            ALDI SUD
            MILCH 1,20
            BROT 2,50
            GESAMT EUR 3,70
            GEGEBEN 10,00
        """.trimIndent()

        val result = ReceiptParser.parse(text)
        assertEquals(3.70, result.amount, 0.001)
    }

    @Test
    fun `parse extracts amount with French MONTANT keyword`() {
        val text = """
            CARREFOUR
            LAIT 1.10
            PAIN 2.20
            MONTANT A PAYER 3.30
        """.trimIndent()

        val result = ReceiptParser.parse(text)
        assertEquals(3.30, result.amount, 0.001)
    }

    @Test
    fun `parse handles value on next line`() {
        val text = """
            SHOPRITE
            GRAND TOTAL
            159.99
        """.trimIndent()

        val result = ReceiptParser.parse(text)
        assertEquals(159.99, result.amount, 0.001)
    }

    @Test
    fun `parse uses fallback for largest amount when no keyword found`() {
        val text = """
            Unknown Store
            INV-12345
            12.50
            85.00
            20.00
        """.trimIndent()

        val result = ReceiptParser.parse(text)
        assertEquals(85.0, result.amount, 0.001)
    }

    @Test
    fun `parse handles DD MMM YYYY text dates`() {
        val text = """
            PICK N PAY
            16 July 2026
            TOTAL R100.00
        """.trimIndent()

        val result = ReceiptParser.parse(text)
        assertEquals(LocalDate.of(2026, 7, 16), result.date)
    }

    @Test
    fun `parse handles ISO dates`() {
        val text = """
            SPAR
            2026-07-16
            TOTAL R50.00
        """.trimIndent()

        val result = ReceiptParser.parse(text)
        assertEquals(LocalDate.of(2026, 7, 16), result.date)
    }

    @Test
    fun `parse extracts store name correctly from known list`() {
        val text = """
            McDonald's Restaurant
            Store #445
            TOTAL $12.50
        """.trimIndent()

        val result = ReceiptParser.parse(text)
        assertEquals("McDonald's Restaurant", result.storeName)
    }

    @Test
    fun `parse extracts unknown store name from top line`() {
        val text = """
            The Gourmet Bistro
            123 Main St
            TOTAL R450.00
        """.trimIndent()

        val result = ReceiptParser.parse(text)
        assertEquals("The Gourmet Bistro", result.storeName)
    }

    @Test
    fun `parse handles OCR noise in numeric context`() {
        // TotaI (I -> l) and R12o.00 (o -> 0)
        val text = """
            CHECKERS
            TotaI R12o.00
        """.trimIndent()

        val result = ReceiptParser.parse(text)
        assertEquals(120.0, result.amount, 0.001)
    }
}
