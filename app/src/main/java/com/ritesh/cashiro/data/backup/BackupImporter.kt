package com.ritesh.cashiro.data.backup

import android.content.Context
import android.net.Uri
import android.util.Log
import com.google.gson.GsonBuilder
import androidx.room.withTransaction
import com.ritesh.cashiro.data.database.CashiroDatabase
import com.ritesh.cashiro.data.database.entity.*
import com.ritesh.cashiro.data.preferences.UserPreferencesRepository
import com.ritesh.cashiro.data.preferences.NavigationBarStyle
import com.ritesh.cashiro.data.preferences.AppFont
import com.ritesh.cashiro.data.preferences.ThemeStyle
import com.ritesh.cashiro.data.preferences.AccentColor
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.first
import java.io.BufferedReader
import java.io.InputStreamReader
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.zip.ZipInputStream
import java.io.FileOutputStream
import java.io.File
import java.math.BigDecimal
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BackupImporter @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: CashiroDatabase,
    private val userPreferencesRepository: UserPreferencesRepository
) {
    
    private val gson = GsonBuilder()
        .setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
        .registerTypeAdapter(LocalDateTime::class.java, LocalDateTimeTypeAdapter())
        .registerTypeAdapter(LocalDate::class.java, LocalDateTypeAdapter())
        .registerTypeAdapter(BigDecimal::class.java, BigDecimalTypeAdapter())
        .create()
    
    /**
     * Import backup from a file URI
     */
    suspend fun importBackup(
        uri: Uri,
        strategy: ImportStrategy = ImportStrategy.MERGE
    ): ImportResult = withContext(Dispatchers.IO) {
        try {
            // Read and parse the backup file
            val backup = readBackupFile(uri)
            
            // Validate backup version
            if (!isCompatibleVersion(backup)) {
                return@withContext ImportResult.Error("Incompatible backup version")
            }
            
            // Import based on strategy
            when (strategy) {
                ImportStrategy.REPLACE_ALL -> replaceAllData(backup)
                ImportStrategy.MERGE -> mergeData(backup)
                ImportStrategy.SELECTIVE -> mergeData(backup) // For now, same as merge
            }
        } catch (e: Exception) {
            Log.e("BackupImporter", "Import failed", e)
            ImportResult.Error("Import failed: ${e.message}")
        }
    }
    
    /**
     * Read and parse backup file
     */
    /**
     * Read and parse backup file (ZIP or JSON)
     */
    private suspend fun readBackupFile(uri: Uri): CashiroBackup {
        return withContext(Dispatchers.IO) {
            // Try to read as ZIP first
            try {
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    val zipInput = ZipInputStream(inputStream)
                    var backup: CashiroBackup? = null
                    var entry = zipInput.nextEntry

                    if (entry != null) {
                        val attachmentsDir = File(context.filesDir, "attachments")
                        if (!attachmentsDir.exists()) attachmentsDir.mkdirs()

                        while (entry != null) {
                            val name = entry.name
                            if (name == "backup.json") {
                                // Read JSON
                                val bytes = zipInput.readBytes()
                                val content = String(bytes, Charsets.UTF_8)
                                backup = gson.fromJson(content, CashiroBackup::class.java)
                            } else if (name.startsWith("attachments/") && !entry.isDirectory) {
                                // Extract Attachment
                                val fileName = File(name).name
                                val outFile = File(attachmentsDir, fileName)
                                FileOutputStream(outFile).use { output ->
                                    zipInput.copyTo(output)
                                }
                            } else if (name.startsWith("profile/") && !entry.isDirectory) {
                                // Extract Profile Images
                                val profileDir = File(context.filesDir, "profile")
                                if (!profileDir.exists()) profileDir.mkdirs()

                                val fileName = File(name).name
                                val outFile = File(profileDir, fileName)
                                FileOutputStream(outFile).use { output ->
                                    zipInput.copyTo(output)
                                }
                            }
                            zipInput.closeEntry()
                            entry = zipInput.nextEntry
                        }
                        // If we found a backup.json, return it
                        if (backup != null) return@withContext backup
                    }
                }
            } catch (e: Exception) {
                // Not a ZIP or failed, fall back to legacy JSON
                Log.d("BackupImporter", "Failed to read as ZIP, trying legacy JSON: ${e.message}")
            }

            // Legacy JSON handling
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val reader = BufferedReader(InputStreamReader(inputStream))
                val content = reader.readText()
                gson.fromJson(content, CashiroBackup::class.java)
            } ?: throw Exception("Failed to read backup file")
        }
    }
    
    /**
     * Check if backup version is compatible
     */
    private fun isCompatibleVersion(backup: CashiroBackup): Boolean {
        // For now, accept all v1.x backups
        return backup.format.startsWith("Cashiro Backup v1")

    }
    
    /**
     * Replace all existing data with backup data
     */
    private suspend fun replaceAllData(backup: CashiroBackup): ImportResult {
        var importedTransactions = 0
        var importedCategories = 0
        
        return database.withTransaction {
            try {
                // Clear existing data
                database.transactionDao().deleteAllTransactions()
                database.categoryDao().deleteAllCategories()
                database.cardDao().deleteAllCards()
                database.accountBalanceDao().deleteAllBalances()
                database.subscriptionDao().deleteAllSubscriptions()
                database.merchantMappingDao().deleteAllMappings()
                database.unrecognizedSmsDao().deleteAll()
                database.merchantMappingDao().deleteAllMappings()
                database.unrecognizedSmsDao().deleteAll()
                database.chatDao().deleteAllMessages()
                database.budgetDao().deleteAllBudgets()
                
                // Import all data
                backup.database.categories.forEach { category ->
                    database.categoryDao().insertCategory(category)
                    importedCategories++
                }
                
                backup.database.transactions.forEach { transaction ->
                    database.transactionDao().insertTransaction(transaction)
                    importedTransactions++
                }
                
                backup.database.cards.forEach { card ->
                    database.cardDao().insertCard(card)
                }
                
                backup.database.accountBalances.forEach { balance ->
                    database.accountBalanceDao().insertBalance(balance)
                }
                
                backup.database.subscriptions.forEach { subscription ->
                    database.subscriptionDao().insertSubscription(subscription)
                }
                
                backup.database.merchantMappings.forEach { mapping ->
                    database.merchantMappingDao().insertMapping(mapping)
                }
                
                backup.database.unrecognizedSms.forEach { sms ->
                    database.unrecognizedSmsDao().insert(sms)
                }
                
                backup.database.chatMessages.forEach { message ->
                    database.chatDao().insertMessage(message)
                }
                
                backup.database.budgets.forEach { budget ->
                    database.budgetDao().insertBudget(budget)
                }

                backup.database.budgetCategoryLimits.forEach { limit ->
                    database.budgetDao().insertCategoryLimit(limit)
                }
                
                // Import preferences
                importPreferences(backup.preferences)
                
                ImportResult.Success(
                    importedTransactions = importedTransactions,
                    importedCategories = importedCategories,
                    skippedDuplicates = 0
                )
            } catch (e: Exception) {
                throw e
            }
        }
    }
    
    /**
     * Merge backup data with existing data
     */
    private suspend fun mergeData(backup: CashiroBackup): ImportResult {
        var importedTransactions = 0
        var importedCategories = 0
        var skippedDuplicates = 0
        
        return database.withTransaction {
            try {
                // Get existing data for duplicate checking
                val existingTransactionsMap = database.transactionDao()
                    .getAllTransactions().first()
                    .associateBy { it.transactionHash }
                
                val existingCategories = database.categoryDao()
                    .getAllCategories().first()
                    .map { it.name }
                    .toSet()
                
                // Import categories (merge by name)
                backup.database.categories.forEach { category ->
                    if (!existingCategories.contains(category.name)) {
                        // Generate new ID for imported category
                        val newCategory = category.copy(id = 0)
                        database.categoryDao().insertCategory(newCategory)
                        importedCategories++
                    }
                }
                
                // Import transactions (merge by hash)
                backup.database.transactions.forEach { backupTxn ->
                    val existingTxn = existingTransactionsMap[backupTxn.transactionHash]
                    if (existingTxn == null) {
                        // New transaction, insert it
                        val newTransaction = backupTxn.copy(id = 0)
                        database.transactionDao().insertTransaction(newTransaction)
                        importedTransactions++
                    } else {
                        // Transaction exists locally. Should we update it with backup data?
                        // If backupTxn has a newer updatedAt or different fields, update local
                        
                        // Heuristic: Update if backup version is "better"
                        // 1. User edits have updatedAt > dateTime
                        // 2. Local unedited SMS scans have updatedAt == dateTime (due to our change in Mapper)
                        
                        val shouldUpdate = when {
                            // Case 1: Backup has a newer edit than local
                            backupTxn.updatedAt.isAfter(existingTxn.updatedAt) -> true
                            
                            // Case 2: Local is just a fresh scan (identical updatedAt/dateTime) 
                            // but backup has an actual edit (updatedAt > dateTime)
                            backupTxn.updatedAt.isAfter(backupTxn.dateTime) && 
                                    !existingTxn.updatedAt.isAfter(existingTxn.dateTime) -> true
                            
                            // Otherwise, keep local
                            else -> false
                        }

                        if (shouldUpdate) {
                            val updatedTxn = backupTxn.copy(id = existingTxn.id)
                            database.transactionDao().updateTransaction(updatedTxn)
                            importedTransactions++
                        } else {
                            skippedDuplicates++
                        }
                    }
                }
                
                // Import other entities with duplicate checking
                importCardsWithMerge(backup.database.cards)
                importAccountBalancesWithMerge(backup.database.accountBalances)
                importSubscriptionsWithMerge(backup.database.subscriptions)
                importMerchantMappingsWithMerge(backup.database.merchantMappings)
                importBudgetsWithMerge(backup.database.budgets, backup.database.budgetCategoryLimits)
                
                // Import preferences (merge with existing)
                importPreferences(backup.preferences)
                
                ImportResult.Success(
                    importedTransactions = importedTransactions,
                    importedCategories = importedCategories,
                    skippedDuplicates = skippedDuplicates
                )
            } catch (e: Exception) {
                throw e
            }
        }
    }
    
    /**
     * Import cards with duplicate checking
     */
    private suspend fun importCardsWithMerge(cards: List<CardEntity>) {
        val existingCards = database.cardDao().getAllCards().first()
        val existingCardKeys = existingCards.map { "${it.bankName}_${it.cardLast4}" }.toSet()
        
        cards.forEach { card ->
            val key = "${card.bankName}_${card.cardLast4}"
            if (!existingCardKeys.contains(key)) {
                val newCard = card.copy(id = 0)
                database.cardDao().insertCard(newCard)
            }
        }
    }
    
    /**
     * Import account balances with duplicate checking
     */
    private suspend fun importAccountBalancesWithMerge(balances: List<AccountBalanceEntity>) {
        // For balances, we'll import all as they represent historical data
        balances.forEach { balance ->
            val newBalance = balance.copy(id = 0)
            database.accountBalanceDao().insertBalance(newBalance)
        }
    }
    
    /**
     * Import subscriptions with duplicate checking
     */
    private suspend fun importSubscriptionsWithMerge(subscriptions: List<SubscriptionEntity>) {
        val existingSubscriptions = database.subscriptionDao().getAllSubscriptions().first()
        val existingKeys = existingSubscriptions.map { "${it.merchantName}_${it.amount}" }.toSet()
        
        subscriptions.forEach { subscription ->
            val key = "${subscription.merchantName}_${subscription.amount}"
            if (!existingKeys.contains(key)) {
                val newSubscription = subscription.copy(id = 0)
                database.subscriptionDao().insertSubscription(newSubscription)
            }
        }
    }
    
    /**
     * Import merchant mappings with merge
     */
    private suspend fun importMerchantMappingsWithMerge(mappings: List<MerchantMappingEntity>) {
        mappings.forEach { mapping ->
            // Merchant mappings use merchant name as primary key, so just insert/replace
            database.merchantMappingDao().insertMapping(mapping)
        }
    }

    /**
     * Import budgets with merge and ID remapping
     */
    private suspend fun importBudgetsWithMerge(
        budgets: List<BudgetEntity>,
        limits: List<BudgetCategoryLimitEntity>
    ) {
        val existingBudgets = database.budgetDao().getAllBudgets().first()
        // Key by Name + Year + Month
        val existingKeys = existingBudgets.map { "${it.name}_${it.year}_${it.month}" }.toSet()
        
        // Map to track OldID -> NewID for limits
        val idMap = mutableMapOf<Long, Long>()

        budgets.forEach { budget ->
            val key = "${budget.name}_${budget.year}_${budget.month}"
            if (!existingKeys.contains(key)) {
                val oldId = budget.id
                val newBudget = budget.copy(id = 0)
                val newId = database.budgetDao().insertBudget(newBudget)
                
                if (oldId != 0L) {
                    idMap[oldId] = newId
                }
            }
        }
        
        // Import limits using new budget IDs
        limits.forEach { limit ->
            val newBudgetId = idMap[limit.budgetId]
            if (newBudgetId != null) {
                // Check if limit already exists for this budget? 
                // Since this is a NEW budget (checked above), limits naturally won't exist.
                val newLimit = limit.copy(id = 0, budgetId = newBudgetId)
                database.budgetDao().insertCategoryLimit(newLimit)
            }
        }
    }
    
    /**
     * Import user preferences
     */
    private suspend fun importPreferences(preferences: PreferencesSnapshot) {
        // Theme preferences
        preferences.theme.isDarkThemeEnabled?.let {
            userPreferencesRepository.updateDarkTheme(it)
        }
        userPreferencesRepository.updateDynamicColor(preferences.theme.isDynamicColorEnabled)
        
        // SMS preferences
        userPreferencesRepository.updateHasSkippedSmsPermission(preferences.sms.hasSkippedSmsPermission)
        userPreferencesRepository.updateSmsScanMonths(preferences.sms.smsScanMonths)
        preferences.sms.lastScanTimestamp?.let {
            userPreferencesRepository.updateLastScanTimestamp(it)
        }
        preferences.sms.lastScanPeriod?.let {
            userPreferencesRepository.updateLastScanPeriod(it)
        }
        
        // Developer preferences
        userPreferencesRepository.updateDeveloperMode(preferences.developer.isDeveloperModeEnabled)
        preferences.developer.systemPrompt?.let {
            userPreferencesRepository.updateSystemPrompt(it)
        }
        
        // App preferences
        userPreferencesRepository.updateHasShownScanTutorial(preferences.app.hasShownScanTutorial)
        preferences.app.firstLaunchTime?.let {
            userPreferencesRepository.updateFirstLaunchTime(it)
        }
        preferences.app.lastReviewPromptTime?.let {
            userPreferencesRepository.updateLastReviewPromptTime(it)
        }

        // Extended Theme Preferences
        preferences.theme.isAmoledMode?.let {
            userPreferencesRepository.updateAmoledMode(it)
        }
        
        preferences.theme.navigationBarStyle?.let { styleName ->
            try {
                val style = NavigationBarStyle.valueOf(styleName)
                userPreferencesRepository.updateNavigationBarStyle(style)
            } catch (e: Exception) {
                // Ignore invalid style
            }
        }

        preferences.theme.appFont?.let { fontName ->
            try {
                val font = AppFont.valueOf(fontName)
                userPreferencesRepository.updateAppFont(font)
            } catch (e: Exception) {
                // Ignore invalid font
            }
        }

        preferences.theme.themeStyle?.let { styleName ->
            try {
                val style = ThemeStyle.valueOf(styleName)
                userPreferencesRepository.updateThemeStyle(style)
            } catch (e: Exception) {
                // Ignore invalid style
            }
        }

        preferences.theme.accentColor?.let { colorName ->
            try {
                val color = AccentColor.valueOf(colorName)
                userPreferencesRepository.updateAccentColor(color)
            } catch (e: Exception) {
                // Ignore invalid color
            }
        }

        // Profile Preferences
        preferences.profile?.let { profile ->
            userPreferencesRepository.updateUserName(profile.userName)
            
            // Profile Image
            if (profile.profileImageUri == "profile/profile_image") {
                val file = File(context.filesDir, "profile/profile_image")
                if (file.exists()) {
                    userPreferencesRepository.updateProfileImageUri(Uri.fromFile(file).toString())
                }
            } else {
                userPreferencesRepository.updateProfileImageUri(profile.profileImageUri)
            }

            userPreferencesRepository.updateProfileBackgroundColor(profile.profileBackgroundColor)
            
            // Banner Image
            if (profile.bannerImageUri == "profile/banner_image") {
                val file = File(context.filesDir, "profile/banner_image")
                if (file.exists()) {
                    userPreferencesRepository.updateBannerImageUri(Uri.fromFile(file).toString())
                }
            } else {
                userPreferencesRepository.updateBannerImageUri(profile.bannerImageUri)
            }

            userPreferencesRepository.updateShowBannerImage(profile.showBannerImage)
        }
    }
}