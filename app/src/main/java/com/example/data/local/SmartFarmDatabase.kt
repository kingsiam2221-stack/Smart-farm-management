package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        UserEntity::class,
        FarmEntity::class,
        AnimalCategoryEntity::class,
        AnimalRecordEntity::class,
        AnimalEventEntity::class,
        EggProductionEntity::class,
        InventoryItemEntity::class,
        InventoryMovementEntity::class,
        CustomerEntity::class,
        SaleEntity::class,
        OrderEntity::class,
        ExpenseEntity::class,
        EmployeeEntity::class,
        SalaryPaymentEntity::class,
        SubscriptionPlanEntity::class,
        UserSubscriptionEntity::class,
        AppNotificationEntity::class,
        SupportRequestEntity::class,
        AdminAuditLogEntity::class,
        SystemSettingsEntity::class,
        EmployeePermissionEntity::class,
        CustomPermissionOptionEntity::class,
        AgriLandEntity::class,
        AgriCropEntity::class,
        AgriExpenseEntity::class,
        ProductAdEntity::class,
        EmployeeTaskTargetEntity::class,
        EmployeeAutoMessageEntity::class
    ],
    version = 11,
    exportSchema = false
)
abstract class SmartFarmDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun farmDao(): FarmDao
    abstract fun animalDao(): AnimalDao
    abstract fun eggDao(): EggDao
    abstract fun inventoryDao(): InventoryDao
    abstract fun commerceDao(): CommerceDao
    abstract fun operationsDao(): OperationsDao
    abstract fun subscriptionAndAdminDao(): SubscriptionAndAdminDao
    abstract fun permissionDao(): PermissionDao
    abstract fun agriDao(): AgriDao
    abstract fun productAdDao(): ProductAdDao

    companion object {
        @Volatile
        private var INSTANCE: SmartFarmDatabase? = null

        private fun hasColumn(db: SupportSQLiteDatabase, tableName: String, columnName: String): Boolean {
            db.query("PRAGMA table_info(`$tableName`)").use { cursor ->
                val nameIndex = cursor.getColumnIndex("name")
                while (cursor.moveToNext()) {
                    if (nameIndex >= 0 && cursor.getString(nameIndex).equals(columnName, ignoreCase = true)) {
                        return true
                    }
                }
            }
            return false
        }

        private fun addColumnIfMissing(
            db: SupportSQLiteDatabase,
            tableName: String,
            columnName: String,
            alterColumnDef: String
        ) {
            if (!hasColumn(db, tableName, columnName)) {
                db.execSQL("ALTER TABLE `$tableName` ADD COLUMN $alterColumnDef")
            }
        }

        private fun ensureV7AndV8SystemSettingsColumns(db: SupportSQLiteDatabase) {
            val defaultNote = "Contact our team by phone or WhatsApp anytime for app support, bug reports, or business setup."
            addColumnIfMissing(db, "system_settings", "developerName", "developerName TEXT NOT NULL DEFAULT 'Smart Farm Support Team'")
            addColumnIfMissing(db, "system_settings", "developerPhone", "developerPhone TEXT NOT NULL DEFAULT '+8801700000000'")
            addColumnIfMissing(db, "system_settings", "developerWhatsApp", "developerWhatsApp TEXT NOT NULL DEFAULT '+8801700000000'")
            addColumnIfMissing(db, "system_settings", "developerEmail", "developerEmail TEXT NOT NULL DEFAULT 'support@smartfarm.app'")
            addColumnIfMissing(db, "system_settings", "developerSupportNote", "developerSupportNote TEXT NOT NULL DEFAULT '$defaultNote'")
            addColumnIfMissing(db, "system_settings", "adminPinHash", "adminPinHash TEXT NOT NULL DEFAULT ''")
        }

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE users ADD COLUMN employerOwnerId INTEGER")
                db.execSQL("ALTER TABLE employees ADD COLUMN email TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE employees ADD COLUMN linkedUserId INTEGER")
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `employee_permissions` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `employeeId` INTEGER NOT NULL,
                        `ownerUserId` INTEGER NOT NULL,
                        `permissionKey` TEXT NOT NULL,
                        `granted` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL,
                        FOREIGN KEY(`employeeId`) REFERENCES `employees`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_employee_permissions_ownerUserId` ON `employee_permissions` (`ownerUserId`)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_employee_permissions_employeeId_permissionKey` ON `employee_permissions` (`employeeId`, `permissionKey`)")
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `custom_permission_options` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `ownerUserId` INTEGER NOT NULL,
                        `permissionKey` TEXT NOT NULL,
                        `titleEn` TEXT NOT NULL,
                        `titleBn` TEXT NOT NULL,
                        `descriptionEn` TEXT NOT NULL,
                        `descriptionBn` TEXT NOT NULL,
                        `createdAt` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_custom_permission_options_ownerUserId` ON `custom_permission_options` (`ownerUserId`)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_custom_permission_options_permissionKey` ON `custom_permission_options` (`permissionKey`)")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                val defaultCsv = SubscriptionFeature.DEFAULT_LOCKED_CSV
                val defaultInstructions = "Send plan fee to the Developer bKash, Nagad, or Bank Account shown and enter your TrxID / Reference."
                db.execSQL("ALTER TABLE system_settings ADD COLUMN subscriptionLockedFeaturesCsv TEXT NOT NULL DEFAULT '$defaultCsv'")
                db.execSQL("ALTER TABLE system_settings ADD COLUMN bkashEnabled INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE system_settings ADD COLUMN bkashNumber TEXT NOT NULL DEFAULT '01700-000000'")
                db.execSQL("ALTER TABLE system_settings ADD COLUMN bkashAccountType TEXT NOT NULL DEFAULT 'Merchant'")
                db.execSQL("ALTER TABLE system_settings ADD COLUMN nagadEnabled INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE system_settings ADD COLUMN nagadNumber TEXT NOT NULL DEFAULT '01800-000000'")
                db.execSQL("ALTER TABLE system_settings ADD COLUMN nagadAccountType TEXT NOT NULL DEFAULT 'Personal'")
                db.execSQL("ALTER TABLE system_settings ADD COLUMN bankEnabled INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE system_settings ADD COLUMN bankName TEXT NOT NULL DEFAULT 'Dutch-Bangla Bank (DBBL)'")
                db.execSQL("ALTER TABLE system_settings ADD COLUMN bankAccountName TEXT NOT NULL DEFAULT 'Smart Farm Management'")
                db.execSQL("ALTER TABLE system_settings ADD COLUMN bankAccountNumber TEXT NOT NULL DEFAULT '123-456-7890123'")
                db.execSQL("ALTER TABLE system_settings ADD COLUMN bankBranch TEXT NOT NULL DEFAULT 'Dhaka Main Branch'")
                db.execSQL("ALTER TABLE system_settings ADD COLUMN paymentInstructions TEXT NOT NULL DEFAULT '$defaultInstructions'")
                db.execSQL("ALTER TABLE system_settings ADD COLUMN requireAdminApprovalForAll INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE inventory_items ADD COLUMN sourcingType TEXT NOT NULL DEFAULT 'FARM_PRODUCED'")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_inventory_items_listInMarketplace_quantity_updatedAt` ON `inventory_items` (`listInMarketplace`, `quantity`, `updatedAt`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_inventory_items_category` ON `inventory_items` (`category`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_orders_farmOwnerUserId_status_createdAt` ON `orders` (`farmOwnerUserId`, `status`, `createdAt`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_notifications_targetUserId_readFlag_createdAt` ON `notifications` (`targetUserId`, `readFlag`, `createdAt`)")
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `agri_lands` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `ownerUserId` INTEGER NOT NULL,
                        `farmId` INTEGER,
                        `name` TEXT NOT NULL,
                        `areaSize` REAL NOT NULL,
                        `areaUnit` TEXT NOT NULL,
                        `location` TEXT NOT NULL,
                        `soilOrLandType` TEXT NOT NULL,
                        `notes` TEXT NOT NULL,
                        `createdAt` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_agri_lands_ownerUserId` ON `agri_lands` (`ownerUserId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_agri_lands_farmId` ON `agri_lands` (`farmId`)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `agri_crops` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `ownerUserId` INTEGER NOT NULL,
                        `farmId` INTEGER,
                        `landId` INTEGER NOT NULL,
                        `cropType` TEXT NOT NULL,
                        `title` TEXT NOT NULL,
                        `variety` TEXT NOT NULL,
                        `season` TEXT NOT NULL,
                        `plantingDate` INTEGER NOT NULL,
                        `status` TEXT NOT NULL,
                        `yieldQuantity` REAL NOT NULL,
                        `yieldUnit` TEXT NOT NULL,
                        `marketPricePerUnit` REAL NOT NULL,
                        `yieldMarketWorth` REAL NOT NULL,
                        `soldAmount` REAL NOT NULL,
                        `notes` TEXT NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL,
                        FOREIGN KEY(`landId`) REFERENCES `agri_lands`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_agri_crops_ownerUserId` ON `agri_crops` (`ownerUserId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_agri_crops_landId` ON `agri_crops` (`landId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_agri_crops_farmId` ON `agri_crops` (`farmId`)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `agri_expenses` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `ownerUserId` INTEGER NOT NULL,
                        `farmId` INTEGER,
                        `landId` INTEGER NOT NULL,
                        `cropId` INTEGER,
                        `sector` TEXT NOT NULL,
                        `customSectorName` TEXT NOT NULL,
                        `amount` REAL NOT NULL,
                        `description` TEXT NOT NULL,
                        `expenseDate` INTEGER NOT NULL,
                        FOREIGN KEY(`landId`) REFERENCES `agri_lands`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_agri_expenses_ownerUserId` ON `agri_expenses` (`ownerUserId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_agri_expenses_landId` ON `agri_expenses` (`landId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_agri_expenses_cropId` ON `agri_expenses` (`cropId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_agri_expenses_expenseDate` ON `agri_expenses` (`expenseDate`)")
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `product_ads` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `ownerUserId` INTEGER NOT NULL,
                        `farmId` INTEGER NOT NULL,
                        `inventoryItemId` INTEGER,
                        `productName` TEXT NOT NULL,
                        `farmName` TEXT NOT NULL,
                        `priceText` TEXT NOT NULL,
                        `imageUri` TEXT,
                        `hookHeadlineBn` TEXT NOT NULL,
                        `promoBadgeBn` TEXT NOT NULL,
                        `benefitsPipeSeparatedBn` TEXT NOT NULL,
                        `callToActionBn` TEXT NOT NULL,
                        `voiceoverScriptBn` TEXT NOT NULL,
                        `orderStepsPipeSeparatedBn` TEXT NOT NULL,
                        `accentColorHex` TEXT NOT NULL,
                        `isActiveInShop` INTEGER NOT NULL,
                        `createdAt` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_product_ads_ownerUserId` ON `product_ads` (`ownerUserId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_product_ads_farmId` ON `product_ads` (`farmId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_product_ads_inventoryItemId` ON `product_ads` (`inventoryItemId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_product_ads_isActiveInShop_createdAt` ON `product_ads` (`isActiveInShop`, `createdAt`)")
            }
        }

        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                ensureV7AndV8SystemSettingsColumns(db)
            }
        }

        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                ensureV7AndV8SystemSettingsColumns(db)
            }
        }

        val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                addColumnIfMissing(db, "farms", "bkashNumber", "bkashNumber TEXT NOT NULL DEFAULT ''")
                addColumnIfMissing(db, "farms", "nagadNumber", "nagadNumber TEXT NOT NULL DEFAULT ''")
                addColumnIfMissing(db, "farms", "bankAccountInfo", "bankAccountInfo TEXT NOT NULL DEFAULT ''")

                addColumnIfMissing(db, "inventory_items", "sellerBkashNumber", "sellerBkashNumber TEXT NOT NULL DEFAULT ''")
                addColumnIfMissing(db, "inventory_items", "sellerNagadNumber", "sellerNagadNumber TEXT NOT NULL DEFAULT ''")
                addColumnIfMissing(db, "inventory_items", "sellerBankAccount", "sellerBankAccount TEXT NOT NULL DEFAULT ''")

                addColumnIfMissing(db, "orders", "deliveryDistrict", "deliveryDistrict TEXT NOT NULL DEFAULT ''")
                addColumnIfMissing(db, "orders", "deliveryPoliceStation", "deliveryPoliceStation TEXT NOT NULL DEFAULT ''")
                addColumnIfMissing(db, "orders", "deliveryLandmark", "deliveryLandmark TEXT NOT NULL DEFAULT ''")
                addColumnIfMissing(db, "orders", "paymentReference", "paymentReference TEXT NOT NULL DEFAULT ''")

                addColumnIfMissing(db, "salary_payments", "paymentType", "paymentType TEXT NOT NULL DEFAULT 'SALARY'")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `employee_task_targets` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `ownerUserId` INTEGER NOT NULL,
                        `farmId` INTEGER NOT NULL,
                        `employeeId` INTEGER NOT NULL,
                        `employeeName` TEXT NOT NULL,
                        `timeSlot` TEXT NOT NULL,
                        `title` TEXT NOT NULL,
                        `description` TEXT NOT NULL,
                        `targetGoalText` TEXT NOT NULL,
                        `rewardBonusAmount` REAL NOT NULL,
                        `status` TEXT NOT NULL,
                        `rewardCredited` INTEGER NOT NULL,
                        `completionNote` TEXT NOT NULL,
                        `targetDateMillis` INTEGER NOT NULL,
                        `completedAt` INTEGER,
                        `createdAt` INTEGER NOT NULL,
                        FOREIGN KEY(`farmId`) REFERENCES `farms`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE,
                        FOREIGN KEY(`employeeId`) REFERENCES `employees`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_employee_task_targets_ownerUserId` ON `employee_task_targets` (`ownerUserId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_employee_task_targets_farmId` ON `employee_task_targets` (`farmId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_employee_task_targets_employeeId` ON `employee_task_targets` (`employeeId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_employee_task_targets_ownerUserId_status_createdAt` ON `employee_task_targets` (`ownerUserId`, `status`, `createdAt`)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `employee_auto_messages` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `ownerUserId` INTEGER NOT NULL,
                        `farmId` INTEGER NOT NULL,
                        `employeeId` INTEGER NOT NULL,
                        `senderUserId` INTEGER NOT NULL,
                        `senderName` TEXT NOT NULL,
                        `senderRole` TEXT NOT NULL,
                        `recipientLabel` TEXT NOT NULL,
                        `messageType` TEXT NOT NULL,
                        `subject` TEXT NOT NULL,
                        `messageBody` TEXT NOT NULL,
                        `isAutoGenerated` INTEGER NOT NULL,
                        `sentAt` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_employee_auto_messages_ownerUserId` ON `employee_auto_messages` (`ownerUserId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_employee_auto_messages_farmId` ON `employee_auto_messages` (`farmId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_employee_auto_messages_employeeId` ON `employee_auto_messages` (`employeeId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_employee_auto_messages_ownerUserId_sentAt` ON `employee_auto_messages` (`ownerUserId`, `sentAt`)")
            }
        }

        val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                addColumnIfMissing(db, "system_settings", "loginShowCustomerLogin", "loginShowCustomerLogin INTEGER NOT NULL DEFAULT 1")
                addColumnIfMissing(db, "system_settings", "loginShowCustomerDemo", "loginShowCustomerDemo INTEGER NOT NULL DEFAULT 1")
                addColumnIfMissing(db, "system_settings", "loginShowCustomerSignup", "loginShowCustomerSignup INTEGER NOT NULL DEFAULT 1")
                addColumnIfMissing(db, "system_settings", "loginShowFarmerLogin", "loginShowFarmerLogin INTEGER NOT NULL DEFAULT 1")
                addColumnIfMissing(db, "system_settings", "loginShowFarmerDemo", "loginShowFarmerDemo INTEGER NOT NULL DEFAULT 1")
                addColumnIfMissing(db, "system_settings", "loginShowFarmerSignup", "loginShowFarmerSignup INTEGER NOT NULL DEFAULT 1")
                addColumnIfMissing(db, "system_settings", "loginShowEmployeeLogin", "loginShowEmployeeLogin INTEGER NOT NULL DEFAULT 1")
                addColumnIfMissing(db, "system_settings", "loginShowEmployeeDemo", "loginShowEmployeeDemo INTEGER NOT NULL DEFAULT 1")
                addColumnIfMissing(db, "system_settings", "loginShowGoogleSignIn", "loginShowGoogleSignIn INTEGER NOT NULL DEFAULT 1")
                addColumnIfMissing(db, "system_settings", "loginShowForgotPassword", "loginShowForgotPassword INTEGER NOT NULL DEFAULT 1")
                addColumnIfMissing(db, "system_settings", "loginBannerTitle", "loginBannerTitle TEXT NOT NULL DEFAULT ''")
                addColumnIfMissing(db, "system_settings", "loginBannerSubtitle", "loginBannerSubtitle TEXT NOT NULL DEFAULT ''")
                addColumnIfMissing(db, "system_settings", "loginCustomerTitle", "loginCustomerTitle TEXT NOT NULL DEFAULT ''")
                addColumnIfMissing(db, "system_settings", "loginCustomerSubtitle", "loginCustomerSubtitle TEXT NOT NULL DEFAULT ''")
                addColumnIfMissing(db, "system_settings", "loginFarmerTitle", "loginFarmerTitle TEXT NOT NULL DEFAULT ''")
                addColumnIfMissing(db, "system_settings", "loginFarmerSubtitle", "loginFarmerSubtitle TEXT NOT NULL DEFAULT ''")
                addColumnIfMissing(db, "system_settings", "loginEmployeeTitle", "loginEmployeeTitle TEXT NOT NULL DEFAULT ''")
                addColumnIfMissing(db, "system_settings", "loginEmployeeSubtitle", "loginEmployeeSubtitle TEXT NOT NULL DEFAULT ''")
                addColumnIfMissing(db, "system_settings", "loginCustomNotice", "loginCustomNotice TEXT NOT NULL DEFAULT ''")
            }
        }

        val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(db: SupportSQLiteDatabase) {
                addColumnIfMissing(db, "system_settings", "orderShowBkash", "orderShowBkash INTEGER NOT NULL DEFAULT 1")
                addColumnIfMissing(db, "system_settings", "orderShowNagad", "orderShowNagad INTEGER NOT NULL DEFAULT 1")
                addColumnIfMissing(db, "system_settings", "orderShowBank", "orderShowBank INTEGER NOT NULL DEFAULT 1")
                addColumnIfMissing(db, "system_settings", "orderShowCod", "orderShowCod INTEGER NOT NULL DEFAULT 1")
                addColumnIfMissing(db, "system_settings", "orderOverrideSellerPayments", "orderOverrideSellerPayments INTEGER NOT NULL DEFAULT 0")
                addColumnIfMissing(db, "system_settings", "orderBkashNumber", "orderBkashNumber TEXT NOT NULL DEFAULT ''")
                addColumnIfMissing(db, "system_settings", "orderNagadNumber", "orderNagadNumber TEXT NOT NULL DEFAULT ''")
                addColumnIfMissing(db, "system_settings", "orderBankAccount", "orderBankAccount TEXT NOT NULL DEFAULT ''")
                addColumnIfMissing(db, "system_settings", "orderCustomNote", "orderCustomNote TEXT NOT NULL DEFAULT ''")
            }
        }

        fun getInstance(context: Context): SmartFarmDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    SmartFarmDatabase::class.java,
                    "smart_farm_management.db"
                )
                    .addMigrations(
                        MIGRATION_1_2,
                        MIGRATION_2_3,
                        MIGRATION_3_4,
                        MIGRATION_4_5,
                        MIGRATION_5_6,
                        MIGRATION_6_7,
                        MIGRATION_7_8,
                        MIGRATION_8_9,
                        MIGRATION_9_10,
                        MIGRATION_10_11
                    )
                    .fallbackToDestructiveMigration(true)
                    .fallbackToDestructiveMigrationOnDowngrade(true)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
