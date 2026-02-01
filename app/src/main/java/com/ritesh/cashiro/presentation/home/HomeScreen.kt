package com.ritesh.cashiro.presentation.home

import android.app.Activity
import android.view.HapticFeedbackConstants
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.MarkChatUnread
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.ritesh.cashiro.data.preferences.NavigationBarStyle
import com.ritesh.cashiro.ui.viewmodel.ThemeViewModel
import coil3.compose.AsyncImage
import com.ritesh.cashiro.R
import com.ritesh.cashiro.data.database.entity.CategoryEntity
import com.ritesh.cashiro.data.database.entity.SubcategoryEntity
import com.ritesh.cashiro.data.database.entity.SubscriptionEntity
import com.ritesh.cashiro.navigation.AccountDetail
import com.ritesh.cashiro.navigation.NotificationSettings
import com.ritesh.cashiro.navigation.UnrecognizedSms
import com.ritesh.cashiro.ui.components.AccountCarousel
import com.ritesh.cashiro.ui.components.BalanceCard
import com.ritesh.cashiro.ui.components.BudgetCarousel
import com.ritesh.cashiro.ui.components.CashiroCard
import com.ritesh.cashiro.ui.components.CurrencySelectionBottomSheet
import com.ritesh.cashiro.ui.components.CustomTitleTopAppBar
import com.ritesh.cashiro.ui.components.GreetingCard
import com.ritesh.cashiro.ui.components.ListItem
import com.ritesh.cashiro.ui.components.ListItemPosition
import com.ritesh.cashiro.ui.components.SectionHeader
import com.ritesh.cashiro.ui.components.SmsParsingProgressDialog
import com.ritesh.cashiro.ui.components.SubscriptionIconsStack
import com.ritesh.cashiro.ui.components.TransactionItem
import com.ritesh.cashiro.ui.components.toShape
import com.ritesh.cashiro.ui.effects.overScrollVertical
import com.ritesh.cashiro.ui.effects.rememberOverscrollFlingBehavior
import com.ritesh.cashiro.ui.theme.Dimensions
import com.ritesh.cashiro.ui.theme.Spacing
import com.ritesh.cashiro.ui.theme.expense_dark
import com.ritesh.cashiro.ui.theme.expense_light
import com.ritesh.cashiro.ui.theme.income_dark
import com.ritesh.cashiro.ui.theme.income_light
import com.ritesh.cashiro.utils.CurrencyFormatter
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel = hiltViewModel(),
    themeViewModel: ThemeViewModel = hiltViewModel(),
    navController: NavController,
    onNavigateToSettings: () -> Unit = {},
    onNavigateToChat: () -> Unit = {},
    onNavigateToTransactions: () -> Unit = {},
    onNavigateToTransactionsWithSearch: () -> Unit = {},
    onNavigateToSubscriptions: () -> Unit = {},
    onNavigateToAddScreen: () -> Unit = {},
    onNavigateToBudgets: (Long?) -> Unit = {},
    onTransactionClick: (Long, String) -> Unit = { _, _ -> },
    onFabPositioned: (Rect) -> Unit = {},
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedContentScope: AnimatedContentScope? = null
) {

    val uiState by viewModel.uiState.collectAsState()
    val deletedTransaction by viewModel.deletedTransaction.collectAsState()
    val smsScanWorkInfo by viewModel.smsScanWorkInfo.collectAsState()
    val categoriesMap by viewModel.categoriesMap.collectAsStateWithLifecycle()
    val subcategoriesMap by viewModel.subcategoriesMap.collectAsStateWithLifecycle()
    val themeUiState by themeViewModel.themeUiState.collectAsStateWithLifecycle()
    val activity = LocalActivity.current

    val snackbarHostState = remember { SnackbarHostState() }
    
    var lastBackPressTime by remember { mutableStateOf(0L) }
    val context = androidx.compose.ui.platform.LocalContext.current
    
    BackHandler {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastBackPressTime < 2000) {
            (context as? Activity)?.finish()
        } else {
            lastBackPressTime = currentTime
            Toast.makeText(context, "Press back again to close the app", Toast.LENGTH_SHORT).show()
        }
    }
    val scope = rememberCoroutineScope()

    // State for full resync confirmation dialog
    var showFullResyncDialog by remember { mutableStateOf(false) }
    var showMoreBottomSheet by remember { mutableStateOf(false) }

    // Haptic feedback
    val view = LocalView.current


    // Check for app updates and reviews when the screen is first displayed
    LaunchedEffect(Unit) {
        // Refresh account balances to ensure proper currency conversion
        viewModel.refreshAccountBalances()

        // Check for app updates
        activity?.let {
            val componentActivity = it as ComponentActivity
            viewModel.checkForAppUpdate(
                activity = componentActivity,
                snackbarHostState = snackbarHostState,
                scope = scope
            )

            // Check for in-app review eligibility
            viewModel.checkForInAppReview(componentActivity)
        }
    }

    // Refresh hidden accounts whenever this screen becomes visible
    // This ensures changes from ManageAccountsScreen are reflected immediately
    DisposableEffect(Unit) {
        viewModel.refreshHiddenAccounts()
        onDispose {}
    }

    // Handle delete undo snackbar
    LaunchedEffect(deletedTransaction) {
        deletedTransaction?.let { transaction ->
            // Clear the state immediately to prevent re-triggering
            viewModel.clearDeletedTransaction()

            scope.launch {
                val result =
                    snackbarHostState.showSnackbar(
                        message = "Transaction deleted",
                        actionLabel = "Undo",
                        duration = SnackbarDuration.Short
                    )
                if (result == SnackbarResult.ActionPerformed) {
                    // Pass the transaction directly since state is already
                    // cleared
                    viewModel.undoDeleteTransaction(transaction)
                }
            }
        }
    }

    // Clear snackbar when navigating away
    DisposableEffect(Unit) { onDispose { snackbarHostState.currentSnackbarData?.dismiss() } }

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val scrollBehaviorSmall = TopAppBarDefaults.pinnedScrollBehavior()
    val hazeState = remember { HazeState() }
    val lazyListState = rememberLazyListState()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = Color.Transparent,
        topBar = {
            CustomTitleTopAppBar(
                title = "Cashiro",
                scrollBehaviorSmall = scrollBehaviorSmall,
                scrollBehaviorLarge = scrollBehavior,
                hazeState = hazeState,
                hasBackButton = false,
                extraInfoCard = {
                    GreetingCard(
                        userName = uiState.userName,
                        profileImageUri = uiState.profileImageUri,
                        profileBackgroundColor = uiState.profileBackgroundColor,
                        unreadUpdatesCount = uiState.unreadUpdatesCount,
                        onProfileClick = { onNavigateToSettings() },
                        onNotificationClick = { navController.navigate(NotificationSettings) },
                        onMoreClick = { showMoreBottomSheet = true },
                        onUpdatesClick = { navController.navigate(UnrecognizedSms) }
                    )
                },
                navigationContent = {
                    Box(
                        modifier = Modifier
                            .padding(start = 16.dp)
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(uiState.profileBackgroundColor)
                            .clickable { onNavigateToSettings() },
                        contentAlignment = Alignment.Center
                    ) {
                        if (uiState.profileImageUri != null) {
                            AsyncImage(
                                model = uiState.profileImageUri,
                                contentDescription = "Profile",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Image(
                                painter = painterResource(id = R.drawable.avatar_1),
                                contentDescription = "Profile",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }
                },
                actionContent = {
                    Box(
                        modifier =
                            Modifier.padding(end = 16.dp)
                                .size(40.dp)
                                .background(
                                    color = MaterialTheme.colorScheme.surfaceContainer,
                                    shape = CircleShape
                                )
                                .clickable(
                                    onClick = { showMoreBottomSheet = true },
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.MoreHoriz,
                            contentDescription = "More options",
                            tint = MaterialTheme.colorScheme.inverseSurface,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.TopCenter
            ) {
                // Banner Image Background
                AnimatedVisibility(
                    visible = uiState.showBannerImage,
                    enter = fadeIn() + slideInVertically(initialOffsetY = { -it }),
                    exit = fadeOut() + slideOutVertically(targetOffsetY = { -it }),
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(300.dp)
                            .align(Alignment.TopCenter)
                    ) {
                        if (uiState.bannerImageUri != null) {
                            AsyncImage(
                                model = uiState.bannerImageUri,
                                contentDescription = "Banner",
                                modifier = Modifier.fillMaxSize().alpha(0.5f),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Image(
                                painter = painterResource(id = R.drawable.banner_bg_image),
                                contentDescription = "Banner",
                                modifier = Modifier.fillMaxSize().alpha(0.5f),
                                contentScale = ContentScale.Crop
                            )
                        }
                        Spacer(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .height(100.dp)
                                .fillMaxWidth()
                                .background(
                                    Brush.verticalGradient(
                                        listOf(
                                            Color.Transparent,
                                            MaterialTheme.colorScheme.background
                                        )
                                    )
                                )
                        )
                    }
                }
            }

            LazyColumn(
                state = lazyListState,
                modifier = Modifier
                    .fillMaxSize()
                    .hazeSource(state = hazeState)
                    .overScrollVertical(),
                flingBehavior = rememberOverscrollFlingBehavior { lazyListState },
                contentPadding =
                    PaddingValues(
                        top = Dimensions.Padding.content + paddingValues.calculateTopPadding(),
                        bottom = 0.dp
                    ),
            ) {
                // Transaction Summary Cards
                item {
                    TransactionSummaryCards(
                        uiState = uiState,
                        onCurrencySelected = {
                            viewModel.selectCurrency(it)
                        },
                    )
                }
                item{
                    Spacer(Modifier.height(Spacing.md))
                }

                // Budgets Carousel
                if (uiState.activeBudgets.isNotEmpty()) {
                    item {
                       BudgetCarousel(
                            budgets = uiState.activeBudgets,
                            onBudgetClick = { onNavigateToBudgets(it) },
                            onEditClick = { onNavigateToBudgets(it) },
                            modifier = Modifier.padding(bottom = Spacing.md),
                            sharedTransitionScope = sharedTransitionScope,
                            animatedVisibilityScope = animatedContentScope
                        )
                    }
                }

                if (uiState.creditCards.isNotEmpty() ||
                    uiState.accountBalances.isNotEmpty()
                ) {
                    item {
                        AccountCarousel(
                            creditCards = uiState.creditCards,
                            bankAccounts = uiState.accountBalances,
                            onAccountClick = { bankName, accountLast4 ->
                                navController.navigate(
                                    AccountDetail(
                                        bankName = bankName,
                                        accountLast4 = accountLast4
                                    )
                                )
                            },
                            sharedTransitionScope = sharedTransitionScope,
                            animatedContentScope = animatedContentScope
                        )
                    }
                }
                item{
                    Spacer(Modifier.height(Spacing.md))
                }
                // Upcoming Subscriptions Alert
                if (uiState.upcomingSubscriptions.isNotEmpty()) {
                    item {
                        val cardModifier = Modifier.padding(
                            start = Dimensions.Padding.content,
                            end = Dimensions.Padding.content,
                        )
                        
                        if (sharedTransitionScope != null && animatedContentScope != null) {
                            with(sharedTransitionScope) {
                                UpcomingSubscriptionsCard(
                                    subscriptions = uiState.upcomingSubscriptions,
                                    totalAmount = uiState.upcomingSubscriptionsTotal,
                                    currency = uiState.upcomingSubscriptionsCurrency,
                                    categoriesMap = categoriesMap,
                                    subcategoriesMap = subcategoriesMap,
                                    onClick = onNavigateToSubscriptions,
                                    modifier = cardModifier.sharedBounds(
                                        rememberSharedContentState(key = "upcoming_subscriptions_card"),
                                        animatedVisibilityScope = animatedContentScope,
                                        boundsTransform = { _, _ ->
                                            spring(
                                                stiffness = Spring.StiffnessLow,
                                                dampingRatio = Spring.DampingRatioNoBouncy
                                            )
                                        },
                                        resizeMode = SharedTransitionScope.ResizeMode.scaleToBounds(
                                            contentScale = ContentScale.Fit,
                                            alignment = Alignment.Center
                                        )
                                    )
                                    .skipToLookaheadSize()
                                )
                            }
                        } else {
                            UpcomingSubscriptionsCard(
                                subscriptions = uiState.upcomingSubscriptions,
                                totalAmount = uiState.upcomingSubscriptionsTotal,
                                currency = uiState.upcomingSubscriptionsCurrency,
                                categoriesMap = categoriesMap,
                                subcategoriesMap = subcategoriesMap,
                                onClick = onNavigateToSubscriptions,
                                modifier = cardModifier
                            )
                        }
                    }
                }
                item{
                    Spacer(Modifier.height(Spacing.md))
                }

                // Recent Transactions Section
                item {
                    SectionHeader(
                        title = "Recent",
                        action = {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Search button
                                IconButton(
                                    onClick = onNavigateToTransactionsWithSearch,
                                    modifier = Modifier.size(36.dp).then(
                                        if (sharedTransitionScope != null && animatedContentScope != null) {
                                            with(sharedTransitionScope) {
                                                Modifier.sharedBounds(
                                                    rememberSharedContentState(key = "transactions_search"),
                                                    animatedVisibilityScope = animatedContentScope,
                                                    boundsTransform = { _, _ ->
                                                        spring(
                                                            stiffness = Spring.StiffnessLow,
                                                            dampingRatio = Spring.DampingRatioNoBouncy
                                                        )
                                                    },
                                                    resizeMode = SharedTransitionScope.ResizeMode.scaleToBounds(
                                                        contentScale = ContentScale.None,
                                                        alignment = Alignment.Center
                                                    )
                                                )
                                                .skipToLookaheadSize()
                                            }
                                        } else Modifier
                                    )
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Search,
                                        contentDescription = "Search transactions",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }

                                // View All button
                                TextButton(
                                    onClick = onNavigateToTransactions,
                                    modifier = Modifier.then(
                                        if (sharedTransitionScope != null && animatedContentScope != null) {
                                            with(sharedTransitionScope) {
                                                Modifier.sharedBounds(
                                                    rememberSharedContentState(key = "transactions_screen"),
                                                    animatedVisibilityScope = animatedContentScope,
                                                    boundsTransform = { _, _ ->
                                                        spring(
                                                            stiffness = Spring.StiffnessLow,
                                                            dampingRatio = Spring.DampingRatioNoBouncy
                                                        )
                                                    },
                                                    resizeMode = SharedTransitionScope.ResizeMode.scaleToBounds(
                                                        contentScale = ContentScale.None,
                                                        alignment = Alignment.Center
                                                    )
                                                )
                                                .skipToLookaheadSize()
                                            }
                                        } else Modifier
                                    )
                                ) { Text("View All") }
                            }
                        },
                        modifier = Modifier.padding(
                            start = Dimensions.Padding.content + 10.dp,
                            end = Dimensions.Padding.content + 10.dp,
                        )
                    )
                }

                item{
                    Spacer(Modifier.height(Spacing.sm))
                }

                if (uiState.isLoading) {
                    item {
                        Box(
                            modifier = Modifier
                                .padding(
                                    start = Dimensions.Padding.content,
                                    end = Dimensions.Padding.content
                                )
                                .fillMaxWidth()
                                .height(Dimensions.Component.minTouchTarget * 2),
                            contentAlignment = Alignment.Center
                        ) { CircularProgressIndicator() }
                    }
                } else if (uiState.recentTransactions.isEmpty()) {
                    item {
                        CashiroCard(
                            modifier = Modifier
                                .padding(
                                    start = Dimensions.Padding.content,
                                    end = Dimensions.Padding.content
                                )
                                .fillMaxWidth()
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(Dimensions.Padding.empty),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No transactions yet",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                } else {
                    itemsIndexed(
                        items = uiState.recentTransactions,
                        key = { _, it -> it.id }
                    ) { index, transaction ->
                        val categoryEntity = categoriesMap[transaction.category]
                        val subcategoryEntity = if (categoryEntity != null && transaction.subcategory != null) {
                            subcategoriesMap[transaction.subcategory]
                        } else null
                        val position = ListItemPosition.from(index, uiState.recentTransactions.size)

                        TransactionItem(
                            transaction = transaction,
                            categoryEntity = categoryEntity,
                            subcategoryEntity = subcategoryEntity,
                            onClick = {
                                onTransactionClick(transaction.id, "home_transaction_${transaction.id}")
                            },
                            shape = position.toShape(),
                            modifier = Modifier.padding(
                                start = Dimensions.Padding.content,
                                end = Dimensions.Padding.content,
                            ),
                            sharedTransitionScope = sharedTransitionScope,
                            animatedContentScope = animatedContentScope,
                            sharedElementKey = "home_transaction_${transaction.id}"
                        )
                    }
                }
                item{
                    Spacer(Modifier.height(200.dp))
                }
            }

            // More Options BottomSheet
            if (showMoreBottomSheet) {
                ModalBottomSheet(
                    onDismissRequest = { showMoreBottomSheet = false },
                    sheetState = rememberModalBottomSheetState(),
                    containerColor = MaterialTheme.colorScheme.surface,
                    dragHandle = { BottomSheetDefaults.DragHandle() }
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 32.dp)
                            .padding(horizontal = 16.dp),
                    ) {
                        Text(
                            text = "More Options",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(bottom = Spacing.sm).fillMaxWidth()
                        )


                        // Settings Option
                        ListItem(
                            headline = { Text("Settings") },
                            leading = {
                                Icon(
                                    imageVector = Icons.Outlined.Settings,
                                    contentDescription = null,
                                )
                            },
                            onClick = {
                                showMoreBottomSheet = false
                                onNavigateToSettings()
                            },
                            shape = ListItemPosition.Top.toShape()
                        )

                        // Ask AI Option
                        ListItem(
                            headline = { Text("Ask AI") },
                            leading = {
                                Icon(
                                    imageVector = Icons.Outlined.MarkChatUnread,
                                    contentDescription = null,
                                )
                            },
                            onClick = {
                                showMoreBottomSheet = false
                                onNavigateToChat()
                            },
                            shape = ListItemPosition.Middle.toShape()
                        )

                        // Sync SMS Option
                        ListItem(
                            headline = { Text("Sync SMS") },
                            leading = {
                                Icon(
                                    imageVector = Icons.Outlined.Sync,
                                    contentDescription = null,
                                )
                            },
                            supporting = { Text("Long press for full resync") },
                            modifier = Modifier.pointerInput(Unit) {
                                detectTapGestures(
                                    onTap = {
                                        showMoreBottomSheet = false
                                        viewModel.scanSmsMessages()
                                    },
                                    onLongPress = {
                                        view.performHapticFeedback(
                                            HapticFeedbackConstants.LONG_PRESS
                                        )
                                        showMoreBottomSheet = false
                                        showFullResyncDialog = true
                                    }
                                )
                            },
                            onClick = null,
                            shape = ListItemPosition.Middle.toShape()
                        )

                        // Banner Image Toggle
                        ListItem(
                            headline = { Text("Show Banner Image") },
                            leading = {
                                Icon(
                                    imageVector = Icons.Outlined.Image,
                                    contentDescription = null,
                                )
                            },
                            trailing = {
                                Switch(
                                    checked = uiState.showBannerImage,
                                    onCheckedChange = { viewModel.toggleBannerImage() }
                                )
                            },
                            shape = ListItemPosition.Bottom.toShape()
                        )
                    }
                }
            }

            // FABs Container
            Column(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(Dimensions.Padding.content)
                    .padding(
                        bottom = if (themeUiState.navigationBarStyle == NavigationBarStyle.FLOATING) 72.dp else 100.dp
                    ),
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(Spacing.sm)
            ) {
                // Sync FAB
                SmallFloatingActionButton(
                    onClick = {
                        view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                        viewModel.scanSmsMessages()
                    },
                    modifier = Modifier.pointerInput(Unit) {
                        detectTapGestures(
                            onLongPress = {
                                view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                                showFullResyncDialog = true
                            }
                        )
                    },
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                ) {
                    Icon(
                        imageVector = Icons.Default.Sync,
                        contentDescription = "Sync SMS transactions",
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Add FAB
                FloatingActionButton(
                    onClick = onNavigateToAddScreen,
                    modifier = Modifier
                        .then(
                            if (sharedTransitionScope != null && animatedContentScope != null) {
                                with(sharedTransitionScope) {
                                    Modifier.sharedBounds(
                                        rememberSharedContentState(key = "fab_to_add"),
                                        animatedVisibilityScope = animatedContentScope,
                                        boundsTransform = { _, _ ->
                                            spring(
                                                stiffness = Spring.StiffnessLow,
                                                dampingRatio = Spring.DampingRatioLowBouncy
                                            )
                                        },
                                        resizeMode = SharedTransitionScope.ResizeMode.scaleToBounds(
                                            contentScale = ContentScale.FillBounds,
                                            alignment = Alignment.Center
                                        )
                                    )
                                    .skipToLookaheadSize()
                                }
                            } else Modifier
                        ),
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add Transaction or Subscription",
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            // Full Resync Confirmation Dialog
            if (showFullResyncDialog) {
                AlertDialog(
                    onDismissRequest = { showFullResyncDialog = false },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Sync,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    title = { Text("Full Resync") },
                    text = {
                        Text(
                            "This will reprocess all SMS messages from scratch. " +
                                    "Use this to fix issues caused by updated bank parsers.\n\n" +
                                    "This may take a few seconds depending on your message history."
                        )
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                showFullResyncDialog = false
                                viewModel.scanSmsMessages(
                                    forceResync = true
                                )
                            }
                        ) { Text("Resync All") }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = { showFullResyncDialog = false }
                        ) { Text("Cancel") }
                    }
                )
            }

            // SMS Parsing Progress Dialog
            SmsParsingProgressDialog(
                isVisible = uiState.isScanning,
                workInfo = smsScanWorkInfo,
                onDismiss = { viewModel.cancelSmsScan() },
                onCancel = { viewModel.cancelSmsScan() }
            )

            // Breakdown Dialog
            if (uiState.showBreakdownDialog) {
                BreakdownDialog(
                    currentMonthIncome = uiState.currentMonthIncome,
                    currentMonthExpenses = uiState.currentMonthExpenses,
                    currentMonthTotal = uiState.currentMonthTotal,
                    lastMonthIncome = uiState.lastMonthIncome,
                    lastMonthExpenses = uiState.lastMonthExpenses,
                    lastMonthTotal = uiState.lastMonthTotal,
                    onDismiss = { viewModel.hideBreakdownDialog() }
                )
            }
        }
    }

}




@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BreakdownDialog(
    currentMonthIncome: BigDecimal,
    currentMonthExpenses: BigDecimal,
    currentMonthTotal: BigDecimal,
    lastMonthIncome: BigDecimal,
    lastMonthExpenses: BigDecimal,
    lastMonthTotal: BigDecimal,
    onDismiss: () -> Unit
) {
    val now = LocalDate.now()
    val currentPeriod = "${now.month.name.lowercase().replaceFirstChar { it.uppercase() }} 1-${now.dayOfMonth}"
    val lastMonth = now.minusMonths(1)
    val lastPeriod = "${lastMonth.month.name.lowercase().replaceFirstChar { it.uppercase() }} 1-${now.dayOfMonth}"

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.md), // Reduced horizontal padding for wider modal
            colors =
                CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(Dimensions.Padding.card),
                verticalArrangement = Arrangement.spacedBy(Spacing.md)
            ) {
                // Title
                Text(
                    text = "Calculation Breakdown",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )

                // Current Period Section
                Text(
                    text = currentPeriod,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )

                BreakdownRow(
                    label = "Income",
                    amount = currentMonthIncome,
                    isIncome = true
                )

                BreakdownRow(
                    label = "Expenses",
                    amount = currentMonthExpenses,
                    isIncome = false
                )

                HorizontalDivider()

                BreakdownRow(
                    label = "Net Balance",
                    amount = currentMonthTotal,
                    isIncome = currentMonthTotal >= BigDecimal.ZERO,
                    isBold = true
                )

                Spacer(modifier = Modifier.height(Spacing.sm))

                // Last Period Section
                Text(
                    text = lastPeriod,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )

                BreakdownRow(
                    label = "Income",
                    amount = lastMonthIncome,
                    isIncome = true
                )

                BreakdownRow(
                    label = "Expenses",
                    amount = lastMonthExpenses,
                    isIncome = false
                )

                HorizontalDivider()

                BreakdownRow(
                    label = "Net Balance",
                    amount = lastMonthTotal,
                    isIncome = lastMonthTotal >= BigDecimal.ZERO,
                    isBold = true
                )

                // Formula explanation
                Spacer(modifier = Modifier.height(Spacing.sm))
                Card(
                    colors =
                        CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Formula: Income - Expenses = Net Balance\n" +
                                    "Green (+) = Savings | Red (-) = Overspending",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(Spacing.sm),
                        textAlign = TextAlign.Center
                    )
                }

                // Close button
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End)
                ) { Text("Close") }
            }
        }
    }
}

@Composable
private fun BreakdownRow(
    label: String,
    amount: BigDecimal,
    isIncome: Boolean,
    isBold: Boolean = false
) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal
        )
        Text(
            text = "${if (isIncome) "+" else "-"}${CurrencyFormatter.formatCurrency(amount.abs())}",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal,
            color =
                if (isIncome) {
                    if (!isSystemInDarkTheme()) income_light else income_dark
                } else {
                    if (!isSystemInDarkTheme()) expense_light else expense_dark
                }
        )
    }
}

@Composable
private fun UpcomingSubscriptionsCard(
    modifier: Modifier = Modifier,
    subscriptions: List<SubscriptionEntity>,
    totalAmount: BigDecimal,
    currency: String,
    categoriesMap: Map<String, CategoryEntity> = emptyMap(),
    subcategoriesMap: Map<String, SubcategoryEntity> = emptyMap(),
    onClick: () -> Unit = {},
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        onClick = onClick,
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer
            ),
        shape = RoundedCornerShape(Spacing.xxl)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(Dimensions.Padding.content),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Default.CalendarToday,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.size(Dimensions.Icon.medium)
                )
                Column {
                    Text(
                        text = "${subscriptions.size} active subscriptions",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Text(
                        text =
                            "Monthly total: ${CurrencyFormatter.formatCurrency(totalAmount, currency)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(
                            alpha = Dimensions.Alpha.subtitle
                        )
                    )
                }
            }
            SubscriptionIconsStack(
                subscriptions = subscriptions,
                iconSize = 38.dp,
                modifier = Modifier.padding(end = Spacing.sm),
                categoriesMap = categoriesMap,
                subcategoriesMap = subcategoriesMap
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TransactionSummaryCards(
    uiState: HomeUiState,
    onCurrencySelected: (String) -> Unit = {},
) {
    val now = LocalDate.now()
    val lastMonth = now.minusMonths(1)
    val periodLabel = "vs ${
        lastMonth.month.name.lowercase().replaceFirstChar { it.uppercase() }
    } 1-${now.dayOfMonth}"

    val expenseChange = uiState.currentMonthExpenses - uiState.lastMonthExpenses

    val comparisonMessage = when {
        uiState.currentMonthExpenses == BigDecimal.ZERO && uiState.lastMonthExpenses == BigDecimal.ZERO -> {
            "No transactions yet"
        }
        expenseChange > BigDecimal.ZERO -> {
            "${CurrencyFormatter.formatCurrency(expenseChange.abs(), uiState.selectedCurrency)} more $periodLabel"
        }
        expenseChange < BigDecimal.ZERO -> {
            "${CurrencyFormatter.formatCurrency(expenseChange.abs(), uiState.selectedCurrency)} less $periodLabel"
        }
        uiState.monthlyChange > BigDecimal.ZERO && uiState.currentMonthTotal > BigDecimal.ZERO -> {
            "Saved ${
                CurrencyFormatter.formatCurrency(
                    uiState.monthlyChange.abs(),
                    uiState.selectedCurrency
                )
            } more $periodLabel"
        }
        else -> {
            "Same as last period"
        }
    }

    var showCurrencySheet by remember { mutableStateOf(false) }

    if (showCurrencySheet) {
        CurrencySelectionBottomSheet(
            selectedCurrency = uiState.selectedCurrency,
            availableCurrencies = uiState.availableCurrencies,
            onCurrencySelected = {
                onCurrencySelected(it)
                showCurrencySheet = false
            },
            onDismiss = { showCurrencySheet = false }
        )
    }

    Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {

        BalanceCard(
            totalBalance = uiState.totalBalance,
            monthlyChange = uiState.monthlyChange,
            currency = uiState.selectedCurrency,
            subtitle = comparisonMessage,
            transfersAmount = CurrencyFormatter.formatCurrency(
                uiState.currentMonthTransfer,
                uiState.selectedCurrency
            ),
            investmentsAmount = CurrencyFormatter.formatCurrency(
                uiState.currentMonthInvestment,
                uiState.selectedCurrency
            ),
            onTransfersClick = {
//                navController.navigate("transactions?type=TRANSFER")
                /* Show Breakdown or something else */
            },
            onInvestmentsClick = {
//                navController.navigate("transactions?type=INVESTMENT")
                /* Show Breakdown or something else */
            },
            onHandleClick = { /* Show Breakdown or something else */ },
            availableCurrenciesCount = uiState.availableCurrencies.size,
            onCurrencyClick = { showCurrencySheet = true },
            modifier = Modifier.padding(
                start = Dimensions.Padding.content,
                end = Dimensions.Padding.content,
            )
        )
    }
}





