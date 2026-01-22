package com.ritesh.cashiro.navigation

import kotlinx.serialization.Serializable

// Define navigation destinations using Kotlin Serialization
@Serializable object AppLock

@Serializable object OnBoarding

@Serializable object Home

@Serializable
data class Transactions(
    val category: String? = null,
    val merchant: String? = null,
    val period: String? = null,
    val currency: String? = null,
    val type: String? = null,
    val focusSearch: Boolean = false
)


@Serializable object Settings
@Serializable object Subscriptions
@Serializable object Categories

@Serializable object Analytics

@Serializable object Chat

@Serializable data class TransactionDetail(val transactionId: Long)

@Serializable data class AddTransaction(val initialTab: Int = 0)

@Serializable data class AccountDetail(val bankName: String, val accountLast4: String)

@Serializable object UnrecognizedSms

@Serializable object Faq

@Serializable object Rules

@Serializable object CreateRule

@Serializable object Appearance

@Serializable object ManageAccounts

@Serializable object Profile

@Serializable object SmsSettings

@Serializable object NotificationSettings

