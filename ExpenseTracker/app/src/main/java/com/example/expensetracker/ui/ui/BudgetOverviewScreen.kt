package com.example.expensetracker.ui.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.expensetracker.R
import com.example.expensetracker.data.AppContainer
import com.example.expensetracker.data.BudgetSummary
import com.example.expensetracker.data.CurrencyManager
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun BudgetOverviewScreen(modifier: Modifier = Modifier, viewModel: ExpenseViewModel = viewModel(
    factory = ExpenseViewModelFactory(
        AppContainer(context = LocalContext.current).expenseRepository,
        AppContainer(context = LocalContext.current).categoryBudgetRepository,
        AppContainer(context = LocalContext.current).currencyManager,
        LocalContext.current
    )
)) {
    val budgets by viewModel.categoryBudgets.collectAsState()
    val spentMap by viewModel.spentPerCategory.collectAsState()
    val categories = listOf("Food", "Transport", "Bills", "Entertainment", "Health", "Other")

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("Monthly Budgets", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        }

        items(categories) { category ->
            BudgetCard(
                category = category,
                budget = budgets.find { it.category == category }?.monthlyBudget?: 0.0,
                spent = spentMap[category]?: 0.0,
                currencyManager = viewModel.currencyManager,
                onBudgetSet = { amount -> viewModel.setBudget(category, amount) }
            )
        }
    }
}

@Composable
fun BudgetCard(
    category: String,
    budget: Double,
    spent: Double,
    currencyManager: CurrencyManager,
    onBudgetSet: (Double) -> Unit,
    modifier: Modifier = Modifier
) {
    var showDialog by remember { mutableStateOf(false) }
    var budgetInput by remember { mutableStateOf(budget.toString()) }

    val progress = if (budget > 0) (spent / budget).coerceIn(0.0, 1.0).toFloat() else 0f
    val progressColor = when {
        progress < 0.6f-> Color(0xFF4CAF50) // green
        progress < 0.9f-> Color(0xFFFFC107) // amber
        else -> Color(0xFFF44336) // red
    }
    val today = LocalDate.now()
    val monthYearText = today.format(DateTimeFormatter.ofPattern("MMMM yyyy"))


    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = { showDialog = true }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(category, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(
                    currencyManager.format(spent),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 14.sp
                )
            }
            Spacer(modifier = Modifier.height(4.dp))

            Text(
                "Budget: ${currencyManager.format(budget)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(modifier = Modifier.height(8.dp))

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp))
                    .testTag("BudgetProgressBar"),
                color = progressColor,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                strokeCap = StrokeCap.Round,
            )

            Spacer(modifier = Modifier.height(6.dp))


            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (budget > 0 && spent > budget) {
                    Text(
                        "Over budget by R${"%.2f".format(spent - budget)}",
                        color = Color.Red,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                } else {
                    Text(
                        "${(progress * 100).toInt()}% used",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    monthYearText,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Set budget for $category") },
            text = {
                OutlinedTextField(
                    value = budgetInput,
                    onValueChange = { budgetInput = it },
                    label = { Text(stringResource(R.string.monthly_budget)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    budgetInput.toDoubleOrNull()?.let { onBudgetSet(it) }
                    showDialog = false
                }) { Text(stringResource(R.string.save)) }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }
}


@Composable
fun BudgetSummaryCard(
    summary: BudgetSummary,
    currencyManager: CurrencyManager,
    onCopyLastMonth: () -> Unit,
    hasLastMonthBudgets: Boolean,
    hasCurrentMonthBudgets: Boolean,
) {
    val progress = if (summary.totalBudget > 0) {
        (summary.totalSpent / summary.totalBudget).coerceIn(0.0, 1.0).toFloat()
    }
    else 0f

        val progressColor = when {
            progress < 0.6f -> Color(0xFF4CAF50)
            progress < 0.9f -> Color(0xFFFFC107)
            else -> Color(0xFFF44336)
        }
    var showConfirmationDialog by remember { mutableStateOf(false) }


    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Total Budget", style = MaterialTheme.typography.labelMedium)
                    Text(
                        currencyManager.format(summary.totalBudget),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Remaining", style = MaterialTheme.typography.labelMedium)
                    Text(
                        currencyManager.format(summary.remaining),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (summary.remaining >= 0) Color(0xFF4CAF50) else Color.Red
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(5.dp)),
            color = progressColor,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
            strokeCap = ProgressIndicatorDefaults.LinearStrokeCap,
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "Spent: ${currencyManager.format(summary.totalSpent)}",
                    style = MaterialTheme.typography.bodySmall
                )
                if (summary.overBudgetCategories > 0) {
                    Text(
                        "${summary.overBudgetCategories} categories over budget",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Red,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            if (hasLastMonthBudgets && !hasCurrentMonthBudgets) {
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedButton(
                    onClick = {showConfirmationDialog = true},
                    modifier = Modifier.fillMaxWidth()
                ) {
                   androidx.compose.material3.Icon(
                       painter = painterResource(id = R.drawable.baseline_content_copy_24),
                       contentDescription = stringResource(R.string.copy_last_month_budget),
                       modifier = Modifier.size(18.dp)
                   )
                       Spacer(modifier = Modifier.width(6.dp))
                       Text(stringResource(R.string.copy_last_month_budget))
                }
            }
        }
    }

    if (showConfirmationDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmationDialog = false },
            title = { Text("Copy Budgets?") },
            text = { Text("This will copy all budget amounts from last month to the current month.") },
            confirmButton = {
                TextButton(onClick = {
                    onCopyLastMonth()
                    showConfirmationDialog = false
                }) {
                    Text("Copy")
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmationDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}


@Preview
@Composable
fun PreviewBudgetCard() {

}