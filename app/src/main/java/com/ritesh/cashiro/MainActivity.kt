package com.ritesh.cashiro

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.fragment.app.FragmentActivity
import com.ritesh.cashiro.receiver.SmsBroadcastReceiver
import com.ritesh.cashiro.data.manager.NotificationScheduler
import androidx.lifecycle.lifecycleScope
import com.ritesh.cashiro.presentation.ui.features.accounts.AccountDetailViewModel
import com.ritesh.cashiro.presentation.ui.features.accounts.ManageAccountsViewModel
import com.ritesh.cashiro.presentation.ui.features.add.AddViewModel
import com.ritesh.cashiro.presentation.ui.features.analytics.AnalyticsViewModel
import com.ritesh.cashiro.presentation.ui.features.budgets.BudgetViewModel
import com.ritesh.cashiro.presentation.ui.features.categories.CategoriesViewModel
import com.ritesh.cashiro.presentation.ui.features.chat.ChatViewModel
import com.ritesh.cashiro.presentation.ui.features.home.HomeViewModel
import com.ritesh.cashiro.presentation.ui.features.onboarding.OnBoardingViewModel
import com.ritesh.cashiro.presentation.ui.features.profile.ProfileViewModel
import com.ritesh.cashiro.presentation.ui.features.settings.SettingsViewModel
import com.ritesh.cashiro.presentation.ui.features.settings.appearance.ThemeViewModel
import com.ritesh.cashiro.presentation.ui.features.settings.applock.AppLockViewModel
import com.ritesh.cashiro.presentation.ui.features.settings.notifications.NotificationViewModel
import com.ritesh.cashiro.presentation.ui.features.settings.rules.RulesViewModel
import com.ritesh.cashiro.presentation.ui.features.settings.unrecognized.UnrecognizedSmsViewModel
import com.ritesh.cashiro.presentation.ui.features.spotlight.SpotlightViewModel
import com.ritesh.cashiro.presentation.ui.features.subscriptions.SubscriptionsViewModel
import com.ritesh.cashiro.presentation.ui.features.transactions.TransactionDetailViewModel
import com.ritesh.cashiro.presentation.ui.features.transactions.TransactionsViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.getValue

@AndroidEntryPoint
class MainActivity : FragmentActivity() {
    private val themeViewModel: ThemeViewModel by viewModels()
    private val appLockViewModel: AppLockViewModel by viewModels()
    
    @Inject
    lateinit var notificationScheduler: NotificationScheduler

    // Transaction ID to edit when launched from notification
    var editTransactionId by mutableStateOf<Long?>(null)
        private set

    override fun onCreate(savedInstanceState: Bundle?) {
        // Install splash screen before super.onCreate()
        installSplashScreen()

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
//            window.isStatusBarContrastEnforced = false
        }

        // Handle intent if activity is launched from notification
        handleEditIntent(intent)

        // Schedule daily reminders
        lifecycleScope.launch {
            notificationScheduler.scheduleDailyReminder()
        }

        setContent {
            CashiroApp(
                editTransactionId = editTransactionId,
                onEditComplete = { editTransactionId = null },
                appLockViewModel = appLockViewModel,
                themeViewModel = themeViewModel,
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // Handle intent when activity is already running
        handleEditIntent(intent)
    }

    private fun handleEditIntent(intent: Intent?) {
        if (intent?.action == SmsBroadcastReceiver.ACTION_EDIT_TRANSACTION) {
            val transactionId = intent.getLongExtra(SmsBroadcastReceiver.EXTRA_TRANSACTION_ID, -1)
            if (transactionId != -1L) {
                editTransactionId = transactionId
            }
        }
    }
}
