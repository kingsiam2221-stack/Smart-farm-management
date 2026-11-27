package com.example.ui.screens.farmer

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
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalTextToolbar
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
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
import com.example.ui.components.ProvideSafeTextToolbar
import com.example.ui.components.SubscriptionRequiredLockCard
import com.example.ui.components.formatShortDate
import com.example.ui.state.FarmerDashboardState

enum class OperationsTab {
    INVENTORY, EXPENSES
}

private val EMPLOYEE_PAY_EXPENSE_CATEGORIES = setOf(
    "Employee Salary",
    "Employee Bonus",
    "Employee Expense",
    "Employee Target Reward"
)

@Composable
fun OperationsScreen(
    state: FarmerDashboardState,
    strings: AppStrings,
    currency: CurrencyOption,
    initialTab: OperationsTab = OperationsTab.INVENTORY,
    onSelectFarm: (Long?) -> Unit,
    onSaveInventory: (InventoryItemEntity) -> Unit,
    onAdjustInventory: (InventoryItemEntity, Double, Boolean, Double, String, String) -> Unit,
    onDeleteInventory: (InventoryItemEntity) -> Unit,
    onAddExpense: (Long, String, Double, String) -> Unit,
    onDeleteExpense: (ExpenseEntity) -> Unit,
    onOpenStaffHr: () -> Unit = {},
    onOpenSubscription: () -> Unit = {}
) {
    val visibleTabs = remember(state.accessPolicy) {
        buildList {
            if (state.accessPolicy.has(AppPermission.INVENTORY_VIEW)) add(OperationsTab.INVENTORY)
            if (state.accessPolicy.has(AppPermission.EXPENSES_VIEW)) add(OperationsTab.EXPENSES)
        }
    }

    if (visibleTabs.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            EmptyStateCard(
                icon = Icons.Default.Lock,
                title = tr("Operations Access Restricted", "পরিচালনা অপশনের অনুমতি নেই"),
                subtitle = tr(
                    "Ask your Farm Admin to grant Inventory or General Expense access.",
                    "মজুদ বা সাধারণ খরচ অপশন দেখতে অ্যাডমিনের কাছ থেকে অনুমতি নিন।"
                )
            )
        }
        return
    }

    var selectedTab by remember(initialTab, visibleTabs) {
        mutableStateOf(if (initialTab in visibleTabs) initialTab else visibleTabs.first())
    }
    val selectedIdx = visibleTabs.indexOf(selectedTab).coerceAtLeast(0)
    val subPolicy = state.subscriptionPolicy
    val canSubscribe = state.accessPolicy.has(AppPermission.SUBSCRIPTION_MANAGE)

    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = selectedIdx) {
            visibleTabs.forEach { tab ->
                val title = when (tab) {
                    OperationsTab.INVENTORY -> tr("Inventory", "মজুদ / ইনভেন্টরি")
                    OperationsTab.EXPENSES -> tr("Expenses", "সাধারণ খরচ")
                }
                Tab(
                    selected = selectedTab == tab,
                    onClick = { selectedTab = tab },
                    text = { Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                )
            }
        }
        when (selectedTab) {
            OperationsTab.INVENTORY -> {
                if (!subPolicy.canAccess(SubscriptionFeature.INVENTORY_EXPENSES)) {
                    Box(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
                        SubscriptionRequiredLockCard(SubscriptionFeature.INVENTORY_EXPENSES, onOpenSubscription, canSubscribe)
                    }
                } else {
                    InventoryTab(
                        state, strings, currency, onSelectFarm, onSaveInventory, onAdjustInventory, onDeleteInventory
                    )
                }
            }
            OperationsTab.EXPENSES -> {
                if (!subPolicy.canAccess(SubscriptionFeature.INVENTORY_EXPENSES)) {
                    Box(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
                        SubscriptionRequiredLockCard(SubscriptionFeature.INVENTORY_EXPENSES, onOpenSubscription, canSubscribe)
                    }
                } else {
                    ExpensesTab(
                        state, strings, currency, onSelectFarm, onAddExpense, onDeleteExpense, onOpenStaffHr
                    )
                }
            }
        }
    }
}

@Composable
private fun InventoryTab(
    state: FarmerDashboardState,
    strings: AppStrings,
    currency: CurrencyOption,
    onSelectFarm: (Long?) -> Unit,
    onSaveInventory: (InventoryItemEntity) -> Unit,
    onAdjustInventory: (InventoryItemEntity, Double, Boolean, Double, String, String) -> Unit,
    onDeleteInventory: (InventoryItemEntity) -> Unit
) {
    val isBn = isBangla()
    val canManage = state.accessPolicy.has(AppPermission.INVENTORY_MANAGE)
    val canDelete = state.accessPolicy.has(AppPermission.INVENTORY_DELETE)
    var showAddDialog by remember { mutableStateOf(false) }
    var adjustTarget by remember { mutableStateOf<Pair<InventoryItemEntity, Boolean>?>(null) }

    val items = remember(state.inventory, state.selectedFarmId) {
        if (state.selectedFarmId == null) state.inventory else state.inventory.filter { it.farmId == state.selectedFarmId }
    }
    val farmsMap = remember(state.farms) { state.farms.associateBy { it.id } }
    val totalValuation = items.sumOf { it.quantity * it.unitCost }
    val lowStockItems = items.filter { it.quantity <= it.lowStockThreshold }
    val now = System.currentTimeMillis()
    val expiringSoon = items.filter { it.expiryDate != null && it.expiryDate <= now + 15 * 86_400_000L }

    Scaffold(
        floatingActionButton = {
            if (canManage) {
                ExtendedFloatingActionButton(
                    onClick = { showAddDialog = true },
                    icon = { Icon(Icons.Default.AddBox, contentDescription = strings.addInventory) },
                    text = { Text(strings.addInventory) },
                    modifier = Modifier.testTag("add_inventory_fab")
                )
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 88.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item(key = "inv_header") {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(tr("Inventory", "মজুদ / ইনভেন্টরি"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    FeaturePlanBadge(
                        isFree = state.subscriptionPolicy.isFree(SubscriptionFeature.INVENTORY_EXPENSES),
                        hasActiveSub = state.subscriptionPolicy.hasActiveSubscription
                    )
                }
                FarmSwitcherBar(state.farms, state.selectedFarmId, strings.allFarms, onSelectFarm)
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    KpiMetricCard(
                        title = tr("Inventory Value", "মজুদের মূল্য"),
                        value = CurrencyFormatter.formatCompact(totalValuation, currency),
                        icon = Icons.Default.Inventory,
                        subtitle = "${items.size} ${tr("SKUs tracked", "টি পণ্য তালিকাভুক্ত")}",
                        modifier = Modifier.weight(1f)
                    )
                    KpiMetricCard(
                        title = tr("Alerts", "সতর্কতা"),
                        value = "${lowStockItems.size} ${tr("Low", "কম")}",
                        icon = Icons.Default.WarningAmber,
                        tint = if (lowStockItems.isNotEmpty() || expiringSoon.isNotEmpty()) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                        subtitle = "${expiringSoon.size} ${tr("Expiring soon", "মেয়াদোত্তীর্ণ প্রায়")}",
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            if (items.isEmpty()) {
                item(key = "inv_empty") {
                    EmptyStateCard(
                        icon = Icons.Default.Inventory2,
                        title = tr("No Inventory Items", "কোনো মজুদ পণ্য নেই"),
                        subtitle = tr(
                            "Add feed, medicines, vaccines, equipment, eggs, milk, or meat.",
                            "খাদ্য, ওষুধ, টিকা, সরঞ্জাম, ডিম, দুধ বা মাংস যোগ করুন।"
                        ),
                        actionLabel = if (canManage) strings.addInventory else null,
                        onAction = if (canManage) ({ showAddDialog = true }) else null
                    )
                }
            } else {
                items(items, key = { "inv_${it.id}" }) { item ->
                    val isLow = item.quantity <= item.lowStockThreshold
                    val farmName = farmsMap[item.farmId]?.name ?: tr("Farm", "খামার")
                    val catDisp = DomainLocalization.productCategory(item.category, isBn)
                    Surface(
                        shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 2.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(item.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    Text(
                                        "$catDisp • $farmName • ${tr("Supplier", "সরবরাহকারী")}: ${item.supplierName.ifBlank { "-" }}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                AssistChip(
                                    onClick = {},
                                    label = {
                                        Text(
                                            "${item.quantity} ${item.unit}",
                                            color = if (isLow) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                )
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    "${tr("Cost", "ক্রয়মূল্য")}: ${CurrencyFormatter.format(item.unitCost, currency)}/${item.unit}",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                if (item.listInMarketplace) {
                                    Text(
                                        "• ${tr("Shop", "দোকান")}: ${CurrencyFormatter.format(item.sellingPrice, currency)}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                            item.expiryDate?.let { exp ->
                                val isNear = exp <= now + 15 * 86_400_000L
                                Text(
                                    "${tr("Expiry", "মেয়াদ")}: ${formatShortDate(exp)}" + if (isNear) tr(" (Expiring Soon!)", " (শীঘ্রই মেয়াদ শেষ!)") else "",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isNear) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (canManage || canDelete) {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                    if (canManage) {
                                        FilledTonalButton(onClick = { adjustTarget = item to true }, modifier = Modifier.weight(1f)) {
                                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(Modifier.width(4.dp))
                                            Text(tr("Stock In", "স্টক ইন"))
                                        }
                                        OutlinedButton(onClick = { adjustTarget = item to false }, modifier = Modifier.weight(1f)) {
                                            Icon(Icons.Default.Remove, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(Modifier.width(4.dp))
                                            Text(tr("Stock Out", "স্টক আউট"))
                                        }
                                    }
                                    if (canDelete) {
                                        IconButton(onClick = { onDeleteInventory(item) }) {
                                            Icon(Icons.Default.DeleteOutline, contentDescription = strings.delete, tint = MaterialTheme.colorScheme.error)
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

    if (showAddDialog) {
        AddInventoryDialog(
            farms = state.farms,
            defaultFarmId = state.selectedFarmId ?: state.farms.firstOrNull()?.id,
            currency = currency,
            onDismiss = { showAddDialog = false },
            onSave = {
                onSaveInventory(it)
                showAddDialog = false
            }
        )
    }

    adjustTarget?.let { (item, isStockIn) ->
        var qtyText by remember(item.id, isStockIn) { mutableStateOf("5") }
        var costText by remember(item.id, isStockIn) { mutableStateOf(CurrencyFormatter.convertFromBase(item.unitCost, currency).toInt().toString()) }
        var supplier by remember(item.id, isStockIn) { mutableStateOf(item.supplierName) }
        var note by remember(item.id, isStockIn) { mutableStateOf("") }

        val focusManager = LocalFocusManager.current
        val textToolbar = LocalTextToolbar.current
        AlertDialog(
            onDismissRequest = {
                textToolbar.hide()
                focusManager.clearFocus(force = true)
                adjustTarget = null
            },
            title = { Text("${if (isStockIn) tr("Stock In", "স্টক যোগ") else tr("Stock Out", "স্টক কমান")}: ${item.name}") },
            text = {
                ProvideSafeTextToolbar {
                    Column(
                        modifier = Modifier.verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = qtyText,
                            onValueChange = { qtyText = it },
                            label = { Text("${tr("Quantity", "পরিমাণ")} (${item.unit})") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true
                        )
                        if (isStockIn) {
                            OutlinedTextField(
                                value = costText,
                                onValueChange = { costText = it },
                                label = { Text("${tr("Unit Cost", "একক খরচ")} (${currency.symbol})") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = supplier,
                                onValueChange = { supplier = it },
                                label = { Text(tr("Supplier Name", "সরবরাহকারীর নাম")) },
                                singleLine = true
                            )
                        }
                        OutlinedTextField(value = note, onValueChange = { note = it }, label = { Text(tr("Note", "নোট")) })
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    textToolbar.hide()
                    focusManager.clearFocus(force = true)
                    val delta = qtyText.toDoubleOrNull() ?: 0.0
                    val baseCost = CurrencyFormatter.toBaseBdt(costText.toDoubleOrNull() ?: 0.0, currency)
                    if (delta > 0) onAdjustInventory(item, delta, isStockIn, baseCost, supplier, note)
                    adjustTarget = null
                }) { Text(strings.save) }
            },
            dismissButton = {
                TextButton(onClick = {
                    textToolbar.hide()
                    focusManager.clearFocus(force = true)
                    adjustTarget = null
                }) { Text(strings.cancel) }
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AddInventoryDialog(
    farms: List<FarmEntity>,
    defaultFarmId: Long?,
    currency: CurrencyOption,
    onDismiss: () -> Unit,
    onSave: (InventoryItemEntity) -> Unit
) {
    val isBn = isBangla()
    val cats = listOf("Animal Feed", "Poultry Feed", "Medicines", "Vaccines", "Farm Equipment", "Eggs", "Milk", "Meat", "Other")
    var selectedFarmId by remember { mutableStateOf(defaultFarmId ?: 0L) }
    var name by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(cats.first()) }
    var qtyText by remember { mutableStateOf("10") }
    var unit by remember { mutableStateOf("kg") }
    var costText by remember { mutableStateOf("0") }
    var sellText by remember { mutableStateOf("0") }
    var thresholdText by remember { mutableStateOf("5") }
    var supplier by remember { mutableStateOf("") }
    var listOnline by remember { mutableStateOf(false) }
    var desc by remember { mutableStateOf("") }

    val focusManager = LocalFocusManager.current
    val textToolbar = LocalTextToolbar.current
    val safeDismiss = {
        textToolbar.hide()
        focusManager.clearFocus(force = true)
        onDismiss()
    }

    AlertDialog(
        onDismissRequest = safeDismiss,
        title = { Text(tr("Add Inventory Item", "মজুদ পণ্য যোগ করুন")) },
        text = {
            ProvideSafeTextToolbar {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 430.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(tr("Farm", "খামার"), style = MaterialTheme.typography.labelLarge)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        farms.forEach { f ->
                            FilterChip(
                                selected = selectedFarmId == f.id,
                                onClick = { selectedFarmId = f.id },
                                label = { Text(f.name) }
                            )
                        }
                    }
                    Text(tr("Category", "ধরন"), style = MaterialTheme.typography.labelLarge)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        cats.forEach { c ->
                            FilterChip(
                                selected = category == c,
                                onClick = { category = c },
                                label = { Text(DomainLocalization.productCategory(c, isBn)) }
                            )
                        }
                    }
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text(tr("Item Name *", "পণ্যের নাম *")) }, singleLine = true)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = qtyText, onValueChange = { qtyText = it }, label = { Text(tr("Qty", "পরিমাণ")) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f), singleLine = true)
                        OutlinedTextField(value = unit, onValueChange = { unit = it }, label = { Text(tr("Unit", "একক")) }, modifier = Modifier.weight(1f), singleLine = true)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = costText, onValueChange = { costText = it }, label = { Text("${tr("Unit Cost", "একক খরচ")} (${currency.symbol})") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f), singleLine = true)
                        OutlinedTextField(value = thresholdText, onValueChange = { thresholdText = it }, label = { Text(tr("Low Alert", "কম সতর্কতা")) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f), singleLine = true)
                    }
                    OutlinedTextField(value = supplier, onValueChange = { supplier = it }, label = { Text(tr("Supplier Name", "সরবরাহকারীর নাম")) }, singleLine = true)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = listOnline, onCheckedChange = { listOnline = it })
                        Text(tr("List in Customer Online Shop", "গ্রাহকের অনলাইন দোকানে যুক্ত করুন"))
                    }
                    if (listOnline) {
                        OutlinedTextField(value = sellText, onValueChange = { sellText = it }, label = { Text("${tr("Online Price", "অনলাইন মূল্য")} (${currency.symbol})") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true)
                        OutlinedTextField(value = desc, onValueChange = { desc = it }, label = { Text(tr("Product Description", "পণ্যের বিবরণ")) })
                    }
                }
            }
        },
        confirmButton = {
            Button(
                enabled = farms.isNotEmpty() && selectedFarmId > 0L && name.isNotBlank(),
                onClick = {
                    textToolbar.hide()
                    focusManager.clearFocus(force = true)
                    onSave(
                        InventoryItemEntity(
                            farmId = selectedFarmId,
                            ownerUserId = 0L,
                            name = name.trim(),
                            category = category,
                            quantity = qtyText.toDoubleOrNull() ?: 0.0,
                            unit = unit.ifBlank { "pcs" },
                            unitCost = CurrencyFormatter.toBaseBdt(costText.toDoubleOrNull() ?: 0.0, currency),
                            sellingPrice = CurrencyFormatter.toBaseBdt(sellText.toDoubleOrNull() ?: 0.0, currency),
                            lowStockThreshold = thresholdText.toDoubleOrNull() ?: 5.0,
                            supplierName = supplier.trim(),
                            listInMarketplace = listOnline,
                            description = desc.trim()
                        )
                    )
                }
            ) { Text(tr("Save Item", "সংরক্ষণ করুন")) }
        },
        dismissButton = { TextButton(onClick = safeDismiss) { Text(tr("Cancel", "বাতিল")) } }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ExpensesTab(
    state: FarmerDashboardState,
    strings: AppStrings,
    currency: CurrencyOption,
    onSelectFarm: (Long?) -> Unit,
    onAddExpense: (Long, String, Double, String) -> Unit,
    onDeleteExpense: (ExpenseEntity) -> Unit,
    onOpenStaffHr: () -> Unit
) {
    val isBn = isBangla()
    val canManage = state.accessPolicy.has(AppPermission.EXPENSES_MANAGE)
    var period by remember { mutableStateOf("All") }
    var showAdd by remember { mutableStateOf(false) }

    val now = System.currentTimeMillis()
    val cutoff = when (period) {
        "Daily" -> now - 86_400_000L
        "Weekly" -> now - 7 * 86_400_000L
        "Monthly" -> now - 30 * 86_400_000L
        "Yearly" -> now - 365 * 86_400_000L
        else -> 0L
    }
    val filtered = remember(state.expenses, state.selectedFarmId, cutoff) {
        state.expenses.filter {
            (state.selectedFarmId == null || it.farmId == state.selectedFarmId) &&
                it.expenseDate >= cutoff &&
                it.category !in EMPLOYEE_PAY_EXPENSE_CATEGORIES
        }
    }
    val farmsMap = remember(state.farms) { state.farms.associateBy { it.id } }

    Scaffold(
        floatingActionButton = {
            if (canManage) {
                ExtendedFloatingActionButton(
                    onClick = { showAdd = true },
                    icon = { Icon(Icons.Default.ReceiptLong, contentDescription = strings.addExpense) },
                    text = { Text(strings.addExpense) }
                )
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 88.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item(key = "exp_header") {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(tr("General Farm Expenses", "সাধারণ খামার খরচ"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    FeaturePlanBadge(
                        isFree = state.subscriptionPolicy.isFree(SubscriptionFeature.INVENTORY_EXPENSES),
                        hasActiveSub = state.subscriptionPolicy.hasActiveSubscription
                    )
                }
                FarmSwitcherBar(state.farms, state.selectedFarmId, strings.allFarms, onSelectFarm)
                Spacer(Modifier.height(8.dp))
                FilledTonalButton(
                    onClick = onOpenStaffHr,
                    modifier = Modifier.fillMaxWidth().testTag("expenses_open_staff_hr_button")
                ) {
                    Icon(Icons.Default.Groups, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        tr(
                            "Employee Salary, Bonus & Staff Expenses → Open Staff HR",
                            "কর্মচারী বেতন, বোনাস ও স্টাফ খরচ আলাদা অপশনে দেখুন →"
                        )
                    )
                }
                Spacer(Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(listOf("All", "Daily", "Weekly", "Monthly", "Yearly")) { p ->
                        FilterChip(selected = period == p, onClick = { period = p }, label = { Text(DomainLocalization.period(p, isBn)) })
                    }
                }
                Spacer(Modifier.height(8.dp))
                KpiMetricCard(
                    title = "${strings.totalExpenses} (${DomainLocalization.period(period, isBn)})",
                    value = CurrencyFormatter.format(filtered.sumOf { it.amount }, currency),
                    icon = Icons.Default.Payments,
                    tint = MaterialTheme.colorScheme.error,
                    subtitle = "${filtered.size} ${tr("entries", "টি হিসাব")}"
                )
            }

            if (filtered.isEmpty()) {
                item(key = "exp_empty") {
                    EmptyStateCard(
                        icon = Icons.Default.ReceiptLong,
                        title = tr("No General Expenses", "কোনো সাধারণ খরচের হিসাব নেই"),
                        subtitle = tr(
                            "Track feed, medicine, electricity, equipment, and maintenance here. Employee salary & bonuses live in Staff, Pay & Targets.",
                            "খাদ্য, ওষুধ, বিদ্যুৎ, সরঞ্জাম ও রক্ষণাবেক্ষণ খরচ এখানে লিখুন। কর্মচারীর বেতন-বোনাস 'কর্মচারী, বেতন ও টার্গেট' অপশনে আছে।"
                        ),
                        actionLabel = if (canManage) strings.addExpense else null,
                        onAction = if (canManage) ({ showAdd = true }) else null
                    )
                }
            } else {
                items(filtered, key = { "exp_${it.id}" }) { exp ->
                    val fName = farmsMap[exp.farmId]?.name ?: tr("Farm", "খামার")
                    Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surface, tonalElevation = 2.dp, modifier = Modifier.fillMaxWidth()) {
                        Row(modifier = Modifier.padding(14.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(DomainLocalization.expenseCategory(exp.category, isBn), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text("$fName • ${exp.description.ifBlank { "-" }}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(formatShortDate(exp.expenseDate), style = MaterialTheme.typography.labelSmall)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(CurrencyFormatter.format(exp.amount, currency), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                                if (canManage) {
                                    IconButton(onClick = { onDeleteExpense(exp) }) {
                                        Icon(Icons.Default.DeleteOutline, contentDescription = strings.delete)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAdd) {
        val expCats = listOf("Animal Purchase", "Bird Purchase", "Food & Feed", "Medicine", "Vaccination", "Electricity", "Water", "Transportation", "Equipment", "Maintenance", "Other")
        var farmId by remember { mutableStateOf(state.selectedFarmId ?: state.farms.firstOrNull()?.id ?: 0L) }
        var cat by remember { mutableStateOf("Food & Feed") }
        var amountText by remember { mutableStateOf("") }
        var desc by remember { mutableStateOf("") }
        val focusManager = LocalFocusManager.current
        val textToolbar = LocalTextToolbar.current

        AlertDialog(
            onDismissRequest = {
                textToolbar.hide()
                focusManager.clearFocus(force = true)
                showAdd = false
            },
            title = { Text(strings.addExpense) },
            text = {
                ProvideSafeTextToolbar {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 420.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(tr("Farm", "খামার"), style = MaterialTheme.typography.labelLarge)
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            state.farms.forEach { f ->
                                FilterChip(selected = farmId == f.id, onClick = { farmId = f.id }, label = { Text(f.name) })
                            }
                        }
                        Text(tr("Expense Category", "খরচের খাত"), style = MaterialTheme.typography.labelLarge)
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            expCats.forEach { c ->
                                FilterChip(selected = cat == c, onClick = { cat = c }, label = { Text(DomainLocalization.expenseCategory(c, isBn)) })
                            }
                        }
                        OutlinedTextField(value = amountText, onValueChange = { amountText = it }, label = { Text("${tr("Amount", "টাকার পরিমাণ")} (${currency.symbol}) *") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true)
                        OutlinedTextField(value = desc, onValueChange = { desc = it }, label = { Text(tr("Description", "বিবরণ")) })
                    }
                }
            },
            confirmButton = {
                Button(enabled = farmId > 0L && (amountText.toDoubleOrNull() ?: 0.0) > 0.0, onClick = {
                    textToolbar.hide()
                    focusManager.clearFocus(force = true)
                    onAddExpense(farmId, cat, CurrencyFormatter.toBaseBdt(amountText.toDoubleOrNull() ?: 0.0, currency), desc)
                    showAdd = false
                }) { Text(strings.save) }
            },
            dismissButton = {
                TextButton(onClick = {
                    textToolbar.hide()
                    focusManager.clearFocus(force = true)
                    showAdd = false
                }) { Text(strings.cancel) }
            }
        )
    }
}
