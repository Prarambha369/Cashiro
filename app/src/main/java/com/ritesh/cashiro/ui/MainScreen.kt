package com.ritesh.cashiro.ui

import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TonalToggleButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.ritesh.cashiro.data.preferences.NavigationBarStyle
import com.ritesh.cashiro.navigation.AddTransaction
import com.ritesh.cashiro.navigation.AnimatedNavHost
import com.ritesh.cashiro.navigation.Subscriptions
import com.ritesh.cashiro.navigation.TransactionDetail
import com.ritesh.cashiro.navigation.Transactions
import com.ritesh.cashiro.navigation.navPage
import com.ritesh.cashiro.presentation.accounts.AddAccountScreen
import com.ritesh.cashiro.presentation.accounts.ManageAccountsScreen
import com.ritesh.cashiro.presentation.categories.CategoriesScreen
import com.ritesh.cashiro.presentation.home.HomeScreen
import com.ritesh.cashiro.presentation.home.HomeViewModel
import com.ritesh.cashiro.presentation.navigation.BottomNavItem
import com.ritesh.cashiro.presentation.profile.ProfileScreen
import com.ritesh.cashiro.navigation.Budgets
import com.ritesh.cashiro.ui.components.SpotlightTutorial
import com.ritesh.cashiro.ui.effects.BlurredAnimatedVisibility
import com.ritesh.cashiro.ui.screens.analytics.AnalyticsScreen
import com.ritesh.cashiro.ui.screens.chat.ChatScreen
import com.ritesh.cashiro.ui.screens.rules.CreateRuleScreen
import com.ritesh.cashiro.ui.screens.rules.RulesScreen
import com.ritesh.cashiro.ui.screens.settings.AppearanceScreen
import com.ritesh.cashiro.ui.screens.settings.DeveloperScreen
import com.ritesh.cashiro.ui.screens.settings.FAQScreen
import com.ritesh.cashiro.ui.screens.settings.NotificationScreen
import com.ritesh.cashiro.ui.screens.settings.SMSScreen
import com.ritesh.cashiro.ui.screens.settings.SettingsScreen
import com.ritesh.cashiro.ui.screens.unrecognized.UnrecognizedSmsScreen
import com.ritesh.cashiro.ui.viewmodel.RulesViewModel
import com.ritesh.cashiro.ui.viewmodel.SpotlightViewModel
import com.ritesh.cashiro.ui.viewmodel.ThemeViewModel

@OptIn(
        ExperimentalMaterial3Api::class,
        ExperimentalMaterial3ExpressiveApi::class,
        ExperimentalSharedTransitionApi::class
)
@Composable
fun MainScreen(
        rootNavController: NavHostController? = null,
        navController: NavHostController = rememberNavController(),
        themeViewModel: ThemeViewModel = hiltViewModel(),
        spotlightViewModel: SpotlightViewModel = hiltViewModel(),
        sharedTransitionScope: SharedTransitionScope? = null,
        animatedContentScope: AnimatedContentScope? = null
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val currentRoute = currentDestination?.route
    val spotlightState by spotlightViewModel.spotlightState.collectAsState()

    val navigationItems =
            listOf(BottomNavItem.Home, BottomNavItem.Analytics, BottomNavItem.Chat)
    
    val themeUiState by themeViewModel.themeUiState.collectAsState()

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            bottomBar = {
                BlurredAnimatedVisibility(
                    visible = themeUiState.navigationBarStyle == NavigationBarStyle.NORMAL &&
                            currentRoute in listOf("home", "analytics"),
                    enter = fadeIn() + slideInVertically(initialOffsetY = { it }),
                    exit = fadeOut() + slideOutVertically(targetOffsetY = { it }),
                ) {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                        tonalElevation = 2.dp
                    ) {
                        navigationItems.forEach { item ->
                            val selected = currentDestination?.hierarchy?.any { it.route == item.route } == true
                            NavigationBarItem(
                                selected = selected,
                                onClick = {
                                    navController.navigate(item.route) {
                                        popUpTo(navController.graph.startDestinationId) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                icon = { 
                                    Icon(
                                        imageVector = item.icon, 
                                        contentDescription = item.title,
                                        tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    ) 
                                },
                                label = { 
                                    Text(
                                        text = item.title,
                                        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        style = MaterialTheme.typography.labelMedium
                                    ) 
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    indicatorColor = MaterialTheme.colorScheme.primaryContainer
                                )
                            )
                        }
                    }
                }
            }
        ) { paddingValues ->
            Box(modifier = Modifier.fillMaxSize()) {
                AnimatedNavHost(
                    navController = navController,
                    startDestination = "home",
                    modifier = Modifier
                        .padding(
                            start = paddingValues.calculateLeftPadding(LayoutDirection.Ltr),
                            end = paddingValues.calculateRightPadding(LayoutDirection.Rtl)
                        ),
                    pages = arrayOf(
                        navPage("home") {
                            val homeViewModel: HomeViewModel = hiltViewModel()
                            HomeScreen(
                                viewModel = homeViewModel,
                                navController = rootNavController ?: navController,
                                onNavigateToSettings = { navController.navigate("settings") },
                                onNavigateToChat = { navController.navigate("chat") },
                                onNavigateToTransactions = { rootNavController?.navigate(Transactions()) },
                                onNavigateToTransactionsWithSearch = { rootNavController?.navigate(
                                    Transactions(focusSearch = true)) },
                                onNavigateToSubscriptions = { rootNavController?.navigate(Subscriptions) },
                                onNavigateToAddScreen = { rootNavController?.navigate(AddTransaction()) },
                                onNavigateToBudgets = { id -> rootNavController?.navigate(Budgets(sharedElementPrefix = id)) },
                                onTransactionClick = { transactionId, key ->
                                    rootNavController?.navigate(
                                        TransactionDetail(transactionId, key)
                                    ) },
                                onFabPositioned = { position ->
                                    spotlightViewModel.updateFabPosition(position) },
                                sharedTransitionScope = sharedTransitionScope,
                                animatedContentScope = animatedContentScope
                            ) },
                        navPage("analytics") {
                            AnalyticsScreen(
                                onNavigateToTransactions = {
                                    category,
                                    merchant,
                                    period,
                                    currency ->
                                    rootNavController?.navigate(
                                        Transactions(
                                            category = category,
                                            merchant = merchant,
                                            period = period,
                                            currency = currency
                                        )
                                    )
                                },
                                sharedTransitionScope = sharedTransitionScope,
                                animatedContentScope = animatedContentScope
                            )
                        },
                        navPage("chat") {
                            ChatScreen(
                                modifier = Modifier.imePadding(),
                                onNavigateToSettings = {
                                    navController.navigate("settings")
                                }
                            )
                        },
                        navPage("settings") {
                            SettingsScreen(
                                themeViewModel = themeViewModel,
                                onNavigateBack = {
                                    navController.popBackStack()
                                },
                                onNavigateToCategories = {
                                    navController.navigate("categories")
                                },
                                onNavigateToUnrecognizedSms = {
                                    navController.navigate("unrecognized_sms")
                                },
                                onNavigateToManageAccounts = {
                                    navController.navigate("manage_accounts")
                                },
                                onNavigateToFaq = {
                                    navController.navigate("faq")
                                },
                                onNavigateToRules = {
                                    navController.navigate("rules")
                                },
                                onNavigateToAppearance = {
                                    navController.navigate("appearance")
                                },
                                onNavigateToProfile = {
                                    navController.navigate("profile")
                                },
                                onNavigateToSms = {
                                    navController.navigate("sms_settings")
                                },
                                onNavigateToNotifications = {
                                    navController.navigate("notification_settings")
                                },
                                onNavigateToBudgets = {
                                    rootNavController?.navigate(Budgets())
                                },
                                onNavigateToDeveloper = {
                                    navController.navigate("developer_options")
                                }
                            )
                        },
                        navPage("developer_options") {
                            DeveloperScreen(
                                onNavigateBack = {
                                    navController.popBackStack()
                                }
                            )
                        },
                        navPage("sms_settings") {
                            SMSScreen(
                                onNavigateBack = {
                                    navController.popBackStack()
                                },
                                onNavigateToUnrecognizedSms = {
                                    navController.navigate("unrecognized_sms")
                                }
                            )
                        },
                        navPage("categories") {
                            CategoriesScreen(
                                onNavigateBack = {
                                    navController.popBackStack()
                                }
                            )
                        },
                        navPage("unrecognized_sms") {
                            UnrecognizedSmsScreen(
                                onNavigateBack = {
                                    navController.popBackStack()
                                }
                            )
                        },
                        navPage("faq") {
                            FAQScreen(
                                onNavigateBack = {
                                    navController.popBackStack()
                                }
                            )
                        },
                        navPage("manage_accounts") {
                            ManageAccountsScreen(
                                onNavigateBack = {
                                    navController.popBackStack()
                                },
                                onNavigateToAddAccount = {
                                    navController.navigate("add_account")
                                }
                            )
                        },
                        navPage("add_account") {
                            AddAccountScreen(
                                onNavigateBack = {
                                    navController.popBackStack()
                                }
                            )
                        },
                        navPage("rules") {
                            RulesScreen(
                                onNavigateBack = {
                                    navController.popBackStack()
                                },
                                onNavigateToCreateRule = {
                                    navController.navigate("create_rule")
                                }
                            )
                        },
                        navPage("create_rule") {
                            val rulesViewModel: RulesViewModel = hiltViewModel()
                            CreateRuleScreen(
                                onNavigateBack = {
                                    navController.popBackStack()
                                },
                                onSaveRule = { rule ->
                                    rulesViewModel.createRule(rule)
                                    navController.popBackStack()
                                }
                            )
                        },
                        navPage("appearance") {
                            AppearanceScreen(
                                onNavigateBack = {
                                    navController.popBackStack()
                                },
                                themeViewModel = themeViewModel
                            )
                        },
                        navPage("profile") {
                            ProfileScreen(
                                onNavigateBack = {
                                    navController.popBackStack()
                                },
                            )
                        },
                        navPage("notification_settings") {
                            NotificationScreen(
                                onNavigateBack = {
                                    navController.popBackStack()
                                }
                            )
                        }
                    )
                )

                // HorizontalFloatingToolbar
                BlurredAnimatedVisibility(
                    visible = themeUiState.navigationBarStyle == NavigationBarStyle.FLOATING &&
                    currentRoute in listOf("home", "analytics"),
                    enter = fadeIn() + slideInVertically(initialOffsetY = { it }),
                    exit = fadeOut() + slideOutVertically(targetOffsetY = { it }),
                    modifier = Modifier.align(Alignment.BottomCenter)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter)
                            .background(
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        MaterialTheme.colorScheme.surface
                                    )
                                )
                            ),
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        HorizontalFloatingToolbar(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .navigationBarsPadding()
                                .shadow(
                                    elevation = 16.dp,
                                    shape = MaterialTheme.shapes.extraLarge
                                )
                                .zIndex(1000f),
                            expanded = true,
                        ) {
                            navigationItems.forEach { item ->
                                val selected =
                                    currentDestination?.hierarchy?.any {
                                        it.route == item.route
                                    } == true

                                TonalToggleButton(
                                    checked = selected,
                                    onCheckedChange = {
                                        navController.navigate(item.route) {
                                            popUpTo(navController.graph.startDestinationId) {
                                                saveState = true
                                            }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    },
                                    modifier = Modifier.padding(horizontal = 4.dp)
                                ) {
                                    Icon(imageVector = item.icon, contentDescription = item.title)
                                    AnimatedVisibility(
                                        visible = selected,
                                        enter = fadeIn() + expandHorizontally(MaterialTheme.motionScheme.fastSpatialSpec()),
                                        exit = fadeOut() + shrinkHorizontally(MaterialTheme.motionScheme.fastSpatialSpec())
                                    ) {
                                        Text(
                                            text = item.title,
                                            modifier = Modifier.padding(start = 8.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Spotlight Tutorial overlay - outside Scaffold to overlay everything
        if (currentRoute == "home" &&
            spotlightState.showTutorial &&
            spotlightState.fabPosition != null
        ) {
            val homeViewModel: HomeViewModel? =
                navController.currentBackStackEntry?.let { hiltViewModel(it) }

            SpotlightTutorial(
                isVisible = true,
                targetPosition = spotlightState.fabPosition,
                message = "Tap here to scan your SMS messages for transactions",
                onDismiss = { spotlightViewModel.dismissTutorial() },
                onTargetClick = { homeViewModel?.scanSmsMessages() }
            )
        }
    }
}
