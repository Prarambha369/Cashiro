package com.ritesh.cashiro.presentation.ui.features.subscriptions

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ritesh.cashiro.data.currency.CurrencyConversionService
import com.ritesh.cashiro.data.database.entity.SubscriptionEntity
import com.ritesh.cashiro.data.repository.AccountBalanceRepository
import com.ritesh.cashiro.data.repository.CategoryRepository
import com.ritesh.cashiro.data.repository.SubcategoryRepository
import com.ritesh.cashiro.data.repository.SubscriptionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.math.BigDecimal
import javax.inject.Inject
import com.ritesh.cashiro.data.repository.CurrencyRepository
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update

@HiltViewModel
class SubscriptionsViewModel @Inject constructor(
    private val subscriptionRepository: SubscriptionRepository,
    private val accountBalanceRepository: AccountBalanceRepository,
    private val categoryRepository: CategoryRepository,
    private val subcategoryRepository: SubcategoryRepository,
    private val currencyConversionService: CurrencyConversionService,
    private val currencyRepository: CurrencyRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {
    
    private val sharedPrefs = context.getSharedPreferences("account_prefs", Context.MODE_PRIVATE)
    
    private val _uiState = MutableStateFlow(SubscriptionsUiState())
    val uiState: StateFlow<SubscriptionsUiState> = _uiState.asStateFlow()

    val categoriesMap = categoryRepository.getAllCategories()
        .map { cats -> cats.associateBy { it.name } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    val subcategoriesMap = subcategoryRepository.getAllSubcategories()
        .map { subcats -> subcats.associateBy { it.name } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())
    
    init {
        loadSubscriptions()
    }
    
    private fun loadSubscriptions() {
        viewModelScope.launch {
            combine(
                subscriptionRepository.getActiveSubscriptions(),
                currencyRepository.baseCurrencyCode
            ) { subscriptions, targetCurrency ->
                val subscriptionCurrencies = subscriptions.map { it.currency }.distinct()
                if (subscriptionCurrencies.any { it != targetCurrency }) {
                    currencyConversionService.refreshExchangeRatesForAccount(subscriptionCurrencies + targetCurrency)
                }

                val totalMonthlyAmount = subscriptions.sumOf { subscription ->
                    if (subscription.currency == targetCurrency) {
                        subscription.amount
                    } else {
                        currencyConversionService.convertAmount(
                            amount = subscription.amount,
                            fromCurrency = subscription.currency,
                            toCurrency = targetCurrency
                        ) ?: subscription.amount
                    }
                }
                
                _uiState.update { 
                    it.copy(
                        activeSubscriptions = subscriptions,
                        totalMonthlyAmount = totalMonthlyAmount,
                        totalYearlyAmount = totalMonthlyAmount * BigDecimal(12),
                        targetCurrency = targetCurrency,
                        isLoading = false
                    )
                }
            }.collectLatest { }
        }
    }
    
    fun hideSubscription(subscriptionId: Long) {
        viewModelScope.launch {
            subscriptionRepository.hideSubscription(subscriptionId)
            _uiState.value = _uiState.value.copy(
                lastHiddenSubscription = _uiState.value.activeSubscriptions.find { it.id == subscriptionId }
            )
        }
    }
    
    fun undoHide() {
        _uiState.value.lastHiddenSubscription?.let { subscription ->
            viewModelScope.launch {
                subscriptionRepository.unhideSubscription(subscription.id)
                _uiState.value = _uiState.value.copy(lastHiddenSubscription = null)
            }
        }
    }

    fun selectSubscription(subscription: SubscriptionEntity?) {
        _uiState.value = _uiState.value.copy(selectedSubscription = subscription)
    }

    fun markAsPaid(subscription: SubscriptionEntity) {
        viewModelScope.launch {
            val today = java.time.LocalDate.now()
            val nextDate = calculateNextBillingDate(subscription.nextPaymentDate ?: today, subscription.billingCycle)
            subscriptionRepository.updatePaymentStatus(subscription.id, nextDate, today)
            selectSubscription(null)
        }
    }

    private fun calculateNextBillingDate(currentDate: java.time.LocalDate, billingCycle: String?): java.time.LocalDate {
        val today = java.time.LocalDate.now()
        var nextDate = when (billingCycle?.lowercase()) {
            "weekly" -> currentDate.plusWeeks(1)
            "quarterly" -> currentDate.plusMonths(3)
            "semi-annual" -> currentDate.plusMonths(6)
            "annual" -> currentDate.plusYears(1)
            else -> currentDate.plusMonths(1) // Default to monthly
        }

        // Catch up to today if needed
        while (nextDate.isBefore(today)) {
            nextDate = when (billingCycle?.lowercase()) {
                "weekly" -> nextDate.plusWeeks(1)
                "quarterly" -> nextDate.plusMonths(3)
                "semi-annual" -> nextDate.plusMonths(6)
                "annual" -> nextDate.plusYears(1)
                else -> nextDate.plusMonths(1)
            }
        }
        return nextDate
    }
}