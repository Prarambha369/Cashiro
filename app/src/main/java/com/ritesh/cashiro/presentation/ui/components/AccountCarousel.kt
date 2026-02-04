package com.ritesh.cashiro.presentation.ui.components

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ritesh.cashiro.data.database.entity.AccountBalanceEntity
import com.ritesh.cashiro.utils.formatBalance

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun SharedTransitionScope.AccountCarousel(
    modifier: Modifier = Modifier,
    bankAccounts: List<AccountBalanceEntity>,
    creditCards: List<AccountBalanceEntity>,
    onAccountClick: (bankName: String, accountLast4: String) -> Unit = { _, _ -> },
    animatedContentScope: AnimatedVisibilityScope? = null
) {
    val totalAccounts = bankAccounts.size + creditCards.size
    val allAccounts = bankAccounts + creditCards
    
    val isTransitioning = animatedContentScope?.transition?.let { 
        it.currentState != it.targetState 
    } ?: false

    if (totalAccounts == 1) {
        // Single account - show single wide card
        Box(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            val account = allAccounts.first()
            AccountCarouselCard(
                bankName = account.bankName,
                accountLast4 = account.accountLast4,
                balance = account.formatBalance(),
                subtitle = when {
                    account.isWallet -> "Wallet"
                    creditCards.contains(account) -> "Credit Card"
                    else -> "Savings account"
                },
                onClick = { onAccountClick(account.bankName, account.accountLast4) },
                animatedContentScope = animatedContentScope,
                isWallet = account.isWallet,
                iconResId = account.iconResId,
                color = account.color,
                modifier = Modifier.fillMaxWidth()
            )
        }
    } else {
        // Multiple accounts - show as carousel
        LazyRow(
            modifier = modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            userScrollEnabled = !isTransitioning
        ) {
            items(bankAccounts) { account ->
                AccountCarouselCard(
                    bankName = account.bankName,
                    accountLast4 = account.accountLast4,
                    balance = account.formatBalance(),
                    subtitle = if (account.isWallet) "Wallet" else "Savings account",
                    onClick = { onAccountClick(account.bankName, account.accountLast4) },
                    animatedContentScope = animatedContentScope,
                    isWallet = account.isWallet,
                    iconResId = account.iconResId,
                    color = account.color
                )
            }
            items(creditCards) { card ->
                AccountCarouselCard(
                    bankName = card.bankName,
                    accountLast4 = card.accountLast4,
                    balance = card.formatBalance(),
                    subtitle = "Credit Card",
                    onClick = { onAccountClick(card.bankName, card.accountLast4) },
                    animatedContentScope = animatedContentScope,
                    iconResId = card.iconResId,
                    color = card.color
                )
            }
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun SharedTransitionScope.AccountCarouselCard(
    bankName: String,
    accountLast4: String,
    balance: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    animatedContentScope: AnimatedVisibilityScope? = null,
    isWallet: Boolean = false,
    iconResId: Int = 0,
    color: String? = null
) {
    Surface(
        modifier = modifier
            .then(
                if (modifier == Modifier) Modifier.width(220.dp) else Modifier.fillMaxWidth()
            )
            .height(180.dp)
            .clip(RoundedCornerShape(28.dp))
            .clickable(onClick = onClick)
            .then(
                if (animatedContentScope != null) {
                    Modifier.sharedBounds(
                        rememberSharedContentState(key = "account_${bankName}_${accountLast4}"),
                        animatedVisibilityScope = animatedContentScope,
                        boundsTransform = { _, _ ->
                            spring(
                                stiffness = Spring.StiffnessLow,
                                dampingRatio = Spring.DampingRatioNoBouncy
                            )
                        },
                        resizeMode = SharedTransitionScope.ResizeMode.scaleToBounds(
                            contentScale = ContentScale.Fit,
                            alignment = Alignment.Center
                        )
                    )
                        .skipToLookaheadSize()
                } else Modifier
            ),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(28.dp),
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            BrandIcon(
                merchantName = bankName,
                size = 48.dp,
                showBackground = true,
                accountIconResId = iconResId,
                accountColorHex = color
            )

            
            Column {
                Text(
                    text = if (isWallet) bankName.uppercase() else "${bankName.uppercase()} ••$accountLast4",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    letterSpacing = 1.sp
                )
                
                Spacer(modifier = Modifier.height(6.dp))
                
                Text(
                    text = balance,
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontSize = 26.sp,
                        fontWeight = FontWeight.ExtraBold
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Spacer(modifier = Modifier.height(6.dp))
                
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}
