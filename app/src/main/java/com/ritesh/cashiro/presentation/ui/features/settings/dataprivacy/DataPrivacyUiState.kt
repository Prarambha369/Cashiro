package com.ritesh.cashiro.presentation.ui.features.settings.dataprivacy

import com.ritesh.cashiro.data.backup.BackupConfiguration
import com.ritesh.cashiro.data.database.entity.AccountBalanceEntity
import java.io.File

/**
 * Holds the result of the PDF analysis phase.
 */
data class PdfAnalysisResult(
    // Parsed transactions waiting to be committed.
    val pendingTransactions: List<com.ritesh.parser.core.ParsedTransaction>,
    // Number of distinct transactions extracted.
    val transactionCount: Int,
    // Accounts found in this PDF (last4 → existing account or null if new).
    val accountMatches: List<PdfAccountMatch>
)

data class PdfAccountMatch(
    val last4: String,
    val bankNameInPdf: String,
    // Existing account in the DB that matches, or null if no match.
    val existingAccount: AccountBalanceEntity?
) {
    val hasExistingMatch: Boolean get() = existingAccount != null
}

/**
 * User's decision for each account found in the PDF.
 */
enum class AccountImportDecision { MERGE_WITH_EXISTING, CREATE_NEW }

data class DataPrivacyUiState(
    val importExportMessage: String? = null,
    val exportedBackupFile: File? = null,
    val backupConfiguration: BackupConfiguration = BackupConfiguration(),

    // PDF import flow
    val isPdfProcessing: Boolean = false,
    val pdfAnalysisResult: PdfAnalysisResult? = null,
    val pdfProcessingError: String? = null,
    val hasNewAccountsCreated: Boolean = false
)
