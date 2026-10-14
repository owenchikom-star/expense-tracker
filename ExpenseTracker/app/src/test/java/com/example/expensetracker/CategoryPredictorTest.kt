package com.example.expensetracker

import com.example.expensetracker.data.CategoryPredictor
import org.junit.Assert.assertEquals
import org.junit.Test

class CategoryPredictorTest {

    @Test
    fun `predict matches known keywords for Food`() {
        assertEquals("Food", CategoryPredictor.predict("Checkers groceries", 200.0))
        assertEquals("Food", CategoryPredictor.predict("Coffee at Starbucks", 45.0))
        assertEquals("Food", CategoryPredictor.predict("Pizza Night", 150.0))
    }

    @Test
    fun `predict matches known keywords for Transport`() {
        assertEquals("Transport", CategoryPredictor.predict("Uber to airport", 150.0))
        assertEquals("Transport", CategoryPredictor.predict("Petrol BP", 800.0))
        assertEquals("Transport", CategoryPredictor.predict("Gautrain trip", 35.0))
    }

    @Test
    fun `predict matches known keywords for Bills`() {
        assertEquals("Bills", CategoryPredictor.predict("Electricity Eskom", 500.0))
        assertEquals("Bills", CategoryPredictor.predict("Rent for July", 6000.0))
        assertEquals("Bills", CategoryPredictor.predict("Vodacom Data", 199.0))
    }

    @Test
    fun `predict matches known keywords for Shopping`() {
        assertEquals("Shopping", CategoryPredictor.predict("Takealot order", 450.0))
        assertEquals("Shopping", CategoryPredictor.predict("New Shoes at Mr Price", 300.0))
    }

    @Test
    fun `predict matches known keywords for Health`() {
        assertEquals("Health", CategoryPredictor.predict("Clicks Pharmacy", 120.0))
        assertEquals("Health", CategoryPredictor.predict("Doctor visit", 550.0))
    }

    @Test
    fun `predict uses amount heuristics when no keywords found`() {
        // High amount defaults to Bills
        assertEquals("Bills", CategoryPredictor.predict("Large Unknown Expense", 7500.0))
        
        // Small amount defaults to Food
        assertEquals("Food", CategoryPredictor.predict("Small snack", 25.0))
    }

    @Test
    fun `predict defaults to Other for unknown items with medium price`() {
        assertEquals("Other", CategoryPredictor.predict("Random Item", 500.0))
    }

    @Test
    fun `predict is case insensitive`() {
        assertEquals("Transport", CategoryPredictor.predict("UBER RIDE", 100.0))
        assertEquals("Food", CategoryPredictor.predict("CHECKERS", 200.0))
    }
}
