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
    val ownerUserId: Long = 0L,
    val accessPolicy: EmployeeAccessPolicy = EmployeeAccessPolicy(),
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
    val supportRequests: List<SupportRequestEntity> = emptyList(),
    val employeePermissions: List<EmployeePermissionEntity> = emptyList(),
    val customPermissionOptions: List<CustomPermissionOptionEntity> = emptyList(),
    val subscriptionPolicy: SubscriptionAccessPolicy = SubscriptionAccessPolicy(),
    val agriLands: List<AgriLandEntity> = emptyList(),
    val agriCrops: List<AgriCropEntity> = emptyList(),
    val agriExpenses: List<AgriExpenseEntity> = emptyList(),
    val productAds: List<ProductAdEntity> = emptyList()
)

data class CustomerPortalState(
    val marketplaceItems: List<InventoryItemEntity> = emptyList(),
    val availableCategories: List<String> = emptyList(),
    val totalMatchingCount: Int = 0,
    val searchQuery: String = "",
    val selectedCategory: String? = null,
    val pageSizeLimit: Int = 30,
    val farmsById: Map<Long, FarmEntity> = emptyMap(),
    val cart: List<CartEntry> = emptyList(),
    val orders: List<OrderEntity> = emptyList(),
    val activeShopAds: List<ProductAdEntity> = emptyList(),
    val supportRequests: List<SupportRequestEntity> = emptyList()
)

data class SuperAdminState(
    val users: List<UserEntity> = emptyList(),
    val farms: List<FarmEntity> = emptyList(),
    val subscriptions: List<UserSubscriptionEntity> = emptyList(),
    val sales: List<SaleEntity> = emptyList(),
    val expenses: List<ExpenseEntity> = emptyList(),
    val orders: List<OrderEntity> = emptyList(),
    val supportRequests: List<SupportRequestEntity> = emptyList(),
    val auditLogs: List<AdminAuditLogEntity> = emptyList(),
    val employees: List<EmployeeEntity> = emptyList(),
    val employeePermissions: List<EmployeePermissionEntity> = emptyList(),
    val customPermissionOptions: List<CustomPermissionOptionEntity> = emptyList(),
    val inventoryItems: List<InventoryItemEntity> = emptyList(),
    val productAds: List<ProductAdEntity> = emptyList()
)

@OptIn(ExperimentalCoroutinesApi::class)
class SmartFarmViewModel(
    private val repository: FarmRepository,
    private val prefsRepo: UserPreferencesRepository
) : ViewModel() {

    val preferences: StateFlow<UserPreferencesState> = prefsRepo.preferences
        .catch { emit(UserPreferencesState()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserPreferencesState())

    val currentUser: StateFlow<UserEntity?> = preferences
        .flatMapLatest { prefs ->
            val uid = prefs.currentUserId
            if (uid == null) flowOf(null) else repository.observeUser(uid)
        }
        .catch { emit(null) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val plans: StateFlow<List<SubscriptionPlanEntity>> = repository.observePlans()
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val systemSettings: StateFlow<SystemSettingsEntity> = repository.observeSystemSettings()
        .map { it ?: SystemSettingsEntity() }
        .catch { emit(SystemSettingsEntity()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SystemSettingsEntity())

    val notifications: StateFlow<List<AppNotificationEntity>> = currentUser
        .flatMapLatest { user ->
            if (user == null) flowOf(emptyList())
            else {
                val (ownerId, _) = repository.resolveEmployeeContext(user)
                repository.observeNotifications(ownerId)
            }
        }
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val employeeAccessPolicy: StateFlow<EmployeeAccessPolicy> = currentUser
        .flatMapLatest { user ->
            when {
                user == null -> flowOf(EmployeeAccessPolicy())
                user.role == UserRole.EMPLOYEE.name -> {
                    val (_, employeeId) = repository.resolveEmployeeContext(user)
                    if (employeeId == null) {
                        flowOf(EmployeeAccessPolicy(isAdminOwner = false, grantedKeys = emptySet()))
                    } else {
                        repository.observePermissionsForEmployee(employeeId).map { list ->
                            EmployeeAccessPolicy(
                                isAdminOwner = false,
                                grantedKeys = list.filter { it.granted }.map { it.permissionKey }.toSet()
                            )
                        }
                    }
                }
                else -> flowOf(EmployeeAccessPolicy(isAdminOwner = true))
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), EmployeeAccessPolicy())

    private val _cart = MutableStateFlow<List<CartEntry>>(emptyList())
    private val _marketplaceSearch = MutableStateFlow("")
    private val _marketplaceCategory = MutableStateFlow<String?>(null)
    private val _marketplaceLimit = MutableStateFlow(30)
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
        if (user == null || (user.role != UserRole.FARMER.name && user.role != UserRole.EMPLOYEE.name)) {
            flowOf(FarmerDashboardState())
        } else {
            val (ownerId, _) = repository.resolveEmployeeContext(user)
            val f1 = combine(
                repository.observeFarms(ownerId),
                repository.observeAnimalCategories(ownerId),
                repository.observeAnimals(ownerId),
                repository.observeAnimalEvents(ownerId),
                repository.observeEggs(ownerId)
            ) { farms, cats, animals, events, eggs ->
                FarmerDashboardState(
                    ownerUserId = ownerId,
                    farms = farms,
                    selectedFarmId = activeFarmId,
                    categories = cats,
                    animals = animals,
                    animalEvents = events,
                    eggs = eggs
                )
            }
            val f2 = combine(
                repository.observeInventory(ownerId),
                repository.observeInventoryMovements(ownerId),
                repository.observeCustomers(ownerId),
                repository.observeSales(ownerId),
                repository.observeFarmerOrders(ownerId)
            ) { inv, moves, custs, sales, orders ->
                Tuple5(inv, moves, custs, sales, orders)
            }
            val f3 = combine(
                repository.observeExpenses(ownerId),
                repository.observeEmployees(ownerId),
                repository.observeSalaryPayments(ownerId),
                repository.observeUserSubscriptions(ownerId),
                repository.observeUserSupport(ownerId)
            ) { exps, emps, sals, subs, sup ->
                Tuple5(exps, emps, sals, subs, sup)
            }
            val f4 = combine(
                repository.observePermissionsByOwner(ownerId),
                repository.observeCustomOptionsByOwner(ownerId),
                employeeAccessPolicy,
                systemSettings
            ) { perms, customOpts, policy, settings ->
                Tuple4(perms, customOpts, policy, settings)
            }
            val f5 = combine(
                repository.observeAgriLands(ownerId),
                repository.observeAgriCrops(ownerId),
                repository.observeAgriExpenses(ownerId),
                repository.observeProductAds(ownerId)
            ) { lands, crops, agriExp, ads ->
                Tuple4(lands, crops, agriExp, ads)
            }
            combine(f1, f2, f3, f4, f5) { base, t2, t3, t4, t5 ->
                base.copy(
                    accessPolicy = t4.c,
                    inventory = t2.a,
                    inventoryMovements = t2.b,
                    customers = t2.c,
                    sales = t2.d,
                    incomingOrders = t2.e,
                    expenses = t3.a,
                    employees = t3.b,
                    salaryPayments = t3.c,
                    subscriptions = t3.d,
                    supportRequests = t3.e,
                    employeePermissions = t4.a,
                    customPermissionOptions = t4.b,
                    subscriptionPolicy = SubscriptionAccessPolicy.fromSettingsAndSubs(t4.d, t3.d),
                    agriLands = t5.a,
                    agriCrops = t5.b,
                    agriExpenses = t5.c,
                    productAds = t5.d
                )
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), FarmerDashboardState())

    private val marketplaceFeedFlow = combine(
        _marketplaceSearch,
        _marketplaceCategory,
        _marketplaceLimit
    ) { search, category, limit ->
        Triple(search.trim(), category, limit)
    }.flatMapLatest { (search, category, limit) ->
        combine(
            repository.observeMarketplaceItemsFiltered(search, category, limit),
            repository.countMarketplaceItemsFiltered(search, category),
            repository.observeMarketplaceCategories()
        ) { items, totalCount, categories ->
            Tuple5(items, categories, totalCount, Triple(search, category, limit), Unit)
        }
    }

    val customerState: StateFlow<CustomerPortalState> = currentUser.flatMapLatest { user ->
        if (user == null || user.role != UserRole.CUSTOMER.name) {
            flowOf(CustomerPortalState())
        } else {
            val baseShopFlow = combine(
                marketplaceFeedFlow,
                repository.observeMarketplaceFarms(),
                _cart,
                repository.observeCustomerOrders(user.id),
                repository.observeActiveShopAds()
            ) { feed, farms, cart, orders, ads ->
                CustomerPortalState(
                    marketplaceItems = feed.a,
                    availableCategories = feed.b,
                    totalMatchingCount = feed.c,
                    searchQuery = feed.d.first,
                    selectedCategory = feed.d.second,
                    pageSizeLimit = feed.d.third,
                    farmsById = farms.associateBy { it.id },
                    cart = cart,
                    orders = orders,
                    activeShopAds = ads
                )
            }
            combine(baseShopFlow, repository.observeUserSupport(user.id)) { state, tickets ->
                state.copy(supportRequests = tickets)
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
            val g3 = combine(
                repository.observeAllEmployeesAdmin(),
                repository.observeAllPermissions(),
                repository.observeAllCustomOptions(),
                repository.observeAllInventoryAdmin(),
                repository.observeAllAdsAdmin()
            ) { emps, perms, customOpts, invItems, ads ->
                Tuple5(emps, perms, customOpts, invItems, ads)
            }
            combine(g1, g2, g3) { a, b, c ->
                SuperAdminState(
                    users = a.a,
                    farms = a.b,
                    subscriptions = a.c,
                    sales = a.d,
                    expenses = b.a,
                    orders = b.b,
                    supportRequests = b.c,
                    auditLogs = b.d,
                    employees = c.a,
                    employeePermissions = c.b,
                    customPermissionOptions = c.c,
                    inventoryItems = c.d,
                    productAds = c.e
                )
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SuperAdminState())

    init {
        viewModelScope.launch {
            try {
                repository.initializeDatabase()
            } catch (_: Throwable) {
                // Keep UI recoverable even if startup initialization encounters an issue
            } finally {
                _isInitialized.value = true
            }
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
            "Product name is required." -> "পণ্যের নাম প্রয়োজন।"
            "Product category is required." -> "পণ্যের ধরন প্রয়োজন।"
            "Online selling price must be greater than 0." -> "অনলাইন বিক্রয় মূল্য ০-এর বেশি হতে হবে।"
            "Quantity cannot be negative." -> "পরিমাণ ঋণাত্মক হতে পারবে না।"
            "Please select a valid farm before adding online products." -> "অনলাইন পণ্য যোগ করার আগে একটি বৈধ খামার নির্বাচন করুন।"
            "Selected farm does not belong to this farm account." -> "নির্বাচিত খামারটি এই খামার অ্যাকাউন্টের নয়।"
            "Set a selling price before publishing online." -> "অনলাইনে প্রকাশ করার আগে বিক্রয় মূল্য নির্ধারণ করুন।"
            "Cart is empty." -> "কার্ট খালি আছে।"
            "Delivery address is required." -> "ডেলিভারি ঠিকানা প্রয়োজন।"
            "Access denied: You don't have permission for this option." -> "অনুমতি নেই: এই অপশনের অ্যাক্সেস আপনার নেই।"
            "Land plot name is required." -> "জমির নাম দেওয়া আবশ্যক।"
            "Land area cannot be negative." -> "জমির পরিমাণ ঋণাত্মক হতে পারবে না।"
            "Crop title is required." -> "ফসলের নাম দেওয়া আবশ্যক।"
            "Please select a valid land plot first." -> "প্রথমে একটি বৈধ জমি নির্বাচন করুন।"
            "Please select a valid land plot." -> "একটি বৈধ জমি নির্বাচন করুন।"
            "Expense amount must be greater than zero." -> "খরচের পরিমাণ ০-এর বেশি হতে হবে।"
            "Ad headline is required." -> "বিজ্ঞাপনের শিরোনাম আবশ্যক।"
            "Email, password, and security PIN are required." -> "ইমেইল, পাসওয়ার্ড এবং সিকিউরিটি পিন আবশ্যক।"
            "Access Denied: Super Admin role required." -> "প্রবেশাধিকার নেই: সুপার অ্যাডমিন অ্যাকাউন্ট প্রয়োজন।"
            "Invalid Developer Security PIN." -> "ভুল ডেভেলপার সিকিউরিটি পিন।"
            "Super Admin password cannot be reset publicly." -> "সুপার অ্যাডমিন পাসওয়ার্ড পাবলিকলি রিসেট করা যায় না।"
            "Current Super Admin password is incorrect." -> "বর্তমান সুপার অ্যাডমিন পাসওয়ার্ড সঠিক নয়।"
            "Enter a valid admin email address." -> "সঠিক অ্যাডমিন ইমেইল ঠিকানা দিন।"
            "New Super Admin password must be at least 6 characters." -> "নতুন সুপার অ্যাডমিন পাসওয়ার্ড কমপক্ষে ৬ অক্ষরের হতে হবে।"
            "Security PIN must be at least 4 characters." -> "সিকিউরিটি পিন কমপক্ষে ৪ অক্ষরের হতে হবে।"
            "Enter a new email, password, or security PIN to update." -> "আপডেট করতে নতুন ইমেইল, পাসওয়ার্ড বা সিকিউরিটি পিন দিন।"
            "Cannot delete a Super Admin account." -> "সুপার অ্যাডমিন অ্যাকাউন্ট মুছে ফেলা যাবে না।"
            else -> when {
                raw?.startsWith("Subscription required:") == true ->
                    "এই অপশনটি ব্যবহার করতে সক্রিয় সাবস্ক্রিপশন প্রয়োজন।"
                raw?.startsWith("Permission denied:") == true ->
                    "অনুমতি নেই: অ্যাডমিন এই অপশনের অ্যাক্সেস বন্ধ রেখেছেন।"
                raw?.endsWith("is currently paused or out of stock.") == true ->
                    "এই পণ্যটি বর্তমানে বন্ধ আছে বা স্টকে নেই।"
                raw?.endsWith("is no longer available.") == true ->
                    "এই পণ্যটি আর পাওয়া যাচ্ছে না।"
                raw?.startsWith("Only ") == true ->
                    "স্টকে পর্যাপ্ত পরিমাণ নেই।"
                else -> raw ?: fallbackBn
            }
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

    fun loginSuperAdmin(identifier: String, password: String, securityPin: String) {
        viewModelScope.launch {
            _authError.value = null
            repository.loginSuperAdmin(identifier, password, securityPin)
                .onSuccess { user ->
                    prefsRepo.setCurrentUserId(user.id)
                    prefsRepo.setCurrency(user.preferredCurrency)
                    repository.checkExpiredSubscriptions()
                }
                .onFailure { _authError.value = localizeError(it.message, "Super Admin login failed.", "সুপার অ্যাডমিন লগইন ব্যর্থ হয়েছে।") }
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

    fun onGoogleAuthenticated(
        displayName: String,
        email: String,
        preferredRole: UserRole = UserRole.CUSTOMER
    ) {
        viewModelScope.launch {
            _authError.value = null
            repository.signInWithGoogleProfile(displayName, email, preferredRole)
                .onSuccess { user ->
                    prefsRepo.setCurrentUserId(user.id)
                    prefsRepo.setCurrency(user.preferredCurrency)
                    repository.checkExpiredSubscriptions()
                    _snackbarMessage.value = msg(
                        "Signed in with Google as ${user.name}.",
                        "গুগল অ্যাকাউন্টে লগইন সম্পন্ন হয়েছে: ${user.name}।"
                    )
                }
                .onFailure { _authError.value = localizeError(it.message, "Google Sign-In failed.", "গুগল লগইন ব্যর্থ হয়েছে।") }
        }
    }

    fun setAuthErrorMessage(message: String) {
        _authError.value = message
    }

    fun logout() {
        viewModelScope.launch {
            _cart.value = emptyList()
            _marketplaceSearch.value = ""
            _marketplaceCategory.value = null
            _marketplaceLimit.value = 30
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

    private fun effectiveOwnerId(): Long? {
        val user = currentUser.value ?: return null
        return if (user.role == UserRole.EMPLOYEE.name) {
            farmerState.value.ownerUserId.takeIf { it > 0L } ?: user.employerOwnerId ?: user.id
        } else user.id
    }

    private fun actorId(): Long? = currentUser.value?.id

    private suspend inline fun runPermitted(crossinline block: suspend (ownerId: Long, actorId: Long) -> Unit) {
        val ownerId = effectiveOwnerId() ?: return
        val actorId = actorId() ?: return
        runCatching { block(ownerId, actorId) }
            .onFailure { err ->
                _snackbarMessage.value = localizeError(
                    err.message,
                    "Action not permitted.",
                    "এই কাজের অনুমতি নেই।"
                )
            }
    }

    fun markNotificationsRead() {
        val ownerId = effectiveOwnerId() ?: return
        viewModelScope.launch {
            repository.markNotificationsRead(ownerId)
                .onFailure { _snackbarMessage.value = it.message ?: msg("Failed to update alerts.", "সতর্কতা আপডেট করা যায়নি।") }
        }
    }

    fun submitSupport(subject: String, message: String) {
        val user = currentUser.value ?: return
        viewModelScope.launch {
            repository.submitSupportRequest(user, subject, message)
                .onSuccess {
                    _snackbarMessage.value = msg("Support request submitted.", "সাপোর্ট অনুরোধ পাঠানো হয়েছে।")
                }
                .onFailure { err ->
                    _snackbarMessage.value = err.message ?: msg("Subject and message are required.", "বিষয় এবং বার্তা দিন।")
                }
        }
    }

    // --- Farm CRUD ---
    fun saveFarm(farm: FarmEntity) {
        val ownerId = effectiveOwnerId()
        val actor = actorId()
        if (ownerId == null || actor == null) {
            _snackbarMessage.value = msg("Please sign in to save a farm.", "খামার সংরক্ষণ করতে আগে লগইন করুন।")
            return
        }
        val hadNoFarms = farmerState.value.farms.isEmpty()
        viewModelScope.launch {
            repository.saveFarm(farm.copy(ownerUserId = ownerId), actorUserId = actor)
                .onSuccess { savedId ->
                    _snackbarMessage.value = msg("${farm.name.trim()} saved.", "${farm.name.trim()} সংরক্ষিত হয়েছে।")
                    if (preferences.value.activeFarmId == null && hadNoFarms) {
                        prefsRepo.setActiveFarmId(savedId)
                    }
                }
                .onFailure { err ->
                    val raw = err.message
                    _snackbarMessage.value = when {
                        raw == "Farm name is required." -> msg("Farm name is required.", "খামারের নাম আবশ্যক।")
                        raw == "Please sign in before creating a farm." -> msg("Please sign in before creating a farm.", "খামার তৈরি করার আগে লগইন করুন।")
                        raw?.startsWith("Farm ID '") == true -> msg(raw, "এই খামার আইডি ইতিমধ্যেই অন্য খামারে ব্যবহৃত হয়েছে।")
                        raw?.startsWith("Subscription required:") == true -> localizeError(raw, "Subscription required.", "সাবস্ক্রিপশন প্রয়োজন।")
                        else -> raw ?: msg("Could not save farm.", "খামার সংরক্ষণ করা যায়নি।")
                    }
                }
        }
    }

    fun deleteFarm(farm: FarmEntity) {
        val actor = actorId() ?: return
        viewModelScope.launch {
            repository.deleteFarm(farm, actorUserId = actor)
                .onSuccess {
                    if (preferences.value.activeFarmId == farm.id) prefsRepo.setActiveFarmId(null)
                    _snackbarMessage.value = msg("${farm.name} deleted.", "${farm.name} মুছে ফেলা হয়েছে।")
                }
                .onFailure { err ->
                    _snackbarMessage.value = err.message ?: msg("Permission denied.", "অনুমতি নেই।")
                }
        }
    }

    // --- Livestock ---
    fun addCustomAnimalCategory(name: String, isBird: Boolean) {
        viewModelScope.launch {
            runPermitted { uid, actor ->
                repository.addCustomAnimalCategory(uid, name, isBird, actorUserId = actor)
                _snackbarMessage.value = msg("Added category: $name", "ক্যাটাগরি যোগ হয়েছে: $name")
            }
        }
    }

    fun saveAnimalRecord(record: AnimalRecordEntity) {
        viewModelScope.launch {
            runPermitted { uid, actor ->
                repository.saveAnimalRecord(record.copy(ownerUserId = uid), actorUserId = actor)
                _snackbarMessage.value = msg("${record.name} saved.", "${record.name} সংরক্ষিত হয়েছে।")
            }
        }
    }

    fun deleteAnimalRecord(record: AnimalRecordEntity) {
        viewModelScope.launch {
            runPermitted { _, actor ->
                repository.deleteAnimalRecord(record, actorUserId = actor)
                _snackbarMessage.value = msg("${record.name} removed.", "${record.name} মুছে ফেলা হয়েছে।")
            }
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
            runPermitted { _, actor ->
                repository.recordAnimalEvent(animal, eventType, delta, costOrValue, description, actorUserId = actor)
                val eventLabel = DomainLocalization.animalEventType(eventType, isBn())
                _snackbarMessage.value = msg(
                    "$eventLabel recorded for ${animal.name}.",
                    "${animal.name}-এর জন্য $eventLabel সংরক্ষিত হয়েছে।"
                )
            }
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
        viewModelScope.launch {
            runPermitted { uid, actor ->
                repository.recordEggProduction(
                    farmId = farmId,
                    ownerId = uid,
                    eggCategory = eggCategory,
                    previousStockOverride = previousStockOverride,
                    producedCount = produced,
                    brokenCount = broken,
                    soldCount = sold,
                    unitSellingPrice = unitPrice,
                    actorUserId = actor
                )
                val catLabel = DomainLocalization.eggCategory(eggCategory, isBn())
                _snackbarMessage.value = msg(
                    "Egg production logged ($catLabel).",
                    "ডিম উৎপাদন সংরক্ষিত হয়েছে ($catLabel)।"
                )
            }
        }
    }

    fun deleteEggRecord(record: EggProductionEntity) {
        viewModelScope.launch {
            runPermitted { _, actor ->
                repository.deleteEggRecord(record, actorUserId = actor)
            }
        }
    }

    // --- Inventory ---
    fun saveInventoryItem(item: InventoryItemEntity) {
        viewModelScope.launch {
            runPermitted { uid, actor ->
                repository.saveInventoryItem(item.copy(ownerUserId = uid), actorUserId = actor)
                _snackbarMessage.value = msg(
                    "${item.name} saved to inventory.",
                    "${item.name} ইনভেন্টরিতে সংরক্ষিত হয়েছে।"
                )
            }
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
            runPermitted { _, actor ->
                repository.adjustInventoryStock(item, delta, isStockIn, unitCost, supplier, note, actorUserId = actor)
                _snackbarMessage.value = if (isStockIn) {
                    msg("Stock added to ${item.name}.", "${item.name}-এ স্টক যোগ হয়েছে।")
                } else {
                    msg("Stock deducted from ${item.name}.", "${item.name} থেকে স্টক কমানো হয়েছে।")
                }
            }
        }
    }

    fun deleteInventoryItem(item: InventoryItemEntity) {
        viewModelScope.launch {
            runPermitted { _, actor ->
                repository.deleteInventoryItem(item, actorUserId = actor)
            }
        }
    }

    // --- Online Product Add & Storefront ---
    fun saveOnlineProduct(item: InventoryItemEntity) {
        val ownerId = effectiveOwnerId()
        val actor = actorId()
        if (ownerId == null || actor == null) return
        viewModelScope.launch {
            repository.saveOnlineProduct(item.copy(ownerUserId = ownerId), actorUserId = actor)
                .onSuccess {
                    val liveLabel = if (item.listInMarketplace) {
                        msg("published to Online Marketplace.", "অনলাইন মার্কেটপ্লেসে প্রকাশিত হয়েছে।")
                    } else {
                        msg("saved as paused listing.", "খসড়া / বিরতি তালিকায় সংরক্ষিত হয়েছে।")
                    }
                    _snackbarMessage.value = "${item.name.trim()} $liveLabel"
                }
                .onFailure { err ->
                    _snackbarMessage.value = localizeError(
                        err.message,
                        "Could not save online product.",
                        "অনলাইন পণ্য সংরক্ষণ করা যায়নি।"
                    )
                }
        }
    }

    fun toggleOnlineProductVisibility(item: InventoryItemEntity, isLive: Boolean) {
        val actor = actorId() ?: return
        viewModelScope.launch {
            repository.setOnlineProductVisibility(item, isLive, actorUserId = actor)
                .onSuccess {
                    _snackbarMessage.value = if (isLive) {
                        msg("${item.name} is now LIVE online.", "${item.name} এখন অনলাইনে লাইভ হয়েছে।")
                    } else {
                        msg("${item.name} paused from online store.", "${item.name} অনলাইন বিক্রি থেকে বিরতিতে রাখা হয়েছে।")
                    }
                }
                .onFailure { err ->
                    _snackbarMessage.value = localizeError(
                        err.message,
                        "Action denied.",
                        "অনুমতি বা মূল্য পরীক্ষা করুন।"
                    )
                }
        }
    }

    fun deleteOnlineProduct(item: InventoryItemEntity) {
        val actor = actorId() ?: return
        viewModelScope.launch {
            repository.deleteOnlineProduct(item, actorUserId = actor)
                .onSuccess {
                    _snackbarMessage.value = msg("${item.name} deleted.", "${item.name} মুছে ফেলা হয়েছে।")
                }
                .onFailure { err ->
                    _snackbarMessage.value = localizeError(err.message, "Could not delete.", "মুছে ফেলা যায়নি।")
                }
        }
    }

    // --- Sales & Customers ---
    fun saveCustomer(customer: CustomerEntity) {
        viewModelScope.launch {
            runPermitted { uid, actor ->
                repository.saveCustomer(customer.copy(ownerUserId = uid), actorUserId = actor)
                _snackbarMessage.value = msg(
                    "Customer ${customer.name} saved.",
                    "গ্রাহক ${customer.name} সংরক্ষিত হয়েছে।"
                )
            }
        }
    }

    fun deleteCustomer(customer: CustomerEntity) {
        viewModelScope.launch {
            runPermitted { _, actor ->
                repository.deleteCustomer(customer, actorUserId = actor)
            }
        }
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
        viewModelScope.launch {
            runPermitted { uid, actor ->
                val sale = repository.createSale(
                    farmId, uid, customerId, customerName, customerPhone,
                    productCategory, productName, inventoryItem, animalRecord,
                    quantity, unitPrice, discount, paidAmount, paymentMethod,
                    actorUserId = actor
                )
                _snackbarMessage.value = msg(
                    "Sale recorded (${sale.invoiceNumber}).",
                    "বিক্রয় সংরক্ষিত হয়েছে (${sale.invoiceNumber})।"
                )
            }
        }
    }

    fun settleSaleDue(sale: SaleEntity, amount: Double) {
        viewModelScope.launch {
            runPermitted { _, actor ->
                repository.settleSaleDue(sale, amount, actorUserId = actor)
                _snackbarMessage.value = msg(
                    "Payment recorded on ${sale.invoiceNumber}.",
                    "${sale.invoiceNumber} এর বকেয়া পরিশোধিত হয়েছে।"
                )
            }
        }
    }

    fun deleteSale(sale: SaleEntity) {
        viewModelScope.launch {
            runPermitted { _, actor ->
                repository.deleteSale(sale, actorUserId = actor)
            }
        }
    }

    fun updateOrderStatus(order: OrderEntity, status: OrderStatus) {
        viewModelScope.launch {
            runPermitted { _, actor ->
                repository.updateOrderStatus(order, status, actorUserId = actor)
                _snackbarMessage.value = msg(
                    "Order ${order.orderNumber} marked ${status.label}.",
                    "অর্ডার ${order.orderNumber} আপডেট হয়েছে।"
                )
            }
        }
    }

    // --- Expenses & Employees ---
    fun addExpense(farmId: Long, category: String, amount: Double, description: String) {
        viewModelScope.launch {
            runPermitted { uid, actor ->
                repository.addExpense(
                    ExpenseEntity(
                        farmId = farmId,
                        ownerUserId = uid,
                        category = category,
                        amount = amount,
                        description = description
                    ),
                    actorUserId = actor
                )
                val catLabel = DomainLocalization.expenseCategory(category, isBn())
                _snackbarMessage.value = msg("Expense added ($catLabel).", "খরচ যোগ হয়েছে ($catLabel)।")
            }
        }
    }

    fun deleteExpense(expense: ExpenseEntity) {
        viewModelScope.launch {
            runPermitted { _, actor ->
                repository.deleteExpense(expense, actorUserId = actor)
            }
        }
    }

    // --- Agriculture, Lands, Crops & Sector Expenses ---
    fun saveAgriLand(land: AgriLandEntity) {
        val ownerId = effectiveOwnerId() ?: return
        val actor = actorId() ?: return
        viewModelScope.launch {
            repository.saveAgriLand(land.copy(ownerUserId = ownerId), actorUserId = actor)
                .onSuccess {
                    _snackbarMessage.value = msg(
                        "Land plot '${land.name.trim()}' saved.",
                        "জমি '${land.name.trim()}' সংরক্ষিত হয়েছে।"
                    )
                }
                .onFailure {
                    _snackbarMessage.value = localizeError(it.message, "Could not save land plot.", "জমি সংরক্ষণ করা যায়নি।")
                }
        }
    }

    fun deleteAgriLand(land: AgriLandEntity) {
        val actor = actorId() ?: return
        viewModelScope.launch {
            repository.deleteAgriLand(land, actorUserId = actor)
                .onSuccess {
                    _snackbarMessage.value = msg(
                        "Land plot '${land.name}' removed.",
                        "জমি '${land.name}' মুছে ফেলা হয়েছে।"
                    )
                }
                .onFailure {
                    _snackbarMessage.value = localizeError(it.message, "Action denied.", "অনুমতি নেই।")
                }
        }
    }

    fun saveAgriCrop(crop: AgriCropEntity) {
        val ownerId = effectiveOwnerId() ?: return
        val actor = actorId() ?: return
        viewModelScope.launch {
            repository.saveAgriCrop(crop.copy(ownerUserId = ownerId), actorUserId = actor)
                .onSuccess {
                    _snackbarMessage.value = msg(
                        "Crop '${crop.title.trim()}' saved.",
                        "ফসল '${crop.title.trim()}' সংরক্ষিত হয়েছে।"
                    )
                }
                .onFailure {
                    _snackbarMessage.value = localizeError(it.message, "Could not save crop.", "ফসল সংরক্ষণ করা যায়নি।")
                }
        }
    }

    fun recordAgriHarvest(
        crop: AgriCropEntity,
        yieldQuantity: Double,
        yieldUnit: String,
        marketPricePerUnit: Double,
        yieldMarketWorth: Double,
        soldAmount: Double,
        status: String
    ) {
        val actor = actorId() ?: return
        viewModelScope.launch {
            repository.recordAgriHarvest(
                crop = crop,
                yieldQuantity = yieldQuantity,
                yieldUnit = yieldUnit,
                marketPricePerUnit = marketPricePerUnit,
                yieldMarketWorth = yieldMarketWorth,
                soldAmount = soldAmount,
                status = status,
                actorUserId = actor
            ).onSuccess {
                _snackbarMessage.value = msg(
                    "Harvest & yield value updated for '${crop.title}'.",
                    "'${crop.title}' এর ফলন ও বাজার মূল্য আপডেট হয়েছে।"
                )
            }.onFailure {
                _snackbarMessage.value = localizeError(it.message, "Could not update harvest.", "ফলনের তথ্য আপডেট করা যায়নি।")
            }
        }
    }

    fun deleteAgriCrop(crop: AgriCropEntity) {
        val actor = actorId() ?: return
        viewModelScope.launch {
            repository.deleteAgriCrop(crop, actorUserId = actor)
                .onSuccess {
                    _snackbarMessage.value = msg(
                        "Crop '${crop.title}' removed.",
                        "ফসল '${crop.title}' মুছে ফেলা হয়েছে।"
                    )
                }
                .onFailure {
                    _snackbarMessage.value = localizeError(it.message, "Action denied.", "অনুমতি নেই।")
                }
        }
    }

    fun addAgriExpense(
        landId: Long,
        cropId: Long?,
        farmId: Long?,
        sector: String,
        customSectorName: String,
        amount: Double,
        description: String
    ) {
        val ownerId = effectiveOwnerId() ?: return
        val actor = actorId() ?: return
        viewModelScope.launch {
            repository.addAgriExpense(
                AgriExpenseEntity(
                    ownerUserId = ownerId,
                    farmId = farmId,
                    landId = landId,
                    cropId = cropId,
                    sector = sector,
                    customSectorName = customSectorName,
                    amount = amount,
                    description = description
                ),
                actorUserId = actor
            ).onSuccess {
                val secLabel = DomainLocalization.agriSector(sector, customSectorName, isBn())
                _snackbarMessage.value = msg(
                    "Agriculture expense recorded ($secLabel).",
                    "কৃষি খরচ সংরক্ষিত হয়েছে ($secLabel)।"
                )
            }.onFailure {
                _snackbarMessage.value = localizeError(it.message, "Could not record expense.", "খরচ সংরক্ষণ করা যায়নি।")
            }
        }
    }

    fun deleteAgriExpense(expense: AgriExpenseEntity) {
        val actor = actorId() ?: return
        viewModelScope.launch {
            repository.deleteAgriExpense(expense, actorUserId = actor)
                .onSuccess {
                    _snackbarMessage.value = msg("Expense deleted.", "খরচ মুছে ফেলা হয়েছে।")
                }
                .onFailure {
                    _snackbarMessage.value = localizeError(it.message, "Action denied.", "অনুমতি নেই।")
                }
        }
    }

    // --- AI Product Ads ---
    fun saveProductAd(ad: ProductAdEntity, onSaved: (Long) -> Unit = {}) {
        val ownerId = effectiveOwnerId() ?: return
        val actor = actorId() ?: return
        viewModelScope.launch {
            repository.saveProductAd(ad.copy(ownerUserId = ownerId), actorUserId = actor)
                .onSuccess { id ->
                    _snackbarMessage.value = msg(
                        "Ad saved & published for '${ad.productName.trim()}'!",
                        "'${ad.productName.trim()}' এর বিজ্ঞাপন সংরক্ষণ ও প্রকাশিত হয়েছে!"
                    )
                    onSaved(id)
                }
                .onFailure {
                    _snackbarMessage.value = localizeError(it.message, "Could not save ad.", "বিজ্ঞাপন সংরক্ষণ করা যায়নি।")
                }
        }
    }

    fun toggleProductAdActive(ad: ProductAdEntity, active: Boolean) {
        val actor = actorId() ?: return
        viewModelScope.launch {
            repository.toggleProductAdActive(ad, active, actorUserId = actor)
                .onSuccess {
                    _snackbarMessage.value = if (active) {
                        msg("Ad for '${ad.productName}' is now LIVE in Shop!", "'${ad.productName}' বিজ্ঞাপন এখন মার্কেটপ্লেসে চালু আছে!")
                    } else {
                        msg("Ad for '${ad.productName}' paused.", "'${ad.productName}' বিজ্ঞাপন বিরতিতে রাখা হয়েছে।")
                    }
                }
                .onFailure {
                    _snackbarMessage.value = localizeError(it.message, "Action denied.", "অনুমতি নেই।")
                }
        }
    }

    fun deleteProductAd(ad: ProductAdEntity) {
        val actor = actorId() ?: return
        viewModelScope.launch {
            repository.deleteProductAd(ad, actorUserId = actor)
                .onSuccess {
                    _snackbarMessage.value = msg("Ad removed.", "বিজ্ঞাপন মুছে ফেলা হয়েছে।")
                }
                .onFailure {
                    _snackbarMessage.value = localizeError(it.message, "Action denied.", "অনুমতি নেই।")
                }
        }
    }

    fun saveEmployee(emp: EmployeeEntity) {
        viewModelScope.launch {
            runPermitted { uid, actor ->
                repository.saveEmployee(emp.copy(ownerUserId = uid), actorUserId = actor)
                _snackbarMessage.value = msg(
                    "Employee ${emp.name} saved.",
                    "কর্মচারী ${emp.name} সংরক্ষিত হয়েছে।"
                )
            }
        }
    }

    fun deleteEmployee(emp: EmployeeEntity) {
        viewModelScope.launch {
            runPermitted { _, actor ->
                repository.deleteEmployee(emp, actorUserId = actor)
            }
        }
    }

    fun markAttendance(emp: EmployeeEntity, status: AttendanceStatus) {
        viewModelScope.launch {
            runPermitted { _, actor ->
                repository.markEmployeeAttendance(emp, status, actorUserId = actor)
                val stLabel = DomainLocalization.attendanceStatus(status, isBn())
                _snackbarMessage.value = msg(
                    "${emp.name} marked $stLabel.",
                    "${emp.name} কে '$stLabel' চিহ্নিত করা হয়েছে।"
                )
            }
        }
    }

    fun paySalary(emp: EmployeeEntity, amount: Double, monthLabel: String, note: String) {
        viewModelScope.launch {
            runPermitted { _, actor ->
                repository.payEmployeeSalary(emp, amount, monthLabel, note, actorUserId = actor)
                _snackbarMessage.value = msg(
                    "Salary paid to ${emp.name}.",
                    "${emp.name} কে বেতন প্রদান করা হয়েছে।"
                )
            }
        }
    }

    // --- Admin-Only User Permissions Management ---
    fun setEmployeePermission(employee: EmployeeEntity, permissionKey: String, granted: Boolean) {
        val actor = actorId() ?: return
        viewModelScope.launch {
            repository.setEmployeePermission(actor, employee, permissionKey, granted)
                .onFailure { _snackbarMessage.value = it.message ?: msg("Action denied.", "অনুমতি নেই।") }
        }
    }

    fun setAllPermissionsForEmployee(employee: EmployeeEntity, granted: Boolean) {
        val actor = actorId() ?: return
        val customOpts = if (currentUser.value?.role == UserRole.SUPER_ADMIN.name) {
            adminState.value.customPermissionOptions.filter { it.ownerUserId == employee.ownerUserId }
        } else {
            farmerState.value.customPermissionOptions
        }
        val keys = PermissionCatalog.allDescriptors(customOpts).map { it.key }
        viewModelScope.launch {
            repository.setAllPermissionsForEmployee(actor, employee, keys, granted)
                .onSuccess {
                    _snackbarMessage.value = if (granted) {
                        msg("All permissions granted to ${employee.name}.", "${employee.name} কে সকল অনুমতি দেওয়া হয়েছে।")
                    } else {
                        msg("All permissions revoked for ${employee.name}.", "${employee.name}-এর সকল অনুমতি বাতিল করা হয়েছে।")
                    }
                }
                .onFailure { _snackbarMessage.value = it.message ?: msg("Action denied.", "অনুমতি নেই।") }
        }
    }

    fun applyPermissionPreset(employee: EmployeeEntity, preset: PermissionPreset) {
        val actor = actorId() ?: return
        val customOpts = if (currentUser.value?.role == UserRole.SUPER_ADMIN.name) {
            adminState.value.customPermissionOptions.filter { it.ownerUserId == employee.ownerUserId }
        } else {
            farmerState.value.customPermissionOptions
        }
        val allKeys = PermissionCatalog.allDescriptors(customOpts).map { it.key }
        viewModelScope.launch {
            repository.applyPermissionPreset(actor, employee, preset, allKeys)
                .onSuccess {
                    val label = if (isBn()) preset.labelBn else preset.labelEn
                    _snackbarMessage.value = msg(
                        "Applied '$label' to ${employee.name}.",
                        "${employee.name}-এর জন্য '$label' প্রয়োগ করা হয়েছে।"
                    )
                }
                .onFailure { _snackbarMessage.value = it.message ?: msg("Action denied.", "অনুমতি নেই।") }
        }
    }

    fun setPermissionForMultipleEmployees(
        ownerUserId: Long,
        employees: List<EmployeeEntity>,
        permissionKey: String,
        granted: Boolean
    ) {
        val actor = actorId() ?: return
        viewModelScope.launch {
            repository.setPermissionForMultipleEmployees(actor, ownerUserId, employees, permissionKey, granted)
                .onSuccess {
                    _snackbarMessage.value = if (granted) {
                        msg("Granted to ${employees.size} employees.", "${employees.size} জন কর্মচারীকে অনুমতি দেওয়া হয়েছে।")
                    } else {
                        msg("Revoked for ${employees.size} employees.", "${employees.size} জন কর্মচারীর অনুমতি বাতিল করা হয়েছে।")
                    }
                }
                .onFailure { _snackbarMessage.value = it.message ?: msg("Action denied.", "অনুমতি নেই।") }
        }
    }

    fun addCustomPermissionOption(
        ownerUserId: Long,
        titleEn: String,
        titleBn: String,
        descEn: String,
        descBn: String
    ) {
        val actor = actorId() ?: return
        viewModelScope.launch {
            repository.addCustomPermissionOption(actor, ownerUserId, titleEn, titleBn, descEn, descBn)
                .onSuccess {
                    _snackbarMessage.value = msg(
                        "Added permission option: ${it.titleEn}",
                        "নতুন অনুমতি অপশন যোগ হয়েছে: ${it.titleBn}"
                    )
                }
                .onFailure { _snackbarMessage.value = it.message ?: msg("Action denied.", "অনুমতি নেই।") }
        }
    }

    fun deleteCustomPermissionOption(option: CustomPermissionOptionEntity) {
        val actor = actorId() ?: return
        viewModelScope.launch {
            repository.deleteCustomPermissionOption(actor, option)
                .onSuccess {
                    _snackbarMessage.value = msg("Option removed.", "অপশন মুছে ফেলা হয়েছে।")
                }
                .onFailure { _snackbarMessage.value = it.message ?: msg("Action denied.", "অনুমতি নেই।") }
        }
    }

    fun provisionEmployeeLogin(
        employee: EmployeeEntity,
        email: String,
        mobile: String,
        password: String,
        onDone: () -> Unit = {}
    ) {
        val actor = actorId() ?: return
        viewModelScope.launch {
            repository.provisionEmployeeLogin(actor, employee, email, mobile, password)
                .onSuccess {
                    _snackbarMessage.value = msg(
                        "Login ready for ${employee.name} (${it.email}).",
                        "${employee.name}-এর লগইন সংরক্ষিত হয়েছে (${it.email})।"
                    )
                    onDone()
                }
                .onFailure { _snackbarMessage.value = localizeError(it.message, "Could not save employee login.", "কর্মচারীর লগইন সংরক্ষণ করা যায়নি।") }
        }
    }

    // --- Subscriptions ---
    fun subscribe(plan: SubscriptionPlanEntity, currencyCode: String, method: String, txRef: String) {
        val user = currentUser.value ?: return
        val ownerId = effectiveOwnerId() ?: user.id
        viewModelScope.launch {
            val ownerUser = if (ownerId == user.id) user else user.copy(id = ownerId)
            repository.purchaseSubscription(ownerUser, plan, currencyCode, method, txRef, actorUserId = user.id)
                .onSuccess {
                    _snackbarMessage.value = msg(
                        "Subscription processed for ${plan.name}.",
                        "${plan.name} সাবস্ক্রিপশন প্রক্রিয়া সম্পন্ন হয়েছে।"
                    )
                }
                .onFailure { err ->
                    _snackbarMessage.value = err.message ?: msg(
                        "Could not process subscription.",
                        "সাবস্ক্রিপশন সম্পন্ন করা যায়নি।"
                    )
                }
        }
    }

    // --- Customer Shopping Cart & Ordering ---
    fun updateMarketplaceSearch(query: String) {
        _marketplaceSearch.value = query
        _marketplaceLimit.value = 30
    }

    fun selectMarketplaceCategory(category: String?) {
        _marketplaceCategory.value = category
        _marketplaceLimit.value = 30
    }

    fun loadMoreMarketplaceItems() {
        _marketplaceLimit.update { it + 30 }
    }

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
                _snackbarMessage.value = localizeError(it.message, "Failed to place order.", "অর্ডার করা যায়নি।")
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
                .onSuccess { _snackbarMessage.value = msg("System & payment settings saved.", "সিস্টেম ও পেমেন্ট সেটিংস সংরক্ষিত হয়েছে।") }
                .onFailure { _snackbarMessage.value = it.message ?: msg("Action denied.", "অনুমতি নেই।") }
        }
    }

    fun adminToggleFeatureSubscription(feature: SubscriptionFeature, requiresSubscription: Boolean) {
        val adminId = currentUser.value?.id ?: return
        viewModelScope.launch {
            repository.adminToggleFeatureSubscription(adminId, feature.key, requiresSubscription)
                .onSuccess {
                    val label = if (isBn()) feature.titleBn else feature.titleEn
                    _snackbarMessage.value = if (requiresSubscription) {
                        msg("$label now requires Subscription.", "$label এখন সাবস্ক্রিপশন প্রয়োজন।")
                    } else {
                        msg("$label is now Free.", "$label এখন ফ্রি করা হয়েছে।")
                    }
                }
                .onFailure { _snackbarMessage.value = it.message ?: msg("Action denied.", "অনুমতি নেই।") }
        }
    }

    fun adminSetAllFeatureSubscriptions(lockedKeys: Set<String>) {
        val adminId = currentUser.value?.id ?: return
        viewModelScope.launch {
            repository.adminSetLockedSubscriptionFeatures(adminId, lockedKeys)
                .onSuccess {
                    _snackbarMessage.value = msg(
                        "Updated Free vs Subscription feature tiers.",
                        "ফ্রি ও সাবস্ক্রিপশন ফিচার তালিকা আপডেট হয়েছে।"
                    )
                }
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

    fun adminChangeCredentials(
        currentPassword: String,
        newEmail: String,
        newPassword: String,
        newSecurityPin: String,
        onDone: () -> Unit = {}
    ) {
        val adminId = currentUser.value?.id ?: return
        viewModelScope.launch {
            repository.adminChangeCredentials(adminId, currentPassword, newEmail, newPassword, newSecurityPin)
                .onSuccess {
                    _snackbarMessage.value = msg(
                        "Super Admin password & security updated.",
                        "সুপার অ্যাডমিন পাসওয়ার্ড ও সিকিউরিটি আপডেট হয়েছে।"
                    )
                    onDone()
                }
                .onFailure { err ->
                    _snackbarMessage.value = localizeError(err.message, "Could not update credentials.", "পাসওয়ার্ড আপডেট করা যায়নি।")
                }
        }
    }

    fun adminDeleteOnlineShopProduct(item: InventoryItemEntity) {
        val adminId = currentUser.value?.id ?: return
        viewModelScope.launch {
            repository.adminDeleteOnlineShopProduct(adminId, item)
                .onSuccess {
                    _snackbarMessage.value = msg(
                        "'${item.name}' removed from online shop.",
                        "'${item.name}' অনলাইন শপ থেকে মুছে ফেলা হয়েছে।"
                    )
                }
                .onFailure { err ->
                    _snackbarMessage.value = localizeError(err.message, "Could not remove product.", "পণ্যটি মুছে ফেলা যায়নি।")
                }
        }
    }

    fun adminDeleteProductAd(ad: ProductAdEntity) {
        val adminId = currentUser.value?.id ?: return
        viewModelScope.launch {
            repository.adminDeleteProductAd(adminId, ad)
                .onSuccess {
                    _snackbarMessage.value = msg(
                        "Ad '${ad.productName}' deleted.",
                        "'${ad.productName}' বিজ্ঞাপন মুছে ফেলা হয়েছে।"
                    )
                }
                .onFailure { err ->
                    _snackbarMessage.value = localizeError(err.message, "Could not delete ad.", "বিজ্ঞাপন মুছে ফেলা যায়নি।")
                }
        }
    }

    fun adminDeleteUserCompletely(target: UserEntity) {
        val adminId = currentUser.value?.id ?: return
        viewModelScope.launch {
            repository.adminDeleteUserCompletely(adminId, target)
                .onSuccess {
                    _snackbarMessage.value = msg(
                        "User ID #${target.id} (${target.name}) and all shop products deleted.",
                        "আইডি #${target.id} (${target.name}) এবং এর সকল পণ্য সম্পূর্ণ মুছে ফেলা হয়েছে।"
                    )
                }
                .onFailure { err ->
                    _snackbarMessage.value = localizeError(err.message, "Could not delete user.", "ব্যবহারকারী মুছে ফেলা যায়নি।")
                }
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
