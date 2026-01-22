package com.ritesh.cashiro.presentation.home

import android.view.HapticFeedbackConstants
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material.icons.automirrored.rounded.LiveHelp
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.automirrored.rounded.Chat
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.MarkChatUnread
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.MarkChatUnread
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material3.*
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import com.ritesh.cashiro.data.database.entity.SubscriptionEntity
import com.ritesh.cashiro.data.database.entity.TransactionEntity
import com.ritesh.cashiro.data.database.entity.TransactionType
import com.ritesh.cashiro.navigation.AccountDetail
import com.ritesh.cashiro.ui.components.AccountCarousel
import com.ritesh.cashiro.ui.components.BalanceCard
import com.ritesh.cashiro.ui.components.BrandIcon
import com.ritesh.cashiro.ui.components.CashiroCard
import com.ritesh.cashiro.ui.components.SectionHeader
import com.ritesh.cashiro.ui.components.SmsParsingProgressDialog
import com.ritesh.cashiro.ui.components.GreetingCard
import com.ritesh.cashiro.ui.components.spotlightTarget
import com.ritesh.cashiro.navigation.UnrecognizedSms
import com.ritesh.cashiro.navigation.Profile
import com.ritesh.cashiro.navigation.NotificationSettings
import com.ritesh.cashiro.ui.components.CustomTitleTopAppBar
import com.ritesh.cashiro.ui.components.ListItem
import com.ritesh.cashiro.ui.components.ListItemPosition
import com.ritesh.cashiro.ui.components.toShape
import com.ritesh.cashiro.ui.components.ListItemCard
import com.ritesh.cashiro.ui.components.SubscriptionIconsStack
import com.ritesh.cashiro.ui.effects.overScrollVertical
import com.ritesh.cashiro.ui.effects.rememberOverscrollFlingBehavior
import com.ritesh.cashiro.ui.theme.*
import com.ritesh.cashiro.utils.CurrencyFormatter
import com.ritesh.cashiro.utils.formatAmount
import com.ritesh.cashiro.R
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import java.math.BigDecimal
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel = hiltViewModel(),
    navController: NavController,
    onNavigateToSettings: () -> Unit = {},
    onNavigateToChat: () -> Unit = {},
    onNavigateToTransactions: () -> Unit = {},
    onNavigateToTransactionsWithSearch: () -> Unit = {},
    onNavigateToSubscriptions: () -> Unit = {},
    onNavigateToAddScreen: () -> Unit = {},
    onTransactionClick: (Long) -> Unit = {},
    onFabPositioned: (Rect) -> Unit = {},
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedContentScope: AnimatedContentScope? = null
) {
    val uiState by viewModel.uiState.collectAsState()
    val deletedTransaction by viewModel.deletedTransaction.collectAsState()
    val smsScanWorkInfo by viewModel.smsScanWorkInfo.collectAsState()
    val activity = LocalActivity.current

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // State for full resync confirmation dialog
    var showFullResyncDialog by remember { mutableStateOf(false) }
    var showMoreBottomSheet by remember { mutableStateOf(false) }

    // Haptic feedback
    val view = LocalView.current

    // Currency dropdown state

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
                onNavigateToSettings = onNavigateToSettings,
                greetingCard = {
                    GreetingCard(
                        userName = uiState.userName,
                        profileImageUri = uiState.profileImageUri,
                        profileBackgroundColor = uiState.profileBackgroundColor,
                        unreadUpdatesCount = uiState.unreadUpdatesCount,
                        onProfileClick = { navController.navigate(Profile) },
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
                            .clickable { navController.navigate(Profile) },
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
                verticalArrangement = Arrangement.spacedBy(Spacing.md)
            ) {
                // Transaction Summary Cards with HorizontalPager
                item {
                    TransactionSummaryCards(
                        uiState = uiState,
                        onCurrencySelected = {
                            viewModel.selectCurrency(it)
                        },
                    )
                }

                // Unified Accounts Section (Credit Cards + Bank Accounts)
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
                                    onClick = onNavigateToSubscriptions,
                                    modifier = cardModifier.sharedBounds(
                                        rememberSharedContentState(key = "upcoming_subscriptions_card"),
                                        animatedVisibilityScope = animatedContentScope,
                                        resizeMode = SharedTransitionScope.ResizeMode.scaleToBounds()
                                    )
                                )
                            }
                        } else {
                            UpcomingSubscriptionsCard(
                                subscriptions = uiState.upcomingSubscriptions,
                                totalAmount = uiState.upcomingSubscriptionsTotal,
                                currency = uiState.upcomingSubscriptionsCurrency,
                                onClick = onNavigateToSubscriptions,
                                modifier = cardModifier
                            )
                        }
                    }
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
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Search,
                                        contentDescription = "Search transactions",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }

                                // View All button
                                TextButton(
                                    onClick = onNavigateToTransactions
                                ) { Text("View All") }
                            }
                        },
                        modifier = Modifier.padding(
                            start = Dimensions.Padding.content + 10.dp,
                            end = Dimensions.Padding.content + 10.dp,
                        )
                    )
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
                        CashiroCard(modifier = Modifier.fillMaxWidth()) {
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
                    items(
                        items = uiState.recentTransactions,
                        key = { it.id }
                    ) { transaction ->
                        SimpleTransactionItem(
                            transaction = transaction,
                            onClick = {
                                onTransactionClick(transaction.id)
                            },
                            modifier = Modifier.padding(
                                start = Dimensions.Padding.content,
                                end = Dimensions.Padding.content,
                            )
                        )
                    }
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
//                        verticalArrangement = Arrangement.spacedBy(1.dp)
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
                                    tint = blue_light
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
                                    tint = red_light
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
                                    tint = orange_light
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
                                    tint = green_light
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

            // Add FAB (bottom end)
            FloatingActionButton(
                onClick = onNavigateToAddScreen,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(Dimensions.Padding.content)
                    .padding(bottom = 72.dp)
                    .then(
                        if (sharedTransitionScope != null && animatedContentScope != null) {
                            with(sharedTransitionScope) {
                                Modifier.sharedBounds(
                                    rememberSharedContentState(key = "fab_to_add"),
                                    animatedVisibilityScope = animatedContentScope,
                                    resizeMode = SharedTransitionScope.ResizeMode.scaleToBounds()
                                )
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
private fun SimpleTransactionItem(
    modifier: Modifier = Modifier,
    transaction: TransactionEntity,
    onClick: () -> Unit = {},
) {
    val amountColor =
        when (transaction.transactionType) {
            TransactionType.INCOME ->
                if (!isSystemInDarkTheme()) income_light else income_dark
            TransactionType.EXPENSE ->
                if (!isSystemInDarkTheme()) expense_light else expense_dark
            TransactionType.CREDIT ->
                if (!isSystemInDarkTheme()) credit_light else credit_dark
            TransactionType.TRANSFER ->
                if (!isSystemInDarkTheme()) transfer_light else transfer_dark
            TransactionType.INVESTMENT ->
                if (!isSystemInDarkTheme()) investment_light else investment_dark
        }

    val dateTimeFormatter = DateTimeFormatter.ofPattern("MMM d • h:mm a")
    val dateTimeText = transaction.dateTime.format(dateTimeFormatter)

    ListItemCard(
        title = transaction.merchantName,
        subtitle = dateTimeText,
        amount = transaction.formatAmount(),
        amountColor = amountColor,
        onClick = onClick,
        leadingContent = {
            BrandIcon(
                merchantName = transaction.merchantName,
                size = 40.dp,
                showBackground = true
            )
        },
        modifier = modifier
    )
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
                modifier = Modifier.padding(end = Spacing.sm)
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CurrencySelectionBottomSheet(
    selectedCurrency: String,
    availableCurrencies: List<String>,
    onCurrencySelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 48.dp)
        ) {
            Text(
                text = "Select Currency",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 16.dp).fillMaxWidth()
            )

            availableCurrencies.forEachIndexed { index, currency ->
                ListItem(
                    headline = {
                        Text(
                            text = when (currency) {
                                "INR" -> "Indian Rupee (₹)"
                                "USD" -> "US Dollar ($)"
                                "AED" -> "UAE Dirham (AED)"
                                "NPR" -> "Nepalese Rupee (₨)"
                                "ETB" -> "Ethiopian Birr (ብር)"
                                else -> currency
                            }
                        )
                    },
                    trailing = {
                        RadioButton(
                            selected = currency == selectedCurrency,
                            onClick = null
                        )
                    },
                    selected = currency == selectedCurrency,
                    onClick = {
                        onCurrencySelected(currency)
                        onDismiss()
                    },
                    shape = ListItemPosition.from(index, availableCurrencies.size).toShape()
                )
            }
        }
    }
}



