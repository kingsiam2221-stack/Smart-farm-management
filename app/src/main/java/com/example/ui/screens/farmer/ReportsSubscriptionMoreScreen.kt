package com.example.ui.screens.farmer

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.local.*
import com.example.i18n.AppStrings
import com.example.i18n.CurrencyFormatter
import com.example.i18n.CurrencyOption
import com.example.i18n.DomainLocalization
import com.example.i18n.isBangla
import com.example.i18n.tr
import com.example.ui.components.*
import com.example.ui.state.FarmerDashboardState
import com.example.util.ReportExportHelper

enum class MoreTab {
    REPORTS, SUBSCRIPTION, ALERTS
}

@Composable
fun ReportsSubscriptionProfileScreen(
    state: FarmerDashboardState,
    plans: List<SubscriptionPlanEntity>,
    systemSettings: SystemSettingsEntity = SystemSettingsEntity(),
    notifications: List<AppNotificationEntity>,
    strings: AppStrings,
    currentCurrency: CurrencyOption,
    initialTab: MoreTab = MoreTab.REPORTS,
    onSubscribe: (SubscriptionPlanEntity, String, String, String) -> Unit,
    onMarkNotificationsRead: () -> Unit,
    onSubmitSupport: (String, String) -> Unit,
    onOpenSettings: () -> Unit = {},
    onMessage: (String) -> Unit
) {
    val visibleTabs = remember(state.accessPolicy) {
        buildList {
            if (state.accessPolicy.has(AppPermission.REPORTS_VIEW_EXPORT)) add(MoreTab.REPORTS)
            if (state.accessPolicy.has(AppPermission.SUBSCRIPTION_MANAGE)) add(MoreTab.SUBSCRIPTION)
            if (state.accessPolicy.has(AppPermission.ALERTS_CAUTION_VIEW)) add(MoreTab.ALERTS)
        }
    }

    if (visibleTabs.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
            EmptyStateCard(
                icon = Icons.Default.Tune,
                title = strings.more,
                subtitle = tr(
                    "Open Settings to manage your profile, language, currency, and developer support.",
                    "প্রোফাইল, ভাষা, মুদ্রা এবং ডেভেলপার সাপোর্ট ব্যবহার করতে সেটিংস খুলুন।"
                )
            )
        }
        return
    }

    var selectedTab by remember(initialTab, visibleTabs) {
        mutableStateOf(if (initialTab in visibleTabs) initialTab else visibleTabs.first())
    }
    val selectedIdx = visibleTabs.indexOf(selectedTab).coerceAtLeast(0)

    val unread = notifications.count { !it.readFlag }
    val alertsLabel = tr("Alerts / Caution", "সতর্কতা")

    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = selectedIdx) {
            visibleTabs.forEach { tab ->
                val title = when (tab) {
                    MoreTab.REPORTS -> strings.reports
                    MoreTab.SUBSCRIPTION -> strings.subscription
                    MoreTab.ALERTS -> if (unread > 0) "$alertsLabel ($unread)" else alertsLabel
                }
                Tab(
                    selected = selectedTab == tab,
                    onClick = { selectedTab = tab },
                    text = { Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                )
            }
        }

        when (selectedTab) {
            MoreTab.REPORTS -> FinancialReportsTab(
                state = state,
                strings = strings,
                currency = currentCurrency,
                onOpenSubscription = {
                    if (MoreTab.SUBSCRIPTION in visibleTabs) selectedTab = MoreTab.SUBSCRIPTION
                },
                onMessage = onMessage
            )
            MoreTab.SUBSCRIPTION -> SubscriptionTab(state, plans, systemSettings, strings, currentCurrency, onSubscribe, onMessage)
            MoreTab.ALERTS -> NotificationsAndSupportTab(
                notifications = notifications,
                supportRequests = state.supportRequests,
                onMarkRead = onMarkNotificationsRead,
                onSubmitSupport = onSubmitSupport,
                onOpenSettings = onOpenSettings
            )
        }
    }
}

@Composable
private fun FinancialReportsTab(
    state: FarmerDashboardState,
    strings: AppStrings,
    currency: CurrencyOption,
    onOpenSubscription: () -> Unit,
    onMessage: (String) -> Unit
) {
    val context = LocalContext.current
    val isBn = isBangla()
    val subPolicy = state.subscriptionPolicy

    if (!subPolicy.canAccess(SubscriptionFeature.FINANCIAL_REPORTS_EXPORT)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(strings.reports, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                FeaturePlanBadge(isFree = false, hasActiveSub = false)
            }
            SubscriptionRequiredLockCard(
                feature = SubscriptionFeature.FINANCIAL_REPORTS_EXPORT,
                onOpenSubscription = onOpenSubscription,
                canSubscribe = state.accessPolicy.has(AppPermission.SUBSCRIPTION_MANAGE)
            )
        }
        return
    }

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
        item(key = "rep_exports") {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(strings.reports, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                FeaturePlanBadge(
                    isFree = subPolicy.isFree(SubscriptionFeature.FINANCIAL_REPORTS_EXPORT),
                    hasActiveSub = subPolicy.hasActiveSubscription
                )
            }
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

        item(key = "rep_kpis") {
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

        item(key = "rep_bar") {
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

        item(key = "rep_line") {
            val profitPoints = state.farms.map { farm ->
                val fSales = state.sales.filter { it.farmId == farm.id }.sumOf { it.totalAmount }
                val fExp = state.expenses.filter { it.farmId == farm.id }.sumOf { it.amount }
                farm.farmCode to CurrencyFormatter.convertFromBase(fSales - fExp, currency)
            }
            SmoothAreaLineChart(title = strings.profitAndLoss, points = profitPoints)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SubscriptionTab(
    state: FarmerDashboardState,
    plans: List<SubscriptionPlanEntity>,
    systemSettings: SystemSettingsEntity,
    strings: AppStrings,
    currency: CurrencyOption,
    onSubscribe: (SubscriptionPlanEntity, String, String, String) -> Unit,
    onMessage: (String) -> Unit
) {
    val isBn = isBangla()
    val clipboard = LocalClipboardManager.current
    var selectedPlan by remember { mutableStateOf<SubscriptionPlanEntity?>(null) }
    val activeSub = state.subscriptions.firstOrNull { it.status == "ACTIVE" && it.expiryDate > System.currentTimeMillis() }
    val activePlans = remember(plans) { plans.filter { it.active } }
    val devMethods = remember(systemSettings) { systemSettings.configuredPaymentMethods() }
    val subPolicy = state.subscriptionPolicy

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item(key = "sub_banner") {
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        if (activeSub != null) "${tr("Active Plan", "সক্রিয় প্ল্যান")}: ${activeSub.planName}"
                        else tr("Free Tier • No Active Subscription", "ফ্রি প্ল্যান • সক্রিয় সাবস্ক্রিপশন নেই"),
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
                                "Free modules work anytime. Subscribe via bKash, Nagad, or Bank Account to unlock paid modules.",
                                "ফ্রি অপশনগুলো সবসময় চলবে। সাবস্ক্রিপশন-ভিত্তিক অপশন চালাতে বিকাশ, নগদ বা ব্যাংক অ্যাকাউন্টে পেমেন্ট করুন।"
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
        }

        item(key = "sub_feature_matrix") {
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        tr("Free vs Subscription Options (Set by App Developer)", "ফ্রি বনাম সাবস্ক্রিপশন অপশন (অ্যাপ ডেভেলপার নির্ধারিত)"),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    SubscriptionFeature.entries.forEach { feat ->
                        val isFree = subPolicy.isFree(feat)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (isBn) feat.titleBn else feat.titleEn,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f).padding(end = 8.dp)
                            )
                            FeaturePlanBadge(isFree = isFree, hasActiveSub = subPolicy.hasActiveSubscription)
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                    }
                }
            }
        }

        if (devMethods.isNotEmpty()) {
            item(key = "sub_payment_channels") {
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            tr("Developer Accepted Payment Accounts", "পেমেন্ট পাঠানোর নম্বর ও অ্যাকাউন্ট"),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        devMethods.forEach { method ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        "${if (isBn) method.accountTitleBn else method.accountTitleEn}: ${method.primaryNumberOrAccount}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        if (isBn) method.secondaryDetailsBn else method.secondaryDetailsEn,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                IconButton(
                                    onClick = {
                                        clipboard.setText(AnnotatedString(method.copyValue))
                                        onMessage(
                                            if (isBn) "কপি হয়েছে: ${method.copyValue}" else "Copied: ${method.copyValue}"
                                        )
                                    }
                                ) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = tr("Copy", "কপি করুন"), modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }
            }
        }

        item(key = "sub_plans_title") {
            Text(
                "${tr("Available Plans", "উপলব্ধ প্ল্যান")} (${currency.countryName})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        if (activePlans.isEmpty()) {
            item(key = "sub_no_plans") {
                EmptyStateCard(
                    icon = Icons.Default.CardMembership,
                    title = tr("No Plans Available", "কোনো প্ল্যান উপলব্ধ নেই"),
                    subtitle = tr(
                        "Contact Super Admin to publish subscription plans.",
                        "সাবস্ক্রিপশন প্ল্যান চালু করতে সুপার অ্যাডমিনের সাথে যোগাযোগ করুন।"
                    )
                )
            }
        } else {
            items(activePlans, key = { "plan_${it.id}" }) { plan ->
                Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surface, tonalElevation = 2.dp, modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text(plan.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
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
        }

        if (state.subscriptions.isNotEmpty()) {
            item(key = "sub_history_title") {
                Text(tr("Payment & Subscription History", "পেমেন্ট ও সাবস্ক্রিপশন ইতিহাস"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            items(state.subscriptions, key = { "sub_${it.id}" }) { sub ->
                Surface(shape = MaterialTheme.shapes.small, color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("${sub.planName} (${DomainLocalization.subscriptionStatus(sub.status, isBn)})", fontWeight = FontWeight.Bold)
                            Text(
                                "${DomainLocalization.paymentMethod(sub.paymentMethod, isBn)} • ${tr("Ref", "রেফ")}: ${sub.transactionRef} • ${tr("Exp", "মেয়াদ")}: ${formatShortDate(sub.expiryDate)}",
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                        Text(CurrencyFormatter.format(sub.pricePaidBdt, currency), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    selectedPlan?.let { plan ->
        var selectedMethodId by remember(plan.id, devMethods) { mutableStateOf(devMethods.firstOrNull()?.id ?: "") }
        val activeMethod = devMethods.firstOrNull { it.id == selectedMethodId } ?: devMethods.firstOrNull()
        var txRef by remember(plan.id) { mutableStateOf("") }
        val copiedMsg = tr("Copied account number", "নম্বর কপি করা হয়েছে")

        AlertDialog(
            onDismissRequest = { selectedPlan = null },
            title = { Text("${tr("Checkout", "চেকআউট")}: ${plan.name}") },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 430.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        "${tr("Payable", "পরিশোধযোগ্য")}: ${CurrencyFormatter.format(plan.priceBaseBdt, currency)} (${plan.durationDays} ${tr("days", "দিন")})",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    if (devMethods.isEmpty()) {
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = MaterialTheme.colorScheme.errorContainer,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                tr(
                                    "No payment channels are currently enabled by the App Developer / Super Admin. Please contact support.",
                                    "অ্যাপ ডেভেলপার / সুপার অ্যাডমিন এখনও কোনো পেমেন্ট মাধ্যম চালু করেননি। সাপোর্টে যোগাযোগ করুন।"
                                ),
                                modifier = Modifier.padding(12.dp),
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    } else {
                        Text(tr("Choose Payment Medium", "পেমেন্ট মাধ্যম বেছে নিন"), style = MaterialTheme.typography.labelLarge)
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            devMethods.forEach { method ->
                                FilterChip(
                                    selected = activeMethod?.id == method.id,
                                    onClick = { selectedMethodId = method.id },
                                    label = { Text(if (isBn) method.labelBn else method.labelEn) }
                                )
                            }
                        }

                        activeMethod?.let { method ->
                            Surface(
                                shape = MaterialTheme.shapes.small,
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            tr("Pay To Developer Account:", "যে নম্বরে / অ্যাকাউন্টে পাঠাবেন:"),
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        TextButton(
                                            onClick = {
                                                clipboard.setText(AnnotatedString(method.copyValue))
                                                onMessage("$copiedMsg: ${method.copyValue}")
                                            },
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                        ) {
                                            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
                                            Spacer(Modifier.width(4.dp))
                                            Text(tr("Copy", "কপি"), style = MaterialTheme.typography.labelSmall)
                                        }
                                    }
                                    Text(
                                        "${if (isBn) method.accountTitleBn else method.accountTitleEn} • ${method.primaryNumberOrAccount}",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        if (isBn) method.secondaryDetailsBn else method.secondaryDetailsEn,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                    if (systemSettings.paymentInstructions.isNotBlank()) {
                                        Text(
                                            systemSettings.paymentInstructions,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }

                        OutlinedTextField(
                            value = txRef,
                            onValueChange = { txRef = it },
                            label = { Text(tr("Transaction ID (TrxID) / Bank Ref *", "ট্রানজেকশন আইডি (TrxID) / রেফারেন্স *")) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("subscription_trx_input")
                        )

                        if (activeMethod?.requiresManualApproval == true) {
                            Text(
                                tr(
                                    "Requires Developer/Super Admin verification — status stays PENDING until approved.",
                                    "সুপার অ্যাডমিন যাচাই না করা পর্যন্ত পেমেন্ট অপেক্ষমাণ (PENDING) থাকবে।"
                                ),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.tertiary
                            )
                        } else {
                            Text(
                                tr(
                                    "Instant activation once you submit your TrxID.",
                                    "TrxID জমা দেওয়ার সাথে সাথেই সাবস্ক্রিপশন চালু হবে।"
                                ),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    enabled = activeMethod != null && txRef.trim().length >= 4,
                    onClick = {
                        if (activeMethod != null) {
                            onSubscribe(plan, currency.code, activeMethod.id, txRef.trim())
                            selectedPlan = null
                        }
                    },
                    modifier = Modifier.testTag("confirm_subscription_payment_button")
                ) { Text(tr("Confirm Payment", "পেমেন্ট নিশ্চিত করুন")) }
            },
            dismissButton = { TextButton(onClick = { selectedPlan = null }) { Text(strings.cancel) } }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun NotificationsAndSupportTab(
    notifications: List<AppNotificationEntity>,
    supportRequests: List<SupportRequestEntity>,
    onMarkRead: () -> Unit,
    onSubmitSupport: (String, String) -> Unit,
    onOpenSettings: () -> Unit = {}
) {
    val isBn = isBangla()
    var showSupportDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item(key = "alerts_header") {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "${tr("Caution & Notifications", "সতর্কতা ও নোটিফিকেশন")} (${notifications.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    TextButton(onClick = onMarkRead, enabled = notifications.any { !it.readFlag }) {
                        Icon(Icons.Default.DoneAll, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(tr("Mark All Read", "সব পঠিত চিহ্নিত করুন"))
                    }
                    FilledTonalButton(onClick = { showSupportDialog = true }) {
                        Icon(Icons.Default.SupportAgent, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(tr("Send Ticket", "টিকিট পাঠান"))
                    }
                    OutlinedButton(onClick = onOpenSettings) {
                        Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(tr("Developer Contact & Settings", "ডেভেলপার যোগাযোগ ও সেটিংস"))
                    }
                }
            }
        }
        if (notifications.isEmpty()) {
            item(key = "alerts_empty") {
                EmptyStateCard(
                    icon = Icons.Default.NotificationsNone,
                    title = tr("No Caution Alerts", "কোনো সতর্কতা বার্তা নেই"),
                    subtitle = tr(
                        "Low-stock warnings, expiry alerts, and broadcasts will appear here.",
                        "স্টক কম হওয়া, মেয়াদোত্তীর্ণ ও গুরুত্বপূর্ণ সতর্কতা এখানে দেখা যাবে।"
                    )
                )
            }
        } else {
            items(notifications, key = { "notif_${it.id}" }) { notif ->
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = if (notif.readFlag) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                    tonalElevation = 1.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(notif.title, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                            Text(notif.category, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                        }
                        Text(notif.message, style = MaterialTheme.typography.bodyMedium)
                        Text(formatShortDate(notif.createdAt), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
        if (supportRequests.isNotEmpty()) {
            item(key = "support_header") {
                Spacer(Modifier.height(6.dp))
                Text(tr("Your Support Tickets", "আপনার সাপোর্ট টিকিট"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            items(supportRequests, key = { "sup_${it.id}" }) { req ->
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
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
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
