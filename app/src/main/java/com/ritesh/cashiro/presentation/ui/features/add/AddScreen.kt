package com.ritesh.cashiro.presentation.ui.features.add

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.ritesh.cashiro.presentation.ui.features.categories.NavigationContent
import com.ritesh.cashiro.presentation.ui.components.CustomTitleTopAppBar
import com.ritesh.cashiro.presentation.ui.components.GenericTypeSwitcher
import com.ritesh.cashiro.presentation.ui.theme.Dimensions
import com.ritesh.cashiro.presentation.ui.theme.Spacing
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
fun SharedTransitionScope.AddScreen(
    addViewModel: AddViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit = {},
    animatedVisibilityScope: AnimatedVisibilityScope,
    initialTab: Int = 0
) {
    val pagerState = rememberPagerState(
        initialPage = initialTab,
        pageCount = { 2 }
    )
    val coroutineScope = rememberCoroutineScope()

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val scrollBehaviorSmall = TopAppBarDefaults.pinnedScrollBehavior()
    val hazeState = remember { HazeState() }
    
    // Track if a transition is currently running to prevent race conditions in UI interaction
    val isTransitioning = animatedVisibilityScope.transition.let { 
        it.currentState != it.targetState 
    }

    // Intercept back button during transition to prevent double-pops or desync
    BackHandler(enabled = isTransitioning) { }

    val tabs = listOf("Transaction", "Subscription")

    Box(
        modifier =
            Modifier.sharedBounds(
                rememberSharedContentState(key = "fab_to_add"),
                animatedVisibilityScope = animatedVisibilityScope,
                boundsTransform = { _, _ ->
                    spring(
                        stiffness = Spring.StiffnessLow,
                        dampingRatio = Spring.DampingRatioLowBouncy
                    )
                },
                resizeMode = SharedTransitionScope.ResizeMode.scaleToBounds(
                    contentScale = ContentScale.Fit,
                    alignment = Alignment.BottomEnd
                )
            )
                .skipToLookaheadSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Scaffold(
            modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
            topBar = {
                CustomTitleTopAppBar(
                    scrollBehaviorSmall = scrollBehaviorSmall,
                    scrollBehaviorLarge = scrollBehavior,
                    title = "Add New",
                    hazeState = hazeState,
                    hasBackButton = true,
                    navigationContent = { NavigationContent { if (!isTransitioning) onNavigateBack() } }
                )
            },
            containerColor = MaterialTheme.colorScheme.background
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .hazeSource(state = hazeState)
                    .padding(
                        start = Dimensions.Padding.content,
                        end = Dimensions.Padding.content,
                        top = Dimensions.Padding.content +
                                paddingValues.calculateTopPadding()
                    ),
                verticalArrangement = Arrangement.spacedBy(Spacing.md)
            ) {
                // Type Switcher
                GenericTypeSwitcher(
                    selectedIndex = pagerState.currentPage,
                    onIndexChange = { index ->
                        coroutineScope.launch { pagerState.animateScrollToPage(index) }},
                    options = tabs,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.lg)
                )



                // Tab Content
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    userScrollEnabled = !isTransitioning
                ) { page ->
                    when (page) {
                        0 -> TransactionTabContent(
                            viewModel = addViewModel,
                            onSave = onNavigateBack,
                            isTransitioning = isTransitioning
                        )
                        1 -> SubscriptionTabContent(
                            viewModel = addViewModel,
                            onSave = onNavigateBack,
                            isTransitioning = isTransitioning
                        )
                    }
                }
            }
        }
    }
}
