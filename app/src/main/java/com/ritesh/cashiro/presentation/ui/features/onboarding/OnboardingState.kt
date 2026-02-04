package com.ritesh.cashiro.presentation.ui.features.onboarding

import com.ritesh.cashiro.presentation.ui.features.profile.EditProfileState

data class OnBoardingUiState(
    val currentStep: Int = 1,
    val hasPermission: Boolean = false,
    val hasSkippedPermission: Boolean = false,
    val showRationale: Boolean = false,
    val profileState: EditProfileState = EditProfileState()
)
