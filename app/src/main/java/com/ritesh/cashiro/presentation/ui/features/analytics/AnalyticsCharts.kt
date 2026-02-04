package com.ritesh.cashiro.presentation.ui.features.analytics

import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ritesh.cashiro.presentation.ui.components.BalancePoint
import com.ritesh.cashiro.presentation.ui.components.CategoryIcon
import com.ritesh.cashiro.presentation.effects.BlurredAnimatedVisibility
import com.ritesh.cashiro.presentation.ui.theme.Spacing
import com.ritesh.cashiro.presentation.common.icons.CategoryMapping
import ir.ehsannarmani.compose_charts.ColumnChart
import ir.ehsannarmani.compose_charts.LineChart
import ir.ehsannarmani.compose_charts.PieChart
import ir.ehsannarmani.compose_charts.models.AnimationMode
import ir.ehsannarmani.compose_charts.models.BarProperties
import ir.ehsannarmani.compose_charts.models.Bars
import ir.ehsannarmani.compose_charts.models.DividerProperties
import ir.ehsannarmani.compose_charts.models.DotProperties
import ir.ehsannarmani.compose_charts.models.DrawStyle
import ir.ehsannarmani.compose_charts.models.GridProperties
import ir.ehsannarmani.compose_charts.models.HorizontalIndicatorProperties
import ir.ehsannarmani.compose_charts.models.LabelHelperProperties
import ir.ehsannarmani.compose_charts.models.LabelProperties
import ir.ehsannarmani.compose_charts.models.Line
import ir.ehsannarmani.compose_charts.models.LineProperties
import ir.ehsannarmani.compose_charts.models.Pie
import ir.ehsannarmani.compose_charts.models.StrokeStyle
import ir.ehsannarmani.compose_charts.models.ZeroLineProperties
import com.ritesh.cashiro.presentation.common.TransactionTypeFilter
import com.ritesh.cashiro.utils.CurrencyFormatter
import java.time.format.DateTimeFormatter
import kotlin.math.abs

@Composable
fun SpendingLineChart(
    data: List<BalancePoint>,
    currency: String,
    typeFilter: TransactionTypeFilter = TransactionTypeFilter.EXPENSE
) {
    if (data.isEmpty()) return
    val themeColors = MaterialTheme.colorScheme

    val cashFlowData = remember(data) { data.map { it.balance.toDouble() } }
    val labels = remember(data) {
        val isYearly = data.size > 1 && data.all { it.timestamp.dayOfYear == 1 }
        val isMonthly = !isYearly && data.all { it.timestamp.dayOfMonth == 1 }
        val spansMultipleYears = if (data.isNotEmpty()) {
            data.first().timestamp.year != data.last().timestamp.year
        } else false

        data.map {
            val date = it.timestamp
            when {
                isYearly -> date.format(DateTimeFormatter.ofPattern("yyyy"))
                isMonthly && spansMultipleYears -> date.format(DateTimeFormatter.ofPattern("MMM yy"))
                isMonthly -> date.format(DateTimeFormatter.ofPattern("MMM"))
                else -> date.format(DateTimeFormatter.ofPattern("dd MMM"))
            }
        }
    }

    LineChart(
        modifier = Modifier
            .fillMaxWidth()
            .height(280.dp)
            .padding(vertical = Spacing.md),
        data = listOf(
            Line(
                label = when (typeFilter) {
                    TransactionTypeFilter.INCOME -> "Income"
                    TransactionTypeFilter.EXPENSE -> "Spending"
                    TransactionTypeFilter.TRANSFER -> "Transfer"
                    TransactionTypeFilter.INVESTMENT -> "Invested"
                    TransactionTypeFilter.CREDIT -> "Credited"
                    else -> "Spending"
                },
                values = cashFlowData,
                color = SolidColor(themeColors.primary),
                firstGradientFillColor = themeColors.primary.copy(alpha = 0.3f),
                secondGradientFillColor = Color.Transparent,
                strokeAnimationSpec = tween(1500, easing = EaseInOutCubic),
                gradientAnimationDelay = 750,
                drawStyle = DrawStyle.Stroke(width = 2.dp),
                curvedEdges = true,
                dotProperties = DotProperties(
                    enabled = true,
                    color = SolidColor(themeColors.primary),
                    strokeWidth = 3.dp,
                    radius = 4.dp,
                    strokeColor = SolidColor(themeColors.surface)
                )
            )
        ),
        dividerProperties = DividerProperties(
            enabled = true,
            xAxisProperties = LineProperties(
                color = SolidColor(themeColors.onSurface.copy(alpha = 0f)),
                thickness = 0.dp
            ),
            yAxisProperties = LineProperties(
                color = SolidColor(themeColors.onSurface.copy(alpha = 0f)),
                thickness = 0.dp
            )
        ),
        indicatorProperties = HorizontalIndicatorProperties(
            enabled = true,
            textStyle = TextStyle.Default.copy(
                fontSize = 10.sp,
                color = themeColors.onSurfaceVariant.copy(0.6f),
                textAlign = TextAlign.Center
            ),
            contentBuilder = { value -> formatPremiumCurrency(value, currency) }
        ),
        labelHelperProperties = LabelHelperProperties(
            enabled = true,
            textStyle = TextStyle.Default.copy(
                fontSize = 10.sp,
                color = themeColors.onSurface,
                textAlign = TextAlign.End
            ),
        ),
        labelProperties = LabelProperties(
            enabled = true,
            textStyle = TextStyle.Default.copy(
                fontSize = 10.sp,
                color = themeColors.onSurface.copy(0.6f),
                textAlign = TextAlign.End
            ),
            labels = labels,
            padding = 16.dp,
            rotation = LabelProperties.Rotation(
                mode = LabelProperties.Rotation.Mode.Force,
                degree = -45f
            )
        ),
        zeroLineProperties = ZeroLineProperties(
            enabled = true,
            style = StrokeStyle.Dashed(),
            color = SolidColor(themeColors.onSurface.copy(alpha = 0.1f)),
        ),
        gridProperties = GridProperties(
            enabled = true,
            xAxisProperties = GridProperties.AxisProperties(
                enabled = true,
                style = StrokeStyle.Dashed(),
                color = SolidColor(themeColors.onSurface.copy(alpha = 0.1f))
            ),
            yAxisProperties = GridProperties.AxisProperties(
                enabled = true,
                style = StrokeStyle.Dashed(),
                color = SolidColor(themeColors.onSurface.copy(alpha = 0.1f))
            )
        ),
        animationMode = AnimationMode.Together(delayBuilder = { it * 200L }),
    )
}

@Composable
fun SpendingBarChart(
    data: List<BalancePoint>,
    currency: String,
    typeFilter: TransactionTypeFilter = TransactionTypeFilter.EXPENSE
) {
    if (data.isEmpty()) return
    val themeColors = MaterialTheme.colorScheme

    val columnData = remember(data) {
        val isYearly = data.size > 1 && data.all { it.timestamp.dayOfYear == 1 }
        val isMonthly = !isYearly && data.all { it.timestamp.dayOfMonth == 1 }
        val spansMultipleYears = if (data.isNotEmpty()) {
            data.first().timestamp.year != data.last().timestamp.year
        } else false

        data.map { point ->
            val label = when {
                isYearly -> point.timestamp.format(DateTimeFormatter.ofPattern("yyyy"))
                isMonthly && spansMultipleYears -> point.timestamp.format(DateTimeFormatter.ofPattern("MMM yy"))
                isMonthly -> point.timestamp.format(DateTimeFormatter.ofPattern("MMM"))
                else -> point.timestamp.format(DateTimeFormatter.ofPattern("dd MMM"))
            }
            Bars(
                label = label,
                values = listOf(
                    Bars.Data(
                        label = when (typeFilter) {
                            TransactionTypeFilter.INCOME -> "Income"
                            TransactionTypeFilter.EXPENSE -> "Spending"
                            TransactionTypeFilter.TRANSFER -> "Transfer"
                            TransactionTypeFilter.INVESTMENT -> "Invested"
                            TransactionTypeFilter.CREDIT -> "Credited"
                            else -> "Spending"
                        },
                        value = point.balance.toDouble(),
                        color = SolidColor(themeColors.primary.copy(alpha = 0.8f))
                    )
                )
            )
        }
    }

    val maxValue = remember(data) { (data.maxOfOrNull { it.balance.toDouble() } ?: 0.0) * 1.2 }

    ColumnChart(
        modifier = Modifier
            .fillMaxWidth()
            .height(280.dp)
            .padding(vertical = Spacing.md),
        data = columnData,
        maxValue = maxValue,
        barProperties = BarProperties(
            cornerRadius = Bars.Data.Radius.Rectangle(
                topLeft = 6.dp,
                topRight = 6.dp,
                bottomLeft = 6.dp,
                bottomRight = 6.dp
            ),
            spacing = 8.dp,
            thickness = 12.dp
        ),
        dividerProperties = DividerProperties(
            enabled = true,
            xAxisProperties = LineProperties(
                color = SolidColor(themeColors.onSurface.copy(alpha = 0f)),
                thickness = 0.dp
            ),
            yAxisProperties = LineProperties(
                color = SolidColor(themeColors.onSurface.copy(alpha = 0f)),
                thickness = 0.dp
            )
        ),
        indicatorProperties = HorizontalIndicatorProperties(
            enabled = true,
            textStyle = TextStyle.Default.copy(
                fontSize = 10.sp,
                color = themeColors.onSurfaceVariant.copy(0.6f),
                textAlign = TextAlign.Center
            ),
            contentBuilder = { value -> formatPremiumCurrency(value, currency) }
        ),
        labelHelperProperties = LabelHelperProperties(
            enabled = true,
            textStyle = TextStyle.Default.copy(
                fontSize = 10.sp,
                color = themeColors.onSurface,
                textAlign = TextAlign.End
            ),
        ),
        labelProperties = LabelProperties(
            enabled = true,
            textStyle = TextStyle.Default.copy(
                fontSize = 10.sp,
                color = themeColors.onSurface.copy(0.6f),
                textAlign = TextAlign.End
            ),
            padding = 16.dp,
            rotation = LabelProperties.Rotation(
                mode = LabelProperties.Rotation.Mode.Force,
                degree = -45f
            )
        ),
        gridProperties = GridProperties(
            enabled = true,
            xAxisProperties = GridProperties.AxisProperties(
                enabled = true,
                style = StrokeStyle.Dashed(),
                color = SolidColor(themeColors.onSurface.copy(alpha = 0.1f))
            ),
            yAxisProperties = GridProperties.AxisProperties(
                enabled = true,
                style = StrokeStyle.Dashed(),
                color = SolidColor(themeColors.onSurface.copy(alpha = 0.1f))
            )
        ),
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
    )
}

@Composable
fun CategoryPieChart(
    categories: List<CategoryData>,
    currency: String
) {
    if (categories.isEmpty()) return
    val themeColors = MaterialTheme.colorScheme

    val total = categories.sumOf { it.amount }.toDouble()
    if (total == 0.0) return

    val pieData = remember(categories) {
        categories.map { category ->
            Pie(
                label = category.name,
                data = category.amount.toDouble(),
                color = CategoryMapping.categories[category.name]?.color ?: Color.Gray,
                selectedColor = (CategoryMapping.categories[category.name]?.color
                    ?: Color.Gray).copy(alpha = 0.8f),
                selected = false
            )
        }
    }

    var chartData by remember { mutableStateOf(pieData) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .padding(Spacing.sm),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .weight(1.2f)
                .fillMaxHeight(),
            contentAlignment = Alignment.Center
        ) {
            PieChart(
                modifier = Modifier.size(160.dp),
                data = chartData,
                onPieClick = { clickedPie ->
                    val pieIndex = chartData.indexOf(clickedPie)
                    chartData = chartData.mapIndexed { index, pie ->
                        pie.copy(selected = index == pieIndex)
                    }
                },
                selectedScale = 1.1f,
                scaleAnimEnterSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                ),
                style = Pie.Style.Stroke(width = 12.dp)
            )

            // Center Icon Overlay
            val selectedPie = chartData.find { it.selected }
            BlurredAnimatedVisibility(
                visible = selectedPie != null,
                enter = fadeIn() + scaleIn(),
                exit = fadeOut() +scaleOut(),
                modifier = Modifier.align(Alignment.Center)
            ) {
                if (selectedPie != null && selectedPie.label != null) {
                    CategoryIcon(
                        category = selectedPie.label!!,
                        size = 32.dp,
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(selectedPie.color.copy(alpha = 0.2f))
                            .padding(12.dp)
                    )
                }
            }
        }

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .padding(start = Spacing.md)
        ) {
            items(chartData.sortedByDescending { it.data }) { pie ->
                LegendItem(
                    label = pie.label ?: "Unknown",
                    value = pie.data,
                    color = pie.color,
                    isSelected = pie.selected,
                    currency = currency,
                    totalAmount = total,
                    onClick = {
                        val pieIndex = chartData.indexOf(pie)
                        chartData = chartData.mapIndexed { index, p ->
                            p.copy(selected = index == pieIndex)
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun LegendItem(
    label: String,
    value: Double,
    color: Color,
    currency: String,
    isSelected: Boolean,
    totalAmount: Double,
    onClick: () -> Unit
) {
    val percentage = (value / totalAmount * 100).toInt()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f) else Color.Transparent)
            .padding(6.dp)
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = formatPremiumCurrency(value, currency),
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "($percentage%)",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
fun SpendingHeatmap(
    data: List<BalancePoint>,
    modifier: Modifier = Modifier
) {
    if (data.isEmpty()) return

    val maxAmount = remember(data) { data.maxOfOrNull { it.balance.toDouble() } ?: 100.0 }
    
    // Animation state
    var animationPlayed by remember { mutableStateOf(false) }
    val animationProgress by animateFloatAsState(
        targetValue = if (animationPlayed) 1f else 0f,
        animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
        label = "HeatMap Animation"
    )

    LaunchedEffect(data) {
        animationPlayed = true
    }

    Column(modifier = modifier.padding(Spacing.md)) {
        Text(
            text = "Activity Heatmap",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(Spacing.sm))

        Box(modifier = Modifier.fillMaxWidth().height(160.dp)) {
            val rows = 7
            LazyHorizontalGrid(
                rows = GridCells.Fixed(rows),
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(data) { point ->
                    val intensity = (point.balance.toDouble() / maxAmount).toFloat().coerceIn(0f, 1f)
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(
                                MaterialTheme.colorScheme.primary.copy(
                                    alpha = (0.1f + (intensity * 0.9f)) * animationProgress
                                )
                            )
                    )
                }
            }
        }
    }
}

fun formatPremiumCurrency(value: Double, currency: String): String {
    val absValue = abs(value)
    val symbol = CurrencyFormatter.getCurrencySymbol(currency)
    
    return when {
        absValue >= 1_000_000 -> {
            val millions = absValue / 1_000_000
            val suffix = "M"
            "${symbol}${String.format("%.1f", millions)}${suffix}"
        }
        absValue >= 1_000 -> {
            val thousands = absValue / 1_000
            val suffix = "k"
            "${symbol}${String.format("%.1f", thousands)}${suffix}"
        }
        else -> "${symbol}${absValue.toInt()}"
    }
}
