package com.example.ui.screens.farmer

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.local.*
import com.example.i18n.AppLanguage
import com.example.i18n.AppStrings
import com.example.i18n.CurrencyFormatter
import com.example.i18n.CurrencyOption
import com.example.ui.components.*
import com.example.ui.state.FarmerDashboardState
import com.example.util.ReportExportHelper

@Composable
fun ReportsSubscriptionProfileScreen(
    user: UserEntity,
    state: FarmerDashboardState,
    plans: List<SubscriptionPlanEntity>,
    notifications: List<AppNotificationEntity>,
    strings: AppStrings,
    currentLanguage: AppLanguage,
    currentCurrency: CurrencyOption,
    darkModeOverride: Boolean?,
    languageOverrides: Map<String, String>,
    initialTab: Int = 0,
    onSelectLanguage: (AppLanguage) -> Unit,
    onSelectCurrency: (CurrencyOption) -> Unit,
    onSelectDarkMode: (Boolean?) -> Unit,
    onSetLanguageOverride: (String, String) -> Unit,
    onUpdateProfile: (String, String, String?) -> Unit,
    onSubscribe: (SubscriptionPlanEntity, String, String, String) -> Unit,
    onMarkNotificationsRead: () -> Unit,
    onSubmitSupport: (String, String) -> Unit,
    onLogout: () -> Unit,
    onMessage: (String) -> Unit
) {
    var tabIndex by remember(initialTab) { mutableIntStateOf(initialTab) }
    val unread = notifications.count { !it.readFlag }
    val tabs = listOf(strings.reports, strings.subscription, if (unread > 0) "Alerts ($unread)" else "Alerts", strings.profile)

    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = tabIndex) {
            tabs.forEachIndexed { idx, title ->
                Tab(selected = tabIndex == idx, onClick = { tabIndex = idx }, text = { Text(title, maxLines = 1) })
            }
        }
        when (tabIndex) {
            0 -> FinancialReportsTab(state, strings, currentCurrency, onMessage)
            1 -> SubscriptionTab(state, plans, strings, currentCurrency, onSubscribe)
            2 -> NotificationsAndSupportTab(notifications, state.supportRequests, onMarkNotificationsRead, onSubmitSupport)
            3 -> ProfileSettingsTab(
                user = user,
                strings = strings,
                currentLanguage = currentLanguage,
                currentCurrency = currentCurrency,
                darkModeOverride = darkModeOverride,
                languageOverrides = languageOverrides,
                onSelectLanguage = onSelectLanguage,
                onSelectCurrency = onSelectCurrency,
                onSelectDarkMode = onSelectDarkMode,
                onSetLanguageOverride = onSetLanguageOverride,
                onUpdateProfile = onUpdateProfile,
                onLogout = onLogout
            )
        }
    }
}

@Composable
private fun FinancialReportsTab(
    state: FarmerDashboardState,
    strings: AppStrings,
    currency: CurrencyOption,
    onMessage: (String) -> Unit
) {
    val context = LocalContext.current
    val totalSales = state.sales.sumOf { it.totalAmount }
    val totalIncome = state.sales.sumOf { it.paidAmount }
    val totalDue = state.sales.sumOf { it.dueAmount }
    val totalExp = state.expenses.sumOf { it.amount }
    val netProfit = totalSales - totalExp

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = {
                        ReportExportHelper.exportFinancialReportPdf(context, state.farms, state.sales, state.expenses, currency)
                            .onFailure { onMessage("PDF export failed: ${it.message}") }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(strings.exportPdf)
                }
                OutlinedButton(
                    onClick = {
                        ReportExportHelper.exportFinancialReportCsv(context, state.farms, state.sales, state.expenses, currency)
                            .onFailure { onMessage("CSV export failed: ${it.message}") }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.TableChart, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(strings.exportCsv)
                }
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                KpiMetricCard(strings.totalIncome, CurrencyFormatter.format(totalIncome, currency), Icons.Default.AttachMoney, subtitle = "Due: ${CurrencyFormatter.format(totalDue, currency)}", modifier = Modifier.weight(1f))
                KpiMetricCard(strings.netProfitLoss, CurrencyFormatter.format(netProfit, currency), Icons.Default.TrendingUp, tint = if (netProfit >= 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error, subtitle = "Exp: ${CurrencyFormatter.format(totalExp, currency)}", modifier = Modifier.weight(1f))
            }
        }

        item {
            val farmEntries = state.farms.map { farm ->
                val fSales = state.sales.filter { it.farmId == farm.id }.sumOf { it.totalAmount }
                val fExp = state.expenses.filter { it.farmId == farm.id }.sumOf { it.amount }
                DualBarEntry(farm.name.take(8), CurrencyFormatter.convertFromBase(fSales, currency), CurrencyFormatter.convertFromBase(fExp, currency))
            }
            DualBarComparisonChart(
                title = "${strings.monthlySales} vs ${strings.monthlyExpenses}",
                primaryLabel = strings.totalSales,
                secondaryLabel = strings.totalExpenses,
                entries = farmEntries
            )
        }

        item {
            val profitPoints = state.farms.map { farm ->
                val fSales = state.sales.filter { it.farmId == farm.id }.sumOf { it.totalAmount }
                val fExp = state.expenses.filter { it.farmId == farm.id }.sumOf { it.amount }
                farm.farmCode to CurrencyFormatter.convertFromBase(fSales - fExp, currency)
            }
            SmoothAreaLineChart(title = strings.profitAndLoss, points = profitPoints)
        }
    }
}

@Composable
private fun SubscriptionTab(
    state: FarmerDashboardState,
    plans: List<SubscriptionPlanEntity>,
    strings: AppStrings,
    currency: CurrencyOption,
    onSubscribe: (SubscriptionPlanEntity, String, String, String) -> Unit
) {
    var selectedPlan by remember { mutableStateOf<SubscriptionPlanEntity?>(null) }
    val activeSub = state.subscriptions.firstOrNull { it.status == "ACTIVE" }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        if (activeSub != null) "Active Plan: ${activeSub.planName}" else "No Active Subscription",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    if (activeSub != null) {
                        Text(
                            "Started: ${formatShortDate(activeSub.startDate)} • Expires: ${formatShortDate(activeSub.expiryDate)}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    } else {
                        Text("Choose a plan below to activate or renew your subscription.", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }

        item {
            Text("Available Plans (${currency.countryName})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }

        items(plans.filter { it.active }, key = { it.id }) { plan ->
            Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surface, tonalElevation = 2.dp, modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(plan.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text(
                            CurrencyFormatter.format(plan.priceBaseBdt, currency),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Text("${plan.durationDays} days • ${plan.description}", style = MaterialTheme.typography.bodyMedium)
                    Text(plan.featuresSummary, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Button(onClick = { selectedPlan = plan }, modifier = Modifier.fillMaxWidth()) {
                        Text(if (activeSub != null) "Renew / Switch Plan" else "Select & Pay")
                    }
                }
            }
        }

        item {
            Text("Payment & Subscription History", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
        items(state.subscriptions, key = { it.id }) { sub ->
            Surface(shape = MaterialTheme.shapes.small, color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), modifier = Modifier.fillMaxWidth()) {
                Row(modifier = Modifier.padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text("${sub.planName} (${sub.status})", fontWeight = FontWeight.Bold)
                        Text("${sub.paymentMethod} • Ref: ${sub.transactionRef} • Exp: ${formatShortDate(sub.expiryDate)}", style = MaterialTheme.typography.labelSmall)
                    }
                    Text(CurrencyFormatter.format(sub.pricePaidBdt, currency), fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    if (selectedPlan != null) {
        val plan = selectedPlan!!
        var gateway by remember { mutableStateOf(currency.gateways.first()) }
        var txRef by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { selectedPlan = null },
            title = { Text("Checkout: ${plan.name}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Payable: ${CurrencyFormatter.format(plan.priceBaseBdt, currency)} (${plan.durationDays} days)",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text("Country Gateways (${currency.countryName}):", style = MaterialTheme.typography.labelLarge)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(currency.gateways) { gw ->
                            FilterChip(selected = gateway == gw, onClick = { gateway = gw }, label = { Text(gw) })
                        }
                    }
                    OutlinedTextField(
                        value = txRef,
                        onValueChange = { txRef = it },
                        label = { Text("Transaction Ref / Wallet # (optional)") },
                        singleLine = true
                    )
                    if (gateway.contains("Manual", ignoreCase = true)) {
                        Text(
                            "Manual verification payments enter PENDING status until approved by Super Admin.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    onSubscribe(plan, currency.code, gateway, txRef)
                    selectedPlan = null
                }) { Text("Confirm Payment") }
            },
            dismissButton = { TextButton(onClick = { selectedPlan = null }) { Text(strings.cancel) } }
        )
    }
}

@Composable
private fun NotificationsAndSupportTab(
    notifications: List<AppNotificationEntity>,
    supportRequests: List<SupportRequestEntity>,
    onMarkRead: () -> Unit,
    onSubmitSupport: (String, String) -> Unit
) {
    var showSupportDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Notifications (${notifications.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Row {
                    TextButton(onClick = onMarkRead) { Text("Mark All Read") }
                    FilledTonalButton(onClick = { showSupportDialog = true }) { Text("Contact Support") }
                }
            }
        }
        items(notifications, key = { it.id }) { notif ->
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = if (notif.readFlag) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                tonalElevation = 1.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(notif.title, fontWeight = FontWeight.Bold)
                        Text(notif.category, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    }
                    Text(notif.message, style = MaterialTheme.typography.bodyMedium)
                    Text(formatShortDate(notif.createdAt), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        if (supportRequests.isNotEmpty()) {
            item {
                Spacer(Modifier.height(6.dp))
                Text("Your Support Tickets", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            items(supportRequests, key = { it.id }) { req ->
                Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surface, tonalElevation = 2.dp, modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("${req.subject} (${req.status})", fontWeight = FontWeight.Bold)
                        Text(req.message, style = MaterialTheme.typography.bodyMedium)
                        if (req.adminReply.isNotBlank()) {
                            Text("Admin Reply: ${req.adminReply}", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }
    }

    if (showSupportDialog) {
        var subject by remember { mutableStateOf("") }
        var msg by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showSupportDialog = false },
            title = { Text("Submit Support Request") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = subject, onValueChange = { subject = it }, label = { Text("Subject *") }, singleLine = true)
                    OutlinedTextField(value = msg, onValueChange = { msg = it }, label = { Text("Message *") })
                }
            },
            confirmButton = {
                Button(enabled = subject.isNotBlank() && msg.isNotBlank(), onClick = {
                    onSubmitSupport(subject, msg)
                    showSupportDialog = false
                }) { Text("Send") }
            },
            dismissButton = { TextButton(onClick = { showSupportDialog = false }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun ProfileSettingsTab(
    user: UserEntity,
    strings: AppStrings,
    currentLanguage: AppLanguage,
    currentCurrency: CurrencyOption,
    darkModeOverride: Boolean?,
    languageOverrides: Map<String, String>,
    onSelectLanguage: (AppLanguage) -> Unit,
    onSelectCurrency: (CurrencyOption) -> Unit,
    onSelectDarkMode: (Boolean?) -> Unit,
    onSetLanguageOverride: (String, String) -> Unit,
    onUpdateProfile: (String, String, String?) -> Unit,
    onLogout: () -> Unit
) {
    var name by remember(user.id) { mutableStateOf(user.name) }
    var mobile by remember(user.id) { mutableStateOf(user.mobile) }
    var imageUri by remember(user.id) { mutableStateOf(user.profileImageUri) }
    var showLangCurrDialog by remember { mutableStateOf(false) }
    var showLabelEditor by remember { mutableStateOf(false) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri: Uri? ->
        if (uri != null) imageUri = uri.toString()
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surface, tonalElevation = 2.dp, modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    if (!imageUri.isNullOrBlank()) {
                        AsyncImage(model = imageUri, contentDescription = user.name, contentScale = ContentScale.Crop, modifier = Modifier.size(76.dp).clip(CircleShape))
                    } else {
                        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.size(76.dp)) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(user.name.take(1).uppercase(), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                    OutlinedButton(onClick = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }) {
                        Icon(Icons.Default.PhotoCamera, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Upload Profile Picture")
                    }
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Full Name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = mobile, onValueChange = { mobile = it }, label = { Text("Mobile Number") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    Text("Email: ${user.email} • Role: ${user.role}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Button(onClick = { onUpdateProfile(name, mobile, imageUri) }, modifier = Modifier.fillMaxWidth()) {
                        Text(strings.save)
                    }
                }
            }
        }

        item {
            Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surface, tonalElevation = 2.dp, modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Internationalization & Appearance", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    FilledTonalButton(onClick = { showLangCurrDialog = true }, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.Language, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("${currentLanguage.nativeName} • ${currentCurrency.symbol} ${currentCurrency.code} (${currentCurrency.countryName})")
                    }
                    OutlinedButton(onClick = { showLabelEditor = true }, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.Translate, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Customize App Labels / Terminology")
                    }
                }
            }
        }

        item {
            Button(
                onClick = onLogout,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                modifier = Modifier.fillMaxWidth().height(48.dp).testTag("logout_button")
            ) {
                Icon(Icons.Default.Logout, contentDescription = strings.logout)
                Spacer(Modifier.width(8.dp))
                Text(strings.logout, fontWeight = FontWeight.Bold)
            }
        }
    }

    if (showLangCurrDialog) {
        LanguageCurrencyDialog(
            currentLanguage = currentLanguage,
            currentCurrency = currentCurrency,
            darkModeOverride = darkModeOverride,
            onSelectLanguage = onSelectLanguage,
            onSelectCurrency = onSelectCurrency,
            onSelectDarkMode = onSelectDarkMode,
            onDismiss = { showLangCurrDialog = false }
        )
    }

    if (showLabelEditor) {
        var customAppName by remember { mutableStateOf(languageOverrides["appName"] ?: "") }
        var customDash by remember { mutableStateOf(languageOverrides["dashboard"] ?: "") }
        var customFarms by remember { mutableStateOf(languageOverrides["farms"] ?: "") }
        AlertDialog(
            onDismissRequest = { showLabelEditor = false },
            title = { Text("Customize App Terminology") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Leave blank to use default ${currentLanguage.displayName} text.", style = MaterialTheme.typography.labelSmall)
                    OutlinedTextField(value = customAppName, onValueChange = { customAppName = it }, label = { Text("App Title") }, singleLine = true)
                    OutlinedTextField(value = customDash, onValueChange = { customDash = it }, label = { Text("Dashboard Tab") }, singleLine = true)
                    OutlinedTextField(value = customFarms, onValueChange = { customFarms = it }, label = { Text("Farms Tab") }, singleLine = true)
                }
            },
            confirmButton = {
                Button(onClick = {
                    onSetLanguageOverride("appName", customAppName)
                    onSetLanguageOverride("dashboard", customDash)
                    onSetLanguageOverride("farms", customFarms)
                    showLabelEditor = false
                }) { Text(strings.save) }
            },
            dismissButton = { TextButton(onClick = { showLabelEditor = false }) { Text(strings.cancel) } }
        )
    }
}
