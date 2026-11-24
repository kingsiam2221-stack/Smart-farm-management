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
        CustomPermissionOptionEntity::class
    ],
    version = 3,
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

    companion object {
        @Volatile
        private var INSTANCE: SmartFarmDatabase? = null

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

        fun getInstance(context: Context): SmartFarmDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    SmartFarmDatabase::class.java,
                    "smart_farm_management.db"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .fallbackToDestructiveMigrationOnDowngrade(true)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
