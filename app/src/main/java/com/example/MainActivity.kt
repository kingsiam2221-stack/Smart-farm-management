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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ai.FarmAiViewModel
import com.example.ai.configureAppCheck
import com.example.auth.authStateFlow
import com.example.auth.signOutGoogle
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
import com.example.ui.components.AppSettingsScreen
import com.example.ui.screens.admin.SuperAdminDashboardScreen
import com.example.ui.screens.auth.AuthScreen
import com.example.ui.screens.customer.CustomerPortalScreen
import com.example.ui.screens.farmer.*
import com.example.ui.state.CloudSyncViewModel
import com.example.ui.state.SmartFarmViewModel
import com.example.ui.theme.AppThemeMode
import com.example.ui.theme.SmartFarmTheme
import com.google.firebase.Firebase
import com.google.firebase.auth.auth

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
                            val context = LocalContext.current
                            val coroutineScope = rememberCoroutineScope()
                            val firebaseAuth = remember { runCatching { Firebase.auth }.getOrNull() }
                            val firebaseReady = remember {
                                runCatching { com.google.firebase.FirebaseApp.getApps(context).isNotEmpty() }.getOrDefault(false)
                            }
                            val firebaseUser by remember(firebaseAuth) {
                                firebaseAuth?.authStateFlow() ?: kotlinx.coroutines.flow.flowOf(null)
                            }.collectAsStateWithLifecycle(
                                initialValue = runCatching { firebaseAuth?.currentUser }.getOrNull()
                            )
                            val handleLogout: () -> Unit = {
                                signOutGoogle(context, coroutineScope) {
                                    viewModel.logout()
                                }
                            }
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
                                    onAdminLogin = viewModel::loginSuperAdmin,
                                    onRegister = viewModel::register,
                                    onResetPassword = viewModel::resetPassword,
                                    onGoogleSignInSuccess = { fbUser, role ->
                                        viewModel.onGoogleAuthenticated(
                                            displayName = fbUser.displayName.orEmpty(),
                                            email = fbUser.email.orEmpty(),
                                            preferredRole = role
                                        )
                                    },
                                    onGoogleSignInError = viewModel::setAuthErrorMessage,
                                    onSelectLanguage = viewModel::setLanguage,
                                    onSelectCurrency = viewModel::setCurrency,
                                    onSelectDarkMode = viewModel::setDarkMode
                                )
                            } else {
                                val fbUid = firebaseUser?.uid
                                if (firebaseReady && !fbUid.isNullOrBlank()) {
                                    key(fbUid) {
                                        CloudSyncSidecar(
                                            currentUserId = fbUid,
                                            activeUser = activeUser,
                                            languageCode = language.code,
                                            currencyCode = currency.code,
                                            viewModel = viewModel
                                        )
                                    }
                                }
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
                                            onDeleteUser = viewModel::adminDeleteUserCompletely,
                                            onDeleteShopProduct = viewModel::adminDeleteOnlineShopProduct,
                                            onDeleteProductAd = viewModel::adminDeleteProductAd,
                                            onChangeAdminCredentials = viewModel::adminChangeCredentials,
                                            onUpdateProfile = viewModel::updateProfile,
                                            onMessage = viewModel::showMessage,
                                            onLogout = handleLogout
                                        )
                                    }

                                    UserRole.CUSTOMER.name -> {
                                        val customerState by viewModel.customerState.collectAsStateWithLifecycle()
                                        CustomerPortalScreen(
                                            user = activeUser,
                                            state = customerState,
                                            systemSettings = settings,
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
                                            onSubmitSupport = viewModel::submitSupport,
                                            onSearchChange = viewModel::updateMarketplaceSearch,
                                            onSelectCategory = viewModel::selectMarketplaceCategory,
                                            onLoadMore = viewModel::loadMoreMarketplaceItems,
                                            onSelectLanguage = viewModel::setLanguage,
                                            onSelectCurrency = viewModel::setCurrency,
                                            onSelectDarkMode = viewModel::setDarkMode,
                                            onMessage = viewModel::showMessage,
                                            onLogout = handleLogout
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
                                            viewModel = viewModel,
                                            onLogout = handleLogout
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
    DASHBOARD, FARMS, AGRICULTURE, ONLINE_PRODUCTS, AI_AD_STUDIO, LIVESTOCK, EGGS, OPERATIONS, COMMERCE, GUIDE, PERMISSIONS, MORE, SETTINGS
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
    FarmerSection.AGRICULTURE ->
        policy.hasAny(AppPermission.AGRICULTURE_VIEW, AppPermission.AGRICULTURE_MANAGE, AppPermission.AGRICULTURE_DELETE)
    FarmerSection.ONLINE_PRODUCTS ->
        policy.hasAny(AppPermission.ONLINE_PRODUCTS_VIEW, AppPermission.ONLINE_PRODUCTS_MANAGE)
    FarmerSection.AI_AD_STUDIO ->
        policy.has(AppPermission.AI_AD_STUDIO)
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
        true // Houses reports, subscriptions, notifications, and overflow modules
    FarmerSection.SETTINGS ->
        true // Dedicated Settings destination (Profile, Language/Currency/Theme, Help & Developer Contact, Log Out)
}

@Composable
private fun CloudSyncSidecar(
    currentUserId: String,
    activeUser: com.example.data.local.UserEntity,
    languageCode: String,
    currencyCode: String,
    viewModel: SmartFarmViewModel
) {
    val cloudVm: CloudSyncViewModel = viewModel(
        key = currentUserId,
        factory = CloudSyncViewModel.factory(currentUserId)
    )

    val profileState by cloudVm.userProfileState.collectAsStateWithLifecycle()
    val farmsState by cloudVm.farmsState.collectAsStateWithLifecycle()
    val marketplaceState by cloudVm.publicMarketplaceState.collectAsStateWithLifecycle()
    val myProductsState by cloudVm.myMarketplaceState.collectAsStateWithLifecycle()
    val customerOrdersState by cloudVm.customerOrdersState.collectAsStateWithLifecycle()
    val farmerOrdersState by cloudVm.farmerOrdersState.collectAsStateWithLifecycle()
    val notificationsState by cloudVm.notificationsState.collectAsStateWithLifecycle()
    val agriLandsState by cloudVm.agriLandsState.collectAsStateWithLifecycle()
    val agriCropsState by cloudVm.agriCropsState.collectAsStateWithLifecycle()
    val agriExpensesState by cloudVm.agriExpensesState.collectAsStateWithLifecycle()
    val farmerState by viewModel.farmerState.collectAsStateWithLifecycle()

    LaunchedEffect(currentUserId, activeUser.id, languageCode, currencyCode) {
        val cloudRole = if (activeUser.role == UserRole.FARMER.name) "FARMER" else "CUSTOMER"
        cloudVm.syncUserProfile(
            displayName = activeUser.name,
            email = activeUser.email,
            mobile = activeUser.mobile,
            role = cloudRole,
            preferredLanguage = languageCode,
            preferredCurrency = currencyCode
        )
    }

    LaunchedEffect(currentUserId, activeUser.role, farmerState.farms) {
        if (activeUser.role == UserRole.FARMER.name) {
            farmerState.farms.forEach { farm ->
                cloudVm.syncFarm(
                    farmDocId = "farm_${farm.id}",
                    farmCode = farm.farmCode,
                    name = farm.name,
                    location = farm.location,
                    address = farm.address,
                    category = farm.category,
                    managerName = farm.managerName,
                    managerPhone = farm.managerPhone,
                    initialInvestment = farm.initialInvestment
                )
            }
        }
    }

    LaunchedEffect(currentUserId, activeUser.role, farmerState.inventory, farmerState.farms) {
        if (activeUser.role == UserRole.FARMER.name) {
            val farmsById = farmerState.farms.associateBy { it.id }
            farmerState.inventory.filter { it.listInMarketplace && it.sellingPrice > 0.0 }.forEach { item ->
                val farm = farmsById[item.farmId]
                cloudVm.syncMarketplaceProduct(
                    productDocId = "prod_${item.id}",
                    farmId = "farm_${item.farmId}",
                    farmName = farm?.name ?: "Smart Farm",
                    farmLocation = farm?.location ?: "",
                    name = item.name,
                    category = item.category,
                    sourcingType = item.sourcingType,
                    quantity = item.quantity,
                    unit = item.unit,
                    unitCost = item.unitCost,
                    sellingPrice = item.sellingPrice,
                    listed = item.listInMarketplace,
                    description = item.description
                )
            }
        }
    }

    LaunchedEffect(currentUserId, activeUser.role, farmerState.agriLands) {
        if (activeUser.role == UserRole.FARMER.name) {
            farmerState.agriLands.forEach { land ->
                cloudVm.syncAgriLand(
                    landDocId = "land_${land.id}",
                    farmId = land.farmId?.let { "farm_$it" },
                    name = land.name,
                    areaSize = land.areaSize,
                    areaUnit = land.areaUnit,
                    location = land.location,
                    soilOrLandType = land.soilOrLandType,
                    notes = land.notes
                )
            }
        }
    }

    LaunchedEffect(currentUserId, activeUser.role, farmerState.agriCrops) {
        if (activeUser.role == UserRole.FARMER.name) {
            farmerState.agriCrops.forEach { crop ->
                cloudVm.syncAgriCrop(
                    cropDocId = "crop_${crop.id}",
                    farmId = crop.farmId?.let { "farm_$it" },
                    landId = "land_${crop.landId}",
                    cropType = crop.cropType,
                    title = crop.title,
                    variety = crop.variety,
                    season = crop.season,
                    status = crop.status,
                    yieldQuantity = crop.yieldQuantity,
                    yieldUnit = crop.yieldUnit,
                    marketPricePerUnit = crop.marketPricePerUnit,
                    yieldMarketWorth = crop.yieldMarketWorth,
                    soldAmount = crop.soldAmount,
                    notes = crop.notes
                )
            }
        }
    }

    LaunchedEffect(currentUserId, activeUser.role, farmerState.agriExpenses) {
        if (activeUser.role == UserRole.FARMER.name) {
            farmerState.agriExpenses.forEach { exp ->
                cloudVm.syncAgriExpense(
                    expenseDocId = "agri_exp_${exp.id}",
                    farmId = exp.farmId?.let { "farm_$it" },
                    landId = "land_${exp.landId}",
                    cropId = exp.cropId?.let { "crop_$it" },
                    sector = exp.sector,
                    customSectorName = exp.customSectorName,
                    amount = exp.amount,
                    description = exp.description
                )
            }
        }
    }

    LaunchedEffect(currentUserId, activeUser.role, farmerState.productAds) {
        if (activeUser.role == UserRole.FARMER.name) {
            farmerState.productAds.forEach { ad ->
                cloudVm.syncProductAd(
                    adDocId = "ad_${ad.id}",
                    productDocId = ad.inventoryItemId?.let { "prod_$it" },
                    productName = ad.productName,
                    farmName = ad.farmName,
                    priceText = ad.priceText,
                    hookHeadlineBn = ad.hookHeadlineBn,
                    promoBadgeBn = ad.promoBadgeBn,
                    benefitsPipeSeparatedBn = ad.benefitsPipeSeparatedBn,
                    callToActionBn = ad.callToActionBn,
                    voiceoverScriptBn = ad.voiceoverScriptBn,
                    orderStepsPipeSeparatedBn = ad.orderStepsPipeSeparatedBn,
                    accentColorHex = ad.accentColorHex,
                    active = ad.isActiveInShop
                )
            }
        }
    }

    // Keep state flows active without unused warnings
    profileState
    farmsState
    marketplaceState
    myProductsState
    customerOrdersState
    farmerOrdersState
    notificationsState
    agriLandsState
    agriCropsState
    agriExpensesState
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
    viewModel: SmartFarmViewModel,
    onLogout: () -> Unit
) {
    val policy = farmerState.accessPolicy
    val allEntries = remember(strings) {
        listOf(
            NavEntry(FarmerSection.DASHBOARD, strings.dashboard, Icons.Default.Dashboard),
            NavEntry(FarmerSection.FARMS, strings.farms, Icons.Default.Agriculture),
            NavEntry(FarmerSection.AGRICULTURE, strings.agriculture, Icons.Default.Grass),
            NavEntry(FarmerSection.ONLINE_PRODUCTS, strings.onlineProducts, Icons.Default.Storefront),
            NavEntry(FarmerSection.AI_AD_STUDIO, strings.aiAdStudio, Icons.Default.MovieCreation),
            NavEntry(FarmerSection.LIVESTOCK, strings.livestock, Icons.Default.Pets),
            NavEntry(FarmerSection.EGGS, strings.eggs, Icons.Default.Egg),
            NavEntry(FarmerSection.OPERATIONS, strings.operations, Icons.Default.Inventory2),
            NavEntry(FarmerSection.COMMERCE, strings.commerce, Icons.Default.PointOfSale),
            NavEntry(FarmerSection.GUIDE, strings.farmGuide, Icons.AutoMirrored.Filled.MenuBook),
            NavEntry(FarmerSection.PERMISSIONS, strings.permissions, Icons.Default.AdminPanelSettings),
            NavEntry(FarmerSection.MORE, strings.more, Icons.Default.Tune),
            NavEntry(FarmerSection.SETTINGS, strings.settings, Icons.Default.Settings)
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
    var preselectedAdProductId by remember { mutableStateOf<Long?>(null) }
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
            if (permittedEntries.size <= 5) {
                permittedEntries
            } else {
                // Keep up to 3 primary sections + dedicated More + dedicated Settings (max 5 items)
                val nonMoreOrSettings = permittedEntries.filter {
                    it.section != FarmerSection.MORE && it.section != FarmerSection.SETTINGS
                }
                val primary = nonMoreOrSettings.take(3)
                val moreEntry = permittedEntries.firstOrNull { it.section == FarmerSection.MORE }
                val settingsEntry = permittedEntries.firstOrNull { it.section == FarmerSection.SETTINGS }
                primary + listOfNotNull(moreEntry, settingsEntry)
            }
        }
        val mobileBarSections = remember(mobileBarEntries) { mobileBarEntries.map { it.section }.toSet() }
        val overflowMobileEntries = remember(permittedEntries, mobileBarSections) {
            permittedEntries.filter {
                it.section != FarmerSection.MORE &&
                    it.section != FarmerSection.SETTINGS &&
                    it.section !in mobileBarSections
            }
        }

        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            bottomBar = {
                if (!isWide) {
                    NavigationBar {
                        mobileBarEntries.forEach { entry ->
                            val isSelected = section == entry.section ||
                                (entry.section == FarmerSection.MORE &&
                                    section !in mobileBarSections &&
                                    section != FarmerSection.SETTINGS)
                            NavigationBarItem(
                                selected = isSelected,
                                onClick = {
                                    if (entry.section == FarmerSection.MORE) moreInitialTab = MoreTab.REPORTS
                                    if (entry.section == FarmerSection.OPERATIONS) operationsInitialTab = OperationsTab.INVENTORY
                                    if (entry.section == FarmerSection.COMMERCE) commerceInitialTab = CommerceTab.SALES
                                    section = entry.section
                                },
                                modifier = if (entry.section == FarmerSection.SETTINGS) Modifier.testTag("nav_settings") else Modifier,
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
                                modifier = if (entry.section == FarmerSection.SETTINGS) Modifier.testTag("nav_settings") else Modifier,
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
                                modifier = Modifier.testTag("farmer_overflow_chips_row"),
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
                                                FarmerSection.AGRICULTURE -> Modifier.testTag("mobile_agriculture_chip")
                                                FarmerSection.AI_AD_STUDIO -> Modifier.testTag("mobile_ai_ad_studio_chip")
                                                FarmerSection.GUIDE -> Modifier.testTag("mobile_guide_chip")
                                                FarmerSection.PERMISSIONS -> Modifier.testTag("mobile_permissions_chip")
                                                FarmerSection.ONLINE_PRODUCTS -> Modifier.testTag("mobile_online_products_chip")
                                                else -> Modifier.testTag("mobile_${entry.section.name.lowercase()}_chip")
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
                                                else strings.more
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
                                onOpenAgriculture = { section = FarmerSection.AGRICULTURE },
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
                                onOpenAiAdStudio = { section = FarmerSection.AI_AD_STUDIO },
                                onOpenSettings = { section = FarmerSection.SETTINGS }
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

                            FarmerSection.AGRICULTURE -> AgricultureScreen(
                                state = farmerState,
                                strings = strings,
                                currency = currentCurrency,
                                onSelectFarm = viewModel::selectActiveFarm,
                                onSaveLand = viewModel::saveAgriLand,
                                onDeleteLand = viewModel::deleteAgriLand,
                                onSaveCrop = viewModel::saveAgriCrop,
                                onRecordHarvest = viewModel::recordAgriHarvest,
                                onDeleteCrop = viewModel::deleteAgriCrop,
                                onAddAgriExpense = viewModel::addAgriExpense,
                                onDeleteAgriExpense = viewModel::deleteAgriExpense,
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
                                onOpenSubscription = openSubscriptionTab,
                                onCreateAdForProduct = { item ->
                                    preselectedAdProductId = item.id
                                    section = FarmerSection.AI_AD_STUDIO
                                }
                            )

                            FarmerSection.AI_AD_STUDIO -> AiProductAdStudioScreen(
                                state = farmerState,
                                strings = strings,
                                currency = currentCurrency,
                                preselectedProductId = preselectedAdProductId,
                                onPreselectConsumed = { preselectedAdProductId = null },
                                onSelectFarm = viewModel::selectActiveFarm,
                                onSaveProductAd = { ad -> viewModel.saveProductAd(ad) },
                                onToggleAdActive = viewModel::toggleProductAdActive,
                                onDeleteAd = viewModel::deleteProductAd,
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
                                state = farmerState,
                                plans = plans,
                                systemSettings = systemSettings,
                                notifications = notifications,
                                strings = strings,
                                currentCurrency = currentCurrency,
                                initialTab = moreInitialTab,
                                onSubscribe = viewModel::subscribe,
                                onMarkNotificationsRead = viewModel::markNotificationsRead,
                                onSubmitSupport = viewModel::submitSupport,
                                onOpenSettings = { section = FarmerSection.SETTINGS },
                                onMessage = viewModel::showMessage
                            )

                            FarmerSection.SETTINGS -> AppSettingsScreen(
                                user = user,
                                systemSettings = systemSettings,
                                strings = strings,
                                currentLanguage = currentLanguage,
                                currentCurrency = currentCurrency,
                                darkModeOverride = darkModeOverride,
                                canEditAppearance = policy.has(AppPermission.PROFILE_APPEARANCE),
                                showLabelCustomization = policy.has(AppPermission.PROFILE_APPEARANCE),
                                languageOverrides = languageOverrides,
                                supportRequests = farmerState.supportRequests,
                                employeeEmptyPermissionsHint = !policy.isAdminOwner && policy.grantedKeys.isEmpty(),
                                onSelectLanguage = viewModel::setLanguage,
                                onSelectCurrency = viewModel::setCurrency,
                                onSelectDarkMode = viewModel::setDarkMode,
                                onSetLanguageOverride = viewModel::setLanguageOverride,
                                onUpdateProfile = viewModel::updateProfile,
                                onSubmitSupport = viewModel::submitSupport,
                                onMessage = viewModel::showMessage,
                                onLogout = onLogout
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
