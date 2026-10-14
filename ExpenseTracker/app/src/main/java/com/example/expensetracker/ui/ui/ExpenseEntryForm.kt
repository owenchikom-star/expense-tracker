package com.example.expensetracker.ui.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.BottomSheetScaffoldState
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import com.example.expensetracker.R
import com.example.expensetracker.data.CategoryPredictor
import com.example.expensetracker.data.ReceiptParser
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel


@RequiresApi(Build.VERSION_CODES.P)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseEntryForm(
    expensesUiState: ExpenseUiState,
    enabled: Boolean,
    sheetScaffoldState: BottomSheetScaffoldState,
    onCancel: () -> Unit,
    onSave: (String, String) -> Unit,
    onValueChange: () -> Unit,
    modifier: Modifier = Modifier,
    expenseViewModel: ExpenseViewModel,
    content: @Composable () -> Unit
) {
    var showCamera by remember { mutableStateOf(false) }
    var category by remember { mutableStateOf("Other") }
    val categories = listOf("Food", "Transport", "Bills", "Entertainment", "Health", "Shopping", "Other")
    var expanded by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = expensesUiState.date
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
    )
    var showDatePicker by remember { mutableStateOf(false) }
    val keyboardController = LocalSoftwareKeyboardController.current
    val uiState by expenseViewModel.expensesUiState.collectAsState()
    val context = LocalContext.current
    val selectedCategory = expensesUiState.category
    val coroutineScope = rememberCoroutineScope()


    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            showCamera = true
        } else {
            coroutineScope.launch {
                sheetScaffoldState.snackbarHostState.showSnackbar("Camera permission denied")
            }
        }
    }

    fun openCameraWithPermissionCheck() {
        val hasCameraPermission = ContextCompat.checkSelfPermission(
            context, android.Manifest.permission.CAMERA
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        if (hasCameraPermission) {
            showCamera = true
        } else {
            cameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
        }
    }

    Box() {
        Column(
            modifier = modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = if (expensesUiState.id == 0.toLong()) stringResource(R.string.add_expense) else stringResource(
                    R.string.edit_expense
                ),
                style = MaterialTheme.typography.headlineSmall
            )

            Button(
                onClick = { openCameraWithPermissionCheck() },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.outline_add_a_photo_24),
                    contentDescription = "Camera"
                )
                Spacer(Modifier.width(8.dp))
                Text("Scan Receipt")
            }

            OutlinedTextField(
                value = expensesUiState.name,
                onValueChange = {
                    it
                    expenseViewModel.updateExpenseName(name = it)
                    expenseViewModel.updateCategory(
                        category = CategoryPredictor.predict(
                            it,
                            expensesUiState.price.toDoubleOrNull() ?: 0.0
                        )
                    )
                    onValueChange()
                },
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Next
                ),
                label = { Text(stringResource(R.string.name)) },
                placeholder = { Text(stringResource(R.string.e_g__groceries)) },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = expensesUiState.price,
                onValueChange = {
                    it
                    expenseViewModel.updateExpensePrice(price = it)
                    expenseViewModel.updateCategory(
                        category = CategoryPredictor.predict(
                            expensesUiState.name,
                            it.toDoubleOrNull() ?: 0.0
                        )
                    )
                    onValueChange()
                },
                label = { Text(stringResource(R.string.price)) },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Decimal,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        keyboardController?.hide()
                    }
                ),
                modifier = Modifier.fillMaxWidth()
            )

            // Show predicted category chip - user can override
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(categories.filter { it != "All" }) { cat ->
                    FilterChip(
                        selected = selectedCategory == cat,
                        onClick = {
                            expenseViewModel.updateCategory(category = cat)
                        }, // user override
                        label = { Text(cat) }
                    )
                }
            }


            // Date Picker
            OutlinedTextField(
                value = expensesUiState.date.toString(),
                onValueChange = {},
                readOnly = true,
                label = { Text("Date") },
                trailingIcon = {
                    IconButton(onClick = { showDatePicker = true }) {
                        Icon(
                            painter = painterResource(id = R.drawable.baseline_date_range_24),
                            contentDescription = "Select date"
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )

            if (showDatePicker) {
                DatePickerDialog(
                    onDismissRequest = { showDatePicker = false },
                    confirmButton = {
                        TextButton(onClick = {
                            datePickerState.selectedDateMillis?.let { millis ->
                                val selectedDate = Instant.ofEpochMilli(millis)
                                    .atZone(ZoneId.systemDefault())
                                    .toLocalDate()
                                expenseViewModel.updateDate(date = selectedDate)
                            }
                            showDatePicker = false
                        }) {
                            Text(stringResource(R.string.ok))
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDatePicker = false }) {
                            Text(stringResource(R.string.cancel))
                        }
                    }
                ) {
                    DatePicker(state = datePickerState)
                }
            }

            OutlinedTextField(
                value = expensesUiState.note,
                onValueChange = { expenseViewModel.updateNote(note = it) },
                label = { Text(stringResource(R.string.note)) },
                keyboardActions = KeyboardActions(
                    onDone = {
                        keyboardController?.hide()
                        onSave("", "")
                    }
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Row(Modifier.fillMaxSize()) {
                Button(
                    onClick = onCancel,
                    modifier = Modifier.fillMaxWidth().weight(1f),
                ) {
                    Text(stringResource(R.string.cancel))
                }
                Button(
                    onClick = { onSave("", "") },
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    enabled = expensesUiState.name.isNotBlank() && expensesUiState.price.isNotBlank(),
                ) {
                    Text(stringResource(R.string.save))
                }
            }

        }
    }

    if (showCamera) {
        Dialog(onDismissRequest = { showCamera = false }) {
            Box(Modifier.fillMaxSize()) {
                ReceiptCameraScreen { text, imageUri ->
                    Log.d("RECEIPT", "Got path from camera: $imageUri")
                        showCamera = false
                        val parsed = ReceiptParser.parse(text)
                        expenseViewModel.fillFromReceipt(parsed, imageUri)
                        onValueChange()

                }
                Button(
                    onClick = { showCamera = false },
                    modifier = Modifier.align(Alignment.TopEnd).padding(16.dp)
                ) { Text(stringResource(R.string.x)) }
            }
        }
    }
}