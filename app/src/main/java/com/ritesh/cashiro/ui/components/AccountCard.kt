package com.ritesh.cashiro.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ritesh.cashiro.data.database.entity.AccountBalanceEntity
import com.ritesh.cashiro.ui.theme.Spacing
import com.ritesh.cashiro.utils.CurrencyFormatter

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AccountCard(
    account: AccountBalanceEntity,
    modifier: Modifier = Modifier,
    isHidden: Boolean = false,
    showMoreOptions: Boolean = true,
    onClick: (() -> Unit)? = null,
    onUpdateBalance: () -> Unit = {},
    onEditAccount: () -> Unit = {},
    onViewHistory: () -> Unit = {},
    onToggleVisibility: () -> Unit = {},
    onDeleteAccount: () -> Unit = {},
    content: @Composable () -> Unit = {}
) {
    var showMenu by remember { mutableStateOf(false) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        colors = CardDefaults.cardColors(
            containerColor = if (isHidden) MaterialTheme.colorScheme.surfaceVariant
            else MaterialTheme.colorScheme.surfaceContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Top Section
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 8.dp, top = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (account.isCreditCard) "Outstanding" else "Balance",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (showMoreOptions) {
                    Box {
                        IconButton(
                            onClick = { showMenu = true },
                            colors = IconButtonDefaults.iconButtonColors(
                                containerColor = MaterialTheme.colorScheme.surface,
                                contentColor = MaterialTheme.colorScheme.onSurface
                            ),
                            shapes = IconButtonDefaults.shapes()
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.MoreHoriz,
                                contentDescription = "More options",
                            )
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false },
                            shape = MaterialTheme.shapes.large,
                            containerColor = Color.Transparent,
                            shadowElevation = 0.dp,
                            modifier = Modifier.padding(8.dp)
                        ) {
                            DropdownMenuItem(
                                text = { Text("Update Balance") },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.Update,
                                        contentDescription = null
                                    )
                                },
                                onClick = {
                                    showMenu = false
                                    onUpdateBalance()
                                },
                                modifier = Modifier
                                    .shadow(
                                        elevation = 2.dp,
                                        shape = RoundedCornerShape(
                                            topStart = 16.dp,
                                            topEnd = 16.dp,
                                            bottomStart = 4.dp,
                                            bottomEnd = 4.dp
                                        )
                                    )
                                    .background(
                                        color = MaterialTheme.colorScheme.surfaceContainer,
                                        shape = RoundedCornerShape(
                                            topStart = 16.dp,
                                            topEnd = 16.dp,
                                            bottomStart = 4.dp,
                                            bottomEnd = 4.dp
                                        )
                                    )
                            )

                            Spacer(modifier = Modifier.height(1.5.dp))
                            DropdownMenuItem(
                                text = { Text("Edit Details") },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.Edit,
                                        contentDescription = null
                                    )
                                },
                                onClick = {
                                    showMenu = false
                                    onEditAccount()
                                },
                                modifier = Modifier
                                    .shadow(
                                        elevation = 2.dp,
                                        shape = RoundedCornerShape(
                                            topStart = 4.dp,
                                            topEnd = 4.dp,
                                            bottomStart = 4.dp,
                                            bottomEnd = 4.dp
                                        )
                                    )
                                    .background(
                                        color = MaterialTheme.colorScheme.surfaceContainer,
                                        shape = RoundedCornerShape(
                                            topStart = 4.dp,
                                            topEnd = 4.dp,
                                            bottomStart = 4.dp,
                                            bottomEnd = 4.dp
                                        )
                                    )
                            )

                            Spacer(modifier = Modifier.height(1.5.dp))
                            DropdownMenuItem(
                                text = { Text("History") },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.History,
                                        contentDescription = null
                                    )
                                },
                                onClick = {
                                    showMenu = false
                                    onViewHistory()
                                },
                                modifier = Modifier
                                    .shadow(
                                        elevation = 2.dp,
                                        shape = RoundedCornerShape(
                                            topStart = 4.dp,
                                            topEnd = 4.dp,
                                            bottomStart = 4.dp,
                                            bottomEnd = 4.dp
                                        )
                                    )
                                    .background(
                                        color = MaterialTheme.colorScheme.surfaceContainer,
                                        shape = RoundedCornerShape(
                                            topStart = 4.dp,
                                            topEnd = 4.dp,
                                            bottomStart = 4.dp,
                                            bottomEnd = 4.dp
                                        )
                                    )
                            )

                            Spacer(modifier = Modifier.height(1.5.dp))
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        if (isHidden) "Show"
                                        else "Hide"
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        if (isHidden)
                                            Icons.Default
                                                .Visibility
                                        else
                                            Icons.Default
                                                .VisibilityOff,
                                        contentDescription = null
                                    )
                                },
                                onClick = {
                                    showMenu = false
                                    onToggleVisibility()
                                },
                                modifier = Modifier
                                    .shadow(
                                        elevation = 2.dp,
                                        shape = RoundedCornerShape(
                                            topStart = 4.dp,
                                            topEnd = 4.dp,
                                            bottomStart = 4.dp,
                                            bottomEnd = 4.dp
                                        )
                                    )
                                    .background(
                                        color = MaterialTheme.colorScheme.surfaceContainer,
                                        shape = RoundedCornerShape(
                                            topStart = 4.dp,
                                            topEnd = 4.dp,
                                            bottomStart = 4.dp,
                                            bottomEnd = 4.dp
                                        )
                                    )
                            )
                            Spacer(modifier = Modifier.height(1.5.dp))
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        "Delete",
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                },
                                onClick = {
                                    showMenu = false
                                    onDeleteAccount()
                                },
                                modifier = Modifier
                                    .shadow(
                                        elevation = 2.dp,
                                        shape = RoundedCornerShape(
                                            topStart = 4.dp,
                                            topEnd = 4.dp,
                                            bottomStart = 16.dp,
                                            bottomEnd = 16.dp
                                        )
                                    )
                                    .background(
                                        color = MaterialTheme.colorScheme.errorContainer,
                                        shape = RoundedCornerShape(
                                            topStart = 4.dp,
                                            topEnd = 4.dp,
                                            bottomStart = 16.dp,
                                            bottomEnd = 16.dp
                                        )
                                    )
                            )
                        }
                    }
                }
            }

            // Balance
            Text(
                text = CurrencyFormatter.formatCurrency(
                    account.balance,
                    account.currency
                ),
                style = MaterialTheme.typography.displaySmall.copy(
                    fontSize = 40.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            // Bottom Section (Bank Info)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.surfaceContainerLow,
                                MaterialTheme.colorScheme.surfaceContainerLow
                            )
                        )
                    )
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = account.bankName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text =
                            "**** **** **** ${account.accountLast4}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.6f)
                        )
                    }
                    if (account.iconResId != 0) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surface,
                            tonalElevation = 2.dp,
                            modifier = Modifier.size(48.dp)
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    painter = painterResource(id = account.iconResId),
                                    contentDescription = null,
                                    modifier = Modifier.size(24.dp),
                                    tint = Color.Unspecified
                                )
                            }
                        }
                    }
                }
            }
            // Extra content (e.g., Credit Card stats, Linked Cards)
            content()
        }
    }
}
