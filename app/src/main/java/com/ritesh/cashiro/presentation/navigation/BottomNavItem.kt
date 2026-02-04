package com.ritesh.cashiro.presentation.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.ui.graphics.vector.ImageVector
import kotlin.reflect.KClass
import com.ritesh.cashiro.presentation.navigation.Home as HomeDestination
import com.ritesh.cashiro.presentation.navigation.Analytics as AnalyticsDestination
import com.ritesh.cashiro.presentation.navigation.Chat as ChatDestination

sealed class BottomNavItem(
    val route: String,
    val title: String,
    val icon: ImageVector,
    val destination: Any,
    val destinationType: KClass<*>
) {
    data object Home : BottomNavItem(
        route = "home",
        title = "Home",
        icon = Icons.Default.Home,
        destination = HomeDestination,
        destinationType = HomeDestination::class
    )
    
    data object Analytics : BottomNavItem(
        route = "analytics",
        title = "Analytics",
        icon = Icons.Default.Analytics,
        destination = AnalyticsDestination,
        destinationType = AnalyticsDestination::class
    )

    data object Settings : BottomNavItem(
        route = "settings",
        title = "Settings",
        icon = Icons.Rounded.Settings,
        destination = com.ritesh.cashiro.presentation.navigation.Settings,
        destinationType = com.ritesh.cashiro.presentation.navigation.Settings::class
    )

    data object Chat : BottomNavItem(
        route = "chat",
        title = "Chat",
        icon = Icons.AutoMirrored.Filled.Chat,
        destination = ChatDestination,
        destinationType = ChatDestination::class
    )
}