package com.example.expensetracker.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.MapColumn
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface ExpenseDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(expense: Expense)

    @Query("SELECT * FROM expenses ORDER BY date DESC")
    fun getAllExpenses(): Flow<List<Expense>>

    @Query("""
        SELECT * FROM expenses 
        WHERE (:category = 'All' OR category = :category)
        AND date >= :startMs AND date <= :endMs
        ORDER BY date DESC
    """)
    fun getFilteredExpenses(
        category: String,
        startMs: Long?,
        endMs: Long?
    ): Flow<List<Expense>>

    @Query("SELECT * FROM expenses WHERE date BETWEEN :startDate AND :endDate ORDER BY date DESC")
    fun getExpensesByDateRange(startDate: Long, endDate: Long): Flow<List<Expense>>

    @Query("SELECT * FROM expenses WHERE category = :category ORDER BY date DESC")
    fun getExpensesByCategory(category: String): Flow<List<Expense>>

    @Query("SELECT SUM(price) FROM expenses WHERE date BETWEEN :startDate AND :endDate")
    fun getTotalBetweenDates(startDate: Long, endDate: Long): Flow<Double?>

    @Query("""
        SELECT COALESCE(SUM(price), 0) 
        FROM expenses 
        WHERE timestamp BETWEEN :startOfDay AND :endOfDay
    """)
    fun getTotalForDate(startOfDay: Long, endOfDay: Long): Float

    @Query("""
    SELECT COALESCE(SUM(price), 0) 
    FROM expenses 
    WHERE timestamp BETWEEN :startOfWeek AND :endOfWeek
""")
    suspend fun getTotalForWeek(startOfWeek: Long, endOfWeek: Long): Float

    @Query("SELECT COALESCE(SUM(price), 0) FROM expenses WHERE category = :category AND timeStamp BETWEEN :start AND :end")
    suspend fun getTotalByCategoryAndMonth(category: String, start: Long, end: Long): Float

    @Delete
    suspend fun delete(expense: Expense)

    @Update
    suspend fun update(expense: Expense)

    @Upsert
    suspend fun upsert(expense: Expense)

    @Query("""
        SELECT category, SUM(price) as total
        FROM expenses
        WHERE date BETWEEN :startDate AND :endDate
        GROUP BY category
        ORDER BY total DESC
    """)
    suspend fun getSpentByCategoryBetween(
        startDate: Long,
        endDate: Long
    ): Map<@MapColumn(columnName = "category") String, @MapColumn(columnName = "total") Double>

    @Query("""
        SELECT *
        FROM expenses
        WHERE date BETWEEN :startDate AND :endDate
        ORDER BY price DESC
    """)
    suspend fun getTopExpensesBetween(
        startDate: Long,
        endDate: Long,
    ): List<Expense>

    @Query("SELECT COUNT(*) FROM expenses WHERE date BETWEEN :start AND :end")
    suspend fun getCountBetween(start: Long, end: Long): Int

    @Query("SELECT SUM(price) FROM expenses WHERE date BETWEEN :start AND :end")
    suspend fun getTotalBetween(start: Long, end: Long): Double
}




@Dao
interface CategoryBudgetDao {
    @Query("SELECT * FROM categories_budget WHERE month = :month AND year = :year")
    fun getBudgetForMonth(month: Int, year: Int): Flow<List<CategoryBudget>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertBudget(budget: CategoryBudget)

    @Insert
    suspend fun insertBudget(budget: CategoryBudget)


    @Query("DELETE FROM categories_budget WHERE category = :category AND month = :month AND year = :year")
    suspend fun deleteBudget(category: String, month: Int, year: Int)

    @Query("SELECT monthlyBudget FROM categories_budget WHERE category = :category AND month = :month AND year = :year LIMIT 1")
    suspend fun getBudgetForOneMonth(category: String, month: Int, year: Int): Double?
}
