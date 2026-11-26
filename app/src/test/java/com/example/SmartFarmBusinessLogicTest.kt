package com.example

import android.content.Context
import android.content.Intent
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.ai.AdVideoMp4Exporter
import com.example.ai.BengaliAdStoryboard
import com.example.ai.SharePlatform
import com.example.data.local.*
import com.example.data.repository.FarmRepository
import com.example.i18n.CurrencyFormatter
import com.example.i18n.CurrencyOption
import java.io.File
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class SmartFarmBusinessLogicTest {
    private lateinit var db: SmartFarmDatabase
    private lateinit var repo: FarmRepository

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, SmartFarmDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repo = FarmRepository(db)
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun seededDatabase_supportsAuthEggCalculationInventoryDeductionAndRbac() = runTest {
        repo.initializeDatabase()

        // 1. Verify authentication & RBAC roles
        val farmer = repo.login("farmer@smartfarm.app", "farmer123").getOrThrow()
        assertEquals(UserRole.FARMER.name, farmer.role)

        val admin = repo.login("admin@smartfarm.app", "admin@2026").getOrThrow()
        assertEquals(UserRole.SUPER_ADMIN.name, admin.role)

        // Farmer must be blocked from Super Admin operations
        val deniedResult = repo.adminBroadcastNotification(farmer.id, "Hack", "Unauthorized")
        assertTrue(deniedResult.isFailure)

        // 2. Verify Egg Production formula: Available = Prev + New - Sold - Broken
        val farms = repo.observeFarms(farmer.id).first()
        assertTrue(farms.size >= 3)
        val farm1 = farms.first()
        repo.recordEggProduction(
            farmId = farm1.id,
            ownerId = farmer.id,
            eggCategory = "Turkey Eggs",
            previousStockOverride = 50,
            producedCount = 40,
            brokenCount = 3,
            soldCount = 20,
            unitSellingPrice = 25.0
        )
        val eggs = repo.observeEggs(farmer.id).first().first { it.eggCategory == "Turkey Eggs" }
        assertEquals(67, eggs.availableCount) // 50 + 40 - 20 - 3 = 67
        assertEquals(1000.0, eggs.totalValue, 0.01)

        // 3. Verify Sale automatically deducts Inventory stock
        val invItem = repo.observeInventory(farmer.id).first().first { it.quantity > 10.0 }
        val initialQty = invItem.quantity
        repo.createSale(
            farmId = invItem.farmId,
            ownerId = farmer.id,
            customerId = null,
            customerName = "Test Buyer",
            customerPhone = "01700000000",
            productCategory = invItem.category,
            productName = invItem.name,
            inventoryItem = invItem,
            animalRecord = null,
            quantity = 4.0,
            unitPrice = 300.0,
            discount = 100.0,
            paidAmount = 900.0,
            paymentMethod = "bKash"
        )
        val updatedItem = db.inventoryDao().getItemById(invItem.id)!!
        assertEquals(initialQty - 4.0, updatedItem.quantity, 0.01)

        // 4. Verify Multi-Currency conversion round-trip
        val usdVal = CurrencyFormatter.convertFromBase(1000.0, CurrencyOption.USD)
        assertTrue(usdVal > 0.0 && usdVal < 1000.0)
        val formatted = CurrencyFormatter.format(100.0, CurrencyOption.BDT)
        assertTrue(formatted.startsWith("৳"))
    }

    @Test
    fun farmCreation_worksForNewFarmerAndPreventsFarmCodeCollision() = runTest {
        repo.initializeDatabase()

        val newFarmer = repo.register(
            name = "Kamal Hossain",
            mobile = "+8801799887766",
            email = "kamal@smartfarm.app",
            password = "pass1234",
            role = UserRole.FARMER
        ).getOrThrow()

        // Blank name should fail cleanly
        val blankResult = repo.saveFarm(
            FarmEntity(
                ownerUserId = newFarmer.id,
                farmCode = "FM-1001",
                name = "   ",
                location = "Rajshahi",
                address = "Ward 4"
            )
        )
        assertTrue(blankResult.isFailure)

        // Creating a farm with a colliding code ("FM-1001" already owned by seeded farmer) must NOT overwrite or fail!
        val created1Id = repo.saveFarm(
            FarmEntity(
                ownerUserId = newFarmer.id,
                farmCode = "FM-1001",
                name = "Kamal Duck Hatchery",
                location = "Natore",
                address = "Main Road"
            )
        ).getOrThrow()
        assertTrue(created1Id > 0L)

        // Ensure seeded farmer's FM-1001 still exists untouched
        val seededFarm = db.farmDao().getFarmByCode("FM-1001")
        assertNotNull(seededFarm)
        assertEquals("Green Valley Poultry", seededFarm!!.name)

        // Ensure new farmer's farm received a distinct unique farmCode
        val createdFarm = db.farmDao().getFarmById(created1Id)!!
        assertNotEquals("FM-1001", createdFarm.farmCode)
        assertEquals("Kamal Duck Hatchery", createdFarm.name)

        // Updating a farm to use another farm's code must return a clean failure instead of crashing
        val conflictUpdate = repo.saveFarm(createdFarm.copy(farmCode = "FM-1001"))
        assertTrue(conflictUpdate.isFailure)
    }

    @Test
    fun farmGuideContent_containsAllTopicsSummaryAndBilingualEntries() {
        val guideTopics = com.example.data.guide.GuideTopic.entries
        assertEquals(6, guideTopics.size)
        assertEquals(5, com.example.data.guide.FarmGuideContent.summaryCards.size)
        assertTrue(com.example.data.guide.FarmGuideContent.duckHatchingSteps.size >= 4)
        assertTrue(com.example.data.guide.FarmGuideContent.raiseDucksSteps.size >= 4)
        assertTrue(com.example.data.guide.FarmGuideContent.raiseChickensSteps.size >= 4)

        val vaccines = com.example.data.guide.FarmGuideContent.vaccineSchedules
        assertTrue(vaccines.any { it.species == com.example.data.guide.GuideSpecies.DUCK })
        assertTrue(vaccines.any { it.species == com.example.data.guide.GuideSpecies.CHICKEN })
        vaccines.forEach { entry ->
            assertTrue(entry.vaccineName.en.isNotBlank() && entry.vaccineName.bn.isNotBlank())
            assertTrue(entry.ageTiming.en.isNotBlank() && entry.ageTiming.bn.isNotBlank())
        }

        val meds = com.example.data.guide.FarmGuideContent.medicineSchedules
        assertTrue(meds.size >= 6)
        meds.forEach { entry ->
            assertTrue(entry.medicineOrSupplement.en.isNotBlank() && entry.medicineOrSupplement.bn.isNotBlank())
            assertTrue(entry.timingOrCondition.en.isNotBlank() && entry.timingOrCondition.bn.isNotBlank())
        }
    }

    @Test
    fun themeMode_supportsDefaultWhiteAndDarkTransitions() = runTest {
        assertEquals(com.example.ui.theme.AppThemeMode.DEFAULT, com.example.ui.theme.AppThemeMode.fromOverride(null))
        assertEquals(com.example.ui.theme.AppThemeMode.WHITE, com.example.ui.theme.AppThemeMode.fromOverride(false))
        assertEquals(com.example.ui.theme.AppThemeMode.DARK, com.example.ui.theme.AppThemeMode.fromOverride(true))

        // Cycle: Default (null) -> White (false) -> Dark (true) -> Default (null)
        val step1 = com.example.ui.components.nextThemeModeOverride(null)
        assertEquals(false, step1)
        val step2 = com.example.ui.components.nextThemeModeOverride(step1)
        assertEquals(true, step2)
        val step3 = com.example.ui.components.nextThemeModeOverride(step2)
        assertNull(step3)

        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefsRepo = UserPreferencesRepository(context)
        prefsRepo.setDarkMode(false)
        assertEquals(false, prefsRepo.preferences.first().darkModeOverride)
        prefsRepo.setDarkMode(true)
        assertEquals(true, prefsRepo.preferences.first().darkModeOverride)
        prefsRepo.setDarkMode(null)
        assertNull(prefsRepo.preferences.first().darkModeOverride)
    }

    private fun createLegacyV1Schema(db: androidx.sqlite.db.SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `users` (
                                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                                `name` TEXT NOT NULL,
                                `mobile` TEXT NOT NULL,
                                `email` TEXT NOT NULL,
                                `passwordHash` TEXT NOT NULL,
                                `role` TEXT NOT NULL,
                                `status` TEXT NOT NULL,
                                `profileImageUri` TEXT,
                                `preferredLanguage` TEXT NOT NULL,
                                `preferredCurrency` TEXT NOT NULL,
                                `createdAt` INTEGER NOT NULL
                            )
                            """.trimIndent()
                        )
                        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_users_email` ON `users` (`email`)")
                        db.execSQL(
                            """
                            CREATE TABLE IF NOT EXISTS `farms` (
                                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                                `ownerUserId` INTEGER NOT NULL,
                                `farmCode` TEXT NOT NULL,
                                `name` TEXT NOT NULL,
                                `location` TEXT NOT NULL,
                                `address` TEXT NOT NULL,
                                `category` TEXT NOT NULL,
                                `customCategoryName` TEXT NOT NULL,
                                `imageUri` TEXT,
                                `openingDate` INTEGER NOT NULL,
                                `managerName` TEXT NOT NULL,
                                `managerPhone` TEXT NOT NULL,
                                `initialInvestment` REAL NOT NULL,
                                `createdAt` INTEGER NOT NULL,
                                FOREIGN KEY(`ownerUserId`) REFERENCES `users`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                            )
                            """.trimIndent()
                        )
                        db.execSQL("CREATE INDEX IF NOT EXISTS `index_farms_ownerUserId` ON `farms` (`ownerUserId`)")
                        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_farms_farmCode` ON `farms` (`farmCode`)")
                        db.execSQL(
                            """
                            CREATE TABLE IF NOT EXISTS `animal_categories` (
                                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                                `ownerUserId` INTEGER NOT NULL,
                                `name` TEXT NOT NULL,
                                `birdCategory` INTEGER NOT NULL,
                                `builtIn` INTEGER NOT NULL
                            )
                            """.trimIndent()
                        )
                        db.execSQL("CREATE INDEX IF NOT EXISTS `index_animal_categories_ownerUserId` ON `animal_categories` (`ownerUserId`)")
                        db.execSQL(
                            """
                            CREATE TABLE IF NOT EXISTS `animal_records` (
                                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                                `farmId` INTEGER NOT NULL,
                                `ownerUserId` INTEGER NOT NULL,
                                `name` TEXT NOT NULL,
                                `category` TEXT NOT NULL,
                                `birdCategory` INTEGER NOT NULL,
                                `tagNumber` TEXT NOT NULL,
                                `breed` TEXT NOT NULL,
                                `gender` TEXT NOT NULL,
                                `dobOrAge` TEXT NOT NULL,
                                `purchaseDate` INTEGER NOT NULL,
                                `purchasePrice` REAL NOT NULL,
                                `quantity` INTEGER NOT NULL,
                                `healthStatus` TEXT NOT NULL,
                                `notes` TEXT NOT NULL,
                                `photoUri` TEXT,
                                `createdAt` INTEGER NOT NULL,
                                FOREIGN KEY(`farmId`) REFERENCES `farms`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                            )
                            """.trimIndent()
                        )
                        db.execSQL("CREATE INDEX IF NOT EXISTS `index_animal_records_farmId` ON `animal_records` (`farmId`)")
                        db.execSQL("CREATE INDEX IF NOT EXISTS `index_animal_records_ownerUserId` ON `animal_records` (`ownerUserId`)")
                        db.execSQL(
                            """
                            CREATE TABLE IF NOT EXISTS `animal_events` (
                                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                                `animalRecordId` INTEGER NOT NULL,
                                `farmId` INTEGER NOT NULL,
                                `ownerUserId` INTEGER NOT NULL,
                                `eventType` TEXT NOT NULL,
                                `quantityDelta` INTEGER NOT NULL,
                                `costOrValue` REAL NOT NULL,
                                `description` TEXT NOT NULL,
                                `eventDate` INTEGER NOT NULL,
                                FOREIGN KEY(`animalRecordId`) REFERENCES `animal_records`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                            )
                            """.trimIndent()
                        )
                        db.execSQL("CREATE INDEX IF NOT EXISTS `index_animal_events_animalRecordId` ON `animal_events` (`animalRecordId`)")
                        db.execSQL("CREATE INDEX IF NOT EXISTS `index_animal_events_farmId` ON `animal_events` (`farmId`)")
                        db.execSQL("CREATE INDEX IF NOT EXISTS `index_animal_events_ownerUserId` ON `animal_events` (`ownerUserId`)")
                        db.execSQL(
                            """
                            CREATE TABLE IF NOT EXISTS `egg_production` (
                                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                                `farmId` INTEGER NOT NULL,
                                `ownerUserId` INTEGER NOT NULL,
                                `eggCategory` TEXT NOT NULL,
                                `productionDate` INTEGER NOT NULL,
                                `previousStock` INTEGER NOT NULL,
                                `producedCount` INTEGER NOT NULL,
                                `brokenCount` INTEGER NOT NULL,
                                `soldCount` INTEGER NOT NULL,
                                `availableCount` INTEGER NOT NULL,
                                `unitSellingPrice` REAL NOT NULL,
                                `totalValue` REAL NOT NULL,
                                FOREIGN KEY(`farmId`) REFERENCES `farms`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                            )
                            """.trimIndent()
                        )
                        db.execSQL("CREATE INDEX IF NOT EXISTS `index_egg_production_farmId` ON `egg_production` (`farmId`)")
                        db.execSQL("CREATE INDEX IF NOT EXISTS `index_egg_production_ownerUserId` ON `egg_production` (`ownerUserId`)")
                        db.execSQL("CREATE INDEX IF NOT EXISTS `index_egg_production_productionDate` ON `egg_production` (`productionDate`)")
                        db.execSQL(
                            """
                            CREATE TABLE IF NOT EXISTS `inventory_items` (
                                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                                `farmId` INTEGER NOT NULL,
                                `ownerUserId` INTEGER NOT NULL,
                                `name` TEXT NOT NULL,
                                `category` TEXT NOT NULL,
                                `quantity` REAL NOT NULL,
                                `unit` TEXT NOT NULL,
                                `unitCost` REAL NOT NULL,
                                `sellingPrice` REAL NOT NULL,
                                `lowStockThreshold` REAL NOT NULL,
                                `expiryDate` INTEGER,
                                `supplierName` TEXT NOT NULL,
                                `supplierPhone` TEXT NOT NULL,
                                `listInMarketplace` INTEGER NOT NULL,
                                `description` TEXT NOT NULL,
                                `imageUri` TEXT,
                                `updatedAt` INTEGER NOT NULL,
                                FOREIGN KEY(`farmId`) REFERENCES `farms`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                            )
                            """.trimIndent()
                        )
                        db.execSQL("CREATE INDEX IF NOT EXISTS `index_inventory_items_farmId` ON `inventory_items` (`farmId`)")
                        db.execSQL("CREATE INDEX IF NOT EXISTS `index_inventory_items_ownerUserId` ON `inventory_items` (`ownerUserId`)")
                        db.execSQL(
                            """
                            CREATE TABLE IF NOT EXISTS `inventory_movements` (
                                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                                `inventoryItemId` INTEGER NOT NULL,
                                `farmId` INTEGER NOT NULL,
                                `ownerUserId` INTEGER NOT NULL,
                                `movementType` TEXT NOT NULL,
                                `quantityDelta` REAL NOT NULL,
                                `unitCost` REAL NOT NULL,
                                `supplierName` TEXT NOT NULL,
                                `note` TEXT NOT NULL,
                                `date` INTEGER NOT NULL,
                                FOREIGN KEY(`inventoryItemId`) REFERENCES `inventory_items`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                            )
                            """.trimIndent()
                        )
                        db.execSQL("CREATE INDEX IF NOT EXISTS `index_inventory_movements_inventoryItemId` ON `inventory_movements` (`inventoryItemId`)")
                        db.execSQL("CREATE INDEX IF NOT EXISTS `index_inventory_movements_farmId` ON `inventory_movements` (`farmId`)")
                        db.execSQL(
                            """
                            CREATE TABLE IF NOT EXISTS `customers` (
                                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                                `ownerUserId` INTEGER NOT NULL,
                                `customerCode` TEXT NOT NULL,
                                `name` TEXT NOT NULL,
                                `mobile` TEXT NOT NULL,
                                `email` TEXT NOT NULL,
                                `address` TEXT NOT NULL,
                                `photoUri` TEXT,
                                `createdAt` INTEGER NOT NULL
                            )
                            """.trimIndent()
                        )
                        db.execSQL("CREATE INDEX IF NOT EXISTS `index_customers_ownerUserId` ON `customers` (`ownerUserId`)")
                        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_customers_customerCode` ON `customers` (`customerCode`)")
                        db.execSQL(
                            """
                            CREATE TABLE IF NOT EXISTS `sales` (
                                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                                `invoiceNumber` TEXT NOT NULL,
                                `farmId` INTEGER NOT NULL,
                                `ownerUserId` INTEGER NOT NULL,
                                `customerId` INTEGER,
                                `customerName` TEXT NOT NULL,
                                `customerPhone` TEXT NOT NULL,
                                `productCategory` TEXT NOT NULL,
                                `productName` TEXT NOT NULL,
                                `inventoryItemId` INTEGER,
                                `animalRecordId` INTEGER,
                                `quantity` REAL NOT NULL,
                                `unitPrice` REAL NOT NULL,
                                `discount` REAL NOT NULL,
                                `totalAmount` REAL NOT NULL,
                                `paidAmount` REAL NOT NULL,
                                `dueAmount` REAL NOT NULL,
                                `paymentMethod` TEXT NOT NULL,
                                `saleDate` INTEGER NOT NULL,
                                FOREIGN KEY(`farmId`) REFERENCES `farms`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                            )
                            """.trimIndent()
                        )
                        db.execSQL("CREATE INDEX IF NOT EXISTS `index_sales_farmId` ON `sales` (`farmId`)")
                        db.execSQL("CREATE INDEX IF NOT EXISTS `index_sales_ownerUserId` ON `sales` (`ownerUserId`)")
                        db.execSQL("CREATE INDEX IF NOT EXISTS `index_sales_customerId` ON `sales` (`customerId`)")
                        db.execSQL("CREATE INDEX IF NOT EXISTS `index_sales_saleDate` ON `sales` (`saleDate`)")
                        db.execSQL(
                            """
                            CREATE TABLE IF NOT EXISTS `orders` (
                                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                                `orderNumber` TEXT NOT NULL,
                                `customerUserId` INTEGER NOT NULL,
                                `customerName` TEXT NOT NULL,
                                `customerPhone` TEXT NOT NULL,
                                `deliveryAddress` TEXT NOT NULL,
                                `farmId` INTEGER NOT NULL,
                                `farmOwnerUserId` INTEGER NOT NULL,
                                `farmName` TEXT NOT NULL,
                                `itemsSummary` TEXT NOT NULL,
                                `serializedItems` TEXT NOT NULL,
                                `totalAmount` REAL NOT NULL,
                                `paymentMethod` TEXT NOT NULL,
                                `status` TEXT NOT NULL,
                                `cancellationRequested` INTEGER NOT NULL,
                                `createdAt` INTEGER NOT NULL
                            )
                            """.trimIndent()
                        )
                        db.execSQL("CREATE INDEX IF NOT EXISTS `index_orders_customerUserId` ON `orders` (`customerUserId`)")
                        db.execSQL("CREATE INDEX IF NOT EXISTS `index_orders_farmId` ON `orders` (`farmId`)")
                        db.execSQL("CREATE INDEX IF NOT EXISTS `index_orders_farmOwnerUserId` ON `orders` (`farmOwnerUserId`)")
                        db.execSQL(
                            """
                            CREATE TABLE IF NOT EXISTS `expenses` (
                                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                                `farmId` INTEGER NOT NULL,
                                `ownerUserId` INTEGER NOT NULL,
                                `category` TEXT NOT NULL,
                                `amount` REAL NOT NULL,
                                `description` TEXT NOT NULL,
                                `expenseDate` INTEGER NOT NULL,
                                FOREIGN KEY(`farmId`) REFERENCES `farms`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                            )
                            """.trimIndent()
                        )
                        db.execSQL("CREATE INDEX IF NOT EXISTS `index_expenses_farmId` ON `expenses` (`farmId`)")
                        db.execSQL("CREATE INDEX IF NOT EXISTS `index_expenses_ownerUserId` ON `expenses` (`ownerUserId`)")
                        db.execSQL("CREATE INDEX IF NOT EXISTS `index_expenses_expenseDate` ON `expenses` (`expenseDate`)")
                        db.execSQL(
                            """
                            CREATE TABLE IF NOT EXISTS `employees` (
                                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                                `farmId` INTEGER NOT NULL,
                                `ownerUserId` INTEGER NOT NULL,
                                `name` TEXT NOT NULL,
                                `phone` TEXT NOT NULL,
                                `designation` TEXT NOT NULL,
                                `salary` REAL NOT NULL,
                                `joiningDate` INTEGER NOT NULL,
                                `canManageInventory` INTEGER NOT NULL,
                                `canManageSales` INTEGER NOT NULL,
                                `canManageLivestock` INTEGER NOT NULL,
                                `lastAttendanceStatus` TEXT NOT NULL,
                                `lastAttendanceDate` INTEGER NOT NULL,
                                FOREIGN KEY(`farmId`) REFERENCES `farms`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                            )
                            """.trimIndent()
                        )
                        db.execSQL("CREATE INDEX IF NOT EXISTS `index_employees_farmId` ON `employees` (`farmId`)")
                        db.execSQL("CREATE INDEX IF NOT EXISTS `index_employees_ownerUserId` ON `employees` (`ownerUserId`)")
                        db.execSQL(
                            """
                            CREATE TABLE IF NOT EXISTS `salary_payments` (
                                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                                `employeeId` INTEGER NOT NULL,
                                `farmId` INTEGER NOT NULL,
                                `ownerUserId` INTEGER NOT NULL,
                                `amount` REAL NOT NULL,
                                `monthLabel` TEXT NOT NULL,
                                `paymentDate` INTEGER NOT NULL,
                                `note` TEXT NOT NULL,
                                FOREIGN KEY(`employeeId`) REFERENCES `employees`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                            )
                            """.trimIndent()
                        )
                        db.execSQL("CREATE INDEX IF NOT EXISTS `index_salary_payments_employeeId` ON `salary_payments` (`employeeId`)")
                        db.execSQL("CREATE INDEX IF NOT EXISTS `index_salary_payments_farmId` ON `salary_payments` (`farmId`)")
                        db.execSQL(
                            """
                            CREATE TABLE IF NOT EXISTS `subscription_plans` (
                                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                                `name` TEXT NOT NULL,
                                `priceBaseBdt` REAL NOT NULL,
                                `durationDays` INTEGER NOT NULL,
                                `description` TEXT NOT NULL,
                                `featuresSummary` TEXT NOT NULL,
                                `active` INTEGER NOT NULL
                            )
                            """.trimIndent()
                        )
                        db.execSQL(
                            """
                            CREATE TABLE IF NOT EXISTS `user_subscriptions` (
                                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                                `userId` INTEGER NOT NULL,
                                `userName` TEXT NOT NULL,
                                `planId` INTEGER NOT NULL,
                                `planName` TEXT NOT NULL,
                                `pricePaidBdt` REAL NOT NULL,
                                `currencyCode` TEXT NOT NULL,
                                `paymentMethod` TEXT NOT NULL,
                                `transactionRef` TEXT NOT NULL,
                                `paymentStatus` TEXT NOT NULL,
                                `startDate` INTEGER NOT NULL,
                                `expiryDate` INTEGER NOT NULL,
                                `status` TEXT NOT NULL
                            )
                            """.trimIndent()
                        )
                        db.execSQL("CREATE INDEX IF NOT EXISTS `index_user_subscriptions_userId` ON `user_subscriptions` (`userId`)")
                        db.execSQL("CREATE INDEX IF NOT EXISTS `index_user_subscriptions_planId` ON `user_subscriptions` (`planId`)")
                        db.execSQL(
                            """
                            CREATE TABLE IF NOT EXISTS `notifications` (
                                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                                `targetUserId` INTEGER NOT NULL,
                                `title` TEXT NOT NULL,
                                `message` TEXT NOT NULL,
                                `category` TEXT NOT NULL,
                                `readFlag` INTEGER NOT NULL,
                                `createdAt` INTEGER NOT NULL
                            )
                            """.trimIndent()
                        )
                        db.execSQL("CREATE INDEX IF NOT EXISTS `index_notifications_targetUserId` ON `notifications` (`targetUserId`)")
                        db.execSQL(
                            """
                            CREATE TABLE IF NOT EXISTS `support_requests` (
                                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                                `userId` INTEGER NOT NULL,
                                `userName` TEXT NOT NULL,
                                `subject` TEXT NOT NULL,
                                `message` TEXT NOT NULL,
                                `status` TEXT NOT NULL,
                                `adminReply` TEXT NOT NULL,
                                `createdAt` INTEGER NOT NULL
                            )
                            """.trimIndent()
                        )
                        db.execSQL("CREATE INDEX IF NOT EXISTS `index_support_requests_userId` ON `support_requests` (`userId`)")
                        db.execSQL(
                            """
                            CREATE TABLE IF NOT EXISTS `admin_audit_logs` (
                                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                                `actorUserId` INTEGER NOT NULL,
                                `actorName` TEXT NOT NULL,
                                `action` TEXT NOT NULL,
                                `target` TEXT NOT NULL,
                                `details` TEXT NOT NULL,
                                `createdAt` INTEGER NOT NULL
                            )
                            """.trimIndent()
                        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `system_settings` (
                `id` INTEGER PRIMARY KEY NOT NULL,
                `announcementBanner` TEXT NOT NULL,
                `maintenanceMode` INTEGER NOT NULL,
                `defaultCurrencyCode` TEXT NOT NULL,
                `allowManualPayments` INTEGER NOT NULL
            )
            """.trimIndent()
        )
    }

    @Test
    fun roomMigration_v1ToV2_preservesExistingUsersAndEmployeesAndBackfillsPermissions() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val dbName = "migration_v1_v2_test_${System.currentTimeMillis()}.db"
        context.deleteDatabase(dbName)

        // Create genuine SQLite schema at version 1 matching pre-migration columns
        val openHelper = androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory().create(
            androidx.sqlite.db.SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(dbName)
                .callback(object : androidx.sqlite.db.SupportSQLiteOpenHelper.Callback(1) {
                    override fun onCreate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                        createLegacyV1Schema(db)

                        // Legacy v1 data
                        db.execSQL("INSERT INTO users (id, name, mobile, email, passwordHash, role, status, profileImageUri, preferredLanguage, preferredCurrency, createdAt) VALUES (1, 'Legacy Farmer', '01711111111', 'farmer@smartfarm.app', '${PasswordHasher.hash("farmer123")}', 'FARMER', 'ACTIVE', NULL, 'bn', 'BDT', 1000)")
                        db.execSQL("INSERT INTO farms (id, ownerUserId, farmCode, name, location, address, category, customCategoryName, imageUri, openingDate, managerName, managerPhone, initialInvestment, createdAt) VALUES (1, 1, 'FM-9001', 'Legacy Farm', 'Dhaka', 'Road 1', 'POULTRY', '', NULL, 1000, 'Karim Mia', '01722334455', 10000.0, 1000)")
                        db.execSQL("INSERT INTO employees (id, farmId, ownerUserId, name, phone, designation, salary, joiningDate, canManageInventory, canManageSales, canManageLivestock, lastAttendanceStatus, lastAttendanceDate) VALUES (1, 1, 1, 'Karim Mia', '01722334455', 'Manager', 12000.0, 1000, 1, 0, 1, 'PRESENT', 1000)")
                    }

                    override fun onUpgrade(db: androidx.sqlite.db.SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
                })
                .build()
        )
        openHelper.writableDatabase.close()

        // Open with Room v4 + MIGRATION_1_2, MIGRATION_2_3 & MIGRATION_3_4 (Room validates exact schema parity!)
        val migratedDb = Room.databaseBuilder(context, SmartFarmDatabase::class.java, dbName)
            .addMigrations(
                SmartFarmDatabase.MIGRATION_1_2,
                SmartFarmDatabase.MIGRATION_2_3,
                SmartFarmDatabase.MIGRATION_3_4,
                SmartFarmDatabase.MIGRATION_4_5,
                SmartFarmDatabase.MIGRATION_5_6
            )
            .allowMainThreadQueries()
            .build()
        try {
            val migratedRepo = FarmRepository(migratedDb)
            migratedRepo.initializeDatabase()

            val karim = migratedRepo.login("karim@smartfarm.app", "employee123").getOrThrow()
            assertEquals(UserRole.EMPLOYEE.name, karim.role)
            assertEquals(1L, karim.employerOwnerId)

            val perms = migratedDb.permissionDao().getPermissionsForEmployee(1L)
            assertTrue(perms.isNotEmpty())
            assertTrue(perms.first { it.permissionKey == AppPermission.INVENTORY_MANAGE.key }.granted)
            assertFalse(perms.first { it.permissionKey == AppPermission.SALES_CREATE_COLLECT.key }.granted)
        } finally {
            migratedDb.close()
            context.deleteDatabase(dbName)
        }
    }

    @Test
    fun subscriptionSwitchAndCautionAlerts_workEndToEndWithoutCrash() = runTest {
        repo.initializeDatabase()
        val farmer = repo.login("farmer@smartfarm.app", "farmer123").getOrThrow()
        val plans = repo.observePlans().first()
        assertTrue(plans.size >= 2)

        val planA = plans.first()
        val planB = plans.last()

        val sub1 = repo.purchaseSubscription(farmer, planA, "BDT", "bKash", "TXN-1001", actorUserId = farmer.id).getOrThrow()
        assertEquals("ACTIVE", sub1.status)

        val sub2 = repo.purchaseSubscription(farmer, planB, "BDT", "Nagad", "TXN-1002", actorUserId = farmer.id).getOrThrow()
        assertEquals("ACTIVE", sub2.status)

        val allFarmerSubs = repo.observeUserSubscriptions(farmer.id).first()
        val activeCount = allFarmerSubs.count { it.status == "ACTIVE" }
        assertEquals(1, activeCount)
        assertEquals(planB.id, allFarmerSubs.first { it.status == "ACTIVE" }.planId)

        // Caution/Alerts mark all read + support ticket submission
        repo.markNotificationsRead(farmer.id).getOrThrow()
        val unreadAfter = repo.observeNotifications(farmer.id).first().count { !it.readFlag }
        assertEquals(0, unreadAfter)

        assertTrue(repo.submitSupportRequest(farmer, "", "msg").isFailure)
        repo.submitSupportRequest(farmer, "Caution Alert Check", "Testing caution & support").getOrThrow()
        assertTrue(repo.observeUserSupport(farmer.id).first().any { it.subject == "Caution Alert Check" })
    }

    @Test
    fun granularPermissions_55EmployeesScaleAndCustomDynamicOptions() = runTest {
        repo.initializeDatabase()
        val farmer = repo.login("farmer@smartfarm.app", "farmer123").getOrThrow()
        val farm1 = repo.observeFarms(farmer.id).first().first()

        // 1. Seeded employee Karim can login and only execute permitted actions
        val karimUser = repo.login("karim@smartfarm.app", "employee123").getOrThrow()
        assertEquals(UserRole.EMPLOYEE.name, karimUser.role)
        val karimEmp = repo.observeEmployees(farmer.id).first().first { it.linkedUserId == karimUser.id }

        // Employee MUST NOT be able to modify permissions (Admin-only guard)
        val employeeSelfGrant = repo.setEmployeePermission(karimUser.id, karimEmp, AppPermission.FARMS_CREATE_EDIT.key, true)
        assertTrue(employeeSelfGrant.isFailure)
        assertTrue(employeeSelfGrant.exceptionOrNull() is SecurityException)

        // Revoke EGGS_RECORD from Karim -> attempting recordEggProduction fails with SecurityException
        repo.setEmployeePermission(farmer.id, karimEmp, AppPermission.EGGS_RECORD.key, false).getOrThrow()
        val blockedEggs = runCatching {
            repo.recordEggProduction(
                farmId = farm1.id,
                ownerId = farmer.id,
                eggCategory = "Duck Eggs",
                previousStockOverride = 10,
                producedCount = 5,
                brokenCount = 0,
                soldCount = 0,
                unitSellingPrice = 15.0,
                actorUserId = karimUser.id
            )
        }
        assertTrue(blockedEggs.isFailure)
        assertTrue(blockedEggs.exceptionOrNull() is SecurityException)

        // Grant EGGS_RECORD back to Karim -> succeeds!
        repo.setEmployeePermission(farmer.id, karimEmp, AppPermission.EGGS_RECORD.key, true).getOrThrow()
        repo.recordEggProduction(
            farmId = farm1.id,
            ownerId = farmer.id,
            eggCategory = "Duck Eggs",
            previousStockOverride = 10,
            producedCount = 5,
            brokenCount = 0,
            soldCount = 0,
            unitSellingPrice = 15.0,
            actorUserId = karimUser.id
        )

        // 2. Scale test: Create 55 employees + dynamic Custom Permission Option
        val createdEmployees = mutableListOf<EmployeeEntity>()
        for (i in 1..55) {
            val empId = repo.saveEmployee(
                EmployeeEntity(
                    farmId = farm1.id,
                    ownerUserId = farmer.id,
                    name = "Staff Worker $i",
                    phone = "01700000${i.toString().padStart(3, '0')}",
                    designation = "Poultry Attendant",
                    salary = 8000.0
                ),
                actorUserId = farmer.id
            )
            createdEmployees += db.operationsDao().getEmployeeById(empId)!!
        }
        assertEquals(55, createdEmployees.size)

        // Add new dynamic option -> initially NOT granted to any employee
        val customOpt = repo.addCustomPermissionOption(
            actorId = farmer.id,
            ownerUserId = farmer.id,
            titleEn = "Hatchery Incubator Control",
            titleBn = "হ্যাচারি ইনকিউবেটর নিয়ন্ত্রণ",
            descEn = "Operate duck & chicken egg incubators",
            descBn = "হাঁস-মুরগির ডিম ফুটানোর যন্ত্র পরিচালনা"
        ).getOrThrow()

        val initialPerms = repo.observePermissionsByOwner(farmer.id).first()
            .filter { it.permissionKey == customOpt.permissionKey && it.granted }
        assertEquals(0, initialPerms.size)

        // Admin grants the new dynamic option to all 55 employees
        repo.setPermissionForMultipleEmployees(
            actorId = farmer.id,
            ownerUserId = farmer.id,
            employees = createdEmployees,
            permissionKey = customOpt.permissionKey,
            granted = true
        ).getOrThrow()
        val afterGrantCount = repo.observePermissionsByOwner(farmer.id).first()
            .count { it.permissionKey == customOpt.permissionKey && it.granted }
        assertEquals(55, afterGrantCount)

        // Admin takes back access from all 55 employees
        repo.setPermissionForMultipleEmployees(
            actorId = farmer.id,
            ownerUserId = farmer.id,
            employees = createdEmployees,
            permissionKey = customOpt.permissionKey,
            granted = false
        ).getOrThrow()
        val afterRevokeCount = repo.observePermissionsByOwner(farmer.id).first()
            .count { it.permissionKey == customOpt.permissionKey && it.granted }
        assertEquals(0, afterRevokeCount)

        // Delete custom option cleans up custom option & permission rows
        repo.deleteCustomPermissionOption(farmer.id, customOpt).getOrThrow()
        assertTrue(repo.observeCustomOptionsByOwner(farmer.id).first().none { it.permissionKey == customOpt.permissionKey })
    }

    @Test
    fun roomMigration_v2ToV3_preservesSettingsAndAddsSubscriptionControls() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val dbName = "test_migration_v2_v3_${System.currentTimeMillis()}.db"
        context.deleteDatabase(dbName)

        val factory = androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory()
        val openHelper = factory.create(
            androidx.sqlite.db.SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(dbName)
                .callback(object : androidx.sqlite.db.SupportSQLiteOpenHelper.Callback(2) {
                    override fun onCreate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                        createLegacyV1Schema(db)
                        SmartFarmDatabase.MIGRATION_1_2.migrate(db)
                        db.execSQL("INSERT INTO users (id, name, mobile, email, passwordHash, role, status, profileImageUri, preferredLanguage, preferredCurrency, createdAt) VALUES (1, 'v2 Farmer', '01711111111', 'farmer@smartfarm.app', '${PasswordHasher.hash("farmer123")}', 'FARMER', 'ACTIVE', NULL, 'bn', 'BDT', 1000)")
                        db.execSQL("INSERT INTO system_settings (id, announcementBanner, maintenanceMode, defaultCurrencyCode, allowManualPayments) VALUES (1, 'v2 preserved banner', 0, 'BDT', 1)")
                    }
                    override fun onUpgrade(db: androidx.sqlite.db.SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
                })
                .build()
        )
        openHelper.writableDatabase.close()

        val migratedDb = Room.databaseBuilder(context, SmartFarmDatabase::class.java, dbName)
            .addMigrations(
                SmartFarmDatabase.MIGRATION_1_2,
                SmartFarmDatabase.MIGRATION_2_3,
                SmartFarmDatabase.MIGRATION_3_4,
                SmartFarmDatabase.MIGRATION_4_5,
                SmartFarmDatabase.MIGRATION_5_6
            )
            .allowMainThreadQueries()
            .build()
        try {
            val migratedRepo = FarmRepository(migratedDb)
            migratedRepo.initializeDatabase()
            val settings = migratedRepo.observeSystemSettings().first()!!
            assertEquals("v2 preserved banner", settings.announcementBanner)
            assertTrue(settings.bkashEnabled)
            assertTrue(settings.nagadEnabled)
            assertTrue(settings.bankEnabled)
            assertTrue(settings.parseLockedFeatureKeys().contains(SubscriptionFeature.BUY_SELL_SALES_CRM.key))
            assertFalse(settings.parseLockedFeatureKeys().contains(SubscriptionFeature.EGG_PRODUCTION.key))
        } finally {
            migratedDb.close()
            context.deleteDatabase(dbName)
        }
    }

    @Test
    fun developerFeatureGatingAndPaymentChannels_workEndToEnd() = runTest {
        repo.initializeDatabase()
        val admin = repo.login("admin@smartfarm.app", "admin@2026").getOrThrow()

        // Register a brand-new farmer WITHOUT any subscription
        val newFarmer = repo.register(
            name = "Karim Farmer",
            mobile = "01899887766",
            email = "unpaid@smartfarm.app",
            password = "pass1234",
            role = UserRole.FARMER
        ).getOrThrow()

        // 1. Free options work without subscription: Multi-Farm + Egg Production
        val farmId = repo.saveFarm(
            FarmEntity(
                ownerUserId = newFarmer.id,
                farmCode = "FM-UNPAID-1",
                name = "Starter Duck Farm",
                location = "Barishal",
                address = "Ward 4",
                category = FarmCategory.DUCK.name,
                openingDate = System.currentTimeMillis(),
                managerName = newFarmer.name,
                managerPhone = newFarmer.mobile,
                initialInvestment = 25000.0
            ),
            actorUserId = newFarmer.id
        ).getOrThrow()
        assertTrue(farmId > 0L)

        // 2. Paid/subscription options blocked when unpaid: Buying/Selling & Expenses
        val blockedSale = runCatching {
            repo.createSale(
                farmId = farmId,
                ownerId = newFarmer.id,
                customerId = null,
                customerName = "Walk-in",
                customerPhone = "01700000001",
                productCategory = "Eggs",
                productName = "Duck Eggs",
                inventoryItem = null,
                animalRecord = null,
                quantity = 10.0,
                unitPrice = 15.0,
                discount = 0.0,
                paidAmount = 150.0,
                paymentMethod = "Cash",
                actorUserId = newFarmer.id
            )
        }
        assertTrue(blockedSale.isFailure)
        assertTrue(blockedSale.exceptionOrNull() is SecurityException)

        val blockedExpense = runCatching {
            repo.addExpense(
                ExpenseEntity(
                    farmId = farmId,
                    ownerUserId = newFarmer.id,
                    category = "Food and feed",
                    amount = 500.0,
                    description = "Duck Feed"
                ),
                actorUserId = newFarmer.id
            )
        }
        assertTrue(blockedExpense.isFailure)
        assertTrue(blockedExpense.exceptionOrNull() is SecurityException)

        // 3. Super Admin toggles BUY_SELL_SALES_CRM to FREE -> unpaid farmer can now record a sale!
        repo.adminToggleFeatureSubscription(admin.id, SubscriptionFeature.BUY_SELL_SALES_CRM.key, requiresSubscription = false).getOrThrow()
        repo.createSale(
            farmId = farmId,
            ownerId = newFarmer.id,
            customerId = null,
            customerName = "Walk-in",
            customerPhone = "01700000001",
            productCategory = "Eggs",
            productName = "Duck Eggs",
            inventoryItem = null,
            animalRecord = null,
            quantity = 10.0,
            unitPrice = 15.0,
            discount = 0.0,
            paidAmount = 150.0,
            paymentMethod = "Cash",
            actorUserId = newFarmer.id
        )
        assertEquals(1, repo.observeSales(newFarmer.id).first().size)

        // 4. Super Admin locks MULTI_FARM_MANAGEMENT -> unpaid farmer cannot add a second farm
        repo.adminToggleFeatureSubscription(admin.id, SubscriptionFeature.MULTI_FARM_MANAGEMENT.key, requiresSubscription = true).getOrThrow()
        val blockedSecondFarm = repo.saveFarm(
            FarmEntity(
                ownerUserId = newFarmer.id,
                farmCode = "FM-UNPAID-2",
                name = "Locked 2nd Farm",
                location = "Dhaka",
                address = "Mirpur",
                category = FarmCategory.POULTRY.name,
                openingDate = System.currentTimeMillis(),
                managerName = newFarmer.name,
                managerPhone = newFarmer.mobile,
                initialInvestment = 10000.0
            ),
            actorUserId = newFarmer.id
        )
        assertTrue(blockedSecondFarm.isFailure)
        assertTrue(blockedSecondFarm.exceptionOrNull() is SecurityException)

        // 5. Super Admin configures bKash, Nagad & Bank Account receiving details
        val baseSettings = repo.observeSystemSettings().first()!!
        repo.adminUpdateSettings(
            admin.id,
            baseSettings.copy(
                bkashEnabled = true,
                bkashNumber = "01799001122",
                bkashAccountType = "Merchant",
                nagadEnabled = false,
                bankEnabled = true,
                bankName = "Islami Bank Bangladesh",
                bankAccountName = "Developer Enterprise",
                bankAccountNumber = "2050-9988-7766",
                bankBranch = "Motijheel",
                requireAdminApprovalForAll = false
            )
        ).getOrThrow()

        val updatedSettings = repo.observeSystemSettings().first()!!
        val configuredMethods = updatedSettings.configuredPaymentMethods()
        assertEquals(listOf("bKash", "Bank Account"), configuredMethods.map { it.id })
        assertEquals("01799001122", configuredMethods.first { it.id == "bKash" }.primaryNumberOrAccount)
        assertTrue(configuredMethods.first { it.id == "Bank Account" }.accountTitleEn.contains("Islami Bank"))

        // 6. Farmer pays via Bank Account -> enters PENDING approval until Super Admin approves
        val monthlyPlan = repo.observePlans().first().first()
        val pendingSub = repo.purchaseSubscription(
            user = newFarmer,
            plan = monthlyPlan,
            currencyCode = "BDT",
            paymentMethod = "Bank Account",
            transactionRef = "BANK-REF-7788",
            actorUserId = newFarmer.id
        ).getOrThrow()
        assertEquals("PENDING", pendingSub.status)
        assertEquals(PaymentStatus.PENDING.name, pendingSub.paymentStatus)

        // Still locked while PENDING!
        val policyWhilePending = SubscriptionAccessPolicy.fromSettingsAndSubs(
            updatedSettings,
            repo.observeUserSubscriptions(newFarmer.id).first()
        )
        assertFalse(policyWhilePending.hasActiveSubscription)
        assertFalse(policyWhilePending.canAccess(SubscriptionFeature.MULTI_FARM_MANAGEMENT))

        // Super Admin approves the bank payment -> subscription becomes ACTIVE & locked modules unlock!
        repo.adminUpdateSubscriptionPayment(
            actorId = admin.id,
            sub = pendingSub,
            paymentStatus = PaymentStatus.SUCCESSFUL,
            subStatus = "ACTIVE"
        ).getOrThrow()

        val policyAfterApproval = SubscriptionAccessPolicy.fromSettingsAndSubs(
            repo.observeSystemSettings().first()!!,
            repo.observeUserSubscriptions(newFarmer.id).first()
        )
        assertTrue(policyAfterApproval.hasActiveSubscription)
        assertTrue(policyAfterApproval.canAccess(SubscriptionFeature.MULTI_FARM_MANAGEMENT))
        assertTrue(policyAfterApproval.canAccess(SubscriptionFeature.INVENTORY_EXPENSES))

        // Unlocked farmer can now create second farm and record expenses
        assertTrue(
            repo.saveFarm(
                FarmEntity(
                    ownerUserId = newFarmer.id,
                    farmCode = "FM-UNPAID-2",
                    name = "Unlocked 2nd Farm",
                    location = "Dhaka",
                    address = "Mirpur",
                    category = FarmCategory.POULTRY.name,
                    openingDate = System.currentTimeMillis(),
                    managerName = newFarmer.name,
                    managerPhone = newFarmer.mobile,
                    initialInvestment = 10000.0
                ),
                actorUserId = newFarmer.id
            ).isSuccess
        )
        repo.addExpense(
            ExpenseEntity(
                farmId = farmId,
                ownerUserId = newFarmer.id,
                category = "Food and feed",
                amount = 500.0,
                description = "Duck Feed"
            ),
            actorUserId = newFarmer.id
        )
        assertEquals(1, repo.observeExpenses(newFarmer.id).first().size)
    }

    @Test
    fun roomMigration_v3ToV4_preservesInventoryAndAddsSourcingType() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val dbName = "test_migration_v3_v4_${System.currentTimeMillis()}.db"
        context.deleteDatabase(dbName)

        val factory = androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory()
        val openHelper = factory.create(
            androidx.sqlite.db.SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(dbName)
                .callback(object : androidx.sqlite.db.SupportSQLiteOpenHelper.Callback(3) {
                    override fun onCreate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                        createLegacyV1Schema(db)
                        SmartFarmDatabase.MIGRATION_1_2.migrate(db)
                        SmartFarmDatabase.MIGRATION_2_3.migrate(db)
                        db.execSQL("INSERT INTO users (id, name, mobile, email, passwordHash, role, status, profileImageUri, preferredLanguage, preferredCurrency, createdAt) VALUES (1, 'v3 Farmer', '01711111111', 'farmer@smartfarm.app', '${PasswordHasher.hash("farmer123")}', 'FARMER', 'ACTIVE', NULL, 'bn', 'BDT', 1000)")
                        db.execSQL("INSERT INTO farms (id, ownerUserId, farmCode, name, location, address, category, customCategoryName, imageUri, openingDate, managerName, managerPhone, initialInvestment, createdAt) VALUES (1, 1, 'FM-3001', 'v3 Agro Farm', 'Bogura', 'Road 2', 'MIXED', '', NULL, 1000, 'v3 Farmer', '01711111111', 20000.0, 1000)")
                        db.execSQL("INSERT INTO inventory_items (id, farmId, ownerUserId, name, category, quantity, unit, unitCost, sellingPrice, lowStockThreshold, expiryDate, supplierName, supplierPhone, listInMarketplace, description, imageUri, updatedAt) VALUES (1, 1, 1, 'v3 Stored Rice', 'Rice', 50.0, 'kg', 50.0, 70.0, 10.0, NULL, 'Partner', '017', 1, 'v3 grain', NULL, 2000)")
                    }
                    override fun onUpgrade(db: androidx.sqlite.db.SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
                })
                .build()
        )
        openHelper.writableDatabase.close()

        val migratedDb = Room.databaseBuilder(context, SmartFarmDatabase::class.java, dbName)
            .addMigrations(
                SmartFarmDatabase.MIGRATION_1_2,
                SmartFarmDatabase.MIGRATION_2_3,
                SmartFarmDatabase.MIGRATION_3_4,
                SmartFarmDatabase.MIGRATION_4_5,
                SmartFarmDatabase.MIGRATION_5_6
            )
            .allowMainThreadQueries()
            .build()
        try {
            val migratedRepo = FarmRepository(migratedDb)
            migratedRepo.initializeDatabase()
            val item = migratedDb.inventoryDao().getItemById(1L)!!
            assertEquals("v3 Stored Rice", item.name)
            assertEquals(ProductSourcingType.FARM_PRODUCED.key, item.sourcingType)
        } finally {
            migratedDb.close()
            context.deleteDatabase(dbName)
        }
    }

    @Test
    fun onlineProductMarketplace_supportsAllAgriProductsResellersPagingAndFarmOrderNotifications() = runTest {
        repo.initializeDatabase()

        // Create two independent Farm Business accounts + a Customer
        val agroSellerA = repo.register(
            name = "Padma Agro Crops",
            mobile = "01711000111",
            email = "agroA@smartfarm.app",
            password = "pass1234",
            role = UserRole.FARMER
        ).getOrThrow()
        val agroSellerB = repo.register(
            name = "Barendra Traders",
            mobile = "01711000222",
            email = "agroB@smartfarm.app",
            password = "pass1234",
            role = UserRole.FARMER
        ).getOrThrow()
        val buyer = repo.register(
            name = "Tania Akter",
            mobile = "01811000333",
            email = "buyer@smartfarm.app",
            password = "pass1234",
            role = UserRole.CUSTOMER
        ).getOrThrow()

        val farmAId = repo.saveFarm(
            FarmEntity(
                ownerUserId = agroSellerA.id,
                farmCode = "FM-AGRO-A",
                name = "Padma Rice & Wheat Farm",
                location = "Dinajpur",
                address = "Sector 1",
                category = FarmCategory.MIXED.name,
                managerName = agroSellerA.name,
                managerPhone = agroSellerA.mobile
            ),
            actorUserId = agroSellerA.id
        ).getOrThrow()

        val farmBId = repo.saveFarm(
            FarmEntity(
                ownerUserId = agroSellerB.id,
                farmCode = "FM-AGRO-B",
                name = "Barendra Mustard & Spice Hub",
                location = "Rajshahi",
                address = "Bazar Road",
                category = FarmCategory.MIXED.name,
                managerName = agroSellerB.name,
                managerPhone = agroSellerB.mobile
            ),
            actorUserId = agroSellerB.id
        ).getOrThrow()

        // Seller A adds Own Harvest Rice + Wheat
        val riceId = repo.saveOnlineProduct(
            InventoryItemEntity(
                farmId = farmAId,
                ownerUserId = agroSellerA.id,
                name = "Kataribhog Aromatic Rice",
                category = "Rice",
                quantity = 100.0,
                unit = "kg",
                unitCost = 70.0,
                sellingPrice = 95.0,
                listInMarketplace = true,
                description = "Aromatic Dinajpur harvest",
                sourcingType = ProductSourcingType.FARM_PRODUCED.key
            ),
            actorUserId = agroSellerA.id
        ).getOrThrow()

        val wheatId = repo.saveOnlineProduct(
            InventoryItemEntity(
                farmId = farmAId,
                ownerUserId = agroSellerA.id,
                name = "Whole Organic Wheat Grain",
                category = "Wheat",
                quantity = 80.0,
                unit = "kg",
                unitCost = 42.0,
                sellingPrice = 58.0,
                listInMarketplace = true,
                sourcingType = ProductSourcingType.FARM_PRODUCED.key
            ),
            actorUserId = agroSellerA.id
        ).getOrThrow()

        // Seller B adds Bought & Resold Mustard + Custom category "Mango"
        val mustardId = repo.saveOnlineProduct(
            InventoryItemEntity(
                farmId = farmBId,
                ownerUserId = agroSellerB.id,
                name = "Pure Yellow Mustard Seeds",
                category = "Mustard",
                quantity = 40.0,
                unit = "kg",
                unitCost = 110.0,
                sellingPrice = 145.0,
                listInMarketplace = true,
                description = "Sourced directly from farmers for online resale",
                sourcingType = ProductSourcingType.PURCHASED_RESALE.key
            ),
            actorUserId = agroSellerB.id
        ).getOrThrow()

        val mangoId = repo.saveOnlineProduct(
            InventoryItemEntity(
                farmId = farmBId,
                ownerUserId = agroSellerB.id,
                name = "Himsagar Fresh Mango",
                category = "Mango",
                quantity = 25.0,
                unit = "kg",
                unitCost = 85.0,
                sellingPrice = 120.0,
                listInMarketplace = true,
                sourcingType = ProductSourcingType.PURCHASED_RESALE.key
            ),
            actorUserId = agroSellerB.id
        ).getOrThrow()

        // Verify scalable indexed SQL search + category filtering + pagination
        val mustardCategoryMatches = repo.observeMarketplaceItemsFiltered("", "Mustard", 30).first()
        assertTrue(mustardCategoryMatches.size >= 2)
        assertTrue(mustardCategoryMatches.any { it.id == mustardId })

        val mustardExactMatches = repo.observeMarketplaceItemsFiltered("Pure Yellow Mustard", null, 30).first()
        assertEquals(1, mustardExactMatches.size)
        assertEquals(mustardId, mustardExactMatches.first().id)

        val paged2 = repo.observeMarketplaceItemsFiltered("", null, 2).first()
        val totalLive = repo.countMarketplaceItemsFiltered("", null).first()
        assertEquals(2, paged2.size)
        assertTrue(totalLive >= 9) // 5 seeded + 4 newly added

        val riceItem = db.inventoryDao().getItemById(riceId)!!
        val wheatItem = db.inventoryDao().getItemById(wheatId)!!
        val mustardItem = db.inventoryDao().getItemById(mustardId)!!
        val mangoItem = db.inventoryDao().getItemById(mangoId)!!

        // Insufficient stock rejection check
        val overStockOrder = repo.placeOnlineOrder(
            customer = buyer,
            deliveryAddress = "House 12, Uttara, Dhaka",
            paymentMethod = "bKash",
            cartItems = listOf(mangoItem to 50.0),
            farmsMap = emptyMap()
        )
        assertTrue(overStockOrder.isFailure)

        // Valid cross-farm order: splits into separate orders and notifies each exact farm owner!
        val createdOrders = repo.placeOnlineOrder(
            customer = buyer,
            deliveryAddress = "House 12, Uttara, Dhaka",
            paymentMethod = "bKash",
            cartItems = listOf(riceItem to 5.0, mustardItem to 2.0),
            farmsMap = emptyMap()
        ).getOrThrow()

        assertEquals(2, createdOrders.size)
        val orderForFarmA = createdOrders.first { it.farmId == farmAId }
        val orderForFarmB = createdOrders.first { it.farmId == farmBId }
        assertEquals(agroSellerA.id, orderForFarmA.farmOwnerUserId)
        assertEquals(agroSellerB.id, orderForFarmB.farmOwnerUserId)
        assertEquals(475.0, orderForFarmA.totalAmount, 0.01)
        assertEquals(290.0, orderForFarmB.totalAmount, 0.01)

        // Seller A sees ONLY farmA order & farmA notification; Seller B sees ONLY farmB order & farmB notification
        val sellerAOrders = repo.observeFarmerOrders(agroSellerA.id).first()
        val sellerBOrders = repo.observeFarmerOrders(agroSellerB.id).first()
        assertEquals(1, sellerAOrders.size)
        assertEquals(1, sellerBOrders.size)
        assertEquals(farmAId, sellerAOrders.first().farmId)
        assertEquals(farmBId, sellerBOrders.first().farmId)

        val sellerANotifs = repo.observeNotifications(agroSellerA.id).first().filter { it.category == "ORDER" }
        val sellerBNotifs = repo.observeNotifications(agroSellerB.id).first().filter { it.category == "ORDER" }
        assertTrue(sellerANotifs.any { it.title.contains(orderForFarmA.orderNumber) && it.message.contains("Padma Rice & Wheat Farm") })
        assertTrue(sellerANotifs.none { it.title.contains(orderForFarmB.orderNumber) })
        assertTrue(sellerBNotifs.any { it.title.contains(orderForFarmB.orderNumber) && it.message.contains("Barendra Mustard & Spice Hub") })
        assertTrue(sellerBNotifs.none { it.title.contains(orderForFarmA.orderNumber) })

        // Verify pausing wheat removes it from marketplace
        repo.setOnlineProductVisibility(wheatItem, isLive = false, actorUserId = agroSellerA.id).getOrThrow()
        assertTrue(repo.observeMarketplaceItemsFiltered("Wheat", null, 30).first().isEmpty())
    }

    @Test
    fun agricultureLedger_tracksCropsLandsSectorExpensesYieldWorthProfitLossAndPermissions() = runTest {
        repo.initializeDatabase()
        val farmer = repo.login("farmer@smartfarm.app", "farmer123").getOrThrow()
        val karimUser = repo.login("karim@smartfarm.app", "employee123").getOrThrow()
        val karimEmp = repo.observeEmployees(farmer.id).first().first { it.linkedUserId == karimUser.id }

        // Employee without AGRICULTURE_MANAGE cannot create a land plot
        repo.setEmployeePermission(farmer.id, karimEmp, AppPermission.AGRICULTURE_MANAGE.key, false).getOrThrow()
        val deniedLand = repo.saveAgriLand(
            AgriLandEntity(ownerUserId = farmer.id, name = "Unauthorized Plot", areaSize = 50.0),
            actorUserId = karimUser.id
        )
        assertTrue(deniedLand.isFailure)
        assertTrue(deniedLand.exceptionOrNull() is SecurityException)

        // Farmer grants AGRICULTURE_MANAGE -> employee can now add a new land plot
        repo.setEmployeePermission(farmer.id, karimEmp, AppPermission.AGRICULTURE_MANAGE.key, true).getOrThrow()
        val testPlotId = repo.saveAgriLand(
            AgriLandEntity(
                ownerUserId = farmer.id,
                name = "South Maize & Wheat Plot",
                areaSize = 100.0,
                areaUnit = AgriLandUnit.DECIMAL.key,
                location = "Bogra South"
            ),
            actorUserId = karimUser.id
        ).getOrThrow()

        // Create Corn & Cattle Feed crops on this plot
        val cornCropId = repo.saveAgriCrop(
            AgriCropEntity(
                ownerUserId = farmer.id,
                landId = testPlotId,
                cropType = AgriCropType.CORN.key,
                title = "Boro Corn Batch",
                season = AgriSeason.RABI.key
            ),
            actorUserId = karimUser.id
        ).getOrThrow()

        val feedCropId = repo.saveAgriCrop(
            AgriCropEntity(
                ownerUserId = farmer.id,
                landId = testPlotId,
                cropType = AgriCropType.CATTLE_FEED.key,
                title = "Napier Cattle Feed",
                season = AgriSeason.YEAR_ROUND.key
            ),
            actorUserId = karimUser.id
        ).getOrThrow()

        // Record sector-wise expenses across Seeds, Fertilizer, Irrigation, and Labor
        repo.addAgriExpense(
            AgriExpenseEntity(
                ownerUserId = farmer.id,
                landId = testPlotId,
                cropId = cornCropId,
                sector = AgriSector.SEEDS.key,
                amount = 3000.0,
                description = "Hybrid corn seeds"
            ),
            actorUserId = farmer.id
        ).getOrThrow()
        repo.addAgriExpense(
            AgriExpenseEntity(
                ownerUserId = farmer.id,
                landId = testPlotId,
                cropId = cornCropId,
                sector = AgriSector.FERTILIZER.key,
                amount = 4500.0,
                description = "Urea & Potash"
            ),
            actorUserId = farmer.id
        ).getOrThrow()
        repo.addAgriExpense(
            AgriExpenseEntity(
                ownerUserId = farmer.id,
                landId = testPlotId,
                cropId = feedCropId,
                sector = AgriSector.IRRIGATION.key,
                amount = 1500.0,
                description = "Fodder irrigation"
            ),
            actorUserId = farmer.id
        ).getOrThrow()
        repo.addAgriExpense(
            AgriExpenseEntity(
                ownerUserId = farmer.id,
                landId = testPlotId,
                cropId = null,
                sector = AgriSector.LABOR.key,
                amount = 2000.0,
                description = "Land bund & clearing"
            ),
            actorUserId = farmer.id
        ).getOrThrow()

        // Record Harvest & Yield Market Worth on Corn (20 mon * 1,100 = 22,000 worth) and Cattle Feed (worth 5,000)
        val cornCrop = db.agriDao().getCropById(cornCropId)!!
        repo.recordAgriHarvest(
            crop = cornCrop,
            yieldQuantity = 20.0,
            yieldUnit = "mon",
            marketPricePerUnit = 1100.0,
            yieldMarketWorth = 0.0,
            soldAmount = 18000.0,
            status = AgriCropStatus.SOLD.key,
            actorUserId = farmer.id
        ).getOrThrow()

        val feedCrop = db.agriDao().getCropById(feedCropId)!!
        repo.recordAgriHarvest(
            crop = feedCrop,
            yieldQuantity = 500.0,
            yieldUnit = "kg",
            marketPricePerUnit = 10.0,
            yieldMarketWorth = 0.0,
            soldAmount = 0.0,
            status = AgriCropStatus.HARVESTED.key,
            actorUserId = farmer.id
        ).getOrThrow()

        val allLands = repo.observeAgriLands(farmer.id).first()
        val allCrops = repo.observeAgriCrops(farmer.id).first()
        val allExpenses = repo.observeAgriExpenses(farmer.id).first()

        val plot = allLands.first { it.id == testPlotId }
        val landSummary = AgriLedgerCalculator.summarizeLand(plot, allCrops, allExpenses)
        // Total spent on plot = 3000 + 4500 + 1500 + 2000 = 11,000; cost per decimal = 110.0
        assertEquals(11000.0, landSummary.totalSpent, 0.01)
        assertEquals(110.0, landSummary.costPerUnitArea, 0.01)
        // Yield worth = (20 * 1100 = 22000) + (500 * 10 = 5000) = 27,000
        assertEquals(27000.0, landSummary.totalYieldWorth, 0.01)
        // Effective return = soldAmount(18000) + feed yieldWorth(5000) = 23000 -> Net profit = 23000 - 11000 = 12000
        assertEquals(12000.0, landSummary.profitOrLoss, 0.01)
        assertEquals(3000.0, landSummary.sectorTotals[AgriSector.SEEDS.key]!!, 0.01)
        assertEquals(4500.0, landSummary.sectorTotals[AgriSector.FERTILIZER.key]!!, 0.01)

        // Employee without AGRICULTURE_DELETE cannot delete plot
        repo.setEmployeePermission(farmer.id, karimEmp, AppPermission.AGRICULTURE_DELETE.key, false).getOrThrow()
        val deniedDelete = repo.deleteAgriLand(plot, actorUserId = karimUser.id)
        assertTrue(deniedDelete.isFailure)
        assertTrue(deniedDelete.exceptionOrNull() is SecurityException)

        // Farmer deletes plot -> crops & expenses on that plot cascade delete cleanly
        repo.deleteAgriLand(plot, actorUserId = farmer.id).getOrThrow()
        assertTrue(repo.observeAgriLands(farmer.id).first().none { it.id == testPlotId })
        assertTrue(repo.observeAgriCrops(farmer.id).first().none { it.landId == testPlotId })
        assertTrue(repo.observeAgriExpenses(farmer.id).first().none { it.landId == testPlotId })
    }

    @Test
    fun roomMigration_v4ToV5_addsAgricultureTablesAndPreservesExistingData() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val dbName = "test_migration_v4_v5_${System.currentTimeMillis()}.db"
        context.deleteDatabase(dbName)

        val factory = androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory()
        val openHelper = factory.create(
            androidx.sqlite.db.SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(dbName)
                .callback(object : androidx.sqlite.db.SupportSQLiteOpenHelper.Callback(4) {
                    override fun onCreate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                        createLegacyV1Schema(db)
                        SmartFarmDatabase.MIGRATION_1_2.migrate(db)
                        SmartFarmDatabase.MIGRATION_2_3.migrate(db)
                        SmartFarmDatabase.MIGRATION_3_4.migrate(db)
                        db.execSQL("INSERT INTO users (id, name, mobile, email, passwordHash, role, status, profileImageUri, preferredLanguage, preferredCurrency, createdAt) VALUES (1, 'v4 Farmer', '01711111111', 'farmer@smartfarm.app', '${PasswordHasher.hash("farmer123")}', 'FARMER', 'ACTIVE', NULL, 'en', 'BDT', 1000)")
                    }
                    override fun onUpgrade(db: androidx.sqlite.db.SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
                })
                .build()
        )
        openHelper.writableDatabase.close()

        val migratedDb = Room.databaseBuilder(context, SmartFarmDatabase::class.java, dbName)
            .addMigrations(
                SmartFarmDatabase.MIGRATION_1_2,
                SmartFarmDatabase.MIGRATION_2_3,
                SmartFarmDatabase.MIGRATION_3_4,
                SmartFarmDatabase.MIGRATION_4_5,
                SmartFarmDatabase.MIGRATION_5_6
            )
            .allowMainThreadQueries()
            .build()
        try {
            val migratedRepo = FarmRepository(migratedDb)
            migratedRepo.initializeDatabase()
            val landId = migratedRepo.saveAgriLand(
                AgriLandEntity(ownerUserId = 1L, name = "Migrated Paddy Land", areaSize = 60.0),
                actorUserId = 1L
            ).getOrThrow()
            val cropId = migratedRepo.saveAgriCrop(
                AgriCropEntity(ownerUserId = 1L, landId = landId, cropType = AgriCropType.WHEAT.key, title = "Wheat Plot 1"),
                actorUserId = 1L
            ).getOrThrow()
            migratedRepo.addAgriExpense(
                AgriExpenseEntity(
                    ownerUserId = 1L,
                    landId = landId,
                    cropId = cropId,
                    sector = AgriSector.SEEDS.key,
                    amount = 1200.0,
                    description = "Wheat Seeds"
                ),
                actorUserId = 1L
            ).getOrThrow()
            assertEquals(1, migratedRepo.observeAgriLands(1L).first().size)
            assertEquals(1, migratedRepo.observeAgriCrops(1L).first().size)
            assertEquals(1, migratedRepo.observeAgriExpenses(1L).first().size)
        } finally {
            migratedDb.close()
            context.deleteDatabase(dbName)
        }
    }

    @Test
    fun productAdStudio_savesPublishesTogglesDeletesAndEnforcesPermissions() = runTest {
        repo.initializeDatabase()
        val farmer = repo.login("farmer@smartfarm.app", "farmer123").getOrThrow()
        val farmId = repo.observeFarms(farmer.id).first().first().id
        val riceId = repo.saveInventoryItem(
            InventoryItemEntity(
                farmId = farmId,
                ownerUserId = farmer.id,
                name = "Kataribhog Rice",
                category = "Rice",
                quantity = 120.0,
                unit = "kg",
                unitCost = 65.0,
                sellingPrice = 88.0,
                listInMarketplace = true,
                description = "Aromatic dinajpur rice",
                sourcingType = ProductSourcingType.FARM_PRODUCED.key
            ),
            actorUserId = farmer.id
        )

        // Blank headline must fail cleanly
        val invalid = repo.saveProductAd(
            ProductAdEntity(
                ownerUserId = farmer.id,
                farmId = farmId,
                inventoryItemId = riceId,
                productName = "Kataribhog Rice",
                farmName = "Green Valley Agro",
                priceText = "৳88 / kg",
                hookHeadlineBn = "   ",
                promoBadgeBn = "তাজা",
                benefitsPipeSeparatedBn = "১|২|৩",
                callToActionBn = "এখনই অর্ডার করুন",
                voiceoverScriptBn = "স্ক্রিপ্ট",
                orderStepsPipeSeparatedBn = "ধাপ ১|ধাপ ২",
                isActiveInShop = true
            ),
            actorUserId = farmer.id
        )
        assertTrue(invalid.isFailure)

        val adId = repo.saveProductAd(
            ProductAdEntity(
                ownerUserId = farmer.id,
                farmId = farmId,
                inventoryItemId = riceId,
                productName = "Kataribhog Rice",
                farmName = "Green Valley Agro",
                priceText = "৳88 / kg",
                hookHeadlineBn = "সরাসরি খামার থেকে সুগন্ধি কাটারিভোগ চাল!",
                promoBadgeBn = "সরাসরি খামার থেকে তাজা",
                benefitsPipeSeparatedBn = "১০০% খাঁটি|রাসায়নিকমুক্ত|সারা দেশে ডেলিভারি",
                callToActionBn = "এখনই অর্ডার করুন • এখানে ক্লিক করুন",
                voiceoverScriptBn = "খাঁটি কাটারিভোগ চাল অর্ডার করতে এখনই এখানে ক্লিক করুন!",
                orderStepsPipeSeparatedBn = "১. পণ্য বাছুন|২. এখানে ক্লিক করুন|৩. ঠিকানা দিন|৪. অর্ডার কনফার্ম",
                isActiveInShop = true
            ),
            actorUserId = farmer.id
        ).getOrThrow()

        val ownerAds = repo.observeProductAds(farmer.id).first()
        assertEquals(1, ownerAds.size)
        assertEquals(adId, ownerAds.first().id)
        assertTrue(repo.observeActiveShopAds().first().any { it.id == adId })

        // Unpublish from marketplace -> hidden from customer active ads, still owned by farmer
        val savedAd = ownerAds.first()
        repo.toggleProductAdActive(savedAd, active = false, actorUserId = farmer.id).getOrThrow()
        assertTrue(repo.observeActiveShopAds().first().none { it.id == adId })
        assertFalse(repo.observeProductAds(farmer.id).first().first().isActiveInShop)

        // Permissions & subscription defaults
        assertTrue(PermissionPreset.MANAGER.keys.contains(AppPermission.AI_AD_STUDIO.key))
        assertTrue(PermissionPreset.SALES_CASHIER.keys.contains(AppPermission.AI_AD_STUDIO.key))
        assertFalse(PermissionPreset.POULTRY_WORKER.keys.contains(AppPermission.AI_AD_STUDIO.key))
        assertFalse(SubscriptionFeature.DEFAULT_LOCKED_KEYS.contains(SubscriptionFeature.AI_AD_VIDEO_STUDIO.key))

        // Delete removes ad cleanly
        repo.deleteProductAd(savedAd, actorUserId = farmer.id).getOrThrow()
        assertTrue(repo.observeProductAds(farmer.id).first().isEmpty())
    }

    @Test
    fun roomMigration_v5ToV6_addsProductAdsTableAndPreservesExistingData() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val dbName = "test_migration_v5_v6_${System.currentTimeMillis()}.db"
        context.deleteDatabase(dbName)

        val factory = androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory()
        val openHelper = factory.create(
            androidx.sqlite.db.SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(dbName)
                .callback(object : androidx.sqlite.db.SupportSQLiteOpenHelper.Callback(5) {
                    override fun onCreate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                        createLegacyV1Schema(db)
                        SmartFarmDatabase.MIGRATION_1_2.migrate(db)
                        SmartFarmDatabase.MIGRATION_2_3.migrate(db)
                        SmartFarmDatabase.MIGRATION_3_4.migrate(db)
                        SmartFarmDatabase.MIGRATION_4_5.migrate(db)
                        db.execSQL("INSERT INTO users (id, name, mobile, email, passwordHash, role, status, profileImageUri, preferredLanguage, preferredCurrency, createdAt) VALUES (1, 'v5 Farmer', '01711111111', 'farmer@smartfarm.app', '${PasswordHasher.hash("farmer123")}', 'FARMER', 'ACTIVE', NULL, 'en', 'BDT', 1000)")
                    }
                    override fun onUpgrade(db: androidx.sqlite.db.SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
                })
                .build()
        )
        openHelper.writableDatabase.close()

        val migratedDb = Room.databaseBuilder(context, SmartFarmDatabase::class.java, dbName)
            .addMigrations(
                SmartFarmDatabase.MIGRATION_1_2,
                SmartFarmDatabase.MIGRATION_2_3,
                SmartFarmDatabase.MIGRATION_3_4,
                SmartFarmDatabase.MIGRATION_4_5,
                SmartFarmDatabase.MIGRATION_5_6
            )
            .allowMainThreadQueries()
            .build()
        try {
            val migratedRepo = FarmRepository(migratedDb)
            migratedRepo.initializeDatabase()
            val adId = migratedRepo.saveProductAd(
                ProductAdEntity(
                    ownerUserId = 1L,
                    farmId = 0L,
                    inventoryItemId = null,
                    productName = "সরিষার তেল",
                    farmName = "Smart Farm",
                    priceText = "৳280 / L",
                    hookHeadlineBn = "১০০% খাঁটি ঘানি ভাঙা সরিষার তেল",
                    promoBadgeBn = "সেরা মানের পণ্য",
                    benefitsPipeSeparatedBn = "খাঁটি|ভেজালমুক্ত|তাজা",
                    callToActionBn = "এখনই অর্ডার করুন",
                    voiceoverScriptBn = "খাঁটি সরিষার তেল কিনতে এখানে ক্লিক করুন।",
                    orderStepsPipeSeparatedBn = "১. বেছে নিন|২. ক্লিক করুন",
                    isActiveInShop = true
                ),
                actorUserId = 1L
            ).getOrThrow()
            assertEquals(1, migratedRepo.observeProductAds(1L).first().size)
            assertTrue(migratedRepo.observeActiveShopAds().first().any { it.id == adId })
        } finally {
            migratedDb.close()
            context.deleteDatabase(dbName)
        }
    }

    @Test
    fun productAdOutroAndMultiPlatformShare_persistsGuideAndBuildsIntents() = runTest {
        repo.initializeDatabase()
        val farmer = repo.login("farmer@smartfarm.app", "farmer123").getOrThrow()
        val customOutro = "Google Play থেকে Smart Farm অ্যাপ নামান, ফ্রি অ্যাকাউন্ট খুলে বিকাশ/নগদ দিয়ে অর্ডার করুন!"
        val storyboard = BengaliAdStoryboard(
            productName = "দিনাজপুরের লিচু",
            farmName = "বরেন্দ্র এগ্রো",
            priceText = "৳250 / ১০০ পিস",
            hookHeadlineBn = "সরাসরি বাগান থেকে রসালো মিষ্টি লিচু!",
            promoBadgeBn = "সিজনাল ফ্রেশ",
            benefitsBn = listOf("ফরমালিনমুক্ত", "মিষ্টি স্বাদ", "সারা দেশে ডেলিভারি"),
            callToActionBn = "এখনই অর্ডার করুন • এখানে ক্লিক করুন",
            voiceoverScriptBn = "তাজা লিচু খেতে অ্যাপ ডাউনলোড করে অনলাইনে অর্ডার করুন!",
            orderStepsBn = BengaliAdStoryboard.defaultOrderSteps(),
            accentColorHex = "#2E7D32",
            downloadAndOrderOutroBn = customOutro
        )

        val entity = storyboard.toEntity(
            ownerUserId = farmer.id,
            farmId = 0L,
            inventoryItemId = null,
            imageUri = null,
            isActiveInShop = true
        )
        val savedId = repo.saveProductAd(entity, actorUserId = farmer.id).getOrThrow()
        val loaded = repo.observeProductAds(farmer.id).first().first { it.id == savedId }
        val roundtrip = BengaliAdStoryboard.fromEntity(loaded)

        assertEquals(customOutro, roundtrip.downloadAndOrderOutroBn)
        assertEquals(4, roundtrip.orderStepsBn.size)

        val caption = AdVideoMp4Exporter.buildShareCaption(roundtrip)
        assertTrue(caption.contains(customOutro))
        assertTrue(caption.contains("এখনই অর্ডার করুন"))

        val context = ApplicationProvider.getApplicationContext<Context>()
        val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val dummyMp4 = File(exportDir, "test_share_ad.mp4").apply { writeBytes(byteArrayOf(0, 1, 2, 3)) }
        try {
            SharePlatform.entries.forEach { platform ->
                val intent = AdVideoMp4Exporter.createTargetedShareVideoIntent(
                    context = context,
                    mp4File = dummyMp4,
                    storyboard = roundtrip,
                    platform = platform
                )
                assertNotNull(intent)
                val targetAction = if (intent.action == Intent.ACTION_CHOOSER) {
                    val inner = intent.getParcelableExtra<Intent>(Intent.EXTRA_INTENT)
                    assertNotNull(inner)
                    assertEquals("video/mp4", inner!!.type)
                    assertEquals(caption, inner.getStringExtra(Intent.EXTRA_TEXT))
                    intent.action
                } else {
                    assertEquals("video/mp4", intent.type)
                    assertEquals(caption, intent.getStringExtra(Intent.EXTRA_TEXT))
                    intent.action
                }
                assertTrue(targetAction == Intent.ACTION_SEND || targetAction == Intent.ACTION_CHOOSER)
            }
        } finally {
            dummyMp4.delete()
        }
    }
}
