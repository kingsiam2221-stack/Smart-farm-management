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
import com.example.i18n.AppLanguage
import com.example.i18n.CurrencyFormatter
import com.example.i18n.CurrencyOption
import com.example.ui.state.FarmerDashboardState
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

private const val TAG = "FarmAiViewModel"
private const val GEMINI_MODEL = "gemini-3.8-flash"
private const val AI_TIMEOUT_MS = 60_000L

sealed interface AiUiState {
    data object Idle : AiUiState
    data object Loading : AiUiState
    data class Streaming(val partialText: String) : AiUiState
    data class Success(val text: String) : AiUiState
    data class Error(val message: String) : AiUiState
}

data class FarmAuditReport(
    val healthScore: Int,
    val headline: String,
    val financialInsight: String,
    val livestockAndEggInsight: String,
    val riskAlert: String,
    val actionSteps: List<String>
)

sealed interface AuditUiState {
    data object Idle : AuditUiState
    data object Loading : AuditUiState
    data class Success(val report: FarmAuditReport) : AuditUiState
    data class Error(val message: String) : AuditUiState
}

data class AiChatMessage(
    val id: Long,
    val isUser: Boolean,
    val text: String,
    val hasImage: Boolean = false
)

class FarmAiViewModel : ViewModel() {

    private val chatModel by lazy {
        Firebase.ai.generativeModel(GEMINI_MODEL)
    }

    private val auditSchema by lazy {
        Schema.obj(
            properties = mapOf(
                "healthScore" to Schema.integer(description = "Overall farm operational health score from 0 to 100"),
                "headline" to Schema.string(description = "One-line executive summary of the farm status"),
                "financialInsight" to Schema.string(description = "Analysis of revenue, expenses, due payments, and profitability"),
                "livestockAndEggInsight" to Schema.string(description = "Insights on herd/flock counts, egg production efficiency, and inventory"),
                "riskAlert" to Schema.string(description = "Biggest immediate operational, health, or stock risk"),
                "actionSteps" to Schema.array(
                    items = Schema.string(),
                    description = "3 to 4 concrete prioritized action recommendations for the farmer"
                )
            )
        )
    }

    private val auditModel by lazy {
        Firebase.ai.generativeModel(
            modelName = GEMINI_MODEL,
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = auditSchema
            }
        )
    }

    private val _uiState = MutableStateFlow<AiUiState>(AiUiState.Idle)
    val uiState: StateFlow<AiUiState> = _uiState.asStateFlow()

    private val _auditState = MutableStateFlow<AuditUiState>(AuditUiState.Idle)
    val auditState: StateFlow<AuditUiState> = _auditState.asStateFlow()

    private val _messages = MutableStateFlow<List<AiChatMessage>>(emptyList())
    val messages: StateFlow<List<AiChatMessage>> = _messages.asStateFlow()

    private var lastFailedPrompt: String? = null
    private var lastFailedBitmap: Bitmap? = null

    fun askFarmAdvisor(
        prompt: String,
        farmState: FarmerDashboardState,
        currency: CurrencyOption,
        language: AppLanguage,
        imageBitmap: Bitmap? = null
    ) {
        val trimmed = prompt.trim()
        if (trimmed.isEmpty() && imageBitmap == null) return

        val effectivePrompt = if (trimmed.isNotEmpty()) trimmed
        else "Inspect this farm photo and advise on animal/bird health, feed quality, or hygiene."

        lastFailedPrompt = effectivePrompt
        lastFailedBitmap = imageBitmap

        val userMsgId = System.nanoTime()
        _messages.value = _messages.value + AiChatMessage(
            id = userMsgId,
            isUser = true,
            text = effectivePrompt,
            hasImage = imageBitmap != null
        )

        val farmContext = buildFarmContextSummary(farmState, currency, language)
        val systemPreamble = """
            You are a senior agricultural & veterinary advisor inside the Smart Farm Management mobile app.
            Provide accurate, practical, actionable guidance tailored to the farmer's live farm metrics below.
            Respond in ${if (language == AppLanguage.BN) "Bengali (Bangla)" else "English"}.
            Keep advice structured with clear short sections and bullet points. Do not invent farm figures that are not in the context.

            LIVE FARM CONTEXT:
            $farmContext
        """.trimIndent()

        viewModelScope.launch {
            _uiState.value = AiUiState.Loading
            val accumulated = StringBuilder()
            try {
                withTimeout(AI_TIMEOUT_MS) {
                    val requestContent = content {
                        text(systemPreamble)
                        if (imageBitmap != null) {
                            image(imageBitmap)
                        }
                        text("Farmer question: $effectivePrompt")
                    }
                    chatModel.generateContentStream(requestContent).collect { chunk ->
                        val chunkText = chunk.text.orEmpty()
                        if (chunkText.isNotEmpty()) {
                            accumulated.append(chunkText)
                            _uiState.value = AiUiState.Streaming(accumulated.toString())
                        }
                    }
                }
                val finalAnswer = accumulated.toString().trim()
                if (finalAnswer.isNotEmpty()) {
                    _messages.value = _messages.value + AiChatMessage(
                        id = System.nanoTime(),
                        isUser = false,
                        text = finalAnswer
                    )
                    _uiState.value = AiUiState.Success(finalAnswer)
                    lastFailedPrompt = null
                    lastFailedBitmap = null
                } else {
                    _uiState.value = AiUiState.Error("Gemini returned an empty response. Please try again.")
                }
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "Streaming farm advisor request timed out", e)
                _uiState.value = AiUiState.Error("Request timed out after 60 seconds. Check your connection and tap Retry.")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Firebase AI chat generation failed", e)
                _uiState.value = AiUiState.Error(e.localizedMessage ?: "Failed to reach Gemini AI.")
            }
        }
    }

    fun retryLastQuestion(
        farmState: FarmerDashboardState,
        currency: CurrencyOption,
        language: AppLanguage
    ) {
        val prompt = lastFailedPrompt ?: return
        val bmp = lastFailedBitmap
        askFarmAdvisor(
            prompt = prompt,
            farmState = farmState,
            currency = currency,
            language = language,
            imageBitmap = bmp
        )
    }

    fun runStructuredFarmAudit(
        farmState: FarmerDashboardState,
        currency: CurrencyOption,
        language: AppLanguage
    ) {
        val farmContext = buildFarmContextSummary(farmState, currency, language)
        val prompt = """
            Analyze the live farm operations data below and produce a structured operational & financial audit.
            Write all string values in ${if (language == AppLanguage.BN) "Bengali (Bangla)" else "English"}.

            LIVE FARM CONTEXT:
            $farmContext
        """.trimIndent()

        viewModelScope.launch {
            _auditState.value = AuditUiState.Loading
            try {
                val response = withTimeout(AI_TIMEOUT_MS) {
                    auditModel.generateContent(prompt)
                }
                val jsonText = response.text
                    ?: throw IllegalStateException("Model returned empty structured audit response.")

                val json = JSONObject(jsonText)
                val actionsArray = json.optJSONArray("actionSteps")
                val steps = buildList {
                    if (actionsArray != null) {
                        for (i in 0 until actionsArray.length()) {
                            val item = actionsArray.optString(i).trim()
                            if (item.isNotEmpty()) add(item)
                        }
                    }
                }
                val report = FarmAuditReport(
                    healthScore = json.optInt("healthScore", 75).coerceIn(0, 100),
                    headline = json.optString("headline", "Farm Audit Complete"),
                    financialInsight = json.optString("financialInsight", ""),
                    livestockAndEggInsight = json.optString("livestockAndEggInsight", ""),
                    riskAlert = json.optString("riskAlert", ""),
                    actionSteps = steps
                )
                _auditState.value = AuditUiState.Success(report)
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "Structured farm audit timed out", e)
                _auditState.value = AuditUiState.Error("Audit timed out after 60 seconds. Please try again.")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Structured farm audit failed", e)
                _auditState.value = AuditUiState.Error(e.localizedMessage ?: "Unable to generate farm audit.")
            }
        }
    }

    fun clearChat() {
        _messages.value = emptyList()
        _uiState.value = AiUiState.Idle
    }

    fun clearAudit() {
        _auditState.value = AuditUiState.Idle
    }

    private fun buildFarmContextSummary(
        state: FarmerDashboardState,
        currency: CurrencyOption,
        language: AppLanguage
    ): String {
        val activeFarm = state.selectedFarmId?.let { id -> state.farms.find { it.id == id } }
        val animals = if (activeFarm == null) state.animals else state.animals.filter { it.farmId == activeFarm.id }
        val eggs = if (activeFarm == null) state.eggs else state.eggs.filter { it.farmId == activeFarm.id }
        val sales = if (activeFarm == null) state.sales else state.sales.filter { it.farmId == activeFarm.id }
        val expenses = if (activeFarm == null) state.expenses else state.expenses.filter { it.farmId == activeFarm.id }
        val inventory = if (activeFarm == null) state.inventory else state.inventory.filter { it.farmId == activeFarm.id }
        val orders = if (activeFarm == null) state.incomingOrders else state.incomingOrders.filter { it.farmId == activeFarm.id }

        val totalAnimals = animals.filter { !it.birdCategory }.sumOf { it.quantity }
        val totalBirds = animals.filter { it.birdCategory }.sumOf { it.quantity }
        val unhealthyGroups = animals.filter { !it.healthStatus.equals("Healthy", ignoreCase = true) }
        val totalSalesAmt = sales.sumOf { it.totalAmount }
        val totalPaidAmt = sales.sumOf { it.paidAmount }
        val totalDueAmt = sales.sumOf { it.dueAmount }
        val totalExpensesAmt = expenses.sumOf { it.amount }
        val netProfit = totalSalesAmt - totalExpensesAmt
        val eggsProduced = eggs.sumOf { it.producedCount }
        val eggsBroken = eggs.sumOf { it.brokenCount }
        val eggsAvailable = eggs.groupBy { it.farmId to it.eggCategory }
            .values.sumOf { list -> list.maxByOrNull { it.productionDate }?.availableCount ?: 0 }
        val lowStockItems = inventory.filter { it.quantity <= it.lowStockThreshold }
        val pendingOrders = orders.count { it.status == "PENDING" }

        val scopeLabel = activeFarm?.let { "${it.name} (${it.category}, ${it.location})" }
            ?: "All ${state.farms.size} Farms (${state.farms.take(8).joinToString { f -> f.name }})"

        return buildString {
            appendLine("- Scope: $scopeLabel")
            appendLine("- Language: ${language.name}, Display Currency: ${currency.code} (${currency.symbol})")
            appendLine("- Livestock (non-bird): $totalAnimals head across ${animals.count { !it.birdCategory }} batches")
            appendLine("- Poultry & Birds: $totalBirds birds across ${animals.count { it.birdCategory }} flocks")
            if (unhealthyGroups.isNotEmpty()) {
                appendLine("- Health Warnings: ${unhealthyGroups.take(5).joinToString { "${it.name} (${it.healthStatus}, qty ${it.quantity})" }}")
            }
            appendLine("- Eggs: Produced=$eggsProduced, Broken=$eggsBroken, Currently Available=$eggsAvailable")
            appendLine("- Financials: Sales=${CurrencyFormatter.format(totalSalesAmt, currency)}, Paid=${CurrencyFormatter.format(totalPaidAmt, currency)}, Due=${CurrencyFormatter.format(totalDueAmt, currency)}, Expenses=${CurrencyFormatter.format(totalExpensesAmt, currency)}, Net P/L=${CurrencyFormatter.format(netProfit, currency)}")
            appendLine("- Low Stock Inventory (${lowStockItems.size}): ${lowStockItems.take(6).joinToString { "${it.name} (${it.quantity} ${it.unit})" }.ifBlank { "None" }}")
            appendLine("- Pending Online Orders: $pendingOrders")
        }
    }

    suspend fun decodeScaledBitmap(context: Context, uri: Uri, maxDimension: Int = 1024): Bitmap? =
        withContext(Dispatchers.IO) {
            try {
                val rawBitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    val source = ImageDecoder.createSource(context.contentResolver, uri)
                    ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                        decoder.isMutableRequired = false
                    }
                } else {
                    @Suppress("DEPRECATION")
                    MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
                }
                val largest = max(rawBitmap.width, rawBitmap.height)
                if (largest <= maxDimension) {
                    rawBitmap
                } else {
                    val scale = maxDimension.toFloat() / largest.toFloat()
                    val w = (rawBitmap.width * scale).roundToInt().coerceAtLeast(1)
                    val h = (rawBitmap.height * scale).roundToInt().coerceAtLeast(1)
                    Bitmap.createScaledBitmap(rawBitmap, w, h, true)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to decode selected photo", e)
                null
            }
        }
}
