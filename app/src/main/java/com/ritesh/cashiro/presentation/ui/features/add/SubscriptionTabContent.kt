package com.ritesh.cashiro.presentation.ui.features.add

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.EventRepeat
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.SubdirectoryArrowRight
import androidx.compose.material.icons.filled.Subscriptions
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ritesh.cashiro.presentation.effects.overScrollVertical
import com.ritesh.cashiro.presentation.effects.rememberOverscrollFlingBehavior
import com.ritesh.cashiro.presentation.ui.components.AccountSelectionSheet
import com.ritesh.cashiro.presentation.ui.components.AttachmentSection
import com.ritesh.cashiro.presentation.ui.components.BrandIcon
import com.ritesh.cashiro.presentation.ui.components.CategorySelectionSheet
import com.ritesh.cashiro.presentation.ui.components.DatePicker
import com.ritesh.cashiro.presentation.ui.components.LoadingCircle
import com.ritesh.cashiro.presentation.ui.features.accounts.NumberPad
import com.ritesh.cashiro.presentation.ui.theme.Dimensions
import com.ritesh.cashiro.presentation.ui.theme.Spacing
import com.ritesh.cashiro.utils.CurrencyFormatter
import dev.chrisbanes.haze.HazeState
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SubscriptionTabContent(
    viewModel: AddViewModel,
    onSave: () -> Unit,
    isTransitioning: Boolean = false,
    blurEffects: Boolean,
    hazeState: HazeState
) {
    val uiState by viewModel.subscriptionUiState.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val accounts by viewModel.accounts.collectAsState()

    var showDatePicker by remember { mutableStateOf(false) }
    var showCategoryMenu by remember { mutableStateOf(false) }
    var showBillingCycleMenu by remember { mutableStateOf(false) }
    var showAccountSheet by remember { mutableStateOf(false) }
    var showNumberPad by remember { mutableStateOf(false) }
    val allSubcategories by viewModel.allSubcategories.collectAsState(initial = emptyMap())

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val subscriptionAttachments by viewModel.subscriptionAttachments.collectAsState()

    val selectedCategoryObj = remember(uiState.category, categories) {
        categories.find { it.name == uiState.category }
    }
    val selectedSubcategoryObj = remember(uiState.subcategory, allSubcategories) { 
        null // Placeholder,
    }
    val subcategories by viewModel.subscriptionSubcategories.collectAsState()

    val selectedSubcategoryObj2 = remember(uiState.subcategory, subcategories) {
        subcategories.find { it.name == uiState.subcategory }
    }

    val billingCycles = listOf("Monthly", "Quarterly", "Semi-Annual", "Annual", "Weekly")
    val scrollState = rememberScrollState()
    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .overScrollVertical()
                .imePadding() // Handle keyboard properly
                .verticalScroll(
                    state = scrollState,
                    flingBehavior = rememberOverscrollFlingBehavior { scrollState },
                    enabled = !isTransitioning
                )
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Error Card
            uiState.error?.let { errorMessage ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.Error,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = errorMessage,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }

            // Info Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text =
                            "Track recurring expenses. You'll need to add transactions manually each month.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            // Amount Input
            AmountInput(
                amount = uiState.amount.ifEmpty { "0" },
                currencySymbol = CurrencyFormatter.getCurrencySymbol(
                    uiState.selectedAccount?.currency ?: "INR"
                ),
                onClick = {
                    showNumberPad = true
                },
                modifier = Modifier.fillMaxWidth()
            )

            // Billing Cycle Dropdown and Date Selection
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {

                // Billing Cycle Dropdown
                ExposedDropdownMenuBox(
                    expanded = showBillingCycleMenu,
                    onExpandedChange = { showBillingCycleMenu = it },
                    modifier = Modifier.weight(1f)
                ) {
                    TextField(
                        value = uiState.billingCycle,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Billing Cycle") },
                        leadingIcon = {
                            Icon(
                                Icons.Default.EventRepeat,
                                contentDescription = null
                            )
                        },
                        shape = RoundedCornerShape(Spacing.md),
                        modifier =
                            Modifier.weight(1f).menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
                        isError = uiState.billingCycleError != null,
                        supportingText = uiState.billingCycleError?.let { { Text(it) } },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            focusedLabelColor = MaterialTheme.colorScheme.primary,
                            unfocusedLabelColor =
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(0.7f)
                        ),
                    )

                    ExposedDropdownMenu(
                        expanded = showBillingCycleMenu,
                        onDismissRequest = { showBillingCycleMenu = false }
                    ) {
                        billingCycles.forEach { cycle ->
                            DropdownMenuItem(
                                text = { Text(cycle) },
                                onClick = {
                                    viewModel.updateSubscriptionBillingCycle(cycle)
                                    showBillingCycleMenu = false
                                }
                            )
                        }
                    }
                }
                // Date Button
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(
                            color = MaterialTheme.colorScheme.surfaceContainerLow,
                            shape = RoundedCornerShape(Dimensions.Radius.md)
                        )
                        .padding(8.dp)
                        .clickable(
                            onClick = { showDatePicker = true },
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
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
                            uiState.nextPaymentDate.format(DateTimeFormatter.ofPattern("dd MMM yyyy"))
                        Text(
                            text = dateLabel,
                            textAlign = TextAlign.Center,
                            fontSize = 16.sp,
                            color = themeColors.onSurface,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }

            // Accounts Section
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(1.5.dp)
            ) {
                OutlinedCard(
                    onClick = { showAccountSheet = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = 4.dp,
                        bottomEnd = 4.dp
                    ),
                    colors = CardDefaults.outlinedCardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                    ),
                    border = BorderStroke(0.dp, Color.Transparent)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        BrandIcon(
                            merchantName = uiState.selectedAccount?.bankName ?: "",
                            accountIconResId = uiState.selectedAccount?.iconResId ?: 0,
                            size = 24.dp,
                            showBackground = false
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
                    leadingIcon = {
                        if (selectedCategoryObj != null && selectedCategoryObj.iconResId != 0) {
                            Icon(
                                painter = painterResource(id = selectedCategoryObj.iconResId),
                                contentDescription = null,
                                tint = Color.Unspecified,
                                modifier = Modifier.size(24.dp)
                            )
                        } else {
                            Icon(Icons.Default.Category, contentDescription = null)
                        }
                    },
                    trailingIcon = {
                        Icon(Icons.Default.KeyboardArrowDown, contentDescription = null)
                    },
                    isError = uiState.categoryError != null,
                    supportingText = uiState.categoryError?.let { { Text(it) } },
                    enabled = false,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedLabelColor = MaterialTheme.colorScheme.primary,
                        unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.7f),
                        disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                        disabledIndicatorColor = Color.Transparent,
                        disabledLabelColor = MaterialTheme.colorScheme.primary,
                        disabledTextColor = MaterialTheme.colorScheme.onSurface,
                        disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        disabledLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )

                // Subcategory Display
                if (uiState.subcategory != null) {
                    Spacer(modifier = Modifier.height(Spacing.md))
                    TextField(
                        value = uiState.subcategory ?: "None",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Subcategory") },
                        leadingIcon = {
                            if (selectedSubcategoryObj2 != null && selectedSubcategoryObj2.iconResId != 0) {
                                Icon(
                                    painter = painterResource(id = selectedSubcategoryObj2.iconResId),
                                    contentDescription = null,
                                    tint = Color.Unspecified,
                                    modifier = Modifier.size(24.dp)
                                )
                            } else {
                                Icon(
                                    Icons.Default.SubdirectoryArrowRight,
                                    contentDescription = null
                                )
                            }
                        },
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth(),
                        enabled = false,
                        colors = if (selectedSubcategoryObj2 != null) {
                            val color = try {
                                Color(android.graphics.Color.parseColor(selectedSubcategoryObj2.color))
                            } catch (e: Exception) {
                                MaterialTheme.colorScheme.surfaceContainerLow
                            }
                            TextFieldDefaults.colors(
                                focusedContainerColor = color.copy(alpha = 0.2f),
                                unfocusedContainerColor = color.copy(alpha = 0.2f),
                                disabledContainerColor = color.copy(alpha = 0.2f),
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                                disabledIndicatorColor = Color.Transparent,
                                focusedLabelColor = MaterialTheme.colorScheme.primary,
                                unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                    0.7f
                                ),
                                disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                disabledTextColor = MaterialTheme.colorScheme.onSurface
                            )
                        } else {
                            TextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                                focusedLabelColor = MaterialTheme.colorScheme.primary,
                                unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                    0.7f
                                ),
                                disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                                disabledIndicatorColor = Color.Transparent,
                                disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                disabledTextColor = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    )
                }
            }

            if (showAccountSheet) {
                ModalBottomSheet(
                    onDismissRequest = { showAccountSheet = false },
                    containerColor = MaterialTheme.colorScheme.surface,
                    dragHandle = { BottomSheetDefaults.DragHandle() }
                ) {
                    AccountSelectionSheet(
                        accounts = accounts,
                        selectedAccount = uiState.selectedAccount,
                        onAccountSelected = { account ->
                            viewModel.updateSubscriptionAccount(account)
                            showAccountSheet = false
                        },
                        isTransitioning = isTransitioning
                    )
                }
            }

            // NumberPad for Amount Input
            if (showNumberPad) {
                ModalBottomSheet(
                    onDismissRequest = { showNumberPad = false },
                    sheetState = sheetState,
                    containerColor = MaterialTheme.colorScheme.surface,
                    dragHandle = { BottomSheetDefaults.DragHandle() }
                ) {
                    NumberPad(
                        initialValue = uiState.amount.ifEmpty { "0" },
                        onDone = { newAmount ->
                            viewModel.updateSubscriptionAmount(newAmount)
                            showNumberPad = false
                        },
                        title = "Enter Amount"
                    )
                }
            }

            // Category Selection Sheet
            if (showCategoryMenu) {
                ModalBottomSheet(
                    onDismissRequest = { showCategoryMenu = false },
                    dragHandle = { BottomSheetDefaults.DragHandle() },
                    containerColor = MaterialTheme.colorScheme.surface,
                ) {
                    CategorySelectionSheet(
                        categories = categories,
                        subcategoriesMap = allSubcategories,
                        onSelectionComplete = { category, subcategory ->
                            viewModel.updateSubscriptionCategory(category.name)
                            viewModel.updateSubscriptionSubcategory(subcategory?.name)
                            showCategoryMenu = false
                        },
                        onDismiss = { showCategoryMenu = false }
                    )
                }
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(1.5.dp)
            ) {
                // Service Name Input
                TextField(
                    value = uiState.serviceName,
                    onValueChange = viewModel::updateSubscriptionService,
                    label = { Text("Service Name *") },
                    placeholder = { Text("e.g., Netflix, Spotify") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape =
                        RoundedCornerShape(
                            topStart = 16.dp,
                            topEnd = 16.dp,
                            bottomStart = 4.dp,
                            bottomEnd = 4.dp
                        ),
                    leadingIcon = { Icon(Icons.Default.Subscriptions, contentDescription = null) },
                    isError = uiState.serviceError != null,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedLabelColor = MaterialTheme.colorScheme.primary,
                        unfocusedLabelColor =
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(0.7f)
                    ),
                    supportingText = uiState.serviceError?.let { { Text(it) } },
                )

                // Notes/Description (Optional)
                TextField(
                    value = uiState.notes,
                    onValueChange = viewModel::updateSubscriptionNotes,
                    label = { Text("Notes (Optional)") },
                    leadingIcon = { Icon(Icons.Default.Description, contentDescription = null) },
                    placeholder = { Text("Add any additional information...") },
                    modifier = Modifier.fillMaxWidth(),
                    shape =
                        RoundedCornerShape(
                            topStart = 4.dp,
                            topEnd = 4.dp,
                            bottomStart = 16.dp,
                            bottomEnd = 16.dp
                        ),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedLabelColor = MaterialTheme.colorScheme.primary,
                        unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.7f)
                    ),
                )
            }

            // Attachments Section
            AttachmentSection(
                attachments = subscriptionAttachments,
                attachmentService = viewModel.attachmentService,
                onAddAttachment = viewModel::addSubscriptionAttachment,
                onRemoveAttachment = viewModel::removeSubscriptionAttachment,
                onAttachmentClick = { /* Preview handled internally */ },
                isEditable = true
            )

            // Bottom padding for keyboard
            Spacer(modifier = Modifier.height(80.dp))
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            MaterialTheme.colorScheme.surface,
                            MaterialTheme.colorScheme.surface
                        )
                    )
                ),
            contentAlignment = Alignment.BottomCenter
        ) {
            Button(
                onClick = { viewModel.saveSubscription(onSuccess = onSave) },
                modifier = Modifier
                    .navigationBarsPadding()
                    .padding(horizontal = Dimensions.Padding.content)
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .height(56.dp),
                shapes = ButtonDefaults.shapes(),
                enabled = uiState.isValid && !uiState.isLoading,
            ) {
                if (uiState.isLoading) {
                    LoadingCircle(modifier = Modifier.size(16.dp))
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
                    uiState.nextPaymentDate
                        .atStartOfDay()
                        .toInstant(ZoneOffset.UTC)
                        .toEpochMilli()
            )

        DatePicker(
            onDismiss = { showDatePicker = false },
            onConfirm = {
                datePickerState.selectedDateMillis?.let { millis ->
                    viewModel.updateSubscriptionNextPaymentDate(millis)
                }
                showDatePicker = false
            },
            datePickerState = datePickerState,
            blurEffects = blurEffects,
            hazeState = hazeState
        )
    }
}
