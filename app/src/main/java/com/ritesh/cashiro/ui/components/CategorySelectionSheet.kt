package com.ritesh.cashiro.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ritesh.cashiro.data.database.entity.CategoryEntity
import com.ritesh.cashiro.data.database.entity.SubcategoryEntity
import com.ritesh.cashiro.ui.theme.Dimensions
import com.ritesh.cashiro.ui.theme.Spacing

@Composable
fun CategorySelectionSheet(
    categories: List<CategoryEntity>,
    subcategoriesMap: Map<Long, List<SubcategoryEntity>>,
    onSelectionComplete: (CategoryEntity, SubcategoryEntity?) -> Unit,
    onDismiss: () -> Unit
) {
    var searchQuery by remember { mutableStateOf(TextFieldValue("")) }
    
    // Filter categories based on search
    val filteredCategories = remember(categories, searchQuery.text) {
        if (searchQuery.text.isBlank()) {
            categories
        } else {
            categories.filter { it.name.contains(searchQuery.text, ignoreCase = true) }
        }
    }

    // Track expanded state for each category
    // Initially, none are expanded.
    // If search is active, maybe expand all? Or just match?
    // Let's stick to manual expansion for now.
    val expandedStates = remember { mutableStateMapOf<Long, Boolean>() }

    Column(
        modifier = Modifier
            .fillMaxWidth()
    ) {
        // Search Bar
        Box(modifier = Modifier.padding(horizontal = Dimensions.Padding.content, vertical = Spacing.sm)) {
             SearchBarBox(
                searchQuery = searchQuery,
                onSearchQueryChange = { searchQuery = it },
                label = { Text("Search Categories") },
                leadingIcon = {},
                trailingIcon = if (searchQuery.text.isNotEmpty()) {
                    {
                        IconButton(onClick = { searchQuery = TextFieldValue("") }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear search")
                        }
                    }
                } else { {} }
            )
        }

        if (filteredCategories.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
                contentAlignment = androidx.compose.ui.Alignment.Center
            ) {
                Text(
                    text = "No categories found",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)),
                contentPadding = PaddingValues(
                    start = Dimensions.Padding.content,
                    end = Dimensions.Padding.content,
                    top = Spacing.sm,
                    bottom = 0.dp
                ),
                verticalArrangement = Arrangement.spacedBy(Spacing.md)
            ) {
                items(
                    items = filteredCategories,
                    key = { it.id }
                ) { category ->
                    val subs = subcategoriesMap[category.id] ?: emptyList()
                    // Auto-expand if search query matches subcategory name? 
                    // For complexity, let's keep it simple. 
                    
                    val isExpanded = expandedStates[category.id] == true

                    // We need to pass subcategories ONLY if expanded (to mimic behavior) 
                    // OR pass them always but CategoryItem handles display? 
                    // CategoryItem displays them if `subcategories` list is not empty.
                    // So we control visibility by passing list or emptyList.
                    
                    val displayedSubcategories = if (isExpanded) subs else emptyList()

                    AnimatedContent(
                        targetState = category,
                        transitionSpec = {
                            fadeIn(animationSpec = tween(220)) +
                                    scaleIn(initialScale = 0.95f) togetherWith fadeOut(animationSpec = tween(90))
                        },
                        label = "CategoryItemAnimation"
                    ) { animatedCategory ->
                         CategoryItem(
                            category = animatedCategory,
                            subcategories = displayedSubcategories,
                            onClick = {
                                if (subs.isNotEmpty()) {
                                    // Toggle expansion
                                    expandedStates[animatedCategory.id] = !isExpanded
                                } else {
                                    // No subcategories, select immediately
                                    onSelectionComplete(animatedCategory, null)
                                }
                            },
                            onAddSubcategory = {}, // Hide add button
                            onEditSubcategory = { sub ->
                                // Selecting a subcategory
                                onSelectionComplete(animatedCategory, sub)
                            },
                            showAddSubcategoryButton = false
                        )
                    }
                }
                item {
                    Spacer(modifier = Modifier.height(Spacing.xxl))
                }
            }
        }
    }
}
