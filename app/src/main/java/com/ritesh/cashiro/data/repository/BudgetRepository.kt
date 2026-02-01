package com.ritesh.cashiro.data.repository

import com.ritesh.cashiro.data.database.dao.BudgetDao
import com.ritesh.cashiro.data.database.dao.TransactionDao
import com.ritesh.cashiro.data.database.entity.BudgetCategoryLimitEntity
import com.ritesh.cashiro.data.database.entity.BudgetEntity
import com.ritesh.cashiro.data.database.entity.TransactionType
import com.ritesh.cashiro.di.ApplicationScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDateTime
import java.time.YearMonth
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Data class representing a budget with its current spending information.
 */
data class BudgetWithSpending(
    val budget: BudgetEntity,
    val currentSpending: BigDecimal,
    val categoryLimits: List<BudgetCategoryLimitEntity>,
    val categorySpending: Map<String, BigDecimal>,
    val daysRemaining: Int,
    val daysInMonth: Int
) {
    val remaining: BigDecimal get() = budget.amount - currentSpending
    val percentUsed: Float get() = if (budget.amount > BigDecimal.ZERO) {
        (currentSpending.toFloat() / budget.amount.toFloat()).coerceIn(0f, 1f)
    } else 0f
    val isOverBudget: Boolean get() = currentSpending > budget.amount
    val spendingPerDay: BigDecimal get() {
        val daysPassed = daysInMonth - daysRemaining
        return if (daysPassed > 0) {
            currentSpending.divide(BigDecimal(daysPassed), 2, RoundingMode.HALF_UP)
        } else BigDecimal.ZERO
    }
    val recommendedDailySpending: BigDecimal get() {
        return if (daysRemaining > 0 && remaining > BigDecimal.ZERO) {
            remaining.divide(BigDecimal(daysRemaining), 2, RoundingMode.HALF_UP)
        } else BigDecimal.ZERO
    }
}

/**
 * Data class representing a category limit with its current spending.
 */
data class CategoryLimitWithSpending(
    val limit: BudgetCategoryLimitEntity,
    val currentSpending: BigDecimal
) {
    val remaining: BigDecimal get() = limit.limitAmount - currentSpending
    val percentUsed: Float get() = if (limit.limitAmount > BigDecimal.ZERO) {
        (currentSpending.toFloat() / limit.limitAmount.toFloat()).coerceIn(0f, 1f)
    } else 0f
    val isOverLimit: Boolean get() = currentSpending > limit.limitAmount
}

@Singleton
class BudgetRepository @Inject constructor(
    private val budgetDao: BudgetDao,
    private val transactionDao: TransactionDao,
    @ApplicationScope private val externalScope: CoroutineScope
) {

    val allBudgets: StateFlow<List<BudgetEntity>> = budgetDao.getAllBudgets()
        .stateIn(
            scope = externalScope,
            started = SharingStarted.Eagerly,
            initialValue = emptyList()
        )

    fun getAllBudgets(): Flow<List<BudgetEntity>> {
        return budgetDao.getAllBudgets()
    }

    fun getActiveBudgets(): Flow<List<BudgetEntity>> {
        return budgetDao.getActiveBudgets()
    }

    suspend fun getBudgetById(budgetId: Long): BudgetEntity? {
        return budgetDao.getBudgetById(budgetId)
    }

    suspend fun getBudgetByYearMonth(year: Int, month: Int): BudgetEntity? {
        return budgetDao.getBudgetByYearMonth(year, month)
    }

    fun getActiveBudgetsForMonth(year: Int, month: Int): Flow<List<BudgetEntity>> {
        return budgetDao.getActiveBudgetsForMonth(year, month)
    }

    suspend fun createBudget(
        name: String,
        amount: BigDecimal,
        year: Int,
        month: Int,
        currency: String = "INR"
    ): Long {
        val budget = BudgetEntity(
            name = name,
            amount = amount,
            year = year,
            month = month,
            currency = currency,
            isActive = true,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )
        return budgetDao.insertBudget(budget)
    }

    suspend fun updateBudget(budget: BudgetEntity) {
        budgetDao.updateBudget(budget.copy(updatedAt = LocalDateTime.now()))
    }

    suspend fun deleteBudget(budgetId: Long) {
        budgetDao.deleteBudget(budgetId)
    }

    // Category limit operations
    fun getCategoryLimitsForBudget(budgetId: Long): Flow<List<BudgetCategoryLimitEntity>> {
        return budgetDao.getCategoryLimitsForBudget(budgetId)
    }

    suspend fun getCategoryLimitsForBudgetSync(budgetId: Long): List<BudgetCategoryLimitEntity> {
        return budgetDao.getCategoryLimitsForBudgetSync(budgetId)
    }

    suspend fun addCategoryLimit(
        budgetId: Long,
        categoryName: String,
        limitAmount: BigDecimal
    ): Long {
        val limit = BudgetCategoryLimitEntity(
            budgetId = budgetId,
            categoryName = categoryName,
            limitAmount = limitAmount,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )
        return budgetDao.insertCategoryLimit(limit)
    }

    suspend fun updateCategoryLimit(limit: BudgetCategoryLimitEntity) {
        budgetDao.updateCategoryLimit(limit.copy(updatedAt = LocalDateTime.now()))
    }

    suspend fun deleteCategoryLimit(limitId: Long) {
        budgetDao.deleteCategoryLimit(limitId)
    }

    suspend fun deleteCategoryLimitsForBudget(budgetId: Long) {
        budgetDao.deleteCategoryLimitsForBudget(budgetId)
    }

    // Spending calculation methods
    suspend fun getBudgetWithSpending(budget: BudgetEntity): BudgetWithSpending {
        val yearMonth = YearMonth.of(budget.year, budget.month)
        val startOfMonth = yearMonth.atDay(1).atStartOfDay()
        val endOfMonth = yearMonth.atEndOfMonth().atTime(23, 59, 59)
        val now = LocalDateTime.now()

        // Get transactions for the budget period
        val transactions = transactionDao.getTransactionsBetweenDatesList(startOfMonth, endOfMonth)
            .filter { it.transactionType == TransactionType.EXPENSE && it.currency == budget.currency }

        // Calculate total spending
        val totalSpending = transactions.sumOf { it.amount }

        // Calculate spending per category
        val categorySpending = transactions
            .groupBy { it.category }
            .mapValues { (_, txns) -> txns.sumOf { it.amount } }

        // Get category limits
        val categoryLimits = budgetDao.getCategoryLimitsForBudgetSync(budget.id)

        // Calculate days remaining
        val daysInMonth = yearMonth.lengthOfMonth()
        val daysRemaining = if (now.year == budget.year && now.monthValue == budget.month) {
            (daysInMonth - now.dayOfMonth).coerceAtLeast(0)
        } else if (now.isAfter(endOfMonth)) {
            0
        } else {
            daysInMonth
        }

        return BudgetWithSpending(
            budget = budget,
            currentSpending = totalSpending,
            categoryLimits = categoryLimits,
            categorySpending = categorySpending,
            daysRemaining = daysRemaining,
            daysInMonth = daysInMonth
        )
    }

    fun getBudgetsWithSpendingForMonth(year: Int, month: Int): Flow<List<BudgetWithSpending>> {
        return budgetDao.getActiveBudgetsForMonth(year, month)
            .map { budgets ->
                budgets.map { getBudgetWithSpending(it) }
            }
    }

    suspend fun getCategoryLimitsWithSpending(budgetId: Long): List<CategoryLimitWithSpending> {
        val budget = budgetDao.getBudgetById(budgetId) ?: return emptyList()
        val yearMonth = YearMonth.of(budget.year, budget.month)
        val startOfMonth = yearMonth.atDay(1).atStartOfDay()
        val endOfMonth = yearMonth.atEndOfMonth().atTime(23, 59, 59)

        // Get transactions for the budget period
        val transactions = transactionDao.getTransactionsBetweenDatesList(startOfMonth, endOfMonth)
            .filter { it.transactionType == TransactionType.EXPENSE && it.currency == budget.currency }

        // Calculate spending per category
        val categorySpending = transactions
            .groupBy { it.category }
            .mapValues { (_, txns) -> txns.sumOf { it.amount } }

        // Get category limits and combine with spending
        val limits = budgetDao.getCategoryLimitsForBudgetSync(budgetId)
        return limits.map { limit ->
            CategoryLimitWithSpending(
                limit = limit,
                currentSpending = categorySpending[limit.categoryName] ?: BigDecimal.ZERO
            )
        }
    }
}
