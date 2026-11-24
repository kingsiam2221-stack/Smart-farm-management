package com.example

import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ai.FarmAiViewModel
import com.example.ai.configureAppCheck
import com.example.data.local.AppPermission
import com.example.data.local.EmployeeAccessPolicy
import com.example.data.local.SmartFarmDatabase
import com.example.data.local.SubscriptionFeature
import com.example.data.local.SystemSettingsEntity
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
import com.example.ui.theme.AppThemeMode
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
            val themeMode = AppThemeMode.fromOverride(prefs.darkModeOverride)
            val systemDark = isSystemInDarkTheme()
            val isDarkEffective = when (themeMode) {
                AppThemeMode.DARK -> true
                AppThemeMode.WHITE -> false
                AppThemeMode.DEFAULT -> systemDark
            }

            val view = LocalView.current
            if (!view.isInEditMode) {
                SideEffect {
                    val window = (view.context as? Activity)?.window
                    if (window != null) {
                        val controller = WindowCompat.getInsetsController(window, view)
                        controller.isAppearanceLightStatusBars = !isDarkEffective
                        controller.isAppearanceLightNavigationBars = !isDarkEffective
                    }
                }
            }

            val snackbarHostState = remember { SnackbarHostState() }
            LaunchedEffect(snackbarMsg) {
                val msg = snackbarMsg
                if (msg != null) {
                    snackbarHostState.showSnackbar(msg)
                    viewModel.clearMessage()
                }
            }

            CompositionLocalProvider(LocalAppLanguage provides language) {
                SmartFarmTheme(themeMode = themeMode, darkTheme = systemDark) {
                    Scaffold(
                        contentWindowInsets = WindowInsets(0, 0, 0, 0),
                        snackbarHost = { SnackbarHost(snackbarHostState) }
                    ) { outerPadding ->
                        Box(modifier = Modifier.fillMaxSize().padding(outerPadding)) {
                            val activeUser = currentUser
                            if (!isInitialized) {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    CircularProgressIndicator()
                                }
                            } else if (activeUser == null) {
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
                                when (activeUser.role) {
                                    UserRole.SUPER_ADMIN.name -> {
                                        val adminState by viewModel.adminState.collectAsStateWithLifecycle()
                                        SuperAdminDashboardScreen(
                                            adminUser = activeUser,
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
                                            onToggleFeatureSubscription = viewModel::adminToggleFeatureSubscription,
                                            onSetAllFeatureSubscriptions = viewModel::adminSetAllFeatureSubscriptions,
                                            onResolveSupport = viewModel::adminResolveSupport,
                                            onSetEmployeePermission = viewModel::setEmployeePermission,
                                            onSetAllPermissionsForEmployee = viewModel::setAllPermissionsForEmployee,
                                            onApplyPermissionPreset = viewModel::applyPermissionPreset,
                                            onSetPermissionForMultipleEmployees = viewModel::setPermissionForMultipleEmployees,
                                            onAddCustomPermissionOption = viewModel::addCustomPermissionOption,
                                            onDeleteCustomPermissionOption = viewModel::deleteCustomPermissionOption,
                                            onProvisionEmployeeLogin = viewModel::provisionEmployeeLogin,
                                            onSelectLanguage = viewModel::setLanguage,
                                            onSelectCurrency = viewModel::setCurrency,
                                            onSelectDarkMode = viewModel::setDarkMode,
                                            onLogout = viewModel::logout
                                        )
                                    }

                                    UserRole.CUSTOMER.name -> {
                                        val customerState by viewModel.customerState.collectAsStateWithLifecycle()
                                        CustomerPortalScreen(
                                            user = activeUser,
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
                                            onSearchChange = viewModel::updateMarketplaceSearch,
                                            onSelectCategory = viewModel::selectMarketplaceCategory,
                                            onLoadMore = viewModel::loadMoreMarketplaceItems,
                                            onSelectLanguage = viewModel::setLanguage,
                                            onSelectCurrency = viewModel::setCurrency,
                                            onSelectDarkMode = viewModel::setDarkMode,
                                            onLogout = viewModel::logout
                                        )
                                    }

                                    else -> {
                                        val farmerState by viewModel.farmerState.collectAsStateWithLifecycle()
                                        FarmerShell(
                                            user = activeUser,
                                            farmerState = farmerState,
                                            plans = plans,
                                            notifications = notifications,
                                            systemSettings = settings,
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
    DASHBOARD, FARMS, ONLINE_PRODUCTS, LIVESTOCK, EGGS, OPERATIONS, COMMERCE, GUIDE, PERMISSIONS, MORE
}

private data class NavEntry(
    val section: FarmerSection,
    val label: String,
    val icon: ImageVector
)

private fun isSectionPermitted(sec: FarmerSection, policy: EmployeeAccessPolicy): Boolean = when (sec) {
    FarmerSection.DASHBOARD ->
        policy.hasAny(AppPermission.DASHBOARD_OVERVIEW, AppPermission.DASHBOARD_CHARTS, AppPermission.AI_ADVISOR) || policy.isAdminOwner
    FarmerSection.FARMS ->
        policy.hasAny(AppPermission.FARMS_VIEW, AppPermission.FARMS_CREATE_EDIT, AppPermission.FARMS_DELETE)
    FarmerSection.ONLINE_PRODUCTS ->
        policy.hasAny(AppPermission.ONLINE_PRODUCTS_VIEW, AppPermission.ONLINE_PRODUCTS_MANAGE)
    FarmerSection.LIVESTOCK ->
        policy.hasAny(AppPermission.LIVESTOCK_VIEW, AppPermission.LIVESTOCK_CREATE_EDIT, AppPermission.LIVESTOCK_HEALTH_EVENTS)
    FarmerSection.EGGS ->
        policy.hasAny(AppPermission.EGGS_VIEW, AppPermission.EGGS_RECORD)
    FarmerSection.OPERATIONS ->
        policy.hasAny(AppPermission.INVENTORY_VIEW, AppPermission.EXPENSES_VIEW, AppPermission.EMPLOYEES_VIEW)
    FarmerSection.COMMERCE ->
        policy.hasAny(AppPermission.SALES_VIEW, AppPermission.CUSTOMERS_VIEW, AppPermission.ORDERS_VIEW)
    FarmerSection.GUIDE ->
        policy.has(AppPermission.FARM_GUIDE_VIEW)
    FarmerSection.PERMISSIONS ->
        policy.canManagePermissions()
    FarmerSection.MORE ->
        true // Always visible so users can access Profile & Log Out
}

@Composable
private fun FarmerShell(
    user: com.example.data.local.UserEntity,
    farmerState: com.example.ui.state.FarmerDashboardState,
    plans: List<com.example.data.local.SubscriptionPlanEntity>,
    notifications: List<com.example.data.local.AppNotificationEntity>,
    systemSettings: SystemSettingsEntity,
    strings: com.example.i18n.AppStrings,
    currentLanguage: AppLanguage,
    currentCurrency: CurrencyOption,
    darkModeOverride: Boolean?,
    languageOverrides: Map<String, String>,
    viewModel: SmartFarmViewModel
) {
    val policy = farmerState.accessPolicy
    val allEntries = remember(strings) {
        listOf(
            NavEntry(FarmerSection.DASHBOARD, strings.dashboard, Icons.Default.Dashboard),
            NavEntry(FarmerSection.FARMS, strings.farms, Icons.Default.Agriculture),
            NavEntry(FarmerSection.ONLINE_PRODUCTS, strings.onlineProducts, Icons.Default.Storefront),
            NavEntry(FarmerSection.LIVESTOCK, strings.livestock, Icons.Default.Pets),
            NavEntry(FarmerSection.EGGS, strings.eggs, Icons.Default.Egg),
            NavEntry(FarmerSection.OPERATIONS, strings.operations, Icons.Default.Inventory2),
            NavEntry(FarmerSection.COMMERCE, strings.commerce, Icons.Default.PointOfSale),
            NavEntry(FarmerSection.GUIDE, strings.farmGuide, Icons.AutoMirrored.Filled.MenuBook),
            NavEntry(FarmerSection.PERMISSIONS, strings.permissions, Icons.Default.AdminPanelSettings),
            NavEntry(FarmerSection.MORE, strings.more, Icons.Default.Tune)
        )
    }
    val permittedEntries = remember(allEntries, policy) {
        allEntries.filter { isSectionPermitted(it.section, policy) }
    }

    var section by remember(user.id) { mutableStateOf(FarmerSection.DASHBOARD) }
    var moreInitialTab by remember { mutableStateOf(MoreTab.REPORTS) }
    var operationsInitialTab by remember { mutableStateOf(OperationsTab.INVENTORY) }
    var commerceInitialTab by remember { mutableStateOf(CommerceTab.SALES) }
    var openCreateFarmOnEntry by remember { mutableStateOf(false) }
    var openAddOnlineProductOnEntry by remember { mutableStateOf(false) }
    var showAiAdvisor by remember { mutableStateOf(false) }
    val aiViewModel: FarmAiViewModel = viewModel()
    val unreadCount = if (policy.has(AppPermission.ALERTS_CAUTION_VIEW)) notifications.count { !it.readFlag } else 0

    // Keep current section valid whenever Admin grants/revokes permissions in real time
    LaunchedEffect(policy, section) {
        if (!isSectionPermitted(section, policy)) {
            section = permittedEntries.firstOrNull()?.section ?: FarmerSection.MORE
        }
    }

    val homeSection = permittedEntries.firstOrNull()?.section ?: FarmerSection.MORE
    if (section != homeSection) {
        BackHandler { section = homeSection }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isWide = maxWidth >= 600.dp

        val mobileBarEntries = remember(permittedEntries) {
            val nonMore = permittedEntries.filter { it.section != FarmerSection.MORE }
            if (nonMore.size <= 4) {
                permittedEntries
            } else {
                // Up to 4 primary items + More (which houses remaining permitted sections via quick pills)
                val primary = nonMore.take(4)
                val moreEntry = permittedEntries.firstOrNull { it.section == FarmerSection.MORE }
                if (moreEntry != null) primary + moreEntry else primary
            }
        }
        val mobileBarSections = remember(mobileBarEntries) { mobileBarEntries.map { it.section }.toSet() }
        val overflowMobileEntries = remember(permittedEntries, mobileBarSections) {
            permittedEntries.filter { it.section != FarmerSection.MORE && it.section !in mobileBarSections }
        }

        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            bottomBar = {
                if (!isWide) {
                    NavigationBar {
                        mobileBarEntries.forEach { entry ->
                            val isSelected = section == entry.section ||
                                (entry.section == FarmerSection.MORE && section !in mobileBarSections)
                            NavigationBarItem(
                                selected = isSelected,
                                onClick = {
                                    if (entry.section == FarmerSection.MORE) moreInitialTab = MoreTab.REPORTS
                                    if (entry.section == FarmerSection.OPERATIONS) operationsInitialTab = OperationsTab.INVENTORY
                                    if (entry.section == FarmerSection.COMMERCE) commerceInitialTab = CommerceTab.SALES
                                    section = entry.section
                                },
                                icon = {
                                    BadgedBox(badge = { if (entry.section == FarmerSection.MORE && unreadCount > 0) Badge { Text("$unreadCount") } }) {
                                        Icon(entry.icon, contentDescription = entry.label)
                                    }
                                },
                                label = { Text(entry.label, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                            )
                        }
                    }
                }
            }
        ) { padding ->
            Row(modifier = Modifier.fillMaxSize().padding(padding)) {
                if (isWide) {
                    NavigationRail {
                        permittedEntries.forEach { entry ->
                            NavigationRailItem(
                                selected = section == entry.section,
                                onClick = {
                                    if (entry.section == FarmerSection.MORE) moreInitialTab = MoreTab.REPORTS
                                    if (entry.section == FarmerSection.OPERATIONS) operationsInitialTab = OperationsTab.INVENTORY
                                    if (entry.section == FarmerSection.COMMERCE) commerceInitialTab = CommerceTab.SALES
                                    section = entry.section
                                },
                                icon = {
                                    BadgedBox(badge = { if (entry.section == FarmerSection.MORE && unreadCount > 0) Badge { Text("$unreadCount") } }) {
                                        Icon(entry.icon, contentDescription = entry.label)
                                    }
                                },
                                label = { Text(entry.label, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                            )
                        }
                    }
                }

                Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    // Quick pill switcher for overflow sections when inside More or an overflow section on compact phones
                    if (!isWide && (section == FarmerSection.MORE || section !in mobileBarSections) && overflowMobileEntries.isNotEmpty()) {
                        Surface(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f), modifier = Modifier.fillMaxWidth()) {
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                overflowMobileEntries.forEach { entry ->
                                    item(key = "overflow_${entry.section.name}") {
                                        FilterChip(
                                            selected = section == entry.section,
                                            onClick = { section = entry.section },
                                            label = { Text(entry.label) },
                                            leadingIcon = { Icon(entry.icon, contentDescription = null, modifier = Modifier.size(16.dp)) },
                                            modifier = when (entry.section) {
                                                FarmerSection.GUIDE -> Modifier.testTag("mobile_guide_chip")
                                                FarmerSection.PERMISSIONS -> Modifier.testTag("mobile_permissions_chip")
                                                FarmerSection.ONLINE_PRODUCTS -> Modifier.testTag("mobile_online_products_chip")
                                                else -> Modifier
                                            }
                                        )
                                    }
                                }
                                item(key = "overflow_MORE") {
                                    FilterChip(
                                        selected = section == FarmerSection.MORE,
                                        onClick = { section = FarmerSection.MORE },
                                        label = {
                                            Text(
                                                if (policy.has(AppPermission.REPORTS_VIEW_EXPORT)) strings.reports
                                                else strings.profile
                                            )
                                        },
                                        leadingIcon = { Icon(Icons.Default.Assessment, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                    )
                                }
                            }
                        }
                    }

                    Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                        val openSubscriptionTab: () -> Unit = {
                            moreInitialTab = MoreTab.SUBSCRIPTION
                            section = FarmerSection.MORE
                        }
                        val openAiIfAllowed: () -> Unit = {
                            if (policy.has(AppPermission.AI_ADVISOR)) {
                                if (farmerState.subscriptionPolicy.canAccess(SubscriptionFeature.DASHBOARD_CHARTS_AI)) {
                                    showAiAdvisor = true
                                } else if (policy.has(AppPermission.SUBSCRIPTION_MANAGE)) {
                                    openSubscriptionTab()
                                } else {
                                    viewModel.showMessage(
                                        if (currentLanguage == AppLanguage.BN) {
                                            "AI পরামর্শ ব্যবহার করতে সক্রিয় সাবস্ক্রিপশন প্রয়োজন।"
                                        } else {
                                            "An active subscription is required to use the AI Advisor."
                                        }
                                    )
                                }
                            }
                        }

                        when (section) {
                            FarmerSection.DASHBOARD -> FarmerDashboardScreen(
                                user = user,
                                state = farmerState,
                                strings = strings,
                                currency = currentCurrency,
                                unreadNotificationsCount = unreadCount,
                                announcementBanner = systemSettings.announcementBanner,
                                darkModeOverride = darkModeOverride,
                                onSelectFarm = viewModel::selectActiveFarm,
                                onOpenFarms = {
                                    openCreateFarmOnEntry = false
                                    section = FarmerSection.FARMS
                                },
                                onOpenAddFarm = {
                                    openCreateFarmOnEntry = true
                                    section = FarmerSection.FARMS
                                },
                                onOpenLivestock = { section = FarmerSection.LIVESTOCK },
                                onOpenEggs = { section = FarmerSection.EGGS },
                                onOpenSales = {
                                    commerceInitialTab = CommerceTab.SALES
                                    section = FarmerSection.COMMERCE
                                },
                                onOpenOrders = {
                                    commerceInitialTab = CommerceTab.ORDERS
                                    section = FarmerSection.COMMERCE
                                },
                                onOpenOnlineProducts = {
                                    openAddOnlineProductOnEntry = false
                                    section = FarmerSection.ONLINE_PRODUCTS
                                },
                                onOpenInventory = {
                                    operationsInitialTab = OperationsTab.INVENTORY
                                    section = FarmerSection.OPERATIONS
                                },
                                onOpenGuide = { section = FarmerSection.GUIDE },
                                onOpenNotifications = {
                                    moreInitialTab = MoreTab.ALERTS
                                    section = FarmerSection.MORE
                                },
                                onOpenSubscription = openSubscriptionTab,
                                onOpenPermissions = { section = FarmerSection.PERMISSIONS },
                                onOpenAiAdvisor = openAiIfAllowed,
                                onToggleLanguage = {
                                    if (policy.has(AppPermission.PROFILE_APPEARANCE)) {
                                        viewModel.setLanguage(if (currentLanguage == AppLanguage.BN) AppLanguage.EN else AppLanguage.BN)
                                    }
                                },
                                onSelectDarkMode = { mode ->
                                    if (policy.has(AppPermission.PROFILE_APPEARANCE)) {
                                        viewModel.setDarkMode(mode)
                                    }
                                }
                            )

                            FarmerSection.FARMS -> FarmsScreen(
                                state = farmerState,
                                strings = strings,
                                currency = currentCurrency,
                                openCreateOnEntry = openCreateFarmOnEntry,
                                onCreateEntryConsumed = { openCreateFarmOnEntry = false },
                                onSelectFarm = viewModel::selectActiveFarm,
                                onSaveFarm = viewModel::saveFarm,
                                onDeleteFarm = viewModel::deleteFarm,
                                onOpenSubscription = openSubscriptionTab
                            )

                            FarmerSection.ONLINE_PRODUCTS -> OnlineProductsScreen(
                                state = farmerState,
                                strings = strings,
                                currency = currentCurrency,
                                openAddOnEntry = openAddOnlineProductOnEntry,
                                onAddEntryConsumed = { openAddOnlineProductOnEntry = false },
                                onSelectFarm = viewModel::selectActiveFarm,
                                onSaveOnlineProduct = viewModel::saveOnlineProduct,
                                onToggleLive = viewModel::toggleOnlineProductVisibility,
                                onDeleteProduct = viewModel::deleteOnlineProduct,
                                onOpenAddFarm = {
                                    openCreateFarmOnEntry = true
                                    section = FarmerSection.FARMS
                                },
                                onOpenOrders = {
                                    commerceInitialTab = CommerceTab.ORDERS
                                    section = FarmerSection.COMMERCE
                                },
                                onOpenSubscription = openSubscriptionTab
                            )

                            FarmerSection.LIVESTOCK -> LivestockScreen(
                                state = farmerState,
                                strings = strings,
                                currency = currentCurrency,
                                onSelectFarm = viewModel::selectActiveFarm,
                                onAddCategory = viewModel::addCustomAnimalCategory,
                                onSaveAnimal = viewModel::saveAnimalRecord,
                                onDeleteAnimal = viewModel::deleteAnimalRecord,
                                onRecordEvent = viewModel::recordAnimalEvent,
                                onOpenSubscription = openSubscriptionTab
                            )

                            FarmerSection.EGGS -> EggsScreen(
                                state = farmerState,
                                strings = strings,
                                currency = currentCurrency,
                                onSelectFarm = viewModel::selectActiveFarm,
                                onRecordEggs = viewModel::recordEggProduction,
                                onDeleteEgg = viewModel::deleteEggRecord,
                                onOpenSubscription = openSubscriptionTab
                            )

                            FarmerSection.OPERATIONS -> OperationsScreen(
                                state = farmerState,
                                strings = strings,
                                currency = currentCurrency,
                                initialTab = operationsInitialTab,
                                onSelectFarm = viewModel::selectActiveFarm,
                                onSaveInventory = viewModel::saveInventoryItem,
                                onAdjustInventory = viewModel::adjustInventoryStock,
                                onDeleteInventory = viewModel::deleteInventoryItem,
                                onAddExpense = viewModel::addExpense,
                                onDeleteExpense = viewModel::deleteExpense,
                                onSaveEmployee = viewModel::saveEmployee,
                                onDeleteEmployee = viewModel::deleteEmployee,
                                onMarkAttendance = viewModel::markAttendance,
                                onPaySalary = viewModel::paySalary,
                                onOpenPermissions = { section = FarmerSection.PERMISSIONS },
                                onOpenSubscription = openSubscriptionTab
                            )

                            FarmerSection.COMMERCE -> CommerceScreen(
                                state = farmerState,
                                strings = strings,
                                currency = currentCurrency,
                                initialTab = commerceInitialTab,
                                onSelectFarm = viewModel::selectActiveFarm,
                                onCreateSale = viewModel::createSale,
                                onSettleDue = viewModel::settleSaleDue,
                                onDeleteSale = viewModel::deleteSale,
                                onSaveCustomer = viewModel::saveCustomer,
                                onDeleteCustomer = viewModel::deleteCustomer,
                                onUpdateOrderStatus = viewModel::updateOrderStatus,
                                onMessage = viewModel::showMessage,
                                onOpenSubscription = openSubscriptionTab
                            )

                            FarmerSection.GUIDE -> FarmGuideScreen(
                                onOpenAiAdvisor = openAiIfAllowed,
                                subscriptionPolicy = farmerState.subscriptionPolicy,
                                canSubscribe = policy.has(AppPermission.SUBSCRIPTION_MANAGE),
                                onOpenSubscription = openSubscriptionTab
                            )

                            FarmerSection.PERMISSIONS -> UserPermissionsScreen(
                                state = farmerState,
                                strings = strings,
                                currency = currentCurrency,
                                onSelectFarm = viewModel::selectActiveFarm,
                                onSetEmployeePermission = viewModel::setEmployeePermission,
                                onSetAllPermissionsForEmployee = viewModel::setAllPermissionsForEmployee,
                                onApplyPermissionPreset = viewModel::applyPermissionPreset,
                                onSetPermissionForMultipleEmployees = viewModel::setPermissionForMultipleEmployees,
                                onAddCustomPermissionOption = viewModel::addCustomPermissionOption,
                                onDeleteCustomPermissionOption = viewModel::deleteCustomPermissionOption,
                                onProvisionEmployeeLogin = viewModel::provisionEmployeeLogin,
                                onSaveEmployee = viewModel::saveEmployee,
                                onOpenSubscription = openSubscriptionTab
                            )

                            FarmerSection.MORE -> ReportsSubscriptionProfileScreen(
                                user = user,
                                state = farmerState,
                                plans = plans,
                                systemSettings = systemSettings,
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

    if (showAiAdvisor && policy.has(AppPermission.AI_ADVISOR) &&
        farmerState.subscriptionPolicy.canAccess(SubscriptionFeature.DASHBOARD_CHARTS_AI)
    ) {
        FarmAiAdvisorDialog(
            farmState = farmerState,
            currency = currentCurrency,
            language = currentLanguage,
            aiViewModel = aiViewModel,
            onDismiss = { showAiAdvisor = false }
        )
    }
}
