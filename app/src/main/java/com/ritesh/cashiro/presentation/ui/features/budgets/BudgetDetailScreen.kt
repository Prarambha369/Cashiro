package com.ritesh.cashiro.presentation.ui.features.budgets

import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ritesh.cashiro.presentation.effects.overScrollVertical
import com.ritesh.cashiro.presentation.ui.components.BudgetCard
import com.ritesh.cashiro.presentation.ui.components.CustomTitleTopAppBar
import com.ritesh.cashiro.presentation.ui.components.ListItemPosition
import com.ritesh.cashiro.presentation.ui.components.SectionHeader
import com.ritesh.cashiro.presentation.ui.components.TransactionItem
import com.ritesh.cashiro.presentation.ui.components.toShape
import com.ritesh.cashiro.presentation.ui.features.analytics.CategoryData
import com.ritesh.cashiro.presentation.ui.features.analytics.CategoryPieChart
import com.ritesh.cashiro.presentation.ui.features.categories.CategoriesViewModel
import com.ritesh.cashiro.presentation.ui.features.categories.NavigationContent
import com.ritesh.cashiro.presentation.ui.theme.Spacing
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class,
    ExperimentalMaterial3ExpressiveApi::class
)
@Composable
fun SharedTransitionScope.BudgetDetailScreen(
    budgetId: Long,
    onNavigateBack: () -> Unit,
    onTransactionClick: (Long, String?) -> Unit,
    budgetViewModel: BudgetViewModel = hiltViewModel(),
    categoriesViewModel: CategoriesViewModel = hiltViewModel(),
    animatedContentScope: AnimatedContentScope? = null,
    sharedElementKey: String? = null
) {
    val uiState by budgetViewModel.uiState.collectAsStateWithLifecycle()
    val editBudgetState by budgetViewModel.editBudgetState.collectAsStateWithLifecycle()
    val categories by categoriesViewModel.categories.collectAsStateWithLifecycle()
    val subcategories by categoriesViewModel.subcategories.collectAsStateWithLifecycle()

    var showEditSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Load budget data
    LaunchedEffect(budgetId) {
        budgetViewModel.selectBudget(budgetId)
    }

    // Clean up when leaving the screen
    DisposableEffect(Unit) {
        onDispose {
            budgetViewModel.clearSelectedBudget()
        }
    }

    val budgetWithSpending = uiState.selectedBudget
    val transactions = uiState.selectedBudgetTransactions

    if (showEditSheet) {
        ModalBottomSheet(
            onDismissRequest = { 
                showEditSheet = false
                budgetViewModel.clearEditState()
            },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface,
            dragHandle = { BottomSheetDefaults.DragHandle() }
        ) {
            EditBudgetSheet(
                budgetState = editBudgetState,
                categories = categories,
                subcategoriesMap = subcategories,
                allAccounts = uiState.allAccounts,
                onAmountChange = budgetViewModel::updateBudgetAmount,
                onNameChange = budgetViewModel::updateBudgetName,
                onStartDateChange = budgetViewModel::updateStartDate,
                onEndDateChange = budgetViewModel::updateEndDate,
                onPeriodTypeChange = budgetViewModel::updatePeriodType,
                onTrackTypeChange = budgetViewModel::updateTrackType,
                onBudgetTypeChange = budgetViewModel::updateBudgetType,
                onAccountIdsChange = budgetViewModel::updateAccountIds,
                onColorChange = budgetViewModel::updateColor,
                onAddCategoryLimit = budgetViewModel::addCategoryLimit,
                onRemoveCategoryLimit = budgetViewModel::removeCategoryLimit,
                onSave = {
                    budgetViewModel.saveBudget(
                        onSuccess = {
                            showEditSheet = false
                            budgetViewModel.clearEditState()
                        },
                        onError = { /* TODO: Show error */ }
                    )
                },
                onDelete = {
                    val currentId = budgetWithSpending?.budget?.id
                    if (currentId != null) {
                        budgetViewModel.deleteBudget(
                            budgetId = currentId,
                            onSuccess = {
                                showEditSheet = false
                                budgetViewModel.clearEditState()
                                onNavigateBack()
                            },
                            onError = { /* TODO: Show error */ }
                        )
                    }
                },
                onDismiss = {
                    showEditSheet = false
                    budgetViewModel.clearEditState()
                }
            )
        }
    }

    val hazeState = remember { HazeState() }
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val scrollBehaviorSmall = TopAppBarDefaults.pinnedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            CustomTitleTopAppBar(
                title = budgetWithSpending?.budget?.name ?: "Budget Details",
                hazeState = hazeState,
                scrollBehaviorSmall = scrollBehaviorSmall,
                scrollBehaviorLarge = scrollBehavior,
                hasBackButton = true,
                hasActionButton = true,
                navigationContent = {
                    NavigationContent { onNavigateBack() }
                },
                actionContent = {
                    IconButton(
                        onClick = {
                            budgetWithSpending?.budget?.let {
                                budgetViewModel.initEditBudget(it)
                                showEditSheet = true
                            }
                        },
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainer,
                            contentColor = MaterialTheme.colorScheme.onBackground
                        ),
                        shapes =  IconButtonDefaults.shapes(),
                        modifier = Modifier.padding(end = 16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Edit,
                            contentDescription = "Edit Budget",
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .hazeSource(state = hazeState)
        ) {
            if (budgetWithSpending == null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().overScrollVertical(),
                    contentPadding = PaddingValues(
                        top = paddingValues.calculateTopPadding() + Spacing.md,
                        bottom = 100.dp
                    )
                ) {
                    // Budget Summary Card
                    item {
                        Box(modifier = Modifier.padding(horizontal = Spacing.md)) {
                            BudgetCard(
                                budgetWithSpending = budgetWithSpending,
                                onEditClick = {
                                    budgetViewModel.initEditBudget(budgetWithSpending.budget)
                                    showEditSheet = true
                                },
                                animatedVisibilityScope = animatedContentScope,
                                sharedElementKey = sharedElementKey
                            )
                        }
                        Spacer(modifier = Modifier.height(Spacing.lg))
                    }

                    // Category Breakdown (Pie Chart)
                    if (budgetWithSpending.categorySpending.isNotEmpty()) {
                        item {
                            Spacer(modifier = Modifier.height(Spacing.sm))
                            
                            val pieChartData = budgetWithSpending.categorySpending.map { (category, amount) ->
                                val percentage = if (budgetWithSpending.currentSpending > java.math.BigDecimal.ZERO) {
                                    (amount.toFloat() / budgetWithSpending.currentSpending.toFloat()) * 100f
                                } else 0f
                                
                                val count = transactions.count { it.category == category }
                                
                                CategoryData(
                                    name = category,
                                    amount = amount,
                                    percentage = percentage,
                                    transactionCount = count
                                )
                            }.sortedByDescending { it.amount }

                            CategoryPieChart(
                                categories = pieChartData,
                                currency = budgetWithSpending.budget.currency,
                                modifier = Modifier.padding(end = Spacing.sm)
                            )
                            Spacer(modifier = Modifier.height(Spacing.xl))
                        }
                    }

                    // Transactions List
                    item {
                        SectionHeader(
                            title = "Transactions",
                            modifier = Modifier.padding(horizontal = Spacing.lg)
                        )
                        Spacer(modifier = Modifier.height(Spacing.sm))
                    }

                    if (transactions.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(Spacing.md)
                                    .background(
                                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                                        shape = MaterialTheme.shapes.large
                                    )
                                    .padding(vertical = Spacing.lg),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalAlignment = Alignment.CenterHorizontally,

                                    ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ReceiptLong,
                                        contentDescription = null,
                                        modifier = Modifier.size(56.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(Spacing.md))
                                    Text(
                                        text = "No transactions yet",
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.7f)
                                    )
                                }
                            }
                        }
                    } else {
                        itemsIndexed(
                            items = transactions,
                            key = { _, it -> it.id }
                        ) { index, transaction ->
                            val position = ListItemPosition.from(index, transactions.size)
                            TransactionItem(
                                transaction = transaction,
                                onClick = { onTransactionClick(transaction.id, "budget_txn_${transaction.id}") },
                                modifier = Modifier.padding(horizontal = Spacing.md),
                                animatedContentScope = animatedContentScope,
                                shape = position.toShape(),
                                sharedElementKey = "budget_txn_${transaction.id}"
                            )
                        }
                    }
                }
            }
        }
    }
}
