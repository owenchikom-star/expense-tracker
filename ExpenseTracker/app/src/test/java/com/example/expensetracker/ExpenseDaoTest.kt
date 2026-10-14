package com.example.expensetracker

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.expensetracker.data.Expense
import com.example.expensetracker.data.ExpenseDao
import com.example.expensetracker.data.ExpenseDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
class ExpenseDaoTest {

    private lateinit var db: ExpenseDatabase
    private lateinit var dao: ExpenseDao

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, ExpenseDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.expenseDao()
    }

    @After
    fun closeDb() {
        db.close()
    }

    @Test
    fun insertExpenseWithReceiptImagePathAndReadItBack() = runBlocking {
        // Given
        val expense = Expense(
            id = 0,
            name = "Checkers",
            price = 249.50,
            category = "Food",
            date = LocalDate.of(2026, 7, 16),
            note = "Groceries",
            receiptImagePath = "/data/user/0/com.example.expensetracker/files/receipts/gallery_123.jpg"
        )

        // When
        dao.insert(expense)
        val all = dao.getAllExpenses().first()

        // Then
        assertEquals(1, all.size)
        val saved = all[0]
        assertEquals("Checkers", saved.name)
        assertEquals(249.50, saved.price, 0.001)
        assertEquals("/data/user/0/com.example.expensetracker/files/receipts/gallery_123.jpg", saved.receiptImagePath)
    }

    @Test
    fun updateExpenseKeepsNewImagePath() = runBlocking {
        // Given
        val original = Expense(
            id = 0, name = "Old", price = 50.0, category = "Other",
            date = LocalDate.now(), note = "", receiptImagePath = "/old.jpg"
        )
        dao.insert(original)
        val inserted = dao.getAllExpenses().first().first()

        // When
        val updated = inserted.copy(name = "New", receiptImagePath = "/new.jpg")
        dao.update(updated)
        val result = dao.getAllExpenses().first().first()

        // Then
        assertEquals("New", result.name)
        assertEquals("/new.jpg", result.receiptImagePath)
    }

    @Test
    fun insertExpenseWithNullImagePath() = runBlocking {
        // Given
        val expense = Expense(
            id = 0, name = "Cash", price = 20.0, category = "Transport",
            date = LocalDate.now(), note = "", receiptImagePath = null
        )

        // When
        dao.insert(expense)
        val saved = dao.getAllExpenses().first().first()

        // Then
        assertNull(saved.receiptImagePath)
    }

    @Test
    fun getSpentByCategoryBetweenSumsCorrectly() = runTest {
        val start = LocalDate.of(2026, 1, 1).toEpochDay()
        val end = LocalDate.of(2026, 1, 31).toEpochDay()
        
        dao.insert(Expense(0, "Shoprite", 500.0, "Food", LocalDate.of(2026, 1, 5), ""))
        dao.insert(Expense(0, "Uber", 200.0, "Transport", LocalDate.of(2026, 1, 10), ""))
        dao.insert(Expense(0, "Woolies", 300.0, "Food", LocalDate.of(2026, 1, 15), ""))
        // Outside range
        dao.insert(Expense(0, "Rent", 5000.0, "Bills", LocalDate.of(2026, 2, 1), ""))

        val result = dao.getSpentByCategoryBetween(start, end)

        assertEquals(800.0, result["Food"] ?: 0.0, 0.001)
        assertEquals(200.0, result["Transport"] ?: 0.0, 0.001)
        assertNull(result["Bills"])
    }

    @Test
    fun getTopExpensesBetweenReturnsOrderedByPrice() = runBlocking {
        val start = LocalDate.of(2026, 1, 1).toEpochDay()
        val end = LocalDate.of(2026, 1, 31).toEpochDay()
        
        dao.insert(Expense(0, "Rent", 5000.0, "Bills", LocalDate.of(2026, 1, 5), ""))
        dao.insert(Expense(0, "Shoprite", 500.0, "Food", LocalDate.of(2026, 1, 10), ""))
        dao.insert(Expense(0, "Coffee", 50.0, "Food", LocalDate.of(2026, 1, 15), ""))

        val result = dao.getTopExpensesBetween(start, end)

        assertTrue(result.size >= 3)
        assertEquals("Rent", result[0].name)
        assertEquals("Shoprite", result[1].name)
        assertEquals("Coffee", result[2].name)
    }
}
