package com.example.ui.screens.farmer

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalTextToolbar
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.local.*
import com.example.i18n.AppStrings
import com.example.i18n.CurrencyFormatter
import com.example.i18n.CurrencyOption
import com.example.i18n.DomainLocalization
import com.example.i18n.isBangla
import com.example.i18n.tr
import com.example.ui.components.*
import com.example.ui.state.FarmerDashboardState
import com.example.util.DeveloperContactHelper

enum class StaffHrTab {
    TARGETS, PAYROLL, MESSAGES, TEAM
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StaffHrTargetsScreen(
    state: FarmerDashboardState,
    strings: AppStrings,
    currency: CurrencyOption,
    initialTab: StaffHrTab = StaffHrTab.TARGETS,
    onSelectFarm: (Long?) -> Unit,
    onSaveEmployee: (EmployeeEntity) -> Unit,
    onDeleteEmployee: (EmployeeEntity) -> Unit,
    onMarkAttendance: (EmployeeEntity, AttendanceStatus) -> Unit,
    onRecordEmployeePayment: (EmployeeEntity, Double, EmployeePaymentType, String, String) -> Unit,
    onDeleteSalaryPayment: (SalaryPaymentEntity) -> Unit,
    onSaveTaskTarget: (EmployeeTaskTargetEntity, () -> Unit) -> Unit,
    onUpdateTaskTargetStatus: (EmployeeTaskTargetEntity, EmployeeTaskStatus, String) -> Unit,
    onDeleteTaskTarget: (EmployeeTaskTargetEntity) -> Unit,
    onSendAutoMessage: (Long, Long, AutoMessageType, String, String, () -> Unit) -> Unit,
    onOpenPermissions: () -> Unit = {},
    onOpenSubscription: () -> Unit = {},
    onMessage: (String) -> Unit = {}
) {
    val isBn = isBangla()
    val policy = state.accessPolicy
    val subPolicy = state.subscriptionPolicy
    val canSubscribe = policy.has(AppPermission.SUBSCRIPTION_MANAGE)
    val canManageHr = policy.isAdminOwner || policy.hasAny(AppPermission.STAFF_HR_MANAGE, AppPermission.EMPLOYEES_MANAGE)
    val isEmployeeSelfView = !policy.isAdminOwner && state.currentEmployeeId != null
    val canAccessScreen = policy.isAdminOwner || policy.hasAny(
        AppPermission.STAFF_TARGETS_VIEW,
        AppPermission.STAFF_HR_MANAGE,
        AppPermission.EMPLOYEES_VIEW,
        AppPermission.EMPLOYEES_MANAGE
    )

    if (!canAccessScreen) {
        Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
            EmptyStateCard(
                icon = Icons.Default.Lock,
                title = tr("Staff HR & Targets Restricted", "কর্মচারী, বেতন ও টার্গেট দেখার অনুমতি নেই"),
                subtitle = tr(
                    "Ask your Farm Admin to enable Staff Targets or Employee access from Permissions.",
                    "অ্যাডমিন প্যানেল থেকে আপনার জন্য কর্মচারী ও টার্গেট অপশন চালু করতে বলুন।"
                )
            )
        }
        return
    }

    if (!subPolicy.canAccess(SubscriptionFeature.EMPLOYEES_PERMISSIONS) && !isEmployeeSelfView) {
        Box(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
            SubscriptionRequiredLockCard(SubscriptionFeature.EMPLOYEES_PERMISSIONS, onOpenSubscription, canSubscribe)
        }
        return
    }

    val visibleTabs = remember(canManageHr, policy) {
        buildList {
            add(StaffHrTab.TARGETS)
            add(StaffHrTab.PAYROLL)
            add(StaffHrTab.MESSAGES)
            if (canManageHr || policy.has(AppPermission.EMPLOYEES_VIEW)) {
                add(StaffHrTab.TEAM)
            }
        }
    }
    var selectedTab by remember(initialTab, visibleTabs) {
        mutableStateOf(if (initialTab in visibleTabs) initialTab else visibleTabs.first())
    }
    val selectedIdx = visibleTabs.indexOf(selectedTab).coerceAtLeast(0)

    var showAssignTargetDialog by remember { mutableStateOf(false) }
    var targetDefaultEmployee by remember { mutableStateOf<EmployeeEntity?>(null) }
    var showPaymentDialog by remember { mutableStateOf(false) }
    var paymentDefaultEmployee by remember { mutableStateOf<EmployeeEntity?>(null) }
    var paymentDefaultType by remember { mutableStateOf(EmployeePaymentType.SALARY) }
    var showMessageDialog by remember { mutableStateOf(false) }
    var messageDefaultEmployee by remember { mutableStateOf<EmployeeEntity?>(null) }
    var showAddEmployeeDialog by remember { mutableStateOf(false) }

    val focusManager = LocalFocusManager.current
    val textToolbar = LocalTextToolbar.current

    val scopedEmployees = remember(state.employees, state.selectedFarmId, state.currentEmployeeId, canManageHr) {
        val byFarm = if (state.selectedFarmId == null) state.employees else state.employees.filter { it.farmId == state.selectedFarmId }
        if (!canManageHr && state.currentEmployeeId != null) {
            byFarm.filter { it.id == state.currentEmployeeId }.ifEmpty { state.employees.filter { it.id == state.currentEmployeeId } }
        } else {
            byFarm
        }
    }

    val scopedTargets = remember(state.taskTargets, state.selectedFarmId, state.currentEmployeeId, canManageHr) {
        state.taskTargets.filter { target ->
            val farmOk = state.selectedFarmId == null || target.farmId == state.selectedFarmId
            val empOk = canManageHr || state.currentEmployeeId == null || target.employeeId == state.currentEmployeeId
            farmOk && empOk
        }.sortedWith(
            compareBy<EmployeeTaskTargetEntity>(
                { it.status == EmployeeTaskStatus.COMPLETED.key },
                { EmployeeTaskTimeSlot.fromKey(it.timeSlot).orderIndex },
                { -it.createdAt }
            )
        )
    }

    val scopedPayments = remember(state.salaryPayments, state.selectedFarmId, state.currentEmployeeId, canManageHr) {
        state.salaryPayments.filter { pay ->
            val farmOk = state.selectedFarmId == null || pay.farmId == state.selectedFarmId
            val empOk = canManageHr || state.currentEmployeeId == null || pay.employeeId == state.currentEmployeeId
            farmOk && empOk
        }
    }

    val scopedMessages = remember(state.employeeAutoMessages, state.selectedFarmId, state.currentEmployeeId, canManageHr) {
        state.employeeAutoMessages.filter { msg ->
            val farmOk = state.selectedFarmId == null || msg.farmId == 0L || msg.farmId == state.selectedFarmId
            val empOk = canManageHr || state.currentEmployeeId == null || msg.employeeId == state.currentEmployeeId
            farmOk && empOk
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = selectedIdx) {
            visibleTabs.forEach { tab ->
                val title = when (tab) {
                    StaffHrTab.TARGETS -> tr("Targets", "টার্গেট")
                    StaffHrTab.PAYROLL -> tr("Pay & Bonus", "বেতন ও বোনাস")
                    StaffHrTab.MESSAGES -> tr("Auto-Msg", "অটো-মেসেজ")
                    StaffHrTab.TEAM -> tr("Team", "কর্মচারী")
                }
                val tag = when (tab) {
                    StaffHrTab.TARGETS -> "staff_hr_tab_targets"
                    StaffHrTab.PAYROLL -> "staff_hr_tab_payroll"
                    StaffHrTab.MESSAGES -> "staff_hr_tab_messages"
                    StaffHrTab.TEAM -> "staff_hr_tab_team"
                }
                Tab(
                    selected = selectedTab == tab,
                    onClick = { selectedTab = tab },
                    text = { Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    modifier = Modifier.testTag(tag)
                )
            }
        }

        when (selectedTab) {
            StaffHrTab.TARGETS -> TargetsTabContent(
                state = state,
                targets = scopedTargets,
                employees = scopedEmployees,
                strings = strings,
                currency = currency,
                canManageHr = canManageHr,
                isBn = isBn,
                onSelectFarm = onSelectFarm,
                onOpenAssignTarget = { emp ->
                    targetDefaultEmployee = emp ?: scopedEmployees.firstOrNull()
                    showAssignTargetDialog = true
                },
                onUpdateTaskStatus = onUpdateTaskTargetStatus,
                onDeleteTaskTarget = onDeleteTaskTarget
            )

            StaffHrTab.PAYROLL -> PayrollAndExpensesTabContent(
                state = state,
                payments = scopedPayments,
                employees = scopedEmployees,
                strings = strings,
                currency = currency,
                canManageHr = canManageHr,
                isBn = isBn,
                onSelectFarm = onSelectFarm,
                onOpenPaymentDialog = { type, emp ->
                    paymentDefaultType = type
                    paymentDefaultEmployee = emp ?: scopedEmployees.firstOrNull()
                    showPaymentDialog = true
                },
                onDeleteSalaryPayment = onDeleteSalaryPayment
            )

            StaffHrTab.MESSAGES -> AutoMessagesTabContent(
                state = state,
                messages = scopedMessages,
                employees = scopedEmployees,
                strings = strings,
                canManageHr = canManageHr,
                isBn = isBn,
                onSelectFarm = onSelectFarm,
                onSendQuickTemplate = { emp, msgType, subject, body ->
                    onSendAutoMessage(emp.farmId, emp.id, msgType, subject, body) {}
                },
                onOpenComposeDialog = { emp ->
                    messageDefaultEmployee = emp ?: scopedEmployees.firstOrNull()
                    showMessageDialog = true
                },
                onMessage = onMessage
            )

            StaffHrTab.TEAM -> TeamDirectoryTabContent(
                state = state,
                employees = scopedEmployees,
                targets = scopedTargets,
                payments = scopedPayments,
                strings = strings,
                currency = currency,
                canManageHr = canManageHr,
                canManagePermissions = policy.canManagePermissions(),
                isBn = isBn,
                onSelectFarm = onSelectFarm,
                onOpenAddEmployee = { showAddEmployeeDialog = true },
                onMarkAttendance = onMarkAttendance,
                onDeleteEmployee = onDeleteEmployee,
                onOpenPayment = { emp, type ->
                    paymentDefaultEmployee = emp
                    paymentDefaultType = type
                    showPaymentDialog = true
                },
                onOpenAssignTarget = { emp ->
                    targetDefaultEmployee = emp
                    showAssignTargetDialog = true
                },
                onOpenSendMessage = { emp ->
                    messageDefaultEmployee = emp
                    showMessageDialog = true
                },
                onOpenPermissions = onOpenPermissions
            )
        }
    }

    if (showAssignTargetDialog) {
        AssignTargetDialog(
            farms = state.farms,
            employees = state.employees,
            initialEmployee = targetDefaultEmployee,
            currency = currency,
            strings = strings,
            isBn = isBn,
            onDismiss = {
                textToolbar.hide()
                focusManager.clearFocus(force = true)
                showAssignTargetDialog = false
            },
            onSave = { target ->
                textToolbar.hide()
                focusManager.clearFocus(force = true)
                onSaveTaskTarget(target) {
                    showAssignTargetDialog = false
                }
            }
        )
    }

    if (showPaymentDialog) {
        RecordStaffPaymentDialog(
            employees = state.employees,
            farms = state.farms,
            initialEmployee = paymentDefaultEmployee,
            initialType = paymentDefaultType,
            currency = currency,
            strings = strings,
            isBn = isBn,
            onDismiss = {
                textToolbar.hide()
                focusManager.clearFocus(force = true)
                showPaymentDialog = false
            },
            onConfirm = { emp, amountBase, payType, periodLabel, note ->
                textToolbar.hide()
                focusManager.clearFocus(force = true)
                onRecordEmployeePayment(emp, amountBase, payType, periodLabel, note)
                showPaymentDialog = false
            }
        )
    }

    if (showMessageDialog) {
        SendAutoMessageDialog(
            employees = scopedEmployees,
            farms = state.farms,
            initialEmployee = messageDefaultEmployee,
            isEmployeeSender = isEmployeeSelfView,
            strings = strings,
            isBn = isBn,
            onDismiss = {
                textToolbar.hide()
                focusManager.clearFocus(force = true)
                showMessageDialog = false
            },
            onSend = { farmId, empId, msgType, subj, body ->
                textToolbar.hide()
                focusManager.clearFocus(force = true)
                onSendAutoMessage(farmId, empId, msgType, subj, body) {
                    showMessageDialog = false
                }
            }
        )
    }

    if (showAddEmployeeDialog) {
        AddStaffEmployeeDialog(
            farms = state.farms,
            defaultFarmId = state.selectedFarmId ?: state.farms.firstOrNull()?.id ?: 0L,
            currency = currency,
            strings = strings,
            onDismiss = {
                textToolbar.hide()
                focusManager.clearFocus(force = true)
                showAddEmployeeDialog = false
            },
            onSave = { emp ->
                textToolbar.hide()
                focusManager.clearFocus(force = true)
                onSaveEmployee(emp)
                showAddEmployeeDialog = false
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TargetsTabContent(
    state: FarmerDashboardState,
    targets: List<EmployeeTaskTargetEntity>,
    employees: List<EmployeeEntity>,
    strings: AppStrings,
    currency: CurrencyOption,
    canManageHr: Boolean,
    isBn: Boolean,
    onSelectFarm: (Long?) -> Unit,
    onOpenAssignTarget: (EmployeeEntity?) -> Unit,
    onUpdateTaskStatus: (EmployeeTaskTargetEntity, EmployeeTaskStatus, String) -> Unit,
    onDeleteTaskTarget: (EmployeeTaskTargetEntity) -> Unit
) {
    val farmsMap = remember(state.farms) { state.farms.associateBy { it.id } }
    var slotFilter by remember { mutableStateOf<String?>(null) }
    val displayed = remember(targets, slotFilter) {
        if (slotFilter == null) targets else targets.filter { it.timeSlot == slotFilter }
    }
    val completedCount = targets.count { it.status == EmployeeTaskStatus.COMPLETED.key }
    val creditedBonusSum = targets.filter { it.rewardCredited }.sumOf { it.rewardBonusAmount }

    Scaffold(
        floatingActionButton = {
            if (canManageHr) {
                ExtendedFloatingActionButton(
                    onClick = { onOpenAssignTarget(employees.firstOrNull()) },
                    icon = { Icon(Icons.Default.TrackChanges, contentDescription = null) },
                    text = { Text(tr("Assign Target", "টার্গেট দিন")) },
                    modifier = Modifier.testTag("assign_target_fab")
                )
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 88.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item(key = "targets_header") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        tr("Morning-to-Night Task Targets & Cash Rewards", "সকাল থেকে রাত পর্যন্ত কাজের টার্গেট ও পুরস্কার"),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    if (canManageHr) {
                        FarmSwitcherBar(state.farms, state.selectedFarmId, strings.allFarms, onSelectFarm)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        KpiMetricCard(
                            title = tr("Active Targets", "চলমান টার্গেট"),
                            value = "${targets.size - completedCount} / ${targets.size}",
                            icon = Icons.Default.Schedule,
                            subtitle = "$completedCount ${tr("completed", "সম্পন্ন")}",
                            modifier = Modifier.weight(1f)
                        )
                        KpiMetricCard(
                            title = tr("Rewards Credited", "টার্গেট বোনাস"),
                            value = CurrencyFormatter.formatCompact(creditedBonusSum, currency),
                            icon = Icons.Default.EmojiEvents,
                            tint = MaterialTheme.colorScheme.primary,
                            subtitle = tr("Auto-added on completion", "সম্পন্ন হলেই স্বয়ংক্রিয় যোগ"),
                            modifier = Modifier.weight(1f)
                        )
                    }
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        item(key = "slot_all") {
                            FilterChip(
                                selected = slotFilter == null,
                                onClick = { slotFilter = null },
                                label = { Text(tr("All Shifts", "সকাল-রাত সব")) }
                            )
                        }
                        items(EmployeeTaskTimeSlot.entries.toList(), key = { it.key }) { slot ->
                            FilterChip(
                                selected = slotFilter == slot.key,
                                onClick = { slotFilter = if (slotFilter == slot.key) null else slot.key },
                                label = { Text(DomainLocalization.taskTimeSlot(slot.key, isBn)) }
                            )
                        }
                    }
                }
            }

            if (displayed.isEmpty()) {
                item(key = "targets_empty") {
                    EmptyStateCard(
                        icon = Icons.Default.TrackChanges,
                        title = tr("No Task Targets Assigned Yet", "এখনও কোনো টার্গেট দেওয়া হয়নি"),
                        subtitle = if (canManageHr) {
                            tr(
                                "Assign Morning, Afternoon, Evening, or Night targets with a ৳10, ৳20 or custom bonus. Completing the target automatically credits the bonus & sends an alert.",
                                "কর্মচারীর জন্য সকাল থেকে রাতের টার্গেট ও ১০/২০ টাকা বা কাস্টম বোনাস সেট করুন। টার্গেট সম্পন্ন করলেই বোনাস অটো যোগ হবে।"
                            )
                        } else {
                            tr(
                                "Your Morning-to-Night task targets assigned by the farm owner will appear here.",
                                "খামার মালিক আপনার জন্য যে সকাল থেকে রাতের টার্গেট দেবেন তা এখানে দেখা যাবে।"
                            )
                        },
                        actionLabel = if (canManageHr && employees.isNotEmpty()) tr("Assign Target", "টার্গেট দিন") else null,
                        onAction = if (canManageHr && employees.isNotEmpty()) ({ onOpenAssignTarget(employees.firstOrNull()) }) else null
                    )
                }
            } else {
                items(displayed, key = { "target_${it.id}" }) { target ->
                    val farmName = farmsMap[target.farmId]?.name ?: tr("Farm", "খামার")
                    val statusEnum = EmployeeTaskStatus.fromKey(target.status)
                    val slotLabel = DomainLocalization.taskTimeSlot(target.timeSlot, isBn)
                    val isOwnerOrAssigned = canManageHr || state.currentEmployeeId == target.employeeId

                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = if (statusEnum == EmployeeTaskStatus.COMPLETED)
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                        else MaterialTheme.colorScheme.surface,
                        tonalElevation = 2.dp,
                        modifier = Modifier.fillMaxWidth().testTag("task_target_card_${target.id}")
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Top
                            ) {
                                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                    Surface(
                                        shape = RoundedCornerShape(50.dp),
                                        color = MaterialTheme.colorScheme.secondaryContainer
                                    ) {
                                        Text(
                                            slotLabel,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }
                                    Text(target.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    Text(
                                        "${target.employeeName} • $farmName",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Surface(
                                        shape = RoundedCornerShape(50.dp),
                                        color = MaterialTheme.colorScheme.primaryContainer
                                    ) {
                                        Text(
                                            "+${CurrencyFormatter.format(target.rewardBonusAmount, currency)}",
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                        )
                                    }
                                    Text(
                                        DomainLocalization.taskStatus(target.status, isBn),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (statusEnum == EmployeeTaskStatus.COMPLETED) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.tertiary
                                    )
                                }
                            }

                            if (target.targetGoalText.isNotBlank()) {
                                Text(
                                    "${tr("Goal", "লক্ষ্য")}: ${target.targetGoalText}",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            if (target.description.isNotBlank()) {
                                Text(target.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            if (target.rewardCredited) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Icon(Icons.Default.Verified, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                    Text(
                                        tr("Bonus credited & automated notification sent", "বোনাস অ্যাকাউন্টে যোগ হয়েছে ও অটো-মেসেজ পাঠানো হয়েছে"),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (isOwnerOrAssigned && statusEnum != EmployeeTaskStatus.COMPLETED) {
                                    if (statusEnum == EmployeeTaskStatus.PENDING) {
                                        OutlinedButton(
                                            onClick = { onUpdateTaskStatus(target, EmployeeTaskStatus.IN_PROGRESS, "") },
                                            modifier = Modifier.heightIn(min = 48.dp)
                                        ) {
                                            Text(tr("Start Task", "শুরু করুন"))
                                        }
                                    }
                                    val defaultCompletionNote = tr("Completed target on time", "সময়মতো টার্গেট সম্পন্ন হয়েছে")
                                    Button(
                                        onClick = {
                                            onUpdateTaskStatus(
                                                target,
                                                EmployeeTaskStatus.COMPLETED,
                                                defaultCompletionNote
                                            )
                                        },
                                        modifier = Modifier
                                            .weight(1f)
                                            .heightIn(min = 48.dp)
                                            .testTag("complete_target_button_${target.id}")
                                    ) {
                                        Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text(
                                            tr(
                                                "Complete (+${CurrencyFormatter.format(target.rewardBonusAmount, currency)})",
                                                "সম্পন্ন (+${CurrencyFormatter.format(target.rewardBonusAmount, currency)})"
                                            ),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                                if (canManageHr) {
                                    IconButton(
                                        onClick = { onDeleteTaskTarget(target) },
                                        modifier = Modifier.testTag("delete_target_${target.id}")
                                    ) {
                                        Icon(Icons.Default.DeleteOutline, contentDescription = strings.delete, tint = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PayrollAndExpensesTabContent(
    state: FarmerDashboardState,
    payments: List<SalaryPaymentEntity>,
    employees: List<EmployeeEntity>,
    strings: AppStrings,
    currency: CurrencyOption,
    canManageHr: Boolean,
    isBn: Boolean,
    onSelectFarm: (Long?) -> Unit,
    onOpenPaymentDialog: (EmployeePaymentType, EmployeeEntity?) -> Unit,
    onDeleteSalaryPayment: (SalaryPaymentEntity) -> Unit
) {
    val empById = remember(state.employees) { state.employees.associateBy { it.id } }
    val farmsMap = remember(state.farms) { state.farms.associateBy { it.id } }
    var typeFilter by remember { mutableStateOf<String?>(null) }

    val totalSalary = payments.filter { it.paymentType == EmployeePaymentType.SALARY.key }.sumOf { it.amount }
    val totalBonus = payments.filter {
        it.paymentType == EmployeePaymentType.BONUS.key || it.paymentType == EmployeePaymentType.TARGET_REWARD.key
    }.sumOf { it.amount }
    val totalEmpExpense = payments.filter { it.paymentType == EmployeePaymentType.EXPENSE.key }.sumOf { it.amount }

    val displayed = remember(payments, typeFilter) {
        if (typeFilter == null) payments else payments.filter { it.paymentType == typeFilter }
    }

    Scaffold(
        floatingActionButton = {
            if (canManageHr && employees.isNotEmpty()) {
                ExtendedFloatingActionButton(
                    onClick = { onOpenPaymentDialog(EmployeePaymentType.SALARY, employees.firstOrNull()) },
                    icon = { Icon(Icons.Default.Payments, contentDescription = null) },
                    text = { Text(tr("Pay / Bonus / Expense", "বেতন / বোনাস / খরচ")) },
                    modifier = Modifier.testTag("record_staff_pay_fab")
                )
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 88.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item(key = "payroll_header") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        tr("Employee Salary, Bonus & Expense Ledger", "কর্মচারী বেতন, বোনাস ও খরচের খতিয়ান"),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    if (canManageHr) {
                        FarmSwitcherBar(state.farms, state.selectedFarmId, strings.allFarms, onSelectFarm)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        KpiMetricCard(
                            title = tr("Salary", "বেতন"),
                            value = CurrencyFormatter.formatCompact(totalSalary, currency),
                            icon = Icons.Default.AccountBalanceWallet,
                            modifier = Modifier.weight(1f)
                        )
                        KpiMetricCard(
                            title = tr("Bonuses", "বোনাস"),
                            value = CurrencyFormatter.formatCompact(totalBonus, currency),
                            icon = Icons.Default.EmojiEvents,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.weight(1f)
                        )
                        KpiMetricCard(
                            title = tr("Expenses", "অন্যান্য খরচ"),
                            value = CurrencyFormatter.formatCompact(totalEmpExpense, currency),
                            icon = Icons.Default.ReceiptLong,
                            tint = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    if (canManageHr && employees.isNotEmpty()) {
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            FilledTonalButton(
                                onClick = { onOpenPaymentDialog(EmployeePaymentType.SALARY, employees.firstOrNull()) },
                                modifier = Modifier.testTag("quick_pay_salary_button")
                            ) { Text(tr("+ Pay Salary", "+ বেতন দিন")) }
                            FilledTonalButton(
                                onClick = { onOpenPaymentDialog(EmployeePaymentType.BONUS, employees.firstOrNull()) },
                                modifier = Modifier.testTag("quick_give_bonus_button")
                            ) { Text(tr("+ Give Bonus", "+ বোনাস দিন")) }
                            OutlinedButton(
                                onClick = { onOpenPaymentDialog(EmployeePaymentType.EXPENSE, employees.firstOrNull()) },
                                modifier = Modifier.testTag("quick_staff_expense_button")
                            ) { Text(tr("+ Employee Expense", "+ কর্মচারী খরচ")) }
                        }
                    }
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        item(key = "pay_all") {
                            FilterChip(
                                selected = typeFilter == null,
                                onClick = { typeFilter = null },
                                label = { Text(tr("All (${payments.size})", "সব (${payments.size})")) }
                            )
                        }
                        items(EmployeePaymentType.entries.toList(), key = { it.key }) { pt ->
                            FilterChip(
                                selected = typeFilter == pt.key,
                                onClick = { typeFilter = if (typeFilter == pt.key) null else pt.key },
                                label = { Text(DomainLocalization.employeePaymentType(pt.key, isBn)) }
                            )
                        }
                    }
                }
            }

            if (displayed.isEmpty()) {
                item(key = "payroll_empty") {
                    EmptyStateCard(
                        icon = Icons.Default.Payments,
                        title = tr("No Staff Payments or Bonuses Yet", "এখনও কোনো বেতন, বোনাস বা খরচ যোগ হয়নি"),
                        subtitle = tr(
                            "Salaries, bonuses, employee expenses, and automatic target rewards appear here cleanly in one place.",
                            "বেতন, বোনাস, কর্মচারী খরচ এবং টার্গেট পূরণ বোনাস সব একসাথে এখানে সংরক্ষিত থাকবে।"
                        )
                    )
                }
            } else {
                items(displayed, key = { "pay_${it.id}" }) { pay ->
                    val empName = empById[pay.employeeId]?.name ?: tr("Staff #${pay.employeeId}", "কর্মচারী #${pay.employeeId}")
                    val farmName = farmsMap[pay.farmId]?.name ?: tr("Farm", "খামার")
                    val typeLabel = DomainLocalization.employeePaymentType(pay.paymentType, isBn)
                    val isBonus = pay.paymentType == EmployeePaymentType.BONUS.key || pay.paymentType == EmployeePaymentType.TARGET_REWARD.key

                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 2.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        shape = RoundedCornerShape(50.dp),
                                        color = if (isBonus) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer
                                    ) {
                                        Text(
                                            typeLabel,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                        )
                                    }
                                    Text(empName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                }
                                Text("$farmName • ${pay.monthLabel}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                if (pay.note.isNotBlank()) {
                                    Text(pay.note, style = MaterialTheme.typography.bodySmall)
                                }
                                Text(formatShortDate(pay.paymentDate), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    CurrencyFormatter.format(pay.amount, currency),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isBonus) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                )
                                if (canManageHr) {
                                    IconButton(onClick = { onDeleteSalaryPayment(pay) }) {
                                        Icon(Icons.Default.DeleteOutline, contentDescription = strings.delete, tint = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AutoMessagesTabContent(
    state: FarmerDashboardState,
    messages: List<EmployeeAutoMessageEntity>,
    employees: List<EmployeeEntity>,
    strings: AppStrings,
    canManageHr: Boolean,
    isBn: Boolean,
    onSelectFarm: (Long?) -> Unit,
    onSendQuickTemplate: (EmployeeEntity, AutoMessageType, String, String) -> Unit,
    onOpenComposeDialog: (EmployeeEntity?) -> Unit,
    onMessage: (String) -> Unit
) {
    val context = LocalContext.current
    val empById = remember(state.employees) { state.employees.associateBy { it.id } }

    Scaffold(
        floatingActionButton = {
            if (employees.isNotEmpty()) {
                ExtendedFloatingActionButton(
                    onClick = { onOpenComposeDialog(employees.firstOrNull()) },
                    icon = { Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null) },
                    text = { Text(tr("Auto-Message", "অটো-মেসেজ পাঠান")) },
                    modifier = Modifier.testTag("send_auto_msg_fab")
                )
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 88.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item(key = "msg_header") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        tr("Automated Staff Messaging System", "অটোমেটেড কর্মচারী মেসেজিং সিস্টেম"),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    if (canManageHr) {
                        FarmSwitcherBar(state.farms, state.selectedFarmId, strings.allFarms, onSelectFarm)
                    }
                    val activeEmp = employees.firstOrNull()
                    if (activeEmp != null) {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    tr(
                                        "1-Tap Quick Automated Updates (${activeEmp.name})",
                                        "১-ক্লিকে অটোমেটেড আপডেট পাঠান (${activeEmp.name})"
                                    ),
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                val morningSubject = tr("Morning Shift Check-In", "সকালের শিফট হাজিরা")
                                val morningBody = tr(
                                    "${activeEmp.name} started morning shift and began Morning targets.",
                                    "${activeEmp.name} সকালের শিফটে যোগ দিয়েছেন এবং সকালের কাজ শুরু করেছেন।"
                                )
                                val middaySubject = tr("Midday Farm Update", "দুপুরের কাজের আপডেট")
                                val middayBody = tr(
                                    "${activeEmp.name}: Feed, water, and routine inspection completed.",
                                    "${activeEmp.name}: দুপুরের খাদ্য, পানি ও তদারকি সম্পন্ন হয়েছে।"
                                )
                                val nightSubject = tr("Night Closing Report", "রাতের সমাপনী রিপোর্ট")
                                val nightBody = tr(
                                    "${activeEmp.name}: Night lockup done; all animals and stock secured.",
                                    "${activeEmp.name}: রাতের নিরাপত্তা ও তালাবদ্ধ তদারকি সম্পন্ন হয়েছে।"
                                )
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    FilledTonalButton(
                                        onClick = {
                                            onSendQuickTemplate(
                                                activeEmp,
                                                AutoMessageType.MORNING_CHECKIN,
                                                morningSubject,
                                                morningBody
                                            )
                                        },
                                        modifier = Modifier.testTag("quick_msg_morning")
                                    ) {
                                        Text(tr("Morning Check-In", "সকালের হাজিরা"))
                                    }
                                    FilledTonalButton(
                                        onClick = {
                                            onSendQuickTemplate(
                                                activeEmp,
                                                AutoMessageType.MIDDAY_UPDATE,
                                                middaySubject,
                                                middayBody
                                            )
                                        },
                                        modifier = Modifier.testTag("quick_msg_midday")
                                    ) {
                                        Text(tr("Midday Update", "দুপুরের আপডেট"))
                                    }
                                    FilledTonalButton(
                                        onClick = {
                                            onSendQuickTemplate(
                                                activeEmp,
                                                AutoMessageType.NIGHT_REPORT,
                                                nightSubject,
                                                nightBody
                                            )
                                        },
                                        modifier = Modifier.testTag("quick_msg_night")
                                    ) {
                                        Text(tr("Night Report", "রাতের রিপোর্ট"))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (messages.isEmpty()) {
                item(key = "msg_empty") {
                    EmptyStateCard(
                        icon = Icons.Default.MarkChatRead,
                        title = tr("No Automated Messages Yet", "এখনও কোনো অটোমেটেড বার্তা নেই"),
                        subtitle = tr(
                            "Every target assignment, completion bonus, or employee shift report generates a tracked automated message here.",
                            "প্রতিটি টার্গেট নির্ধারণ, টার্গেট পূরণ বোনাস বা শিফট রিপোর্টের অটোমেটেড বার্তা এখানে জমা হবে।"
                        )
                    )
                }
            } else {
                items(messages, key = { "automsg_${it.id}" }) { msg ->
                    val targetEmp = empById[msg.employeeId]
                    val smsUnavailableMsg = tr("No SMS app available on device", "ডিভাইসে SMS অ্যাপ পাওয়া যায়নি")
                    val whatsappUnavailableMsg = tr("Could not open WhatsApp", "হোয়াটসঅ্যাপ খোলা যায়নি")

                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 2.dp,
                        modifier = Modifier.fillMaxWidth().testTag("auto_msg_card_${msg.id}")
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(50.dp),
                                    color = MaterialTheme.colorScheme.secondaryContainer
                                ) {
                                    Text(
                                        DomainLocalization.autoMessageType(msg.messageType, isBn),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                                Text(
                                    formatShortDate(msg.sentAt),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(msg.subject, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Text(msg.messageBody, style = MaterialTheme.typography.bodyMedium)
                            Text(
                                "${tr("From", "প্রেরক")}: ${msg.senderName} → ${msg.recipientLabel}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                            val phoneTarget = targetEmp?.phone.orEmpty()
                            if (phoneTarget.isNotBlank()) {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedButton(
                                        onClick = {
                                            val ok = DeveloperContactHelper.launchWhatsAppDeveloper(
                                                context = context,
                                                whatsAppNumber = phoneTarget,
                                                prefillMessage = "${msg.subject}\n${msg.messageBody}"
                                            )
                                            if (!ok) onMessage(whatsappUnavailableMsg)
                                        }
                                    ) {
                                        Text(tr("WhatsApp", "হোয়াটসঅ্যাপ"))
                                    }
                                    OutlinedButton(
                                        onClick = {
                                            val dial = DeveloperContactHelper.normalizePhoneForDial(phoneTarget)
                                            val smsIntent = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:$dial")).apply {
                                                putExtra("sms_body", "${msg.subject}: ${msg.messageBody}")
                                                if (context !is Activity) addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                            }
                                            val launched = runCatching { context.startActivity(smsIntent); true }.getOrDefault(false)
                                            if (!launched) onMessage(smsUnavailableMsg)
                                        }
                                    ) {
                                        Text(tr("SMS App", "এসএমএস"))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TeamDirectoryTabContent(
    state: FarmerDashboardState,
    employees: List<EmployeeEntity>,
    targets: List<EmployeeTaskTargetEntity>,
    payments: List<SalaryPaymentEntity>,
    strings: AppStrings,
    currency: CurrencyOption,
    canManageHr: Boolean,
    canManagePermissions: Boolean,
    isBn: Boolean,
    onSelectFarm: (Long?) -> Unit,
    onOpenAddEmployee: () -> Unit,
    onMarkAttendance: (EmployeeEntity, AttendanceStatus) -> Unit,
    onDeleteEmployee: (EmployeeEntity) -> Unit,
    onOpenPayment: (EmployeeEntity, EmployeePaymentType) -> Unit,
    onOpenAssignTarget: (EmployeeEntity) -> Unit,
    onOpenSendMessage: (EmployeeEntity) -> Unit,
    onOpenPermissions: () -> Unit
) {
    val farmsMap = remember(state.farms) { state.farms.associateBy { it.id } }
    val permCounts = remember(state.employeePermissions) {
        state.employeePermissions.filter { it.granted }.groupingBy { it.employeeId }.eachCount()
    }

    Scaffold(
        floatingActionButton = {
            if (canManageHr) {
                ExtendedFloatingActionButton(
                    onClick = onOpenAddEmployee,
                    icon = { Icon(Icons.Default.PersonAdd, contentDescription = strings.addEmployee) },
                    text = { Text(strings.addEmployee) },
                    modifier = Modifier.testTag("staff_hr_add_employee_fab")
                )
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 88.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item(key = "team_header") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        tr("Farm Staff, Attendance & Permissions", "কর্মচারী তালিকা, উপস্থিতি ও অনুমতি"),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    FarmSwitcherBar(state.farms, state.selectedFarmId, strings.allFarms, onSelectFarm)
                    if (canManagePermissions) {
                        FilledTonalButton(
                            onClick = onOpenPermissions,
                            modifier = Modifier.fillMaxWidth().testTag("employees_manage_permissions_button")
                        ) {
                            Icon(Icons.Default.AdminPanelSettings, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(tr("Manage User Permissions & Logins", "কর্মচারী লগইন ও পারমিশন পরিচালনা"))
                        }
                    }
                }
            }

            if (employees.isEmpty()) {
                item(key = "team_empty") {
                    EmptyStateCard(
                        icon = Icons.Default.Groups,
                        title = tr("No Employees Yet", "কোনো কর্মচারী নেই"),
                        subtitle = tr(
                            "Add staff members here to assign Morning-to-Night targets, salary, bonuses, and automated messaging.",
                            "সকাল থেকে রাতের টার্গেট, বেতন, বোনাস ও অটোমেটেড মেসেজের জন্য কর্মচারী যোগ করুন।"
                        ),
                        actionLabel = if (canManageHr) strings.addEmployee else null,
                        onAction = if (canManageHr) onOpenAddEmployee else null
                    )
                }
            } else {
                items(employees, key = { "hr_emp_${it.id}" }) { emp ->
                    val farmName = farmsMap[emp.farmId]?.name ?: tr("Farm", "খামার")
                    val totalPaid = payments.filter { it.employeeId == emp.id }.sumOf { it.amount }
                    val pendingTargets = targets.count { it.employeeId == emp.id && it.status != EmployeeTaskStatus.COMPLETED.key }
                    val grantedCount = permCounts[emp.id] ?: 0

                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 2.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(emp.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    Text(
                                        "${emp.designation} • $farmName • ${emp.phone}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                if (canManageHr) {
                                    IconButton(onClick = { onDeleteEmployee(emp) }) {
                                        Icon(Icons.Default.DeleteOutline, contentDescription = strings.delete, tint = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                            Text(
                                "${tr("Monthly Salary", "মাসিক বেতন")}: ${CurrencyFormatter.format(emp.salary, currency)} • ${tr("Paid/Bonus", "প্রদত্ত")}: ${CurrencyFormatter.format(totalPaid, currency)} • ${tr("Open Targets", "বাকি টার্গেট")}: $pendingTargets • ${tr("Permissions", "অনুমতি")}: $grantedCount",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                AttendanceStatus.entries.forEach { st ->
                                    FilterChip(
                                        selected = emp.lastAttendanceStatus == st.name,
                                        enabled = canManageHr,
                                        onClick = { if (canManageHr) onMarkAttendance(emp, st) },
                                        label = { Text(DomainLocalization.attendanceStatus(st, isBn)) }
                                    )
                                }
                            }
                            if (canManageHr) {
                                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    FilledTonalButton(onClick = { onOpenAssignTarget(emp) }) {
                                        Text(tr("Assign Target", "টার্গেট দিন"))
                                    }
                                    FilledTonalButton(onClick = { onOpenPayment(emp, EmployeePaymentType.SALARY) }) {
                                        Text(tr("Pay / Bonus", "বেতন / বোনাস"))
                                    }
                                    OutlinedButton(onClick = { onOpenSendMessage(emp) }) {
                                        Text(tr("Message", "মেসেজ"))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AssignTargetDialog(
    farms: List<FarmEntity>,
    employees: List<EmployeeEntity>,
    initialEmployee: EmployeeEntity?,
    currency: CurrencyOption,
    strings: AppStrings,
    isBn: Boolean,
    onDismiss: () -> Unit,
    onSave: (EmployeeTaskTargetEntity) -> Unit
) {
    var selectedEmployeeId by remember { mutableStateOf(initialEmployee?.id ?: employees.firstOrNull()?.id ?: 0L) }
    val selectedEmp = employees.firstOrNull { it.id == selectedEmployeeId } ?: employees.firstOrNull()
    var timeSlot by remember { mutableStateOf(EmployeeTaskTimeSlot.MORNING.key) }
    var title by remember { mutableStateOf("") }
    var goalText by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var bonusText by remember { mutableStateOf("10") }
    val bonusAmt = bonusText.toDoubleOrNull() ?: 0.0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(tr("Assign Morning-to-Night Target", "সকাল-রাতের কাজের টার্গেট দিন")) },
        text = {
            ProvideSafeTextToolbar {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 460.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (employees.isEmpty()) {
                        Text(tr("Add an employee first.", "আগে একজন কর্মচারী যোগ করুন।"), color = MaterialTheme.colorScheme.error)
                    } else {
                        Text(tr("Select Employee *", "কর্মচারী নির্বাচন করুন *"), style = MaterialTheme.typography.labelLarge)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            employees.forEach { emp ->
                                FilterChip(
                                    selected = selectedEmployeeId == emp.id,
                                    onClick = { selectedEmployeeId = emp.id },
                                    label = { Text(emp.name) }
                                )
                            }
                        }

                        Text(tr("Time Slot (Morning to Night) *", "সময়কাল (সকাল থেকে রাত) *"), style = MaterialTheme.typography.labelLarge)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            EmployeeTaskTimeSlot.entries.forEach { slot ->
                                FilterChip(
                                    selected = timeSlot == slot.key,
                                    onClick = { timeSlot = slot.key },
                                    label = { Text(DomainLocalization.taskTimeSlot(slot.key, isBn)) }
                                )
                            }
                        }

                        OutlinedTextField(
                            value = title,
                            onValueChange = { title = it },
                            label = { Text(tr("Task / Target Title *", "টার্গেটের নাম / কাজ *")) },
                            placeholder = { Text(tr("e.g., Morning egg collection & shed clean", "যেমন: সকালের ডিম সংগ্রহ ও ঘর পরিষ্কার")) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("target_title_input")
                        )
                        OutlinedTextField(
                            value = goalText,
                            onValueChange = { goalText = it },
                            label = { Text(tr("Daily Target Goal", "দৈনিক লক্ষ্যমাত্রা")) },
                            placeholder = { Text(tr("e.g., 100% trays sorted by 9 AM", "যেমন: সকাল ৯টার মধ্যে সব ট্রে গোছানো")) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = bonusText,
                            onValueChange = { bonusText = it },
                            label = { Text("${tr("Target Completion Reward *", "টার্গেট পূরণ বোনাস *")} (${currency.symbol})") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("target_bonus_input")
                        )
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf("10", "20", "50", "100").forEach { quick ->
                                FilterChip(
                                    selected = bonusText.trim() == quick,
                                    onClick = { bonusText = quick },
                                    label = { Text("+${currency.symbol}$quick") }
                                )
                            }
                        }
                        OutlinedTextField(
                            value = description,
                            onValueChange = { description = it },
                            label = { Text(tr("Instructions / Notes", "নির্দেশনা ও বিবরণ")) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                enabled = selectedEmp != null && title.isNotBlank() && bonusAmt >= 0.0,
                onClick = {
                    if (selectedEmp != null) {
                        val baseReward = CurrencyFormatter.toBaseBdt(bonusAmt.coerceAtLeast(0.0), currency)
                        onSave(
                            EmployeeTaskTargetEntity(
                                ownerUserId = selectedEmp.ownerUserId,
                                farmId = selectedEmp.farmId,
                                employeeId = selectedEmp.id,
                                employeeName = selectedEmp.name,
                                timeSlot = timeSlot,
                                title = title.trim(),
                                description = description.trim(),
                                targetGoalText = goalText.trim(),
                                rewardBonusAmount = baseReward
                            )
                        )
                    }
                },
                modifier = Modifier.testTag("save_target_button")
            ) { Text(strings.save) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(strings.cancel) }
        }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RecordStaffPaymentDialog(
    employees: List<EmployeeEntity>,
    farms: List<FarmEntity>,
    initialEmployee: EmployeeEntity?,
    initialType: EmployeePaymentType,
    currency: CurrencyOption,
    strings: AppStrings,
    isBn: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (EmployeeEntity, Double, EmployeePaymentType, String, String) -> Unit
) {
    var selectedEmployeeId by remember { mutableStateOf(initialEmployee?.id ?: employees.firstOrNull()?.id ?: 0L) }
    val selectedEmp = employees.firstOrNull { it.id == selectedEmployeeId } ?: employees.firstOrNull()
    var paymentType by remember { mutableStateOf(initialType) }
    var amountText by remember(paymentType, selectedEmp?.id) {
        mutableStateOf(
            when (paymentType) {
                EmployeePaymentType.SALARY -> selectedEmp?.salary?.let {
                    CurrencyFormatter.convertFromBase(it, currency).toInt().toString()
                } ?: "10000"
                EmployeePaymentType.BONUS -> "500"
                else -> "200"
            }
        )
    }
    var periodLabel by remember { mutableStateOf("November 2026") }
    var note by remember { mutableStateOf("") }
    val amountVal = amountText.toDoubleOrNull() ?: 0.0
    val manualTypes = listOf(EmployeePaymentType.SALARY, EmployeePaymentType.BONUS, EmployeePaymentType.EXPENSE)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(tr("Employee Salary, Bonus & Expense", "কর্মচারী বেতন, বোনাস ও খরচ")) },
        text = {
            ProvideSafeTextToolbar {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 440.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(tr("Payment Category", "লেনদেনের ধরন"), style = MaterialTheme.typography.labelLarge)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        manualTypes.forEach { t ->
                            FilterChip(
                                selected = paymentType == t,
                                onClick = { paymentType = t },
                                label = { Text(if (isBn) t.labelBn else t.labelEn) }
                            )
                        }
                    }
                    Text(tr("Employee", "কর্মচারী"), style = MaterialTheme.typography.labelLarge)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        employees.forEach { emp ->
                            FilterChip(
                                selected = selectedEmployeeId == emp.id,
                                onClick = { selectedEmployeeId = emp.id },
                                label = { Text(emp.name) }
                            )
                        }
                    }
                    OutlinedTextField(
                        value = amountText,
                        onValueChange = { amountText = it },
                        label = { Text("${tr("Amount *", "টাকার পরিমাণ *")} (${currency.symbol})") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("staff_pay_amount_input")
                    )
                    OutlinedTextField(
                        value = periodLabel,
                        onValueChange = { periodLabel = it },
                        label = { Text(tr("Month / Title", "মাস / শিরোনাম")) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = note,
                        onValueChange = { note = it },
                        label = { Text(tr("Note / Reason", "নোট / কারণ")) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Button(
                enabled = selectedEmp != null && amountVal > 0.0,
                onClick = {
                    if (selectedEmp != null) {
                        val baseAmt = CurrencyFormatter.toBaseBdt(amountVal, currency)
                        onConfirm(
                            selectedEmp,
                            baseAmt,
                            paymentType,
                            periodLabel.trim().ifBlank { "General" },
                            note.trim().ifBlank { if (isBn) paymentType.labelBn else paymentType.labelEn }
                        )
                    }
                },
                modifier = Modifier.testTag("confirm_staff_payment_button")
            ) { Text(tr("Confirm Payment", "নিশ্চিত করুন")) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(strings.cancel) }
        }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SendAutoMessageDialog(
    employees: List<EmployeeEntity>,
    farms: List<FarmEntity>,
    initialEmployee: EmployeeEntity?,
    isEmployeeSender: Boolean,
    strings: AppStrings,
    isBn: Boolean,
    onDismiss: () -> Unit,
    onSend: (Long, Long, AutoMessageType, String, String) -> Unit
) {
    var selectedEmployeeId by remember { mutableStateOf(initialEmployee?.id ?: employees.firstOrNull()?.id ?: 0L) }
    val selectedEmp = employees.firstOrNull { it.id == selectedEmployeeId } ?: employees.firstOrNull()
    var msgType by remember {
        mutableStateOf(
            if (isEmployeeSender) AutoMessageType.MORNING_CHECKIN else AutoMessageType.CUSTOM
        )
    }
    var subject by remember { mutableStateOf("") }
    var body by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(tr("Send Automated Message", "অটোমেটেড বার্তা পাঠান")) },
        text = {
            ProvideSafeTextToolbar {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 440.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (employees.size > 1) {
                        Text(tr("Employee", "কর্মচারী"), style = MaterialTheme.typography.labelLarge)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            employees.forEach { emp ->
                                FilterChip(
                                    selected = selectedEmployeeId == emp.id,
                                    onClick = { selectedEmployeeId = emp.id },
                                    label = { Text(emp.name) }
                                )
                            }
                        }
                    }
                    Text(tr("Message Type", "বার্তার ধরন"), style = MaterialTheme.typography.labelLarge)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(
                            AutoMessageType.MORNING_CHECKIN,
                            AutoMessageType.MIDDAY_UPDATE,
                            AutoMessageType.NIGHT_REPORT,
                            AutoMessageType.CUSTOM
                        ).forEach { t ->
                            FilterChip(
                                selected = msgType == t,
                                onClick = { msgType = t },
                                label = { Text(if (isBn) t.labelBn else t.labelEn) }
                            )
                        }
                    }
                    OutlinedTextField(
                        value = subject,
                        onValueChange = { subject = it },
                        label = { Text(tr("Subject (Optional)", "বিষয় (ঐচ্ছিক)")) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = body,
                        onValueChange = { body = it },
                        label = { Text(tr("Automated Message *", "বার্তার বিবরণ *")) },
                        minLines = 3,
                        modifier = Modifier.fillMaxWidth().testTag("auto_msg_body_input")
                    )
                }
            }
        },
        confirmButton = {
            Button(
                enabled = selectedEmp != null && body.isNotBlank(),
                onClick = {
                    if (selectedEmp != null) {
                        onSend(
                            selectedEmp.farmId,
                            selectedEmp.id,
                            msgType,
                            subject.trim().ifBlank { if (isBn) msgType.labelBn else msgType.labelEn },
                            body.trim()
                        )
                    }
                },
                modifier = Modifier.testTag("send_auto_msg_button")
            ) { Text(tr("Send", "পাঠান")) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(strings.cancel) }
        }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AddStaffEmployeeDialog(
    farms: List<FarmEntity>,
    defaultFarmId: Long,
    currency: CurrencyOption,
    strings: AppStrings,
    onDismiss: () -> Unit,
    onSave: (EmployeeEntity) -> Unit
) {
    val defaultDesignation = tr("Farm Manager", "খামার ম্যানেজার")
    var farmId by remember { mutableStateOf(defaultFarmId) }
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var designation by remember { mutableStateOf(defaultDesignation) }
    var salaryText by remember { mutableStateOf("") }
    var permInv by remember { mutableStateOf(true) }
    var permSales by remember { mutableStateOf(false) }
    var permLive by remember { mutableStateOf(true) }

    AlertDialog(
        onDismissRequest = onDismiss,
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
                    Text(tr("Farm", "খামার"), style = MaterialTheme.typography.labelLarge)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        farms.forEach { f ->
                            FilterChip(selected = farmId == f.id, onClick = { farmId = f.id }, label = { Text(f.name) })
                        }
                    }
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text(tr("Full Name *", "পূর্ণ নাম *")) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text(tr("Phone", "মোবাইল নম্বর")) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = designation, onValueChange = { designation = it }, label = { Text(tr("Designation", "পদবি")) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(
                        value = salaryText,
                        onValueChange = { salaryText = it },
                        label = { Text("${tr("Monthly Salary", "মাসিক বেতন")} (${currency.symbol})") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(tr("Starter Permissions (more in User Permissions):", "প্রাথমিক অনুমতি (বিস্তারিত পারমিশন ট্যাবে):"), style = MaterialTheme.typography.labelSmall)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        FilterChip(selected = permInv, onClick = { permInv = !permInv }, label = { Text(tr("Inventory", "মজুদ")) })
                        FilterChip(selected = permSales, onClick = { permSales = !permSales }, label = { Text(tr("Sales", "বিক্রয়")) })
                        FilterChip(selected = permLive, onClick = { permLive = !permLive }, label = { Text(tr("Livestock", "পশুপাখি")) })
                    }
                }
            }
        },
        confirmButton = {
            Button(
                enabled = farmId > 0L && name.isNotBlank(),
                onClick = {
                    onSave(
                        EmployeeEntity(
                            farmId = farmId,
                            ownerUserId = 0L,
                            name = name.trim(),
                            phone = phone.trim(),
                            designation = designation.trim(),
                            salary = CurrencyFormatter.toBaseBdt(salaryText.toDoubleOrNull() ?: 0.0, currency),
                            canManageInventory = permInv,
                            canManageSales = permSales,
                            canManageLivestock = permLive
                        )
                    )
                }
            ) { Text(strings.save) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(strings.cancel) }
        }
    )
}
