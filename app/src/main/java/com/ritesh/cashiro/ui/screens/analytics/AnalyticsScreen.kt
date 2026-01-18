package com.ritesh.cashiro.ui.screens.analytics

import android.graphics.Color
import androidx.compose.animation.*
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ritesh.cashiro.presentation.common.TimePeriod
import com.ritesh.cashiro.presentation.common.TransactionTypeFilter
import com.ritesh.cashiro.ui.components.*
import com.ritesh.cashiro.ui.effects.BlurredAnimatedVisibility
import com.ritesh.cashiro.ui.effects.overScrollVertical
import com.ritesh.cashiro.ui.effects.rememberOverscrollFlingBehavior
import com.ritesh.cashiro.ui.icons.CategoryMapping
import com.ritesh.cashiro.ui.theme.Dimensions
import com.ritesh.cashiro.ui.theme.Spacing
import com.ritesh.cashiro.utils.CurrencyFormatter
import com.ritesh.cashiro.utils.DateRangeUtils
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.haze
import dev.chrisbanes.haze.hazeChild
import java.math.BigDecimal

enum class ChartType(val icon: ImageVector, val label: String) {
    LINE(Icons.AutoMirrored.Filled.ShowChart, "Line"),
    BAR(Icons.Default.BarChart, "Bar"),
    HEATMAP(Icons.Default.GridOn, "Heatmap")
}

enum class BreakdownType {
    PIE, LIST
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AnalyticsScreen(
    viewModel: AnalyticsViewModel = hiltViewModel(),
    onNavigateToChat: () -> Unit = {},
    onNavigateToTransactions: (category: String?, merchant: String?, period: String?, currency: String?) -> Unit = { _, _, _, _ -> },
    onNavigateToSettings: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val selectedPeriod by viewModel.selectedPeriod.collectAsStateWithLifecycle()
    val transactionTypeFilter by viewModel.transactionTypeFilter.collectAsStateWithLifecycle()
    val selectedCurrency by viewModel.selectedCurrency.collectAsStateWithLifecycle()
    val availableCurrencies by viewModel.availableCurrencies.collectAsStateWithLifecycle()
    val customDateRange by viewModel.customDateRange.collectAsStateWithLifecycle()
    var showAdvancedFilters by remember { mutableStateOf(false) }
    var showDateRangePicker by remember { mutableStateOf(false) }
    
    // Chart Selection State
    var selectedChartType by remember { mutableStateOf(ChartType.LINE) }
    var showChartTypeSelector by remember { mutableStateOf(false) }
    var selectedBreakdownType by remember { mutableStateOf(BreakdownType.PIE) }

    // Calculate active filter count
    val activeFilterCount = if (transactionTypeFilter != TransactionTypeFilter.EXPENSE) 1 else 0

    // Cache expensive operations
    val timePeriods = remember { TimePeriod.values().toList() }
    val customRangeLabel = remember(customDateRange) {
        DateRangeUtils.formatDateRange(customDateRange)
    }

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val scrollBehaviorSmall = TopAppBarDefaults.pinnedScrollBehavior()
    val hazeState = remember { HazeState() }
    val context = LocalContext.current
    val lazyListState = androidx.compose.foundation.lazy.rememberLazyListState()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            CustomTitleTopAppBar(
                title = "Analytics",
                scrollBehaviorSmall = scrollBehaviorSmall,
                scrollBehaviorLarge = scrollBehavior,
                hazeState = hazeState,
                hasBackButton = false,
                onNavigateToSettings = onNavigateToSettings
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            LazyColumn(
                state = lazyListState,
                modifier = Modifier
                    .fillMaxSize()
                    .haze(state = hazeState)
                    .background(MaterialTheme.colorScheme.background)
                    .overScrollVertical(),
                flingBehavior = rememberOverscrollFlingBehavior { lazyListState },
                contentPadding = PaddingValues(
                    start = 0.dp,
                    end = 0.dp,
                    top = paddingValues.calculateTopPadding() + Dimensions.Padding.content,
                    bottom = Dimensions.Padding.content + 80.dp // Extra padding for bottom nav
                ),
                verticalArrangement = Arrangement.spacedBy(Spacing.md)
            ) {
                // Filters (Period and Type)
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                        // Time Period Filter
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            item{
                                Spacer(modifier = Modifier.width(16.dp))
                            }
                            items(timePeriods) { period ->
                                FilterChip(
                                    selected = if (period == TimePeriod.CUSTOM) {
                                        selectedPeriod == period && customDateRange != null
                                    } else {
                                        selectedPeriod == period
                                    },
                                    onClick = {
                                        if (period == TimePeriod.CUSTOM) {
                                            showDateRangePicker = true
                                        } else {
                                            viewModel.selectPeriod(period)
                                        }
                                    },
                                    label = {
                                        Text(
                                            if (period == TimePeriod.CUSTOM && customRangeLabel != null) {
                                                customRangeLabel
                                            } else {
                                                period.label
                                            }
                                        )
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                )
                            }
                            item{
                                Spacer(modifier = Modifier.width(16.dp))
                            }
                        }

                        // Collapsible Filter Row for Type
                        CollapsibleFilterRow(
                            isExpanded = showAdvancedFilters,
                            activeFilterCount = activeFilterCount,
                            onToggle = { showAdvancedFilters = !showAdvancedFilters },
                            modifier = Modifier.fillMaxWidth()

                        ) {
                            LazyRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
                            ) {
                                item{
                                    Spacer(modifier = Modifier.width(16.dp))
                                }
                                items(TransactionTypeFilter.values().toList()) { typeFilter ->
                                    FilterChip(
                                        selected = transactionTypeFilter == typeFilter,
                                        onClick = { viewModel.setTransactionTypeFilter(typeFilter) },
                                        label = { Text(typeFilter.label) },
                                        leadingIcon = if (transactionTypeFilter == typeFilter) {
                                            {
                                                TypeFilterIcon(typeFilter)
                                            }
                                        } else null,
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = MaterialTheme.colorScheme.tertiaryContainer,
                                            selectedLabelColor = MaterialTheme.colorScheme.onTertiaryContainer
                                        )
                                    )
                                }
                                item{
                                    Spacer(modifier = Modifier.width(16.dp))
                                }
                            }
                        }
                    }
                }

                // Currency Selector (if multiple available)
                if (availableCurrencies.size > 1) {
                    item {
                        CurrencyFilterRow(
                            selectedCurrency = selectedCurrency,
                            availableCurrencies = availableCurrencies,
                            onCurrencySelected = { viewModel.selectCurrency(it) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                // Analytics Summary
                if (uiState.totalSpending > BigDecimal.ZERO || uiState.transactionCount > 0) {
                    item {
                        AnalyticsSummaryCard(
                            totalAmount = uiState.totalSpending,
                            transactionCount = uiState.transactionCount,
                            averageAmount = uiState.averageAmount,
                            topCategory = uiState.topCategory,
                            topCategoryPercentage = uiState.topCategoryPercentage,
                            currency = uiState.currency,
                            isLoading = uiState.isLoading,
                            modifier = Modifier.padding(
                                start = Dimensions.Padding.content,
                                end = Dimensions.Padding.content,
                            )
                        )
                    }
                }

                // Spending Trend Chart
                if (uiState.spendingTrend.isNotEmpty()) {
                    item {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.Center
                        ) {
                            SectionHeader(
                                title = "Trends",
                                action = {
                                    Button(
                                        onClick = {
                                            showChartTypeSelector = !showChartTypeSelector
                                        },
                                        shapes = ButtonDefaults.shapes(),
                                        contentPadding = PaddingValues(horizontal = Spacing.sm),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                        ),
                                        modifier = Modifier.height(20.dp)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = selectedChartType.icon,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onSurface,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(modifier = Modifier.width(Spacing.sm))
                                            Text(
                                                text = "${selectedChartType.label} Chart",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurface,
                                            )
                                        }
                                    }
                                },
                                modifier = Modifier.padding(
                                    start = Spacing.lg,
                                    end = Spacing.lg,
                                    top = Dimensions.Padding.content,
                                    bottom = 0.dp
                                )
                            )
                            // Premium Chart Type Selector
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(
                                        start = Dimensions.Padding.content,
                                        end = Dimensions.Padding.content,
                                    )
                                    .animateContentSize()
                            ) {

                                BlurredAnimatedVisibility(
                                    visible = showChartTypeSelector,
                                    modifier = Modifier
                                        .padding(horizontal = Spacing.sm)
                                ) {
                                    CashiroCard(
                                        modifier = Modifier.fillMaxWidth(),
                                        containerColor = CardDefaults.cardColors(
                                            containerColor = MaterialTheme.colorScheme.surface.copy(
                                                alpha = 0.9f
                                            )
                                        )
                                    ) {
                                        Column(modifier = Modifier.padding(horizontal = 8.dp)) {
                                            ChartType.values().forEach { type ->
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .clip(RoundedCornerShape(12.dp))
                                                        .clickable {
                                                            selectedChartType = type
                                                            showChartTypeSelector = false
                                                        }
                                                        .padding(12.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Icon(
                                                            imageVector = type.icon,
                                                            contentDescription = null,
                                                            tint = if (selectedChartType == type) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                                            modifier = Modifier.size(20.dp)
                                                        )
                                                        Spacer(modifier = Modifier.width(12.dp))
                                                        Text(
                                                            text = type.label,
                                                            style = MaterialTheme.typography.bodyMedium,
                                                            color = if (selectedChartType == type) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                                            fontWeight = if (selectedChartType == type) FontWeight.Bold else FontWeight.Normal
                                                        )
                                                    }
                                                    if (selectedChartType == type) {
                                                        Icon(
                                                            imageVector = Icons.Default.Check,
                                                            contentDescription = null,
                                                            tint = MaterialTheme.colorScheme.primary,
                                                            modifier = Modifier.size(20.dp)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    item {
                        CashiroCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(
                                    start = Dimensions.Padding.content,
                                    end = Dimensions.Padding.content,
                                )
                                .animateContentSize()
                        ) {
                            Column(modifier = Modifier.padding(Spacing.md)) {

                                when (selectedChartType) {

                                    ChartType.LINE ->  AnimatedVisibility(
                                        visible = true,
                                        enter = fadeIn() + expandVertically(),
                                        exit = fadeOut() + shrinkVertically()
                                    ) {
                                        SpendingLineChart(
                                            data = uiState.spendingTrend,
                                            currency = uiState.currency
                                        )
                                    }
                                    ChartType.BAR ->  AnimatedVisibility(
                                        visible = true,
                                        enter = fadeIn() + expandVertically(),
                                        exit = fadeOut() + shrinkVertically()
                                    ) {
                                        SpendingBarChart(
                                            data = uiState.spendingTrend,
                                            currency = uiState.currency
                                        )
                                    }
                                    ChartType.HEATMAP ->  AnimatedVisibility(
                                        visible = true,
                                        enter = fadeIn() + expandVertically(),
                                        exit = fadeOut() + shrinkVertically()
                                    ) {
                                        SpendingHeatmap(
                                            data = uiState.spendingTrend
                                        )
                                    }
                                }

                            }
                        }
                    }
                }

                // Category Breakdown
                if (uiState.categoryBreakdown.isNotEmpty()) {
                    item {
                        SectionHeader(
                            title = "Top Categories",
                            action = {
                                IconButton(onClick = {
                                    selectedBreakdownType = if (selectedBreakdownType == BreakdownType.PIE) {
                                        BreakdownType.LIST
                                    } else {
                                        BreakdownType.PIE
                                    }
                                }) {
                                    Icon(
                                        imageVector = if (selectedBreakdownType == BreakdownType.PIE) {
                                            Icons.Default.List
                                        } else {
                                            Icons.Default.PieChart
                                        },
                                        contentDescription = "Toggle View",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            },
                            modifier = Modifier.padding(
                                start = Spacing.lg,
                                end = Spacing.lg,
                                top = Dimensions.Padding.content,
                                bottom = 0.dp
                            )
                        )
                    }

                    // Pie Chart
                    item {
                        Column(
                            modifier = Modifier
                                .animateContentSize()
                                .padding(
                                    start = Dimensions.Padding.content,
                                    end = Dimensions.Padding.content,
                                )
                        ) {
                            BlurredAnimatedVisibility(
                                visible = selectedBreakdownType == BreakdownType.PIE,
                                enter = fadeIn() + slideInVertically(initialOffsetY = { -it }),
                                exit = fadeOut() + slideOutVertically(targetOffsetY = { -it }),
                            ) {
                                CashiroCard(
                                    modifier = Modifier
                                        .animateContentSize()
                                        .fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(Spacing.md)) {
                                        CategoryPieChart(
                                            categories = uiState.categoryBreakdown,
                                            currency = uiState.currency
                                        )
                                    }
                                }
                            }
                            uiState.categoryBreakdown.take(5).forEach { category ->
                                BlurredAnimatedVisibility(
                                    visible = selectedBreakdownType != BreakdownType.PIE,
                                    enter = fadeIn() + slideInVertically(initialOffsetY = { -it }),
                                    exit = fadeOut() + slideOutVertically(targetOffsetY = { -it }),
                                ) {
                                    CategoryProgressItem(
                                        name = category.name,
                                        amount = category.amount,
                                        percentage = category.percentage / 100f,
                                        currency = uiState.currency,
                                        onClick = {
                                            onNavigateToTransactions(
                                                category.name,
                                                null,
                                                selectedPeriod.name,
                                                uiState.currency
                                            )
                                        },
                                    )
                                }
                            }
                        }
                    }
                    if (uiState.categoryBreakdown.size > 5) {
                        item {
                            TextButton(
                                onClick = { /* TODO: Navigate to full Breakdown */ },
                                modifier = Modifier.fillMaxWidth().padding(
                                    start = Dimensions.Padding.content,
                                    end = Dimensions.Padding.content,
                                )
                            ) {
                                Text("View All Categories")
                            }
                        }
                    }

                }

                // Top Merchants
                if (uiState.topMerchants.isNotEmpty()) {
                    item {
                        SectionHeader(
                            title = "Top Merchants",
                            modifier = Modifier.padding(
                                start = Spacing.lg,
                                end = Spacing.lg,
                                top = Dimensions.Padding.content,
                                bottom = Spacing.sm
                            )
                        )
                    }
                    item {
                         ExpandableList(
                            items = uiState.topMerchants,
                            visibleItemCount = 3,
                            modifier = Modifier.fillMaxWidth()
                        ) { merchant ->
                            MerchantListItem(
                                merchant = merchant,
                                currency = uiState.currency,
                                onClick = {
                                    onNavigateToTransactions(
                                        null, // category
                                        merchant.name, // merchant
                                        selectedPeriod.name, // period
                                        uiState.currency // currency
                                    )
                                },
                                modifier = Modifier.padding(
                                    start = Dimensions.Padding.content,
                                    end = Dimensions.Padding.content,
                                )
                            )
                        }
                    }
                }

                // Empty State
                if (uiState.topMerchants.isEmpty() && uiState.categoryBreakdown.isEmpty() && !uiState.isLoading) {
                    item {
                        EmptyAnalyticsState(
                            modifier = Modifier.padding(
                                start = Dimensions.Padding.content,
                                end = Dimensions.Padding.content,
                            )
                        )
                    }
                }
            }
        }

        if (showDateRangePicker) {
            CustomDateRangePickerDialog(
                onDismiss = { showDateRangePicker = false },
                onConfirm = { startDate, endDate ->
                    viewModel.setCustomDateRange(startDate, endDate)
                    showDateRangePicker = false
                },
                initialStartDate = customDateRange?.first,
                initialEndDate = customDateRange?.second
            )
        }
    }
}

@Composable
private fun TypeFilterIcon(typeFilter: TransactionTypeFilter) {
    when (typeFilter) {
        TransactionTypeFilter.INCOME -> Icon(
            Icons.AutoMirrored.Filled.TrendingUp,
            contentDescription = null,
            modifier = Modifier.size(Dimensions.Icon.small)
        )
        TransactionTypeFilter.EXPENSE -> Icon(
            Icons.AutoMirrored.Filled.TrendingDown,
            contentDescription = null,
            modifier = Modifier.size(Dimensions.Icon.small)
        )
        TransactionTypeFilter.CREDIT -> Icon(
            Icons.Default.CreditCard,
            contentDescription = null,
            modifier = Modifier.size(Dimensions.Icon.small)
        )
        TransactionTypeFilter.TRANSFER -> Icon(
            Icons.Default.SwapHoriz,
            contentDescription = null,
            modifier = Modifier.size(Dimensions.Icon.small)
        )
        TransactionTypeFilter.INVESTMENT -> Icon(
            Icons.AutoMirrored.Filled.ShowChart,
            contentDescription = null,
            modifier = Modifier.size(Dimensions.Icon.small)
        )
        else -> {}
    }
}

@Composable
fun CategoryProgressItem(
    name: String,
    amount: BigDecimal,
    percentage: Float,
    currency: String,
    onClick: () -> Unit
) {
    val categoryInfo = CategoryMapping.categories[name]
        ?: CategoryMapping.categories["Others"]!!

    Column(
        modifier = Modifier
            .animateContentSize()
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = Spacing.md)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Category Icon
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(categoryInfo.color.copy(0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    CategoryIcon(
                        category = name,
                        size = 32.dp,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.width(Spacing.sm))
                Column {
                    Text(
                        text = name,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "${(percentage * 100).toInt()}%",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Text(
                text = CurrencyFormatter.formatCurrency(amount, currency),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold
            )
        }
        Spacer(modifier = Modifier.height(Spacing.sm))
        LinearProgressIndicator(
            progress = { percentage },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(CircleShape),
            trackColor = categoryInfo.color.copy(alpha = 0.2f),
            color = categoryInfo.color,
            strokeCap = StrokeCap.Round,
        )
    }
}

@Composable
fun MerchantListItem(
    modifier: Modifier = Modifier,
    merchant: MerchantData,
    currency: String,
    onClick: () -> Unit
) {
    val subtitle = buildString {
        append("${merchant.transactionCount} ")
        append(if (merchant.transactionCount == 1) "transaction" else "transactions")
        if (merchant.isSubscription) {
            append(" • Subscription")
        }
    }

    ListItemCard(
        leadingContent = {
            BrandIcon(
                merchantName = merchant.name,
                size = 40.dp,
                showBackground = true
            )
        },
        title = merchant.name,
        subtitle = subtitle,
        amount = CurrencyFormatter.formatCurrency(merchant.amount, currency),
        onClick = onClick,
        modifier = modifier
    )
}

@Composable
private fun EmptyAnalyticsState(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(Dimensions.Padding.content),
        contentAlignment = Alignment.Center
    ) {
        CashiroCard(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Dimensions.Padding.empty),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.BarChart, // Changed icon for variety
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(Spacing.md))
                Text(
                    text = "No analytics data",
                    style = MaterialTheme.typography.bodyLarge
                )
                Spacer(modifier = Modifier.height(Spacing.xs))
                Text(
                    text = "Try changing the filters or add more transactions",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun CurrencyFilterRow(
    selectedCurrency: String,
    availableCurrencies: List<String>,
    onCurrencySelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
    ) {
        item {
            Spacer(modifier = Modifier.width(16.dp))
        }
        items(availableCurrencies) { currency ->
            FilterChip(
                selected = selectedCurrency == currency,
                onClick = { onCurrencySelected(currency) },
                label = { Text(currency) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
        item{
            Spacer(modifier = Modifier.width(16.dp))
        }
    }
}
