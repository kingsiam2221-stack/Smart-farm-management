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

    @Query("DELETE FROM users WHERE id = :userId")
    suspend fun deleteById(userId: Long)

    @Query("SELECT * FROM users WHERE employerOwnerId = :ownerId")
    suspend fun getEmployeesByOwnerId(ownerId: Long): List<UserEntity>

    @Query("DELETE FROM users WHERE employerOwnerId = :ownerId")
    suspend fun deleteEmployeesByOwnerId(ownerId: Long)
}

@Dao
interface FarmDao {
    @Query("SELECT * FROM farms WHERE ownerUserId = :ownerId ORDER BY createdAt DESC")
    fun observeFarmsByOwner(ownerId: Long): Flow<List<FarmEntity>>

    @Query("SELECT * FROM farms ORDER BY createdAt DESC")
    fun observeAllFarms(): Flow<List<FarmEntity>>

    @Query("SELECT * FROM farms WHERE id IN (SELECT DISTINCT farmId FROM inventory_items WHERE listInMarketplace = 1 AND quantity > 0)")
    fun observeMarketplaceFarms(): Flow<List<FarmEntity>>

    @Query("SELECT * FROM farms WHERE id = :farmId LIMIT 1")
    suspend fun getFarmById(farmId: Long): FarmEntity?

    @Query("SELECT * FROM farms WHERE farmCode = :code LIMIT 1")
    suspend fun getFarmByCode(code: String): FarmEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(farm: FarmEntity): Long

    @Update
    suspend fun update(farm: FarmEntity)

    @Delete
    suspend fun delete(farm: FarmEntity)

    @Query("DELETE FROM farms WHERE ownerUserId = :ownerId")
    suspend fun deleteFarmsByOwner(ownerId: Long)
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

    @Query("DELETE FROM animal_categories WHERE ownerUserId = :ownerId AND builtIn = 0")
    suspend fun deleteCustomCategoriesByOwner(ownerId: Long)
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

    @Query("SELECT * FROM inventory_items ORDER BY updatedAt DESC")
    fun observeAllInventoryAdmin(): Flow<List<InventoryItemEntity>>

    @Query("SELECT * FROM inventory_items WHERE listInMarketplace = 1 AND quantity > 0 ORDER BY updatedAt DESC")
    fun observeMarketplaceItems(): Flow<List<InventoryItemEntity>>

    @Query("""
        SELECT * FROM inventory_items
        WHERE listInMarketplace = 1 AND quantity > 0
          AND (:category IS NULL OR category = :category)
          AND (:search = '' OR name LIKE '%' || :search || '%' OR description LIKE '%' || :search || '%' OR category LIKE '%' || :search || '%')
        ORDER BY updatedAt DESC
        LIMIT :limit
    """)
    fun observeMarketplaceItemsFiltered(search: String, category: String?, limit: Int): Flow<List<InventoryItemEntity>>

    @Query("""
        SELECT COUNT(*) FROM inventory_items
        WHERE listInMarketplace = 1 AND quantity > 0
          AND (:category IS NULL OR category = :category)
          AND (:search = '' OR name LIKE '%' || :search || '%' OR description LIKE '%' || :search || '%' OR category LIKE '%' || :search || '%')
    """)
    fun countMarketplaceItemsFiltered(search: String, category: String?): Flow<Int>

    @Query("SELECT DISTINCT category FROM inventory_items WHERE listInMarketplace = 1 AND quantity > 0 ORDER BY category ASC")
    fun observeMarketplaceCategories(): Flow<List<String>>

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

    @Query("DELETE FROM inventory_items WHERE ownerUserId = :ownerId")
    suspend fun deleteByOwner(ownerId: Long)

    @Query("DELETE FROM inventory_movements WHERE ownerUserId = :ownerId")
    suspend fun deleteMovementsByOwner(ownerId: Long)
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

    @Query("DELETE FROM customers WHERE ownerUserId = :ownerId")
    suspend fun deleteCustomersByOwner(ownerId: Long)

    @Query("DELETE FROM orders WHERE farmOwnerUserId = :ownerId OR customerUserId = :ownerId")
    suspend fun deleteOrdersByUser(ownerId: Long)
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

    @Query("SELECT * FROM employees ORDER BY joiningDate DESC")
    fun observeAllEmployees(): Flow<List<EmployeeEntity>>

    @Query("SELECT * FROM employees WHERE id = :id LIMIT 1")
    suspend fun getEmployeeById(id: Long): EmployeeEntity?

    @Query("SELECT * FROM employees WHERE linkedUserId = :userId LIMIT 1")
    suspend fun getEmployeeByLinkedUserId(userId: Long): EmployeeEntity?

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

    @Query("UPDATE employees SET linkedUserId = NULL WHERE linkedUserId = :userId")
    suspend fun unlinkEmployeeUser(userId: Long)
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

    @Query("UPDATE user_subscriptions SET status = :newStatus WHERE userId = :userId AND status = 'ACTIVE'")
    suspend fun deactivateActiveSubscriptionsForUser(userId: Long, newStatus: String)

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

    @Query("DELETE FROM user_subscriptions WHERE userId = :userId")
    suspend fun deleteSubscriptionsByUser(userId: Long)

    @Query("DELETE FROM notifications WHERE targetUserId = :userId")
    suspend fun deleteNotificationsForUser(userId: Long)

    @Query("DELETE FROM support_requests WHERE userId = :userId")
    suspend fun deleteSupportByUser(userId: Long)
}

@Dao
interface PermissionDao {
    @Query("SELECT * FROM employee_permissions WHERE ownerUserId = :ownerId")
    fun observePermissionsByOwner(ownerId: Long): Flow<List<EmployeePermissionEntity>>

    @Query("SELECT * FROM employee_permissions")
    fun observeAllPermissions(): Flow<List<EmployeePermissionEntity>>

    @Query("SELECT * FROM employee_permissions WHERE employeeId = :employeeId")
    fun observePermissionsForEmployee(employeeId: Long): Flow<List<EmployeePermissionEntity>>

    @Query("SELECT * FROM employee_permissions WHERE employeeId = :employeeId")
    suspend fun getPermissionsForEmployee(employeeId: Long): List<EmployeePermissionEntity>

    @Query("SELECT * FROM employee_permissions WHERE employeeId = :employeeId AND permissionKey = :permissionKey LIMIT 1")
    suspend fun getPermission(employeeId: Long, permissionKey: String): EmployeePermissionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPermission(permission: EmployeePermissionEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPermissions(permissions: List<EmployeePermissionEntity>)

    @Query("SELECT * FROM custom_permission_options WHERE ownerUserId = :ownerId ORDER BY createdAt DESC")
    fun observeCustomOptionsByOwner(ownerId: Long): Flow<List<CustomPermissionOptionEntity>>

    @Query("SELECT * FROM custom_permission_options ORDER BY createdAt DESC")
    fun observeAllCustomOptions(): Flow<List<CustomPermissionOptionEntity>>

    @Query("SELECT * FROM custom_permission_options WHERE permissionKey = :key LIMIT 1")
    suspend fun getCustomOptionByKey(key: String): CustomPermissionOptionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomOption(option: CustomPermissionOptionEntity): Long

    @Delete
    suspend fun deleteCustomOption(option: CustomPermissionOptionEntity)

    @Query("DELETE FROM employee_permissions WHERE permissionKey = :permissionKey")
    suspend fun deletePermissionsByKey(permissionKey: String)

    @Query("DELETE FROM employee_permissions WHERE ownerUserId = :ownerId")
    suspend fun deletePermissionsByOwner(ownerId: Long)

    @Query("DELETE FROM custom_permission_options WHERE ownerUserId = :ownerId")
    suspend fun deleteCustomOptionsByOwner(ownerId: Long)
}

@Dao
interface AgriDao {
    @Query("SELECT * FROM agri_lands WHERE ownerUserId = :ownerId ORDER BY createdAt DESC")
    fun observeLandsByOwner(ownerId: Long): Flow<List<AgriLandEntity>>

    @Query("SELECT * FROM agri_lands WHERE id = :id LIMIT 1")
    suspend fun getLandById(id: Long): AgriLandEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLand(land: AgriLandEntity): Long

    @Update
    suspend fun updateLand(land: AgriLandEntity)

    @Delete
    suspend fun deleteLand(land: AgriLandEntity)

    @Query("SELECT * FROM agri_crops WHERE ownerUserId = :ownerId ORDER BY updatedAt DESC, createdAt DESC")
    fun observeCropsByOwner(ownerId: Long): Flow<List<AgriCropEntity>>

    @Query("SELECT * FROM agri_crops WHERE id = :id LIMIT 1")
    suspend fun getCropById(id: Long): AgriCropEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCrop(crop: AgriCropEntity): Long

    @Update
    suspend fun updateCrop(crop: AgriCropEntity)

    @Delete
    suspend fun deleteCrop(crop: AgriCropEntity)

    @Query("SELECT * FROM agri_expenses WHERE ownerUserId = :ownerId ORDER BY expenseDate DESC, id DESC")
    fun observeExpensesByOwner(ownerId: Long): Flow<List<AgriExpenseEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpense(expense: AgriExpenseEntity): Long

    @Delete
    suspend fun deleteExpense(expense: AgriExpenseEntity)

    @Query("DELETE FROM agri_expenses WHERE cropId = :cropId")
    suspend fun deleteExpensesByCropId(cropId: Long)

    @Query("DELETE FROM agri_expenses WHERE ownerUserId = :ownerId")
    suspend fun deleteExpensesByOwner(ownerId: Long)

    @Query("DELETE FROM agri_crops WHERE ownerUserId = :ownerId")
    suspend fun deleteCropsByOwner(ownerId: Long)

    @Query("DELETE FROM agri_lands WHERE ownerUserId = :ownerId")
    suspend fun deleteLandsByOwner(ownerId: Long)
}

@Dao
interface ProductAdDao {
    @Query("SELECT * FROM product_ads WHERE ownerUserId = :ownerId ORDER BY createdAt DESC")
    fun observeAdsByOwner(ownerId: Long): Flow<List<ProductAdEntity>>

    @Query("SELECT * FROM product_ads ORDER BY createdAt DESC")
    fun observeAllAdsAdmin(): Flow<List<ProductAdEntity>>

    @Query("SELECT * FROM product_ads WHERE isActiveInShop = 1 ORDER BY createdAt DESC LIMIT 50")
    fun observeActiveShopAds(): Flow<List<ProductAdEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAd(ad: ProductAdEntity): Long

    @Update
    suspend fun updateAd(ad: ProductAdEntity)

    @Delete
    suspend fun deleteAd(ad: ProductAdEntity)

    @Query("DELETE FROM product_ads WHERE inventoryItemId = :inventoryItemId")
    suspend fun deleteAdsByInventoryItem(inventoryItemId: Long)

    @Query("DELETE FROM product_ads WHERE ownerUserId = :ownerId")
    suspend fun deleteAdsByOwner(ownerId: Long)
}


