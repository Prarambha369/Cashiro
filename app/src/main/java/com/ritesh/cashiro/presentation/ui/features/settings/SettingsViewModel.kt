package com.ritesh.cashiro.presentation.ui.features.settings

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import androidx.core.app.NotificationCompat
import com.ritesh.cashiro.MainActivity
import com.ritesh.cashiro.R
import com.ritesh.cashiro.receiver.SmsBroadcastReceiver
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import com.ritesh.cashiro.data.repository.ModelRepository
import com.ritesh.cashiro.data.repository.ModelState
import com.ritesh.cashiro.data.repository.UnrecognizedSmsRepository
import com.ritesh.cashiro.data.preferences.UserPreferencesRepository
import com.ritesh.cashiro.data.backup.BackupExporter
import com.ritesh.cashiro.data.backup.BackupImporter
import com.ritesh.cashiro.data.backup.ExportResult
import com.ritesh.cashiro.data.backup.ImportResult
import com.ritesh.cashiro.data.backup.ImportStrategy
import android.content.Intent
import androidx.core.content.FileProvider
import com.ritesh.cashiro.core.Constants
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import java.net.URLEncoder
import java.io.File
import javax.inject.Inject
import androidx.core.net.toUri
import com.ritesh.cashiro.data.repository.TransactionRepository
import com.ritesh.cashiro.data.repository.AccountBalanceRepository
import com.ritesh.cashiro.data.repository.CardRepository
import com.ritesh.cashiro.data.repository.BudgetRepository
import com.ritesh.cashiro.data.database.entity.AccountBalanceEntity
import com.ritesh.cashiro.data.database.entity.CardEntity
import com.ritesh.cashiro.data.database.entity.CardType
import com.ritesh.cashiro.data.database.entity.BudgetEntity
import com.ritesh.cashiro.data.database.entity.BudgetPeriod
import com.ritesh.cashiro.data.database.entity.BudgetTrackType
import com.ritesh.cashiro.data.database.entity.BudgetType
import com.ritesh.cashiro.data.database.entity.TransactionEntity
import com.ritesh.cashiro.data.database.entity.TransactionType
import kotlinx.coroutines.flow.Flow
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.UUID

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val modelRepository: ModelRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val unrecognizedSmsRepository: UnrecognizedSmsRepository,
    private val transactionRepository: TransactionRepository,
    private val accountBalanceRepository: AccountBalanceRepository,
    private val cardRepository: CardRepository,
    private val budgetRepository: BudgetRepository,
    private val backupExporter: BackupExporter,
    private val backupImporter: BackupImporter
) : ViewModel() {

    val userPreferences = userPreferencesRepository.userPreferences

    val totalTransactions = transactionRepository.getTransactionCount()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0
        )

    private val downloadManager =
        context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    private var currentDownloadId: Long? = null

    // Developer mode state
    // Developer mode state
    val isDeveloperModeEnabled = userPreferencesRepository.isDeveloperModeEnabled
    val isTestNotificationAlertsEnabled = userPreferencesRepository.isTestNotificationAlertsEnabled

    // SMS scan period state
    val smsScanMonths = userPreferencesRepository.smsScanMonths
    val smsScanAllTime = userPreferencesRepository.smsScanAllTime

    // Unrecognized SMS state
    val unreportedSmsCount = unrecognizedSmsRepository.getUnreportedCount()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0
        )

    val isSampleDataSeeded: Flow<Boolean> = userPreferencesRepository.isSampleDataSeeded

    init {
        checkDownloadStatus()
        // Also sync with model repository
        modelRepository.checkModelState()
    }

    private fun checkDownloadStatus() {
        viewModelScope.launch {
            // First check for active download
            val savedDownloadId = userPreferencesRepository.getActiveDownloadId()
            Log.d("SettingsViewModel", "Checking download status, saved ID: $savedDownloadId")

            if (savedDownloadId != null) {
                // Query DownloadManager for this ID
                val query = DownloadManager.Query().setFilterById(savedDownloadId)
                val cursor = downloadManager.query(query)

                if (cursor != null && cursor.moveToFirst()) {
                    val statusIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)

                    if (statusIndex != -1) {
                        val status = cursor.getInt(statusIndex)
                        Log.d("SettingsViewModel", "Found active download with status: $status")

                        when (status) {
                            DownloadManager.STATUS_RUNNING,
                            DownloadManager.STATUS_PENDING -> {
                                _uiState.update { it.copy(downloadStatus = DownloadState.DOWNLOADING) }
                                currentDownloadId = savedDownloadId
                                // Sync ModelRepository state
                                modelRepository.updateModelState(ModelState.DOWNLOADING)
                                // Get current progress
                                val bytesIndex =
                                    cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)
                                val totalIndex =
                                    cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)
                                if (bytesIndex != -1 && totalIndex != -1) {
                                    val bytes = cursor.getLong(bytesIndex)
                                    val total = cursor.getLong(totalIndex)
                                    _uiState.update {
                                        it.copy(
                                            downloadedMB = bytes / (1024 * 1024),
                                            totalMB = total / (1024 * 1024)
                                        )
                                    }
                                    if (total > 0) {
                                        _uiState.update { it.copy(downloadProgress = (bytes * 100 / total).toInt()) }
                                    }
                                }
                                monitorDownload(savedDownloadId)
                            }

                            DownloadManager.STATUS_SUCCESSFUL -> {
                                _uiState.update {
                                    it.copy(
                                        downloadStatus = DownloadState.COMPLETED,
                                        downloadProgress = 100
                                    )
                                }
                                userPreferencesRepository.clearActiveDownloadId()
                                modelRepository.updateModelState(ModelState.READY)
                            }

                            DownloadManager.STATUS_FAILED -> {
                                _uiState.update { it.copy(downloadStatus = DownloadState.FAILED) }
                                userPreferencesRepository.clearActiveDownloadId()
                                // Sync ModelRepository state
                                modelRepository.updateModelState(ModelState.NOT_DOWNLOADED)
                            }

                            DownloadManager.STATUS_PAUSED -> {
                                _uiState.update { it.copy(downloadStatus = DownloadState.PAUSED) }
                                currentDownloadId = savedDownloadId
                                // Sync ModelRepository state - still downloading but paused
                                modelRepository.updateModelState(ModelState.DOWNLOADING)
                            }
                        }
                    }
                    cursor.close()
                } else {
                    // Download ID not found in DownloadManager, clear it and check file
                    Log.d(
                        "SettingsViewModel",
                        "Download ID not found in DownloadManager, checking file"
                    )
                    userPreferencesRepository.clearActiveDownloadId()
                    checkModelFile()
                }
            } else {
                // No active download, check if model file exists
                checkModelFile()
            }
        }
    }


    private fun checkModelFile() {
        val modelFile = File(
            context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS),
            Constants.ModelDownload.MODEL_FILE_NAME
        )
        Log.d("SettingsViewModel", "Checking model file at: ${modelFile.absolutePath}")
        Log.d(
            "SettingsViewModel",
            "Model file exists: ${modelFile.exists()}, size: ${modelFile.length()}, expected: ${Constants.ModelDownload.MODEL_SIZE_BYTES}"
        )

        // Check against expected size to ensure it's complete
        // Allow 5% variance in file size as download sizes can vary slightly
        val minSize = (Constants.ModelDownload.MODEL_SIZE_BYTES * 0.95).toLong()
        val maxSize = (Constants.ModelDownload.MODEL_SIZE_BYTES * 1.05).toLong()

        if (modelFile.exists() && modelFile.length() in minSize..maxSize) {
            _uiState.update {
                it.copy(
                    downloadStatus = DownloadState.COMPLETED,
                    totalMB = modelFile.length() / (1024 * 1024),
                    downloadedMB = modelFile.length() / (1024 * 1024),
                    downloadProgress = 100
                )
            }
            // Update model repository state
            Log.d(
                "SettingsViewModel",
                "Model complete (${modelFile.length()} bytes), updating repository state to READY"
            )
            modelRepository.updateModelState(ModelState.READY)
        } else if (modelFile.exists() && modelFile.length() > maxSize) {
            // File is too large, but might still be valid - mark as complete
            _uiState.update {
                it.copy(
                    downloadStatus = DownloadState.COMPLETED,
                    totalMB = modelFile.length() / (1024 * 1024),
                    downloadedMB = modelFile.length() / (1024 * 1024),
                    downloadProgress = 100
                )
            }
            Log.d(
                "SettingsViewModel",
                "Model file larger than expected (${modelFile.length()} bytes), but marking as complete"
            )
            modelRepository.updateModelState(ModelState.READY)
        } else if (modelFile.exists()) {
            // Partial file exists, delete it
            Log.d(
                "SettingsViewModel",
                "Partial model file found (${modelFile.length()} bytes), deleting"
            )
            modelFile.delete()
            _uiState.update { it.copy(downloadStatus = DownloadState.NOT_DOWNLOADED) }
        } else {
            Log.d("SettingsViewModel", "Model not found")
            _uiState.update { it.copy(downloadStatus = DownloadState.NOT_DOWNLOADED) }
        }
    }

    fun startModelDownload() {
        viewModelScope.launch {
            // Check if download is already active
            val existingDownloadId = userPreferencesRepository.getActiveDownloadId()
            if (existingDownloadId != null) {
                // Check if this download is still active
                val query = DownloadManager.Query().setFilterById(existingDownloadId)
                val cursor = downloadManager.query(query)

                if (cursor != null && cursor.moveToFirst()) {
                    val statusIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
                    if (statusIndex != -1) {
                        val status = cursor.getInt(statusIndex)
                        if (status == DownloadManager.STATUS_RUNNING ||
                            status == DownloadManager.STATUS_PENDING ||
                            status == DownloadManager.STATUS_PAUSED
                        ) {
                            // Download is already active, just monitor it
                            Log.d(
                                "SettingsViewModel",
                                "Download already active with ID: $existingDownloadId"
                            )
                            cursor.close()
                            _uiState.update { it.copy(downloadStatus = DownloadState.DOWNLOADING) }
                            currentDownloadId = existingDownloadId
                            modelRepository.updateModelState(ModelState.DOWNLOADING)
                            monitorDownload(existingDownloadId)
                            return@launch
                        }
                    }
                    cursor.close()
                }
            }

            // Check storage space
            val availableSpace = context.filesDir.usableSpace
            if (availableSpace < Constants.ModelDownload.REQUIRED_SPACE_BYTES) {
                _uiState.update { it.copy(downloadStatus = DownloadState.ERROR_INSUFFICIENT_SPACE) }
                return@launch
            }

            // Create download request
            val request = DownloadManager.Request(Constants.ModelDownload.MODEL_URL.toUri())
                .setTitle("Qwen 2.5 Chat Model")
                .setDescription("Downloading AI chat assistant for Cashiro")
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setDestinationInExternalFilesDir(
                    context,
                    Environment.DIRECTORY_DOWNLOADS,
                    Constants.ModelDownload.MODEL_FILE_NAME
                )
                .setAllowedOverMetered(true) // Allow mobile data downloads
                .setAllowedOverRoaming(false)

            currentDownloadId = downloadManager.enqueue(request)
            _uiState.update { it.copy(downloadStatus = DownloadState.DOWNLOADING) }

            // Sync ModelRepository state
            modelRepository.updateModelState(ModelState.DOWNLOADING)

            // Save download ID
            userPreferencesRepository.saveActiveDownloadId(currentDownloadId!!)
            Log.d("SettingsViewModel", "Started download with ID: $currentDownloadId")

            // Start monitoring progress
            monitorDownload(currentDownloadId!!)
        }
    }

    private fun monitorDownload(downloadId: Long) {
        viewModelScope.launch {
            while (isActive && _uiState.value.downloadStatus == DownloadState.DOWNLOADING) {
                val query = DownloadManager.Query().setFilterById(downloadId)
                val cursor = downloadManager.query(query)

                if (cursor != null && cursor.moveToFirst()) {
                    val bytesColumnIndex =
                        cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)
                    val totalBytesColumnIndex =
                        cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)
                    val statusColumnIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)

                    if (bytesColumnIndex != -1 && totalBytesColumnIndex != -1) {
                        val bytesDownloaded = cursor.getLong(bytesColumnIndex)
                        val bytesTotal = cursor.getLong(totalBytesColumnIndex)

                        // Calculate progress
                        val progress = if (bytesTotal > 0) {
                            (bytesDownloaded * 100 / bytesTotal).toInt()
                        } else 0

                        _uiState.update {
                            it.copy(
                                downloadProgress = progress,
                                downloadedMB = bytesDownloaded / (1024 * 1024),
                                totalMB = bytesTotal / (1024 * 1024)
                            )
                        }
                    }

                    // Check status
                    if (statusColumnIndex != -1) {
                        when (cursor.getInt(statusColumnIndex)) {
                            DownloadManager.STATUS_SUCCESSFUL -> {
                                _uiState.update {
                                    it.copy(
                                        downloadStatus = DownloadState.COMPLETED,
                                        downloadProgress = 100
                                    )
                                }
                                // Clear saved download ID
                                userPreferencesRepository.clearActiveDownloadId()
                                // Update model repository state
                                modelRepository.updateModelState(ModelState.READY)
                                Log.d("SettingsViewModel", "Download completed successfully")
                            }

                            DownloadManager.STATUS_FAILED -> {
                                _uiState.update { it.copy(downloadStatus = DownloadState.FAILED) }
                                // Clear saved download ID
                                userPreferencesRepository.clearActiveDownloadId()
                                // Sync ModelRepository state
                                modelRepository.updateModelState(ModelState.NOT_DOWNLOADED)
                                Log.d("SettingsViewModel", "Download failed")
                            }

                            DownloadManager.STATUS_PAUSED -> {
                                _uiState.update { it.copy(downloadStatus = DownloadState.PAUSED) }
                            }
                        }
                    }
                }
                cursor?.close()
                delay(1000) // Update every second
            }
        }
    }

    fun cancelDownload() {
        viewModelScope.launch {
            currentDownloadId?.let { it ->
                downloadManager.remove(it)
                _uiState.update { it.copy(downloadStatus = DownloadState.NOT_DOWNLOADED) }
                _uiState.update { it.copy(downloadProgress = 0) }
                _uiState.update { it.copy(downloadedMB = 0) }
                _uiState.update { it.copy(totalMB = 0) }

                // Clear saved download ID
                userPreferencesRepository.clearActiveDownloadId()

                // Delete partial file
                val modelFile = File(
                    context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS),
                    Constants.ModelDownload.MODEL_FILE_NAME
                )
                if (modelFile.exists()) {
                    modelFile.delete()
                }
                Log.d("SettingsViewModel", "Download cancelled and cleaned up")
            }
        }
    }

    fun deleteModel() {
        viewModelScope.launch {
            val modelFile = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), Constants.ModelDownload.MODEL_FILE_NAME)
            if (modelFile.exists()) {
                modelFile.delete()
                _uiState.update { it.copy(downloadStatus = DownloadState.NOT_DOWNLOADED) }
                _uiState.update { it.copy(downloadProgress = 0) }
                _uiState.update { it.copy(downloadedMB = 0) }
                _uiState.update { it.copy(totalMB = 0) }
                // Clear any saved download ID
                userPreferencesRepository.clearActiveDownloadId()
                // Update model repository state
                modelRepository.updateModelState(ModelState.NOT_DOWNLOADED)
                Log.d("SettingsViewModel", "Model deleted")
            }
        }
    }

    fun toggleDeveloperMode(enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.setDeveloperModeEnabled(enabled)
        }
    }

    fun toggleTestNotificationAlerts(enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.setTestNotificationAlertsEnabled(enabled)
            if (enabled) {
                sendTestNotification()
            }
        }
    }

    fun toggleSampleData(enabled: Boolean) {
        if (enabled) {
            seedSampleData()
        } else {
            removeSampleData()
        }
    }

    private fun sendTestNotification() {
        try {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // Ensure channel exists
            val channel = NotificationChannel(
                SmsBroadcastReceiver.CHANNEL_ID,
                SmsBroadcastReceiver.CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifications for new transactions"
            }
            notificationManager.createNotificationChannel(channel)

            // Create intent to open app
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }

            val pendingIntent = PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val notification = NotificationCompat.Builder(context, SmsBroadcastReceiver.CHANNEL_ID)
                .setSmallIcon(R.drawable.cashiro)
                .setContentTitle("Test Notification")
                .setContentText("This is a test notification from Cashiro.")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .build()

            notificationManager.notify(999, notification) // ID 999 for test
            Log.d("SettingsViewModel", "Sent test notification")
        } catch (e: Exception) {
            Log.e("SettingsViewModel", "Error sending test notification", e)
        }
    }

    fun updateSmsScanMonths(months: Int) {
        viewModelScope.launch {
            val currentMonths = userPreferencesRepository.getSmsScanMonths()

            // If increasing scan period, reset scan timestamp to force full scan
            if (months > currentMonths) {
                userPreferencesRepository.setLastScanTimestamp(0L)
                Log.d("SettingsViewModel", "Scan period increased from $currentMonths to $months months - will perform full scan")
            }

            userPreferencesRepository.updateSmsScanMonths(months)
        }
    }

    fun updateSmsScanAllTime(allTime: Boolean) {
        viewModelScope.launch {
            // If enabling all time scanning, reset scan timestamp to force full scan
            if (allTime) {
                userPreferencesRepository.setLastScanTimestamp(0L)
                Log.d("SettingsViewModel", "All time scanning enabled - will perform full scan")
            }

            userPreferencesRepository.updateSmsScanAllTime(allTime)
        }
    }

    fun openUnrecognizedSmsReport(context: Context) {
        viewModelScope.launch {
            try {
                val firstUnreported = unrecognizedSmsRepository.getFirstUnreported()

                if (firstUnreported != null) {
                    val issueTitle = "[Unrecognized SMS] From: ${firstUnreported.sender}"
                    val issueBody = """
                        ### Unrecognized SMS Details
                        - **Sender:** ${firstUnreported.sender}
                        - **Date:** ${firstUnreported.receivedAt}
                        
                        ### Original SMS
                        ```
                        ${firstUnreported.smsBody}
                        ```
                        
                        ### Expected Behavior
                        _Describe how this SMS should have been parsed_
                    """.trimIndent()

                    val encodedTitle = URLEncoder.encode(issueTitle, "UTF-8")
                    val encodedBody = URLEncoder.encode(issueBody, "UTF-8")

                    val url = "https://github.com/ritesh-kanwar/Cashiro/issues/new?title=$encodedTitle&body=$encodedBody"

                    // Open in browser
                    val intent = Intent(Intent.ACTION_VIEW, url.toUri())
                    context.startActivity(intent)

                    // Mark as reported
                    unrecognizedSmsRepository.markAsReported(listOf(firstUnreported.id))

                    Log.d("SettingsViewModel", "Opened report for unrecognized SMS from: ${firstUnreported.sender}")
                } else {
                    Log.d("SettingsViewModel", "No unreported SMS messages found")
                }
            } catch (e: Exception) {
                Log.e("SettingsViewModel", "Error opening unrecognized SMS report", e)
            }
        }
    }

    fun exportBackup() {
        viewModelScope.launch {
            try {
                when (val result = backupExporter.exportBackup()) {
                    is ExportResult.Success -> {
                        // Store the file for later saving
                        _uiState.update { it.copy(
                            exportedBackupFile = result.file,
                            importExportMessage = "Backup created successfully! Choose where to save it."
                        ) }
                    }
                    is ExportResult.Error -> {
                        _uiState.update { it.copy(importExportMessage = "Export failed: ${result.message}") }
                        Log.e("SettingsViewModel", "Export failed: ${result.message}")
                    }
                    else -> {}
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(importExportMessage = "Export error: ${e.message}") }
                Log.e("SettingsViewModel", "Export error", e)
            }
        }
    }

    fun saveBackupToFile(uri: Uri) {
        viewModelScope.launch {
            try {
                _uiState.value.exportedBackupFile?.let { file ->
                    context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                        file.inputStream().use { inputStream ->
                            inputStream.copyTo(outputStream)
                        }
                    }
                    _uiState.update { it.copy(
                        importExportMessage = "Backup saved successfully!",
                        exportedBackupFile = null
                    ) }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(importExportMessage = "Failed to save backup: ${e.message}") }
                Log.e("SettingsViewModel", "Error saving backup", e)
            }
        }
    }


    fun shareBackup() {
        _uiState.value.exportedBackupFile?.let { file ->
            shareBackupFile(file)
        }
    }

    private fun shareBackupFile(file: File) {
        try {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/octet-stream"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Cashiro Backup")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            context.startActivity(Intent.createChooser(intent, "Share Backup").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
        } catch (e: Exception) {
            Log.e("SettingsViewModel", "Error sharing backup file", e)
        }
    }

    fun importBackup(uri: Uri) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(importExportMessage = "Importing backup...") }
                when (val result = backupImporter.importBackup(uri, ImportStrategy.MERGE)) {
                    is ImportResult.Success -> {
                        _uiState.update { it.copy(importExportMessage = "Import successful! Imported ${result.importedTransactions} transactions, ${result.importedCategories} categories. Skipped ${result.skippedDuplicates} duplicates.") }
                    }
                    is ImportResult.Error -> {
                        _uiState.update { it.copy(importExportMessage = "Import failed: ${result.message}") }
                        Log.e("SettingsViewModel", "Import failed: ${result.message}")
                    }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(importExportMessage = "Import error: ${e.message}") }
                Log.e("SettingsViewModel", "Import error", e)
            }
        }
    }

    fun clearImportExportMessage() {
        _uiState.update { it.copy(importExportMessage = null) }
    }

    fun seedSampleData() {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isSeeding = true) }

                val now = LocalDateTime.now()

                // HDFC Bank (Savings) with History
                val hdfcLast4 = "1234"
                for (i in 4 downTo 0) {
                    accountBalanceRepository.insertBalance(
                        AccountBalanceEntity(
                            bankName = "HDFC Bank",
                            accountLast4 = hdfcLast4,
                            balance = BigDecimal(50000 - (i * 1000)),
                            timestamp = now.minusDays(i.toLong()),
                            sourceType = "MANUAL",
                            iconResId = R.drawable.type_finance_bank,
                            color = "#33B5E5",
                            isSample = true
                        )
                    )
                }

                // ICICI Bank (Credit Card)
                accountBalanceRepository.insertBalance(
                    AccountBalanceEntity(
                        bankName = "ICICI Bank",
                        accountLast4 = "5678",
                        balance = BigDecimal(25000),
                        creditLimit = BigDecimal(100000),
                        timestamp = now,
                        isCreditCard = true,
                        sourceType = "MANUAL",
                        iconResId = R.drawable.type_stationary_card_file_box,
                        color = "#E91E63",
                        isSample = true
                    )
                )

                // SBI Bank (Current)
                accountBalanceRepository.insertBalance(
                    AccountBalanceEntity(
                        bankName = "SBI Bank",
                        accountLast4 = "9012",
                        balance = BigDecimal(75000),
                        timestamp = now,
                        iconResId = R.drawable.type_finance_bank,
                        color = "#1976D2",
                        isSample = true
                    )
                )

                // Cash (Wallet)
                accountBalanceRepository.insertBalance(
                    AccountBalanceEntity(
                        bankName = "Cash",
                        accountLast4 = "wallet", // Special identifier for wallet
                        balance = BigDecimal(2500),
                        timestamp = now,
                        sourceType = "MANUAL",
                        isWallet = true,
                        isSample = true,
                        iconResId = R.drawable.type_finance_dollar_banknote
                    )
                )

                // Linked Card for HDFC
                cardRepository.insertCard(
                    CardEntity(
                        cardLast4 = "4321",
                        cardType = CardType.DEBIT,
                        bankName = "HDFC Bank",
                        accountLast4 = hdfcLast4,
                        nickname = "Salary Card",
                        lastBalance = BigDecimal(48000),
                        lastBalanceSource = "Your HDFC Bank account ending in 1234 has been credited with INR 50,000.00. Avl bal: INR 48,000.00",
                        lastBalanceDate = now,
                        isSample = true
                    )
                )

                // Unlinked Card
                cardRepository.insertCard(
                    CardEntity(
                        cardLast4 = "8765",
                        cardType = CardType.DEBIT,
                        bankName = "Axis Bank",
                        nickname = "Travel Card",
                        lastBalance = BigDecimal(15420),
                        lastBalanceSource = "Bank Alert: Your Axis Bank Card XX8765 was used for a transaction of INR 500 at STARBUCKS. Avl Bal: INR 15,420.75",
                        lastBalanceDate = now,
                        isSample = true
                    )
                )

                // --- SEED BUDGETS & TRANSACTIONS FOR HISTORY ---
                val foodCategory = "Food & Drinks"
                val entertainmentCategory = "Entertainment"

                // 1. Monthly Food Budget (Active in current month, but history goes back)
                val foodBudgetStart = now.withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0).withNano(0)
                val foodBudgetId = budgetRepository.insertBudget(
                    BudgetEntity(
                        name = "Monthly Food",
                        amount = BigDecimal(15000),
                        year = foodBudgetStart.year,
                        month = foodBudgetStart.monthValue,
                        startDate = foodBudgetStart,
                        endDate = foodBudgetStart.plusMonths(1).minusSeconds(1),
                        periodType = BudgetPeriod.MONTHLY,
                        trackType = BudgetTrackType.ALL_TRANSACTIONS,
                        budgetType = BudgetType.EXPENSE,
                        createdAt = now.minusMonths(3).withDayOfMonth(1), // History from 3 months ago
                        color = "#FF9800", // Orange
                        isSample = true
                    )
                )

                // Seed transactions for Monthly Food (Last 4 months inclusive)
                for (monthOffset in 0..3) {
                    val monthDate = now.minusMonths(monthOffset.toLong())
                    val baseAmount = when(monthOffset) {
                        0 -> 8000 // Current month (ongoing)
                        1 -> 16500 // Last month (over budget)
                        2 -> 14000 // 2 months ago (within budget)
                        3 -> 12000 // 3 months ago (within budget)
                        else -> 10000
                    }
                    
                    // Split into few transactions per month
                    for (i in 1..4) {
                        transactionRepository.insertTransaction(
                            TransactionEntity(
                                amount = BigDecimal(baseAmount / 4),
                                merchantName = "Groceries $i",
                                category = foodCategory,
                                transactionType = TransactionType.EXPENSE,
                                dateTime = monthDate.withDayOfMonth(i * 5).withHour(12),
                                transactionHash = UUID.randomUUID().toString(),
                                currency = "INR",
                                bankName = "HDFC Bank",
                                accountNumber = hdfcLast4,
                                isSample = true
                            )
                        )
                    }
                }

                // 2. Weekly Entertainment Budget (Active this week, history goes back)
                val weeklyStart = now.with(java.time.DayOfWeek.MONDAY).withHour(0).withMinute(0).withSecond(0).withNano(0)
                budgetRepository.insertBudget(
                    BudgetEntity(
                        name = "Weekly Fun",
                        amount = BigDecimal(2000),
                        year = weeklyStart.year,
                        month = weeklyStart.monthValue,
                        startDate = weeklyStart,
                        endDate = weeklyStart.plusWeeks(1).minusSeconds(1),
                        periodType = BudgetPeriod.WEEKLY,
                        trackType = BudgetTrackType.ALL_TRANSACTIONS,
                        budgetType = BudgetType.EXPENSE,
                        createdAt = now.minusWeeks(4), // History from 4 weeks ago
                        isSample = true,
                        color = "#9C27B0" // Purple
                    )
                )

                // Seed transactions for Weekly Fun (Last 5 weeks inclusive)
                for (weekOffset in 0..4) {
                    val weekDate = now.minusWeeks(weekOffset.toLong())
                    val amount = when(weekOffset) {
                        0 -> 1200 // Current week
                        1 -> 2500 // Last week (over)
                        2 -> 1800 // 2 weeks ago
                        3 -> 2100 // 3 weeks ago (over)
                        4 -> 1500 // 4 weeks ago
                        else -> 1000
                    }

                    transactionRepository.insertTransaction(
                        TransactionEntity(
                            amount = BigDecimal(amount),
                            merchantName = "Movie/Games",
                            category = entertainmentCategory,
                            transactionType = TransactionType.EXPENSE,
                            dateTime = weekDate.withHour(20),
                            transactionHash = UUID.randomUUID().toString(),
                            isSample = true,
                            currency = "INR"
                        )
                    )
                }

                 // 3. Savings Budget: New Car Fund
                 val savingsStart = now.withDayOfMonth(1).withHour(0).withMinute(0)
                 budgetRepository.insertBudget(
                    BudgetEntity(
                        name = "New Car Fund",
                        amount = BigDecimal(500000),
                        year = savingsStart.year,
                        month = savingsStart.monthValue,
                        startDate = savingsStart,
                        endDate = savingsStart.plusYears(2), // 2 year goal
                        periodType = BudgetPeriod.CUSTOM,
                        trackType = BudgetTrackType.ALL_TRANSACTIONS,
                        budgetType = BudgetType.SAVINGS,
                        createdAt = now,
                        color = "#4CAF50", // Green
                        isSample = true
                    )
                 )

                 // Add some savings (Income transactions)
                 transactionRepository.insertTransaction(
                     TransactionEntity(
                         amount = BigDecimal(50000),
                         merchantName = "Monthly Savings",
                         category = "Investment",
                         transactionType = TransactionType.INCOME,
                         dateTime = now,
                         transactionHash = UUID.randomUUID().toString(),
                         currency = "INR",
                         isSample = true
                     )
                 )

                userPreferencesRepository.setSampleDataSeeded(true)

                _uiState.update {
                    it.copy(
                        isSeeding = false,
                        seedMessage = "Sample data (including history) seeded successfully"
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isSeeding = false,
                        seedMessage = "Failed to seed sample data: ${e.message}"
                    )
                }
            }
        }
    }

    fun removeSampleData() {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isSeeding = true) }

                transactionRepository.deleteSampleTransactions()
                accountBalanceRepository.deleteSampleBalances()
                cardRepository.deleteSampleCards()
                budgetRepository.deleteSampleBudgets()
                
                userPreferencesRepository.setSampleDataSeeded(false)

                _uiState.update {
                    it.copy(
                        isSeeding = false,
                        seedMessage = "Sample data removed successfully"
                    )
                }
                delay(3000)
                _uiState.update { it.copy(seedMessage = null) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isSeeding = false,
                        seedMessage = "Failed to remove sample data: ${e.message}"
                    )
                }
            }
        }
    }

    fun clearSeedMessage() {
        _uiState.update { it.copy(seedMessage = null) }
    }
}
