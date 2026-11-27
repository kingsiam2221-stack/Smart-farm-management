package com.example.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

enum class UserRole { FARMER, CUSTOMER, EMPLOYEE, SUPER_ADMIN }
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
    val employerOwnerId: Long? = null,
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
    val bkashNumber: String = "",
    val nagadNumber: String = "",
    val bankAccountInfo: String = "",
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

enum class ProductSourcingType(val key: String) {
    FARM_PRODUCED("FARM_PRODUCED"),
    PURCHASED_RESALE("PURCHASED_RESALE")
}

object AgriProductCatalog {
    val onlineCategories: List<String> = listOf(
        "Rice",
        "Wheat",
        "Mustard",
        "Vegetables",
        "Fruits",
        "Pulses & Lentils",
        "Spices",
        "Seeds",
        "Fertilizer",
        "Honey",
        "Fish",
        "Eggs",
        "Milk",
        "Meat",
        "Live Animals",
        "Chickens",
        "Ducks",
        "Cows",
        "Goats",
        "Birds",
        "Animal Feed",
        "Poultry Feed",
        "Farm Equipment",
        "Other"
    )

    val units: List<String> = listOf(
        "kg",
        "mon",
        "ton",
        "liter",
        "pcs",
        "dozen",
        "tray",
        "bag",
        "pack"
    )
}

@Entity(
    tableName = "inventory_items",
    foreignKeys = [
        ForeignKey(entity = FarmEntity::class, parentColumns = ["id"], childColumns = ["farmId"], onDelete = ForeignKey.CASCADE)
    ],
    indices = [
        Index("farmId"),
        Index("ownerUserId"),
        Index(value = ["listInMarketplace", "quantity", "updatedAt"]),
        Index(value = ["category"])
    ]
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
    val sourcingType: String = ProductSourcingType.FARM_PRODUCED.key,
    val sellerBkashNumber: String = "",
    val sellerNagadNumber: String = "",
    val sellerBankAccount: String = "",
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
    indices = [
        Index("customerUserId"),
        Index("farmId"),
        Index("farmOwnerUserId"),
        Index(value = ["farmOwnerUserId", "status", "createdAt"])
    ]
)
data class OrderEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val orderNumber: String,
    val customerUserId: Long,
    val customerName: String,
    val customerPhone: String,
    val deliveryAddress: String,
    val deliveryDistrict: String = "",
    val deliveryPoliceStation: String = "",
    val deliveryLandmark: String = "",
    val paymentReference: String = "",
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
    val lastAttendanceDate: Long = System.currentTimeMillis(),
    val email: String = "",
    val linkedUserId: Long? = null
)

enum class EmployeePaymentType(val key: String, val labelEn: String, val labelBn: String) {
    SALARY("SALARY", "Salary", "বেতন"),
    BONUS("BONUS", "Bonus", "বোনাস"),
    EXPENSE("EXPENSE", "Employee Expense", "কর্মচারী খরচ"),
    TARGET_REWARD("TARGET_REWARD", "Target Reward Bonus", "টার্গেট বোনাস");

    companion object {
        fun fromKey(key: String): EmployeePaymentType =
            entries.firstOrNull { it.key == key } ?: SALARY
    }
}

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
    val note: String = "",
    val paymentType: String = EmployeePaymentType.SALARY.key
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
    indices = [
        Index("targetUserId"),
        Index(value = ["targetUserId", "readFlag", "createdAt"])
    ]
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
    val announcementBanner: String = "Welcome to Smart Buy and Sell! Keep livestock vaccinations & low-stock alerts updated.",
    val maintenanceMode: Boolean = false,
    val defaultCurrencyCode: String = "BDT",
    val allowManualPayments: Boolean = true,
    val subscriptionLockedFeaturesCsv: String = SubscriptionFeature.DEFAULT_LOCKED_CSV,
    val bkashEnabled: Boolean = true,
    val bkashNumber: String = "01700-000000",
    val bkashAccountType: String = "Merchant",
    val nagadEnabled: Boolean = true,
    val nagadNumber: String = "01800-000000",
    val nagadAccountType: String = "Personal",
    val bankEnabled: Boolean = true,
    val bankName: String = "Dutch-Bangla Bank (DBBL)",
    val bankAccountName: String = "Smart Farm Management",
    val bankAccountNumber: String = "123-456-7890123",
    val bankBranch: String = "Dhaka Main Branch",
    val paymentInstructions: String = "Send plan fee to the Developer bKash, Nagad, or Bank Account shown and enter your TrxID / Reference.",
    val requireAdminApprovalForAll: Boolean = false,
    val developerName: String = "Smart Farm Support Team",
    val developerPhone: String = "+8801700000000",
    val developerWhatsApp: String = "+8801700000000",
    val developerEmail: String = "support@smartfarm.app",
    val developerSupportNote: String = "Contact our team by phone or WhatsApp anytime for app support, bug reports, or business setup.",
    val adminPinHash: String = "",
    // Super Admin Login Screen Customization & Portal Controls
    val loginShowCustomerLogin: Boolean = true,
    val loginShowCustomerDemo: Boolean = true,
    val loginShowCustomerSignup: Boolean = true,
    val loginShowFarmerLogin: Boolean = true,
    val loginShowFarmerDemo: Boolean = true,
    val loginShowFarmerSignup: Boolean = true,
    val loginShowEmployeeLogin: Boolean = true,
    val loginShowEmployeeDemo: Boolean = true,
    val loginShowGoogleSignIn: Boolean = true,
    val loginShowForgotPassword: Boolean = true,
    val loginBannerTitle: String = "",
    val loginBannerSubtitle: String = "",
    val loginCustomerTitle: String = "",
    val loginCustomerSubtitle: String = "",
    val loginFarmerTitle: String = "",
    val loginFarmerSubtitle: String = "",
    val loginEmployeeTitle: String = "",
    val loginEmployeeSubtitle: String = "",
    val loginCustomNotice: String = "",
    // Super Admin Order Payment Options Control (Customer Checkout & Seller Receiving Accounts)
    val orderShowBkash: Boolean = true,
    val orderShowNagad: Boolean = true,
    val orderShowBank: Boolean = true,
    val orderShowCod: Boolean = true,
    val orderOverrideSellerPayments: Boolean = false,
    val orderBkashNumber: String = "",
    val orderNagadNumber: String = "",
    val orderBankAccount: String = "",
    val orderCustomNote: String = ""
)

@Entity(
    tableName = "employee_permissions",
    foreignKeys = [
        ForeignKey(entity = EmployeeEntity::class, parentColumns = ["id"], childColumns = ["employeeId"], onDelete = ForeignKey.CASCADE)
    ],
    indices = [
        Index("ownerUserId"),
        Index(value = ["employeeId", "permissionKey"], unique = true)
    ]
)
data class EmployeePermissionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val employeeId: Long,
    val ownerUserId: Long,
    val permissionKey: String,
    val granted: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "custom_permission_options",
    indices = [
        Index("ownerUserId"),
        Index(value = ["permissionKey"], unique = true)
    ]
)
data class CustomPermissionOptionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val ownerUserId: Long,
    val permissionKey: String,
    val titleEn: String,
    val titleBn: String = "",
    val descriptionEn: String = "",
    val descriptionBn: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

// --- Agriculture, Crop, Land Plot & Sector Expense Ledger ---

enum class AgriCropType(val key: String, val labelEn: String, val labelBn: String) {
    RICE("Rice", "Rice / Paddy", "ধান / চাল"),
    WHEAT("Wheat", "Wheat", "গম"),
    CORN("Corn", "Corn / Maize", "ভুট্টা"),
    CATTLE_FEED("Cattle Feed", "Cattle Feed / Fodder", "গোখাদ্য / ঘাস"),
    MUSTARD("Mustard", "Mustard", "সরিষা"),
    POTATO("Potato", "Potato", "আলু"),
    JUTE("Jute", "Jute", "পাট"),
    VEGETABLES("Vegetables", "Vegetables", "শাকসবজি"),
    PULSES("Pulses", "Pulses & Lentils", "ডাল শস্য"),
    OTHER("Other Crop", "Other Crop", "অন্যান্য ফসল");

    companion object {
        fun fromKey(key: String): AgriCropType = entries.firstOrNull { it.key.equals(key, ignoreCase = true) || it.name.equals(key, ignoreCase = true) } ?: OTHER
    }
}

enum class AgriLandUnit(val key: String, val labelEn: String, val labelBn: String) {
    DECIMAL("Decimal", "Decimal", "শতাংশ"),
    BIGHA("Bigha", "Bigha", "বিঘা"),
    KATHA("Katha", "Katha", "কাঠা"),
    ACRE("Acre", "Acre", "একর"),
    HECTARE("Hectare", "Hectare", "হেক্টর");

    companion object {
        fun fromKey(key: String): AgriLandUnit = entries.firstOrNull { it.key.equals(key, ignoreCase = true) || it.name.equals(key, ignoreCase = true) } ?: DECIMAL
    }
}

enum class AgriSector(val key: String, val labelEn: String, val labelBn: String) {
    SEEDS("SEEDS", "Seeds & Seedlings", "বীজ ও চারা"),
    FERTILIZER("FERTILIZER", "Fertilizer & Manure", "সার ও জৈব সার"),
    PESTICIDE("PESTICIDE", "Pesticide & Medicine", "কীটনাশক ও ওষুধ"),
    IRRIGATION("IRRIGATION", "Irrigation & Water", "সেচ ও পানি"),
    LABOR("LABOR", "Labor & Workers", "শ্রমিক মজুরি"),
    TILLAGE("TILLAGE", "Tillage & Tractor", "হালচাষ ও ট্রাক্টর"),
    LAND_RENT("LAND_RENT", "Land Rent / Lease", "জমির ভাড়া / লিজ"),
    HARVEST_TRANSPORT("HARVEST_TRANSPORT", "Harvest & Transport", "কাটা, মাড়াই ও পরিবহন"),
    OTHER("OTHER", "Other Sector", "অন্যান্য খাত");

    companion object {
        fun fromKey(key: String): AgriSector = entries.firstOrNull { it.key.equals(key, ignoreCase = true) || it.name.equals(key, ignoreCase = true) } ?: OTHER
    }
}

enum class AgriCropStatus(val key: String, val labelEn: String, val labelBn: String) {
    GROWING("GROWING", "Growing", "চাষাবাদ চলছে"),
    HARVESTED("HARVESTED", "Harvested", "ফসল তোলা হয়েছে"),
    SOLD("SOLD", "Sold / Closed", "বিক্রি সম্পন্ন");

    companion object {
        fun fromKey(key: String): AgriCropStatus = entries.firstOrNull { it.key.equals(key, ignoreCase = true) || it.name.equals(key, ignoreCase = true) } ?: GROWING
    }
}

enum class AgriSeason(val key: String, val labelEn: String, val labelBn: String) {
    BORO("Boro", "Boro (Winter-Spring)", "বোরো মৌসুম"),
    AMAN("Aman", "Aman (Monsoon)", "আমন মৌসুম"),
    AUS("Aus", "Aus (Summer)", "আউশ মৌসুম"),
    RABI("Rabi", "Rabi (Winter)", "রবি মৌসুম"),
    KHARIF("Kharif", "Kharif", "খরিপ মৌসুম"),
    YEAR_ROUND("Year-Round", "Year-Round", "সারা বছর");

    companion object {
        fun fromKey(key: String): AgriSeason = entries.firstOrNull { it.key.equals(key, ignoreCase = true) || it.name.equals(key, ignoreCase = true) } ?: RABI
    }
}

@Entity(
    tableName = "agri_lands",
    indices = [Index("ownerUserId"), Index("farmId")]
)
data class AgriLandEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val ownerUserId: Long,
    val farmId: Long? = null,
    val name: String,
    val areaSize: Double = 0.0,
    val areaUnit: String = AgriLandUnit.DECIMAL.key,
    val location: String = "",
    val soilOrLandType: String = "",
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "agri_crops",
    foreignKeys = [
        ForeignKey(entity = AgriLandEntity::class, parentColumns = ["id"], childColumns = ["landId"], onDelete = ForeignKey.CASCADE)
    ],
    indices = [Index("ownerUserId"), Index("landId"), Index("farmId")]
)
data class AgriCropEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val ownerUserId: Long,
    val farmId: Long? = null,
    val landId: Long,
    val cropType: String = AgriCropType.RICE.key,
    val title: String,
    val variety: String = "",
    val season: String = AgriSeason.BORO.key,
    val plantingDate: Long = System.currentTimeMillis(),
    val status: String = AgriCropStatus.GROWING.key,
    val yieldQuantity: Double = 0.0,
    val yieldUnit: String = "mon",
    val marketPricePerUnit: Double = 0.0,
    val yieldMarketWorth: Double = 0.0,
    val soldAmount: Double = 0.0,
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "agri_expenses",
    foreignKeys = [
        ForeignKey(entity = AgriLandEntity::class, parentColumns = ["id"], childColumns = ["landId"], onDelete = ForeignKey.CASCADE)
    ],
    indices = [Index("ownerUserId"), Index("landId"), Index("cropId"), Index("expenseDate")]
)
data class AgriExpenseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val ownerUserId: Long,
    val farmId: Long? = null,
    val landId: Long,
    val cropId: Long? = null,
    val sector: String = AgriSector.SEEDS.key,
    val customSectorName: String = "",
    val amount: Double,
    val description: String = "",
    val expenseDate: Long = System.currentTimeMillis()
)

data class AgriCropSummary(
    val crop: AgriCropEntity,
    val totalSpent: Double,
    val yieldWorth: Double,
    val effectiveReturn: Double,
    val profitOrLoss: Double,
    val sectorTotals: Map<String, Double>
)

data class AgriLandSummary(
    val land: AgriLandEntity,
    val cropsCount: Int,
    val totalSpent: Double,
    val totalYieldWorth: Double,
    val totalSoldRevenue: Double,
    val effectiveReturn: Double,
    val profitOrLoss: Double,
    val costPerUnitArea: Double,
    val sectorTotals: Map<String, Double>
)

data class AgriOverviewSummary(
    val totalLands: Int,
    val totalCrops: Int,
    val totalSpent: Double,
    val totalYieldWorth: Double,
    val totalSoldRevenue: Double,
    val effectiveReturn: Double,
    val netProfitOrLoss: Double,
    val sectorTotals: Map<String, Double>
)

object AgriLedgerCalculator {
    fun resolveYieldWorth(crop: AgriCropEntity): Double {
        return if (crop.yieldMarketWorth > 0.0) {
            crop.yieldMarketWorth
        } else {
            (crop.yieldQuantity.coerceAtLeast(0.0) * crop.marketPricePerUnit.coerceAtLeast(0.0))
        }
    }

    fun resolveEffectiveReturn(crop: AgriCropEntity): Double {
        val worth = resolveYieldWorth(crop)
        return when {
            crop.soldAmount > 0.0 -> crop.soldAmount
            worth > 0.0 -> worth
            else -> 0.0
        }
    }

    fun summarizeCrop(crop: AgriCropEntity, expenses: List<AgriExpenseEntity>): AgriCropSummary {
        val cropExpenses = expenses.filter { it.cropId == crop.id }
        val spent = cropExpenses.sumOf { it.amount }
        val worth = resolveYieldWorth(crop)
        val ret = resolveEffectiveReturn(crop)
        val sectors = cropExpenses.groupBy { it.sector }.mapValues { (_, list) -> list.sumOf { it.amount } }
        return AgriCropSummary(
            crop = crop,
            totalSpent = spent,
            yieldWorth = worth,
            effectiveReturn = ret,
            profitOrLoss = ret - spent,
            sectorTotals = sectors
        )
    }

    fun summarizeLand(
        land: AgriLandEntity,
        crops: List<AgriCropEntity>,
        expenses: List<AgriExpenseEntity>
    ): AgriLandSummary {
        val landCrops = crops.filter { it.landId == land.id }
        val landExpenses = expenses.filter { it.landId == land.id }
        val spent = landExpenses.sumOf { it.amount }
        val yieldWorth = landCrops.sumOf { resolveYieldWorth(it) }
        val sold = landCrops.sumOf { it.soldAmount }
        val effectiveRet = landCrops.sumOf { resolveEffectiveReturn(it) }
        val costPerUnit = if (land.areaSize > 0.0) spent / land.areaSize else 0.0
        val sectors = landExpenses.groupBy { it.sector }.mapValues { (_, list) -> list.sumOf { it.amount } }
        return AgriLandSummary(
            land = land,
            cropsCount = landCrops.size,
            totalSpent = spent,
            totalYieldWorth = yieldWorth,
            totalSoldRevenue = sold,
            effectiveReturn = effectiveRet,
            profitOrLoss = effectiveRet - spent,
            costPerUnitArea = costPerUnit,
            sectorTotals = sectors
        )
    }

    fun summarizeOverview(
        lands: List<AgriLandEntity>,
        crops: List<AgriCropEntity>,
        expenses: List<AgriExpenseEntity>
    ): AgriOverviewSummary {
        val totalSpent = expenses.sumOf { it.amount }
        val yieldWorth = crops.sumOf { resolveYieldWorth(it) }
        val sold = crops.sumOf { it.soldAmount }
        val effectiveRet = crops.sumOf { resolveEffectiveReturn(it) }
        val sectors = expenses.groupBy { it.sector }.mapValues { (_, list) -> list.sumOf { it.amount } }
        return AgriOverviewSummary(
            totalLands = lands.size,
            totalCrops = crops.size,
            totalSpent = totalSpent,
            totalYieldWorth = yieldWorth,
            totalSoldRevenue = sold,
            effectiveReturn = effectiveRet,
            netProfitOrLoss = effectiveRet - totalSpent,
            sectorTotals = sectors
        )
    }
}

@Entity(
    tableName = "product_ads",
    indices = [
        Index("ownerUserId"),
        Index("farmId"),
        Index("inventoryItemId"),
        Index(value = ["isActiveInShop", "createdAt"])
    ]
)
data class ProductAdEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val ownerUserId: Long,
    val farmId: Long = 0L,
    val inventoryItemId: Long? = null,
    val productName: String,
    val farmName: String = "",
    val priceText: String = "",
    val imageUri: String? = null,
    val hookHeadlineBn: String,
    val promoBadgeBn: String = "১০০% খাঁটি",
    val benefitsPipeSeparatedBn: String,
    val callToActionBn: String = "এখনই অর্ডার করুন • এখানে ক্লিক করুন",
    val voiceoverScriptBn: String,
    val orderStepsPipeSeparatedBn: String,
    val accentColorHex: String = "#1B5E20",
    val isActiveInShop: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)

enum class EmployeeTaskTimeSlot(val key: String, val orderIndex: Int, val labelEn: String, val labelBn: String) {
    MORNING("MORNING", 0, "Morning (6 AM - 11 AM)", "সকাল (৬টা - ১১টা)"),
    AFTERNOON("AFTERNOON", 1, "Afternoon (11 AM - 4 PM)", "দুপুর (১১টা - ৪টা)"),
    EVENING("EVENING", 2, "Evening (4 PM - 8 PM)", "সন্ধ্যা (৪টা - ৮টা)"),
    NIGHT("NIGHT", 3, "Night (8 PM - 11 PM)", "রাত (৮টা - ১১টা)"),
    FULL_DAY("FULL_DAY", 4, "Full Day (Morning - Night)", "সারাদিন (সকাল - রাত)");

    companion object {
        fun fromKey(key: String): EmployeeTaskTimeSlot =
            entries.firstOrNull { it.key == key } ?: MORNING
    }
}

enum class EmployeeTaskStatus(val key: String, val labelEn: String, val labelBn: String) {
    PENDING("PENDING", "Pending", "বাকি আছে"),
    IN_PROGRESS("IN_PROGRESS", "In Progress", "চলমান"),
    COMPLETED("COMPLETED", "Completed", "সম্পন্ন");

    companion object {
        fun fromKey(key: String): EmployeeTaskStatus =
            entries.firstOrNull { it.key == key } ?: PENDING
    }
}

enum class AutoMessageType(val key: String, val labelEn: String, val labelBn: String) {
    MORNING_CHECKIN("MORNING_CHECKIN", "Morning Check-In", "সকালের হাজিরা ও আপডেট"),
    MIDDAY_UPDATE("MIDDAY_UPDATE", "Midday Progress", "দুপুরের কাজের আপডেট"),
    TASK_ASSIGNED("TASK_ASSIGNED", "Target Assigned", "নতুন টার্গেট দেওয়া হয়েছে"),
    TASK_COMPLETED("TASK_COMPLETED", "Target Completed", "টার্গেট সম্পন্ন"),
    NIGHT_REPORT("NIGHT_REPORT", "Night Closing Report", "রাতের সমাপনী রিপোর্ট"),
    CUSTOM("CUSTOM", "Quick Automated Update", "অটোমেটেড বার্তা");

    companion object {
        fun fromKey(key: String): AutoMessageType =
            entries.firstOrNull { it.key == key } ?: CUSTOM
    }
}

@Entity(
    tableName = "employee_task_targets",
    foreignKeys = [
        ForeignKey(entity = FarmEntity::class, parentColumns = ["id"], childColumns = ["farmId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = EmployeeEntity::class, parentColumns = ["id"], childColumns = ["employeeId"], onDelete = ForeignKey.CASCADE)
    ],
    indices = [
        Index("ownerUserId"),
        Index("farmId"),
        Index("employeeId"),
        Index(value = ["ownerUserId", "status", "createdAt"])
    ]
)
data class EmployeeTaskTargetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val ownerUserId: Long,
    val farmId: Long,
    val employeeId: Long,
    val employeeName: String,
    val timeSlot: String = EmployeeTaskTimeSlot.MORNING.key,
    val title: String,
    val description: String = "",
    val targetGoalText: String = "",
    val rewardBonusAmount: Double = 10.0,
    val status: String = EmployeeTaskStatus.PENDING.key,
    val rewardCredited: Boolean = false,
    val completionNote: String = "",
    val targetDateMillis: Long = System.currentTimeMillis(),
    val completedAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "employee_auto_messages",
    indices = [
        Index("ownerUserId"),
        Index("farmId"),
        Index("employeeId"),
        Index(value = ["ownerUserId", "sentAt"])
    ]
)
data class EmployeeAutoMessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val ownerUserId: Long,
    val farmId: Long = 0L,
    val employeeId: Long,
    val senderUserId: Long,
    val senderName: String,
    val senderRole: String,
    val recipientLabel: String,
    val messageType: String = AutoMessageType.CUSTOM.key,
    val subject: String,
    val messageBody: String,
    val isAutoGenerated: Boolean = true,
    val sentAt: Long = System.currentTimeMillis()
)


