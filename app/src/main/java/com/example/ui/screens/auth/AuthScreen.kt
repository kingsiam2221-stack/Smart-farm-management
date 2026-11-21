package com.example.ui.screens.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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

private enum class AuthTab { LOGIN, SIGN_UP, FORGOT_PASSWORD }

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
    var activeTab by remember { mutableStateOf(AuthTab.LOGIN) }
    var identifier by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }

    var regName by remember { mutableStateOf("") }
    var regMobile by remember { mutableStateOf("") }
    var regEmail by remember { mutableStateOf("") }
    var regPassword by remember { mutableStateOf("") }
    var regRole by remember { mutableStateOf(UserRole.FARMER) }

    var resetIdentifier by remember { mutableStateOf("") }
    var resetNewPassword by remember { mutableStateOf("") }

    var showSettingsDialog by remember { mutableStateOf(false) }
    var showAdminAccessDialog by remember { mutableStateOf(false) }

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(modifier = Modifier.widthIn(max = 520.dp)) {
                // Top quick bar: Language & Currency
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AssistChip(
                        onClick = { showSettingsDialog = true },
                        label = { Text("${currentLanguage.nativeName} • ${currentCurrency.symbol} ${currentCurrency.code}") },
                        leadingIcon = { Icon(Icons.Default.Language, contentDescription = strings.language, modifier = Modifier.size(18.dp)) }
                    )
                }

                Spacer(Modifier.height(8.dp))

                // Hero Card
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(176.dp)
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
                                        Color(0xFF0A2E11).copy(alpha = 0.30f),
                                        Color(0xFF0A2E11).copy(alpha = 0.85f)
                                    )
                                )
                            )
                    )
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(18.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.Eco, contentDescription = null, tint = Color(0xFFA5F3B0))
                            Text(
                                strings.appName,
                                style = MaterialTheme.typography.headlineSmall,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Multi-Farm • Livestock & Eggs • Inventory • Sales • Online Store",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.88f)
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Auth Mode Tabs
                TabRow(selectedTabIndex = activeTab.ordinal) {
                    Tab(
                        selected = activeTab == AuthTab.LOGIN,
                        onClick = { activeTab = AuthTab.LOGIN; onClearError() },
                        text = { Text(strings.login) }
                    )
                    Tab(
                        selected = activeTab == AuthTab.SIGN_UP,
                        onClick = { activeTab = AuthTab.SIGN_UP; onClearError() },
                        text = { Text(strings.signUp) }
                    )
                    Tab(
                        selected = activeTab == AuthTab.FORGOT_PASSWORD,
                        onClick = { activeTab = AuthTab.FORGOT_PASSWORD; onClearError() },
                        text = { Text(strings.resetPassword) }
                    )
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
                            Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                            Text(authError, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                }

                when (activeTab) {
                    AuthTab.LOGIN -> {
                        OutlinedTextField(
                            value = identifier,
                            onValueChange = { identifier = it },
                            label = { Text("Email or Mobile Number") },
                            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("login_identifier_input")
                        )
                        Spacer(Modifier.height(10.dp))
                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            label = { Text("Password") },
                            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                            trailingIcon = {
                                IconButton(onClick = { showPassword = !showPassword }) {
                                    Icon(
                                        if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = "Toggle password"
                                    )
                                }
                            },
                            visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("login_password_input")
                        )
                        Spacer(Modifier.height(14.dp))
                        Button(
                            onClick = { onLogin(identifier, password) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("login_button")
                        ) {
                            Icon(Icons.Default.Login, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text(strings.login, fontWeight = FontWeight.Bold)
                        }

                        Spacer(Modifier.height(16.dp))
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                            shape = MaterialTheme.shapes.medium,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    "Quick Demo Accounts",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(Modifier.height(8.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    FilledTonalButton(
                                        onClick = { onLogin("farmer@smartfarm.app", "farmer123") },
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("demo_farmer_button")
                                    ) {
                                        Icon(Icons.Default.Agriculture, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("Farmer Demo")
                                    }
                                    FilledTonalButton(
                                        onClick = { onLogin("customer@smartfarm.app", "customer123") },
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("demo_customer_button")
                                    ) {
                                        Icon(Icons.Default.ShoppingBag, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("Customer Demo")
                                    }
                                }
                            }
                        }
                    }

                    AuthTab.SIGN_UP -> {
                        Text("Account Type", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            FilterChip(
                                selected = regRole == UserRole.FARMER,
                                onClick = { regRole = UserRole.FARMER },
                                label = { Text("Farmer / Owner") },
                                leadingIcon = { Icon(Icons.Default.Agriculture, contentDescription = null, modifier = Modifier.size(16.dp)) }
                            )
                            FilterChip(
                                selected = regRole == UserRole.CUSTOMER,
                                onClick = { regRole = UserRole.CUSTOMER },
                                label = { Text("Customer / Buyer") },
                                leadingIcon = { Icon(Icons.Default.ShoppingCart, contentDescription = null, modifier = Modifier.size(16.dp)) }
                            )
                        }
                        Spacer(Modifier.height(10.dp))
                        OutlinedTextField(
                            value = regName,
                            onValueChange = { regName = it },
                            label = { Text("Full Name") },
                            leadingIcon = { Icon(Icons.Default.Badge, contentDescription = null) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = regMobile,
                            onValueChange = { regMobile = it },
                            label = { Text("Mobile Number") },
                            leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = regEmail,
                            onValueChange = { regEmail = it },
                            label = { Text("Email Address") },
                            leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = regPassword,
                            onValueChange = { regPassword = it },
                            label = { Text("Password (min 4 chars)") },
                            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                            visualTransformation = PasswordVisualTransformation(),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(14.dp))
                        Button(
                            onClick = { onRegister(regName, regMobile, regEmail, regPassword, regRole) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("signup_button")
                        ) {
                            Icon(Icons.Default.PersonAdd, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text(strings.signUp, fontWeight = FontWeight.Bold)
                        }
                    }

                    AuthTab.FORGOT_PASSWORD -> {
                        Text(
                            "Enter your registered email or mobile number and set a new password.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(10.dp))
                        OutlinedTextField(
                            value = resetIdentifier,
                            onValueChange = { resetIdentifier = it },
                            label = { Text("Registered Email or Mobile") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = resetNewPassword,
                            onValueChange = { resetNewPassword = it },
                            label = { Text("New Password") },
                            visualTransformation = PasswordVisualTransformation(),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(14.dp))
                        Button(
                            onClick = {
                                onResetPassword(resetIdentifier, resetNewPassword) {
                                    activeTab = AuthTab.LOGIN
                                    identifier = resetIdentifier
                                    password = ""
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(48.dp)
                        ) {
                            Text(strings.resetPassword, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(Modifier.height(22.dp))
                // Private Developer / Super Admin Entry (isolated from Farmer/Customer dashboards)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showAdminAccessDialog = true }
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.AdminPanelSettings,
                        contentDescription = "Super Admin Access",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "Private Developer / Super Admin Access",
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
            title = { Text("Super Admin Portal") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
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
                        label = { Text("Admin Email") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = adminPass,
                        onValueChange = { adminPass = it },
                        label = { Text("Admin Password") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = devPin,
                        onValueChange = { devPin = it },
                        label = { Text("Developer Security PIN") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (devPin.trim() != "2026") {
                        localErr = "Invalid Developer Security PIN."
                    } else {
                        showAdminAccessDialog = false
                        onLogin(adminEmail, adminPass)
                    }
                }) { Text("Unlock Console") }
            },
            dismissButton = {
                TextButton(onClick = { showAdminAccessDialog = false }) { Text(strings.cancel) }
            }
        )
    }
}
