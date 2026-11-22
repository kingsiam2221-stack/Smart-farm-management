package com.example.ui.screens.customer

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.R
import com.example.data.local.*
import com.example.i18n.AppLanguage
import com.example.i18n.AppStrings
import com.example.i18n.CurrencyFormatter
import com.example.i18n.CurrencyOption
import com.example.i18n.DomainLocalization
import com.example.i18n.isBangla
import com.example.i18n.tr
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.LanguageCurrencyDialog
import com.example.ui.components.formatShortDate
import com.example.ui.state.CustomerPortalState

@Composable
fun CustomerPortalScreen(
    user: UserEntity,
    state: CustomerPortalState,
    notifications: List<AppNotificationEntity>,
    strings: AppStrings,
    currentLanguage: AppLanguage,
    currentCurrency: CurrencyOption,
    darkModeOverride: Boolean?,
    onAddToCart: (InventoryItemEntity) -> Unit,
    onUpdateCartQty: (Long, Double) -> Unit,
    onPlaceOrder: (String, String, () -> Unit) -> Unit,
    onRequestCancelOrder: (OrderEntity) -> Unit,
    onMarkNotificationsRead: () -> Unit,
    onUpdateProfile: (String, String, String?) -> Unit,
    onSelectLanguage: (AppLanguage) -> Unit,
    onSelectCurrency: (CurrencyOption) -> Unit,
    onSelectDarkMode: (Boolean?) -> Unit,
    onLogout: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val cartCount = state.cart.sumOf { it.quantity.toInt() }
    val unreadNotifs = notifications.count { !it.readFlag }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isWide = maxWidth >= 600.dp
        val navItems = listOf(
            Triple(strings.marketplace, Icons.Default.Storefront, 0),
            Triple(if (cartCount > 0) "${strings.cart} ($cartCount)" else strings.cart, Icons.Default.ShoppingCart, 0),
            Triple(strings.myOrders, Icons.Default.LocalShipping, 0),
            Triple(strings.profile, Icons.Default.Person, unreadNotifs)
        )

        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            bottomBar = {
                if (!isWide) {
                    NavigationBar {
                        navItems.forEachIndexed { idx, (label, icon, badge) ->
                            NavigationBarItem(
                                selected = selectedTab == idx,
                                onClick = { selectedTab = idx },
                                icon = {
                                    BadgedBox(badge = { if (badge > 0) Badge { Text("$badge") } }) {
                                        Icon(icon, contentDescription = label)
                                    }
                                },
                                label = { Text(label, maxLines = 1) }
                            )
                        }
                    }
                }
            }
        ) { padding ->
            Row(modifier = Modifier.fillMaxSize().padding(padding)) {
                if (isWide) {
                    NavigationRail {
                        navItems.forEachIndexed { idx, (label, icon, badge) ->
                            NavigationRailItem(
                                selected = selectedTab == idx,
                                onClick = { selectedTab = idx },
                                icon = {
                                    BadgedBox(badge = { if (badge > 0) Badge { Text("$badge") } }) {
                                        Icon(icon, contentDescription = label)
                                    }
                                },
                                label = { Text(label) }
                            )
                        }
                    }
                }

                Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    when (selectedTab) {
                        0 -> ShopCatalogTab(
                            state = state,
                            strings = strings,
                            currentLanguage = currentLanguage,
                            currency = currentCurrency,
                            onToggleLanguage = {
                                onSelectLanguage(if (currentLanguage == AppLanguage.BN) AppLanguage.EN else AppLanguage.BN)
                            },
                            onAddToCart = onAddToCart
                        )
                        1 -> CartCheckoutTab(state, strings, currentCurrency, onUpdateCartQty) { addr, method ->
                            onPlaceOrder(addr, method) { selectedTab = 2 }
                        }
                        2 -> CustomerOrdersTab(state, strings, currentCurrency, onRequestCancelOrder)
                        else -> CustomerProfileTab(
                            user, notifications, strings, currentLanguage, currentCurrency, darkModeOverride,
                            onMarkNotificationsRead, onUpdateProfile, onSelectLanguage, onSelectCurrency, onSelectDarkMode, onLogout
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ShopCatalogTab(
    state: CustomerPortalState,
    strings: AppStrings,
    currentLanguage: AppLanguage,
    currency: CurrencyOption,
    onToggleLanguage: () -> Unit,
    onAddToCart: (InventoryItemEntity) -> Unit
) {
    val isBn = isBangla()
    var search by remember { mutableStateOf("") }
    var selectedCat by remember { mutableStateOf<String?>(null) }
    val categories = remember(state.marketplaceItems) { state.marketplaceItems.map { it.category }.distinct() }
    val filtered = remember(state.marketplaceItems, search, selectedCat) {
        state.marketplaceItems.filter { item ->
            val q = search.isBlank() || item.name.contains(search, true) || item.description.contains(search, true)
            val c = selectedCat == null || item.category == selectedCat
            q && c
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Box(modifier = Modifier.fillMaxWidth().height(150.dp).clip(RoundedCornerShape(18.dp))) {
                Image(
                    painter = painterResource(id = R.drawable.img_marketplace_fresh),
                    contentDescription = tr("Farm Marketplace", "খামার বাজার"),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier.fillMaxSize().background(
                        Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.25f), Color(0xFF0E3E16).copy(alpha = 0.88f)))
                    )
                )
                AssistChip(
                    onClick = onToggleLanguage,
                    label = {
                        Text(
                            if (currentLanguage == AppLanguage.BN) "EN" else "বাংলা",
                            fontWeight = FontWeight.Bold
                        )
                    },
                    leadingIcon = { Icon(Icons.Default.Translate, contentDescription = strings.language, modifier = Modifier.size(16.dp)) },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = Color.White.copy(alpha = 0.92f),
                        labelColor = Color(0xFF0E3E16),
                        leadingIconContentColor = Color(0xFF0E3E16)
                    ),
                    modifier = Modifier.align(Alignment.TopEnd).padding(10.dp)
                )
                Column(modifier = Modifier.align(Alignment.BottomStart).padding(16.dp)) {
                    Text(
                        tr("Farm-to-Table Marketplace", "খামার থেকে সরাসরি বাজার"),
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        tr(
                            "Direct fresh eggs, dairy, meat & produce from local farms",
                            "স্থানীয় খামার থেকে তাজা ডিম, দুধ, মাংস ও খামার পণ্য"
                        ),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = search,
                onValueChange = { search = it },
                placeholder = { Text(strings.search) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item { FilterChip(selected = selectedCat == null, onClick = { selectedCat = null }, label = { Text(tr("All", "সব")) }) }
                items(categories) { cat ->
                    FilterChip(
                        selected = selectedCat == cat,
                        onClick = { selectedCat = if (selectedCat == cat) null else cat },
                        label = { Text(DomainLocalization.productCategory(cat, isBn)) }
                    )
                }
            }
        }

        if (filtered.isEmpty()) {
            item {
                EmptyStateCard(
                    icon = Icons.Default.Storefront,
                    title = tr("No Products Listed", "কোনো পণ্য তালিকাভুক্ত নেই"),
                    subtitle = tr(
                        "Farmers can list Eggs, Milk, Meat, and Produce from Inventory.",
                        "খামারিরা ইনভেন্টরি থেকে ডিম, দুধ, মাংস ও অন্যান্য পণ্য তালিকায় যোগ করতে পারেন।"
                    )
                )
            }
        } else {
            items(filtered, key = { it.id }) { item ->
                val farmName = state.farmsById[item.farmId]?.name ?: tr("Local Farm", "স্থানীয় খামার")
                Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surface, tonalElevation = 2.dp, modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(item.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text(
                                    "$farmName • ${DomainLocalization.productCategory(item.category, isBn)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Text(
                                CurrencyFormatter.format(item.sellingPrice, currency),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        if (item.description.isNotBlank()) {
                            Text(item.description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("${tr("Available", "মজুদ")}: ${item.quantity.toInt()} ${item.unit}", style = MaterialTheme.typography.labelSmall)
                            Button(onClick = { onAddToCart(item) }) {
                                Icon(Icons.Default.AddShoppingCart, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(tr("Add to Cart", "কার্টে যোগ করুন"))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CartCheckoutTab(
    state: CustomerPortalState,
    strings: AppStrings,
    currency: CurrencyOption,
    onUpdateQty: (Long, Double) -> Unit,
    onCheckout: (String, String) -> Unit
) {
    val isBn = isBangla()
    var address by remember { mutableStateOf("") }
    var paymentMethod by remember { mutableStateOf(currency.gateways.firstOrNull() ?: "Cash on Delivery") }
    val total = state.cart.sumOf { it.item.sellingPrice * it.quantity }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("${strings.cart} (${state.cart.size})", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        }
        if (state.cart.isEmpty()) {
            item {
                EmptyStateCard(
                    icon = Icons.Default.ShoppingCart,
                    title = tr("Your Cart Is Empty", "আপনার কার্ট খালি"),
                    subtitle = tr(
                        "Browse farm-fresh products in the Shop tab and add items to checkout.",
                        "বাজার ট্যাব থেকে তাজা খামার পণ্য কার্টে যোগ করে অর্ডার করুন।"
                    )
                )
            }
        } else {
            items(state.cart, key = { it.item.id }) { entry ->
                Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surface, tonalElevation = 2.dp, modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.padding(14.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(entry.item.name, fontWeight = FontWeight.Bold)
                            Text("${CurrencyFormatter.format(entry.item.sellingPrice, currency)} / ${entry.item.unit}", style = MaterialTheme.typography.labelSmall)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { onUpdateQty(entry.item.id, -1.0) }) {
                                Icon(Icons.Default.RemoveCircleOutline, contentDescription = tr("Decrease", "কমান"))
                            }
                            Text("${entry.quantity.toInt()}", fontWeight = FontWeight.Bold)
                            IconButton(onClick = { onUpdateQty(entry.item.id, 1.0) }) {
                                Icon(Icons.Default.AddCircleOutline, contentDescription = tr("Increase", "বাড়ান"))
                            }
                        }
                    }
                }
            }
            item {
                Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surface, tonalElevation = 2.dp, modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(strings.checkout, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        OutlinedTextField(
                            value = address,
                            onValueChange = { address = it },
                            label = { Text(tr("Delivery Address *", "ডেলিভারির ঠিকানা *")) },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text(tr("Payment Method", "পেমেন্ট পদ্ধতি"), style = MaterialTheme.typography.labelLarge)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(listOf("Cash on Delivery") + currency.gateways) { gw ->
                                FilterChip(
                                    selected = paymentMethod == gw,
                                    onClick = { paymentMethod = gw },
                                    label = { Text(DomainLocalization.paymentMethod(gw, isBn)) }
                                )
                            }
                        }
                        HorizontalDivider()
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(tr("Order Total", "মোট মূল্য"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(CurrencyFormatter.format(total, currency), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                        Button(
                            enabled = address.isNotBlank(),
                            onClick = { onCheckout(address, paymentMethod) },
                            modifier = Modifier.fillMaxWidth().height(50.dp)
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text(strings.placeOrder, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CustomerOrdersTab(
    state: CustomerPortalState,
    strings: AppStrings,
    currency: CurrencyOption,
    onRequestCancel: (OrderEntity) -> Unit
) {
    val isBn = isBangla()
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(strings.orderHistory, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        }
        if (state.orders.isEmpty()) {
            item {
                EmptyStateCard(
                    icon = Icons.Default.LocalShipping,
                    title = tr("No Orders Yet", "এখনও কোনো অর্ডার নেই"),
                    subtitle = tr(
                        "Your online farm orders and delivery tracking will appear here.",
                        "আপনার অনলাইন অর্ডার এবং ডেলিভারি অবস্থা এখানে দেখা যাবে।"
                    )
                )
            }
        } else {
            items(state.orders, key = { it.id }) { order ->
                val status = OrderStatus.entries.firstOrNull { it.name == order.status } ?: OrderStatus.PENDING
                val progress = when (status) {
                    OrderStatus.PENDING -> 0.15f
                    OrderStatus.CONFIRMED -> 0.35f
                    OrderStatus.PROCESSING -> 0.52f
                    OrderStatus.READY_FOR_DELIVERY -> 0.70f
                    OrderStatus.SHIPPED -> 0.85f
                    OrderStatus.DELIVERED -> 1f
                    OrderStatus.CANCELLED -> 0f
                }
                Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surface, tonalElevation = 2.dp, modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("${order.orderNumber} • ${order.farmName}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            AssistChip(onClick = {}, label = { Text(DomainLocalization.orderStatus(status, isBn), fontWeight = FontWeight.Bold) })
                        }
                        Text(order.itemsSummary, style = MaterialTheme.typography.bodyMedium)
                        Text("${tr("Address", "ঠিকানা")}: ${order.deliveryAddress}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            "${tr("Total", "মোট")}: ${CurrencyFormatter.format(order.totalAmount, currency)} (${DomainLocalization.paymentMethod(order.paymentMethod, isBn)}) • ${formatShortDate(order.createdAt)}",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                        if (status != OrderStatus.CANCELLED) {
                            LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
                        }
                        if (status == OrderStatus.PENDING || status == OrderStatus.CONFIRMED) {
                            if (order.cancellationRequested) {
                                Text(
                                    tr("Cancellation requested (awaiting farm review)", "বাতিলের অনুরোধ পাঠানো হয়েছে (অনুমোদনের অপেক্ষায়)"),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.error
                                )
                            } else {
                                OutlinedButton(onClick = { onRequestCancel(order) }) {
                                    Text(tr("Request Cancellation", "অর্ডার বাতিলের অনুরোধ"))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CustomerProfileTab(
    user: UserEntity,
    notifications: List<AppNotificationEntity>,
    strings: AppStrings,
    currentLanguage: AppLanguage,
    currentCurrency: CurrencyOption,
    darkModeOverride: Boolean?,
    onMarkRead: () -> Unit,
    onUpdateProfile: (String, String, String?) -> Unit,
    onSelectLanguage: (AppLanguage) -> Unit,
    onSelectCurrency: (CurrencyOption) -> Unit,
    onSelectDarkMode: (Boolean?) -> Unit,
    onLogout: () -> Unit
) {
    var name by remember(user.id) { mutableStateOf(user.name) }
    var mobile by remember(user.id) { mutableStateOf(user.mobile) }
    var imageUri by remember(user.id) { mutableStateOf(user.profileImageUri) }
    var showSettings by remember { mutableStateOf(false) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri: Uri? ->
        if (uri != null) imageUri = uri.toString()
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surface, tonalElevation = 2.dp, modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    if (!imageUri.isNullOrBlank()) {
                        AsyncImage(model = imageUri, contentDescription = user.name, contentScale = ContentScale.Crop, modifier = Modifier.size(68.dp).clip(CircleShape))
                    } else {
                        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.size(68.dp)) {
                            Box(contentAlignment = Alignment.Center) { Text(user.name.take(1).uppercase(), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold) }
                        }
                    }
                    OutlinedButton(onClick = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }) {
                        Text(tr("Change Photo", "ছবি পরিবর্তন করুন"))
                    }
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text(tr("Name", "নাম")) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = mobile, onValueChange = { mobile = it }, label = { Text(tr("Mobile", "মোবাইল নম্বর")) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    Button(onClick = { onUpdateProfile(name, mobile, imageUri) }, modifier = Modifier.fillMaxWidth()) { Text(strings.save) }
                    FilledTonalButton(onClick = { showSettings = true }, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.Language, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("${currentLanguage.nativeName} • ${currentCurrency.symbol} ${currentCurrency.code}")
                    }
                    Button(
                        onClick = onLogout,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier.fillMaxWidth()
                    ) { Text(strings.logout) }
                }
            }
        }
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(strings.notifications, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                TextButton(onClick = onMarkRead) { Text(tr("Mark Read", "পঠিত চিহ্নিত")) }
            }
        }
        items(notifications, key = { it.id }) { n ->
            Surface(shape = MaterialTheme.shapes.small, color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(n.title, fontWeight = FontWeight.Bold)
                    Text(n.message, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }

    if (showSettings) {
        LanguageCurrencyDialog(
            currentLanguage = currentLanguage,
            currentCurrency = currentCurrency,
            darkModeOverride = darkModeOverride,
            onSelectLanguage = onSelectLanguage,
            onSelectCurrency = onSelectCurrency,
            onSelectDarkMode = onSelectDarkMode,
            onDismiss = { showSettings = false }
        )
    }
}
