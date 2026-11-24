package com.example.data.repository

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
                title = "Welcome to Smart Farm Management",
                message = "Your ${role.name.lowercase()} account is ready.",
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
    fun observeUserSubscriptions(userId: Long): Flow<List<UserSubscriptionEntity>> = subAdminDao.observeSubscriptionsByUser(userId)
    fun observeNotifications(userId: Long): Flow<List<AppNotificationEntity>> = subAdminDao.observeNotificationsForUser(userId)
    fun observePlans(): Flow<List<SubscriptionPlanEntity>> = subAdminDao.observePlans()
    fun observeSystemSettings(): Flow<SystemSettingsEntity?> = subAdminDao.observeSystemSettings()
    fun observeUserSupport(userId: Long): Flow<List<SupportRequestEntity>> = subAdminDao.observeSupportByUser(userId)

    // --- Customer Marketplace Streams ---
    fun observeMarketplaceItems(): Flow<List<InventoryItemEntity>> = inventoryDao.observeMarketplaceItems()
    fun observeAllFarmsPublic(): Flow<List<FarmEntity>> = farmDao.observeAllFarms()
    fun observeCustomerOrders(customerId: Long): Flow<List<OrderEntity>> = commerceDao.observeOrdersForCustomer(customerId)

    // --- Super Admin Streams ---
    fun observeAllUsers(): Flow<List<UserEntity>> = userDao.observeAllUsers()
    fun observeAllFarmsAdmin(): Flow<List<FarmEntity>> = farmDao.observeAllFarms()
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
                    address = farm.address.trim()
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
                    address = farm.address.trim()
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
        return if (item.id == 0L) {
            val id = inventoryDao.insertItem(item)
            if (item.quantity > 0.0) {
                inventoryDao.insertMovement(
                    InventoryMovementEntity(
                        inventoryItemId = id,
                        farmId = item.farmId,
                        ownerUserId = item.ownerUserId,
                        movementType = "STOCK_IN",
                        quantityDelta = item.quantity,
                        unitCost = item.unitCost,
                        supplierName = item.supplierName,
                        note = "Initial stock added"
                    )
                )
            }
            checkLowStockNotification(item.copy(id = id))
            id
        } else {
            inventoryDao.updateItem(item.copy(updatedAt = System.currentTimeMillis()))
            checkLowStockNotification(item)
            item.id
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
        farmsMap: Map<Long, FarmEntity>
    ): Result<List<OrderEntity>> {
        if (cartItems.isEmpty()) return Result.failure(IllegalArgumentException("Cart is empty."))
        if (deliveryAddress.isBlank()) return Result.failure(IllegalArgumentException("Delivery address is required."))

        val groupedByFarm = cartItems.groupBy { it.first.farmId }
        val createdOrders = mutableListOf<OrderEntity>()
        for ((farmId, items) in groupedByFarm) {
            val farm = farmsMap[farmId] ?: farmDao.getFarmById(farmId)
            val farmOwnerId = farm?.ownerUserId ?: items.first().first.ownerUserId
            val farmName = farm?.name ?: "Smart Farm"
            val total = items.sumOf { it.first.sellingPrice * it.second }
            val summary = items.joinToString(", ") { "${it.second.toInt()}x ${it.first.name}" }
            val serialized = items.joinToString("|") { "${it.first.id}:${it.second}:${it.first.sellingPrice}:${it.first.name}" }
            val orderNo = "ORD-${(1000..9999).random()}"

            val order = OrderEntity(
                orderNumber = orderNo,
                customerUserId = customer.id,
                customerName = customer.name,
                customerPhone = customer.mobile,
                deliveryAddress = deliveryAddress.trim(),
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
                    title = "New Customer Order $orderNo",
                    message = "${customer.name} ordered $summary ($farmName).",
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
        requireActorPermission(actorUserId, employee.ownerUserId, AppPermission.EMPLOYEES_MANAGE)
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
        requireActorPermission(actorUserId, employee.ownerUserId, AppPermission.EMPLOYEES_MANAGE)
        opsDao.deleteEmployee(employee)
    }

    suspend fun markEmployeeAttendance(employee: EmployeeEntity, status: AttendanceStatus, actorUserId: Long? = null) {
        requireActorPermission(actorUserId, employee.ownerUserId, AppPermission.EMPLOYEES_MANAGE)
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
        requireActorPermission(actorUserId, employee.ownerUserId, AppPermission.EMPLOYEES_MANAGE)
        requireActiveSubscription(employee.ownerUserId, actorUserId, SubscriptionFeature.EMPLOYEES_PERMISSIONS)
        opsDao.insertSalaryPayment(
            SalaryPaymentEntity(
                employeeId = employee.id,
                farmId = employee.farmId,
                ownerUserId = employee.ownerUserId,
                amount = amount,
                monthLabel = monthLabel,
                note = note
            )
        )
        opsDao.insertExpense(
            ExpenseEntity(
                farmId = employee.farmId,
                ownerUserId = employee.ownerUserId,
                category = "Employee Salary",
                amount = amount,
                description = "Salary ($monthLabel) - ${employee.name}"
            )
        )
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

    suspend fun markNotificationsRead(userId: Long): Result<Unit> = runCatching {
        subAdminDao.markAllRead(userId)
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
                details = "Maintenance=${settings.maintenanceMode}, bKash=${settings.bkashNumber}, Nagad=${settings.nagadNumber}, Bank=${settings.bankAccountNumber}"
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
}
