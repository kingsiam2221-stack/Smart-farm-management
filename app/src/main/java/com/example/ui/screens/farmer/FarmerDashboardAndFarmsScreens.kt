package com.example.ui.screens.farmer

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
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
    onSelectFarm: (Long?) -> Unit,
    onOpenFarms: () -> Unit,
    onOpenLivestock: () -> Unit,
    onOpenEggs: () -> Unit,
    onOpenSales: () -> Unit,
    onOpenNotifications: () -> Unit,
    onOpenSubscription: () -> Unit,
    onOpenAiAdvisor: () -> Unit = {}
) {
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

    val activeSub = state.subscriptions.firstOrNull { it.status == "ACTIVE" }
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
                    .height(152.dp)
                    .clip(RoundedCornerShape(20.dp))
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_farm_hero),
                    contentDescription = "Smart Farm Banner",
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
                        Column {
                            Text(
                                "${strings.welcomeBack}, ${user.name}",
                                style = MaterialTheme.typography.titleLarge,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                if (activeSub != null) "${activeSub.planName} • ${strings.active}"
                                else "Subscription inactive • Tap to activate",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFFB9F6CA),
                                modifier = Modifier.clickable { onOpenSubscription() }
                            )
                        }
                        BadgedBox(
                            badge = {
                                if (unreadNotificationsCount > 0) {
                                    Badge { Text("$unreadNotificationsCount") }
                                }
                            }
                        ) {
                            IconButton(
                                onClick = onOpenNotifications,
                                colors = IconButtonDefaults.iconButtonColors(containerColor = Color.White.copy(alpha = 0.2f))
                            ) {
                                Icon(Icons.Default.Notifications, contentDescription = strings.notifications, tint = Color.White)
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
        item {
            FarmSwitcherBar(
                farms = state.farms,
                selectedFarmId = state.selectedFarmId,
                allFarmsLabel = strings.allFarms,
                onSelectFarm = onSelectFarm
            )
        }

        // Alerts Strip (Pending Orders / Low Inventory)
        if (pendingOrdersCount > 0 || lowStockCount > 0) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (pendingOrdersCount > 0) {
                        AssistChip(
                            onClick = onOpenSales,
                            label = { Text("$pendingOrdersCount New Orders") },
                            leadingIcon = { Icon(Icons.Default.LocalShipping, contentDescription = null, modifier = Modifier.size(16.dp)) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    if (lowStockCount > 0) {
                        AssistChip(
                            onClick = onOpenFarms,
                            label = { Text("$lowStockCount Low Stock Alerts") },
                            leadingIcon = { Icon(Icons.Default.WarningAmber, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.tertiary) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // Quick Actions Row
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                item {
                    QuickActionPill(Icons.Default.AutoAwesome, "AI Advisor", onOpenAiAdvisor, testTag = "open_ai_advisor_pill")
                }
                item {
                    QuickActionPill(Icons.Default.AddBusiness, strings.addFarm, onOpenFarms)
                }
                item {
                    QuickActionPill(Icons.Default.Pets, strings.addAnimal, onOpenLivestock)
                }
                item {
                    QuickActionPill(Icons.Default.Egg, strings.recordEggs, onOpenEggs)
                }
                item {
                    QuickActionPill(Icons.Default.PointOfSale, strings.newSale, onOpenSales)
                }
            }
        }

        // Gemini AI Farm Advisor Highlight Card
        item {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.72f),
                tonalElevation = 2.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("open_ai_advisor_button")
                    .clickable { onOpenAiAdvisor() }
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
                                    contentDescription = "AI Farm Advisor",
                                    tint = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                        }
                        Column {
                            Text(
                                "Smart Farm AI Advisor",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                "Ask questions, run a live Farm Audit, or inspect livestock/feed photos",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                            )
                        }
                    }
                    FilledTonalButton(onClick = onOpenAiAdvisor) {
                        Text("Ask AI")
                    }
                }
            }
        }

        // Financial & Livestock KPI Grid (2-column pairs)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    KpiMetricCard(
                        title = strings.netProfitLoss,
                        value = CurrencyFormatter.formatCompact(netProfit, currency),
                        icon = Icons.Default.TrendingUp,
                        tint = if (netProfit >= 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                        subtitle = if (netProfit >= 0) "Profitable" else "Net Loss",
                        modifier = Modifier.weight(1f)
                    )
                    KpiMetricCard(
                        title = strings.totalFarms,
                        value = "${state.farms.size}",
                        icon = Icons.Default.Agriculture,
                        subtitle = "Multi-farm active",
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    KpiMetricCard(
                        title = strings.totalSales,
                        value = CurrencyFormatter.formatCompact(totalSales, currency),
                        icon = Icons.Default.ShoppingBag,
                        subtitle = "Paid: ${CurrencyFormatter.formatCompact(totalIncome, currency)}",
                        modifier = Modifier.weight(1f)
                    )
                    KpiMetricCard(
                        title = strings.totalExpenses,
                        value = CurrencyFormatter.formatCompact(totalExpenses, currency),
                        icon = Icons.Default.ReceiptLong,
                        tint = MaterialTheme.colorScheme.error,
                        subtitle = "Due: ${CurrencyFormatter.formatCompact(totalDue, currency)}",
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

        // Charts: Farm-wise Sales vs Expenses + Egg Production Trend
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
                "D${idx + 1}" to egg.producedCount.toDouble()
            }
            SmoothAreaLineChart(
                title = strings.eggProductionTrend,
                points = eggPoints
            )
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
    onSelectFarm: (Long?) -> Unit,
    onSaveFarm: (FarmEntity) -> Unit,
    onDeleteFarm: (FarmEntity) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    var editingFarm by remember { mutableStateOf<FarmEntity?>(null) }
    var showCreateDialog by remember { mutableStateOf(false) }

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
            ExtendedFloatingActionButton(
                onClick = { showCreateDialog = true },
                icon = { Icon(Icons.Default.AddBusiness, contentDescription = strings.addFarm) },
                text = { Text(strings.addFarm) },
                modifier = Modifier.testTag("add_farm_fab")
            )
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
                Text(
                    "${strings.farms} (${state.farms.size})",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
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
                            label = { Text("All") }
                        )
                    }
                    items(FarmCategory.entries) { cat ->
                        FilterChip(
                            selected = selectedCategory == cat.name,
                            onClick = { selectedCategory = if (selectedCategory == cat.name) null else cat.name },
                            label = { Text(cat.label) }
                        )
                    }
                }
            }

            if (filteredFarms.isEmpty()) {
                item {
                    EmptyStateCard(
                        icon = Icons.Default.Agriculture,
                        title = "No Farms Found",
                        subtitle = "Create unlimited independent farms (1, 10, 50, 100+).",
                        actionLabel = strings.addFarm,
                        onAction = { showCreateDialog = true }
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
                                        "${farm.farmCode} • ${farm.customCategoryName.ifBlank { FarmCategory.entries.find { it.name == farm.category }?.label ?: farm.category }}",
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
                                            "Active",
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
                                    Row {
                                        IconButton(onClick = { editingFarm = farm }) {
                                            Icon(Icons.Default.Edit, contentDescription = strings.edit)
                                        }
                                        IconButton(onClick = { onDeleteFarm(farm) }) {
                                            Icon(Icons.Default.DeleteOutline, contentDescription = strings.delete, tint = MaterialTheme.colorScheme.error)
                                        }
                                    }
                                }

                                Text(
                                    "Manager: ${farm.managerName.ifBlank { "Unassigned" }} (${farm.managerPhone.ifBlank { "-" }}) • Opened ${formatShortDate(farm.openingDate)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    FarmStatChip("Livestock", "$farmAnimals")
                                    FarmStatChip("Investment", CurrencyFormatter.formatCompact(farm.initialInvestment, currency))
                                    FarmStatChip("Income", CurrencyFormatter.formatCompact(farmIncome, currency))
                                    FarmStatChip("Expenses", CurrencyFormatter.formatCompact(farmExpenses, currency))
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        "Net P/L: ${CurrencyFormatter.format(farmNet, currency)}",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (farmNet >= 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                                    )
                                    FilledTonalButton(onClick = { onSelectFarm(if (isActive) null else farm.id) }) {
                                        Text(if (isActive) "Show All Farms" else "Focus This Farm")
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

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri: Uri? ->
        if (uri != null) imageUri = uri.toString()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Create New Farm" else "Edit Farm") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 440.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.PhotoLibrary, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(if (imageUri == null) "Choose Farm Image" else "Change Selected Image")
                }
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Farm Name *") }, singleLine = true)
                OutlinedTextField(value = farmCode, onValueChange = { farmCode = it }, label = { Text("Farm ID") }, singleLine = true)
                OutlinedTextField(value = location, onValueChange = { location = it }, label = { Text("Location (City/District)") }, singleLine = true)
                OutlinedTextField(value = address, onValueChange = { address = it }, label = { Text("Full Address") })

                Text("Farm Category", style = MaterialTheme.typography.labelLarge)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(FarmCategory.entries) { cat ->
                        FilterChip(
                            selected = category == cat.name,
                            onClick = { category = cat.name },
                            label = { Text(cat.label) }
                        )
                    }
                }
                if (category == FarmCategory.CUSTOM.name) {
                    OutlinedTextField(value = customCat, onValueChange = { customCat = it }, label = { Text("Custom Category Name") }, singleLine = true)
                }
                OutlinedTextField(value = managerName, onValueChange = { managerName = it }, label = { Text("Manager Name") }, singleLine = true)
                OutlinedTextField(value = managerPhone, onValueChange = { managerPhone = it }, label = { Text("Manager Phone") }, singleLine = true)
                OutlinedTextField(
                    value = investmentText,
                    onValueChange = { investmentText = it },
                    label = { Text("Initial Investment (${currency.symbol})") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                enabled = name.isNotBlank(),
                onClick = {
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
                }
            ) { Text("Save Farm") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
