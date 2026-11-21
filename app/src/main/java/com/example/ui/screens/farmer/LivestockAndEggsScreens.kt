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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.local.*
import com.example.i18n.AppStrings
import com.example.i18n.CurrencyFormatter
import com.example.i18n.CurrencyOption
import com.example.ui.components.*
import com.example.ui.state.FarmerDashboardState

@Composable
fun LivestockScreen(
    state: FarmerDashboardState,
    strings: AppStrings,
    currency: CurrencyOption,
    onSelectFarm: (Long?) -> Unit,
    onAddCategory: (String, Boolean) -> Unit,
    onSaveAnimal: (AnimalRecordEntity) -> Unit,
    onDeleteAnimal: (AnimalRecordEntity) -> Unit,
    onRecordEvent: (AnimalRecordEntity, AnimalEventType, Int, Double, String) -> Unit
) {
    var search by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    var showAddAnimal by remember { mutableStateOf(false) }
    var editingAnimal by remember { mutableStateOf<AnimalRecordEntity?>(null) }
    var eventTargetAnimal by remember { mutableStateOf<AnimalRecordEntity?>(null) }
    var showCustomCategoryDialog by remember { mutableStateOf(false) }

    val farmFiltered = remember(state.animals, state.selectedFarmId) {
        if (state.selectedFarmId == null) state.animals else state.animals.filter { it.farmId == state.selectedFarmId }
    }
    val displayed = remember(farmFiltered, search, selectedCategory) {
        farmFiltered.filter { a ->
            val q = search.isBlank() || a.name.contains(search, true) || a.tagNumber.contains(search, true) || a.breed.contains(search, true)
            val c = selectedCategory == null || a.category == selectedCategory
            q && c
        }
    }
    val farmsMap = remember(state.farms) { state.farms.associateBy { it.id } }

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddAnimal = true },
                icon = { Icon(Icons.Default.Pets, contentDescription = strings.addAnimal) },
                text = { Text(strings.addAnimal) },
                modifier = Modifier.testTag("add_animal_fab")
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(strings.livestock, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    TextButton(onClick = { showCustomCategoryDialog = true }) {
                        Icon(Icons.Default.AddCircleOutline, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Custom Type")
                    }
                }
                Spacer(Modifier.height(6.dp))
                FarmSwitcherBar(state.farms, state.selectedFarmId, strings.allFarms, onSelectFarm)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = search,
                    onValueChange = { search = it },
                    placeholder = { Text("Search name, tag #, breed...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        FilterChip(selected = selectedCategory == null, onClick = { selectedCategory = null }, label = { Text("All (${farmFiltered.sumOf { it.quantity }})") })
                    }
                    items(state.categories, key = { it.id }) { cat ->
                        FilterChip(
                            selected = selectedCategory == cat.name,
                            onClick = { selectedCategory = if (selectedCategory == cat.name) null else cat.name },
                            label = { Text(cat.name) }
                        )
                    }
                }
            }

            // Livestock & Bird Breakdown Donut Chart
            item {
                val palette = listOf(
                    Color(0xFF175B22), Color(0xFF2E7D32), Color(0xFF00796B),
                    Color(0xFFD98A00), Color(0xFF558B2F), Color(0xFF8D6E63)
                )
                val slices = farmFiltered.groupBy { it.category }.entries.mapIndexed { i, (cat, list) ->
                    ChartSlice(cat, list.sumOf { it.quantity }.toDouble(), palette[i % palette.size])
                }
                CategoryBreakdownDonut(title = strings.animalGrowth, slices = slices)
            }

            if (displayed.isEmpty()) {
                item {
                    EmptyStateCard(
                        icon = Icons.Default.Pets,
                        title = "No Animals or Birds",
                        subtitle = "Add cattle, goats, poultry, ducks, quail, or custom types.",
                        actionLabel = strings.addAnimal,
                        onAction = { showAddAnimal = true }
                    )
                }
            } else {
                items(displayed, key = { it.id }) { animal ->
                    val farmName = farmsMap[animal.farmId]?.name ?: "Farm"
                    val animalEvents = state.animalEvents.filter { it.animalRecordId == animal.id }
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
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.weight(1f)) {
                                    if (!animal.photoUri.isNullOrBlank()) {
                                        AsyncImage(
                                            model = animal.photoUri,
                                            contentDescription = animal.name,
                                            modifier = Modifier.size(48.dp)
                                        )
                                    } else {
                                        Surface(
                                            color = MaterialTheme.colorScheme.primaryContainer,
                                            shape = MaterialTheme.shapes.small,
                                            modifier = Modifier.size(44.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    if (animal.birdCategory) Icons.Default.FlutterDash else Icons.Default.Pets,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        }
                                    }
                                    Column {
                                        Text(animal.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                        Text("${animal.category} • ${animal.tagNumber} • $farmName", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                                AssistChip(onClick = {}, label = { Text("Qty: ${animal.quantity}", fontWeight = FontWeight.Bold) })
                            }

                            Text(
                                "Breed: ${animal.breed.ifBlank { "-" }} • Gender: ${animal.gender} • Age: ${animal.dobOrAge.ifBlank { "-" }} • Health: ${animal.healthStatus}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                "Purchase: ${CurrencyFormatter.format(animal.purchasePrice, currency)} (${formatShortDate(animal.purchaseDate)})",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (animal.notes.isNotBlank()) {
                                Text("Notes: ${animal.notes}", style = MaterialTheme.typography.labelSmall)
                            }
                            if (animalEvents.isNotEmpty()) {
                                val latest = animalEvents.first()
                                Text(
                                    "Latest Event: ${latest.eventType} (${latest.description}) • ${formatShortDate(latest.eventDate)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                FilledTonalButton(
                                    onClick = { eventTargetAnimal = animal },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.MedicalServices, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Log Event / Health")
                                }
                                IconButton(onClick = { editingAnimal = animal }) {
                                    Icon(Icons.Default.Edit, contentDescription = strings.edit)
                                }
                                IconButton(onClick = { onDeleteAnimal(animal) }) {
                                    Icon(Icons.Default.DeleteOutline, contentDescription = strings.delete, tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCustomCategoryDialog) {
        var catName by remember { mutableStateOf("") }
        var isBird by remember { mutableStateOf(false) }
        AlertDialog(
            onDismissRequest = { showCustomCategoryDialog = false },
            title = { Text("Add Custom Animal / Bird Type") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = catName, onValueChange = { catName = it }, label = { Text("Category Name (e.g., Emu, Rabbit)") }, singleLine = true)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = isBird, onCheckedChange = { isBird = it })
                        Text("Count as Bird / Poultry")
                    }
                }
            },
            confirmButton = {
                Button(enabled = catName.isNotBlank(), onClick = {
                    onAddCategory(catName, isBird)
                    showCustomCategoryDialog = false
                }) { Text(strings.save) }
            },
            dismissButton = { TextButton(onClick = { showCustomCategoryDialog = false }) { Text(strings.cancel) } }
        )
    }

    if (showAddAnimal || editingAnimal != null) {
        AnimalEditorDialog(
            farms = state.farms,
            categories = state.categories,
            defaultFarmId = state.selectedFarmId ?: state.farms.firstOrNull()?.id,
            initial = editingAnimal,
            currency = currency,
            onDismiss = { showAddAnimal = false; editingAnimal = null },
            onSave = { rec ->
                onSaveAnimal(rec)
                showAddAnimal = false
                editingAnimal = null
            }
        )
    }

    if (eventTargetAnimal != null) {
        AnimalEventDialog(
            animal = eventTargetAnimal!!,
            currency = currency,
            onDismiss = { eventTargetAnimal = null },
            onSave = { type, delta, cost, desc ->
                onRecordEvent(
                    eventTargetAnimal!!,
                    type,
                    delta,
                    CurrencyFormatter.toBaseBdt(cost, currency),
                    desc
                )
                eventTargetAnimal = null
            }
        )
    }
}

@Composable
private fun AnimalEditorDialog(
    farms: List<FarmEntity>,
    categories: List<AnimalCategoryEntity>,
    defaultFarmId: Long?,
    initial: AnimalRecordEntity?,
    currency: CurrencyOption,
    onDismiss: () -> Unit,
    onSave: (AnimalRecordEntity) -> Unit
) {
    var selectedFarmId by remember { mutableStateOf(initial?.farmId ?: defaultFarmId ?: 0L) }
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var category by remember { mutableStateOf(initial?.category ?: categories.firstOrNull()?.name ?: "Cow") }
    var tag by remember { mutableStateOf(initial?.tagNumber ?: "TAG-${(100..999).random()}") }
    var breed by remember { mutableStateOf(initial?.breed ?: "") }
    var gender by remember { mutableStateOf(initial?.gender ?: "Mixed") }
    var age by remember { mutableStateOf(initial?.dobOrAge ?: "") }
    var qtyText by remember { mutableStateOf((initial?.quantity ?: 1).toString()) }
    var priceText by remember {
        mutableStateOf(
            if (initial != null && initial.purchasePrice > 0)
                CurrencyFormatter.convertFromBase(initial.purchasePrice, currency).toInt().toString()
            else ""
        )
    }
    var health by remember { mutableStateOf(initial?.healthStatus ?: "Healthy") }
    var notes by remember { mutableStateOf(initial?.notes ?: "") }
    var photoUri by remember { mutableStateOf(initial?.photoUri) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri: Uri? ->
        if (uri != null) photoUri = uri.toString()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Add Animal / Bird" else "Edit Record") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 440.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (farms.isEmpty()) {
                    Text("Please create a Farm first.", color = MaterialTheme.colorScheme.error)
                } else {
                    Text("Select Farm", style = MaterialTheme.typography.labelLarge)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(farms) { f ->
                            FilterChip(selected = selectedFarmId == f.id, onClick = { selectedFarmId = f.id }, label = { Text(f.name) })
                        }
                    }
                    Text("Category", style = MaterialTheme.typography.labelLarge)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(categories) { c ->
                            FilterChip(selected = category == c.name, onClick = { category = c.name }, label = { Text(c.name) })
                        }
                    }
                    OutlinedButton(
                        onClick = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.PhotoCamera, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text(if (photoUri == null) "Add Photo" else "Photo Selected")
                    }
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Animal / Batch Name *") }, singleLine = true)
                    OutlinedTextField(value = tag, onValueChange = { tag = it }, label = { Text("Tag / ID Number") }, singleLine = true)
                    OutlinedTextField(value = breed, onValueChange = { breed = it }, label = { Text("Breed") }, singleLine = true)
                    OutlinedTextField(value = gender, onValueChange = { gender = it }, label = { Text("Gender (Male/Female/Mixed)") }, singleLine = true)
                    OutlinedTextField(value = age, onValueChange = { age = it }, label = { Text("DOB or Age") }, singleLine = true)
                    OutlinedTextField(
                        value = qtyText,
                        onValueChange = { qtyText = it },
                        label = { Text("Current Quantity") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = priceText,
                        onValueChange = { priceText = it },
                        label = { Text("Purchase Price (${currency.symbol})") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                    OutlinedTextField(value = health, onValueChange = { health = it }, label = { Text("Health Status") }, singleLine = true)
                    OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text("Notes") })
                }
            }
        },
        confirmButton = {
            Button(
                enabled = farms.isNotEmpty() && selectedFarmId > 0L && name.isNotBlank(),
                onClick = {
                    val isBird = categories.find { it.name == category }?.birdCategory ?: false
                    val basePrice = CurrencyFormatter.toBaseBdt(priceText.toDoubleOrNull() ?: 0.0, currency)
                    onSave(
                        (initial ?: AnimalRecordEntity(farmId = selectedFarmId, ownerUserId = 0L, name = name, category = category, tagNumber = tag)).copy(
                            farmId = selectedFarmId,
                            name = name.trim(),
                            category = category,
                            birdCategory = isBird,
                            tagNumber = tag.trim(),
                            breed = breed.trim(),
                            gender = gender.trim(),
                            dobOrAge = age.trim(),
                            purchasePrice = basePrice,
                            quantity = (qtyText.toIntOrNull() ?: 1).coerceAtLeast(0),
                            healthStatus = health.trim(),
                            notes = notes.trim(),
                            photoUri = photoUri
                        )
                    )
                }
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun AnimalEventDialog(
    animal: AnimalRecordEntity,
    currency: CurrencyOption,
    onDismiss: () -> Unit,
    onSave: (AnimalEventType, Int, Double, String) -> Unit
) {
    var eventType by remember { mutableStateOf(AnimalEventType.VACCINATION) }
    var deltaText by remember { mutableStateOf("1") }
    var costText by remember { mutableStateOf("0") }
    var desc by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Log Event: ${animal.name}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(AnimalEventType.entries) { t ->
                        FilterChip(selected = eventType == t, onClick = { eventType = t }, label = { Text(t.label) })
                    }
                }
                if (eventType in listOf(AnimalEventType.BIRTH, AnimalEventType.PURCHASE, AnimalEventType.DEATH, AnimalEventType.SALE, AnimalEventType.TRANSFER)) {
                    OutlinedTextField(
                        value = deltaText,
                        onValueChange = { deltaText = it },
                        label = { Text("Quantity Change") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                }
                OutlinedTextField(
                    value = costText,
                    onValueChange = { costText = it },
                    label = { Text("Cost / Value (${currency.symbol})") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
                OutlinedTextField(value = desc, onValueChange = { desc = it }, label = { Text("Details / Medicine / Notes") })
            }
        },
        confirmButton = {
            Button(onClick = {
                onSave(eventType, deltaText.toIntOrNull() ?: 0, costText.toDoubleOrNull() ?: 0.0, desc.ifBlank { eventType.label })
            }) { Text("Save Event") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun EggsScreen(
    state: FarmerDashboardState,
    strings: AppStrings,
    currency: CurrencyOption,
    onSelectFarm: (Long?) -> Unit,
    onRecordEggs: (Long, String, Int?, Int, Int, Int, Double) -> Unit,
    onDeleteEgg: (EggProductionEntity) -> Unit
) {
    var periodFilter by remember { mutableStateOf("All") } // Daily, Weekly, Monthly, Yearly, All
    var showAddDialog by remember { mutableStateOf(false) }

    val now = System.currentTimeMillis()
    val cutoff = when (periodFilter) {
        "Daily" -> now - 86_400_000L
        "Weekly" -> now - 7 * 86_400_000L
        "Monthly" -> now - 30 * 86_400_000L
        "Yearly" -> now - 365 * 86_400_000L
        else -> 0L
    }

    val filtered = remember(state.eggs, state.selectedFarmId, cutoff) {
        state.eggs.filter {
            (state.selectedFarmId == null || it.farmId == state.selectedFarmId) && it.productionDate >= cutoff
        }
    }
    val farmsMap = remember(state.farms) { state.farms.associateBy { it.id } }

    val totalProduced = filtered.sumOf { it.producedCount }
    val totalBroken = filtered.sumOf { it.brokenCount }
    val totalSold = filtered.sumOf { it.soldCount }
    val totalAvailable = filtered
        .groupBy { it.farmId to it.eggCategory }
        .values
        .sumOf { it.maxByOrNull { r -> r.productionDate }?.availableCount ?: 0 }
    val totalValue = filtered.sumOf { it.totalValue }

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddDialog = true },
                icon = { Icon(Icons.Default.Egg, contentDescription = strings.recordEggs) },
                text = { Text(strings.recordEggs) },
                modifier = Modifier.testTag("record_eggs_fab")
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
                Text(strings.eggs, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                FarmSwitcherBar(state.farms, state.selectedFarmId, strings.allFarms, onSelectFarm)
                Spacer(Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(listOf("All", "Daily", "Weekly", "Monthly", "Yearly")) { p ->
                        FilterChip(selected = periodFilter == p, onClick = { periodFilter = p }, label = { Text(p) })
                    }
                }
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    KpiMetricCard(strings.eggsProduced, "$totalProduced", Icons.Default.Egg, subtitle = "Broken: $totalBroken • Sold: $totalSold", modifier = Modifier.weight(1f))
                    KpiMetricCard(strings.eggsAvailable, "$totalAvailable", Icons.Default.Inventory2, subtitle = "Val: ${CurrencyFormatter.formatCompact(totalValue, currency)}", modifier = Modifier.weight(1f))
                }
            }

            item {
                val points = filtered.take(7).reversed().mapIndexed { idx, rec ->
                    "R${idx + 1}" to rec.producedCount.toDouble()
                }
                SmoothAreaLineChart(title = "${strings.eggProductionTrend} ($periodFilter)", points = points)
            }

            if (filtered.isEmpty()) {
                item {
                    EmptyStateCard(
                        icon = Icons.Default.Egg,
                        title = "No Egg Records",
                        subtitle = "Track daily Chicken, Duck, Quail, Pigeon, or Custom bird eggs.",
                        actionLabel = strings.recordEggs,
                        onAction = { showAddDialog = true }
                    )
                }
            } else {
                items(filtered, key = { it.id }) { egg ->
                    val farmName = farmsMap[egg.farmId]?.name ?: "Farm"
                    Surface(
                        shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 2.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("${egg.eggCategory} • $farmName", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text(
                                    "Prev: ${egg.previousStock} + New: ${egg.producedCount} - Sold: ${egg.soldCount} - Broken: ${egg.brokenCount} = Avail: ${egg.availableCount}",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    "${formatShortDate(egg.productionDate)} • Value: ${CurrencyFormatter.format(egg.totalValue, currency)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            IconButton(onClick = { onDeleteEgg(egg) }) {
                                Icon(Icons.Default.DeleteOutline, contentDescription = strings.delete, tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        RecordEggDialog(
            farms = state.farms,
            defaultFarmId = state.selectedFarmId ?: state.farms.firstOrNull()?.id,
            existingEggs = state.eggs,
            currency = currency,
            onDismiss = { showAddDialog = false },
            onSave = { farmId, cat, prevOverride, prod, broken, sold, price ->
                onRecordEggs(farmId, cat, prevOverride, prod, broken, sold, CurrencyFormatter.toBaseBdt(price, currency))
                showAddDialog = false
            }
        )
    }
}

@Composable
private fun RecordEggDialog(
    farms: List<FarmEntity>,
    defaultFarmId: Long?,
    existingEggs: List<EggProductionEntity>,
    currency: CurrencyOption,
    onDismiss: () -> Unit,
    onSave: (Long, String, Int?, Int, Int, Int, Double) -> Unit
) {
    val defaultCats = listOf("Chicken Eggs", "Duck Eggs", "Quail Eggs", "Pigeon Eggs", "Other Bird Eggs", "Custom")
    var selectedFarmId by remember { mutableStateOf(defaultFarmId ?: 0L) }
    var selectedCat by remember { mutableStateOf("Chicken Eggs") }
    var customCat by remember { mutableStateOf("") }
    var producedText by remember { mutableStateOf("") }
    var brokenText by remember { mutableStateOf("0") }
    var soldText by remember { mutableStateOf("0") }
    var priceText by remember { mutableStateOf("12") }

    val resolvedCat = if (selectedCat == "Custom") customCat.ifBlank { "Custom Eggs" } else selectedCat
    val latestPrev = existingEggs
        .filter { it.farmId == selectedFarmId && it.eggCategory == resolvedCat }
        .maxByOrNull { it.productionDate }?.availableCount ?: 0

    val prod = producedText.toIntOrNull() ?: 0
    val broken = brokenText.toIntOrNull() ?: 0
    val sold = soldText.toIntOrNull() ?: 0
    val availPreview = (latestPrev + prod - sold - broken).coerceAtLeast(0)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Record Daily Egg Production") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (farms.isEmpty()) {
                    Text("Create a farm first.", color = MaterialTheme.colorScheme.error)
                } else {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(farms) { f ->
                            FilterChip(selected = selectedFarmId == f.id, onClick = { selectedFarmId = f.id }, label = { Text(f.name) })
                        }
                    }
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(defaultCats) { c ->
                            FilterChip(selected = selectedCat == c, onClick = { selectedCat = c }, label = { Text(c) })
                        }
                    }
                    if (selectedCat == "Custom") {
                        OutlinedTextField(value = customCat, onValueChange = { customCat = it }, label = { Text("Custom Egg Category") }, singleLine = true)
                    }
                    Text("Previous Stock: $latestPrev eggs", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                    OutlinedTextField(value = producedText, onValueChange = { producedText = it }, label = { Text("Produced Today *") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true)
                    OutlinedTextField(value = brokenText, onValueChange = { brokenText = it }, label = { Text("Broken Eggs") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true)
                    OutlinedTextField(value = soldText, onValueChange = { soldText = it }, label = { Text("Sold Eggs") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true)
                    OutlinedTextField(value = priceText, onValueChange = { priceText = it }, label = { Text("Selling Price / Egg (${currency.symbol})") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true)
                    Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = MaterialTheme.shapes.small) {
                        Text(
                            "Auto Available: $availPreview eggs",
                            modifier = Modifier.padding(10.dp),
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                enabled = farms.isNotEmpty() && selectedFarmId > 0L && prod > 0,
                onClick = {
                    onSave(selectedFarmId, resolvedCat, latestPrev, prod, broken, sold, priceText.toDoubleOrNull() ?: 0.0)
                }
            ) { Text("Save Eggs") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
