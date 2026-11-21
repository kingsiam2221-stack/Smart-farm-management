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
                Text("403 • Unauthorized Access", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Button(onClick = onLogout) { Text("Return to Sign In") }
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
                            Text("Private Developer Console • ${adminUser.email}", style = MaterialTheme.typography.labelSmall)
                        }
                    },
                    actions = {
                        IconButton(onClick = { showSettingsModal = true }) {
                            Icon(Icons.Default.Language, contentDescription = "Language & Currency")
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
                KpiMetricCard("Total Users", "${state.users.size}", Icons.Default.People, subtitle = "$activeUsers Active • $inactiveUsers Inactive", modifier = Modifier.weight(1f))
                KpiMetricCard("Total Farms", "${state.farms.size}", Icons.Default.Agriculture, subtitle = "${state.orders.size} Platform Orders", modifier = Modifier.weight(1f))
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                KpiMetricCard("Subscriptions", "${state.subscriptions.size}", Icons.Default.CardMembership, subtitle = "$activeSubs Active • $expiredSubs Expired", modifier = Modifier.weight(1f))
                KpiMetricCard("Lifetime Revenue", CurrencyFormatter.format(lifetimeSubRev, currency), Icons.Default.Paid, subtitle = "Month: ${CurrencyFormatter.format(monthlySubRev, currency)}", modifier = Modifier.weight(1f))
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                KpiMetricCard("Monthly Sub Rev", CurrencyFormatter.format(monthlySubRev, currency), Icons.Default.CalendarMonth, modifier = Modifier.weight(1f))
                KpiMetricCard("Yearly Sub Rev", CurrencyFormatter.format(yearlySubRev, currency), Icons.Default.EventRepeat, modifier = Modifier.weight(1f))
            }
        }
        item {
            val chartPoints = listOf(
                "30d Rev" to CurrencyFormatter.convertFromBase(monthlySubRev, currency),
                "1y Rev" to CurrencyFormatter.convertFromBase(yearlySubRev, currency),
                "Total Rev" to CurrencyFormatter.convertFromBase(lifetimeSubRev, currency)
            )
            SmoothAreaLineChart(title = "Subscription Revenue Growth (${currency.symbol})", points = chartPoints)
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
                placeholder = { Text("Search users by name, email, mobile...") },
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
                            Text("${u.name} (${u.role})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text("${u.email} • ${u.mobile}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        AssistChip(onClick = {}, label = { Text(u.status) })
                    }
                    Text(
                        "Farms: $userFarms • Subscription: ${userSub?.planName ?: "None"} • Joined ${formatShortDate(u.createdAt)}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    if (u.role != UserRole.SUPER_ADMIN.name) {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            item {
                                FilledTonalButton(onClick = { onSetUserStatus(u, AccountStatus.ACTIVE) }) { Text("Activate") }
                            }
                            item {
                                OutlinedButton(onClick = { onSetUserStatus(u, AccountStatus.INACTIVE) }) { Text("Deactivate") }
                            }
                            item {
                                OutlinedButton(onClick = { onSetUserStatus(u, AccountStatus.SUSPENDED) }) { Text("Suspend") }
                            }
                            item {
                                Button(onClick = { grantTarget = u }) { Text("Assign Plan") }
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
            title = { Text("Assign Subscription to ${target.name}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    plans.forEach { p ->
                        FilledTonalButton(onClick = { onGrantPlan(target, p); grantTarget = null }, modifier = Modifier.fillMaxWidth()) {
                            Text("${p.name} (${p.durationDays} days)")
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { grantTarget = null }) { Text("Cancel") } }
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
    var editingPlan by remember { mutableStateOf<SubscriptionPlanEntity?>(null) }
    var showCreate by remember { mutableStateOf(false) }
    val pendingSubs = subs.filter { it.paymentStatus == PaymentStatus.PENDING.name || it.status == "PENDING" }

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showCreate = true },
                icon = { Icon(Icons.Default.Add, contentDescription = "Add Plan") },
                text = { Text("Create Plan") }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 88.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Text("Subscription Plans (Live Editable)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            items(plans, key = { it.id }) { plan ->
                Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surface, tonalElevation = 2.dp, modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.padding(14.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("${plan.name} (${if (plan.active) "Active" else "Hidden"})", fontWeight = FontWeight.Bold)
                            Text("${CurrencyFormatter.format(plan.priceBaseBdt, currency)} • ${plan.durationDays} days", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                            Text(plan.description, style = MaterialTheme.typography.labelSmall)
                        }
                        Row {
                            IconButton(onClick = { editingPlan = plan }) { Icon(Icons.Default.Edit, contentDescription = "Edit Plan") }
                            IconButton(onClick = { onDeletePlan(plan) }) { Icon(Icons.Default.DeleteOutline, contentDescription = "Delete Plan", tint = MaterialTheme.colorScheme.error) }
                        }
                    }
                }
            }
            if (pendingSubs.isNotEmpty()) {
                item {
                    Spacer(Modifier.height(6.dp))
                    Text("Manual Payment Verification Queue (${pendingSubs.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
                items(pendingSubs, key = { it.id }) { sub ->
                    Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.tertiaryContainer, modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("${sub.userName} • ${sub.planName} (${CurrencyFormatter.format(sub.pricePaidBdt, currency)})", fontWeight = FontWeight.Bold)
                            Text("${sub.paymentMethod} • Ref: ${sub.transactionRef}", style = MaterialTheme.typography.labelSmall)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(onClick = { onUpdatePayment(sub, PaymentStatus.SUCCESSFUL, "ACTIVE") }) { Text("Approve") }
                                OutlinedButton(onClick = { onUpdatePayment(sub, PaymentStatus.FAILED, "EXPIRED") }) { Text("Reject") }
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
            title = { Text(if (init == null) "Create Subscription Plan" else "Edit Subscription Plan") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Plan Name *") }, singleLine = true)
                    OutlinedTextField(value = priceText, onValueChange = { priceText = it }, label = { Text("Base Price (BDT ৳) *") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true)
                    OutlinedTextField(value = daysText, onValueChange = { daysText = it }, label = { Text("Duration (days) *") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true)
                    OutlinedTextField(value = desc, onValueChange = { desc = it }, label = { Text("Description") })
                    OutlinedTextField(value = features, onValueChange = { features = it }, label = { Text("Features") })
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = active, onCheckedChange = { active = it })
                        Text("Active & Visible to Farmers")
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
                }) { Text("Save Plan") }
            },
            dismissButton = { TextButton(onClick = { showCreate = false; editingPlan = null }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun AdminFinanceTab(
    state: SuperAdminState,
    currency: CurrencyOption,
    onUpdatePayment: (UserSubscriptionEntity, PaymentStatus, String) -> Unit
) {
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
                item { FilterChip(selected = statusFilter == null, onClick = { statusFilter = null }, label = { Text("All (${state.subscriptions.size})") }) }
                items(PaymentStatus.entries) { st ->
                    FilterChip(selected = statusFilter == st.name, onClick = { statusFilter = st.name }, label = { Text(st.label) })
                }
            }
        }
        items(filtered, key = { it.id }) { sub ->
            Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surface, tonalElevation = 2.dp, modifier = Modifier.fillMaxWidth()) {
                Row(modifier = Modifier.padding(14.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("${sub.userName} • ${sub.planName}", fontWeight = FontWeight.Bold)
                        Text("${sub.paymentMethod} • Ref: ${sub.transactionRef} • ${sub.paymentStatus}", style = MaterialTheme.typography.labelSmall)
                        Text("Start: ${formatShortDate(sub.startDate)} • Exp: ${formatShortDate(sub.expiryDate)}", style = MaterialTheme.typography.labelSmall)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(CurrencyFormatter.format(sub.pricePaidBdt, currency), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        if (sub.paymentStatus == PaymentStatus.SUCCESSFUL.name) {
                            TextButton(onClick = { onUpdatePayment(sub, PaymentStatus.REFUNDED, "EXPIRED") }) {
                                Text("Refund", color = MaterialTheme.colorScheme.error)
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
                    Text("System Settings & App Banner", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    OutlinedTextField(value = banner, onValueChange = { banner = it }, label = { Text("Dashboard Announcement Banner") }, modifier = Modifier.fillMaxWidth())
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Switch(checked = maint, onCheckedChange = { maint = it })
                        Spacer(Modifier.width(8.dp))
                        Text("Maintenance Mode")
                    }
                    Button(onClick = { onSaveSettings(settings.copy(announcementBanner = banner, maintenanceMode = maint)) }) { Text("Save Settings") }
                }
            }
        }

        item {
            Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surface, tonalElevation = 2.dp, modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Broadcast Notification", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    OutlinedTextField(value = bTitle, onValueChange = { bTitle = it }, label = { Text("Announcement Title") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = bMsg, onValueChange = { bMsg = it }, label = { Text("Message to All Users") }, modifier = Modifier.fillMaxWidth())
                    Button(enabled = bTitle.isNotBlank() && bMsg.isNotBlank(), onClick = {
                        onBroadcast(bTitle, bMsg)
                        bTitle = ""; bMsg = ""
                    }) { Text("Send Broadcast") }
                }
            }
        }

        item {
            Text("Support Requests (${state.supportRequests.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
        items(state.supportRequests, key = { it.id }) { req ->
            Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surface, tonalElevation = 2.dp, modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("${req.subject} • ${req.userName} (${req.status})", fontWeight = FontWeight.Bold)
                    Text(req.message, style = MaterialTheme.typography.bodyMedium)
                    if (req.status == "OPEN") {
                        FilledTonalButton(onClick = { replyTarget = req }) { Text("Reply & Resolve") }
                    } else {
                        Text("Reply: ${req.adminReply}", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }

        item {
            Text("Admin Activity Audit Logs (${state.auditLogs.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
        items(state.auditLogs, key = { it.id }) { log ->
            Surface(shape = MaterialTheme.shapes.small, color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Text("${log.action} • ${log.target}", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                    Text("${log.details} — by ${log.actorName} (${formatShortDate(log.createdAt)})", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }

    if (replyTarget != null) {
        var reply by remember { mutableStateOf("Resolved by Super Admin team.") }
        AlertDialog(
            onDismissRequest = { replyTarget = null },
            title = { Text("Resolve Ticket: ${replyTarget!!.subject}") },
            text = { OutlinedTextField(value = reply, onValueChange = { reply = it }, label = { Text("Admin Reply") }) },
            confirmButton = {
                Button(onClick = {
                    onResolveSupport(replyTarget!!, reply)
                    replyTarget = null
                }) { Text("Send & Resolve") }
            },
            dismissButton = { TextButton(onClick = { replyTarget = null }) { Text("Cancel") } }
        )
    }
}
