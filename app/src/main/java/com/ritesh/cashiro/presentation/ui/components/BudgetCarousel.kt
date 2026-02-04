package com.ritesh.cashiro.presentation.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ritesh.cashiro.data.repository.BudgetWithSpending

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope

/**
 * Carousel component for displaying multiple budgets.
 * Shows single budget as full-width card, multiple budgets as scrollable carousel.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun SharedTransitionScope.BudgetCarousel(
    budgets: List<BudgetWithSpending>,
    onBudgetClick: (Long) -> Unit,
    onEditClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
    animatedVisibilityScope: AnimatedVisibilityScope? = null
) {
    val isTransitioning = animatedVisibilityScope?.transition?.let { 
        it.currentState != it.targetState 
    } ?: false

    if (budgets.isEmpty()) return
    
    if (budgets.size == 1) {
        // Single budget - show full-width card
        Box(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            BudgetCard(
                budgetWithSpending = budgets.first(),
                onClick = { onBudgetClick(budgets.first().budget.id) },
                onEditClick = { onEditClick(budgets.first().budget.id) },
                modifier = Modifier.fillMaxWidth(),
                animatedVisibilityScope = animatedVisibilityScope,
                sharedElementKey = "budget_card_${budgets.first().budget.id}"
            )
        }
    } else {
        // Multiple budgets - show as carousel
        LazyRow(
            modifier = modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            userScrollEnabled = !isTransitioning
        ) {
            items(
                items = budgets,
                key = { it.budget.id }
            ) { budgetWithSpending ->
                BudgetCardCompact(
                    budgetWithSpending = budgetWithSpending,
                    onClick = { onBudgetClick(budgetWithSpending.budget.id) },
                    modifier = Modifier,
                    animatedVisibilityScope = animatedVisibilityScope,
                    sharedElementKey = "budget_card_${budgetWithSpending.budget.id}"
                )
            }
        }
    }
}
