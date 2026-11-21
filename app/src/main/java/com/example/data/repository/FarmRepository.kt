package com.example.data.repository

import com.example.data.local.*
import kotlinx.coroutines.flow.Flow
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
        if (role == UserRole.SUPER_ADMIN) {
            return Result.failure(SecurityException("Public Super Admin registration is restricted."))
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

    // --- Farm CRUD ---
    suspend fun saveFarm(farm: FarmEntity): Long {
        return if (farm.id == 0L) {
            val code = farm.farmCode.ifBlank { "FM-${(1000..9999).random()}" }
            farmDao.insert(farm.copy(farmCode = code))
        } else {
            farmDao.update(farm)
            farm.id
        }
    }

    suspend fun deleteFarm(farm: FarmEntity) {
        farmDao.delete(farm)
    }

    // --- Animal & Bird CRUD + Auto Count Sync ---
    suspend fun addCustomAnimalCategory(ownerId: Long, name: String, isBird: Boolean) {
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

    suspend fun saveAnimalRecord(record: AnimalRecordEntity): Long {
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

    suspend fun deleteAnimalRecord(record: AnimalRecordEntity) {
        animalDao.deleteAnimal(record)
    }

    suspend fun recordAnimalEvent(
        animal: AnimalRecordEntity,
        eventType: AnimalEventType,
        quantityDelta: Int,
        costOrValue: Double,
        description: String
    ) {
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
        productionDate: Long = System.currentTimeMillis()
    ) {
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

    suspend fun deleteEggRecord(record: EggProductionEntity) {
        eggDao.delete(record)
    }

    // --- Inventory Management ---
    suspend fun saveInventoryItem(item: InventoryItemEntity): Long {
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
        note: String
    ) {
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

    suspend fun deleteInventoryItem(item: InventoryItemEntity) {
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
    suspend fun saveCustomer(customer: CustomerEntity): Long {
        return if (customer.id == 0L) {
            val code = customer.customerCode.ifBlank { "CUS-${(100..999).random()}" }
            commerceDao.insertCustomer(customer.copy(customerCode = code))
        } else {
            commerceDao.updateCustomer(customer)
            customer.id
        }
    }

    suspend fun deleteCustomer(customer: CustomerEntity) {
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
        paymentMethod: String
    ): SaleEntity {
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

    suspend fun settleSaleDue(sale: SaleEntity, additionalPayment: Double) {
        if (additionalPayment <= 0.0) return
        val newPaid = (sale.paidAmount + additionalPayment).coerceAtMost(sale.totalAmount)
        val newDue = (sale.totalAmount - newPaid).coerceAtLeast(0.0)
        commerceDao.updateSale(sale.copy(paidAmount = newPaid, dueAmount = newDue))
    }

    suspend fun deleteSale(sale: SaleEntity) {
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

    suspend fun updateOrderStatus(order: OrderEntity, newStatus: OrderStatus) {
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
    suspend fun addExpense(expense: ExpenseEntity) {
        opsDao.insertExpense(expense)
    }

    suspend fun deleteExpense(expense: ExpenseEntity) {
        opsDao.deleteExpense(expense)
    }

    suspend fun saveEmployee(employee: EmployeeEntity) {
        if (employee.id == 0L) opsDao.insertEmployee(employee) else opsDao.updateEmployee(employee)
    }

    suspend fun deleteEmployee(employee: EmployeeEntity) {
        opsDao.deleteEmployee(employee)
    }

    suspend fun markEmployeeAttendance(employee: EmployeeEntity, status: AttendanceStatus) {
        opsDao.updateEmployee(
            employee.copy(
                lastAttendanceStatus = status.name,
                lastAttendanceDate = System.currentTimeMillis()
            )
        )
    }

    suspend fun payEmployeeSalary(employee: EmployeeEntity, amount: Double, monthLabel: String, note: String) {
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

    // --- Subscriptions & Notifications ---
    suspend fun checkExpiredSubscriptions() {
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
        transactionRef: String
    ) {
        val now = System.currentTimeMillis()
        val isManual = paymentMethod.contains("Manual", ignoreCase = true)
        val status = if (isManual) "PENDING" else "ACTIVE"
        val payStatus = if (isManual) PaymentStatus.PENDING.name else PaymentStatus.SUCCESSFUL.name
        val expiry = now + plan.durationDays * 86_400_000L

        subAdminDao.insertUserSubscription(
            UserSubscriptionEntity(
                userId = user.id,
                userName = user.name,
                planId = plan.id,
                planName = plan.name,
                pricePaidBdt = plan.priceBaseBdt,
                currencyCode = currencyCode,
                paymentMethod = paymentMethod,
                transactionRef = transactionRef.ifBlank { "TXN-${(100000..999999).random()}" },
                paymentStatus = payStatus,
                startDate = now,
                expiryDate = expiry,
                status = status
            )
        )
        subAdminDao.insertNotification(
            AppNotificationEntity(
                targetUserId = user.id,
                title = if (isManual) "Payment Verification Pending" else "Subscription Activated!",
                message = if (isManual) "Your ${plan.name} payment via $paymentMethod is awaiting admin approval."
                else "Your ${plan.name} is active for ${plan.durationDays} days.",
                category = if (isManual) "PAYMENT" else "SUBSCRIPTION"
            )
        )
    }

    suspend fun markNotificationsRead(userId: Long) {
        subAdminDao.markAllRead(userId)
    }

    suspend fun submitSupportRequest(user: UserEntity, subject: String, message: String) {
        if (subject.isBlank() || message.isBlank()) return
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
                details = "Maintenance=${settings.maintenanceMode}, DefaultCurrency=${settings.defaultCurrencyCode}"
            )
        )
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
