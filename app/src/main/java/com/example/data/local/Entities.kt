package com.example.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

enum class UserRole { FARMER, CUSTOMER, SUPER_ADMIN }
enum class AccountStatus { ACTIVE, INACTIVE, SUSPENDED }

enum class FarmCategory(val label: String) {
    POULTRY("Poultry Farm"),
    DAIRY("Dairy Farm"),
    CATTLE("Cattle Farm"),
    GOAT("Goat Farm"),
    DUCK("Duck Farm"),
    BIRD("Bird Farm"),
    QUAIL("Quail Farm"),
    MIXED("Mixed Farm"),
    CUSTOM("Custom Farm")
}

enum class AnimalEventType(val label: String) {
    BIRTH("Birth"),
    PURCHASE("Purchase"),
    DEATH("Death"),
    TRANSFER("Transfer"),
    SALE("Sale"),
    VACCINATION("Vaccination"),
    FEEDING("Feeding"),
    MEDICAL("Medical Expense")
}

enum class OrderStatus(val label: String) {
    PENDING("Pending"),
    CONFIRMED("Confirmed"),
    PROCESSING("Processing"),
    READY_FOR_DELIVERY("Ready for Delivery"),
    SHIPPED("Shipped"),
    DELIVERED("Delivered"),
    CANCELLED("Cancelled")
}

enum class PaymentStatus(val label: String) {
    PENDING("Pending"),
    SUCCESSFUL("Successful"),
    FAILED("Failed"),
    REFUNDED("Refunded")
}

enum class AttendanceStatus(val label: String) {
    PRESENT("Present"),
    ABSENT("Absent"),
    LEAVE("Leave")
}

@Entity(
    tableName = "users",
    indices = [Index(value = ["email"], unique = true)]
)
data class UserEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val mobile: String,
    val email: String,
    val passwordHash: String,
    val role: String = UserRole.FARMER.name,
    val status: String = AccountStatus.ACTIVE.name,
    val profileImageUri: String? = null,
    val preferredLanguage: String = "en",
    val preferredCurrency: String = "BDT",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "farms",
    foreignKeys = [
        ForeignKey(entity = UserEntity::class, parentColumns = ["id"], childColumns = ["ownerUserId"], onDelete = ForeignKey.CASCADE)
    ],
    indices = [Index("ownerUserId"), Index(value = ["farmCode"], unique = true)]
)
data class FarmEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val ownerUserId: Long,
    val farmCode: String,
    val name: String,
    val location: String,
    val address: String,
    val category: String = FarmCategory.POULTRY.name,
    val customCategoryName: String = "",
    val imageUri: String? = null,
    val openingDate: Long = System.currentTimeMillis(),
    val managerName: String = "",
    val managerPhone: String = "",
    val initialInvestment: Double = 0.0,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "animal_categories",
    indices = [Index("ownerUserId")]
)
data class AnimalCategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val ownerUserId: Long,
    val name: String,
    val birdCategory: Boolean = false,
    val builtIn: Boolean = false
)

@Entity(
    tableName = "animal_records",
    foreignKeys = [
        ForeignKey(entity = FarmEntity::class, parentColumns = ["id"], childColumns = ["farmId"], onDelete = ForeignKey.CASCADE)
    ],
    indices = [Index("farmId"), Index("ownerUserId")]
)
data class AnimalRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val farmId: Long,
    val ownerUserId: Long,
    val name: String,
    val category: String,
    val birdCategory: Boolean = false,
    val tagNumber: String,
    val breed: String = "",
    val gender: String = "Mixed",
    val dobOrAge: String = "",
    val purchaseDate: Long = System.currentTimeMillis(),
    val purchasePrice: Double = 0.0,
    val quantity: Int = 1,
    val healthStatus: String = "Healthy",
    val notes: String = "",
    val photoUri: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "animal_events",
    foreignKeys = [
        ForeignKey(entity = AnimalRecordEntity::class, parentColumns = ["id"], childColumns = ["animalRecordId"], onDelete = ForeignKey.CASCADE)
    ],
    indices = [Index("animalRecordId"), Index("farmId"), Index("ownerUserId")]
)
data class AnimalEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val animalRecordId: Long,
    val farmId: Long,
    val ownerUserId: Long,
    val eventType: String,
    val quantityDelta: Int = 0,
    val costOrValue: Double = 0.0,
    val description: String = "",
    val eventDate: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "egg_production",
    foreignKeys = [
        ForeignKey(entity = FarmEntity::class, parentColumns = ["id"], childColumns = ["farmId"], onDelete = ForeignKey.CASCADE)
    ],
    indices = [Index("farmId"), Index("ownerUserId"), Index("productionDate")]
)
data class EggProductionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val farmId: Long,
    val ownerUserId: Long,
    val eggCategory: String,
    val productionDate: Long = System.currentTimeMillis(),
    val previousStock: Int = 0,
    val producedCount: Int = 0,
    val brokenCount: Int = 0,
    val soldCount: Int = 0,
    val availableCount: Int = 0,
    val unitSellingPrice: Double = 0.0,
    val totalValue: Double = 0.0
)

@Entity(
    tableName = "inventory_items",
    foreignKeys = [
        ForeignKey(entity = FarmEntity::class, parentColumns = ["id"], childColumns = ["farmId"], onDelete = ForeignKey.CASCADE)
    ],
    indices = [Index("farmId"), Index("ownerUserId")]
)
data class InventoryItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val farmId: Long,
    val ownerUserId: Long,
    val name: String,
    val category: String,
    val quantity: Double = 0.0,
    val unit: String = "pcs",
    val unitCost: Double = 0.0,
    val sellingPrice: Double = 0.0,
    val lowStockThreshold: Double = 10.0,
    val expiryDate: Long? = null,
    val supplierName: String = "",
    val supplierPhone: String = "",
    val listInMarketplace: Boolean = false,
    val description: String = "",
    val imageUri: String? = null,
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "inventory_movements",
    foreignKeys = [
        ForeignKey(entity = InventoryItemEntity::class, parentColumns = ["id"], childColumns = ["inventoryItemId"], onDelete = ForeignKey.CASCADE)
    ],
    indices = [Index("inventoryItemId"), Index("farmId")]
)
data class InventoryMovementEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val inventoryItemId: Long,
    val farmId: Long,
    val ownerUserId: Long,
    val movementType: String, // STOCK_IN, STOCK_OUT, SALE
    val quantityDelta: Double,
    val unitCost: Double = 0.0,
    val supplierName: String = "",
    val note: String = "",
    val date: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "customers",
    indices = [Index("ownerUserId"), Index(value = ["customerCode"], unique = true)]
)
data class CustomerEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val ownerUserId: Long,
    val customerCode: String,
    val name: String,
    val mobile: String,
    val email: String = "",
    val address: String = "",
    val photoUri: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "sales",
    foreignKeys = [
        ForeignKey(entity = FarmEntity::class, parentColumns = ["id"], childColumns = ["farmId"], onDelete = ForeignKey.CASCADE)
    ],
    indices = [Index("farmId"), Index("ownerUserId"), Index("customerId"), Index("saleDate")]
)
data class SaleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val invoiceNumber: String,
    val farmId: Long,
    val ownerUserId: Long,
    val customerId: Long? = null,
    val customerName: String,
    val customerPhone: String,
    val productCategory: String,
    val productName: String,
    val inventoryItemId: Long? = null,
    val animalRecordId: Long? = null,
    val quantity: Double,
    val unitPrice: Double,
    val discount: Double = 0.0,
    val totalAmount: Double,
    val paidAmount: Double,
    val dueAmount: Double,
    val paymentMethod: String = "Cash",
    val saleDate: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "orders",
    indices = [Index("customerUserId"), Index("farmId"), Index("farmOwnerUserId")]
)
data class OrderEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val orderNumber: String,
    val customerUserId: Long,
    val customerName: String,
    val customerPhone: String,
    val deliveryAddress: String,
    val farmId: Long,
    val farmOwnerUserId: Long,
    val farmName: String,
    val itemsSummary: String,
    val serializedItems: String, // "itemId:qty:price:name|..."
    val totalAmount: Double,
    val paymentMethod: String,
    val status: String = OrderStatus.PENDING.name,
    val cancellationRequested: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "expenses",
    foreignKeys = [
        ForeignKey(entity = FarmEntity::class, parentColumns = ["id"], childColumns = ["farmId"], onDelete = ForeignKey.CASCADE)
    ],
    indices = [Index("farmId"), Index("ownerUserId"), Index("expenseDate")]
)
data class ExpenseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val farmId: Long,
    val ownerUserId: Long,
    val category: String,
    val amount: Double,
    val description: String = "",
    val expenseDate: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "employees",
    foreignKeys = [
        ForeignKey(entity = FarmEntity::class, parentColumns = ["id"], childColumns = ["farmId"], onDelete = ForeignKey.CASCADE)
    ],
    indices = [Index("farmId"), Index("ownerUserId")]
)
data class EmployeeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val farmId: Long,
    val ownerUserId: Long,
    val name: String,
    val phone: String,
    val designation: String,
    val salary: Double,
    val joiningDate: Long = System.currentTimeMillis(),
    val canManageInventory: Boolean = true,
    val canManageSales: Boolean = false,
    val canManageLivestock: Boolean = true,
    val lastAttendanceStatus: String = AttendanceStatus.PRESENT.name,
    val lastAttendanceDate: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "salary_payments",
    foreignKeys = [
        ForeignKey(entity = EmployeeEntity::class, parentColumns = ["id"], childColumns = ["employeeId"], onDelete = ForeignKey.CASCADE)
    ],
    indices = [Index("employeeId"), Index("farmId")]
)
data class SalaryPaymentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val employeeId: Long,
    val farmId: Long,
    val ownerUserId: Long,
    val amount: Double,
    val monthLabel: String,
    val paymentDate: Long = System.currentTimeMillis(),
    val note: String = ""
)

@Entity(tableName = "subscription_plans")
data class SubscriptionPlanEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val priceBaseBdt: Double,
    val durationDays: Int,
    val description: String,
    val featuresSummary: String,
    val active: Boolean = true
)

@Entity(
    tableName = "user_subscriptions",
    indices = [Index("userId"), Index("planId")]
)
data class UserSubscriptionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: Long,
    val userName: String,
    val planId: Long,
    val planName: String,
    val pricePaidBdt: Double,
    val currencyCode: String = "BDT",
    val paymentMethod: String,
    val transactionRef: String,
    val paymentStatus: String = PaymentStatus.SUCCESSFUL.name,
    val startDate: Long = System.currentTimeMillis(),
    val expiryDate: Long,
    val status: String = "ACTIVE" // ACTIVE, EXPIRED, PENDING
)

@Entity(
    tableName = "notifications",
    indices = [Index("targetUserId")]
)
data class AppNotificationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val targetUserId: Long, // 0 = broadcast to all
    val title: String,
    val message: String,
    val category: String,
    val readFlag: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "support_requests",
    indices = [Index("userId")]
)
data class SupportRequestEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: Long,
    val userName: String,
    val subject: String,
    val message: String,
    val status: String = "OPEN", // OPEN, RESOLVED
    val adminReply: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "admin_audit_logs")
data class AdminAuditLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val actorUserId: Long,
    val actorName: String,
    val action: String,
    val target: String,
    val details: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "system_settings")
data class SystemSettingsEntity(
    @PrimaryKey val id: Int = 1,
    val announcementBanner: String = "Welcome to Smart Farm Management! Keep livestock vaccinations & low-stock alerts updated.",
    val maintenanceMode: Boolean = false,
    val defaultCurrencyCode: String = "BDT",
    val allowManualPayments: Boolean = true
)
