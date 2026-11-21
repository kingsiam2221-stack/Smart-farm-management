package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.*
import com.example.data.repository.FarmRepository
import com.example.i18n.CurrencyFormatter
import com.example.i18n.CurrencyOption
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
}
