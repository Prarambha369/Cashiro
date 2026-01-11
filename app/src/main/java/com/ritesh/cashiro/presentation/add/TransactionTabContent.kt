package com.ritesh.cashiro.presentation.add

import androidx.compose.animation.animateContentSize
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
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import com.ritesh.cashiro.ui.components.AccountCard
import com.ritesh.cashiro.ui.components.CategorySelectionSheet
import com.ritesh.cashiro.ui.theme.*
import java.time.format.DateTimeFormatter
import java.util.Locale
import androidx.compose.ui.res.painterResource
import com.ritesh.cashiro.ui.effects.BlurredAnimatedVisibility
import androidx.core.graphics.toColorInt
import com.ritesh.cashiro.ui.effects.overScrollVertical
import com.ritesh.cashiro.ui.effects.rememberOverscrollFlingBehavior
import kotlinx.coroutines.delay
import com.ritesh.cashiro.presentation.accounts.NumberPad

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun TransactionTabContent(viewModel: AddViewModel, onSave: () -> Unit) {
    val uiState by viewModel.transactionUiState.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val transactionSubcategories by viewModel.transactionSubcategories.collectAsState()

    val selectedCategoryObj = remember(uiState.category, categories) {
        categories.find { it.name == uiState.category }
    }
    val selectedSubcategoryObj = remember(uiState.subcategory, transactionSubcategories) {
        transactionSubcategories.find { it.name == uiState.subcategory }
    }

    val labels = listOf("Search Fruits", "Search Shopping", "Search Fitness", "Search Sports")
    var currentLabelIndex by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(3000)
            currentLabelIndex = (currentLabelIndex + 1) % labels.size
        }
    }

    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var showCategoryMenu by remember { mutableStateOf(false) }
    var showNumberPad by remember { mutableStateOf(false) }

    val scrollState = rememberScrollState()
    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .animateContentSize()
                .fillMaxSize()
                .overScrollVertical()
                .imePadding() // Handle keyboard properly
                .verticalScroll(
                    state = scrollState,
                    flingBehavior = rememberOverscrollFlingBehavior { scrollState }
                )
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Amount Input
            val amountInteractionSource = remember { MutableInteractionSource() }
            // Amount Input
            AmountInput(
                amount = uiState.amount.ifEmpty { "0" },
                currencySymbol = com.ritesh.cashiro.utils.CurrencyFormatter.getCurrencySymbol(
                    uiState.selectedAccount?.currency ?: "INR"
                ),
                onClick = {
                    showNumberPad = true
                },
                modifier = Modifier.fillMaxWidth()
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
            // Date and Time Selection
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
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
                        val hour = if (uiState.date.hour % 12 == 0) 12 else uiState.date.hour % 12
                        val minute = uiState.date.minute
                        val amPm = if (uiState.date.hour < 12) "AM" else "PM"

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

            // Accounts Section
            val accounts by viewModel.accounts.collectAsState()
            var showAccountSheet by remember { mutableStateOf(false) }
            var showTargetAccountSheet by remember { mutableStateOf(false) }

            // Conditional UI based on transaction type
            BlurredAnimatedVisibility(uiState.transactionType == TransactionType.TRANSFER) {
                // Transfer Type UI: Source Account + Exchange Icon + Target Account + Category
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
                                onClick = { showAccountSheet = true },
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
                                    if (uiState.selectedAccount != null && uiState.selectedAccount?.iconResId != 0) {
                                        Icon(
                                            painter = painterResource(id = uiState.selectedAccount!!.iconResId),
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
                                            text = uiState.selectedAccount?.bankName
                                                ?: "Select Source Account",
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

                            // Target Account Card
                            OutlinedCard(
                                onClick = { showTargetAccountSheet = true },
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
                                    if (uiState.targetAccount != null && uiState.targetAccount?.iconResId != 0) {
                                        Icon(
                                            painter = painterResource(id = uiState.targetAccount!!.iconResId),
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
                                            text = uiState.targetAccount?.bankName
                                                ?: "Select Target Account",
                                            style = MaterialTheme.typography.bodyLarge,
                                            color =
                                                if (uiState.targetAccount != null)
                                                    MaterialTheme.colorScheme.onSurface
                                                else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        if (uiState.targetAccount != null) {
                                            Text(
                                                text = "••${uiState.targetAccount?.accountLast4}",
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

                    Spacer(modifier = Modifier.height(16.dp))

                    // Category Selection with full rounded corners
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
                        shape = RoundedCornerShape(16.dp),
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
                            unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(
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
                }
            }
            BlurredAnimatedVisibility(uiState.transactionType != TransactionType.TRANSFER){
                // Non-Transfer Type UI: Original layout with connected sections
                Column(
                    modifier = Modifier.animateContentSize().fillMaxWidth(),
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
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            if (uiState.selectedAccount != null && uiState.selectedAccount?.iconResId != 0) {
                                Icon(
                                    painter = painterResource(id = uiState.selectedAccount!!.iconResId),
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
                    if (uiState.subcategory != null) {
                        Spacer(modifier = Modifier.height(Spacing.md))
                        TextField(
                            value = uiState.subcategory ?: "None",
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

            // Show subcategory for Transfer type outside the conditional
            if (uiState.transactionType == TransactionType.TRANSFER && uiState.subcategory != null) {
                Spacer(modifier = Modifier.height(Spacing.md))
                TextField(
                    value = uiState.subcategory ?: "None",
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


            if (showAccountSheet) {
                ModalBottomSheet(
                    onDismissRequest = { showAccountSheet = false },
                    containerColor = MaterialTheme.colorScheme.surface,
                    dragHandle = { BottomSheetDefaults.DragHandle() }
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
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
                                modifier = Modifier.fillMaxWidth().padding(top = 8.dp).padding(horizontal = 16.dp).clip(RoundedCornerShape(16.dp)),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                item {
                                    // Option to deselect/None
                                    Surface(
                                        onClick = {
                                            viewModel.updateTransactionAccount(null)
                                            showAccountSheet = false },
                                        shape = RoundedCornerShape(12.dp),
                                        color = if (uiState.selectedAccount == null) MaterialTheme.colorScheme.primaryContainer
                                        else MaterialTheme.colorScheme.surface,
                                        border = if (uiState.selectedAccount == null) null
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
                                item{
                                    Spacer(modifier = Modifier.height(64.dp))
                                }
                            }
                        }
                    }
                }
            }

            // Target Account BottomSheet (for Transfer type)
            if (showTargetAccountSheet) {
                ModalBottomSheet(
                    onDismissRequest = { showTargetAccountSheet = false },
                    containerColor = MaterialTheme.colorScheme.surface,
                    dragHandle = { BottomSheetDefaults.DragHandle() }
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "Select Target Account",
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
                                // Filter out the source account from target selection
                                val availableAccounts = accounts.filter { it.id != uiState.selectedAccount?.id }
                                
                                items(availableAccounts) { account ->
                                    val isSelected = uiState.targetAccount?.id == account.id
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
                                                viewModel.updateTransactionTargetAccount(account)
                                                showTargetAccountSheet = false
                                            }
                                        )
                                    }
                                }
                                item{
                                    Spacer(modifier = Modifier.height(64.dp))
                                }
                            }
                        }
                    }
                }
            }
            // NumberPad for Amount Input
            if (showNumberPad) {
                ModalBottomSheet(
                    onDismissRequest = { showNumberPad = false },
                    containerColor = MaterialTheme.colorScheme.surface,
                    dragHandle = { BottomSheetDefaults.DragHandle() }
                ) {
                    NumberPad(
                        initialValue = uiState.amount.ifEmpty { "0" },
                        onDone = { newAmount ->
                            viewModel.updateTransactionAmount(newAmount)
                            showNumberPad = false
                        },
                        title = "Enter Amount"
                    )
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
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedLabelColor = MaterialTheme.colorScheme.primary,
                        unfocusedLabelColor =
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(0.7f)
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
                onClick = { viewModel.saveTransaction(onSuccess = onSave) },
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
                ) { Text("OK") } },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) { Text("Cancel") }
            }
        )
    }
}
