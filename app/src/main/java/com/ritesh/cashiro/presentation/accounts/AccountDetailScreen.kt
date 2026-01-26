 package com.ritesh.cashiro.presentation.accounts

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import com.ritesh.cashiro.data.database.entity.AccountBalanceEntity
import com.ritesh.cashiro.data.database.entity.TransactionEntity
import com.ritesh.cashiro.data.database.entity.TransactionType
import com.ritesh.cashiro.presentation.categories.NavigationContent
import com.ritesh.cashiro.ui.components.*
import com.ritesh.cashiro.ui.effects.overScrollVertical
import com.ritesh.cashiro.ui.effects.rememberOverscrollFlingBehavior
import com.ritesh.cashiro.ui.theme.*
import com.ritesh.cashiro.utils.CurrencyFormatter
import dev.chrisbanes.haze.HazeDefaults
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.haze
import dev.chrisbanes.haze.hazeSource
import java.math.BigDecimal
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
fun AccountDetailScreen(
    navController: NavController,
    bankName: String = "",
    accountLast4: String = "",
    viewModel: AccountDetailViewModel = hiltViewModel(),
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedContentScope: AnimatedVisibilityScope? = null
) {
    val uiState by viewModel.uiState.collectAsState()
    val selectedDateRange by viewModel.selectedDateRange.collectAsState()
    
    val scrollBehaviorSmall = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
    val scrollBehaviorLarge = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
    val hazeState = remember { HazeState() }
    val lazyListState = rememberLazyListState()
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .then(
                if (sharedTransitionScope != null && animatedContentScope != null) {
                    with(sharedTransitionScope) {
                        Modifier.sharedBounds(
                            rememberSharedContentState(key = "account_${bankName}_${accountLast4}"),
                            animatedVisibilityScope = animatedContentScope,
                            boundsTransform = { _, _ ->
                                spring(
                                    stiffness = Spring.StiffnessLow,
                                    dampingRatio = Spring.DampingRatioLowBouncy
                                )
                            },
                            resizeMode = SharedTransitionScope.ResizeMode.scaleToBounds()
                        )
                    }
                } else Modifier
            )
    ) {
        Scaffold(
            modifier = Modifier.nestedScroll(scrollBehaviorLarge.nestedScrollConnection),
        topBar = {
            CustomTitleTopAppBar(
                scrollBehaviorSmall = scrollBehaviorSmall,
                scrollBehaviorLarge = scrollBehaviorLarge,
                title = uiState.bankName.ifEmpty { "Account Details" },
                hasBackButton = true,
                hazeState = hazeState,
                navigationContent = { NavigationContent({navController.navigateUp()}) },
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .overScrollVertical()
                .hazeSource(state = hazeState),
            flingBehavior = rememberOverscrollFlingBehavior { lazyListState },
            contentPadding = PaddingValues(
                top =Dimensions.Padding.content + paddingValues.calculateTopPadding(),
                bottom = 0.dp
            ),
            verticalArrangement = Arrangement.spacedBy(Spacing.md)
        ) {
            // Account Card
            item {
                uiState.currentBalance?.let { balance ->
                    AccountCard(
                        account = balance,
                        showMoreOptions = false, // Keep it simple in detail view
                        modifier = Modifier.padding(horizontal = Dimensions.Padding.content)
                    )
                }
            }

            // Date Range Filter
            item {
                DateRangeFilter(
                    selectedRange = selectedDateRange,
                    onRangeSelected = viewModel::selectDateRange
                )
            }

            // Balance Chart (Expandable) - Updates based on selected timeframe
            if (uiState.balanceChartData.isNotEmpty()) {
                item {
                    ExpandableBalanceChart(
                        primaryCurrency = uiState.primaryCurrency,
                        balanceHistory = uiState.balanceChartData,
                        selectedTimeframe = selectedDateRange.label,
                        modifier = Modifier.padding(horizontal = Dimensions.Padding.content)
                    )
                }
            }

            // Summary Statistics
            item {
                SummaryStatistics(
                    totalIncome = uiState.totalIncome,
                    totalExpenses = uiState.totalExpenses,
                    netBalance = uiState.netBalance,
                    period = selectedDateRange.label,
                    primaryCurrency = uiState.primaryCurrency,
                    hasMultipleCurrencies = uiState.hasMultipleCurrencies,
                    modifier = Modifier.padding(horizontal = Dimensions.Padding.content)
                )
            }
            
            // Transactions Header
            item {
                SectionHeader(
                    title = "Transactions (${uiState.transactions.size})",
                    modifier = Modifier.padding(horizontal = Dimensions.Padding.content)
                )
            }
            
            // Transaction List
            if (uiState.transactions.isEmpty() && !uiState.isLoading) {
                item {
                    EmptyTransactionsState(
                        modifier = Modifier.padding(horizontal = Dimensions.Padding.content)
                    )
                }
            } else {
                items(
                    items = uiState.transactions,
                    key = { it.id }
                ) { transaction ->
                    TransactionItem(
                        transaction = transaction,
                        primaryCurrency = uiState.primaryCurrency,
                        onClick = {
                            navController.navigate(
                                com.ritesh.cashiro.navigation.TransactionDetail(transaction.id)
                            )
                        },
                        modifier = Modifier.padding(horizontal = Dimensions.Padding.content)
                    )
                }
            }
            
            // Loading State
            if (uiState.isLoading) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
            }
        }
    }
}
}

@Composable
private fun ExpandableBalanceChart(
    modifier: Modifier = Modifier,
    primaryCurrency: String,
    balanceHistory: List<BalancePoint>,
    selectedTimeframe: String
) {
    var isExpanded by remember { mutableStateOf(false) }
    
    CashiroCard(
        modifier = modifier
            .fillMaxWidth()
            .clickable { isExpanded = !isExpanded }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Dimensions.Padding.content)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ShowChart,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "Balance Trend",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = selectedTimeframe,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 28.dp)
                    )
                }
                
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = if (isExpanded) "Collapse" else "Expand",
                    modifier = Modifier
                        .size(24.dp)
                        .rotate(if (isExpanded) 180f else 0f),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column {
                    Spacer(modifier = Modifier.height(Spacing.md))
                    BalanceChart(
                        primaryCurrency = primaryCurrency,
                        balanceHistory = balanceHistory,
                        height = 180
                    )
                }
            }
        }
    }
}



@Composable
private fun SummaryStatistics(
    modifier: Modifier = Modifier,
    totalIncome: BigDecimal,
    totalExpenses: BigDecimal,
    netBalance: BigDecimal,
    period: String,
    primaryCurrency: String,
    hasMultipleCurrencies: Boolean = false
) {
    CashiroCard(
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Dimensions.Padding.content)
        ) {
            Text(
                text = period,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(Spacing.md))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatisticItem(
                    label = "Income",
                    value = formatWithEstimatedDisplay(totalIncome, primaryCurrency, hasMultipleCurrencies),
                    icon = Icons.AutoMirrored.Filled.TrendingUp,
                    color = if (!isSystemInDarkTheme()) income_light else income_dark
                )
                StatisticItem(
                    label = "Expenses",
                    value = formatWithEstimatedDisplay(totalExpenses, primaryCurrency, hasMultipleCurrencies),
                    icon = Icons.AutoMirrored.Filled.TrendingDown,
                    color = if (!isSystemInDarkTheme()) expense_light else expense_dark
                )
                StatisticItem(
                    label = "Net",
                    value = formatWithEstimatedDisplay(netBalance, primaryCurrency, hasMultipleCurrencies),
                    icon = Icons.Default.AccountBalanceWallet,
                    color = if (netBalance >= BigDecimal.ZERO) {
                        if (!isSystemInDarkTheme()) income_light else income_dark
                    } else {
                        if (!isSystemInDarkTheme()) expense_light else income_dark
                    }
                )
            }
        }
    }
}

/**
 * Formats currency with estimated display for multi-currency accounts
 */
private fun formatWithEstimatedDisplay(
    amount: BigDecimal,
    currency: String,
    hasMultipleCurrencies: Boolean
): String {
    val formattedAmount = CurrencyFormatter.formatCurrency(amount, currency)
    return if (hasMultipleCurrencies) {
        "est. $formattedAmount"
    } else {
        formattedAmount
    }
}

@Composable
private fun StatisticItem(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = color
        )
        Spacer(modifier = Modifier.height(Spacing.xs))
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateRangeFilter(
    selectedRange: DateRange,
    onRangeSelected: (DateRange) -> Unit
) {
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        contentPadding = PaddingValues(horizontal = 0.dp)
    ) {
        item{
            Spacer(modifier = Modifier.width(Spacing.md))
        }
        items(DateRange.values().toList()) { range ->
            FilterChip(
                selected = selectedRange == range,
                onClick = { onRangeSelected(range) },
                label = { Text(range.label) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
        item{
            Spacer(modifier = Modifier.width(Spacing.md))
        }
    }
}

@Composable
private fun TransactionItem(
    modifier: Modifier = Modifier,
    transaction: TransactionEntity,
    primaryCurrency: String,
    onClick: () -> Unit
) {
    val amountColor = when (transaction.transactionType) {
        TransactionType.INCOME -> if (!isSystemInDarkTheme()) income_light else income_dark
        TransactionType.EXPENSE -> if (!isSystemInDarkTheme()) expense_light else expense_dark
        TransactionType.CREDIT -> if (!isSystemInDarkTheme()) credit_light else credit_dark
        TransactionType.TRANSFER -> if (!isSystemInDarkTheme()) transfer_light else transfer_dark
        TransactionType.INVESTMENT -> if (!isSystemInDarkTheme()) investment_light else investment_dark
    }
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Dimensions.Padding.content),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(Spacing.md),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BrandIcon(
                    merchantName = transaction.merchantName,
                    size = 40.dp,
                    showBackground = true
                )
                
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = transaction.merchantName,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = transaction.dateTime.format(
                                DateTimeFormatter.ofPattern("MMM d, h:mm a")
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        
                        // Show balance after if available
                        transaction.balanceAfter?.let { balance ->
                            Text(
                                text = "• Bal: ${CurrencyFormatter.formatCurrency(balance, primaryCurrency)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
            
            Column(
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = CurrencyFormatter.formatCurrency(transaction.amount, transaction.currency),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = amountColor
                )
                
                // Transaction type indicator
                when (transaction.transactionType) {
                    TransactionType.CREDIT -> Icon(
                        Icons.Default.CreditCard,
                        contentDescription = "Credit",
                        modifier = Modifier.size(14.dp),
                        tint = amountColor
                    )
                    TransactionType.TRANSFER -> Icon(
                        Icons.Default.SwapHoriz,
                        contentDescription = "Transfer",
                        modifier = Modifier.size(14.dp),
                        tint = amountColor
                    )
                    TransactionType.INVESTMENT -> Icon(
                        Icons.AutoMirrored.Filled.ShowChart,
                        contentDescription = "Investment",
                        modifier = Modifier.size(14.dp),
                        tint = amountColor
                    )
                    else -> {}
                }
            }
        }
    }
}

@Composable
private fun EmptyTransactionsState(
    modifier: Modifier = Modifier
) {
    CashiroCard(
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Dimensions.Padding.empty),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Receipt,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(Spacing.md))
            Text(
                text = "No transactions found",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(Spacing.xs))
            Text(
                text = "Transactions for this account will appear here",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}