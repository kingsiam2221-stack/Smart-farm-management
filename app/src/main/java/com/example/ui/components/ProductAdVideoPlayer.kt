package com.example.ui.components

import android.speech.tts.TextToSpeech
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.ai.BengaliAdStoryboard
import com.example.i18n.tr
import kotlinx.coroutines.delay
import java.util.Locale

private const val TOTAL_AD_DURATION_MS = 18_000L

@Composable
fun ProductAdVideoPlayer(
    storyboard: BengaliAdStoryboard,
    imageUri: String?,
    modifier: Modifier = Modifier,
    autoPlay: Boolean = true,
    onOrderNowClick: (() -> Unit)? = null
) {
    val context = LocalContext.current
    var isPlaying by remember(storyboard) { mutableStateOf(autoPlay) }
    var voiceEnabled by remember { mutableStateOf(true) }
    var elapsedMs by remember(storyboard) { mutableLongStateOf(0L) }
    var ttsReady by remember { mutableStateOf(false) }

    val tts = remember {
        var instance: TextToSpeech? = null
        instance = TextToSpeech(context.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val bnBd = Locale("bn", "BD")
                val res = instance?.setLanguage(bnBd)
                if (res == TextToSpeech.LANG_MISSING_DATA || res == TextToSpeech.LANG_NOT_SUPPORTED) {
                    instance?.setLanguage(Locale("bn", "IN"))
                }
                instance?.setSpeechRate(0.95f)
                ttsReady = true
            }
        }
        instance
    }

    DisposableEffect(tts) {
        onDispose {
            runCatching { tts.stop() }
            runCatching { tts.shutdown() }
        }
    }

    LaunchedEffect(isPlaying, storyboard) {
        while (isPlaying) {
            delay(50L)
            elapsedMs = (elapsedMs + 50L) % TOTAL_AD_DURATION_MS
        }
    }

    LaunchedEffect(isPlaying, voiceEnabled, ttsReady, storyboard.voiceoverScriptBn) {
        if (ttsReady) {
            if (isPlaying && voiceEnabled && storyboard.voiceoverScriptBn.isNotBlank()) {
                tts.speak(
                    storyboard.voiceoverScriptBn,
                    TextToSpeech.QUEUE_FLUSH,
                    null,
                    "product_ad_${storyboard.hashCode()}"
                )
            } else {
                tts.stop()
            }
        }
    }

    val accentColor = remember(storyboard.accentColorHex) {
        try {
            Color(android.graphics.Color.parseColor(storyboard.accentColorHex))
        } catch (_: Exception) {
            Color(0xFF1B5E20)
        }
    }
    val darkAccent = remember(accentColor) {
        Color(
            red = accentColor.red * 0.42f,
            green = accentColor.green * 0.42f,
            blue = accentColor.blue * 0.42f,
            alpha = 1f
        )
    }

    val sceneIndex = when {
        elapsedMs < 3_800L -> 0
        elapsedMs < 7_600L -> 1
        elapsedMs < 13_000L -> 2
        else -> 3
    }
    val progress = (elapsedMs.toFloat() / TOTAL_AD_DURATION_MS.toFloat()).coerceIn(0f, 1f)
    val pulseTick = ((elapsedMs % 1000L) / 1000f)

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // 9:16 Vertical Ad Theater Viewport
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 390.dp)
                .align(Alignment.CenterHorizontally)
                .aspectRatio(9f / 14f)
                .clip(RoundedCornerShape(22.dp))
                .background(
                    Brush.verticalGradient(
                        listOf(darkAccent, accentColor, Color(0xFF0A2210))
                    )
                )
                .testTag("ai_video_ad_player")
        ) {
            // Top farm & promo bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(50.dp),
                    color = Color.White.copy(alpha = 0.92f)
                ) {
                    Text(
                        "${storyboard.farmName} • ${storyboard.promoBadgeBn}",
                        color = darkAccent,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
                IconButton(
                    onClick = { voiceEnabled = !voiceEnabled },
                    colors = IconButtonDefaults.iconButtonColors(containerColor = Color.Black.copy(alpha = 0.45f)),
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(
                        if (voiceEnabled) Icons.AutoMirrored.Filled.VolumeUp else Icons.AutoMirrored.Filled.VolumeOff,
                        contentDescription = tr("Voiceover", "ভয়েসওভার"),
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Scene Stage
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 52.dp, bottom = 78.dp, start = 14.dp, end = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                when (sceneIndex) {
                    // Scene 1: Hero Product Spotlight + Bengali Hook
                    0 -> {
                        val zoom = 1.0f + (elapsedMs / 3800f) * 0.08f
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(0.86f)
                                    .aspectRatio(1.1f)
                                    .clip(RoundedCornerShape(18.dp))
                                    .border(3.dp, Color.White.copy(alpha = 0.8f), RoundedCornerShape(18.dp))
                                    .background(Color(0xFFE8F5E9))
                            ) {
                                ProductPhotoOrFallback(
                                    imageUri = imageUri,
                                    productName = storyboard.productName,
                                    modifier = Modifier.fillMaxSize().scale(zoom)
                                )
                                if (storyboard.priceText.isNotBlank()) {
                                    Surface(
                                        shape = RoundedCornerShape(50.dp),
                                        color = Color(0xFFFF6F00),
                                        modifier = Modifier
                                            .align(Alignment.BottomEnd)
                                            .padding(8.dp)
                                    ) {
                                        Text(
                                            storyboard.priceText,
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.labelLarge,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                            Text(
                                storyboard.hookHeadlineBn,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                storyboard.productName,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFB9F6CA),
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    // Scene 2: Bengali Quality Points
                    1 -> {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(0.55f)
                                    .aspectRatio(1.4f)
                                    .clip(RoundedCornerShape(14.dp))
                                    .border(2.dp, Color.White, RoundedCornerShape(14.dp))
                            ) {
                                ProductPhotoOrFallback(imageUri, storyboard.productName, Modifier.fillMaxSize())
                            }
                            Text(
                                "কেন আমাদের ${storyboard.productName} সেরা?",
                                color = Color(0xFFFFE082),
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium,
                                textAlign = TextAlign.Center
                            )
                            storyboard.benefitsBn.take(3).forEachIndexed { idx, benefit ->
                                val showDelay = idx * 450L
                                val shown = (elapsedMs - 3800L) >= showDelay
                                AnimatedVisibility(visible = shown, enter = fadeIn(), exit = fadeOut()) {
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = Color.White.copy(alpha = 0.94f),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = accentColor, modifier = Modifier.size(18.dp))
                                            Text(
                                                benefit,
                                                color = Color(0xFF1B1B1B),
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Scene 3: App Screen-Recording Ordering Walkthrough
                    2 -> {
                        val subElapsed = elapsedMs - 7_600L
                        val subStage = when {
                            subElapsed < 1_800L -> 0
                            subElapsed < 3_700L -> 1
                            else -> 2
                        }
                        val stepText = storyboard.orderStepsBn.getOrNull(subStage)
                            ?: storyboard.orderStepsBn.lastOrNull().orEmpty()

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Surface(
                                shape = RoundedCornerShape(50.dp),
                                color = Color.Red.copy(alpha = 0.85f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Box(Modifier.size(8.dp).clip(CircleShape).background(Color.White))
                                    Text(
                                        "অ্যাপ থেকে অর্ডার করার স্ক্রিন রেকর্ড",
                                        color = Color.White,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            // Miniature Animated Phone Screen-Record Frame
                            Surface(
                                shape = RoundedCornerShape(18.dp),
                                color = Color(0xFFF3F6F1),
                                modifier = Modifier
                                    .fillMaxWidth(0.84f)
                                    .weight(1f)
                                    .border(3.dp, Color(0xFF1E293B), RoundedCornerShape(18.dp))
                            ) {
                                Box(modifier = Modifier.fillMaxSize()) {
                                    Column(modifier = Modifier.fillMaxSize()) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(accentColor)
                                                .padding(horizontal = 10.dp, vertical = 6.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text("Smart Farm Shop", color = Color.White, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                            Text(
                                                if (subStage == 0) "বাজার" else if (subStage == 1) "চেকআউট" else "সম্পন্ন ✓",
                                                color = Color(0xFFFFE082),
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }

                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .padding(10.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            when (subStage) {
                                                0 -> {
                                                    Column(
                                                        verticalArrangement = Arrangement.spacedBy(8.dp),
                                                        horizontalAlignment = Alignment.CenterHorizontally
                                                    ) {
                                                        Surface(shape = RoundedCornerShape(10.dp), color = Color.White, tonalElevation = 2.dp) {
                                                            Row(
                                                                modifier = Modifier.fillMaxWidth().padding(8.dp),
                                                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                                verticalAlignment = Alignment.CenterVertically
                                                            ) {
                                                                Box(Modifier.size(46.dp).clip(RoundedCornerShape(8.dp))) {
                                                                    ProductPhotoOrFallback(imageUri, storyboard.productName, Modifier.fillMaxSize())
                                                                }
                                                                Column(Modifier.weight(1f)) {
                                                                    Text(storyboard.productName, fontWeight = FontWeight.Bold, color = Color(0xFF111827), fontSize = 12.sp, maxLines = 1)
                                                                    Text(storyboard.priceText.ifBlank { "তাজা পণ্য" }, color = accentColor, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                                                }
                                                            }
                                                        }
                                                        Box(contentAlignment = Alignment.Center) {
                                                            Surface(
                                                                shape = RoundedCornerShape(50.dp),
                                                                color = accentColor,
                                                                modifier = Modifier.border(2.dp, Color(0xFFFFEB3B), RoundedCornerShape(50.dp))
                                                            ) {
                                                                Text(
                                                                    "এখানে ক্লিক করুন • কার্টে যোগ করুন",
                                                                    color = Color.White,
                                                                    fontWeight = FontWeight.Bold,
                                                                    fontSize = 11.sp,
                                                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                                                                )
                                                            }
                                                            TapCursorOverlay(pulse = pulseTick)
                                                        }
                                                    }
                                                }
                                                1 -> {
                                                    Column(
                                                        verticalArrangement = Arrangement.spacedBy(8.dp),
                                                        horizontalAlignment = Alignment.CenterHorizontally
                                                    ) {
                                                        Surface(shape = RoundedCornerShape(10.dp), color = Color.White, modifier = Modifier.fillMaxWidth()) {
                                                            Column(Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                                                Text("ঠিকানা: আপনার বাসা / অফিস", fontSize = 11.sp, color = Color(0xFF374151))
                                                                Text("পেমেন্ট: বিকাশ / নগদ / ক্যাশ", fontSize = 11.sp, color = Color(0xFF374151), fontWeight = FontWeight.SemiBold)
                                                            }
                                                        }
                                                        Box(contentAlignment = Alignment.Center) {
                                                            Surface(
                                                                shape = RoundedCornerShape(50.dp),
                                                                color = Color(0xFFD32F2F),
                                                                modifier = Modifier.border(2.dp, Color(0xFFFFD54F), RoundedCornerShape(50.dp))
                                                            ) {
                                                                Text(
                                                                    "এখনই অর্ডার করুন 👆",
                                                                    color = Color.White,
                                                                    fontWeight = FontWeight.Bold,
                                                                    fontSize = 12.sp,
                                                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 9.dp)
                                                                )
                                                            }
                                                            TapCursorOverlay(pulse = pulseTick)
                                                        }
                                                    }
                                                }
                                                else -> {
                                                    Surface(shape = RoundedCornerShape(12.dp), color = Color(0xFFE8F5E9), modifier = Modifier.fillMaxWidth()) {
                                                        Column(
                                                            modifier = Modifier.padding(12.dp),
                                                            horizontalAlignment = Alignment.CenterHorizontally,
                                                            verticalArrangement = Arrangement.spacedBy(4.dp)
                                                        ) {
                                                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF1B5E20), modifier = Modifier.size(34.dp))
                                                            Text(
                                                                "অর্ডার সম্পন্ন!",
                                                                color = Color(0xFF1B5E20),
                                                                fontWeight = FontWeight.ExtraBold,
                                                                fontSize = 14.sp
                                                            )
                                                            Text(
                                                                "সরাসরি ${storyboard.farmName}-এ অর্ডার নোটিফিকেশন পৌঁছে গেছে!",
                                                                color = Color(0xFF2E7D32),
                                                                fontSize = 11.sp,
                                                                textAlign = TextAlign.Center
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color.White.copy(alpha = 0.94f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    stepText,
                                    color = Color(0xFF111827),
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
                                )
                            }
                        }
                    }

                    // Scene 4: Grand Closing — How to Download the App + How to Order Online
                    else -> {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                        ) {
                            Surface(
                                shape = RoundedCornerShape(50.dp),
                                color = Color(0xFFFFD54F)
                            ) {
                                Text(
                                    "📲 অ্যাপ ডাউনলোড ও অনলাইনে অর্ডার নিয়ম",
                                    color = Color(0xFF1B1B1B),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                                )
                            }

                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = Color.White.copy(alpha = 0.96f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(10.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(Icons.Default.DownloadForOffline, contentDescription = null, tint = accentColor, modifier = Modifier.size(18.dp))
                                        Text(
                                            "১. কিভাবে অ্যাপ ডাউনলোড করবেন:",
                                            color = accentColor,
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.ExtraBold
                                        )
                                    }
                                    Text(
                                        "Google Play Store বা শেয়ার লিংক থেকে 'Smart Farm' অ্যাপ ডাউনলোড ও ইনস্টল করুন এবং ফ্রি কাস্টমার অ্যাকাউন্ট খুলুন।",
                                        color = Color(0xFF1B1B1B),
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = Color(0xFFE8F5E9),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(10.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(Icons.Default.ShoppingCartCheckout, contentDescription = null, tint = Color(0xFF1B5E20), modifier = Modifier.size(18.dp))
                                        Text(
                                            "২. কিভাবে অনলাইনে অর্ডার করবেন:",
                                            color = Color(0xFF1B5E20),
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.ExtraBold
                                        )
                                    }
                                    Text(
                                        storyboard.downloadAndOrderOutroBn,
                                        color = Color(0xFF111827),
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }

                            Text(
                                "${storyboard.farmName} • ${storyboard.productName} • ${storyboard.priceText}",
                                color = Color(0xFFB9F6CA),
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.labelMedium,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }

            // Bottom CTA Pill & Scrubber
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(50.dp),
                    color = Color(0xFFD32F2F),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(2.dp, Color(0xFFFFD54F), RoundedCornerShape(50.dp))
                        .then(
                            if (onOrderNowClick != null) Modifier.clickable { onOrderNowClick() }
                            else Modifier
                        )
                ) {
                    Text(
                        storyboard.callToActionBn,
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold,
                        style = MaterialTheme.typography.labelLarge,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                    )
                }
                LinearProgressIndicator(
                    progress = { progress },
                    color = Color(0xFFFFD54F),
                    trackColor = Color.White.copy(alpha = 0.25f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                )
            }
        }

        // Player Transport & Scene Selector Controls
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilledTonalIconButton(
                onClick = { isPlaying = !isPlaying },
                modifier = Modifier.testTag("ad_player_play_pause")
            ) {
                Icon(
                    if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) tr("Pause", "থামান") else tr("Play", "চালান")
                )
            }
            FilledTonalIconButton(
                onClick = {
                    elapsedMs = 0L
                    isPlaying = true
                    if (ttsReady && voiceEnabled && storyboard.voiceoverScriptBn.isNotBlank()) {
                        tts.speak(
                            storyboard.voiceoverScriptBn,
                            TextToSpeech.QUEUE_FLUSH,
                            null,
                            "product_ad_replay_${System.currentTimeMillis()}"
                        )
                    }
                }
            ) {
                Icon(Icons.Default.Replay, contentDescription = tr("Replay", "পুনরায় চালান"))
            }

            val scenes = listOf(
                0L to "১. পরিচিতি",
                3_800L to "২. সেরা মান",
                7_600L to "৩. অর্ডার ডেমো",
                13_000L to "৪. ডাউনলোড"
            )
            scenes.forEachIndexed { idx, (jumpMs, label) ->
                FilterChip(
                    selected = sceneIndex == idx,
                    onClick = { elapsedMs = jumpMs },
                    label = { Text(label, maxLines = 1, style = MaterialTheme.typography.labelSmall) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun TapCursorOverlay(pulse: Float) {
    Box(modifier = Modifier.size(48.dp), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                color = Color(0xFFFFEB3B).copy(alpha = (1f - pulse).coerceIn(0.15f, 0.9f)),
                radius = (size.minDimension * 0.25f) + pulse * (size.minDimension * 0.22f),
                style = Stroke(width = 3.dp.toPx())
            )
            drawCircle(
                color = Color(0xFFFFEB3B),
                radius = 5.dp.toPx(),
                center = Offset(size.width / 2f, size.height / 2f)
            )
        }
    }
}

@Composable
private fun ProductPhotoOrFallback(
    imageUri: String?,
    productName: String,
    modifier: Modifier = Modifier
) {
    if (!imageUri.isNullOrBlank()) {
        AsyncImage(
            model = imageUri,
            contentDescription = productName,
            contentScale = ContentScale.Crop,
            modifier = modifier
        )
    } else {
        Box(
            modifier = modifier.background(Color(0xFFE8F5E9)),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Icon(Icons.Default.Eco, contentDescription = null, tint = Color(0xFF1B5E20), modifier = Modifier.size(36.dp))
                Text(productName, color = Color(0xFF1B5E20), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium, textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 6.dp))
            }
        }
    }
}
