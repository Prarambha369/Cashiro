package com.ritesh.cashiro.presentation.ui.components

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.animateColor
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.rounded.PieChart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ritesh.cashiro.data.repository.BudgetWithSpending
import com.ritesh.cashiro.utils.CurrencyFormatter
import androidx.core.graphics.toColorInt

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun SharedTransitionScope.BudgetCard(
    modifier: Modifier = Modifier,
    budgetWithSpending: BudgetWithSpending,
    onClick: () -> Unit = {},
    onEditClick: () -> Unit,
    animatedVisibilityScope: AnimatedVisibilityScope? = null,
    sharedElementKey: String? = null
) {
    val budget = budgetWithSpending.budget
    
    // Animate the progress
    var animatedProgress by remember { mutableFloatStateOf(0f) }
    val targetProgress = budgetWithSpending.percentUsed
    val animatedProgressState by animateFloatAsState(
        targetValue = animatedProgress,
        animationSpec = tween(durationMillis = 800),
        label = "progressAnimation"
    )
    
    LaunchedEffect(targetProgress) {
        animatedProgress = targetProgress
    }
    
    // Determine colors based on spending status
    val budgetColor = try {
        Color(budget.color.toColorInt())
    } catch (e: Exception) {
        MaterialTheme.colorScheme.primary
    }

    val progressColor = when {
        budgetWithSpending.isOverBudget -> MaterialTheme.colorScheme.error
        budgetWithSpending.percentUsed > 0.8f -> MaterialTheme.colorScheme.tertiary
        else -> budgetColor
    }
    
    val progressBackgroundColor = progressColor.copy(alpha = 0.15f)
    
    
    val sharedModifier = if (animatedVisibilityScope != null && sharedElementKey != null) {
        Modifier.sharedBounds(
            rememberSharedContentState(key = sharedElementKey),
            animatedVisibilityScope = animatedVisibilityScope,
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
    } else {
        Modifier
    }

    BudgetAnimatedGradientMeshCard(
        budgetColor = budgetColor,
        modifier = modifier
            .then(sharedModifier)
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
        ) {
            // Header row
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(top = 24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Budget icon
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(progressColor.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.PieChart,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                            tint = progressColor
                        )
                    }
                    
                    Column {
                        Text(
                            text = budget.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${budgetWithSpending.daysRemaining} days remaining",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                // Edit button
                IconButton(
                    onClick = onEditClick,
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                        contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                    ),
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.MoreHoriz,
                        contentDescription = "Edit budget",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(20.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        shape = RoundedCornerShape(
                            topStart = 20.dp,
                            topEnd = 20.dp,
                            bottomStart = 28.dp,
                            bottomEnd = 28.dp)
                    )
                    .padding(horizontal = 24.dp, vertical = 12.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Spending amounts
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Column {
                            Text(
                                text = "Spent",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = CurrencyFormatter.formatCurrency(
                                    budgetWithSpending.currentSpending,
                                    budget.currency
                                ),
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontSize = 26.sp,
                                    fontWeight = FontWeight.ExtraBold
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "of ${
                                    CurrencyFormatter.formatCurrency(
                                        budget.amount,
                                        budget.currency
                                    )
                                }",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Progress bar
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(progressBackgroundColor)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(animatedProgressState.coerceIn(0f, 1f))
                                .clip(RoundedCornerShape(4.dp))
                                .background(progressColor)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Stats row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Remaining
                        Column {
                            Text(
                                text = "Remaining",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = if (budgetWithSpending.isOverBudget) {
                                    "-${
                                        CurrencyFormatter.formatCurrency(
                                            budgetWithSpending.remaining.abs(),
                                            budget.currency
                                        )
                                    }"
                                } else {
                                    CurrencyFormatter.formatCurrency(
                                        budgetWithSpending.remaining,
                                        budget.currency
                                    )
                                },
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (budgetWithSpending.isOverBudget) {
                                    MaterialTheme.colorScheme.error
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                }
                            )
                        }

                        // Daily average
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "Per day avg",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = CurrencyFormatter.formatCurrency(
                                    budgetWithSpending.spendingPerDay,
                                    budget.currency
                                ),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        // Recommended daily
                        if (!budgetWithSpending.isOverBudget && budgetWithSpending.daysRemaining > 0) {
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "Can spend/day",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = CurrencyFormatter.formatCurrency(
                                        budgetWithSpending.recommendedDailySpending,
                                        budget.currency
                                    ),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = progressColor
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun BudgetAnimatedGradientMeshCard(
    budgetColor: Color,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "BudgetGradient")
    val animatedColor by infiniteTransition.animateColor(
        initialValue = budgetColor.copy(alpha = 0.6f),
        targetValue = Color.White.copy(alpha = 0.4f),
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "PrimaryColor"
    )
    val reverseAnimatedColor by infiniteTransition.animateColor(
        initialValue = Color.White.copy(alpha = 0.4f),
        targetValue = budgetColor.copy(alpha = 0.6f),
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "SecondaryColor"
    )

    val surfaceColor = MaterialTheme.colorScheme.surfaceContainerLow

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp)),
        shape = RoundedCornerShape(28.dp),
        border = BorderStroke(1.dp, animatedColor.copy(alpha = 0.1f)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Canvas(
                modifier = Modifier
                    .matchParentSize()
                    .blur(60.dp, edgeTreatment = BlurredEdgeTreatment.Unbounded)
            ) {
                val canvasWidth = size.width
                val canvasHeight = size.height

                val gradientBrush = Brush.linearGradient(
                    colors = listOf(reverseAnimatedColor, animatedColor, surfaceColor.copy(alpha = 0.5f)),
                    start = Offset(0f, 0f),
                    end = Offset(canvasWidth, canvasHeight),
                    tileMode = TileMode.Mirror
                )

                drawRect(
                    brush = gradientBrush,
                    topLeft = Offset(0f, 0f),
                    size = size
                )
            }

            // Soft overlay to ensure readability
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                surfaceColor.copy(alpha = 0.2f),
                                surfaceColor.copy(alpha = 0.6f),
                                surfaceColor
                            )
                        )
                    )
            )

            content()
        }
    }
}

/**
 * Compact version of BudgetCard for carousel display.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun SharedTransitionScope.BudgetCardCompact(
    budgetWithSpending: BudgetWithSpending,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    animatedVisibilityScope: AnimatedVisibilityScope? = null,
    sharedElementKey: String? = null
) {
    val budget = budgetWithSpending.budget
    
    // Animate the progress
    var animatedProgress by remember { mutableFloatStateOf(0f) }
    val targetProgress = budgetWithSpending.percentUsed
    val animatedProgressState by animateFloatAsState(
        targetValue = animatedProgress,
        animationSpec = tween(durationMillis = 800),
        label = "progressAnimation"
    )
    
    LaunchedEffect(targetProgress) {
        animatedProgress = targetProgress
    }
    
    // Determine colors based on spending status
    val budgetColor = try {
        Color(budget.color.toColorInt())
    } catch (e: Exception) {
        MaterialTheme.colorScheme.primary
    }

    val progressColor = when {
        budgetWithSpending.isOverBudget -> MaterialTheme.colorScheme.error
        budgetWithSpending.percentUsed > 0.8f -> MaterialTheme.colorScheme.tertiary
        else -> budgetColor
    }
    
    val progressBackgroundColor = progressColor.copy(alpha = 0.15f)
    
    val sharedModifier = if (animatedVisibilityScope != null && sharedElementKey != null) {
        Modifier.sharedBounds(
            rememberSharedContentState(key = sharedElementKey),
            animatedVisibilityScope = animatedVisibilityScope,
            boundsTransform = { _, _ ->
                spring(
                    stiffness = Spring.StiffnessLow,
                    dampingRatio = Spring.DampingRatioNoBouncy
                )
            },
            resizeMode = SharedTransitionScope.ResizeMode.scaleToBounds(
                contentScale = ContentScale.Fit,
                alignment = Alignment.TopCenter
            )
        )
    } else {
        Modifier
    }

    BudgetAnimatedGradientMeshCard(
        budgetColor = budgetColor,
        modifier = modifier
            .then(sharedModifier)
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(top = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(progressColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.PieChart,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = progressColor
                    )
                }
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = budget.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${budgetWithSpending.daysRemaining} days left",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        shape = RoundedCornerShape(
                            topStart = 18.dp,
                            topEnd = 18.dp,
                            bottomStart = 28.dp,
                            bottomEnd = 28.dp)
                    )
                    .padding(horizontal = 24.dp, vertical = 12.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Amounts
                    Text(
                        text = CurrencyFormatter.formatCurrency(
                            budgetWithSpending.currentSpending,
                            budget.currency
                        ),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = "of ${
                            CurrencyFormatter.formatCurrency(
                                budget.amount,
                                budget.currency
                            )
                        }",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Progress bar
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(progressBackgroundColor)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(animatedProgressState.coerceIn(0f, 1f))
                                .clip(RoundedCornerShape(3.dp))
                                .background(progressColor)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Remaining
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Remaining",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = if (budgetWithSpending.isOverBudget) {
                                "-${
                                    CurrencyFormatter.formatCurrency(
                                        budgetWithSpending.remaining.abs(),
                                        budget.currency
                                    )
                                }"
                            } else {
                                CurrencyFormatter.formatCurrency(
                                    budgetWithSpending.remaining,
                                    budget.currency
                                )
                            },
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (budgetWithSpending.isOverBudget) {
                                MaterialTheme.colorScheme.error
                            } else {
                                progressColor
                            }
                        )
                    }
                }
            }
        }
    }
}
