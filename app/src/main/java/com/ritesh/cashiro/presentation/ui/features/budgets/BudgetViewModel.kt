package com.ritesh.cashiro.presentation.ui.features.budgets

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ritesh.cashiro.data.database.entity.BudgetEntity
import com.ritesh.cashiro.data.repository.BudgetRepository
import com.ritesh.cashiro.data.repository.BudgetWithSpending
import com.ritesh.cashiro.data.repository.CategoryLimitWithSpending
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale
import javax.inject.Inject


@HiltViewModel
class BudgetViewModel @Inject constructor(
    private val budgetRepository: BudgetRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(BudgetUiState())
    val uiState: StateFlow<BudgetUiState> = _uiState.asStateFlow()

    private val _editBudgetState = MutableStateFlow(EditBudgetState())
    val editBudgetState: StateFlow<EditBudgetState> = _editBudgetState.asStateFlow()

    init {
        loadBudgets()
    }

    fun loadBudgets() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                budgetRepository.getAllBudgets().collect { budgets ->
                    val budgetsWithSpending = budgets.map { budget ->
                        budgetRepository.getBudgetWithSpending(budget)
                    }
                    _uiState.update { 
                        it.copy(
                            isLoading = false,
                            budgets = budgetsWithSpending,
                            error = null
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Failed to load budgets"
                    )
                }
            }
        }
    }

    fun selectBudget(budgetId: Long) {
        viewModelScope.launch {
            val budget = budgetRepository.getBudgetById(budgetId) ?: return@launch
            val budgetWithSpending = budgetRepository.getBudgetWithSpending(budget)
            val categoryLimitsWithSpending = budgetRepository.getCategoryLimitsWithSpending(budgetId)
            
            _uiState.update { 
                it.copy(
                    selectedBudget = budgetWithSpending,
                    categoryLimitsWithSpending = categoryLimitsWithSpending
                )
            }
        }
    }

    fun clearSelectedBudget() {
        _uiState.update { 
            it.copy(
                selectedBudget = null,
                categoryLimitsWithSpending = emptyList()
            )
        }
    }

    // Edit budget state management
    fun initNewBudget(currency: String = "INR") {
        val yearMonth = YearMonth.now()
        _editBudgetState.value = EditBudgetState(
            year = yearMonth.year,
            month = yearMonth.monthValue,
            currency = currency
        )
    }

    fun initEditBudget(budget: BudgetEntity) {
        viewModelScope.launch {
            val limits = budgetRepository.getCategoryLimitsForBudgetSync(budget.id)
            _editBudgetState.value = EditBudgetState(
                budgetId = budget.id,
                name = budget.name,
                amount = budget.amount,
                year = budget.year,
                month = budget.month,
                currency = budget.currency,
                categoryLimits = limits.map { limit ->
                    EditCategoryLimit(
                        id = limit.id,
                        categoryName = limit.categoryName,
                        limitAmount = limit.limitAmount
                    )
                }
            )
        }
    }

    fun updateBudgetAmount(amount: BigDecimal) {
        _editBudgetState.update { it.copy(amount = amount) }
    }

    fun updateBudgetName(name: String) {
        _editBudgetState.update { it.copy(name = name) }
    }

    fun updateBudgetMonth(year: Int, month: Int) {
        _editBudgetState.update { it.copy(year = year, month = month) }
    }

    fun addCategoryLimit(categoryName: String, limitAmount: BigDecimal) {
        _editBudgetState.update { state ->
            val existingIndex = state.categoryLimits.indexOfFirst { it.categoryName == categoryName }
            if (existingIndex >= 0) {
                // Update existing
                val updatedLimits = state.categoryLimits.toMutableList()
                updatedLimits[existingIndex] = updatedLimits[existingIndex].copy(limitAmount = limitAmount)
                state.copy(categoryLimits = updatedLimits)
            } else {
                // Add new
                state.copy(
                    categoryLimits = state.categoryLimits + EditCategoryLimit(
                        categoryName = categoryName,
                        limitAmount = limitAmount
                    )
                )
            }
        }
    }

    fun removeCategoryLimit(categoryName: String) {
        _editBudgetState.update { state ->
            state.copy(
                categoryLimits = state.categoryLimits.filter { it.categoryName != categoryName }
            )
        }
    }

    fun saveBudget(onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val state = _editBudgetState.value
                val name = state.name.ifBlank { state.getDefaultName() }
                
                if (state.budgetId != null) {
                    // Update existing budget
                    val existingBudget = budgetRepository.getBudgetById(state.budgetId)
                    if (existingBudget != null) {
                        budgetRepository.updateBudget(
                            existingBudget.copy(
                                name = name,
                                amount = state.amount,
                                year = state.year,
                                month = state.month,
                                currency = state.currency,
                                updatedAt = LocalDateTime.now()
                            )
                        )
                        // Update category limits
                        budgetRepository.deleteCategoryLimitsForBudget(state.budgetId)
                        state.categoryLimits.forEach { limit ->
                            budgetRepository.addCategoryLimit(
                                budgetId = state.budgetId,
                                categoryName = limit.categoryName,
                                limitAmount = limit.limitAmount
                            )
                        }
                    }
                } else {
                    // Create new budget
                    val budgetId = budgetRepository.createBudget(
                        name = name,
                        amount = state.amount,
                        year = state.year,
                        month = state.month,
                        currency = state.currency
                    )
                    // Add category limits
                    state.categoryLimits.forEach { limit ->
                        budgetRepository.addCategoryLimit(
                            budgetId = budgetId,
                            categoryName = limit.categoryName,
                            limitAmount = limit.limitAmount
                        )
                    }
                }
                
                loadBudgets()
                onSuccess()
            } catch (e: Exception) {
                onError(e.message ?: "Failed to save budget")
            }
        }
    }

    fun deleteBudget(budgetId: Long, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                budgetRepository.deleteBudget(budgetId)
                loadBudgets()
                onSuccess()
            } catch (e: Exception) {
                onError(e.message ?: "Failed to delete budget")
            }
        }
    }

    fun clearEditState() {
        _editBudgetState.value = EditBudgetState()
    }
}
