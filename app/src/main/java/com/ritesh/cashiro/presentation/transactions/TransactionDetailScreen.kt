package com.ritesh.cashiro.presentation.transactions

import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.ArrowBackIosNew
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.painterResource
import androidx.core.graphics.toColorInt
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ritesh.cashiro.data.database.entity.TransactionEntity
import com.ritesh.cashiro.data.database.entity.TransactionType
import com.ritesh.cashiro.presentation.accounts.NumberPad
import com.ritesh.cashiro.presentation.add.AmountInput
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontStyle
import com.ritesh.cashiro.ui.components.*
import com.ritesh.cashiro.ui.effects.BlurredAnimatedVisibility
import com.ritesh.cashiro.ui.effects.overScrollVertical
import com.ritesh.cashiro.ui.theme.Dimensions
import com.ritesh.cashiro.ui.theme.Spacing
import com.ritesh.cashiro.utils.CurrencyFormatter
import com.ritesh.cashiro.utils.formatAmount
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionDetailScreen(
    transactionId: Long,
    onNavigateBack: () -> Unit,
    viewModel: TransactionDetailViewModel = hiltViewModel()
) {
    val transaction by viewModel.transaction.collectAsStateWithLifecycle()
    val isEditMode by viewModel.isEditMode.collectAsStateWithLifecycle()
    val editableTransaction by viewModel.editableTransaction.collectAsStateWithLifecycle()
    val isSaving by viewModel.isSaving.collectAsStateWithLifecycle()
    val saveSuccess by viewModel.saveSuccess.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()
    val applyToAllFromMerchant by viewModel.applyToAllFromMerchant.collectAsStateWithLifecycle()
    val updateExistingTransactions by viewModel.updateExistingTransactions.collectAsStateWithLifecycle()
    val existingTransactionCount by viewModel.existingTransactionCount.collectAsStateWithLifecycle()
    val showDeleteDialog by viewModel.showDeleteDialog.collectAsStateWithLifecycle()
    val isDeleting by viewModel.isDeleting.collectAsStateWithLifecycle()
    val deleteSuccess by viewModel.deleteSuccess.collectAsStateWithLifecycle()
    val accountPrimaryCurrency by viewModel.primaryCurrency.collectAsStateWithLifecycle()
    val convertedAmount by viewModel.convertedAmount.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val scrollBehaviorSmall = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
    val scrollBehaviorLarge = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
    val hazeState = remember { HazeState() }
    var showNumberPad by remember { mutableStateOf(false) }
    var showCategoryMenu by remember { mutableStateOf(false) }
    var showAccountSheet by remember { mutableStateOf(false) }
    var showTargetAccountSheet by remember { mutableStateOf(false) }
    var showBillingCycleMenu by remember { mutableStateOf(false) }

    // Show success snackbar
    LaunchedEffect(saveSuccess) {
        if (saveSuccess) {
            scope.launch {
                snackbarHostState.showSnackbar("Transaction updated successfully")
                viewModel.clearSaveSuccess()
            }
        }
    }

    // Show error snackbar
    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            scope.launch {
                snackbarHostState.showSnackbar(it)
            }
        }
    }

    LaunchedEffect(transactionId) {
        viewModel.loadTransaction(transactionId)
    }

    // Handle delete success
    LaunchedEffect(deleteSuccess) {
        if (deleteSuccess) {
            onNavigateBack()
        }
    }

    val context = LocalContext.current

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehaviorLarge.nestedScrollConnection),
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        floatingActionButton = {
            // Show FABs only when not in edit mode and transaction exists
            if (!isEditMode && transaction != null) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Delete FAB
                    SmallFloatingActionButton(
                        onClick = { viewModel.showDeleteDialog() },
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete Transaction"
                        )
                    }

                    // Report Issue FAB
                    FloatingActionButton(
                        onClick = {
                            val reportUrl = viewModel.getReportUrl()
                            Log.d("TransactionDetail", "Report FAB clicked, opening URL: ${reportUrl.take(200)}...")
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(reportUrl))
                            try {
                                context.startActivity(intent)
                                Log.d("TransactionDetail", "Successfully launched browser intent")
                            } catch (e: Exception) {
                                Log.e("TransactionDetail", "Error launching browser", e)
                            }
                        },
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    ) {
                        Icon(
                            imageVector = Icons.Default.BugReport,
                            contentDescription = "Report Issue"
                        )
                    }
                }
            }
        },
        topBar = {
            CustomTitleTopAppBar(
                scrollBehaviorSmall = scrollBehaviorSmall,
                scrollBehaviorLarge = scrollBehaviorLarge,
                title = if (isEditMode) "Edit Transaction" else "Transaction Details",
                onBackClick = {
                    if (isEditMode) {
                        viewModel.cancelEdit()
                    } else {
                        onNavigateBack()
                    }
                },
                hasBackButton = true,
                hazeState = hazeState,
                navigationContent = {
                    TransactionNavigationContent(
                        isEditMode = isEditMode,
                        onBackClick = {
                            if (isEditMode) {
                                viewModel.cancelEdit()
                            } else {
                                onNavigateBack()
                            }
                        }
                    )
                },
                actionContent = {
                    TransactionActionContent(
                        isEditMode = isEditMode,
                        isSaving = isSaving,
                        onEditClick = { viewModel.enterEditMode() },
                        onSaveClick = { viewModel.saveChanges() }
                    )
                }
            )
        }
    )
 { paddingValues ->
        val displayTransaction = if (isEditMode) editableTransaction else transaction
        displayTransaction?.let { txn ->
            TransactionDetailContent(
                transaction = txn,
                isEditMode = isEditMode,
                applyToAllFromMerchant = applyToAllFromMerchant,
                updateExistingTransactions = updateExistingTransactions,
                existingTransactionCount = existingTransactionCount,
                viewModel = viewModel,
                accountPrimaryCurrency = accountPrimaryCurrency,
                convertedAmount = convertedAmount,
                hazeState = hazeState,
                onAmountClick = { showNumberPad = true },
                onCategoryClick = { showCategoryMenu = true },
                onAccountClick = { showAccountSheet = true },
                onTargetAccountClick = { showTargetAccountSheet = true },
                showBillingCycleMenu = showBillingCycleMenu,
                onBillingCycleMenuChange = { showBillingCycleMenu = it },
                paddingValues = paddingValues
            )
        }
    }

    // NumberPad for Amount Input
    if (showNumberPad && isEditMode) {
        ModalBottomSheet(
            onDismissRequest = { showNumberPad = false },
            containerColor = MaterialTheme.colorScheme.surface,
            dragHandle = { BottomSheetDefaults.DragHandle() }
        ) {
            val amount = editableTransaction?.amount?.stripTrailingZeros()?.toPlainString() ?: "0"
            NumberPad(
                initialValue = amount,
                onDone = { newAmount ->
                    viewModel.updateAmount(newAmount)
                    showNumberPad = false
                },
                title = "Enter Amount"
            )
        }
    }

    // Category Selection Sheet
    if (showCategoryMenu) {
        val allSubcategories by viewModel.allSubcategories.collectAsStateWithLifecycle()
        val categories by viewModel.categories.collectAsStateWithLifecycle()
        ModalBottomSheet(
            onDismissRequest = { showCategoryMenu = false },
            dragHandle = { BottomSheetDefaults.DragHandle() },
            containerColor = MaterialTheme.colorScheme.surface,
        ) {
            CategorySelectionSheet(
                categories = categories,
                subcategoriesMap = allSubcategories,
                onSelectionComplete = { category, subcategory ->
                    viewModel.updateCategory(category.name)
                    viewModel.updateSubcategory(subcategory?.name)
                    showCategoryMenu = false
                },
                onDismiss = { showCategoryMenu = false }
            )
        }
    }

    // Account Selection Sheets
    if (showAccountSheet) {
        val accounts by viewModel.availableAccounts.collectAsStateWithLifecycle()
        val selectedAccount by viewModel.selectedAccount.collectAsStateWithLifecycle()
        ModalBottomSheet(
            onDismissRequest = { showAccountSheet = false },
            containerColor = MaterialTheme.colorScheme.surface,
            dragHandle = { BottomSheetDefaults.DragHandle() }
        ) {
            AccountSelectionSheet(
                accounts = accounts,
                selectedAccount = selectedAccount,
                onAccountSelected = {
                    viewModel.updateTransactionAccount(it)
                    showAccountSheet = false
                }
            )
        }
    }

    if (showTargetAccountSheet) {
        val accounts by viewModel.availableAccounts.collectAsStateWithLifecycle()
        val targetAccount by viewModel.targetAccount.collectAsStateWithLifecycle()
        ModalBottomSheet(
            onDismissRequest = { showTargetAccountSheet = false },
            containerColor = MaterialTheme.colorScheme.surface,
            dragHandle = { BottomSheetDefaults.DragHandle() }
        ) {
            AccountSelectionSheet(
                accounts = accounts,
                selectedAccount = targetAccount,
                title = "Select Target Account",
                onAccountSelected = {
                    viewModel.updateTransactionTargetAccount(it)
                    showTargetAccountSheet = false
                }
            )
        }
    }

    // Delete Confirmation Dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.hideDeleteDialog() },
            title = { Text("Delete Transaction") },
            text = {
                Text("Are you sure you want to delete this transaction? This action cannot be undone.")
            },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.deleteTransaction() },
                    enabled = !isDeleting
                ) {
                    if (isDeleting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            "Delete",
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.hideDeleteDialog() }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun TransactionNavigationContent(
    isEditMode: Boolean,
    onBackClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .animateContentSize()
            .padding(start = 16.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onBackClick,
            ),
    ) {
        IconButton(
            onClick = onBackClick,
            colors = IconButtonDefaults.iconButtonColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer,
                contentColor = MaterialTheme.colorScheme.onBackground
            )
        ) {
            Icon(
                imageVector = if (isEditMode) Icons.Default.Close else Icons.Rounded.ArrowBackIosNew,
                contentDescription = if (isEditMode) "Cancel" else "Back",
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun TransactionActionContent(
    isEditMode: Boolean,
    isSaving: Boolean,
    onEditClick: () -> Unit,
    onSaveClick: () -> Unit
) {
    if (isEditMode) {
        TextButton(
            onClick = onSaveClick,
            enabled = !isSaving,
            modifier = Modifier.padding(end = 16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                contentColor = MaterialTheme.colorScheme.onTertiaryContainer
            )
        ) {
            if (isSaving) {
                CircularProgressIndicator(
                    modifier = Modifier.size(Dimensions.Icon.small),
                    strokeWidth = 2.dp
                )
            } else {
                Text(
                    text = "Save",
                    modifier = Modifier.padding(horizontal = 8.dp),
                )
            }
        }
    } else {
        Box(
            modifier = Modifier
                .animateContentSize()
                .padding(end = 16.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onEditClick,
                ),
        ) {
            IconButton(
                onClick = onEditClick,
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    contentColor = MaterialTheme.colorScheme.onBackground
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Edit",
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun TransactionDetailContent(
    transaction: TransactionEntity,
    isEditMode: Boolean,
    applyToAllFromMerchant: Boolean,
    updateExistingTransactions: Boolean,
    existingTransactionCount: Int,
    viewModel: TransactionDetailViewModel,
    accountPrimaryCurrency: String,
    convertedAmount: BigDecimal?,
    hazeState: HazeState,
    onAmountClick: () -> Unit,
    onCategoryClick: () -> Unit,
    onAccountClick: () -> Unit,
    onTargetAccountClick: () -> Unit,
    showBillingCycleMenu: Boolean,
    onBillingCycleMenuChange: (Boolean) -> Unit,
    paddingValues: PaddingValues,
    modifier: Modifier = Modifier
) {

    Column(
        modifier = modifier
            .fillMaxSize()
            .imePadding()
            .hazeSource(state = hazeState)
            .overScrollVertical()
            .verticalScroll(rememberScrollState())
            .padding(
                start = Dimensions.Padding.content,
                end = Dimensions.Padding.content,
                top = Dimensions.Padding.content +
                        paddingValues.calculateTopPadding()
            ),
    ) {
        // Header with amount and merchant
        if (isEditMode) {
            EditableTransactionHeader(
                transaction = transaction,
                viewModel = viewModel,
                onAmountClick = onAmountClick
            )
        } else {
            TransactionHeader(transaction, accountPrimaryCurrency, convertedAmount)
        }

        Spacer(modifier = Modifier.height(Spacing.lg))

        // SMS Body - Always read-only
        if (!transaction.smsBody.isNullOrBlank()) {
            SmsBodyCard(transaction.smsBody)
            Spacer(modifier = Modifier.height(Spacing.md))
        }

        // Extracted Information
        if (isEditMode) {
            EditableExtractedInfoCard(
                transaction = transaction,
                applyToAllFromMerchant = applyToAllFromMerchant,
                updateExistingTransactions = updateExistingTransactions,
                existingTransactionCount = existingTransactionCount,
                onTargetAccountClick = onTargetAccountClick,
                showBillingCycleMenu = showBillingCycleMenu,
                onBillingCycleMenuChange = onBillingCycleMenuChange,
                viewModel = viewModel,
                onCategoryClick = onCategoryClick,
                onAccountClick = onAccountClick
            )
        } else {
            ExtractedInfoCard(transaction)
        }

        Spacer(modifier = Modifier.height(Spacing.md))

        // Additional Details - Always read-only
        if (transaction.balanceAfter != null || transaction.accountNumber != null ||
            transaction.fromAccount != null || transaction.toAccount != null) {
            AdditionalDetailsCard(viewModel,transaction)
        }

        // Add extra bottom padding when in edit mode to ensure description field is visible above keyboard
        if (isEditMode) {
            Spacer(modifier = Modifier.height(100.dp))
        }
    }
}

@Composable
private fun TransactionHeader(
    transaction: TransactionEntity,
    primaryCurrency: String,
    convertedAmount: BigDecimal?
) {
    CashiroCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Dimensions.Padding.content),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Brand Icon
            BrandIcon(
                merchantName = transaction.merchantName,
                size = 64.dp,
                showBackground = true
            )

            Spacer(modifier = Modifier.height(Spacing.md))

            // Merchant Name
            Text(
                text = transaction.merchantName,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            // Amount with sign
            val amountColor = when (transaction.transactionType) {
                TransactionType.INCOME -> Color(0xFF4CAF50)
                TransactionType.EXPENSE -> MaterialTheme.colorScheme.error
                TransactionType.CREDIT -> Color(0xFFFF6B35)  // Orange for credit
                TransactionType.TRANSFER -> Color(0xFF9C27B0)  // Purple for transfer
                TransactionType.INVESTMENT -> Color(0xFF00796B)  // Teal for investment
            }
            val sign = when (transaction.transactionType) {
                TransactionType.INCOME -> "+"
                TransactionType.EXPENSE -> "-"
                TransactionType.CREDIT -> "💳"
                TransactionType.TRANSFER -> "↔"
                TransactionType.INVESTMENT -> "📈"
            }

            Text(
                text = "$sign${transaction.formatAmount()}",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = amountColor
            )

            // Show converted amount if different from transaction currency
            if (transaction.currency.isNotEmpty() && !transaction.currency.equals(primaryCurrency, ignoreCase = true) && convertedAmount != null) {
                Spacer(modifier = Modifier.height(Spacing.xs))
                Text(
                    text = "≈ ${CurrencyFormatter.formatCurrency(convertedAmount, primaryCurrency)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Normal
                )
            }

            // Date and Time
            Text(
                text = transaction.dateTime.format(
                    DateTimeFormatter.ofPattern("MMMM d, yyyy • h:mm a")
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SmsBodyCard(smsBody: String) {
    CashiroCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Dimensions.Padding.content)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.Chat,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(Spacing.sm))
                Text(
                    text = "Original SMS",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(Spacing.sm))

            // SMS text in monospace font
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = smsBody,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = FontFamily.Monospace
                    ),
                    modifier = Modifier.padding(Spacing.md)
                )
            }
        }
    }
}

@Composable
private fun ExtractedInfoCard(transaction: TransactionEntity) {
    CashiroCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Dimensions.Padding.content)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Analytics,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(Spacing.sm))
                Text(
                    text = "Extracted Information",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(Spacing.md))

            // Category
            InfoRow(
                label = "Category",
                value = transaction.category,
                icon = Icons.Default.Category
            )

            // Bank
            transaction.bankName?.let {
                InfoRow(
                    label = "Bank",
                    value = it,
                    icon = Icons.Default.AccountBalance
                )
            }

            // Transaction Type
            InfoRow(
                label = "Type",
                value = transaction.transactionType.name.lowercase().replaceFirstChar { it.uppercase() },
                icon = when (transaction.transactionType) {
                    TransactionType.INCOME -> Icons.AutoMirrored.Filled.TrendingUp
                    TransactionType.EXPENSE -> Icons.AutoMirrored.Filled.TrendingDown
                    TransactionType.CREDIT -> Icons.Default.CreditCard
                    TransactionType.TRANSFER -> Icons.Default.SwapHoriz
                    TransactionType.INVESTMENT -> Icons.AutoMirrored.Filled.ShowChart
                }
            )

            // Description
            transaction.description?.let {
                InfoRow(
                    label = "Description",
                    value = it,
                    icon = Icons.Default.Description
                )
            }

            // Recurring status
            if (transaction.isRecurring) {
                InfoRow(
                    label = "Status",
                    value = "Recurring Transaction${transaction.billingCycle?.let { " ($it)" } ?: ""}",
                    icon = Icons.Default.Repeat
                )
            }
        }
    }
}

@Composable
private fun AdditionalDetailsCard(viewModel: TransactionDetailViewModel, transaction: TransactionEntity) {
    CashiroCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Dimensions.Padding.content)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(Spacing.sm))
                Text(
                    text = "Additional Details",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(Spacing.md))

            // Account Number (masked) - only show for non-transfer transactions
            transaction.accountNumber?.let {
                // Don't show generic account field when we have specific transfer accounts
                if (transaction.fromAccount == null && transaction.toAccount == null) {
                    val masked = if (it.length > 4) {
                        "*".repeat(it.length - 4) + it.takeLast(4)
                    } else it
                    InfoRow(
                        label = "Account",
                        value = masked,
                        icon = Icons.Default.CreditCard
                    )
                }
            }

            // From Account (for transfers)
            transaction.fromAccount?.let { from ->
                val masked = if (from.length > 4) {
                    "*".repeat(from.length - 4) + from.takeLast(4)
                } else from
                InfoRow(
                    label = "From Account",
                    value = masked,
                    icon = Icons.Default.ArrowUpward
                )
            }

            // To Account (for transfers)
            transaction.toAccount?.let { to ->
                val masked = if (to.length > 4) {
                    "*".repeat(to.length - 4) + to.takeLast(4)
                } else to
                InfoRow(
                    label = "To Account",
                    value = masked,
                    icon = Icons.Default.ArrowDownward
                )
            }

            // Balance After
            transaction.balanceAfter?.let {
                InfoRow(
                    label = "Balance After",
                    value = CurrencyFormatter.formatCurrency(it,viewModel.primaryCurrency.value),
                    icon = Icons.Default.AccountBalanceWallet
                )
            }
        }
    }
}

@Composable
private fun InfoRow(
    label: String,
    value: String,
    icon: ImageVector
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = Spacing.xs),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(Dimensions.Icon.small)
        )
        Spacer(modifier = Modifier.width(Spacing.sm))
        Text(
            text = "$label:",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(100.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun EditableTransactionHeader(
    transaction: TransactionEntity,
    viewModel: TransactionDetailViewModel,
    onAmountClick: () -> Unit
) {
    CashiroCard(
        modifier = Modifier.fillMaxWidth(),
        containerColor = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(Spacing.md)
        ) {
            // Amount Input
            AmountInput(
                amount = transaction.amount.stripTrailingZeros().toPlainString(),
                currencySymbol = CurrencyFormatter.getCurrencySymbol(transaction.currency),
                onClick = onAmountClick,
                modifier = Modifier.fillMaxWidth()
            )

            // Transaction Type - Using FlowRow for responsive layout
            Column(modifier = Modifier.fillMaxWidth()) {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TransactionType.values().forEach { type ->
                        FilterChip(
                            selected = transaction.transactionType == type,
                            onClick = { viewModel.updateTransactionType(type) },
                            label = {
                                Text(
                                    text = type.name.lowercase().replaceFirstChar { it.uppercase() },
                                    maxLines = 1
                                )
                            },
                            leadingIcon = if (transaction.transactionType == type) {
                                {
                                    Icon(
                                        imageVector = when (type) {
                                            TransactionType.INCOME -> Icons.AutoMirrored.Filled.TrendingUp
                                            TransactionType.EXPENSE -> Icons.AutoMirrored.Filled.TrendingDown
                                            TransactionType.CREDIT -> Icons.Default.CreditCard
                                            TransactionType.TRANSFER -> Icons.Default.SwapHoriz
                                            TransactionType.INVESTMENT -> Icons.AutoMirrored.Filled.ShowChart
                                        },
                                        contentDescription = null,
                                        modifier = Modifier.size(Dimensions.Icon.small)
                                    )
                                }
                            } else null
                        )
                    }
                }
            }

            // Date and Time
            DateTimeField(
                dateTime = transaction.dateTime,
                onDateTimeChange = { viewModel.updateDateTime(it) }
            )


            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(1.5.dp)
            ) {
                // Merchant Name
                TextField(
                    value = transaction.merchantName,
                    onValueChange = { viewModel.updateMerchantName(it) },
                    label = { Text("Merchant", fontWeight = FontWeight.SemiBold) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = 4.dp,
                        bottomEnd = 4.dp
                    ),
                    leadingIcon = {
                        BrandIcon(
                            merchantName = transaction.merchantName,
                            size = 24.dp,
                            showBackground = false
                        )
                    },
                    isError = transaction.merchantName.isBlank(),
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

                // Description
                TextField(
                    value = transaction.description ?: "",
                    onValueChange = { viewModel.updateDescription(it) },
                    label = { Text("Description (Optional)", fontWeight = FontWeight.SemiBold) },
                    shape = RoundedCornerShape(
                        topStart = 4.dp,
                        topEnd = 4.dp,
                        bottomStart = 16.dp,
                        bottomEnd = 16.dp
                    ),
                    leadingIcon = {
                        Icon(
                            Icons.Default.Description,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedLabelColor = MaterialTheme.colorScheme.primary,
                        unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.7f)
                    )
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditableExtractedInfoCard(
    transaction: TransactionEntity,
    applyToAllFromMerchant: Boolean,
    updateExistingTransactions: Boolean,
    existingTransactionCount: Int,
    onCategoryClick: () -> Unit,
    onAccountClick: () -> Unit,
    onTargetAccountClick: () -> Unit,
    showBillingCycleMenu: Boolean,
    onBillingCycleMenuChange: (Boolean) -> Unit,
    viewModel: TransactionDetailViewModel
) {
    val selectedAccount by viewModel.selectedAccount.collectAsStateWithLifecycle()
    val targetAccount by viewModel.targetAccount.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val allSubcategories by viewModel.allSubcategories.collectAsStateWithLifecycle()

    val selectedCategoryObj = remember(transaction.category, categories) {
        categories.find { it.name == transaction.category }
    }
    
    val categoryId = selectedCategoryObj?.id
    val selectedSubcategoryObj = remember(transaction.subcategory, categoryId, allSubcategories) {
        if (categoryId != null) {
            allSubcategories[categoryId]?.find { it.name == transaction.subcategory }
        } else null
    }

    CashiroCard(
        modifier = Modifier.fillMaxWidth(),
        containerColor = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
        ) {
            // Account Selection
            val transactionType = transaction.transactionType
            
            BlurredAnimatedVisibility(transactionType == TransactionType.TRANSFER) {
                // Transfer Type UI:
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(1.5.dp)
                        ) {
                            // Source Account Card
                            OutlinedCard(
                                onClick = onAccountClick,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(
                                    topStart = 16.dp,
                                    topEnd = 16.dp,
                                    bottomStart = 4.dp,
                                    bottomEnd = 4.dp),
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
                                    if (selectedAccount != null && selectedAccount?.iconResId != 0) {
                                        Icon(
                                            painter = painterResource(id = selectedAccount!!.iconResId),
                                            contentDescription = null,
                                            tint = Color.Unspecified,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Default.AccountBalance,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = selectedAccount?.bankName
                                                ?: "Select Source Account",
                                            style = MaterialTheme.typography.bodyLarge,
                                            color =
                                                if (selectedAccount != null)
                                                    MaterialTheme.colorScheme.onSurface
                                                else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        if (selectedAccount != null) {
                                            Text(
                                                text = "••${selectedAccount?.accountLast4}",
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

                            // Target Account Card
                            OutlinedCard(
                                onClick = onTargetAccountClick,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(
                                    topStart = 4.dp,
                                    topEnd = 4.dp,
                                    bottomStart = 16.dp,
                                    bottomEnd = 16.dp),
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
                                    if (targetAccount != null && targetAccount?.iconResId != 0) {
                                        Icon(
                                            painter = painterResource(id = targetAccount!!.iconResId),
                                            contentDescription = null,
                                            tint = Color.Unspecified,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Default.AccountBalance,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = targetAccount?.bankName
                                                ?: "Select Target Account",
                                            style = MaterialTheme.typography.bodyLarge,
                                            color =
                                                if (targetAccount != null)
                                                    MaterialTheme.colorScheme.onSurface
                                                else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        if (targetAccount != null) {
                                            Text(
                                                text = "••${targetAccount?.accountLast4}",
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
                        }
                        // Exchange Icon
                        Box(
                            modifier = Modifier.fillMaxWidth().align(Alignment.Center),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .shadow(elevation = 3.dp, shape = CircleShape)
                                    .clip(CircleShape)
                                    .background(
                                        MaterialTheme.colorScheme.surface,
                                        shape = CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SwapVert,
                                    contentDescription = "Transfer",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Category Selection
                    CategoryDropdown(
                        selectedCategory = transaction.category,
                        selectedSubcategory = transaction.subcategory,
                        onClick = onCategoryClick,
                        isTransferType = transactionType == TransactionType.TRANSFER,
                        viewModel = viewModel
                    )
                }
            }
            
            BlurredAnimatedVisibility(transactionType != TransactionType.TRANSFER){
                // Non-Transfer Type UI
                Column(
                    modifier = Modifier.animateContentSize().fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(1.5.dp)
                ) {
                    OutlinedCard(
                        onClick = onAccountClick,
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
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            if (selectedAccount != null && selectedAccount?.iconResId != 0) {
                                Icon(
                                    painter = painterResource(id = selectedAccount!!.iconResId),
                                    contentDescription = null,
                                    tint = Color.Unspecified,
                                    modifier = Modifier.size(24.dp)
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.AccountBalance,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = selectedAccount?.bankName ?: "Select Account",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color =
                                        if (selectedAccount != null)
                                            MaterialTheme.colorScheme.onSurface
                                        else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (selectedAccount != null) {
                                    Text(
                                        text = "••${selectedAccount?.accountLast4}",
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
                    CategoryDropdown(
                        selectedCategory = transaction.category,
                        selectedSubcategory = transaction.subcategory,
                        onClick = onCategoryClick,
                        viewModel = viewModel
                    )
                }
            }

            if (!transaction.smsBody.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(Spacing.sm))

                // Apply to all from merchant checkbox
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = applyToAllFromMerchant,
                        onCheckedChange = { viewModel.toggleApplyToAllFromMerchant() }
                    )
                    Spacer(modifier = Modifier.width(Spacing.sm))
                    Text(
                        text = "Apply this category to all future transactions from ${transaction.merchantName}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                // Update existing transactions checkbox (only show if there are other transactions)
                if (existingTransactionCount > 0) {
                    Spacer(modifier = Modifier.height(Spacing.xs))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = updateExistingTransactions,
                            onCheckedChange = { viewModel.toggleUpdateExistingTransactions() }
                        )
                        Spacer(modifier = Modifier.width(Spacing.sm))
                        Text(
                            text = "Also update $existingTransactionCount existing ${if (existingTransactionCount == 1) "transaction" else "transactions"} from ${transaction.merchantName}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(Spacing.sm))

            // Recurring Switch and Billing Cycle
            PreferenceSwitch(
                title = "Recurring Transaction",
                subtitle = "Mark this as a repeating payment",
                checked = transaction.isRecurring,
                onCheckedChange = { viewModel.updateRecurringStatus(it) },
                leadingIcon = {
                    Icon(
                        Icons.Default.EventRepeat,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                isSingle = !transaction.isRecurring,
                isFirst = transaction.isRecurring,
                padding = PaddingValues(horizontal = 0.dp, vertical = 1.5.dp)
            )

            AnimatedVisibility(visible = transaction.isRecurring) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(1.5.dp)
                ) {
                    val billingCycles = listOf("Weekly", "Monthly", "Quarterly", "Semi-Annual", "Annual")
                    
                    ExposedDropdownMenuBox(
                        expanded = showBillingCycleMenu,
                        onExpandedChange = { onBillingCycleMenuChange(it) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        TextField(
                            value = transaction.billingCycle ?: "Monthly",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Billing Cycle", fontWeight = FontWeight.SemiBold) },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Repeat,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                            },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showBillingCycleMenu) },
                            shape = RoundedCornerShape(
                                topStart = 4.dp,
                                topEnd = 4.dp,
                                bottomStart = 16.dp,
                                bottomEnd = 16.dp
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                                focusedLabelColor = MaterialTheme.colorScheme.primary,
                                unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.7f)
                            )
                        )

                        ExposedDropdownMenu(
                            expanded = showBillingCycleMenu,
                            onDismissRequest = { onBillingCycleMenuChange(false) }
                        ) {
                            billingCycles.forEach { cycle ->
                                DropdownMenuItem(
                                    text = { Text(cycle) },
                                    onClick = {
                                        viewModel.updateBillingCycle(cycle)
                                        onBillingCycleMenuChange(false)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryDropdown(
    selectedCategory: String,
    selectedSubcategory: String?,
    isTransferType: Boolean = false,
    onClick: () -> Unit,
    viewModel: TransactionDetailViewModel
) {
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val allSubcategories by viewModel.allSubcategories.collectAsStateWithLifecycle()

    val selectedCategoryObj = remember(selectedCategory, categories) {
        categories.find { it.name == selectedCategory }
    }
    val categoryId = selectedCategoryObj?.id
    val selectedSubcategoryObj = remember(selectedSubcategory, categoryId, allSubcategories) {
        if (categoryId != null) {
            allSubcategories[categoryId]?.find { it.name == selectedSubcategory }
        } else null
    }

    val categoryInteractionSource = remember { MutableInteractionSource() }

    Column(modifier = Modifier.fillMaxWidth()) {
        TextField(
            value = selectedCategory,
            onValueChange = {},
            label = { Text("Category", fontWeight = FontWeight.SemiBold) },
            readOnly = true,
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    interactionSource = categoryInteractionSource,
                    indication = null
                ) {
                    onClick()
                },
            shape = RoundedCornerShape(
                topEnd = if (isTransferType) 16.dp else 4.dp,
                topStart = if (isTransferType) 16.dp else 4.dp,
                bottomEnd = 16.dp,
                bottomStart = 16.dp),
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
            enabled = false, // Disable typing, handle click above
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

        // Subcategory Display (Read-only, selected via sheet)
        if (selectedSubcategory != null) {
            Spacer(modifier = Modifier.height(Spacing.md))
            TextField(
                value = selectedSubcategory,
                onValueChange = {},
                readOnly = true,
                label = { Text("Subcategory") },
                leadingIcon = {
                    if (selectedSubcategoryObj != null && selectedSubcategoryObj.iconResId != 0) {
                        Icon(
                            painter = painterResource(id = selectedSubcategoryObj.iconResId),
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
                colors = if (selectedSubcategoryObj != null) {
                    val color = try {
                        Color(selectedSubcategoryObj.color.toColorInt())
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
                        unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.7f),
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
                        unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.7f),
                        disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                        disabledIndicatorColor = Color.Transparent,
                        disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        disabledTextColor = MaterialTheme.colorScheme.onSurface
                    )
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateTimeField(
    dateTime: LocalDateTime,
    onDateTimeChange: (LocalDateTime) -> Unit
) {
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
    ) {
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
                    dateTime.format(DateTimeFormatter.ofPattern("dd MMM yyyy"))
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
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 8.dp, vertical = 12.dp)
                .clickable { showTimePicker = true },
            contentAlignment = Alignment.CenterEnd
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End
            ) {
                val hour = if (dateTime.hour % 12 == 0) 12 else dateTime.hour % 12
                val minute = dateTime.minute
                val amPm = if (dateTime.hour < 12) "AM" else "PM"

                Box(modifier = Modifier
                    .padding(5.dp)
                    .background(
                        color = MaterialTheme.colorScheme.primary.copy(0.2f),
                        shape = RoundedCornerShape(8.dp)
                    )
                ) {
                    Text(
                        text = String.format("%02d", hour),
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        lineHeight = 16.sp,
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
                    modifier = Modifier
                        .padding(5.dp)
                        .background(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(8.dp)
                        )
                ) {
                    Text(
                        text = String.format("%02d", minute),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 16.sp,
                        lineHeight = 16.sp,
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

    // Date Picker Dialog
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = dateTime.toLocalDate().toEpochDay() * 24 * 60 * 60 * 1000
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val newDate = Instant.ofEpochMilli(millis)
                            .atZone(ZoneId.systemDefault())
                            .toLocalDate()
                        onDateTimeChange(dateTime.withYear(newDate.year)
                            .withMonth(newDate.monthValue)
                            .withDayOfMonth(newDate.dayOfMonth))
                    }
                    showDatePicker = false
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    // Time Picker Dialog
    if (showTimePicker) {
        val timePickerState = rememberTimePickerState(
            initialHour = dateTime.hour,
            initialMinute = dateTime.minute
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = { Text("Select Time") },
            text = {
                TimePicker(state = timePickerState)
            },
            confirmButton = {
                TextButton(onClick = {
                    onDateTimeChange(dateTime.withHour(timePickerState.hour)
                        .withMinute(timePickerState.minute))
                    showTimePicker = false
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CurrencyDropdown(
    selectedCurrency: String,
    onCurrencySelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    // Common currencies
    val currencies = listOf(
        "INR", "USD", "EUR", "GBP", "AED", "SGD",
        "CAD", "AUD", "JPY", "CNY", "NPR", "ETB",
        "THB", "MYR", "KWD", "KRW"
    )

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = selectedCurrency,
            onValueChange = { },
            label = { Text("Currency") },
            leadingIcon = {
                Text(
                    CurrencyFormatter.getCurrencySymbol(selectedCurrency),
                    style = MaterialTheme.typography.bodyLarge
                )
            },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(MenuAnchorType.PrimaryNotEditable),
            readOnly = true,
            singleLine = true
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            currencies.forEach { currency ->
                DropdownMenuItem(
                    text = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                CurrencyFormatter.getCurrencySymbol(currency),
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.width(32.dp)
                            )
                            Text(currency)
                        }
                    },
                    onClick = {
                        onCurrencySelected(currency)
                        expanded = false
                    },
                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                )
            }
        }
    }
}

@Composable
private fun AccountSelectionSheet(
    accounts: List<TransactionDetailViewModel.AccountInfo>,
    selectedAccount: TransactionDetailViewModel.AccountInfo?,
    title: String = "Select Account",
    onAccountSelected: (TransactionDetailViewModel.AccountInfo?) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
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
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp).padding(horizontal = 16.dp).clip(RoundedCornerShape(16.dp)),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    // Option to deselect/None
                    Surface(
                        onClick = { onAccountSelected(null) },
                        shape = RoundedCornerShape(12.dp),
                        color = if (selectedAccount == null) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surface,
                        border = if (selectedAccount == null) null
                        else BorderStroke(
                            1.dp,
                            MaterialTheme.colorScheme.outlineVariant
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
                    val isSelected = selectedAccount?.id == account.id
                    Surface(
                        onClick = { onAccountSelected(account) },
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surface,
                        border = if (isSelected) null
                        else BorderStroke(
                            1.dp,
                            MaterialTheme.colorScheme.outlineVariant
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            if (account.iconResId != 0) {
                                Icon(
                                    painter = painterResource(id = account.iconResId),
                                    contentDescription = null,
                                    tint = Color.Unspecified,
                                    modifier = Modifier.size(24.dp)
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.AccountBalance,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = account.bankName,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "••${account.accountLast4}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }
                }
                
                item {
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}