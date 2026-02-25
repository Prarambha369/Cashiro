package com.ritesh.cashiro.presentation.ui.features.transactions

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Deselect
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.outlined.FilterAlt
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.ritesh.cashiro.presentation.common.TimePeriod
import com.ritesh.cashiro.presentation.common.TransactionTypeFilter
import com.ritesh.cashiro.presentation.effects.BlurredAnimatedVisibility
import com.ritesh.cashiro.presentation.effects.overScrollVertical
import com.ritesh.cashiro.presentation.effects.rememberOverscrollFlingBehavior
import com.ritesh.cashiro.presentation.ui.components.CashiroCard
import com.ritesh.cashiro.presentation.ui.components.CategoryChip
import com.ritesh.cashiro.presentation.ui.components.CollapsibleFilterRow
import com.ritesh.cashiro.presentation.ui.components.CurrencySelectionBottomSheet
import com.ritesh.cashiro.presentation.ui.components.CustomTitleTopAppBar
import com.ritesh.cashiro.presentation.ui.components.DateRangePickerDialog
import com.ritesh.cashiro.presentation.ui.components.DeleteMultipleTransactionsDialog
import com.ritesh.cashiro.presentation.ui.components.ListItemPosition
import com.ritesh.cashiro.presentation.ui.components.LoadingCircle
import com.ritesh.cashiro.presentation.ui.components.SearchBarBox
import com.ritesh.cashiro.presentation.ui.components.SectionHeader
import com.ritesh.cashiro.presentation.ui.components.TransactionItem
import com.ritesh.cashiro.presentation.ui.components.TransactionTotalsCard
import com.ritesh.cashiro.presentation.ui.components.toShape
import com.ritesh.cashiro.presentation.ui.features.categories.NavigationContent
import com.ritesh.cashiro.presentation.ui.theme.Dimensions
import com.ritesh.cashiro.presentation.ui.theme.Spacing
import com.ritesh.cashiro.utils.DateRangeUtils
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class,
    ExperimentalMaterial3ExpressiveApi::class
)
@Composable
fun SharedTransitionScope.TransactionsScreen(
    initialCategory: String? = null,
    initialMerchant: String? = null,
    initialPeriod: String? = null,
    initialCurrency: String? = null,
    initialType: String? = null,
    focusSearch: Boolean = false,
    transactionsViewModel: TransactionsViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit = {},
    onTransactionClick: (Long, String) -> Unit = { _, _ -> },
    onNavigateToSettings: () -> Unit = {},
    animatedContentScope: AnimatedVisibilityScope? = null,
    blurEffects: Boolean
) {
    val uiState by transactionsViewModel.uiState.collectAsState()
    val searchQuery by transactionsViewModel.searchQuery.collectAsState()
    val selectedPeriod by transactionsViewModel.selectedPeriod.collectAsState()
    val categoryFilter by transactionsViewModel.categoryFilter.collectAsState()
    val transactionTypeFilter by transactionsViewModel.transactionTypeFilter.collectAsState()
    val deletedTransaction by transactionsViewModel.deletedTransaction.collectAsState()
    val categoriesMap by transactionsViewModel.categories.collectAsState()
    val subcategoriesMap by transactionsViewModel.subcategories.collectAsState()
    val accountsMap by transactionsViewModel.accountsMap.collectAsState()
    val filteredTotals by transactionsViewModel.filteredTotals.collectAsState()
    val currencyGroupedTotals by transactionsViewModel.currencyGroupedTotals.collectAsState()
    val availableCurrencies by transactionsViewModel.availableCurrencies.collectAsState()
    val selectedCurrency by transactionsViewModel.selectedCurrency.collectAsState()
    val sortOption by transactionsViewModel.sortOption.collectAsState()
    val smsScanMonths by transactionsViewModel.smsScanMonths.collectAsState()
    val customDateRange by transactionsViewModel.customDateRange.collectAsState()
    val selectionMode by transactionsViewModel.selectionMode.collectAsState()
    val selectedTransactionIds by transactionsViewModel.selectedTransactionIds.collectAsState()
    val deletedTransactions by transactionsViewModel.deletedTransactions.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var showAdvancedFilters by remember { mutableStateOf(false) }
    var showSortMenu by remember { mutableStateOf(false) }
    var showDateRangePicker by remember { mutableStateOf(false) }
    var showCurrencySheet by remember { mutableStateOf(false) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }

    val hazeState = remember { HazeState() }

    
    // Focus management for search field
    val searchFocusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val scrollBehaviorSmall = TopAppBarDefaults.pinnedScrollBehavior()
    val scrollBehaviorLarge = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    var searchTextFieldValue by remember {
        mutableStateOf(
            TextFieldValue(
                text = searchQuery,
                selection = TextRange(searchQuery.length)
            )
        )
    }

    // Sync with external updates (like initial filters)
    LaunchedEffect(searchQuery) {
        if (searchQuery != searchTextFieldValue.text) {
            searchTextFieldValue = searchTextFieldValue.copy(
                text = searchQuery,
                selection = TextRange(searchQuery.length)
            )
        }
    }
    
    // Calculate active filter count for advanced filters
    val activeFilterCount = listOf(
        transactionTypeFilter != TransactionTypeFilter.ALL,
        categoryFilter != null
    ).count { it }

    // Remember scroll position across navigation
    val listState = rememberSaveable(saver = LazyListState.Saver) {
        LazyListState()
    }

    // Cache expensive operations
    val timePeriods = remember { TimePeriod.entries }
    val customRangeLabel = remember(customDateRange) {
        DateRangeUtils.formatDateRange(customDateRange)
    }
    
    // Apply initial filters only once when screen is first created
    LaunchedEffect(Unit) {
        transactionsViewModel.applyInitialFilters(
            initialCategory,
            initialMerchant,
            initialPeriod,
            initialCurrency,
            initialType
        )
        // Delay heavy data loading until transition finished for smoothness
        delay(500)
        transactionsViewModel.startLoading()
    }

    // Apply navigation filters when navigation parameters change (for deep links)
    LaunchedEffect(initialCategory, initialMerchant, initialPeriod, initialCurrency, initialType) {
        if (initialCategory != null || initialMerchant != null || initialPeriod != null || initialCurrency != null || initialType != null) {
            transactionsViewModel.applyNavigationFilters(
                initialCategory,
                initialMerchant,
                initialPeriod,
                initialCurrency,
                initialType
            )
        }
    }
    
    // Handle delete undo snackbar
    LaunchedEffect(deletedTransaction) {
        deletedTransaction?.let { transaction ->
            // Clear the state immediately to prevent re-triggering
            transactionsViewModel.clearDeletedTransaction()
            
            scope.launch {
                val result = snackbarHostState.showSnackbar(
                    message = "Transaction deleted",
                    actionLabel = "Undo",
                    duration = SnackbarDuration.Short
                )
                if (result == SnackbarResult.ActionPerformed) {
                    // Pass the transaction directly since state is already cleared
                    transactionsViewModel.undoDeleteTransaction(transaction)
                }
            }
        }
    }
    
    // Handle bulk delete undo snackbar
    LaunchedEffect(deletedTransactions) {
        deletedTransactions?.let { transactions ->
            // Clear the state immediately to prevent re-triggering
            transactionsViewModel.clearDeletedTransactions()
            
            scope.launch {
                val result = snackbarHostState.showSnackbar(
                    message = "${transactions.size} transactions deleted",
                    actionLabel = "Undo",
                    duration = SnackbarDuration.Short
                )
                if (result == SnackbarResult.ActionPerformed) {
                    transactionsViewModel.undoDeleteTransactions(transactions)
                }
            }
        }
    }
    
    // Focus search field if requested
    LaunchedEffect(focusSearch) {
        if (focusSearch) {
            searchFocusRequester.requestFocus()
            keyboardController?.show()
        }
    }
    
    // Clear snackbar when navigating away
    DisposableEffect(Unit) {
        onDispose {
            snackbarHostState.currentSnackbarData?.dismiss()
        }
    }
    
    // Handle back button in selection mode
    BackHandler(enabled = selectionMode) {
        transactionsViewModel.toggleSelectionMode()
    }
    
    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehaviorLarge.nestedScrollConnection)
            .then(
                if (animatedContentScope != null) {
                    Modifier.sharedBounds(
                        rememberSharedContentState(
                            key = if (initialCategory != null) {
                                "category_$initialCategory"
                            } else if (initialMerchant != null) {
                                "merchant_$initialMerchant"
                            } else {
                                "transactions_screen"
                            }
                        ),
                        animatedVisibilityScope = animatedContentScope,
                        boundsTransform = { _, _ ->
                            spring(
                                stiffness = Spring.StiffnessLow,
                                dampingRatio = Spring.DampingRatioLowBouncy
                            )
                        },
                        resizeMode = SharedTransitionScope.ResizeMode.scaleToBounds(ContentScale.Fit, Alignment.Center)
                    )
                } else Modifier
            ),
        topBar = {
            CustomTitleTopAppBar(
                title = if (selectionMode) "${selectedTransactionIds.size} selected" else "Transactions",
                scrollBehaviorSmall = scrollBehaviorSmall,
                scrollBehaviorLarge = scrollBehaviorLarge,
                hazeState = hazeState,
                hasBackButton = true,
                navigationContent = {
                    NavigationContent {
                        if (selectionMode) {
                            transactionsViewModel.toggleSelectionMode()
                        } else {
                            onNavigateBack()
                        }
                    } },
                actionContent = {
                    BlurredAnimatedVisibility(selectionMode) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
                        ) {
                            // Select All/Deselect All toggle button
                            val allSelected =
                                selectedTransactionIds.size == uiState.transactions.size && uiState.transactions.isNotEmpty()
                            IconButton(
                                onClick = {
                                    if (allSelected) {
                                        transactionsViewModel.clearSelection()
                                    } else {
                                        transactionsViewModel.selectAllTransactions()
                                    }
                                },
                                colors = IconButtonDefaults.iconButtonColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                                    contentColor = MaterialTheme.colorScheme.onBackground
                                ),
                                shapes =  IconButtonDefaults.shapes(),
                            ) {
                                Icon(
                                    imageVector = if (allSelected) Icons.Default.Deselect else Icons.Default.SelectAll,
                                    contentDescription = if (allSelected) "Deselect All" else "Select All",
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            // Delete button
                            IconButton(
                                onClick = { showDeleteConfirmation = true },
                                enabled = selectedTransactionIds.isNotEmpty(),
                                colors = IconButtonDefaults.iconButtonColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                                    contentColor = if (selectedTransactionIds.isNotEmpty())
                                        MaterialTheme.colorScheme.error
                                    else MaterialTheme.colorScheme.error.copy(alpha = 0.5f)
                                ),
                                shapes =  IconButtonDefaults.shapes(),
                                modifier = Modifier.padding(end = 16.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete Selected",
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            )
        },
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState,
                snackbar = {
                    Snackbar(
                        snackbarData = it,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        shape = MaterialTheme.shapes.large,
                    )
                }
            ) },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .hazeSource(state = hazeState)
                .padding(top = paddingValues.calculateTopPadding())
        ) {
            // Search Bar with Sort Button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Dimensions.Padding.content)
                    .padding(top = Spacing.md),
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SearchBarBox(
                    modifier = Modifier
                        .weight(1f)
                        .focusRequester(searchFocusRequester)
                        .then(Modifier),
                    searchQuery = searchTextFieldValue,
                    onSearchQueryChange = {
                        searchTextFieldValue = it
                        transactionsViewModel.updateSearchQuery(it.text)
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    trailingIcon = {
                        Row{
                            BlurredAnimatedVisibility(searchTextFieldValue.text.isNotEmpty()) {
                                IconButton(onClick = {
                                    searchTextFieldValue = TextFieldValue("")
                                    transactionsViewModel.updateSearchQuery("")
                                }) {
                                    Icon(
                                        imageVector = Icons.Default.Clear,
                                        contentDescription = "Clear search",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            // Sort button
                            Box {
                                IconButton(
                                    onClick = { showSortMenu = true },
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(MaterialTheme.shapes.medium)
                                        .background(Color.Transparent)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.FilterAlt,
                                        contentDescription = "Sort",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                DropdownMenu(
                                    expanded = showSortMenu,
                                    onDismissRequest = { showSortMenu = false }
                                ) {
                                    SortOption.entries.forEach { option ->
                                        DropdownMenuItem(
                                            text = {
                                                Row(
                                                    horizontalArrangement = Arrangement.spacedBy(
                                                        Spacing.sm),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    RadioButton(
                                                        selected = sortOption == option,
                                                        onClick = null,
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                    Text(option.label)
                                                }
                                            },
                                            onClick = {
                                                transactionsViewModel.setSortOption(option)
                                                showSortMenu = false
                                            }
                                        )
                                    }
                                }
                            }
                        }

                    },
                    label = {
                        Text(
                            text = if (categoryFilter != null) "Search in $categoryFilter..."
                            else "Search transactions...",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.5f)
                        )
                    }
                )
            }

            // Period Filter Chips - Always visible
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = Spacing.sm),
                contentPadding = PaddingValues(horizontal = Dimensions.Padding.content),
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
            ) {
                // Period filter chips
                items(timePeriods) { period ->
                    FilterChip(
                        // Only show CUSTOM as selected if both period is CUSTOM AND dates are set
                        selected = if (period == TimePeriod.CUSTOM) {
                            selectedPeriod == period && customDateRange != null
                        } else {
                            selectedPeriod == period
                        },
                        onClick = {
                            if (period == TimePeriod.CUSTOM) {
                                showDateRangePicker = true
                                // Don't change selectedPeriod until user confirms dates
                            } else {
                                transactionsViewModel.selectPeriod(period)
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
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow.copy(0.7f),
                            labelColor = MaterialTheme.colorScheme.onSurface
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            borderWidth = 0.dp,
                            selected = if (period == TimePeriod.CUSTOM) {
                                selectedPeriod == period && customDateRange != null
                            } else {
                                selectedPeriod == period
                            },
                            enabled = true
                        ),
                    )
                }
            }

            // Data scope info banner
            if (transactionsViewModel.isShowingLimitedData()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Dimensions.Padding.content, vertical = Spacing.xs),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f)
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .padding(Spacing.md)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.size(Dimensions.Icon.small)
                        )
                        Spacer(modifier = Modifier.width(Spacing.sm))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Showing last $smsScanMonths months of SMS data",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Text(
                                text = "Adjust in Settings to scan more history",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                            )
                        }
                        TextButton(
                            onClick = onNavigateToSettings,
                            contentPadding = PaddingValues(horizontal = Spacing.sm)
                        ) {
                            Text("Settings", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }

            // Collapsible Advanced Filters
            CollapsibleFilterRow(
                isExpanded = showAdvancedFilters,
                activeFilterCount = activeFilterCount,
                onToggle = { showAdvancedFilters = !showAdvancedFilters },
                modifier = Modifier.fillMaxWidth()
            ) {
                // Transaction Type Filter Chips
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = Dimensions.Padding.content),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
                ) {
                    items(TransactionTypeFilter.entries) { typeFilter ->
                        FilterChip(
                            selected = transactionTypeFilter == typeFilter,
                            onClick = { transactionsViewModel.setTransactionTypeFilter(typeFilter) },
                            label = { Text(typeFilter.label) },
                            leadingIcon = if (transactionTypeFilter == typeFilter) {
                                {
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
                                        else -> null
                                    }
                                }
                            } else null,
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.tertiaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onTertiaryContainer,
                                containerColor = MaterialTheme.colorScheme.surfaceContainerLow.copy(0.7f),
                                labelColor = MaterialTheme.colorScheme.onSurface
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                borderWidth = 0.dp,
                                selected = transactionTypeFilter == typeFilter,
                                enabled = true
                            ),
                        )
                    }
                }
            }

            // Transaction List
            when {
                uiState.isLoading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(Dimensions.Padding.content),
                        contentAlignment = Alignment.Center
                    ) {
                        LoadingCircle()
                    }
                }
                uiState.transactions.isEmpty() -> {
                    EmptyTransactionsState(
                        searchQuery = searchQuery,
                        selectedPeriod = selectedPeriod
                    )
                }
                else -> {


                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxSize()
                            .overScrollVertical(),
                        flingBehavior = rememberOverscrollFlingBehavior { listState },
                        contentPadding = PaddingValues(
                            horizontal = Dimensions.Padding.content,
                            vertical = Spacing.md
                        ),
                    ) {
                        item {
                            // Totals Card - Moved after filters
                            TransactionTotalsCard(
                                income = filteredTotals.income,
                                expenses = filteredTotals.expenses,
                                netBalance = filteredTotals.netBalance,
                                currency = selectedCurrency,
                                availableCurrenciesCount = availableCurrencies.size,
                                onCurrencyClick = { showCurrencySheet = true },
                                isLoading = uiState.isLoading,
                            )

                            // Category Filter Chip
                            categoryFilter?.let { category ->
                                LazyRow(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = Spacing.xs),
                                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
                                ) {
                                    item {
                                        FilterChip(
                                            selected = true,
                                            onClick = { /* No action on click, use trailing icon to clear */ },
                                            label = { Text(category) },
                                            leadingIcon = {
                                                categoriesMap[category]?.let { categoryEntity ->
                                                    CategoryChip(
                                                        category = categoryEntity,
                                                        showText = false,
                                                        modifier = Modifier.padding(start = 4.dp)
                                                    )
                                                }
                                            },
                                            trailingIcon = {
                                                IconButton(
                                                    onClick = { transactionsViewModel.clearCategoryFilter() },
                                                    modifier = Modifier.size(18.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Close,
                                                        contentDescription = "Clear category filter",
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                            },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                                                selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                                containerColor = MaterialTheme.colorScheme.surfaceContainerLow.copy(0.7f),
                                                labelColor = MaterialTheme.colorScheme.onSurface
                                            ),
                                            modifier = Modifier
                                        )
                                    }
                                }
                            }
                        }
                        // Iterate through date groups in order
                        listOf(
                            DateGroup.TODAY,
                            DateGroup.YESTERDAY,
                            DateGroup.THIS_WEEK,
                            DateGroup.EARLIER
                        ).forEach { dateGroup ->
                            uiState.groupedTransactions[dateGroup]?.let { transactions ->
                                // Date group header
                                item {
                                    SectionHeader(
                                        title = dateGroup.label,
                                        modifier = Modifier.padding(top = Spacing.md, bottom = Spacing.sm, start = Spacing.md)
                                    )
                                }

                                // Transactions in this group
                                itemsIndexed(
                                    items = transactions,
                                    key = { _, it -> it.id }
                                ) { index, transaction ->
                                    val position = ListItemPosition.from(index, transactions.size)
                                    TransactionItem(
                                        transaction = transaction,
                                        categoryEntity = categoriesMap[transaction.category],
                                        subcategoryEntity = transaction.subcategory?.let { subcategoriesMap[it] },
                                        accountIconResId = accountsMap["${transaction.bankName}_${transaction.accountNumber}"]?.iconResId ?: 0,
                                        accountColorHex = accountsMap["${transaction.bankName}_${transaction.accountNumber}"]?.color,
                                        showDate = dateGroup == DateGroup.EARLIER,
                                        shape = position.toShape(),
                                        onClick = { onTransactionClick(transaction.id, "transaction_${transaction.id}") },
                                        animatedContentScope = animatedContentScope,
                                        sharedElementKey = "transaction_${transaction.id}",
                                        isSelectionMode = selectionMode,
                                        isSelected = selectedTransactionIds.contains(transaction.id),
                                        onSelectionToggle = { transactionsViewModel.toggleTransactionSelection(transaction.id) },
                                        onLongClick = {
                                            if (!selectionMode) {
                                                transactionsViewModel.toggleSelectionMode()
                                                transactionsViewModel.toggleTransactionSelection(transaction.id)
                                            }
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
    
    // Export Dialog

    if (showDateRangePicker) {
        DateRangePickerDialog(
            onDismiss = { showDateRangePicker = false },
            onConfirm = { startDate, endDate ->
                transactionsViewModel.setCustomDateRange(startDate, endDate)
                showDateRangePicker = false
            },
            initialStartDate = customDateRange?.first,
            initialEndDate = customDateRange?.second,
            blurEffects = blurEffects,
            hazeState = hazeState
        )
    }

    if (showCurrencySheet) {
        CurrencySelectionBottomSheet(
            selectedCurrency = selectedCurrency,
            availableCurrencies = availableCurrencies,
            onCurrencySelected = {
                transactionsViewModel.selectCurrency(it)
                showCurrencySheet = false
            },
            onDismiss = { showCurrencySheet = false }
        )
    }
    
    // Delete confirmation dialog
    if (showDeleteConfirmation) {
        DeleteMultipleTransactionsDialog(
            onDelete = {
                transactionsViewModel.deleteSelectedTransactions()
                showDeleteConfirmation = false
            },
            onDismiss = { showDeleteConfirmation = false },
            selectedTransactionIds = selectedTransactionIds,
            blurEffects = blurEffects,
            hazeState = hazeState
        )
    }
}




@Composable
private fun EmptyTransactionsState(
    searchQuery: String,
    selectedPeriod: TimePeriod
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(Dimensions.Padding.content),
        contentAlignment = Alignment.Center
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
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ReceiptLong,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(Spacing.md))
                Text(
                    text = when {
                        searchQuery.isNotEmpty() -> "No transactions matching \"$searchQuery\""
                        selectedPeriod != TimePeriod.ALL -> "No transactions for ${selectedPeriod.label.lowercase()}"
                        else -> "No transactions yet"
                    },
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (searchQuery.isEmpty() && selectedPeriod == TimePeriod.ALL) {
                    Spacer(modifier = Modifier.height(Spacing.xs))
                    Text(
                        text = "Sync your SMS to see transactions",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
