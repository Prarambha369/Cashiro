package com.ritesh.cashiro.presentation.transactions

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ritesh.cashiro.data.currency.CurrencyConversionService
import com.ritesh.cashiro.data.database.entity.CategoryEntity
import com.ritesh.cashiro.data.database.entity.TransactionEntity
import com.ritesh.cashiro.data.database.entity.TransactionType
import com.ritesh.cashiro.data.repository.AccountBalanceRepository
import com.ritesh.cashiro.data.repository.CategoryRepository
import com.ritesh.cashiro.data.repository.MerchantMappingRepository
import com.ritesh.cashiro.data.repository.TransactionRepository
import com.ritesh.cashiro.data.repository.SubcategoryRepository
import com.ritesh.cashiro.data.repository.SubscriptionRepository
import com.ritesh.cashiro.data.database.entity.SubscriptionEntity
import com.ritesh.cashiro.data.database.entity.SubscriptionState
import com.ritesh.cashiro.core.Constants
import com.ritesh.cashiro.data.database.entity.AccountBalanceEntity
import com.ritesh.cashiro.data.database.entity.SubcategoryEntity
import com.ritesh.cashiro.utils.DeviceEncryption
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.time.LocalDateTime
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class TransactionDetailViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val merchantMappingRepository: MerchantMappingRepository,
    private val categoryRepository: CategoryRepository,
    private val subcategoryRepository: SubcategoryRepository,
    private val accountBalanceRepository: AccountBalanceRepository,
    private val subscriptionRepository: SubscriptionRepository,
    private val currencyConversionService: CurrencyConversionService,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _transaction = MutableStateFlow<TransactionEntity?>(null)
    val transaction: StateFlow<TransactionEntity?> = _transaction.asStateFlow()

    private val _primaryCurrency = MutableStateFlow("INR")
    val primaryCurrency: StateFlow<String> = _primaryCurrency.asStateFlow()

    private val _convertedAmount = MutableStateFlow<BigDecimal?>(null)
    val convertedAmount: StateFlow<BigDecimal?> = _convertedAmount.asStateFlow()

    private val _isEditMode = MutableStateFlow(false)
    val isEditMode: StateFlow<Boolean> = _isEditMode.asStateFlow()

    private val _editableTransaction = MutableStateFlow<TransactionEntity?>(null)
    val editableTransaction: StateFlow<TransactionEntity?> = _editableTransaction.asStateFlow()

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    private val _saveSuccess = MutableStateFlow(false)
    val saveSuccess: StateFlow<Boolean> = _saveSuccess.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _applyToAllFromMerchant = MutableStateFlow(false)
    val applyToAllFromMerchant: StateFlow<Boolean> = _applyToAllFromMerchant.asStateFlow()

    private val _updateExistingTransactions = MutableStateFlow(false)
    val updateExistingTransactions: StateFlow<Boolean> = _updateExistingTransactions.asStateFlow()

    private val _existingTransactionCount = MutableStateFlow(0)

    private val _showDeleteDialog = MutableStateFlow(false)
    val showDeleteDialog: StateFlow<Boolean> = _showDeleteDialog.asStateFlow()

    private val _isDeleting = MutableStateFlow(false)
    val isDeleting: StateFlow<Boolean> = _isDeleting.asStateFlow()

    private val _deleteSuccess = MutableStateFlow(false)
    val deleteSuccess: StateFlow<Boolean> = _deleteSuccess.asStateFlow()
    val existingTransactionCount: StateFlow<Int> = _existingTransactionCount.asStateFlow()

    private val _subscription = MutableStateFlow<SubscriptionEntity?>(null)
    val subscription: StateFlow<SubscriptionEntity?> = _subscription.asStateFlow()

    // Categories should be based on transaction type
    val categories: StateFlow<List<CategoryEntity>> = combine(
        _editableTransaction,
        _transaction
    ) { editable, original ->
        val transaction = editable ?: original
        transaction?.transactionType == TransactionType.INCOME
    }.flatMapLatest { isIncome ->
        if (isIncome) {
            categoryRepository.getIncomeCategories()
        } else {
            categoryRepository.getExpenseCategories()
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Available accounts for linking (excluding hidden accounts)
    private val sharedPrefs =
        context.getSharedPreferences("account_prefs", Context.MODE_PRIVATE)

    val availableAccounts = accountBalanceRepository.getAllLatestBalances()
        .map { balances ->
            val hiddenAccounts =
                sharedPrefs.getStringSet("hidden_accounts", emptySet()) ?: emptySet()
            balances
                .filter { balance ->
                    val key = "${balance.bankName}_${balance.accountLast4}"
                    !hiddenAccounts.contains(key)
                }
                .map { balance ->
                    AccountInfo(
                        id = balance.id,
                        bankName = balance.bankName,
                        accountLast4 = balance.accountLast4,
                        displayName = "${balance.bankName} ••••${balance.accountLast4}",
                        isCreditCard = balance.isCreditCard,
                        iconResId = balance.iconResId,
                        currency = balance.currency
                    )
                }
                .distinctBy { "${it.bankName}_${it.accountLast4}" }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val selectedAccount: StateFlow<AccountInfo?> = combine(
        _editableTransaction,
        availableAccounts
    ) { transaction, accounts ->
        if (transaction == null) return@combine null
        accounts.find {
            it.bankName == transaction.bankName && it.accountLast4 == transaction.accountNumber
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val targetAccount: StateFlow<AccountInfo?> = combine(
        _editableTransaction,
        availableAccounts
    ) { transaction, accounts ->
        if (transaction == null) return@combine null
        accounts.find { it.accountLast4 == transaction.toAccount }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)


    val allSubcategories: StateFlow<Map<Long, List<SubcategoryEntity>>> =
        subcategoryRepository.subcategoriesMap

    data class AccountInfo(
        val id: Long,
        val bankName: String,
        val accountLast4: String,
        val displayName: String,
        val isCreditCard: Boolean,
        val iconResId: Int,
        val currency: String
    )

    fun loadTransaction(transactionId: Long) {
        viewModelScope.launch {
            val transaction = transactionRepository.getTransactionById(transactionId)
            _transaction.value = transaction
            transaction?.let {
                determinePrimaryCurrency(it)
                calculateConvertedAmount(it)
                findLinkedSubscription(it)
            }
        }
    }

    private suspend fun findLinkedSubscription(transaction: TransactionEntity) {
        if (transaction.isRecurring) {
            val linked = subscriptionRepository.matchTransactionToSubscription(
                transaction.merchantName,
                transaction.amount
            )
            _subscription.value = linked
        } else {
            _subscription.value = null
        }
    }

    private suspend fun determinePrimaryCurrency(transaction: TransactionEntity) {
        val bankName = transaction.bankName
        val primaryCurrency = if (!bankName.isNullOrEmpty()) {
            com.ritesh.cashiro.utils.CurrencyFormatter.getBankBaseCurrency(bankName)
        } else {
            transaction.currency.takeIf { it.isNotEmpty() } ?: "INR"
        }
        _primaryCurrency.value = primaryCurrency
    }

    private suspend fun calculateConvertedAmount(transaction: TransactionEntity) {
        val primaryCurrency = _primaryCurrency.value
        if (transaction.currency.isNotEmpty() && !transaction.currency.equals(
                primaryCurrency,
                ignoreCase = true
            )
        ) {
            // Convert the amount to the primary currency
            val converted = currencyConversionService.convertAmount(
                amount = transaction.amount,
                fromCurrency = transaction.currency,
                toCurrency = primaryCurrency
            )
            _convertedAmount.value = converted
        } else {
            // No conversion needed if currencies are the same
            _convertedAmount.value = null
        }
    }

    fun enterEditMode() {
        _editableTransaction.value = _transaction.value?.copy()
        _isEditMode.value = true
        _errorMessage.value = null

        // Load count of other transactions from same merchant
        _transaction.value?.let { txn ->
            viewModelScope.launch {
                val count = transactionRepository.getOtherTransactionCountForMerchant(
                    txn.merchantName,
                    txn.id
                )
                _existingTransactionCount.value = count
            }
        }
    }

    fun exitEditMode() {
        _editableTransaction.value = null
        _isEditMode.value = false
        _errorMessage.value = null
        _applyToAllFromMerchant.value = false
        _updateExistingTransactions.value = false
        _existingTransactionCount.value = 0
    }

    fun toggleApplyToAllFromMerchant() {
        _applyToAllFromMerchant.value = !_applyToAllFromMerchant.value
    }

    fun toggleUpdateExistingTransactions() {
        _updateExistingTransactions.value = !_updateExistingTransactions.value
    }

    fun updateMerchantName(name: String) {
        _editableTransaction.update { current ->
            current?.copy(merchantName = name)
        }
        validateMerchantName(name)
    }

    fun updateAmount(amountStr: String) {
        val amount = amountStr.toBigDecimalOrNull()
        if (amount != null && amount > BigDecimal.ZERO) {
            _editableTransaction.update { current ->
                current?.copy(amount = amount)
            }
            _errorMessage.value = null
        } else if (amountStr.isNotEmpty()) {
            _errorMessage.value = "Amount must be a positive number"
        }
    }

    fun updateTransactionType(type: TransactionType) {
        _editableTransaction.update { current ->
            current?.copy(transactionType = type)
        }

        // Auto-select category based on type
        val newCategory = when (type) {
            TransactionType.INCOME -> "Salary"
            TransactionType.EXPENSE -> "Miscellaneous"
            TransactionType.CREDIT -> "Credit Bill"
            TransactionType.TRANSFER -> {
                val targetAccount = _editableTransaction.value?.toAccount
                if (targetAccount == "wallet") "Cash Withdrawal" else "Self Transfer"
            }

            TransactionType.INVESTMENT -> "Investment"
        }

        updateCategory(newCategory)
    }

    fun updateCategory(category: String) {
        _editableTransaction.update { current ->
            current?.copy(
                category = category.ifEmpty { "Miscellaneous" },
                subcategory = if (current?.category != category) null else current.subcategory
            )
        }
    }

    fun updateSubcategory(subcategory: String?) {
        _editableTransaction.update { current ->
            current?.copy(subcategory = subcategory)
        }
    }

    fun updateDateTime(dateTime: LocalDateTime) {
        _editableTransaction.update { current ->
            current?.copy(dateTime = dateTime)
        }
    }

    fun updateDescription(description: String?) {
        _editableTransaction.update { current ->
            current?.copy(description = if (description.isNullOrEmpty()) null else description)
        }
    }

    fun updateRecurringStatus(isRecurring: Boolean) {
        _editableTransaction.update { current ->
            current?.copy(
                isRecurring = isRecurring,
                billingCycle = if (isRecurring) current.billingCycle ?: "Monthly" else null
            )
        }
    }

    fun updateBillingCycle(cycle: String) {
        _editableTransaction.update { current ->
            current?.copy(billingCycle = cycle)
        }
    }

    fun updateAccountNumber(accountNumber: String?) {
        _editableTransaction.update { current ->
            current?.copy(accountNumber = if (accountNumber.isNullOrEmpty()) null else accountNumber)
        }
    }

    fun updateTransactionAccount(account: AccountInfo?) {
        _editableTransaction.update { current ->
            current?.copy(
                bankName = account?.bankName ?: "Manual Entry",
                accountNumber = account?.accountLast4,
                currency = account?.currency ?: current.currency
            )
        }
    }

    fun updateTransactionTargetAccount(account: AccountInfo?) {
        _editableTransaction.update { current ->
            current?.copy(
                toAccount = account?.accountLast4
            )
        }

        // Update category if type is TRANSFER
        _editableTransaction.value?.let { txn ->
            if (txn.transactionType == TransactionType.TRANSFER) {
                val newCategory = if (account?.accountLast4 == "wallet") {
                    "Cash Withdrawal"
                } else {
                    "Self Transfer"
                }
                updateCategory(newCategory)
            }
        }
    }

    fun updateCurrency(currency: String) {
        _editableTransaction.update { current ->
            current?.copy(currency = currency)
        }
        // Recalculate converted amount when currency changes
        _editableTransaction.value?.let { transaction ->
            viewModelScope.launch {
                calculateConvertedAmount(transaction)
            }
        }
    }

    fun saveChanges() {
        val toSave = _editableTransaction.value ?: return

        // Validate before saving
        if (toSave.merchantName.isBlank()) {
            _errorMessage.value = "Merchant name is required"
            return
        }

        if (toSave.amount <= BigDecimal.ZERO) {
            _errorMessage.value = "Amount must be positive"
            return
        }

        viewModelScope.launch {
            _isSaving.value = true
            try {
                // Normalize merchant name before saving
                val normalizedTransaction = toSave.copy(
                    merchantName = normalizeMerchantName(toSave.merchantName),
                    updatedAt = LocalDateTime.now()
                )

                transactionRepository.updateTransaction(normalizedTransaction)

                // Handle Balance Updates
                val originalTransaction = _transaction.value
                if (originalTransaction != null) {
                    updateAccountBalances(originalTransaction, normalizedTransaction)
                }

                // Sync with subscriptions if recurring
                syncSubscriptionForTransaction(normalizedTransaction)

                // Save merchant mapping if checkbox is checked
                if (_applyToAllFromMerchant.value) {
                    merchantMappingRepository.setMapping(
                        normalizedTransaction.merchantName,
                        normalizedTransaction.category
                    )
                }

                // Update existing transactions if checkbox is checked
                if (_updateExistingTransactions.value) {
                    transactionRepository.updateCategoryForMerchant(
                        normalizedTransaction.merchantName,
                        normalizedTransaction.category
                    )
                }

                _transaction.value = normalizedTransaction
                findLinkedSubscription(normalizedTransaction) // Refresh linked subscription
                _saveSuccess.value = true
                _isEditMode.value = false
                _editableTransaction.value = null
                _errorMessage.value = null
                _applyToAllFromMerchant.value = false
                _updateExistingTransactions.value = false
                _existingTransactionCount.value = 0
            } catch (e: Exception) {
                _errorMessage.value = "Failed to save changes: ${e.message}"
            } finally {
                _isSaving.value = false
            }
        }
    }

    fun cancelEdit() {
        exitEditMode()
    }

    fun clearSaveSuccess() {
        _saveSuccess.value = false
    }

    private fun validateMerchantName(name: String) {
        if (name.isBlank()) {
            _errorMessage.value = "Merchant name is required"
        } else {
            _errorMessage.value = null
        }
    }

    /**
     * Normalizes merchant name to consistent format.
     * Converts all-caps to proper case, preserves already mixed case.
     */
    private fun normalizeMerchantName(name: String): String {
        val trimmed = name.trim()

        // If it's all uppercase, convert to proper case
        return if (trimmed == trimmed.uppercase()) {
            trimmed.lowercase().split(" ").joinToString(" ") { word ->
                word.replaceFirstChar { it.uppercase() }
            }
        } else {
            // Already has mixed case, keep as is
            trimmed
        }
    }

    fun getReportUrl(): String {
        val txn = _transaction.value ?: return ""

        // Use the original SMS body if available
        val smsBody = txn.smsBody ?: "Transaction: ${txn.merchantName} - ${txn.amount}"

        // Use the original SMS sender if available
        val sender = txn.smsSender ?: ""

        Log.d("TransactionDetailVM", "Generating report URL for transaction")

        // URL encode the parameters
        val encodedMessage = java.net.URLEncoder.encode(smsBody, "UTF-8")
        val encodedSender = java.net.URLEncoder.encode(sender, "UTF-8")

        // Encrypt device data for verification
        val encryptedDeviceData =
            DeviceEncryption.encryptDeviceData(context)
        val encodedDeviceData = if (encryptedDeviceData != null) {
            java.net.URLEncoder.encode(encryptedDeviceData, "UTF-8")
        } else {
            ""
        }

        // Create the report URL using hash fragment for privacy
        val url =
            "${Constants.Links.WEB_PARSER_URL}/#message=$encodedMessage&sender=$encodedSender&device=$encodedDeviceData&autoparse=true"
        Log.d("TransactionDetailVM", "Report URL: ${url.take(200)}...")

        return url
    }

    fun showDeleteDialog() {
        _showDeleteDialog.value = true
    }

    fun hideDeleteDialog() {
        _showDeleteDialog.value = false
    }

    fun deleteTransaction() {
        viewModelScope.launch {
            _transaction.value?.let { txn ->
                _isDeleting.value = true
                _showDeleteDialog.value = false

                try {
                    transactionRepository.deleteTransaction(txn)
                    _deleteSuccess.value = true
                } catch (e: Exception) {
                    _errorMessage.value = "Failed to delete transaction"
                } finally {
                    _isDeleting.value = false
                }
            }
        }
    }

    private suspend fun syncSubscriptionForTransaction(transaction: TransactionEntity) {
        if (transaction.isRecurring) {
            val existing = subscriptionRepository.matchTransactionToSubscription(
                transaction.merchantName,
                transaction.amount
            )

            val nextPaymentDate = calculateNextPaymentDate(
                (transaction.dateTime ?: LocalDateTime.now()).toLocalDate(),
                transaction.billingCycle
            )

            val subscription = existing?.copy(
                amount = transaction.amount,
                nextPaymentDate = nextPaymentDate,
                category = transaction.category,
                subcategory = transaction.subcategory,
                bankName = transaction.bankName,
                currency = transaction.currency,
                billingCycle = transaction.billingCycle,
                state = SubscriptionState.ACTIVE,
                updatedAt = LocalDateTime.now()
            )
                ?: SubscriptionEntity(
                    merchantName = transaction.merchantName,
                    amount = transaction.amount,
                    nextPaymentDate = nextPaymentDate,
                    state = SubscriptionState.ACTIVE,
                    bankName = transaction.bankName,
                    category = transaction.category,
                    subcategory = transaction.subcategory,
                    currency = transaction.currency,
                    billingCycle = transaction.billingCycle,
                    createdAt = LocalDateTime.now(),
                    updatedAt = LocalDateTime.now()
                )
            subscriptionRepository.insertSubscription(subscription)
        } else {
            // Find existing matching subscription and hide it
            val existing = subscriptionRepository.matchTransactionToSubscription(
                transaction.merchantName,
                transaction.amount
            )
            if (existing != null) {
                subscriptionRepository.hideSubscription(existing.id)
            }
        }
    }

    private fun calculateNextPaymentDate(
        fromDate: java.time.LocalDate,
        billingCycle: String?
    ): java.time.LocalDate {
        return when (billingCycle) {
            "Weekly" -> fromDate.plusWeeks(1)
            "Monthly" -> fromDate.plusMonths(1)
            "Quarterly" -> fromDate.plusMonths(3)
            "Semi-Annual" -> fromDate.plusMonths(6)
            "Annual" -> fromDate.plusYears(1)
            else -> fromDate.plusMonths(1)
        }
    }

    private suspend fun updateAccountBalances(
        oldTransaction: TransactionEntity,
        newTransaction: TransactionEntity
    ) {
        //Revert effect of old transaction
        revertBalanceEffect(oldTransaction)

        //Apply effect of new transaction
        applyBalanceEffect(newTransaction)
    }

    private suspend fun revertBalanceEffect(transaction: TransactionEntity) {
        val bankName = transaction.bankName
        val accountLast4 = transaction.accountNumber

        if (bankName != null && accountLast4 != null) {
            when (transaction.transactionType) {
                TransactionType.INCOME -> {
                    // Originally added, so subtract to revert
                    updateBalance(
                        bankName,
                        accountLast4,
                        transaction.amount.negate(),
                        transaction.currency
                    )
                }

                TransactionType.EXPENSE, TransactionType.INVESTMENT -> {
                    // Originally subtracted, so add to revert
                    updateBalance(bankName, accountLast4, transaction.amount, transaction.currency)
                }

                TransactionType.CREDIT -> {
                    // No balance update to revert for now
                }

                TransactionType.TRANSFER -> {
                    // Revert source: Originally subtracted, so add
                    updateBalance(bankName, accountLast4, transaction.amount, transaction.currency)

                   //find an account with that last4.
                    transaction.toAccount?.let { targetLast4 ->
                        findAccountByLast4(targetLast4)?.let { targetAccount ->
                            updateBalance(
                                targetAccount.bankName,
                                targetLast4,
                                transaction.amount.negate(),
                                transaction.currency
                            )
                        }
                    }
                }
            }
        }
    }

    private suspend fun applyBalanceEffect(transaction: TransactionEntity) {
        val bankName = transaction.bankName
        val accountLast4 = transaction.accountNumber

        if (bankName != null && accountLast4 != null) {
            when (transaction.transactionType) {
                TransactionType.INCOME -> {
                    updateBalance(bankName, accountLast4, transaction.amount, transaction.currency)
                }

                TransactionType.EXPENSE, TransactionType.INVESTMENT -> {
                    updateBalance(
                        bankName,
                        accountLast4,
                        transaction.amount.negate(),
                        transaction.currency
                    )
                }

                TransactionType.CREDIT -> {
                    // No balance update for now
                }

                TransactionType.TRANSFER -> {
                    // Source: Subtract
                    updateBalance(
                        bankName,
                        accountLast4,
                        transaction.amount.negate(),
                        transaction.currency
                    )

                    // Target: Add
                    transaction.toAccount?.let { targetLast4 ->
                        findAccountByLast4(targetLast4)?.let { targetAccount ->
                            updateBalance(
                                targetAccount.bankName,
                                targetLast4,
                                transaction.amount,
                                transaction.currency
                            )
                        }
                    }
                }
            }
        }
    }

    private suspend fun updateBalance(
        bankName: String,
        accountLast4: String,
        amountDelta: BigDecimal,
        currency: String,
        transactionId: Long? = null
    ) {
        val currentBalance = accountBalanceRepository.getLatestBalance(bankName, accountLast4)
        val newBalance = (currentBalance?.balance ?: BigDecimal.ZERO) + amountDelta

        accountBalanceRepository.insertBalance(
            AccountBalanceEntity(
                bankName = bankName,
                accountLast4 = accountLast4,
                balance = newBalance,
                timestamp = LocalDateTime.now(),
                transactionId = transactionId,
                sourceType = "MANUAL_EDIT",
                iconResId = currentBalance?.iconResId ?: 0,
                isCreditCard = currentBalance?.isCreditCard ?: false,
                isWallet = currentBalance?.isWallet ?: false,
                creditLimit = currentBalance?.creditLimit,
                currency = currency
            )
        )
    }

    private suspend fun findAccountByLast4(last4: String): AccountBalanceEntity? {
        return accountBalanceRepository.getAllLatestBalances().first()
            .find { it.accountLast4 == last4 }
    }
}
