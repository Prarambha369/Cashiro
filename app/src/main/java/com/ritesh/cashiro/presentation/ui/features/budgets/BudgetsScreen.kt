package com.ritesh.cashiro.presentation.ui.features.budgets

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ritesh.cashiro.data.repository.BudgetWithSpending
import com.ritesh.cashiro.presentation.ui.features.categories.CategoriesViewModel
import com.ritesh.cashiro.presentation.ui.features.categories.NavigationContent
import com.ritesh.cashiro.presentation.ui.components.BudgetCard
import com.ritesh.cashiro.presentation.ui.components.CustomTitleTopAppBar
import com.ritesh.cashiro.presentation.effects.overScrollVertical
import com.ritesh.cashiro.presentation.effects.rememberOverscrollFlingBehavior
import com.ritesh.cashiro.presentation.ui.theme.Spacing
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.ui.Alignment
import androidx.compose.ui.layout.ContentScale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class,
    ExperimentalMaterial3ExpressiveApi::class
)
@Composable
fun SharedTransitionScope.BudgetsScreen(
    onNavigateBack: () -> Unit,
    budgetViewModel: BudgetViewModel = hiltViewModel(),
    categoriesViewModel: CategoriesViewModel = hiltViewModel(),
    animatedContentScope: AnimatedContentScope? = null,
    sharedElementPrefix: Long? = null
) {
    val uiState by budgetViewModel.uiState.collectAsStateWithLifecycle()
    val editBudgetState by budgetViewModel.editBudgetState.collectAsStateWithLifecycle()
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
                onAmountChange = budgetViewModel::updateBudgetAmount,
                onNameChange = budgetViewModel::updateBudgetName,
                onMonthChange = budgetViewModel::updateBudgetMonth,
                onAddCategoryLimit = budgetViewModel::addCategoryLimit,
                onRemoveCategoryLimit = budgetViewModel::removeCategoryLimit,
                onSave = {
                    budgetViewModel.saveBudget(
                        onSuccess = {
                            showEditSheet = false
                            editingBudgetId = null
                            budgetViewModel.clearEditState()
                        },
                        onError = { /* TODO: Show error */ }
                    )
                },
                onDelete = if (editingBudgetId != null) {
                    {
                        budgetViewModel.deleteBudget(
                            budgetId = editingBudgetId!!,
                            onSuccess = {
                                showEditSheet = false
                                editingBudgetId = null
                                budgetViewModel.clearEditState()
                            },
                            onError = { /* TODO: Show error */ }
                        )
                    }
                } else null,
                onDismiss = {
                    showEditSheet = false
                    editingBudgetId = null
                    budgetViewModel.clearEditState()
                }
            )
        }
    }
    
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val scrollBehaviorSmall = TopAppBarDefaults.pinnedScrollBehavior()
    val lazyListState = rememberLazyListState()
    val hazeState = remember { HazeState() }

    var showFloatingLabel by remember { mutableStateOf(true) }

    LaunchedEffect(lazyListState) {
        snapshotFlow { lazyListState.firstVisibleItemIndex }.collect { firstVisibleItem ->
            // Show the label only when the list is scrolled to the top
            showFloatingLabel = firstVisibleItem == 0
        }
    }


    val sharedModifier = if (animatedContentScope != null && sharedElementPrefix != null) {
        Modifier.sharedBounds(
            rememberSharedContentState(key = "budget_card_$sharedElementPrefix"),
            animatedVisibilityScope = animatedContentScope,
            boundsTransform = { _, _ ->
                spring(
                    stiffness = Spring.StiffnessLow,
                    dampingRatio = Spring.DampingRatioNoBouncy
                )
            },
            resizeMode = SharedTransitionScope.ResizeMode.scaleToBounds(
                contentScale = ContentScale.Inside,
                alignment = Alignment.Center
            ),
        ).skipToLookaheadSize()
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
                    budgetViewModel.initNewBudget()
                    editingBudgetId = null
                    showEditSheet = true
                },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("New Budget") },
                shape = if (showFloatingLabel) MaterialTheme.shapes.extraLargeIncreased else MaterialTheme.shapes.large
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
                            budgetViewModel.initNewBudget()
                            editingBudgetId = null
                            showEditSheet = true
                        }
                    )
                }
                
                else -> {
                    var lastClickTime by remember { mutableLongStateOf(0L) }
                    BudgetsList(
                        lazyListState = lazyListState,
                        paddingValues = paddingValues,
                        budgets = uiState.budgets,
                        onEditClick = { budgetId ->
                            val currentTime = System.currentTimeMillis()
                            if (currentTime - lastClickTime > 500) { // Debounce 500ms
                                lastClickTime = currentTime
                                val budget = uiState.budgets.find { it.budget.id == budgetId }?.budget
                                if (budget != null) {
                                    budgetViewModel.initEditBudget(budget)
                                    editingBudgetId = budgetId
                                    showEditSheet = true
                                }
                            }
                        },
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
private fun SharedTransitionScope.BudgetsList(
    budgets: List<BudgetWithSpending>,
    onEditClick: (Long) -> Unit,
    paddingValues: PaddingValues,
    lazyListState: LazyListState,
    animatedContentScope: AnimatedContentScope? = null,
    sharedElementPrefix: Long? = null
) {
    val isTransitioning = animatedContentScope?.transition?.let { 
        it.currentState != it.targetState 
    } ?: false

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
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        items(
            items = budgets,
            key = { it.budget.id }
        ) { budgetWithSpending ->
            BudgetCard(
                budgetWithSpending = budgetWithSpending,
                onEditClick = { onEditClick(budgetWithSpending.budget.id) },
                animatedVisibilityScope = animatedContentScope,
                sharedElementKey = if (sharedElementPrefix != null) null else "budget_card_${budgetWithSpending.budget.id}"
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
