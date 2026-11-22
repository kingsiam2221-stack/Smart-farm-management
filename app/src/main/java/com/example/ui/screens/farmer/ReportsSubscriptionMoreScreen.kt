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
import com.example.i18n.DomainLocalization
import com.example.i18n.isBangla
import com.example.i18n.tr
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
    val alertsLabel = tr("Alerts", "সতর্কতা")
    val tabs = listOf(
        strings.reports,
        strings.subscription,
        if (unread > 0) "$alertsLabel ($unread)" else alertsLabel,
        strings.profile
    )

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
    val isBn = isBangla()
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
                            .onFailure {
                                val errPrefix = if (isBn) "পিডিএফ রপ্তানি ব্যর্থ: " else "PDF export failed: "
                                onMessage("$errPrefix${it.message}")
                            }
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
                            .onFailure {
                                val errPrefix = if (isBn) "সিএসভি রপ্তানি ব্যর্থ: " else "CSV export failed: "
                                onMessage("$errPrefix${it.message}")
                            }
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
                KpiMetricCard(
                    strings.totalIncome,
                    CurrencyFormatter.format(totalIncome, currency),
                    Icons.Default.AttachMoney,
                    subtitle = "${tr("Due", "বকেয়া")}: ${CurrencyFormatter.format(totalDue, currency)}",
                    modifier = Modifier.weight(1f)
                )
                KpiMetricCard(
                    strings.netProfitLoss,
                    CurrencyFormatter.format(netProfit, currency),
                    Icons.Default.TrendingUp,
                    tint = if (netProfit >= 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                    subtitle = "${tr("Exp", "খরচ")}: ${CurrencyFormatter.format(totalExp, currency)}",
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            val farmEntries = state.farms.map { farm ->
                val fSales = state.sales.filter { it.farmId == farm.id }.sumOf { it.totalAmount }
                val fExp = state.expenses.filter { it.farmId == farm.id }.sumOf { it.amount }
                DualBarEntry(farm.name.take(8), CurrencyFormatter.convertFromBase(fSales, currency), CurrencyFormatter.convertFromBase(fExp, currency))
            }
            DualBarComparisonChart(
                title = "${strings.monthlySales} ${tr("vs", "বনাম")} ${strings.monthlyExpenses}",
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
    val isBn = isBangla()
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
                        if (activeSub != null) "${tr("Active Plan", "সক্রিয় প্ল্যান")}: ${activeSub.planName}"
                        else tr("No Active Subscription", "কোনো সক্রিয় সাবস্ক্রিপশন নেই"),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    if (activeSub != null) {
                        Text(
                            "${tr("Started", "শুরু")}: ${formatShortDate(activeSub.startDate)} • ${tr("Expires", "মেয়াদ শেষ")}: ${formatShortDate(activeSub.expiryDate)}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    } else {
                        Text(
                            tr(
                                "Choose a plan below to activate or renew your subscription.",
                                "আপনার সাবস্ক্রিপশন চালু বা নবায়ন করতে নিচে থেকে একটি প্ল্যান বেছে নিন।"
                            ),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }

        item {
            Text(
                "${tr("Available Plans", "উপলব্ধ প্ল্যান")} (${currency.countryName})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
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
                    Text("${plan.durationDays} ${tr("days", "দিন")} • ${plan.description}", style = MaterialTheme.typography.bodyMedium)
                    Text(plan.featuresSummary, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Button(onClick = { selectedPlan = plan }, modifier = Modifier.fillMaxWidth()) {
                        Text(
                            if (activeSub != null) tr("Renew / Switch Plan", "প্ল্যান নবায়ন / পরিবর্তন")
                            else tr("Select & Pay", "নির্বাচন ও পেমেন্ট করুন")
                        )
                    }
                }
            }
        }

        item {
            Text(tr("Payment & Subscription History", "পেমেন্ট ও সাবস্ক্রিপশন ইতিহাস"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
        items(state.subscriptions, key = { it.id }) { sub ->
            Surface(shape = MaterialTheme.shapes.small, color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), modifier = Modifier.fillMaxWidth()) {
                Row(modifier = Modifier.padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text("${sub.planName} (${DomainLocalization.subscriptionStatus(sub.status, isBn)})", fontWeight = FontWeight.Bold)
                        Text("${DomainLocalization.paymentMethod(sub.paymentMethod, isBn)} • ${tr("Ref", "রেফ")}: ${sub.transactionRef} • ${tr("Exp", "মেয়াদ")}: ${formatShortDate(sub.expiryDate)}", style = MaterialTheme.typography.labelSmall)
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
            title = { Text("${tr("Checkout", "চেকআউট")}: ${plan.name}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "${tr("Payable", "পরিশোধযোগ্য")}: ${CurrencyFormatter.format(plan.priceBaseBdt, currency)} (${plan.durationDays} ${tr("days", "দিন")})",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text("${tr("Country Gateways", "পেমেন্ট গেটওয়ে")} (${currency.countryName}):", style = MaterialTheme.typography.labelLarge)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(currency.gateways) { gw ->
                            FilterChip(selected = gateway == gw, onClick = { gateway = gw }, label = { Text(DomainLocalization.paymentMethod(gw, isBn)) })
                        }
                    }
                    OutlinedTextField(
                        value = txRef,
                        onValueChange = { txRef = it },
                        label = { Text(tr("Transaction Ref / Wallet # (optional)", "ট্রানজেকশন নম্বর / ওয়ালেট (ঐচ্ছিক)")) },
                        singleLine = true
                    )
                    if (gateway.contains("Manual", ignoreCase = true)) {
                        Text(
                            tr(
                                "Manual verification payments enter PENDING status until approved by Super Admin.",
                                "ম্যানুয়াল পেমেন্ট সুপার অ্যাডমিন অনুমোদন না করা পর্যন্ত অপেক্ষমাণ (PENDING) থাকবে।"
                            ),
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
                }) { Text(tr("Confirm Payment", "পেমেন্ট নিশ্চিত করুন")) }
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
    val isBn = isBangla()
    var showSupportDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("${tr("Notifications", "নোটিফিকেশন")} (${notifications.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Row {
                    TextButton(onClick = onMarkRead) { Text(tr("Mark All Read", "সব পঠিত চিহ্নিত করুন")) }
                    FilledTonalButton(onClick = { showSupportDialog = true }) { Text(tr("Contact Support", "সাপোর্টে যোগাযোগ")) }
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
                Text(tr("Your Support Tickets", "আপনার সাপোর্ট টিকিট"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            items(supportRequests, key = { it.id }) { req ->
                val stLabel = if (req.status == "OPEN") tr("OPEN", "খোলা") else tr("RESOLVED", "সমাধান হয়েছে")
                Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surface, tonalElevation = 2.dp, modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("${req.subject} ($stLabel)", fontWeight = FontWeight.Bold)
                        Text(req.message, style = MaterialTheme.typography.bodyMedium)
                        if (req.adminReply.isNotBlank()) {
                            Text("${tr("Admin Reply", "অ্যাডমিন উত্তর")}: ${req.adminReply}", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodyMedium)
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
            title = { Text(tr("Submit Support Request", "সাপোর্ট অনুরোধ পাঠান")) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = subject, onValueChange = { subject = it }, label = { Text(tr("Subject *", "বিষয় *")) }, singleLine = true)
                    OutlinedTextField(value = msg, onValueChange = { msg = it }, label = { Text(tr("Message *", "বার্তা *")) })
                }
            },
            confirmButton = {
                Button(enabled = subject.isNotBlank() && msg.isNotBlank(), onClick = {
                    onSubmitSupport(subject, msg)
                    showSupportDialog = false
                }) { Text(tr("Send", "পাঠান")) }
            },
            dismissButton = { TextButton(onClick = { showSupportDialog = false }) { Text(if (isBn) "বাতিল" else "Cancel") } }
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
    val isBn = isBangla()
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
                        Text(tr("Upload Profile Picture", "প্রোফাইল ছবি আপলোড"))
                    }
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text(tr("Full Name", "পূর্ণ নাম")) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = mobile, onValueChange = { mobile = it }, label = { Text(tr("Mobile Number", "মোবাইল নম্বর")) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    Text(
                        "${tr("Email", "ইমেইল")}: ${user.email} • ${tr("Role", "ভূমিকা")}: ${DomainLocalization.userRole(user.role, isBn)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Button(onClick = { onUpdateProfile(name, mobile, imageUri) }, modifier = Modifier.fillMaxWidth()) {
                        Text(strings.save)
                    }
                }
            }
        }

        item {
            Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surface, tonalElevation = 2.dp, modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(tr("Internationalization & Appearance", "ভাষা, মুদ্রা ও থিম"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    FilledTonalButton(onClick = { showLangCurrDialog = true }, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.Language, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("${currentLanguage.nativeName} • ${currentCurrency.symbol} ${currentCurrency.code} (${currentCurrency.countryName})")
                    }
                    OutlinedButton(onClick = { showLabelEditor = true }, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.Translate, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(tr("Customize App Labels / Terminology", "অ্যাপের নাম ও ট্যাব শিরোনাম পরিবর্তন"))
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
            title = { Text(tr("Customize App Terminology", "অ্যাপের শিরোনাম কাস্টমাইজ করুন")) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        tr(
                            "Leave blank to use default ${currentLanguage.displayName} text.",
                            "ডিফল্ট ${currentLanguage.nativeName} লেখা ব্যবহার করতে ফাঁকা রাখুন।"
                        ),
                        style = MaterialTheme.typography.labelSmall
                    )
                    OutlinedTextField(value = customAppName, onValueChange = { customAppName = it }, label = { Text(tr("App Title", "অ্যাপের নাম")) }, singleLine = true)
                    OutlinedTextField(value = customDash, onValueChange = { customDash = it }, label = { Text(tr("Dashboard Tab", "ড্যাশবোর্ড ট্যাব")) }, singleLine = true)
                    OutlinedTextField(value = customFarms, onValueChange = { customFarms = it }, label = { Text(tr("Farms Tab", "খামার ট্যাব")) }, singleLine = true)
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
