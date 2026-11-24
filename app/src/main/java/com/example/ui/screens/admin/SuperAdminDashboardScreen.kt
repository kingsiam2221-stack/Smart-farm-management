package com.example.ui.screens.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.local.*
import com.example.i18n.AppLanguage
import com.example.i18n.AppStrings
import com.example.i18n.CurrencyFormatter
import com.example.i18n.CurrencyOption
import com.example.i18n.DomainLocalization
import com.example.i18n.isBangla
import com.example.i18n.tr
import com.example.ui.components.*
import com.example.ui.screens.farmer.AdminPermissionsManagementPanel
import com.example.ui.state.SuperAdminState

@Composable
fun SuperAdminDashboardScreen(
    adminUser: UserEntity,
    state: SuperAdminState,
    plans: List<SubscriptionPlanEntity>,
    systemSettings: SystemSettingsEntity,
    strings: AppStrings,
    currentLanguage: AppLanguage,
    currentCurrency: CurrencyOption,
    darkModeOverride: Boolean?,
    onSetUserStatus: (UserEntity, AccountStatus) -> Unit,
    onSavePlan: (SubscriptionPlanEntity) -> Unit,
    onDeletePlan: (SubscriptionPlanEntity) -> Unit,
    onUpdatePayment: (UserSubscriptionEntity, PaymentStatus, String) -> Unit,
    onGrantPlan: (UserEntity, SubscriptionPlanEntity) -> Unit,
    onBroadcast: (String, String) -> Unit,
    onSaveSettings: (SystemSettingsEntity) -> Unit,
    onToggleFeatureSubscription: (SubscriptionFeature, Boolean) -> Unit = { _, _ -> },
    onSetAllFeatureSubscriptions: (Set<String>) -> Unit = {},
    onResolveSupport: (SupportRequestEntity, String) -> Unit,
    onSetEmployeePermission: (EmployeeEntity, String, Boolean) -> Unit,
    onSetAllPermissionsForEmployee: (EmployeeEntity, Boolean) -> Unit,
    onApplyPermissionPreset: (EmployeeEntity, PermissionPreset) -> Unit,
    onSetPermissionForMultipleEmployees: (Long, List<EmployeeEntity>, String, Boolean) -> Unit,
    onAddCustomPermissionOption: (Long, String, String, String, String) -> Unit,
    onDeleteCustomPermissionOption: (CustomPermissionOptionEntity) -> Unit,
    onProvisionEmployeeLogin: (EmployeeEntity, String, String, String, () -> Unit) -> Unit,
    onSelectLanguage: (AppLanguage) -> Unit,
    onSelectCurrency: (CurrencyOption) -> Unit,
    onSelectDarkMode: (Boolean?) -> Unit,
    onLogout: () -> Unit
) {
    // Hard UI-level RBAC guard matching Repository-level RBAC guard
    if (adminUser.role != UserRole.SUPER_ADMIN.name) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(Icons.Default.GppBad, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(56.dp))
                Text(tr("403 • Unauthorized Access", "৪০৩ • অননুমোদিত প্রবেশ"), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Button(onClick = onLogout) { Text(tr("Return to Sign In", "লগইন পাতায় ফিরুন")) }
            }
        }
        return
    }

    var selectedTab by remember { mutableIntStateOf(0) }
    var showSettingsModal by remember { mutableStateOf(false) }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isWide = maxWidth >= 600.dp
        val navItems = listOf(
            strings.adminOverview to Icons.Default.Dashboard,
            strings.adminUsers to Icons.Default.ManageAccounts,
            strings.permissions to Icons.Default.AdminPanelSettings,
            strings.adminPlans to Icons.Default.CardMembership,
            strings.adminFinance to Icons.Default.AccountBalance,
            strings.adminSystem to Icons.Default.SettingsApplications
        )

        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            topBar = {
                @OptIn(ExperimentalMaterial3Api::class)
                TopAppBar(
                    title = {
                        Column {
                            Text(strings.superAdminPortal, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text("${tr("Private Developer Console", "প্রাইভেট ডেভেলপার কনসোল")} • ${adminUser.email}", style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    },
                    actions = {
                        TextButton(onClick = {
                            onSelectLanguage(if (currentLanguage == AppLanguage.BN) AppLanguage.EN else AppLanguage.BN)
                        }) {
                            Text(
                                if (currentLanguage == AppLanguage.BN) "EN" else "বাংলা",
                                fontWeight = FontWeight.Bold
                            )
                        }
                        IconButton(
                            onClick = { onSelectDarkMode(nextThemeModeOverride(darkModeOverride)) },
                            modifier = Modifier.testTag("admin_theme_toggle")
                        ) {
                            Icon(themeModeIcon(darkModeOverride), contentDescription = strings.themeMode)
                        }
                        IconButton(onClick = { showSettingsModal = true }) {
                            Icon(Icons.Default.Language, contentDescription = tr("Language & Currency", "ভাষা ও মুদ্রা"))
                        }
                        IconButton(onClick = onLogout) {
                            Icon(Icons.Default.Logout, contentDescription = strings.logout, tint = MaterialTheme.colorScheme.error)
                        }
                    }
                )
            },
            bottomBar = {
                if (!isWide) {
                    NavigationBar {
                        navItems.forEachIndexed { idx, (label, icon) ->
                            NavigationBarItem(
                                selected = selectedTab == idx,
                                onClick = { selectedTab = idx },
                                icon = { Icon(icon, contentDescription = label) },
                                label = { Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                            )
                        }
                    }
                }
            }
        ) { padding ->
            Row(modifier = Modifier.fillMaxSize().padding(padding)) {
                if (isWide) {
                    NavigationRail {
                        navItems.forEachIndexed { idx, (label, icon) ->
                            NavigationRailItem(
                                selected = selectedTab == idx,
                                onClick = { selectedTab = idx },
                                icon = { Icon(icon, contentDescription = label) },
                                label = { Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                            )
                        }
                    }
                }
                Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    when (selectedTab) {
                        0 -> AdminOverviewTab(state, currentCurrency)
                        1 -> AdminUsersTab(state, plans, onSetUserStatus, onGrantPlan)
                        2 -> AdminPermissionsTab(
                            state = state,
                            strings = strings,
                            currency = currentCurrency,
                            onSetEmployeePermission = onSetEmployeePermission,
                            onSetAllPermissionsForEmployee = onSetAllPermissionsForEmployee,
                            onApplyPermissionPreset = onApplyPermissionPreset,
                            onSetPermissionForMultipleEmployees = onSetPermissionForMultipleEmployees,
                            onAddCustomPermissionOption = onAddCustomPermissionOption,
                            onDeleteCustomPermissionOption = onDeleteCustomPermissionOption,
                            onProvisionEmployeeLogin = onProvisionEmployeeLogin
                        )
                        3 -> AdminPlansTab(
                            plans = plans,
                            subs = state.subscriptions,
                            systemSettings = systemSettings,
                            currency = currentCurrency,
                            onSavePlan = onSavePlan,
                            onDeletePlan = onDeletePlan,
                            onUpdatePayment = onUpdatePayment,
                            onSaveSettings = onSaveSettings,
                            onToggleFeatureSubscription = onToggleFeatureSubscription,
                            onSetAllFeatureSubscriptions = onSetAllFeatureSubscriptions
                        )
                        4 -> AdminFinanceTab(state, currentCurrency, onUpdatePayment)
                        else -> AdminSystemTab(state, systemSettings, onBroadcast, onSaveSettings, onResolveSupport)
                    }
                }
            }
        }
    }

    if (showSettingsModal) {
        LanguageCurrencyDialog(
            currentLanguage = currentLanguage,
            currentCurrency = currentCurrency,
            darkModeOverride = darkModeOverride,
            onSelectLanguage = onSelectLanguage,
            onSelectCurrency = onSelectCurrency,
            onSelectDarkMode = onSelectDarkMode,
            onDismiss = { showSettingsModal = false }
        )
    }
}

@Composable
private fun AdminPermissionsTab(
    state: SuperAdminState,
    strings: AppStrings,
    currency: CurrencyOption,
    onSetEmployeePermission: (EmployeeEntity, String, Boolean) -> Unit,
    onSetAllPermissionsForEmployee: (EmployeeEntity, Boolean) -> Unit,
    onApplyPermissionPreset: (EmployeeEntity, PermissionPreset) -> Unit,
    onSetPermissionForMultipleEmployees: (Long, List<EmployeeEntity>, String, Boolean) -> Unit,
    onAddCustomPermissionOption: (Long, String, String, String, String) -> Unit,
    onDeleteCustomPermissionOption: (CustomPermissionOptionEntity) -> Unit,
    onProvisionEmployeeLogin: (EmployeeEntity, String, String, String, () -> Unit) -> Unit
) {
    val farmerOwners = remember(state.users) {
        state.users.filter { it.role == UserRole.FARMER.name }
    }
    var selectedOwnerId by remember(farmerOwners) {
        mutableStateOf(
            farmerOwners.firstOrNull { owner ->
                state.employees.any { it.ownerUserId == owner.id }
            }?.id ?: farmerOwners.firstOrNull()?.id ?: 0L
        )
    }
    var selectedFarmId by remember(selectedOwnerId) { mutableStateOf<Long?>(null) }

    val ownerFarms = remember(state.farms, selectedOwnerId) {
        state.farms.filter { it.ownerUserId == selectedOwnerId }
    }
    val ownerEmployees = remember(state.employees, selectedOwnerId) {
        state.employees.filter { it.ownerUserId == selectedOwnerId }
    }
    val ownerPerms = remember(state.employeePermissions, selectedOwnerId) {
        state.employeePermissions.filter { it.ownerUserId == selectedOwnerId }
    }
    val ownerCustomOpts = remember(state.customPermissionOptions, selectedOwnerId) {
        state.customPermissionOptions.filter { it.ownerUserId == selectedOwnerId }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        if (farmerOwners.size > 1) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(farmerOwners, key = { "owner_${it.id}" }) { owner ->
                    val empCount = state.employees.count { it.ownerUserId == owner.id }
                    FilterChip(
                        selected = selectedOwnerId == owner.id,
                        onClick = { selectedOwnerId = owner.id },
                        label = { Text("${owner.name} ($empCount)") }
                    )
                }
            }
        }
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            AdminPermissionsManagementPanel(
                isAdminAuthorized = true,
                ownerUserId = selectedOwnerId,
                farms = ownerFarms,
                selectedFarmId = selectedFarmId,
                employees = ownerEmployees,
                employeePermissions = ownerPerms,
                customPermissionOptions = ownerCustomOpts,
                strings = strings,
                currency = currency,
                onSelectFarm = { selectedFarmId = it },
                onSetEmployeePermission = onSetEmployeePermission,
                onSetAllPermissionsForEmployee = onSetAllPermissionsForEmployee,
                onApplyPermissionPreset = onApplyPermissionPreset,
                onSetPermissionForMultipleEmployees = onSetPermissionForMultipleEmployees,
                onAddCustomPermissionOption = onAddCustomPermissionOption,
                onDeleteCustomPermissionOption = onDeleteCustomPermissionOption,
                onProvisionEmployeeLogin = onProvisionEmployeeLogin,
                onSaveEmployee = null
            )
        }
    }
}

@Composable
private fun AdminOverviewTab(state: SuperAdminState, currency: CurrencyOption) {
    val now = System.currentTimeMillis()
    val monthAgo = now - 30 * 86_400_000L
    val yearAgo = now - 365 * 86_400_000L

    val activeUsers = state.users.count { it.status == AccountStatus.ACTIVE.name }
    val inactiveUsers = state.users.size - activeUsers
    val activeSubs = state.subscriptions.count { it.status == "ACTIVE" }
    val expiredSubs = state.subscriptions.count { it.status == "EXPIRED" }

    val successfulSubs = state.subscriptions.filter { it.paymentStatus == PaymentStatus.SUCCESSFUL.name }
    val monthlySubRev = successfulSubs.filter { it.startDate >= monthAgo }.sumOf { it.pricePaidBdt }
    val yearlySubRev = successfulSubs.filter { it.startDate >= yearAgo }.sumOf { it.pricePaidBdt }
    val lifetimeSubRev = successfulSubs.sumOf { it.pricePaidBdt }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item(key = "ov_kpis1") {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                KpiMetricCard(
                    tr("Total Users", "মোট ব্যবহারকারী"),
                    "${state.users.size}",
                    Icons.Default.People,
                    subtitle = "$activeUsers ${tr("Active", "সক্রিয়")} • $inactiveUsers ${tr("Inactive", "নিষ্ক্রিয়")}",
                    modifier = Modifier.weight(1f)
                )
                KpiMetricCard(
                    tr("Total Farms", "মোট খামার"),
                    "${state.farms.size}",
                    Icons.Default.Agriculture,
                    subtitle = "${state.orders.size} ${tr("Platform Orders", "প্ল্যাটফর্ম অর্ডার")}",
                    modifier = Modifier.weight(1f)
                )
            }
        }
        item(key = "ov_kpis2") {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                KpiMetricCard(
                    tr("Subscriptions", "সাবস্ক্রিপশন"),
                    "${state.subscriptions.size}",
                    Icons.Default.CardMembership,
                    subtitle = "$activeSubs ${tr("Active", "সক্রিয়")} • $expiredSubs ${tr("Expired", "মেয়াদোত্তীর্ণ")}",
                    modifier = Modifier.weight(1f)
                )
                KpiMetricCard(
                    tr("Lifetime Revenue", "সর্বমোট আয়"),
                    CurrencyFormatter.format(lifetimeSubRev, currency),
                    Icons.Default.Paid,
                    subtitle = "${tr("Month", "মাস")}: ${CurrencyFormatter.format(monthlySubRev, currency)}",
                    modifier = Modifier.weight(1f)
                )
            }
        }
        item(key = "ov_kpis3") {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                KpiMetricCard(tr("Monthly Sub Rev", "মাসিক সাবস্ক্রিপশন আয়"), CurrencyFormatter.format(monthlySubRev, currency), Icons.Default.CalendarMonth, modifier = Modifier.weight(1f))
                KpiMetricCard(tr("Yearly Sub Rev", "বাৎসরিক সাবস্ক্রিপশন আয়"), CurrencyFormatter.format(yearlySubRev, currency), Icons.Default.EventRepeat, modifier = Modifier.weight(1f))
            }
        }
        item(key = "ov_chart") {
            val chartPoints = listOf(
                tr("30d Rev", "৩০ দিনের আয়") to CurrencyFormatter.convertFromBase(monthlySubRev, currency),
                tr("1y Rev", "১ বছরের আয়") to CurrencyFormatter.convertFromBase(yearlySubRev, currency),
                tr("Total Rev", "মোট আয়") to CurrencyFormatter.convertFromBase(lifetimeSubRev, currency)
            )
            SmoothAreaLineChart(title = "${tr("Subscription Revenue Growth", "সাবস্ক্রিপশন আয় বৃদ্ধি")} (${currency.symbol})", points = chartPoints)
        }
    }
}

@Composable
private fun AdminUsersTab(
    state: SuperAdminState,
    plans: List<SubscriptionPlanEntity>,
    onSetUserStatus: (UserEntity, AccountStatus) -> Unit,
    onGrantPlan: (UserEntity, SubscriptionPlanEntity) -> Unit
) {
    val isBn = isBangla()
    var search by remember { mutableStateOf("") }
    var grantTarget by remember { mutableStateOf<UserEntity?>(null) }
    val filtered = remember(state.users, search) {
        state.users.filter {
            it.name.contains(search, true) || it.email.contains(search, true) || it.mobile.contains(search, true)
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item(key = "users_search") {
            OutlinedTextField(
                value = search,
                onValueChange = { search = it },
                placeholder = { Text(tr("Search users by name, email, mobile...", "নাম, ইমেইল বা মোবাইল দিয়ে খুঁজুন...")) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }
        items(filtered, key = { "user_${it.id}" }) { u ->
            val userFarms = state.farms.count { it.ownerUserId == u.id }
            val userSub = state.subscriptions.firstOrNull { it.userId == u.id && it.status == "ACTIVE" }
            Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surface, tonalElevation = 2.dp, modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("${u.name} (${DomainLocalization.userRole(u.role, isBn)})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text("${u.email} • ${u.mobile}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        AssistChip(onClick = {}, label = { Text(DomainLocalization.accountStatus(u.status, isBn)) })
                    }
                    Text(
                        "${tr("Farms", "খামার")}: $userFarms • ${tr("Subscription", "সাবস্ক্রিপশন")}: ${userSub?.planName ?: tr("None", "নেই")} • ${tr("Joined", "যোগদান")} ${formatShortDate(u.createdAt)}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    if (u.role != UserRole.SUPER_ADMIN.name) {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            item {
                                FilledTonalButton(onClick = { onSetUserStatus(u, AccountStatus.ACTIVE) }) { Text(tr("Activate", "সক্রিয় করুন")) }
                            }
                            item {
                                OutlinedButton(onClick = { onSetUserStatus(u, AccountStatus.INACTIVE) }) { Text(tr("Deactivate", "নিষ্ক্রিয়")) }
                            }
                            item {
                                OutlinedButton(onClick = { onSetUserStatus(u, AccountStatus.SUSPENDED) }) { Text(tr("Suspend", "স্থগিত")) }
                            }
                            item {
                                Button(onClick = { grantTarget = u }) { Text(tr("Assign Plan", "প্ল্যান দিন")) }
                            }
                        }
                    }
                }
            }
        }
    }

    grantTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { grantTarget = null },
            title = { Text("${tr("Assign Subscription to", "সাবস্ক্রিপশন দিন:")} ${target.name}") },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    plans.forEach { p ->
                        FilledTonalButton(onClick = { onGrantPlan(target, p); grantTarget = null }, modifier = Modifier.fillMaxWidth()) {
                            Text("${p.name} (${p.durationDays} ${tr("days", "দিন")})")
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { grantTarget = null }) { Text(tr("Cancel", "বাতিল")) } }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AdminPlansTab(
    plans: List<SubscriptionPlanEntity>,
    subs: List<UserSubscriptionEntity>,
    systemSettings: SystemSettingsEntity,
    currency: CurrencyOption,
    onSavePlan: (SubscriptionPlanEntity) -> Unit,
    onDeletePlan: (SubscriptionPlanEntity) -> Unit,
    onUpdatePayment: (UserSubscriptionEntity, PaymentStatus, String) -> Unit,
    onSaveSettings: (SystemSettingsEntity) -> Unit,
    onToggleFeatureSubscription: (SubscriptionFeature, Boolean) -> Unit,
    onSetAllFeatureSubscriptions: (Set<String>) -> Unit
) {
    val isBn = isBangla()
    var subTab by remember { mutableIntStateOf(0) } // 0 = Plans, 1 = Free vs Paid, 2 = Payment Accounts
    var editingPlan by remember { mutableStateOf<SubscriptionPlanEntity?>(null) }
    var showCreate by remember { mutableStateOf(false) }
    val pendingSubs = subs.filter { it.paymentStatus == PaymentStatus.PENDING.name || it.status == "PENDING" }
    val lockedKeys = remember(systemSettings.subscriptionLockedFeaturesCsv) { systemSettings.parseLockedFeatureKeys() }

    Scaffold(
        floatingActionButton = {
            if (subTab == 0) {
                ExtendedFloatingActionButton(
                    onClick = { showCreate = true },
                    icon = { Icon(Icons.Default.Add, contentDescription = tr("Add Plan", "প্ল্যান যোগ করুন")) },
                    text = { Text(tr("Create Plan", "নতুন প্ল্যান")) }
                )
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            TabRow(selectedTabIndex = subTab) {
                Tab(
                    selected = subTab == 0,
                    onClick = { subTab = 0 },
                    text = { Text(tr("Plans (${plans.size})", "প্ল্যান (${plans.size})"), maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    modifier = Modifier.testTag("admin_subtab_plans")
                )
                Tab(
                    selected = subTab == 1,
                    onClick = { subTab = 1 },
                    text = { Text(tr("Free vs Paid", "ফ্রি / পেইড"), maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    modifier = Modifier.testTag("admin_subtab_features")
                )
                Tab(
                    selected = subTab == 2,
                    onClick = { subTab = 2 },
                    text = { Text(tr("Payment Accounts", "পেমেন্ট নম্বর"), maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    modifier = Modifier.testTag("admin_subtab_payments")
                )
            }

            when (subTab) {
                0 -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 88.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        item(key = "plans_title") {
                            Text(tr("Subscription Plans (Live Editable)", "সাবস্ক্রিপশন প্ল্যানসমূহ"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                        items(plans, key = { "plan_${it.id}" }) { plan ->
                            val stateLabel = if (plan.active) tr("Active", "সক্রিয়") else tr("Hidden", "লুকানো")
                            Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surface, tonalElevation = 2.dp, modifier = Modifier.fillMaxWidth()) {
                                Row(modifier = Modifier.padding(14.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("${plan.name} ($stateLabel)", fontWeight = FontWeight.Bold)
                                        Text("${CurrencyFormatter.format(plan.priceBaseBdt, currency)} • ${plan.durationDays} ${tr("days", "দিন")}", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                                        Text(plan.description, style = MaterialTheme.typography.labelSmall)
                                    }
                                    Row {
                                        IconButton(onClick = { editingPlan = plan }) { Icon(Icons.Default.Edit, contentDescription = tr("Edit Plan", "প্ল্যান সম্পাদনা")) }
                                        IconButton(onClick = { onDeletePlan(plan) }) { Icon(Icons.Default.DeleteOutline, contentDescription = tr("Delete Plan", "প্ল্যান মুছুন"), tint = MaterialTheme.colorScheme.error) }
                                    }
                                }
                            }
                        }
                        if (pendingSubs.isNotEmpty()) {
                            item(key = "pending_subs_header") {
                                Spacer(Modifier.height(6.dp))
                                Text("${tr("Manual Payment Verification Queue", "ম্যানুয়াল পেমেন্ট যাচাইকরণ তালিকা")} (${pendingSubs.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            }
                            items(pendingSubs, key = { "pending_sub_${it.id}" }) { sub ->
                                Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.tertiaryContainer, modifier = Modifier.fillMaxWidth()) {
                                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Text("${sub.userName} • ${sub.planName} (${CurrencyFormatter.format(sub.pricePaidBdt, currency)})", fontWeight = FontWeight.Bold)
                                        Text("${DomainLocalization.paymentMethod(sub.paymentMethod, isBn)} • ${tr("Ref", "রেফ")}: ${sub.transactionRef}", style = MaterialTheme.typography.labelSmall)
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Button(onClick = { onUpdatePayment(sub, PaymentStatus.SUCCESSFUL, "ACTIVE") }) { Text(tr("Approve", "অনুমোদন")) }
                                            OutlinedButton(onClick = { onUpdatePayment(sub, PaymentStatus.FAILED, "EXPIRED") }) { Text(tr("Reject", "বাতিল")) }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                1 -> {
                    val paidCount = SubscriptionFeature.entries.count { lockedKeys.contains(it.key) }
                    val freeCount = SubscriptionFeature.entries.size - paidCount
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        item(key = "feat_summary_card") {
                            Surface(
                                shape = MaterialTheme.shapes.medium,
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(
                                        tr("Developer Control: Free vs Subscription Modules", "ডেভেলপার নিয়ন্ত্রণ: ফ্রি বনাম সাবস্ক্রিপশন অপশন"),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                    Text(
                                        tr(
                                            "Toggle each option below. Users clearly see which options are FREE and which require an active Subscription ($freeCount Free • $paidCount Subscription).",
                                            "নিচের প্রতিটি অপশন নিয়ন্ত্রণ করুন। ব্যবহারকারীরা অ্যাপে স্পষ্ট দেখতে পাবে কোন অপশন ফ্রি আর কোনটিতে সাবস্ক্রিপশন লাগে (${freeCount}টি ফ্রি • ${paidCount}টি সাবস্ক্রিপশন)।"
                                        ),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        FilledTonalButton(onClick = { onSetAllFeatureSubscriptions(emptySet()) }) {
                                            Text(tr("All Free", "সব ফ্রি"))
                                        }
                                        FilledTonalButton(onClick = { onSetAllFeatureSubscriptions(SubscriptionFeature.DEFAULT_LOCKED_KEYS) }) {
                                            Text(tr("Recommended Paid", "প্রস্তাবিত পেইড"))
                                        }
                                        OutlinedButton(onClick = { onSetAllFeatureSubscriptions(SubscriptionFeature.ALL_LOCKED_KEYS) }) {
                                            Text(tr("All Subscription", "সব সাবস্ক্রিপশন"))
                                        }
                                    }
                                }
                            }
                        }
                        items(SubscriptionFeature.entries, key = { "sub_feat_${it.key}" }) { feature ->
                            val isPaid = lockedKeys.contains(feature.key)
                            val title = if (isBn) feature.titleBn else feature.titleEn
                            val desc = if (isBn) feature.descBn else feature.descEn
                            Surface(
                                shape = MaterialTheme.shapes.medium,
                                color = MaterialTheme.colorScheme.surface,
                                tonalElevation = 2.dp,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        FeaturePlanBadge(isFree = !isPaid, hasActiveSub = false)
                                        Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                        Text(desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Switch(
                                            checked = isPaid,
                                            onCheckedChange = { checked -> onToggleFeatureSubscription(feature, checked) },
                                            modifier = Modifier.testTag("feature_gate_switch_${feature.key.lowercase()}")
                                        )
                                        Text(
                                            if (isPaid) tr("Paid", "সাবস্ক্রিপশন") else tr("Free", "ফ্রি"),
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isPaid) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                else -> {
                    AdminDeveloperPaymentAccountsForm(
                        systemSettings = systemSettings,
                        onSaveSettings = onSaveSettings
                    )
                }
            }
        }
    }

    if (showCreate || editingPlan != null) {
        val init = editingPlan
        var name by remember(init?.id) { mutableStateOf(init?.name ?: "") }
        var priceText by remember(init?.id) { mutableStateOf(init?.priceBaseBdt?.toInt()?.toString() ?: "50") }
        var daysText by remember(init?.id) { mutableStateOf(init?.durationDays?.toString() ?: "30") }
        var desc by remember(init?.id) { mutableStateOf(init?.description ?: "") }
        var features by remember(init?.id) { mutableStateOf(init?.featuresSummary ?: "Unlimited Farms • Full Modules") }
        var active by remember(init?.id) { mutableStateOf(init?.active ?: true) }

        AlertDialog(
            onDismissRequest = { showCreate = false; editingPlan = null },
            title = { Text(if (init == null) tr("Create Subscription Plan", "নতুন সাবস্ক্রিপশন প্ল্যান") else tr("Edit Subscription Plan", "সাবস্ক্রিপশন প্ল্যান সম্পাদনা")) },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 420.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text(tr("Plan Name *", "প্ল্যানের নাম *")) }, singleLine = true)
                    OutlinedTextField(value = priceText, onValueChange = { priceText = it }, label = { Text(tr("Base Price (BDT ৳) *", "মূল মূল্য (টাকা ৳) *")) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true)
                    OutlinedTextField(value = daysText, onValueChange = { daysText = it }, label = { Text(tr("Duration (days) *", "মেয়াদ (দিন) *")) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true)
                    OutlinedTextField(value = desc, onValueChange = { desc = it }, label = { Text(tr("Description", "বিবরণ")) })
                    OutlinedTextField(value = features, onValueChange = { features = it }, label = { Text(tr("Features", "সুবিধাসমূহ")) })
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = active, onCheckedChange = { active = it })
                        Text(tr("Active & Visible to Farmers", "সক্রিয় ও খামারিদের জন্য দৃশ্যমান"))
                    }
                }
            },
            confirmButton = {
                Button(enabled = name.isNotBlank(), onClick = {
                    onSavePlan(
                        (init ?: SubscriptionPlanEntity(name = name, priceBaseBdt = 50.0, durationDays = 30, description = desc, featuresSummary = features)).copy(
                            name = name.trim(),
                            priceBaseBdt = priceText.toDoubleOrNull() ?: 50.0,
                            durationDays = (daysText.toIntOrNull() ?: 30).coerceAtLeast(1),
                            description = desc.trim(),
                            featuresSummary = features.trim(),
                            active = active
                        )
                    )
                    showCreate = false
                    editingPlan = null
                }) { Text(tr("Save Plan", "সংরক্ষণ করুন")) }
            },
            dismissButton = { TextButton(onClick = { showCreate = false; editingPlan = null }) { Text(tr("Cancel", "বাতিল")) } }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AdminDeveloperPaymentAccountsForm(
    systemSettings: SystemSettingsEntity,
    onSaveSettings: (SystemSettingsEntity) -> Unit
) {
    var bkashEnabled by remember(systemSettings) { mutableStateOf(systemSettings.bkashEnabled) }
    var bkashNumber by remember(systemSettings) { mutableStateOf(systemSettings.bkashNumber) }
    var bkashType by remember(systemSettings) { mutableStateOf(systemSettings.bkashAccountType) }

    var nagadEnabled by remember(systemSettings) { mutableStateOf(systemSettings.nagadEnabled) }
    var nagadNumber by remember(systemSettings) { mutableStateOf(systemSettings.nagadNumber) }
    var nagadType by remember(systemSettings) { mutableStateOf(systemSettings.nagadAccountType) }

    var bankEnabled by remember(systemSettings) { mutableStateOf(systemSettings.bankEnabled) }
    var bankName by remember(systemSettings) { mutableStateOf(systemSettings.bankName) }
    var bankAccountName by remember(systemSettings) { mutableStateOf(systemSettings.bankAccountName) }
    var bankAccountNumber by remember(systemSettings) { mutableStateOf(systemSettings.bankAccountNumber) }
    var bankBranch by remember(systemSettings) { mutableStateOf(systemSettings.bankBranch) }

    var instructions by remember(systemSettings) { mutableStateOf(systemSettings.paymentInstructions) }
    var requireAllApproval by remember(systemSettings) { mutableStateOf(systemSettings.requireAdminApprovalForAll) }

    val accountTypes = listOf("Personal", "Merchant", "Agent")

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item(key = "pay_intro") {
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        tr("Developer Payment Channels & Account Numbers", "ডেভেলপার পেমেন্ট মাধ্যম ও অ্যাকাউন্ট নম্বর"),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        tr(
                            "Configure bKash, Nagad, and Bank Account details shown to users when paying for subscriptions.",
                            "ব্যবহারকারীরা সাবস্ক্রিপশনের টাকা যে বিকাশ, নগদ বা ব্যাংক অ্যাকাউন্টে পাঠাবে তা এখানে নির্ধারণ করুন।"
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }

        item(key = "pay_bkash") {
            Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surface, tonalElevation = 2.dp, modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.PhoneAndroid, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Text(tr("bKash Payment Channel", "বিকাশ (bKash) পেমেন্ট নম্বর"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                        Switch(checked = bkashEnabled, onCheckedChange = { bkashEnabled = it })
                    }
                    if (bkashEnabled) {
                        OutlinedTextField(
                            value = bkashNumber,
                            onValueChange = { bkashNumber = it },
                            label = { Text(tr("bKash Number *", "বিকাশ নম্বর *")) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("admin_bkash_number_input")
                        )
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            accountTypes.forEach { t ->
                                FilterChip(selected = bkashType == t, onClick = { bkashType = t }, label = { Text(t) })
                            }
                        }
                    }
                }
            }
        }

        item(key = "pay_nagad") {
            Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surface, tonalElevation = 2.dp, modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary)
                            Text(tr("Nagad Payment Channel", "নগদ (Nagad) পেমেন্ট নম্বর"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                        Switch(checked = nagadEnabled, onCheckedChange = { nagadEnabled = it })
                    }
                    if (nagadEnabled) {
                        OutlinedTextField(
                            value = nagadNumber,
                            onValueChange = { nagadNumber = it },
                            label = { Text(tr("Nagad Number *", "নগদ নম্বর *")) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("admin_nagad_number_input")
                        )
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            accountTypes.forEach { t ->
                                FilterChip(selected = nagadType == t, onClick = { nagadType = t }, label = { Text(t) })
                            }
                        }
                    }
                }
            }
        }

        item(key = "pay_bank") {
            Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surface, tonalElevation = 2.dp, modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.AccountBalance, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                            Text(tr("Bank Account Transfer", "ব্যাংক অ্যাকাউন্ট তথ্য"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                        Switch(checked = bankEnabled, onCheckedChange = { bankEnabled = it })
                    }
                    if (bankEnabled) {
                        OutlinedTextField(value = bankName, onValueChange = { bankName = it }, label = { Text(tr("Bank Name", "ব্যাংকের নাম")) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = bankAccountName, onValueChange = { bankAccountName = it }, label = { Text(tr("Account Holder Name", "অ্যাকাউন্টের নাম")) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(
                            value = bankAccountNumber,
                            onValueChange = { bankAccountNumber = it },
                            label = { Text(tr("Bank Account Number *", "ব্যাংক অ্যাকাউন্ট নম্বর *")) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("admin_bank_number_input")
                        )
                        OutlinedTextField(value = bankBranch, onValueChange = { bankBranch = it }, label = { Text(tr("Branch Name", "শাখার নাম")) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    }
                }
            }
        }

        item(key = "pay_verification") {
            Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surface, tonalElevation = 2.dp, modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(tr("Verification & Instructions", "যাচাইকরণ ও নির্দেশনা"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(tr("Require Admin Approval for All Payments", "সকল পেমেন্টে অ্যাডমিন অনুমোদন বাধ্যতামূলক"), fontWeight = FontWeight.SemiBold)
                            Text(
                                tr(
                                    "When ON, bKash & Nagad stay PENDING until you approve. When OFF, bKash/Nagad activate immediately and Bank waits for approval.",
                                    "চালু থাকলে বিকাশ ও নগদ পেমেন্ট আপনার অনুমোদনের আগ পর্যন্ত অপেক্ষমাণ থাকবে। বন্ধ থাকলে শুধু ব্যাংক অনুমোদনের অপেক্ষায় থাকবে।"
                                ),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(checked = requireAllApproval, onCheckedChange = { requireAllApproval = it })
                    }
                    OutlinedTextField(
                        value = instructions,
                        onValueChange = { instructions = it },
                        label = { Text(tr("Checkout Payment Instructions", "চেকআউট পেমেন্ট নির্দেশনা")) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Button(
                        onClick = {
                            onSaveSettings(
                                systemSettings.copy(
                                    bkashEnabled = bkashEnabled,
                                    bkashNumber = bkashNumber.trim(),
                                    bkashAccountType = bkashType,
                                    nagadEnabled = nagadEnabled,
                                    nagadNumber = nagadNumber.trim(),
                                    nagadAccountType = nagadType,
                                    bankEnabled = bankEnabled,
                                    bankName = bankName.trim(),
                                    bankAccountName = bankAccountName.trim(),
                                    bankAccountNumber = bankAccountNumber.trim(),
                                    bankBranch = bankBranch.trim(),
                                    paymentInstructions = instructions.trim(),
                                    requireAdminApprovalForAll = requireAllApproval
                                )
                            )
                        },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("admin_save_payment_accounts_button")
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(tr("Save Payment Accounts", "পেমেন্ট তথ্য সংরক্ষণ করুন"), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminFinanceTab(
    state: SuperAdminState,
    currency: CurrencyOption,
    onUpdatePayment: (UserSubscriptionEntity, PaymentStatus, String) -> Unit
) {
    val isBn = isBangla()
    var statusFilter by remember { mutableStateOf<String?>(null) }
    val filtered = remember(state.subscriptions, statusFilter) {
        if (statusFilter == null) state.subscriptions else state.subscriptions.filter { it.paymentStatus == statusFilter }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item(key = "fin_filters") {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                item { FilterChip(selected = statusFilter == null, onClick = { statusFilter = null }, label = { Text("${tr("All", "সব")} (${state.subscriptions.size})") }) }
                items(PaymentStatus.entries) { st ->
                    FilterChip(
                        selected = statusFilter == st.name,
                        onClick = { statusFilter = st.name },
                        label = { Text(DomainLocalization.paymentStatus(st, isBn)) }
                    )
                }
            }
        }
        items(filtered, key = { "fin_sub_${it.id}" }) { sub ->
            val pStatusEnum = PaymentStatus.entries.find { it.name == sub.paymentStatus }
            val pStatusText = if (pStatusEnum != null) DomainLocalization.paymentStatus(pStatusEnum, isBn) else sub.paymentStatus
            Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surface, tonalElevation = 2.dp, modifier = Modifier.fillMaxWidth()) {
                Row(modifier = Modifier.padding(14.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("${sub.userName} • ${sub.planName}", fontWeight = FontWeight.Bold)
                        Text("${DomainLocalization.paymentMethod(sub.paymentMethod, isBn)} • ${tr("Ref", "রেফ")}: ${sub.transactionRef} • $pStatusText", style = MaterialTheme.typography.labelSmall)
                        Text("${tr("Start", "শুরু")}: ${formatShortDate(sub.startDate)} • ${tr("Exp", "মেয়াদ")}: ${formatShortDate(sub.expiryDate)}", style = MaterialTheme.typography.labelSmall)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(CurrencyFormatter.format(sub.pricePaidBdt, currency), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        if (sub.paymentStatus == PaymentStatus.SUCCESSFUL.name) {
                            TextButton(onClick = { onUpdatePayment(sub, PaymentStatus.REFUNDED, "EXPIRED") }) {
                                Text(tr("Refund", "রিফান্ড"), color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminSystemTab(
    state: SuperAdminState,
    settings: SystemSettingsEntity,
    onBroadcast: (String, String) -> Unit,
    onSaveSettings: (SystemSettingsEntity) -> Unit,
    onResolveSupport: (SupportRequestEntity, String) -> Unit
) {
    var banner by remember(settings.announcementBanner) { mutableStateOf(settings.announcementBanner) }
    var maint by remember(settings.maintenanceMode) { mutableStateOf(settings.maintenanceMode) }
    var bTitle by remember { mutableStateOf("") }
    var bMsg by remember { mutableStateOf("") }
    var replyTarget by remember { mutableStateOf<SupportRequestEntity?>(null) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item(key = "sys_settings") {
            Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surface, tonalElevation = 2.dp, modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(tr("System Settings & App Banner", "সিস্টেম সেটিংস ও ঘোষণা ব্যানার"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    OutlinedTextField(value = banner, onValueChange = { banner = it }, label = { Text(tr("Dashboard Announcement Banner", "ড্যাশবোর্ড ঘোষণা ব্যানার")) }, modifier = Modifier.fillMaxWidth())
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Switch(checked = maint, onCheckedChange = { maint = it })
                        Spacer(Modifier.width(8.dp))
                        Text(tr("Maintenance Mode", "রক্ষণাবেক্ষণ মোড"))
                    }
                    Button(onClick = { onSaveSettings(settings.copy(announcementBanner = banner, maintenanceMode = maint)) }) {
                        Text(tr("Save Settings", "সেটিংস সংরক্ষণ"))
                    }
                }
            }
        }

        item(key = "sys_broadcast") {
            Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surface, tonalElevation = 2.dp, modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(tr("Broadcast Notification", "সবার কাছে নোটিফিকেশন পাঠান"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    OutlinedTextField(value = bTitle, onValueChange = { bTitle = it }, label = { Text(tr("Announcement Title", "ঘোষণার শিরোনাম")) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = bMsg, onValueChange = { bMsg = it }, label = { Text(tr("Message to All Users", "সকল ব্যবহারকারীর জন্য বার্তা")) }, modifier = Modifier.fillMaxWidth())
                    Button(enabled = bTitle.isNotBlank() && bMsg.isNotBlank(), onClick = {
                        onBroadcast(bTitle, bMsg)
                        bTitle = ""; bMsg = ""
                    }) { Text(tr("Send Broadcast", "বার্তা পাঠান")) }
                }
            }
        }

        item(key = "sys_support_header") {
            Text("${tr("Support Requests", "সাপোর্ট অনুরোধ")} (${state.supportRequests.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
        items(state.supportRequests, key = { "sup_${it.id}" }) { req ->
            val stLabel = if (req.status == "OPEN") tr("OPEN", "খোলা") else tr("RESOLVED", "সমাধান হয়েছে")
            Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surface, tonalElevation = 2.dp, modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("${req.subject} • ${req.userName} ($stLabel)", fontWeight = FontWeight.Bold)
                    Text(req.message, style = MaterialTheme.typography.bodyMedium)
                    if (req.status == "OPEN") {
                        FilledTonalButton(onClick = { replyTarget = req }) { Text(tr("Reply & Resolve", "উত্তর ও সমাধান করুন")) }
                    } else {
                        Text("${tr("Reply", "উত্তর")}: ${req.adminReply}", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }

        item(key = "sys_audit_header") {
            Text("${tr("Admin Activity Audit Logs", "অ্যাডমিন কার্যকলাপ লগ")} (${state.auditLogs.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
        items(state.auditLogs, key = { "log_${it.id}" }) { log ->
            Surface(shape = MaterialTheme.shapes.small, color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Text("${log.action} • ${log.target}", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                    Text("${log.details} — ${tr("by", "দ্বারা")} ${log.actorName} (${formatShortDate(log.createdAt)})", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }

    replyTarget?.let { req ->
        val defaultReply = tr("Resolved by Super Admin team.", "সুপার অ্যাডমিন টিম দ্বারা সমাধান করা হয়েছে।")
        var reply by remember(req.id) { mutableStateOf(defaultReply) }
        AlertDialog(
            onDismissRequest = { replyTarget = null },
            title = { Text("${tr("Resolve Ticket", "টিকিট সমাধান")}: ${req.subject}") },
            text = { OutlinedTextField(value = reply, onValueChange = { reply = it }, label = { Text(tr("Admin Reply", "অ্যাডমিন উত্তর")) }) },
            confirmButton = {
                Button(onClick = {
                    onResolveSupport(req, reply)
                    replyTarget = null
                }) { Text(tr("Send & Resolve", "পাঠান ও সমাধান")) }
            },
            dismissButton = { TextButton(onClick = { replyTarget = null }) { Text(tr("Cancel", "বাতিল")) } }
        )
    }
}
