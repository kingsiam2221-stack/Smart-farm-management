package com.example.ui.screens.farmer

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.FactCheck
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.guide.*
import com.example.data.local.SubscriptionAccessPolicy
import com.example.data.local.SubscriptionFeature
import com.example.i18n.isBangla
import com.example.i18n.tr
import com.example.ui.components.FeaturePlanBadge
import com.example.ui.components.SubscriptionRequiredLockCard

@Composable
fun FarmGuideScreen(
    onOpenAiAdvisor: (() -> Unit)? = null,
    subscriptionPolicy: SubscriptionAccessPolicy = SubscriptionAccessPolicy(),
    canSubscribe: Boolean = true,
    onOpenSubscription: () -> Unit = {}
) {
    val isBn = isBangla()
    var selectedTopic by remember { mutableStateOf(GuideTopic.SUMMARY) }
    var selectedSpecies by remember { mutableStateOf(GuideSpecies.ALL) }
    var searchQuery by remember { mutableStateOf("") }

    fun topicIcon(topic: GuideTopic): ImageVector = when (topic) {
        GuideTopic.SUMMARY -> Icons.Default.Summarize
        GuideTopic.HATCH_DUCK_EGGS -> Icons.Default.Egg
        GuideTopic.RAISE_DUCKS -> Icons.Default.Water
        GuideTopic.RAISE_CHICKENS -> Icons.Default.CrueltyFree
        GuideTopic.VACCINE_SCHEDULE -> Icons.Default.Vaccines
        GuideTopic.MEDICINE_SCHEDULE -> Icons.Default.Medication
    }

    val speciesLabel: (GuideSpecies) -> String = { sp ->
        when (sp) {
            GuideSpecies.ALL -> if (isBn) "সব (হাঁস ও মুরগি)" else "All (Duck & Chicken)"
            GuideSpecies.DUCK -> if (isBn) "হাঁস" else "Duck"
            GuideSpecies.CHICKEN -> if (isBn) "মুরগি" else "Chicken"
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("farm_guide_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Atmospheric Hero Header
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.colorScheme.secondary
                            )
                        )
                    )
                    .padding(18.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color.White.copy(alpha = 0.2f),
                                modifier = Modifier.size(44.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.AutoMirrored.Filled.MenuBook,
                                        contentDescription = tr("Poultry & Duck Farming Guide", "হাঁস ও মুরগি পালন গাইড"),
                                        tint = Color.White
                                    )
                                }
                            }
                            Column {
                                Text(
                                    tr("Poultry & Duck Farming Guide", "হাঁস-মুরগি পালন ও চিকিৎসা গাইড"),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    tr(
                                        "Hatching, brooding, feed, vaccines & medicine schedule",
                                        "ডিম ফুটানো, পালন পদ্ধতি, টিকা ও ওষুধের সম্পূর্ণ সময়সূচি"
                                    ),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.88f)
                                )
                            }
                        }
                        FeaturePlanBadge(
                            isFree = subscriptionPolicy.isFree(SubscriptionFeature.FARM_GUIDE),
                            hasActiveSub = subscriptionPolicy.hasActiveSubscription
                        )
                    }

                    if (onOpenAiAdvisor != null && subscriptionPolicy.canAccess(SubscriptionFeature.FARM_GUIDE)) {
                        Surface(
                            onClick = onOpenAiAdvisor,
                            shape = RoundedCornerShape(50.dp),
                            color = Color.White.copy(alpha = 0.18f),
                            modifier = Modifier.testTag("guide_ask_ai_button")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    Icons.Default.AutoAwesome,
                                    contentDescription = tr("Ask AI Vet Advisor", "এআই পরামর্শ নিন"),
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    if (subscriptionPolicy.canAccess(SubscriptionFeature.DASHBOARD_CHARTS_AI)) {
                                        tr("Need custom dose or symptom help? Ask AI Advisor", "নির্দিষ্ট মাত্রা বা রোগের লক্ষণ জানতে এআইকে জিজ্ঞাসা করুন")
                                    } else {
                                        tr("AI Vet Advisor requires subscription • Unlock", "এআই পরামর্শ দেখতে সাবস্ক্রিপশন নিন • আনলক")
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }
        }

        if (!subscriptionPolicy.canAccess(SubscriptionFeature.FARM_GUIDE)) {
            item(key = "guide_subscription_lock") {
                SubscriptionRequiredLockCard(
                    feature = SubscriptionFeature.FARM_GUIDE,
                    onOpenSubscription = onOpenSubscription,
                    canSubscribe = canSubscribe
                )
            }
            return@LazyColumn
        }

        // Topic Filter Chips + Search
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(GuideTopic.entries) { topic ->
                        val label = FarmGuideContent.topicLabels[topic]?.get(isBn) ?: topic.name
                        FilterChip(
                            selected = selectedTopic == topic,
                            onClick = { selectedTopic = topic },
                            leadingIcon = {
                                Icon(
                                    topicIcon(topic),
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            label = { Text(label, fontWeight = if (selectedTopic == topic) FontWeight.Bold else FontWeight.Medium) },
                            modifier = Modifier.testTag("guide_topic_${topic.name.lowercase()}")
                        )
                    }
                }

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = {
                        Text(
                            tr(
                                "Search guide (e.g., humidity, Ranikhet, Day 15, calcium...)",
                                "গাইডে খুঁজুন (যেমন: আর্দ্রতা, রানীক্ষেত, ডাক প্লেগ, ক্যালসিয়াম...)"
                            )
                        )
                    },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = tr("Clear search", "অনুসন্ধান মুছুন"))
                            }
                        }
                    },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("guide_search_input")
                )

                if (selectedTopic == GuideTopic.VACCINE_SCHEDULE || selectedTopic == GuideTopic.MEDICINE_SCHEDULE) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(GuideSpecies.entries) { sp ->
                            FilterChip(
                                selected = selectedSpecies == sp,
                                onClick = { selectedSpecies = sp },
                                label = { Text(speciesLabel(sp)) },
                                modifier = Modifier.testTag("guide_species_${sp.name.lowercase()}")
                            )
                        }
                    }
                }
            }
        }

        // Main Content Area
        when (selectedTopic) {
            GuideTopic.SUMMARY -> {
                item {
                    QuickReferenceComparisonCard(isBn = isBn)
                }
                val filteredSummary = FarmGuideContent.summaryCards.filter { card ->
                    searchQuery.isBlank() ||
                        card.title.get(isBn).contains(searchQuery, ignoreCase = true) ||
                        card.highlightMetric.get(isBn).contains(searchQuery, ignoreCase = true) ||
                        card.summaryPoints.any { it.get(isBn).contains(searchQuery, ignoreCase = true) }
                }
                items(filteredSummary, key = { it.topic.name }) { card ->
                    SummaryTopicCard(
                        card = card,
                        icon = topicIcon(card.topic),
                        isBn = isBn,
                        onOpenTopic = { selectedTopic = card.topic }
                    )
                }
            }

            GuideTopic.HATCH_DUCK_EGGS -> {
                item {
                    IncubationQuickSpecBar(isBn = isBn)
                }
                val steps = FarmGuideContent.duckHatchingSteps.filter {
                    searchQuery.isBlank() ||
                        it.title.get(isBn).contains(searchQuery, ignoreCase = true) ||
                        it.details.get(isBn).contains(searchQuery, ignoreCase = true)
                }
                items(steps, key = { "hatch_${it.stepNumber}" }) { step ->
                    GuideStepCard(step = step, isBn = isBn)
                }
            }

            GuideTopic.RAISE_DUCKS -> {
                val steps = FarmGuideContent.raiseDucksSteps.filter {
                    searchQuery.isBlank() ||
                        it.title.get(isBn).contains(searchQuery, ignoreCase = true) ||
                        it.details.get(isBn).contains(searchQuery, ignoreCase = true)
                }
                items(steps, key = { "duck_${it.stepNumber}" }) { step ->
                    GuideStepCard(step = step, isBn = isBn)
                }
            }

            GuideTopic.RAISE_CHICKENS -> {
                val steps = FarmGuideContent.raiseChickensSteps.filter {
                    searchQuery.isBlank() ||
                        it.title.get(isBn).contains(searchQuery, ignoreCase = true) ||
                        it.details.get(isBn).contains(searchQuery, ignoreCase = true)
                }
                items(steps, key = { "chicken_${it.stepNumber}" }) { step ->
                    GuideStepCard(step = step, isBn = isBn)
                }
            }

            GuideTopic.VACCINE_SCHEDULE -> {
                item {
                    ColdChainSafetyBanner(isBn = isBn)
                }
                val filteredVaccines = FarmGuideContent.vaccineSchedules.filter { entry ->
                    val matchesSpecies = selectedSpecies == GuideSpecies.ALL || entry.species == selectedSpecies
                    val matchesQuery = searchQuery.isBlank() ||
                        entry.vaccineName.get(isBn).contains(searchQuery, ignoreCase = true) ||
                        entry.targetDisease.get(isBn).contains(searchQuery, ignoreCase = true) ||
                        entry.ageTiming.get(isBn).contains(searchQuery, ignoreCase = true)
                    matchesSpecies && matchesQuery
                }
                items(filteredVaccines.size) { index ->
                    VaccineScheduleCard(entry = filteredVaccines[index], isBn = isBn)
                }
            }

            GuideTopic.MEDICINE_SCHEDULE -> {
                item {
                    MedicineSafetyBanner(isBn = isBn)
                }
                val filteredMeds = FarmGuideContent.medicineSchedules.filter { entry ->
                    val matchesSpecies = selectedSpecies == GuideSpecies.ALL ||
                        entry.species == GuideSpecies.ALL ||
                        entry.species == selectedSpecies
                    val matchesQuery = searchQuery.isBlank() ||
                        entry.medicineOrSupplement.get(isBn).contains(searchQuery, ignoreCase = true) ||
                        entry.timingOrCondition.get(isBn).contains(searchQuery, ignoreCase = true) ||
                        entry.purpose.get(isBn).contains(searchQuery, ignoreCase = true)
                    matchesSpecies && matchesQuery
                }
                items(filteredMeds.size) { index ->
                    MedicineScheduleCard(entry = filteredMeds[index], isBn = isBn)
                }
            }
        }
    }
}

@Composable
private fun QuickReferenceComparisonCard(isBn: Boolean) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("guide_summary_overview_card")
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.AutoMirrored.Filled.FactCheck, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text(
                    if (isBn) "এক নজরে হাঁস ও মুরগি পালনের সারাংশ" else "At-a-Glance Summary Table",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MiniCompareColumn(
                    title = if (isBn) "হাঁস" else "Duck",
                    rows = listOf(
                        if (isBn) "ডিম ফোটা: ২৮ দিন" else "Hatch: 28 days",
                        if (isBn) "ব্রুডিং: ৩২°->২৫°সে" else "Brood: 32°->25°C",
                        if (isBn) "মূল টিকা: ১৫ ও ৪২ দিন" else "Vaccines: D15 & D42"
                    ),
                    modifier = Modifier.weight(1f)
                )
                MiniCompareColumn(
                    title = if (isBn) "মুরগি" else "Chicken",
                    rows = listOf(
                        if (isBn) "ডিম ফোটা: ২১ দিন" else "Hatch: 21 days",
                        if (isBn) "ব্রুডিং: ৩৪°->২৩°সে" else "Brood: 34°->23°C",
                        if (isBn) "মূল টিকা: ৫, ১২, ২১ দিন" else "Vaccines: D5, 12, 21"
                    ),
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun MiniCompareColumn(title: String, rows: List<String>, modifier: Modifier = Modifier) {
    Surface(
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surface,
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            rows.forEach { r ->
                Text("• $r", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
            }
        }
    }
}

@Composable
private fun SummaryTopicCard(
    card: GuideSummaryCardData,
    icon: ImageVector,
    isBn: Boolean,
    onOpenTopic: () -> Unit
) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpenTopic() }
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                    }
                    Column {
                        Text(card.title.get(isBn), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(
                            card.highlightMetric.get(isBn),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
                IconButton(onClick = onOpenTopic) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = if (isBn) "বিস্তারিত দেখুন" else "Open full section",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            HorizontalDivider()

            card.summaryPoints.forEach { point ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Top) {
                    Text("•", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    Text(
                        point.get(isBn),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                TextButton(onClick = onOpenTopic) {
                    Text(if (isBn) "বিস্তারিত গাইড দেখুন" else "View Detailed Guide")
                    Spacer(Modifier.width(4.dp))
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

@Composable
private fun IncubationQuickSpecBar(isBn: Boolean) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.7f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                if (isBn) "২৮ দিনের হাঁসের ডিম ফুটানোর মূল তাপমাত্রা ও আর্দ্রতা" else "28-Day Duck Incubation Telemetry Cheat Sheet",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onTertiaryContainer
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SpecMiniTile(
                    label = if (isBn) "১–২৫ দিন" else "Days 1–25",
                    value = "37.5°C\n55–60% RH",
                    modifier = Modifier.weight(1f)
                )
                SpecMiniTile(
                    label = if (isBn) "১০–২৫ দিন" else "Days 10–25",
                    value = if (isBn) "১৫ মি. ঠান্ডা\nপানি স্প্রে" else "15m Cool\nWarm Mist",
                    modifier = Modifier.weight(1f)
                )
                SpecMiniTile(
                    label = if (isBn) "২৬–২৮ দিন" else "Days 26–28",
                    value = "37.0°C\n75–80% RH",
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun SpecMiniTile(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surface,
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(2.dp))
            Text(value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun GuideStepCard(step: GuideStepItem, isBn: Boolean) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Text("${step.stepNumber}", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = RoundedCornerShape(50.dp)
                    ) {
                        Text(
                            step.stageBadge.get(isBn),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(step.title.get(isBn), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
            }

            Text(
                step.details.get(isBn),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (step.keySpec != null) {
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Default.TipsAndUpdates, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                        Text(
                            step.keySpec.get(isBn),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ColdChainSafetyBanner(isBn: Boolean) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(Icons.Default.AcUnit, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    if (isBn) "টিকা দেওয়ার জরুরি নিয়ম (কোল্ড চেইন)" else "Essential Vaccine Rules (Cold Chain)",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    if (isBn)
                        "ভ্যাকসিন ২–৮°সে তাপমাত্রায় রাখুন। অসুস্থ পাখিকে টিকা দেবেন না। পানির ভ্যাকসিনের আগে ও পরে ২৪ ঘণ্টা কোনো স্যানিটাইজার বা অ্যান্টিবায়োটিক দেবেন না।"
                    else
                        "Store vaccines at 2–8°C away from sunlight. Never vaccinate sick birds. Avoid sanitizers or antibiotics 24h before and after live water vaccines.",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
private fun VaccineScheduleCard(entry: VaccineScheduleEntry, isBn: Boolean) {
    val spBadge = if (entry.species == GuideSpecies.DUCK) {
        if (isBn) "হাঁস" else "Duck"
    } else {
        if (isBn) "মুরগি" else "Chicken"
    }
    val spContainer = if (entry.species == GuideSpecies.DUCK) {
        MaterialTheme.colorScheme.secondaryContainer
    } else {
        MaterialTheme.colorScheme.primaryContainer
    }

    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(color = MaterialTheme.colorScheme.tertiaryContainer, shape = RoundedCornerShape(50.dp)) {
                    Text(
                        entry.ageTiming.get(isBn),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
                Surface(color = spContainer, shape = RoundedCornerShape(50.dp)) {
                    Text(
                        spBadge,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }
            Text(entry.vaccineName.get(isBn), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
                "${if (isBn) "রোগ" else "Target"}: ${entry.targetDisease.get(isBn)}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                "${if (isBn) "প্রয়োগ পদ্ধতি" else "Route"}: ${entry.route.get(isBn)}",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium
            )
            Text(
                entry.doseAndNotes.get(isBn),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun MedicineSafetyBanner(isBn: Boolean) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.65f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(Icons.Default.HealthAndSafety, contentDescription = null, tint = MaterialTheme.colorScheme.error)
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    if (isBn) "অ্যান্টিবায়োটিক ও ওষুধ সতর্কতা" else "Veterinary Dosage & Withdrawal Warning",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error
                )
                Text(
                    if (isBn)
                        "বিনা প্রয়োজনে প্রতিদিন অ্যান্টিবায়োটিক খাওয়াবেন না। যেকোনো রোগ বা অ্যান্টিবায়োটিকের সঠিক মাত্রার জন্য স্থানীয় প্রাণীসম্পদ চিকিৎসকের পরামর্শ নিন এবং ওষুধ শেষের বিরতিকাল (Withdrawal Period) মেনে ডিম/মাংস বিক্রি করুন।"
                    else
                        "Never use antibiotics routinely without symptoms. Always verify body-weight dosage with a qualified veterinarian and follow the label withdrawal period before selling eggs or meat.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
private fun MedicineScheduleCard(entry: MedicineScheduleEntry, isBn: Boolean) {
    val spBadge = when (entry.species) {
        GuideSpecies.ALL -> if (isBn) "হাঁস ও মুরগি" else "Duck & Chicken"
        GuideSpecies.DUCK -> if (isBn) "হাঁস" else "Duck"
        GuideSpecies.CHICKEN -> if (isBn) "মুরগি" else "Chicken"
    }

    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = RoundedCornerShape(50.dp)) {
                    Text(
                        entry.timingOrCondition.get(isBn),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (entry.requiresVetCheck) {
                        Surface(color = MaterialTheme.colorScheme.errorContainer, shape = RoundedCornerShape(50.dp)) {
                            Text(
                                if (isBn) "পশুচিকিৎসক" else "Vet Guided",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.error,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                    Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = RoundedCornerShape(50.dp)) {
                        Text(
                            spBadge,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
            Text(entry.medicineOrSupplement.get(isBn), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
                "${if (isBn) "কাজ" else "Purpose"}: ${entry.purpose.get(isBn)}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                entry.administrationAndSafety.get(isBn),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
