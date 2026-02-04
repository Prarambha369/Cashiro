package com.ritesh.cashiro.presentation.ui.features.add

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ritesh.cashiro.data.database.entity.AccountBalanceEntity
import com.ritesh.cashiro.data.database.entity.SubcategoryEntity
import com.ritesh.cashiro.data.database.entity.TransactionType
import com.ritesh.cashiro.data.repository.AccountBalanceRepository
import com.ritesh.cashiro.data.repository.SubcategoryRepository
import com.ritesh.cashiro.domain.usecase.AddSubscriptionUseCase
import com.ritesh.cashiro.domain.usecase.AddTransactionUseCase
import com.ritesh.cashiro.domain.usecase.GetCategoriesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import javax.inject.Inject
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalTime

@HiltViewModel
class AddViewModel
@Inject
constructor(
    private val addTransactionUseCase: AddTransactionUseCase,
    private val addSubscriptionUseCase: AddSubscriptionUseCase,
    private val getCategoriesUseCase: GetCategoriesUseCase,
    private val subcategoryRepository: SubcategoryRepository,
    private val accountBalanceRepository:
    AccountBalanceRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val sharedPrefs = context.getSharedPreferences("account_prefs", Context.MODE_PRIVATE)


    // General UI State
    private val _uiState = MutableStateFlow(AddUiState())
    val uiState: StateFlow<AddUiState> = _uiState.asStateFlow()

    // Transaction Tab State
    private val _transactionUiState = MutableStateFlow(TransactionUiState())
    val transactionUiState: StateFlow<TransactionUiState> = _transactionUiState.asStateFlow()

    // Subscription Tab State
    private val _subscriptionUiState = MutableStateFlow(SubscriptionUiState())
    val subscriptionUiState: StateFlow<SubscriptionUiState> = _subscriptionUiState.asStateFlow()

    // Categories for dropdowns
    val categories =
        getCategoriesUseCase
            .execute()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )

    // Accounts for dropdown
    val accounts = accountBalanceRepository
            .getAllLatestBalances()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )

    // All Subcategories for sheet
    val allSubcategories = subcategoryRepository.subcategoriesMap



    // Subcategories for the selected category
    private val _transactionSubcategories =
        MutableStateFlow<List<SubcategoryEntity>>(
            emptyList()
        )
    val transactionSubcategories:
            StateFlow<List<SubcategoryEntity>> =
        _transactionSubcategories.asStateFlow()

    private val _subscriptionSubcategories =
        MutableStateFlow<List<SubcategoryEntity>>(
            emptyList()
        )
    val subscriptionSubcategories:
            StateFlow<List<SubcategoryEntity>> =
        _subscriptionSubcategories.asStateFlow()

    init {
        // Load default subcategories for both tabs
        updateTransactionSubcategories("Miscellaneous")
        updateSubscriptionSubcategories("Subscription")

        // Pre-select main account
        viewModelScope.launch {
            accounts.filter { it.isNotEmpty() }.first().let { availableAccounts ->
                val mainAccountKey = sharedPrefs.getString("main_account", null)
                if (mainAccountKey != null) {
                    val mainAccount = availableAccounts.find { 
                        "${it.bankName}_${it.accountLast4}" == mainAccountKey 
                    }
                    if (mainAccount != null) {
                        _transactionUiState.update { it.copy(selectedAccount = mainAccount, currency = mainAccount.currency) }
                        _subscriptionUiState.update { it.copy(selectedAccount = mainAccount, currency = mainAccount.currency) }
                    }
                }
            }
        }
    }

    // Transaction Tab Functions
    fun updateTransactionAmount(amount: String) {
        val filtered = amount.filter { it.isDigit() || it == '.' }
        val decimalCount = filtered.count { it == '.' }
        val validAmount = if (decimalCount <= 1) filtered else _transactionUiState.value.amount

        _transactionUiState.update { currentState ->
            currentState.copy(amount = validAmount, amountError = validateAmount(validAmount))
        }
    }

    fun updateTransactionType(type: TransactionType) {
        val newCategory = when (type) {
            TransactionType.INCOME -> "Income"
            TransactionType.EXPENSE -> "Miscellaneous"
            TransactionType.INVESTMENT -> "Investment"
            TransactionType.CREDIT -> "Shopping"
            TransactionType.TRANSFER -> "Self Transfer"
            else -> _transactionUiState.value.category
        }

        _transactionUiState.update { currentState ->
            currentState.copy(
                transactionType = type,
                category = newCategory,
                subcategory = null,
                categoryError = validateCategory(newCategory)
            )
        }

        // Update subcategories for the new default category
        updateTransactionSubcategories(newCategory)
    }

    fun updateTransactionMerchant(merchant: String) {
        _transactionUiState.update { currentState ->
            currentState.copy(merchant = merchant, merchantError = validateMerchant(merchant))
        }
    }

    fun updateTransactionCategory(category: String) {
        _transactionUiState.update { currentState ->
            currentState.copy(
                category = category,
                subcategory = null,
                categoryError = validateCategory(category)
            )
        }

        updateTransactionSubcategories(category)
    }

    private fun updateTransactionSubcategories(category: String) {
        viewModelScope.launch {
            val cat = categories.value.find { it.name == category }
            if (cat != null) {
                subcategoryRepository.getSubcategoriesByCategoryId(cat.id).collect {
                    _transactionSubcategories.value = it
                }
            } else {
                _transactionSubcategories.value = emptyList()
            }
        }
    }

    fun updateTransactionSubcategory(subcategory: String?) {
        _transactionUiState.update { currentState -> currentState.copy(subcategory = subcategory) }
    }

    fun updateTransactionDate(dateMillis: Long) {
        val instant = Instant.ofEpochMilli(dateMillis)
        val localDate = instant.atZone(ZoneId.systemDefault()).toLocalDate()
        val currentTime = _transactionUiState.value.date.toLocalTime()
        val newDateTime = LocalDateTime.of(localDate, currentTime)

        _transactionUiState.update { currentState -> currentState.copy(date = newDateTime) }
    }

    fun updateTransactionTime(hour: Int, minute: Int) {
        val currentDate = _transactionUiState.value.date.toLocalDate()
        val newDateTime = currentDate.atTime(hour, minute)

        _transactionUiState.update { currentState -> currentState.copy(date = newDateTime) }
    }

    fun updateTransactionNotes(notes: String) {
        _transactionUiState.update { currentState -> currentState.copy(notes = notes) }
    }

    fun updateTransactionRecurring(isRecurring: Boolean) {
        _transactionUiState.update { currentState -> currentState.copy(isRecurring = isRecurring) }
    }

    fun saveTransaction(onSuccess: () -> Unit) {
        val state = _transactionUiState.value

        val amountError = validateAmount(state.amount)
        val merchantError = validateMerchant(state.merchant)
        val categoryError = validateCategory(state.category)

        // Additional validation for Transfer transactions
        if (state.transactionType == TransactionType.TRANSFER) {
            if (state.selectedAccount == null || state.targetAccount == null) {
                _transactionUiState.update { currentState ->
                    currentState.copy(
                        error = "Both source and target accounts are required for transfers"
                    )
                }
                return
            }
            
            if (state.selectedAccount?.id == state.targetAccount?.id) {
                _transactionUiState.update { currentState ->
                    currentState.copy(
                        error = "Source and target accounts must be different"
                    )
                }
                return
            }
        }

        if (amountError != null || merchantError != null || categoryError != null) {
            _transactionUiState.update { currentState ->
                currentState.copy(
                    amountError = amountError,
                    merchantError = merchantError,
                    categoryError = categoryError
                )
            }
            return
        }

        viewModelScope.launch {
            try {
                _transactionUiState.update { it.copy(isLoading = true) }

                val amount = BigDecimal(state.amount)

                addTransactionUseCase.execute(
                    amount = amount,
                    merchant = state.merchant.trim(),
                    category = state.category,
                    subcategory = state.subcategory,
                    type = state.transactionType,
                    date = state.date,
                    notes = state.notes.takeIf { it.isNotBlank() },
                    isRecurring = state.isRecurring,
                    bankName = state.selectedAccount?.bankName,
                    accountLast4 = state.selectedAccount?.accountLast4,
                    currency = state.currency,
                    sourceAccountId = state.selectedAccount?.id,
                    targetAccountBankName = state.targetAccount?.bankName,
                    targetAccountLast4 = state.targetAccount?.accountLast4
                )

                onSuccess()
            } catch (e: Exception) {
                _transactionUiState.update { currentState ->
                    currentState.copy(
                        isLoading = false,
                        error = e.message ?: "Failed to save transaction"
                    )
                }
            }
        }
    }

    fun updateTransactionAccount(
        account: AccountBalanceEntity?
    ) {
        _transactionUiState.update { currentState -> 
            currentState.copy(
                selectedAccount = account,
                currency = account?.currency ?: "INR"
            ) 
        }
    }

    fun updateTransactionTargetAccount(
        account: AccountBalanceEntity?
    ) {
        _transactionUiState.update { currentState -> currentState.copy(targetAccount = account) }
    }

    // Subscription Tab Functions
    fun updateSubscriptionService(service: String) {
        _subscriptionUiState.update { currentState ->
            currentState.copy(
                serviceName = service,
                serviceError = if (service.isBlank()) "Service name is required" else null
            )
        }
    }

    fun updateSubscriptionAmount(amount: String) {
        val filtered = amount.filter { it.isDigit() || it == '.' }
        val decimalCount = filtered.count { it == '.' }
        val validAmount = if (decimalCount <= 1) filtered else _subscriptionUiState.value.amount

        _subscriptionUiState.update { currentState ->
            currentState.copy(amount = validAmount, amountError = validateAmount(validAmount))
        }
    }

    fun updateSubscriptionBillingCycle(cycle: String) {
        _subscriptionUiState.update { currentState ->
            currentState.copy(billingCycle = cycle, billingCycleError = null)
        }
    }

    fun updateSubscriptionNextPaymentDate(dateMillis: Long) {
        val instant = Instant.ofEpochMilli(dateMillis)
        val localDate = instant.atZone(ZoneId.systemDefault()).toLocalDate()

        _subscriptionUiState.update { currentState ->
            currentState.copy(nextPaymentDate = localDate)
        }
    }

    fun updateSubscriptionCategory(category: String) {
        _subscriptionUiState.update { currentState ->
            currentState.copy(
                category = category,
                subcategory = null,
                categoryError = validateCategory(category)
            )
        }

        updateSubscriptionSubcategories(category)
    }

    private fun updateSubscriptionSubcategories(category: String) {
        viewModelScope.launch {
            val cat = categories.value.find { it.name == category }
            if (cat != null) {
                subcategoryRepository.getSubcategoriesByCategoryId(cat.id).collect {
                    _subscriptionSubcategories.value = it
                }
            } else {
                _subscriptionSubcategories.value = emptyList()
            }
        }
    }

    fun updateSubscriptionSubcategory(subcategory: String?) {
        _subscriptionUiState.update { currentState -> currentState.copy(subcategory = subcategory) }
    }

    fun updateSubscriptionNotes(notes: String) {
        _subscriptionUiState.update { currentState -> currentState.copy(notes = notes) }
    }

    fun updateSubscriptionAccount(
        account: AccountBalanceEntity?
    ) {
        _subscriptionUiState.update { currentState -> 
            currentState.copy(
                selectedAccount = account,
                currency = account?.currency ?: "INR"
            ) 
        }
    }

    fun saveSubscription(onSuccess: () -> Unit) {
        val state = _subscriptionUiState.value
        Log.d("AddViewModel", "saveSubscription called with state: $state")

        // Validate all fields
        val serviceError = if (state.serviceName.isBlank()) "Service name is required" else null
        val amountError = validateAmount(state.amount)
        val categoryError = validateCategory(state.category)

        Log.d(
            "AddViewModel",
            "Validation - serviceError: $serviceError, amountError: $amountError, categoryError: $categoryError"
        )

        if (serviceError != null || amountError != null || categoryError != null) {
            _subscriptionUiState.update { currentState ->
                currentState.copy(
                    serviceError = serviceError,
                    amountError = amountError,
                    categoryError = categoryError
                )
            }
            return
        }

        viewModelScope.launch {
            try {
                Log.d("AddViewModel", "Starting to save subscription...")
                _subscriptionUiState.update { it.copy(isLoading = true) }

                val amount = BigDecimal(state.amount)

                val transactionDate = state.nextPaymentDate.atTime(LocalTime.now())
                
                addTransactionUseCase.execute(
                    amount = amount,
                    merchant = state.serviceName.trim(),
                    category = state.category,
                    subcategory = state.subcategory,
                    type = TransactionType.EXPENSE, // Subscriptions are expenses
                    date = transactionDate,
                    notes = state.notes.takeIf { it.isNotBlank() },
                    isRecurring = true, // It is part of a subscription
                    bankName = state.selectedAccount?.bankName,
                    accountLast4 = state.selectedAccount?.accountLast4,
                    currency = state.currency,
                    sourceAccountId = state.selectedAccount?.id,
                    billingCycle = state.billingCycle,
                    createSubscription = false
                )

                val actualNextPaymentDate = calculateNextPaymentDate(state.nextPaymentDate, state.billingCycle)

                Log.d(
                    "AddViewModel",
                    "Calling addSubscriptionUseCase.execute with: " +
                            "merchantName=${state.serviceName.trim()}, amount=$amount, " +
                            "nextPaymentDate=$actualNextPaymentDate, billingCycle=${state.billingCycle}, " +
                            "category=${state.category}"
                )

                val subscriptionId =
                    addSubscriptionUseCase.execute(
                        merchantName = state.serviceName.trim(),
                        amount = amount,
                        nextPaymentDate = actualNextPaymentDate,
                        billingCycle = state.billingCycle,
                        category = state.category,
                        subcategory = state.subcategory,
                        bankName = state.selectedAccount?.bankName,
                        autoRenewal = false, // Not implemented yet
                        paymentReminder = false, // Not implemented yet
                        currency = state.currency,
                        notes = state.notes.takeIf { it.isNotBlank() }
                    )

                Log.d("AddViewModel", "Subscription saved successfully with ID: $subscriptionId")
                onSuccess()
            } catch (e: Exception) {
                Log.e("AddViewModel", "Error saving subscription", e)
                e.printStackTrace()
                _subscriptionUiState.update { currentState ->
                    currentState.copy(
                        isLoading = false,
                        error = e.message ?: "Failed to save subscription"
                    )
                }
            } finally {
                _subscriptionUiState.update { it.copy(isLoading = false) }
            }
        }
    }
    
    private fun calculateNextPaymentDate(
        fromDate: LocalDate,
        billingCycle: String?
    ):LocalDate {
        return when (billingCycle) {
            "Weekly" -> fromDate.plusWeeks(1)
            "Monthly" -> fromDate.plusMonths(1)
            "Quarterly" -> fromDate.plusMonths(3)
            "Semi-Annual" -> fromDate.plusMonths(6)
            "Annual" -> fromDate.plusYears(1)
            else -> fromDate.plusMonths(1)
        }
    }

    // Validation helpers
    private fun validateAmount(amount: String): String? {
        return when {
            amount.isBlank() -> "Amount is required"
            amount.toDoubleOrNull() == null -> "Invalid amount"
            amount.toDouble() <= 0 -> "Amount must be greater than 0"
            else -> null
        }
    }

    private fun validateMerchant(merchant: String): String? {
        return when {
            merchant.isBlank() -> "Merchant/Description is required"
            merchant.length < 2 -> "Too short"
            else -> null
        }
    }

    private fun validateCategory(category: String): String? {
        return when {
            category.isBlank() -> "Category is required"
            else -> null
        }
    }
}

// UI State Classes
data class AddUiState(val currentTab: Int = 0)

data class TransactionUiState(
    val amount: String = "",
    val amountError: String? = null,
    val transactionType: TransactionType = TransactionType.EXPENSE,
    val merchant: String = "",
    val merchantError: String? = null,
    val category: String = "Miscellaneous",
    val subcategory: String? = null,
    val categoryError: String? = null,
    val date: LocalDateTime = LocalDateTime.now(),
    val notes: String = "",
    val isRecurring: Boolean = false,
    val selectedAccount: AccountBalanceEntity? = null,
    val targetAccount: AccountBalanceEntity? = null,
    val currency: String = "INR",
    val isLoading: Boolean = false,
    val error: String? = null
) {
    val isValid: Boolean
        get() =
            amount.isNotBlank() &&
                    amount.toDoubleOrNull() != null &&
                    amount.toDouble() > 0 &&
                    merchant.isNotBlank() &&
                    category.isNotBlank() &&
                    amountError == null &&
                    merchantError == null &&
                    categoryError == null
}

data class SubscriptionUiState(
    val serviceName: String = "",
    val serviceError: String? = null,
    val amount: String = "",
    val amountError: String? = null,
    val billingCycle: String = "Monthly",
    val billingCycleError: String? = null,
    val nextPaymentDate: LocalDate = LocalDate.now(), // Default to today as "First Payment Date"
    val category: String = "Subscription",
    val subcategory: String? = null,
    val categoryError: String? = null,
    val selectedAccount: AccountBalanceEntity? = null,
    val currency: String = "INR",
    val notes: String = "",
    val isLoading: Boolean = false,
    val error: String? = null
) {
    val isValid: Boolean
        get() =
            serviceName.isNotBlank() &&
                    amount.isNotBlank() &&
                    amount.toDoubleOrNull() != null &&
                    amount.toDouble() > 0 &&
                    billingCycle.isNotBlank() &&
                    category.isNotBlank() &&
                    serviceError == null &&
                    amountError == null &&
                    categoryError == null
}
