package com.example.ui.screens.farmer

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.local.*
import com.example.i18n.AppStrings
import com.example.i18n.CurrencyFormatter
import com.example.i18n.CurrencyOption
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.FarmSwitcherBar
import com.example.ui.components.KpiMetricCard
import com.example.ui.components.formatShortDate
import com.example.ui.state.FarmerDashboardState
import com.example.util.ReportExportHelper

@Composable
fun CommerceScreen(
    state: FarmerDashboardState,
    strings: AppStrings,
    currency: CurrencyOption,
    onSelectFarm: (Long?) -> Unit,
    onCreateSale: (Long, Long?, String, String, String, String, InventoryItemEntity?, AnimalRecordEntity?, Double, Double, Double, Double, String) -> Unit,
    onSettleDue: (SaleEntity, Double) -> Unit,
    onDeleteSale: (SaleEntity) -> Unit,
    onSaveCustomer: (CustomerEntity) -> Unit,
    onDeleteCustomer: (CustomerEntity) -> Unit,
    onUpdateOrderStatus: (OrderEntity, OrderStatus) -> Unit,
    onMessage: (String) -> Unit
) {
    var tabIndex by remember { mutableIntStateOf(0) }
    val pendingOrders = state.incomingOrders.count { it.status == OrderStatus.PENDING.name }
    val tabs = listOf("Sales", "Customers", if (pendingOrders > 0) "Orders ($pendingOrders)" else "Orders")

    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = tabIndex) {
            tabs.forEachIndexed { idx, title ->
                Tab(selected = tabIndex == idx, onClick = { tabIndex = idx }, text = { Text(title) })
            }
        }
        when (tabIndex) {
            0 -> SalesTab(state, strings, currency, onSelectFarm, onCreateSale, onSettleDue, onDeleteSale, onMessage)
            1 -> CustomersTab(state, strings, currency, onSaveCustomer, onDeleteCustomer)
            2 -> IncomingOrdersTab(state, currency, onUpdateOrderStatus)
        }
    }
}

@Composable
private fun SalesTab(
    state: FarmerDashboardState,
    strings: AppStrings,
    currency: CurrencyOption,
    onSelectFarm: (Long?) -> Unit,
    onCreateSale: (Long, Long?, String, String, String, String, InventoryItemEntity?, AnimalRecordEntity?, Double, Double, Double, Double, String) -> Unit,
    onSettleDue: (SaleEntity, Double) -> Unit,
    onDeleteSale: (SaleEntity) -> Unit,
    onMessage: (String) -> Unit
) {
    val context = LocalContext.current
    var showCreate by remember { mutableStateOf(false) }
    var invoiceSale by remember { mutableStateOf<SaleEntity?>(null) }
    var settleTarget by remember { mutableStateOf<SaleEntity?>(null) }

    val filteredSales = remember(state.sales, state.selectedFarmId) {
        if (state.selectedFarmId == null) state.sales else state.sales.filter { it.farmId == state.selectedFarmId }
    }
    val farmsMap = remember(state.farms) { state.farms.associateBy { it.id } }

    val totalSales = filteredSales.sumOf { it.totalAmount }
    val totalPaid = filteredSales.sumOf { it.paidAmount }
    val totalDue = filteredSales.sumOf { it.dueAmount }

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showCreate = true },
                icon = { Icon(Icons.Default.PointOfSale, contentDescription = strings.newSale) },
                text = { Text(strings.newSale) }
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
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    KpiMetricCard(strings.totalSales, CurrencyFormatter.formatCompact(totalSales, currency), Icons.Default.PointOfSale, subtitle = "Paid: ${CurrencyFormatter.formatCompact(totalPaid, currency)}", modifier = Modifier.weight(1f))
                    KpiMetricCard(strings.outstandingPayments, CurrencyFormatter.formatCompact(totalDue, currency), Icons.Default.AccountBalanceWallet, tint = if (totalDue > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary, subtitle = "${filteredSales.size} invoices", modifier = Modifier.weight(1f))
                }
            }

            if (filteredSales.isEmpty()) {
                item {
                    EmptyStateCard(
                        icon = Icons.Default.PointOfSale,
                        title = "No Sales Yet",
                        subtitle = "Sell livestock, poultry, eggs, milk, meat, or farm produce.",
                        actionLabel = strings.newSale,
                        onAction = { showCreate = true }
                    )
                }
            } else {
                items(filteredSales, key = { it.id }) { sale ->
                    val farmName = farmsMap[sale.farmId]?.name ?: "Farm"
                    Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surface, tonalElevation = 2.dp, modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("${sale.invoiceNumber} • ${sale.productName}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    Text("${sale.customerName} (${sale.customerPhone}) • $farmName", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Text(CurrencyFormatter.format(sale.totalAmount, currency), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            }
                            Text(
                                "Qty: ${sale.quantity} • Paid: ${CurrencyFormatter.format(sale.paidAmount, currency)} • Due: ${CurrencyFormatter.format(sale.dueAmount, currency)} • ${sale.paymentMethod} • ${formatShortDate(sale.saleDate)}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                FilledTonalButton(onClick = { invoiceSale = sale }, modifier = Modifier.weight(1f)) {
                                    Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Invoice / PDF")
                                }
                                if (sale.dueAmount > 0.0) {
                                    OutlinedButton(onClick = { settleTarget = sale }, modifier = Modifier.weight(1f)) {
                                        Text("Collect Due")
                                    }
                                }
                                IconButton(onClick = { onDeleteSale(sale) }) {
                                    Icon(Icons.Default.DeleteOutline, contentDescription = strings.delete, tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreate) {
        CreateSaleDialog(
            state = state,
            currency = currency,
            onDismiss = { showCreate = false },
            onSave = { fId, cId, cName, cPhone, pCat, pName, invItem, animRec, qty, unitP, disc, paid, method ->
                onCreateSale(
                    fId, cId, cName, cPhone, pCat, pName, invItem, animRec, qty,
                    CurrencyFormatter.toBaseBdt(unitP, currency),
                    CurrencyFormatter.toBaseBdt(disc, currency),
                    CurrencyFormatter.toBaseBdt(paid, currency),
                    method
                )
                showCreate = false
            }
        )
    }

    if (invoiceSale != null) {
        val sale = invoiceSale!!
        val farmName = farmsMap[sale.farmId]?.name ?: "Smart Farm"
        AlertDialog(
            onDismissRequest = { invoiceSale = null },
            title = { Text("Invoice ${sale.invoiceNumber}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Farm: $farmName", fontWeight = FontWeight.Bold)
                    Text("Customer: ${sale.customerName} (${sale.customerPhone})")
                    Text("Date: ${formatShortDate(sale.saleDate)}")
                    HorizontalDivider()
                    Text("${sale.productName} (${sale.productCategory})")
                    Text("Qty: ${sale.quantity} × ${CurrencyFormatter.format(sale.unitPrice, currency)}")
                    Text("Discount: ${CurrencyFormatter.format(sale.discount, currency)}")
                    Text("Net Total: ${CurrencyFormatter.format(sale.totalAmount, currency)}", fontWeight = FontWeight.Bold)
                    Text("Paid: ${CurrencyFormatter.format(sale.paidAmount, currency)}")
                    Text("Due: ${CurrencyFormatter.format(sale.dueAmount, currency)}", color = if (sale.dueAmount > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)
                    Text("Method: ${sale.paymentMethod}")
                }
            },
            confirmButton = {
                Button(onClick = {
                    ReportExportHelper.exportInvoicePdf(context, sale, farmName, currency)
                        .onFailure { onMessage("Unable to export PDF: ${it.message}") }
                    invoiceSale = null
                }) {
                    Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Download / Print PDF")
                }
            },
            dismissButton = { TextButton(onClick = { invoiceSale = null }) { Text("Close") } }
        )
    }

    if (settleTarget != null) {
        val s = settleTarget!!
        var amtText by remember { mutableStateOf(CurrencyFormatter.convertFromBase(s.dueAmount, currency).toInt().toString()) }
        AlertDialog(
            onDismissRequest = { settleTarget = null },
            title = { Text("Collect Due Payment (${s.invoiceNumber})") },
            text = {
                OutlinedTextField(
                    value = amtText,
                    onValueChange = { amtText = it },
                    label = { Text("Amount Collected (${currency.symbol})") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
            },
            confirmButton = {
                Button(onClick = {
                    onSettleDue(s, CurrencyFormatter.toBaseBdt(amtText.toDoubleOrNull() ?: 0.0, currency))
                    settleTarget = null
                }) { Text("Confirm") }
            },
            dismissButton = { TextButton(onClick = { settleTarget = null }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun CreateSaleDialog(
    state: FarmerDashboardState,
    currency: CurrencyOption,
    onDismiss: () -> Unit,
    onSave: (Long, Long?, String, String, String, String, InventoryItemEntity?, AnimalRecordEntity?, Double, Double, Double, Double, String) -> Unit
) {
    val cats = listOf("Eggs", "Milk", "Meat", "Live Animals", "Chickens", "Ducks", "Cows", "Goats", "Birds", "Other")
    var farmId by remember { mutableStateOf(state.selectedFarmId ?: state.farms.firstOrNull()?.id ?: 0L) }
    var selectedCustomer by remember { mutableStateOf<CustomerEntity?>(null) }
    var custName by remember { mutableStateOf("") }
    var custPhone by remember { mutableStateOf("") }
    var prodCat by remember { mutableStateOf("Eggs") }
    var prodName by remember { mutableStateOf("") }
    var selectedInv by remember { mutableStateOf<InventoryItemEntity?>(null) }
    var selectedAnim by remember { mutableStateOf<AnimalRecordEntity?>(null) }
    var qtyText by remember { mutableStateOf("1") }
    var priceText by remember { mutableStateOf("100") }
    var discountText by remember { mutableStateOf("0") }
    var paidText by remember { mutableStateOf("") }
    var payMethod by remember { mutableStateOf(currency.gateways.firstOrNull() ?: "Cash") }

    val qty = qtyText.toDoubleOrNull() ?: 0.0
    val unitPrice = priceText.toDoubleOrNull() ?: 0.0
    val disc = discountText.toDoubleOrNull() ?: 0.0
    val totalPreview = (qty * unitPrice - disc).coerceAtLeast(0.0)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create Sales Invoice") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().heightIn(max = 450.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Farm", style = MaterialTheme.typography.labelLarge)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(state.farms) { f -> FilterChip(selected = farmId == f.id, onClick = { farmId = f.id }, label = { Text(f.name) }) }
                }
                if (state.customers.isNotEmpty()) {
                    Text("Customer (optional)", style = MaterialTheme.typography.labelLarge)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(state.customers) { c ->
                            FilterChip(
                                selected = selectedCustomer?.id == c.id,
                                onClick = {
                                    selectedCustomer = if (selectedCustomer?.id == c.id) null else c
                                    if (selectedCustomer != null) {
                                        custName = c.name
                                        custPhone = c.mobile
                                    }
                                },
                                label = { Text(c.name) }
                            )
                        }
                    }
                }
                OutlinedTextField(value = custName, onValueChange = { custName = it }, label = { Text("Customer Name") }, singleLine = true)
                OutlinedTextField(value = custPhone, onValueChange = { custPhone = it }, label = { Text("Customer Phone") }, singleLine = true)

                if (state.inventory.isNotEmpty()) {
                    Text("Link Inventory Item (auto-reduces stock)", style = MaterialTheme.typography.labelSmall)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(state.inventory.filter { it.farmId == farmId }) { item ->
                            FilterChip(
                                selected = selectedInv?.id == item.id,
                                onClick = {
                                    selectedInv = if (selectedInv?.id == item.id) null else item
                                    selectedAnim = null
                                    if (selectedInv != null) {
                                        prodName = item.name
                                        prodCat = item.category
                                        val sp = if (item.sellingPrice > 0) item.sellingPrice else item.unitCost
                                        priceText = CurrencyFormatter.convertFromBase(sp, currency).toInt().toString()
                                    }
                                },
                                label = { Text("${item.name} (${item.quantity})") }
                            )
                        }
                    }
                }
                if (state.animals.isNotEmpty()) {
                    Text("Or Link Livestock Batch (auto-reduces count)", style = MaterialTheme.typography.labelSmall)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(state.animals.filter { it.farmId == farmId && it.quantity > 0 }) { anim ->
                            FilterChip(
                                selected = selectedAnim?.id == anim.id,
                                onClick = {
                                    selectedAnim = if (selectedAnim?.id == anim.id) null else anim
                                    selectedInv = null
                                    if (selectedAnim != null) {
                                        prodName = anim.name
                                        prodCat = anim.category
                                    }
                                },
                                label = { Text("${anim.name} (${anim.quantity})") }
                            )
                        }
                    }
                }

                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(cats) { c -> FilterChip(selected = prodCat == c, onClick = { prodCat = c }, label = { Text(c) }) }
                }
                OutlinedTextField(value = prodName, onValueChange = { prodName = it }, label = { Text("Product Name *") }, singleLine = true)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = qtyText, onValueChange = { qtyText = it }, label = { Text("Qty") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f), singleLine = true)
                    OutlinedTextField(value = priceText, onValueChange = { priceText = it }, label = { Text("Unit (${currency.symbol})") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f), singleLine = true)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = discountText, onValueChange = { discountText = it }, label = { Text("Discount") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f), singleLine = true)
                    OutlinedTextField(value = paidText, onValueChange = { paidText = it }, label = { Text("Paid (default full)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f), singleLine = true)
                }
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(listOf("Cash", "Bank Transfer") + currency.gateways) { m ->
                        FilterChip(selected = payMethod == m, onClick = { payMethod = m }, label = { Text(m) })
                    }
                }
                Text("Total Payable: ${currency.symbol}${totalPreview.toInt()}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            }
        },
        confirmButton = {
            Button(
                enabled = farmId > 0L && prodName.isNotBlank() && qty > 0.0,
                onClick = {
                    val paid = paidText.toDoubleOrNull() ?: totalPreview
                    onSave(
                        farmId, selectedCustomer?.id, custName, custPhone, prodCat, prodName.trim(),
                        selectedInv, selectedAnim, qty, unitPrice, disc, paid, payMethod
                    )
                }
            ) { Text("Complete Sale") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun CustomersTab(
    state: FarmerDashboardState,
    strings: AppStrings,
    currency: CurrencyOption,
    onSaveCustomer: (CustomerEntity) -> Unit,
    onDeleteCustomer: (CustomerEntity) -> Unit
) {
    var showAdd by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<CustomerEntity?>(null) }

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAdd = true },
                icon = { Icon(Icons.Default.PersonAdd, contentDescription = strings.addCustomer) },
                text = { Text(strings.addCustomer) }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 88.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (state.customers.isEmpty()) {
                item {
                    EmptyStateCard(
                        icon = Icons.Default.People,
                        title = "No Customers",
                        subtitle = "Track customer profiles, purchase history, payments, and due balances.",
                        actionLabel = strings.addCustomer,
                        onAction = { showAdd = true }
                    )
                }
            } else {
                items(state.customers, key = { it.id }) { cust ->
                    val custSales = state.sales.filter { it.customerId == cust.id || it.customerPhone == cust.mobile }
                    val totalBuy = custSales.sumOf { it.totalAmount }
                    val totalPaid = custSales.sumOf { it.paidAmount }
                    val totalDue = custSales.sumOf { it.dueAmount }

                    Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surface, tonalElevation = 2.dp, modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.weight(1f)) {
                                    if (!cust.photoUri.isNullOrBlank()) {
                                        AsyncImage(model = cust.photoUri, contentDescription = cust.name, contentScale = ContentScale.Crop, modifier = Modifier.size(44.dp).clip(CircleShape))
                                    } else {
                                        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.size(44.dp)) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Text(cust.name.take(1).uppercase(), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                            }
                                        }
                                    }
                                    Column {
                                        Text("${cust.name} (${cust.customerCode})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                        Text("${cust.mobile} • ${cust.email.ifBlank { "-" }}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                                Row {
                                    IconButton(onClick = { editing = cust }) { Icon(Icons.Default.Edit, contentDescription = strings.edit) }
                                    IconButton(onClick = { onDeleteCustomer(cust) }) { Icon(Icons.Default.DeleteOutline, contentDescription = strings.delete, tint = MaterialTheme.colorScheme.error) }
                                }
                            }
                            if (cust.address.isNotBlank()) Text("Address: ${cust.address}", style = MaterialTheme.typography.bodyMedium)
                            Text(
                                "Orders: ${custSales.size} • Purchases: ${CurrencyFormatter.format(totalBuy, currency)} • Paid: ${CurrencyFormatter.format(totalPaid, currency)} • Due: ${CurrencyFormatter.format(totalDue, currency)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (totalDue > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    }

    if (showAdd || editing != null) {
        var name by remember { mutableStateOf(editing?.name ?: "") }
        var mobile by remember { mutableStateOf(editing?.mobile ?: "") }
        var email by remember { mutableStateOf(editing?.email ?: "") }
        var address by remember { mutableStateOf(editing?.address ?: "") }
        var photoUri by remember { mutableStateOf(editing?.photoUri) }
        val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri: Uri? ->
            if (uri != null) photoUri = uri.toString()
        }

        AlertDialog(
            onDismissRequest = { showAdd = false; editing = null },
            title = { Text(if (editing == null) "Add Customer" else "Edit Customer") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.PhotoCamera, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text(if (photoUri == null) "Add Photo" else "Photo Selected")
                    }
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Customer Name *") }, singleLine = true)
                    OutlinedTextField(value = mobile, onValueChange = { mobile = it }, label = { Text("Mobile Number *") }, singleLine = true)
                    OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email") }, singleLine = true)
                    OutlinedTextField(value = address, onValueChange = { address = it }, label = { Text("Address") })
                }
            },
            confirmButton = {
                Button(enabled = name.isNotBlank() && mobile.isNotBlank(), onClick = {
                    onSaveCustomer(
                        (editing ?: CustomerEntity(ownerUserId = 0L, customerCode = "", name = name, mobile = mobile)).copy(
                            name = name.trim(),
                            mobile = mobile.trim(),
                            email = email.trim(),
                            address = address.trim(),
                            photoUri = photoUri
                        )
                    )
                    showAdd = false
                    editing = null
                }) { Text(strings.save) }
            },
            dismissButton = { TextButton(onClick = { showAdd = false; editing = null }) { Text(strings.cancel) } }
        )
    }
}

@Composable
private fun IncomingOrdersTab(
    state: FarmerDashboardState,
    currency: CurrencyOption,
    onUpdateOrderStatus: (OrderEntity, OrderStatus) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (state.incomingOrders.isEmpty()) {
            item {
                EmptyStateCard(
                    icon = Icons.Default.LocalShipping,
                    title = "No Online Orders",
                    subtitle = "List inventory items for online sale to receive customer orders here."
                )
            }
        } else {
            items(state.incomingOrders, key = { it.id }) { order ->
                val currentStatus = OrderStatus.entries.firstOrNull { it.name == order.status } ?: OrderStatus.PENDING
                Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surface, tonalElevation = 2.dp, modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("${order.orderNumber} • ${order.farmName}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            AssistChip(onClick = {}, label = { Text(currentStatus.label, fontWeight = FontWeight.Bold) })
                        }
                        if (order.cancellationRequested && currentStatus != OrderStatus.CANCELLED) {
                            Text("Customer requested cancellation!", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelLarge)
                        }
                        Text("Items: ${order.itemsSummary}", style = MaterialTheme.typography.bodyMedium)
                        Text("Customer: ${order.customerName} (${order.customerPhone}) • ${order.deliveryAddress}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("Total: ${CurrencyFormatter.format(order.totalAmount, currency)} (${order.paymentMethod}) • ${formatShortDate(order.createdAt)}", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)

                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(OrderStatus.entries) { st ->
                                FilterChip(
                                    selected = currentStatus == st,
                                    onClick = { onUpdateOrderStatus(order, st) },
                                    label = { Text(st.label) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
