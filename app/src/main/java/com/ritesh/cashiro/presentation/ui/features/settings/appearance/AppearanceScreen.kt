package com.ritesh.cashiro.presentation.ui.features.settings.appearance

import android.os.Build
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ritesh.cashiro.data.preferences.AccentColor
import com.ritesh.cashiro.data.preferences.AppFont
import com.ritesh.cashiro.data.preferences.NavigationBarStyle
import com.ritesh.cashiro.data.preferences.ThemeStyle
import com.ritesh.cashiro.presentation.effects.BlurredAnimatedVisibility
import com.ritesh.cashiro.presentation.effects.overScrollVertical
import com.ritesh.cashiro.presentation.ui.components.CustomTitleTopAppBar
import com.ritesh.cashiro.presentation.ui.components.PreferenceSwitch
import com.ritesh.cashiro.presentation.ui.components.SectionHeader
import com.ritesh.cashiro.presentation.ui.features.categories.NavigationContent
import com.ritesh.cashiro.presentation.ui.theme.Dimensions
import com.ritesh.cashiro.presentation.ui.theme.Latte_Blue
import com.ritesh.cashiro.presentation.ui.theme.Latte_Blue_secondary
import com.ritesh.cashiro.presentation.ui.theme.Latte_Blue_tertiary
import com.ritesh.cashiro.presentation.ui.theme.Latte_Flamingo
import com.ritesh.cashiro.presentation.ui.theme.Latte_Flamingo_secondary
import com.ritesh.cashiro.presentation.ui.theme.Latte_Flamingo_tertiary
import com.ritesh.cashiro.presentation.ui.theme.Latte_Green
import com.ritesh.cashiro.presentation.ui.theme.Latte_Green_secondary
import com.ritesh.cashiro.presentation.ui.theme.Latte_Green_tertiary
import com.ritesh.cashiro.presentation.ui.theme.Latte_Lavender
import com.ritesh.cashiro.presentation.ui.theme.Latte_Lavender_secondary
import com.ritesh.cashiro.presentation.ui.theme.Latte_Lavender_tertiary
import com.ritesh.cashiro.presentation.ui.theme.Latte_Mauve
import com.ritesh.cashiro.presentation.ui.theme.Latte_Mauve_secondary
import com.ritesh.cashiro.presentation.ui.theme.Latte_Mauve_tertiary
import com.ritesh.cashiro.presentation.ui.theme.Latte_Peach
import com.ritesh.cashiro.presentation.ui.theme.Latte_Peach_secondary
import com.ritesh.cashiro.presentation.ui.theme.Latte_Peach_tertiary
import com.ritesh.cashiro.presentation.ui.theme.Latte_Pink
import com.ritesh.cashiro.presentation.ui.theme.Latte_Pink_secondary
import com.ritesh.cashiro.presentation.ui.theme.Latte_Pink_tertiary
import com.ritesh.cashiro.presentation.ui.theme.Latte_Red
import com.ritesh.cashiro.presentation.ui.theme.Latte_Red_secondary
import com.ritesh.cashiro.presentation.ui.theme.Latte_Red_tertiary
import com.ritesh.cashiro.presentation.ui.theme.Latte_Rosewater
import com.ritesh.cashiro.presentation.ui.theme.Latte_Rosewater_secondary
import com.ritesh.cashiro.presentation.ui.theme.Latte_Rosewater_tertiary
import com.ritesh.cashiro.presentation.ui.theme.Latte_Sapphire
import com.ritesh.cashiro.presentation.ui.theme.Latte_Sapphire_secondary
import com.ritesh.cashiro.presentation.ui.theme.Latte_Sapphire_tertiary
import com.ritesh.cashiro.presentation.ui.theme.Latte_Teal
import com.ritesh.cashiro.presentation.ui.theme.Latte_Teal_secondary
import com.ritesh.cashiro.presentation.ui.theme.Latte_Teal_tertiary
import com.ritesh.cashiro.presentation.ui.theme.Latte_Yellow
import com.ritesh.cashiro.presentation.ui.theme.Latte_Yellow_secondary
import com.ritesh.cashiro.presentation.ui.theme.Latte_Yellow_tertiary
import com.ritesh.cashiro.presentation.ui.theme.Macchiato_Blue_dim
import com.ritesh.cashiro.presentation.ui.theme.Macchiato_Blue_dim_secondary
import com.ritesh.cashiro.presentation.ui.theme.Macchiato_Blue_dim_tertiary
import com.ritesh.cashiro.presentation.ui.theme.Macchiato_Flamingo_dim
import com.ritesh.cashiro.presentation.ui.theme.Macchiato_Flamingo_dim_secondary
import com.ritesh.cashiro.presentation.ui.theme.Macchiato_Flamingo_dim_tertiary
import com.ritesh.cashiro.presentation.ui.theme.Macchiato_Green_dim
import com.ritesh.cashiro.presentation.ui.theme.Macchiato_Green_dim_secondary
import com.ritesh.cashiro.presentation.ui.theme.Macchiato_Green_dim_tertiary
import com.ritesh.cashiro.presentation.ui.theme.Macchiato_Lavender_dim
import com.ritesh.cashiro.presentation.ui.theme.Macchiato_Lavender_dim_secondary
import com.ritesh.cashiro.presentation.ui.theme.Macchiato_Lavender_dim_tertiary
import com.ritesh.cashiro.presentation.ui.theme.Macchiato_Mauve_dim
import com.ritesh.cashiro.presentation.ui.theme.Macchiato_Mauve_dim_secondary
import com.ritesh.cashiro.presentation.ui.theme.Macchiato_Mauve_dim_tertiary
import com.ritesh.cashiro.presentation.ui.theme.Macchiato_Peach_dim
import com.ritesh.cashiro.presentation.ui.theme.Macchiato_Peach_dim_secondary
import com.ritesh.cashiro.presentation.ui.theme.Macchiato_Peach_dim_tertiary
import com.ritesh.cashiro.presentation.ui.theme.Macchiato_Pink_dim
import com.ritesh.cashiro.presentation.ui.theme.Macchiato_Pink_dim_secondary
import com.ritesh.cashiro.presentation.ui.theme.Macchiato_Pink_dim_tertiary
import com.ritesh.cashiro.presentation.ui.theme.Macchiato_Red_dim
import com.ritesh.cashiro.presentation.ui.theme.Macchiato_Red_dim_secondary
import com.ritesh.cashiro.presentation.ui.theme.Macchiato_Red_dim_tertiary
import com.ritesh.cashiro.presentation.ui.theme.Macchiato_Rosewater_dim
import com.ritesh.cashiro.presentation.ui.theme.Macchiato_Rosewater_dim_secondary
import com.ritesh.cashiro.presentation.ui.theme.Macchiato_Rosewater_dim_tertiary
import com.ritesh.cashiro.presentation.ui.theme.Macchiato_Sapphire_dim
import com.ritesh.cashiro.presentation.ui.theme.Macchiato_Sapphire_dim_secondary
import com.ritesh.cashiro.presentation.ui.theme.Macchiato_Sapphire_dim_tertiary
import com.ritesh.cashiro.presentation.ui.theme.Macchiato_Teal_dim
import com.ritesh.cashiro.presentation.ui.theme.Macchiato_Teal_dim_secondary
import com.ritesh.cashiro.presentation.ui.theme.Macchiato_Teal_dim_tertiary
import com.ritesh.cashiro.presentation.ui.theme.Macchiato_Yellow_dim
import com.ritesh.cashiro.presentation.ui.theme.Macchiato_Yellow_dim_secondary
import com.ritesh.cashiro.presentation.ui.theme.Macchiato_Yellow_dim_tertiary
import com.ritesh.cashiro.presentation.ui.theme.SNProFontFamily
import com.ritesh.cashiro.presentation.ui.theme.Spacing
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource


@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AppearanceScreen(
    onNavigateBack: () -> Unit,
    themeViewModel: ThemeViewModel = hiltViewModel()
) {
    val themeUiState by themeViewModel.themeUiState.collectAsStateWithLifecycle()
    
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val scrollBehaviorSmall = TopAppBarDefaults.pinnedScrollBehavior()
    val hazeState = remember { HazeState() }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            CustomTitleTopAppBar(
                title = "Appearance",
                scrollBehaviorSmall = scrollBehaviorSmall,
                scrollBehaviorLarge = scrollBehavior,
                hazeState = hazeState,
                hasBackButton = true,
                navigationContent = { NavigationContent(onNavigateBack) }
            )
        }
    ) { paddingValues ->
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .hazeSource(state = hazeState)
                    .overScrollVertical()
                    .verticalScroll(rememberScrollState())
                    .padding(
                        top = Dimensions.Padding.content + paddingValues.calculateTopPadding()
                    ),
                verticalArrangement = Arrangement.spacedBy(Spacing.md)
            ) {
                Column(
                    modifier = Modifier
                        .animateContentSize(
                            MaterialTheme.motionScheme.fastSpatialSpec()
                        ),
                    verticalArrangement = Arrangement.spacedBy(Spacing.sm)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.md),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(
                                    RoundedCornerShape(
                                        topStart = Dimensions.Radius.md,
                                        topEnd = Dimensions.Radius.xs,
                                        bottomStart = Dimensions.Radius.md,
                                        bottomEnd = Dimensions.Radius.xs
                                    )
                                )
                                .background(
                                    color = if(themeUiState.isDarkTheme == null) {
                                        MaterialTheme.colorScheme.secondary
                                    } else MaterialTheme.colorScheme.surfaceContainerLow,
                                    shape = RoundedCornerShape(
                                        topStart = Dimensions.Radius.md,
                                        topEnd = Dimensions.Radius.xs,
                                        bottomStart = Dimensions.Radius.md,
                                        bottomEnd = Dimensions.Radius.xs
                                    )
                                )
                                .padding(horizontal = Spacing.xs, vertical = Spacing.md)
                                .clickable(
                                    onClick = {themeViewModel.updateDarkTheme(null)},
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ),
                            contentAlignment = Alignment.Center
                        ){
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = if(themeUiState.isDarkTheme == null)
                                        MaterialTheme.colorScheme.onSecondary
                                    else MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = "System",
                                    style = MaterialTheme.typography.bodySmall,
                                    textAlign = TextAlign.Center,
                                    color = if(themeUiState.isDarkTheme == null)
                                        MaterialTheme.colorScheme.onSecondary
                                    else MaterialTheme.colorScheme.onSurface,
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(2.dp))
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(
                                    RoundedCornerShape(
                                        topStart = Dimensions.Radius.xs,
                                        topEnd = Dimensions.Radius.xs,
                                        bottomStart = Dimensions.Radius.xs,
                                        bottomEnd = Dimensions.Radius.xs
                                    )
                                )
                                .background(
                                    color = if(themeUiState.isDarkTheme == false) {
                                        MaterialTheme.colorScheme.secondary
                                    } else MaterialTheme.colorScheme.surfaceContainerLow,
                                    shape = RoundedCornerShape(
                                        topStart = Dimensions.Radius.xs,
                                        topEnd = Dimensions.Radius.xs,
                                        bottomStart = Dimensions.Radius.xs,
                                        bottomEnd = Dimensions.Radius.xs
                                    )
                                )
                                .padding(horizontal = Spacing.xs, vertical = Spacing.md)
                                .clickable(
                                    onClick = {themeViewModel.updateDarkTheme(false)},
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ),
                            contentAlignment = Alignment.Center
                        ){
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.LightMode,
                                    contentDescription = null,
                                    tint = if(themeUiState.isDarkTheme == false)
                                        MaterialTheme.colorScheme.onSecondary
                                    else MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = "Light",
                                    style = MaterialTheme.typography.bodySmall,
                                    textAlign = TextAlign.Center,
                                    color = if(themeUiState.isDarkTheme == false)
                                        MaterialTheme.colorScheme.onSecondary
                                    else MaterialTheme.colorScheme.onSurface,
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(2.dp))
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(
                                    RoundedCornerShape(
                                        topStart = Dimensions.Radius.xs,
                                        topEnd = Dimensions.Radius.md,
                                        bottomStart = Dimensions.Radius.xs,
                                        bottomEnd = Dimensions.Radius.md
                                    )
                                )
                                .background(
                                    color = if(themeUiState.isDarkTheme == true) {
                                        MaterialTheme.colorScheme.secondary
                                    } else MaterialTheme.colorScheme.surfaceContainerLow,
                                    shape = RoundedCornerShape(
                                        topStart = Dimensions.Radius.xs,
                                        topEnd = Dimensions.Radius.md,
                                        bottomStart = Dimensions.Radius.xs,
                                        bottomEnd = Dimensions.Radius.md
                                    )
                                )
                                .padding(horizontal = Spacing.xs, vertical = Spacing.md)
                                .clickable(
                                    onClick = {themeViewModel.updateDarkTheme(true)},
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ),
                            contentAlignment = Alignment.Center
                        ){
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.DarkMode,
                                    contentDescription = null,
                                    tint = if(themeUiState.isDarkTheme == true)
                                        MaterialTheme.colorScheme.onSecondary
                                    else MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = "Dark",
                                    style = MaterialTheme.typography.bodySmall,
                                    textAlign = TextAlign.Center,
                                    color = if(themeUiState.isDarkTheme == true)
                                        MaterialTheme.colorScheme.onSecondary
                                    else MaterialTheme.colorScheme.onSurface,
                                )
                            }
                        }
                    }

                    Column(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.md),
                        verticalArrangement = Arrangement.spacedBy(Spacing.sm)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
                        ) {
                            // Dynamic Option
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(80.dp)
                                    .clip(RoundedCornerShape(Dimensions.Radius.md))
                                    .background(
                                        color = if (themeUiState.themeStyle == ThemeStyle.DYNAMIC)
                                            MaterialTheme.colorScheme.primaryContainer
                                        else MaterialTheme.colorScheme.surfaceContainerLow
                                    )
                                    .clickable {
                                        themeViewModel.updateThemeStyle(ThemeStyle.DYNAMIC)
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "Dynamic",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (themeUiState.themeStyle == ThemeStyle.DYNAMIC)
                                            MaterialTheme.colorScheme.onPrimaryContainer
                                        else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "Wallpaper Colors",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (themeUiState.themeStyle == ThemeStyle.DYNAMIC)
                                            MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                                        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                    )
                                }
                            }

                            // Default Option
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(80.dp)
                                    .clip(RoundedCornerShape(Dimensions.Radius.md))
                                    .background(
                                        color = if (themeUiState.themeStyle == ThemeStyle.DEFAULT)
                                            MaterialTheme.colorScheme.primaryContainer
                                        else MaterialTheme.colorScheme.surfaceContainerLow
                                    )
                                    .clickable {
                                        themeViewModel.updateThemeStyle(ThemeStyle.DEFAULT)
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "Default",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (themeUiState.themeStyle == ThemeStyle.DEFAULT)
                                            MaterialTheme.colorScheme.onPrimaryContainer
                                        else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "Default Colors",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (themeUiState.themeStyle == ThemeStyle.DEFAULT)
                                            MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                                        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                    )
                                }
                            }
                        }
                    }

                    BlurredAnimatedVisibility(
                        visible = themeUiState.themeStyle == ThemeStyle.DEFAULT,
                        enter = fadeIn() + slideInVertically{-it},
                        exit = fadeOut() + slideOutVertically{-it},
                        modifier = Modifier
                            .animateContentSize(
                            MaterialTheme.motionScheme.defaultSpatialSpec()
                            )
                            .zIndex(-1f)
                    ) {
                        val isDark = themeUiState.isDarkTheme ?: isSystemInDarkTheme()

                        LazyRow(
                            contentPadding = PaddingValues(horizontal = Spacing.md),
                            horizontalArrangement = Arrangement.spacedBy(Spacing.md)
                        ) {
                            items(AccentColor.entries) { accent ->
                                val color = getAccentColorForDisplay(accent, isDark)
                                val secondary = getSecondaryColorForDisplay(accent, isDark)
                                val tertiary = getTertiaryColorForDisplay(accent, isDark)
                                val isSelected = themeUiState.accentColor == accent

                                ColorSchemeBox(
                                    accent = color,
                                    secondary = secondary,
                                    tertiary = tertiary,
                                    onClick = { themeViewModel.updateAccentColor(accent) },
                                    isSelected = isSelected
                                )
                            }
                        }
                    }
                    Column(
                        verticalArrangement = Arrangement.spacedBy(1.5.dp)
                    ) {
                        if (themeUiState.isDarkTheme != false) {
                            PreferenceSwitch(
                                title = "Amoled Black",
                                subtitle = "Use pure black background for deeper contrast",
                                checked = themeUiState.isAmoledMode,
                                onCheckedChange = { themeViewModel.updateAmoledMode(it) },
                                leadingIcon = {
                                    Box(
                                        modifier = Modifier
                                            .size(48.dp)
                                            .background(
                                                color = if (themeUiState.isAmoledMode) {
                                                    MaterialTheme.colorScheme.primaryContainer
                                                } else MaterialTheme.colorScheme.surfaceContainerHigh,
                                                shape = CircleShape
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Default.DarkMode,
                                            contentDescription = null,
                                            tint = if (themeUiState.isAmoledMode) {
                                                MaterialTheme.colorScheme.onPrimaryContainer
                                            } else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                },
                                padding = PaddingValues(horizontal = Spacing.md),
                                isFirst = themeUiState.isDarkTheme != false && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S,
                                isSingle = Build.VERSION.SDK_INT <= Build.VERSION_CODES.S
                            )
                        }

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            PreferenceSwitch(
                                title = "Blur Effects",
                                subtitle = "Enable glassmorphism blur effects in UI components",
                                checked = themeUiState.blurEffects,
                                onCheckedChange = { themeViewModel.updateBlurEffects(it) },
                                padding = PaddingValues(horizontal = Spacing.md),
                                isLast = themeUiState.isDarkTheme != false,
                                isSingle = themeUiState.isDarkTheme == false
                            )
                        }
                    }
                }

                // Navigation Style Section
                SectionHeader(
                    title = "Navigation",
                    modifier = Modifier.padding(start = Spacing.xl, top = Spacing.md)
                )
                Column(
                    modifier = Modifier
                        .animateContentSize(
                            MaterialTheme.motionScheme.defaultSpatialSpec()
                        )
                        .fillMaxWidth()
                        .padding(horizontal = Spacing.md),
                    verticalArrangement = Arrangement.spacedBy(Spacing.sm)
                ){
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(Spacing.xs)
                    ) {
                        // Floating Option
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(80.dp)
                                .clip(RoundedCornerShape(
                                    topStart = Dimensions.Radius.md,
                                    topEnd = Dimensions.Radius.xs,
                                    bottomStart = Dimensions.Radius.md,
                                    bottomEnd = Dimensions.Radius.xs
                                ))
                                .background(
                                    color = if (themeUiState.navigationBarStyle == NavigationBarStyle.FLOATING)
                                        MaterialTheme.colorScheme.tertiaryContainer
                                    else MaterialTheme.colorScheme.surfaceContainerLow
                                )
                                .clickable {
                                    themeViewModel.updateNavigationBarStyle(NavigationBarStyle.FLOATING)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "Floating",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (themeUiState.navigationBarStyle == NavigationBarStyle.FLOATING)
                                        MaterialTheme.colorScheme.onTertiaryContainer
                                    else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "Modern & Sleek",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (themeUiState.navigationBarStyle == NavigationBarStyle.FLOATING)
                                        MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.7f)
                                    else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                )
                            }
                        }

                        // Normal Option
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(80.dp)
                                .clip(RoundedCornerShape(
                                    topStart = Dimensions.Radius.xs,
                                    topEnd = Dimensions.Radius.md,
                                    bottomStart = Dimensions.Radius.xs,
                                    bottomEnd = Dimensions.Radius.md
                                ))
                                .background(
                                    color = if (themeUiState.navigationBarStyle == NavigationBarStyle.NORMAL)
                                        MaterialTheme.colorScheme.secondaryContainer
                                    else MaterialTheme.colorScheme.surfaceContainerLow
                                )
                                .clickable {
                                    themeViewModel.updateNavigationBarStyle(NavigationBarStyle.NORMAL)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "Normal",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (themeUiState.navigationBarStyle == NavigationBarStyle.NORMAL)
                                        MaterialTheme.colorScheme.onSecondaryContainer
                                    else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "Standard M3",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (themeUiState.navigationBarStyle == NavigationBarStyle.NORMAL)
                                        MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                                    else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }

                    BlurredAnimatedVisibility(
                        visible = themeUiState.navigationBarStyle == NavigationBarStyle.NORMAL,
                        enter = fadeIn() + slideInVertically { -it },
                        exit = fadeOut() + slideOutVertically { -it },
                        modifier = Modifier.zIndex(-1f)
                    ) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(1.5.dp)
                        ) {
                            PreferenceSwitch(
                                title = "Hide Navigation Labels",
                                subtitle = "Hide labels in the navigation bar",
                                checked = themeUiState.hideNavigationLabels,
                                onCheckedChange = { themeViewModel.updateHideNavigationLabels(it) },
                                padding = PaddingValues(0.dp),
                                isFirst = true
                            )
                            PreferenceSwitch(
                                title = "Hide Pill Indicator",
                                subtitle = "Hide the selection indicator in the navigation bar",
                                checked = themeUiState.hidePillIndicator,
                                onCheckedChange = { themeViewModel.updateHidePillIndicator(it) },
                                padding = PaddingValues(0.dp),
                                isLast = true
                            )
                        }
                    }
                }
                
                // Font Family Section
                SectionHeader(
                    title = "Fonts",
                    modifier = Modifier.padding(start = Spacing.xl)
                )

                Column(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.md),
                    verticalArrangement = Arrangement.spacedBy(Spacing.sm)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(Spacing.xs)
                    ) {
                        // System Default Option
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(80.dp)
                                .clip(RoundedCornerShape(
                                    topStart = Dimensions.Radius.md,
                                    topEnd = Dimensions.Radius.xs,
                                    bottomStart = Dimensions.Radius.md,
                                    bottomEnd = Dimensions.Radius.xs
                                ))
                                .background(
                                    color = if (themeUiState.appFont == AppFont.SYSTEM)
                                        MaterialTheme.colorScheme.primaryContainer
                                    else MaterialTheme.colorScheme.surfaceContainerLow
                                )
                                .clickable {
                                    themeViewModel.updateAppFont(AppFont.SYSTEM)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "Default",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Default,
                                    color = if (themeUiState.appFont == AppFont.SYSTEM)
                                        MaterialTheme.colorScheme.onPrimaryContainer
                                    else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "System",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontFamily = FontFamily.Default,
                                    color = if (themeUiState.appFont == AppFont.SYSTEM)
                                        MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                                    else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                )
                            }
                        }

                        // SN Pro Option
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(80.dp)

                                .clip(RoundedCornerShape(
                                    topStart = Dimensions.Radius.xs,
                                    topEnd = Dimensions.Radius.md,
                                    bottomStart = Dimensions.Radius.xs,
                                    bottomEnd = Dimensions.Radius.md
                                ))
                                .background(
                                    color = if (themeUiState.appFont == AppFont.SN_PRO)
                                        MaterialTheme.colorScheme.tertiaryContainer
                                    else MaterialTheme.colorScheme.surfaceContainerLow
                                )
                                .clickable {
                                    themeViewModel.updateAppFont(AppFont.SN_PRO)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "SN Pro",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = SNProFontFamily,
                                    color = if (themeUiState.appFont == AppFont.SN_PRO)
                                        MaterialTheme.colorScheme.onTertiaryContainer
                                    else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "Modern Mono",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontFamily = SNProFontFamily,
                                    color = if (themeUiState.appFont == AppFont.SN_PRO)
                                        MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.7f)
                                    else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(Spacing.xl))
            }
        }
    }
}


@Composable
fun getAccentColorForDisplay(accent: AccentColor, isDark: Boolean): Color {
    return if (isDark) {
        when (accent) {
            AccentColor.ROSEWATER -> Macchiato_Rosewater_dim
            AccentColor.FLAMINGO -> Macchiato_Flamingo_dim
            AccentColor.PINK -> Macchiato_Pink_dim
            AccentColor.MAUVE -> Macchiato_Mauve_dim
            AccentColor.RED -> Macchiato_Red_dim
            AccentColor.PEACH -> Macchiato_Peach_dim
            AccentColor.YELLOW -> Macchiato_Yellow_dim
            AccentColor.GREEN -> Macchiato_Green_dim
            AccentColor.TEAL -> Macchiato_Teal_dim
            AccentColor.SAPPHIRE -> Macchiato_Sapphire_dim
            AccentColor.BLUE -> Macchiato_Blue_dim
            AccentColor.LAVENDER -> Macchiato_Lavender_dim
        }
    } else {
        when (accent) {
            AccentColor.ROSEWATER -> Latte_Rosewater
            AccentColor.FLAMINGO -> Latte_Flamingo
            AccentColor.PINK -> Latte_Pink
            AccentColor.MAUVE -> Latte_Mauve
            AccentColor.RED -> Latte_Red
            AccentColor.PEACH -> Latte_Peach
            AccentColor.YELLOW -> Latte_Yellow
            AccentColor.GREEN -> Latte_Green
            AccentColor.TEAL -> Latte_Teal
            AccentColor.SAPPHIRE -> Latte_Sapphire
            AccentColor.BLUE -> Latte_Blue
            AccentColor.LAVENDER -> Latte_Lavender
        }
    }
}

@Composable
fun getSecondaryColorForDisplay(accent: AccentColor, isDark: Boolean): Color {
    return if (isDark) {
        when (accent) {
            AccentColor.ROSEWATER -> Macchiato_Rosewater_dim_secondary
            AccentColor.FLAMINGO -> Macchiato_Flamingo_dim_secondary
            AccentColor.PINK -> Macchiato_Pink_dim_secondary
            AccentColor.MAUVE -> Macchiato_Mauve_dim_secondary
            AccentColor.RED -> Macchiato_Red_dim_secondary
            AccentColor.PEACH -> Macchiato_Peach_dim_secondary
            AccentColor.YELLOW -> Macchiato_Yellow_dim_secondary
            AccentColor.GREEN -> Macchiato_Green_dim_secondary
            AccentColor.TEAL -> Macchiato_Teal_dim_secondary
            AccentColor.SAPPHIRE -> Macchiato_Sapphire_dim_secondary
            AccentColor.BLUE -> Macchiato_Blue_dim_secondary
            AccentColor.LAVENDER -> Macchiato_Lavender_dim_secondary
        }
    } else {
        when (accent) {
            AccentColor.ROSEWATER -> Latte_Rosewater_secondary
            AccentColor.FLAMINGO -> Latte_Flamingo_secondary
            AccentColor.PINK -> Latte_Pink_secondary
            AccentColor.MAUVE -> Latte_Mauve_secondary
            AccentColor.RED -> Latte_Red_secondary
            AccentColor.PEACH -> Latte_Peach_secondary
            AccentColor.YELLOW -> Latte_Yellow_secondary
            AccentColor.GREEN -> Latte_Green_secondary
            AccentColor.TEAL -> Latte_Teal_secondary
            AccentColor.SAPPHIRE -> Latte_Sapphire_secondary
            AccentColor.BLUE -> Latte_Blue_secondary
            AccentColor.LAVENDER -> Latte_Lavender_secondary
        }
    }
}

@Composable
fun getTertiaryColorForDisplay(accent: AccentColor, isDark: Boolean): Color {
    return if (isDark) {
        when (accent) {
            AccentColor.ROSEWATER -> Macchiato_Rosewater_dim_tertiary
            AccentColor.FLAMINGO -> Macchiato_Flamingo_dim_tertiary
            AccentColor.PINK -> Macchiato_Pink_dim_tertiary
            AccentColor.MAUVE -> Macchiato_Mauve_dim_tertiary
            AccentColor.RED -> Macchiato_Red_dim_tertiary
            AccentColor.PEACH -> Macchiato_Peach_dim_tertiary
            AccentColor.YELLOW -> Macchiato_Yellow_dim_tertiary
            AccentColor.GREEN -> Macchiato_Green_dim_tertiary
            AccentColor.TEAL -> Macchiato_Teal_dim_tertiary
            AccentColor.SAPPHIRE -> Macchiato_Sapphire_dim_tertiary
            AccentColor.BLUE -> Macchiato_Blue_dim_tertiary
            AccentColor.LAVENDER -> Macchiato_Lavender_dim_tertiary
        }
    } else {
        when (accent) {
            AccentColor.ROSEWATER -> Latte_Rosewater_tertiary
            AccentColor.FLAMINGO -> Latte_Flamingo_tertiary
            AccentColor.PINK -> Latte_Pink_tertiary
            AccentColor.MAUVE -> Latte_Mauve_tertiary
            AccentColor.RED -> Latte_Red_tertiary
            AccentColor.PEACH -> Latte_Peach_tertiary
            AccentColor.YELLOW -> Latte_Yellow_tertiary
            AccentColor.GREEN -> Latte_Green_tertiary
            AccentColor.TEAL -> Latte_Teal_tertiary
            AccentColor.SAPPHIRE -> Latte_Sapphire_tertiary
            AccentColor.BLUE -> Latte_Blue_tertiary
            AccentColor.LAVENDER -> Latte_Lavender_tertiary
        }
    }
}

@Composable
fun ColorSchemeBox(
    accent: Color,
    secondary: Color,
    tertiary: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    isSelected: Boolean = false
){
    Box(
        modifier = modifier
            .size(110.dp)
            .clip(RoundedCornerShape(Spacing.md))
            .then(
                if (isSelected) {
                    Modifier.border(
                        width = 2.dp,
                        color = accent.copy(0.7f),
                        shape = RoundedCornerShape(Spacing.md)
                    )
                } else Modifier
            )
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .clickable(
                onClick = onClick,
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            )
    ){
        Column(
            modifier = Modifier.fillMaxWidth().padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            Text(
                text = "Abc",
                style = MaterialTheme.typography.labelMedium,
                color = accent,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Column{
                Box(
                    modifier = Modifier
                        .height(16.dp)
                        .width(27.dp)
                        .background(
                            color = tertiary,
                            shape = RoundedCornerShape(Spacing.sm)
                        )
                )
                Spacer(modifier = Modifier.height(Spacing.xs))
                Box(
                    modifier = Modifier
                        .height(16.dp)
                        .width(47.dp)
                        .background(
                            color = secondary,
                            shape = RoundedCornerShape(Spacing.sm)
                        )
                )
            }

            Box(
                modifier = Modifier.align(Alignment.End).size(20.dp).background(
                    accent,
                    RoundedCornerShape(Spacing.xs)
                )
            )
        }
    }
}
