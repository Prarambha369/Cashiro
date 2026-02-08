package com.ritesh.cashiro.presentation.ui.components

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ritesh.cashiro.data.database.entity.CategoryEntity
import com.ritesh.cashiro.data.database.entity.SubcategoryEntity
import com.ritesh.cashiro.data.database.entity.TransactionEntity
import com.ritesh.cashiro.data.database.entity.TransactionType
import com.ritesh.cashiro.presentation.effects.BlurredAnimatedVisibility
import com.ritesh.cashiro.presentation.ui.theme.Dimensions
import com.ritesh.cashiro.presentation.ui.theme.Spacing
import com.ritesh.cashiro.presentation.ui.theme.credit_dark
import com.ritesh.cashiro.presentation.ui.theme.credit_light
import com.ritesh.cashiro.presentation.ui.theme.expense_dark
import com.ritesh.cashiro.presentation.ui.theme.expense_light
import com.ritesh.cashiro.presentation.ui.theme.income_dark
import com.ritesh.cashiro.presentation.ui.theme.income_light
import com.ritesh.cashiro.presentation.ui.theme.investment_dark
import com.ritesh.cashiro.presentation.ui.theme.investment_light
import com.ritesh.cashiro.presentation.ui.theme.transfer_dark
import com.ritesh.cashiro.presentation.ui.theme.transfer_light
import com.ritesh.cashiro.utils.CurrencyFormatter
import com.ritesh.cashiro.utils.formatAmount
import java.math.BigDecimal
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun SharedTransitionScope.TransactionItem(
    modifier: Modifier = Modifier,
    transaction: TransactionEntity? = null,
    merchantName: String? = null,
    amount: BigDecimal? = null,
    transactionType: TransactionType? = null,
    categoryEntity: CategoryEntity? = null,
    subcategoryEntity: SubcategoryEntity? = null,
    accountIconResId: Int = 0,
    accountColorHex: String? = null,
    showDate: Boolean = true,
    useCardStyle: Boolean = false,
    shape: CornerBasedShape = listSingleItemShape,
    onClick: () -> Unit = {},
    subtitleOverride: String? = null,
    amountOverride: String? = null,
    amountColorOverride: Color? = null,
    balanceAfter: BigDecimal? = null,
    balanceCurrency: String? = null,
    animatedContentScope: AnimatedVisibilityScope? = null,
    sharedElementKey: String? = null,
    isSelectionMode: Boolean = false,
    isSelected: Boolean = false,
    onSelectionToggle: () -> Unit = {},
    onLongClick: () -> Unit = {}
) {
    val finalMerchantName = merchantName ?: transaction?.merchantName ?: ""
    val finalAmount = amount ?: transaction?.amount ?: BigDecimal.ZERO
    val finalType = transactionType ?: transaction?.transactionType ?: TransactionType.EXPENSE
    val isRecurring = transaction?.isRecurring ?: false

    val isDark = isSystemInDarkTheme()
    val amountColor = amountColorOverride ?: remember(finalType, isRecurring, isDark) {
        when (finalType) {
            TransactionType.INCOME -> if (!isDark) income_light else income_dark
            TransactionType.EXPENSE -> if (!isDark) expense_light else expense_dark
            TransactionType.CREDIT -> if (!isDark) credit_light else credit_dark
            TransactionType.TRANSFER -> if (!isDark) transfer_light else transfer_dark
            TransactionType.INVESTMENT -> if (!isDark) investment_light else investment_dark
        }
    }

    val dateTimeFormatter = remember { DateTimeFormatter.ofPattern("MMM d • h:mm a") }
    val defaultSubtitle = remember(transaction?.dateTime) { 
        transaction?.dateTime?.format(dateTimeFormatter) ?: "" 
    }
    val amountText = remember(transaction, amountOverride, finalAmount) {
        amountOverride ?: transaction?.formatAmount() ?: finalAmount.toString()
    }

    val itemModifier = modifier.then(
        if (animatedContentScope != null && sharedElementKey != null) {
            Modifier.sharedBounds(
                rememberSharedContentState(key = sharedElementKey),
                animatedVisibilityScope = animatedContentScope,
                boundsTransform = { _, _ ->
                    spring(
                        stiffness = 600f,
                        dampingRatio = Spring.DampingRatioNoBouncy
                    )
                },
                resizeMode = SharedTransitionScope.ResizeMode.scaleToBounds(ContentScale.Fit, Alignment.Center)
            )
                .skipToLookaheadSize()
        } else Modifier
    )

    val leadingContent: @Composable () -> Unit = {
        BlurredAnimatedVisibility(isSelectionMode) {
            Checkbox(
                checked = isSelected,
                onCheckedChange = { onSelectionToggle() },
                modifier = Modifier.size(40.dp)
            )
        }
        BlurredAnimatedVisibility(!isSelectionMode){
            BrandIcon(
                merchantName = finalMerchantName,
                size = 40.dp,
                showBackground = true,
                categoryEntity = categoryEntity,
                subcategoryEntity = subcategoryEntity,
                accountIconResId = accountIconResId,
                accountColorHex = accountColorHex
            )
        }
    }

    // Build subtitle parts
    val (subtitleParts, subtitleFinal) = remember(
        subtitleOverride,
        transaction,
        finalType,
        defaultSubtitle,
        isRecurring,
        balanceAfter,
        balanceCurrency
    ) {
        val parts = buildList {
            if (subtitleOverride != null) {
                add(subtitleOverride)
            } else {
                if (transaction != null) {
                    when (finalType) {
                        TransactionType.CREDIT -> add("Credit Card")
                        TransactionType.TRANSFER -> add("Transfer")
                        TransactionType.INVESTMENT -> add("Investment")
                        else -> {}
                    }
                }
                if (defaultSubtitle.isNotEmpty()) {
                    add(defaultSubtitle)
                }
                if (isRecurring) add("Recurring")

                balanceAfter?.let { balance ->
                    add("Bal: ${CurrencyFormatter.formatCurrency(balance, balanceCurrency ?: "INR")}")
                }
            }
        }
        parts to parts.joinToString(" • ")
    }

    if (useCardStyle) {
        ListItemCard(
            title = finalMerchantName,
            subtitle = subtitleFinal,
            amount = amountText,
            amountColor = amountColor,
            onClick = onClick,
            leadingContent = leadingContent,
            modifier = itemModifier
        )
    } else {
        ListItem(
            headline = {
                Text(
                    text = finalMerchantName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
            },
            supporting = {
                Text(
                    text = subtitleParts.joinToString(" • "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.85f)
                )
            },
            leading = leadingContent,
            trailing = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
                ) {
                    if (subtitleOverride == null) {
                        when (finalType) {
                            TransactionType.CREDIT -> Icon(
                                Icons.Default.CreditCard,
                                contentDescription = "Credit Card",
                                modifier = Modifier.size(Dimensions.Icon.small),
                                tint = if (!isSystemInDarkTheme()) credit_light else credit_dark
                            )
                            TransactionType.TRANSFER -> Icon(
                                Icons.Default.SwapHoriz,
                                contentDescription = "Transfer",
                                modifier = Modifier.size(Dimensions.Icon.small),
                                tint = if (!isSystemInDarkTheme()) transfer_light else transfer_dark
                            )
                            TransactionType.INVESTMENT -> Icon(
                                Icons.AutoMirrored.Filled.ShowChart,
                                contentDescription = "Investment",
                                modifier = Modifier.size(Dimensions.Icon.small),
                                tint = if (!isSystemInDarkTheme()) investment_light else investment_dark
                            )
                            TransactionType.INCOME -> Icon(
                                Icons.AutoMirrored.Filled.TrendingUp,
                                contentDescription = "Income",
                                modifier = Modifier.size(Dimensions.Icon.small),
                                tint = if (!isSystemInDarkTheme()) income_light else income_dark
                            )
                            TransactionType.EXPENSE -> Icon(
                                Icons.AutoMirrored.Filled.TrendingDown,
                                contentDescription = "Expense",
                                modifier = Modifier.size(Dimensions.Icon.small),
                                tint = if (!isSystemInDarkTheme()) expense_light else expense_dark
                            )
                        }
                    }

                    Text(
                        text = amountText,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = amountColor
                    )
                }
            },
            onClick = {
                if (isSelectionMode) {
                    onSelectionToggle()
                } else {
                    onClick()
                }
            },
            onLongClick = onLongClick,
            shape = shape,
            listColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f) else MaterialTheme.colorScheme.surfaceContainerLow,
            padding = PaddingValues(vertical = 1.5.dp),
            modifier = itemModifier
        )
    }
}
