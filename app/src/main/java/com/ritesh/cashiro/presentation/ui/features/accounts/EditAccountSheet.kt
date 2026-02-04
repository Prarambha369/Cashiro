package com.ritesh.cashiro.presentation.ui.features.accounts

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Merge
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.toColorInt
import com.ritesh.cashiro.R
import com.ritesh.cashiro.data.database.entity.AccountBalanceEntity
import com.ritesh.cashiro.presentation.accounts.CurrencyBottomSheet
import com.ritesh.cashiro.presentation.ui.features.categories.IconSelector
import com.ritesh.cashiro.presentation.ui.components.ColorPickerContent
import com.ritesh.cashiro.presentation.effects.BlurredAnimatedVisibility
import com.ritesh.cashiro.presentation.ui.theme.Spacing
import com.ritesh.cashiro.utils.CurrencyFormatter
import java.math.BigDecimal

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun EditAccountSheet(
    account: AccountBalanceEntity? = null,
    allAccounts: List<AccountBalanceEntity> = emptyList(),
    onDismiss: () -> Unit,
    onMerge: (AccountBalanceEntity, List<AccountBalanceEntity>, BigDecimal?) -> Unit =
        { _, _, _ -> },
    onDelete: (() -> Unit)? = null,
    onSave: (bankName: String,
        balance: BigDecimal,
        accountLast4: String,
        iconResId: Int,
        colorHex: String,
        isCreditCard: Boolean,
        isWallet: Boolean,
        creditLimit: BigDecimal?,
        currency: String
    ) -> Unit
) {
    var bankName by remember { mutableStateOf(account?.bankName ?: "") }
    var balance by remember { mutableStateOf(account?.balance ?: BigDecimal.ZERO) }
    var creditLimit by remember { mutableStateOf(account?.creditLimit ?: BigDecimal.ZERO) }
    var isCreditCard by remember { mutableStateOf(account?.isCreditCard ?: false) }
    var isWallet by remember { mutableStateOf(account?.isWallet ?: false) }
    var accountLast4 by remember { mutableStateOf(account?.accountLast4 ?: "") }
    var selectedCurrency by remember { mutableStateOf(account?.currency ?: "INR") }
    var iconResId by remember {
        mutableStateOf(
            if (account?.iconResId != 0 && account?.iconResId != null) account.iconResId
            else R.drawable.type_finance_bank
        )
    }
    var colorHex by remember { mutableStateOf(account?.color ?: "#33B5E5") }

    var showNumberPad by remember { mutableStateOf(false) }
    var editingCreditLimit by remember { mutableStateOf(false) }
    var showIconSelector by remember { mutableStateOf(false) }
    var showCurrencySheet by remember { mutableStateOf(false) }

    // Merge Flow States
    var showMergeSelection by remember { mutableStateOf(false) }
    var showMergeBalanceOption by remember { mutableStateOf(false) }
    var showMergeConfirmation by remember { mutableStateOf(false) }
    var showMergeManualInput by remember { mutableStateOf(false) }

    var selectedMergeAccounts by remember {
        mutableStateOf<List<AccountBalanceEntity>>(emptyList())
    }
    var mergeNewBalance by remember { mutableStateOf<BigDecimal?>(null) }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    if (showNumberPad) {
        ModalBottomSheet(
            onDismissRequest = { showNumberPad = false },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface,
            dragHandle = { BottomSheetDefaults.DragHandle() }
        ) {
            NumberPad(
                initialValue = if (editingCreditLimit) creditLimit.toString() else balance.toString(),
                onDone = {
                    if (editingCreditLimit) {
                        creditLimit = it.toBigDecimalOrNull() ?: BigDecimal.ZERO
                    } else {
                        balance = it.toBigDecimalOrNull() ?: BigDecimal.ZERO
                    }
                    showNumberPad = false
                    },
                title = if (editingCreditLimit) "Enter Credit Limit" 
                        else if (account == null) "Enter Amount" 
                        else "Update Amount"
            )
        }
    }

    if (showIconSelector) {
        ModalBottomSheet(
            onDismissRequest = { showIconSelector = false },
            containerColor = MaterialTheme.colorScheme.surface,
            dragHandle = { BottomSheetDefaults.DragHandle() }
        ) {
            IconSelector(
                selectedIconId = iconResId,
                onIconSelected = {
                    iconResId = it
                    showIconSelector = false
                }
            )
        }
    }

    if (showCurrencySheet) {
        CurrencyBottomSheet(
            selectedCurrency = selectedCurrency,
            onCurrencySelected = { currency ->
                selectedCurrency = currency
                showCurrencySheet = false
            },
            onDismiss = { showCurrencySheet = false }
        )
    }

    // Merge Dialogs
    if (showMergeSelection && account != null) {
        MergeAccountSelectionDialog(
            currentAccount = account,
            allAccounts = allAccounts,
            onDismiss = { showMergeSelection = false },
            onNext = { accounts ->
                selectedMergeAccounts = accounts
                showMergeSelection = false
                showMergeBalanceOption = true
            }
        )
    }

    if (showMergeBalanceOption && account != null) {
        MergeBalanceOptionDialog(
            selectedAccounts = selectedMergeAccounts,
            currentAccount = account,
            onDismiss = { showMergeBalanceOption = false },
            onOptionSelected = { option ->
                showMergeBalanceOption = false
                when (option) {
                    BalanceMergeOption.SUM -> {
                        mergeNewBalance = account.balance + selectedMergeAccounts.sumOf { it.balance }
                        showMergeConfirmation = true
                    }
                    BalanceMergeOption.MANUAL -> {
                        showMergeManualInput = true
                    }
                    BalanceMergeOption.NONE -> {
                        mergeNewBalance = null
                        showMergeConfirmation = true
                    }
                }
            }
        )
    }

    if (showMergeManualInput) {
        ModalBottomSheet(
            onDismissRequest = { showMergeManualInput = false },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface,
            dragHandle = { BottomSheetDefaults.DragHandle() }
        ) { NumberPad(
            initialValue = "",
            onDone = {
                mergeNewBalance = it.toBigDecimalOrNull() ?: BigDecimal.ZERO
                showMergeManualInput = false
                showMergeConfirmation = true },
            title = "Set Final Balance"
        ) }
    }

    if (showMergeConfirmation && account != null) {
        MergeConfirmationDialog(
            onDismiss = { showMergeConfirmation = false },
            onConfirm = {
                onMerge(account, selectedMergeAccounts, mergeNewBalance)
                showMergeConfirmation = false
                onDismiss() // Close the edit sheet
            }
        )
    }

    Box(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .imePadding()
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.md, vertical = Spacing.md),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Text(
                text = if (account == null) "Add Account" else "Edit Account",
                style = MaterialTheme.typography.titleMediumEmphasized,
                fontWeight = FontWeight.Bold
            )

            // Account Type Selection
            if (account == null) {
                SingleChoiceSegmentedButtonRow(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    SegmentedButton(
                        selected = !isCreditCard && !isWallet,
                        onClick = { 
                            if (isWallet) {
                                // Clear fields if coming from Wallet
                                bankName = ""
                                accountLast4 = ""
                                iconResId = R.drawable.type_finance_bank
                                colorHex = "#33B5E5"
                            }
                            isCreditCard = false
                            isWallet = false 
                        },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 3)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AccountBalance, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Bank")
                        }
                    }
                    SegmentedButton(
                        selected = isCreditCard,
                        onClick = { 
                            isCreditCard = true
                            isWallet = false 
                        },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 3)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CreditCard, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Credit Card")
                        }
                    }
                    SegmentedButton(
                        selected = isWallet,
                        onClick = { 
                            isWallet = true
                            isCreditCard = false
                            accountLast4 = "wallet"
                            bankName = "Cash"
                            iconResId = R.drawable.type_finance_dollar_banknote
                            colorHex = "#8BC34A"
                        },
                        shape = SegmentedButtonDefaults.itemShape(index = 2, count = 3)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AccountBalanceWallet, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Wallet")
                        }
                    }
                }
            }

            // Preview Section
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(Spacing.sm)
            ) {
                PreviewAccountCard(
                    bankName = bankName.ifEmpty { "Bank Name" },
                    balance = balance,
                    accountLast4 = accountLast4.ifEmpty { "0000" },
                    iconResId = iconResId,
                    colorHex = colorHex,
                    currency = selectedCurrency,
                    isCreditCard = isCreditCard,
                    isWallet = isWallet,
                    creditLimit = creditLimit
                )
                Text(
                    text = "Preview",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }

            // Input Fields
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Icon Button
                    Box(
                        modifier = Modifier
                            .size(58.dp)
                            .background(
                                color = Color(colorHex.toColorInt()).copy(alpha = 0.2f),
                                shape = MaterialTheme.shapes.large
                            )
                            .clickable { showIconSelector = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(id = iconResId),
                            contentDescription = null,
                            modifier = Modifier.size(34.dp),
                            tint = Color.Unspecified
                        )
                        // Edit badge
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .size(16.dp)
                                .background(
                                    MaterialTheme.colorScheme.primary,
                                    CircleShape
                                )
                                .border(
                                    1.dp,
                                    MaterialTheme.colorScheme.surface,
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = null,
                                modifier = Modifier.size(10.dp),
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                    // Balance/Outstanding Input
                    Surface(
                        onClick = { 
                            editingCreditLimit = false
                            showNumberPad = true 
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.large,
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Leading Icon
                            Icon(
                                imageVector = Icons.Default.AccountBalanceWallet,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(modifier = Modifier.width(16.dp))

                            // Label and Value
                            Column(verticalArrangement = Arrangement.Center) {
                                Text(
                                    text = if (isCreditCard) "Outstanding" else "Balance",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = CurrencyFormatter.formatCurrency(
                                        balance,
                                        selectedCurrency
                                    ),
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }

                if (isCreditCard) {
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // Credit Limit Input
                    Surface(
                        onClick = { 
                            editingCreditLimit = true
                            showNumberPad = true 
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.large,
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CreditCard,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(modifier = Modifier.width(16.dp))

                            Column(verticalArrangement = Arrangement.Center) {
                                Text(
                                    text = "Credit Limit",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = CurrencyFormatter.formatCurrency(
                                        creditLimit,
                                        selectedCurrency
                                    ),
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }

                    // Available Credit Tip
                    val availableCredit = creditLimit - balance
                    val utilization = if (creditLimit > BigDecimal.ZERO) {
                        ((balance.toDouble() / creditLimit.toDouble()) * 100).toInt()
                    } else 0

                    Spacer(modifier = Modifier.height(12.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)
                        ),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Row(
                            modifier = Modifier.padding(Spacing.sm),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
                        ) {
                            Icon(
                                Icons.Default.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(20.dp)
                            )
                            Column {
                                Text(
                                    text = "Available Credit: ${CurrencyFormatter.formatCurrency(availableCredit, selectedCurrency)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                                Text(
                                    text = "Utilization: $utilization%",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Bank Name Row
                TextField(
                    value = bankName,
                    onValueChange = { bankName = it },
                    label = { Text(if (isWallet) "Wallet Name" else "Bank Name", fontWeight = FontWeight.SemiBold) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = 4.dp,
                        bottomEnd = 4.dp
                    ),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedLabelColor = MaterialTheme.colorScheme.primary,
                        unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                            0.7f
                        )
                    ),
                    leadingIcon = { Icon(Icons.Default.DriveFileRenameOutline, contentDescription = null)
                    }
                )

                if (!isWallet) {
                    TextField(
                        value = accountLast4,
                        onValueChange = { if (it.length <= 4 && it.all { char -> char.isDigit() })
                            accountLast4 = it },
                        label = { Text("Account Number (Last 4)", fontWeight = FontWeight.SemiBold) },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("e.g. 1234") },
                        singleLine = true,
                        shape = RoundedCornerShape(4.dp),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            focusedLabelColor = MaterialTheme.colorScheme.primary,
                            unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                0.7f
                            )
                        ),
                        leadingIcon = { Icon(Icons.Default.Pin, contentDescription = null) }
                    )
                }

                // Currency Selection
                val currencyInteractionSource = remember { MutableInteractionSource() }
                TextField(
                    value = "$selectedCurrency (${CurrencyFormatter.getCurrencySymbol(selectedCurrency)})",
                    onValueChange = {},
                    label = { Text("Currency", fontWeight = FontWeight.SemiBold) },
                    readOnly = true,
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(
                            interactionSource = currencyInteractionSource,
                            indication = null
                        ) {
                            showCurrencySheet = true
                        },
                    shape = RoundedCornerShape(
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
                        unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.7f),
                        disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                        disabledIndicatorColor = Color.Transparent,
                        disabledLabelColor = MaterialTheme.colorScheme.primary,
                        disabledTextColor = MaterialTheme.colorScheme.onSurface,
                        disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        disabledLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.AttachMoney,
                            contentDescription = null
                        )
                    },
                    trailingIcon = {
                        Icon(Icons.Default.KeyboardArrowDown, contentDescription = null)
                    },
                    enabled = false
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Color Picker Section
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                    )
                ) {
                    Column(modifier = Modifier.padding(Spacing.md)) {
                        ColorPickerContent(
                            initialColor = colorHex.toColorInt(),
                            onColorChanged = { colorInt ->
                                colorHex = String.format("#%06X", 0xFFFFFF and colorInt)
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(80.dp))
        }

        var checked by remember { mutableStateOf(false) }
        // Action Button at Bottom
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

            SplitButtonLayout(
                leadingButton = {
                    SplitButtonDefaults.LeadingButton(
                        onClick = {
                            onSave(
                                bankName,
                                balance,
                                accountLast4,
                                iconResId,
                                colorHex,
                                isCreditCard,
                                isWallet,
                                if (isCreditCard) creditLimit else null,
                                selectedCurrency) },
                        enabled = bankName.isNotBlank() && (isWallet || accountLast4.length == 4),
                        modifier = Modifier.height(56.dp)
                    ) {
                        Text(
                            text = if (account == null) "Add Account" else "Save Changes",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth(0.8f)
                        )
                    }
                },
                trailingButton = {
                    val description = "Toggle Button"
                    TooltipBox(
                        positionProvider =
                            TooltipDefaults.rememberTooltipPositionProvider(
                                TooltipAnchorPosition.Above
                            ),
                        tooltip = { PlainTooltip { Text(description) } },
                        state = rememberTooltipState(),
                    ) {
                        SplitButtonDefaults.TrailingButton(
                            checked = checked,
                            onCheckedChange = { checked = it },
                            modifier =
                                Modifier
                                    .height(56.dp)
                                    .semantics {
                                        stateDescription = if (checked) "Expanded" else "Collapsed"
                                        contentDescription = description
                                    },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        ) {
                            val rotation: Float by
                            animateFloatAsState(
                                targetValue = if (checked) 90f else 0f,
                                label = "Trailing Icon Rotation",
                            )
                            Icon(
                                Icons.Filled.MoreVert,
                                modifier =
                                    Modifier
                                        .size(SplitButtonDefaults.TrailingIconSize)
                                        .weight(0.2f)
                                        .graphicsLayer {
                                            this.rotationZ = rotation
                                        },
                                contentDescription = "Localized description",
                            )
                        }
                    }
                },
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .height(56.dp),
            )
            DropdownMenu(
                expanded = checked,
                onDismissRequest = { checked = false },
                containerColor = Color.Transparent,
                shadowElevation = 0.dp,
                modifier = Modifier.padding(8.dp),
                offset = DpOffset(100.dp, 0.dp),
                shape = MaterialTheme.shapes.large
            ) {
                if (account != null) {
                    DropdownMenuItem(
                        text = { Text("Merge Account") },
                        onClick = {
                            checked = false
                            showMergeSelection = true
                        },
                        leadingIcon = { Icon(Icons.Outlined.Merge, contentDescription = null) },
                        modifier = Modifier
                            .shadow(
                                elevation = 2.dp,
                                shape = RoundedCornerShape(
                                    topStart = 16.dp,
                                    topEnd = 16.dp,
                                    bottomStart = 4.dp,
                                    bottomEnd = 4.dp
                                )
                            )
                            .background(
                                color = MaterialTheme.colorScheme.surfaceContainer,
                                shape = RoundedCornerShape(
                                    topStart = 16.dp,
                                    topEnd = 16.dp,
                                    bottomStart = 4.dp,
                                    bottomEnd = 4.dp
                                )
                            ),
                    )
                    Spacer(modifier = Modifier.height(1.5.dp))
                    DropdownMenuItem(
                        text = { Text("Delete Account", color = MaterialTheme.colorScheme.error) },
                        onClick = {
                            checked = false
                            onDelete?.invoke()
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Delete Account",
                                tint = MaterialTheme.colorScheme.error
                            )
                        },
                        modifier = Modifier
                            .shadow(
                                elevation = 2.dp,
                                shape = RoundedCornerShape(
                                    topStart = 4.dp,
                                    topEnd = 4.dp,
                                    bottomStart = 16.dp,
                                    bottomEnd = 16.dp
                                )
                            )
                            .background(
                                color = MaterialTheme.colorScheme.surfaceContainer,
                                shape = RoundedCornerShape(
                                    topStart = 4.dp,
                                    topEnd = 4.dp,
                                    bottomStart = 16.dp,
                                    bottomEnd = 16.dp
                                )
                            ),
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun PreviewAccountCard(
    bankName: String,
    balance: BigDecimal,
    accountLast4: String,
    iconResId: Int,
    colorHex: String,
    currency: String,
    isCreditCard: Boolean = false,
    isWallet: Boolean = false,
    creditLimit: BigDecimal = BigDecimal.ZERO
) {
    Card(
        modifier = Modifier.padding(bottom = Spacing.sm).fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().animateContentSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Balance/Outstanding Section
            Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 20.dp)) {
                Text(
                    text = if (isCreditCard) "Outstanding" else "Balance",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = CurrencyFormatter.formatCurrency(balance, currency),
                    style = MaterialTheme.typography.displaySmall.copy(
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            BlurredAnimatedVisibility(
                visible = isCreditCard,
                enter = fadeIn() + slideInVertically(MaterialTheme.motionScheme.fastEffectsSpec()),
                exit = fadeOut() + slideOutVertically(MaterialTheme.motionScheme.fastEffectsSpec())
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Credit Limit",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = CurrencyFormatter.formatCurrency(creditLimit, currency),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // Bottom Bank Info Section
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceContainerLow)
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = bankName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (isWallet) "wallet" else "**** **** **** $accountLast4",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.6f)
                        )
                    }

                    Surface(
                        shape = CircleShape,
                        color = Color(colorHex.toColorInt()).copy(alpha = 0.1f),
                        modifier = Modifier.size(48.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                painter = painterResource(id = iconResId),
                                contentDescription = null,
                                modifier = Modifier.size(24.dp),
                                tint = Color.Unspecified
                            )
                        }
                    }
                }
            }
        }
    }
}
