package com.example.ui.screens.farmer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
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

private enum class AgriTab {
    OVERVIEW_CROPS, LAND_PLOTS, SECTOR_EXPENSES
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AgricultureScreen(
    state: FarmerDashboardState,
    strings: AppStrings,
    currency: CurrencyOption,
    onSelectFarm: (Long?) -> Unit,
    onSaveLand: (AgriLandEntity) -> Unit,
    onDeleteLand: (AgriLandEntity) -> Unit,
    onSaveCrop: (AgriCropEntity) -> Unit,
    onRecordHarvest: (AgriCropEntity, Double, String, Double, Double, Double, String) -> Unit,
    onDeleteCrop: (AgriCropEntity) -> Unit,
    onAddAgriExpense: (landId: Long, cropId: Long?, farmId: Long?, sector: String, customSector: String, amount: Double, desc: String) -> Unit,
    onDeleteAgriExpense: (AgriExpenseEntity) -> Unit,
    onOpenSubscription: () -> Unit
) {
    val isBn = isBangla()
    val policy = state.accessPolicy
    val subPolicy = state.subscriptionPolicy

    if (!policy.hasAny(AppPermission.AGRICULTURE_VIEW, AppPermission.AGRICULTURE_MANAGE)) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            EmptyStateCard(
                icon = Icons.Default.Lock,
                title = tr("Agriculture Access Restricted", "কৃষি হিসাব অপশনের অনুমতি নেই"),
                subtitle = tr(
                    "Ask your Farm Admin to grant Agriculture, Crops & Land Ledger access.",
                    "কৃষি, ফসল ও জমির হিসাব দেখতে খামার অ্যাডমিনের অনুমতি নিন।"
                )
            )
        }
        return
    }

    if (!subPolicy.canAccess(SubscriptionFeature.AGRICULTURE_CROP_LEDGER)) {
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
                Text(strings.agriculture, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                FeaturePlanBadge(isFree = false, hasActiveSub = false)
            }
            SubscriptionRequiredLockCard(
                feature = SubscriptionFeature.AGRICULTURE_CROP_LEDGER,
                onOpenSubscription = onOpenSubscription,
                canSubscribe = policy.has(AppPermission.SUBSCRIPTION_MANAGE)
            )
        }
        return
    }

    val canManage = policy.has(AppPermission.AGRICULTURE_MANAGE)
    val canDelete = policy.has(AppPermission.AGRICULTURE_DELETE)

    var selectedTab by remember { mutableStateOf(AgriTab.OVERVIEW_CROPS) }
    var selectedCropTypeFilter by remember { mutableStateOf<String?>(null) }
    var selectedSectorFilter by remember { mutableStateOf<String?>(null) }
    var selectedLandFilterId by remember { mutableStateOf<Long?>(null) }

    var showLandDialog by remember { mutableStateOf(false) }
    var editingLand by remember { mutableStateOf<AgriLandEntity?>(null) }
    var deleteLandCandidate by remember { mutableStateOf<AgriLandEntity?>(null) }

    var showCropDialog by remember { mutableStateOf(false) }
    var editingCrop by remember { mutableStateOf<AgriCropEntity?>(null) }
    var preselectedLandForCrop by remember { mutableStateOf<Long?>(null) }
    var deleteCropCandidate by remember { mutableStateOf<AgriCropEntity?>(null) }

    var harvestCropTarget by remember { mutableStateOf<AgriCropEntity?>(null) }

    var showExpenseDialog by remember { mutableStateOf(false) }
    var expensePreselectedLandId by remember { mutableStateOf<Long?>(null) }
    var expensePreselectedCropId by remember { mutableStateOf<Long?>(null) }
    var deleteExpenseCandidate by remember { mutableStateOf<AgriExpenseEntity?>(null) }

    val activeFarmId = state.selectedFarmId
    val lands = remember(state.agriLands, activeFarmId) {
        if (activeFarmId == null) state.agriLands else state.agriLands.filter { it.farmId == activeFarmId }
    }
    val landIds = remember(lands) { lands.map { it.id }.toSet() }
    val crops = remember(state.agriCrops, activeFarmId, landIds) {
        if (activeFarmId == null) state.agriCrops
        else state.agriCrops.filter { it.farmId == activeFarmId || it.landId in landIds }
    }
    val expenses = remember(state.agriExpenses, activeFarmId, landIds) {
        if (activeFarmId == null) state.agriExpenses
        else state.agriExpenses.filter { it.farmId == activeFarmId || it.landId in landIds }
    }

    val landsById = remember(state.agriLands) { state.agriLands.associateBy { it.id } }
    val cropsById = remember(state.agriCrops) { state.agriCrops.associateBy { it.id } }

    val overview = remember(lands, crops, expenses) {
        AgriLedgerCalculator.summarizeOverview(lands, crops, expenses)
    }
    val cropSummaries = remember(crops, expenses) {
        crops.map { AgriLedgerCalculator.summarizeCrop(it, expenses) }
    }
    val landSummaries = remember(lands, crops, expenses) {
        lands.map { AgriLedgerCalculator.summarizeLand(it, crops, expenses) }
    }

    val filteredCrops = remember(cropSummaries, selectedCropTypeFilter, selectedLandFilterId) {
        cropSummaries.filter { s ->
            val matchType = selectedCropTypeFilter == null || s.crop.cropType.equals(selectedCropTypeFilter, ignoreCase = true)
            val matchLand = selectedLandFilterId == null || s.crop.landId == selectedLandFilterId
            matchType && matchLand
        }
    }
    val filteredExpenses = remember(expenses, selectedSectorFilter, selectedLandFilterId) {
        expenses.filter { e ->
            val matchSector = selectedSectorFilter == null || e.sector == selectedSectorFilter
            val matchLand = selectedLandFilterId == null || e.landId == selectedLandFilterId
            matchSector && matchLand
        }
    }

    Scaffold(
        floatingActionButton = {
            if (canManage) {
                ExtendedFloatingActionButton(
                    onClick = {
                        when {
                            lands.isEmpty() || selectedTab == AgriTab.LAND_PLOTS -> {
                                editingLand = null
                                showLandDialog = true
                            }
                            crops.isEmpty() -> {
                                editingCrop = null
                                preselectedLandForCrop = lands.firstOrNull()?.id
                                showCropDialog = true
                            }
                            else -> {
                                expensePreselectedLandId = lands.firstOrNull()?.id
                                expensePreselectedCropId = null
                                showExpenseDialog = true
                            }
                        }
                    },
                    icon = {
                        Icon(
                            when {
                                lands.isEmpty() || selectedTab == AgriTab.LAND_PLOTS -> Icons.Default.Landscape
                                crops.isEmpty() -> Icons.Default.Grass
                                else -> Icons.Default.ReceiptLong
                            },
                            contentDescription = null
                        )
                    },
                    text = {
                        Text(
                            when {
                                lands.isEmpty() || selectedTab == AgriTab.LAND_PLOTS -> tr("Add Land", "জমি যোগ করুন")
                                crops.isEmpty() -> tr("Add Crop", "ফসল যোগ করুন")
                                else -> tr("Add Expense", "খরচ যোগ করুন")
                            }
                        )
                    },
                    modifier = Modifier.testTag("agri_primary_fab")
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            TabRow(selectedTabIndex = selectedTab.ordinal) {
                Tab(
                    selected = selectedTab == AgriTab.OVERVIEW_CROPS,
                    onClick = { selectedTab = AgriTab.OVERVIEW_CROPS },
                    text = { Text(tr("Crops & P/L", "ফসল ও লাভ-ক্ষতি"), maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    modifier = Modifier.testTag("agri_tab_crops")
                )
                Tab(
                    selected = selectedTab == AgriTab.LAND_PLOTS,
                    onClick = { selectedTab = AgriTab.LAND_PLOTS },
                    text = { Text(tr("Land Plots (${lands.size})", "জমিসমূহ (${lands.size})"), maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    modifier = Modifier.testTag("agri_tab_lands")
                )
                Tab(
                    selected = selectedTab == AgriTab.SECTOR_EXPENSES,
                    onClick = { selectedTab = AgriTab.SECTOR_EXPENSES },
                    text = { Text(tr("Sector Costs", "খরচের খাত"), maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    modifier = Modifier.testTag("agri_tab_sectors")
                )
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 92.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item(key = "agri_header") {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    tr("Agriculture, Crops & Land Ledger", "কৃষি, ফসল ও জমির হিসাব"),
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    tr(
                                        "Track Rice, Wheat, Corn & Cattle Feed • Sector spending, yield worth & land profit/loss",
                                        "ধান, গম, ভুট্টা ও গোখাদ্য • কোন খাতে কত খরচ, ফলনের মূল্য ও প্রতি জমির লাভ-ক্ষতি"
                                    ),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            FeaturePlanBadge(
                                isFree = subPolicy.isFree(SubscriptionFeature.AGRICULTURE_CROP_LEDGER),
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

                        if (canManage) {
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                FilledTonalButton(
                                    onClick = {
                                        editingLand = null
                                        showLandDialog = true
                                    },
                                    modifier = Modifier.testTag("agri_add_land_btn")
                                ) {
                                    Icon(Icons.Default.Landscape, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text(tr("+ Land Plot", "+ নতুন জমি"))
                                }
                                FilledTonalButton(
                                    onClick = {
                                        editingCrop = null
                                        preselectedLandForCrop = selectedLandFilterId ?: lands.firstOrNull()?.id
                                        showCropDialog = true
                                    },
                                    enabled = lands.isNotEmpty(),
                                    modifier = Modifier.testTag("agri_add_crop_btn")
                                ) {
                                    Icon(Icons.Default.Grass, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text(tr("+ Crop Batch", "+ নতুন ফসল"))
                                }
                                Button(
                                    onClick = {
                                        expensePreselectedLandId = selectedLandFilterId ?: lands.firstOrNull()?.id
                                        expensePreselectedCropId = null
                                        showExpenseDialog = true
                                    },
                                    enabled = lands.isNotEmpty(),
                                    modifier = Modifier.testTag("agri_add_expense_btn")
                                ) {
                                    Icon(Icons.Default.AddCard, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text(tr("+ Sector Cost", "+ খাতভিত্তিক খরচ"))
                                }
                            }
                        }
                    }
                }

                when (selectedTab) {
                    AgriTab.OVERVIEW_CROPS -> {
                        item(key = "agri_kpis") {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    KpiMetricCard(
                                        title = tr("Total Agri Spent", "মোট কৃষি খরচ"),
                                        value = CurrencyFormatter.format(overview.totalSpent, currency),
                                        icon = Icons.Default.Payments,
                                        tint = MaterialTheme.colorScheme.error,
                                        subtitle = tr("${expenses.size} sector entries", "${expenses.size} টি খরচের এন্ট্রি"),
                                        modifier = Modifier.weight(1f)
                                    )
                                    KpiMetricCard(
                                        title = tr("Yield Worth", "উৎপাদিত ফসলের মূল্য"),
                                        value = CurrencyFormatter.format(overview.totalYieldWorth, currency),
                                        icon = Icons.Default.Grain,
                                        tint = MaterialTheme.colorScheme.tertiary,
                                        subtitle = if (overview.totalSoldRevenue > 0.0) {
                                            "${tr("Sold", "বিক্রি")}: ${CurrencyFormatter.format(overview.totalSoldRevenue, currency)}"
                                        } else tr("Estimated harvest value", "ফলনের বাজার মূল্য"),
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    val isProfit = overview.netProfitOrLoss >= 0.0
                                    KpiMetricCard(
                                        title = tr("Net Profit / Loss", "নিট লাভ / ক্ষতি"),
                                        value = CurrencyFormatter.format(overview.netProfitOrLoss, currency),
                                        icon = if (isProfit) Icons.Default.TrendingUp else Icons.Default.TrendingDown,
                                        tint = if (isProfit) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                                        subtitle = if (isProfit) tr("Net Profit", "মোট লাভ") else tr("Net Deficit / Ongoing", "চলমান খরচ / ঘাটতি"),
                                        modifier = Modifier.weight(1f)
                                    )
                                    KpiMetricCard(
                                        title = tr("Lands & Crops", "জমি ও ফসল"),
                                        value = "${overview.totalLands} / ${overview.totalCrops}",
                                        icon = Icons.Default.Landscape,
                                        subtitle = tr("Plots / Crop batches", "টুকরো জমি / ফসল"),
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }

                        if (overview.sectorTotals.isNotEmpty()) {
                            item(key = "agri_sector_breakdown") {
                                SectorBreakdownCard(
                                    title = tr("Where Money Was Spent (By Sector)", "কোন খাতে কত টাকা খরচ হয়েছে"),
                                    sectorTotals = overview.sectorTotals,
                                    totalSpent = overview.totalSpent,
                                    currency = currency,
                                    isBn = isBn
                                )
                            }
                        }

                        item(key = "agri_crop_filters") {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    tr("Crop Batches (${filteredCrops.size})", "ফসলের তালিকা (${filteredCrops.size})"),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    item(key = "crop_type_all") {
                                        FilterChip(
                                            selected = selectedCropTypeFilter == null,
                                            onClick = { selectedCropTypeFilter = null },
                                            label = { Text(tr("All Crops", "সব ফসল")) }
                                        )
                                    }
                                    items(AgriCropType.entries, key = { it.key }) { type ->
                                        FilterChip(
                                            selected = selectedCropTypeFilter == type.key,
                                            onClick = {
                                                selectedCropTypeFilter = if (selectedCropTypeFilter == type.key) null else type.key
                                            },
                                            label = { Text(if (isBn) type.labelBn else type.labelEn) }
                                        )
                                    }
                                }
                            }
                        }

                        if (lands.isEmpty()) {
                            item(key = "agri_no_lands_empty") {
                                EmptyStateCard(
                                    icon = Icons.Default.Landscape,
                                    title = tr("Add Your First Piece of Land", "প্রথমে একটি জমির তথ্য যোগ করুন"),
                                    subtitle = tr(
                                        "Add each plot of land (e.g. North Paddy Plot 50 Decimal) so you can record Rice, Wheat, Corn, Cattle Feed and exact profit/loss per land.",
                                        "প্রতিটি জমির টুকরো যোগ করুন যাতে ধান, গম, ভুট্টা, গোখাদ্যের খরচ, ফলনের মূল্য ও লাভ-ক্ষতি আলাদাভাবে দেখা যায়।"
                                    ),
                                    actionLabel = if (canManage) tr("Add Land Plot", "নতুন জমি যোগ করুন") else null,
                                    onAction = if (canManage) { { editingLand = null; showLandDialog = true } } else null
                                )
                            }
                        } else if (filteredCrops.isEmpty()) {
                            item(key = "agri_no_crops_empty") {
                                EmptyStateCard(
                                    icon = Icons.Default.Grass,
                                    title = tr("No Crop Batches Found", "কোনো ফসলের হিসাব পাওয়া যায়নি"),
                                    subtitle = tr(
                                        "Add Rice, Wheat, Corn, or Cattle Feed on your land plots to track sector costs and yield profit/loss.",
                                        "আপনার জমিতে ধান, গম, ভুট্টা বা গোখাদ্য যোগ করে খাতভিত্তিক খরচ ও লাভ-ক্ষতি হিসাব করুন।"
                                    ),
                                    actionLabel = if (canManage) tr("Add Crop Batch", "ফসল যোগ করুন") else null,
                                    onAction = if (canManage) {
                                        {
                                            editingCrop = null
                                            preselectedLandForCrop = lands.firstOrNull()?.id
                                            showCropDialog = true
                                        }
                                    } else null
                                )
                            }
                        } else {
                            items(filteredCrops, key = { "crop_${it.crop.id}" }) { summary ->
                                val land = landsById[summary.crop.landId]
                                AgriCropCard(
                                    summary = summary,
                                    land = land,
                                    currency = currency,
                                    canManage = canManage,
                                    canDelete = canDelete,
                                    onAddExpense = {
                                        expensePreselectedLandId = summary.crop.landId
                                        expensePreselectedCropId = summary.crop.id
                                        showExpenseDialog = true
                                    },
                                    onRecordHarvest = { harvestCropTarget = summary.crop },
                                    onEdit = {
                                        editingCrop = summary.crop
                                        showCropDialog = true
                                    },
                                    onDelete = { deleteCropCandidate = summary.crop }
                                )
                            }
                        }
                    }

                    AgriTab.LAND_PLOTS -> {
                        if (landSummaries.isEmpty()) {
                            item(key = "agri_lands_empty") {
                                EmptyStateCard(
                                    icon = Icons.Default.Landscape,
                                    title = tr("No Land Plots Recorded", "কোনো জমি যোগ করা হয়নি"),
                                    subtitle = tr(
                                        "Record every piece of agricultural land to track exact spending per plot, crop yield value, and profit or loss.",
                                        "প্রতিটি জমির টুকরো যোগ করে সেই জমিতে মোট খরচ, ফলনের মূল্য ও লাভ-ক্ষতি আলাদাভাবে দেখুন।"
                                    ),
                                    actionLabel = if (canManage) tr("Add Land Plot", "নতুন জমি যোগ করুন") else null,
                                    onAction = if (canManage) { { editingLand = null; showLandDialog = true } } else null
                                )
                            }
                        } else {
                            items(landSummaries, key = { "land_${it.land.id}" }) { ls ->
                                AgriLandSummaryCard(
                                    summary = ls,
                                    crops = crops.filter { it.landId == ls.land.id },
                                    currency = currency,
                                    canManage = canManage,
                                    canDelete = canDelete,
                                    onAddCropOnLand = {
                                        editingCrop = null
                                        preselectedLandForCrop = ls.land.id
                                        showCropDialog = true
                                    },
                                    onAddExpenseOnLand = {
                                        expensePreselectedLandId = ls.land.id
                                        expensePreselectedCropId = null
                                        showExpenseDialog = true
                                    },
                                    onEditLand = {
                                        editingLand = ls.land
                                        showLandDialog = true
                                    },
                                    onDeleteLand = { deleteLandCandidate = ls.land }
                                )
                            }
                        }
                    }

                    AgriTab.SECTOR_EXPENSES -> {
                        item(key = "agri_sector_filters") {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(tr("Filter by Sector", "খাত অনুযায়ী খুঁজুন"), style = MaterialTheme.typography.labelLarge)
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    item(key = "sec_all") {
                                        FilterChip(
                                            selected = selectedSectorFilter == null,
                                            onClick = { selectedSectorFilter = null },
                                            label = { Text(tr("All Sectors", "সব খাত")) }
                                        )
                                    }
                                    items(AgriSector.entries, key = { it.key }) { sec ->
                                        FilterChip(
                                            selected = selectedSectorFilter == sec.key,
                                            onClick = {
                                                selectedSectorFilter = if (selectedSectorFilter == sec.key) null else sec.key
                                            },
                                            label = { Text(if (isBn) sec.labelBn else sec.labelEn) }
                                        )
                                    }
                                }

                                if (lands.isNotEmpty()) {
                                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        item(key = "land_filter_all") {
                                            FilterChip(
                                                selected = selectedLandFilterId == null,
                                                onClick = { selectedLandFilterId = null },
                                                label = { Text(tr("All Lands", "সব জমি")) }
                                            )
                                        }
                                        items(lands, key = { "lf_${it.id}" }) { land ->
                                            FilterChip(
                                                selected = selectedLandFilterId == land.id,
                                                onClick = {
                                                    selectedLandFilterId = if (selectedLandFilterId == land.id) null else land.id
                                                },
                                                label = { Text(land.name) }
                                            )
                                        }
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.65f),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            tr("Filtered Sector Total (${filteredExpenses.size})", "নির্বাচিত খাতের মোট খরচ (${filteredExpenses.size})"),
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                            CurrencyFormatter.format(filteredExpenses.sumOf { it.amount }, currency),
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            }
                        }

                        if (filteredExpenses.isEmpty()) {
                            item(key = "agri_exp_empty") {
                                EmptyStateCard(
                                    icon = Icons.Default.ReceiptLong,
                                    title = tr("No Sector Expenses Recorded", "কোনো খাতভিত্তিক খরচ নেই"),
                                    subtitle = tr(
                                        "Log seeds, fertilizer, pesticide, irrigation, tractor, labor or land lease expenses.",
                                        "বীজ, সার, কীটনাশক, সেচ, শ্রমিক বা জমির ভাড়ার খরচ যোগ করুন।"
                                    ),
                                    actionLabel = if (canManage && lands.isNotEmpty()) tr("Add Expense", "খরচ যোগ করুন") else null,
                                    onAction = if (canManage && lands.isNotEmpty()) {
                                        {
                                            expensePreselectedLandId = selectedLandFilterId ?: lands.firstOrNull()?.id
                                            expensePreselectedCropId = null
                                            showExpenseDialog = true
                                        }
                                    } else null
                                )
                            }
                        } else {
                            items(filteredExpenses, key = { "aexp_${it.id}" }) { exp ->
                                val landName = landsById[exp.landId]?.name ?: tr("Land #${exp.landId}", "জমি #${exp.landId}")
                                val cropTitle = exp.cropId?.let { cropsById[it]?.title }
                                AgriExpenseCard(
                                    expense = exp,
                                    landName = landName,
                                    cropTitle = cropTitle,
                                    currency = currency,
                                    canDelete = canDelete,
                                    onDelete = { deleteExpenseCandidate = exp }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showLandDialog) {
        AgriLandDialog(
            initial = editingLand,
            farms = state.farms,
            defaultFarmId = activeFarmId,
            strings = strings,
            onDismiss = { showLandDialog = false },
            onSave = {
                onSaveLand(it)
                showLandDialog = false
            }
        )
    }

    if (showCropDialog) {
        AgriCropDialog(
            initial = editingCrop,
            lands = lands.ifEmpty { state.agriLands },
            defaultLandId = preselectedLandForCrop,
            currency = currency,
            strings = strings,
            onDismiss = { showCropDialog = false },
            onSave = {
                onSaveCrop(it)
                showCropDialog = false
            }
        )
    }

    harvestCropTarget?.let { target ->
        AgriHarvestDialog(
            crop = target,
            currency = currency,
            strings = strings,
            onDismiss = { harvestCropTarget = null },
            onSave = { qty, unit, price, worth, sold, status ->
                onRecordHarvest(target, qty, unit, price, worth, sold, status)
                harvestCropTarget = null
            }
        )
    }

    if (showExpenseDialog) {
        AgriExpenseDialog(
            lands = lands.ifEmpty { state.agriLands },
            crops = crops.ifEmpty { state.agriCrops },
            initialLandId = expensePreselectedLandId,
            initialCropId = expensePreselectedCropId,
            currency = currency,
            strings = strings,
            onDismiss = { showExpenseDialog = false },
            onSave = { lId, cId, fId, sector, customSector, amountBase, desc ->
                onAddAgriExpense(lId, cId, fId, sector, customSector, amountBase, desc)
                showExpenseDialog = false
            }
        )
    }

    deleteLandCandidate?.let { land ->
        AlertDialog(
            onDismissRequest = { deleteLandCandidate = null },
            title = { Text(tr("Delete Land Plot?", "জমির হিসাব মুছবেন?")) },
            text = {
                Text(
                    tr(
                        "Deleting '${land.name}' will also remove all crops and expenses recorded on this land.",
                        "'${land.name}' মুছলে এই জমির সব ফসল ও খরচের হিসাব মুছে যাবে।"
                    )
                )
            },
            confirmButton = {
                Button(
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    onClick = {
                        onDeleteLand(land)
                        deleteLandCandidate = null
                    }
                ) { Text(strings.delete) }
            },
            dismissButton = {
                TextButton(onClick = { deleteLandCandidate = null }) { Text(strings.cancel) }
            }
        )
    }

    deleteCropCandidate?.let { crop ->
        AlertDialog(
            onDismissRequest = { deleteCropCandidate = null },
            title = { Text(tr("Delete Crop Batch?", "ফসল মুছবেন?")) },
            text = {
                Text(
                    tr(
                        "Delete '${crop.title}' and its linked crop expenses?",
                        "'${crop.title}' এবং এর খরচের হিসাব মুছে ফেলবেন?"
                    )
                )
            },
            confirmButton = {
                Button(
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    onClick = {
                        onDeleteCrop(crop)
                        deleteCropCandidate = null
                    }
                ) { Text(strings.delete) }
            },
            dismissButton = {
                TextButton(onClick = { deleteCropCandidate = null }) { Text(strings.cancel) }
            }
        )
    }

    deleteExpenseCandidate?.let { exp ->
        AlertDialog(
            onDismissRequest = { deleteExpenseCandidate = null },
            title = { Text(tr("Delete Expense Entry?", "খরচ মুছবেন?")) },
            text = { Text("${DomainLocalization.agriSector(exp.sector, exp.customSectorName, isBn)} • ${CurrencyFormatter.format(exp.amount, currency)}") },
            confirmButton = {
                Button(
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    onClick = {
                        onDeleteAgriExpense(exp)
                        deleteExpenseCandidate = null
                    }
                ) { Text(strings.delete) }
            },
            dismissButton = {
                TextButton(onClick = { deleteExpenseCandidate = null }) { Text(strings.cancel) }
            }
        )
    }
}

@Composable
private fun SectorBreakdownCard(
    title: String,
    sectorTotals: Map<String, Double>,
    totalSpent: Double,
    currency: CurrencyOption,
    isBn: Boolean
) {
    val sorted = remember(sectorTotals) {
        sectorTotals.entries.sortedByDescending { it.value }
    }
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            sorted.forEach { (sectorKey, amt) ->
                val ratio = if (totalSpent > 0.0) (amt / totalSpent).toFloat().coerceIn(0f, 1f) else 0f
                val pct = (ratio * 100f).toInt()
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            DomainLocalization.agriSector(sectorKey, "", isBn),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            "${CurrencyFormatter.format(amt, currency)} ($pct%)",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    LinearProgressIndicator(
                        progress = { ratio },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AgriCropCard(
    summary: AgriCropSummary,
    land: AgriLandEntity?,
    currency: CurrencyOption,
    canManage: Boolean,
    canDelete: Boolean,
    onAddExpense: () -> Unit,
    onRecordHarvest: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val isBn = isBangla()
    val crop = summary.crop
    val isProfit = summary.profitOrLoss >= 0.0
    val statusColor = when (crop.status) {
        AgriCropStatus.SOLD.key, AgriCropStatus.HARVESTED.key -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.tertiary
    }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(crop.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    val landLabel = land?.let {
                        "${it.name} (${formatArea(it.areaSize)} ${DomainLocalization.agriLandUnit(it.areaUnit, isBn)})"
                    } ?: tr("Unknown Land", "জমি")
                    Text(
                        "${DomainLocalization.agriCropType(crop.cropType, isBn)} • ${DomainLocalization.agriSeason(crop.season, isBn)} • $landLabel",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = statusColor.copy(alpha = 0.14f)
                ) {
                    Text(
                        DomainLocalization.agriCropStatus(crop.status, isBn),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = statusColor
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MetricMiniBox(
                    label = tr("Spent", "খরচ"),
                    value = CurrencyFormatter.format(summary.totalSpent, currency),
                    valueColor = MaterialTheme.colorScheme.error,
                    modifier = Modifier.weight(1f)
                )
                MetricMiniBox(
                    label = tr("Yield Worth", "ফলনের মূল্য"),
                    value = if (summary.yieldWorth > 0.0) CurrencyFormatter.format(summary.yieldWorth, currency) else tr("Not harvested", "বাকি"),
                    valueColor = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.weight(1f)
                )
                MetricMiniBox(
                    label = if (isProfit) tr("Profit", "লাভ") else tr("Loss / Spend", "ক্ষতি / খরচ"),
                    value = CurrencyFormatter.format(summary.profitOrLoss, currency),
                    valueColor = if (isProfit) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                    modifier = Modifier.weight(1f)
                )
            }

            if (crop.yieldQuantity > 0.0 || crop.soldAmount > 0.0) {
                Text(
                    buildString {
                        if (crop.yieldQuantity > 0.0) {
                            append("${tr("Harvest", "উৎপাদন")}: ${formatArea(crop.yieldQuantity)} ${crop.yieldUnit}")
                        }
                        if (crop.soldAmount > 0.0) {
                            if (isNotEmpty()) append(" • ")
                            append("${tr("Sold Cash", "বিক্রি আয়")}: ${CurrencyFormatter.format(crop.soldAmount, currency)}")
                        }
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            if (summary.sectorTotals.isNotEmpty()) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    summary.sectorTotals.entries.sortedByDescending { it.value }.forEach { (sec, amt) ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
                        ) {
                            Text(
                                "${DomainLocalization.agriSector(sec, "", isBn)}: ${CurrencyFormatter.format(amt, currency)}",
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            if (canManage || canDelete) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (canManage) {
                        FilledTonalButton(
                            onClick = onAddExpense,
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(tr("Add Cost", "খরচ যোগ"))
                        }
                        OutlinedButton(
                            onClick = onRecordHarvest,
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Icon(Icons.Default.Grain, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(tr("Yield / Worth", "ফলন ও মূল্য"))
                        }
                        TextButton(onClick = onEdit) {
                            Text(tr("Edit", "সম্পাদনা"))
                        }
                    }
                    if (canDelete) {
                        IconButton(onClick = onDelete) {
                            Icon(Icons.Default.Delete, contentDescription = tr("Delete crop", "ফসল মুছুন"), tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AgriLandSummaryCard(
    summary: AgriLandSummary,
    crops: List<AgriCropEntity>,
    currency: CurrencyOption,
    canManage: Boolean,
    canDelete: Boolean,
    onAddCropOnLand: () -> Unit,
    onAddExpenseOnLand: () -> Unit,
    onEditLand: () -> Unit,
    onDeleteLand: () -> Unit
) {
    val isBn = isBangla()
    val land = summary.land
    val isProfit = summary.profitOrLoss >= 0.0
    val unitLabel = DomainLocalization.agriLandUnit(land.areaUnit, isBn)

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(land.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        buildString {
                            append("${formatArea(land.areaSize)} $unitLabel")
                            if (land.location.isNotBlank()) append(" • ${land.location}")
                            if (land.soilOrLandType.isNotBlank()) append(" • ${land.soilOrLandType}")
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = (if (isProfit) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error).copy(alpha = 0.14f)
                ) {
                    Text(
                        if (isProfit) "${tr("Profit", "লাভ")}: ${CurrencyFormatter.format(summary.profitOrLoss, currency)}"
                        else "${tr("Loss", "ক্ষতি")}: ${CurrencyFormatter.format(summary.profitOrLoss, currency)}",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isProfit) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MetricMiniBox(
                    label = tr("Total Spent", "মোট খরচ"),
                    value = CurrencyFormatter.format(summary.totalSpent, currency),
                    valueColor = MaterialTheme.colorScheme.error,
                    modifier = Modifier.weight(1f)
                )
                MetricMiniBox(
                    label = tr("Yield Worth", "ফলনের মূল্য"),
                    value = CurrencyFormatter.format(summary.totalYieldWorth, currency),
                    valueColor = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.weight(1f)
                )
                MetricMiniBox(
                    label = tr("Cost / $unitLabel", "প্রতি $unitLabel খরচ"),
                    value = CurrencyFormatter.format(summary.costPerUnitArea, currency),
                    valueColor = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.weight(1f)
                )
            }

            if (crops.isNotEmpty()) {
                Text(
                    "${tr("Crops on this land", "এই জমির ফসল")} (${crops.size}): " +
                        crops.joinToString(", ") { "${it.title} (${DomainLocalization.agriCropType(it.cropType, isBn)})" },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (summary.sectorTotals.isNotEmpty()) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    summary.sectorTotals.entries.sortedByDescending { it.value }.forEach { (sec, amt) ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
                        ) {
                            Text(
                                "${DomainLocalization.agriSector(sec, "", isBn)}: ${CurrencyFormatter.format(amt, currency)}",
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            if (canManage || canDelete) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (canManage) {
                        FilledTonalButton(onClick = onAddCropOnLand) {
                            Text(tr("+ Add Crop", "+ ফসল যোগ"))
                        }
                        OutlinedButton(onClick = onAddExpenseOnLand) {
                            Text(tr("+ Land Cost", "+ জমির খরচ"))
                        }
                        TextButton(onClick = onEditLand) {
                            Text(tr("Edit", "সম্পাদনা"))
                        }
                    }
                    if (canDelete) {
                        IconButton(onClick = onDeleteLand) {
                            Icon(Icons.Default.Delete, contentDescription = tr("Delete land", "জমি মুছুন"), tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AgriExpenseCard(
    expense: AgriExpenseEntity,
    landName: String,
    cropTitle: String?,
    currency: CurrencyOption,
    canDelete: Boolean,
    onDelete: () -> Unit
) {
    val isBn = isBangla()
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    DomainLocalization.agriSector(expense.sector, expense.customSectorName, isBn),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    buildString {
                        append(landName)
                        if (!cropTitle.isNullOrBlank()) append(" • $cropTitle")
                        append(" • ${formatShortDate(expense.expenseDate)}")
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (expense.description.isNotBlank()) {
                    Text(expense.description, style = MaterialTheme.typography.bodySmall)
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    CurrencyFormatter.format(expense.amount, currency),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error
                )
                if (canDelete) {
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = tr("Delete", "মুছুন"), tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricMiniBox(
    label: String,
    value: String,
    valueColor: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = valueColor, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

private fun formatArea(value: Double): String {
    return if (value % 1.0 == 0.0) value.toInt().toString() else String.format("%.2f", value)
}
