package com.ritesh.cashiro.presentation.subscriptions

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ritesh.cashiro.data.currency.CurrencyConversionService
import com.ritesh.cashiro.data.database.entity.SubscriptionEntity
import com.ritesh.cashiro.data.repository.AccountBalanceRepository
import com.ritesh.cashiro.data.repository.SubscriptionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.math.BigDecimal
import javax.inject.Inject

@HiltViewModel
class SubscriptionsViewModel @Inject constructor(
    private val subscriptionRepository: SubscriptionRepository,
    private val accountBalanceRepository: AccountBalanceRepository,
    private val currencyConversionService: CurrencyConversionService,
    @ApplicationContext private val context: Context
) : ViewModel() {
    
    private val sharedPrefs = context.getSharedPreferences("account_prefs", Context.MODE_PRIVATE)
    
    private val _uiState = MutableStateFlow(SubscriptionsUiState())
    val uiState: StateFlow<SubscriptionsUiState> = _uiState.asStateFlow()
    
    init {
        loadSubscriptions()
    }
    
    private fun loadSubscriptions() {
        viewModelScope.launch {
            subscriptionRepository.getActiveSubscriptions().collect { subscriptions ->
                // Get main account currency for conversion
                val mainAccountKey = sharedPrefs.getString("main_account", null)
                val targetCurrency = if (mainAccountKey != null) {
                    val parts = mainAccountKey.split("_")
                    if (parts.size >= 2) {
                        accountBalanceRepository.getLatestBalance(parts[0], parts[1])?.currency
                            ?: "INR"
                    } else {
                        "INR"
                    }
                } else {
                    "INR"
                }

                // Check if we need to refresh rates for subscription currencies
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
                
                _uiState.value = _uiState.value.copy(
                    activeSubscriptions = subscriptions,
                    totalMonthlyAmount = totalMonthlyAmount,
                    totalYearlyAmount = totalMonthlyAmount * BigDecimal(12),
                    targetCurrency = targetCurrency,
                    isLoading = false
                )
            }
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
}

data class SubscriptionsUiState(
    val activeSubscriptions: List<SubscriptionEntity> = emptyList(),
    val totalMonthlyAmount: BigDecimal = BigDecimal.ZERO,
    val totalYearlyAmount: BigDecimal = BigDecimal.ZERO,
    val targetCurrency: String = "INR",
    val isLoading: Boolean = true,
    val lastHiddenSubscription: SubscriptionEntity? = null
)