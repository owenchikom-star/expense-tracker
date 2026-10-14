package com.example.expensetracker.ui.ui

import android.annotation.SuppressLint
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SheetValue
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberStandardBottomSheetState
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.material3.windowsizeclass.WindowHeightSizeClass
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.expensetracker.R
import com.example.expensetracker.data.AppContainer
import com.example.expensetracker.data.BudgetSummary
import com.example.expensetracker.data.CategoryBudget
import com.example.expensetracker.data.CurrencyManager
import com.example.expensetracker.data.DateFilter
import com.example.expensetracker.data.Expense
import com.example.expensetracker.data.TimeRange
import kotlinx.coroutines.launch

@RequiresApi(Build.VERSION_CODES.P)
@SuppressLint("RememberReturnType")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseApp(
    windowSizeClass: WindowSizeClass,
    modifier: Modifier = Modifier,
    viewModel: ExpenseViewModel = viewModel(factory = ExpenseViewModelFactory(
        AppContainer(context = LocalContext.current).expenseRepository,
        AppContainer(context = LocalContext.current).categoryBudgetRepository,
        AppContainer(context = LocalContext.current).currencyManager,
        LocalContext.current
    ))
) {
    val categorySpendData by viewModel.categorySpendData.collectAsState()
    val hasLastMonthBudgets by viewModel.hasLastMonthBudgets.collectAsState()
    val hasCurrentMonthBudgets by viewModel.hasCurrentBudgets.collectAsState()
    val budgetSummary by viewModel.budgetSummary.collectAsState()
    val budget by viewModel.categoryBudgets.collectAsState()
    val spentMap by viewModel.spentPerCategory.collectAsState()
    val suggestions by viewModel.suggestions.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val categories =
        listOf("All", "Food", "Transport", "Bills", "Entertainment", "Health", "Shopping", "Other")
    val budgetCategories = categories.drop(1)
    val searchExpenseList by viewModel.searchExpenseList.collectAsState()
    val monthlyTotalAmount by viewModel.monthlyTotal.collectAsState()
    val expenseList by viewModel.expenseList.collectAsState()
    val expensesUiState by viewModel.expensesUiState.collectAsState()
    val expenses = expensesUiState
    val sheetScaffoldState = rememberBottomSheetScaffoldState(
        bottomSheetState = rememberStandardBottomSheetState(
            initialValue = SheetValue.Hidden,
            skipHiddenState = false
        )
    )
    var selectedExpense by remember { mutableStateOf<Expense?>(null) }
    val keyboardController = LocalSoftwareKeyboardController.current
    val snackBarHostState = remember { SnackbarHostState() }
    var recentlyDeletedExpense by remember { mutableStateOf(emptyList<Expense>()) }
    val scope = rememberCoroutineScope()
    var currentTab by rememberSaveable { mutableIntStateOf(0) }
    val tabTitles = listOf("OverView", "Transactions")
    val expense = viewModel.expense.collectAsState().value
    val dateFilter by viewModel.dateFilter.collectAsState()
    val filteredExpenses by viewModel.filteredExpense.collectAsState()
    val filterCounts by viewModel.filterCounts.collectAsState()
    var sheetKey by remember { mutableIntStateOf(0) }
    val dashboardUiState by viewModel.dashUiState.collectAsState()
    var currentScreen by rememberSaveable { mutableStateOf("main") }

    if (currentScreen == "currency") {
        CurrencySettingsScreen(
            viewModel = viewModel,
            onBack = { currentScreen = "main" }
        )
        return
    }

    BottomSheetScaffold(
        scaffoldState = sheetScaffoldState,
        snackbarHost = { SnackbarHost(snackBarHostState) },
        sheetContainerColor = colorScheme.surface,
        sheetPeekHeight = 0.dp,
        modifier = modifier.fillMaxSize(),
        sheetContent = {
            key(expensesUiState.editExpense?.id ?: 0L) {
                ExpenseEntryForm(
                    expenseViewModel = viewModel,
                    expensesUiState = expensesUiState,
                    enabled = expensesUiState.name.isNotBlank() && expensesUiState.price.isNotBlank(),
                    onSave = { text, imageUri ->
                        viewModel.saveExpense()
                        scope.launch { sheetScaffoldState.bottomSheetState.hide() }
                        viewModel.cancelEdit()
                        keyboardController?.hide()
                    },
                    onCancel = {
                        viewModel.cancelEdit()
                        scope.launch { sheetScaffoldState.bottomSheetState.hide() }
                    },
                    modifier = Modifier.padding(16.dp),
                    sheetScaffoldState = sheetScaffoldState,
                    onValueChange = { viewModel::updateFormFields },
                    content = {}
                )
            }
        }
    ) { paddingFromScaffold ->
        NavigationSuiteScaffold(
            modifier = Modifier.padding(paddingFromScaffold),
            navigationSuiteItems = {
                item(
                    selected = currentTab == 0,
                    onClick = { currentTab = 0 },
                    icon = { Icon(Icons.Default.PieChart, contentDescription = null) },
                    label = { Text(tabTitles[0]) }
                )
                item(
                    selected = currentTab == 1,
                    onClick = { currentTab = 1 },
                    icon = { Icon(Icons.Default.History, contentDescription = null) },
                    label = { Text(tabTitles[1]) },
                )
            }
        ) {
            Scaffold(
                topBar = {
                    ExpenseTopBar(
                        onSettingsClick = { currentScreen = "currency" },
                        windowSizeClass = windowSizeClass,
                        currencyManager = viewModel.currencyManager,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                floatingActionButton = {
                    // Only show FAB here on Compact screens. 
                    // On Medium/Expanded, it's often better to have it in a fixed location or in the rail.
                    if (windowSizeClass.widthSizeClass != WindowWidthSizeClass.Compact) {

                            ExtendedFloatingActionButton(
                                onClick = {
                                    viewModel.resetExpense()
                                    scope.launch { sheetScaffoldState.bottomSheetState.expand() }
                                },
                                icon = { Icon(Icons.Default.Add, null) },
                                text = { Text(stringResource(R.string.add_expense)) },
                            )
                    }
                },
                contentWindowInsets = WindowInsets(0, 0, 0, 0),
            ) { innerPadding ->
                Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
                    when (currentTab) {
                        0 -> DashboardScreen(
                            monthlyTotal = monthlyTotalAmount,
                            categorySpendData = categorySpendData,
                            budgetCategories = budgetCategories,
                            budget = budget,
                            spentMap = spentMap,
                            budgetSummary = budgetSummary,
                            hasLastMonthBudgets = hasLastMonthBudgets,
                            hasCurrentMonthBudgets = hasCurrentMonthBudgets,
                            onSetBudget = { category, amount -> viewModel.setBudget(category, amount) },
                            onCopyLastMonth = { viewModel.copyLastMonthBudgets() },
                            onAddClick = {
                                viewModel.resetExpense()
                                scope.launch { sheetScaffoldState.bottomSheetState.expand() }
                            },
                            viewModel = viewModel,
                            windowSizeClass = windowSizeClass,
                            currencyManager = viewModel.currencyManager,
                        )

                        1 -> ExpensesScreen(
                            searchQuery = searchQuery,
                            suggestions = suggestions,
                            onQueryChange = viewModel::updateSearchQuery,
                            onSearch = viewModel::triggerSearch,
                            onSuggestionClick = viewModel::selectedSuggestion,
                            categories = categories,
                            selectedCategory = selectedCategory,
                            onCategoryChange = viewModel::updateCategoryFilter,
                            expenses = filteredExpenses,
                            onExpenseClick = { expense -> selectedExpense = expense },
                            onDelete = { expense ->
                                viewModel.deleteExpense(expense)
                                scope.launch {
                                    val result = snackBarHostState.showSnackbar(
                                        message = "${expense.name} deleted",
                                        actionLabel = "Undo",
                                        duration = SnackbarDuration.Long
                                    )
                                    if (result == SnackbarResult.ActionPerformed) {
                                        viewModel.undoDelete()
                                    }
                                }
                            },
                            onEdit = { expense ->
                                viewModel.onExpenseClick(expense)
                                scope.launch { sheetScaffoldState.bottomSheetState.expand() }
                            },
                            monthlyTotalFormatted = viewModel.currencyManager.format(monthlyTotalAmount),
                            onAddClicked = {
                                viewModel.resetExpense()
                                scope.launch { sheetScaffoldState.bottomSheetState.expand() }
                            },
                            currencyManager = viewModel.currencyManager,
                            windowSizeClass = windowSizeClass,
                            dateFilter = dateFilter,
                            filterCounts = filterCounts,
                            onDateFilterChange = viewModel::updateDateFilter,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    // Detail Dialog (Fallback for non-adaptive detail)
                    if (windowSizeClass.widthSizeClass == WindowWidthSizeClass.Compact) {
                        selectedExpense?.let { expense ->
                            AlertDialog(
                                onDismissRequest = { selectedExpense = null },
                                confirmButton = {
                                    TextButton(onClick = { selectedExpense = null }) { Text(stringResource(R.string.close)) }
                                },
                                title = { Text(expense.name) },
                                text = {
                                    Column {
                                        Text("Amount: ${viewModel.currencyManager.format(expense.price)}")
                                        Text("Date: ${expense.date}")
                                        Text("Category: ${expense.category}")

                                        expense.receiptImagePath?.let { uri ->
                                            Spacer(Modifier.height(8.dp))
                                            AsyncImage(
                                                model = uri.toUri(),
                                                contentDescription = stringResource(R.string.full_receipt),
                                                modifier = Modifier.fillMaxWidth().height(400.dp),
                                                contentScale = ContentScale.Fit
                                            )
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}



@Composable
fun DashboardScreen(
    monthlyTotal: Double,
    categorySpendData: List<Pair<String, Double>>,
    budgetCategories: List<String>,
    budget: List<CategoryBudget>,
    spentMap: Map<String, Double>,
    budgetSummary: BudgetSummary,
    hasLastMonthBudgets: Boolean,
    hasCurrentMonthBudgets: Boolean,
    onSetBudget: (String, Double) -> Unit,
    onCopyLastMonth: () -> Unit,
    onAddClick: () -> Unit,
    currencyManager: CurrencyManager,
    modifier: Modifier = Modifier,
    viewModel: ExpenseViewModel,
    windowSizeClass: WindowSizeClass
) {
    val dashboardUiState by viewModel.dashUiState.collectAsState()
    val isWide = windowSizeClass.widthSizeClass != WindowWidthSizeClass.Compact

    Box(modifier = modifier.fillMaxSize()) {
        if (isWide) {
            // Adaptive Layout for Tablets/Landscape
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // Left Column: Overview and Charts
                Column(
                    modifier = Modifier
                        .weight(1.2f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = stringResource(R.string.spending),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    AdaptiveSpendingCard(dashboardUiState, viewModel)

                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(20.dp)) {
                            Text(stringResource(R.string.spent_this_month), style = MaterialTheme.typography.labelLarge)
                            Text(
                                viewModel.currencyManager.format(monthlyTotal),
                                style = MaterialTheme.typography.displaySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    SpendingPieChart(
                        data = categorySpendData,
                        currencyManager = viewModel.currencyManager,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(350.dp)
                    )
                }

                // Right Column: Budgets
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = stringResource(R.string.budget_overview),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    BudgetSection(
                        categories = budgetCategories,
                        budgets = budget,
                        spentMap = spentMap,
                        budgetSummary = budgetSummary,
                        currencyManager = viewModel.currencyManager,
                        hasLastMonthBudgets = hasLastMonthBudgets,
                        onBudgetSet = onSetBudget,
                        onCopyLastMonth = onCopyLastMonth,
                        hasCurrentMonthBudgets = hasCurrentMonthBudgets,
                    )
                }
            }
        } else {
            // Standard Phone Layout (Single Column)
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                AdaptiveSpendingCard(dashboardUiState, viewModel)

                // 1. Monthly total card
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(20.dp)) {
                        Text(stringResource(R.string.spent_this_month), style = MaterialTheme.typography.labelLarge)
                        Text(
                            viewModel.currencyManager.format(monthlyTotal),
                            style = MaterialTheme.typography.displaySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                SpendingPieChart(
                    data = categorySpendData,
                    currencyManager = viewModel.currencyManager,
                    modifier = Modifier.fillMaxWidth()
                        .height(320.dp)
                )

                BudgetSection(
                    categories = budgetCategories,
                    budgets = budget,
                    spentMap = spentMap,
                    budgetSummary = budgetSummary,
                    currencyManager = viewModel.currencyManager,
                    hasLastMonthBudgets = hasLastMonthBudgets,
                    onBudgetSet = onSetBudget,
                    onCopyLastMonth = onCopyLastMonth,
                    hasCurrentMonthBudgets = hasCurrentMonthBudgets,
                )
                MonthlyReportScreen(
                    viewModel = viewModel(),
                    context = LocalContext.current,
                    modifier = Modifier.padding(end = 16.dp)
                )
            }
        }

        // FAB only visible if not in compact mode or handled by Rail? 
        // Actually for large screens, FAB is often integrated in Rail, but we can keep it here too.
        if (!isWide) {
            ExpenseTrackerFAB(
                onClick = onAddClick,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
            )
        }
    }
}

@Composable
fun AdaptiveSpendingCard(
    dashboardUiState: com.example.expensetracker.data.DashboardUiState,
    viewModel: ExpenseViewModel
) {
    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Column(Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(stringResource(R.string.spending))

                SingleChoiceSegmentedButtonRow {
                    TimeRange.entries.forEachIndexed { index, range ->
                        SegmentedButton(
                            shape = SegmentedButtonDefaults.itemShape(
                                index = index,
                                count = 3
                            ),
                            onClick = { viewModel.onRangeSelected(range) },
                            selected = dashboardUiState.selectedRange == range
                        ) {
                            Text(range.name)
                        }
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            BarChart(
                data = dashboardUiState.chartData,
                currencyManager = viewModel.currencyManager
            )
        }
    }
}


@Composable
fun ExpensesScreen(
    searchQuery: String,
    suggestions: List<String>,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    onSuggestionClick: (String) -> Unit,
    categories: List<String>,
    selectedCategory: String,
    onCategoryChange: (String) -> Unit,
    expenses: List<Expense>,
    onExpenseClick: (Expense) -> Unit,
    onDelete: (Expense) -> Unit,
    onEdit: (Expense) -> Unit,
    monthlyTotalFormatted: String,
    onAddClicked: () -> Unit,
    currencyManager: CurrencyManager,
    windowSizeClass: WindowSizeClass,
    dateFilter: DateFilter,
    filterCounts: Map<DateFilter, Int>,
    onDateFilterChange: (DateFilter) -> Unit,
    modifier: Modifier = Modifier,
) {
    val isWide = windowSizeClass.widthSizeClass != WindowWidthSizeClass.Compact
    val showTotalBar = windowSizeClass.heightSizeClass != WindowHeightSizeClass.Compact || 
                      windowSizeClass.widthSizeClass == WindowWidthSizeClass.Expanded
    var selectedInList by remember { mutableStateOf<Expense?>(null) }

    Row(modifier = modifier.fillMaxSize()) {
        // List Column
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
        ) {
            // Search
            SearchBarWithSuggestions(
                searchQuery = searchQuery,
                suggestions = suggestions,
                onQueryChange = onQueryChange,
                onSearch = onSearch,
                onSuggestionClick = onSuggestionClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.small)
            )

            // Category filter chips
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(categories) { category ->
                    val selected = selectedCategory == category
                    FilterChip(
                        selected = selected,
                        onClick = { onCategoryChange(category) },
                        label = { Text(category) },
                    )
                }
            }

            // Expense List
            ExpenseList(
                expenses = expenses,
                onClick = {
                    if (isWide) {
                        selectedInList = it
                    } else {
                        onExpenseClick(it)
                    }
                },
                onDeleted = onDelete,
                onEdit = onEdit,
                dateFilter = DateFilter.ALL,
                currencyManager = currencyManager,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            )

            // Total Bar
            if (showTotalBar) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(72.dp)
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ExpenseTotalBar(
                        totalFormatted = monthlyTotalFormatted,
                        modifier = Modifier
                            .then(if (!isWide) Modifier.weight(1f) else Modifier.fillMaxWidth())
                            .fillMaxHeight()
                            .clip(MaterialTheme.shapes.small)
                    )
                    if (!isWide) {
                        Spacer(Modifier.width(8.dp))
                        ExpenseTrackerFAB(
                            onClick = onAddClicked,
                        )
                    }

                }
            }
        }

            // Detail Column (Only for Wide Screens)
            if (isWide) {
                VerticalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant)
                Column(
                    modifier = Modifier
                        .weight(0.8f)
                        .fillMaxHeight()
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (selectedInList != null) {
                        val exp = selectedInList!!
                        Text(
                            exp.name,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(16.dp))

                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(16.dp)) {
                                DetailRow("Amount", currencyManager.format(exp.price))
                                DetailRow("Date", exp.date.toString())
                                DetailRow("Category", exp.category)
                                if (exp.note.isNotBlank()) {
                                    DetailRow("Note", exp.note)
                                }
                            }
                        }

                        if (!exp.receiptImagePath.isNullOrBlank()) {
                            Spacer(Modifier.height(16.dp))
                            Text("Receipt", style = MaterialTheme.typography.titleMedium)
                            Spacer(Modifier.height(8.dp))
                            AsyncImage(
                                model = exp.receiptImagePath.toUri(),
                                contentDescription = "Receipt",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(500.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .border(1.dp, Color.LightGray, RoundedCornerShape(12.dp)),
                                contentScale = ContentScale.Fit
                            )
                        }

                        Spacer(Modifier.height(24.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            Button(onClick = { onEdit(exp) }) {
                                Icon(Icons.Default.Edit, null)
                                Spacer(Modifier.width(8.dp))
                                Text(stringResource(R.string.edit))
                            }
                            OutlinedButton(onClick = {
                                onDelete(exp)
                                selectedInList = null
                            }) {
                                Text(stringResource(R.string.delete), color = Color.Red)
                            }
                        }
                    } else {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("Select an expense to view details", color = Color.Gray)
                        }
                    }

                }
            }
        }
    }


@Composable
fun DetailRow(label: String, value: String) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.labelLarge, color = Color.Gray)
        Text(value, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun ExpenseTotalBar(totalFormatted: String, modifier: Modifier = Modifier) {

    Card(modifier = modifier.fillMaxWidth()
        .height(56.dp),
        colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surfaceContainer),
        shape = MaterialTheme.shapes.medium,
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp)
                .fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(id = R.string.total),
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(0.2f)
            )
            Text(
                text = totalFormatted,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.End
            )

        }
    }
}

@Composable
fun ExpenseList(
    expenses: List<Expense>,
    dateFilter: DateFilter,
    onEdit: (Expense) -> Unit,
    onDeleted: (Expense) -> Unit,
    currencyManager: CurrencyManager,
    modifier: Modifier = Modifier,
    onClick: (Expense) -> Unit,
) {

    Box(
        modifier = modifier.fillMaxSize()
    ) {
        if (expenses.isEmpty()) {
            EmptyTransactions(dateFilter = dateFilter)
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 80.dp),
            ) {
                items(expenses, key = { it.id }
                ) { expense ->

                    SwipeToDismissBox(
                        state = rememberSwipeToDismissBoxState(),
                        backgroundContent = {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.DarkGray)
                                    .padding(horizontal = 20.dp),
                                contentAlignment = Alignment.CenterEnd
                            ) {
                                Row {
                                    Text(
                                        text = stringResource(id = R.string.delete),
                                        color = Color.Red,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    androidx.compose.material3.Icon(
                                        painter = painterResource(id = R.drawable.baseline_delete_sweep_24),
                                        contentDescription = stringResource(id = R.string.delete),
                                        tint = Color.White
                                    )
                                }
                            }
                        },
                        onDismiss = {
                            onDeleted(expense)

                        }
                    ) {
                        ExpenseItem(
                        expense = expense,
                        currencyManager = currencyManager,
                        onEdit = { onEdit(expense) },
                        onClick = { onClick(expense) }
                    )
                    }
                    modifier.animateItem()
                }
            }
        }
    }
}
@Composable
fun ExpenseTrackerFAB(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    FloatingActionButton(
        onClick = onClick,
        modifier = modifier
    ) {
        androidx.compose.material3.Icon(
            painter = painterResource(R.drawable.outline_note_add_24) ,
            contentDescription = stringResource(R.string.add_expense),
        )
    }
}



@Composable
fun ExpenseItem(
    expense: Expense,
    currencyManager: CurrencyManager,
    modifier: Modifier = Modifier,
    onEdit: () -> Unit = {},
    onClick: () -> Unit,
) {
    Card(
        modifier = modifier.padding(all = 8.dp)
            .fillMaxWidth()
            .clickable { onClick() },
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiary
        )
    ) {
        Row(
            modifier = Modifier.padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.Top

        ) {
            if (!expense.receiptImagePath.isNullOrBlank()) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(expense.receiptImagePath)
                        .crossfade(true)
                        .build(),
                    contentDescription = "Receipt Image",
                    modifier = Modifier
                        .size(60.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .border(1.dp, Color.Gray, RoundedCornerShape(8.dp))
                        .testTag("ReceiptThumbnail"),
                    contentScale = ContentScale.Crop,
                    placeholder = painterResource(R.drawable.outline_autorenew_24),
                )
                Spacer(Modifier.width(12.dp))
            }
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "${expense.name} - ${currencyManager.format(expense.price)}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    modifier = Modifier
                )

                Text(
                    text = expense.note.ifBlank { "No note" },
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                )

                Text(expense.category, fontSize = 12.sp, color = Color.Gray)

                Row(
                    modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = expense.date.toString(),
                        fontSize = 12.sp,
                        color = Color.Gray,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(bottom = 0.dp)
                    )

                    Text(
                        text = "swipe delete",
                        fontSize = 12.sp,
                        color = Color.Gray,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(bottom = 0.dp)
                    )

                    IconButton(
                        onClick = onEdit,
                    ) {
                            androidx.compose.material3.Icon(
                                painter = painterResource(id = R.drawable.outline_edit_24),
                                contentDescription = stringResource(id = R.string.edit),
                                tint = colorScheme.inverseOnSurface,
                            )
                        }
                    }

                }
            }
        }
    }



@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseTopBar(
    onSettingsClick: () -> Unit,
    windowSizeClass: WindowSizeClass,
    currencyManager: CurrencyManager,
    modifier: Modifier = Modifier
) {

    val isWide = windowSizeClass.widthSizeClass != WindowWidthSizeClass.Compact
    TopAppBar(
        title = {
            Text(text = stringResource(id = R.string.app_name), fontWeight = FontWeight.Bold)
        },
        actions = {
            IconButton(onClick = onSettingsClick) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = stringResource(id = R.string.settings))
            }
        },
        modifier = modifier.padding(bottom = 4.dp).height(if (isWide) { 60.dp } else { 80.dp })
    )
}


@Preview
@Composable
fun ExpenseEntryFormPreview() {

}
