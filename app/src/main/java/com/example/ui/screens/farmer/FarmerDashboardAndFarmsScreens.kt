package com.example.ui.screens.farmer

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.R
import com.example.data.local.*
import com.example.i18n.AppStrings
import com.example.i18n.CurrencyFormatter
import com.example.i18n.CurrencyOption
import com.example.i18n.DomainLocalization
import com.example.i18n.isBangla
import com.example.i18n.tr
import com.example.ui.components.*
import com.example.ui.state.FarmerDashboardState

@Composable
fun FarmerDashboardScreen(
    user: UserEntity,
    state: FarmerDashboardState,
    strings: AppStrings,
    currency: CurrencyOption,
    unreadNotificationsCount: Int,
    announcementBanner: String,
    darkModeOverride: Boolean? = null,
    onSelectFarm: (Long?) -> Unit,
    onOpenFarms: () -> Unit,
    onOpenAddFarm: () -> Unit = onOpenFarms,
    onOpenLivestock: () -> Unit,
    onOpenEggs: () -> Unit,
    onOpenSales: () -> Unit,
    onOpenOrders: () -> Unit = onOpenSales,
    onOpenOnlineProducts: () -> Unit = {},
    onOpenAgriculture: () -> Unit = {},
    onOpenInventory: () -> Unit = onOpenFarms,
    onOpenPermissions: () -> Unit = {},
    onOpenGuide: () -> Unit = {},
    onOpenNotifications: () -> Unit,
    onOpenSubscription: () -> Unit,
    onOpenAiAdvisor: () -> Unit = {},
    onOpenAiAdStudio: () -> Unit = {},
    onOpenSettings: () -> Unit = {}
) {
    val isBn = isBangla()
    val policy = state.accessPolicy
    val subPolicy = state.subscriptionPolicy
    val canSubscribe = policy.has(AppPermission.SUBSCRIPTION_MANAGE)
    val activeFarmId = state.selectedFarmId
    val filteredAnimals = remember(state.animals, activeFarmId) {
        if (activeFarmId == null) state.animals else state.animals.filter { it.farmId == activeFarmId }
    }
    val filteredEggs = remember(state.eggs, activeFarmId) {
        if (activeFarmId == null) state.eggs else state.eggs.filter { it.farmId == activeFarmId }
    }
    val filteredSales = remember(state.sales, activeFarmId) {
        if (activeFarmId == null) state.sales else state.sales.filter { it.farmId == activeFarmId }
    }
    val filteredExpenses = remember(state.expenses, activeFarmId) {
        if (activeFarmId == null) state.expenses else state.expenses.filter { it.farmId == activeFarmId }
    }

    val totalAnimals = filteredAnimals.filter { !it.birdCategory }.sumOf { it.quantity }
    val totalBirds = filteredAnimals.filter { it.birdCategory }.sumOf { it.quantity }
    val eggsProduced = filteredEggs.sumOf { it.producedCount }
    val eggsAvailable = filteredEggs
        .groupBy { it.farmId to it.eggCategory }
        .values
        .sumOf { list -> list.maxByOrNull { it.productionDate }?.availableCount ?: 0 }

    val totalSales = filteredSales.sumOf { it.totalAmount }
    val totalIncome = filteredSales.sumOf { it.paidAmount }
    val totalDue = filteredSales.sumOf { it.dueAmount }
    val totalExpenses = filteredExpenses.sumOf { it.amount }
    val netProfit = totalSales - totalExpenses

    val activeSub = state.subscriptions.firstOrNull { it.status == "ACTIVE" && it.expiryDate > System.currentTimeMillis() }
    val lowStockCount = state.inventory.count { it.quantity <= it.lowStockThreshold }
    val pendingOrdersCount = state.incomingOrders.count { it.status == OrderStatus.PENDING.name }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Hero Greeting Card
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(156.dp)
                    .clip(RoundedCornerShape(20.dp))
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_farm_hero),
                    contentDescription = tr("Smart Farm Banner", "স্মার্ট ফার্ম ব্যানার"),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.horizontalGradient(
                                listOf(
                                    Color(0xFF0E3E16).copy(alpha = 0.88f),
                                    Color(0xFF175B22).copy(alpha = 0.45f)
                                )
                            )
                        )
                )
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "${strings.welcomeBack}, ${user.name}",
                                style = MaterialTheme.typography.titleLarge,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                            val canOpenSub = policy.has(AppPermission.SUBSCRIPTION_MANAGE)
                            Text(
                                if (activeSub != null) "${activeSub.planName} • ${strings.active}"
                                else if (canOpenSub) tr("Subscription inactive • Tap to activate", "সাবস্ক্রিপশন নিষ্ক্রিয় • সক্রিয় করতে ট্যাপ করুন")
                                else tr("Subscription inactive", "সাবস্ক্রিপশন নিষ্ক্রিয়"),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFFB9F6CA),
                                modifier = if (canOpenSub) Modifier.clickable { onOpenSubscription() } else Modifier
                            )
                        }
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (policy.has(AppPermission.ALERTS_CAUTION_VIEW)) {
                                BadgedBox(
                                    badge = {
                                        if (unreadNotificationsCount > 0) {
                                            Badge { Text("$unreadNotificationsCount") }
                                        }
                                    }
                                ) {
                                    IconButton(
                                        onClick = onOpenNotifications,
                                        colors = IconButtonDefaults.iconButtonColors(containerColor = Color.White.copy(alpha = 0.22f))
                                    ) {
                                        Icon(Icons.Default.Notifications, contentDescription = strings.notifications, tint = Color.White)
                                    }
                                }
                            }
                            IconButton(
                                onClick = onOpenSettings,
                                colors = IconButtonDefaults.iconButtonColors(containerColor = Color.White.copy(alpha = 0.22f)),
                                modifier = Modifier.testTag("dashboard_settings_button")
                            ) {
                                Icon(Icons.Default.Settings, contentDescription = strings.settings, tint = Color.White)
                            }
                        }
                    }
                    Text(
                        announcementBanner,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.9f),
                        maxLines = 2
                    )
                }
            }
        }

        // Farm Switcher (Supports 1..100+ farms)
        if (policy.has(AppPermission.FARMS_VIEW)) {
            item {
                FarmSwitcherBar(
                    farms = state.farms,
                    selectedFarmId = state.selectedFarmId,
                    allFarmsLabel = strings.allFarms,
                    onSelectFarm = onSelectFarm
                )
            }
        }

        // Empty-Access Employee Notice
        if (!policy.isAdminOwner && policy.grantedKeys.isEmpty()) {
            item {
                EmptyStateCard(
                    icon = Icons.Default.LockPerson,
                    title = tr("No Options Enabled Yet", "এখনও কোনো অপশনের অনুমতি নেই"),
                    subtitle = tr(
                        "Ask your Farm Admin to grant you access to farms, livestock, eggs, sales, or custom modules from the Permissions screen.",
                        "খামারের অ্যাডমিনকে বলুন অনুমতি পাতা থেকে আপনার জন্য খামার, পশুপাখি, ডিম বা বিক্রয়ের অ্যাক্সেস চালু করে দিতে।"
                    )
                )
            }
        }

        // Alerts Strip (Pending Orders / Low Inventory)
        val showPendingChip = pendingOrdersCount > 0 && policy.has(AppPermission.ORDERS_VIEW)
        val showLowStockChip = lowStockCount > 0 && policy.has(AppPermission.INVENTORY_VIEW)
        if (showPendingChip || showLowStockChip) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (showPendingChip) {
                        AssistChip(
                            onClick = onOpenOrders,
                            label = { Text(tr("$pendingOrdersCount New Orders", "$pendingOrdersCount টি নতুন অর্ডার")) },
                            leadingIcon = { Icon(Icons.Default.LocalShipping, contentDescription = null, modifier = Modifier.size(16.dp)) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    if (showLowStockChip) {
                        AssistChip(
                            onClick = onOpenInventory,
                            label = { Text(tr("$lowStockCount Low Stock Alerts", "$lowStockCount টি কম স্টক সতর্কতা")) },
                            leadingIcon = { Icon(Icons.Default.WarningAmber, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.tertiary) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // Quick Actions Row (only permitted actions)
        val showAnyQuickAction = policy.canManagePermissions() ||
            policy.hasAny(AppPermission.AGRICULTURE_VIEW, AppPermission.AGRICULTURE_MANAGE) ||
            policy.hasAny(AppPermission.ONLINE_PRODUCTS_VIEW, AppPermission.ONLINE_PRODUCTS_MANAGE) ||
            policy.has(AppPermission.AI_ADVISOR) ||
            policy.has(AppPermission.FARM_GUIDE_VIEW) ||
            policy.has(AppPermission.FARMS_CREATE_EDIT) ||
            policy.has(AppPermission.LIVESTOCK_CREATE_EDIT) ||
            policy.has(AppPermission.EGGS_RECORD) ||
            policy.has(AppPermission.SALES_CREATE_COLLECT)
        if (showAnyQuickAction) {
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (policy.hasAny(AppPermission.AGRICULTURE_VIEW, AppPermission.AGRICULTURE_MANAGE)) {
                        item {
                            QuickActionPill(
                                Icons.Default.Grass,
                                strings.agriculture,
                                onOpenAgriculture,
                                testTag = "dashboard_agriculture_pill"
                            )
                        }
                    }
                    if (policy.hasAny(AppPermission.ONLINE_PRODUCTS_VIEW, AppPermission.ONLINE_PRODUCTS_MANAGE)) {
                        item {
                            QuickActionPill(
                                Icons.Default.AddShoppingCart,
                                tr("Online Product Add", "অনলাইন পণ্য যোগ"),
                                onOpenOnlineProducts,
                                testTag = "dashboard_online_products_pill"
                            )
                        }
                    }
                    if (policy.canManagePermissions()) {
                        item {
                            QuickActionPill(Icons.Default.AdminPanelSettings, strings.permissions, onOpenPermissions, testTag = "dashboard_permissions_pill")
                        }
                    }
                    if (policy.has(AppPermission.AI_ADVISOR)) {
                        item {
                            QuickActionPill(Icons.Default.AutoAwesome, tr("AI Advisor", "এআই উপদেষ্টা"), onOpenAiAdvisor, testTag = "open_ai_advisor_pill")
                        }
                    }
                    if (policy.has(AppPermission.AI_AD_STUDIO)) {
                        item {
                            QuickActionPill(Icons.Default.MovieCreation, strings.aiAdStudio, onOpenAiAdStudio, testTag = "dashboard_ai_ad_studio_pill")
                        }
                    }
                    if (policy.has(AppPermission.FARM_GUIDE_VIEW)) {
                        item {
                            QuickActionPill(Icons.AutoMirrored.Filled.MenuBook, strings.farmGuide, onOpenGuide, testTag = "open_farm_guide_pill")
                        }
                    }
                    if (policy.has(AppPermission.FARMS_CREATE_EDIT)) {
                        item {
                            QuickActionPill(Icons.Default.AddBusiness, strings.addFarm, onOpenAddFarm, testTag = "dashboard_add_farm_pill")
                        }
                    }
                    if (policy.has(AppPermission.LIVESTOCK_CREATE_EDIT)) {
                        item {
                            QuickActionPill(Icons.Default.Pets, strings.addAnimal, onOpenLivestock)
                        }
                    }
                    if (policy.has(AppPermission.EGGS_RECORD)) {
                        item {
                            QuickActionPill(Icons.Default.Egg, strings.recordEggs, onOpenEggs)
                        }
                    }
                    if (policy.has(AppPermission.SALES_CREATE_COLLECT)) {
                        item {
                            QuickActionPill(Icons.Default.PointOfSale, strings.newSale, onOpenSales)
                        }
                    }
                }
            }
        }

        // Farm Business Dedicated Option Card: Online Product Add & Marketplace Selling
        if (policy.hasAny(AppPermission.ONLINE_PRODUCTS_VIEW, AppPermission.ONLINE_PRODUCTS_MANAGE)) {
            item {
                val onlineUnlocked = subPolicy.canAccess(SubscriptionFeature.ONLINE_PRODUCT_STORE)
                val liveOnlineCount = state.inventory.count { it.listInMarketplace && it.quantity > 0.0 }
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.65f),
                    tonalElevation = 2.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("open_online_products_card")
                        .clickable { if (onlineUnlocked) onOpenOnlineProducts() else if (canSubscribe) onOpenSubscription() }
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.size(44.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Default.Storefront,
                                        contentDescription = strings.onlineProducts,
                                        tint = MaterialTheme.colorScheme.onTertiary
                                    )
                                }
                            }
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                FeaturePlanBadge(
                                    isFree = subPolicy.isFree(SubscriptionFeature.ONLINE_PRODUCT_STORE),
                                    hasActiveSub = subPolicy.hasActiveSubscription
                                )
                                Text(
                                    tr("Online Product Add (Agri Marketplace)", "অনলাইন পণ্য যোগ ও কৃষি মার্কেটপ্লেস"),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                                Text(
                                    tr(
                                        "$liveOnlineCount live • Sell Rice, Wheat, Mustard, Crops, Dairy or resold goods & receive farm orders",
                                        "$liveOnlineCount টি লাইভ • চাল, গম, সরিষা বা যেকোনো কৃষি পণ্য অনলাইনে বিক্রি করুন ও অর্ডার পান"
                                    ),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.85f)
                                )
                            }
                        }
                        FilledTonalButton(onClick = { if (onlineUnlocked) onOpenOnlineProducts() else if (canSubscribe) onOpenSubscription() }) {
                            Text(if (onlineUnlocked) tr("Add / Sell", "যোগ করুন") else tr("Unlock", "আনলক"))
                        }
                    }
                }
            }
        }

        // Separate Option Card: Agriculture, Crops (Rice, Wheat, Corn, Cattle Feed) & Land P/L Ledger
        if (policy.hasAny(AppPermission.AGRICULTURE_VIEW, AppPermission.AGRICULTURE_MANAGE)) {
            item {
                val agriUnlocked = subPolicy.canAccess(SubscriptionFeature.AGRICULTURE_CROP_LEDGER)
                val scopedAgriLands = remember(state.agriLands, activeFarmId) {
                    if (activeFarmId == null) state.agriLands else state.agriLands.filter { it.farmId == activeFarmId }
                }
                val scopedAgriLandIds = remember(scopedAgriLands) { scopedAgriLands.map { it.id }.toSet() }
                val scopedAgriCrops = remember(state.agriCrops, scopedAgriLandIds, activeFarmId) {
                    if (activeFarmId == null) state.agriCrops else state.agriCrops.filter { it.farmId == activeFarmId || it.landId in scopedAgriLandIds }
                }
                val scopedAgriExpenses = remember(state.agriExpenses, scopedAgriLandIds, activeFarmId) {
                    if (activeFarmId == null) state.agriExpenses else state.agriExpenses.filter { it.farmId == activeFarmId || it.landId in scopedAgriLandIds }
                }
                val agriOverview = remember(scopedAgriLands, scopedAgriCrops, scopedAgriExpenses) {
                    AgriLedgerCalculator.summarizeOverview(scopedAgriLands, scopedAgriCrops, scopedAgriExpenses)
                }
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.72f),
                    tonalElevation = 2.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("open_agriculture_card")
                        .clickable { if (agriUnlocked) onOpenAgriculture() else if (canSubscribe) onOpenSubscription() }
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(44.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Default.Grass,
                                        contentDescription = strings.agriculture,
                                        tint = MaterialTheme.colorScheme.onSecondary
                                    )
                                }
                            }
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                FeaturePlanBadge(
                                    isFree = subPolicy.isFree(SubscriptionFeature.AGRICULTURE_CROP_LEDGER),
                                    hasActiveSub = subPolicy.hasActiveSubscription
                                )
                                Text(
                                    tr("Agriculture, Crops & Land Ledger", "কৃষি, ফসল ও জমির লাভ-ক্ষতি খতিয়ান"),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                                val spentFmt = CurrencyFormatter.formatCompact(agriOverview.totalSpent, currency)
                                val worthFmt = CurrencyFormatter.formatCompact(agriOverview.totalYieldWorth, currency)
                                val pnlFmt = CurrencyFormatter.formatCompact(agriOverview.netProfitOrLoss, currency)
                                val pnlPrefix = if (agriOverview.netProfitOrLoss >= 0.0) "+" else ""
                                Text(
                                    tr(
                                        "${agriOverview.totalLands} plots • ${agriOverview.totalCrops} crops (Rice, Wheat, Corn, Feed) • Spent $spentFmt • Yield $worthFmt • P/L $pnlPrefix$pnlFmt",
                                        "${agriOverview.totalLands} জমি • ${agriOverview.totalCrops} ফসল (ধান, গম, ভুট্টা, গোখাদ্য) • খরচ $spentFmt • ফসল মূল্য $worthFmt • নিট $pnlPrefix$pnlFmt"
                                    ),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.88f)
                                )
                            }
                        }
                        FilledTonalButton(onClick = { if (agriUnlocked) onOpenAgriculture() else if (canSubscribe) onOpenSubscription() }) {
                            Text(if (agriUnlocked) tr("Open", "খুলুন") else tr("Unlock", "আনলক"))
                        }
                    }
                }
            }
        }

        // Gemini AI Farm Advisor Highlight Card
        if (policy.has(AppPermission.AI_ADVISOR)) {
            item {
                val aiUnlocked = subPolicy.canAccess(SubscriptionFeature.DASHBOARD_CHARTS_AI)
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.72f),
                    tonalElevation = 2.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("open_ai_advisor_button")
                        .clickable { if (aiUnlocked) onOpenAiAdvisor() else if (canSubscribe) onOpenSubscription() }
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(44.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Default.AutoAwesome,
                                        contentDescription = tr("AI Farm Advisor", "স্মার্ট ফার্ম এআই উপদেষ্টা"),
                                        tint = MaterialTheme.colorScheme.onPrimary
                                    )
                                }
                            }
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                FeaturePlanBadge(
                                    isFree = subPolicy.isFree(SubscriptionFeature.DASHBOARD_CHARTS_AI),
                                    hasActiveSub = subPolicy.hasActiveSubscription
                                )
                                Text(
                                    tr("Smart Farm AI Advisor", "স্মার্ট ফার্ম এআই উপদেষ্টা"),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    tr(
                                        "Ask questions, run a live Farm Audit, or inspect livestock/feed photos",
                                        "প্রশ্ন করুন, লাইভ ফার্ম অডিট চালান অথবা পশুপাখি ও খাদ্যের ছবি পরীক্ষা করুন"
                                    ),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                                )
                            }
                        }
                        FilledTonalButton(onClick = { if (aiUnlocked) onOpenAiAdvisor() else if (canSubscribe) onOpenSubscription() }) {
                            Text(if (aiUnlocked) tr("Ask AI", "জিজ্ঞাসা") else tr("Unlock", "আনলক"))
                        }
                    }
                }
            }
        }

        // Google AI Bengali Product Video Ad Studio Highlight Card
        if (policy.has(AppPermission.AI_AD_STUDIO)) {
            item {
                val adStudioUnlocked = subPolicy.canAccess(SubscriptionFeature.AI_AD_VIDEO_STUDIO)
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.72f),
                    tonalElevation = 2.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("open_ai_ad_studio_card")
                        .clickable { if (adStudioUnlocked) onOpenAiAdStudio() else if (canSubscribe) onOpenSubscription() }
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.size(44.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Default.MovieCreation,
                                        contentDescription = strings.aiAdStudio,
                                        tint = MaterialTheme.colorScheme.onTertiary
                                    )
                                }
                            }
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                FeaturePlanBadge(
                                    isFree = subPolicy.isFree(SubscriptionFeature.AI_AD_VIDEO_STUDIO),
                                    hasActiveSub = subPolicy.hasActiveSubscription
                                )
                                Text(
                                    tr("Google AI Bengali Product Video Ads", "Google AI বাংলা পণ্য বিজ্ঞাপন ও অর্ডার ভিডিও"),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                                Text(
                                    tr(
                                        "${state.productAds.size} ads • Turn any product photo into a Bengali video commercial + screen-record order demo",
                                        "${state.productAds.size}টি বিজ্ঞাপন • যেকোনো পণ্যের ছবি দিয়ে বাংলা বিজ্ঞাপন ও 'এখনই অর্ডার করুন' ভিডিও বানান"
                                    ),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.85f)
                                )
                            }
                        }
                        FilledTonalButton(onClick = { if (adStudioUnlocked) onOpenAiAdStudio() else if (canSubscribe) onOpenSubscription() }) {
                            Text(if (adStudioUnlocked) tr("Create Ad", "বিজ্ঞাপন") else tr("Unlock", "আনলক"))
                        }
                    }
                }
            }
        }

        // Separate Option Card: Duck & Chicken Farming Guide + Summary
        if (policy.has(AppPermission.FARM_GUIDE_VIEW)) {
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.72f),
                    tonalElevation = 2.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("open_farm_guide_card")
                        .clickable { onOpenGuide() }
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(44.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.AutoMirrored.Filled.MenuBook,
                                        contentDescription = strings.farmGuide,
                                        tint = MaterialTheme.colorScheme.onSecondary
                                    )
                                }
                            }
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                FeaturePlanBadge(
                                    isFree = subPolicy.isFree(SubscriptionFeature.FARM_GUIDE),
                                    hasActiveSub = subPolicy.hasActiveSubscription
                                )
                                Text(
                                    tr("Duck & Chicken Farming Guide + Summary", "হাঁস-মুরগি পালন, ডিম ফুটানো ও টিকা গাইড"),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                                Text(
                                    tr(
                                        "Hatch duck eggs • Raise ducks & chickens • Vaccine & medicine timing",
                                        "সারাংশ • হাঁসের ডিম ফুটানো • হাঁস-মুরগি পালন • টিকা ও ওষুধের সময়সূচি"
                                    ),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.85f)
                                )
                            }
                        }
                        FilledTonalButton(onClick = onOpenGuide) {
                            Text(tr("Open", "খুলুন"))
                        }
                    }
                }
            }
        }

        // Custom Added Options granted to this user
        val visibleCustomOptions = state.customPermissionOptions.filter { policy.hasKey(it.permissionKey) }
        if (visibleCustomOptions.isNotEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 2.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            tr("Enabled Custom Options", "চালু থাকা নতুন অপশনসমূহ"),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        visibleCustomOptions.forEach { opt ->
                            val title = if (isBn && opt.titleBn.isNotBlank()) opt.titleBn else opt.titleEn
                            val desc = if (isBn && opt.descriptionBn.isNotBlank()) opt.descriptionBn else opt.descriptionEn
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text(title, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                                    if (desc.isNotBlank()) {
                                        Text(desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Financial & Livestock KPI Grid (2-column pairs)
        if (policy.has(AppPermission.DASHBOARD_OVERVIEW)) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        KpiMetricCard(
                            title = strings.netProfitLoss,
                            value = CurrencyFormatter.formatCompact(netProfit, currency),
                            icon = Icons.Default.TrendingUp,
                            tint = if (netProfit >= 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                            subtitle = if (netProfit >= 0) tr("Profitable", "লাভজনক") else tr("Net Loss", "নিট ক্ষতি"),
                            modifier = Modifier.weight(1f)
                        )
                        KpiMetricCard(
                            title = strings.totalFarms,
                            value = "${state.farms.size}",
                            icon = Icons.Default.Agriculture,
                            subtitle = tr("Multi-farm active", "একাধিক খামার সক্রিয়"),
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        KpiMetricCard(
                            title = strings.totalSales,
                            value = CurrencyFormatter.formatCompact(totalSales, currency),
                            icon = Icons.Default.ShoppingBag,
                            subtitle = "${tr("Paid", "পরিশোধিত")}: ${CurrencyFormatter.formatCompact(totalIncome, currency)}",
                            modifier = Modifier.weight(1f)
                        )
                        KpiMetricCard(
                            title = strings.totalExpenses,
                            value = CurrencyFormatter.formatCompact(totalExpenses, currency),
                            icon = Icons.Default.ReceiptLong,
                            tint = MaterialTheme.colorScheme.error,
                            subtitle = "${tr("Due", "বকেয়া")}: ${CurrencyFormatter.formatCompact(totalDue, currency)}",
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        KpiMetricCard(
                            title = strings.totalAnimals,
                            value = "$totalAnimals",
                            icon = Icons.Default.CrueltyFree,
                            subtitle = "${strings.totalBirds}: $totalBirds",
                            modifier = Modifier.weight(1f)
                        )
                        KpiMetricCard(
                            title = strings.eggsAvailable,
                            value = "$eggsAvailable",
                            icon = Icons.Default.Egg,
                            tint = MaterialTheme.colorScheme.tertiary,
                            subtitle = "${strings.eggsProduced}: $eggsProduced",
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // Charts: Farm-wise Sales vs Expenses + Egg Production Trend
        if (policy.has(AppPermission.DASHBOARD_CHARTS)) {
            if (!subPolicy.canAccess(SubscriptionFeature.DASHBOARD_CHARTS_AI)) {
                item {
                    SubscriptionRequiredLockCard(
                        feature = SubscriptionFeature.DASHBOARD_CHARTS_AI,
                        onOpenSubscription = onOpenSubscription,
                        canSubscribe = canSubscribe
                    )
                }
            } else {
                item {
                    val farmBarEntries = state.farms.take(6).map { farm ->
                        val fSales = state.sales.filter { it.farmId == farm.id }.sumOf { it.totalAmount }
                        val fExp = state.expenses.filter { it.farmId == farm.id }.sumOf { it.amount }
                        DualBarEntry(
                            label = farm.name.take(9),
                            primaryValue = CurrencyFormatter.convertFromBase(fSales, currency),
                            secondaryValue = CurrencyFormatter.convertFromBase(fExp, currency)
                        )
                    }
                    DualBarComparisonChart(
                        title = "${strings.farmPerformance} (${currency.symbol})",
                        primaryLabel = strings.totalSales,
                        secondaryLabel = strings.totalExpenses,
                        entries = farmBarEntries
                    )
                }

                item {
                    val eggPoints = filteredEggs.take(6).reversed().mapIndexed { idx, egg ->
                        "${tr("D", "দিন")}${idx + 1}" to egg.producedCount.toDouble()
                    }
                    SmoothAreaLineChart(
                        title = strings.eggProductionTrend,
                        points = eggPoints
                    )
                }
            }
        }
    }
}

@Composable
private fun QuickActionPill(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
    testTag: String? = null
) {
    ElevatedAssistChip(
        onClick = onClick,
        label = { Text(label, fontWeight = FontWeight.SemiBold) },
        leadingIcon = { Icon(icon, contentDescription = label, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary) },
        modifier = if (testTag != null) Modifier.testTag(testTag) else Modifier
    )
}

@Composable
fun FarmsScreen(
    state: FarmerDashboardState,
    strings: AppStrings,
    currency: CurrencyOption,
    openCreateOnEntry: Boolean = false,
    onCreateEntryConsumed: () -> Unit = {},
    onSelectFarm: (Long?) -> Unit,
    onSaveFarm: (FarmEntity) -> Unit,
    onDeleteFarm: (FarmEntity) -> Unit,
    onOpenSubscription: () -> Unit = {}
) {
    val isBn = isBangla()
    val policy = state.accessPolicy
    val subPolicy = state.subscriptionPolicy
    if (!subPolicy.canAccess(SubscriptionFeature.MULTI_FARM_MANAGEMENT)) {
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
                Text(strings.farms, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                FeaturePlanBadge(isFree = false, hasActiveSub = false)
            }
            SubscriptionRequiredLockCard(
                feature = SubscriptionFeature.MULTI_FARM_MANAGEMENT,
                onOpenSubscription = onOpenSubscription,
                canSubscribe = policy.has(AppPermission.SUBSCRIPTION_MANAGE)
            )
        }
        return
    }

    val canCreateEdit = policy.has(AppPermission.FARMS_CREATE_EDIT)
    val canDelete = policy.has(AppPermission.FARMS_DELETE)
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    var editingFarm by remember { mutableStateOf<FarmEntity?>(null) }
    var showCreateDialog by remember { mutableStateOf(false) }

    LaunchedEffect(openCreateOnEntry, canCreateEdit) {
        if (openCreateOnEntry) {
            if (canCreateEdit) showCreateDialog = true
            onCreateEntryConsumed()
        }
    }

    val filteredFarms = remember(state.farms, searchQuery, selectedCategory) {
        state.farms.filter { farm ->
            val matchesQuery = farm.name.contains(searchQuery, ignoreCase = true) ||
                farm.farmCode.contains(searchQuery, ignoreCase = true) ||
                farm.location.contains(searchQuery, ignoreCase = true)
            val matchesCat = selectedCategory == null || farm.category == selectedCategory
            matchesQuery && matchesCat
        }
    }

    Scaffold(
        floatingActionButton = {
            if (canCreateEdit) {
                ExtendedFloatingActionButton(
                    onClick = { showCreateDialog = true },
                    icon = { Icon(Icons.Default.AddBusiness, contentDescription = strings.addFarm) },
                    text = { Text(strings.addFarm) },
                    modifier = Modifier.testTag("add_farm_fab")
                )
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 88.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "${strings.farms} (${state.farms.size})",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    FeaturePlanBadge(
                        isFree = subPolicy.isFree(SubscriptionFeature.MULTI_FARM_MANAGEMENT),
                        hasActiveSub = subPolicy.hasActiveSubscription
                    )
                }
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text(strings.search) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        FilterChip(
                            selected = selectedCategory == null,
                            onClick = { selectedCategory = null },
                            label = { Text(tr("All", "সব")) }
                        )
                    }
                    items(FarmCategory.entries) { cat ->
                        FilterChip(
                            selected = selectedCategory == cat.name,
                            onClick = { selectedCategory = if (selectedCategory == cat.name) null else cat.name },
                            label = { Text(DomainLocalization.farmCategory(cat.name, "", isBn)) }
                        )
                    }
                }
            }

            if (filteredFarms.isEmpty()) {
                item {
                    EmptyStateCard(
                        icon = Icons.Default.Agriculture,
                        title = tr("No Farms Found", "কোনো খামার পাওয়া যায়নি"),
                        subtitle = tr(
                            "Create unlimited independent farms (1, 10, 50, 100+).",
                            "সীমাহীন স্বাধীন খামার তৈরি করুন (১, ১০, ৫০, ১০০+)।"
                        ),
                        actionLabel = if (canCreateEdit) strings.addFarm else null,
                        onAction = if (canCreateEdit) ({ showCreateDialog = true }) else null
                    )
                }
            } else {
                items(filteredFarms, key = { it.id }) { farm ->
                    val farmAnimals = state.animals.filter { it.farmId == farm.id }.sumOf { it.quantity }
                    val farmSales = state.sales.filter { it.farmId == farm.id }.sumOf { it.totalAmount }
                    val farmIncome = state.sales.filter { it.farmId == farm.id }.sumOf { it.paidAmount }
                    val farmExpenses = state.expenses.filter { it.farmId == farm.id }.sumOf { it.amount }
                    val farmNet = farmSales - farmExpenses
                    val isActive = state.selectedFarmId == farm.id
                    val catDisplay = DomainLocalization.farmCategory(farm.category, farm.customCategoryName, isBn)

                    Surface(
                        shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = if (isActive) 4.dp else 2.dp,
                        border = if (isActive) ButtonDefaults.outlinedButtonBorder(true) else null,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column {
                            // Farm Banner Image
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(110.dp)
                            ) {
                                if (!farm.imageUri.isNullOrBlank()) {
                                    AsyncImage(
                                        model = farm.imageUri,
                                        contentDescription = farm.name,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    val fallbackRes = when (farm.category) {
                                        FarmCategory.DAIRY.name, FarmCategory.CATTLE.name, FarmCategory.GOAT.name -> R.drawable.img_dairy_cattle
                                        else -> R.drawable.img_poultry_farm
                                    }
                                    Image(
                                        painter = painterResource(id = fallbackRes),
                                        contentDescription = farm.name,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                                Surface(
                                    color = Color.Black.copy(alpha = 0.62f),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier
                                        .align(Alignment.TopStart)
                                        .padding(10.dp)
                                ) {
                                    Text(
                                        "${farm.farmCode} • $catDisplay",
                                        color = Color.White,
                                        style = MaterialTheme.typography.labelSmall,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                                if (isActive) {
                                    Surface(
                                        color = MaterialTheme.colorScheme.primary,
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(10.dp)
                                    ) {
                                        Text(
                                            strings.active,
                                            color = MaterialTheme.colorScheme.onPrimary,
                                            style = MaterialTheme.typography.labelSmall,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }

                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(farm.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                                        Text("${farm.location} • ${farm.address}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    if (canCreateEdit || canDelete) {
                                        Row {
                                            if (canCreateEdit) {
                                                IconButton(onClick = { editingFarm = farm }) {
                                                    Icon(Icons.Default.Edit, contentDescription = strings.edit)
                                                }
                                            }
                                            if (canDelete) {
                                                IconButton(onClick = { onDeleteFarm(farm) }) {
                                                    Icon(Icons.Default.DeleteOutline, contentDescription = strings.delete, tint = MaterialTheme.colorScheme.error)
                                                }
                                            }
                                        }
                                    }
                                }

                                Text(
                                    "${tr("Manager", "ম্যানেজার")}: ${farm.managerName.ifBlank { tr("Unassigned", "নির্ধারিত নয়") }} (${farm.managerPhone.ifBlank { "-" }}) • ${tr("Opened", "শুরু")} ${formatShortDate(farm.openingDate)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    FarmStatChip(tr("Livestock", "পশুপাখি"), "$farmAnimals")
                                    FarmStatChip(tr("Investment", "বিনিয়োগ"), CurrencyFormatter.formatCompact(farm.initialInvestment, currency))
                                    FarmStatChip(tr("Income", "আয়"), CurrencyFormatter.formatCompact(farmIncome, currency))
                                    FarmStatChip(tr("Expenses", "খরচ"), CurrencyFormatter.formatCompact(farmExpenses, currency))
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        "${tr("Net P/L", "নিট লাভ/ক্ষতি")}: ${CurrencyFormatter.format(farmNet, currency)}",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (farmNet >= 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                                    )
                                    FilledTonalButton(onClick = { onSelectFarm(if (isActive) null else farm.id) }) {
                                        Text(if (isActive) tr("Show All Farms", "সকল খামার দেখুন") else tr("Focus This Farm", "এই খামার দেখুন"))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreateDialog || editingFarm != null) {
        FarmEditorDialog(
            initial = editingFarm,
            currency = currency,
            onDismiss = { showCreateDialog = false; editingFarm = null },
            onSave = { farm ->
                onSaveFarm(farm)
                showCreateDialog = false
                editingFarm = null
            }
        )
    }
}

@Composable
private fun FarmStatChip(label: String, value: String) {
    Column {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun FarmEditorDialog(
    initial: FarmEntity?,
    currency: CurrencyOption,
    onDismiss: () -> Unit,
    onSave: (FarmEntity) -> Unit
) {
    val isBn = isBangla()
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var farmCode by remember { mutableStateOf(initial?.farmCode ?: "FM-${(1000..9999).random()}") }
    var location by remember { mutableStateOf(initial?.location ?: "") }
    var address by remember { mutableStateOf(initial?.address ?: "") }
    var category by remember { mutableStateOf(initial?.category ?: FarmCategory.POULTRY.name) }
    var customCat by remember { mutableStateOf(initial?.customCategoryName ?: "") }
    var managerName by remember { mutableStateOf(initial?.managerName ?: "") }
    var managerPhone by remember { mutableStateOf(initial?.managerPhone ?: "") }
    var investmentText by remember {
        mutableStateOf(
            if (initial != null && initial.initialInvestment > 0)
                CurrencyFormatter.convertFromBase(initial.initialInvestment, currency).toInt().toString()
            else ""
        )
    }
    var imageUri by remember { mutableStateOf(initial?.imageUri) }
    var showNameError by remember { mutableStateOf(false) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri: Uri? ->
        if (uri != null) imageUri = uri.toString()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (initial == null) tr("Create New Farm", "নতুন খামার তৈরি করুন")
                else tr("Edit Farm", "খামার সম্পাদনা করুন")
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 440.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { safeLaunchImagePicker(picker) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.PhotoLibrary, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        if (imageUri == null) tr("Choose Farm Image", "খামারের ছবি নির্বাচন করুন")
                        else tr("Change Selected Image", "নির্বাচিত ছবি পরিবর্তন করুন")
                    )
                }
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        if (it.isNotBlank()) showNameError = false
                    },
                    label = { Text(tr("Farm Name *", "খামারের নাম *")) },
                    isError = showNameError && name.isBlank(),
                    supportingText = if (showNameError && name.isBlank()) {
                        { Text(tr("Please enter a farm name", "অনুগ্রহ করে খামারের নাম লিখুন")) }
                    } else null,
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("farm_name_input")
                )
                OutlinedTextField(
                    value = farmCode,
                    onValueChange = { farmCode = it },
                    label = { Text(tr("Farm ID", "খামার আইডি")) },
                    trailingIcon = {
                        IconButton(onClick = { farmCode = "FM-${(1000..9999).random()}" }) {
                            Icon(Icons.Default.Refresh, contentDescription = tr("Generate new Farm ID", "নতুন খামার আইডি তৈরি করুন"))
                        }
                    },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("farm_code_input")
                )
                OutlinedTextField(
                    value = location,
                    onValueChange = { location = it },
                    label = { Text(tr("Location (City/District)", "অবস্থান (শহর/জেলা)")) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("farm_location_input")
                )
                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text(tr("Full Address", "সম্পূর্ণ ঠিকানা")) },
                    modifier = Modifier.fillMaxWidth()
                )

                Text(tr("Farm Category", "খামারের ধরন"), style = MaterialTheme.typography.labelLarge)
                @OptIn(ExperimentalLayoutApi::class)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FarmCategory.entries.forEach { cat ->
                        FilterChip(
                            selected = category == cat.name,
                            onClick = { category = cat.name },
                            label = { Text(DomainLocalization.farmCategory(cat.name, "", isBn)) }
                        )
                    }
                }
                if (category == FarmCategory.CUSTOM.name) {
                    OutlinedTextField(
                        value = customCat,
                        onValueChange = { customCat = it },
                        label = { Text(tr("Custom Category Name", "কাস্টম ধরনের নাম")) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                OutlinedTextField(
                    value = managerName,
                    onValueChange = { managerName = it },
                    label = { Text(tr("Manager Name", "ম্যানেজারের নাম")) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = managerPhone,
                    onValueChange = { managerPhone = it },
                    label = { Text(tr("Manager Phone", "ম্যানেজারের মোবাইল")) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = investmentText,
                    onValueChange = { investmentText = it },
                    label = { Text("${tr("Initial Investment", "প্রাথমিক বিনিয়োগ")} (${currency.symbol})") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isBlank()) {
                        showNameError = true
                        return@Button
                    }
                    val baseInvestment = CurrencyFormatter.toBaseBdt(investmentText.toDoubleOrNull() ?: 0.0, currency)
                    onSave(
                        (initial ?: FarmEntity(ownerUserId = 0L, farmCode = farmCode, name = name, location = location, address = address)).copy(
                            name = name.trim(),
                            farmCode = farmCode.trim(),
                            location = location.trim(),
                            address = address.trim(),
                            category = category,
                            customCategoryName = customCat.trim(),
                            managerName = managerName.trim(),
                            managerPhone = managerPhone.trim(),
                            initialInvestment = baseInvestment,
                            imageUri = imageUri
                        )
                    )
                },
                modifier = Modifier.testTag("save_farm_button")
            ) { Text(tr("Save Farm", "খামার সংরক্ষণ করুন")) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(tr("Cancel", "বাতিল")) } }
    )
}
