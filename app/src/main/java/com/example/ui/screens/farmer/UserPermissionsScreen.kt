package com.example.ui.screens.farmer

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.data.local.*
import com.example.i18n.AppStrings
import com.example.i18n.CurrencyFormatter
import com.example.i18n.CurrencyOption
import com.example.i18n.isBangla
import com.example.i18n.tr
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.FeaturePlanBadge
import com.example.ui.components.ProvideSafeTextToolbar
import com.example.ui.components.SubscriptionRequiredLockCard
import com.example.ui.state.FarmerDashboardState

private enum class PermissionViewMode {
    BY_EMPLOYEE,
    BY_OPTION
}

@Composable
fun UserPermissionsScreen(
    state: FarmerDashboardState,
    strings: AppStrings,
    currency: CurrencyOption,
    onSelectFarm: (Long?) -> Unit,
    onSetEmployeePermission: (EmployeeEntity, String, Boolean) -> Unit,
    onSetAllPermissionsForEmployee: (EmployeeEntity, Boolean) -> Unit,
    onApplyPermissionPreset: (EmployeeEntity, PermissionPreset) -> Unit,
    onSetPermissionForMultipleEmployees: (Long, List<EmployeeEntity>, String, Boolean) -> Unit,
    onAddCustomPermissionOption: (Long, String, String, String, String) -> Unit,
    onDeleteCustomPermissionOption: (CustomPermissionOptionEntity) -> Unit,
    onProvisionEmployeeLogin: (EmployeeEntity, String, String, String, () -> Unit) -> Unit,
    onSaveEmployee: (EmployeeEntity) -> Unit,
    onOpenSubscription: () -> Unit = {}
) {
    if (state.accessPolicy.canManagePermissions() &&
        !state.subscriptionPolicy.canAccess(SubscriptionFeature.EMPLOYEES_PERMISSIONS)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            SubscriptionRequiredLockCard(
                feature = SubscriptionFeature.EMPLOYEES_PERMISSIONS,
                onOpenSubscription = onOpenSubscription,
                canSubscribe = state.accessPolicy.has(AppPermission.SUBSCRIPTION_MANAGE)
            )
        }
        return
    }

    AdminPermissionsManagementPanel(
        isAdminAuthorized = state.accessPolicy.canManagePermissions(),
        ownerUserId = state.ownerUserId,
        farms = state.farms,
        selectedFarmId = state.selectedFarmId,
        employees = state.employees,
        employeePermissions = state.employeePermissions,
        customPermissionOptions = state.customPermissionOptions,
        strings = strings,
        currency = currency,
        onSelectFarm = onSelectFarm,
        onSetEmployeePermission = onSetEmployeePermission,
        onSetAllPermissionsForEmployee = onSetAllPermissionsForEmployee,
        onApplyPermissionPreset = onApplyPermissionPreset,
        onSetPermissionForMultipleEmployees = onSetPermissionForMultipleEmployees,
        onAddCustomPermissionOption = onAddCustomPermissionOption,
        onDeleteCustomPermissionOption = onDeleteCustomPermissionOption,
        onProvisionEmployeeLogin = onProvisionEmployeeLogin,
        onSaveEmployee = onSaveEmployee,
        subscriptionPolicy = state.subscriptionPolicy
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AdminPermissionsManagementPanel(
    isAdminAuthorized: Boolean,
    ownerUserId: Long,
    farms: List<FarmEntity>,
    selectedFarmId: Long?,
    employees: List<EmployeeEntity>,
    employeePermissions: List<EmployeePermissionEntity>,
    customPermissionOptions: List<CustomPermissionOptionEntity>,
    strings: AppStrings,
    currency: CurrencyOption,
    onSelectFarm: (Long?) -> Unit,
    onSetEmployeePermission: (EmployeeEntity, String, Boolean) -> Unit,
    onSetAllPermissionsForEmployee: (EmployeeEntity, Boolean) -> Unit,
    onApplyPermissionPreset: (EmployeeEntity, PermissionPreset) -> Unit,
    onSetPermissionForMultipleEmployees: (Long, List<EmployeeEntity>, String, Boolean) -> Unit,
    onAddCustomPermissionOption: (Long, String, String, String, String) -> Unit,
    onDeleteCustomPermissionOption: (CustomPermissionOptionEntity) -> Unit,
    onProvisionEmployeeLogin: (EmployeeEntity, String, String, String, () -> Unit) -> Unit,
    onSaveEmployee: ((EmployeeEntity) -> Unit)? = null,
    subscriptionPolicy: SubscriptionAccessPolicy? = null
) {
    val isBn = isBangla()

    if (!isAdminAuthorized) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            EmptyStateCard(
                icon = Icons.Default.AdminPanelSettings,
                title = tr("403 • Admin Only", "৪০৩ • শুধুমাত্র অ্যাডমিন"),
                subtitle = tr(
                    "Only the Farm Owner or Super Admin can grant or revoke user permissions.",
                    "শুধুমাত্র খামারের মালিক বা সুপার অ্যাডমিন কর্মচারীদের অনুমতি দিতে ও ফিরিয়ে নিতে পারবেন।"
                )
            )
        }
        return
    }

    var mode by remember { mutableStateOf(PermissionViewMode.BY_EMPLOYEE) }
    var searchEmployee by remember { mutableStateOf("") }
    var searchOption by remember { mutableStateOf("") }
    var selectedEmployeeId by remember { mutableStateOf<Long?>(null) }
    var selectedOptionKey by remember { mutableStateOf(AppPermission.DASHBOARD_OVERVIEW.key) }
    var selectedGroupFilter by remember { mutableStateOf<PermissionGroup?>(null) }

    var showAddCustomOptionDialog by remember { mutableStateOf(false) }
    var loginTargetEmployee by remember { mutableStateOf<EmployeeEntity?>(null) }
    var showAddEmployeeDialog by remember { mutableStateOf(false) }

    val descriptors = remember(customPermissionOptions) {
        PermissionCatalog.allDescriptors(customPermissionOptions)
    }

    LaunchedEffect(descriptors) {
        if (descriptors.none { it.key == selectedOptionKey }) {
            selectedOptionKey = descriptors.firstOrNull()?.key ?: AppPermission.DASHBOARD_OVERVIEW.key
        }
    }

    val filteredEmployees = remember(employees, selectedFarmId, searchEmployee) {
        employees.filter { emp ->
            val farmMatch = selectedFarmId == null || emp.farmId == selectedFarmId
            val queryMatch = searchEmployee.isBlank() ||
                emp.name.contains(searchEmployee, ignoreCase = true) ||
                emp.phone.contains(searchEmployee, ignoreCase = true) ||
                emp.designation.contains(searchEmployee, ignoreCase = true) ||
                emp.email.contains(searchEmployee, ignoreCase = true)
            farmMatch && queryMatch
        }
    }

    val selectedEmployee = remember(employees, selectedEmployeeId) {
        employees.firstOrNull { it.id == selectedEmployeeId }
    }

    val permissionsByEmployee = remember(employeePermissions) {
        employeePermissions
            .filter { it.granted }
            .groupBy { it.employeeId }
            .mapValues { entry -> entry.value.map { it.permissionKey }.toSet() }
    }

    val farmsMap = remember(farms) { farms.associateBy { it.id } }

    val filteredDescriptors = remember(descriptors, searchOption, selectedGroupFilter) {
        descriptors.filter { d ->
            val groupMatch = selectedGroupFilter == null || d.group == selectedGroupFilter
            val queryMatch = searchOption.isBlank() ||
                d.titleEn.contains(searchOption, ignoreCase = true) ||
                d.titleBn.contains(searchOption, ignoreCase = true) ||
                d.descEn.contains(searchOption, ignoreCase = true) ||
                d.descBn.contains(searchOption, ignoreCase = true)
            groupMatch && queryMatch
        }
    }

    val activeDescriptor = descriptors.firstOrNull { it.key == selectedOptionKey } ?: descriptors.first()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("user_permissions_screen"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Header Banner
        item(key = "perm_header") {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                tonalElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                tr("User Permissions & Access Control", "ব্যবহারকারী অনুমতি ও অ্যাক্সেস নিয়ন্ত্রণ"),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                tr(
                                    "Admin-only • ${employees.size} employees • ${descriptors.size} separate options",
                                    "শুধুমাত্র অ্যাডমিন • ${employees.size} জন কর্মচারী • ${descriptors.size}টি আলাদা অপশন"
                                ),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                            )
                        }
                        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            if (subscriptionPolicy != null) {
                                FeaturePlanBadge(
                                    isFree = subscriptionPolicy.isFree(SubscriptionFeature.EMPLOYEES_PERMISSIONS),
                                    hasActiveSub = subscriptionPolicy.hasActiveSubscription
                                )
                            }
                            Icon(
                                Icons.Default.AdminPanelSettings,
                                contentDescription = strings.permissions,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilledTonalButton(
                            onClick = { showAddCustomOptionDialog = true },
                            modifier = Modifier.testTag("add_custom_permission_option_button")
                        ) {
                            Icon(Icons.Default.AddCircleOutline, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(tr("Add New Option", "নতুন অপশন যোগ করুন"))
                        }
                        if (onSaveEmployee != null) {
                            OutlinedButton(
                                onClick = { showAddEmployeeDialog = true },
                                modifier = Modifier.testTag("permissions_add_employee_button")
                            ) {
                                Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(strings.addEmployee)
                            }
                        }
                    }
                }
            }
        }

        // Mode Switcher
        item(key = "perm_mode_switch") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = mode == PermissionViewMode.BY_EMPLOYEE,
                    onClick = { mode = PermissionViewMode.BY_EMPLOYEE },
                    leadingIcon = { Icon(Icons.Default.Badge, contentDescription = null, modifier = Modifier.size(16.dp)) },
                    label = { Text(tr("By Employee", "কর্মচারী অনুযায়ী"), maxLines = 1) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("perm_mode_by_employee")
                )
                FilterChip(
                    selected = mode == PermissionViewMode.BY_OPTION,
                    onClick = {
                        mode = PermissionViewMode.BY_OPTION
                        selectedEmployeeId = null
                    },
                    leadingIcon = { Icon(Icons.Default.Checklist, contentDescription = null, modifier = Modifier.size(16.dp)) },
                    label = { Text(tr("By Option (Bulk 50+)", "অপশন অনুযায়ী (বাল্ক)"), maxLines = 1) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("perm_mode_by_option")
                )
            }
        }

        // Farm Filter Chips (helpful when managing 50+ employees across multiple farms)
        if (farms.isNotEmpty()) {
            item(key = "perm_farm_filter") {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item(key = "farm_all") {
                        FilterChip(
                            selected = selectedFarmId == null,
                            onClick = { onSelectFarm(null) },
                            label = { Text("${strings.allFarms} (${employees.size})") }
                        )
                    }
                    items(farms, key = { "farm_${it.id}" }) { farm ->
                        val count = employees.count { it.farmId == farm.id }
                        FilterChip(
                            selected = selectedFarmId == farm.id,
                            onClick = { onSelectFarm(farm.id) },
                            label = { Text("${farm.name} ($count)") }
                        )
                    }
                }
            }
        }

        if (mode == PermissionViewMode.BY_EMPLOYEE) {
            if (selectedEmployee == null) {
                item(key = "emp_search_bar") {
                    OutlinedTextField(
                        value = searchEmployee,
                        onValueChange = { searchEmployee = it },
                        placeholder = {
                            Text(tr("Search 1 to 50+ employees (name, phone, email)...", "১ থেকে ৫০+ কর্মচারী খুঁজুন (নাম, ফোন, ইমেইল)..."))
                        },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("perm_employee_search")
                    )
                }

                if (filteredEmployees.isEmpty()) {
                    item(key = "emp_empty") {
                        EmptyStateCard(
                            icon = Icons.Default.Groups,
                            title = tr("No Employees Found", "কোনো কর্মচারী পাওয়া যায়নি"),
                            subtitle = tr(
                                "Add employees to assign granular permissions and employee login access.",
                                "আলাদা আলাদা অনুমতি ও লগইন অ্যাক্সেস দিতে কর্মচারী যোগ করুন।"
                            ),
                            actionLabel = if (onSaveEmployee != null) strings.addEmployee else null,
                            onAction = if (onSaveEmployee != null) ({ showAddEmployeeDialog = true }) else null
                        )
                    }
                } else {
                    items(filteredEmployees, key = { "perm_emp_${it.id}" }) { emp ->
                        val grantedSet = permissionsByEmployee[emp.id] ?: emptySet()
                        val farmName = farmsMap[emp.farmId]?.name ?: tr("Farm", "খামার")
                        Surface(
                            shape = MaterialTheme.shapes.medium,
                            color = MaterialTheme.colorScheme.surface,
                            tonalElevation = 2.dp,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(emp.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                        Text(
                                            "${emp.designation} • $farmName • ${emp.phone.ifBlank { "-" }}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            if (emp.email.isNotBlank()) "${tr("Login", "লগইন")}: ${emp.email}"
                                            else tr("Login not set yet", "লগইন এখনও সেট করা হয়নি"),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (emp.email.isNotBlank()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary
                                        )
                                    }
                                    AssistChip(
                                        onClick = { selectedEmployeeId = emp.id },
                                        label = {
                                            Text(
                                                "${grantedSet.size}/${descriptors.size}",
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    )
                                }

                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Button(
                                        onClick = { selectedEmployeeId = emp.id },
                                        modifier = Modifier.testTag("manage_emp_perm_${emp.id}")
                                    ) {
                                        Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text(tr("Manage Access", "অ্যাক্সেস নিয়ন্ত্রণ"))
                                    }
                                    FilledTonalButton(
                                        onClick = { onSetAllPermissionsForEmployee(emp, true) },
                                        modifier = Modifier.testTag("grant_all_emp_${emp.id}")
                                    ) {
                                        Text(tr("Give All", "সব দিন"))
                                    }
                                    OutlinedButton(
                                        onClick = { onSetAllPermissionsForEmployee(emp, false) },
                                        modifier = Modifier.testTag("revoke_all_emp_${emp.id}")
                                    ) {
                                        Text(tr("Take Back All", "সব নিন"), color = MaterialTheme.colorScheme.error)
                                    }
                                    TextButton(
                                        onClick = { loginTargetEmployee = emp },
                                        modifier = Modifier.testTag("setup_login_emp_${emp.id}")
                                    ) {
                                        Icon(Icons.Default.VpnKey, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text(if (emp.linkedUserId == null) tr("Set Login", "লগইন সেট") else tr("Reset Login", "লগইন পরিবর্তন"))
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // Selected Employee Granular Switch Editor
                val grantedKeys = permissionsByEmployee[selectedEmployee.id] ?: emptySet()
                val farmName = farmsMap[selectedEmployee.farmId]?.name ?: if (isBn) "খামার" else "Farm"

                item(key = "selected_emp_card") {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f),
                        tonalElevation = 2.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TextButton(
                                    onClick = { selectedEmployeeId = null },
                                    modifier = Modifier.testTag("back_to_employee_list")
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text(tr("All Employees (${employees.size})", "সকল কর্মচারী (${employees.size})"))
                                }
                                AssistChip(
                                    onClick = {},
                                    label = {
                                        Text(
                                            "${grantedKeys.size} / ${descriptors.size} ${tr("Active", "সক্রিয়")}",
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                )
                            }

                            Text(
                                "${selectedEmployee.name} • ${selectedEmployee.designation}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "$farmName • ${selectedEmployee.phone} • " +
                                    if (selectedEmployee.email.isNotBlank()) selectedEmployee.email else tr("No login email yet", "লগইন ইমেইল নেই"),
                                style = MaterialTheme.typography.bodySmall
                            )

                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Button(onClick = { onSetAllPermissionsForEmployee(selectedEmployee, true) }) {
                                    Text(tr("Grant Every Option", "সব অপশন দিন"))
                                }
                                OutlinedButton(onClick = { onSetAllPermissionsForEmployee(selectedEmployee, false) }) {
                                    Text(tr("Revoke Every Option", "সব অপশন ফিরিয়ে নিন"), color = MaterialTheme.colorScheme.error)
                                }
                                FilledTonalButton(onClick = { loginTargetEmployee = selectedEmployee }) {
                                    Icon(Icons.Default.VpnKey, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text(tr("Employee Login", "কর্মচারী লগইন"))
                                }
                            }

                            Text(
                                tr("Quick Role Presets:", "দ্রুত রোল প্রিসেট:"),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                PermissionPreset.entries.forEach { preset ->
                                    SuggestionChip(
                                        onClick = { onApplyPermissionPreset(selectedEmployee, preset) },
                                        label = { Text(if (isBn) preset.labelBn else preset.labelEn) }
                                    )
                                }
                            }
                        }
                    }
                }

                item(key = "selected_emp_filter") {
                    OutlinedTextField(
                        value = searchOption,
                        onValueChange = { searchOption = it },
                        placeholder = { Text(tr("Search options (e.g., Eggs, Subscription, Caution, Reports)...", "অপশন খুঁজুন (যেমন: ডিম, সাবস্ক্রিপশন, সতর্কতা, রিপোর্ট)...")) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(6.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        item(key = "grp_all") {
                            FilterChip(
                                selected = selectedGroupFilter == null,
                                onClick = { selectedGroupFilter = null },
                                label = { Text(tr("All Groups", "সব বিভাগ")) }
                            )
                        }
                        items(PermissionGroup.entries, key = { "grp_${it.name}" }) { grp ->
                            FilterChip(
                                selected = selectedGroupFilter == grp,
                                onClick = { selectedGroupFilter = if (selectedGroupFilter == grp) null else grp },
                                label = { Text(if (isBn) grp.titleBn else grp.titleEn) }
                            )
                        }
                    }
                }

                val grouped = filteredDescriptors.groupBy { it.group }
                grouped.forEach { (group, groupDescriptors) ->
                    item(key = "group_hdr_${group.name}") {
                        Text(
                            if (isBn) group.titleBn else group.titleEn,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                    items(groupDescriptors, key = { "perm_row_${selectedEmployee.id}_${it.key}" }) { desc ->
                        val isGranted = grantedKeys.contains(desc.key)
                        val customEntity = if (desc.isCustom) {
                            customPermissionOptions.firstOrNull { it.permissionKey == desc.key }
                        } else null
                        Surface(
                            shape = MaterialTheme.shapes.medium,
                            color = if (isGranted) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
                            else MaterialTheme.colorScheme.surface,
                            tonalElevation = 1.dp,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(end = 10.dp),
                                    verticalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            if (isBn) desc.titleBn else desc.titleEn,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                        if (desc.isCustom) {
                                            Surface(
                                                color = MaterialTheme.colorScheme.tertiaryContainer,
                                                shape = RoundedCornerShape(6.dp)
                                            ) {
                                                Text(
                                                    tr("Custom", "নতুন"),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    }
                                    Text(
                                        if (isBn) desc.descBn else desc.descEn,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (customEntity != null) {
                                        IconButton(onClick = { onDeleteCustomPermissionOption(customEntity) }) {
                                            Icon(
                                                Icons.Default.DeleteOutline,
                                                contentDescription = strings.delete,
                                                tint = MaterialTheme.colorScheme.error
                                            )
                                        }
                                    }
                                    Switch(
                                        checked = isGranted,
                                        onCheckedChange = { checked ->
                                            onSetEmployeePermission(selectedEmployee, desc.key, checked)
                                        },
                                        modifier = Modifier.testTag("perm_switch_${selectedEmployee.id}_${desc.key}")
                                    )
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // Mode 2: By Option (Bulk manage 50+ employees for any single option)
            item(key = "bulk_option_selector") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        tr("1. Choose Any Option (Built-in or Newly Added)", "১. যেকোনো একটি অপশন নির্বাচন করুন"),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    OutlinedTextField(
                        value = searchOption,
                        onValueChange = { searchOption = it },
                        placeholder = { Text(tr("Filter options...", "অপশন খুঁজুন...")) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(filteredDescriptors, key = { "opt_chip_${it.key}" }) { desc ->
                            FilterChip(
                                selected = activeDescriptor.key == desc.key,
                                onClick = { selectedOptionKey = desc.key },
                                label = { Text(if (isBn) desc.titleBn else desc.titleEn) }
                            )
                        }
                    }
                }
            }

            item(key = "bulk_action_card") {
                val grantedCount = filteredEmployees.count { emp ->
                    permissionsByEmployee[emp.id]?.contains(activeDescriptor.key) == true
                }
                val activeCustomEntity = if (activeDescriptor.isCustom) {
                    customPermissionOptions.firstOrNull { it.permissionKey == activeDescriptor.key }
                } else null

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    if (isBn) activeDescriptor.titleBn else activeDescriptor.titleEn,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    if (isBn) activeDescriptor.descBn else activeDescriptor.descEn,
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Text(
                                    tr(
                                        "Granted to $grantedCount of ${filteredEmployees.size} shown employees",
                                        "প্রদর্শিত ${filteredEmployees.size} জনের মধ্যে $grantedCount জনকে অনুমতি দেওয়া আছে"
                                    ),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            if (activeCustomEntity != null) {
                                IconButton(onClick = { onDeleteCustomPermissionOption(activeCustomEntity) }) {
                                    Icon(Icons.Default.DeleteOutline, contentDescription = strings.delete, tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                enabled = filteredEmployees.isNotEmpty(),
                                onClick = {
                                    val groupedByOwner = filteredEmployees.groupBy { it.ownerUserId }
                                    groupedByOwner.forEach { (ownId, list) ->
                                        onSetPermissionForMultipleEmployees(ownId, list, activeDescriptor.key, true)
                                    }
                                },
                                modifier = Modifier.testTag("grant_bulk_button")
                            ) {
                                Icon(Icons.Default.DoneAll, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(tr("Grant to All (${filteredEmployees.size})", "সবাইকে দিন (${filteredEmployees.size})"))
                            }
                            OutlinedButton(
                                enabled = filteredEmployees.isNotEmpty(),
                                onClick = {
                                    val groupedByOwner = filteredEmployees.groupBy { it.ownerUserId }
                                    groupedByOwner.forEach { (ownId, list) ->
                                        onSetPermissionForMultipleEmployees(ownId, list, activeDescriptor.key, false)
                                    }
                                },
                                modifier = Modifier.testTag("revoke_bulk_button")
                            ) {
                                Icon(Icons.Default.RemoveDone, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.error)
                                Spacer(Modifier.width(6.dp))
                                Text(tr("Take Back All", "সবার কাছ থেকে নিন"), color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }

            item(key = "bulk_emp_search") {
                OutlinedTextField(
                    value = searchEmployee,
                    onValueChange = { searchEmployee = it },
                    placeholder = { Text(tr("Filter employees...", "কর্মচারী খুঁজুন...")) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (filteredEmployees.isEmpty()) {
                item(key = "bulk_emp_empty") {
                    EmptyStateCard(
                        icon = Icons.Default.Groups,
                        title = tr("No Employees", "কোনো কর্মচারী নেই"),
                        subtitle = tr("Add employees to assign permissions.", "অনুমতি দিতে কর্মচারী যোগ করুন।")
                    )
                }
            } else {
                items(filteredEmployees, key = { "bulk_emp_${it.id}" }) { emp ->
                    val hasOption = permissionsByEmployee[emp.id]?.contains(activeDescriptor.key) == true
                    val farmName = farmsMap[emp.farmId]?.name ?: tr("Farm", "খামার")
                    Surface(
                        shape = MaterialTheme.shapes.medium,
                        color = if (hasOption) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
                        else MaterialTheme.colorScheme.surface,
                        tonalElevation = 1.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(emp.name, fontWeight = FontWeight.Bold)
                                Text(
                                    "${emp.designation} • $farmName • ${emp.phone}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = hasOption,
                                onCheckedChange = { checked ->
                                    onSetEmployeePermission(emp, activeDescriptor.key, checked)
                                },
                                modifier = Modifier.testTag("bulk_perm_switch_${emp.id}_${activeDescriptor.key}")
                            )
                        }
                    }
                }
            }
        }
    }

    // Dialog 1: Add New Custom Option (NO LazyRow/LazyColumn inside AlertDialog!)
    if (showAddCustomOptionDialog) {
        var titleEn by remember { mutableStateOf("") }
        var titleBn by remember { mutableStateOf("") }
        var descEn by remember { mutableStateOf("") }
        var descBn by remember { mutableStateOf("") }
        val resolvedOwnerId = ownerUserId.takeIf { it > 0L }
            ?: farms.firstOrNull()?.ownerUserId
            ?: employees.firstOrNull()?.ownerUserId
            ?: 0L

        AlertDialog(
            onDismissRequest = { showAddCustomOptionDialog = false },
            title = { Text(tr("Add New Permission Option", "নতুন অনুমতি অপশন যোগ করুন")) },
            text = {
                ProvideSafeTextToolbar {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 400.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        tr(
                            "New options start OFF for all employees. Only Admin can grant or take back access.",
                            "নতুন অপশন শুরুতে সকল কর্মচারীর জন্য বন্ধ থাকবে। শুধুমাত্র অ্যাডমিন এটি দিতে বা ফিরিয়ে নিতে পারবেন।"
                        ),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = titleEn,
                        onValueChange = { titleEn = it },
                        label = { Text(tr("Option Name (English) *", "অপশনের নাম (ইংরেজি) *")) },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("custom_option_title_en_input")
                    )
                    OutlinedTextField(
                        value = titleBn,
                        onValueChange = { titleBn = it },
                        label = { Text(tr("Option Name (Bangla)", "অপশনের নাম (বাংলা)")) },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("custom_option_title_bn_input")
                    )
                    OutlinedTextField(
                        value = descEn,
                        onValueChange = { descEn = it },
                        label = { Text(tr("Description (English)", "বিবরণ (ইংরেজি)")) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = descBn,
                        onValueChange = { descBn = it },
                        label = { Text(tr("Description (Bangla)", "বিবরণ (বাংলা)")) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                }
            },
            confirmButton = {
                Button(
                    enabled = titleEn.isNotBlank() && resolvedOwnerId > 0L,
                    onClick = {
                        onAddCustomPermissionOption(resolvedOwnerId, titleEn, titleBn, descEn, descBn)
                        showAddCustomOptionDialog = false
                    },
                    modifier = Modifier.testTag("save_custom_option_button")
                ) { Text(strings.save) }
            },
            dismissButton = {
                TextButton(onClick = { showAddCustomOptionDialog = false }) { Text(strings.cancel) }
            }
        )
    }

    // Dialog 2: Setup / Reset Employee Login
    val empForLogin = loginTargetEmployee
    if (empForLogin != null) {
        var email by remember(empForLogin.id) {
            mutableStateOf(
                empForLogin.email.ifBlank {
                    empForLogin.name.lowercase().replace(Regex("[^a-z0-9]"), "") + "@smartfarm.app"
                }
            )
        }
        var mobile by remember(empForLogin.id) { mutableStateOf(empForLogin.phone) }
        var password by remember(empForLogin.id) { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { loginTargetEmployee = null },
            title = { Text("${tr("Employee Login", "কর্মচারী লগইন")}: ${empForLogin.name}") },
            text = {
                ProvideSafeTextToolbar {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 360.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        tr(
                            "Create or update sign-in credentials. Employee will ONLY see options enabled by Admin.",
                            "কর্মচারীর লগইন তথ্য সেট করুন। কর্মচারী শুধুমাত্র অ্যাডমিনের দেওয়া অপশনগুলোই দেখতে পাবেন।"
                        ),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text(tr("Login Email *", "লগইন ইমেইল *")) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = mobile,
                        onValueChange = { mobile = it },
                        label = { Text(tr("Mobile *", "মোবাইল *")) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text(tr("Password (min 4 chars) *", "পাসওয়ার্ড (কমপক্ষে ৪ অক্ষর) *")) },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                }
            },
            confirmButton = {
                Button(
                    enabled = email.isNotBlank() && mobile.isNotBlank() && password.length >= 4,
                    onClick = {
                        onProvisionEmployeeLogin(empForLogin, email, mobile, password) {
                            loginTargetEmployee = null
                        }
                    }
                ) { Text(tr("Save Login", "লগইন সংরক্ষণ")) }
            },
            dismissButton = {
                TextButton(onClick = { loginTargetEmployee = null }) { Text(strings.cancel) }
            }
        )
    }

    // Dialog 3: Quick Add Employee (NO LazyRow/LazyColumn!)
    if (showAddEmployeeDialog && onSaveEmployee != null) {
        val defaultDesignation = tr("Farm Staff", "খামার কর্মচারী")
        var farmId by remember { mutableStateOf(selectedFarmId ?: farms.firstOrNull()?.id ?: 0L) }
        var name by remember { mutableStateOf("") }
        var phone by remember { mutableStateOf("") }
        var email by remember { mutableStateOf("") }
        var designation by remember { mutableStateOf(defaultDesignation) }
        var salaryText by remember { mutableStateOf("10000") }

        AlertDialog(
            onDismissRequest = { showAddEmployeeDialog = false },
            title = { Text(strings.addEmployee) },
            text = {
                ProvideSafeTextToolbar {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 420.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (farms.isEmpty()) {
                        Text(tr("Please create a farm first.", "আগে একটি খামার তৈরি করুন।"), color = MaterialTheme.colorScheme.error)
                    } else {
                        Text(tr("Assign Farm", "খামার নির্বাচন করুন"), style = MaterialTheme.typography.labelLarge)
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            farms.forEach { f ->
                                FilterChip(
                                    selected = farmId == f.id,
                                    onClick = { farmId = f.id },
                                    label = { Text(f.name) }
                                )
                            }
                        }
                        OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text(tr("Full Name *", "পূর্ণ নাম *")) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text(tr("Mobile *", "মোবাইল *")) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text(tr("Email (optional)", "ইমেইল (ঐচ্ছিক)")) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = designation, onValueChange = { designation = it }, label = { Text(tr("Designation", "পদবি")) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(
                            value = salaryText,
                            onValueChange = { salaryText = it },
                            label = { Text("${tr("Monthly Salary", "মাসিক বেতন")} (${currency.symbol})") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
                }
            },
            confirmButton = {
                Button(
                    enabled = farms.isNotEmpty() && farmId > 0L && name.isNotBlank(),
                    onClick = {
                        onSaveEmployee(
                            EmployeeEntity(
                                farmId = farmId,
                                ownerUserId = ownerUserId,
                                name = name.trim(),
                                phone = phone.trim(),
                                email = email.trim(),
                                designation = designation.trim().ifBlank { defaultDesignation },
                                salary = CurrencyFormatter.toBaseBdt(salaryText.toDoubleOrNull() ?: 0.0, currency),
                                canManageInventory = false,
                                canManageSales = false,
                                canManageLivestock = false
                            )
                        )
                        showAddEmployeeDialog = false
                    }
                ) { Text(strings.save) }
            },
            dismissButton = { TextButton(onClick = { showAddEmployeeDialog = false }) { Text(strings.cancel) } }
        )
    }
}
