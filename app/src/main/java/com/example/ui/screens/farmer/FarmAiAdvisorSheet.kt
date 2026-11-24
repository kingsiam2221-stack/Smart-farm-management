package com.example.ui.screens.farmer

import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ai.*
import com.example.i18n.AppLanguage
import com.example.i18n.CurrencyOption
import com.example.i18n.tr
import com.example.ui.state.FarmerDashboardState
import kotlinx.coroutines.launch

@Composable
fun FarmAiAdvisorDialog(
    farmState: FarmerDashboardState,
    currency: CurrencyOption,
    language: AppLanguage,
    aiViewModel: FarmAiViewModel,
    onDismiss: () -> Unit
) {
    BackHandler(onBack = onDismiss)

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val uiState by aiViewModel.uiState.collectAsStateWithLifecycle()
    val auditState by aiViewModel.auditState.collectAsStateWithLifecycle()
    val messages by aiViewModel.messages.collectAsStateWithLifecycle()

    var promptText by remember { mutableStateOf("") }
    var attachedBitmap by remember { mutableStateOf<Bitmap?>(null) }

    val listState = rememberLazyListState()
    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                attachedBitmap = aiViewModel.decodeScaledBitmap(context, uri)
            }
        }
    }

    val isBangla = language == AppLanguage.BN
    val activeFarmName = remember(farmState.farms, farmState.selectedFarmId, isBangla) {
        farmState.selectedFarmId?.let { id -> farmState.farms.find { it.id == id }?.name }
            ?: if (isBangla) "সকল খামার (${farmState.farms.size})" else "All Farms (${farmState.farms.size})"
    }

    val quickPrompts = remember(isBangla) {
        if (isBangla) {
            listOf(
                "খামারের লাভ-ক্ষতি ও খরচ কমানোর পরামর্শ দিন",
                "ডিম উৎপাদন ও মুরগির স্বাস্থ্য বাড়ানোর উপায় কী?",
                "কম স্টক ও খাদ্য ব্যবস্থাপনা পর্যালোচনা করুন",
                "গবাদি পশুর রোগ প্রতিরোধ ও টিকা পরিকল্পনা দিন"
            )
        } else {
            listOf(
                "Analyze profit margin & top cost-saving opportunities",
                "How can I boost daily egg yield & reduce breakage?",
                "Review low inventory & feed procurement priorities",
                "Biosecurity & vaccination checklist for my flock/herd"
            )
        }
    }

    LaunchedEffect(messages.size, uiState) {
        val total = listState.layoutInfo.totalItemsCount
        if (messages.isNotEmpty() && total > 0) {
            listState.animateScrollToItem((total - 1).coerceAtLeast(0))
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp, vertical = 16.dp)
                .widthIn(max = 680.dp),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.background,
            tonalElevation = 6.dp
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Top Header Bar
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(40.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Default.AutoAwesome,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimary
                                    )
                                }
                            }
                            Column {
                                Text(
                                    if (isBangla) "স্মার্ট ফার্ম এআই উপদেষ্টা" else "Smart Farm AI Advisor",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    activeFarmName,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                )
                            }
                        }
                        Row {
                            if (messages.isNotEmpty() || auditState !is AuditUiState.Idle) {
                                IconButton(
                                    onClick = {
                                        aiViewModel.clearChat()
                                        aiViewModel.clearAudit()
                                    },
                                    modifier = Modifier.testTag("ai_clear_button")
                                ) {
                                    Icon(
                                        Icons.Default.DeleteSweep,
                                        contentDescription = if (isBangla) "পরিষ্কার করুন" else "Clear Chat",
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                            IconButton(
                                onClick = onDismiss,
                                modifier = Modifier.testTag("ai_close_button")
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = if (isBangla) "বন্ধ করুন" else "Close",
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }
                }

                // Quick Actions & Structured Audit Trigger
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = {
                                aiViewModel.runStructuredFarmAudit(farmState, currency, language)
                            },
                            enabled = auditState !is AuditUiState.Loading,
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                                .testTag("ai_run_audit_button")
                        ) {
                            if (auditState is AuditUiState.Loading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(if (isBangla) "বিশ্লেষণ চলছে..." else "Auditing Farm...")
                            } else {
                                Icon(Icons.Default.Analytics, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(if (isBangla) "এআই ফার্ম অডিট" else "Run AI Farm Audit")
                            }
                        }
                        OutlinedButton(
                            onClick = {
                                com.example.ui.components.safeLaunchImagePicker(photoPicker)
                            },
                            modifier = Modifier
                                .heightIn(min = 48.dp)
                                .testTag("ai_attach_photo_button")
                        ) {
                            Icon(Icons.Default.AddAPhoto, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(if (isBangla) "ছবি যোগ" else "Photo")
                        }
                    }

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(quickPrompts) { quickPrompt ->
                            SuggestionChip(
                                onClick = {
                                    if (uiState !is AiUiState.Loading && uiState !is AiUiState.Streaming) {
                                        aiViewModel.askFarmAdvisor(
                                            prompt = quickPrompt,
                                            farmState = farmState,
                                            currency = currency,
                                            language = language
                                        )
                                    }
                                },
                                label = { Text(quickPrompt, maxLines = 1) }
                            )
                        }
                    }
                }

                HorizontalDivider()

                // Main Scrollable Feed (Structured Audit Card + Chat History + Streaming Bubble + Errors)
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentPadding = PaddingValues(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Structured Audit Result / Error
                    when (val aState = auditState) {
                        is AuditUiState.Success -> {
                            item(key = "audit_card") {
                                FarmAuditReportCard(
                                    report = aState.report,
                                    isBangla = isBangla,
                                    onDismiss = aiViewModel::clearAudit
                                )
                            }
                        }
                        is AuditUiState.Error -> {
                            item(key = "audit_error") {
                                AiErrorBanner(
                                    message = aState.message,
                                    isBangla = isBangla,
                                    onRetry = { aiViewModel.runStructuredFarmAudit(farmState, currency, language) }
                                )
                            }
                        }
                        else -> Unit
                    }

                    if (messages.isEmpty() && uiState is AiUiState.Idle && auditState is AuditUiState.Idle) {
                        item {
                            Surface(
                                shape = MaterialTheme.shapes.medium,
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(18.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Eco,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(32.dp)
                                    )
                                    Text(
                                        if (isBangla)
                                            "আপনার খামারের তথ্য বিশ্লেষণ বা পশুপাখির ছবি পরীক্ষা করতে নিচে প্রশ্ন লিখুন অথবা এআই ফার্ম অডিট চাপুন।"
                                        else
                                            "Ask anything about livestock health, egg yield, feed planning, or farm profit — or attach a photo for visual inspection.",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }
                        }
                    }

                    items(messages, key = { it.id }) { msg ->
                        AiMessageBubble(msg)
                    }

                    // Active Loading / Streaming / Error state for chat
                    when (val state = uiState) {
                        is AiUiState.Loading -> {
                            item(key = "chat_loading") {
                                Row(
                                    modifier = Modifier.padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                                    Text(
                                        if (isBangla) "উত্তর তৈরি হচ্ছে..." else "Analyzing your farm data...",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                        is AiUiState.Streaming -> {
                            item(key = "chat_streaming") {
                                AiMessageBubble(
                                    AiChatMessage(
                                        id = -1L,
                                        isUser = false,
                                        text = state.partialText
                                    )
                                )
                            }
                        }
                        is AiUiState.Error -> {
                            item(key = "chat_error") {
                                AiErrorBanner(
                                    message = state.message,
                                    isBangla = isBangla,
                                    onRetry = { aiViewModel.retryLastQuestion(farmState, currency, language) }
                                )
                            }
                        }
                        else -> Unit
                    }
                }

                // Selected Photo Preview Strip
                val previewBmp = attachedBitmap
                if (previewBmp != null) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Image(
                                    bitmap = previewBmp.asImageBitmap(),
                                    contentDescription = tr("Selected photo", "নির্বাচিত ছবি"),
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                )
                                Text(
                                    if (isBangla) "ছবি সংযুক্ত হয়েছে" else "Photo attached for inspection",
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
                            IconButton(onClick = { attachedBitmap = null }) {
                                Icon(Icons.Default.Close, contentDescription = tr("Remove photo", "ছবি সরান"))
                            }
                        }
                    }
                }

                // Bottom Prompt Input Bar
                Surface(
                    tonalElevation = 3.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = promptText,
                            onValueChange = { promptText = it },
                            placeholder = {
                                Text(
                                    if (isBangla) "খামার সম্পর্কিত প্রশ্ন লিখুন..."
                                    else "Ask about feed, illness, eggs, or profit..."
                                )
                            },
                            maxLines = 3,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("ai_prompt_input")
                        )
                        val canSend = (promptText.isNotBlank() || attachedBitmap != null) &&
                            uiState !is AiUiState.Loading &&
                            uiState !is AiUiState.Streaming
                        FilledIconButton(
                            onClick = {
                                val textToSend = promptText
                                val bmpToSend = attachedBitmap
                                promptText = ""
                                attachedBitmap = null
                                aiViewModel.askFarmAdvisor(
                                    prompt = textToSend,
                                    farmState = farmState,
                                    currency = currency,
                                    language = language,
                                    imageBitmap = bmpToSend
                                )
                            },
                            enabled = canSend,
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("ai_send_button")
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.Send,
                                contentDescription = if (isBangla) "পাঠান" else "Send"
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AiMessageBubble(msg: AiChatMessage) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (msg.isUser) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (msg.isUser) 16.dp else 4.dp,
                bottomEnd = if (msg.isUser) 4.dp else 16.dp
            ),
            color = if (msg.isUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.widthIn(max = 520.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                if (msg.hasImage) {
                    Text(
                        tr("[Photo Attached]", "[ছবি সংযুক্ত]"),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (msg.isUser) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f)
                        else MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Text(
                    msg.text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (msg.isUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun FarmAuditReportCard(
    report: FarmAuditReport,
    isBangla: Boolean,
    onDismiss: () -> Unit
) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f),
        tonalElevation = 2.dp,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("ai_audit_report_card")
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(44.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                "${report.healthScore}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                    Column {
                        Text(
                            if (isBangla) "এআই ফার্ম স্কোরকার্ড" else "AI Farm Audit Scorecard",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            report.headline,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = tr("Close Audit", "অডিট বন্ধ করুন"))
                }
            }

            if (report.financialInsight.isNotBlank()) {
                Text(
                    "• ${if (isBangla) "আর্থিক" else "Financial"}: ${report.financialInsight}",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            if (report.livestockAndEggInsight.isNotBlank()) {
                Text(
                    "• ${if (isBangla) "পশুপাখি ও ডিম" else "Livestock & Eggs"}: ${report.livestockAndEggInsight}",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            if (report.riskAlert.isNotBlank()) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.errorContainer
                ) {
                    Text(
                        "${if (isBangla) "ঝুঁকি সতর্কতা" else "Risk Alert"}: ${report.riskAlert}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }
            if (report.actionSteps.isNotEmpty()) {
                Text(
                    if (isBangla) "করণীয় পদক্ষেপ:" else "Recommended Next Steps:",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
                report.actionSteps.forEachIndexed { idx, step ->
                    Text("${idx + 1}. $step", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
private fun AiErrorBanner(
    message: String,
    isBangla: Boolean,
    onRetry: () -> Unit
) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.errorContainer,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("ai_error_banner")
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    Icons.Default.ErrorOutline,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onErrorContainer
                )
                Text(
                    message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
            TextButton(onClick = onRetry) {
                Text(if (isBangla) "আবার চেষ্টা" else "Retry")
            }
        }
    }
}
