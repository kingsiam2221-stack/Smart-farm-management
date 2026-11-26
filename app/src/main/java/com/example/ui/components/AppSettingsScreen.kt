package com.example.ui.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.local.*
import com.example.i18n.AppLanguage
import com.example.i18n.AppStrings
import com.example.i18n.CurrencyOption
import com.example.i18n.DomainLocalization
import com.example.i18n.isBangla
import com.example.i18n.tr
import com.example.util.DeveloperContactHelper

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AppSettingsScreen(
    user: UserEntity,
    systemSettings: SystemSettingsEntity,
    strings: AppStrings,
    currentLanguage: AppLanguage,
    currentCurrency: CurrencyOption,
    darkModeOverride: Boolean?,
    canEditAppearance: Boolean = true,
    showLabelCustomization: Boolean = false,
    languageOverrides: Map<String, String> = emptyMap(),
    supportRequests: List<SupportRequestEntity> = emptyList(),
    notifications: List<AppNotificationEntity>? = null,
    employeeEmptyPermissionsHint: Boolean = false,
    onSelectLanguage: (AppLanguage) -> Unit,
    onSelectCurrency: (CurrencyOption) -> Unit,
    onSelectDarkMode: (Boolean?) -> Unit,
    onSetLanguageOverride: (String, String) -> Unit = { _, _ -> },
    onUpdateProfile: ((String, String, String?) -> Unit)? = null,
    onSubmitSupport: ((String, String) -> Unit)? = null,
    onSaveDeveloperSettings: ((SystemSettingsEntity) -> Unit)? = null,
    onMarkNotificationsRead: (() -> Unit)? = null,
    onMessage: (String) -> Unit = {},
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val isBn = isBangla()

    var showProfileEditor by remember(user.id) { mutableStateOf(false) }
    var name by remember(user.id, user.name) { mutableStateOf(user.name) }
    var mobile by remember(user.id, user.mobile) { mutableStateOf(user.mobile) }
    var imageUri by remember(user.id, user.profileImageUri) { mutableStateOf(user.profileImageUri) }

    var showCurrencyDialog by remember { mutableStateOf(false) }
    var showLabelEditor by remember { mutableStateOf(false) }
    var showSupportDialog by remember { mutableStateOf(false) }
    var showEditDevContactDialog by remember { mutableStateOf(false) }
    var showFaqSection by remember { mutableStateOf(false) }
    var showLogoutConfirm by remember { mutableStateOf(false) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri: Uri? ->
        if (uri != null) imageUri = uri.toString()
    }

    val devPhone = systemSettings.developerPhone.trim()
    val devWhatsApp = systemSettings.developerWhatsApp.ifBlank { systemSettings.developerPhone }.trim()
    val devEmail = systemSettings.developerEmail.trim()

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 720.dp),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. Dedicated Settings Hero Header
            item(key = "settings_header") {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(48.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.Settings,
                                    contentDescription = strings.settings,
                                    tint = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                strings.settings,
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                tr(
                                    "Profile details, language, currency, theme, developer support & logout",
                                    "প্রোফাইল তথ্য, ভাষা, মুদ্রা, থিম, ডেভেলপার সাপোর্ট ও লগ আউট"
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                            )
                        }
                    }
                }
            }

            if (employeeEmptyPermissionsHint) {
                item(key = "settings_emp_hint") {
                    Surface(
                        shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.tertiaryContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.LockPerson, contentDescription = null, tint = MaterialTheme.colorScheme.onTertiaryContainer)
                            Text(
                                tr(
                                    "Your employee account does not have farm module permissions enabled yet. Ask your Farm Admin to grant module access.",
                                    "আপনার কর্মচারী অ্যাকাউন্টে এখনও খামারের মডিউল অনুমতি চালু করা হয়নি। খামারের অ্যাডমিনকে অনুমতি চালু করতে বলুন।"
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        }
                    }
                }
            }

            // 2. Profile & Profile Details Card
            item(key = "settings_profile_card") {
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 2.dp,
                    modifier = Modifier.fillMaxWidth().testTag("settings_profile_card")
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            if (!imageUri.isNullOrBlank()) {
                                AsyncImage(
                                    model = imageUri,
                                    contentDescription = user.name,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.size(64.dp).clip(CircleShape)
                                )
                            } else {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    modifier = Modifier.size(64.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            user.name.take(1).uppercase().ifBlank { "U" },
                                            style = MaterialTheme.typography.headlineSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    user.name,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    user.email,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (user.mobile.isNotBlank()) {
                                    Text(
                                        "${tr("Mobile", "মোবাইল")}: ${user.mobile}",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                            if (onUpdateProfile != null) {
                                FilledTonalIconButton(
                                    onClick = { showProfileEditor = !showProfileEditor },
                                    modifier = Modifier.minimumInteractiveComponentSize().testTag("settings_toggle_profile_editor")
                                ) {
                                    Icon(
                                        if (showProfileEditor) Icons.Default.ExpandLess else Icons.Default.Edit,
                                        contentDescription = tr("Edit Profile Details", "প্রোফাইল তথ্য সম্পাদনা")
                                    )
                                }
                            }
                        }

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            AssistChip(
                                onClick = {},
                                leadingIcon = { Icon(Icons.Default.Badge, contentDescription = null, modifier = Modifier.size(16.dp)) },
                                label = { Text("${tr("Role", "ভূমিকা")}: ${DomainLocalization.userRole(user.role, isBn)}") }
                            )
                            AssistChip(
                                onClick = {},
                                leadingIcon = { Icon(Icons.Default.VerifiedUser, contentDescription = null, modifier = Modifier.size(16.dp)) },
                                label = { Text("${tr("Status", "অবস্থা")}: ${DomainLocalization.accountStatus(user.status, isBn)}") }
                            )
                            AssistChip(
                                onClick = {},
                                leadingIcon = { Icon(Icons.Default.CalendarToday, contentDescription = null, modifier = Modifier.size(16.dp)) },
                                label = { Text("${tr("Joined", "যোগদান")}: ${formatShortDate(user.createdAt)}") }
                            )
                        }

                        if (onUpdateProfile != null) {
                            AnimatedVisibility(visible = showProfileEditor) {
                                Column(
                                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    HorizontalDivider()
                                    Text(
                                        tr("Profile Details", "প্রোফাইল বিস্তারিত"),
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    OutlinedButton(
                                        onClick = { safeLaunchImagePicker(picker) },
                                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                                    ) {
                                        Icon(Icons.Default.PhotoCamera, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.width(8.dp))
                                        Text(tr("Upload / Change Profile Photo", "প্রোফাইল ছবি পরিবর্তন করুন"))
                                    }
                                    OutlinedTextField(
                                        value = name,
                                        onValueChange = { name = it },
                                        label = { Text(tr("Full Name", "পূর্ণ নাম")) },
                                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth().testTag("settings_profile_name_input")
                                    )
                                    OutlinedTextField(
                                        value = mobile,
                                        onValueChange = { mobile = it },
                                        label = { Text(tr("Mobile Number", "মোবাইল নম্বর")) },
                                        leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth().testTag("settings_profile_mobile_input")
                                    )
                                    Button(
                                        onClick = {
                                            onUpdateProfile(name, mobile, imageUri)
                                            showProfileEditor = false
                                        },
                                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("settings_save_profile_button")
                                    ) {
                                        Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.width(8.dp))
                                        Text(tr("Save Profile Details", "প্রোফাইল তথ্য সংরক্ষণ করুন"), fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 3. Language, Currency & Appearance Preferences Card (Consolidated)
            if (canEditAppearance) {
                item(key = "settings_preferences_card") {
                    Surface(
                        shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 2.dp,
                        modifier = Modifier.fillMaxWidth().testTag("settings_preferences_card")
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Default.Tune, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Text(
                                    tr("Language, Currency & Appearance", "ভাষা, মুদ্রা ও থিম সেটিংস"),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            // Language Picker
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(strings.language, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    AppLanguage.entries.forEach { lang ->
                                        FilterChip(
                                            selected = currentLanguage == lang,
                                            onClick = { onSelectLanguage(lang) },
                                            leadingIcon = {
                                                Icon(
                                                    if (currentLanguage == lang) Icons.Default.CheckCircle else Icons.Default.Translate,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            },
                                            label = { Text("${lang.nativeName} (${lang.displayName})", maxLines = 1) },
                                            modifier = Modifier
                                                .weight(1f)
                                                .testTag(if (lang == AppLanguage.BN) "settings_lang_bn" else "settings_lang_en")
                                        )
                                    }
                                }
                            }

                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                            // Currency & Country Selector
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(strings.currency, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                FilledTonalButton(
                                    onClick = { showCurrencyDialog = true },
                                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("settings_currency_button")
                                ) {
                                    Icon(Icons.Default.CurrencyExchange, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        "${currentCurrency.symbol} ${currentCurrency.code} — ${currentCurrency.countryName}",
                                        modifier = Modifier.weight(1f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Icon(Icons.Default.ChevronRight, contentDescription = null)
                                }
                            }

                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                            // Theme Mode
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(strings.themeMode, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                ThemeModeSelectorRow(
                                    darkModeOverride = darkModeOverride,
                                    onSelectDarkMode = onSelectDarkMode
                                )
                            }

                            if (showLabelCustomization) {
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                OutlinedButton(
                                    onClick = { showLabelEditor = true },
                                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                                ) {
                                    Icon(Icons.Default.EditNote, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text(tr("Customize App Labels / Terminology", "অ্যাপের নাম ও ট্যাব শিরোনাম পরিবর্তন"))
                                }
                            }
                        }
                    }
                }
            }

            // 4. Help, Support & Contact App Developer / Company
            item(key = "settings_help_support_card") {
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 2.dp,
                    modifier = Modifier.fillMaxWidth().testTag("settings_developer_contact_card")
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Default.SupportAgent, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Text(
                                    strings.helpAndSupport,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            if (onSaveDeveloperSettings != null) {
                                TextButton(
                                    onClick = { showEditDevContactDialog = true },
                                    modifier = Modifier.testTag("settings_edit_dev_contact_button")
                                ) {
                                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text(tr("Edit Contact", "নম্বর পরিবর্তন"))
                                }
                            }
                        }

                        // Developer / Company Support Card
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    systemSettings.developerName.ifBlank { tr("App Developer & Support Team", "অ্যাপ ডেভেলপার ও সাপোর্ট টিম") },
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                                if (systemSettings.developerSupportNote.isNotBlank()) {
                                    Text(
                                        systemSettings.developerSupportNote,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.9f)
                                    )
                                }

                                if (devPhone.isNotBlank()) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Icon(Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                                            Text(
                                                "${tr("Mobile", "মোবাইল")}: $devPhone",
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                        IconButton(
                                            onClick = {
                                                clipboard.setText(AnnotatedString(devPhone))
                                                onMessage(if (isBn) "নম্বর কপি হয়েছে: $devPhone" else "Copied: $devPhone")
                                            }
                                        ) {
                                            Icon(Icons.Default.ContentCopy, contentDescription = tr("Copy Number", "নম্বর কপি করুন"), modifier = Modifier.size(18.dp))
                                        }
                                    }
                                }

                                if (devWhatsApp.isNotBlank()) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color(0xFF1B8E3E))
                                        Text(
                                            "WhatsApp: $devWhatsApp",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Button(
                                        enabled = devPhone.isNotBlank(),
                                        onClick = {
                                            val launched = DeveloperContactHelper.launchCallDeveloper(context, devPhone)
                                            if (!launched) {
                                                onMessage(if (isBn) "কল ডায়ালার খোলা যায়নি।" else "No phone dialer app found.")
                                            }
                                        },
                                        modifier = Modifier
                                            .weight(1f)
                                            .heightIn(min = 48.dp)
                                            .testTag("settings_call_developer_button")
                                    ) {
                                        Icon(Icons.Default.Call, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text(tr("Call", "কল করুন"), maxLines = 1)
                                    }

                                    Button(
                                        enabled = devWhatsApp.isNotBlank(),
                                        onClick = {
                                            val msgText = if (isBn) {
                                                "আসসালামু আলাইকুম ${systemSettings.developerName}, Smart Farm অ্যাপ সম্পর্কে সাহায্য প্রয়োজন। ব্যবহারকারী: ${user.name} (${user.email})"
                                            } else {
                                                "Hello ${systemSettings.developerName}, I need support with Smart Farm Management. User: ${user.name} (${user.email})"
                                            }
                                            val launched = DeveloperContactHelper.launchWhatsAppDeveloper(context, devWhatsApp, msgText)
                                            if (!launched) {
                                                onMessage(if (isBn) "হোয়াটসঅ্যাপ খোলা যায়নি।" else "Could not open WhatsApp.")
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color(0xFF25D366),
                                            contentColor = Color.White
                                        ),
                                        modifier = Modifier
                                            .weight(1f)
                                            .heightIn(min = 48.dp)
                                            .testTag("settings_whatsapp_developer_button")
                                    ) {
                                        Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text(tr("WhatsApp", "হোয়াটসঅ্যাপ"), maxLines = 1)
                                    }
                                }

                                if (devEmail.isNotBlank()) {
                                    OutlinedButton(
                                        onClick = {
                                            val launched = DeveloperContactHelper.launchEmailDeveloper(
                                                context = context,
                                                email = devEmail,
                                                subject = "Smart Farm Support Request (${user.name})",
                                                body = "User: ${user.name}\nEmail: ${user.email}\nMobile: ${user.mobile}\n\nIssue:\n"
                                            )
                                            if (!launched) {
                                                clipboard.setText(AnnotatedString(devEmail))
                                                onMessage(if (isBn) "ইমেইল কপি হয়েছে: $devEmail" else "Email copied: $devEmail")
                                            }
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .heightIn(min = 48.dp)
                                            .testTag("settings_email_developer_button")
                                    ) {
                                        Icon(Icons.Default.Email, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.width(8.dp))
                                        Text("${tr("Email Support", "ইমেইল সাপোর্ট")}: $devEmail", maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    }
                                }
                            }
                        }

                        // Report a problem / In-App Support Ticket
                        if (onSubmitSupport != null) {
                            FilledTonalButton(
                                onClick = { showSupportDialog = true },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 48.dp)
                                    .testTag("settings_report_problem_button")
                            ) {
                                Icon(Icons.Default.BugReport, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(tr("Report App Issue / Send Support Ticket", "অ্যাপের সমস্যা জানান / সাপোর্ট টিকিট পাঠান"), fontWeight = FontWeight.SemiBold)
                            }
                        }

                        // User's Support Tickets
                        if (supportRequests.isNotEmpty()) {
                            Text(
                                tr("Your Support Requests (${supportRequests.size})", "আপনার সাপোর্ট অনুরোধ (${supportRequests.size})"),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            supportRequests.take(5).forEach { req ->
                                val stLabel = if (req.status == "OPEN") tr("OPEN", "খোলা") else tr("RESOLVED", "সমাধান হয়েছে")
                                Surface(
                                    shape = MaterialTheme.shapes.small,
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                        Text("${req.subject} ($stLabel)", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                        Text(req.message, style = MaterialTheme.typography.bodySmall)
                                        if (req.adminReply.isNotBlank()) {
                                            Text(
                                                "${tr("Developer Reply", "ডেভেলপারের উত্তর")}: ${req.adminReply}",
                                                color = MaterialTheme.colorScheme.primary,
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Quick Help FAQ
                        OutlinedButton(
                            onClick = { showFaqSection = !showFaqSection },
                            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("settings_faq_toggle")
                        ) {
                            Icon(Icons.AutoMirrored.Filled.HelpOutline, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                tr("Quick Help & User Guide", "দ্রুত সাহায্য ও ব্যবহারবিধি"),
                                modifier = Modifier.weight(1f)
                            )
                            Icon(if (showFaqSection) Icons.Default.ExpandLess else Icons.Default.ExpandMore, contentDescription = null)
                        }

                        AnimatedVisibility(visible = showFaqSection) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                HelpFaqRow(
                                    q = tr("How do I change Language, Currency, or Theme?", "ভাষা, মুদ্রা ও থিম কিভাবে পরিবর্তন করবো?"),
                                    a = tr(
                                        "Use the Language, Country & Currency, and Theme controls right above inside Settings.",
                                        "এই সেটিংস পাতার উপরেই ভাষা (বাংলা/English), মুদ্রা ও থিম মোড পরিবর্তনের অপশন রয়েছে।"
                                    )
                                )
                                HelpFaqRow(
                                    q = tr("How do I report a bug or contact the Developer?", "কোনো সমস্যা হলে ডেভেলপারের সাথে কিভাবে যোগাযোগ করবো?"),
                                    a = tr(
                                        "Tap Call or WhatsApp under Help & Support to reach the Developer directly, or send an in-app support ticket.",
                                        "সমস্যা হলে কল করুন অথবা হোয়াটসঅ্যাপ বাটনে ট্যাপ করে সরাসরি ডেভেলপারের সাথে কথা বলুন বা সাপোর্ট টিকিট পাঠান।"
                                    )
                                )
                                HelpFaqRow(
                                    q = tr("How do Farm Records, Agriculture & AI Video Ads work?", "খামার, কৃষি হিসাব ও AI বিজ্ঞাপন ভিডিও কিভাবে কাজ করে?"),
                                    a = tr(
                                        "Record land plots, crop expenses, and profit/loss in Agriculture, create Bangla ad videos from product photos in AI Ad Studio, and share to WhatsApp/Facebook/Instagram.",
                                        "কৃষি হিসাব থেকে জমি, খাতভিত্তিক খরচ ও লাভ-ক্ষতি রাখুন এবং AI বিজ্ঞাপন ভিডিও দিয়ে বাংলা ভিডিও তৈরি করে ফেসবুক, হোয়াটসঅ্যাপ ও ইনস্টাগ্রামে শেয়ার করুন।"
                                    )
                                )
                            }
                        }
                    }
                }
            }

            // 5. Optional Notifications Inbox (used in Customer Portal Settings)
            if (notifications != null && onMarkNotificationsRead != null) {
                item(key = "settings_notifications_card") {
                    Surface(
                        shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 2.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "${strings.notifications} (${notifications.size})",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                TextButton(
                                    onClick = onMarkNotificationsRead,
                                    enabled = notifications.any { !it.readFlag }
                                ) {
                                    Text(tr("Mark Read", "সব পঠিত"))
                                }
                            }
                            if (notifications.isEmpty()) {
                                Text(
                                    tr("No notifications yet.", "এখনও কোনো নোটিফিকেশন নেই।"),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            } else {
                                notifications.take(6).forEach { n ->
                                    Surface(
                                        shape = MaterialTheme.shapes.small,
                                        color = if (n.readFlag) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                                        else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(10.dp)) {
                                            Text(n.title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                            Text(n.message, style = MaterialTheme.typography.bodySmall)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 6. Log Out Card
            item(key = "settings_logout_card") {
                Button(
                    onClick = { showLogoutConfirm = true },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 50.dp)
                        .testTag("logout_button")
                ) {
                    Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = strings.logout)
                    Spacer(Modifier.width(8.dp))
                    Text(strings.logout, fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    // Currency & Country Dialog
    if (showCurrencyDialog) {
        AlertDialog(
            onDismissRequest = { showCurrencyDialog = false },
            title = { Text(strings.currency) },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 360.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    CurrencyOption.entries.forEach { curr ->
                        ListItem(
                            headlineContent = { Text("${curr.symbol} ${curr.code} — ${curr.countryName}", fontWeight = FontWeight.SemiBold) },
                            supportingContent = { Text("${tr("Gateways", "পেমেন্ট মাধ্যম")}: ${curr.gateways.joinToString(", ")}") },
                            trailingContent = {
                                RadioButton(selected = currentCurrency == curr, onClick = { onSelectCurrency(curr) })
                            },
                            modifier = Modifier.clickable {
                                onSelectCurrency(curr)
                                showCurrencyDialog = false
                            }
                        )
                    }
                }
            },
            confirmButton = {
                Button(onClick = { showCurrencyDialog = false }) { Text(tr("Done", "সম্পন্ন")) }
            }
        )
    }

    // Label Customization Dialog
    if (showLabelEditor) {
        var customAppName by remember { mutableStateOf(languageOverrides["appName"] ?: "") }
        var customDash by remember { mutableStateOf(languageOverrides["dashboard"] ?: "") }
        var customFarms by remember { mutableStateOf(languageOverrides["farms"] ?: "") }
        AlertDialog(
            onDismissRequest = { showLabelEditor = false },
            title = { Text(tr("Customize App Terminology", "অ্যাপের শিরোনাম কাস্টমাইজ করুন")) },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        tr(
                            "Leave blank to use default ${currentLanguage.displayName} text.",
                            "ডিফল্ট ${currentLanguage.nativeName} লেখা ব্যবহার করতে ফাঁকা রাখুন।"
                        ),
                        style = MaterialTheme.typography.labelSmall
                    )
                    OutlinedTextField(value = customAppName, onValueChange = { customAppName = it }, label = { Text(tr("App Title", "অ্যাপের নাম")) }, singleLine = true)
                    OutlinedTextField(value = customDash, onValueChange = { customDash = it }, label = { Text(tr("Dashboard Tab", "ড্যাশবোর্ড ট্যাব")) }, singleLine = true)
                    OutlinedTextField(value = customFarms, onValueChange = { customFarms = it }, label = { Text(tr("Farms Tab", "খামার ট্যাব")) }, singleLine = true)
                }
            },
            confirmButton = {
                Button(onClick = {
                    onSetLanguageOverride("appName", customAppName)
                    onSetLanguageOverride("dashboard", customDash)
                    onSetLanguageOverride("farms", customFarms)
                    showLabelEditor = false
                }) { Text(strings.save) }
            },
            dismissButton = { TextButton(onClick = { showLabelEditor = false }) { Text(strings.cancel) } }
        )
    }

    // Submit Support Ticket Dialog
    if (showSupportDialog && onSubmitSupport != null) {
        var subject by remember { mutableStateOf("") }
        var msg by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showSupportDialog = false },
            title = { Text(tr("Report Problem / Support Request", "সমস্যা জানান / সাপোর্ট অনুরোধ")) },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = subject,
                        onValueChange = { subject = it },
                        label = { Text(tr("Subject *", "বিষয় *")) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("support_subject_input")
                    )
                    OutlinedTextField(
                        value = msg,
                        onValueChange = { msg = it },
                        label = { Text(tr("Describe your issue *", "সমস্যার বিবরণ লিখুন *")) },
                        minLines = 3,
                        modifier = Modifier.fillMaxWidth().testTag("support_message_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    enabled = subject.isNotBlank() && msg.isNotBlank(),
                    onClick = {
                        onSubmitSupport(subject, msg)
                        showSupportDialog = false
                    },
                    modifier = Modifier.testTag("support_submit_button")
                ) { Text(tr("Send", "পাঠান")) }
            },
            dismissButton = { TextButton(onClick = { showSupportDialog = false }) { Text(strings.cancel) } }
        )
    }

    // Admin/Developer Editable Contact Settings Dialog
    if (showEditDevContactDialog && onSaveDeveloperSettings != null) {
        var editName by remember(systemSettings) { mutableStateOf(systemSettings.developerName) }
        var editPhone by remember(systemSettings) { mutableStateOf(systemSettings.developerPhone) }
        var editWhatsApp by remember(systemSettings) { mutableStateOf(systemSettings.developerWhatsApp) }
        var editEmail by remember(systemSettings) { mutableStateOf(systemSettings.developerEmail) }
        var editNote by remember(systemSettings) { mutableStateOf(systemSettings.developerSupportNote) }

        AlertDialog(
            onDismissRequest = { showEditDevContactDialog = false },
            title = { Text(tr("Configure Developer Support Contact", "ডেভেলপার ও কোম্পানি যোগাযোগ তথ্য")) },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = editName,
                        onValueChange = { editName = it },
                        label = { Text(tr("Developer / Company Name", "ডেভেলপার / কোম্পানির নাম")) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editPhone,
                        onValueChange = { editPhone = it },
                        label = { Text(tr("Mobile Number (Call)", "মোবাইল নম্বর (কল)")) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editWhatsApp,
                        onValueChange = { editWhatsApp = it },
                        label = { Text(tr("WhatsApp Number", "হোয়াটসঅ্যাপ নম্বর")) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editEmail,
                        onValueChange = { editEmail = it },
                        label = { Text(tr("Support Email", "সাপোর্ট ইমেইল")) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editNote,
                        onValueChange = { editNote = it },
                        label = { Text(tr("Support Message / Note", "সাপোর্ট নির্দেশনা")) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    onSaveDeveloperSettings(
                        systemSettings.copy(
                            developerName = editName.trim(),
                            developerPhone = editPhone.trim(),
                            developerWhatsApp = editWhatsApp.trim(),
                            developerEmail = editEmail.trim(),
                            developerSupportNote = editNote.trim()
                        )
                    )
                    showEditDevContactDialog = false
                }) { Text(strings.save) }
            },
            dismissButton = { TextButton(onClick = { showEditDevContactDialog = false }) { Text(strings.cancel) } }
        )
    }

    // Logout Confirmation Dialog
    if (showLogoutConfirm) {
        AlertDialog(
            onDismissRequest = { showLogoutConfirm = false },
            icon = { Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text(tr("Log Out?", "লগ আউট করবেন?")) },
            text = {
                Text(
                    tr(
                        "You will be signed out of ${user.email}. You can sign back in anytime.",
                        "${user.email} থেকে লগ আউট হবেন। পরে যেকোনো সময় আবার লগইন করতে পারবেন।"
                    )
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showLogoutConfirm = false
                        onLogout()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.testTag("logout_confirm_button")
                ) { Text(strings.logout) }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutConfirm = false }) { Text(strings.cancel) }
            }
        )
    }
}

@Composable
private fun HelpFaqRow(q: String, a: String) {
    Surface(
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(q, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            Text(a, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
