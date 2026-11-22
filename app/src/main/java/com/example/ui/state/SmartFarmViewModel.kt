package com.example.ui.state

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.*
import com.example.data.repository.FarmRepository
import com.example.i18n.AppLanguage
import com.example.i18n.CurrencyFormatter
import com.example.i18n.CurrencyOption
import com.example.i18n.DomainLocalization
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class CartEntry(
    val item: InventoryItemEntity,
    val quantity: Double
)

data class FarmerDashboardState(
    val farms: List<FarmEntity> = emptyList(),
    val selectedFarmId: Long? = null, // null = All Farms
    val categories: List<AnimalCategoryEntity> = emptyList(),
    val animals: List<AnimalRecordEntity> = emptyList(),
    val animalEvents: List<AnimalEventEntity> = emptyList(),
    val eggs: List<EggProductionEntity> = emptyList(),
    val inventory: List<InventoryItemEntity> = emptyList(),
    val inventoryMovements: List<InventoryMovementEntity> = emptyList(),
    val customers: List<CustomerEntity> = emptyList(),
    val sales: List<SaleEntity> = emptyList(),
    val incomingOrders: List<OrderEntity> = emptyList(),
    val expenses: List<ExpenseEntity> = emptyList(),
    val employees: List<EmployeeEntity> = emptyList(),
    val salaryPayments: List<SalaryPaymentEntity> = emptyList(),
    val subscriptions: List<UserSubscriptionEntity> = emptyList(),
    val supportRequests: List<SupportRequestEntity> = emptyList()
)

data class CustomerPortalState(
    val marketplaceItems: List<InventoryItemEntity> = emptyList(),
    val farmsById: Map<Long, FarmEntity> = emptyMap(),
    val cart: List<CartEntry> = emptyList(),
    val orders: List<OrderEntity> = emptyList()
)

data class SuperAdminState(
    val users: List<UserEntity> = emptyList(),
    val farms: List<FarmEntity> = emptyList(),
    val subscriptions: List<UserSubscriptionEntity> = emptyList(),
    val sales: List<SaleEntity> = emptyList(),
    val expenses: List<ExpenseEntity> = emptyList(),
    val orders: List<OrderEntity> = emptyList(),
    val supportRequests: List<SupportRequestEntity> = emptyList(),
    val auditLogs: List<AdminAuditLogEntity> = emptyList()
)

@OptIn(ExperimentalCoroutinesApi::class)
class SmartFarmViewModel(
    private val repository: FarmRepository,
    private val prefsRepo: UserPreferencesRepository
) : ViewModel() {

    val preferences: StateFlow<UserPreferencesState> = prefsRepo.preferences
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserPreferencesState())

    val currentUser: StateFlow<UserEntity?> = preferences
        .flatMapLatest { prefs ->
            val uid = prefs.currentUserId
            if (uid == null) flowOf(null) else repository.observeUser(uid)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val plans: StateFlow<List<SubscriptionPlanEntity>> = repository.observePlans()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val systemSettings: StateFlow<SystemSettingsEntity> = repository.observeSystemSettings()
        .map { it ?: SystemSettingsEntity() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SystemSettingsEntity())

    val notifications: StateFlow<List<AppNotificationEntity>> = currentUser
        .flatMapLatest { user ->
            if (user == null) flowOf(emptyList()) else repository.observeNotifications(user.id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _cart = MutableStateFlow<List<CartEntry>>(emptyList())
    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

    private val _authError = MutableStateFlow<String?>(null)
    val authError: StateFlow<String?> = _authError.asStateFlow()

    private val _isInitialized = MutableStateFlow(false)
    val isInitialized: StateFlow<Boolean> = _isInitialized.asStateFlow()

    private val _languageOverrides = MutableStateFlow<Map<String, String>>(emptyMap())
    val languageOverrides: StateFlow<Map<String, String>> = _languageOverrides.asStateFlow()

    val farmerState: StateFlow<FarmerDashboardState> = combine(currentUser, preferences) { user, prefs ->
        user to prefs.activeFarmId
    }.flatMapLatest { (user, activeFarmId) ->
        if (user == null || user.role != UserRole.FARMER.name) {
            flowOf(FarmerDashboardState())
        } else {
            val uid = user.id
            val f1 = combine(
                repository.observeFarms(uid),
                repository.observeAnimalCategories(uid),
                repository.observeAnimals(uid),
                repository.observeAnimalEvents(uid),
                repository.observeEggs(uid)
            ) { farms, cats, animals, events, eggs ->
                FarmerDashboardState(
                    farms = farms,
                    selectedFarmId = activeFarmId,
                    categories = cats,
                    animals = animals,
                    animalEvents = events,
                    eggs = eggs
                )
            }
            val f2 = combine(
                repository.observeInventory(uid),
                repository.observeInventoryMovements(uid),
                repository.observeCustomers(uid),
                repository.observeSales(uid),
                repository.observeFarmerOrders(uid)
            ) { inv, moves, custs, sales, orders ->
                Tuple5(inv, moves, custs, sales, orders)
            }
            val f3 = combine(
                repository.observeExpenses(uid),
                repository.observeEmployees(uid),
                repository.observeSalaryPayments(uid),
                repository.observeUserSubscriptions(uid),
                repository.observeUserSupport(uid)
            ) { exps, emps, sals, subs, sup ->
                Tuple5(exps, emps, sals, subs, sup)
            }
            combine(f1, f2, f3) { base, t2, t3 ->
                base.copy(
                    inventory = t2.a,
                    inventoryMovements = t2.b,
                    customers = t2.c,
                    sales = t2.d,
                    incomingOrders = t2.e,
                    expenses = t3.a,
                    employees = t3.b,
                    salaryPayments = t3.c,
                    subscriptions = t3.d,
                    supportRequests = t3.e
                )
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), FarmerDashboardState())

    val customerState: StateFlow<CustomerPortalState> = currentUser.flatMapLatest { user ->
        if (user == null || user.role != UserRole.CUSTOMER.name) {
            flowOf(CustomerPortalState())
        } else {
            combine(
                repository.observeMarketplaceItems(),
                repository.observeAllFarmsPublic(),
                _cart,
                repository.observeCustomerOrders(user.id)
            ) { items, farms, cart, orders ->
                CustomerPortalState(
                    marketplaceItems = items,
                    farmsById = farms.associateBy { it.id },
                    cart = cart,
                    orders = orders
                )
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), CustomerPortalState())

    val adminState: StateFlow<SuperAdminState> = currentUser.flatMapLatest { user ->
        if (user == null || user.role != UserRole.SUPER_ADMIN.name) {
            flowOf(SuperAdminState())
        } else {
            val g1 = combine(
                repository.observeAllUsers(),
                repository.observeAllFarmsAdmin(),
                repository.observeAllSubscriptionsAdmin(),
                repository.observeAllSalesAdmin()
            ) { users, farms, subs, sales ->
                Tuple4(users, farms, subs, sales)
            }
            val g2 = combine(
                repository.observeAllExpensesAdmin(),
                repository.observeAllOrdersAdmin(),
                repository.observeAllSupportAdmin(),
                repository.observeAuditLogs()
            ) { exps, orders, support, logs ->
                Tuple4(exps, orders, support, logs)
            }
            combine(g1, g2) { a, b ->
                SuperAdminState(
                    users = a.a,
                    farms = a.b,
                    subscriptions = a.c,
                    sales = a.d,
                    expenses = b.a,
                    orders = b.b,
                    supportRequests = b.c,
                    auditLogs = b.d
                )
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SuperAdminState())

    init {
        viewModelScope.launch {
            repository.initializeDatabase()
            _isInitialized.value = true
        }
    }

    fun clearMessage() { _snackbarMessage.value = null }
    fun showMessage(msg: String) { _snackbarMessage.value = msg }
    fun clearAuthError() { _authError.value = null }

    private fun isBn(): Boolean = preferences.value.languageCode == "bn"
    private fun msg(en: String, bn: String): String = if (isBn()) bn else en
    private fun localizeError(raw: String?, fallbackEn: String, fallbackBn: String): String {
        if (!isBn()) return raw ?: fallbackEn
        return when (raw) {
            "Email/mobile and password are required." -> "ইমেইল/মোবাইল এবং পাসওয়ার্ড প্রয়োজন।"
            "No account found with those credentials." -> "এই তথ্যে কোনো অ্যাকাউন্ট পাওয়া যায়নি।"
            "Incorrect password." -> "ভুল পাসওয়ার্ড।"
            "This account is suspended. Contact Super Admin." -> "এই অ্যাকাউন্টটি স্থগিত করা হয়েছে। সুপার অ্যাডমিনের সাথে যোগাযোগ করুন।"
            "This account is inactive. Contact Super Admin." -> "এই অ্যাকাউন্টটি নিষ্ক্রিয়। সুপার অ্যাডমিনের সাথে যোগাযোগ করুন।"
            "Please complete all fields (password min 4 chars)." -> "সব তথ্য পূরণ করুন (পাসওয়ার্ড কমপক্ষে ৪ অক্ষর)।"
            "Public Super Admin registration is restricted." -> "পাবলিক সুপার অ্যাডমিন নিবন্ধন অনুমোদিত নয়।"
            "An account with this email already exists." -> "এই ইমেইল দিয়ে আগেই অ্যাকাউন্ট তৈরি করা হয়েছে।"
            "New password must be at least 4 characters." -> "নতুন পাসওয়ার্ড কমপক্ষে ৪ অক্ষরের হতে হবে।"
            "Account not found." -> "অ্যাকাউন্ট পাওয়া যায়নি।"
            else -> raw ?: fallbackBn
        }
    }

    fun login(identifier: String, password: String) {
        viewModelScope.launch {
            _authError.value = null
            repository.login(identifier, password)
                .onSuccess { user ->
                    prefsRepo.setCurrentUserId(user.id)
                    prefsRepo.setCurrency(user.preferredCurrency)
                    repository.checkExpiredSubscriptions()
                }
                .onFailure { _authError.value = localizeError(it.message, "Login failed.", "লগইন ব্যর্থ হয়েছে।") }
        }
    }

    fun register(name: String, mobile: String, email: String, password: String, role: UserRole) {
        viewModelScope.launch {
            _authError.value = null
            repository.register(name, mobile, email, password, role)
                .onSuccess { user ->
                    prefsRepo.setCurrentUserId(user.id)
                    _snackbarMessage.value = msg(
                        "Account created! Welcome, ${user.name}.",
                        "অ্যাকাউন্ট তৈরি হয়েছে! স্বাগতম, ${user.name}।"
                    )
                }
                .onFailure { _authError.value = localizeError(it.message, "Registration failed.", "নিবন্ধন ব্যর্থ হয়েছে।") }
        }
    }

    fun resetPassword(identifier: String, newPassword: String, onDone: () -> Unit) {
        viewModelScope.launch {
            _authError.value = null
            repository.resetPassword(identifier, newPassword)
                .onSuccess {
                    _snackbarMessage.value = msg(
                        "Password updated. You can now sign in.",
                        "পাসওয়ার্ড আপডেট হয়েছে। এখন লগইন করুন।"
                    )
                    onDone()
                }
                .onFailure { _authError.value = localizeError(it.message, "Reset failed.", "পাসওয়ার্ড রিসেট ব্যর্থ হয়েছে।") }
        }
    }

    fun logout() {
        viewModelScope.launch {
            _cart.value = emptyList()
            prefsRepo.setCurrentUserId(null)
        }
    }

    fun setLanguage(lang: AppLanguage) {
        viewModelScope.launch { prefsRepo.setLanguage(lang.code) }
    }

    fun setCurrency(currency: CurrencyOption) {
        viewModelScope.launch { prefsRepo.setCurrency(currency.code) }
    }

    fun setDarkMode(dark: Boolean?) {
        viewModelScope.launch { prefsRepo.setDarkMode(dark) }
    }

    fun setLanguageOverride(key: String, value: String) {
        _languageOverrides.update { current ->
            if (value.isBlank()) current - key else current + (key to value.trim())
        }
    }

    fun selectActiveFarm(farmId: Long?) {
        viewModelScope.launch { prefsRepo.setActiveFarmId(farmId) }
    }

    fun updateProfile(name: String, mobile: String, imageUri: String?) {
        val user = currentUser.value ?: return
        viewModelScope.launch {
            repository.updateProfile(user, name, mobile, imageUri)
            _snackbarMessage.value = msg("Profile updated.", "প্রোফাইল আপডেট হয়েছে।")
        }
    }

    fun markNotificationsRead() {
        val user = currentUser.value ?: return
        viewModelScope.launch { repository.markNotificationsRead(user.id) }
    }

    fun submitSupport(subject: String, message: String) {
        val user = currentUser.value ?: return
        viewModelScope.launch {
            repository.submitSupportRequest(user, subject, message)
            _snackbarMessage.value = msg("Support request submitted.", "সাপোর্ট অনুরোধ পাঠানো হয়েছে।")
        }
    }

    // --- Farm CRUD ---
    fun saveFarm(farm: FarmEntity) {
        val user = currentUser.value ?: return
        viewModelScope.launch {
            val savedId = repository.saveFarm(farm.copy(ownerUserId = user.id))
            _snackbarMessage.value = msg("${farm.name} saved.", "${farm.name} সংরক্ষিত হয়েছে।")
            if (preferences.value.activeFarmId == null && farmerState.value.farms.isEmpty()) {
                prefsRepo.setActiveFarmId(savedId)
            }
        }
    }

    fun deleteFarm(farm: FarmEntity) {
        viewModelScope.launch {
            if (preferences.value.activeFarmId == farm.id) prefsRepo.setActiveFarmId(null)
            repository.deleteFarm(farm)
            _snackbarMessage.value = msg("${farm.name} deleted.", "${farm.name} মুছে ফেলা হয়েছে।")
        }
    }

    // --- Livestock ---
    fun addCustomAnimalCategory(name: String, isBird: Boolean) {
        val uid = currentUser.value?.id ?: return
        viewModelScope.launch {
            repository.addCustomAnimalCategory(uid, name, isBird)
            _snackbarMessage.value = msg("Added category: $name", "ক্যাটাগরি যোগ হয়েছে: $name")
        }
    }

    fun saveAnimalRecord(record: AnimalRecordEntity) {
        val uid = currentUser.value?.id ?: return
        viewModelScope.launch {
            repository.saveAnimalRecord(record.copy(ownerUserId = uid))
            _snackbarMessage.value = msg("${record.name} saved.", "${record.name} সংরক্ষিত হয়েছে।")
        }
    }

    fun deleteAnimalRecord(record: AnimalRecordEntity) {
        viewModelScope.launch {
            repository.deleteAnimalRecord(record)
            _snackbarMessage.value = msg("${record.name} removed.", "${record.name} মুছে ফেলা হয়েছে।")
        }
    }

    fun recordAnimalEvent(
        animal: AnimalRecordEntity,
        eventType: AnimalEventType,
        delta: Int,
        costOrValue: Double,
        description: String
    ) {
        viewModelScope.launch {
            repository.recordAnimalEvent(animal, eventType, delta, costOrValue, description)
            val eventLabel = DomainLocalization.animalEventType(eventType, isBn())
            _snackbarMessage.value = msg(
                "$eventLabel recorded for ${animal.name}.",
                "${animal.name}-এর জন্য $eventLabel সংরক্ষিত হয়েছে।"
            )
        }
    }

    // --- Eggs ---
    fun recordEggProduction(
        farmId: Long,
        eggCategory: String,
        previousStockOverride: Int?,
        produced: Int,
        broken: Int,
        sold: Int,
        unitPrice: Double
    ) {
        val uid = currentUser.value?.id ?: return
        viewModelScope.launch {
            repository.recordEggProduction(
                farmId = farmId,
                ownerId = uid,
                eggCategory = eggCategory,
                previousStockOverride = previousStockOverride,
                producedCount = produced,
                brokenCount = broken,
                soldCount = sold,
                unitSellingPrice = unitPrice
            )
            val catLabel = DomainLocalization.eggCategory(eggCategory, isBn())
            _snackbarMessage.value = msg(
                "Egg production logged ($catLabel).",
                "ডিম উৎপাদন সংরক্ষিত হয়েছে ($catLabel)।"
            )
        }
    }

    fun deleteEggRecord(record: EggProductionEntity) {
        viewModelScope.launch { repository.deleteEggRecord(record) }
    }

    // --- Inventory ---
    fun saveInventoryItem(item: InventoryItemEntity) {
        val uid = currentUser.value?.id ?: return
        viewModelScope.launch {
            repository.saveInventoryItem(item.copy(ownerUserId = uid))
            _snackbarMessage.value = msg(
                "${item.name} saved to inventory.",
                "${item.name} ইনভেন্টরিতে সংরক্ষিত হয়েছে।"
            )
        }
    }

    fun adjustInventoryStock(
        item: InventoryItemEntity,
        delta: Double,
        isStockIn: Boolean,
        unitCost: Double,
        supplier: String,
        note: String
    ) {
        viewModelScope.launch {
            repository.adjustInventoryStock(item, delta, isStockIn, unitCost, supplier, note)
            _snackbarMessage.value = if (isStockIn) {
                msg("Stock added to ${item.name}.", "${item.name}-এ স্টক যোগ হয়েছে।")
            } else {
                msg("Stock deducted from ${item.name}.", "${item.name} থেকে স্টক কমানো হয়েছে।")
            }
        }
    }

    fun deleteInventoryItem(item: InventoryItemEntity) {
        viewModelScope.launch { repository.deleteInventoryItem(item) }
    }

    // --- Sales & Customers ---
    fun saveCustomer(customer: CustomerEntity) {
        val uid = currentUser.value?.id ?: return
        viewModelScope.launch {
            repository.saveCustomer(customer.copy(ownerUserId = uid))
            _snackbarMessage.value = msg(
                "Customer ${customer.name} saved.",
                "গ্রাহক ${customer.name} সংরক্ষিত হয়েছে।"
            )
        }
    }

    fun deleteCustomer(customer: CustomerEntity) {
        viewModelScope.launch { repository.deleteCustomer(customer) }
    }

    fun createSale(
        farmId: Long,
        customerId: Long?,
        customerName: String,
        customerPhone: String,
        productCategory: String,
        productName: String,
        inventoryItem: InventoryItemEntity?,
        animalRecord: AnimalRecordEntity?,
        quantity: Double,
        unitPrice: Double,
        discount: Double,
        paidAmount: Double,
        paymentMethod: String
    ) {
        val uid = currentUser.value?.id ?: return
        viewModelScope.launch {
            val sale = repository.createSale(
                farmId, uid, customerId, customerName, customerPhone,
                productCategory, productName, inventoryItem, animalRecord,
                quantity, unitPrice, discount, paidAmount, paymentMethod
            )
            _snackbarMessage.value = msg(
                "Sale recorded (${sale.invoiceNumber}).",
                "বিক্রয় সংরক্ষিত হয়েছে (${sale.invoiceNumber})।"
            )
        }
    }

    fun settleSaleDue(sale: SaleEntity, amount: Double) {
        viewModelScope.launch {
            repository.settleSaleDue(sale, amount)
            _snackbarMessage.value = msg(
                "Payment recorded on ${sale.invoiceNumber}.",
                "${sale.invoiceNumber} এর বকেয়া পরিশোধিত হয়েছে।"
            )
        }
    }

    fun deleteSale(sale: SaleEntity) {
        viewModelScope.launch { repository.deleteSale(sale) }
    }

    fun updateOrderStatus(order: OrderEntity, status: OrderStatus) {
        viewModelScope.launch {
            repository.updateOrderStatus(order, status)
            _snackbarMessage.value = msg(
                "Order ${order.orderNumber} marked ${status.label}.",
                "অর্ডার ${order.orderNumber} আপডেট হয়েছে।"
            )
        }
    }

    // --- Expenses & Employees ---
    fun addExpense(farmId: Long, category: String, amount: Double, description: String) {
        val uid = currentUser.value?.id ?: return
        viewModelScope.launch {
            repository.addExpense(
                ExpenseEntity(
                    farmId = farmId,
                    ownerUserId = uid,
                    category = category,
                    amount = amount,
                    description = description
                )
            )
            val catLabel = DomainLocalization.expenseCategory(category, isBn())
            _snackbarMessage.value = msg("Expense added ($catLabel).", "খরচ যোগ হয়েছে ($catLabel)।")
        }
    }

    fun deleteExpense(expense: ExpenseEntity) {
        viewModelScope.launch { repository.deleteExpense(expense) }
    }

    fun saveEmployee(emp: EmployeeEntity) {
        val uid = currentUser.value?.id ?: return
        viewModelScope.launch {
            repository.saveEmployee(emp.copy(ownerUserId = uid))
            _snackbarMessage.value = msg(
                "Employee ${emp.name} saved.",
                "কর্মচারী ${emp.name} সংরক্ষিত হয়েছে।"
            )
        }
    }

    fun deleteEmployee(emp: EmployeeEntity) {
        viewModelScope.launch { repository.deleteEmployee(emp) }
    }

    fun markAttendance(emp: EmployeeEntity, status: AttendanceStatus) {
        viewModelScope.launch {
            repository.markEmployeeAttendance(emp, status)
            val stLabel = DomainLocalization.attendanceStatus(status, isBn())
            _snackbarMessage.value = msg(
                "${emp.name} marked $stLabel.",
                "${emp.name} কে '$stLabel' চিহ্নিত করা হয়েছে।"
            )
        }
    }

    fun paySalary(emp: EmployeeEntity, amount: Double, monthLabel: String, note: String) {
        viewModelScope.launch {
            repository.payEmployeeSalary(emp, amount, monthLabel, note)
            _snackbarMessage.value = msg(
                "Salary paid to ${emp.name}.",
                "${emp.name} কে বেতন প্রদান করা হয়েছে।"
            )
        }
    }

    // --- Subscriptions ---
    fun subscribe(plan: SubscriptionPlanEntity, currencyCode: String, method: String, txRef: String) {
        val user = currentUser.value ?: return
        viewModelScope.launch {
            repository.purchaseSubscription(user, plan, currencyCode, method, txRef)
            _snackbarMessage.value = msg(
                "Subscription processed for ${plan.name}.",
                "${plan.name} সাবস্ক্রিপশন প্রক্রিয়া সম্পন্ন হয়েছে।"
            )
        }
    }

    // --- Customer Shopping Cart & Ordering ---
    fun addToCart(item: InventoryItemEntity, qty: Double = 1.0) {
        _cart.update { current ->
            val existing = current.find { it.item.id == item.id }
            if (existing == null) {
                current + CartEntry(item, qty.coerceAtMost(item.quantity))
            } else {
                current.map {
                    if (it.item.id == item.id) {
                        it.copy(quantity = (it.quantity + qty).coerceAtMost(item.quantity))
                    } else it
                }
            }
        }
        _snackbarMessage.value = msg(
            "Added ${item.name} to cart.",
            "${item.name} কার্টে যোগ করা হয়েছে।"
        )
    }

    fun updateCartQuantity(itemId: Long, delta: Double) {
        _cart.update { current ->
            current.mapNotNull { entry ->
                if (entry.item.id != itemId) entry
                else {
                    val next = entry.quantity + delta
                    if (next <= 0.0) null
                    else entry.copy(quantity = next.coerceAtMost(entry.item.quantity))
                }
            }
        }
    }

    fun placeOrder(deliveryAddress: String, paymentMethod: String, onSuccess: () -> Unit) {
        val customer = currentUser.value ?: return
        val entries = _cart.value
        val farmsMap = customerState.value.farmsById
        viewModelScope.launch {
            repository.placeOnlineOrder(
                customer = customer,
                deliveryAddress = deliveryAddress,
                paymentMethod = paymentMethod,
                cartItems = entries.map { it.item to it.quantity },
                farmsMap = farmsMap
            ).onSuccess { orders ->
                _cart.value = emptyList()
                val orderNums = orders.joinToString { it.orderNumber }
                _snackbarMessage.value = msg(
                    "Order placed ($orderNums)!",
                    "অর্ডার সম্পন্ন হয়েছে ($orderNums)!"
                )
                onSuccess()
            }.onFailure {
                _snackbarMessage.value = it.message ?: msg("Failed to place order.", "অর্ডার করা যায়নি।")
            }
        }
    }

    fun requestOrderCancellation(order: OrderEntity) {
        viewModelScope.launch {
            repository.requestOrderCancellation(order)
            _snackbarMessage.value = msg(
                "Cancellation request sent for ${order.orderNumber}.",
                "${order.orderNumber} বাতিলের অনুরোধ পাঠানো হয়েছে।"
            )
        }
    }

    // --- Super Admin Actions ---
    fun adminSetUserStatus(target: UserEntity, status: AccountStatus) {
        val adminId = currentUser.value?.id ?: return
        viewModelScope.launch {
            repository.adminSetUserStatus(adminId, target, status)
                .onSuccess {
                    val stLabel = DomainLocalization.accountStatus(status.name, isBn())
                    _snackbarMessage.value = msg(
                        "${target.name} set to ${status.name}.",
                        "${target.name}-এর অবস্থা '$stLabel' করা হয়েছে।"
                    )
                }
                .onFailure { _snackbarMessage.value = it.message ?: msg("Action denied.", "অনুমতি নেই।") }
        }
    }

    fun adminSavePlan(plan: SubscriptionPlanEntity) {
        val adminId = currentUser.value?.id ?: return
        viewModelScope.launch {
            repository.adminSaveSubscriptionPlan(adminId, plan)
                .onSuccess { _snackbarMessage.value = msg("Plan ${plan.name} saved.", "প্ল্যান ${plan.name} সংরক্ষিত হয়েছে।") }
                .onFailure { _snackbarMessage.value = it.message ?: msg("Action denied.", "অনুমতি নেই।") }
        }
    }

    fun adminDeletePlan(plan: SubscriptionPlanEntity) {
        val adminId = currentUser.value?.id ?: return
        viewModelScope.launch {
            repository.adminDeleteSubscriptionPlan(adminId, plan)
                .onSuccess { _snackbarMessage.value = msg("Plan ${plan.name} deleted.", "প্ল্যান ${plan.name} মুছে ফেলা হয়েছে।") }
                .onFailure { _snackbarMessage.value = it.message ?: msg("Action denied.", "অনুমতি নেই।") }
        }
    }

    fun adminUpdatePayment(sub: UserSubscriptionEntity, paymentStatus: PaymentStatus, subStatus: String) {
        val adminId = currentUser.value?.id ?: return
        viewModelScope.launch {
            repository.adminUpdateSubscriptionPayment(adminId, sub, paymentStatus, subStatus)
                .onSuccess {
                    _snackbarMessage.value = msg(
                        "Subscription ${sub.transactionRef} updated.",
                        "সাবস্ক্রিপশন ${sub.transactionRef} আপডেট হয়েছে।"
                    )
                }
                .onFailure { _snackbarMessage.value = it.message ?: msg("Action denied.", "অনুমতি নেই।") }
        }
    }

    fun adminGrantPlan(target: UserEntity, plan: SubscriptionPlanEntity) {
        val adminId = currentUser.value?.id ?: return
        viewModelScope.launch {
            repository.adminGrantSubscription(adminId, target, plan)
                .onSuccess {
                    _snackbarMessage.value = msg(
                        "Granted ${plan.name} to ${target.name}.",
                        "${target.name} কে ${plan.name} প্ল্যান দেওয়া হয়েছে।"
                    )
                }
                .onFailure { _snackbarMessage.value = it.message ?: msg("Action denied.", "অনুমতি নেই।") }
        }
    }

    fun adminBroadcast(title: String, message: String) {
        val adminId = currentUser.value?.id ?: return
        viewModelScope.launch {
            repository.adminBroadcastNotification(adminId, title, message)
                .onSuccess { _snackbarMessage.value = msg("Announcement broadcast sent.", "ঘোষণা সবার কাছে পাঠানো হয়েছে।") }
                .onFailure { _snackbarMessage.value = it.message ?: msg("Action denied.", "অনুমতি নেই।") }
        }
    }

    fun adminUpdateSettings(settings: SystemSettingsEntity) {
        val adminId = currentUser.value?.id ?: return
        viewModelScope.launch {
            repository.adminUpdateSettings(adminId, settings)
                .onSuccess { _snackbarMessage.value = msg("System settings saved.", "সিস্টেম সেটিংস সংরক্ষিত হয়েছে।") }
                .onFailure { _snackbarMessage.value = it.message ?: msg("Action denied.", "অনুমতি নেই।") }
        }
    }

    fun adminResolveSupport(req: SupportRequestEntity, reply: String) {
        val adminId = currentUser.value?.id ?: return
        viewModelScope.launch {
            repository.adminResolveSupport(adminId, req, reply)
                .onSuccess { _snackbarMessage.value = msg("Support ticket resolved.", "সাপোর্ট টিকিট সমাধান হয়েছে।") }
                .onFailure { _snackbarMessage.value = it.message ?: msg("Action denied.", "অনুমতি নেই।") }
        }
    }

    // Helper for base currency input conversion
    fun toBaseBdt(amountInSelectedCurrency: Double): Double {
        val currency = CurrencyOption.fromCode(preferences.value.currencyCode)
        return CurrencyFormatter.toBaseBdt(amountInSelectedCurrency, currency)
    }

    private data class Tuple4<A, B, C, D>(val a: A, val b: B, val c: C, val d: D)
    private data class Tuple5<A, B, C, D, E>(val a: A, val b: B, val c: C, val d: D, val e: E)

    companion object {
        fun provideFactory(
            repository: FarmRepository,
            prefsRepo: UserPreferencesRepository
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return SmartFarmViewModel(repository, prefsRepo) as T
            }
        }
    }
}
