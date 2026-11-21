package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Query("SELECT * FROM users ORDER BY createdAt DESC")
    fun observeAllUsers(): Flow<List<UserEntity>>

    @Query("SELECT * FROM users WHERE id = :id LIMIT 1")
    fun observeUserById(id: Long): Flow<UserEntity?>

    @Query("SELECT * FROM users WHERE id = :id LIMIT 1")
    suspend fun getUserById(id: Long): UserEntity?

    @Query("SELECT * FROM users WHERE LOWER(email) = LOWER(:email) LIMIT 1")
    suspend fun getUserByEmail(email: String): UserEntity?

    @Query("SELECT * FROM users WHERE mobile = :mobile LIMIT 1")
    suspend fun getUserByMobile(mobile: String): UserEntity?

    @Query("SELECT COUNT(*) FROM users")
    suspend fun countUsers(): Int

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(user: UserEntity): Long

    @Update
    suspend fun update(user: UserEntity)
}

@Dao
interface FarmDao {
    @Query("SELECT * FROM farms WHERE ownerUserId = :ownerId ORDER BY createdAt DESC")
    fun observeFarmsByOwner(ownerId: Long): Flow<List<FarmEntity>>

    @Query("SELECT * FROM farms ORDER BY createdAt DESC")
    fun observeAllFarms(): Flow<List<FarmEntity>>

    @Query("SELECT * FROM farms WHERE id = :farmId LIMIT 1")
    suspend fun getFarmById(farmId: Long): FarmEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(farm: FarmEntity): Long

    @Update
    suspend fun update(farm: FarmEntity)

    @Delete
    suspend fun delete(farm: FarmEntity)
}

@Dao
interface AnimalDao {
    @Query("SELECT * FROM animal_categories WHERE ownerUserId = :ownerId OR builtIn = 1 ORDER BY builtIn DESC, name ASC")
    fun observeCategories(ownerId: Long): Flow<List<AnimalCategoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(cat: AnimalCategoryEntity): Long

    @Query("SELECT * FROM animal_records WHERE ownerUserId = :ownerId ORDER BY createdAt DESC")
    fun observeAnimalsByOwner(ownerId: Long): Flow<List<AnimalRecordEntity>>

    @Query("SELECT * FROM animal_records WHERE id = :id LIMIT 1")
    suspend fun getAnimalById(id: Long): AnimalRecordEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAnimal(animal: AnimalRecordEntity): Long

    @Update
    suspend fun updateAnimal(animal: AnimalRecordEntity)

    @Delete
    suspend fun deleteAnimal(animal: AnimalRecordEntity)

    @Query("SELECT * FROM animal_events WHERE ownerUserId = :ownerId ORDER BY eventDate DESC")
    fun observeEventsByOwner(ownerId: Long): Flow<List<AnimalEventEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvent(event: AnimalEventEntity): Long
}

@Dao
interface EggDao {
    @Query("SELECT * FROM egg_production WHERE ownerUserId = :ownerId ORDER BY productionDate DESC, id DESC")
    fun observeEggsByOwner(ownerId: Long): Flow<List<EggProductionEntity>>

    @Query("SELECT * FROM egg_production WHERE farmId = :farmId AND eggCategory = :category ORDER BY productionDate DESC, id DESC LIMIT 1")
    suspend fun getLatestByFarmAndCategory(farmId: Long, category: String): EggProductionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(egg: EggProductionEntity): Long

    @Update
    suspend fun update(egg: EggProductionEntity)

    @Delete
    suspend fun delete(egg: EggProductionEntity)
}

@Dao
interface InventoryDao {
    @Query("SELECT * FROM inventory_items WHERE ownerUserId = :ownerId ORDER BY updatedAt DESC")
    fun observeInventoryByOwner(ownerId: Long): Flow<List<InventoryItemEntity>>

    @Query("SELECT * FROM inventory_items WHERE listInMarketplace = 1 AND quantity > 0 ORDER BY updatedAt DESC")
    fun observeMarketplaceItems(): Flow<List<InventoryItemEntity>>

    @Query("SELECT * FROM inventory_items WHERE id = :id LIMIT 1")
    suspend fun getItemById(id: Long): InventoryItemEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: InventoryItemEntity): Long

    @Update
    suspend fun updateItem(item: InventoryItemEntity)

    @Delete
    suspend fun deleteItem(item: InventoryItemEntity)

    @Query("SELECT * FROM inventory_movements WHERE ownerUserId = :ownerId ORDER BY date DESC")
    fun observeMovementsByOwner(ownerId: Long): Flow<List<InventoryMovementEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMovement(movement: InventoryMovementEntity): Long
}

@Dao
interface CommerceDao {
    @Query("SELECT * FROM customers WHERE ownerUserId = :ownerId ORDER BY createdAt DESC")
    fun observeCustomersByOwner(ownerId: Long): Flow<List<CustomerEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomer(customer: CustomerEntity): Long

    @Update
    suspend fun updateCustomer(customer: CustomerEntity)

    @Delete
    suspend fun deleteCustomer(customer: CustomerEntity)

    @Query("SELECT * FROM sales WHERE ownerUserId = :ownerId ORDER BY saleDate DESC")
    fun observeSalesByOwner(ownerId: Long): Flow<List<SaleEntity>>

    @Query("SELECT * FROM sales ORDER BY saleDate DESC")
    fun observeAllSales(): Flow<List<SaleEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSale(sale: SaleEntity): Long

    @Update
    suspend fun updateSale(sale: SaleEntity)

    @Delete
    suspend fun deleteSale(sale: SaleEntity)

    @Query("SELECT * FROM orders WHERE farmOwnerUserId = :ownerId ORDER BY createdAt DESC")
    fun observeOrdersForFarmer(ownerId: Long): Flow<List<OrderEntity>>

    @Query("SELECT * FROM orders WHERE customerUserId = :customerId ORDER BY createdAt DESC")
    fun observeOrdersForCustomer(customerId: Long): Flow<List<OrderEntity>>

    @Query("SELECT * FROM orders ORDER BY createdAt DESC")
    fun observeAllOrders(): Flow<List<OrderEntity>>

    @Query("SELECT * FROM orders WHERE id = :orderId LIMIT 1")
    suspend fun getOrderById(orderId: Long): OrderEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrder(order: OrderEntity): Long

    @Update
    suspend fun updateOrder(order: OrderEntity)
}

@Dao
interface OperationsDao {
    @Query("SELECT * FROM expenses WHERE ownerUserId = :ownerId ORDER BY expenseDate DESC")
    fun observeExpensesByOwner(ownerId: Long): Flow<List<ExpenseEntity>>

    @Query("SELECT * FROM expenses ORDER BY expenseDate DESC")
    fun observeAllExpenses(): Flow<List<ExpenseEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpense(expense: ExpenseEntity): Long

    @Delete
    suspend fun deleteExpense(expense: ExpenseEntity)

    @Query("SELECT * FROM employees WHERE ownerUserId = :ownerId ORDER BY joiningDate DESC")
    fun observeEmployeesByOwner(ownerId: Long): Flow<List<EmployeeEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEmployee(employee: EmployeeEntity): Long

    @Update
    suspend fun updateEmployee(employee: EmployeeEntity)

    @Delete
    suspend fun deleteEmployee(employee: EmployeeEntity)

    @Query("SELECT * FROM salary_payments WHERE ownerUserId = :ownerId ORDER BY paymentDate DESC")
    fun observeSalaryPaymentsByOwner(ownerId: Long): Flow<List<SalaryPaymentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSalaryPayment(payment: SalaryPaymentEntity): Long
}

@Dao
interface SubscriptionAndAdminDao {
    @Query("SELECT * FROM subscription_plans ORDER BY priceBaseBdt ASC")
    fun observePlans(): Flow<List<SubscriptionPlanEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlan(plan: SubscriptionPlanEntity): Long

    @Update
    suspend fun updatePlan(plan: SubscriptionPlanEntity)

    @Delete
    suspend fun deletePlan(plan: SubscriptionPlanEntity)

    @Query("SELECT * FROM user_subscriptions WHERE userId = :userId ORDER BY startDate DESC")
    fun observeSubscriptionsByUser(userId: Long): Flow<List<UserSubscriptionEntity>>

    @Query("SELECT * FROM user_subscriptions ORDER BY startDate DESC")
    fun observeAllSubscriptions(): Flow<List<UserSubscriptionEntity>>

    @Query("SELECT * FROM user_subscriptions WHERE status = 'ACTIVE' AND expiryDate < :now")
    suspend fun findExpiredActiveSubscriptions(now: Long): List<UserSubscriptionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUserSubscription(sub: UserSubscriptionEntity): Long

    @Update
    suspend fun updateUserSubscription(sub: UserSubscriptionEntity)

    @Query("SELECT * FROM notifications WHERE targetUserId = :userId OR targetUserId = 0 ORDER BY createdAt DESC")
    fun observeNotificationsForUser(userId: Long): Flow<List<AppNotificationEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotification(notification: AppNotificationEntity): Long

    @Query("UPDATE notifications SET readFlag = 1 WHERE targetUserId = :userId OR targetUserId = 0")
    suspend fun markAllRead(userId: Long)

    @Query("SELECT * FROM support_requests WHERE userId = :userId ORDER BY createdAt DESC")
    fun observeSupportByUser(userId: Long): Flow<List<SupportRequestEntity>>

    @Query("SELECT * FROM support_requests ORDER BY createdAt DESC")
    fun observeAllSupport(): Flow<List<SupportRequestEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSupport(req: SupportRequestEntity): Long

    @Update
    suspend fun updateSupport(req: SupportRequestEntity)

    @Query("SELECT * FROM admin_audit_logs ORDER BY createdAt DESC LIMIT 150")
    fun observeAuditLogs(): Flow<List<AdminAuditLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAuditLog(log: AdminAuditLogEntity): Long

    @Query("SELECT * FROM system_settings WHERE id = 1 LIMIT 1")
    fun observeSystemSettings(): Flow<SystemSettingsEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveSystemSettings(settings: SystemSettingsEntity)
}
