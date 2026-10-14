package com.example.expensetracker.ui.ui

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.expensetracker.R
import com.example.expensetracker.data.ContentType
import com.example.expensetracker.data.CurrencyManager
import com.example.expensetracker.data.MonthlyReport

@Composable
fun MonthlyReportScreen(viewModel: ExpenseViewModel, context: Context, modifier: Modifier) {
    val uri by viewModel.pdfUri.collectAsState()
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { uri ->
        // user picked location
    }

    Button(onClick = { viewModel.exportMonthlyReport(7, 2026) }) {
        Text("Export PDF")
    }

    // Auto open share sheet when pdf is ready
    LaunchedEffect(uri) {
        uri?.let {
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, it)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Share Report"))
        }
    }
}


@OptIn(ExperimentalMaterial3AdaptiveApi::class, ExperimentalMaterial3WindowSizeClassApi::class)
@Composable
fun MonthlyReportScreens(
    viewModel: ExpenseViewModel,
    currencyManager: CurrencyManager,
    modifier: Modifier = Modifier
) {
    val windowSizeClass = calculateWindowSizeClass(LocalActivity.current as Activity)
    val contentType = when (windowSizeClass.widthSizeClass) {
        WindowWidthSizeClass.Compact -> ContentType.SINGLE_PANE
        else -> ContentType.DUAL_PANE
    }

    val report by viewModel.report.collectAsState()
    val currencyCode by viewModel.currencyCode.collectAsState()

    if (contentType == ContentType.SINGLE_PANE) {
        // PHONE PORTRAIT: Vertical stack
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { ReportHeader(report, currencyManager, currencyCode) }
            item { SpendingChartCard(report, currencyManager, currencyCode) }
            item { CategoryBreakdown(report, currencyManager, currencyCode) }
            item { TopExpensesList(report, currencyManager, currencyCode) }
            item { ExportPdfButton { viewModel.exportMonthlyReport(7, 2026) }}
        }
    } else {
        // TABLET / LANDSCAPE / FOLDABLE: 2 Pane
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // LEFT PANE: Chart + Summary - 40%
            Column(
                modifier = Modifier.weight(0.4f),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                ReportHeader(report, currencyManager, currencyCode)
                SpendingChartCard(report, currencyManager, currencyCode)
                ExportPdfButton { viewModel.exportMonthlyReport(7, 2026) }
            }

            // RIGHT PANE: Details List - 60%
            LazyColumn(
                modifier = Modifier.weight(0.6f),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item { CategoryBreakdown(report, currencyManager, currencyCode) }
                item { TopExpensesList(report, currencyManager, currencyCode) }
            }
        }
    }
}

@Composable
fun ExportPdfButton(onClick: () -> Unit) {
    Button(onClick = onClick) {
        Text(stringResource(R.string.export_pdf))
    }
}


@Composable
fun ReportHeader(report: MonthlyReport, currencyManager: CurrencyManager, code: String) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text("Monthly Summary", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("Total Spent", style = MaterialTheme.typography.labelMedium)
                    Text(
                        currencyManager.format(report.totalSpent, code),
                        style = MaterialTheme.typography.displaySmall
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Budget", style = MaterialTheme.typography.labelMedium)
                    Text(
                        currencyManager.format(report.totalBudget, code),
                        style = MaterialTheme.typography.titleLarge
                    )
                }
            }
        }
    }
}

@Composable
fun SpendingChartCard(report: MonthlyReport, currencyManager: CurrencyManager, code: String) {
    Card(modifier = Modifier.fillMaxWidth().height(280.dp)) {
        // Use MPAndroidChart or Vico here
        Box(Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
            Text("PieChart: Spent by Category")
            // Your chart composable goes here
            SpendingPieChart(report.spentByCategory.toList(), currencyManager)
        }
    }
}

@Composable
fun CategoryBreakdown(report: MonthlyReport, currencyManager: CurrencyManager, code: String) {
    Card {
        Column(Modifier.padding(16.dp)) {
            Text("By Category", style = MaterialTheme.typography.titleMedium)
            report.spentByCategory.forEach { (category, amount) ->
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(category)
                    Text(currencyManager.format(amount, code), fontWeight = FontWeight.Bold)
                }
                HorizontalDivider()
            }
        }
    }
}

@Composable
fun TopExpensesList(report: MonthlyReport, currencyManager: CurrencyManager, code: String) {
    Card {
        Column(Modifier.padding(16.dp)) {
            Text("Top Expenses", style = MaterialTheme.typography.titleMedium)
            report.topExpenses.forEach { expense ->
                ListItem(
                    headlineContent = { Text(expense.name) },
                    supportingContent = { Text(expense.category) },
                    trailingContent = {
                        Text(
                            currencyManager.format(expense.price, code),
                            fontWeight = FontWeight.Bold
                        )
                    }
                )
            }
        }
    }
}