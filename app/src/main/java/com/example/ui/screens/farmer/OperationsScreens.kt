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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
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
import com.example.ui.components.KpiMetricCard
import com.example.ui.components.formatShortDate
import com.example.ui.state.FarmerDashboardState

@Composable
fun OperationsScreen(
    state: FarmerDashboardState,
    strings: AppStrings,
    currency: CurrencyOption,
    onSelectFarm: (Long?) -> Unit,
    onSaveInventory: (InventoryItemEntity) -> Unit,
    onAdjustInventory: (InventoryItemEntity, Double, Boolean, Double, String, String) -> Unit,
    onDeleteInventory: (InventoryItemEntity) -> Unit,
    onAddExpense: (Long, String, Double, String) -> Unit,
    onDeleteExpense: (ExpenseEntity) -> Unit,
    onSaveEmployee: (EmployeeEntity) -> Unit,
    onDeleteEmployee: (EmployeeEntity) -> Unit,
    onMarkAttendance: (EmployeeEntity, AttendanceStatus) -> Unit,
    onPaySalary: (EmployeeEntity, Double, String, String) -> Unit
) {
    var tabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf(
        tr("Inventory", "মজুদ / ইনভেন্টরি"),
        tr("Expenses", "খরচ"),
        tr("Employees", "কর্মচারী")
    )

    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = tabIndex) {
            tabs.forEachIndexed { idx, title ->
                Tab(selected = tabIndex == idx, onClick = { tabIndex = idx }, text = { Text(title) })
            }
        }
        when (tabIndex) {
            0 -> InventoryTab(state, strings, currency, onSelectFarm, onSaveInventory, onAdjustInventory, onDeleteInventory)
            1 -> ExpensesTab(state, strings, currency, onSelectFarm, onAddExpense, onDeleteExpense)
            2 -> EmployeesTab(state, strings, currency, onSelectFarm, onSaveEmployee, onDeleteEmployee, onMarkAttendance, onPaySalary)
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
            ExtendedFloatingActionButton(
                onClick = { showAddDialog = true },
                icon = { Icon(Icons.Default.AddBox, contentDescription = strings.addInventory) },
                text = { Text(strings.addInventory) }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 88.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
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
                item {
                    EmptyStateCard(
                        icon = Icons.Default.Inventory2,
                        title = tr("No Inventory Items", "কোনো মজুদ পণ্য নেই"),
                        subtitle = tr(
                            "Add feed, medicines, vaccines, equipment, eggs, milk, or meat.",
                            "খাদ্য, ওষুধ, টিকা, সরঞ্জাম, ডিম, দুধ বা মাংস যোগ করুন।"
                        ),
                        actionLabel = strings.addInventory,
                        onAction = { showAddDialog = true }
                    )
                }
            } else {
                items(items, key = { it.id }) { item ->
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
                                    Text("$catDisp • $farmName • ${tr("Supplier", "সরবরাহকারী")}: ${item.supplierName.ifBlank { "-" }}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                                Text("${tr("Cost", "ক্রয়মূল্য")}: ${CurrencyFormatter.format(item.unitCost, currency)}/${item.unit}", style = MaterialTheme.typography.bodyMedium)
                                if (item.listInMarketplace) {
                                    Text("• ${tr("Shop", "দোকান")}: ${CurrencyFormatter.format(item.sellingPrice, currency)}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
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
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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

    if (adjustTarget != null) {
        val (item, isStockIn) = adjustTarget!!
        var qtyText by remember { mutableStateOf("5") }
        var costText by remember { mutableStateOf(CurrencyFormatter.convertFromBase(item.unitCost, currency).toInt().toString()) }
        var supplier by remember { mutableStateOf(item.supplierName) }
        var note by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { adjustTarget = null },
            title = { Text("${if (isStockIn) tr("Stock In", "স্টক যোগ") else tr("Stock Out", "স্টক কমান")}: ${item.name}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = qtyText, onValueChange = { qtyText = it }, label = { Text("${tr("Quantity", "পরিমাণ")} (${item.unit})") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true)
                    if (isStockIn) {
                        OutlinedTextField(value = costText, onValueChange = { costText = it }, label = { Text("${tr("Unit Cost", "একক খরচ")} (${currency.symbol})") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true)
                        OutlinedTextField(value = supplier, onValueChange = { supplier = it }, label = { Text(tr("Supplier Name", "সরবরাহকারীর নাম")) }, singleLine = true)
                    }
                    OutlinedTextField(value = note, onValueChange = { note = it }, label = { Text(tr("Note", "নোট")) })
                }
            },
            confirmButton = {
                Button(onClick = {
                    val delta = qtyText.toDoubleOrNull() ?: 0.0
                    val baseCost = CurrencyFormatter.toBaseBdt(costText.toDoubleOrNull() ?: 0.0, currency)
                    if (delta > 0) onAdjustInventory(item, delta, isStockIn, baseCost, supplier, note)
                    adjustTarget = null
                }) { Text(strings.save) }
            },
            dismissButton = { TextButton(onClick = { adjustTarget = null }) { Text(strings.cancel) } }
        )
    }
}

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

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(tr("Add Inventory Item", "মজুদ পণ্য যোগ করুন")) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 430.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(farms) { f -> FilterChip(selected = selectedFarmId == f.id, onClick = { selectedFarmId = f.id }, label = { Text(f.name) }) }
                }
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(cats) { c -> FilterChip(selected = category == c, onClick = { category = c }, label = { Text(DomainLocalization.productCategory(c, isBn)) }) }
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
        },
        confirmButton = {
            Button(
                enabled = farms.isNotEmpty() && selectedFarmId > 0L && name.isNotBlank(),
                onClick = {
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
        dismissButton = { TextButton(onClick = onDismiss) { Text(tr("Cancel", "বাতিল")) } }
    )
}

@Composable
private fun ExpensesTab(
    state: FarmerDashboardState,
    strings: AppStrings,
    currency: CurrencyOption,
    onSelectFarm: (Long?) -> Unit,
    onAddExpense: (Long, String, Double, String) -> Unit,
    onDeleteExpense: (ExpenseEntity) -> Unit
) {
    val isBn = isBangla()
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
        state.expenses.filter { (state.selectedFarmId == null || it.farmId == state.selectedFarmId) && it.expenseDate >= cutoff }
    }
    val farmsMap = remember(state.farms) { state.farms.associateBy { it.id } }

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAdd = true },
                icon = { Icon(Icons.Default.ReceiptLong, contentDescription = strings.addExpense) },
                text = { Text(strings.addExpense) }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 88.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                FarmSwitcherBar(state.farms, state.selectedFarmId, strings.allFarms, onSelectFarm)
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
                item {
                    EmptyStateCard(
                        icon = Icons.Default.ReceiptLong,
                        title = tr("No Expenses", "কোনো খরচের হিসাব নেই"),
                        subtitle = tr(
                            "Track feed, medicine, salary, electricity, equipment, and more.",
                            "খাদ্য, ওষুধ, বেতন, বিদ্যুৎ, সরঞ্জাম ও অন্যান্য খরচের হিসাব রাখুন।"
                        ),
                        actionLabel = strings.addExpense,
                        onAction = { showAdd = true }
                    )
                }
            } else {
                items(filtered, key = { it.id }) { exp ->
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

    if (showAdd) {
        val expCats = listOf("Animal Purchase", "Bird Purchase", "Food & Feed", "Medicine", "Vaccination", "Employee Salary", "Electricity", "Water", "Transportation", "Equipment", "Maintenance", "Other")
        var farmId by remember { mutableStateOf(state.selectedFarmId ?: state.farms.firstOrNull()?.id ?: 0L) }
        var cat by remember { mutableStateOf("Food & Feed") }
        var amountText by remember { mutableStateOf("") }
        var desc by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAdd = false },
            title = { Text(strings.addExpense) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(state.farms) { f -> FilterChip(selected = farmId == f.id, onClick = { farmId = f.id }, label = { Text(f.name) }) }
                    }
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(expCats) { c -> FilterChip(selected = cat == c, onClick = { cat = c }, label = { Text(DomainLocalization.expenseCategory(c, isBn)) }) }
                    }
                    OutlinedTextField(value = amountText, onValueChange = { amountText = it }, label = { Text("${tr("Amount", "টাকার পরিমাণ")} (${currency.symbol}) *") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true)
                    OutlinedTextField(value = desc, onValueChange = { desc = it }, label = { Text(tr("Description", "বিবরণ")) })
                }
            },
            confirmButton = {
                Button(enabled = farmId > 0L && (amountText.toDoubleOrNull() ?: 0.0) > 0.0, onClick = {
                    onAddExpense(farmId, cat, CurrencyFormatter.toBaseBdt(amountText.toDoubleOrNull() ?: 0.0, currency), desc)
                    showAdd = false
                }) { Text(strings.save) }
            },
            dismissButton = { TextButton(onClick = { showAdd = false }) { Text(strings.cancel) } }
        )
    }
}

@Composable
private fun EmployeesTab(
    state: FarmerDashboardState,
    strings: AppStrings,
    currency: CurrencyOption,
    onSelectFarm: (Long?) -> Unit,
    onSaveEmployee: (EmployeeEntity) -> Unit,
    onDeleteEmployee: (EmployeeEntity) -> Unit,
    onMarkAttendance: (EmployeeEntity, AttendanceStatus) -> Unit,
    onPaySalary: (EmployeeEntity, Double, String, String) -> Unit
) {
    val isBn = isBangla()
    var showAdd by remember { mutableStateOf(false) }
    var salaryTarget by remember { mutableStateOf<EmployeeEntity?>(null) }
    val farmsMap = remember(state.farms) { state.farms.associateBy { it.id } }
    val filtered = remember(state.employees, state.selectedFarmId) {
        if (state.selectedFarmId == null) state.employees else state.employees.filter { it.farmId == state.selectedFarmId }
    }

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAdd = true },
                icon = { Icon(Icons.Default.PersonAdd, contentDescription = strings.addEmployee) },
                text = { Text(strings.addEmployee) }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 88.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                FarmSwitcherBar(state.farms, state.selectedFarmId, strings.allFarms, onSelectFarm)
            }
            if (filtered.isEmpty()) {
                item {
                    EmptyStateCard(
                        icon = Icons.Default.Groups,
                        title = tr("No Employees Yet", "কোনো কর্মচারী নেই"),
                        subtitle = tr(
                            "Add farm managers and staff, track attendance, permissions, and salaries.",
                            "খামার ম্যানেজার ও কর্মচারী যোগ করুন, উপস্থিতি ও বেতনের হিসাব রাখুন।"
                        ),
                        actionLabel = strings.addEmployee,
                        onAction = { showAdd = true }
                    )
                }
            } else {
                items(filtered, key = { it.id }) { emp ->
                    val farmName = farmsMap[emp.farmId]?.name ?: tr("Farm", "খামার")
                    val paidCount = state.salaryPayments.count { it.employeeId == emp.id }
                    Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surface, tonalElevation = 2.dp, modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(emp.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    Text("${emp.designation} • $farmName • ${emp.phone}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                IconButton(onClick = { onDeleteEmployee(emp) }) {
                                    Icon(Icons.Default.DeleteOutline, contentDescription = strings.delete, tint = MaterialTheme.colorScheme.error)
                                }
                            }
                            Text(
                                "${tr("Salary", "বেতন")}: ${CurrencyFormatter.format(emp.salary, currency)}/${tr("mo", "মাস")} • ${tr("Payments logged", "বেতন প্রদান")}: $paidCount • ${tr("Access", "অনুমতি")}: " +
                                    listOfNotNull(
                                        if (emp.canManageInventory) tr("Inventory", "মজুদ") else null,
                                        if (emp.canManageSales) tr("Sales", "বিক্রয়") else null,
                                        if (emp.canManageLivestock) tr("Livestock", "পশুপাখি") else null
                                    ).joinToString(", ").ifBlank { tr("View Only", "শুধু দেখা") },
                                style = MaterialTheme.typography.labelSmall
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text("${tr("Attendance", "উপস্থিতি")}:", style = MaterialTheme.typography.labelSmall)
                                AttendanceStatus.entries.forEach { st ->
                                    FilterChip(
                                        selected = emp.lastAttendanceStatus == st.name,
                                        onClick = { onMarkAttendance(emp, st) },
                                        label = { Text(DomainLocalization.attendanceStatus(st, isBn)) }
                                    )
                                }
                            }
                            FilledTonalButton(onClick = { salaryTarget = emp }, modifier = Modifier.fillMaxWidth()) {
                                Icon(Icons.Default.Payments, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("${tr("Pay Salary", "বেতন পরিশোধ")} (${CurrencyFormatter.format(emp.salary, currency)})")
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAdd) {
        val defaultDesignation = tr("Farm Manager", "খামার ম্যানেজার")
        var farmId by remember { mutableStateOf(state.selectedFarmId ?: state.farms.firstOrNull()?.id ?: 0L) }
        var name by remember { mutableStateOf("") }
        var phone by remember { mutableStateOf("") }
        var designation by remember { mutableStateOf(defaultDesignation) }
        var salaryText by remember { mutableStateOf("") }
        var permInv by remember { mutableStateOf(true) }
        var permSales by remember { mutableStateOf(false) }
        var permLive by remember { mutableStateOf(true) }

        AlertDialog(
            onDismissRequest = { showAdd = false },
            title = { Text(strings.addEmployee) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(state.farms) { f -> FilterChip(selected = farmId == f.id, onClick = { farmId = f.id }, label = { Text(f.name) }) }
                    }
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text(tr("Full Name *", "পূর্ণ নাম *")) }, singleLine = true)
                    OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text(tr("Phone", "মোবাইল নম্বর")) }, singleLine = true)
                    OutlinedTextField(value = designation, onValueChange = { designation = it }, label = { Text(tr("Designation", "পদবি")) }, singleLine = true)
                    OutlinedTextField(value = salaryText, onValueChange = { salaryText = it }, label = { Text("${tr("Monthly Salary", "মাসিক বেতন")} (${currency.symbol})") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = permInv, onCheckedChange = { permInv = it })
                        Text(tr("Inventory", "মজুদ"))
                        Checkbox(checked = permSales, onCheckedChange = { permSales = it })
                        Text(tr("Sales", "বিক্রয়"))
                        Checkbox(checked = permLive, onCheckedChange = { permLive = it })
                        Text(tr("Livestock", "পশুপাখি"))
                    }
                }
            },
            confirmButton = {
                Button(enabled = farmId > 0L && name.isNotBlank(), onClick = {
                    onSaveEmployee(
                        EmployeeEntity(
                            farmId = farmId,
                            ownerUserId = 0L,
                            name = name.trim(),
                            phone = phone.trim(),
                            designation = designation.trim(),
                            salary = CurrencyFormatter.toBaseBdt(salaryText.toDoubleOrNull() ?: 0.0, currency),
                            canManageInventory = permInv,
                            canManageSales = permSales,
                            canManageLivestock = permLive
                        )
                    )
                    showAdd = false
                }) { Text(strings.save) }
            },
            dismissButton = { TextButton(onClick = { showAdd = false }) { Text(strings.cancel) } }
        )
    }

    if (salaryTarget != null) {
        val emp = salaryTarget!!
        val defaultSalaryNote = tr("Monthly salary", "মাসিক বেতন")
        var amountText by remember { mutableStateOf(CurrencyFormatter.convertFromBase(emp.salary, currency).toInt().toString()) }
        var monthLabel by remember { mutableStateOf("November 2026") }
        var note by remember { mutableStateOf(defaultSalaryNote) }
        AlertDialog(
            onDismissRequest = { salaryTarget = null },
            title = { Text("${tr("Pay Salary", "বেতন দিন")}: ${emp.name}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = amountText, onValueChange = { amountText = it }, label = { Text("${tr("Amount", "পরিমাণ")} (${currency.symbol})") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true)
                    OutlinedTextField(value = monthLabel, onValueChange = { monthLabel = it }, label = { Text(tr("Month", "মাস")) }, singleLine = true)
                    OutlinedTextField(value = note, onValueChange = { note = it }, label = { Text(tr("Note", "নোট")) })
                }
            },
            confirmButton = {
                Button(onClick = {
                    onPaySalary(emp, CurrencyFormatter.toBaseBdt(amountText.toDoubleOrNull() ?: 0.0, currency), monthLabel, note)
                    salaryTarget = null
                }) { Text(tr("Confirm Payment", "পরিশোধ নিশ্চিত করুন")) }
            },
            dismissButton = { TextButton(onClick = { salaryTarget = null }) { Text(strings.cancel) } }
        )
    }
}
