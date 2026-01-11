package com.ritesh.cashiro.presentation.accounts

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ritesh.cashiro.data.model.Currency
import com.ritesh.cashiro.ui.effects.BlurredAnimatedVisibility
import com.ritesh.cashiro.ui.effects.overScrollVertical
import com.ritesh.cashiro.ui.effects.rememberOverscrollFlingBehavior
import com.ritesh.cashiro.ui.theme.Dimensions
import com.ritesh.cashiro.ui.components.SearchBarBox
import com.ritesh.cashiro.ui.theme.Spacing
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class,
    ExperimentalMaterial3ExpressiveApi::class
)
@Composable
fun CurrencyBottomSheet(
    selectedCurrency: String,
    onCurrencySelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var searchQuery by remember { mutableStateOf(TextFieldValue("")) }
    var showAllCurrencies by rememberSaveable { mutableStateOf(false) }
    var showExchangeRateInfo by rememberSaveable { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()
    ModalBottomSheet(
        sheetState = sheetState,
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.background,
        dragHandle = { BottomSheetDefaults.DragHandle() },
    ) {
        Box(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
                    .verticalScroll(
                        state = scrollState,
                        flingBehavior = rememberOverscrollFlingBehavior { scrollState }
                    )
            ) {
                Text(
                    text = "Currencies",
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .padding(bottom = 16.dp)
                        .fillMaxWidth()
                )

                // Search Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SearchBarBox(
                        searchQuery = searchQuery,
                        onSearchQueryChange = { searchQuery = it },
                        label = { Text("Search currencies...") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        trailingIcon = {
                            if (searchQuery.text.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = TextFieldValue("") }) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Clear",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            } else {
                                IconButton(onClick = { showExchangeRateInfo = true }) {
                                    Icon(
                                        imageVector = Icons.Outlined.Info,
                                        contentDescription = "Info",
                                        tint = MaterialTheme.colorScheme.inverseSurface.copy(0.5f),
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                        },
                        modifier = Modifier.padding(horizontal = Spacing.md).padding(bottom = 16.dp)
                    )
                }

                // Filter currencies based on search query
                val filteredCurrencies = Currency.SUPPORTED_CURRENCIES.filter {
                    it.code.contains(searchQuery.text, ignoreCase = true) ||
                            it.name.contains(searchQuery.text, ignoreCase = true) ||
                            it.symbol.contains(searchQuery.text, ignoreCase = true)
                }

                if (filteredCurrencies.isEmpty()) {
                    Text(
                        text = "Currency not available",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 16.dp)
                    )
                } else {
                    // Display currencies in a FlowRow
                    val displayedCurrencies = if (showAllCurrencies) {
                        filteredCurrencies
                    } else {
                        filteredCurrencies.filter { currency ->
                            Currency.POPULAR_CURRENCY_CODES.contains(currency.code)
                        }.take(15)
                    }

                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        maxItemsInEachRow = 3,
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        displayedCurrencies.forEach { currency ->
                            CurrencyCard(
                                currency = currency,
                                isSelected = currency.code == selectedCurrency,
                                onCurrencyCardClick = {
                                    scope.launch { sheetState.hide() }
                                        .invokeOnCompletion {
                                            if (!sheetState.isVisible) {
                                                onCurrencySelected(currency.code)
                                            }
                                        }
                                }
                            )
                        }
                    }

                    // View All Currencies button
                    if (!showAllCurrencies && filteredCurrencies.size > 15) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            TextButton(
                                onClick = { showAllCurrencies = true },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color.Transparent,
                                    contentColor = MaterialTheme.colorScheme.inverseSurface.copy(0.5f)
                                ),
                                shapes = ButtonDefaults.shapes()
                            ) {
                                Text(
                                    text = "View All Currencies",
                                    fontSize = 12.sp,
                                    modifier = Modifier
                                        .background(
                                            color = MaterialTheme.colorScheme.surfaceContainerLow,
                                            shape = RoundedCornerShape(15.dp)
                                        )
                                        .padding(horizontal = 10.dp, vertical = 5.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(80.dp))
            }

            // Exchange Rate Info Dialog
            BlurredAnimatedVisibility(
                visible = showExchangeRateInfo,
                enter = fadeIn() + scaleIn(
                    animationSpec = tween(durationMillis = 300),
                    initialScale = 0f,
                ),
                exit = fadeOut() + scaleOut(
                    animationSpec = tween(durationMillis = 100),
                    targetScale = 0f,
                ),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { showExchangeRateInfo = false }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .shadow(
                                elevation = 25.dp,
                                RoundedCornerShape(15.dp),
                                clip = true,
                                spotColor = Color.Black,
                                ambientColor = Color.Black
                            )
                            .fillMaxWidth(0.85f)
                            .clip(RoundedCornerShape(15.dp))
                            .background(
                                MaterialTheme.colorScheme.surfaceBright,
                                RoundedCornerShape(15.dp)
                            )
                            .clickable(onClick = {})
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(15.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Info,
                                contentDescription = "Info",
                                tint = MaterialTheme.colorScheme.primary.copy(0.5f),
                                modifier = Modifier.size(50.dp)
                            )
                            Text(
                                text = "Currency Selection",
                                textAlign = TextAlign.Center,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.inverseSurface,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Text(
                                text = "Select the currency for this account. The currency will be used to display balances and format transaction amounts. Exchange rates are automatically fetched for multi-currency support.",
                                textAlign = TextAlign.Center,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.inverseSurface.copy(0.8f),
                                modifier = Modifier.fillMaxWidth()
                            )
                            Button(
                                onClick = { showExchangeRateInfo = false },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = Color.White
                                ),
                            ) {
                                Text(
                                    text = "Got it",
                                    textAlign = TextAlign.Center,
                                    color = Color.White,
                                    modifier = Modifier
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CurrencyCard(
    currency: Currency,
    isSelected: Boolean = false,
    onCurrencyCardClick: () -> Unit = {}
) {
    Card(
        modifier = Modifier
            .sizeIn(minWidth = 90.dp, minHeight = 70.dp, maxHeight = 90.dp, maxWidth = 110.dp)
            .clip(RoundedCornerShape(Dimensions.Radius.md))
            .clickable(onClick = onCurrencyCardClick)
            .padding(5.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) 
                MaterialTheme.colorScheme.primaryContainer 
            else 
                MaterialTheme.colorScheme.surfaceContainerLow,
            contentColor = if (isSelected)
                MaterialTheme.colorScheme.onPrimaryContainer
            else
                MaterialTheme.colorScheme.inverseSurface
        ),
        border = if (isSelected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(5.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = currency.code.uppercase(),
                lineHeight = 12.sp,
                fontSize = 12.sp,
                color = if (isSelected)
                    MaterialTheme.colorScheme.onPrimaryContainer.copy(0.7f)
                else
                    MaterialTheme.colorScheme.inverseSurface.copy(0.5f)
            )
            Text(
                text = currency.symbol,
                lineHeight = 20.sp,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = if (isSelected)
                    MaterialTheme.colorScheme.onPrimaryContainer
                else
                    MaterialTheme.colorScheme.inverseSurface,
                modifier = Modifier.padding(vertical = 5.dp)
            )
            Text(
                text = currency.name,
                lineHeight = 10.sp,
                fontSize = 10.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = if (isSelected)
                    MaterialTheme.colorScheme.onPrimaryContainer.copy(0.7f)
                else
                    MaterialTheme.colorScheme.inverseSurface.copy(0.5f),
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .basicMarquee(
                        iterations = Int.MAX_VALUE,
                        animationMode = MarqueeAnimationMode.Immediately,
                        initialDelayMillis = 1000,
                        velocity = 30.dp
                    )
            )
        }
    }
}
