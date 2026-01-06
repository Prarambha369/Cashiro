package com.ritesh.cashiro.presentation.add

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ritesh.cashiro.data.database.entity.TransactionType

import androidx.compose.foundation.border

import androidx.compose.foundation.interaction.MutableInteractionSource
import com.ritesh.cashiro.ui.components.AccountCard
import com.ritesh.cashiro.ui.components.CategorySelectionSheet
import com.ritesh.cashiro.ui.theme.*
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun TransactionTabContent(viewModel: AddViewModel, onSave: () -> Unit) {
    val uiState by viewModel.transactionUiState.collectAsState()
    val categories by viewModel.categories.collectAsState()

    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var showCategoryMenu by remember { mutableStateOf(false) }
    var showSubcategoryMenu by remember { mutableStateOf(false) }
    val subcategories by viewModel.transactionSubcategories.collectAsState()

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
                modifier =
                        Modifier.fillMaxWidth()
                                .imePadding() // Handle keyboard properly
                                .verticalScroll(rememberScrollState())
                                .padding(horizontal = 16.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Amount Input
            TextField(
                    value = uiState.amount,
                    onValueChange = viewModel::updateTransactionAmount,
                    label = { Text("Amount", fontWeight = FontWeight.SemiBold) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape =
                            RoundedCornerShape(
                                    topStart = 16.dp,
                                    topEnd = 16.dp,
                                    bottomStart = 16.dp,
                                    bottomEnd = 16.dp
                            ),
                    isError = uiState.amountError != null,
                    colors =
                            TextFieldDefaults.colors(
                                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent,
                                    focusedLabelColor = MaterialTheme.colorScheme.primary,
                                    unfocusedLabelColor =
                                            MaterialTheme.colorScheme.onSurfaceVariant.copy(0.7f)
                            ),
                    supportingText = uiState.amountError?.let { { Text(it) } },
                    leadingIcon = { Icon(Icons.Default.CurrencyRupee, contentDescription = null) },
            )

            // Transaction Type Selection
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                        text = "Transaction Type *",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 8.dp)
                )
                FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TransactionType.values().forEach { type ->
                        FilterChip(
                                selected = uiState.transactionType == type,
                                onClick = { viewModel.updateTransactionType(type) },
                                label = {
                                    Text(
                                            type.name.lowercase(Locale.getDefault())
                                                    .replaceFirstChar {
                                                        it.titlecase(Locale.getDefault())
                                                    }
                                    )
                                },
                                leadingIcon =
                                        if (uiState.transactionType == type) {
                                            {
                                                Icon(
                                                        Icons.Default.Check,
                                                        contentDescription = null,
                                                        modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        } else null
                        )
                    }
                }
            }

            // Accounts Section
            val accounts by viewModel.accounts.collectAsState()
            var showAccountSheet by remember { mutableStateOf(false) }

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(1.5.dp)
            ) {

                OutlinedCard(
                        onClick = { showAccountSheet = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape =
                        RoundedCornerShape(
                            topStart = 16.dp,
                            topEnd = 16.dp,
                            bottomStart = 4.dp,
                            bottomEnd = 4.dp
                        ),
                        colors =
                        CardDefaults.outlinedCardColors(
                            containerColor =
                            MaterialTheme.colorScheme.surfaceContainerLow
                        ),
                        border = BorderStroke(0.dp, Color.Transparent)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccountBalance,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                    text = uiState.selectedAccount?.bankName ?: "Select Account",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color =
                                    if (uiState.selectedAccount != null)
                                        MaterialTheme.colorScheme.onSurface
                                    else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (uiState.selectedAccount != null) {
                                Text(
                                        text = "••${uiState.selectedAccount?.accountLast4}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                // Category Selection
                val categoryInteractionSource = remember { MutableInteractionSource() }
                TextField(
                    value = uiState.category,
                    onValueChange = {},
                    label = { Text("Category", fontWeight = FontWeight.SemiBold) },
                    readOnly = true,
                    singleLine = true,
                    modifier =
                        Modifier.fillMaxWidth()
                            .clickable(
                                interactionSource = categoryInteractionSource,
                                indication = null
                            ) {
                                showCategoryMenu = true
                            },
                    shape =
                        RoundedCornerShape(
                            topStart = 4.dp,
                            topEnd = 4.dp,
                            bottomStart = 16.dp,
                            bottomEnd = 16.dp
                        ),
                    leadingIcon = { Icon(Icons.Default.Category, contentDescription = null) },
                    trailingIcon = {
                        Icon(Icons.Default.KeyboardArrowDown, contentDescription = null)
                    },
                    isError = uiState.categoryError != null,
                    supportingText = uiState.categoryError?.let { { Text(it) } },
                    enabled = false, // Disable typing, handle click above
                    colors =
                        TextFieldDefaults.colors(
                            focusedContainerColor =
                                MaterialTheme.colorScheme.surfaceContainerLow,
                            unfocusedContainerColor =
                                MaterialTheme.colorScheme.surfaceContainerLow,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            focusedLabelColor = MaterialTheme.colorScheme.primary,
                            unfocusedLabelColor =
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                    0.7f
                                ),
                            disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                            disabledIndicatorColor = Color.Transparent,
                            disabledLabelColor = MaterialTheme.colorScheme.primary,
                            disabledTextColor = MaterialTheme.colorScheme.onSurface,
                            disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            disabledLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                )

                // Subcategory Display (Read-only, selected via sheet)
                if (uiState.subcategory != null) {
                     TextField(
                        value = uiState.subcategory ?: "None",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Subcategory") },
                        leadingIcon = {
                            Icon(
                                Icons.Default.SubdirectoryArrowRight,
                                contentDescription = null
                            )
                        },
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth(),
                        enabled = false,
                        colors =
                            TextFieldDefaults.colors(
                                focusedContainerColor =
                                    MaterialTheme.colorScheme.surfaceContainerLow,
                                unfocusedContainerColor =
                                    MaterialTheme.colorScheme.surfaceContainerLow,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                                focusedLabelColor = MaterialTheme.colorScheme.primary,
                                unfocusedLabelColor =
                                    MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                        0.7f
                                    ),
                                disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                                disabledIndicatorColor = Color.Transparent,
                                disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                disabledTextColor = MaterialTheme.colorScheme.onSurface
                            )
                    )
                }
            }

            if (showAccountSheet) {
                ModalBottomSheet(
                        onDismissRequest = { showAccountSheet = false },
                        containerColor = MaterialTheme.colorScheme.surface,
                        dragHandle = { BottomSheetDefaults.DragHandle() }
                ) {
                    Column(modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
                        Text(
                                text = "Select Account",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
                        )

                        if (accounts.isEmpty()) {
                            Box(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
                                    contentAlignment = Alignment.Center
                            ) {
                                Text(
                                        text = "No accounts found",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        } else {
                            LazyColumn(
                                    contentPadding =
                                            PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                item {
                                    // Option to deselect/None
                                    Surface(
                                            onClick = {
                                                viewModel.updateTransactionAccount(null)
                                                showAccountSheet = false
                                            },
                                            shape = RoundedCornerShape(12.dp),
                                            color =
                                                    if (uiState.selectedAccount == null)
                                                            MaterialTheme.colorScheme
                                                                    .primaryContainer
                                                    else MaterialTheme.colorScheme.surface,
                                            border =
                                                    if (uiState.selectedAccount == null) null
                                                    else
                                                            BorderStroke(
                                                                    1.dp,
                                                                    MaterialTheme.colorScheme
                                                                            .outlineVariant
                                                            ),
                                            modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                                modifier = Modifier.padding(16.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                    text = "None (Manual Entry)",
                                                    style = MaterialTheme.typography.bodyLarge,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }
                                }

                                items(accounts) { account ->
                                    val isSelected = uiState.selectedAccount?.id == account.id
                                    Surface(
                                        shape = CardDefaults.shape,
                                        border = if (isSelected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
                                        color = Color.Transparent,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        AccountCard(
                                            account = account,
                                            showMoreOptions = false,
                                            onClick = {
                                                viewModel.updateTransactionAccount(account)
                                                showAccountSheet = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Category Selection Sheet
            if (showCategoryMenu) {
                val allSubcategories by viewModel.allSubcategories.collectAsState(initial = emptyMap())
                ModalBottomSheet(
                    onDismissRequest = { showCategoryMenu = false },
                    dragHandle = { BottomSheetDefaults.DragHandle() },
                    containerColor = MaterialTheme.colorScheme.surface,
                ) {
                   CategorySelectionSheet(
                       categories = categories,
                       subcategoriesMap = allSubcategories,
                       onSelectionComplete = { category, subcategory ->
                           viewModel.updateTransactionCategory(category.name)
                           viewModel.updateTransactionSubcategory(subcategory?.name)
                           showCategoryMenu = false
                       },
                       onDismiss = { showCategoryMenu = false }
                   )
                }
            }

            // Date and Time Selection
            Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
            ) {
                // Date Button
                Box(
                        modifier =
                                Modifier.weight(1f).padding(8.dp).clickable {
                                    showDatePicker = true
                                },
                        contentAlignment = Alignment.CenterStart
                ) {
                    Row(
                            modifier = Modifier.padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Start
                    ) {
                        val themeColors = MaterialTheme.colorScheme
                        Icon(
                                imageVector = Icons.Default.CalendarToday,
                                contentDescription = "Date Picker",
                                modifier = Modifier.size(16.dp),
                                tint = themeColors.onSurface
                        )
                        Spacer(Modifier.size(8.dp))

                        val dateLabel =
                                uiState.date.format(DateTimeFormatter.ofPattern("dd MMM yyyy"))
                        Text(
                                text = dateLabel,
                                textAlign = TextAlign.Center,
                                fontSize = 16.sp,
                                color = themeColors.onSurface,
                                style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }

                // Time Button
                Box(
                        modifier =
                                Modifier.weight(1f)
                                        .padding(horizontal = 8.dp)
                                        .clickable { showTimePicker = true },
                        contentAlignment = Alignment.CenterEnd
                ) {
                    Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.End
                    ) {
                        val hour = if (uiState.date.hour % 12 == 0) 12 else uiState.date.hour % 12
                        val minute = uiState.date.minute
                        val amPm = if (uiState.date.hour < 12) "AM" else "PM"

                        Box(
                                modifier =
                                        Modifier.padding(5.dp)
                                                .background(
                                                        color =
                                                                MaterialTheme.colorScheme.primary
                                                                        .copy(0.2f),
                                                        shape = RoundedCornerShape(8.dp)
                                                )
                        ) {
                            Text(
                                    text = String.format("%02d", hour),
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    modifier = Modifier.padding(5.dp)
                            )
                        }

                        Text(
                                text = ":",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 16.sp,
                        )

                        Box(
                                modifier =
                                        Modifier.padding(5.dp)
                                                .background(
                                                        color =
                                                                MaterialTheme.colorScheme
                                                                        .surfaceVariant,
                                                        shape = RoundedCornerShape(8.dp)
                                                )
                        ) {
                            Text(
                                    text = String.format("%02d", minute),
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontSize = 16.sp,
                                    modifier = Modifier.padding(5.dp)
                            )
                        }

                        Box(modifier = Modifier.padding(5.dp)) {
                            Text(
                                    text = amPm,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 14.sp,
                            )
                        }
                    }
                }
            }
            Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(1.5.dp)
            ) {
                // Merchant Name Input
                TextField(
                        value = uiState.merchant,
                        onValueChange = viewModel::updateTransactionMerchant,
                        label = { Text("Merchant", fontWeight = FontWeight.SemiBold) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape =
                                RoundedCornerShape(
                                        topStart = 16.dp,
                                        topEnd = 16.dp,
                                        bottomStart = 4.dp,
                                        bottomEnd = 4.dp
                                ),
                        leadingIcon = { Icon(Icons.Default.Store, contentDescription = null) },
                        isError = uiState.merchantError != null,
                        colors =
                                TextFieldDefaults.colors(
                                        focusedContainerColor =
                                                MaterialTheme.colorScheme.surfaceContainerLow,
                                        unfocusedContainerColor =
                                                MaterialTheme.colorScheme.surfaceContainerLow,
                                        focusedIndicatorColor = Color.Transparent,
                                        unfocusedIndicatorColor = Color.Transparent,
                                        focusedLabelColor = MaterialTheme.colorScheme.primary,
                                        unfocusedLabelColor =
                                                MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                                        0.7f
                                                )
                                ),
                        supportingText = uiState.merchantError?.let { { Text(it) } },
                )

                // Notes/Description (Optional)
                TextField(
                        value = uiState.notes,
                        onValueChange = viewModel::updateTransactionNotes,
                        label = { Text("Notes (Optional)", fontWeight = FontWeight.SemiBold) },
                        modifier = Modifier.fillMaxWidth(),
                        shape =
                                RoundedCornerShape(
                                        topStart = 4.dp,
                                        topEnd = 4.dp,
                                        bottomStart = 16.dp,
                                        bottomEnd = 16.dp
                                ),
                        leadingIcon = {
                            Icon(Icons.Default.Description, contentDescription = null)
                        },
                        colors =
                                TextFieldDefaults.colors(
                                        focusedContainerColor =
                                                MaterialTheme.colorScheme.surfaceContainerLow,
                                        unfocusedContainerColor =
                                                MaterialTheme.colorScheme.surfaceContainerLow,
                                        focusedIndicatorColor = Color.Transparent,
                                        unfocusedIndicatorColor = Color.Transparent,
                                        focusedLabelColor = MaterialTheme.colorScheme.primary,
                                        unfocusedLabelColor =
                                                MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                                        0.7f
                                                )
                                ),
                )
            }

            // Bottom padding for keyboard
            Spacer(modifier = Modifier.height(80.dp))
        }
        Box(
                modifier =
                        Modifier.fillMaxWidth()
                                .align(Alignment.BottomCenter)
                                .background(
                                        brush =
                                                Brush.verticalGradient(
                                                        colors =
                                                                listOf(
                                                                        Color.Transparent,
                                                                        MaterialTheme.colorScheme
                                                                                .surface,
                                                                        MaterialTheme.colorScheme
                                                                                .surface
                                                                )
                                                )
                                ),
                contentAlignment = Alignment.BottomCenter
        ) {
            Button(
                    onClick = { viewModel.saveTransaction(onSuccess = onSave) },
                    modifier =
                            Modifier.navigationBarsPadding()
                                    .padding(horizontal = Dimensions.Padding.content)
                                    .fillMaxWidth()
                                    .align(Alignment.BottomCenter)
                                    .height(56.dp),
                    shapes = ButtonDefaults.shapes(),
                    enabled = uiState.isValid && !uiState.isLoading,
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Default.Done, contentDescription = null)
                    Spacer(Modifier.width(Spacing.sm))
                    Text("Save", style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }

    // Date Picker Dialog
    if (showDatePicker) {
        val datePickerState =
                rememberDatePickerState(
                        initialSelectedDateMillis =
                                uiState.date
                                        .toLocalDate()
                                        .atStartOfDay()
                                        .toInstant(java.time.ZoneOffset.UTC)
                                        .toEpochMilli()
                )

        DatePickerDialog(
                onDismissRequest = { showDatePicker = false },
                confirmButton = {
                    TextButton(
                            onClick = {
                                datePickerState.selectedDateMillis?.let { millis ->
                                    viewModel.updateTransactionDate(millis)
                                }
                                showDatePicker = false
                            }
                    ) { Text("OK") }
                },
                dismissButton = {
                    TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
                }
        ) { DatePicker(state = datePickerState) }
    }

    // Time Picker Dialog
    if (showTimePicker) {
        val timePickerState =
                rememberTimePickerState(
                        initialHour = uiState.date.hour,
                        initialMinute = uiState.date.minute
                )

        AlertDialog(
                onDismissRequest = { showTimePicker = false },
                title = { Text("Select Time") },
                text = { TimePicker(state = timePickerState) },
                confirmButton = {
                    TextButton(
                            onClick = {
                                viewModel.updateTransactionTime(
                                        timePickerState.hour,
                                        timePickerState.minute
                                )
                                showTimePicker = false
                            }
                    ) { Text("OK") }
                },
                dismissButton = {
                    TextButton(onClick = { showTimePicker = false }) { Text("Cancel") }
                }
        )
    }
}
