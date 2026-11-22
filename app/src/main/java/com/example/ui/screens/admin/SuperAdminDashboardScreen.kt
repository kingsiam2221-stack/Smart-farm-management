package com.example.ui.screens.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
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
    onResolveSupport: (SupportRequestEntity, String) -> Unit,
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
            strings.adminPlans to Icons.Default.CardMembership,
            strings.adminFinance to Icons.Default.AccountBalance,
            strings.adminSystem to Icons.Default.AdminPanelSettings
        )

        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            topBar = {
                @OptIn(ExperimentalMaterial3Api::class)
                TopAppBar(
                    title = {
                        Column {
                            Text(strings.superAdminPortal, fontWeight = FontWeight.Bold)
                            Text("${tr("Private Developer Console", "প্রাইভেট ডেভেলপার কনসোল")} • ${adminUser.email}", style = MaterialTheme.typography.labelSmall)
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
                        navItems.forEachIndexed { idx, (label, icon) ->
                            NavigationRailItem(
                                selected = selectedTab == idx,
                                onClick = { selectedTab = idx },
                                icon = { Icon(icon, contentDescription = label) },
                                label = { Text(label) }
                            )
                        }
                    }
                }
                Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    when (selectedTab) {
                        0 -> AdminOverviewTab(state, currentCurrency)
                        1 -> AdminUsersTab(state, plans, onSetUserStatus, onGrantPlan)
                        2 -> AdminPlansTab(plans, state.subscriptions, currentCurrency, onSavePlan, onDeletePlan, onUpdatePayment)
                        3 -> AdminFinanceTab(state, currentCurrency, onUpdatePayment)
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
        item {
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
        item {
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
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                KpiMetricCard(tr("Monthly Sub Rev", "মাসিক সাবস্ক্রিপশন আয়"), CurrencyFormatter.format(monthlySubRev, currency), Icons.Default.CalendarMonth, modifier = Modifier.weight(1f))
                KpiMetricCard(tr("Yearly Sub Rev", "বাৎসরিক সাবস্ক্রিপশন আয়"), CurrencyFormatter.format(yearlySubRev, currency), Icons.Default.EventRepeat, modifier = Modifier.weight(1f))
            }
        }
        item {
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
        item {
            OutlinedTextField(
                value = search,
                onValueChange = { search = it },
                placeholder = { Text(tr("Search users by name, email, mobile...", "নাম, ইমেইল বা মোবাইল দিয়ে খুঁজুন...")) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }
        items(filtered, key = { it.id }) { u ->
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

    if (grantTarget != null) {
        val target = grantTarget!!
        AlertDialog(
            onDismissRequest = { grantTarget = null },
            title = { Text("${tr("Assign Subscription to", "সাবস্ক্রিপশন দিন:")} ${target.name}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
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

@Composable
private fun AdminPlansTab(
    plans: List<SubscriptionPlanEntity>,
    subs: List<UserSubscriptionEntity>,
    currency: CurrencyOption,
    onSavePlan: (SubscriptionPlanEntity) -> Unit,
    onDeletePlan: (SubscriptionPlanEntity) -> Unit,
    onUpdatePayment: (UserSubscriptionEntity, PaymentStatus, String) -> Unit
) {
    val isBn = isBangla()
    var editingPlan by remember { mutableStateOf<SubscriptionPlanEntity?>(null) }
    var showCreate by remember { mutableStateOf(false) }
    val pendingSubs = subs.filter { it.paymentStatus == PaymentStatus.PENDING.name || it.status == "PENDING" }

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showCreate = true },
                icon = { Icon(Icons.Default.Add, contentDescription = tr("Add Plan", "প্ল্যান যোগ করুন")) },
                text = { Text(tr("Create Plan", "নতুন প্ল্যান")) }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 88.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Text(tr("Subscription Plans (Live Editable)", "সাবস্ক্রিপশন প্ল্যানসমূহ"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            items(plans, key = { it.id }) { plan ->
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
                item {
                    Spacer(Modifier.height(6.dp))
                    Text("${tr("Manual Payment Verification Queue", "ম্যানুয়াল পেমেন্ট যাচাইকরণ তালিকা")} (${pendingSubs.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
                items(pendingSubs, key = { it.id }) { sub ->
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

    if (showCreate || editingPlan != null) {
        val init = editingPlan
        var name by remember { mutableStateOf(init?.name ?: "") }
        var priceText by remember { mutableStateOf(init?.priceBaseBdt?.toInt()?.toString() ?: "50") }
        var daysText by remember { mutableStateOf(init?.durationDays?.toString() ?: "30") }
        var desc by remember { mutableStateOf(init?.description ?: "") }
        var features by remember { mutableStateOf(init?.featuresSummary ?: "Unlimited Farms • Full Modules") }
        var active by remember { mutableStateOf(init?.active ?: true) }

        AlertDialog(
            onDismissRequest = { showCreate = false; editingPlan = null },
            title = { Text(if (init == null) tr("Create Subscription Plan", "নতুন সাবস্ক্রিপশন প্ল্যান") else tr("Edit Subscription Plan", "সাবস্ক্রিপশন প্ল্যান সম্পাদনা")) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
        item {
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
        items(filtered, key = { it.id }) { sub ->
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
        item {
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

        item {
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

        item {
            Text("${tr("Support Requests", "সাপোর্ট অনুরোধ")} (${state.supportRequests.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
        items(state.supportRequests, key = { it.id }) { req ->
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

        item {
            Text("${tr("Admin Activity Audit Logs", "অ্যাডমিন কার্যকলাপ লগ")} (${state.auditLogs.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
        items(state.auditLogs, key = { it.id }) { log ->
            Surface(shape = MaterialTheme.shapes.small, color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Text("${log.action} • ${log.target}", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                    Text("${log.details} — ${tr("by", "দ্বারা")} ${log.actorName} (${formatShortDate(log.createdAt)})", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }

    if (replyTarget != null) {
        val defaultReply = tr("Resolved by Super Admin team.", "সুপার অ্যাডমিন টিম দ্বারা সমাধান করা হয়েছে।")
        var reply by remember { mutableStateOf(defaultReply) }
        AlertDialog(
            onDismissRequest = { replyTarget = null },
            title = { Text("${tr("Resolve Ticket", "টিকিট সমাধান")}: ${replyTarget!!.subject}") },
            text = { OutlinedTextField(value = reply, onValueChange = { reply = it }, label = { Text(tr("Admin Reply", "অ্যাডমিন উত্তর")) }) },
            confirmButton = {
                Button(onClick = {
                    onResolveSupport(replyTarget!!, reply)
                    replyTarget = null
                }) { Text(tr("Send & Resolve", "পাঠান ও সমাধান")) }
            },
            dismissButton = { TextButton(onClick = { replyTarget = null }) { Text(tr("Cancel", "বাতিল")) } }
        )
    }
}
