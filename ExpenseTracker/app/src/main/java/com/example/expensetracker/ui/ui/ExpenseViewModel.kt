package com.example.expensetracker.ui.ui

import android.content.Context
import android.icu.util.Currency
import android.net.Uri
import android.util.Log
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.expensetracker.data.BarData
import com.example.expensetracker.data.BudgetSummary
import com.example.expensetracker.data.CategoryBudget
import com.example.expensetracker.data.CategoryBudgetRepository
import com.example.expensetracker.data.CategoryPredictor
import com.example.expensetracker.data.CurrencyManager
import com.example.expensetracker.data.DashboardUiState
import com.example.expensetracker.data.DateFilter
import com.example.expensetracker.data.Expense
import com.example.expensetracker.data.ExpenseRepository
import com.example.expensetracker.data.MonthlyReport
import com.example.expensetracker.data.ParsedReceipt
import com.example.expensetracker.data.PdfReportGenerator
import com.example.expensetracker.data.TimeRange
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters

class ExpenseViewModel(
    private val expenseRepository: ExpenseRepository,
    private val categoryBudgetRepository: CategoryBudgetRepository,
    val currencyManager: CurrencyManager,
    private val context: Context
    ) : ViewModel() {

        private val today = LocalDate.now()


    private val _pdfUri = MutableStateFlow<Uri?>(null)
    val pdfUri: StateFlow<Uri?> = _pdfUri

    private val _currencyCode = MutableStateFlow(currencyManager.getCurrencyCode())
    val currencyCode: StateFlow<String> = _currencyCode.asStateFlow()

    val allCurrencies = currencyManager.getAllCurrencies()

    fun updateCurrency(code: String) {
        currencyManager.setCurrencyCode(code)
        _currencyCode.value = code
    }


    val _report = MutableStateFlow(MonthlyReport(0, 0, 0.0, 0.0, emptyMap(), emptyMap(), emptyList()))
    val report = _report.asStateFlow()


    private var lastDeletedExpense: Expense? = null

    private val _dateFilter = MutableStateFlow(DateFilter.ALL)
    val dateFilter = _dateFilter.asStateFlow()

    private val expenseFlow = expenseRepository.getAllExpenses()

    private val _selectedRange = MutableStateFlow(TimeRange.WEEK)

    private val _dashUiState = MutableStateFlow(DashboardUiState())


    val dashUiState: StateFlow<DashboardUiState> = combine(
        expenseFlow,
        _selectedRange
    ) { expenses, range -> // 👈 This only runs when BOTH change
        val chartData = when (range) {
            TimeRange.DAY -> calculateLastNDays(expenses, 7)
            TimeRange.WEEK -> calculateLastNWeeks(expenses, 4)
            TimeRange.MONTH -> calculateLastNMonths(expenses, 6)
        }
        DashboardUiState(
            expenses = expenses,
            chartData = chartData,
            selectedRange = range
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DashboardUiState())



    init {
        viewModelScope.launch {
            expenseRepository.getAllExpenses().collect { expenses ->
                updateChart(expenses, _dashUiState.value.selectedRange)
            }
        }
    }


        val categoryBudgets: StateFlow<List<CategoryBudget>> =
            categoryBudgetRepository.getBudgetForMonth(today.monthValue, today.year)
                .map { list ->
                    list.groupBy { it.category }
                        .mapValues { (_, dupes) -> dupes.maxByOrNull { it.id }!! }
                        .values.toList()
                }
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        val spentPerCategory: StateFlow<Map<String, Double>> = expenseRepository.getAllExpenses()
            .map { expenses ->
                expenses.filter {
                    it.date.monthValue == today.monthValue && it.date.year == today.year
                            && it.category.isNotBlank()
                }.groupBy { it.category }
                    .mapValues { (_, list) ->list.sumOf { it.price } }
            } .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())


    val chartData: StateFlow<List<Pair<String, Double>>> = spentPerCategory
        .map { map -> map.filter { it.value > 0 }.toList().sortedByDescending { it.second } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())


    val budgetSummary: StateFlow<BudgetSummary> = combine(categoryBudgets,spentPerCategory) {
              budgets, spent ->
              val totalBudget = budgets.sumOf { it.monthlyBudget }
              val totalSpent = budgets.sumOf { category -> spent[category.category]?:0.0 }
              val overBudget = budgets.count { category ->
                  val spentAmount = spent[category.category]?:0.0
                  spentAmount > category.monthlyBudget
              }

              Log.d("BUDGET", "budgets=${budgets.map{"${it.category}:${it.monthlyBudget}"}} spentKeys=${spent.keys} totalBudget=$totalBudget")

              BudgetSummary(
                  totalBudget = totalBudget,
                  totalSpent = totalSpent,
                  remaining = totalBudget - totalSpent,
                  overBudgetCategories = overBudget
              )
              }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), BudgetSummary(0.0,0.0,0.0,0))


    init {
        viewModelScope.launch {
            if (categoryBudgetRepository.getBudgetForMonth(today.monthValue, today.year).first().isEmpty()) {
                 listOf("Food", "Transport", "Bills", "Entertainment", "Health", "Shopping", "Other").forEach {
                     categoryBudgetRepository.upsertBudget(CategoryBudget(0, it, 0.0, today.monthValue, today.year))
                 }
            }
        }
    }



    val hasLastMonthBudgets: StateFlow<Boolean> = flow {
        val lastMonth = LocalDate.now().minusMonths(1)
        val budgets = categoryBudgetRepository.getBudgetForMonth(lastMonth.monthValue, lastMonth.year).first()
        emit(budgets.isNotEmpty())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val hasCurrentBudgets: StateFlow<Boolean> = categoryBudgets
        .map { it.isNotEmpty() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)


        private val _searchQuery = MutableStateFlow("")
        val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

        val _searchTrigger = MutableStateFlow("")

    val _selectedExpense = MutableStateFlow(Expense(id = 0, name = "", price = 0.0, category = "", date = LocalDate.now(), note = "", timeStamp = 0, receiptImagePath = ""))
    val selectedExpense: StateFlow<Expense> = _selectedExpense.asStateFlow()


    private val _selectedCategory = MutableStateFlow("All")
        val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

        private val allExpenses: StateFlow<List<Expense>> = expenseRepository.getAllExpenses()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())


    @OptIn(ExperimentalCoroutinesApi::class)
    val expenseList: StateFlow<List<Expense>> = expenseRepository.getAllExpenses()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

        val searchExpenseList: StateFlow<List<Expense>> = combine(
            allExpenses, _searchQuery, _selectedCategory
        ) { expenses, query, category ->
            expenses.filter { expense ->
                val matchesSearch = query.isBlank() ||
                        expense.name.contains(query, ignoreCase = true) ||
                        expense.note.contains(query, ignoreCase = true)
                val matchesCategory = category == "All" || expense.category == category
                matchesSearch && matchesCategory
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val suggestions: StateFlow<List<String>> = combine(allExpenses, _searchQuery) { expenses, query ->
        if (query.isBlank()) {
            emptyList()
        } else {
            expenses.map { it.name }
                .distinct()
                .filter { it.contains(query, ignoreCase = true) }
                .take(5)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList() )

    private val _selectedMonth = MutableStateFlow(YearMonth.now())
    val selectedMonth: StateFlow<YearMonth> = _selectedMonth.asStateFlow()
    private val emptyExpense = Expense(id = 0, name = "", price = 0.0, category = "", date = LocalDate.now(), note = "", timeStamp = 0, receiptText = "", receiptImagePath = null).copy()
    private val _uiState = MutableStateFlow(ExpenseUiState())


    @OptIn(ExperimentalCoroutinesApi::class)
    val monthlyTotal: StateFlow<Double> = selectedMonth
        .flatMapLatest { yearMonth ->
            val startDate = yearMonth.atDay(1).toEpochDay()
            val endDate = yearMonth.atEndOfMonth().toEpochDay()
            expenseRepository.getTotalBetweenDates(startDate, endDate)
        }. map { it ?: 0.0 }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = 0.0
        )
    val expensesUiState: StateFlow<ExpenseUiState> = _uiState.asStateFlow()

    private val _expense: MutableStateFlow<Expense> = MutableStateFlow(emptyExpense)
    val expense: StateFlow<Expense> = _expense.asStateFlow()



    val filteredExpense: StateFlow<List<Expense>> = combine(
        expenseList,
        searchQuery,
        selectedCategory,
        _dateFilter
    ) { expenses, query, category, dateFilter ->


        Log.d("FILTER_DEBUG",
            "Selected category='$category' | " +
                    "DB categories=${expenses.map { "'${it.category}'" }.distinct()} | " +
                    "Total expenses=${expenses.size}"
        )

        val now = LocalDate.now()

        // Calculate Week Range precisely
        val startOfWeek = now.with(DayOfWeek.MONDAY)
        val endOfWeek = now.with(DayOfWeek.SUNDAY)

        expenses.filter { expense ->
            // 1. Category Filter: If "All", ignore category check
            val matchesCategory = category.equals("All", ignoreCase = true) ||
                    expense.category.trim().equals(category.trim(), ignoreCase = true)

            if (category != "All") {
                Log.d("FILTER_DEBUG",
                    "Checking: ${expense.name} | cat='${expense.category}' | " +
                            "matchesCat=$matchesCategory | date=${expense.date}"
                )
            }

            // 2. Search Filter
            val matchesSearch = query.isBlank() ||
                    expense.name.contains(query, ignoreCase = true) ||
                    expense.note.contains(query, ignoreCase = true)

            // 3. Date Filter
            val expenseDate = expense.date
            val matchesDate = when (dateFilter) {
                DateFilter.ALL -> true
                DateFilter.THIS_WEEK -> !expenseDate.isBefore(startOfWeek) && !expenseDate.isAfter(endOfWeek)
                DateFilter.THIS_MONTH -> expenseDate.month == now.month && expenseDate.year == now.year
                DateFilter.LAST_3_MONTHS -> !expenseDate.isBefore(now.minusMonths(3))
            }

            // Only return true if ALL THREE conditions are met
            matchesCategory && matchesSearch && matchesDate
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )



    val filterCounts: StateFlow<Map<DateFilter, Int>> =
        combine(expenseList, _selectedCategory, _dateFilter) { expenses, category, dateFilter ->
            val zone = ZoneId.systemDefault()
            val now = LocalDate.now(zone)




            // Filter by category first, same as your list
            val filtered = if (category == "All") expenses else expenses.filter {
                it.category.equals(category, ignoreCase = true)
            }

            // Convert Long to LocalDate once
            val expenseDates = filtered.map {
                Instant.ofEpochMilli(it.date.atStartOfDay(zone).toInstant().toEpochMilli()).atZone(zone).toLocalDate()
            }

            val startOfWeek = now.with(DayOfWeek.MONDAY)
            val endOfWeek = now.with(DayOfWeek.SUNDAY)
            val startOfMonth = now.withDayOfMonth(1)
            val endOfMonth = now.withDayOfMonth(now.lengthOfMonth())
            val start3Months = now.minusMonths(12).withDayOfMonth(1)

            Log.d("COUNTS", "cat=$category, ALL=${expenseDates.size}, WEEK=${expenseDates.count { d -> d in startOfWeek..endOfWeek }}")


            Log.d("COUNTS", "now=$now, startWeek=$startOfWeek, endWeek=$endOfWeek")
            expenseDates.take(5).forEach { d ->
                Log.d("COUNTS", "expense date=$d, inWeek=${d in startOfWeek..endOfWeek}")
            }


            mapOf(
                DateFilter.ALL to expenseDates.size,
                DateFilter.THIS_WEEK to expenseDates.count {
                    !it.isBefore(startOfWeek) && !it.isAfter(endOfWeek)
                },
                DateFilter.THIS_MONTH to expenseDates.count { it >=
                        startOfMonth && it <= endOfMonth && it.year == now.year
                },
                DateFilter.LAST_3_MONTHS to expenseDates.count { it >=
                        start3Months && it <= now && it.year == now.year
                }
            )

        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())


    val categorySpendData: StateFlow<List<Pair<String, Double>>> = combine(
        expenseList, // Use the full list
        _dateFilter, // Only filter by date, NOT by selectedCategory
        _selectedCategory
    ) { expenses, dateFilter, selectedCategory ->
        val now = LocalDate.now()
        expenses
            .filter { expense ->
                // Apply ONLY date filtering here so the chart stays visible
                when (dateFilter) {
                    DateFilter.ALL -> true
                    DateFilter.THIS_WEEK -> !expense.date.isBefore(now.minusDays(7)) && !expense.date.isAfter(now)
                    else -> true
                }
            }
            .groupBy { it.category }
            .mapValues { it.value.sumOf { e -> e.price } }
            .toList()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())


    fun fillFromReceipt(parsed: ParsedReceipt, imagePath: String?) {
        _uiState.value = _uiState.value.copy(
            name = parsed.storeName,
            price = parsed.amount.toString(),
            category = parsed.category,
            date = parsed.date,
            receiptImagePath = imagePath ?: _uiState.value.receiptImagePath
        )

    }

    fun exportMonthlyReport(month: Int, year: Int) {
        viewModelScope.launch {
            val report = categoryBudgetRepository.getMonthlyReportData(month, year)
            val file = PdfReportGenerator(context, currencyManager).generateMonthlyReport(report)
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
            _pdfUri.value = uri
        }
    }

    fun showCurrencySettingsScreen() {
        currencyManager.getAllCurrencies()
    }

    fun setCurrency(currency: String) {
        currencyManager.setCurrencyCode(currency)
        _uiState.update { it.copy(isEntryValid = true) }
    }

    fun onRangeSelected(range: TimeRange) { // 👈 Called from UI buttons
        _selectedRange.value = range
    }

    private fun updateChart(expenses: List<Expense>, range: TimeRange) {
        val newData = when (range) {
            TimeRange.DAY -> calculateLastNDays(expenses, 7) // Mon, Tue
            TimeRange.WEEK -> calculateLastNWeeks(expenses, 4) // W1, W2...
            TimeRange.MONTH -> calculateLastNMonths(expenses, 6) // Jan, Feb...
        }
        _dashUiState.update {
            it.copy(chartData = newData, selectedRange = range)
        }
    }

    private fun calculateLastNDays(expenses: List<Expense>, n: Int): List<BarData> {
        val today = LocalDate.now()
        return (n-1 downTo 0).map { i ->
                val date = today.minusDays(i.toLong())
                val total = expenses.filter { it.date == date }.sumOf { it.price }.toFloat()
                BarData(
                    label = date.format(DateTimeFormatter.ofPattern("EEE")),
                    value = total,
                    subLabel = date.format(DateTimeFormatter.ofPattern("EEE, dd MMM"))
                )
            }
    }

    private fun calculateLastNWeeks(expenses: List<Expense>, n: Int): List<BarData> {
        val today = LocalDate.now()
        val thisMonday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

        return (n-1 downTo 0).map {i ->
            val weekStart = thisMonday.minusWeeks(i.toLong())
            val weekEnd = weekStart.plusDays(6)
            val total = expenses.filter { it.date in weekStart..weekEnd }.sumOf { it.price }.toFloat()
            BarData(
                label = "W${n - i}",
                value = total,
                subLabel = "${weekStart.format(DateTimeFormatter.ofPattern("dd MMM"))} - ${weekEnd.format(DateTimeFormatter.ofPattern("dd MMM"))}"
            ) // W1 = this week
        }
    }

    private fun calculateLastNMonths(expenses: List<Expense>, n: Int): List<BarData> {
        val today = LocalDate.now()
        return (0 until n).map { today.minusMonths(it.toLong()).withDayOfMonth(1) }.reversed()
            .map { monthStart ->
                val monthEnd = monthStart.plusMonths(1).minusDays(1)
                val total = expenses.filter { it.date in monthStart..monthEnd }.sumOf { it.price }.toFloat()
                BarData(label = monthStart.format(DateTimeFormatter.ofPattern("MMM")), value = total) // Jan
            }
    }



    fun deleteExpense(expense: Expense) {
        lastDeletedExpense = expense.copy() // <- save BEFORE deleting
        viewModelScope.launch {
            expenseRepository.delete(expense)
        }
    }

    fun undoDelete() {
        lastDeletedExpense?.let { expense ->
            lastDeletedExpense = null
            viewModelScope.launch {
                val restored = expense.copy(id = 0)
                expenseRepository.insert(restored)
            }
        }
    }




    // Function to update filter from UI
fun updateDateFilter(filter: DateFilter) {
    _dateFilter.value = filter
}


     fun setBudget(category: String, amount: Double) {
         viewModelScope.launch {
             val today = LocalDate.now()
             val newBudget = CategoryBudget(
                 id = 0, // id 0 tells Room to match by unique index: category+month+year
                 category = category.trim(),
                 monthlyBudget = amount,
                 month = today.monthValue,
                 year = today.year
             )
             Log.d("BUDGET", "Upserting: $newBudget")

             categoryBudgetRepository.upsertBudget(newBudget)
             val check = categoryBudgetRepository.getBudgetForOneMonth(
                 category,
                 today.monthValue,
                 today.year
             )
             Log.d("BUDGET", "Verify in DB: $check")
         }
     }

    fun copyLastMonthBudgets(
        targetMonth: Int = LocalDate.now().monthValue,
        targetYear: Int = LocalDate.now().year
    ) {
        viewModelScope.launch {
            categoryBudgetRepository.copyLastMonthBudgets(targetMonth, targetYear)
        }
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun triggerSearch() {
        _searchTrigger.value = searchQuery.value
    }

    fun selectedSuggestion(name: String) {
        _searchQuery.value = name
        _searchTrigger.value = name
        viewModelScope.launch {
            expenseRepository.getExpensesByCategory(category = "").first().find { it.name == name }
        }
    }

    fun updateCategoryFilter(category: String) {
        _selectedCategory.value = category
    }

    fun resetExpense() = _uiState.update { ExpenseUiState() }


    fun updateFormFields(id: Long, name: String, price: String) {
        _uiState.update {
            it.copy(
                id = id,
                name = name,
                price = price
            )
        }
    }

    fun onExpenseClick(expense: Expense) {
        _uiState.update {
            it.copy(
                editExpense = expense,
                id = expense.id,
                name = expense.name,
                price = expense.price.toString(),
                category = expense.category,
                date = expense.date,
                note = expense.note,
                isEditSheetOpen = true,
                receiptImagePath = expense.receiptImagePath
            )
        }
    }
    fun cancelEdit() {
        _uiState.update { ExpenseUiState() }
    }

    fun updateExpenseName(name: String) {
        val predictedCategory = CategoryPredictor.predict(name, _uiState.value.price.toDoubleOrNull() ?: 0.0)
        _uiState.update {
            it.copy(
                name = name,
                category = if (name.isNotBlank()) predictedCategory else it.category
            )
        }
    }

    fun updateExpensePrice(price: String) {
        _uiState.update { it.copy(price = price) }
    }

    fun updateId(id: Long) {
        _uiState.update { it.copy(id = id) }
    }

    fun updateCategory(category: String) {
        _uiState.update { it.copy(category = category) }
    }
    fun updateDate(date: LocalDate) {
        _uiState.update { it.copy(date = date) }
    }
    fun updateNote(note: String) {
        _uiState.update { it.copy(note = note) }
    }

    fun updateImagePath(imagePath: String) {
        _uiState.update { it.copy(receiptImagePath = imagePath) }
    }



    fun saveExpense() {
        viewModelScope.launch {
            val s = _uiState.value

            val base = s.editExpense ?: Expense( 0L, "", 0.0, "", LocalDate.now(), "", receiptText = "", receiptImagePath = null, ) // id will be 0 for Add, >0 for Edit

            val expenseToSave = base.copy(
                name = s.name.trim(),
                price = s.price.toDoubleOrNull() ?: 0.0,
                category = s.category.ifBlank {"Other"}.trim(),
                date = s.date,
                note = s.note.trim(),
                receiptImagePath = if (s.receiptImagePath.isNullOrBlank()) base.receiptImagePath else s.receiptImagePath
            ).let { 
                // Ensure "" becomes null for the DB
                if (it.receiptImagePath.isNullOrBlank()) it.copy(receiptImagePath = null) else it 
            }

            expenseRepository.upsert(expenseToSave) // Room: id=0 -> INSERT, id=5 -> UPDATE

                categoryBudgetRepository.onTransactionAdded(expenseToSave)
            _uiState.value = ExpenseUiState() // reset sheet
        }
    }
    

    }

data class ExpenseUiState  (
    val id: Long = 0L,
    val name: String = "",
    val price: String = "",
    val category: String = "Other",
    val date: LocalDate = LocalDate.now(),
    val note: String = "",
    val isEntryValid: Boolean = false,
    val isEditSheetOpen: Boolean = false,
    val editExpense: Expense? = null,
    val receiptImagePath: String? = null
)

class ExpenseViewModelFactory(
    private val expenseRepository: ExpenseRepository,
    private val categoryBudgetRepository: CategoryBudgetRepository,
    private val currencyManager: CurrencyManager,
    private val context: Context
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ExpenseViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ExpenseViewModel(
                expenseRepository,
                categoryBudgetRepository,
                currencyManager,
                context
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
