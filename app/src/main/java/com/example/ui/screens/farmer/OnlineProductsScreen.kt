package com.example.ui.screens.farmer

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.local.*
import com.example.i18n.AppStrings
import com.example.i18n.CurrencyFormatter
import com.example.i18n.CurrencyOption
import com.example.i18n.DomainLocalization
import com.example.i18n.isBangla
import com.example.i18n.tr
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.FarmSwitcherBar
import com.example.ui.components.FeaturePlanBadge
import com.example.ui.components.KpiMetricCard
import com.example.ui.components.SubscriptionRequiredLockCard
import com.example.ui.components.formatShortDate
import com.example.ui.components.safeLaunchImagePicker
import com.example.ui.state.FarmerDashboardState

private enum class OnlineFilterMode {
    ALL, LIVE, PAUSED, FARM_PRODUCED, PURCHASED_RESALE
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OnlineProductsScreen(
    state: FarmerDashboardState,
    strings: AppStrings,
    currency: CurrencyOption,
    openAddOnEntry: Boolean = false,
    onAddEntryConsumed: () -> Unit = {},
    onSelectFarm: (Long?) -> Unit,
    onSaveOnlineProduct: (InventoryItemEntity) -> Unit,
    onToggleLive: (InventoryItemEntity, Boolean) -> Unit,
    onDeleteProduct: (InventoryItemEntity) -> Unit,
    onOpenAddFarm: () -> Unit,
    onOpenOrders: () -> Unit,
    onOpenSubscription: () -> Unit
) {
    val isBn = isBangla()
    val policy = state.accessPolicy
    val subPolicy = state.subscriptionPolicy

    if (!policy.hasAny(AppPermission.ONLINE_PRODUCTS_VIEW, AppPermission.ONLINE_PRODUCTS_MANAGE)) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            EmptyStateCard(
                icon = Icons.Default.Lock,
                title = tr("Online Store Access Restricted", "অনলাইন পণ্য অপশনের অনুমতি নেই"),
                subtitle = tr(
                    "Ask your Farm Admin to grant Online Store Products access.",
                    "অনলাইন পণ্য দেখা ও যোগ করার জন্য খামার অ্যাডমিনের অনুমতি নিন।"
                )
            )
        }
        return
    }

    if (!subPolicy.canAccess(SubscriptionFeature.ONLINE_PRODUCT_STORE)) {
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
                Text(strings.onlineProducts, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                FeaturePlanBadge(isFree = false, hasActiveSub = false)
            }
            SubscriptionRequiredLockCard(
                feature = SubscriptionFeature.ONLINE_PRODUCT_STORE,
                onOpenSubscription = onOpenSubscription,
                canSubscribe = policy.has(AppPermission.SUBSCRIPTION_MANAGE)
            )
        }
        return
    }

    val canManage = policy.has(AppPermission.ONLINE_PRODUCTS_MANAGE)
    var showProductDialog by remember { mutableStateOf(false) }
    var editingProduct by remember { mutableStateOf<InventoryItemEntity?>(null) }
    var deleteCandidate by remember { mutableStateOf<InventoryItemEntity?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var filterMode by remember { mutableStateOf(OnlineFilterMode.ALL) }

    LaunchedEffect(openAddOnEntry, canManage) {
        if (openAddOnEntry) {
            if (canManage && state.farms.isNotEmpty()) {
                editingProduct = null
                showProductDialog = true
            }
            onAddEntryConsumed()
        }
    }

    val activeFarmId = state.selectedFarmId
    val farmsById = remember(state.farms) { state.farms.associateBy { it.id } }

    // Items relevant to online selling: listed in marketplace, or priced > 0, or matching agri crop categories
    val farmScopedItems = remember(state.inventory, activeFarmId) {
        if (activeFarmId == null) state.inventory else state.inventory.filter { it.farmId == activeFarmId }
    }
    val storeCandidates = remember(farmScopedItems) {
        farmScopedItems.filter { it.listInMarketplace || it.sellingPrice > 0.0 }
    }

    val displayedProducts = remember(storeCandidates, searchQuery, filterMode) {
        storeCandidates.filter { item ->
            val q = searchQuery.trim()
            val matchesSearch = q.isBlank() ||
                item.name.contains(q, ignoreCase = true) ||
                item.category.contains(q, ignoreCase = true) ||
                item.description.contains(q, ignoreCase = true)
            val matchesFilter = when (filterMode) {
                OnlineFilterMode.ALL -> true
                OnlineFilterMode.LIVE -> item.listInMarketplace && item.quantity > 0.0
                OnlineFilterMode.PAUSED -> !item.listInMarketplace || item.quantity <= 0.0
                OnlineFilterMode.FARM_PRODUCED -> item.sourcingType != ProductSourcingType.PURCHASED_RESALE.key
                OnlineFilterMode.PURCHASED_RESALE -> item.sourcingType == ProductSourcingType.PURCHASED_RESALE.key
            }
            matchesSearch && matchesFilter
        }
    }

    val liveCount = storeCandidates.count { it.listInMarketplace && it.quantity > 0.0 }
    val catalogValue = storeCandidates.filter { it.listInMarketplace }.sumOf { it.quantity * it.sellingPrice }
    val scopedOrders = remember(state.incomingOrders, activeFarmId) {
        if (activeFarmId == null) state.incomingOrders else state.incomingOrders.filter { it.farmId == activeFarmId }
    }
    val pendingOrders = scopedOrders.filter { it.status == OrderStatus.PENDING.name }

    Scaffold(
        floatingActionButton = {
            if (canManage && state.farms.isNotEmpty()) {
                ExtendedFloatingActionButton(
                    onClick = {
                        editingProduct = null
                        showProductDialog = true
                    },
                    icon = { Icon(Icons.Default.AddShoppingCart, contentDescription = tr("Add Online Product", "অনলাইন পণ্য যোগ করুন")) },
                    text = { Text(tr("Add Online Product", "অনলাইন পণ্য যোগ করুন")) },
                    modifier = Modifier.testTag("add_online_product_fab")
                )
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 92.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item(key = "online_header") {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                tr("Online Product Add", "অনলাইন পণ্য যোগ ও বিক্রয়"),
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                tr(
                                    "Farm Business Storefront • Rice, Wheat, Mustard, Crops, Dairy & More",
                                    "খামার ও কৃষি ব্যবসা স্টোর • চাল, গম, সরিষা, ফসল ও খামার পণ্য বিক্রি করুন"
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        FeaturePlanBadge(
                            isFree = subPolicy.isFree(SubscriptionFeature.ONLINE_PRODUCT_STORE),
                            hasActiveSub = subPolicy.hasActiveSubscription
                        )
                    }

                    if (state.farms.isNotEmpty()) {
                        FarmSwitcherBar(
                            farms = state.farms,
                            selectedFarmId = state.selectedFarmId,
                            allFarmsLabel = strings.allFarms,
                            onSelectFarm = onSelectFarm
                        )
                    }
                }
            }

            if (state.farms.isEmpty()) {
                item(key = "online_no_farm") {
                    EmptyStateCard(
                        icon = Icons.Default.AddBusiness,
                        title = tr("Create a Farm Business First", "প্রথমে একটি খামার বা ব্যবসা যোগ করুন"),
                        subtitle = tr(
                            "Every online product belongs to a Farm Business account so customer orders and notifications route directly to the right farm.",
                            "প্রতিটি অনলাইন পণ্য আপনার খামার অ্যাকাউন্টের সাথে যুক্ত থাকে যাতে ক্রেতার অর্ডার ও নোটিফিকেশন সরাসরি সেই খামারে পৌঁছায়।"
                        ),
                        actionLabel = if (policy.has(AppPermission.FARMS_CREATE_EDIT)) strings.addFarm else null,
                        onAction = if (policy.has(AppPermission.FARMS_CREATE_EDIT)) onOpenAddFarm else null
                    )
                }
            } else {
                // Pending Farm Orders Alert Banner
                if (pendingOrders.isNotEmpty() && policy.has(AppPermission.ORDERS_VIEW)) {
                    item(key = "online_pending_orders_banner") {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.78f),
                            tonalElevation = 2.dp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("online_pending_orders_banner")
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = MaterialTheme.colorScheme.tertiary,
                                        modifier = Modifier.size(40.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                Icons.Default.NotificationsActive,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onTertiary
                                            )
                                        }
                                    }
                                    Column {
                                        Text(
                                            tr(
                                                "${pendingOrders.size} New Online Order(s) Received!",
                                                "${pendingOrders.size}টি নতুন অনলাইন অর্ডার এসেছে!"
                                            ),
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onTertiaryContainer
                                        )
                                        val latest = pendingOrders.first()
                                        Text(
                                            "${latest.orderNumber} • ${latest.customerName} (${latest.farmName})",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.85f),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                                FilledTonalButton(
                                    onClick = onOpenOrders,
                                    modifier = Modifier.testTag("online_manage_orders_button")
                                ) {
                                    Text(tr("Orders", "অর্ডার"))
                                }
                            }
                        }
                    }
                }

                // KPI Summary Row
                item(key = "online_kpi") {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        KpiMetricCard(
                            title = tr("Live Products", "সক্রিয় পণ্য"),
                            value = "$liveCount",
                            icon = Icons.Default.Storefront,
                            subtitle = tr("${storeCandidates.size} total listed", "মোট ${storeCandidates.size}টি তালিকাভুক্ত"),
                            modifier = Modifier.weight(1f)
                        )
                        KpiMetricCard(
                            title = tr("Store Value", "অনলাইন মজুদ মূল্য"),
                            value = CurrencyFormatter.formatCompact(catalogValue, currency),
                            icon = Icons.Default.Sell,
                            tint = MaterialTheme.colorScheme.secondary,
                            subtitle = tr("${scopedOrders.size} total orders", "মোট ${scopedOrders.size}টি অর্ডার"),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Search & Filter Controls
                item(key = "online_filters") {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = {
                                Text(
                                    tr(
                                        "Search rice, wheat, mustard, eggs, or farm item...",
                                        "চাল, গম, সরিষা, ডিম বা যেকোনো পণ্য খুঁজুন..."
                                    )
                                )
                            },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { searchQuery = "" }) {
                                        Icon(Icons.Default.Close, contentDescription = tr("Clear", "মুছুন"))
                                    }
                                }
                            },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("online_products_search")
                        )

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            FilterChip(
                                selected = filterMode == OnlineFilterMode.ALL,
                                onClick = { filterMode = OnlineFilterMode.ALL },
                                label = { Text(tr("All (${storeCandidates.size})", "সব (${storeCandidates.size})")) }
                            )
                            FilterChip(
                                selected = filterMode == OnlineFilterMode.LIVE,
                                onClick = { filterMode = OnlineFilterMode.LIVE },
                                label = { Text(tr("Live ($liveCount)", "লাইভ ($liveCount)")) }
                            )
                            FilterChip(
                                selected = filterMode == OnlineFilterMode.PAUSED,
                                onClick = { filterMode = OnlineFilterMode.PAUSED },
                                label = { Text(tr("Paused", "বিরতিতে")) }
                            )
                            FilterChip(
                                selected = filterMode == OnlineFilterMode.FARM_PRODUCED,
                                onClick = { filterMode = OnlineFilterMode.FARM_PRODUCED },
                                label = { Text(tr("Own Harvest", "নিজস্ব উৎপাদন")) }
                            )
                            FilterChip(
                                selected = filterMode == OnlineFilterMode.PURCHASED_RESALE,
                                onClick = { filterMode = OnlineFilterMode.PURCHASED_RESALE },
                                label = { Text(tr("Bought & Resold", "ক্রয়-পুনর্বিক্রয়")) }
                            )
                        }
                    }
                }

                if (displayedProducts.isEmpty()) {
                    item(key = "online_empty_list") {
                        EmptyStateCard(
                            icon = Icons.Default.Inventory,
                            title = tr("No Online Products Listed Yet", "এখনও কোনো অনলাইন পণ্য যোগ হয়নি"),
                            subtitle = tr(
                                "Tap 'Add Online Product' to sell Rice, Wheat, Mustard, Vegetables, Eggs, Milk, Meat, or bought agri goods directly to customers.",
                                "গ্রাহকদের কাছে চাল, গম, সরিষা, শাকসবজি, ডিম, দুধ বা ক্রয়কৃত কৃষি পণ্য বিক্রি করতে 'অনলাইন পণ্য যোগ করুন' বোতামে চাপ দিন।"
                            ),
                            actionLabel = if (canManage) tr("Add Online Product", "অনলাইন পণ্য যোগ করুন") else null,
                            onAction = if (canManage) {
                                {
                                    editingProduct = null
                                    showProductDialog = true
                                }
                            } else null
                        )
                    }
                } else {
                    items(displayedProducts, key = { "online_prod_${it.id}" }) { item ->
                        val farm = farmsById[item.farmId]
                        val farmLabel = farm?.name ?: tr("Farm", "খামার")
                        val isLive = item.listInMarketplace && item.quantity > 0.0
                        val isResale = item.sourcingType == ProductSourcingType.PURCHASED_RESALE.key

                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surface,
                            tonalElevation = 2.dp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("online_product_card_${item.id}")
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    if (!item.imageUri.isNullOrBlank()) {
                                        AsyncImage(
                                            model = item.imageUri,
                                            contentDescription = item.name,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier
                                                .size(64.dp)
                                                .clip(RoundedCornerShape(12.dp))
                                        )
                                    } else {
                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = MaterialTheme.colorScheme.primaryContainer,
                                            modifier = Modifier.size(56.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    if (isResale) Icons.Default.LocalMall else Icons.Default.Grass,
                                                    contentDescription = item.category,
                                                    tint = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        }
                                    }

                                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                item.name,
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.weight(1f)
                                            )
                                            Spacer(Modifier.width(8.dp))
                                            Text(
                                                CurrencyFormatter.format(item.sellingPrice, currency),
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }

                                        FlowRow(
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            verticalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            AssistChip(
                                                onClick = {},
                                                label = {
                                                    Text(
                                                        DomainLocalization.productCategory(item.category, isBn),
                                                        style = MaterialTheme.typography.labelSmall
                                                    )
                                                }
                                            )
                                            Surface(
                                                shape = RoundedCornerShape(50.dp),
                                                color = if (isResale) MaterialTheme.colorScheme.secondaryContainer
                                                else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                                            ) {
                                                Text(
                                                    DomainLocalization.sourcingType(item.sourcingType, isBn),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.SemiBold,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                )
                                            }
                                            Surface(
                                                shape = RoundedCornerShape(50.dp),
                                                color = if (isLive) MaterialTheme.colorScheme.primaryContainer
                                                else MaterialTheme.colorScheme.surfaceVariant
                                            ) {
                                                Text(
                                                    if (isLive) tr("LIVE", "লাইভ")
                                                    else if (item.quantity <= 0.0) tr("OUT OF STOCK", "স্টক শেষ")
                                                    else tr("PAUSED", "বিরতি"),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isLive) MaterialTheme.colorScheme.primary
                                                    else MaterialTheme.colorScheme.onSurfaceVariant,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                )
                                            }
                                        }
                                    }
                                }

                                if (item.description.isNotBlank()) {
                                    Text(
                                        item.description,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            "$farmLabel • ${tr("Stock", "মজুদ")}: ${item.quantity.toInt()} ${item.unit}",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        if (item.unitCost > 0.0) {
                                            val margin = item.sellingPrice - item.unitCost
                                            Text(
                                                "${tr("Buy Cost", "ক্রয়মূল্য")}: ${CurrencyFormatter.format(item.unitCost, currency)} • ${tr("Margin", "লাভ")}: ${CurrencyFormatter.format(margin, currency)}/${item.unit}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = if (margin >= 0) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.error
                                            )
                                        }
                                        Text(
                                            "${tr("Updated", "আপডেট")}: ${formatShortDate(item.updatedAt)}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    if (canManage) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Switch(
                                                checked = item.listInMarketplace,
                                                onCheckedChange = { checked -> onToggleLive(item, checked) },
                                                modifier = Modifier.testTag("online_product_live_switch_${item.id}")
                                            )
                                            IconButton(
                                                onClick = {
                                                    editingProduct = item
                                                    showProductDialog = true
                                                }
                                            ) {
                                                Icon(Icons.Default.Edit, contentDescription = strings.edit)
                                            }
                                            IconButton(onClick = { deleteCandidate = item }) {
                                                Icon(
                                                    Icons.Default.DeleteOutline,
                                                    contentDescription = strings.delete,
                                                    tint = MaterialTheme.colorScheme.error
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
    }

    if (showProductDialog && state.farms.isNotEmpty()) {
        OnlineProductEditorDialog(
            existing = editingProduct,
            farms = state.farms,
            defaultFarmId = state.selectedFarmId ?: state.farms.first().id,
            currency = currency,
            strings = strings,
            onDismiss = { showProductDialog = false; editingProduct = null },
            onSave = { saved ->
                onSaveOnlineProduct(saved)
                showProductDialog = false
                editingProduct = null
            }
        )
    }

    deleteCandidate?.let { victim ->
        AlertDialog(
            onDismissRequest = { deleteCandidate = null },
            title = { Text(tr("Remove Online Product?", "অনলাইন পণ্য মুছে ফেলবেন?")) },
            text = {
                Text(
                    tr(
                        "Remove '${victim.name}' from your catalog? This cannot be undone.",
                        "'${victim.name}' তালিকা থেকে মুছে ফেলা হবে?"
                    )
                )
            },
            confirmButton = {
                Button(
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    onClick = {
                        onDeleteProduct(victim)
                        deleteCandidate = null
                    }
                ) { Text(strings.delete) }
            },
            dismissButton = {
                TextButton(onClick = { deleteCandidate = null }) { Text(strings.cancel) }
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun OnlineProductEditorDialog(
    existing: InventoryItemEntity?,
    farms: List<FarmEntity>,
    defaultFarmId: Long,
    currency: CurrencyOption,
    strings: AppStrings,
    onDismiss: () -> Unit,
    onSave: (InventoryItemEntity) -> Unit
) {
    val isBn = isBangla()
    var farmId by remember(existing?.id) { mutableStateOf(existing?.farmId ?: defaultFarmId) }
    var sourcingType by remember(existing?.id) {
        mutableStateOf(existing?.sourcingType ?: ProductSourcingType.FARM_PRODUCED.key)
    }
    val builtInCategories = AgriProductCatalog.onlineCategories
    val initialCat = existing?.category ?: "Rice"
    val isPresetCat = builtInCategories.contains(initialCat)

    var selectedCategory by remember(existing?.id) {
        mutableStateOf(if (isPresetCat) initialCat else "Custom")
    }
    var customCategory by remember(existing?.id) {
        mutableStateOf(if (isPresetCat) "" else initialCat)
    }
    var name by remember(existing?.id) { mutableStateOf(existing?.name ?: "") }
    var qtyText by remember(existing?.id) {
        mutableStateOf(existing?.quantity?.let { if (it % 1.0 == 0.0) it.toInt().toString() else it.toString() } ?: "50")
    }
    var unit by remember(existing?.id) { mutableStateOf(existing?.unit?.ifBlank { "kg" } ?: "kg") }
    var costText by remember(existing?.id) {
        mutableStateOf(
            existing?.unitCost?.takeIf { it > 0.0 }
                ?.let { CurrencyFormatter.convertFromBase(it, currency).toInt().toString() } ?: ""
        )
    }
    var priceText by remember(existing?.id) {
        mutableStateOf(
            existing?.sellingPrice?.takeIf { it > 0.0 }
                ?.let { CurrencyFormatter.convertFromBase(it, currency).toInt().toString() } ?: ""
        )
    }
    var supplierName by remember(existing?.id) { mutableStateOf(existing?.supplierName ?: "") }
    var supplierPhone by remember(existing?.id) { mutableStateOf(existing?.supplierPhone ?: "") }
    var description by remember(existing?.id) { mutableStateOf(existing?.description ?: "") }
    var imageUri by remember(existing?.id) { mutableStateOf(existing?.imageUri) }
    var listLive by remember(existing?.id) { mutableStateOf(existing?.listInMarketplace ?: true) }

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri: Uri? ->
        if (uri != null) imageUri = uri.toString()
    }

    val effectiveCategory = if (selectedCategory == "Custom") customCategory.trim() else selectedCategory
    val parsedPrice = priceText.toDoubleOrNull() ?: 0.0
    val parsedQty = qtyText.toDoubleOrNull() ?: 0.0
    val canSave = name.isNotBlank() && effectiveCategory.isNotBlank() && parsedPrice > 0.0 && parsedQty >= 0.0 && farmId > 0L

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (existing == null) tr("Add Online Product", "নতুন অনলাইন পণ্য যোগ করুন")
                else tr("Edit Online Product", "অনলাইন পণ্য সম্পাদনা")
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 480.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Target Farm Account Selection (Orders & Alerts route to this Farm)
                Text(tr("Selling Farm / Business *", "বিক্রয়কারী খামার / ব্যবসা *"), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    farms.forEach { farm ->
                        FilterChip(
                            selected = farmId == farm.id,
                            onClick = { farmId = farm.id },
                            label = { Text("${farm.name} (${farm.farmCode})") }
                        )
                    }
                }

                // Business Sourcing Model: Own Harvest vs Bought & Resold
                Text(tr("Business Sourcing Type *", "পণ্যের উৎস ও ব্যবসার ধরন *"), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    ProductSourcingType.entries.forEach { type ->
                        FilterChip(
                            selected = sourcingType == type.key,
                            onClick = { sourcingType = type.key },
                            label = { Text(DomainLocalization.sourcingType(type.key, isBn)) }
                        )
                    }
                }

                // Product Photo Picker
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (!imageUri.isNullOrBlank()) {
                        AsyncImage(
                            model = imageUri,
                            contentDescription = name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(52.dp)
                                .clip(RoundedCornerShape(10.dp))
                        )
                    }
                    OutlinedButton(
                        onClick = { safeLaunchImagePicker(imagePicker) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.PhotoCamera, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(
                            if (imageUri == null) tr("Add Product Photo", "পণ্যের ছবি যোগ করুন")
                            else tr("Change Photo", "ছবি পরিবর্তন করুন")
                        )
                    }
                }

                // Agricultural Category Selection (Rice, Wheat, Mustard, etc. + Custom)
                Text(tr("Agricultural Category *", "কৃষি পণ্যের ক্যাটাগরি *"), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    builtInCategories.forEach { cat ->
                        FilterChip(
                            selected = selectedCategory == cat,
                            onClick = { selectedCategory = cat },
                            label = { Text(DomainLocalization.productCategory(cat, isBn)) }
                        )
                    }
                    FilterChip(
                        selected = selectedCategory == "Custom",
                        onClick = { selectedCategory = "Custom" },
                        label = { Text(tr("+ Custom Category", "+ অন্যান্য / কাস্টম")) }
                    )
                }

                if (selectedCategory == "Custom") {
                    OutlinedTextField(
                        value = customCategory,
                        onValueChange = { customCategory = it },
                        label = { Text(tr("Custom Product Category *", "কাস্টম ক্যাটাগরির নাম *")) },
                        placeholder = { Text(tr("e.g., Jute, Potato, Mango, Onion", "যেমন: পাট, আলু, আম, পেঁয়াজ")) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(tr("Product Name *", "পণ্যের নাম *")) },
                    placeholder = { Text(tr("e.g., Kataribhog Rice 5kg / Mustard Oil 1L", "যেমন: কাটারিভোগ চাল ৫ কেজি / খাঁটি সরিষার তেল")) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("online_product_name_input")
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = qtyText,
                        onValueChange = { qtyText = it },
                        label = { Text(tr("Stock Qty *", "মজুদ পরিমাণ *")) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("online_product_qty_input")
                    )
                    OutlinedTextField(
                        value = unit,
                        onValueChange = { unit = it },
                        label = { Text(tr("Unit", "একক")) },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    AgriProductCatalog.units.forEach { u ->
                        FilterChip(
                            selected = unit.equals(u, ignoreCase = true),
                            onClick = { unit = u },
                            label = { Text(u) }
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = costText,
                        onValueChange = { costText = it },
                        label = { Text("${tr("Buy / Cost Price", "ক্রয় / উৎপাদন খরচ")} (${currency.symbol})") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = priceText,
                        onValueChange = { priceText = it },
                        label = { Text("${tr("Online Price *", "অনলাইন বিক্রয় মূল্য *")} (${currency.symbol})") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("online_product_price_input")
                    )
                }

                OutlinedTextField(
                    value = supplierName,
                    onValueChange = { supplierName = it },
                    label = { Text(tr("Grower / Supplier / Origin (Optional)", "উৎপাদক / সরবরাহকারীর নাম (ঐচ্ছিক)")) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = supplierPhone,
                    onValueChange = { supplierPhone = it },
                    label = { Text(tr("Contact Phone (Optional)", "যোগাযোগের মোবাইল (ঐচ্ছিক)")) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text(tr("Product Description & Quality Details", "পণ্যের বিবরণ ও গুণগত মান")) },
                    modifier = Modifier.fillMaxWidth()
                )

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                tr("Publish Live on Online Marketplace", "অনলাইন মার্কেটপ্লেসে সরাসরি প্রকাশ করুন"),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                tr(
                                    "Customers can order immediately & your farm receives alerts.",
                                    "ক্রেতারা অর্ডার করলে সরাসরি আপনার খামারে অর্ডার ও নোটিফিকেশন আসবে।"
                                ),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = listLive,
                            onCheckedChange = { listLive = it },
                            modifier = Modifier.testTag("online_product_publish_switch")
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                enabled = canSave,
                onClick = {
                    val item = (existing ?: InventoryItemEntity(
                        farmId = farmId,
                        ownerUserId = 0L,
                        name = name.trim(),
                        category = effectiveCategory
                    )).copy(
                        farmId = farmId,
                        name = name.trim(),
                        category = effectiveCategory,
                        quantity = parsedQty.coerceAtLeast(0.0),
                        unit = unit.trim().ifBlank { "kg" },
                        unitCost = CurrencyFormatter.toBaseBdt(costText.toDoubleOrNull() ?: 0.0, currency),
                        sellingPrice = CurrencyFormatter.toBaseBdt(parsedPrice, currency),
                        lowStockThreshold = existing?.lowStockThreshold ?: 5.0,
                        supplierName = supplierName.trim(),
                        supplierPhone = supplierPhone.trim(),
                        listInMarketplace = listLive,
                        description = description.trim(),
                        imageUri = imageUri,
                        sourcingType = sourcingType
                    )
                    onSave(item)
                },
                modifier = Modifier.testTag("save_online_product_button")
            ) {
                Text(strings.save)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(strings.cancel) }
        }
    )
}
