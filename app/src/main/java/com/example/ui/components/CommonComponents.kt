package com.example.ui.components

import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.local.FarmEntity
import com.example.data.local.SubscriptionFeature
import com.example.i18n.AppLanguage
import com.example.i18n.CurrencyOption
import com.example.i18n.DomainLocalization
import com.example.i18n.isBangla
import com.example.i18n.tr
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun KpiMetricCard(
    title: String,
    value: String,
    icon: ImageVector,
    tint: Color = MaterialTheme.colorScheme.primary,
    subtitle: String? = null,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(tint.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = title, tint = tint, modifier = Modifier.size(18.dp))
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (!subtitle.isNullOrBlank()) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = tint,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun EmptyStateCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, contentDescription = title, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(44.dp))
            Spacer(Modifier.height(10.dp))
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text(
                subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (actionLabel != null && onAction != null) {
                Spacer(Modifier.height(12.dp))
                Button(onClick = onAction, modifier = Modifier.minimumInteractiveComponentSize()) {
                    Text(actionLabel)
                }
            }
        }
    }
}

@Composable
fun FarmSwitcherBar(
    farms: List<FarmEntity>,
    selectedFarmId: Long?,
    allFarmsLabel: String,
    onSelectFarm: (Long?) -> Unit
) {
    var showSheet by remember { mutableStateOf(false) }
    val isBn = isBangla()
    val activeFarm = farms.find { it.id == selectedFarmId }
    val displayTitle = activeFarm?.let { "${it.name} (${it.farmCode})" } ?: "$allFarmsLabel (${farms.size})"

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("farm_switcher_button")
            .clickable { showSheet = true },
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.primaryContainer
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.Agriculture, contentDescription = tr("Active Farm", "সক্রিয় খামার"), tint = MaterialTheme.colorScheme.primary)
                Column {
                    Text(
                        displayTitle,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        activeFarm?.location ?: tr(
                            "Showing aggregated data across ${farms.size} farms",
                            "সকল ${farms.size}টি খামারের সমন্বিত তথ্য দেখানো হচ্ছে"
                        ),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                }
            }
            Icon(Icons.Default.SwapHoriz, contentDescription = tr("Switch Farm", "খামার পরিবর্তন"), tint = MaterialTheme.colorScheme.primary)
        }
    }

    if (showSheet) {
        var search by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showSheet = false },
            title = { Text(tr("Select Active Farm (${farms.size})", "সক্রিয় খামার নির্বাচন করুন (${farms.size})")) },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = search,
                        onValueChange = { search = it },
                        placeholder = { Text(tr("Search 1 to 100+ farms...", "১ থেকে ১০০+ খামার খুঁজুন...")) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(10.dp))
                    val filtered = farms.filter {
                        it.name.contains(search, ignoreCase = true) ||
                            it.farmCode.contains(search, ignoreCase = true) ||
                            it.location.contains(search, ignoreCase = true)
                    }
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 320.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        ListItem(
                            headlineContent = { Text(allFarmsLabel, fontWeight = FontWeight.Bold) },
                            supportingContent = {
                                Text(tr("Combined view of all ${farms.size} farms", "সকল ${farms.size}টি খামারের যৌথ দৃশ্য"))
                            },
                            leadingContent = { Icon(Icons.Default.Dashboard, contentDescription = null) },
                            trailingContent = { if (selectedFarmId == null) Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                            modifier = Modifier.clickable {
                                onSelectFarm(null)
                                showSheet = false
                            }
                        )
                        HorizontalDivider()
                        filtered.forEach { farm ->
                            val catLabel = DomainLocalization.farmCategory(farm.category, farm.customCategoryName, isBn)
                            ListItem(
                                headlineContent = { Text(farm.name, fontWeight = FontWeight.SemiBold) },
                                supportingContent = { Text("${farm.farmCode} • ${farm.location} • $catLabel") },
                                leadingContent = { Icon(Icons.Default.Eco, contentDescription = null) },
                                trailingContent = { if (selectedFarmId == farm.id) Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                                modifier = Modifier.clickable {
                                    onSelectFarm(farm.id)
                                    showSheet = false
                                }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSheet = false }) { Text(tr("Close", "বন্ধ করুন")) }
            }
        )
    }
}

@Composable
fun themeModeLabel(darkModeOverride: Boolean?): String {
    return when (darkModeOverride) {
        null -> tr("Default", "ডিফল্ট")
        false -> tr("White", "হোয়াইট")
        true -> tr("Dark", "ডার্ক")
    }
}

fun themeModeIcon(darkModeOverride: Boolean?): ImageVector {
    return when (darkModeOverride) {
        null -> Icons.Default.BrightnessAuto
        false -> Icons.Default.LightMode
        true -> Icons.Default.DarkMode
    }
}

fun nextThemeModeOverride(current: Boolean?): Boolean? = when (current) {
    null -> false // Default -> White
    false -> true // White -> Dark
    true -> null  // Dark -> Default
}

@Composable
fun ThemeModeSelectorRow(
    darkModeOverride: Boolean?,
    onSelectDarkMode: (Boolean?) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilterChip(
            selected = darkModeOverride == null,
            onClick = { onSelectDarkMode(null) },
            leadingIcon = {
                Icon(
                    Icons.Default.BrightnessAuto,
                    contentDescription = tr("Default Mode", "ডিফল্ট মোড"),
                    modifier = Modifier.size(16.dp)
                )
            },
            label = { Text(tr("Default", "ডিফল্ট"), maxLines = 1) },
            modifier = Modifier
                .weight(1f)
                .testTag("theme_mode_default")
        )
        FilterChip(
            selected = darkModeOverride == false,
            onClick = { onSelectDarkMode(false) },
            leadingIcon = {
                Icon(
                    Icons.Default.LightMode,
                    contentDescription = tr("White Mode", "হোয়াইট মোড"),
                    modifier = Modifier.size(16.dp)
                )
            },
            label = { Text(tr("White", "হোয়াইট"), maxLines = 1) },
            modifier = Modifier
                .weight(1f)
                .testTag("theme_mode_white")
        )
        FilterChip(
            selected = darkModeOverride == true,
            onClick = { onSelectDarkMode(true) },
            leadingIcon = {
                Icon(
                    Icons.Default.DarkMode,
                    contentDescription = tr("Dark Mode", "ডার্ক মোড"),
                    modifier = Modifier.size(16.dp)
                )
            },
            label = { Text(tr("Dark", "ডার্ক"), maxLines = 1) },
            modifier = Modifier
                .weight(1f)
                .testTag("theme_mode_dark")
        )
    }
}

@Composable
fun LanguageCurrencyDialog(
    currentLanguage: AppLanguage,
    currentCurrency: CurrencyOption,
    darkModeOverride: Boolean?,
    onSelectLanguage: (AppLanguage) -> Unit,
    onSelectCurrency: (CurrencyOption) -> Unit,
    onSelectDarkMode: (Boolean?) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(tr("Language, Currency & Theme", "ভাষা, মুদ্রা ও থিম")) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(tr("Language / ভাষা", "ভাষা / Language"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AppLanguage.entries.forEach { lang ->
                        FilterChip(
                            selected = currentLanguage == lang,
                            onClick = { onSelectLanguage(lang) },
                            label = { Text("${lang.nativeName} (${lang.displayName})") }
                        )
                    }
                }
                Text(tr("Theme Mode", "থিম মোড"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                ThemeModeSelectorRow(
                    darkModeOverride = darkModeOverride,
                    onSelectDarkMode = onSelectDarkMode
                )
                Text(tr("Country & Currency", "দেশ ও মুদ্রা"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                CurrencyOption.entries.forEach { curr ->
                    ListItem(
                        headlineContent = { Text("${curr.symbol} ${curr.code} — ${curr.countryName}", fontWeight = FontWeight.SemiBold) },
                        supportingContent = { Text("${tr("Gateways", "পেমেন্ট মাধ্যম")}: ${curr.gateways.joinToString(", ")}") },
                        trailingContent = {
                            RadioButton(selected = currentCurrency == curr, onClick = { onSelectCurrency(curr) })
                        },
                        modifier = Modifier.clickable { onSelectCurrency(curr) }
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) { Text(tr("Done", "সম্পন্ন")) }
        }
    )
}

fun formatShortDate(millis: Long): String {
    return SimpleDateFormat("dd MMM yyyy", Locale.US).format(Date(millis))
}

fun safeLaunchImagePicker(
    launcher: ManagedActivityResultLauncher<PickVisualMediaRequest, android.net.Uri?>
) {
    runCatching {
        launcher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }
}

@Composable
fun FeaturePlanBadge(
    isFree: Boolean,
    hasActiveSub: Boolean,
    modifier: Modifier = Modifier
) {
    val containerColor = when {
        isFree -> MaterialTheme.colorScheme.primaryContainer
        hasActiveSub -> MaterialTheme.colorScheme.secondaryContainer
        else -> MaterialTheme.colorScheme.tertiaryContainer
    }
    val contentColor = when {
        isFree -> MaterialTheme.colorScheme.onPrimaryContainer
        hasActiveSub -> MaterialTheme.colorScheme.onSecondaryContainer
        else -> MaterialTheme.colorScheme.onTertiaryContainer
    }
    val label = when {
        isFree -> tr("FREE", "ফ্রি")
        hasActiveSub -> tr("PRO ACTIVE", "সক্রিয়")
        else -> tr("SUBSCRIPTION", "সাবস্ক্রিপশন")
    }
    val icon = when {
        isFree -> Icons.Default.CheckCircle
        hasActiveSub -> Icons.Default.Verified
        else -> Icons.Default.Lock
    }
    Surface(
        color = containerColor,
        contentColor = contentColor,
        shape = RoundedCornerShape(50.dp),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(12.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
        }
    }
}

@Composable
fun SubscriptionRequiredLockCard(
    feature: SubscriptionFeature,
    onOpenSubscription: () -> Unit,
    canSubscribe: Boolean = true,
    modifier: Modifier = Modifier
) {
    val isBn = isBangla()
    val title = if (isBn) feature.titleBn else feature.titleEn
    val desc = if (isBn) feature.descBn else feature.descEn
    Surface(
        modifier = modifier.fillMaxWidth().testTag("subscription_lock_${feature.key.lowercase()}"),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.65f),
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.16f),
                modifier = Modifier.size(52.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.WorkspacePremium,
                        contentDescription = title,
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
            FeaturePlanBadge(isFree = false, hasActiveSub = false)
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = desc,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = tr(
                    "This module requires an active subscription plan. Payment options (bKash, Nagad, Bank Account) are available in Subscriptions.",
                    "এই অপশনটি চালাতে একটি সক্রিয় সাবস্ক্রিপশন প্ল্যান প্রয়োজন। বিকাশ, নগদ বা ব্যাংক অ্যাকাউন্টের মাধ্যমে সহজেই পেমেন্ট করুন।"
                ),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onTertiaryContainer
            )
            if (canSubscribe) {
                Button(
                    onClick = onOpenSubscription,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .testTag("subscribe_now_button_${feature.key.lowercase()}")
                ) {
                    Icon(Icons.Default.CardMembership, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        tr("Subscribe Now / View Plans", "সাবস্ক্রিপশন নিন / প্ল্যান দেখুন"),
                        fontWeight = FontWeight.Bold
                    )
                }
            } else {
                Text(
                    text = tr(
                        "Ask your Farm Owner to activate a subscription plan.",
                        "সাবস্ক্রিপশন চালু করতে খামারের মালিকের সাথে যোগাযোগ করুন।"
                    ),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}
