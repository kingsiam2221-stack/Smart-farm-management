package com.example.ui.screens.auth

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.local.UserRole
import com.example.i18n.AppLanguage
import com.example.i18n.AppStrings
import com.example.i18n.CurrencyOption
import com.example.ui.components.LanguageCurrencyDialog

private enum class AuthScreenMode {
    CUSTOMER_LOGIN,
    CREATE_CUSTOMER,
    CREATE_FARMER,
    FARMER_LOGIN,
    FORGOT_PASSWORD
}

@Composable
fun AuthScreen(
    strings: AppStrings,
    currentLanguage: AppLanguage,
    currentCurrency: CurrencyOption,
    darkModeOverride: Boolean?,
    authError: String?,
    onClearError: () -> Unit,
    onLogin: (String, String) -> Unit,
    onRegister: (String, String, String, String, UserRole) -> Unit,
    onResetPassword: (String, String, () -> Unit) -> Unit,
    onSelectLanguage: (AppLanguage) -> Unit,
    onSelectCurrency: (CurrencyOption) -> Unit,
    onSelectDarkMode: (Boolean?) -> Unit
) {
    var authMode by remember { mutableStateOf(AuthScreenMode.CUSTOMER_LOGIN) }

    var identifier by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }

    var regName by remember { mutableStateOf("") }
    var regMobile by remember { mutableStateOf("") }
    var regEmail by remember { mutableStateOf("") }
    var regPassword by remember { mutableStateOf("") }
    var showRegPassword by remember { mutableStateOf(false) }

    var resetIdentifier by remember { mutableStateOf("") }
    var resetNewPassword by remember { mutableStateOf("") }

    var showSettingsDialog by remember { mutableStateOf(false) }
    var showAdminAccessDialog by remember { mutableStateOf(false) }

    val isBangla = currentLanguage == AppLanguage.BN

    fun switchMode(target: AuthScreenMode) {
        onClearError()
        authMode = target
    }

    if (authMode != AuthScreenMode.CUSTOMER_LOGIN) {
        BackHandler { switchMode(AuthScreenMode.CUSTOMER_LOGIN) }
    }

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(modifier = Modifier.widthIn(max = 520.dp)) {
                // Top Bar: Back (on secondary modes) + Language / Currency Selector
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (authMode != AuthScreenMode.CUSTOMER_LOGIN) {
                        TextButton(
                            onClick = { switchMode(AuthScreenMode.CUSTOMER_LOGIN) },
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("back_to_customer_login_button")
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = if (isBangla) "গ্রাহক লগইনে ফিরুন" else "Back to Customer Login",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(if (isBangla) "গ্রাহক লগইন" else "Customer Login")
                        }
                    } else {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(50.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    Icons.Default.ShoppingBag,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    if (isBangla) "গ্রাহক পোর্টাল" else "Customer Portal",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FilterChip(
                            selected = isBangla,
                            onClick = {
                                onSelectLanguage(if (isBangla) AppLanguage.EN else AppLanguage.BN)
                            },
                            label = {
                                Text(
                                    if (isBangla) "EN" else "বাংলা",
                                    fontWeight = FontWeight.Bold
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Translate,
                                    contentDescription = strings.language,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            modifier = Modifier.testTag("auth_quick_language_toggle")
                        )

                        AssistChip(
                            onClick = { showSettingsDialog = true },
                            label = { Text("${currentCurrency.symbol} ${currentCurrency.code}") },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Language,
                                    contentDescription = strings.currency,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        )
                    }
                }

                Spacer(Modifier.height(10.dp))

                // Hero Card
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(156.dp)
                        .clip(RoundedCornerShape(22.dp))
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_farm_hero),
                        contentDescription = strings.appName,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    listOf(
                                        Color(0xFF0A2E11).copy(alpha = 0.25f),
                                        Color(0xFF0A2E11).copy(alpha = 0.88f)
                                    )
                                )
                            )
                    )
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(18.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.Eco, contentDescription = null, tint = Color(0xFFA5F3B0))
                            Text(
                                strings.appName,
                                style = MaterialTheme.typography.titleLarge,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(
                            when (authMode) {
                                AuthScreenMode.CUSTOMER_LOGIN ->
                                    if (isBangla) "তাজা ডিম, দুধ, মাংস ও খামার পণ্য অর্ডার করতে লগইন করুন"
                                    else "Customer Login • Fresh farm eggs, dairy, meat & direct orders"
                                AuthScreenMode.CREATE_CUSTOMER ->
                                    if (isBangla) "নতুন গ্রাহক অ্যাকাউন্ট তৈরি করুন"
                                    else "Create Customer Account • Browse & order fresh farm goods"
                                AuthScreenMode.CREATE_FARMER ->
                                    if (isBangla) "খামারি অ্যাকাউন্ট তৈরি করে আপনার খামার পরিচালনা করুন"
                                    else "Make Farmer Account • Manage farms, livestock, eggs & sales"
                                AuthScreenMode.FARMER_LOGIN ->
                                    if (isBangla) "খামারি অ্যাকাউন্টে প্রবেশ করুন"
                                    else "Farmer Login • Access your farms, inventory & AI advisor"
                                AuthScreenMode.FORGOT_PASSWORD ->
                                    if (isBangla) "আপনার অ্যাকাউন্টের পাসওয়ার্ড রিসেট করুন"
                                    else "Reset your account password securely"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.90f)
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                if (!authError.isNullOrBlank()) {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Default.ErrorOutline,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error
                            )
                            Text(
                                authError,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                }

                AnimatedContent(targetState = authMode, label = "auth_mode_transition") { mode ->
                    when (mode) {
                        AuthScreenMode.CUSTOMER_LOGIN -> {
                            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                                // Primary Customer Login Card
                                Surface(
                                    shape = RoundedCornerShape(20.dp),
                                    color = MaterialTheme.colorScheme.surface,
                                    tonalElevation = 2.dp,
                                    border = BorderStroke(
                                        1.dp,
                                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(18.dp),
                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            Surface(
                                                shape = RoundedCornerShape(12.dp),
                                                color = MaterialTheme.colorScheme.primaryContainer,
                                                modifier = Modifier.size(40.dp)
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Icon(
                                                        Icons.Default.ShoppingCart,
                                                        contentDescription = null,
                                                        tint = MaterialTheme.colorScheme.primary
                                                    )
                                                }
                                            }
                                            Column {
                                                Text(
                                                    if (isBangla) "গ্রাহক লগইন" else "Customer Login",
                                                    style = MaterialTheme.typography.titleLarge,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Text(
                                                    if (isBangla) "ইমেইল বা মোবাইল ও পাসওয়ার্ড দিন"
                                                    else "Sign in with your customer email or mobile",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }

                                        Spacer(Modifier.height(2.dp))

                                        OutlinedTextField(
                                            value = identifier,
                                            onValueChange = { identifier = it },
                                            label = {
                                                Text(if (isBangla) "ইমেইল বা মোবাইল নম্বর" else "Email or Mobile Number")
                                            },
                                            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                                            singleLine = true,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .testTag("login_identifier_input")
                                        )

                                        OutlinedTextField(
                                            value = password,
                                            onValueChange = { password = it },
                                            label = { Text(if (isBangla) "পাসওয়ার্ড" else "Password") },
                                            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                                            trailingIcon = {
                                                IconButton(onClick = { showPassword = !showPassword }) {
                                                    Icon(
                                                        if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                                        contentDescription = if (isBangla) "পাসওয়ার্ড দেখুন/লুকান" else "Toggle password"
                                                    )
                                                }
                                            },
                                            visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                                            singleLine = true,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .testTag("login_password_input")
                                        )

                                        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                                            TextButton(
                                                onClick = { switchMode(AuthScreenMode.FORGOT_PASSWORD) },
                                                modifier = Modifier.testTag("forgot_password_button")
                                            ) {
                                                Text(strings.forgotPassword)
                                            }
                                        }

                                        Button(
                                            onClick = { onLogin(identifier, password) },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(50.dp)
                                                .testTag("login_button")
                                        ) {
                                            Icon(Icons.Default.Login, contentDescription = null)
                                            Spacer(Modifier.width(8.dp))
                                            Text(
                                                if (isBangla) "গ্রাহক হিসেবে লগইন" else "Customer Sign In",
                                                fontWeight = FontWeight.Bold
                                            )
                                        }

                                        FilledTonalButton(
                                            onClick = { onLogin("customer@smartfarm.app", "customer123") },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(48.dp)
                                                .testTag("demo_customer_button")
                                        ) {
                                            Icon(
                                                Icons.Default.ShoppingBag,
                                                contentDescription = null,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(Modifier.width(8.dp))
                                            Text(if (isBangla) "গ্রাহক ডেমো দেখুন" else "Try Customer Demo")
                                        }
                                    }
                                }

                                // Other Account Options Section
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Text(
                                        if (isBangla) "অন্যান্য অপশন" else "Other Account Options",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        // Option 1: Create Account (Customer)
                                        OptionActionCard(
                                            icon = Icons.Default.PersonAdd,
                                            title = if (isBangla) "অ্যাকাউন্ট তৈরি" else "Create Account",
                                            subtitle = if (isBangla) "নতুন গ্রাহক" else "New Customer",
                                            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.65f),
                                            modifier = Modifier
                                                .weight(1f)
                                                .testTag("create_account_option_button"),
                                            onClick = { switchMode(AuthScreenMode.CREATE_CUSTOMER) }
                                        )

                                        // Option 2: Make Farmer Account
                                        OptionActionCard(
                                            icon = Icons.Default.Agriculture,
                                            title = if (isBangla) "খামারি অ্যাকাউন্ট" else "Make Farmer Account",
                                            subtitle = if (isBangla) "নতুন খামার তৈরি" else "Farm Owner Sign Up",
                                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.72f),
                                            modifier = Modifier
                                                .weight(1f)
                                                .testTag("make_farmer_account_option_button"),
                                            onClick = { switchMode(AuthScreenMode.CREATE_FARMER) }
                                        )
                                    }

                                    // Existing Farmer Login button
                                    OutlinedButton(
                                        onClick = { switchMode(AuthScreenMode.FARMER_LOGIN) },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(50.dp)
                                            .testTag("farmer_login_option_button")
                                    ) {
                                        Icon(
                                            Icons.Default.Storefront,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        Text(
                                            if (isBangla) "খামারি লগইন (বিদ্যমান খামার)"
                                            else "Farmer Login (Existing Farm Owners)",
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }
                        }

                        AuthScreenMode.CREATE_CUSTOMER -> {
                            RegistrationCard(
                                title = if (isBangla) "নতুন গ্রাহক অ্যাকাউন্ট তৈরি করুন" else "Create Customer Account",
                                subtitle = if (isBangla)
                                    "পণ্য কিনতে ও অর্ডার ট্র্যাক করতে তথ্য পূরণ করুন"
                                else
                                    "Register as a customer to browse products & place orders",
                                badgeLabel = if (isBangla) "গ্রাহক" else "Customer / Buyer",
                                icon = Icons.Default.PersonAdd,
                                submitLabel = if (isBangla) "গ্রাহক অ্যাকাউন্ট তৈরি করুন" else "Create Customer Account",
                                submitTag = "signup_button",
                                isBangla = isBangla,
                                name = regName,
                                onNameChange = { regName = it },
                                mobile = regMobile,
                                onMobileChange = { regMobile = it },
                                email = regEmail,
                                onEmailChange = { regEmail = it },
                                password = regPassword,
                                onPasswordChange = { regPassword = it },
                                showPassword = showRegPassword,
                                onToggleShowPassword = { showRegPassword = !showRegPassword },
                                onSubmit = { onRegister(regName, regMobile, regEmail, regPassword, UserRole.CUSTOMER) },
                                alternatePromptLabel = if (isBangla)
                                    "আপনি কি খামার মালিক? খামারি অ্যাকাউন্ট তৈরি করুন"
                                else
                                    "Own a farm instead? Make a Farmer Account",
                                onAlternateClick = { switchMode(AuthScreenMode.CREATE_FARMER) }
                            )
                        }

                        AuthScreenMode.CREATE_FARMER -> {
                            RegistrationCard(
                                title = if (isBangla) "খামারি অ্যাকাউন্ট তৈরি করুন" else "Make Farmer Account",
                                subtitle = if (isBangla)
                                    "একাধিক খামার, পশুপাখি, ডিম, বিক্রি ও হিসাব পরিচালনা করুন"
                                else
                                    "Create unlimited farms, manage livestock, eggs, sales & AI insights",
                                badgeLabel = if (isBangla) "খামারি" else "Farmer / Farm Owner",
                                icon = Icons.Default.Agriculture,
                                submitLabel = if (isBangla) "খামারি অ্যাকাউন্ট তৈরি করুন" else "Create Farmer Account",
                                submitTag = "signup_farmer_button",
                                isBangla = isBangla,
                                name = regName,
                                onNameChange = { regName = it },
                                mobile = regMobile,
                                onMobileChange = { regMobile = it },
                                email = regEmail,
                                onEmailChange = { regEmail = it },
                                password = regPassword,
                                onPasswordChange = { regPassword = it },
                                showPassword = showRegPassword,
                                onToggleShowPassword = { showRegPassword = !showRegPassword },
                                onSubmit = { onRegister(regName, regMobile, regEmail, regPassword, UserRole.FARMER) },
                                alternatePromptLabel = if (isBangla)
                                    "আগেই খামারি অ্যাকাউন্ট আছে? খামারি লগইন করুন"
                                else
                                    "Already have a farmer account? Farmer Login",
                                onAlternateClick = { switchMode(AuthScreenMode.FARMER_LOGIN) }
                            )
                        }

                        AuthScreenMode.FARMER_LOGIN -> {
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = MaterialTheme.colorScheme.surface,
                                tonalElevation = 2.dp,
                                border = BorderStroke(
                                    1.dp,
                                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(18.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = MaterialTheme.colorScheme.primaryContainer,
                                            modifier = Modifier.size(40.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    Icons.Default.Agriculture,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        }
                                        Column {
                                            Text(
                                                if (isBangla) "খামারি লগইন" else "Farmer Login",
                                                style = MaterialTheme.typography.titleLarge,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                if (isBangla) "আপনার খামার ড্যাশবোর্ডে প্রবেশ করুন"
                                                else "Sign in to manage your farms & operations",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    OutlinedTextField(
                                        value = identifier,
                                        onValueChange = { identifier = it },
                                        label = {
                                            Text(if (isBangla) "ইমেইল বা মোবাইল নম্বর" else "Email or Mobile Number")
                                        },
                                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                                        singleLine = true,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("farmer_login_identifier_input")
                                    )

                                    OutlinedTextField(
                                        value = password,
                                        onValueChange = { password = it },
                                        label = { Text(if (isBangla) "পাসওয়ার্ড" else "Password") },
                                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                                        trailingIcon = {
                                            IconButton(onClick = { showPassword = !showPassword }) {
                                                Icon(
                                                    if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                                    contentDescription = if (isBangla) "পাসওয়ার্ড দেখুন/লুকান" else "Toggle password"
                                                )
                                            }
                                        },
                                        visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                                        singleLine = true,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("farmer_login_password_input")
                                    )

                                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                                        TextButton(onClick = { switchMode(AuthScreenMode.FORGOT_PASSWORD) }) {
                                            Text(strings.forgotPassword)
                                        }
                                    }

                                    Button(
                                        onClick = { onLogin(identifier, password) },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(50.dp)
                                            .testTag("farmer_login_button")
                                    ) {
                                        Icon(Icons.Default.Login, contentDescription = null)
                                        Spacer(Modifier.width(8.dp))
                                        Text(
                                            if (isBangla) "খামারি হিসেবে লগইন" else "Sign In as Farmer",
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        FilledTonalButton(
                                            onClick = { onLogin("farmer@smartfarm.app", "farmer123") },
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(48.dp)
                                                .testTag("demo_farmer_button")
                                        ) {
                                            Icon(
                                                Icons.Default.Agriculture,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(Modifier.width(4.dp))
                                            Text(if (isBangla) "খামারি ডেমো" else "Farmer Demo")
                                        }
                                        OutlinedButton(
                                            onClick = { switchMode(AuthScreenMode.CREATE_FARMER) },
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(48.dp)
                                        ) {
                                            Text(if (isBangla) "নতুন খামারি" else "Make Farmer")
                                        }
                                    }
                                }
                            }
                        }

                        AuthScreenMode.FORGOT_PASSWORD -> {
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = MaterialTheme.colorScheme.surface,
                                tonalElevation = 2.dp,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(18.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Text(
                                        strings.resetPassword,
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        if (isBangla)
                                            "আপনার নিবন্ধিত ইমেইল বা মোবাইল নম্বর এবং নতুন পাসওয়ার্ড দিন।"
                                        else
                                            "Enter your registered email or mobile number and set a new password.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    OutlinedTextField(
                                        value = resetIdentifier,
                                        onValueChange = { resetIdentifier = it },
                                        label = { Text(if (isBangla) "নিবন্ধিত ইমেইল বা মোবাইল" else "Registered Email or Mobile") },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    OutlinedTextField(
                                        value = resetNewPassword,
                                        onValueChange = { resetNewPassword = it },
                                        label = { Text(if (isBangla) "নতুন পাসওয়ার্ড" else "New Password") },
                                        visualTransformation = PasswordVisualTransformation(),
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    Button(
                                        onClick = {
                                            onResetPassword(resetIdentifier, resetNewPassword) {
                                                authMode = AuthScreenMode.CUSTOMER_LOGIN
                                                identifier = resetIdentifier
                                                password = ""
                                            }
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(48.dp)
                                    ) {
                                        Text(strings.resetPassword, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(22.dp))
                // Private Developer / Super Admin Entry (isolated from Farmer/Customer dashboards)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .minimumInteractiveComponentSize()
                        .clickable { showAdminAccessDialog = true }
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.AdminPanelSettings,
                        contentDescription = if (isBangla) "সুপার অ্যাডমিন প্রবেশ" else "Super Admin Access",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        if (isBangla) "প্রাইভেট ডেভেলপার / সুপার অ্যাডমিন প্রবেশ" else "Private Developer / Super Admin Access",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }

    if (showSettingsDialog) {
        LanguageCurrencyDialog(
            currentLanguage = currentLanguage,
            currentCurrency = currentCurrency,
            darkModeOverride = darkModeOverride,
            onSelectLanguage = onSelectLanguage,
            onSelectCurrency = onSelectCurrency,
            onSelectDarkMode = onSelectDarkMode,
            onDismiss = { showSettingsDialog = false }
        )
    }

    if (showAdminAccessDialog) {
        var adminEmail by remember { mutableStateOf("admin@smartfarm.app") }
        var adminPass by remember { mutableStateOf("") }
        var devPin by remember { mutableStateOf("") }
        var localErr by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { showAdminAccessDialog = false },
            icon = { Icon(Icons.Default.Security, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            title = { Text(if (isBangla) "সুপার অ্যাডমিন পোর্টাল" else "Super Admin Portal") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        if (isBangla)
                            "অনুমোদিত ডেভেলপার তথ্য প্রয়োজন। ডেমো পিন: 2026 • পাসওয়ার্ড: admin@2026"
                        else
                            "Authorized developer credentials required. Demo PIN: 2026 • Pass: admin@2026",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (localErr != null) {
                        Text(localErr!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall)
                    }
                    OutlinedTextField(
                        value = adminEmail,
                        onValueChange = { adminEmail = it },
                        label = { Text(if (isBangla) "অ্যাডমিন ইমেইল" else "Admin Email") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = adminPass,
                        onValueChange = { adminPass = it },
                        label = { Text(if (isBangla) "অ্যাডমিন পাসওয়ার্ড" else "Admin Password") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = devPin,
                        onValueChange = { devPin = it },
                        label = { Text(if (isBangla) "ডেভেলপার সিকিউরিটি পিন" else "Developer Security PIN") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (devPin.trim() != "2026") {
                        localErr = if (isBangla) "ভুল ডেভেলপার সিকিউরিটি পিন।" else "Invalid Developer Security PIN."
                    } else {
                        showAdminAccessDialog = false
                        onLogin(adminEmail, adminPass)
                    }
                }) { Text(if (isBangla) "কনসোল খুলুন" else "Unlock Console") }
            },
            dismissButton = {
                TextButton(onClick = { showAdminAccessDialog = false }) { Text(strings.cancel) }
            }
        )
    }
}

@Composable
private fun OptionActionCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    containerColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = containerColor,
        tonalElevation = 1.dp,
        modifier = modifier.heightIn(min = 78.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                icon,
                contentDescription = title,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp)
            )
            Text(
                title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
            Text(
                subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun RegistrationCard(
    title: String,
    subtitle: String,
    badgeLabel: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    submitLabel: String,
    submitTag: String,
    isBangla: Boolean,
    name: String,
    onNameChange: (String) -> Unit,
    mobile: String,
    onMobileChange: (String) -> Unit,
    email: String,
    onEmailChange: (String) -> Unit,
    password: String,
    onPasswordChange: (String) -> Unit,
    showPassword: Boolean,
    onToggleShowPassword: () -> Unit,
    onSubmit: () -> Unit,
    alternatePromptLabel: String,
    onAlternateClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
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
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                    Column {
                        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.65f),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    badgeLabel,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }

            OutlinedTextField(
                value = name,
                onValueChange = onNameChange,
                label = { Text(if (isBangla) "পূর্ণ নাম" else "Full Name") },
                leadingIcon = { Icon(Icons.Default.Badge, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = mobile,
                onValueChange = onMobileChange,
                label = { Text(if (isBangla) "মোবাইল নম্বর" else "Mobile Number") },
                leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = email,
                onValueChange = onEmailChange,
                label = { Text(if (isBangla) "ইমেইল ঠিকানা" else "Email Address") },
                leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = password,
                onValueChange = onPasswordChange,
                label = { Text(if (isBangla) "পাসওয়ার্ড (কমপক্ষে ৪ অক্ষর)" else "Password (min 4 chars)") },
                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                trailingIcon = {
                    IconButton(onClick = onToggleShowPassword) {
                        Icon(
                            if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = if (isBangla) "পাসওয়ার্ড দেখুন/লুকান" else "Toggle password"
                        )
                    }
                },
                visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(2.dp))
            Button(
                onClick = onSubmit,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag(submitTag)
            ) {
                Icon(icon, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(submitLabel, fontWeight = FontWeight.Bold)
            }

            TextButton(
                onClick = onAlternateClick,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(alternatePromptLabel)
            }
        }
    }
}
