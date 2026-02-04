package com.ritesh.cashiro.presentation.effects

import androidx.compose.animation.BoundsTransform
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.ui.Alignment
import androidx.compose.ui.layout.ContentScale

/**
 * Centralized configuration for smooth shared element transitions.
 * 
 * This object provides consistent spring animations and helper functions
 * to ensure smooth transitions without jitter during scrolling or when
 * higher z-indexed elements are present.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
object SharedTransitionConfig {
    
    /**
     * Standard spring configuration for smooth transitions.
     * Low stiffness prevents jitter during quick scrolling.
     */
    val standardSpring: BoundsTransform = { _, _ ->
        spring(
            stiffness = Spring.StiffnessLow,
            dampingRatio = Spring.DampingRatioNoBouncy
        )
    }
    
    /**
     * Spring configuration for bouncy transitions (e.g., FAB).
     */
    val bouncySpring: BoundsTransform = { _, _ ->
        spring(
            stiffness = Spring.StiffnessLow,
            dampingRatio = Spring.DampingRatioLowBouncy
        )
    }
    
    /**
     * Resize mode for content that should scale proportionally.
     */
    val scaleToFitMode = SharedTransitionScope.ResizeMode.scaleToBounds(
        contentScale = ContentScale.Fit,
        alignment = Alignment.Center
    )
    
    /**
     * Resize mode for content that should fill the bounds.
     */
    val scaleToBoundsMode = SharedTransitionScope.ResizeMode.scaleToBounds(
        contentScale = ContentScale.FillBounds,
        alignment = Alignment.Center
    )
    
    /**
     * Resize mode that maintains no scaling during transition.
     */
    val noScaleMode = SharedTransitionScope.ResizeMode.scaleToBounds(
        contentScale = ContentScale.None,
        alignment = Alignment.Center
    )
    
    /**
     * Z-index for elements that should render above scrolling content.
     * Use high values to ensure transitions are always visible on top.
     */
    const val OVERLAY_Z_INDEX = 1000f
}
