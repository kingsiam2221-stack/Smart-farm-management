package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import com.example.i18n.AppLanguage
import com.example.i18n.CurrencyOption
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
                Icon(Icons.Default.Agriculture, contentDescription = "Active Farm", tint = MaterialTheme.colorScheme.primary)
                Column {
                    Text(
                        displayTitle,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        activeFarm?.location ?: "Showing aggregated data across ${farms.size} farms",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                }
            }
            Icon(Icons.Default.SwapHoriz, contentDescription = "Switch Farm", tint = MaterialTheme.colorScheme.primary)
        }
    }

    if (showSheet) {
        var search by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showSheet = false },
            title = { Text("Select Active Farm (${farms.size})") },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = search,
                        onValueChange = { search = it },
                        placeholder = { Text("Search 1 to 100+ farms...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(10.dp))
                    LazyColumn(modifier = Modifier.heightIn(max = 320.dp)) {
                        item {
                            ListItem(
                                headlineContent = { Text(allFarmsLabel, fontWeight = FontWeight.Bold) },
                                supportingContent = { Text("Combined view of all ${farms.size} farms") },
                                leadingContent = { Icon(Icons.Default.Dashboard, contentDescription = null) },
                                trailingContent = { if (selectedFarmId == null) Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                                modifier = Modifier.clickable {
                                    onSelectFarm(null)
                                    showSheet = false
                                }
                            )
                            HorizontalDivider()
                        }
                        val filtered = farms.filter {
                            it.name.contains(search, ignoreCase = true) ||
                                it.farmCode.contains(search, ignoreCase = true) ||
                                it.location.contains(search, ignoreCase = true)
                        }
                        items(filtered, key = { it.id }) { farm ->
                            ListItem(
                                headlineContent = { Text(farm.name, fontWeight = FontWeight.SemiBold) },
                                supportingContent = { Text("${farm.farmCode} • ${farm.location} • ${farm.category}") },
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
                TextButton(onClick = { showSheet = false }) { Text("Close") }
            }
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
        title = { Text("Language, Currency & Theme") },
        text = {
            LazyColumn(
                modifier = Modifier.heightIn(max = 420.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    Text("Language / ভাষা", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AppLanguage.entries.forEach { lang ->
                            FilterChip(
                                selected = currentLanguage == lang,
                                onClick = { onSelectLanguage(lang) },
                                label = { Text("${lang.nativeName} (${lang.displayName})") }
                            )
                        }
                    }
                }
                item {
                    Text("Theme Appearance", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(selected = darkModeOverride == false, onClick = { onSelectDarkMode(false) }, label = { Text("Light") })
                        FilterChip(selected = darkModeOverride == true, onClick = { onSelectDarkMode(true) }, label = { Text("Dark") })
                        FilterChip(selected = darkModeOverride == null, onClick = { onSelectDarkMode(null) }, label = { Text("Auto") })
                    }
                }
                item {
                    Text("Country & Currency", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
                items(CurrencyOption.entries) { curr ->
                    ListItem(
                        headlineContent = { Text("${curr.symbol} ${curr.code} — ${curr.countryName}", fontWeight = FontWeight.SemiBold) },
                        supportingContent = { Text("Gateways: ${curr.gateways.joinToString(", ")}") },
                        trailingContent = {
                            RadioButton(selected = currentCurrency == curr, onClick = { onSelectCurrency(curr) })
                        },
                        modifier = Modifier.clickable { onSelectCurrency(curr) }
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) { Text("Done") }
        }
    )
}

fun formatShortDate(millis: Long): String {
    return SimpleDateFormat("dd MMM yyyy", Locale.US).format(Date(millis))
}
