package com.example.ai

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.ProductAdEntity
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.json.JSONObject
import kotlin.math.max
import kotlin.math.roundToInt

private const val AD_TAG = "ProductAdAiViewModel"
private const val AD_GEMINI_MODEL = "gemini-3.8-flash"
private const val AD_AI_TIMEOUT_MS = 60_000L

data class BengaliAdStoryboard(
    val productName: String,
    val farmName: String,
    val priceText: String,
    val hookHeadlineBn: String,
    val promoBadgeBn: String,
    val benefitsBn: List<String>,
    val callToActionBn: String,
    val voiceoverScriptBn: String,
    val orderStepsBn: List<String>,
    val accentColorHex: String,
    val downloadAndOrderOutroBn: String = DEFAULT_DOWNLOAD_AND_ORDER_OUTRO_BN
) {
    fun toEntity(
        existingId: Long = 0L,
        ownerUserId: Long,
        farmId: Long,
        inventoryItemId: Long?,
        imageUri: String?,
        isActiveInShop: Boolean
    ): ProductAdEntity {
        val packedSteps = (orderStepsBn.take(4) + listOf("$OUTRO_PREFIX${downloadAndOrderOutroBn.trim().ifBlank { DEFAULT_DOWNLOAD_AND_ORDER_OUTRO_BN }}"))
            .joinToString("|")
            .take(780)
        return ProductAdEntity(
            id = existingId,
            ownerUserId = ownerUserId,
            farmId = farmId,
            inventoryItemId = inventoryItemId,
            productName = productName,
            farmName = farmName,
            priceText = priceText,
            imageUri = imageUri,
            hookHeadlineBn = hookHeadlineBn,
            promoBadgeBn = promoBadgeBn,
            benefitsPipeSeparatedBn = benefitsBn.joinToString("|"),
            callToActionBn = callToActionBn,
            voiceoverScriptBn = voiceoverScriptBn,
            orderStepsPipeSeparatedBn = packedSteps,
            accentColorHex = accentColorHex,
            isActiveInShop = isActiveInShop
        )
    }

    companion object {
        private const val OUTRO_PREFIX = "OUTRO:"
        const val DEFAULT_DOWNLOAD_AND_ORDER_OUTRO_BN =
            "Smart Farm অ্যাপ Play Store বা শেয়ার লিংক থেকে ডাউনলোড করে কাস্টমার হিসেবে ফ্রি অ্যাকাউন্ট খুলুন, এরপর বাজার থেকে পণ্য বেছে বিকাশ/নগদ/ক্যাশে অনলাইনে অর্ডার করুন!"

        fun fromEntity(entity: ProductAdEntity): BengaliAdStoryboard {
            val rawParts = entity.orderStepsPipeSeparatedBn.split("|").map { it.trim() }.filter { it.isNotEmpty() }
            val outroPart = rawParts.firstOrNull { it.startsWith(OUTRO_PREFIX) }
                ?.removePrefix(OUTRO_PREFIX)
                ?.trim()
                ?.ifBlank { DEFAULT_DOWNLOAD_AND_ORDER_OUTRO_BN }
                ?: DEFAULT_DOWNLOAD_AND_ORDER_OUTRO_BN
            val stepsOnly = rawParts.filterNot { it.startsWith(OUTRO_PREFIX) }
                .ifEmpty { defaultOrderSteps() }

            return BengaliAdStoryboard(
                productName = entity.productName,
                farmName = entity.farmName,
                priceText = entity.priceText,
                hookHeadlineBn = entity.hookHeadlineBn,
                promoBadgeBn = entity.promoBadgeBn,
                benefitsBn = entity.benefitsPipeSeparatedBn.split("|").map { it.trim() }.filter { it.isNotEmpty() }
                    .ifEmpty { defaultBenefits(entity.productName) },
                callToActionBn = entity.callToActionBn.ifBlank { "এখনই অর্ডার করুন • এখানে ক্লিক করুন" },
                voiceoverScriptBn = entity.voiceoverScriptBn.ifBlank {
                    "${entity.productName} কিনতে Smart Farm অ্যাপ ডাউনলোড করুন এবং এখনই অনলাইনে অর্ডার করুন!"
                },
                orderStepsBn = stepsOnly,
                accentColorHex = entity.accentColorHex.ifBlank { "#1B5E20" },
                downloadAndOrderOutroBn = outroPart
            )
        }

        fun defaultBenefits(productName: String): List<String> = listOf(
            "সরাসরি খামার থেকে সংগৃহীত ১০০% তাজা ও খাঁটি ${productName.ifBlank { "পণ্য" }}",
            "কোনো ভেজাল বা ক্ষতিকর রাসায়নিক ছাড়া সম্পূর্ণ স্বাস্থ্যসম্মত",
            "সারা দেশে দ্রুত ডেলিভারি ও সাশ্রয়ী খামার মূল্য"
        )

        fun defaultOrderSteps(): List<String> = listOf(
            "১. বাজার থেকে আপনার পছন্দের খামার পণ্য বেছে নিন",
            "২. 'এখানে ক্লিক করুন' / 'কার্টে যোগ করুন' বাটনে ট্যাপ করুন",
            "৩. ডেলিভারি ঠিকানা ও বিকাশ/নগদ/ক্যাশ পেমেন্ট দিন",
            "৪. 'এখনই অর্ডার করুন' চাপুন — খামারে সাথে সাথে অর্ডার পৌঁছাবে!"
        )
    }
}

sealed interface ProductAdAiUiState {
    data object Idle : ProductAdAiUiState
    data object Loading : ProductAdAiUiState
    data class Success(val storyboard: BengaliAdStoryboard) : ProductAdAiUiState
    data class Error(val message: String) : ProductAdAiUiState
}

sealed interface ExportVideoUiState {
    data object Idle : ExportVideoUiState
    data class Rendering(val progress: Float, val pendingPlatform: SharePlatform? = null) : ExportVideoUiState
    data class Ready(
        val filePath: String,
        val savedToGallery: Boolean,
        val autoSharePlatform: SharePlatform? = null
    ) : ExportVideoUiState
    data class Error(val message: String) : ExportVideoUiState
}

class ProductAdAiViewModel : ViewModel() {

    private val adSchema by lazy {
        Schema.obj(
            properties = mapOf(
                "hookHeadlineBn" to Schema.string(
                    description = "Catchy commercial hook headline in Bengali (Bangla), max 10 words, e.g. গ্রামবাংলার সেরা খাঁটি সুগন্ধি চাল!"
                ),
                "promoBadgeBn" to Schema.string(
                    description = "Short 2-4 word Bengali promo badge, e.g. ১০০% খাঁটি বা স্পেশাল অফার"
                ),
                "benefitsBn" to Schema.array(
                    items = Schema.string(),
                    description = "Exactly 3 compelling short product selling points in natural Bengali script"
                ),
                "callToActionBn" to Schema.string(
                    description = "High-energy Bengali call to action including Order Now and Click Here, e.g. এখনই অর্ডার করুন • এখানে ক্লিক করুন"
                ),
                "voiceoverScriptBn" to Schema.string(
                    description = "Lively 16-second Bengali TV/Reels commercial voiceover script introducing the product quality, price, guiding the viewer on how to tap Order Now / Click Here, and ending by telling viewers how to download the Smart Farm app and order online"
                ),
                "orderStepsBn" to Schema.array(
                    items = Schema.string(),
                    description = "Exactly 4 concise Bengali screen-recording tutorial steps showing how to order from the app: 1) browse product, 2) tap click here / add to cart, 3) enter address & bKash/Nagad/COD, 4) tap order now & farm gets notified"
                ),
                "downloadAndOrderOutroBn" to Schema.string(
                    description = "1-2 short Bengali sentences for the video ending outro explaining how to download the Smart Farm app (from Play Store or shared link) and place an online order with bKash/Nagad/Cash on Delivery"
                ),
                "accentColorHex" to Schema.string(
                    description = "Rich vibrant commercial theme hex color fitting the product, e.g. #1B5E20, #E65100, #B71C1C, #0D47A1, or #2E7D32"
                )
            )
        )
    }

    private val adModel by lazy {
        Firebase.ai.generativeModel(
            modelName = AD_GEMINI_MODEL,
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = adSchema
            }
        )
    }

    private val _adState = MutableStateFlow<ProductAdAiUiState>(ProductAdAiUiState.Idle)
    val adState: StateFlow<ProductAdAiUiState> = _adState.asStateFlow()

    private val _exportState = MutableStateFlow<ExportVideoUiState>(ExportVideoUiState.Idle)
    val exportState: StateFlow<ExportVideoUiState> = _exportState.asStateFlow()

    fun loadExistingStoryboard(storyboard: BengaliAdStoryboard) {
        _adState.value = ProductAdAiUiState.Success(storyboard)
        _exportState.value = ExportVideoUiState.Idle
    }

    fun updateStoryboard(storyboard: BengaliAdStoryboard) {
        _adState.value = ProductAdAiUiState.Success(storyboard)
        _exportState.value = ExportVideoUiState.Idle
    }

    fun generateBengaliAdStoryboard(
        productName: String,
        category: String,
        farmName: String,
        priceText: String,
        notes: String,
        productBitmap: Bitmap?
    ) {
        val cleanName = productName.trim()
        if (cleanName.isEmpty()) {
            _adState.value = ProductAdAiUiState.Error("পণ্যের নাম লিখুন বা তালিকা থেকে একটি পণ্য নির্বাচন করুন।")
            return
        }

        val promptText = """
            You are an award-winning Bangladeshi TV & social media commercial director and copywriter.
            Generate a high-converting 16-second Bengali (Bangla) video commercial storyboard, app ordering screen-record tutorial, and closing app-download + online-order outro for this farm/agricultural product:
            - Product Name: $cleanName
            - Category: ${category.ifBlank { "Agricultural Farm Product" }}
            - Farm / Business Name: ${farmName.ifBlank { "স্মার্ট খামার" }}
            - Price: ${priceText.ifBlank { "সাশ্রয়ী খামার মূল্য" }}
            - Farmer Promo Notes: ${notes.ifBlank { "তাজা, ভেজালমুক্ত এবং সরাসরি খামার থেকে সংগ্রহ করা।" }}
            ${if (productBitmap != null) "- A real photo of the product is attached. Describe its authentic visual freshness, color, and appeal naturally in Bengali." else ""}

            Strict Rules:
            1. Write ALL commercial copy strictly in natural, persuasive, clear Bengali script (বাংলা).
            2. Make it feel like a major trusted food/agro brand advertisement suitable for Facebook, WhatsApp, Instagram Reels, and in-app promo.
            3. Emphasize "এখনই অর্ডার করুন" (Order Now) and "এখানে ক্লিক করুন" (Click Here).
            4. In `orderStepsBn`, provide 4 crystal-clear Bengali step captions matching a screen-recording walkthrough of browsing the product, tapping "এখানে ক্লিক করুন / কার্টে যোগ করুন", entering delivery address & bKash/Nagad/Cash on Delivery, and tapping "এখনই অর্ডার করুন".
            5. In `downloadAndOrderOutroBn` (and at the end of `voiceoverScriptBn`), clearly tell viewers how to download the Smart Farm app (from Play Store or the shared link), open a free customer account, and order online easily.
        """.trimIndent()

        viewModelScope.launch {
            _adState.value = ProductAdAiUiState.Loading
            _exportState.value = ExportVideoUiState.Idle
            try {
                val response = withTimeout(AD_AI_TIMEOUT_MS) {
                    val req = content {
                        if (productBitmap != null) {
                            image(productBitmap)
                        }
                        text(promptText)
                    }
                    adModel.generateContent(req)
                }
                val jsonText = response.text?.trim()
                if (jsonText.isNullOrEmpty()) {
                    _adState.value = ProductAdAiUiState.Error("Google AI কোনো উত্তর দেয়নি। অনুগ্রহ করে আবার চেষ্টা করুন।")
                    return@launch
                }

                val json = JSONObject(jsonText)
                val benefitsArray = json.optJSONArray("benefitsBn")
                val benefits = buildList {
                    if (benefitsArray != null) {
                        for (i in 0 until benefitsArray.length()) {
                            val b = benefitsArray.optString(i).trim()
                            if (b.isNotEmpty()) add(b)
                        }
                    }
                }.ifEmpty { BengaliAdStoryboard.defaultBenefits(cleanName) }

                val stepsArray = json.optJSONArray("orderStepsBn")
                val steps = buildList {
                    if (stepsArray != null) {
                        for (i in 0 until stepsArray.length()) {
                            val s = stepsArray.optString(i).trim()
                            if (s.isNotEmpty()) add(s)
                        }
                    }
                }.ifEmpty { BengaliAdStoryboard.defaultOrderSteps() }

                val rawHex = json.optString("accentColorHex", "#1B5E20").trim()
                val validHex = if (Regex("^#[0-9A-Fa-f]{6}$").matches(rawHex)) rawHex else "#1B5E20"

                val storyboard = BengaliAdStoryboard(
                    productName = cleanName,
                    farmName = farmName.ifBlank { "স্মার্ট খামার" },
                    priceText = priceText.ifBlank { "খামার মূল্য" },
                    hookHeadlineBn = json.optString("hookHeadlineBn").trim().ifBlank {
                        "খাঁটি ও তাজা $cleanName — সরাসরি খামার থেকে!"
                    },
                    promoBadgeBn = json.optString("promoBadgeBn").trim().ifBlank { "১০০% খাঁটি" },
                    benefitsBn = benefits.take(4),
                    callToActionBn = json.optString("callToActionBn").trim().ifBlank {
                        "এখনই অর্ডার করুন • এখানে ক্লিক করুন"
                    },
                    voiceoverScriptBn = json.optString("voiceoverScriptBn").trim().ifBlank {
                        "সরাসরি খামার থেকে তাজা $cleanName পেতে এখনই Play Store থেকে Smart Farm অ্যাপ ডাউনলোড করুন, এখানে ক্লিক করে অনলাইনে অর্ডার করুন!"
                    },
                    orderStepsBn = steps.take(4),
                    accentColorHex = validHex,
                    downloadAndOrderOutroBn = json.optString("downloadAndOrderOutroBn").trim().ifBlank {
                        BengaliAdStoryboard.DEFAULT_DOWNLOAD_AND_ORDER_OUTRO_BN
                    }
                )
                _adState.value = ProductAdAiUiState.Success(storyboard)
            } catch (e: TimeoutCancellationException) {
                Log.e(AD_TAG, "Ad generation timed out", e)
                _adState.value = ProductAdAiUiState.Error("সময় শেষ হয়েছে (৬০ সেকেন্ড)। ইন্টারনেট সংযোগ দেখে আবার চেষ্টা করুন।")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(AD_TAG, "Ad generation failed", e)
                _adState.value = ProductAdAiUiState.Error(e.localizedMessage ?: "Google AI দিয়ে বিজ্ঞাপন তৈরি করা যায়নি।")
            }
        }
    }

    fun exportMp4Video(
        context: Context,
        storyboard: BengaliAdStoryboard,
        productBitmap: Bitmap?,
        saveToGallery: Boolean,
        autoSharePlatform: SharePlatform? = null
    ) {
        viewModelScope.launch {
            _exportState.value = ExportVideoUiState.Rendering(0.02f, pendingPlatform = autoSharePlatform)
            try {
                val mp4File = AdVideoMp4Exporter.exportCommercialMp4(
                    context = context.applicationContext,
                    storyboard = storyboard,
                    productBitmap = productBitmap,
                    onProgress = { progress ->
                        _exportState.value = ExportVideoUiState.Rendering(progress, pendingPlatform = autoSharePlatform)
                    }
                )
                var savedGallery = false
                if (saveToGallery) {
                    savedGallery = withContext(Dispatchers.IO) {
                        AdVideoMp4Exporter.saveMp4ToMoviesGallery(context.applicationContext, mp4File) != null
                    }
                }
                _exportState.value = ExportVideoUiState.Ready(
                    filePath = mp4File.absolutePath,
                    savedToGallery = savedGallery,
                    autoSharePlatform = autoSharePlatform
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(AD_TAG, "MP4 export failed", e)
                _exportState.value = ExportVideoUiState.Error(
                    e.localizedMessage ?: "ভিডিও MP4 তৈরি করতে সমস্যা হয়েছে।"
                )
            }
        }
    }

    fun consumeAutoShare() {
        val current = _exportState.value
        if (current is ExportVideoUiState.Ready && current.autoSharePlatform != null) {
            _exportState.value = current.copy(autoSharePlatform = null)
        }
    }

    fun resetExportState() {
        _exportState.value = ExportVideoUiState.Idle
    }

    suspend fun decodeBitmap(context: Context, uriString: String?, maxDimension: Int = 1024): Bitmap? {
        if (uriString.isNullOrBlank()) return null
        return withContext(Dispatchers.IO) {
            try {
                val uri = Uri.parse(uriString)
                val raw = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    val src = ImageDecoder.createSource(context.contentResolver, uri)
                    ImageDecoder.decodeBitmap(src) { decoder, _, _ ->
                        decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                        decoder.isMutableRequired = false
                    }
                } else {
                    @Suppress("DEPRECATION")
                    MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
                }
                val largest = max(raw.width, raw.height)
                if (largest <= maxDimension) {
                    raw
                } else {
                    val scale = maxDimension.toFloat() / largest.toFloat()
                    Bitmap.createScaledBitmap(
                        raw,
                        (raw.width * scale).roundToInt().coerceAtLeast(1),
                        (raw.height * scale).roundToInt().coerceAtLeast(1),
                        true
                    )
                }
            } catch (e: Exception) {
                Log.w(AD_TAG, "Could not decode product image URI: $uriString", e)
                null
            }
        }
    }
}
