package com.example.ui.screens.farmer

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.automirrored.filled.Chat
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.ai.AdVideoMp4Exporter
import com.example.ai.BengaliAdStoryboard
import com.example.ai.ExportVideoUiState
import com.example.ai.ProductAdAiUiState
import com.example.ai.ProductAdAiViewModel
import com.example.ai.SharePlatform
import com.example.data.local.*
import com.example.i18n.AppStrings
import com.example.i18n.CurrencyFormatter
import com.example.i18n.CurrencyOption
import com.example.ui.components.ProvideSafeTextToolbar
import com.example.i18n.tr
import com.example.ui.components.*
import com.example.ui.state.FarmerDashboardState
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AiProductAdStudioScreen(
    state: FarmerDashboardState,
    strings: AppStrings,
    currency: CurrencyOption,
    preselectedProductId: Long? = null,
    onPreselectConsumed: () -> Unit = {},
    onSelectFarm: (Long?) -> Unit,
    onSaveProductAd: (ProductAdEntity) -> Unit,
    onToggleAdActive: (ProductAdEntity, Boolean) -> Unit,
    onDeleteAd: (ProductAdEntity) -> Unit,
    onOpenSubscription: () -> Unit,
    adAiViewModel: ProductAdAiViewModel = viewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val policy = state.accessPolicy
    val subPolicy = state.subscriptionPolicy

    if (!policy.has(AppPermission.AI_AD_STUDIO)) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            EmptyStateCard(
                icon = Icons.Default.Lock,
                title = tr("AI Video Ad Studio Restricted", "AI বিজ্ঞাপন স্টুডিওর অনুমতি নেই"),
                subtitle = tr(
                    "Ask your Farm Admin to grant AI Product Video Ad Studio access.",
                    "AI দিয়ে পণ্যের বিজ্ঞাপন ভিডিও তৈরি করতে খামার অ্যাডমিনের অনুমতি নিন।"
                )
            )
        }
        return
    }

    if (!subPolicy.canAccess(SubscriptionFeature.AI_AD_VIDEO_STUDIO)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(strings.aiAdStudio, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                FeaturePlanBadge(isFree = false, hasActiveSub = false)
            }
            SubscriptionRequiredLockCard(
                feature = SubscriptionFeature.AI_AD_VIDEO_STUDIO,
                onOpenSubscription = onOpenSubscription,
                canSubscribe = policy.has(AppPermission.SUBSCRIPTION_MANAGE)
            )
        }
        return
    }

    val adState by adAiViewModel.adState.collectAsStateWithLifecycle()
    val exportState by adAiViewModel.exportState.collectAsStateWithLifecycle()

    val farmsById = remember(state.farms) { state.farms.associateBy { it.id } }
    val activeFarmId = state.selectedFarmId
    val availableProducts = remember(state.inventory, activeFarmId) {
        if (activeFarmId == null) state.inventory else state.inventory.filter { it.farmId == activeFarmId }
    }

    val defaultFarmName = tr("Smart Farm", "স্মার্ট খামার")
    var selectedInventoryId by remember { mutableStateOf<Long?>(null) }
    var productName by remember { mutableStateOf("") }
    var productCategory by remember { mutableStateOf("Rice") }
    var farmName by remember {
        mutableStateOf(
            (activeFarmId?.let { farmsById[it] } ?: state.farms.firstOrNull())?.name
                ?: defaultFarmName
        )
    }
    var priceText by remember { mutableStateOf("") }
    var promoNotes by remember { mutableStateOf("") }
    var selectedImageUri by remember { mutableStateOf<String?>(null) }
    var publishToMarketplace by remember { mutableStateOf(true) }

    var viewingSavedAd by remember { mutableStateOf<ProductAdEntity?>(null) }
    var deleteCandidate by remember { mutableStateOf<ProductAdEntity?>(null) }
    var showEditScriptDialog by remember { mutableStateOf(false) }

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri: Uri? ->
        if (uri != null) selectedImageUri = uri.toString()
    }

    fun applyInventoryProduct(item: InventoryItemEntity) {
        selectedInventoryId = item.id
        productName = item.name
        productCategory = item.category
        farmName = farmsById[item.farmId]?.name ?: defaultFarmName
        priceText = if (item.sellingPrice > 0.0) {
            "${CurrencyFormatter.format(item.sellingPrice, currency)} / ${item.unit}"
        } else ""
        promoNotes = item.description
        selectedImageUri = item.imageUri
    }

    LaunchedEffect(preselectedProductId, state.inventory) {
        val pid = preselectedProductId
        if (pid != null) {
            val match = state.inventory.firstOrNull { it.id == pid }
            if (match != null) {
                applyInventoryProduct(match)
            }
            onPreselectConsumed()
        }
    }

    LaunchedEffect(exportState, adState) {
        val ready = exportState as? ExportVideoUiState.Ready ?: return@LaunchedEffect
        val platform = ready.autoSharePlatform ?: return@LaunchedEffect
        adAiViewModel.consumeAutoShare()
        val storyboard = (adState as? ProductAdAiUiState.Success)?.storyboard
        val mp4File = File(ready.filePath)
        if (mp4File.exists()) {
            AdVideoMp4Exporter.launchShareVideo(
                context = context,
                mp4File = mp4File,
                storyboard = storyboard,
                platform = platform
            )
        }
    }

    fun triggerPlatformShare(
        platform: SharePlatform,
        targetStoryboard: BengaliAdStoryboard,
        imageUri: String?
    ) {
        val ready = exportState as? ExportVideoUiState.Ready
        val currentStoryboard = (adState as? ProductAdAiUiState.Success)?.storyboard
        if (ready != null && currentStoryboard == targetStoryboard && File(ready.filePath).exists()) {
            AdVideoMp4Exporter.launchShareVideo(
                context = context,
                mp4File = File(ready.filePath),
                storyboard = targetStoryboard,
                platform = platform
            )
        } else {
            adAiViewModel.loadExistingStoryboard(targetStoryboard)
            scope.launch {
                val bmp = adAiViewModel.decodeBitmap(context, imageUri)
                adAiViewModel.exportMp4Video(
                    context = context,
                    storyboard = targetStoryboard,
                    productBitmap = bmp,
                    saveToGallery = true,
                    autoSharePlatform = platform
                )
            }
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item(key = "ad_studio_header") {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        Brush.horizontalGradient(
                            listOf(Color(0xFF1B5E20), Color(0xFF00695C))
                        )
                    )
                    .padding(16.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.MovieCreation, contentDescription = null, tint = Color(0xFFFFD54F))
                            Text(
                                tr("Google AI Bengali Video Ad Studio", "Google AI বাংলা পণ্য বিজ্ঞাপন ভিডিও স্টুডিও"),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                        FeaturePlanBadge(
                            isFree = subPolicy.isFree(SubscriptionFeature.AI_AD_VIDEO_STUDIO),
                            hasActiveSub = subPolicy.hasActiveSubscription
                        )
                    }
                    Text(
                        tr(
                            "Select any product or photo — Google AI creates a Bengali commercial ad + app screen-record 'Order Now / Click Here' demo anyone can publish or export as MP4.",
                            "যেকোনো পণ্যের ছবি বা নাম দিন — Google AI তৈরি করবে বাংলা বিজ্ঞাপন ভিডিও ও 'এখনই অর্ডার করুন / এখানে ক্লিক করুন' অ্যাপ অর্ডার স্ক্রিন রেকর্ড।"
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                }
            }
        }

        if (state.farms.isNotEmpty()) {
            item(key = "ad_studio_farms") {
                FarmSwitcherBar(
                    farms = state.farms,
                    selectedFarmId = state.selectedFarmId,
                    allFarmsLabel = strings.allFarms,
                    onSelectFarm = onSelectFarm
                )
            }
        }

        // Step 1: Pick Existing Product or Custom Photo
        item(key = "ad_creator_card") {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        tr("1. Pick Product & Photo", "১. পণ্য ও ছবি নির্বাচন করুন"),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    if (availableProducts.isNotEmpty()) {
                        Text(
                            tr("Quick-fill from your Farm Products:", "আপনার খামারের পণ্য থেকে দ্রুত বেছে নিন:"),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(availableProducts, key = { "pick_${it.id}" }) { item ->
                                FilterChip(
                                    selected = selectedInventoryId == item.id,
                                    onClick = { applyInventoryProduct(item) },
                                    label = { Text(item.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                    leadingIcon = { Icon(Icons.Default.Inventory2, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(76.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer)
                                .clickable { safeLaunchImagePicker(imagePicker) },
                            contentAlignment = Alignment.Center
                        ) {
                            if (!selectedImageUri.isNullOrBlank()) {
                                AsyncImage(
                                    model = selectedImageUri,
                                    contentDescription = productName,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Icon(
                                    Icons.Default.AddPhotoAlternate,
                                    contentDescription = tr("Pick Product Photo", "পণ্যের ছবি বাছুন"),
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            OutlinedButton(
                                onClick = { safeLaunchImagePicker(imagePicker) },
                                modifier = Modifier.fillMaxWidth().testTag("ad_studio_pick_photo_button")
                            ) {
                                Icon(Icons.Default.PhotoCamera, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    if (selectedImageUri.isNullOrBlank()) tr("Choose Product Photo", "পণ্যের ছবি আপলোড করুন")
                                    else tr("Change Photo", "ছবি পরিবর্তন করুন")
                                )
                            }
                            if (!selectedImageUri.isNullOrBlank()) {
                                TextButton(onClick = { selectedImageUri = null }) {
                                    Text(tr("Remove Photo", "ছবি সরান"))
                                }
                            }
                        }
                    }

                    OutlinedTextField(
                        value = productName,
                        onValueChange = { productName = it },
                        label = { Text(tr("Product Name *", "পণ্যের নাম *")) },
                        placeholder = { Text(tr("e.g., Kataribhog Rice 5kg / Mustard Oil 1L", "যেমন: কাটারিভোগ চাল ৫ কেজি / সরিষার তেল ১ লিটার")) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("ad_studio_product_name_input")
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = priceText,
                            onValueChange = { priceText = it },
                            label = { Text(tr("Ad Price Label", "বিজ্ঞাপনে মূল্য")) },
                            placeholder = { Text(tr("e.g., ৳450 / 5kg", "যেমন: ৪৫০৳ / ৫ কেজি")) },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = farmName,
                            onValueChange = { farmName = it },
                            label = { Text(tr("Brand / Farm Name", "খামার / ব্র্যান্ডের নাম")) },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    OutlinedTextField(
                        value = promoNotes,
                        onValueChange = { promoNotes = it },
                        label = { Text(tr("Offer / Quality Notes for AI", "AI বিজ্ঞাপনের জন্য বিশেষ তথ্য / অফার")) },
                        placeholder = { Text(tr("e.g., Organic harvest, 10% discount, cash on delivery", "যেমন: সম্পূর্ণ ভেজালমুক্ত, ১০% ছাড়, হোম ডেলিভারি")) },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Button(
                        onClick = {
                            scope.launch {
                                val bmp = adAiViewModel.decodeBitmap(context, selectedImageUri)
                                adAiViewModel.generateBengaliAdStoryboard(
                                    productName = productName,
                                    category = productCategory,
                                    farmName = farmName,
                                    priceText = priceText,
                                    notes = promoNotes,
                                    productBitmap = bmp
                                )
                            }
                        },
                        enabled = productName.isNotBlank() && adState !is ProductAdAiUiState.Loading,
                        modifier = Modifier.fillMaxWidth().height(50.dp).testTag("generate_bengali_ad_button")
                    ) {
                        if (adState is ProductAdAiUiState.Loading) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                            Spacer(Modifier.width(8.dp))
                            Text(tr("Google AI creating Bengali Ad Video...", "Google AI বাংলা বিজ্ঞাপন তৈরি করছে..."))
                        } else {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text(tr("Create Bengali Video Ad with Google AI", "Google AI দিয়ে বাংলা বিজ্ঞাপন ভিডিও বানান"), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Step 2: Generated Video Preview + Bengali Voiceover + Order Demo + Publish/Export
        when (val st = adState) {
            is ProductAdAiUiState.Error -> {
                item(key = "ad_ai_error") {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.errorContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                            Text(st.message, color = MaterialTheme.colorScheme.onErrorContainer, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
            is ProductAdAiUiState.Success -> {
                val storyboard = st.storyboard
                item(key = "ad_video_preview") {
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 3.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    tr("2. Live Bengali Ad & Order Demo", "২. বাংলা বিজ্ঞাপন ও অর্ডার স্ক্রিন রেকর্ড"),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.weight(1f)
                                )
                                TextButton(onClick = { showEditScriptDialog = true }) {
                                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text(tr("Edit Bangla", "বাংলা এডিট"))
                                }
                            }

                            ProductAdVideoPlayer(
                                storyboard = storyboard,
                                imageUri = selectedImageUri,
                                autoPlay = true
                            )

                            // Bengali Voiceover Script Card
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(
                                        tr("Bengali Voiceover Script (বাংলা ভয়েস স্ক্রিপ্ট)", "বাংলা ভয়েসওভার স্ক্রিপ্ট"),
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(storyboard.voiceoverScriptBn, style = MaterialTheme.typography.bodySmall)
                                }
                            }

                            // Screen-Record Ordering Steps Summary
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.45f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(
                                        tr("App Order Screen-Record Steps (কিভাবে অর্ডার করবেন)", "অ্যাপ থেকে অর্ডার করার ধাপসমূহ"),
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    storyboard.orderStepsBn.forEach { step ->
                                        Text("• $step", style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                            }

                            // Video Ending Outro Card: App Download & Online Order Guide
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(
                                        tr("Video Ending Outro: App Download & Online Order", "ভিডিও শেষে: অ্যাপ ডাউনলোড ও অনলাইনে অর্ডার নিয়ম"),
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(storyboard.downloadAndOrderOutroBn, style = MaterialTheme.typography.bodySmall)
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    tr("Show Ad in Customer Marketplace", "ক্রেতাদের বাজারে বিজ্ঞাপন দেখান"),
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.weight(1f)
                                )
                                Switch(
                                    checked = publishToMarketplace,
                                    onCheckedChange = { publishToMarketplace = it }
                                )
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = {
                                        val farmId = (activeFarmId ?: availableProducts.firstOrNull { it.id == selectedInventoryId }?.farmId
                                            ?: state.farms.firstOrNull()?.id ?: 0L)
                                        onSaveProductAd(
                                            storyboard.toEntity(
                                                ownerUserId = state.ownerUserId,
                                                farmId = farmId,
                                                inventoryItemId = selectedInventoryId,
                                                imageUri = selectedImageUri,
                                                isActiveInShop = publishToMarketplace
                                            )
                                        )
                                    },
                                    modifier = Modifier.weight(1f).height(48.dp).testTag("save_publish_ad_button")
                                ) {
                                    Icon(Icons.Default.Campaign, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text(tr("Save & Publish Ad", "বিজ্ঞাপন প্রকাশ করুন"), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }

                                OutlinedButton(
                                    onClick = {
                                        scope.launch {
                                            val bmp = adAiViewModel.decodeBitmap(context, selectedImageUri)
                                            adAiViewModel.exportMp4Video(
                                                context = context,
                                                storyboard = storyboard,
                                                productBitmap = bmp,
                                                saveToGallery = true
                                            )
                                        }
                                    },
                                    enabled = exportState !is ExportVideoUiState.Rendering,
                                    modifier = Modifier.weight(1f).height(48.dp).testTag("export_ad_mp4_button")
                                ) {
                                    Icon(Icons.Default.VideoFile, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text(tr("Download MP4", "ভিডিও MP4 নামান"), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                            }

                            // 1-Tap Social Media Sharing Bar (WhatsApp, Facebook, Instagram, Any App)
                            SocialVideoShareBar(
                                enabled = exportState !is ExportVideoUiState.Rendering,
                                onSharePlatform = { platform ->
                                    triggerPlatformShare(platform, storyboard, selectedImageUri)
                                }
                            )

                            // Export progress or share banner
                            when (val ex = exportState) {
                                is ExportVideoUiState.Rendering -> {
                                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        val targetLabel = ex.pendingPlatform?.let {
                                            " → ${tr(it.labelEn, it.labelBn)}"
                                        }.orEmpty()
                                        Text(
                                            tr(
                                                "Rendering HD MP4 Ad Video (${(ex.progress * 100).toInt()}%)$targetLabel...",
                                                "HD MP4 বিজ্ঞাপন ভিডিও তৈরি হচ্ছে (${(ex.progress * 100).toInt()}%)$targetLabel..."
                                            ),
                                            style = MaterialTheme.typography.labelMedium
                                        )
                                        LinearProgressIndicator(
                                            progress = { ex.progress.coerceIn(0f, 1f) },
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                }
                                is ExportVideoUiState.Ready -> {
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = MaterialTheme.colorScheme.primaryContainer,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    tr("MP4 Video Ad Ready!", "MP4 বিজ্ঞাপন ভিডিও প্রস্তুত!"),
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                                Text(
                                                    if (ex.savedToGallery) tr("Saved to Movies/SmartFarmAds", "গ্যালারিতে Movies/SmartFarmAds-এ সংরক্ষিত")
                                                    else tr("Ready to share", "শেয়ার করার জন্য প্রস্তুত"),
                                                    style = MaterialTheme.typography.labelSmall
                                                )
                                            }
                                            FilledTonalButton(
                                                onClick = {
                                                    AdVideoMp4Exporter.launchShareVideo(
                                                        context = context,
                                                        mp4File = File(ex.filePath),
                                                        storyboard = storyboard,
                                                        platform = SharePlatform.ANY_APP
                                                    )
                                                },
                                                modifier = Modifier.testTag("share_ad_mp4_button")
                                            ) {
                                                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Spacer(Modifier.width(4.dp))
                                                Text(tr("Share", "শেয়ার"))
                                            }
                                        }
                                    }
                                }
                                is ExportVideoUiState.Error -> {
                                    Text(ex.message, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                                }
                                else -> Unit
                            }
                        }
                    }
                }
            }
            else -> Unit
        }

        // Saved Ads Library
        item(key = "saved_ads_header") {
            Text(
                tr("Published & Saved Product Ads (${state.productAds.size})", "প্রকাশিত ও সংরক্ষিত পণ্যের বিজ্ঞাপন (${state.productAds.size})"),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        if (state.productAds.isEmpty()) {
            item(key = "saved_ads_empty") {
                EmptyStateCard(
                    icon = Icons.Default.Movie,
                    title = tr("No Product Video Ads Created Yet", "এখনও কোনো পণ্যের ভিডিও বিজ্ঞাপন তৈরি হয়নি"),
                    subtitle = tr(
                        "Pick a product or photo above and tap 'Create Bengali Video Ad with Google AI' to generate your first commercial & app ordering demo.",
                        "উপর থেকে যেকোনো পণ্য বা ছবি নির্বাচন করে Google AI দিয়ে আপনার প্রথম বাংলা বিজ্ঞাপন ভিডিও তৈরি করুন।"
                    )
                )
            }
        } else {
            items(state.productAds, key = { "saved_ad_${it.id}" }) { ad ->
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 2.dp,
                    modifier = Modifier.fillMaxWidth().testTag("product_ad_card_${ad.id}")
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                if (!ad.imageUri.isNullOrBlank()) {
                                    AsyncImage(
                                        model = ad.imageUri,
                                        contentDescription = ad.productName,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    Icon(Icons.Default.PlayCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                }
                            }
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(ad.productName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                Text(ad.hookHeadlineBn, style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                Text(
                                    "${ad.farmName} • ${if (ad.isActiveInShop) tr("LIVE IN SHOP", "বাজারে চালু") else tr("PAUSED", "বিরতিতে")}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (ad.isActiveInShop) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Switch(
                                checked = ad.isActiveInShop,
                                onCheckedChange = { onToggleAdActive(ad, it) },
                                modifier = Modifier.testTag("toggle_ad_active_${ad.id}")
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilledTonalButton(
                                onClick = { viewingSavedAd = ad },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(tr("Watch", "দেখুন"), maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            OutlinedButton(
                                onClick = {
                                    selectedInventoryId = ad.inventoryItemId
                                    productName = ad.productName
                                    farmName = ad.farmName
                                    priceText = ad.priceText
                                    selectedImageUri = ad.imageUri
                                    triggerPlatformShare(SharePlatform.ANY_APP, BengaliAdStoryboard.fromEntity(ad), ad.imageUri)
                                },
                                modifier = Modifier.weight(1f).testTag("share_saved_ad_${ad.id}")
                            ) {
                                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(tr("Share Video", "শেয়ার"), maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            IconButton(onClick = { deleteCandidate = ad }) {
                                Icon(Icons.Default.DeleteOutline, contentDescription = strings.delete, tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }
    }

    viewingSavedAd?.let { ad ->
        val adStoryboard = remember(ad) { BengaliAdStoryboard.fromEntity(ad) }
        AlertDialog(
            onDismissRequest = { viewingSavedAd = null },
            title = { Text(ad.productName, maxLines = 1, overflow = TextOverflow.Ellipsis) },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ProductAdVideoPlayer(
                        storyboard = adStoryboard,
                        imageUri = ad.imageUri,
                        autoPlay = true
                    )
                    SocialVideoShareBar(
                        enabled = exportState !is ExportVideoUiState.Rendering,
                        onSharePlatform = { platform ->
                            viewingSavedAd = null
                            selectedInventoryId = ad.inventoryItemId
                            productName = ad.productName
                            farmName = ad.farmName
                            priceText = ad.priceText
                            selectedImageUri = ad.imageUri
                            triggerPlatformShare(platform, adStoryboard, ad.imageUri)
                        }
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { viewingSavedAd = null }) { Text(tr("Close", "বন্ধ করুন")) }
            }
        )
    }

    if (showEditScriptDialog && adState is ProductAdAiUiState.Success) {
        val current = (adState as ProductAdAiUiState.Success).storyboard
        var hook by remember(current) { mutableStateOf(current.hookHeadlineBn) }
        var badge by remember(current) { mutableStateOf(current.promoBadgeBn) }
        var cta by remember(current) { mutableStateOf(current.callToActionBn) }
        var voiceover by remember(current) { mutableStateOf(current.voiceoverScriptBn) }
        var outroGuide by remember(current) { mutableStateOf(current.downloadAndOrderOutroBn) }

        AlertDialog(
            onDismissRequest = { showEditScriptDialog = false },
            title = { Text(tr("Customize Bengali Ad Copy", "বাংলা বিজ্ঞাপনের লেখা সম্পাদনা")) },
            text = {
                ProvideSafeTextToolbar {
                    Column(
                        modifier = Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(value = hook, onValueChange = { hook = it }, label = { Text(tr("Headline (Bangla)", "শিরোনাম (বাংলা)")) }, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = badge, onValueChange = { badge = it }, label = { Text(tr("Promo Badge", "অফার ব্যাজ")) }, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = cta, onValueChange = { cta = it }, label = { Text(tr("Order Button CTA", "অর্ডার বাটনের লেখা")) }, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = voiceover, onValueChange = { voiceover = it }, label = { Text(tr("Voiceover Narration (Bangla)", "ভয়েসওভার স্ক্রিপ্ট")) }, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = outroGuide, onValueChange = { outroGuide = it }, label = { Text(tr("Video End: App Download & Online Order", "ভিডিও শেষে: ডাউনলোড ও অর্ডার নিয়ম")) }, modifier = Modifier.fillMaxWidth())
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        adAiViewModel.updateStoryboard(
                            current.copy(
                                hookHeadlineBn = hook.trim().ifBlank { current.hookHeadlineBn },
                                promoBadgeBn = badge.trim().ifBlank { current.promoBadgeBn },
                                callToActionBn = cta.trim().ifBlank { current.callToActionBn },
                                voiceoverScriptBn = voiceover.trim().ifBlank { current.voiceoverScriptBn },
                                downloadAndOrderOutroBn = outroGuide.trim().ifBlank { current.downloadAndOrderOutroBn }
                            )
                        )
                        showEditScriptDialog = false
                    }
                ) { Text(strings.save) }
            },
            dismissButton = {
                TextButton(onClick = { showEditScriptDialog = false }) { Text(strings.cancel) }
            }
        )
    }

    deleteCandidate?.let { victim ->
        AlertDialog(
            onDismissRequest = { deleteCandidate = null },
            title = { Text(tr("Delete Product Ad?", "পণ্যের বিজ্ঞাপন মুছবেন?")) },
            text = { Text(tr("Remove ad for '${victim.productName}'?", "'${victim.productName}' এর বিজ্ঞাপন মুছে ফেলা হবে?")) },
            confirmButton = {
                Button(
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    onClick = {
                        onDeleteAd(victim)
                        deleteCandidate = null
                    }
                ) { Text(strings.delete) }
            },
            dismissButton = {
                TextButton(onClick = { deleteCandidate = null }) { Text(strings.cancel) }
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SocialVideoShareBar(
    enabled: Boolean,
    onSharePlatform: (SharePlatform) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(Icons.Default.Share, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                Text(
                    tr("Share Video on Any Platform:", "যেকোনো প্ল্যাটফর্মে ভিডিও শেয়ার করুন:"),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
            }
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    enabled = enabled,
                    onClick = { onSharePlatform(SharePlatform.WHATSAPP) },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366), contentColor = Color.White),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    modifier = Modifier.defaultMinSize(minHeight = 48.dp).testTag("share_video_whatsapp")
                ) {
                    Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(tr("WhatsApp", "হোয়াটসঅ্যাপ"), fontWeight = FontWeight.Bold)
                }
                Button(
                    enabled = enabled,
                    onClick = { onSharePlatform(SharePlatform.FACEBOOK) },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1877F2), contentColor = Color.White),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    modifier = Modifier.defaultMinSize(minHeight = 48.dp).testTag("share_video_facebook")
                ) {
                    Icon(Icons.Default.Public, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(tr("Facebook", "ফেসবুক"), fontWeight = FontWeight.Bold)
                }
                Button(
                    enabled = enabled,
                    onClick = { onSharePlatform(SharePlatform.INSTAGRAM) },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE1306C), contentColor = Color.White),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    modifier = Modifier.defaultMinSize(minHeight = 48.dp).testTag("share_video_instagram")
                ) {
                    Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(tr("Instagram", "ইনস্টাগ্রাম"), fontWeight = FontWeight.Bold)
                }
                OutlinedButton(
                    enabled = enabled,
                    onClick = { onSharePlatform(SharePlatform.ANY_APP) },
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    modifier = Modifier.defaultMinSize(minHeight = 48.dp).testTag("share_video_all_apps")
                ) {
                    Icon(Icons.Default.MoreHoriz, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(tr("Any App", "সব অ্যাপ"), fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
