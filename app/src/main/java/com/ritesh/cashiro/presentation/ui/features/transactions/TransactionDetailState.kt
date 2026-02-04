package com.ritesh.cashiro.presentation.ui.features.transactions

import com.ritesh.cashiro.data.database.entity.SubscriptionEntity
import com.ritesh.cashiro.data.database.entity.TransactionEntity
import java.math.BigDecimal

data class TransactionDetailUiState(
    val transaction: TransactionEntity? = null,
    val primaryCurrency: String = "INR",
    val convertedAmount: BigDecimal? = null,
    val isEditMode: Boolean = false,
    val editableTransaction: TransactionEntity? = null,
    val isSaving: Boolean = false,
    val saveSuccess: Boolean = false,
    val errorMessage: String? = null,
    val applyToAllFromMerchant: Boolean = false,
    val updateExistingTransactions: Boolean = false,
    val existingTransactionCount: Int = 0,
    val showDeleteDialog: Boolean = false,
    val isDeleting: Boolean = false,
    val deleteSuccess: Boolean = false,
    val subscription: SubscriptionEntity? = null
)

data class AccountInfo(
    val id: Long,
    val bankName: String,
    val accountLast4: String,
    val displayName: String,
    val isCreditCard: Boolean,
    val iconResId: Int,
    val currency: String
)
