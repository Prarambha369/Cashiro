package com.ritesh.cashiro.presentation.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.ritesh.cashiro.presentation.ui.theme.Spacing

/**
 * Reusable expandable list component
 * Shows limited items initially with "View All" button
 */
@Composable
fun <T> ExpandableList(
    items: List<T>,
    visibleItemCount: Int = 5,
    modifier: Modifier = Modifier,
    itemContent: @Composable (Int, Int, T) -> Unit
) {
    var isExpanded by rememberSaveable { mutableStateOf(false) }
    val displayItems = if (isExpanded) items else items.take(visibleItemCount)
    
    Column(modifier = modifier) {
        // Display items
        displayItems.forEachIndexed { index, item ->
            itemContent(index, displayItems.size, item)
        }
        
        // View All / View Less button
        if (items.size > visibleItemCount) {
            Spacer(Modifier.size(Spacing.md))
            TextButton(
                onClick = { isExpanded = !isExpanded },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = if (isExpanded) {
                        "View Less"
                    } else {
                        "View All (${items.size - visibleItemCount} more)"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}