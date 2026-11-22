package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ai.FarmAiViewModel
import com.example.ai.configureAppCheck
import com.example.data.local.SmartFarmDatabase
import com.example.data.local.UserPreferencesRepository
import com.example.data.local.UserRole
import com.example.data.repository.FarmRepository
import com.example.i18n.AppLanguage
import com.example.i18n.CurrencyOption
import com.example.i18n.LocalAppLanguage
import com.example.i18n.Localization
import com.example.ui.screens.admin.SuperAdminDashboardScreen
import com.example.ui.screens.auth.AuthScreen
import com.example.ui.screens.customer.CustomerPortalScreen
import com.example.ui.screens.farmer.*
import com.example.ui.state.SmartFarmViewModel
import com.example.ui.theme.SmartFarmTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        configureAppCheck(this, intent)
        enableEdgeToEdge()

        val db = SmartFarmDatabase.getInstance(applicationContext)
        val repository = FarmRepository(db)
        val prefsRepo = UserPreferencesRepository(applicationContext)

        setContent {
            val viewModel: SmartFarmViewModel = viewModel(
                factory = SmartFarmViewModel.provideFactory(repository, prefsRepo)
            )
            val prefs by viewModel.preferences.collectAsStateWithLifecycle()
            val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
            val isInitialized by viewModel.isInitialized.collectAsStateWithLifecycle()
            val snackbarMsg by viewModel.snackbarMessage.collectAsStateWithLifecycle()
            val authError by viewModel.authError.collectAsStateWithLifecycle()
            val overrides by viewModel.languageOverrides.collectAsStateWithLifecycle()
            val plans by viewModel.plans.collectAsStateWithLifecycle()
            val settings by viewModel.systemSettings.collectAsStateWithLifecycle()
            val notifications by viewModel.notifications.collectAsStateWithLifecycle()

            val language = if (prefs.languageCode == "bn") AppLanguage.BN else AppLanguage.EN
            val currency = CurrencyOption.fromCode(prefs.currencyCode)
            val strings = Localization.strings(language, overrides)
            val darkTheme = prefs.darkModeOverride ?: isSystemInDarkTheme()

            val snackbarHostState = remember { SnackbarHostState() }
            LaunchedEffect(snackbarMsg) {
                val msg = snackbarMsg
                if (msg != null) {
                    snackbarHostState.showSnackbar(msg)
                    viewModel.clearMessage()
                }
            }

            CompositionLocalProvider(LocalAppLanguage provides language) {
                SmartFarmTheme(darkTheme = darkTheme) {
                    Scaffold(
                        contentWindowInsets = WindowInsets(0, 0, 0, 0),
                        snackbarHost = { SnackbarHost(snackbarHostState) }
                    ) { outerPadding ->
                    Box(modifier = Modifier.fillMaxSize().padding(outerPadding)) {
                        if (!isInitialized) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator()
                            }
                        } else if (currentUser == null) {
                            AuthScreen(
                                strings = strings,
                                currentLanguage = language,
                                currentCurrency = currency,
                                darkModeOverride = prefs.darkModeOverride,
                                authError = authError,
                                onClearError = viewModel::clearAuthError,
                                onLogin = viewModel::login,
                                onRegister = viewModel::register,
                                onResetPassword = viewModel::resetPassword,
                                onSelectLanguage = viewModel::setLanguage,
                                onSelectCurrency = viewModel::setCurrency,
                                onSelectDarkMode = viewModel::setDarkMode
                            )
                        } else {
                            val user = currentUser!!
                            when (user.role) {
                                UserRole.SUPER_ADMIN.name -> {
                                    val adminState by viewModel.adminState.collectAsStateWithLifecycle()
                                    SuperAdminDashboardScreen(
                                        adminUser = user,
                                        state = adminState,
                                        plans = plans,
                                        systemSettings = settings,
                                        strings = strings,
                                        currentLanguage = language,
                                        currentCurrency = currency,
                                        darkModeOverride = prefs.darkModeOverride,
                                        onSetUserStatus = viewModel::adminSetUserStatus,
                                        onSavePlan = viewModel::adminSavePlan,
                                        onDeletePlan = viewModel::adminDeletePlan,
                                        onUpdatePayment = viewModel::adminUpdatePayment,
                                        onGrantPlan = viewModel::adminGrantPlan,
                                        onBroadcast = viewModel::adminBroadcast,
                                        onSaveSettings = viewModel::adminUpdateSettings,
                                        onResolveSupport = viewModel::adminResolveSupport,
                                        onSelectLanguage = viewModel::setLanguage,
                                        onSelectCurrency = viewModel::setCurrency,
                                        onSelectDarkMode = viewModel::setDarkMode,
                                        onLogout = viewModel::logout
                                    )
                                }

                                UserRole.CUSTOMER.name -> {
                                    val customerState by viewModel.customerState.collectAsStateWithLifecycle()
                                    CustomerPortalScreen(
                                        user = user,
                                        state = customerState,
                                        notifications = notifications,
                                        strings = strings,
                                        currentLanguage = language,
                                        currentCurrency = currency,
                                        darkModeOverride = prefs.darkModeOverride,
                                        onAddToCart = { viewModel.addToCart(it) },
                                        onUpdateCartQty = viewModel::updateCartQuantity,
                                        onPlaceOrder = viewModel::placeOrder,
                                        onRequestCancelOrder = viewModel::requestOrderCancellation,
                                        onMarkNotificationsRead = viewModel::markNotificationsRead,
                                        onUpdateProfile = viewModel::updateProfile,
                                        onSelectLanguage = viewModel::setLanguage,
                                        onSelectCurrency = viewModel::setCurrency,
                                        onSelectDarkMode = viewModel::setDarkMode,
                                        onLogout = viewModel::logout
                                    )
                                }

                                else -> {
                                    val farmerState by viewModel.farmerState.collectAsStateWithLifecycle()
                                    FarmerShell(
                                        user = user,
                                        farmerState = farmerState,
                                        plans = plans,
                                        notifications = notifications,
                                        announcementBanner = settings.announcementBanner,
                                        strings = strings,
                                        currentLanguage = language,
                                        currentCurrency = currency,
                                        darkModeOverride = prefs.darkModeOverride,
                                        languageOverrides = overrides,
                                        viewModel = viewModel
                                    )
                                }
                            }
                        }
                    }
                }
            }
            }
        }
    }
}

private enum class FarmerSection {
    DASHBOARD, FARMS, LIVESTOCK, EGGS, OPERATIONS, COMMERCE, MORE
}

@Composable
private fun FarmerShell(
    user: com.example.data.local.UserEntity,
    farmerState: com.example.ui.state.FarmerDashboardState,
    plans: List<com.example.data.local.SubscriptionPlanEntity>,
    notifications: List<com.example.data.local.AppNotificationEntity>,
    announcementBanner: String,
    strings: com.example.i18n.AppStrings,
    currentLanguage: AppLanguage,
    currentCurrency: CurrencyOption,
    darkModeOverride: Boolean?,
    languageOverrides: Map<String, String>,
    viewModel: SmartFarmViewModel
) {
    var section by remember { mutableStateOf(FarmerSection.DASHBOARD) }
    var moreInitialTab by remember { mutableIntStateOf(0) }
    var showAiAdvisor by remember { mutableStateOf(false) }
    val aiViewModel: FarmAiViewModel = viewModel()
    val unreadCount = notifications.count { !it.readFlag }

    if (section != FarmerSection.DASHBOARD) {
        BackHandler { section = FarmerSection.DASHBOARD }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isWide = maxWidth >= 600.dp

        // Compact mobile bottom bar shows 5 core items + "More" sheet/hub; wide sidebar shows all 7!
        val railItems = listOf(
            Triple(FarmerSection.DASHBOARD, strings.dashboard, Icons.Default.Dashboard),
            Triple(FarmerSection.FARMS, strings.farms, Icons.Default.Agriculture),
            Triple(FarmerSection.LIVESTOCK, strings.livestock, Icons.Default.Pets),
            Triple(FarmerSection.EGGS, strings.eggs, Icons.Default.Egg),
            Triple(FarmerSection.OPERATIONS, strings.operations, Icons.Default.Inventory2),
            Triple(FarmerSection.COMMERCE, strings.commerce, Icons.Default.PointOfSale),
            Triple(FarmerSection.MORE, strings.more, Icons.Default.Tune)
        )

        val mobileBottomItems = listOf(
            Triple(FarmerSection.DASHBOARD, strings.dashboard, Icons.Default.Dashboard),
            Triple(FarmerSection.FARMS, strings.farms, Icons.Default.Agriculture),
            Triple(FarmerSection.LIVESTOCK, strings.livestock, Icons.Default.Pets),
            Triple(FarmerSection.COMMERCE, strings.commerce, Icons.Default.PointOfSale),
            Triple(FarmerSection.MORE, strings.more, Icons.Default.GridView)
        )

        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            bottomBar = {
                if (!isWide) {
                    NavigationBar {
                        mobileBottomItems.forEach { (sec, label, icon) ->
                            val isSelected = section == sec || (sec == FarmerSection.MORE && (section == FarmerSection.EGGS || section == FarmerSection.OPERATIONS))
                            NavigationBarItem(
                                selected = isSelected,
                                onClick = {
                                    moreInitialTab = 0
                                    section = sec
                                },
                                icon = {
                                    BadgedBox(badge = { if (sec == FarmerSection.MORE && unreadCount > 0) Badge { Text("$unreadCount") } }) {
                                        Icon(icon, contentDescription = label)
                                    }
                                },
                                label = { Text(label, maxLines = 1) }
                            )
                        }
                    }
                }
            }
        ) { padding ->
            Row(modifier = Modifier.fillMaxSize().padding(padding)) {
                if (isWide) {
                    NavigationRail {
                        railItems.forEach { (sec, label, icon) ->
                            NavigationRailItem(
                                selected = section == sec,
                                onClick = {
                                    moreInitialTab = 0
                                    section = sec
                                },
                                icon = {
                                    BadgedBox(badge = { if (sec == FarmerSection.MORE && unreadCount > 0) Badge { Text("$unreadCount") } }) {
                                        Icon(icon, contentDescription = label)
                                    }
                                },
                                label = { Text(label) }
                            )
                        }
                    }
                }

                Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    // On mobile compact width, provide quick pill switcher for Eggs & Operations when inside More or Livestock
                    if (!isWide && (section == FarmerSection.MORE || section == FarmerSection.EGGS || section == FarmerSection.OPERATIONS)) {
                        Surface(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f), modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                FilterChip(
                                    selected = section == FarmerSection.EGGS,
                                    onClick = { section = FarmerSection.EGGS },
                                    label = { Text(strings.eggs) },
                                    leadingIcon = { Icon(Icons.Default.Egg, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                )
                                FilterChip(
                                    selected = section == FarmerSection.OPERATIONS,
                                    onClick = { section = FarmerSection.OPERATIONS },
                                    label = { Text(strings.operations) },
                                    leadingIcon = { Icon(Icons.Default.Inventory2, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                )
                                FilterChip(
                                    selected = section == FarmerSection.MORE,
                                    onClick = { section = FarmerSection.MORE },
                                    label = { Text(strings.reports) },
                                    leadingIcon = { Icon(Icons.Default.Assessment, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                )
                            }
                        }
                    }

                    Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                        when (section) {
                            FarmerSection.DASHBOARD -> FarmerDashboardScreen(
                                user = user,
                                state = farmerState,
                                strings = strings,
                                currency = currentCurrency,
                                unreadNotificationsCount = unreadCount,
                                announcementBanner = announcementBanner,
                                onSelectFarm = viewModel::selectActiveFarm,
                                onOpenFarms = { section = FarmerSection.FARMS },
                                onOpenLivestock = { section = FarmerSection.LIVESTOCK },
                                onOpenEggs = { section = FarmerSection.EGGS },
                                onOpenSales = { section = FarmerSection.COMMERCE },
                                onOpenNotifications = { moreInitialTab = 2; section = FarmerSection.MORE },
                                onOpenSubscription = { moreInitialTab = 1; section = FarmerSection.MORE },
                                onOpenAiAdvisor = { showAiAdvisor = true },
                                onToggleLanguage = {
                                    viewModel.setLanguage(if (currentLanguage == AppLanguage.BN) AppLanguage.EN else AppLanguage.BN)
                                }
                            )

                            FarmerSection.FARMS -> FarmsScreen(
                                state = farmerState,
                                strings = strings,
                                currency = currentCurrency,
                                onSelectFarm = viewModel::selectActiveFarm,
                                onSaveFarm = viewModel::saveFarm,
                                onDeleteFarm = viewModel::deleteFarm
                            )

                            FarmerSection.LIVESTOCK -> LivestockScreen(
                                state = farmerState,
                                strings = strings,
                                currency = currentCurrency,
                                onSelectFarm = viewModel::selectActiveFarm,
                                onAddCategory = viewModel::addCustomAnimalCategory,
                                onSaveAnimal = viewModel::saveAnimalRecord,
                                onDeleteAnimal = viewModel::deleteAnimalRecord,
                                onRecordEvent = viewModel::recordAnimalEvent
                            )

                            FarmerSection.EGGS -> EggsScreen(
                                state = farmerState,
                                strings = strings,
                                currency = currentCurrency,
                                onSelectFarm = viewModel::selectActiveFarm,
                                onRecordEggs = viewModel::recordEggProduction,
                                onDeleteEgg = viewModel::deleteEggRecord
                            )

                            FarmerSection.OPERATIONS -> OperationsScreen(
                                state = farmerState,
                                strings = strings,
                                currency = currentCurrency,
                                onSelectFarm = viewModel::selectActiveFarm,
                                onSaveInventory = viewModel::saveInventoryItem,
                                onAdjustInventory = viewModel::adjustInventoryStock,
                                onDeleteInventory = viewModel::deleteInventoryItem,
                                onAddExpense = viewModel::addExpense,
                                onDeleteExpense = viewModel::deleteExpense,
                                onSaveEmployee = viewModel::saveEmployee,
                                onDeleteEmployee = viewModel::deleteEmployee,
                                onMarkAttendance = viewModel::markAttendance,
                                onPaySalary = viewModel::paySalary
                            )

                            FarmerSection.COMMERCE -> CommerceScreen(
                                state = farmerState,
                                strings = strings,
                                currency = currentCurrency,
                                onSelectFarm = viewModel::selectActiveFarm,
                                onCreateSale = viewModel::createSale,
                                onSettleDue = viewModel::settleSaleDue,
                                onDeleteSale = viewModel::deleteSale,
                                onSaveCustomer = viewModel::saveCustomer,
                                onDeleteCustomer = viewModel::deleteCustomer,
                                onUpdateOrderStatus = viewModel::updateOrderStatus,
                                onMessage = viewModel::showMessage
                            )

                            FarmerSection.MORE -> ReportsSubscriptionProfileScreen(
                                user = user,
                                state = farmerState,
                                plans = plans,
                                notifications = notifications,
                                strings = strings,
                                currentLanguage = currentLanguage,
                                currentCurrency = currentCurrency,
                                darkModeOverride = darkModeOverride,
                                languageOverrides = languageOverrides,
                                initialTab = moreInitialTab,
                                onSelectLanguage = viewModel::setLanguage,
                                onSelectCurrency = viewModel::setCurrency,
                                onSelectDarkMode = viewModel::setDarkMode,
                                onSetLanguageOverride = viewModel::setLanguageOverride,
                                onUpdateProfile = viewModel::updateProfile,
                                onSubscribe = viewModel::subscribe,
                                onMarkNotificationsRead = viewModel::markNotificationsRead,
                                onSubmitSupport = viewModel::submitSupport,
                                onLogout = viewModel::logout,
                                onMessage = viewModel::showMessage
                            )
                        }
                    }
                }
            }
        }
    }

    if (showAiAdvisor) {
        FarmAiAdvisorDialog(
            farmState = farmerState,
            currency = currentCurrency,
            language = currentLanguage,
            aiViewModel = aiViewModel,
            onDismiss = { showAiAdvisor = false }
        )
    }
}
