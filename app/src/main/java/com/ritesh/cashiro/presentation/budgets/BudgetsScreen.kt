package com.ritesh.cashiro.presentation.budgets

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ritesh.cashiro.data.repository.BudgetWithSpending
import com.ritesh.cashiro.presentation.categories.CategoriesViewModel
import com.ritesh.cashiro.presentation.categories.NavigationContent
import com.ritesh.cashiro.ui.components.BudgetCard
import com.ritesh.cashiro.ui.components.CustomTitleTopAppBar
import com.ritesh.cashiro.ui.effects.overScrollVertical
import com.ritesh.cashiro.ui.effects.rememberOverscrollFlingBehavior
import com.ritesh.cashiro.ui.theme.Spacing
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource

import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.ui.layout.ContentScale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
fun BudgetsScreen(
    onNavigateBack: () -> Unit,
    viewModel: BudgetViewModel = hiltViewModel(),
    categoriesViewModel: CategoriesViewModel = hiltViewModel(),

    sharedTransitionScope: SharedTransitionScope? = null,
    animatedContentScope: AnimatedContentScope? = null,
    sharedElementPrefix: Long? = null
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val editBudgetState by viewModel.editBudgetState.collectAsStateWithLifecycle()
    val categories by categoriesViewModel.categories.collectAsStateWithLifecycle()
    val subcategories by categoriesViewModel.subcategories.collectAsStateWithLifecycle()
    
    var showEditSheet by remember { mutableStateOf(false) }
    var editingBudgetId by remember { mutableStateOf<Long?>(null) }
    
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    
    // Edit budget sheet
    if (showEditSheet) {
        ModalBottomSheet(
            onDismissRequest = { 
                showEditSheet = false
                editingBudgetId = null
                viewModel.clearEditState()
            },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface,
            dragHandle = { BottomSheetDefaults.DragHandle() }
        ) {
            EditBudgetSheet(
                budgetState = editBudgetState,
                categories = categories,
                subcategoriesMap = subcategories,
                onAmountChange = viewModel::updateBudgetAmount,
                onNameChange = viewModel::updateBudgetName,
                onMonthChange = viewModel::updateBudgetMonth,
                onAddCategoryLimit = viewModel::addCategoryLimit,
                onRemoveCategoryLimit = viewModel::removeCategoryLimit,
                onSave = {
                    viewModel.saveBudget(
                        onSuccess = {
                            showEditSheet = false
                            editingBudgetId = null
                            viewModel.clearEditState()
                        },
                        onError = { /* TODO: Show error */ }
                    )
                },
                onDelete = if (editingBudgetId != null) {
                    {
                        viewModel.deleteBudget(
                            budgetId = editingBudgetId!!,
                            onSuccess = {
                                showEditSheet = false
                                editingBudgetId = null
                                viewModel.clearEditState()
                            },
                            onError = { /* TODO: Show error */ }
                        )
                    }
                } else null,
                onDismiss = {
                    showEditSheet = false
                    editingBudgetId = null
                    viewModel.clearEditState()
                }
            )
        }
    }
    
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val scrollBehaviorSmall = TopAppBarDefaults.pinnedScrollBehavior()
    val lazyListState = rememberLazyListState()
    val hazeState = remember { HazeState() }


    val sharedModifier = if (sharedTransitionScope != null && animatedContentScope != null && sharedElementPrefix != null) {
        with(sharedTransitionScope) {
            Modifier.sharedBounds(
                rememberSharedContentState(key = "budget_card_$sharedElementPrefix"),
                animatedVisibilityScope = animatedContentScope,
                boundsTransform = { _, _ ->
                    androidx.compose.animation.core.spring(
                        stiffness = androidx.compose.animation.core.Spring.StiffnessLow,
                        dampingRatio = androidx.compose.animation.core.Spring.DampingRatioNoBouncy
                    )
                },
                resizeMode = SharedTransitionScope.ResizeMode.scaleToBounds(
                    contentScale = ContentScale.Fit,
                    alignment = Alignment.Center
                ),
                zIndexInOverlay = -1000f,
            )
        }
    } else {
        Modifier
    }

    Scaffold(
        modifier = Modifier
            .nestedScroll(scrollBehavior.nestedScrollConnection)
            .then(sharedModifier),
        topBar = {
            CustomTitleTopAppBar(
                title = "Budgets",
                hazeState = hazeState,
                scrollBehaviorSmall = scrollBehaviorSmall,
                scrollBehaviorLarge = scrollBehavior,
                hasBackButton = true,
                navigationContent = {
                    NavigationContent { onNavigateBack() }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    viewModel.initNewBudget()
                    editingBudgetId = null
                    showEditSheet = true
                },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("New Budget") }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier.fillMaxSize()
                .hazeSource(state = hazeState)
        ) {
            when {
                uiState.isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                
                uiState.budgets.isEmpty() -> {
                    EmptyBudgetsContent(
                        onCreateBudget = {
                            viewModel.initNewBudget()
                            editingBudgetId = null
                            showEditSheet = true
                        }
                    )
                }
                
                else -> {
                    BudgetsList(
                        lazyListState = lazyListState,
                        hazeState = hazeState,
                        paddingValues = paddingValues,
                        budgets = uiState.budgets,
                        onBudgetClick = { budgetId ->
                            val budget = uiState.budgets.find { it.budget.id == budgetId }?.budget
                            if (budget != null) {
                                viewModel.initEditBudget(budget)
                                editingBudgetId = budgetId
                                showEditSheet = true
                            }
                        },
                        onEditClick = { budgetId ->
                            val budget = uiState.budgets.find { it.budget.id == budgetId }?.budget
                            if (budget != null) {
                                viewModel.initEditBudget(budget)
                                editingBudgetId = budgetId
                                showEditSheet = true
                            }
                        },
                        sharedTransitionScope = sharedTransitionScope,
                        animatedContentScope = animatedContentScope,
                        sharedElementPrefix = sharedElementPrefix
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun BudgetsList(
    budgets: List<BudgetWithSpending>,
    onBudgetClick: (Long) -> Unit,
    onEditClick: (Long) -> Unit,
    paddingValues: PaddingValues,
    lazyListState: LazyListState,
    hazeState: HazeState,
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedContentScope: AnimatedContentScope? = null,
    sharedElementPrefix: Long? = null
) {
    LazyColumn(
        state = lazyListState,
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .overScrollVertical(),
        flingBehavior = rememberOverscrollFlingBehavior { lazyListState },
        contentPadding = PaddingValues(
            start = Spacing.md,
            end = Spacing.md,
            top = Spacing.md + paddingValues.calculateTopPadding(),
            bottom = 100.dp // Space for FAB
        ),
        verticalArrangement = Arrangement.spacedBy(Spacing.md)
    ) {
        items(
            items = budgets,
            key = { it.budget.id }
        ) { budgetWithSpending ->
            BudgetCard(
                budgetWithSpending = budgetWithSpending,
                onClick = { onBudgetClick(budgetWithSpending.budget.id) },
                onEditClick = { onEditClick(budgetWithSpending.budget.id) },
                sharedTransitionScope = sharedTransitionScope,
                animatedVisibilityScope = animatedContentScope,
                sharedElementKey = if (budgetWithSpending.budget.id == sharedElementPrefix) null else "budget_card_${budgetWithSpending.budget.id}"
            )
        }
    }
}

@Composable
private fun EmptyBudgetsContent(
    onCreateBudget: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(Spacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "🎯",
            style = MaterialTheme.typography.displayLarge
        )
        
        Spacer(modifier = Modifier.height(Spacing.md))
        
        Text(
            text = "No Budgets Yet",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
        
        Spacer(modifier = Modifier.height(Spacing.xs))
        
        Text(
            text = "Create your first budget to start tracking your spending goals",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = Spacing.xl)
        )
        
        Spacer(modifier = Modifier.height(Spacing.lg))
        
        Button(onClick = onCreateBudget) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Create Budget")
        }
    }
}
