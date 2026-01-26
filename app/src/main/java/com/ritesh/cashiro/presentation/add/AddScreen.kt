package com.ritesh.cashiro.presentation.add

import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.ritesh.cashiro.presentation.categories.NavigationContent
import com.ritesh.cashiro.ui.components.CustomTitleTopAppBar
import com.ritesh.cashiro.ui.components.GenericTypeSwitcher
import com.ritesh.cashiro.ui.effects.overScrollVertical
import com.ritesh.cashiro.ui.theme.Dimensions
import com.ritesh.cashiro.ui.theme.Spacing
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
fun AddScreen(
    viewModel: AddViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit = {},
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedContentScope: AnimatedContentScope? = null,
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

    val tabs = listOf("Transaction", "Subscription")

    Box(
        modifier =
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
                        resizeMode = SharedTransitionScope.ResizeMode.scaleToBounds()
                    )
                }
            } else {
                Modifier
            }.background(MaterialTheme.colorScheme.background)
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
                    navigationContent = { NavigationContent(onNavigateBack) }
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
                        .weight(1f)
                ) { page ->
                    when (page) {
                        0 -> TransactionTabContent(viewModel = viewModel, onSave = onNavigateBack)
                        1 -> SubscriptionTabContent(viewModel = viewModel, onSave = onNavigateBack)
                    }
                }
            }
        }
    }
}
