package com.ritesh.cashiro.presentation.ui.features.settings.dataprivacy

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MergeType
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.ritesh.cashiro.presentation.effects.overScrollVertical
import com.ritesh.cashiro.presentation.ui.components.LoadingCircle
import com.ritesh.cashiro.presentation.ui.theme.LocalBlurEffects
import com.ritesh.cashiro.presentation.ui.theme.Spacing
import dev.chrisbanes.haze.ExperimentalHazeApi
import dev.chrisbanes.haze.HazeDefaults
import dev.chrisbanes.haze.HazeEffectScope
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeEffect

// Shows while the PDF is being analyzed (processing indicator).
@OptIn(ExperimentalHazeApi::class)
@Composable
fun PdfProcessingDialog(
    isVisible: Boolean,
    error: String?,
    onDismissError: () -> Unit,
    blurEffects: Boolean = LocalBlurEffects.current,
    hazeState: HazeState = remember { HazeState() }
) {
    if (!isVisible && error == null) return

    val containerColor = MaterialTheme.colorScheme.surfaceContainerLow
    Dialog(onDismissRequest = { if (error != null) onDismissError() }) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .then(
                    if (blurEffects) Modifier.hazeEffect(
                        state = hazeState,
                        block = fun HazeEffectScope.() {
                            style = HazeDefaults.style(
                                backgroundColor = Color.Transparent,
                                tint = HazeDefaults.tint(containerColor),
                                blurRadius = 20.dp,
                                noiseFactor = -1f
                            )
                            blurredEdgeTreatment = BlurredEdgeTreatment.Unbounded
                        }
                    ) else Modifier
                ),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (blurEffects) containerColor.copy(0.5f) else containerColor
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (error != null) {
                    Icon(
                        Icons.Default.Error,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(40.dp)
                    )
                    Text(
                        text = "PDF Import Error",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold
                    )
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.large,
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer
                        ),
                    ) {
                        Text(
                            text = error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(12.dp)
                        )
                    }

                    Button(
                        onClick = onDismissError,
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Close") }
                } else {
                    LoadingCircle(modifier = Modifier.size(48.dp))
                    Text(
                        text = "Analyzing PDF...",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Extracting transactions and bank accounts",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

// Shown after analysis is complete.

@OptIn(ExperimentalMaterial3Api::class, ExperimentalHazeApi::class,
    ExperimentalMaterial3ExpressiveApi::class
)
@Composable
fun PdfImportConfirmationDialog(
    analysisResult: PdfAnalysisResult,
    onConfirm: (decisions: Map<String, AccountImportDecision>) -> Unit,
    onCancel: () -> Unit,
    blurEffects: Boolean = LocalBlurEffects.current,
    hazeState: HazeState = remember { HazeState() }
) {
    // Track the decision per account last4
    val decisions = remember(analysisResult) {
        mutableStateMapOf<String, AccountImportDecision>().apply {
            analysisResult.accountMatches.forEach { match ->
                // Default: merge if there's an existing match, create new otherwise
                put(match.last4, if (match.hasExistingMatch) AccountImportDecision.MERGE_WITH_EXISTING else AccountImportDecision.CREATE_NEW)
            }
        }
    }

    val containerColor = MaterialTheme.colorScheme.surfaceContainerLow
    val infiniteTransition = rememberInfiniteTransition(label = "rotation")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    Dialog(onDismissRequest = onCancel) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .then(
                    if (blurEffects) Modifier.hazeEffect(
                        state = hazeState,
                        block = fun HazeEffectScope.() {
                            style = HazeDefaults.style(
                                backgroundColor = Color.Transparent,
                                tint = HazeDefaults.tint(containerColor),
                                blurRadius = 20.dp,
                                noiseFactor = -1f
                            )
                            blurredEdgeTreatment = BlurredEdgeTreatment.Unbounded
                        }
                    ) else Modifier
                ),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (blurEffects) containerColor.copy(0.5f) else containerColor
            )
        ) {
            Box(
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .overScrollVertical()
                        .padding(Spacing.md),
                    verticalArrangement = Arrangement.spacedBy(Spacing.md)
                ) {
                    // Header
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .rotate(rotation)
                            .background(
                                color = MaterialTheme.colorScheme.primary,
                                shape = MaterialShapes.Cookie9Sided.toShape()
                            )
                            .align(Alignment.CenterHorizontally),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.rotate(-rotation).size(24.dp)
                        )
                    }
                    Text(
                        text = "PDF Analysis",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )

                    // Summary stats
                    PdfStatsSummary(analysisResult)

                    // Per-account decision cards
                    if (analysisResult.accountMatches.isNotEmpty()) {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(MaterialTheme.shapes.large)
                        ) {
                            items(analysisResult.accountMatches) { match ->
                                PdfAccountDecisionCard(
                                    match = match,
                                    currentDecision = decisions[match.last4]
                                        ?: AccountImportDecision.CREATE_NEW,
                                    onDecisionChanged = { decisions[match.last4] = it }
                                )
                            }
                        }
                    }
                    Spacer(
                        modifier = Modifier.height(28.dp)
                    )
                }
                // Action buttons
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    MaterialTheme.colorScheme.surfaceContainerLow,
                                    MaterialTheme.colorScheme.surfaceContainerLow
                                )
                            )
                        ),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Row(
                        modifier = Modifier
                            .padding(horizontal = Spacing.md, vertical = Spacing.sm)
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter),
                        horizontalArrangement = Arrangement.spacedBy(Spacing.xs)
                    ) {
                        Button(
                            onClick = onCancel,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(
                                topStart = 24.dp, topEnd = 6.dp,
                                bottomStart = 24.dp, bottomEnd = 6.dp
                            ),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.surface,
                                contentColor = MaterialTheme.colorScheme.onSurface
                            )
                        ) { Text("Cancel") }

                        Button(
                            onClick = { onConfirm(decisions.toMap()) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(
                                topStart = 6.dp, topEnd = 24.dp,
                                bottomStart = 6.dp, bottomEnd = 24.dp
                            )
                        ) {
                            Text("Import")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PdfStatsSummary(result: PdfAnalysisResult) {
    val matchedCount = result.accountMatches.count { it.hasExistingMatch }
    val newCount = result.accountMatches.size - matchedCount
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Spacing.xs)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(Spacing.md),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {

                Column(
                    modifier = Modifier.weight(1f),
                ) {
                    Text(
                        text = "Transactions",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = result.transactionCount.toString(),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary,
                    )
                }
                // Header
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .background(
                            color = MaterialTheme.colorScheme.surfaceContainerLow,
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ReceiptLong,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Spacing.xs)
        ) {
            if (matchedCount > 0) {
                PdfStatChip(
                    label = "Matched Banks",
                    value = matchedCount.toString(),
                    valueColor = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
            }
            if (newCount > 0) {
                PdfStatChip(
                    label = "New Banks",
                    value = newCount.toString(),
                    valueColor = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.weight(1f)
                )
            }
        }

    }
}

@Composable
private fun PdfStatChip(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    valueColor: Color = MaterialTheme.colorScheme.onSurface,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier.padding(horizontal = Spacing.sm, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = valueColor
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun PdfAccountDecisionCard(
    match: PdfAccountMatch,
    currentDecision: AccountImportDecision,
    onDecisionChanged: (AccountImportDecision) -> Unit
) {
    val cardColor = when (currentDecision) {
        AccountImportDecision.MERGE_WITH_EXISTING -> MaterialTheme.colorScheme.primaryContainer.copy(0.25f)
        AccountImportDecision.CREATE_NEW -> MaterialTheme.colorScheme.tertiaryContainer.copy(0.25f)
    }
    val subCardColor = when (currentDecision) {
        AccountImportDecision.MERGE_WITH_EXISTING -> MaterialTheme.colorScheme.primaryContainer
        AccountImportDecision.CREATE_NEW -> MaterialTheme.colorScheme.tertiaryContainer
    }
    val subCardTextColor = when (currentDecision) {
        AccountImportDecision.MERGE_WITH_EXISTING -> MaterialTheme.colorScheme.onPrimaryContainer
        AccountImportDecision.CREATE_NEW -> MaterialTheme.colorScheme.onTertiaryContainer
    }
    val cardTextColor = when (currentDecision) {
        AccountImportDecision.MERGE_WITH_EXISTING -> MaterialTheme.colorScheme.primary
        AccountImportDecision.CREATE_NEW -> MaterialTheme.colorScheme.tertiary
    }


    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = cardColor),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm)
        ) {
            // Account identity
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
            ) {
                Icon(
                    Icons.Default.AccountBalance,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.onSurface
                )
                Column {
                    Text(
                        text = "Account ....${match.last4}",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    if (match.existingAccount != null) {
                        Text(
                            text = "Matches: ${match.existingAccount.bankName}",
                            style = MaterialTheme.typography.labelSmall,
                            color = cardTextColor,
                        )
                    } else {
                        Text(
                            text = "No existing account found",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(0.7f)
                        )
                    }
                }
            }

            // Decision options
            if (match.hasExistingMatch) {
                // Show both options
                DecisionOption(
                    selected = currentDecision == AccountImportDecision.MERGE_WITH_EXISTING,
                    label = "Merge with ${match.existingAccount?.bankName}",
                    description = "Link transactions to existing account",
                    icon = Icons.AutoMirrored.Filled.MergeType,
                    cardColor = subCardColor,
                    cardTextColor =  subCardTextColor,
                    onClick = { onDecisionChanged(AccountImportDecision.MERGE_WITH_EXISTING) }
                )
                DecisionOption(
                    selected = currentDecision == AccountImportDecision.CREATE_NEW,
                    label = "Create new bank \"${match.bankNameInPdf}\"",
                    description = "Add as a separate account",
                    icon = Icons.Default.Add,
                    cardColor = subCardColor,
                    cardTextColor =  subCardTextColor,
                    onClick = { onDecisionChanged(AccountImportDecision.CREATE_NEW) }
                )
            } else {
                // Only CREATE_NEW available
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.tertiaryContainer.copy(0.4f)
                ) {
                    Row(
                        modifier = Modifier.padding(Spacing.sm).fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.tertiary
                        )
                        Text(
                            text = "Will create new bank: \"${match.bankNameInPdf}\"",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DecisionOption(
    selected: Boolean,
    label: String,
    description: String,
    icon: ImageVector,
    cardColor: Color,
    cardTextColor: Color,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = if (selected) cardColor
                else MaterialTheme.colorScheme.surface,
    ) {
        Row(
            modifier = Modifier.padding(Spacing.sm).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = if (selected) cardTextColor else MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (selected) cardTextColor.copy(0.7f) else MaterialTheme.colorScheme.onSurface.copy(0.7f),
                )
            }
        }
    }
}
