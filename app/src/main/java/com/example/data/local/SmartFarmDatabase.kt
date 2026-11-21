package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

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
        SystemSettingsEntity::class
    ],
    version = 1,
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

    companion object {
        @Volatile
        private var INSTANCE: SmartFarmDatabase? = null

        fun getInstance(context: Context): SmartFarmDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    SmartFarmDatabase::class.java,
                    "smart_farm_management.db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
