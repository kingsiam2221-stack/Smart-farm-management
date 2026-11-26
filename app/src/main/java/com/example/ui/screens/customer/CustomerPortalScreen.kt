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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.R
import com.example.ai.AdVideoMp4Exporter
import com.example.ai.BengaliAdStoryboard
import com.example.ai.ProductAdAiViewModel
import com.example.ai.SharePlatform
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
import com.example.ui.components.ProductAdVideoPlayer
import kotlinx.coroutines.launch
import com.example.ui.components.ThemeModeSelectorRow
import com.example.ui.components.formatShortDate
import com.example.ui.components.nextThemeModeOverride
import com.example.ui.components.themeModeIcon
import com.example.ui.components.themeModeLabel
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
    onSearchChange: (String) -> Unit = {},
    onSelectCategory: (String?) -> Unit = {},
    onLoadMore: () -> Unit = {},
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
                            darkModeOverride = darkModeOverride,
                            onSearchChange = onSearchChange,
                            onSelectCategory = onSelectCategory,
                            onLoadMore = onLoadMore,
                            onToggleLanguage = {
                                onSelectLanguage(if (currentLanguage == AppLanguage.BN) AppLanguage.EN else AppLanguage.BN)
                            },
                            onSelectDarkMode = onSelectDarkMode,
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
    darkModeOverride: Boolean?,
    onSearchChange: (String) -> Unit,
    onSelectCategory: (String?) -> Unit,
    onLoadMore: () -> Unit,
    onToggleLanguage: () -> Unit,
    onSelectDarkMode: (Boolean?) -> Unit,
    onAddToCart: (InventoryItemEntity) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val helperVm = remember { ProductAdAiViewModel() }
    val isBn = isBangla()
    val categories = state.availableCategories
    val filtered = state.marketplaceItems
    val hasMore = state.totalMatchingCount > filtered.size
    var watchingAd by remember { mutableStateOf<ProductAdEntity?>(null) }
    var showHowToOrderDemo by remember { mutableStateOf(false) }
    var exportingShare by remember { mutableStateOf(false) }

    fun shareAdVideo(storyboard: BengaliAdStoryboard, imageUri: String?, platform: SharePlatform) {
        if (exportingShare) return
        scope.launch {
            exportingShare = true
            try {
                val bmp = helperVm.decodeBitmap(context, imageUri)
                val mp4File = AdVideoMp4Exporter.exportCommercialMp4(
                    context = context.applicationContext,
                    storyboard = storyboard,
                    productBitmap = bmp
                )
                AdVideoMp4Exporter.saveMp4ToMoviesGallery(context.applicationContext, mp4File)
                AdVideoMp4Exporter.launchShareVideo(
                    context = context,
                    mp4File = mp4File,
                    storyboard = storyboard,
                    platform = platform
                )
            } finally {
                exportingShare = false
            }
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item(key = "shop_header") {
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
                Row(
                    modifier = Modifier.align(Alignment.TopEnd).padding(10.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AssistChip(
                        onClick = { onSelectDarkMode(nextThemeModeOverride(darkModeOverride)) },
                        label = { Text(themeModeLabel(darkModeOverride), fontWeight = FontWeight.SemiBold) },
                        leadingIcon = { Icon(themeModeIcon(darkModeOverride), contentDescription = strings.themeMode, modifier = Modifier.size(16.dp)) },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = Color.White.copy(alpha = 0.92f),
                            labelColor = Color(0xFF0E3E16),
                            leadingIconContentColor = Color(0xFF0E3E16)
                        ),
                        modifier = Modifier.testTag("customer_theme_toggle")
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
                        )
                    )
                }
                Column(modifier = Modifier.align(Alignment.BottomStart).padding(16.dp)) {
                    Text(
                        tr("Farm-to-Table Agri Marketplace", "খামার থেকে সরাসরি কৃষি বাজার"),
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        tr(
                            "Rice, wheat, mustard, fresh eggs, dairy & farm goods direct from farm businesses",
                            "সরাসরি খামার ও কৃষি ব্যবসা থেকে চাল, গম, সরিষা, তাজা ডিম, দুধ ও ফসল কিনুন"
                        ),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = state.searchQuery,
                onValueChange = onSearchChange,
                placeholder = { Text(tr("Search rice, wheat, mustard, eggs...", "চাল, গম, সরিষা, ডিম খুঁজুন...")) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (state.searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchChange("") }) {
                            Icon(Icons.Default.Close, contentDescription = tr("Clear", "মুছুন"))
                        }
                    }
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("marketplace_search_input")
            )
            Spacer(Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item(key = "cat_all") {
                    FilterChip(
                        selected = state.selectedCategory == null,
                        onClick = { onSelectCategory(null) },
                        label = { Text(tr("All", "সব")) }
                    )
                }
                items(categories, key = { "cat_$it" }) { cat ->
                    FilterChip(
                        selected = state.selectedCategory == cat,
                        onClick = { onSelectCategory(if (state.selectedCategory == cat) null else cat) },
                        label = { Text(DomainLocalization.productCategory(cat, isBn)) }
                    )
                }
            }
            if (state.totalMatchingCount > 0) {
                Spacer(Modifier.height(4.dp))
                Text(
                    tr(
                        "Showing ${filtered.size} of ${state.totalMatchingCount} products",
                        "${state.totalMatchingCount}টির মধ্যে ${filtered.size}টি পণ্য দেখানো হচ্ছে"
                    ),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(8.dp))
            FilledTonalButton(
                onClick = { showHowToOrderDemo = true },
                modifier = Modifier.fillMaxWidth().testTag("watch_how_to_order_video_button")
            ) {
                Icon(Icons.Default.OndemandVideo, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    tr("Watch How to Order Video (বাংলা ডেমো)", "কিভাবে অর্ডার করবেন? ভিডিও দেখুন (বাংলা)"),
                    fontWeight = FontWeight.Bold
                )
            }
        }

        if (state.activeShopAds.isNotEmpty()) {
            item(key = "shop_featured_ads") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        tr("Sponsored Farm Video Ads (বিজ্ঞাপন)", "খামার পণ্যের ভিডিও বিজ্ঞাপন"),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(state.activeShopAds, key = { "shop_ad_${it.id}" }) { ad ->
                            Surface(
                                onClick = { watchingAd = ad },
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.72f),
                                tonalElevation = 2.dp,
                                modifier = Modifier
                                    .width(240.dp)
                                    .testTag("shop_ad_card_${ad.id}")
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(Icons.Default.PlayArrow, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary)
                                            }
                                        }
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(ad.productName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, maxLines = 1)
                                            Text(ad.promoBadgeBn, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, maxLines = 1)
                                        }
                                    }
                                    Text(ad.hookHeadlineBn, style = MaterialTheme.typography.bodySmall, maxLines = 2)
                                    Text(
                                        "▶ ${tr("Watch Ad & Order", "বিজ্ঞাপন দেখুন ও অর্ডার করুন")}",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        if (filtered.isEmpty()) {
            item(key = "shop_empty") {
                EmptyStateCard(
                    icon = Icons.Default.Storefront,
                    title = tr("No Products Listed", "কোনো পণ্য তালিকাভুক্ত নেই"),
                    subtitle = tr(
                        "Farm business accounts can list Rice, Wheat, Mustard, Eggs, Milk, Meat & any agri produce.",
                        "খামার ও কৃষি ব্যবসা অ্যাকাউন্ট থেকে চাল, গম, সরিষা, ডিম, দুধ বা যেকোনো পণ্য বিক্রি করা যায়।"
                    )
                )
            }
        } else {
            items(filtered, key = { "mkt_${it.id}" }) { item ->
                val farm = state.farmsById[item.farmId]
                val farmName = farm?.name ?: tr("Local Farm", "স্থানীয় খামার")
                val farmLoc = farm?.location?.takeIf { it.isNotBlank() }
                val sellerLine = if (farmLoc != null) "$farmName ($farmLoc)" else farmName
                val isResale = item.sourcingType == ProductSourcingType.PURCHASED_RESALE.key

                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 2.dp,
                    modifier = Modifier.fillMaxWidth().testTag("marketplace_item_${item.id}")
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            if (!item.imageUri.isNullOrBlank()) {
                                AsyncImage(
                                    model = item.imageUri,
                                    contentDescription = item.name,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.size(64.dp).clip(RoundedCornerShape(12.dp))
                                )
                            } else {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    modifier = Modifier.size(52.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            if (isResale) Icons.Default.LocalMall else Icons.Default.Eco,
                                            contentDescription = item.category,
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        item.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        CurrencyFormatter.format(item.sellingPrice, currency),
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Text(
                                    "$sellerLine • ${DomainLocalization.productCategory(item.category, isBn)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    DomainLocalization.sourcingType(item.sourcingType, isBn),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }
                        }
                        if (item.description.isNotBlank()) {
                            Text(item.description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "${tr("Available", "মজুদ")}: ${item.quantity.toInt()} ${item.unit} • /${item.unit}",
                                style = MaterialTheme.typography.labelSmall
                            )
                            Button(
                                onClick = { onAddToCart(item) },
                                modifier = Modifier.testTag("add_to_cart_${item.id}")
                            ) {
                                Icon(Icons.Default.AddShoppingCart, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(tr("Add to Cart", "কার্টে যোগ করুন"))
                            }
                        }
                    }
                }
            }

            if (hasMore) {
                item(key = "shop_load_more") {
                    OutlinedButton(
                        onClick = onLoadMore,
                        modifier = Modifier.fillMaxWidth().height(48.dp).testTag("marketplace_load_more")
                    ) {
                        Icon(Icons.Default.ExpandMore, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text(
                            tr(
                                "Load More (${state.totalMatchingCount - filtered.size} remaining)",
                                "আরও পণ্য দেখুন (বাকি ${state.totalMatchingCount - filtered.size}টি)"
                            ),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }

    watchingAd?.let { ad ->
        val adStoryboard = remember(ad) { BengaliAdStoryboard.fromEntity(ad) }
        val linkedProduct = remember(ad.inventoryItemId, state.marketplaceItems) {
            ad.inventoryItemId?.let { targetId -> state.marketplaceItems.firstOrNull { it.id == targetId } }
        }
        AlertDialog(
            onDismissRequest = { watchingAd = null },
            title = {
                Text(
                    "${ad.productName} • ${ad.farmName}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ProductAdVideoPlayer(
                        storyboard = adStoryboard,
                        imageUri = ad.imageUri,
                        autoPlay = true,
                        onOrderNowClick = {
                            if (linkedProduct != null) {
                                onAddToCart(linkedProduct)
                                watchingAd = null
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            enabled = !exportingShare,
                            onClick = { shareAdVideo(adStoryboard, ad.imageUri, SharePlatform.WHATSAPP) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(tr("WhatsApp", "হোয়াটসঅ্যাপ"), maxLines = 1)
                        }
                        OutlinedButton(
                            enabled = !exportingShare,
                            onClick = { shareAdVideo(adStoryboard, ad.imageUri, SharePlatform.FACEBOOK) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(tr("Facebook", "ফেসবুক"), maxLines = 1)
                        }
                        OutlinedButton(
                            enabled = !exportingShare,
                            onClick = { shareAdVideo(adStoryboard, ad.imageUri, SharePlatform.ANY_APP) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(if (exportingShare) "..." else tr("Share All", "শেয়ার"), maxLines = 1)
                        }
                    }
                }
            },
            confirmButton = {
                if (linkedProduct != null) {
                    Button(
                        onClick = {
                            onAddToCart(linkedProduct)
                            watchingAd = null
                        },
                        modifier = Modifier.testTag("ad_dialog_order_now_button")
                    ) {
                        Icon(Icons.Default.AddShoppingCart, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(tr("Order Now • Add to Cart", "এখনই অর্ডার করুন • কার্টে দিন"))
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { watchingAd = null }) { Text(tr("Close", "বন্ধ করুন")) }
            }
        )
    }

    if (showHowToOrderDemo) {
        val sample = state.marketplaceItems.firstOrNull()
        val demoStoryboard = BengaliAdStoryboard(
            productName = sample?.name ?: "খাঁটি খামার পণ্য (চাল / গম / ডিম)",
            farmName = state.farmsById[sample?.farmId]?.name ?: "Smart Farm বাজার",
            priceText = sample?.let { CurrencyFormatter.format(it.sellingPrice, currency) } ?: "৳85 / kg",
            hookHeadlineBn = "সরাসরি খামার থেকে মোবাইল অ্যাপে অর্ডার করার সহজ নিয়ম!",
            promoBadgeBn = "অ্যাপ স্ক্রিন রেকর্ড টিউটোরিয়াল",
            benefitsBn = BengaliAdStoryboard.defaultBenefits(sample?.name ?: "খামার পণ্য"),
            callToActionBn = "এখনই অর্ডার করুন • এখানে ক্লিক করুন",
            voiceoverScriptBn = "Smart Farm অ্যাপ থেকে অর্ডার করা একদম সহজ! Play Store থেকে Smart Farm অ্যাপ ডাউনলোড করে কাস্টমার অ্যাকাউন্ট খুলুন। এরপর বাজার থেকে পছন্দের পণ্য বেছে কার্টে যোগ করুন বাটনে এখানে ক্লিক করুন, ঠিকানা ও বিকাশ বা নগদ পেমেন্ট দিয়ে অনলাইনে অর্ডার করুন!",
            orderStepsBn = BengaliAdStoryboard.defaultOrderSteps(),
            accentColorHex = "#1B5E20",
            downloadAndOrderOutroBn = BengaliAdStoryboard.DEFAULT_DOWNLOAD_AND_ORDER_OUTRO_BN
        )
        AlertDialog(
            onDismissRequest = { showHowToOrderDemo = false },
            title = {
                Text(
                    tr("How to Order from App (বাংলা ভিডিও গাইড)", "অ্যাপ থেকে অর্ডার করার ভিডিও গাইড"),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ProductAdVideoPlayer(
                        storyboard = demoStoryboard,
                        imageUri = sample?.imageUri,
                        autoPlay = true,
                        onOrderNowClick = {
                            if (sample != null) onAddToCart(sample)
                            showHowToOrderDemo = false
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            enabled = !exportingShare,
                            onClick = { shareAdVideo(demoStoryboard, sample?.imageUri, SharePlatform.WHATSAPP) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(tr("WhatsApp", "হোয়াটসঅ্যাপ"), maxLines = 1)
                        }
                        OutlinedButton(
                            enabled = !exportingShare,
                            onClick = { shareAdVideo(demoStoryboard, sample?.imageUri, SharePlatform.FACEBOOK) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(tr("Facebook", "ফেসবুক"), maxLines = 1)
                        }
                        OutlinedButton(
                            enabled = !exportingShare,
                            onClick = { shareAdVideo(demoStoryboard, sample?.imageUri, SharePlatform.ANY_APP) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(if (exportingShare) "..." else tr("Share All", "শেয়ার"), maxLines = 1)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showHowToOrderDemo = false }) { Text(tr("Close", "বন্ধ করুন")) }
            }
        )
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
                    OutlinedButton(onClick = { com.example.ui.components.safeLaunchImagePicker(picker) }) {
                        Text(tr("Change Photo", "ছবি পরিবর্তন করুন"))
                    }
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text(tr("Name", "নাম")) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = mobile, onValueChange = { mobile = it }, label = { Text(tr("Mobile", "মোবাইল নম্বর")) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    Button(onClick = { onUpdateProfile(name, mobile, imageUri) }, modifier = Modifier.fillMaxWidth()) { Text(strings.save) }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    Text(
                        tr("Theme Mode", "থিম মোড"),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.align(Alignment.Start)
                    )
                    ThemeModeSelectorRow(
                        darkModeOverride = darkModeOverride,
                        onSelectDarkMode = onSelectDarkMode
                    )
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
