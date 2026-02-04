package com.ritesh.cashiro.presentation.ui.features.budgets

import com.ritesh.cashiro.data.repository.BudgetWithSpending
import com.ritesh.cashiro.data.repository.CategoryLimitWithSpending
import java.math.BigDecimal
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

data class BudgetUiState(
    val isLoading: Boolean = true,
    val budgets: List<BudgetWithSpending> = emptyList(),
    val selectedBudget: BudgetWithSpending? = null,
    val categoryLimitsWithSpending: List<CategoryLimitWithSpending> = emptyList(),
    val error: String? = null
)

/**
 * Data class for editing a budget in the sheet.
 */
data class EditBudgetState(
    val budgetId: Long? = null,
    val name: String = "",
    val amount: BigDecimal = BigDecimal.ZERO,
    val year: Int = YearMonth.now().year,
    val month: Int = YearMonth.now().monthValue,
    val currency: String = "INR",
    val categoryLimits: List<EditCategoryLimit> = emptyList()
) {
    val isNewBudget: Boolean get() = budgetId == null
    
    fun getDefaultName(): String {
        val yearMonth = YearMonth.of(year, month)
        val monthName = yearMonth.month.getDisplayName(TextStyle.FULL, Locale.getDefault())
        return "$monthName $year"
    }
}

data class EditCategoryLimit(
    val id: Long? = null,
    val categoryName: String,
    val limitAmount: BigDecimal
)
