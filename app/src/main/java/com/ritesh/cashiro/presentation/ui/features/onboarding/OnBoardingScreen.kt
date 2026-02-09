package com.ritesh.cashiro.presentation.ui.features.onboarding

import android.Manifest
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.filled.MailOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ritesh.cashiro.presentation.ui.theme.Spacing
import com.ritesh.cashiro.presentation.ui.theme.Dimensions
import com.ritesh.cashiro.R
import com.ritesh.cashiro.presentation.ui.features.profile.EditProfileState
import com.ritesh.cashiro.presentation.ui.features.profile.PresetAvatarSelection
import com.ritesh.cashiro.presentation.ui.features.profile.ProfileCardPreview
import com.ritesh.cashiro.presentation.ui.components.ColorPickerContent
import com.ritesh.cashiro.presentation.effects.overScrollVertical
import com.ritesh.cashiro.presentation.ui.components.SmsParsingProgressIndicator
import com.ritesh.cashiro.presentation.ui.components.AccountCard
import com.ritesh.cashiro.data.database.entity.AccountBalanceEntity
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Merge
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.filled.Balance
import androidx.compose.material.icons.filled.DriveFileRenameOutline
import androidx.compose.material.icons.filled.Pin
import androidx.work.WorkInfo

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnBoardingScreen(
    modifier: Modifier = Modifier,
    onOnBoardingComplete: () -> Unit,
    onBoardingViewModel: OnBoardingViewModel = hiltViewModel(),
) {
    val uiState by onBoardingViewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.onboardingFinished) {
        if (uiState.onboardingFinished) {
            onOnBoardingComplete()
        }
    }

    val multiplePermissionLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            val readSmsGranted = permissions[Manifest.permission.READ_SMS] == true
            if (readSmsGranted) {
                onBoardingViewModel.onPermissionResult(true)
                onBoardingViewModel.nextStep() // Move to Sync step
            } else {
                onBoardingViewModel.onPermissionDenied()
            }
        }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        bottomBar = {
            OnBoardingBottomBar(
                currentStep = uiState.currentStep,
                onBack = { onBoardingViewModel.previousStep() },
                onContinue = {
                    when (uiState.currentStep) {
                        1 -> onBoardingViewModel.nextStep()
                        2 -> onBoardingViewModel.saveProfile()
                        3 -> {
                            val permissions =
                                mutableListOf(
                                    Manifest.permission.READ_SMS,
                                    Manifest.permission.RECEIVE_SMS
                                )
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                permissions.add(Manifest.permission.POST_NOTIFICATIONS)
                            }
                            multiplePermissionLauncher.launch(permissions.toTypedArray())
                        }
                        4 -> {
                            if (uiState.scanWorkInfo?.state == WorkInfo.State.SUCCEEDED) {
                                onBoardingViewModel.nextStep()
                            } else {
                                onBoardingViewModel.startSmsScan()
                            }
                        }
                        5 -> {
                            onBoardingViewModel.finishOnboarding()
                            onOnBoardingComplete()
                        }
                        6 -> {
                            onBoardingViewModel.saveManualAccount()
                        }
                    }
                },
                isScanning = uiState.isScanning,
                isScanComplete = uiState.scanWorkInfo?.state == WorkInfo.State.SUCCEEDED,
                isContinueEnabled =
                    when (uiState.currentStep) {
                        2 -> uiState.profileState.editedUserName.isNotBlank()
                        4 -> !uiState.isScanning
                        5 -> uiState.mainAccountKey != null
                        6 -> uiState.manualAccountName.isNotBlank() && 
                             uiState.manualAccountBalance.isNotBlank() && 
                             uiState.manualAccountLast4.length == 4
                        else -> true
                    }
            )
        }
    ) { innerPadding ->
        AnimatedContent(
            targetState = uiState.currentStep,
            transitionSpec = {
                if (targetState > initialState) {
                    slideInHorizontally { it } + fadeIn() togetherWith
                            slideOutHorizontally { -it } + fadeOut()
                } else {
                    slideInHorizontally { -it } + fadeIn() togetherWith
                            slideOutHorizontally { it } + fadeOut()
                }
                    .using(SizeTransform(clip = false))
            },
            label = "OnBoardingStepTransition"
        ) { step ->
            Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                when (step) {
                    1 -> WelcomeStep()
                    2 ->
                        ProfileStep(
                            state = uiState.profileState,
                            onNameChange = { onBoardingViewModel.onNameChange(it) },
                            onProfileImageChange = { onBoardingViewModel.onProfileImageChange(it) },
                            onBackgroundColorChange = {
                                onBoardingViewModel.onBackgroundColorChange(it)
                            }
                        )
                    3 -> PermissionsStep(showRationale = uiState.showRationale)
                    4 -> SyncStep(
                        isScanning = uiState.isScanning,
                        workInfo = uiState.scanWorkInfo,
                        onStartScan = { onBoardingViewModel.startSmsScan() },
                        onSkip = { onBoardingViewModel.skipSync() }
                    )
                    5 -> AccountSetupStep(
                        accounts = uiState.accounts,
                        mainAccountKey = uiState.mainAccountKey,
                        selectedForMerge = uiState.selectedAccountsForMerge,
                        onSetMain = { b, a -> onBoardingViewModel.setAsMainAccount(b, a) },
                        onToggleMerge = { onBoardingViewModel.toggleAccountSelectionForMerge(it) },
                        onMerge = { onBoardingViewModel.mergeSelectedAccounts(it) }
                    )
                    6 -> ManualAccountEntryStep(
                        accountName = uiState.manualAccountName,
                        balance = uiState.manualAccountBalance,
                        accountLast4 = uiState.manualAccountLast4,
                        onUpdateName = { onBoardingViewModel.updateManualAccountName(it) },
                        onUpdateBalance = { onBoardingViewModel.updateManualAccountBalance(it) },
                        onUpdateLast4 = { onBoardingViewModel.updateManualAccountLast4(it) }
                    )
                }
            }
        }
    }
}

@Composable
fun WelcomeStep() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(Spacing.xl)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(125.dp)
                .clip(CircleShape)
                .background(
                    color = Color(0xFF181818),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ){
            Image(
                painter = painterResource(id = R.drawable.cashiro),
                contentDescription = "App Logo",
                modifier = Modifier.size(100.dp),
                contentScale = ContentScale.Fit
            )
        }

        Spacer(modifier = Modifier.height(Spacing.xl))

        Text(
            text = "Welcome to Cashiro",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(Spacing.md))

        Text(
            text =
                "Your personal finance assistant that stays on your device. Manage transactions, track budgets, and gain insights effortlessly.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ProfileStep(
    state: EditProfileState,
    onNameChange: (String) -> Unit,
    onProfileImageChange: (Uri?) -> Unit,
    onBackgroundColorChange: (Color) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = Spacing.lg)
            .overScrollVertical()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.lg)
    ) {
        Text(
            text = "Set Up Your Profile",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = Spacing.md)
        )

        ProfileCardPreview(
            profileImageUri = state.editedProfileImageUri,
            backgroundColor = state.editedProfileBackgroundColor,
            bannerImageUri = state.editedBannerImageUri,
            modifier = Modifier.padding(vertical = Spacing.md,horizontal = Spacing.md)
        )

        TextField(
            value = state.editedUserName,
            onValueChange = onNameChange,
            label = { Text("What should we call you?") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.md),
            shape = RoundedCornerShape(Dimensions.Radius.md),
            colors = TextFieldDefaults.colors(
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent
            )
        )

        Column(horizontalAlignment = Alignment.Start, modifier = Modifier.fillMaxWidth()) {
            PresetAvatarSelection(
                selectedUri = state.editedProfileImageUri,
                onSelect = onProfileImageChange
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.md)
                .background(
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    shape = RoundedCornerShape(Dimensions.Radius.md)
                )
        ) {
            ColorPickerContent(
                initialColor = state.editedProfileBackgroundColor.toArgb(),
                onColorChanged = { onBackgroundColorChange(Color(it)) }
            )
        }
    }
}

@Composable
fun PermissionsStep(showRationale: Boolean) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(Spacing.xl)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Filled.MailOutline,
            contentDescription = null,
            modifier = Modifier.size(120.dp),
            tint = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(Spacing.xl))

        Text(
            text = "Automatic Detection",
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(Spacing.md))

        Text(
            text =
                "Cashiro can automatically detect and categorize your bank transactions from SMS messages, saving you time and effort.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(Spacing.lg))

        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(Spacing.md)) {
                Text(
                    text = "Your Privacy Matters",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Spacer(modifier = Modifier.height(Spacing.sm))
                Text(
                    text =
                        "• Only transaction messages are processed\n" +
                                "• All data stays on your device\n" +
                                "• No personal messages are read",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }

        if (showRationale) {
            Spacer(modifier = Modifier.height(Spacing.md))
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text =
                        "Without SMS access, you'll need to manually add all your transactions. We only read bank transaction messages, not personal conversations.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.padding(Spacing.md)
                )
            }
        }
    }
}

@Composable
fun SyncStep(
    isScanning: Boolean,
    workInfo: WorkInfo?,
    onStartScan: () -> Unit,
    onSkip: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(Spacing.xl)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Sync,
            contentDescription = null,
            modifier = Modifier.size(120.dp),
            tint = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(Spacing.xl))

        Text(
            text = "Sync Your Accounts",
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(Spacing.md))

        Text(
            text = "Let's find your existing accounts and transactions by scanning your SMS messages.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(Spacing.xl))

        if (isScanning || workInfo?.state == WorkInfo.State.SUCCEEDED) {
            SmsParsingProgressIndicator(
                workInfo = workInfo,
                modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.md)
            )
        } else {
            Button(
                onClick = onStartScan,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(Dimensions.Radius.md)
            ) {
                Text("Scan Messages Now")
            }

            Spacer(modifier = Modifier.height(Spacing.md))

            TextButton(
                onClick = onSkip,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Skip for Now")
            }
        }
    }
}

@Composable
fun AccountSetupStep(
    accounts: List<AccountBalanceEntity>,
    mainAccountKey: String?,
    selectedForMerge: Set<String>,
    onSetMain: (String, String) -> Unit,
    onToggleMerge: (String) -> Unit,
    onMerge: (AccountBalanceEntity) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(Spacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Setup Your Accounts",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(Spacing.sm))

        Text(
            text = "We found multiple accounts. Select your main account and merge any duplicates.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(Spacing.lg))

        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
            contentPadding = PaddingValues(bottom = Spacing.xl)
        ) {
            items(accounts) { account ->
                val key = "${account.bankName}_${account.accountLast4}"
                val isMain = mainAccountKey == key
                val isSelectedForMerge = selectedForMerge.contains(key)

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(28.dp),
                    border = if (isMain) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
                    onClick = { onSetMain(account.bankName, account.accountLast4) }
                ) {
                    Box {
                        AccountCard(
                            account = account,
                            showMoreOptions = false,
                            onClick = { onSetMain(account.bankName, account.accountLast4) }
                        )

                        Row(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(Spacing.sm),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (isMain) {
                                Badge(containerColor = MaterialTheme.colorScheme.primary) {
                                    Text("Main", color = MaterialTheme.colorScheme.onPrimary)
                                }
                                Spacer(modifier = Modifier.width(Spacing.sm))
                            }

                            IconButton(onClick = { onToggleMerge(key) }) {
                                Icon(
                                    imageVector = if (isSelectedForMerge) Icons.Default.Merge else Icons.Default.Merge,
                                    contentDescription = "Merge",
                                    tint = if (isSelectedForMerge) MaterialTheme.colorScheme.primary 
                                           else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                )
                            }
                        }
                    }
                }
            }

            if (selectedForMerge.size > 1) {
                item {
                    Spacer(modifier = Modifier.height(Spacing.md))
                    Button(
                        onClick = { 
                            // Merge into the first selected account or main account
                            val targetKey = mainAccountKey ?: selectedForMerge.first()
                            val targetAccount = accounts.find { "${it.bankName}_${it.accountLast4}" == targetKey }
                            targetAccount?.let { onMerge(it) }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    ) {
                        Icon(Icons.Default.Merge, contentDescription = null)
                        Spacer(modifier = Modifier.width(Spacing.sm))
                        Text("Merge Selected Accounts")
                    }
                }
            }
        }
    }
}

@Composable
fun ManualAccountEntryStep(
    accountName: String,
    balance: String,
    accountLast4: String,
    onUpdateName: (String) -> Unit,
    onUpdateBalance: (String) -> Unit,
    onUpdateLast4: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(Spacing.lg)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.AccountBalance,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(Spacing.lg))

        Text(
            text = "Add Your Main Account",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(Spacing.sm))

        Text(
            text = "Enter your primary account details to get started with tracking.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(Spacing.xl))

        TextField(
            value = accountName,
            onValueChange = onUpdateName,
            label = { Text("Bank Name (e.g., HDFC, SBI)", fontWeight = FontWeight.SemiBold) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(Dimensions.Radius.md),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                focusedLabelColor = MaterialTheme.colorScheme.primary,
                unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                    0.7f
                )
            ),
            leadingIcon = { Icon(Icons.Default.DriveFileRenameOutline, contentDescription = null)
            }
        )

        Spacer(modifier = Modifier.height(Spacing.md))

        TextField(
            value = balance,
            onValueChange = onUpdateBalance,
            label = { Text("Current Balance") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(Dimensions.Radius.md),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                focusedLabelColor = MaterialTheme.colorScheme.primary,
                unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                    0.7f
                )
            ),
            leadingIcon = { Icon(Icons.Default.Balance, contentDescription = null) },
            singleLine = true
        )

        Spacer(modifier = Modifier.height(Spacing.md))

        TextField(
            value = accountLast4,
            onValueChange = onUpdateLast4,
            label = { Text("Last 4 Digits of Account", fontWeight = FontWeight.SemiBold) },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("e.g. 1234") },
            singleLine = true,
            shape = RoundedCornerShape(Dimensions.Radius.md),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                focusedLabelColor = MaterialTheme.colorScheme.primary,
                unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                    0.7f
                )
            ),
            leadingIcon = { Icon(Icons.Default.Pin, contentDescription = null) }
        )
    }
}

@Composable
fun OnBoardingBottomBar(
    currentStep: Int,
    isScanning: Boolean = false,
    isScanComplete: Boolean = false,
    onBack: () -> Unit,
    onContinue: () -> Unit,
    isContinueEnabled: Boolean
) {
    Surface(tonalElevation = 3.dp, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(Spacing.lg).navigationBarsPadding(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (currentStep > 1 && currentStep != 4) { // Don't allow back from Sync during/after scan
                TextButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = null)
                    Spacer(Modifier.width(Spacing.sm))
                    Text("Back")
                }
            } else {
                Spacer(Modifier.width(80.dp))
            }

            Button(
                onClick = onContinue,
                enabled = isContinueEnabled,
                modifier = Modifier.height(56.dp),
                shape = RoundedCornerShape(Dimensions.Radius.md)
            ) {
                Text(
                    text =
                        when (currentStep) {
                             1 -> "Get Started"
                             2 -> "Save & Continue"
                             3 -> "Enable Permissions"
                             4 -> if (isScanning) "Scanning..." else if (isScanComplete) "Continue" else "Scan Now"
                             5 -> "Finish"
                             6 -> "Save & Finish"
                             else -> "Continue"
                         }
                )
                Spacer(Modifier.width(Spacing.sm))
                Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = null)
            }
        }
    }
}
