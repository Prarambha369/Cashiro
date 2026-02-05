package com.ritesh.cashiro.presentation.ui.features.settings.developer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeveloperMode
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Science
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ritesh.cashiro.presentation.ui.features.categories.NavigationContent
import com.ritesh.cashiro.presentation.ui.components.CustomTitleTopAppBar
import com.ritesh.cashiro.presentation.ui.components.PreferenceSwitch
import com.ritesh.cashiro.presentation.ui.components.ListItem
import com.ritesh.cashiro.presentation.ui.components.ListItemPosition
import com.ritesh.cashiro.presentation.ui.components.toShape
import com.ritesh.cashiro.presentation.ui.features.settings.SettingsViewModel
import com.ritesh.cashiro.presentation.ui.theme.Dimensions
import com.ritesh.cashiro.presentation.ui.theme.grey_dark
import com.ritesh.cashiro.presentation.ui.theme.grey_light
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeveloperScreen(
    modifier: Modifier = Modifier,
    onNavigateBack: () -> Unit,
    settingsViewModel: SettingsViewModel = hiltViewModel()
) {
    val isDeveloperModeEnabled by
            settingsViewModel.isDeveloperModeEnabled.collectAsStateWithLifecycle(
                initialValue = false
            )
    
    val isTestNotificationAlertsEnabled by
        settingsViewModel.isTestNotificationAlertsEnabled.collectAsStateWithLifecycle(
            initialValue = false
        )

    val uiState by settingsViewModel.uiState.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val scrollBehaviorSmall = TopAppBarDefaults.pinnedScrollBehavior()
    val hazeState = remember { HazeState() }

    LaunchedEffect(uiState.seedMessage) {
        uiState.seedMessage?.let {
            snackbarHostState.showSnackbar(it)
            settingsViewModel.clearSeedMessage()
        }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            CustomTitleTopAppBar(
                title = "Developer Options",
                scrollBehaviorSmall = scrollBehaviorSmall,
                scrollBehaviorLarge = scrollBehavior,
                hazeState = hazeState,
                hasBackButton = true,
                navigationContent = { NavigationContent(onNavigateBack) }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Surface(
            modifier = modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .hazeSource(state = hazeState)
                    .padding(
                        start = Dimensions.Padding.content,
                        end = Dimensions.Padding.content,
                        top = Dimensions.Padding.content + paddingValues.calculateTopPadding()
                    ),
                verticalArrangement = Arrangement.spacedBy(1.5.dp)
            ) {
                PreferenceSwitch(
                    title = "Developer Mode",
                    subtitle = "Show technical information in chat",
                    checked = isDeveloperModeEnabled,
                    onCheckedChange = { settingsViewModel.toggleDeveloperMode(it) },
                    leadingIcon = {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .background(
                                    color = grey_light,
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.DeveloperMode,
                                contentDescription = null,
                                tint = grey_dark
                            )
                        }
                    },
                    padding = PaddingValues(0.dp),
                    isSingle = false,
                    isFirst = true
                )
                
                PreferenceSwitch(
                    title = "Test Notification Alerts",
                    subtitle = "Send test notifications periodically",
                    checked = isTestNotificationAlertsEnabled,
                    onCheckedChange = { settingsViewModel.toggleTestNotificationAlerts(it) },
                    leadingIcon = {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .background(
                                    color = grey_light,
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.NotificationsActive,
                                contentDescription = null,
                                tint = grey_dark
                            )
                        }
                    },
                    padding = PaddingValues(0.dp),
                    isSingle = false,
                    isLast = false
                )

                ListItem(
                    headline = { Text("Seed Sample Data") },
                    supporting = { Text("Create dummy accounts and transactions") },
                    leading = {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .background(
                                    color = grey_light,
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (uiState.isSeeding) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    strokeWidth = 2.dp,
                                    color = grey_dark
                                )
                            } else {
                                Icon(
                                    Icons.Default.Science,
                                    contentDescription = null,
                                    tint = grey_dark
                                )
                            }
                        }
                    },
                    onClick = { if (!uiState.isSeeding) settingsViewModel.seedSampleData() },
                    shape = ListItemPosition.Bottom.toShape(),
                    padding = PaddingValues(0.dp)
                )
            }
        }
    }
}
