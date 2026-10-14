package com.example.expensetracker.ui.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ExpenseNavigationRail(
    currentTab: Int,
    onTabSelected: (Int) -> Unit,
    onAddClick: () -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    NavigationRail(
        modifier = modifier.fillMaxHeight(),
        header = {
            FloatingActionButton(
                onClick = onAddClick,
                elevation = FloatingActionButtonDefaults.bottomAppBarFabElevation()
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Expense")
            }
        },
        content = {
            Column(modifier = Modifier.padding(top = 16.dp)) {
                NavigationRailItem(
                    selected = currentTab == 0,
                    onClick = { onTabSelected(0) },
                    icon = { Icon(Icons.Default.PieChart, contentDescription = "Overview") },
                    label = { Text("Overview") }
                )
                NavigationRailItem(
                    selected = currentTab == 1,
                    onClick = { onTabSelected(1) },
                    icon = { Icon(Icons.Default.History, contentDescription = "Transactions") },
                    label = { Text("History") }
                )
            }
            Spacer(Modifier.weight(1f))
            NavigationRailItem(
                selected = false,
                onClick = onSettingsClick,
                icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                label = { Text("Settings") }
            )
        }
    )
}

