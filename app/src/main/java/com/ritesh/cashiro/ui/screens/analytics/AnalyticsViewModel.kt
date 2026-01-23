package com.ritesh.cashiro.ui.screens.analytics


import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ritesh.cashiro.data.repository.TransactionRepository
import com.ritesh.cashiro.presentation.common.TimePeriod
import com.ritesh.cashiro.presentation.common.TransactionTypeFilter
import com.ritesh.cashiro.presentation.common.getDateRangeForPeriod
import com.ritesh.cashiro.ui.components.BalancePoint
import com.ritesh.cashiro.utils.CurrencyUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject

@HiltViewModel
class AnalyticsViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val savedStateHandle: androidx.lifecycle.SavedStateHandle
) : ViewModel() {
    
    private val _selectedPeriod = MutableStateFlow(TimePeriod.THIS_MONTH)
    val selectedPeriod: StateFlow<TimePeriod> = _selectedPeriod.asStateFlow()
    
    private val _transactionTypeFilter = MutableStateFlow(TransactionTypeFilter.EXPENSE)
    val transactionTypeFilter: StateFlow<TransactionTypeFilter> = _transactionTypeFilter.asStateFlow()

    private val _selectedCurrency = MutableStateFlow("INR") // Default to INR
    val selectedCurrency: StateFlow<String> = _selectedCurrency.asStateFlow()

    // Store custom date range as epoch days to survive process death
    // Stored as Pair<Long, Long> (startEpochDay, endEpochDay) in SavedStateHandle
    private val _customDateRangeEpochDays = savedStateHandle.getStateFlow<Pair<Long, Long>?>("customDateRange", null)

    // Expose as LocalDate pair for convenience
    val customDateRange: StateFlow<Pair<LocalDate, LocalDate>?> = _customDateRangeEpochDays
        .map { epochDays ->
            epochDays?.let { (startEpochDay, endEpochDay) ->
                LocalDate.ofEpochDay(startEpochDay) to LocalDate.ofEpochDay(endEpochDay)
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = null
        )

    private val _availableCurrencies = MutableStateFlow<List<String>>(emptyList())
    val availableCurrencies: StateFlow<List<String>> = _availableCurrencies.asStateFlow()

    // Reactive UI state that automatically updates when any filter changes
    // Uses flatMapLatest to cancel previous data loads when filters change (prevents race conditions)
    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<AnalyticsUiState> = combine(
        _selectedPeriod,
        customDateRange,
        _transactionTypeFilter,
        _selectedCurrency
    ) { period, customRange, typeFilter, currency ->
        // Combine all filter states
        FilterState(period, customRange, typeFilter, currency)
    }.flatMapLatest { filterState ->
        // Determine date range based on selected period
        val dateRange = if (filterState.period == TimePeriod.CUSTOM) {
            val customRange = filterState.customRange
            // Guard against invalid state: CUSTOM period must have a date range
            if (customRange == null) {
                android.util.Log.e("AnalyticsViewModel",
                    "CUSTOM period selected but no date range set - falling back to THIS_MONTH")
                // Auto-correct the invalid state
                _selectedPeriod.value = TimePeriod.THIS_MONTH
                getDateRangeForPeriod(TimePeriod.THIS_MONTH)
            } else {
                customRange
            }
        } else {
            getDateRangeForPeriod(filterState.period)
        }

        if (dateRange == null) {
            // No valid date range, return empty state
            flowOf(AnalyticsUiState(isLoading = false))
        } else {
            // First load all transactions for the date range to get available currencies
            transactionRepository.getTransactionsBetweenDates(
                startDate = dateRange.first,
                endDate = dateRange.second
            ).flatMapLatest { allTransactions ->
                // Update available currencies using standard sorting (INR first, then alphabetical)
                val allCurrencies = CurrencyUtils.sortCurrencies(
                    allTransactions.map { it.currency }.distinct()
                )
                _availableCurrencies.value = allCurrencies

                // Auto-select primary currency if not already selected or if current currency no longer exists
                val currentSelectedCurrency = filterState.currency
                if (!allCurrencies.contains(currentSelectedCurrency) && allCurrencies.isNotEmpty()) {
                    _selectedCurrency.value = if (allCurrencies.contains("INR")) "INR" else allCurrencies.first()
                }

                // Use database-level filtering for better performance
                // Convert TransactionTypeFilter to TransactionType for database query
                val dbTransactionType = when (filterState.typeFilter) {
                    TransactionTypeFilter.ALL -> null // null means no type filter at DB level
                    TransactionTypeFilter.INCOME -> com.ritesh.cashiro.data.database.entity.TransactionType.INCOME
                    TransactionTypeFilter.EXPENSE -> com.ritesh.cashiro.data.database.entity.TransactionType.EXPENSE
                    TransactionTypeFilter.CREDIT -> com.ritesh.cashiro.data.database.entity.TransactionType.CREDIT
                    TransactionTypeFilter.TRANSFER -> com.ritesh.cashiro.data.database.entity.TransactionType.TRANSFER
                    TransactionTypeFilter.INVESTMENT -> com.ritesh.cashiro.data.database.entity.TransactionType.INVESTMENT
                }

                // Load filtered transactions from database (filtered at DB level for performance)
                transactionRepository.getTransactionsFiltered(
                    startDate = dateRange.first,
                    endDate = dateRange.second,
                    currency = filterState.currency,
                    transactionType = dbTransactionType
                )
            }.map { filteredTransactions ->

                // Calculate total
                val totalSpending = filteredTransactions.sumOf { it.amount.toDouble() }.toBigDecimal()

                // Group by category
                val categoryBreakdown = filteredTransactions
                    .groupBy { it.category ?: "Miscellaneous" }
                    .map { (categoryName, txns) ->
                        val categoryTotal = txns.sumOf { it.amount.toDouble() }.toBigDecimal()
                        CategoryData(
                            name = categoryName,
                            amount = categoryTotal,
                            percentage = if (totalSpending > BigDecimal.ZERO) {
                                (categoryTotal.divide(totalSpending, 4, java.math.RoundingMode.HALF_UP) * BigDecimal(100)).toFloat()
                            } else 0f,
                            transactionCount = txns.size
                        )
                    }
                    .sortedByDescending { it.amount }

                // Group by merchant
                val merchantBreakdown = filteredTransactions
                    .groupBy { it.merchantName }
                    .mapValues { (merchant, txns) ->
                        MerchantData(
                            name = merchant,
                            amount = txns.sumOf { it.amount.toDouble() }.toBigDecimal(),
                            transactionCount = txns.size,
                            isSubscription = txns.any { it.isRecurring }
                        )
                    }
                    .values
                    .sortedByDescending { it.amount }
                    .take(10) // Top 10 merchants

                // Calculate average amount
                val averageAmount = if (filteredTransactions.isNotEmpty()) {
                    totalSpending.divide(BigDecimal(filteredTransactions.size), 2, java.math.RoundingMode.HALF_UP)
                } else {
                    BigDecimal.ZERO
                }

                // Get top category info
                val topCategory = categoryBreakdown.firstOrNull()

                AnalyticsUiState(
                    totalSpending = totalSpending,
                    categoryBreakdown = categoryBreakdown,
                    topMerchants = merchantBreakdown,
                    transactionCount = filteredTransactions.size,
                    averageAmount = averageAmount,
                    topCategory = topCategory?.name,
                    topCategoryPercentage = topCategory?.percentage ?: 0f,
                    currency = filterState.currency,
                    isLoading = false,
                    spendingTrend = calculateSpendingTrend(filteredTransactions, dateRange.first, dateRange.second)
                )
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Lazily,
        initialValue = AnalyticsUiState(isLoading = true)
    )

    fun selectPeriod(period: TimePeriod) {
        _selectedPeriod.value = period
    }

    fun setTransactionTypeFilter(filter: TransactionTypeFilter) {
        _transactionTypeFilter.value = filter
    }

    fun selectCurrency(currency: String) {
        _selectedCurrency.value = currency
    }

    /**
     * Sets a custom date range filter and switches the period to CUSTOM.
     * Date range is persisted in SavedStateHandle to survive process death.
     *
     * @param startDate The start date (inclusive)
     * @param endDate The end date (inclusive)
     * @throws IllegalArgumentException if startDate > endDate
     */
    fun setCustomDateRange(startDate: LocalDate, endDate: LocalDate) {
        require(startDate <= endDate) {
            "Start date ($startDate) must be before or equal to end date ($endDate)"
        }
        // Store as epoch days for process death survival
        savedStateHandle["customDateRange"] = startDate.toEpochDay() to endDate.toEpochDay()
        _selectedPeriod.value = TimePeriod.CUSTOM
    }

    /**
     * Clears the custom date range and resets to THIS_MONTH period.
     * Always safe to call - ensures we never have CUSTOM period with null dates.
     */
    fun clearCustomDateRange() {
        savedStateHandle["customDateRange"] = null
        // Always reset to a valid period to prevent CUSTOM with null dates
        if (_selectedPeriod.value == TimePeriod.CUSTOM) {
            _selectedPeriod.value = TimePeriod.THIS_MONTH
        }
    }

    private fun calculateSpendingTrend(
        transactions: List<com.ritesh.cashiro.data.database.entity.TransactionEntity>,
        startDate: LocalDate,
        endDate: LocalDate
    ): List<BalancePoint> {
        val selectedPeriod = _selectedPeriod.value
        val trend = mutableListOf<BalancePoint>()

        val currency = transactions.firstOrNull()?.currency ?: _selectedCurrency.value

        when {
            selectedPeriod == TimePeriod.ALL || selectedPeriod == TimePeriod.CURRENT_FY -> {
                // Determine the actual start date for "All Time" to avoid showing years of empty data
                val actualStartDate = if (selectedPeriod == TimePeriod.ALL && transactions.isNotEmpty()) {
                    val firstTxDate = transactions.minByOrNull { it.dateTime }?.dateTime?.toLocalDate() ?: startDate
                    if (firstTxDate.isAfter(startDate)) firstTxDate.withDayOfMonth(1) else startDate
                } else {
                    startDate
                }

                // Decide aggregation level
                val yearsInRange = java.time.temporal.ChronoUnit.YEARS.between(actualStartDate, endDate)
                val aggregateByYear = selectedPeriod == TimePeriod.ALL && yearsInRange >= 2

                if (aggregateByYear) {
                    var currentYear = actualStartDate.withDayOfYear(1)
                    val lastYear = endDate.withDayOfYear(1)

                    while (!currentYear.isAfter(lastYear) && !currentYear.isAfter(LocalDate.now().withDayOfYear(1))) {
                        val endOfYear = currentYear.withDayOfYear(currentYear.lengthOfYear())
                        val transactionsForYear = transactions.filter {
                            !it.dateTime.toLocalDate().isBefore(currentYear) && !it.dateTime.toLocalDate().isAfter(endOfYear)
                        }
                        val totalAmount = transactionsForYear.sumOf { it.amount.toDouble() }.toBigDecimal()

                        trend.add(
                            BalancePoint(
                                timestamp = currentYear.atStartOfDay(),
                                balance = totalAmount,
                                currency = currency
                            )
                        )
                        currentYear = currentYear.plusYears(1)
                    }
                } else {
                    // Aggregate by Month
                    var currentMonth = actualStartDate.withDayOfMonth(1)
                    val lastMonth = endDate.withDayOfMonth(1)

                    while (!currentMonth.isAfter(lastMonth) && !currentMonth.isAfter(LocalDate.now().withDayOfMonth(1))) {
                        val endOfMonth = currentMonth.withDayOfMonth(currentMonth.lengthOfMonth())
                        val transactionsForMonth = transactions.filter {
                            !it.dateTime.toLocalDate().isBefore(currentMonth) && !it.dateTime.toLocalDate().isAfter(endOfMonth)
                        }
                        val totalAmount = transactionsForMonth.sumOf { it.amount.toDouble() }.toBigDecimal()

                        trend.add(
                            BalancePoint(
                                timestamp = currentMonth.atStartOfDay(),
                                balance = totalAmount,
                                currency = currency
                            )
                        )
                        currentMonth = currentMonth.plusMonths(1)
                    }
                }
            }
            else -> {
                // Daily aggregation for smaller periods (This Month, Last Month, etc.)
                val transactionsByDate = transactions.groupBy { it.dateTime.toLocalDate() }
                var currentDate = startDate
                while (!currentDate.isAfter(endDate) && !currentDate.isAfter(LocalDate.now())) {
                    val transactionsForDay = transactionsByDate[currentDate] ?: emptyList()
                    val totalAmount = transactionsForDay.sumOf { it.amount.toDouble() }.toBigDecimal()

                    trend.add(
                        BalancePoint(
                            timestamp = currentDate.atStartOfDay(),
                            balance = totalAmount,
                            currency = currency
                        )
                    )
                    currentDate = currentDate.plusDays(1)
                }
            }
        }
        return trend
    }
}


/**
 * Internal state for combining all filter parameters.
 * Used in reactive Flow to trigger data reload when any filter changes.
 */
private data class FilterState(
    val period: TimePeriod,
    val customRange: Pair<LocalDate, LocalDate>?,
    val typeFilter: TransactionTypeFilter,
    val currency: String
)

data class AnalyticsUiState(
    val totalSpending: BigDecimal = BigDecimal.ZERO,
    val categoryBreakdown: List<CategoryData> = emptyList(),
    val topMerchants: List<MerchantData> = emptyList(),
    val transactionCount: Int = 0,
    val averageAmount: BigDecimal = BigDecimal.ZERO,
    val topCategory: String? = null,
    val topCategoryPercentage: Float = 0f,
    val currency: String = "INR",
    val isLoading: Boolean = true,
    val spendingTrend: List<BalancePoint> = emptyList()
)

data class CategoryData(
    val name: String,
    val amount: BigDecimal,
    val percentage: Float,
    val transactionCount: Int
)

data class MerchantData(
    val name: String,
    val amount: BigDecimal,
    val transactionCount: Int,
    val isSubscription: Boolean
)

