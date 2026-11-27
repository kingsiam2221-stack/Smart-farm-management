package com.example.data.repository

import androidx.room.withTransaction
import com.example.data.local.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlin.math.max

class FarmRepository(private val db: SmartFarmDatabase) {
    private val userDao = db.userDao()
    private val farmDao = db.farmDao()
    private val animalDao = db.animalDao()
    private val eggDao = db.eggDao()
    private val inventoryDao = db.inventoryDao()
    private val commerceDao = db.commerceDao()
    private val opsDao = db.operationsDao()
    private val subAdminDao = db.subscriptionAndAdminDao()
    private val permissionDao = db.permissionDao()
    private val agriDao = db.agriDao()
    private val productAdDao = db.productAdDao()

    suspend fun initializeDatabase() {
        DatabaseSeeder.seedIfEmpty(db)
        checkExpiredSubscriptions()
    }

    // --- Authentication & Profile ---
    fun observeUser(userId: Long): Flow<UserEntity?> = userDao.observeUserById(userId)

    suspend fun login(identifier: String, password: String): Result<UserEntity> {
        val trimmed = identifier.trim()
        if (trimmed.isBlank() || password.isBlank()) {
            return Result.failure(IllegalArgumentException("Email/mobile and password are required."))
        }
        val user = userDao.getUserByEmail(trimmed) ?: userDao.getUserByMobile(trimmed)
            ?: return Result.failure(IllegalArgumentException("No account found with those credentials."))
        if (!PasswordHasher.verify(password, user.passwordHash)) {
            return Result.failure(IllegalArgumentException("Incorrect password."))
        }
        if (user.status == AccountStatus.SUSPENDED.name) {
            return Result.failure(IllegalStateException("This account is suspended. Contact Super Admin."))
        }
        if (user.status == AccountStatus.INACTIVE.name) {
            return Result.failure(IllegalStateException("This account is inactive. Contact Super Admin."))
        }
        return Result.success(user)
    }

    suspend fun loginSuperAdmin(
        identifier: String,
        password: String,
        securityPin: String
    ): Result<UserEntity> {
        val trimmed = identifier.trim()
        val trimmedPin = securityPin.trim()
        if (trimmed.isBlank() || password.isBlank() || trimmedPin.isBlank()) {
            return Result.failure(IllegalArgumentException("Email, password, and security PIN are required."))
        }
        val user = userDao.getUserByEmail(trimmed) ?: userDao.getUserByMobile(trimmed)
            ?: return Result.failure(IllegalArgumentException("No account found with those credentials."))
        if (user.role != UserRole.SUPER_ADMIN.name) {
            return Result.failure(SecurityException("Access Denied: Super Admin role required."))
        }
        if (!PasswordHasher.verify(password, user.passwordHash)) {
            return Result.failure(IllegalArgumentException("Incorrect password."))
        }
        val settings = subAdminDao.observeSystemSettings().firstOrNull() ?: SystemSettingsEntity()
        val validPin = if (settings.adminPinHash.isNotBlank()) {
            PasswordHasher.verify(trimmedPin, settings.adminPinHash)
        } else {
            trimmedPin == "2026"
        }
        if (!validPin) {
            return Result.failure(SecurityException("Invalid Developer Security PIN."))
        }
        if (user.status == AccountStatus.SUSPENDED.name) {
            return Result.failure(IllegalStateException("This account is suspended. Contact Super Admin."))
        }
        if (user.status == AccountStatus.INACTIVE.name) {
            return Result.failure(IllegalStateException("This account is inactive. Contact Super Admin."))
        }
        return Result.success(user)
    }

    suspend fun register(
        name: String,
        mobile: String,
        email: String,
        password: String,
        role: UserRole
    ): Result<UserEntity> {
        if (name.isBlank() || mobile.isBlank() || email.isBlank() || password.length < 4) {
            return Result.failure(IllegalArgumentException("Please complete all fields (password min 4 chars)."))
        }
        if (role == UserRole.SUPER_ADMIN || role == UserRole.EMPLOYEE) {
            return Result.failure(SecurityException("Public registration for this role is restricted."))
        }
        if (userDao.getUserByEmail(email.trim()) != null) {
            return Result.failure(IllegalArgumentException("An account with this email already exists."))
        }
        val user = UserEntity(
            name = name.trim(),
            mobile = mobile.trim(),
            email = email.trim().lowercase(),
            passwordHash = PasswordHasher.hash(password),
            role = role.name,
            status = AccountStatus.ACTIVE.name
        )
        val id = userDao.insert(user)
        val inserted = user.copy(id = id)
        subAdminDao.insertNotification(
            AppNotificationEntity(
                targetUserId = id,
                title = "Welcome to Smart Buy and Sell",
                message = "Your ${role.name.lowercase()} account is ready.",
                category = "SYSTEM"
            )
        )
        return Result.success(inserted)
    }

    suspend fun signInWithGoogleProfile(
        name: String,
        email: String,
        preferredRole: UserRole = UserRole.CUSTOMER
    ): Result<UserEntity> {
        val cleanEmail = email.trim().lowercase()
        if (cleanEmail.isBlank()) {
            return Result.failure(IllegalArgumentException("Google account email is missing."))
        }
        val safeRole = if (preferredRole == UserRole.FARMER) UserRole.FARMER else UserRole.CUSTOMER
        val existing = userDao.getUserByEmail(cleanEmail)
        if (existing != null) {
            if (existing.status == AccountStatus.SUSPENDED.name) {
                return Result.failure(IllegalStateException("This account is suspended. Contact Super Admin."))
            }
            if (existing.status == AccountStatus.INACTIVE.name) {
                return Result.failure(IllegalStateException("This account is inactive. Contact Super Admin."))
            }
            return Result.success(existing)
        }
        val cleanName = name.trim().ifBlank { cleanEmail.substringBefore("@") }
        val user = UserEntity(
            name = cleanName,
            mobile = "-",
            email = cleanEmail,
            passwordHash = PasswordHasher.hash("google-oauth-${System.nanoTime()}"),
            role = safeRole.name,
            status = AccountStatus.ACTIVE.name
        )
        val id = userDao.insert(user)
        val inserted = user.copy(id = id)
        subAdminDao.insertNotification(
            AppNotificationEntity(
                targetUserId = id,
                title = "Welcome to Smart Buy and Sell",
                message = "Signed in with Google. Your ${safeRole.name.lowercase()} cloud account is ready.",
                category = "SYSTEM"
            )
        )
        return Result.success(inserted)
    }

    suspend fun resetPassword(identifier: String, newPassword: String): Result<Unit> {
        if (newPassword.length < 4) {
            return Result.failure(IllegalArgumentException("New password must be at least 4 characters."))
        }
        val user = userDao.getUserByEmail(identifier.trim()) ?: userDao.getUserByMobile(identifier.trim())
            ?: return Result.failure(IllegalArgumentException("Account not found."))
        if (user.role == UserRole.SUPER_ADMIN.name) {
            return Result.failure(SecurityException("Super Admin password cannot be reset publicly."))
        }
        userDao.update(user.copy(passwordHash = PasswordHasher.hash(newPassword)))
        return Result.success(Unit)
    }

    suspend fun updateProfile(user: UserEntity, name: String, mobile: String, imageUri: String?): UserEntity {
        val updated = user.copy(name = name.trim(), mobile = mobile.trim(), profileImageUri = imageUri)
        userDao.update(updated)
        return updated
    }

    // --- Farmer Streams ---
    fun observeFarms(ownerId: Long): Flow<List<FarmEntity>> = farmDao.observeFarmsByOwner(ownerId)
    fun observeAnimalCategories(ownerId: Long): Flow<List<AnimalCategoryEntity>> = animalDao.observeCategories(ownerId)
    fun observeAnimals(ownerId: Long): Flow<List<AnimalRecordEntity>> = animalDao.observeAnimalsByOwner(ownerId)
    fun observeAnimalEvents(ownerId: Long): Flow<List<AnimalEventEntity>> = animalDao.observeEventsByOwner(ownerId)
    fun observeEggs(ownerId: Long): Flow<List<EggProductionEntity>> = eggDao.observeEggsByOwner(ownerId)
    fun observeInventory(ownerId: Long): Flow<List<InventoryItemEntity>> = inventoryDao.observeInventoryByOwner(ownerId)
    fun observeInventoryMovements(ownerId: Long): Flow<List<InventoryMovementEntity>> = inventoryDao.observeMovementsByOwner(ownerId)
    fun observeCustomers(ownerId: Long): Flow<List<CustomerEntity>> = commerceDao.observeCustomersByOwner(ownerId)
    fun observeSales(ownerId: Long): Flow<List<SaleEntity>> = commerceDao.observeSalesByOwner(ownerId)
    fun observeFarmerOrders(ownerId: Long): Flow<List<OrderEntity>> = commerceDao.observeOrdersForFarmer(ownerId)
    fun observeExpenses(ownerId: Long): Flow<List<ExpenseEntity>> = opsDao.observeExpensesByOwner(ownerId)
    fun observeEmployees(ownerId: Long): Flow<List<EmployeeEntity>> = opsDao.observeEmployeesByOwner(ownerId)
    fun observeSalaryPayments(ownerId: Long): Flow<List<SalaryPaymentEntity>> = opsDao.observeSalaryPaymentsByOwner(ownerId)
    fun observeTaskTargets(ownerId: Long): Flow<List<EmployeeTaskTargetEntity>> = opsDao.observeTaskTargetsByOwner(ownerId)
    fun observeAutoMessages(ownerId: Long): Flow<List<EmployeeAutoMessageEntity>> = opsDao.observeAutoMessagesByOwner(ownerId)
    fun observeUserSubscriptions(userId: Long): Flow<List<UserSubscriptionEntity>> = subAdminDao.observeSubscriptionsByUser(userId)
    fun observeNotifications(userId: Long): Flow<List<AppNotificationEntity>> = subAdminDao.observeNotificationsForUser(userId)
    fun observeNotificationsForUserOrOwner(userId: Long, ownerId: Long): Flow<List<AppNotificationEntity>> =
        subAdminDao.observeNotificationsForUserOrOwner(userId, ownerId)
    fun observePlans(): Flow<List<SubscriptionPlanEntity>> = subAdminDao.observePlans()
    fun observeSystemSettings(): Flow<SystemSettingsEntity?> = subAdminDao.observeSystemSettings()
    fun observeUserSupport(userId: Long): Flow<List<SupportRequestEntity>> = subAdminDao.observeSupportByUser(userId)

    // --- Agriculture, Lands, Crops & Sector Expenses ---
    fun observeAgriLands(ownerId: Long): Flow<List<AgriLandEntity>> = agriDao.observeLandsByOwner(ownerId)
    fun observeAgriCrops(ownerId: Long): Flow<List<AgriCropEntity>> = agriDao.observeCropsByOwner(ownerId)
    fun observeAgriExpenses(ownerId: Long): Flow<List<AgriExpenseEntity>> = agriDao.observeExpensesByOwner(ownerId)

    // --- AI Product Ads ---
    fun observeProductAds(ownerId: Long): Flow<List<ProductAdEntity>> = productAdDao.observeAdsByOwner(ownerId)
    fun observeActiveShopAds(): Flow<List<ProductAdEntity>> = productAdDao.observeActiveShopAds()

    // --- Customer Marketplace Streams ---
    fun observeMarketplaceItems(): Flow<List<InventoryItemEntity>> = inventoryDao.observeMarketplaceItems()
    fun observeMarketplaceItemsFiltered(search: String, category: String?, limit: Int): Flow<List<InventoryItemEntity>> =
        inventoryDao.observeMarketplaceItemsFiltered(search, category, limit)
    fun countMarketplaceItemsFiltered(search: String, category: String?): Flow<Int> =
        inventoryDao.countMarketplaceItemsFiltered(search, category)
    fun observeMarketplaceCategories(): Flow<List<String>> = inventoryDao.observeMarketplaceCategories()
    fun observeMarketplaceFarms(): Flow<List<FarmEntity>> = farmDao.observeMarketplaceFarms()
    fun observeAllFarmsPublic(): Flow<List<FarmEntity>> = farmDao.observeAllFarms()
    fun observeCustomerOrders(customerId: Long): Flow<List<OrderEntity>> = commerceDao.observeOrdersForCustomer(customerId)

    // --- Super Admin Streams ---
    fun observeAllUsers(): Flow<List<UserEntity>> = userDao.observeAllUsers()
    fun observeAllFarmsAdmin(): Flow<List<FarmEntity>> = farmDao.observeAllFarms()
    fun observeAllInventoryAdmin(): Flow<List<InventoryItemEntity>> = inventoryDao.observeAllInventoryAdmin()
    fun observeAllAdsAdmin(): Flow<List<ProductAdEntity>> = productAdDao.observeAllAdsAdmin()
    fun observeAllSalesAdmin(): Flow<List<SaleEntity>> = commerceDao.observeAllSales()
    fun observeAllExpensesAdmin(): Flow<List<ExpenseEntity>> = opsDao.observeAllExpenses()
    fun observeAllOrdersAdmin(): Flow<List<OrderEntity>> = commerceDao.observeAllOrders()
    fun observeAllSubscriptionsAdmin(): Flow<List<UserSubscriptionEntity>> = subAdminDao.observeAllSubscriptions()
    fun observeAllSupportAdmin(): Flow<List<SupportRequestEntity>> = subAdminDao.observeAllSupport()
    fun observeAuditLogs(): Flow<List<AdminAuditLogEntity>> = subAdminDao.observeAuditLogs()

    // --- Employee Permissions & Context Streams ---
    fun observePermissionsByOwner(ownerId: Long): Flow<List<EmployeePermissionEntity>> =
        permissionDao.observePermissionsByOwner(ownerId)

    fun observeAllPermissions(): Flow<List<EmployeePermissionEntity>> =
        permissionDao.observeAllPermissions()

    fun observePermissionsForEmployee(employeeId: Long): Flow<List<EmployeePermissionEntity>> =
        permissionDao.observePermissionsForEmployee(employeeId)

    fun observeCustomOptionsByOwner(ownerId: Long): Flow<List<CustomPermissionOptionEntity>> =
        permissionDao.observeCustomOptionsByOwner(ownerId)

    fun observeAllCustomOptions(): Flow<List<CustomPermissionOptionEntity>> =
        permissionDao.observeAllCustomOptions()

    fun observeAllEmployeesAdmin(): Flow<List<EmployeeEntity>> =
        opsDao.observeAllEmployees()

    suspend fun resolveEmployeeContext(user: UserEntity): Pair<Long, Long?> {
        if (user.role != UserRole.EMPLOYEE.name) return user.id to null
        val linked = opsDao.getEmployeeByLinkedUserId(user.id)
        val ownerId = user.employerOwnerId ?: linked?.ownerUserId ?: user.id
        return ownerId to linked?.id
    }

    private suspend fun requireFarmAdminOrSuperAdmin(actorUserId: Long, targetOwnerId: Long): UserEntity {
        val actor = userDao.getUserById(actorUserId)
            ?: throw SecurityException("Unauthorized: user not found.")
        if (actor.status != AccountStatus.ACTIVE.name) {
            throw SecurityException("Account is not active.")
        }
        val isSuperAdmin = actor.role == UserRole.SUPER_ADMIN.name
        val isOwnerAdmin = actor.role == UserRole.FARMER.name && actor.id == targetOwnerId
        if (!isSuperAdmin && !isOwnerAdmin) {
            throw SecurityException("Access Denied: Only Admin can manage user permissions.")
        }
        return actor
    }

    suspend fun requireActorPermission(
        actorUserId: Long?,
        targetOwnerId: Long,
        permission: AppPermission
    ) {
        if (actorUserId == null || actorUserId == targetOwnerId) return
        val actor = userDao.getUserById(actorUserId)
            ?: throw SecurityException("Unauthorized: user not found.")
        if (actor.status != AccountStatus.ACTIVE.name) {
            throw SecurityException("Account is not active.")
        }
        if (actor.role == UserRole.SUPER_ADMIN.name) return
        if (actor.role == UserRole.FARMER.name && actor.id == targetOwnerId) return
        if (actor.role == UserRole.EMPLOYEE.name && actor.employerOwnerId == targetOwnerId) {
            val emp = opsDao.getEmployeeByLinkedUserId(actor.id)
                ?: throw SecurityException("Employee profile not linked.")
            val row = permissionDao.getPermission(emp.id, permission.key)
            if (row?.granted == true) return
            throw SecurityException("Permission denied: ${permission.titleEn}")
        }
        throw SecurityException("Access Denied.")
    }

    private suspend fun requireAnyActorPermission(
        actorUserId: Long?,
        targetOwnerId: Long,
        vararg permissions: AppPermission
    ) {
        if (actorUserId == null || actorUserId == targetOwnerId) return
        val actor = userDao.getUserById(actorUserId)
            ?: throw SecurityException("Unauthorized: user not found.")
        if (actor.status != AccountStatus.ACTIVE.name) {
            throw SecurityException("Account is not active.")
        }
        if (actor.role == UserRole.SUPER_ADMIN.name) return
        if (actor.role == UserRole.FARMER.name && actor.id == targetOwnerId) return
        if (actor.role == UserRole.EMPLOYEE.name && actor.employerOwnerId == targetOwnerId) {
            val emp = opsDao.getEmployeeByLinkedUserId(actor.id)
                ?: throw SecurityException("Employee profile not linked.")
            val anyGranted = permissions.any { perm ->
                permissionDao.getPermission(emp.id, perm.key)?.granted == true
            }
            if (anyGranted) return
            throw SecurityException("Permission denied: ${permissions.firstOrNull()?.titleEn ?: "Action"}")
        }
        throw SecurityException("Access Denied.")
    }

    private suspend fun requireActiveSubscription(
        ownerId: Long,
        actorUserId: Long?,
        feature: SubscriptionFeature
    ) {
        if (actorUserId != null) {
            val actor = userDao.getUserById(actorUserId)
            if (actor?.role == UserRole.SUPER_ADMIN.name) return
        }
        val settings = subAdminDao.observeSystemSettings().firstOrNull() ?: SystemSettingsEntity()
        val lockedKeys = settings.parseLockedFeatureKeys()
        if (!lockedKeys.contains(feature.key)) return

        val subs = subAdminDao.observeSubscriptionsByUser(ownerId).firstOrNull().orEmpty()
        val now = System.currentTimeMillis()
        val hasActive = subs.any {
            it.status == "ACTIVE" &&
                it.paymentStatus == PaymentStatus.SUCCESSFUL.name &&
                it.expiryDate > now
        }
        if (!hasActive) {
            throw SecurityException("Subscription required: ${feature.titleEn}")
        }
    }

    // --- Farm CRUD ---
    private suspend fun generateUniqueFarmCode(preferredCode: String): String {
        val base = preferredCode.trim().ifBlank { "FM-${(1000..9999).random()}" }
        if (farmDao.getFarmByCode(base) == null) return base
        repeat(20) {
            val candidate = if (preferredCode.isBlank()) {
                "FM-${(1000..9999).random()}"
            } else {
                "$base-${(10..99).random()}"
            }
            if (farmDao.getFarmByCode(candidate) == null) return candidate
        }
        return "FM-${System.currentTimeMillis().toString().takeLast(6)}"
    }

    suspend fun saveFarm(farm: FarmEntity, actorUserId: Long? = null): Result<Long> = runCatching {
        requireActorPermission(actorUserId, farm.ownerUserId, AppPermission.FARMS_CREATE_EDIT)
        requireActiveSubscription(farm.ownerUserId, actorUserId, SubscriptionFeature.MULTI_FARM_MANAGEMENT)
        val cleanName = farm.name.trim()
        if (cleanName.isBlank()) {
            throw IllegalArgumentException("Farm name is required.")
        }
        userDao.getUserById(farm.ownerUserId)
            ?: throw IllegalStateException("Please sign in before creating a farm.")

        if (farm.id == 0L) {
            val uniqueCode = generateUniqueFarmCode(farm.farmCode)
            farmDao.insert(
                farm.copy(
                    name = cleanName,
                    farmCode = uniqueCode,
                    location = farm.location.trim(),
                    address = farm.address.trim(),
                    bkashNumber = farm.bkashNumber.trim(),
                    nagadNumber = farm.nagadNumber.trim(),
                    bankAccountInfo = farm.bankAccountInfo.trim()
                )
            )
        } else {
            val trimmedCode = farm.farmCode.trim().ifBlank { "FM-${farm.id}" }
            val conflicting = farmDao.getFarmByCode(trimmedCode)
            if (conflicting != null && conflicting.id != farm.id) {
                throw IllegalArgumentException("Farm ID '$trimmedCode' is already used by another farm.")
            }
            farmDao.update(
                farm.copy(
                    name = cleanName,
                    farmCode = trimmedCode,
                    location = farm.location.trim(),
                    address = farm.address.trim(),
                    bkashNumber = farm.bkashNumber.trim(),
                    nagadNumber = farm.nagadNumber.trim(),
                    bankAccountInfo = farm.bankAccountInfo.trim()
                )
            )
            farm.id
        }
    }

    suspend fun deleteFarm(farm: FarmEntity, actorUserId: Long? = null): Result<Unit> = runCatching {
        requireActorPermission(actorUserId, farm.ownerUserId, AppPermission.FARMS_DELETE)
        farmDao.delete(farm)
    }

    // --- Animal & Bird CRUD + Auto Count Sync ---
    suspend fun addCustomAnimalCategory(ownerId: Long, name: String, isBird: Boolean, actorUserId: Long? = null) {
        requireActorPermission(actorUserId, ownerId, AppPermission.LIVESTOCK_CREATE_EDIT)
        requireActiveSubscription(ownerId, actorUserId, SubscriptionFeature.LIVESTOCK_TRACKING)
        if (name.isBlank()) return
        animalDao.insertCategory(
            AnimalCategoryEntity(
                ownerUserId = ownerId,
                name = name.trim(),
                birdCategory = isBird,
                builtIn = false
            )
        )
    }

    suspend fun saveAnimalRecord(record: AnimalRecordEntity, actorUserId: Long? = null): Long {
        requireActorPermission(actorUserId, record.ownerUserId, AppPermission.LIVESTOCK_CREATE_EDIT)
        requireActiveSubscription(record.ownerUserId, actorUserId, SubscriptionFeature.LIVESTOCK_TRACKING)
        return if (record.id == 0L) {
            val id = animalDao.insertAnimal(record)
            if (record.purchasePrice > 0.0) {
                opsDao.insertExpense(
                    ExpenseEntity(
                        farmId = record.farmId,
                        ownerUserId = record.ownerUserId,
                        category = if (record.birdCategory) "Bird Purchase" else "Animal Purchase",
                        amount = record.purchasePrice,
                        description = "Purchased ${record.quantity}x ${record.name} (${record.category})",
                        expenseDate = record.purchaseDate
                    )
                )
            }
            id
        } else {
            animalDao.updateAnimal(record)
            record.id
        }
    }

    suspend fun deleteAnimalRecord(record: AnimalRecordEntity, actorUserId: Long? = null) {
        requireActorPermission(actorUserId, record.ownerUserId, AppPermission.LIVESTOCK_DELETE)
        animalDao.deleteAnimal(record)
    }

    suspend fun recordAnimalEvent(
        animal: AnimalRecordEntity,
        eventType: AnimalEventType,
        quantityDelta: Int,
        costOrValue: Double,
        description: String,
        actorUserId: Long? = null
    ) {
        requireActorPermission(actorUserId, animal.ownerUserId, AppPermission.LIVESTOCK_HEALTH_EVENTS)
        requireActiveSubscription(animal.ownerUserId, actorUserId, SubscriptionFeature.LIVESTOCK_TRACKING)
        val newQty = when (eventType) {
            AnimalEventType.BIRTH, AnimalEventType.PURCHASE -> animal.quantity + max(0, quantityDelta)
            AnimalEventType.DEATH, AnimalEventType.SALE, AnimalEventType.TRANSFER -> (animal.quantity - max(0, quantityDelta)).coerceAtLeast(0)
            AnimalEventType.VACCINATION, AnimalEventType.FEEDING, AnimalEventType.MEDICAL -> animal.quantity
        }
        animalDao.updateAnimal(
            animal.copy(
                quantity = newQty,
                healthStatus = if (eventType == AnimalEventType.MEDICAL) "Under Care" else animal.healthStatus
            )
        )
        animalDao.insertEvent(
            AnimalEventEntity(
                animalRecordId = animal.id,
                farmId = animal.farmId,
                ownerUserId = animal.ownerUserId,
                eventType = eventType.name,
                quantityDelta = quantityDelta,
                costOrValue = costOrValue,
                description = description
            )
        )
        if (costOrValue > 0.0) {
            when (eventType) {
                AnimalEventType.VACCINATION -> opsDao.insertExpense(
                    ExpenseEntity(
                        farmId = animal.farmId,
                        ownerUserId = animal.ownerUserId,
                        category = "Vaccination",
                        amount = costOrValue,
                        description = "${animal.name}: $description"
                    )
                )
                AnimalEventType.MEDICAL -> opsDao.insertExpense(
                    ExpenseEntity(
                        farmId = animal.farmId,
                        ownerUserId = animal.ownerUserId,
                        category = "Medicine",
                        amount = costOrValue,
                        description = "${animal.name}: $description"
                    )
                )
                AnimalEventType.FEEDING -> opsDao.insertExpense(
                    ExpenseEntity(
                        farmId = animal.farmId,
                        ownerUserId = animal.ownerUserId,
                        category = "Food & Feed",
                        amount = costOrValue,
                        description = "${animal.name}: $description"
                    )
                )
                AnimalEventType.PURCHASE -> opsDao.insertExpense(
                    ExpenseEntity(
                        farmId = animal.farmId,
                        ownerUserId = animal.ownerUserId,
                        category = if (animal.birdCategory) "Bird Purchase" else "Animal Purchase",
                        amount = costOrValue,
                        description = "${animal.name}: $description"
                    )
                )
                AnimalEventType.SALE -> commerceDao.insertSale(
                    SaleEntity(
                        invoiceNumber = "INV-${System.currentTimeMillis().toString().takeLast(6)}",
                        farmId = animal.farmId,
                        ownerUserId = animal.ownerUserId,
                        customerName = "Walk-in Buyer",
                        customerPhone = "-",
                        productCategory = animal.category,
                        productName = animal.name,
                        animalRecordId = animal.id,
                        quantity = max(1, quantityDelta).toDouble(),
                        unitPrice = costOrValue / max(1, quantityDelta),
                        totalAmount = costOrValue,
                        paidAmount = costOrValue,
                        dueAmount = 0.0,
                        paymentMethod = "Cash"
                    )
                )
                else -> Unit
            }
        }
    }

    // --- Egg Production (Auto Available Calculation) ---
    suspend fun recordEggProduction(
        farmId: Long,
        ownerId: Long,
        eggCategory: String,
        previousStockOverride: Int?,
        producedCount: Int,
        brokenCount: Int,
        soldCount: Int,
        unitSellingPrice: Double,
        productionDate: Long = System.currentTimeMillis(),
        actorUserId: Long? = null
    ) {
        requireActorPermission(actorUserId, ownerId, AppPermission.EGGS_RECORD)
        requireActiveSubscription(ownerId, actorUserId, SubscriptionFeature.EGG_PRODUCTION)
        val prev = previousStockOverride
            ?: (eggDao.getLatestByFarmAndCategory(farmId, eggCategory)?.availableCount ?: 0)
        val available = (prev + producedCount - soldCount - brokenCount).coerceAtLeast(0)
        val value = producedCount * unitSellingPrice
        eggDao.insert(
            EggProductionEntity(
                farmId = farmId,
                ownerUserId = ownerId,
                eggCategory = eggCategory,
                productionDate = productionDate,
                previousStock = prev,
                producedCount = producedCount,
                brokenCount = brokenCount,
                soldCount = soldCount,
                availableCount = available,
                unitSellingPrice = unitSellingPrice,
                totalValue = value
            )
        )
        subAdminDao.insertNotification(
            AppNotificationEntity(
                targetUserId = ownerId,
                title = "Egg Production Logged",
                message = "$producedCount $eggCategory recorded. Available stock: $available.",
                category = "EGGS"
            )
        )
    }

    suspend fun deleteEggRecord(record: EggProductionEntity, actorUserId: Long? = null) {
        requireActorPermission(actorUserId, record.ownerUserId, AppPermission.EGGS_DELETE)
        eggDao.delete(record)
    }

    // --- Inventory Management ---
    suspend fun saveInventoryItem(item: InventoryItemEntity, actorUserId: Long? = null): Long {
        requireActorPermission(actorUserId, item.ownerUserId, AppPermission.INVENTORY_MANAGE)
        requireActiveSubscription(item.ownerUserId, actorUserId, SubscriptionFeature.INVENTORY_EXPENSES)
        val farm = farmDao.getFarmById(item.farmId)
        val enrichedItem = item.copy(
            sellerBkashNumber = item.sellerBkashNumber.trim().ifBlank { farm?.bkashNumber.orEmpty() },
            sellerNagadNumber = item.sellerNagadNumber.trim().ifBlank { farm?.nagadNumber.orEmpty() },
            sellerBankAccount = item.sellerBankAccount.trim().ifBlank { farm?.bankAccountInfo.orEmpty() }
        )
        return if (enrichedItem.id == 0L) {
            val id = inventoryDao.insertItem(enrichedItem)
            if (enrichedItem.quantity > 0.0) {
                inventoryDao.insertMovement(
                    InventoryMovementEntity(
                        inventoryItemId = id,
                        farmId = enrichedItem.farmId,
                        ownerUserId = enrichedItem.ownerUserId,
                        movementType = "STOCK_IN",
                        quantityDelta = enrichedItem.quantity,
                        unitCost = enrichedItem.unitCost,
                        supplierName = enrichedItem.supplierName,
                        note = "Initial stock added"
                    )
                )
            }
            checkLowStockNotification(enrichedItem.copy(id = id))
            id
        } else {
            inventoryDao.updateItem(enrichedItem.copy(updatedAt = System.currentTimeMillis()))
            checkLowStockNotification(enrichedItem)
            enrichedItem.id
        }
    }

    suspend fun adjustInventoryStock(
        item: InventoryItemEntity,
        delta: Double,
        isStockIn: Boolean,
        unitCost: Double,
        supplierName: String,
        note: String,
        actorUserId: Long? = null
    ) {
        requireActorPermission(actorUserId, item.ownerUserId, AppPermission.INVENTORY_MANAGE)
        requireActiveSubscription(item.ownerUserId, actorUserId, SubscriptionFeature.INVENTORY_EXPENSES)
        val newQty = if (isStockIn) item.quantity + delta else (item.quantity - delta).coerceAtLeast(0.0)
        val updated = item.copy(
            quantity = newQty,
            unitCost = if (isStockIn && unitCost > 0) unitCost else item.unitCost,
            supplierName = supplierName.ifBlank { item.supplierName },
            updatedAt = System.currentTimeMillis()
        )
        inventoryDao.updateItem(updated)
        inventoryDao.insertMovement(
            InventoryMovementEntity(
                inventoryItemId = item.id,
                farmId = item.farmId,
                ownerUserId = item.ownerUserId,
                movementType = if (isStockIn) "STOCK_IN" else "STOCK_OUT",
                quantityDelta = if (isStockIn) delta else -delta,
                unitCost = unitCost,
                supplierName = supplierName,
                note = note
            )
        )
        if (isStockIn && unitCost > 0.0) {
            val expCat = when (item.category) {
                "Animal Feed", "Poultry Feed" -> "Food & Feed"
                "Medicines" -> "Medicine"
                "Vaccines" -> "Vaccination"
                "Farm Equipment" -> "Equipment"
                else -> "Other"
            }
            opsDao.insertExpense(
                ExpenseEntity(
                    farmId = item.farmId,
                    ownerUserId = item.ownerUserId,
                    category = expCat,
                    amount = delta * unitCost,
                    description = "Stock In: ${delta} ${item.unit} ${item.name}"
                )
            )
        }
        checkLowStockNotification(updated)
    }

    suspend fun deleteInventoryItem(item: InventoryItemEntity, actorUserId: Long? = null) {
        requireActorPermission(actorUserId, item.ownerUserId, AppPermission.INVENTORY_DELETE)
        inventoryDao.deleteItem(item)
    }

    // --- Dedicated Online Product Storefront (Farm Business Accounts) ---
    suspend fun saveOnlineProduct(item: InventoryItemEntity, actorUserId: Long? = null): Result<Long> = runCatching {
        requireActorPermission(actorUserId, item.ownerUserId, AppPermission.ONLINE_PRODUCTS_MANAGE)
        requireActiveSubscription(item.ownerUserId, actorUserId, SubscriptionFeature.ONLINE_PRODUCT_STORE)

        val cleanName = item.name.trim()
        val cleanCategory = item.category.trim()
        if (cleanName.isBlank()) throw IllegalArgumentException("Product name is required.")
        if (cleanCategory.isBlank()) throw IllegalArgumentException("Product category is required.")
        if (item.sellingPrice <= 0.0) throw IllegalArgumentException("Online selling price must be greater than 0.")
        if (item.quantity < 0.0) throw IllegalArgumentException("Quantity cannot be negative.")

        val farm = farmDao.getFarmById(item.farmId)
            ?: throw IllegalArgumentException("Please select a valid farm before adding online products.")
        if (farm.ownerUserId != item.ownerUserId) {
            throw SecurityException("Selected farm does not belong to this farm account.")
        }

        val sellerBkash = item.sellerBkashNumber.trim().ifBlank { farm.bkashNumber }
        val sellerNagad = item.sellerNagadNumber.trim().ifBlank { farm.nagadNumber }
        val sellerBank = item.sellerBankAccount.trim().ifBlank { farm.bankAccountInfo }

        if ((farm.bkashNumber.isBlank() && sellerBkash.isNotBlank()) ||
            (farm.nagadNumber.isBlank() && sellerNagad.isNotBlank()) ||
            (farm.bankAccountInfo.isBlank() && sellerBank.isNotBlank())
        ) {
            farmDao.update(
                farm.copy(
                    bkashNumber = farm.bkashNumber.ifBlank { sellerBkash },
                    nagadNumber = farm.nagadNumber.ifBlank { sellerNagad },
                    bankAccountInfo = farm.bankAccountInfo.ifBlank { sellerBank }
                )
            )
        }

        val normalized = item.copy(
            name = cleanName,
            category = cleanCategory,
            unit = item.unit.trim().ifBlank { "kg" },
            supplierName = item.supplierName.trim().ifBlank { farm.name },
            supplierPhone = item.supplierPhone.trim().ifBlank { farm.managerPhone },
            description = item.description.trim(),
            sourcingType = if (item.sourcingType == ProductSourcingType.PURCHASED_RESALE.key) {
                ProductSourcingType.PURCHASED_RESALE.key
            } else {
                ProductSourcingType.FARM_PRODUCED.key
            },
            sellerBkashNumber = sellerBkash,
            sellerNagadNumber = sellerNagad,
            sellerBankAccount = sellerBank,
            updatedAt = System.currentTimeMillis()
        )

        if (normalized.id == 0L) {
            val id = inventoryDao.insertItem(normalized)
            if (normalized.quantity > 0.0) {
                inventoryDao.insertMovement(
                    InventoryMovementEntity(
                        inventoryItemId = id,
                        farmId = normalized.farmId,
                        ownerUserId = normalized.ownerUserId,
                        movementType = "STOCK_IN",
                        quantityDelta = normalized.quantity,
                        unitCost = normalized.unitCost,
                        supplierName = normalized.supplierName,
                        note = if (normalized.sourcingType == ProductSourcingType.PURCHASED_RESALE.key) {
                            "Purchased & listed for online resale"
                        } else {
                            "Added directly for online sale"
                        }
                    )
                )
            }
            id
        } else {
            inventoryDao.updateItem(normalized)
            normalized.id
        }
    }

    suspend fun setOnlineProductVisibility(
        item: InventoryItemEntity,
        isLive: Boolean,
        actorUserId: Long? = null
    ): Result<Unit> = runCatching {
        requireActorPermission(actorUserId, item.ownerUserId, AppPermission.ONLINE_PRODUCTS_MANAGE)
        requireActiveSubscription(item.ownerUserId, actorUserId, SubscriptionFeature.ONLINE_PRODUCT_STORE)
        if (isLive && item.sellingPrice <= 0.0) {
            throw IllegalArgumentException("Set a selling price before publishing online.")
        }
        inventoryDao.updateItem(
            item.copy(
                listInMarketplace = isLive,
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun deleteOnlineProduct(item: InventoryItemEntity, actorUserId: Long? = null): Result<Unit> = runCatching {
        requireActorPermission(actorUserId, item.ownerUserId, AppPermission.ONLINE_PRODUCTS_MANAGE)
        inventoryDao.deleteItem(item)
    }

    private suspend fun checkLowStockNotification(item: InventoryItemEntity) {
        if (item.quantity <= item.lowStockThreshold) {
            subAdminDao.insertNotification(
                AppNotificationEntity(
                    targetUserId = item.ownerUserId,
                    title = "Low Inventory: ${item.name}",
                    message = "Remaining ${item.quantity} ${item.unit} (threshold ${item.lowStockThreshold}).",
                    category = "INVENTORY"
                )
            )
        }
    }

    // --- Sales & Customers ---
    suspend fun saveCustomer(customer: CustomerEntity, actorUserId: Long? = null): Long {
        requireActorPermission(actorUserId, customer.ownerUserId, AppPermission.CUSTOMERS_MANAGE)
        requireActiveSubscription(customer.ownerUserId, actorUserId, SubscriptionFeature.BUY_SELL_SALES_CRM)
        return if (customer.id == 0L) {
            val code = customer.customerCode.ifBlank { "CUS-${(100..999).random()}" }
            commerceDao.insertCustomer(customer.copy(customerCode = code))
        } else {
            commerceDao.updateCustomer(customer)
            customer.id
        }
    }

    suspend fun deleteCustomer(customer: CustomerEntity, actorUserId: Long? = null) {
        requireActorPermission(actorUserId, customer.ownerUserId, AppPermission.CUSTOMERS_MANAGE)
        commerceDao.deleteCustomer(customer)
    }

    suspend fun createSale(
        farmId: Long,
        ownerId: Long,
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
        paymentMethod: String,
        actorUserId: Long? = null
    ): SaleEntity {
        requireActorPermission(actorUserId, ownerId, AppPermission.SALES_CREATE_COLLECT)
        requireActiveSubscription(ownerId, actorUserId, SubscriptionFeature.BUY_SELL_SALES_CRM)
        val gross = quantity * unitPrice
        val total = (gross - discount).coerceAtLeast(0.0)
        val paid = paidAmount.coerceIn(0.0, total)
        val due = (total - paid).coerceAtLeast(0.0)
        val invoice = "INV-${System.currentTimeMillis().toString().takeLast(6)}"

        val sale = SaleEntity(
            invoiceNumber = invoice,
            farmId = farmId,
            ownerUserId = ownerId,
            customerId = customerId,
            customerName = customerName.ifBlank { "Walk-in Customer" },
            customerPhone = customerPhone.ifBlank { "-" },
            productCategory = productCategory,
            productName = productName,
            inventoryItemId = inventoryItem?.id,
            animalRecordId = animalRecord?.id,
            quantity = quantity,
            unitPrice = unitPrice,
            discount = discount,
            totalAmount = total,
            paidAmount = paid,
            dueAmount = due,
            paymentMethod = paymentMethod
        )
        val id = commerceDao.insertSale(sale)

        if (inventoryItem != null) {
            val updated = inventoryItem.copy(
                quantity = (inventoryItem.quantity - quantity).coerceAtLeast(0.0),
                updatedAt = System.currentTimeMillis()
            )
            inventoryDao.updateItem(updated)
            inventoryDao.insertMovement(
                InventoryMovementEntity(
                    inventoryItemId = inventoryItem.id,
                    farmId = farmId,
                    ownerUserId = ownerId,
                    movementType = "SALE",
                    quantityDelta = -quantity,
                    note = "Sold via $invoice"
                )
            )
            checkLowStockNotification(updated)
        }
        if (animalRecord != null) {
            val qtyInt = quantity.toInt().coerceAtLeast(1)
            val newQty = (animalRecord.quantity - qtyInt).coerceAtLeast(0)
            animalDao.updateAnimal(animalRecord.copy(quantity = newQty))
            animalDao.insertEvent(
                AnimalEventEntity(
                    animalRecordId = animalRecord.id,
                    farmId = farmId,
                    ownerUserId = ownerId,
                    eventType = AnimalEventType.SALE.name,
                    quantityDelta = qtyInt,
                    costOrValue = total,
                    description = "Sold via $invoice"
                )
            )
        }
        if (due > 0.0) {
            subAdminDao.insertNotification(
                AppNotificationEntity(
                    targetUserId = ownerId,
                    title = "Outstanding Sale Due ($invoice)",
                    message = "$customerName has a due balance on $productName.",
                    category = "DUE"
                )
            )
        }
        return sale.copy(id = id)
    }

    suspend fun settleSaleDue(sale: SaleEntity, additionalPayment: Double, actorUserId: Long? = null) {
        requireActorPermission(actorUserId, sale.ownerUserId, AppPermission.SALES_CREATE_COLLECT)
        requireActiveSubscription(sale.ownerUserId, actorUserId, SubscriptionFeature.BUY_SELL_SALES_CRM)
        if (additionalPayment <= 0.0) return
        val newPaid = (sale.paidAmount + additionalPayment).coerceAtMost(sale.totalAmount)
        val newDue = (sale.totalAmount - newPaid).coerceAtLeast(0.0)
        commerceDao.updateSale(sale.copy(paidAmount = newPaid, dueAmount = newDue))
    }

    suspend fun deleteSale(sale: SaleEntity, actorUserId: Long? = null) {
        requireActorPermission(actorUserId, sale.ownerUserId, AppPermission.SALES_DELETE)
        commerceDao.deleteSale(sale)
    }

    // --- Online Customer Orders ---
    suspend fun placeOnlineOrder(
        customer: UserEntity,
        deliveryAddress: String,
        paymentMethod: String,
        cartItems: List<Pair<InventoryItemEntity, Double>>,
        farmsMap: Map<Long, FarmEntity>,
        deliveryDistrict: String = "",
        deliveryPoliceStation: String = "",
        deliveryLandmark: String = "",
        customerNameOverride: String = customer.name,
        customerPhoneOverride: String = customer.mobile,
        paymentReference: String = "",
        requireStructuredFields: Boolean = false
    ): Result<List<OrderEntity>> {
        if (cartItems.isEmpty()) return Result.failure(IllegalArgumentException("Cart is empty."))
        val cleanName = customerNameOverride.trim().ifBlank { customer.name.trim() }
        val cleanPhone = customerPhoneOverride.trim().ifBlank { customer.mobile.trim() }
        val cleanDistrict = deliveryDistrict.trim()
        val cleanPoliceStation = deliveryPoliceStation.trim()
        val cleanStreet = deliveryAddress.trim()
        val cleanLandmark = deliveryLandmark.trim()
        val cleanPaymentRef = paymentReference.trim()

        if (cleanStreet.isBlank()) {
            return Result.failure(IllegalArgumentException("Delivery address is required."))
        }

        val systemSettings = subAdminDao.observeSystemSettings().firstOrNull() ?: SystemSettingsEntity()
        val anyOrderGatewayEnabled = systemSettings.orderShowBkash ||
            systemSettings.orderShowNagad ||
            systemSettings.orderShowBank ||
            systemSettings.orderShowCod
        if (!anyOrderGatewayEnabled) {
            return Result.failure(IllegalStateException("All online order payment methods are currently turned off by Super Admin."))
        }
        val isMethodAllowed = when {
            paymentMethod.contains("bkash", ignoreCase = true) -> systemSettings.orderShowBkash
            paymentMethod.contains("nagad", ignoreCase = true) -> systemSettings.orderShowNagad
            paymentMethod.contains("bank", ignoreCase = true) -> systemSettings.orderShowBank
            paymentMethod.contains("cash", ignoreCase = true) || paymentMethod.contains("cod", ignoreCase = true) -> systemSettings.orderShowCod
            else -> true
        }
        if (!isMethodAllowed) {
            return Result.failure(IllegalStateException("$paymentMethod is currently turned off by Super Admin."))
        }
        if (requireStructuredFields) {
            if (cleanName.isBlank()) {
                return Result.failure(IllegalArgumentException("Recipient name is required."))
            }
            if (cleanPhone.isBlank()) {
                return Result.failure(IllegalArgumentException("Customer phone number is required."))
            }
            if (cleanDistrict.isBlank()) {
                return Result.failure(IllegalArgumentException("Delivery district is required."))
            }
            if (cleanPoliceStation.isBlank()) {
                return Result.failure(IllegalArgumentException("Police station / Thana is required."))
            }
        }

        val formattedAddress = if (cleanDistrict.isNotBlank() || cleanPoliceStation.isNotBlank()) {
            buildString {
                append(cleanStreet)
                if (cleanPoliceStation.isNotBlank()) append(", PS: $cleanPoliceStation")
                if (cleanDistrict.isNotBlank()) append(", District: $cleanDistrict")
                if (cleanLandmark.isNotBlank()) append(" ($cleanLandmark)")
            }
        } else {
            cleanStreet
        }

        // Verify live stock and prices against the database so concurrent or stale carts cannot over-order
        val verifiedItems = mutableListOf<Pair<InventoryItemEntity, Double>>()
        for ((cartItem, requestedQty) in cartItems) {
            if (requestedQty <= 0.0) {
                return Result.failure(IllegalArgumentException("Quantity must be greater than zero for ${cartItem.name}."))
            }
            val liveItem = inventoryDao.getItemById(cartItem.id)
                ?: return Result.failure(IllegalStateException("${cartItem.name} is no longer available."))
            if (!liveItem.listInMarketplace || liveItem.quantity <= 0.0) {
                return Result.failure(IllegalStateException("${liveItem.name} is currently paused or out of stock."))
            }
            if (liveItem.quantity < requestedQty) {
                return Result.failure(
                    IllegalStateException("Only ${liveItem.quantity.toInt()} ${liveItem.unit} left for ${liveItem.name}.")
                )
            }
            verifiedItems += liveItem to requestedQty
        }

        val groupedByFarm = verifiedItems.groupBy { it.first.farmId }
        val createdOrders = mutableListOf<OrderEntity>()
        for ((farmId, items) in groupedByFarm) {
            val farm = farmsMap[farmId] ?: farmDao.getFarmById(farmId)
            val farmOwnerId = farm?.ownerUserId ?: items.first().first.ownerUserId
            val farmName = farm?.name ?: "Smart Farm"
            val total = items.sumOf { it.first.sellingPrice * it.second }
            val summary = items.joinToString(", ") { "${it.second.toInt()}x ${it.first.name}" }
            val serialized = items.joinToString("|") {
                val safeName = it.first.name.replace("|", "/").replace(":", " ")
                "${it.first.id}:${it.second}:${it.first.sellingPrice}:$safeName"
            }
            val orderNo = "ORD-${(1000..9999).random()}"

            val order = OrderEntity(
                orderNumber = orderNo,
                customerUserId = customer.id,
                customerName = cleanName,
                customerPhone = cleanPhone,
                deliveryAddress = formattedAddress,
                deliveryDistrict = cleanDistrict,
                deliveryPoliceStation = cleanPoliceStation,
                deliveryLandmark = cleanLandmark,
                paymentReference = cleanPaymentRef,
                farmId = farmId,
                farmOwnerUserId = farmOwnerId,
                farmName = farmName,
                itemsSummary = summary,
                serializedItems = serialized,
                totalAmount = total,
                paymentMethod = paymentMethod,
                status = OrderStatus.PENDING.name
            )
            val orderId = commerceDao.insertOrder(order)
            createdOrders += order.copy(id = orderId)

            subAdminDao.insertNotification(
                AppNotificationEntity(
                    targetUserId = farmOwnerId,
                    title = "New Online Order $orderNo • $farmName",
                    message = "$cleanName ($cleanPhone) ordered $summary for $farmName. Delivery: $formattedAddress. Total: ${total.toInt()} BDT.",
                    category = "ORDER"
                )
            )
        }
        return Result.success(createdOrders)
    }

    suspend fun updateOrderStatus(order: OrderEntity, newStatus: OrderStatus, actorUserId: Long? = null) {
        requireActorPermission(actorUserId, order.farmOwnerUserId, AppPermission.ORDERS_MANAGE)
        requireActiveSubscription(order.farmOwnerUserId, actorUserId, SubscriptionFeature.BUY_SELL_SALES_CRM)
        if (order.status == newStatus.name) return
        val wasPending = order.status == OrderStatus.PENDING.name
        commerceDao.updateOrder(order.copy(status = newStatus.name))

        // Auto-deduct inventory and create confirmed sale when transitioning out of PENDING into CONFIRMED
        if (wasPending && newStatus == OrderStatus.CONFIRMED) {
            val parts = order.serializedItems.split("|").filter { it.isNotBlank() }
            for (part in parts) {
                val fields = part.split(":")
                if (fields.size >= 4) {
                    val itemId = fields[0].toLongOrNull()
                    val qty = fields[1].toDoubleOrNull() ?: 1.0
                    val price = fields[2].toDoubleOrNull() ?: 0.0
                    val name = fields[3]
                    val invItem = itemId?.let { inventoryDao.getItemById(it) }
                    if (invItem != null) {
                        val updated = invItem.copy(
                            quantity = (invItem.quantity - qty).coerceAtLeast(0.0),
                            updatedAt = System.currentTimeMillis()
                        )
                        inventoryDao.updateItem(updated)
                        inventoryDao.insertMovement(
                            InventoryMovementEntity(
                                inventoryItemId = invItem.id,
                                farmId = order.farmId,
                                ownerUserId = order.farmOwnerUserId,
                                movementType = "SALE",
                                quantityDelta = -qty,
                                note = "Online order ${order.orderNumber} confirmed"
                            )
                        )
                        checkLowStockNotification(updated)
                    }
                    commerceDao.insertSale(
                        SaleEntity(
                            invoiceNumber = "INV-${order.orderNumber}",
                            farmId = order.farmId,
                            ownerUserId = order.farmOwnerUserId,
                            customerName = order.customerName,
                            customerPhone = order.customerPhone,
                            productCategory = invItem?.category ?: "Farm Product",
                            productName = name,
                            inventoryItemId = itemId,
                            quantity = qty,
                            unitPrice = price,
                            discount = 0.0,
                            totalAmount = qty * price,
                            paidAmount = qty * price,
                            dueAmount = 0.0,
                            paymentMethod = order.paymentMethod
                        )
                    )
                }
            }
        }
        subAdminDao.insertNotification(
            AppNotificationEntity(
                targetUserId = order.customerUserId,
                title = "Order ${order.orderNumber}: ${newStatus.label}",
                message = "Your order from ${order.farmName} is now ${newStatus.label}.",
                category = "ORDER"
            )
        )
    }

    suspend fun requestOrderCancellation(order: OrderEntity) {
        commerceDao.updateOrder(order.copy(cancellationRequested = true))
        subAdminDao.insertNotification(
            AppNotificationEntity(
                targetUserId = order.farmOwnerUserId,
                title = "Cancellation Request: ${order.orderNumber}",
                message = "${order.customerName} requested to cancel ${order.orderNumber}.",
                category = "ORDER"
            )
        )
    }

    // --- Expenses & Employees ---
    suspend fun addExpense(expense: ExpenseEntity, actorUserId: Long? = null) {
        requireActorPermission(actorUserId, expense.ownerUserId, AppPermission.EXPENSES_MANAGE)
        requireActiveSubscription(expense.ownerUserId, actorUserId, SubscriptionFeature.INVENTORY_EXPENSES)
        opsDao.insertExpense(expense)
    }

    suspend fun deleteExpense(expense: ExpenseEntity, actorUserId: Long? = null) {
        requireActorPermission(actorUserId, expense.ownerUserId, AppPermission.EXPENSES_MANAGE)
        opsDao.deleteExpense(expense)
    }

    suspend fun saveEmployee(employee: EmployeeEntity, actorUserId: Long? = null): Long {
        requireAnyActorPermission(actorUserId, employee.ownerUserId, AppPermission.EMPLOYEES_MANAGE, AppPermission.STAFF_HR_MANAGE)
        requireActiveSubscription(employee.ownerUserId, actorUserId, SubscriptionFeature.EMPLOYEES_PERMISSIONS)
        return if (employee.id == 0L) {
            val newId = opsDao.insertEmployee(employee)
            val defaults = PermissionCatalog.defaultGrantedKeysFromLegacyFlags(employee)
            permissionDao.upsertPermissions(
                AppPermission.entries.map { perm ->
                    EmployeePermissionEntity(
                        employeeId = newId,
                        ownerUserId = employee.ownerUserId,
                        permissionKey = perm.key,
                        granted = defaults.contains(perm.key)
                    )
                }
            )
            newId
        } else {
            opsDao.updateEmployee(employee)
            employee.id
        }
    }

    suspend fun deleteEmployee(employee: EmployeeEntity, actorUserId: Long? = null) {
        requireAnyActorPermission(actorUserId, employee.ownerUserId, AppPermission.EMPLOYEES_MANAGE, AppPermission.STAFF_HR_MANAGE)
        opsDao.deleteEmployee(employee)
    }

    suspend fun markEmployeeAttendance(employee: EmployeeEntity, status: AttendanceStatus, actorUserId: Long? = null) {
        requireAnyActorPermission(actorUserId, employee.ownerUserId, AppPermission.EMPLOYEES_MANAGE, AppPermission.STAFF_HR_MANAGE)
        requireActiveSubscription(employee.ownerUserId, actorUserId, SubscriptionFeature.EMPLOYEES_PERMISSIONS)
        opsDao.updateEmployee(
            employee.copy(
                lastAttendanceStatus = status.name,
                lastAttendanceDate = System.currentTimeMillis()
            )
        )
    }

    suspend fun payEmployeeSalary(
        employee: EmployeeEntity,
        amount: Double,
        monthLabel: String,
        note: String,
        actorUserId: Long? = null
    ) {
        recordEmployeePayment(
            employee = employee,
            amount = amount,
            paymentType = EmployeePaymentType.SALARY,
            periodOrTitle = monthLabel,
            note = note,
            actorUserId = actorUserId
        ).getOrThrow()
    }

    suspend fun recordEmployeePayment(
        employee: EmployeeEntity,
        amount: Double,
        paymentType: EmployeePaymentType,
        periodOrTitle: String,
        note: String,
        actorUserId: Long? = null
    ): Result<Long> = runCatching {
        requireAnyActorPermission(actorUserId, employee.ownerUserId, AppPermission.EMPLOYEES_MANAGE, AppPermission.STAFF_HR_MANAGE)
        requireActiveSubscription(employee.ownerUserId, actorUserId, SubscriptionFeature.EMPLOYEES_PERMISSIONS)
        if (amount <= 0.0) throw IllegalArgumentException("Amount must be greater than 0.")
        val cleanTitle = periodOrTitle.trim().ifBlank { paymentType.labelEn }
        val cleanNote = note.trim()

        val expCategory = when (paymentType) {
            EmployeePaymentType.SALARY -> "Employee Salary"
            EmployeePaymentType.BONUS -> "Employee Bonus"
            EmployeePaymentType.EXPENSE -> "Employee Expense"
            EmployeePaymentType.TARGET_REWARD -> "Employee Target Reward"
        }

        val paymentId = db.withTransaction {
            val id = opsDao.insertSalaryPayment(
                SalaryPaymentEntity(
                    employeeId = employee.id,
                    farmId = employee.farmId,
                    ownerUserId = employee.ownerUserId,
                    amount = amount,
                    monthLabel = cleanTitle,
                    note = cleanNote,
                    paymentType = paymentType.key
                )
            )
            opsDao.insertExpense(
                ExpenseEntity(
                    farmId = employee.farmId,
                    ownerUserId = employee.ownerUserId,
                    category = expCategory,
                    amount = amount,
                    description = "${paymentType.labelEn} ($cleanTitle) - ${employee.name}" +
                        if (cleanNote.isNotBlank()) " • $cleanNote" else ""
                )
            )
            employee.linkedUserId?.let { empUid ->
                subAdminDao.insertNotification(
                    AppNotificationEntity(
                        targetUserId = empUid,
                        title = "${paymentType.labelEn}: ${amount.toInt()} BDT",
                        message = "Recorded $cleanTitle for ${employee.name}. $cleanNote".trim(),
                        category = "STAFF_PAY"
                    )
                )
            }
            id
        }
        paymentId
    }

    suspend fun deleteSalaryPayment(payment: SalaryPaymentEntity, actorUserId: Long? = null): Result<Unit> = runCatching {
        requireAnyActorPermission(actorUserId, payment.ownerUserId, AppPermission.EMPLOYEES_MANAGE, AppPermission.STAFF_HR_MANAGE)
        opsDao.deleteSalaryPayment(payment)
    }

    // --- Employee Morning-to-Night Task Targets & Automated Messaging ---
    suspend fun saveEmployeeTaskTarget(
        target: EmployeeTaskTargetEntity,
        actorUserId: Long? = null
    ): Result<Long> = runCatching {
        requireAnyActorPermission(actorUserId, target.ownerUserId, AppPermission.STAFF_HR_MANAGE, AppPermission.EMPLOYEES_MANAGE)
        requireActiveSubscription(target.ownerUserId, actorUserId, SubscriptionFeature.EMPLOYEES_PERMISSIONS)
        val cleanTitle = target.title.trim()
        if (cleanTitle.isBlank()) throw IllegalArgumentException("Target title is required.")
        if (target.rewardBonusAmount < 0.0) throw IllegalArgumentException("Reward bonus cannot be negative.")
        val employee = opsDao.getEmployeeById(target.employeeId)
            ?: throw IllegalArgumentException("Selected employee was not found.")
        val farm = farmDao.getFarmById(target.farmId)
            ?: throw IllegalArgumentException("Selected farm was not found.")

        val normalized = target.copy(
            employeeName = employee.name,
            title = cleanTitle,
            description = target.description.trim(),
            targetGoalText = target.targetGoalText.trim(),
            completionNote = target.completionNote.trim()
        )
        val actor = actorUserId?.let { userDao.getUserById(it) }
        val slot = EmployeeTaskTimeSlot.fromKey(normalized.timeSlot)
        val savedId = db.withTransaction {
            val id = if (normalized.id == 0L) {
                opsDao.insertTaskTarget(normalized)
            } else {
                opsDao.updateTaskTarget(normalized)
                normalized.id
            }
            if (target.id == 0L) {
                opsDao.insertAutoMessage(
                    EmployeeAutoMessageEntity(
                        ownerUserId = normalized.ownerUserId,
                        farmId = normalized.farmId,
                        employeeId = employee.id,
                        senderUserId = actor?.id ?: normalized.ownerUserId,
                        senderName = actor?.name ?: "Farm Admin",
                        senderRole = actor?.role ?: UserRole.FARMER.name,
                        recipientLabel = employee.name,
                        messageType = AutoMessageType.TASK_ASSIGNED.key,
                        subject = "New Target (${slot.labelEn}): $cleanTitle",
                        messageBody = "Farm: ${farm.name} • Goal: ${normalized.targetGoalText.ifBlank { cleanTitle }} • Bonus on completion: ${normalized.rewardBonusAmount.toInt()} BDT.",
                        isAutoGenerated = true
                    )
                )
                subAdminDao.insertNotification(
                    AppNotificationEntity(
                        targetUserId = employee.linkedUserId ?: normalized.ownerUserId,
                        title = "New Target for ${employee.name} (+${normalized.rewardBonusAmount.toInt()} BDT)",
                        message = "${slot.labelEn}: $cleanTitle (${farm.name})",
                        category = "TARGET"
                    )
                )
            }
            id
        }
        savedId
    }

    suspend fun updateEmployeeTaskTargetStatus(
        target: EmployeeTaskTargetEntity,
        newStatus: EmployeeTaskStatus,
        completionNote: String = "",
        actorUserId: Long? = null
    ): Result<Boolean> = runCatching {
        val emp = opsDao.getEmployeeById(target.employeeId)
            ?: throw IllegalArgumentException("Employee not found.")
        val isLinkedEmployeeActor = actorUserId != null && emp.linkedUserId == actorUserId
        if (isLinkedEmployeeActor) {
            requireAnyActorPermission(
                actorUserId,
                target.ownerUserId,
                AppPermission.STAFF_TARGETS_VIEW,
                AppPermission.STAFF_HR_MANAGE,
                AppPermission.EMPLOYEES_MANAGE
            )
        } else {
            requireAnyActorPermission(
                actorUserId,
                target.ownerUserId,
                AppPermission.STAFF_HR_MANAGE,
                AppPermission.EMPLOYEES_MANAGE
            )
        }
        requireActiveSubscription(target.ownerUserId, actorUserId, SubscriptionFeature.EMPLOYEES_PERMISSIONS)

        val actor = actorUserId?.let { userDao.getUserById(it) }
        val cleanNote = completionNote.trim()
        val now = System.currentTimeMillis()
        val slot = EmployeeTaskTimeSlot.fromKey(target.timeSlot)

        db.withTransaction {
            val live = opsDao.getTaskTargetById(target.id) ?: target
            val shouldCreditBonus = newStatus == EmployeeTaskStatus.COMPLETED &&
                !live.rewardCredited &&
                live.rewardBonusAmount > 0.0

            val updated = live.copy(
                status = newStatus.key,
                rewardCredited = live.rewardCredited || shouldCreditBonus,
                completionNote = cleanNote.ifBlank { live.completionNote },
                completedAt = if (newStatus == EmployeeTaskStatus.COMPLETED) (live.completedAt ?: now) else null
            )
            opsDao.updateTaskTarget(updated)

            if (shouldCreditBonus) {
                opsDao.insertSalaryPayment(
                    SalaryPaymentEntity(
                        employeeId = emp.id,
                        farmId = live.farmId,
                        ownerUserId = live.ownerUserId,
                        amount = live.rewardBonusAmount,
                        monthLabel = "${slot.labelEn} Target",
                        paymentDate = now,
                        note = "Completed: ${live.title}" + if (cleanNote.isNotBlank()) " ($cleanNote)" else "",
                        paymentType = EmployeePaymentType.TARGET_REWARD.key
                    )
                )
                opsDao.insertExpense(
                    ExpenseEntity(
                        farmId = live.farmId,
                        ownerUserId = live.ownerUserId,
                        category = "Employee Target Reward",
                        amount = live.rewardBonusAmount,
                        description = "Target Reward (${live.title}) - ${emp.name}",
                        expenseDate = now
                    )
                )
            }

            if (newStatus == EmployeeTaskStatus.COMPLETED && live.status != EmployeeTaskStatus.COMPLETED.key) {
                val rewardText = if (live.rewardBonusAmount > 0.0) {
                    " Bonus credited: ${live.rewardBonusAmount.toInt()} BDT."
                } else {
                    ""
                }
                opsDao.insertAutoMessage(
                    EmployeeAutoMessageEntity(
                        ownerUserId = live.ownerUserId,
                        farmId = live.farmId,
                        employeeId = emp.id,
                        senderUserId = actor?.id ?: (emp.linkedUserId ?: live.ownerUserId),
                        senderName = actor?.name ?: emp.name,
                        senderRole = actor?.role ?: UserRole.EMPLOYEE.name,
                        recipientLabel = "Farm Owner",
                        messageType = AutoMessageType.TASK_COMPLETED.key,
                        subject = "Target Completed (${slot.labelEn}): ${live.title}",
                        messageBody = "${emp.name} completed '${live.title}'.${if (cleanNote.isNotBlank()) " Note: $cleanNote." else ""}$rewardText",
                        isAutoGenerated = true,
                        sentAt = now
                    )
                )
                subAdminDao.insertNotification(
                    AppNotificationEntity(
                        targetUserId = live.ownerUserId,
                        title = "Target Completed: ${emp.name}",
                        message = "${live.title} marked completed.$rewardText",
                        category = "TARGET"
                    )
                )
                if (emp.linkedUserId != null && actorUserId != emp.linkedUserId && shouldCreditBonus) {
                    subAdminDao.insertNotification(
                        AppNotificationEntity(
                            targetUserId = emp.linkedUserId,
                            title = "Bonus Earned: +${live.rewardBonusAmount.toInt()} BDT",
                            message = "Your target '${live.title}' was completed and ${live.rewardBonusAmount.toInt()} BDT was added.",
                            category = "TARGET"
                        )
                    )
                }
            }
            shouldCreditBonus
        }
    }

    suspend fun deleteEmployeeTaskTarget(target: EmployeeTaskTargetEntity, actorUserId: Long? = null): Result<Unit> = runCatching {
        requireAnyActorPermission(actorUserId, target.ownerUserId, AppPermission.STAFF_HR_MANAGE, AppPermission.EMPLOYEES_MANAGE)
        opsDao.deleteTaskTarget(target)
    }

    suspend fun sendEmployeeAutoMessage(
        ownerUserId: Long,
        farmId: Long,
        employeeId: Long,
        messageType: AutoMessageType,
        subject: String,
        body: String,
        actorUserId: Long,
        isAutoGenerated: Boolean = true
    ): Result<Long> = runCatching {
        val actor = userDao.getUserById(actorUserId)
            ?: throw SecurityException("Please sign in to send automated messages.")
        val emp = opsDao.getEmployeeById(employeeId)
            ?: throw IllegalArgumentException("Please select an employee.")
        if (emp.ownerUserId != ownerUserId) {
            throw SecurityException("Employee does not belong to this farm account.")
        }
        val authorized = actor.role == UserRole.SUPER_ADMIN.name ||
            (actor.role == UserRole.FARMER.name && actor.id == ownerUserId) ||
            (actor.role == UserRole.EMPLOYEE.name && actor.employerOwnerId == ownerUserId)
        if (!authorized) {
            throw SecurityException("Access Denied.")
        }
        val cleanSubject = subject.trim().ifBlank { messageType.labelEn }
        val cleanBody = body.trim()
        if (cleanBody.isBlank()) throw IllegalArgumentException("Message text cannot be empty.")

        val ownerUser = userDao.getUserById(ownerUserId)
        val recipientLabel = if (actor.role == UserRole.EMPLOYEE.name) {
            ownerUser?.name ?: "Farm Owner"
        } else {
            emp.name
        }
        val notifTargetId = if (actor.role == UserRole.EMPLOYEE.name) {
            ownerUserId
        } else {
            emp.linkedUserId ?: ownerUserId
        }

        db.withTransaction {
            val id = opsDao.insertAutoMessage(
                EmployeeAutoMessageEntity(
                    ownerUserId = ownerUserId,
                    farmId = farmId.takeIf { it > 0L } ?: emp.farmId,
                    employeeId = emp.id,
                    senderUserId = actor.id,
                    senderName = actor.name,
                    senderRole = actor.role,
                    recipientLabel = recipientLabel,
                    messageType = messageType.key,
                    subject = cleanSubject,
                    messageBody = cleanBody,
                    isAutoGenerated = isAutoGenerated
                )
            )
            subAdminDao.insertNotification(
                AppNotificationEntity(
                    targetUserId = notifTargetId,
                    title = "Auto Message: $cleanSubject",
                    message = "${actor.name}: $cleanBody",
                    category = "STAFF_MESSAGE"
                )
            )
            id
        }
    }

    // --- Admin-Only User Permissions & Employee Login Management ---
    suspend fun provisionEmployeeLogin(
        actorId: Long,
        employee: EmployeeEntity,
        email: String,
        mobile: String,
        password: String
    ): Result<EmployeeEntity> = runCatching {
        requireFarmAdminOrSuperAdmin(actorId, employee.ownerUserId)
        val cleanEmail = email.trim().lowercase()
        val cleanMobile = mobile.trim().ifBlank { employee.phone }
        if (cleanEmail.isBlank() || !cleanEmail.contains("@")) {
            throw IllegalArgumentException("Valid employee email is required.")
        }
        val existingByEmail = userDao.getUserByEmail(cleanEmail)
        val currentLinked = employee.linkedUserId?.let { userDao.getUserById(it) }

        val linkedUserId = if (currentLinked != null) {
            if (existingByEmail != null && existingByEmail.id != currentLinked.id) {
                throw IllegalArgumentException("An account with this email already exists.")
            }
            val updatedPasswordHash = if (password.length >= 4) {
                PasswordHasher.hash(password)
            } else {
                currentLinked.passwordHash
            }
            userDao.update(
                currentLinked.copy(
                    name = employee.name,
                    mobile = cleanMobile,
                    email = cleanEmail,
                    passwordHash = updatedPasswordHash,
                    role = UserRole.EMPLOYEE.name,
                    employerOwnerId = employee.ownerUserId
                )
            )
            currentLinked.id
        } else {
            if (existingByEmail != null) {
                throw IllegalArgumentException("An account with this email already exists.")
            }
            if (password.length < 4) {
                throw IllegalArgumentException("Password must be at least 4 characters.")
            }
            userDao.insert(
                UserEntity(
                    name = employee.name,
                    mobile = cleanMobile,
                    email = cleanEmail,
                    passwordHash = PasswordHasher.hash(password),
                    role = UserRole.EMPLOYEE.name,
                    status = AccountStatus.ACTIVE.name,
                    employerOwnerId = employee.ownerUserId
                )
            )
        }

        val updatedEmployee = employee.copy(
            email = cleanEmail,
            phone = cleanMobile,
            linkedUserId = linkedUserId
        )
        opsDao.updateEmployee(updatedEmployee)
        updatedEmployee
    }

    private suspend fun syncLegacyFlagsFromPermissions(employee: EmployeeEntity) {
        val currentPerms = permissionDao.getPermissionsForEmployee(employee.id).associate { it.permissionKey to it.granted }
        opsDao.updateEmployee(
            employee.copy(
                canManageInventory = currentPerms[AppPermission.INVENTORY_MANAGE.key] ?: employee.canManageInventory,
                canManageSales = currentPerms[AppPermission.SALES_CREATE_COLLECT.key] ?: employee.canManageSales,
                canManageLivestock = currentPerms[AppPermission.LIVESTOCK_CREATE_EDIT.key] ?: employee.canManageLivestock
            )
        )
    }

    suspend fun setEmployeePermission(
        actorId: Long,
        employee: EmployeeEntity,
        permissionKey: String,
        granted: Boolean
    ): Result<Unit> = runCatching {
        requireFarmAdminOrSuperAdmin(actorId, employee.ownerUserId)
        val existing = permissionDao.getPermission(employee.id, permissionKey)
        permissionDao.upsertPermission(
            EmployeePermissionEntity(
                id = existing?.id ?: 0L,
                employeeId = employee.id,
                ownerUserId = employee.ownerUserId,
                permissionKey = permissionKey,
                granted = granted,
                updatedAt = System.currentTimeMillis()
            )
        )
        syncLegacyFlagsFromPermissions(employee)
    }

    suspend fun setAllPermissionsForEmployee(
        actorId: Long,
        employee: EmployeeEntity,
        allKeys: Collection<String>,
        granted: Boolean
    ): Result<Unit> = runCatching {
        requireFarmAdminOrSuperAdmin(actorId, employee.ownerUserId)
        val existingMap = permissionDao.getPermissionsForEmployee(employee.id).associateBy { it.permissionKey }
        val now = System.currentTimeMillis()
        val rows = allKeys.distinct().map { key ->
            EmployeePermissionEntity(
                id = existingMap[key]?.id ?: 0L,
                employeeId = employee.id,
                ownerUserId = employee.ownerUserId,
                permissionKey = key,
                granted = granted,
                updatedAt = now
            )
        }
        permissionDao.upsertPermissions(rows)
        syncLegacyFlagsFromPermissions(employee)
    }

    suspend fun applyPermissionPreset(
        actorId: Long,
        employee: EmployeeEntity,
        preset: PermissionPreset,
        allKnownKeys: Collection<String>
    ): Result<Unit> = runCatching {
        requireFarmAdminOrSuperAdmin(actorId, employee.ownerUserId)
        val existingMap = permissionDao.getPermissionsForEmployee(employee.id).associateBy { it.permissionKey }
        val now = System.currentTimeMillis()
        val combinedKeys = (allKnownKeys + AppPermission.entries.map { it.key }).distinct()
        val rows = combinedKeys.map { key ->
            EmployeePermissionEntity(
                id = existingMap[key]?.id ?: 0L,
                employeeId = employee.id,
                ownerUserId = employee.ownerUserId,
                permissionKey = key,
                granted = preset.keys.contains(key),
                updatedAt = now
            )
        }
        permissionDao.upsertPermissions(rows)
        syncLegacyFlagsFromPermissions(employee)
    }

    suspend fun setPermissionForMultipleEmployees(
        actorId: Long,
        ownerUserId: Long,
        employees: List<EmployeeEntity>,
        permissionKey: String,
        granted: Boolean
    ): Result<Unit> = runCatching {
        requireFarmAdminOrSuperAdmin(actorId, ownerUserId)
        val now = System.currentTimeMillis()
        for (emp in employees) {
            if (emp.ownerUserId != ownerUserId) continue
            val existing = permissionDao.getPermission(emp.id, permissionKey)
            permissionDao.upsertPermission(
                EmployeePermissionEntity(
                    id = existing?.id ?: 0L,
                    employeeId = emp.id,
                    ownerUserId = ownerUserId,
                    permissionKey = permissionKey,
                    granted = granted,
                    updatedAt = now
                )
            )
            syncLegacyFlagsFromPermissions(emp)
        }
    }

    suspend fun addCustomPermissionOption(
        actorId: Long,
        ownerUserId: Long,
        titleEn: String,
        titleBn: String,
        descEn: String,
        descBn: String
    ): Result<CustomPermissionOptionEntity> = runCatching {
        requireFarmAdminOrSuperAdmin(actorId, ownerUserId)
        val cleanTitleEn = titleEn.trim()
        if (cleanTitleEn.isBlank()) {
            throw IllegalArgumentException("Option title is required.")
        }
        val key = "CUSTOM_${System.currentTimeMillis()}_${(100..999).random()}"
        val entity = CustomPermissionOptionEntity(
            ownerUserId = ownerUserId,
            permissionKey = key,
            titleEn = cleanTitleEn,
            titleBn = titleBn.trim().ifBlank { cleanTitleEn },
            descriptionEn = descEn.trim(),
            descriptionBn = descBn.trim()
        )
        val id = permissionDao.insertCustomOption(entity)
        entity.copy(id = id)
    }

    suspend fun deleteCustomPermissionOption(
        actorId: Long,
        option: CustomPermissionOptionEntity
    ): Result<Unit> = runCatching {
        requireFarmAdminOrSuperAdmin(actorId, option.ownerUserId)
        permissionDao.deletePermissionsByKey(option.permissionKey)
        permissionDao.deleteCustomOption(option)
    }

    // --- Subscriptions & Notifications ---
    suspend fun checkExpiredSubscriptions(): Result<Unit> = runCatching {
        val now = System.currentTimeMillis()
        val expired = subAdminDao.findExpiredActiveSubscriptions(now)
        for (sub in expired) {
            subAdminDao.updateUserSubscription(sub.copy(status = "EXPIRED"))
            subAdminDao.insertNotification(
                AppNotificationEntity(
                    targetUserId = sub.userId,
                    title = "Subscription Expired",
                    message = "Your ${sub.planName} has expired. Renew now to keep full access.",
                    category = "SUBSCRIPTION"
                )
            )
        }
    }

    suspend fun purchaseSubscription(
        user: UserEntity,
        plan: SubscriptionPlanEntity,
        currencyCode: String,
        paymentMethod: String,
        transactionRef: String,
        actorUserId: Long? = null
    ): Result<UserSubscriptionEntity> = runCatching {
        requireActorPermission(actorUserId, user.id, AppPermission.SUBSCRIPTION_MANAGE)
        val now = System.currentTimeMillis()
        val settings = subAdminDao.observeSystemSettings().firstOrNull() ?: SystemSettingsEntity()
        val isManual = settings.requireAdminApprovalForAll ||
            paymentMethod.contains("Bank", ignoreCase = true) ||
            paymentMethod.contains("Manual", ignoreCase = true)
        val status = if (isManual) "PENDING" else "ACTIVE"
        val payStatus = if (isManual) PaymentStatus.PENDING.name else PaymentStatus.SUCCESSFUL.name
        val safeDays = plan.durationDays.coerceAtLeast(1)
        val expiry = now + safeDays * 86_400_000L

        if (status == "ACTIVE") {
            subAdminDao.deactivateActiveSubscriptionsForUser(user.id, "REPLACED")
        }

        val entity = UserSubscriptionEntity(
            userId = user.id,
            userName = user.name,
            planId = plan.id,
            planName = plan.name,
            pricePaidBdt = plan.priceBaseBdt,
            currencyCode = currencyCode.ifBlank { "BDT" },
            paymentMethod = paymentMethod.ifBlank { "bKash" },
            transactionRef = transactionRef.trim().ifBlank { "TXN-${(100000..999999).random()}" },
            paymentStatus = payStatus,
            startDate = now,
            expiryDate = expiry,
            status = status
        )
        val id = subAdminDao.insertUserSubscription(entity)
        subAdminDao.insertNotification(
            AppNotificationEntity(
                targetUserId = user.id,
                title = if (isManual) "Payment Verification Pending" else "Subscription Activated!",
                message = if (isManual) "Your ${plan.name} payment via $paymentMethod is awaiting admin approval."
                else "Your ${plan.name} is active for $safeDays days.",
                category = if (isManual) "PAYMENT" else "SUBSCRIPTION"
            )
        )
        entity.copy(id = id)
    }

    suspend fun markNotificationsRead(userId: Long, ownerId: Long = userId): Result<Unit> = runCatching {
        if (ownerId == userId) {
            subAdminDao.markAllRead(userId)
        } else {
            subAdminDao.markAllReadForUserOrOwner(userId, ownerId)
        }
    }

    suspend fun submitSupportRequest(user: UserEntity, subject: String, message: String): Result<Unit> = runCatching {
        if (subject.isBlank() || message.isBlank()) {
            throw IllegalArgumentException("Subject and message are required.")
        }
        subAdminDao.insertSupport(
            SupportRequestEntity(
                userId = user.id,
                userName = user.name,
                subject = subject.trim(),
                message = message.trim()
            )
        )
    }

    // --- Super Admin Protected Actions (RBAC enforced) ---
    private suspend fun requireSuperAdmin(actorUserId: Long): UserEntity {
        val actor = userDao.getUserById(actorUserId)
            ?: throw SecurityException("Unauthorized: user not found.")
        if (actor.role != UserRole.SUPER_ADMIN.name) {
            throw SecurityException("Access Denied: Super Admin role required.")
        }
        return actor
    }

    suspend fun adminSetUserStatus(actorId: Long, targetUser: UserEntity, newStatus: AccountStatus): Result<Unit> = runCatching {
        val admin = requireSuperAdmin(actorId)
        if (targetUser.id == admin.id) throw IllegalArgumentException("Cannot suspend your own Super Admin account.")
        userDao.update(targetUser.copy(status = newStatus.name))
        subAdminDao.insertAuditLog(
            AdminAuditLogEntity(
                actorUserId = admin.id,
                actorName = admin.name,
                action = "USER_STATUS_${newStatus.name}",
                target = "${targetUser.name} (${targetUser.email})",
                details = "Account status changed to ${newStatus.name}."
            )
        )
    }

    suspend fun adminSaveSubscriptionPlan(actorId: Long, plan: SubscriptionPlanEntity): Result<Unit> = runCatching {
        val admin = requireSuperAdmin(actorId)
        if (plan.id == 0L) subAdminDao.insertPlan(plan) else subAdminDao.updatePlan(plan)
        subAdminDao.insertAuditLog(
            AdminAuditLogEntity(
                actorUserId = admin.id,
                actorName = admin.name,
                action = if (plan.id == 0L) "PLAN_CREATED" else "PLAN_UPDATED",
                target = plan.name,
                details = "Price: ${plan.priceBaseBdt} BDT, Duration: ${plan.durationDays} days"
            )
        )
    }

    suspend fun adminDeleteSubscriptionPlan(actorId: Long, plan: SubscriptionPlanEntity): Result<Unit> = runCatching {
        val admin = requireSuperAdmin(actorId)
        subAdminDao.deletePlan(plan)
        subAdminDao.insertAuditLog(
            AdminAuditLogEntity(
                actorUserId = admin.id,
                actorName = admin.name,
                action = "PLAN_DELETED",
                target = plan.name,
                details = "Deleted subscription plan."
            )
        )
    }

    suspend fun adminUpdateSubscriptionPayment(
        actorId: Long,
        sub: UserSubscriptionEntity,
        paymentStatus: PaymentStatus,
        subStatus: String
    ): Result<Unit> = runCatching {
        val admin = requireSuperAdmin(actorId)
        if (paymentStatus == PaymentStatus.SUCCESSFUL && subStatus == "ACTIVE") {
            subAdminDao.deactivateActiveSubscriptionsForUser(sub.userId, "REPLACED")
        }
        subAdminDao.updateUserSubscription(sub.copy(paymentStatus = paymentStatus.name, status = subStatus))
        subAdminDao.insertNotification(
            AppNotificationEntity(
                targetUserId = sub.userId,
                title = "Subscription Update: $subStatus",
                message = "Payment status for ${sub.planName} updated to ${paymentStatus.label}.",
                category = "PAYMENT"
            )
        )
        subAdminDao.insertAuditLog(
            AdminAuditLogEntity(
                actorUserId = admin.id,
                actorName = admin.name,
                action = "PAYMENT_${paymentStatus.name}",
                target = "${sub.userName} - ${sub.planName}",
                details = "Ref ${sub.transactionRef} marked ${paymentStatus.label} ($subStatus)."
            )
        )
    }

    suspend fun adminGrantSubscription(actorId: Long, targetUser: UserEntity, plan: SubscriptionPlanEntity): Result<Unit> = runCatching {
        val admin = requireSuperAdmin(actorId)
        val now = System.currentTimeMillis()
        subAdminDao.insertUserSubscription(
            UserSubscriptionEntity(
                userId = targetUser.id,
                userName = targetUser.name,
                planId = plan.id,
                planName = plan.name,
                pricePaidBdt = plan.priceBaseBdt,
                currencyCode = "BDT",
                paymentMethod = "Admin Grant",
                transactionRef = "ADM-${(10000..99999).random()}",
                paymentStatus = PaymentStatus.SUCCESSFUL.name,
                startDate = now,
                expiryDate = now + plan.durationDays * 86_400_000L,
                status = "ACTIVE"
            )
        )
        subAdminDao.insertAuditLog(
            AdminAuditLogEntity(
                actorUserId = admin.id,
                actorName = admin.name,
                action = "SUBSCRIPTION_GRANTED",
                target = targetUser.name,
                details = "Granted ${plan.name} (${plan.durationDays} days)."
            )
        )
    }

    suspend fun adminBroadcastNotification(actorId: Long, title: String, message: String): Result<Unit> = runCatching {
        val admin = requireSuperAdmin(actorId)
        subAdminDao.insertNotification(
            AppNotificationEntity(
                targetUserId = 0L,
                title = title.trim(),
                message = message.trim(),
                category = "SYSTEM"
            )
        )
        subAdminDao.insertAuditLog(
            AdminAuditLogEntity(
                actorUserId = admin.id,
                actorName = admin.name,
                action = "BROADCAST_NOTIFICATION",
                target = "All Users",
                details = title
            )
        )
    }

    suspend fun adminUpdateSettings(actorId: Long, settings: SystemSettingsEntity): Result<Unit> = runCatching {
        val admin = requireSuperAdmin(actorId)
        subAdminDao.saveSystemSettings(settings)
        subAdminDao.insertAuditLog(
            AdminAuditLogEntity(
                actorUserId = admin.id,
                actorName = admin.name,
                action = "SETTINGS_UPDATED",
                target = "System Settings",
                details = "Maintenance=${settings.maintenanceMode}, Cust=${settings.loginShowCustomerLogin}, Farmer=${settings.loginShowFarmerLogin}, Emp=${settings.loginShowEmployeeLogin}, Demos=(C:${settings.loginShowCustomerDemo},F:${settings.loginShowFarmerDemo},E:${settings.loginShowEmployeeDemo}), OrderPay=(bKash:${settings.orderShowBkash},Nagad:${settings.orderShowNagad},Bank:${settings.orderShowBank},COD:${settings.orderShowCod},Override:${settings.orderOverrideSellerPayments})"
            )
        )
    }

    suspend fun adminToggleFeatureSubscription(
        actorId: Long,
        featureKey: String,
        requiresSubscription: Boolean
    ): Result<SystemSettingsEntity> = runCatching {
        val admin = requireSuperAdmin(actorId)
        val current = subAdminDao.observeSystemSettings().firstOrNull() ?: SystemSettingsEntity()
        val updatedKeys = current.parseLockedFeatureKeys().toMutableSet()
        if (requiresSubscription) updatedKeys.add(featureKey) else updatedKeys.remove(featureKey)
        val updated = current.copy(subscriptionLockedFeaturesCsv = updatedKeys.joinToString(","))
        subAdminDao.saveSystemSettings(updated)
        val featureTitle = SubscriptionFeature.fromKey(featureKey)?.titleEn ?: featureKey
        subAdminDao.insertAuditLog(
            AdminAuditLogEntity(
                actorUserId = admin.id,
                actorName = admin.name,
                action = if (requiresSubscription) "FEATURE_SET_SUBSCRIPTION" else "FEATURE_SET_FREE",
                target = featureTitle,
                details = if (requiresSubscription) "Marked as Subscription Required" else "Marked as Free for all users"
            )
        )
        updated
    }

    suspend fun adminSetLockedSubscriptionFeatures(
        actorId: Long,
        lockedKeys: Set<String>
    ): Result<SystemSettingsEntity> = runCatching {
        val admin = requireSuperAdmin(actorId)
        val current = subAdminDao.observeSystemSettings().firstOrNull() ?: SystemSettingsEntity()
        val validKeys = lockedKeys.filter { SubscriptionFeature.fromKey(it) != null }.toSet()
        val updated = current.copy(subscriptionLockedFeaturesCsv = validKeys.joinToString(","))
        subAdminDao.saveSystemSettings(updated)
        subAdminDao.insertAuditLog(
            AdminAuditLogEntity(
                actorUserId = admin.id,
                actorName = admin.name,
                action = "FEATURE_TIERS_BULK_UPDATED",
                target = "Subscription Features",
                details = "${validKeys.size} modules require subscription; ${SubscriptionFeature.entries.size - validKeys.size} free"
            )
        )
        updated
    }

    suspend fun adminResolveSupport(actorId: Long, req: SupportRequestEntity, reply: String): Result<Unit> = runCatching {
        val admin = requireSuperAdmin(actorId)
        subAdminDao.updateSupport(req.copy(status = "RESOLVED", adminReply = reply.trim()))
        subAdminDao.insertNotification(
            AppNotificationEntity(
                targetUserId = req.userId,
                title = "Support Resolved: ${req.subject}",
                message = reply.trim(),
                category = "SYSTEM"
            )
        )
        subAdminDao.insertAuditLog(
            AdminAuditLogEntity(
                actorUserId = admin.id,
                actorName = admin.name,
                action = "SUPPORT_RESOLVED",
                target = req.subject,
                details = "Replied to ${req.userName}"
            )
        )
    }

    suspend fun adminChangeCredentials(
        actorId: Long,
        currentPassword: String,
        newEmail: String,
        newPassword: String,
        newSecurityPin: String
    ): Result<UserEntity> = runCatching {
        val admin = requireSuperAdmin(actorId)
        if (!PasswordHasher.verify(currentPassword, admin.passwordHash)) {
            throw SecurityException("Current Super Admin password is incorrect.")
        }
        val cleanEmail = newEmail.trim().lowercase().ifBlank { admin.email }
        if (!cleanEmail.contains("@")) {
            throw IllegalArgumentException("Enter a valid admin email address.")
        }
        if (cleanEmail != admin.email && userDao.getUserByEmail(cleanEmail) != null) {
            throw IllegalArgumentException("An account with this email already exists.")
        }
        val cleanNewPass = newPassword
        if (cleanNewPass.isNotBlank() && cleanNewPass.length < 6) {
            throw IllegalArgumentException("New Super Admin password must be at least 6 characters.")
        }
        val cleanNewPin = newSecurityPin.trim()
        if (cleanNewPin.isNotBlank() && cleanNewPin.length < 4) {
            throw IllegalArgumentException("Security PIN must be at least 4 characters.")
        }
        val emailChanged = cleanEmail != admin.email
        val passChanged = cleanNewPass.isNotBlank()
        val pinChanged = cleanNewPin.isNotBlank()
        if (!emailChanged && !passChanged && !pinChanged) {
            throw IllegalArgumentException("Enter a new email, password, or security PIN to update.")
        }

        val updatedAdmin = admin.copy(
            email = cleanEmail,
            passwordHash = if (passChanged) PasswordHasher.hash(cleanNewPass) else admin.passwordHash
        )
        db.withTransaction {
            userDao.update(updatedAdmin)
            if (pinChanged) {
                val currentSettings = subAdminDao.observeSystemSettings().firstOrNull() ?: SystemSettingsEntity()
                subAdminDao.saveSystemSettings(
                    currentSettings.copy(adminPinHash = PasswordHasher.hash(cleanNewPin))
                )
            }
            subAdminDao.insertAuditLog(
                AdminAuditLogEntity(
                    actorUserId = updatedAdmin.id,
                    actorName = updatedAdmin.name,
                    action = "ADMIN_CREDENTIALS_UPDATED",
                    target = updatedAdmin.email,
                    details = buildList {
                        if (emailChanged) add("Email updated")
                        if (passChanged) add("Password rotated")
                        if (pinChanged) add("Security PIN rotated")
                    }.joinToString(", ")
                )
            )
        }
        updatedAdmin
    }

    suspend fun adminDeleteOnlineShopProduct(
        actorId: Long,
        item: InventoryItemEntity
    ): Result<Unit> = runCatching {
        val admin = requireSuperAdmin(actorId)
        db.withTransaction {
            productAdDao.deleteAdsByInventoryItem(item.id)
            inventoryDao.deleteItem(item)
            subAdminDao.insertAuditLog(
                AdminAuditLogEntity(
                    actorUserId = admin.id,
                    actorName = admin.name,
                    action = "SHOP_PRODUCT_DELETED",
                    target = item.name,
                    details = "Removed product #${item.id} (owner ID #${item.ownerUserId}, category ${item.category}) and linked ads."
                )
            )
        }
    }

    suspend fun adminDeleteProductAd(
        actorId: Long,
        ad: ProductAdEntity
    ): Result<Unit> = runCatching {
        val admin = requireSuperAdmin(actorId)
        db.withTransaction {
            productAdDao.deleteAd(ad)
            subAdminDao.insertAuditLog(
                AdminAuditLogEntity(
                    actorUserId = admin.id,
                    actorName = admin.name,
                    action = "PRODUCT_AD_DELETED",
                    target = ad.productName,
                    details = "Removed shop video ad #${ad.id} (owner ID #${ad.ownerUserId})."
                )
            )
        }
    }

    suspend fun adminDeleteUserCompletely(
        actorId: Long,
        targetUser: UserEntity
    ): Result<Unit> = runCatching {
        val admin = requireSuperAdmin(actorId)
        if (targetUser.id == admin.id || targetUser.role == UserRole.SUPER_ADMIN.name) {
            throw SecurityException("Cannot delete a Super Admin account.")
        }
        db.withTransaction {
            val childEmployeeUsers = userDao.getEmployeesByOwnerId(targetUser.id)

            productAdDao.deleteAdsByOwner(targetUser.id)
            inventoryDao.deleteMovementsByOwner(targetUser.id)
            inventoryDao.deleteByOwner(targetUser.id)
            agriDao.deleteExpensesByOwner(targetUser.id)
            agriDao.deleteCropsByOwner(targetUser.id)
            agriDao.deleteLandsByOwner(targetUser.id)
            commerceDao.deleteOrdersByUser(targetUser.id)
            commerceDao.deleteCustomersByOwner(targetUser.id)
            animalDao.deleteCustomCategoriesByOwner(targetUser.id)
            permissionDao.deletePermissionsByOwner(targetUser.id)
            permissionDao.deleteCustomOptionsByOwner(targetUser.id)
            opsDao.deleteTaskTargetsByOwner(targetUser.id)
            opsDao.deleteAutoMessagesByOwner(targetUser.id)
            opsDao.deleteSalaryPaymentsByOwner(targetUser.id)
            subAdminDao.deleteSubscriptionsByUser(targetUser.id)
            subAdminDao.deleteNotificationsForUser(targetUser.id)
            subAdminDao.deleteSupportByUser(targetUser.id)
            opsDao.unlinkEmployeeUser(targetUser.id)

            childEmployeeUsers.forEach { child ->
                subAdminDao.deleteSubscriptionsByUser(child.id)
                subAdminDao.deleteNotificationsForUser(child.id)
                subAdminDao.deleteSupportByUser(child.id)
                commerceDao.deleteOrdersByUser(child.id)
                opsDao.unlinkEmployeeUser(child.id)
            }
            userDao.deleteEmployeesByOwnerId(targetUser.id)

            farmDao.deleteFarmsByOwner(targetUser.id)
            userDao.deleteById(targetUser.id)

            subAdminDao.insertAuditLog(
                AdminAuditLogEntity(
                    actorUserId = admin.id,
                    actorName = admin.name,
                    action = "USER_DELETED_COMPLETELY",
                    target = "${targetUser.name} (#${targetUser.id}, ${targetUser.email})",
                    details = "Permanently removed user ID #${targetUser.id}, role ${targetUser.role}, farms, online shop products, ads, and child employee accounts."
                )
            )
        }
    }

    // --- Agriculture, Crops, Lands & Sector Expenses ---

    suspend fun saveAgriLand(land: AgriLandEntity, actorUserId: Long? = null): Result<Long> = runCatching {
        requireActorPermission(actorUserId, land.ownerUserId, AppPermission.AGRICULTURE_MANAGE)
        requireActiveSubscription(land.ownerUserId, actorUserId, SubscriptionFeature.AGRICULTURE_CROP_LEDGER)
        val cleanName = land.name.trim()
        if (cleanName.isBlank()) throw IllegalArgumentException("Land plot name is required.")
        if (land.areaSize < 0.0) throw IllegalArgumentException("Land area cannot be negative.")
        val cleaned = land.copy(
            name = cleanName,
            location = land.location.trim(),
            soilOrLandType = land.soilOrLandType.trim(),
            notes = land.notes.trim()
        )
        if (cleaned.id == 0L) agriDao.insertLand(cleaned) else {
            agriDao.updateLand(cleaned)
            cleaned.id
        }
    }

    suspend fun deleteAgriLand(land: AgriLandEntity, actorUserId: Long? = null): Result<Unit> = runCatching {
        requireActorPermission(actorUserId, land.ownerUserId, AppPermission.AGRICULTURE_DELETE)
        agriDao.deleteLand(land)
    }

    suspend fun saveAgriCrop(crop: AgriCropEntity, actorUserId: Long? = null): Result<Long> = runCatching {
        requireActorPermission(actorUserId, crop.ownerUserId, AppPermission.AGRICULTURE_MANAGE)
        requireActiveSubscription(crop.ownerUserId, actorUserId, SubscriptionFeature.AGRICULTURE_CROP_LEDGER)
        val cleanTitle = crop.title.trim()
        if (cleanTitle.isBlank()) throw IllegalArgumentException("Crop title is required.")
        val parentLand = agriDao.getLandById(crop.landId)
            ?: throw IllegalArgumentException("Please select a valid land plot first.")
        if (parentLand.ownerUserId != crop.ownerUserId) {
            throw SecurityException("Selected land plot does not belong to this account.")
        }
        val computedWorth = if (crop.yieldMarketWorth > 0.0) {
            crop.yieldMarketWorth
        } else {
            crop.yieldQuantity.coerceAtLeast(0.0) * crop.marketPricePerUnit.coerceAtLeast(0.0)
        }
        val cleaned = crop.copy(
            farmId = crop.farmId ?: parentLand.farmId,
            title = cleanTitle,
            variety = crop.variety.trim(),
            yieldQuantity = crop.yieldQuantity.coerceAtLeast(0.0),
            marketPricePerUnit = crop.marketPricePerUnit.coerceAtLeast(0.0),
            yieldMarketWorth = computedWorth.coerceAtLeast(0.0),
            soldAmount = crop.soldAmount.coerceAtLeast(0.0),
            notes = crop.notes.trim(),
            updatedAt = System.currentTimeMillis()
        )
        if (cleaned.id == 0L) agriDao.insertCrop(cleaned) else {
            agriDao.updateCrop(cleaned)
            cleaned.id
        }
    }

    suspend fun recordAgriHarvest(
        crop: AgriCropEntity,
        yieldQuantity: Double,
        yieldUnit: String,
        marketPricePerUnit: Double,
        yieldMarketWorth: Double,
        soldAmount: Double,
        status: String,
        actorUserId: Long? = null
    ): Result<Unit> = runCatching {
        requireActorPermission(actorUserId, crop.ownerUserId, AppPermission.AGRICULTURE_MANAGE)
        requireActiveSubscription(crop.ownerUserId, actorUserId, SubscriptionFeature.AGRICULTURE_CROP_LEDGER)
        val safeQty = yieldQuantity.coerceAtLeast(0.0)
        val safePrice = marketPricePerUnit.coerceAtLeast(0.0)
        val safeWorth = if (yieldMarketWorth > 0.0) yieldMarketWorth else safeQty * safePrice
        agriDao.updateCrop(
            crop.copy(
                yieldQuantity = safeQty,
                yieldUnit = yieldUnit.ifBlank { crop.yieldUnit },
                marketPricePerUnit = safePrice,
                yieldMarketWorth = safeWorth.coerceAtLeast(0.0),
                soldAmount = soldAmount.coerceAtLeast(0.0),
                status = status,
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun deleteAgriCrop(crop: AgriCropEntity, actorUserId: Long? = null): Result<Unit> = runCatching {
        requireActorPermission(actorUserId, crop.ownerUserId, AppPermission.AGRICULTURE_DELETE)
        agriDao.deleteExpensesByCropId(crop.id)
        agriDao.deleteCrop(crop)
    }

    suspend fun addAgriExpense(expense: AgriExpenseEntity, actorUserId: Long? = null): Result<Long> = runCatching {
        requireActorPermission(actorUserId, expense.ownerUserId, AppPermission.AGRICULTURE_MANAGE)
        requireActiveSubscription(expense.ownerUserId, actorUserId, SubscriptionFeature.AGRICULTURE_CROP_LEDGER)
        if (expense.amount <= 0.0) throw IllegalArgumentException("Expense amount must be greater than zero.")
        val parentLand = agriDao.getLandById(expense.landId)
            ?: throw IllegalArgumentException("Please select a valid land plot.")
        if (parentLand.ownerUserId != expense.ownerUserId) {
            throw SecurityException("Selected land plot does not belong to this account.")
        }
        val cleaned = expense.copy(
            farmId = expense.farmId ?: parentLand.farmId,
            customSectorName = expense.customSectorName.trim(),
            description = expense.description.trim()
        )
        agriDao.insertExpense(cleaned)
    }

    suspend fun deleteAgriExpense(expense: AgriExpenseEntity, actorUserId: Long? = null): Result<Unit> = runCatching {
        requireActorPermission(actorUserId, expense.ownerUserId, AppPermission.AGRICULTURE_DELETE)
        agriDao.deleteExpense(expense)
    }

    // --- AI Product Video Ads ---
    suspend fun saveProductAd(ad: ProductAdEntity, actorUserId: Long? = null): Result<Long> = runCatching {
        requireActorPermission(actorUserId, ad.ownerUserId, AppPermission.AI_AD_STUDIO)
        requireActiveSubscription(ad.ownerUserId, actorUserId, SubscriptionFeature.AI_AD_VIDEO_STUDIO)
        val cleanProductName = ad.productName.trim()
        if (cleanProductName.isBlank()) throw IllegalArgumentException("Product name is required.")
        val cleanHook = ad.hookHeadlineBn.trim()
        if (cleanHook.isBlank()) throw IllegalArgumentException("Ad headline is required.")
        val cleaned = ad.copy(
            productName = cleanProductName,
            farmName = ad.farmName.trim(),
            priceText = ad.priceText.trim(),
            hookHeadlineBn = cleanHook,
            promoBadgeBn = ad.promoBadgeBn.trim().ifBlank { "১০০% খাঁটি" },
            benefitsPipeSeparatedBn = ad.benefitsPipeSeparatedBn.trim(),
            callToActionBn = ad.callToActionBn.trim().ifBlank { "এখনই অর্ডার করুন • এখানে ক্লিক করুন" },
            voiceoverScriptBn = ad.voiceoverScriptBn.trim(),
            orderStepsPipeSeparatedBn = ad.orderStepsPipeSeparatedBn.trim(),
            accentColorHex = ad.accentColorHex.trim().ifBlank { "#1B5E20" }
        )
        if (cleaned.id == 0L) {
            productAdDao.insertAd(cleaned)
        } else {
            productAdDao.updateAd(cleaned)
            cleaned.id
        }
    }

    suspend fun toggleProductAdActive(ad: ProductAdEntity, active: Boolean, actorUserId: Long? = null): Result<Unit> = runCatching {
        requireActorPermission(actorUserId, ad.ownerUserId, AppPermission.AI_AD_STUDIO)
        requireActiveSubscription(ad.ownerUserId, actorUserId, SubscriptionFeature.AI_AD_VIDEO_STUDIO)
        productAdDao.updateAd(ad.copy(isActiveInShop = active))
    }

    suspend fun deleteProductAd(ad: ProductAdEntity, actorUserId: Long? = null): Result<Unit> = runCatching {
        requireActorPermission(actorUserId, ad.ownerUserId, AppPermission.AI_AD_STUDIO)
        productAdDao.deleteAd(ad)
    }
}

