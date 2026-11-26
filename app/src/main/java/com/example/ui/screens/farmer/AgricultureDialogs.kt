package com.example.ui.screens.farmer

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AgriLandDialog(
    initial: AgriLandEntity?,
    farms: List<FarmEntity>,
    defaultFarmId: Long?,
    strings: AppStrings,
    onDismiss: () -> Unit,
    onSave: (AgriLandEntity) -> Unit
) {
    val isBn = isBangla()
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var areaSizeText by remember { mutableStateOf(initial?.areaSize?.takeIf { it > 0.0 }?.toString() ?: "") }
    var areaUnit by remember { mutableStateOf(initial?.areaUnit ?: AgriLandUnit.DECIMAL.key) }
    var selectedFarmId by remember { mutableStateOf(initial?.farmId ?: defaultFarmId) }
    var location by remember { mutableStateOf(initial?.location ?: "") }
    var soilType by remember { mutableStateOf(initial?.soilOrLandType ?: "") }
    var notes by remember { mutableStateOf(initial?.notes ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (initial == null) tr("Add Piece of Land", "নতুন জমির তথ্য যোগ")
                else tr("Edit Land Plot", "জমির তথ্য সম্পাদনা")
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 460.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(tr("Land Name / Plot Number *", "জমির নাম / দাগ নম্বর *")) },
                    placeholder = { Text(tr("e.g. North Paddy Plot 1", "যেমন: উত্তর বিলের জমি")) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("agri_land_name_input")
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = areaSizeText,
                        onValueChange = { areaSizeText = it },
                        label = { Text(tr("Area Size", "জমির পরিমাণ")) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f).testTag("agri_land_area_input")
                    )
                }
                Text(tr("Land Unit", "জমির একক"), style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    AgriLandUnit.entries.forEach { u ->
                        FilterChip(
                            selected = areaUnit == u.key,
                            onClick = { areaUnit = u.key },
                            label = { Text(if (isBn) u.labelBn else u.labelEn) }
                        )
                    }
                }
                if (farms.isNotEmpty()) {
                    Text(tr("Link to Farm (Optional)", "খামারের সাথে যুক্ত (ঐচ্ছিক)"), style = MaterialTheme.typography.labelLarge)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        FilterChip(
                            selected = selectedFarmId == null,
                            onClick = { selectedFarmId = null },
                            label = { Text(tr("Independent / All", "স্বতন্ত্র জমি")) }
                        )
                        farms.forEach { farm ->
                            FilterChip(
                                selected = selectedFarmId == farm.id,
                                onClick = { selectedFarmId = farm.id },
                                label = { Text(farm.name) }
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = location,
                    onValueChange = { location = it },
                    label = { Text(tr("Location / Mouza", "অবস্থান / এলাকা")) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = soilType,
                    onValueChange = { soilType = it },
                    label = { Text(tr("Soil / Land Type", "মাটির ধরন (দোআঁশ/বেলে)")) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text(tr("Notes", "নোট")) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                enabled = name.isNotBlank(),
                onClick = {
                    val area = areaSizeText.toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0
                    onSave(
                        (initial ?: AgriLandEntity(ownerUserId = 0L, name = name.trim())).copy(
                            farmId = selectedFarmId,
                            name = name.trim(),
                            areaSize = area,
                            areaUnit = areaUnit,
                            location = location.trim(),
                            soilOrLandType = soilType.trim(),
                            notes = notes.trim()
                        )
                    )
                },
                modifier = Modifier.testTag("save_agri_land_button")
            ) { Text(strings.save) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(strings.cancel) } }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AgriCropDialog(
    initial: AgriCropEntity?,
    lands: List<AgriLandEntity>,
    defaultLandId: Long?,
    currency: CurrencyOption,
    strings: AppStrings,
    onDismiss: () -> Unit,
    onSave: (AgriCropEntity) -> Unit
) {
    val isBn = isBangla()
    var selectedLandId by remember { mutableStateOf(initial?.landId ?: defaultLandId ?: lands.firstOrNull()?.id ?: 0L) }
    var cropType by remember { mutableStateOf(initial?.cropType ?: AgriCropType.RICE.key) }
    var title by remember { mutableStateOf(initial?.title ?: "") }
    var variety by remember { mutableStateOf(initial?.variety ?: "") }
    var season by remember { mutableStateOf(initial?.season ?: AgriSeason.BORO.key) }
    var status by remember { mutableStateOf(initial?.status ?: AgriCropStatus.GROWING.key) }
    var yieldQtyText by remember { mutableStateOf(initial?.yieldQuantity?.takeIf { it > 0.0 }?.toString() ?: "") }
    var yieldUnit by remember { mutableStateOf(initial?.yieldUnit ?: "mon") }
    var marketPriceText by remember {
        mutableStateOf(
            initial?.marketPricePerUnit?.takeIf { it > 0.0 }?.let {
                CurrencyFormatter.convertFromBase(it, currency).toInt().toString()
            } ?: ""
        )
    }
    var soldAmountText by remember {
        mutableStateOf(
            initial?.soldAmount?.takeIf { it > 0.0 }?.let {
                CurrencyFormatter.convertFromBase(it, currency).toInt().toString()
            } ?: ""
        )
    }
    var notes by remember { mutableStateOf(initial?.notes ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (initial == null) tr("Add Crop / Feed Batch", "নতুন ফসল / গোখাদ্য যোগ")
                else tr("Edit Crop Batch", "ফসলের তথ্য সম্পাদনা")
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 470.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(tr("Piece of Land *", "জমি নির্বাচন করুন *"), style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    lands.forEach { land ->
                        FilterChip(
                            selected = selectedLandId == land.id,
                            onClick = { selectedLandId = land.id },
                            label = { Text(land.name) }
                        )
                    }
                }

                Text(tr("Crop / Agri Type", "ফসলের ধরন"), style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    AgriCropType.entries.forEach { t ->
                        FilterChip(
                            selected = cropType == t.key,
                            onClick = {
                                cropType = t.key
                                if (title.isBlank() && initial == null) {
                                    title = if (isBn) t.labelBn else t.labelEn
                                }
                            },
                            label = { Text(if (isBn) t.labelBn else t.labelEn) }
                        )
                    }
                }

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(tr("Crop Batch Name *", "ফসলের নাম / ব্যাচ *")) },
                    placeholder = { Text(tr("e.g. BRRI-28 Rice, Napier Fodder", "যেমন: বোরো ধান, নেপিয়ার ঘাস")) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("agri_crop_title_input")
                )
                OutlinedTextField(
                    value = variety,
                    onValueChange = { variety = it },
                    label = { Text(tr("Seed Variety (Optional)", "জাত (ঐচ্ছিক)")) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text(tr("Season", "মৌসুম"), style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    AgriSeason.entries.forEach { s ->
                        FilterChip(
                            selected = season == s.key,
                            onClick = { season = s.key },
                            label = { Text(if (isBn) s.labelBn else s.labelEn) }
                        )
                    }
                }

                Text(tr("Status", "অবস্থা"), style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    AgriCropStatus.entries.forEach { st ->
                        FilterChip(
                            selected = status == st.key,
                            onClick = { status = st.key },
                            label = { Text(if (isBn) st.labelBn else st.labelEn) }
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = yieldQtyText,
                        onValueChange = { yieldQtyText = it },
                        label = { Text(tr("Yield Qty", "ফলনের পরিমাণ")) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = marketPriceText,
                        onValueChange = { marketPriceText = it },
                        label = { Text("${tr("Rate", "দর")} (${currency.symbol}/$yieldUnit)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("mon", "kg", "ton", "bale", "bundle", "bag").forEach { u ->
                        FilterChip(
                            selected = yieldUnit == u,
                            onClick = { yieldUnit = u },
                            label = { Text(u) }
                        )
                    }
                }
                OutlinedTextField(
                    value = soldAmountText,
                    onValueChange = { soldAmountText = it },
                    label = { Text("${tr("Actual Sold Cash (Optional)", "বিক্রি আয় (ঐচ্ছিক)")} (${currency.symbol})") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text(tr("Notes", "নোট")) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                enabled = selectedLandId > 0L && title.isNotBlank(),
                onClick = {
                    val qty = yieldQtyText.toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0
                    val priceBase = CurrencyFormatter.toBaseBdt(marketPriceText.toDoubleOrNull() ?: 0.0, currency)
                    val worthBase = qty * priceBase
                    val soldBase = CurrencyFormatter.toBaseBdt(soldAmountText.toDoubleOrNull() ?: 0.0, currency)
                    val linkedFarmId = lands.firstOrNull { it.id == selectedLandId }?.farmId
                    onSave(
                        (initial ?: AgriCropEntity(ownerUserId = 0L, landId = selectedLandId, title = title.trim())).copy(
                            farmId = linkedFarmId,
                            landId = selectedLandId,
                            cropType = cropType,
                            title = title.trim(),
                            variety = variety.trim(),
                            season = season,
                            status = status,
                            yieldQuantity = qty,
                            yieldUnit = yieldUnit,
                            marketPricePerUnit = priceBase,
                            yieldMarketWorth = worthBase,
                            soldAmount = soldBase,
                            notes = notes.trim()
                        )
                    )
                },
                modifier = Modifier.testTag("save_agri_crop_button")
            ) { Text(strings.save) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(strings.cancel) } }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AgriHarvestDialog(
    crop: AgriCropEntity,
    currency: CurrencyOption,
    strings: AppStrings,
    onDismiss: () -> Unit,
    onSave: (qty: Double, unit: String, pricePerUnitBase: Double, worthBase: Double, soldBase: Double, status: String) -> Unit
) {
    val isBn = isBangla()
    var status by remember { mutableStateOf(if (crop.status == AgriCropStatus.GROWING.key) AgriCropStatus.HARVESTED.key else crop.status) }
    var qtyText by remember { mutableStateOf(crop.yieldQuantity.takeIf { it > 0.0 }?.toString() ?: "") }
    var yieldUnit by remember { mutableStateOf(crop.yieldUnit.ifBlank { "mon" }) }
    var rateText by remember {
        mutableStateOf(
            crop.marketPricePerUnit.takeIf { it > 0.0 }?.let {
                CurrencyFormatter.convertFromBase(it, currency).toInt().toString()
            } ?: ""
        )
    }
    var worthOverrideText by remember {
        mutableStateOf(
            crop.yieldMarketWorth.takeIf { it > 0.0 }?.let {
                CurrencyFormatter.convertFromBase(it, currency).toInt().toString()
            } ?: ""
        )
    }
    var soldText by remember {
        mutableStateOf(
            crop.soldAmount.takeIf { it > 0.0 }?.let {
                CurrencyFormatter.convertFromBase(it, currency).toInt().toString()
            } ?: ""
        )
    }

    val autoWorthDisplay = remember(qtyText, rateText) {
        val q = qtyText.toDoubleOrNull() ?: 0.0
        val r = rateText.toDoubleOrNull() ?: 0.0
        q * r
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("${tr("Harvest & Yield Worth", "ফলন ও বাজার মূল্য")}: ${crop.title}") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 440.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(tr("Crop Status", "ফসলের অবস্থা"), style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    AgriCropStatus.entries.forEach { st ->
                        FilterChip(
                            selected = status == st.key,
                            onClick = { status = st.key },
                            label = { Text(if (isBn) st.labelBn else st.labelEn) }
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = qtyText,
                        onValueChange = { qtyText = it },
                        label = { Text(tr("Harvested Qty", "ফলনের পরিমাণ")) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = rateText,
                        onValueChange = { rateText = it },
                        label = { Text("${tr("Price", "দর")} (${currency.symbol}/$yieldUnit)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("mon", "kg", "ton", "bale", "bundle", "bag").forEach { u ->
                        FilterChip(
                            selected = yieldUnit == u,
                            onClick = { yieldUnit = u },
                            label = { Text(u) }
                        )
                    }
                }
                if (autoWorthDisplay > 0.0) {
                    Text(
                        "${tr("Calculated Yield Worth", "হিসাবকৃত ফলনের মূল্য")}: ${currency.symbol}${String.format("%,.0f", autoWorthDisplay)}",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                OutlinedTextField(
                    value = worthOverrideText,
                    onValueChange = { worthOverrideText = it },
                    label = { Text("${tr("Total Yield Worth (Optional Override)", "মোট ফলনের মূল্য (না লিখলে অটো হিসাব)")} (${currency.symbol})") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = soldText,
                    onValueChange = { soldText = it },
                    label = { Text("${tr("Cash Sold Amount (Optional)", "নগদ বিক্রি আয় (ঐচ্ছিক)")} (${currency.symbol})") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val qty = qtyText.toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0
                    val priceBase = CurrencyFormatter.toBaseBdt(rateText.toDoubleOrNull() ?: 0.0, currency)
                    val overrideWorthBase = CurrencyFormatter.toBaseBdt(worthOverrideText.toDoubleOrNull() ?: 0.0, currency)
                    val finalWorth = if (overrideWorthBase > 0.0) overrideWorthBase else qty * priceBase
                    val soldBase = CurrencyFormatter.toBaseBdt(soldText.toDoubleOrNull() ?: 0.0, currency)
                    onSave(qty, yieldUnit, priceBase, finalWorth, soldBase, status)
                },
                modifier = Modifier.testTag("save_agri_harvest_button")
            ) { Text(strings.save) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(strings.cancel) } }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AgriExpenseDialog(
    lands: List<AgriLandEntity>,
    crops: List<AgriCropEntity>,
    initialLandId: Long?,
    initialCropId: Long?,
    currency: CurrencyOption,
    strings: AppStrings,
    onDismiss: () -> Unit,
    onSave: (landId: Long, cropId: Long?, farmId: Long?, sector: String, customSector: String, amountBase: Double, desc: String) -> Unit
) {
    val isBn = isBangla()
    var selectedLandId by remember { mutableStateOf(initialLandId ?: lands.firstOrNull()?.id ?: 0L) }
    var selectedCropId by remember { mutableStateOf<Long?>(initialCropId) }
    var sector by remember { mutableStateOf(AgriSector.SEEDS.key) }
    var customSectorName by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }

    val landCrops = remember(crops, selectedLandId) {
        crops.filter { it.landId == selectedLandId }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(tr("Record Agriculture Expense", "কৃষি খাতভিত্তিক খরচ যোগ")) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 470.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(tr("Piece of Land *", "কোন জমির খরচ *"), style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    lands.forEach { land ->
                        FilterChip(
                            selected = selectedLandId == land.id,
                            onClick = {
                                selectedLandId = land.id
                                if (selectedCropId != null && crops.none { it.id == selectedCropId && it.landId == land.id }) {
                                    selectedCropId = null
                                }
                            },
                            label = { Text(land.name) }
                        )
                    }
                }

                if (landCrops.isNotEmpty()) {
                    Text(tr("Crop Batch (Optional)", "ফসল (ঐচ্ছিক)"), style = MaterialTheme.typography.labelLarge)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        FilterChip(
                            selected = selectedCropId == null,
                            onClick = { selectedCropId = null },
                            label = { Text(tr("General Land Cost", "সাধারণ জমির খরচ")) }
                        )
                        landCrops.forEach { crop ->
                            FilterChip(
                                selected = selectedCropId == crop.id,
                                onClick = { selectedCropId = crop.id },
                                label = { Text(crop.title) }
                            )
                        }
                    }
                }

                Text(tr("Spending Sector *", "খরচের খাত *"), style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    AgriSector.entries.forEach { sec ->
                        FilterChip(
                            selected = sector == sec.key,
                            onClick = { sector = sec.key },
                            label = { Text(if (isBn) sec.labelBn else sec.labelEn) }
                        )
                    }
                }

                if (sector == AgriSector.OTHER.key) {
                    OutlinedTextField(
                        value = customSectorName,
                        onValueChange = { customSectorName = it },
                        label = { Text(tr("Custom Sector Name", "খাতের নাম")) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("${tr("Amount Spent *", "খরচের পরিমাণ *")} (${currency.symbol})") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("agri_expense_amount_input")
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text(tr("Details / Purpose", "বিবরণ (যেমন: ২ বস্তা ইউরিয়া, ৩ জন শ্রমিক)")) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            val parsedAmt = amountText.toDoubleOrNull() ?: 0.0
            Button(
                enabled = selectedLandId > 0L && parsedAmt > 0.0,
                onClick = {
                    val baseAmt = CurrencyFormatter.toBaseBdt(parsedAmt, currency)
                    val linkedFarmId = lands.firstOrNull { it.id == selectedLandId }?.farmId
                    onSave(selectedLandId, selectedCropId, linkedFarmId, sector, customSectorName.trim(), baseAmt, description.trim())
                },
                modifier = Modifier.testTag("save_agri_expense_button")
            ) { Text(strings.save) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(strings.cancel) } }
    )
}
