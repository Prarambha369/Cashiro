package com.ritesh.cashiro.presentation.ui.features.budgets

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ritesh.cashiro.data.database.entity.BudgetEntity
import com.ritesh.cashiro.data.repository.BudgetRepository
import com.ritesh.cashiro.data.repository.CategoryLimitWithSpending
import com.ritesh.cashiro.data.database.dao.AccountBalanceDao
import com.ritesh.cashiro.data.database.entity.BudgetPeriod
import com.ritesh.cashiro.data.database.entity.BudgetTrackType
import com.ritesh.cashiro.data.database.entity.BudgetType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.time.LocalDateTime
import javax.inject.Inject


@HiltViewModel
class BudgetViewModel @Inject constructor(
    private val budgetRepository: BudgetRepository,
    private val accountBalanceDao: AccountBalanceDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(BudgetUiState())
    val uiState: StateFlow<BudgetUiState> = _uiState.asStateFlow()

    private val _editBudgetState = MutableStateFlow(EditBudgetState())
    val editBudgetState: StateFlow<EditBudgetState> = _editBudgetState.asStateFlow()

    private var transactionCollectionJob: kotlinx.coroutines.Job? = null
    private var selectedBudgetCollectionJob: kotlinx.coroutines.Job? = null

    init {
        loadBudgets()
        loadAccounts()
    }

    private fun loadAccounts() {
        viewModelScope.launch {
            accountBalanceDao.getAllLatestBalances().collect { accounts ->
                _uiState.update { it.copy(allAccounts = accounts) }
            }
        }
    }

    fun loadBudgets() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                budgetRepository.getAllBudgetsWithSpending().collect { budgetsWithSpending ->
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

    fun selectBudget(budgetId: Long, startDate: String? = null, endDate: String? = null) {
        selectedBudgetCollectionJob?.cancel()
        selectedBudgetCollectionJob = viewModelScope.launch {
            budgetRepository.getAllBudgetsWithSpending().collect { allBudgets ->
                val found = allBudgets.find { it.budget.id == budgetId }
                if (found != null) {
                    var budgetWithSpending = found
                    
                    // If custom dates provided, recalculate spending for that period
                    if (startDate != null && endDate != null) {
                        try {
                            val start = LocalDateTime.parse(startDate)
                            val end = LocalDateTime.parse(endDate)
                            val customBudget = budgetWithSpending.budget.copy(
                                startDate = start,
                                endDate = end
                            )
                            budgetWithSpending = budgetRepository.getBudgetWithSpending(customBudget)
                        } catch (e: Exception) {
                            // Fallback to normal if parsing fails
                        }
                    }

                    val categoryLimitsWithSpending = budgetWithSpending.categoryLimits.map { limit ->
                        CategoryLimitWithSpending(
                            limit = limit,
                            currentSpending = budgetWithSpending.categorySpending[limit.categoryName] ?: BigDecimal.ZERO
                        )
                    }
                    _uiState.update { 
                        it.copy(
                            selectedBudget = budgetWithSpending,
                            categoryLimitsWithSpending = categoryLimitsWithSpending
                        )
                    }
                }
            }
        }

        // Collect transactions for the selected budget
        transactionCollectionJob?.cancel()
        viewModelScope.launch {
            val budget = budgetRepository.getBudgetById(budgetId) ?: return@launch
            
            // Override dates if provided
            val collectionBudget = if (startDate != null && endDate != null) {
                try {
                    budget.copy(
                        startDate = LocalDateTime.parse(startDate),
                        endDate = LocalDateTime.parse(endDate)
                    )
                } catch (e: Exception) { budget }
            } else budget

            transactionCollectionJob = viewModelScope.launch {
                budgetRepository.getTransactionsForBudget(collectionBudget).collect { transactions ->
                    _uiState.update { it.copy(selectedBudgetTransactions = transactions) }
                }
            }
        }
    }

    fun clearSelectedBudget() {
        transactionCollectionJob?.cancel()
        transactionCollectionJob = null
        selectedBudgetCollectionJob?.cancel()
        selectedBudgetCollectionJob = null
        _uiState.update { 
            it.copy(
                selectedBudget = null,
                categoryLimitsWithSpending = emptyList(),
                selectedBudgetTransactions = emptyList()
            )
        }
    }

    // Edit budget state management
    fun initNewBudget(currency: String = "INR") {
        val now = LocalDateTime.now()
        _editBudgetState.value = EditBudgetState(
            year = now.year,
            month = now.monthValue,
            startDate = now,
            endDate = now.plusMonths(1).minusDays(1),
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
                startDate = budget.startDate,
                endDate = budget.endDate,
                periodType = budget.periodType,
                trackType = budget.trackType,
                budgetType = budget.budgetType,
                accountIds = budget.accountIds,
                color = budget.color,
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
        val startDate = LocalDateTime.of(year, month, 1, 0, 0)
        val endDate = startDate.plusMonths(1).minusDays(1)
        
        _editBudgetState.update { 
            it.copy(
                year = year, 
                month = month,
                startDate = startDate,
                endDate = endDate
            ) 
        }
    }
    
    fun updateStartDate(date: LocalDateTime) {
        _editBudgetState.update { state -> 
            val newEndDate = calculateEndDate(date, state.periodType)
            state.copy(
                startDate = date,
                endDate = newEndDate,
                year = date.year,
                month = date.monthValue
            )
        }
    }

    fun updateEndDate(date: LocalDateTime) {
         _editBudgetState.update { it.copy(endDate = date) }
    }

    fun updatePeriodType(periodType: BudgetPeriod) {
        _editBudgetState.update { state ->
            val newEndDate = calculateEndDate(state.startDate, periodType)
            state.copy(
                periodType = periodType,
                endDate = newEndDate
            )
        }
    }

    fun updateTrackType(trackType: BudgetTrackType) {
        _editBudgetState.update { it.copy(trackType = trackType) }
    }

    fun updateBudgetType(budgetType: BudgetType) {
        _editBudgetState.update { it.copy(budgetType = budgetType) }
    }

    fun updateAccountIds(accountIds: List<String>) {
        _editBudgetState.update { it.copy(accountIds = accountIds) }
    }
    
    fun updateColor(color: String) {
        _editBudgetState.update { it.copy(color = color) }
    }

    private fun calculateEndDate(startDate: LocalDateTime, periodType: BudgetPeriod): LocalDateTime {
        return when (periodType) {
            BudgetPeriod.DAILY -> startDate
            BudgetPeriod.WEEKLY -> startDate.plusDays(6)
            BudgetPeriod.MONTHLY -> startDate.plusMonths(1).minusDays(1)
            BudgetPeriod.YEARLY -> startDate.plusYears(1).minusDays(1)
            BudgetPeriod.CUSTOM -> startDate // User selects end date manually
        }
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
                                year = state.startDate.year,
                                month = state.startDate.monthValue,
                                startDate = state.startDate,
                                endDate = state.endDate,
                                periodType = state.periodType,
                                trackType = state.trackType,
                                budgetType = state.budgetType,
                                accountIds = state.accountIds,
                                color = state.color,
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
                    val budget = BudgetEntity(
                        name = name,
                        amount = state.amount,
                        year = state.startDate.year,
                        month = state.startDate.monthValue,
                        startDate = state.startDate,
                        endDate = state.endDate,
                        periodType = state.periodType,
                        trackType = state.trackType,
                        budgetType = state.budgetType,
                        accountIds = state.accountIds,
                        color = state.color,
                        currency = state.currency
                    )
                    
                    val budgetId = budgetRepository.insertBudget(budget)
                    
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
