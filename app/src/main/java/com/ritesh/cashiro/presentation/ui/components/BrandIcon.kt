package com.ritesh.cashiro.presentation.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ritesh.cashiro.data.database.entity.CategoryEntity
import com.ritesh.cashiro.data.database.entity.SubcategoryEntity
import com.ritesh.cashiro.presentation.common.icons.CategoryMapping
import com.ritesh.cashiro.presentation.common.icons.IconProvider
import com.ritesh.cashiro.presentation.common.icons.IconResource

/**
 * Displays a brand icon with intelligent fallback
 * Fallback priority: Brand Icon > Subcategory Icon > Category Icon
 */
@Composable
fun BrandIcon(
    merchantName: String,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    showBackground: Boolean = true,
    categoryEntity: CategoryEntity? = null,
    subcategoryEntity: SubcategoryEntity? = null,
    accountIconResId: Int = 0,
    accountColorHex: String? = null
) {
    val iconResource = remember(merchantName, categoryEntity, subcategoryEntity, accountIconResId) {
        IconProvider.getIconForTransaction(
            merchantName = merchantName,
            categoryEntity = categoryEntity,
            subcategoryEntity = subcategoryEntity,
            accountIconResId = accountIconResId
        )
    }
    val brandColor = remember(merchantName, categoryEntity, subcategoryEntity, accountColorHex) {
        IconProvider.getColorForTransaction(
            merchantName = merchantName,
            categoryEntity = categoryEntity,
            subcategoryEntity = subcategoryEntity,
            accountColorHex = accountColorHex
        )
    }
    
    Box(
        modifier = modifier
            .size(size)
            .then(
                if (showBackground) {
                    Modifier
                        .clip(CircleShape)
                        .background(
                            brandColor?.let { (colorHex, alpha) -> 
                                Color(colorHex.toColorInt()).copy(alpha = alpha)
                            } ?: generateColorFromString(merchantName)
                        )
                        .padding(8.dp)
                } else {
                    Modifier
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        when (iconResource) {
            is IconResource.DrawableResource -> {
                // Brand logo
                Image(
                    painter = painterResource(id = iconResource.resId),
                    contentDescription = merchantName,
                    modifier = Modifier.fillMaxSize()
                )
            }
            is IconResource.VectorIcon -> {
                // Category icon fallback
                Icon(
                    imageVector = iconResource.icon,
                    contentDescription = merchantName,
                    tint = if (showBackground) Color.White else iconResource.tint,
                    modifier = Modifier.fillMaxSize()
                )
            }
            is IconResource.TintedResIcon -> {
                Icon(
                    painter = painterResource(id = iconResource.resId),
                    contentDescription = merchantName,
                    tint = Color.Unspecified,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

@Composable
fun CategoryIcon(
    category: String,
    modifier: Modifier = Modifier,
    size: Dp = 24.dp,
    tint: Color? = Color.Unspecified,
) {
    val categoryInfo = CategoryMapping.categories[category]
        ?: CategoryMapping.categories["Miscellaneous"]!!
    
    Icon(
        painter = painterResource(id = categoryInfo.iconResId),
        contentDescription = category,
        tint = tint ?: Color.Unspecified,
        modifier = modifier.size(size)
    )
}

/**
 * Utility function to generate consistent colors from strings
 */
private fun generateColorFromString(str: String): Color {
    val colors = listOf(
        Color(0xFF6750A4), // Material Purple
        Color(0xFF0061A4), // Material Blue
        Color(0xFF006D40), // Material Green
        Color(0xFFB3261E), // Material Red
        Color(0xFF9A4521), // Material Orange
        Color(0xFF6D4C41), // Material Brown
        Color(0xFF455A64), // Material Blue Grey
        Color(0xFF5E35B1), // Deep Purple
        Color(0xFF43A047), // Green
        Color(0xFFE53935), // Red
    )
    
    val hash = str.hashCode()
    return colors[Math.abs(hash) % colors.size]
}

/**
 * Extension to convert hex string to Color Int
 */
private fun String.toColorInt(): Int {
    // Remove # if present and parse hex
    val hex = this.removePrefix("#")
    return android.graphics.Color.parseColor("#$hex")
}