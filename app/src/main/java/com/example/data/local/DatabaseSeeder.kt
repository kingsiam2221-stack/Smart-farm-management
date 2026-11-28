package com.example.data.local

import kotlinx.coroutines.flow.first
import java.security.MessageDigest
import java.util.Locale

object PasswordHasher {
    fun hash(password: String, salt: String = "SmartFarmSalt2026"): String {
        val bytes = MessageDigest.getInstance("SHA-256")
            .digest("$salt:$password".toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { String.format(Locale.US, "%02x", it) }
    }

    fun verify(password: String, storedHash: String): Boolean {
        if (hash(password) == storedHash) return true
        val trimmed = password.trim()
        return trimmed != password && hash(trimmed) == storedHash
    }
}

object DatabaseSeeder {
    private const val DAY_MS = 86_400_000L

    suspend fun seedIfEmpty(db: SmartFarmDatabase) {
        val userDao = db.userDao()
        if (userDao.countUsers() > 0) {
            ensureDefaultEmployeePermissionsIfMissing(db)
            return
        }

        val now = System.currentTimeMillis()

        // 1. Seed Users (Farmer, Customer, Private Super Admin)
        val farmerId = userDao.insert(
            UserEntity(
                name = "Rahim Uddin",
                mobile = "+8801711223344",
                email = "farmer@smartfarm.app",
                passwordHash = PasswordHasher.hash("farmer123"),
                role = UserRole.FARMER.name,
                status = AccountStatus.ACTIVE.name,
                preferredLanguage = "en",
                preferredCurrency = "BDT",
                createdAt = now - 90 * DAY_MS
            )
        )
        val customerId = userDao.insert(
            UserEntity(
                name = "Nadia Rahman",
                mobile = "+8801811556677",
                email = "customer@smartfarm.app",
                passwordHash = PasswordHasher.hash("customer123"),
                role = UserRole.CUSTOMER.name,
                status = AccountStatus.ACTIVE.name,
                preferredLanguage = "en",
                preferredCurrency = "BDT",
                createdAt = now - 45 * DAY_MS
            )
        )
        val adminId = userDao.insert(
            UserEntity(
                name = "Platform Developer",
                mobile = "+8801900000001",
                email = "admin@smartfarm.app",
                passwordHash = PasswordHasher.hash("admin@2026"),
                role = UserRole.SUPER_ADMIN.name,
                status = AccountStatus.ACTIVE.name,
                preferredLanguage = "en",
                preferredCurrency = "BDT",
                createdAt = now - 120 * DAY_MS
            )
        )
        val empUser1Id = userDao.insert(
            UserEntity(
                name = "Karim Mia",
                mobile = "+8801722334455",
                email = "karim@smartfarm.app",
                passwordHash = PasswordHasher.hash("employee123"),
                role = UserRole.EMPLOYEE.name,
                status = AccountStatus.ACTIVE.name,
                preferredLanguage = "en",
                preferredCurrency = "BDT",
                employerOwnerId = farmerId,
                createdAt = now - 80 * DAY_MS
            )
        )
        val empUser2Id = userDao.insert(
            UserEntity(
                name = "Salma Khatun",
                mobile = "+8801733445566",
                email = "salma@smartfarm.app",
                passwordHash = PasswordHasher.hash("employee123"),
                role = UserRole.EMPLOYEE.name,
                status = AccountStatus.ACTIVE.name,
                preferredLanguage = "en",
                preferredCurrency = "BDT",
                employerOwnerId = farmerId,
                createdAt = now - 80 * DAY_MS
            )
        )

        // 2. Seed Built-in Animal Categories
        val animalDao = db.animalDao()
        val defaultCategories = listOf(
            "Cow" to false,
            "Buffalo" to false,
            "Goat" to false,
            "Sheep" to false,
            "Chicken" to true,
            "Duck" to true,
            "Turkey" to true,
            "Pigeon" to true,
            "Quail" to true,
            "Parrot" to true,
            "Other Birds" to true
        )
        defaultCategories.forEach { (name, isBird) ->
            animalDao.insertCategory(
                AnimalCategoryEntity(
                    ownerUserId = 0L,
                    name = name,
                    birdCategory = isBird,
                    builtIn = true
                )
            )
        }

        // 3. Seed 3 Distinct Farms for Farmer
        val farmDao = db.farmDao()
        val farm1Id = farmDao.insert(
            FarmEntity(
                ownerUserId = farmerId,
                farmCode = "FM-1001",
                name = "Green Valley Poultry",
                location = "Gazipur, Dhaka",
                address = "Plot 14, Bhawal Agro Zone, Gazipur",
                category = FarmCategory.POULTRY.name,
                openingDate = now - 180 * DAY_MS,
                managerName = "Karim Mia",
                managerPhone = "+8801722334455",
                initialInvestment = 240_000.0,
                bkashNumber = "01711-223344",
                nagadNumber = "01811-223344",
                bankAccountInfo = "Islami Bank • Green Valley Agro • A/C 2050-1122-3344"
            )
        )
        val farm2Id = farmDao.insert(
            FarmEntity(
                ownerUserId = farmerId,
                farmCode = "FM-1002",
                name = "Padma Dairy & Cattle",
                location = "Sirajganj",
                address = "Riverview Dairy Road, Sirajganj",
                category = FarmCategory.DAIRY.name,
                openingDate = now - 300 * DAY_MS,
                managerName = "Salma Khatun",
                managerPhone = "+8801733445566",
                initialInvestment = 620_000.0,
                bkashNumber = "01711-223344",
                nagadNumber = "01811-223344",
                bankAccountInfo = "Dutch-Bangla Bank • Padma Dairy • A/C 118-110-445566"
            )
        )
        val farm3Id = farmDao.insert(
            FarmEntity(
                ownerUserId = farmerId,
                farmCode = "FM-1003",
                name = "Sunbeam Quail & Duck",
                location = "Bogura",
                address = "Sector 3, Sherpur Road, Bogura",
                category = FarmCategory.MIXED.name,
                openingDate = now - 95 * DAY_MS,
                managerName = "Tariq Hasan",
                managerPhone = "+8801744556677",
                initialInvestment = 135_000.0,
                bkashNumber = "01744-556677",
                nagadNumber = "01944-556677",
                bankAccountInfo = "Sonali Bank • Sunbeam Hatchery • A/C 0041-889911"
            )
        )

        // 4. Seed Livestock & Birds
        val chickenId = animalDao.insertAnimal(
            AnimalRecordEntity(
                farmId = farm1Id,
                ownerUserId = farmerId,
                name = "Layer Flock A (ISA Brown)",
                category = "Chicken",
                birdCategory = true,
                tagNumber = "CHK-A01",
                breed = "ISA Brown",
                gender = "Female",
                dobOrAge = "6 months",
                purchaseDate = now - 120 * DAY_MS,
                purchasePrice = 115_000.0,
                quantity = 320,
                healthStatus = "Healthy",
                notes = "High laying rate (~88%)"
            )
        )
        val cowId = animalDao.insertAnimal(
            AnimalRecordEntity(
                farmId = farm2Id,
                ownerUserId = farmerId,
                name = "Holstein Friesian Herd",
                category = "Cow",
                birdCategory = false,
                tagNumber = "COW-HF12",
                breed = "Holstein Friesian",
                gender = "Female",
                dobOrAge = "3.5 years",
                purchaseDate = now - 260 * DAY_MS,
                purchasePrice = 480_000.0,
                quantity = 12,
                healthStatus = "Healthy",
                notes = "Daily milk yield 180L total"
            )
        )
        val goatId = animalDao.insertAnimal(
            AnimalRecordEntity(
                farmId = farm2Id,
                ownerUserId = farmerId,
                name = "Black Bengal Goats",
                category = "Goat",
                birdCategory = false,
                tagNumber = "GOT-BB24",
                breed = "Black Bengal",
                gender = "Mixed",
                dobOrAge = "1.2 years",
                purchaseDate = now - 150 * DAY_MS,
                purchasePrice = 96_000.0,
                quantity = 24,
                healthStatus = "Healthy",
                notes = "PPR vaccinated"
            )
        )
        val duckId = animalDao.insertAnimal(
            AnimalRecordEntity(
                farmId = farm3Id,
                ownerUserId = farmerId,
                name = "Khaki Campbell Ducks",
                category = "Duck",
                birdCategory = true,
                tagNumber = "DCK-KC01",
                breed = "Khaki Campbell",
                gender = "Female",
                dobOrAge = "7 months",
                purchaseDate = now - 90 * DAY_MS,
                purchasePrice = 42_000.0,
                quantity = 140,
                healthStatus = "Healthy",
                notes = "Pond-side free range"
            )
        )
        val quailId = animalDao.insertAnimal(
            AnimalRecordEntity(
                farmId = farm3Id,
                ownerUserId = farmerId,
                name = "Japanese Quail Batch",
                category = "Quail",
                birdCategory = true,
                tagNumber = "QUL-JP02",
                breed = "Coturnix",
                gender = "Mixed",
                dobOrAge = "9 weeks",
                purchaseDate = now - 50 * DAY_MS,
                purchasePrice = 24_000.0,
                quantity = 400,
                healthStatus = "Healthy",
                notes = "Peak egg production"
            )
        )

        animalDao.insertEvent(
            AnimalEventEntity(
                animalRecordId = cowId,
                farmId = farm2Id,
                ownerUserId = farmerId,
                eventType = AnimalEventType.VACCINATION.name,
                quantityDelta = 0,
                costOrValue = 3_600.0,
                description = "FMD Booster Vaccination for 12 cattle",
                eventDate = now - 12 * DAY_MS
            )
        )
        animalDao.insertEvent(
            AnimalEventEntity(
                animalRecordId = chickenId,
                farmId = farm1Id,
                ownerUserId = farmerId,
                eventType = AnimalEventType.FEEDING.name,
                quantityDelta = 0,
                costOrValue = 14_500.0,
                description = "Layer Mash 500kg ration update",
                eventDate = now - 3 * DAY_MS
            )
        )
        animalDao.insertEvent(
            AnimalEventEntity(
                animalRecordId = goatId,
                farmId = farm2Id,
                ownerUserId = farmerId,
                eventType = AnimalEventType.BIRTH.name,
                quantityDelta = 3,
                costOrValue = 0.0,
                description = "3 healthy kids born in Pen B",
                eventDate = now - 6 * DAY_MS
            )
        )
        animalDao.insertEvent(
            AnimalEventEntity(
                animalRecordId = duckId,
                farmId = farm3Id,
                ownerUserId = farmerId,
                eventType = AnimalEventType.VACCINATION.name,
                quantityDelta = 0,
                costOrValue = 1_800.0,
                description = "Duck Plague vaccine completed",
                eventDate = now - 20 * DAY_MS
            )
        )
        animalDao.insertEvent(
            AnimalEventEntity(
                animalRecordId = quailId,
                farmId = farm3Id,
                ownerUserId = farmerId,
                eventType = AnimalEventType.PURCHASE.name,
                quantityDelta = 400,
                costOrValue = 24_000.0,
                description = "Initial Coturnix batch stocked",
                eventDate = now - 50 * DAY_MS
            )
        )

        // 5. Seed Egg Production
        val eggDao = db.eggDao()
        eggDao.insert(
            EggProductionEntity(
                farmId = farm1Id,
                ownerUserId = farmerId,
                eggCategory = "Chicken Eggs",
                productionDate = now - 2 * DAY_MS,
                previousStock = 420,
                producedCount = 285,
                brokenCount = 5,
                soldCount = 200,
                availableCount = 500,
                unitSellingPrice = 12.0,
                totalValue = 285 * 12.0
            )
        )
        eggDao.insert(
            EggProductionEntity(
                farmId = farm1Id,
                ownerUserId = farmerId,
                eggCategory = "Chicken Eggs",
                productionDate = now - 1 * DAY_MS,
                previousStock = 500,
                producedCount = 290,
                brokenCount = 4,
                soldCount = 180,
                availableCount = 606,
                unitSellingPrice = 12.0,
                totalValue = 290 * 12.0
            )
        )
        eggDao.insert(
            EggProductionEntity(
                farmId = farm3Id,
                ownerUserId = farmerId,
                eggCategory = "Duck Eggs",
                productionDate = now - 1 * DAY_MS,
                previousStock = 140,
                producedCount = 112,
                brokenCount = 2,
                soldCount = 60,
                availableCount = 190,
                unitSellingPrice = 15.0,
                totalValue = 112 * 15.0
            )
        )
        eggDao.insert(
            EggProductionEntity(
                farmId = farm3Id,
                ownerUserId = farmerId,
                eggCategory = "Quail Eggs",
                productionDate = now,
                previousStock = 300,
                producedCount = 340,
                brokenCount = 6,
                soldCount = 120,
                availableCount = 514,
                unitSellingPrice = 4.5,
                totalValue = 340 * 4.5
            )
        )

        // 6. Seed Inventory (also some listed in Marketplace for Customers!)
        val invDao = db.inventoryDao()
        val invEggsId = invDao.insertItem(
            InventoryItemEntity(
                farmId = farm1Id,
                ownerUserId = farmerId,
                name = "Organic Brown Chicken Eggs (Tray 30)",
                category = "Eggs",
                quantity = 42.0,
                unit = "trays",
                unitCost = 290.0,
                sellingPrice = 360.0,
                lowStockThreshold = 10.0,
                expiryDate = now + 18 * DAY_MS,
                supplierName = "Green Valley Coop",
                supplierPhone = "+8801722334455",
                listInMarketplace = true,
                description = "Farm-fresh free-range brown eggs packed in hygienic 30-egg trays.",
                sellerBkashNumber = "01711-223344",
                sellerNagadNumber = "01811-223344",
                sellerBankAccount = "Islami Bank • Green Valley Agro • A/C 2050-1122-3344"
            )
        )
        val invMilkId = invDao.insertItem(
            InventoryItemEntity(
                farmId = farm2Id,
                ownerUserId = farmerId,
                name = "Pure Holstein Cow Milk (1L Bottle)",
                category = "Milk",
                quantity = 95.0,
                unit = "L",
                unitCost = 58.0,
                sellingPrice = 85.0,
                lowStockThreshold = 20.0,
                expiryDate = now + 3 * DAY_MS,
                supplierName = "Padma Chilling Unit",
                supplierPhone = "+8801733445566",
                listInMarketplace = true,
                description = "Chilled unprocessed whole cow milk bottled fresh every morning.",
                sellerBkashNumber = "01711-223344",
                sellerNagadNumber = "01811-223344",
                sellerBankAccount = "Dutch-Bangla Bank • Padma Dairy • A/C 118-110-445566"
            )
        )
        invDao.insertItem(
            InventoryItemEntity(
                farmId = farm3Id,
                ownerUserId = farmerId,
                name = "Fresh Quail Eggs Pack (24 pcs)",
                category = "Eggs",
                quantity = 30.0,
                unit = "packs",
                unitCost = 75.0,
                sellingPrice = 110.0,
                lowStockThreshold = 8.0,
                expiryDate = now + 20 * DAY_MS,
                supplierName = "Sunbeam Hatchery",
                supplierPhone = "+8801744556677",
                listInMarketplace = true,
                description = "Protein-rich speckled quail eggs from healthy Coturnix birds.",
                sourcingType = ProductSourcingType.FARM_PRODUCED.key,
                sellerBkashNumber = "01744-556677",
                sellerNagadNumber = "01944-556677",
                sellerBankAccount = "Sonali Bank • Sunbeam Hatchery • A/C 0041-889911"
            )
        )
        invDao.insertItem(
            InventoryItemEntity(
                farmId = farm1Id,
                ownerUserId = farmerId,
                name = "Miniket Premium Rice (5kg Pack)",
                category = "Rice",
                quantity = 120.0,
                unit = "pack",
                unitCost = 340.0,
                sellingPrice = 420.0,
                lowStockThreshold = 15.0,
                supplierName = "Green Valley Agro Harvest",
                supplierPhone = "+8801722334455",
                listInMarketplace = true,
                description = "Clean, aged Dinajpur Miniket rice harvested and packed directly from partner fields.",
                sourcingType = ProductSourcingType.FARM_PRODUCED.key,
                sellerBkashNumber = "01711-223344",
                sellerNagadNumber = "01811-223344",
                sellerBankAccount = "Islami Bank • Green Valley Agro • A/C 2050-1122-3344"
            )
        )
        invDao.insertItem(
            InventoryItemEntity(
                farmId = farm3Id,
                ownerUserId = farmerId,
                name = "Deshi Pure Mustard Seeds & Oil (1L)",
                category = "Mustard",
                quantity = 65.0,
                unit = "liter",
                unitCost = 210.0,
                sellingPrice = 285.0,
                lowStockThreshold = 10.0,
                supplierName = "Bogura Farmers Cooperative",
                supplierPhone = "+8801744556677",
                listInMarketplace = true,
                description = "Cold-pressed mustard oil and fresh mustard seeds sourced from Bogura growers for resale.",
                sourcingType = ProductSourcingType.PURCHASED_RESALE.key,
                sellerBkashNumber = "01744-556677",
                sellerNagadNumber = "01944-556677",
                sellerBankAccount = "Sonali Bank • Sunbeam Hatchery • A/C 0041-889911"
            )
        )
        invDao.insertItem(
            InventoryItemEntity(
                farmId = farm1Id,
                ownerUserId = farmerId,
                name = "Layer Grower Poultry Feed",
                category = "Poultry Feed",
                quantity = 18.0,
                unit = "bags (50kg)",
                unitCost = 2_850.0,
                sellingPrice = 0.0,
                lowStockThreshold = 15.0,
                expiryDate = now + 60 * DAY_MS,
                supplierName = "Nourish Agro Feeds",
                supplierPhone = "+8801911220099",
                listInMarketplace = false,
                description = "Balanced 17% protein layer crumble."
            )
        )
        invDao.insertItem(
            InventoryItemEntity(
                farmId = farm2Id,
                ownerUserId = farmerId,
                name = "FMD Cattle Vaccine Vials",
                category = "Vaccines",
                quantity = 6.0,
                unit = "vials",
                unitCost = 650.0,
                sellingPrice = 0.0,
                lowStockThreshold = 8.0, // Triggers Low Stock alert!
                expiryDate = now + 14 * DAY_MS,
                supplierName = "AgroVet Pharma",
                supplierPhone = "+8801812003344",
                listInMarketplace = false,
                description = "Cold-chain stored cattle vaccine."
            )
        )

        // 7. Seed Customers & Sales
        val commerceDao = db.commerceDao()
        val cust1Id = commerceDao.insertCustomer(
            CustomerEntity(
                ownerUserId = farmerId,
                customerCode = "CUS-201",
                name = "Dhaka Fresh Mart",
                mobile = "+8801755667788",
                email = "orders@dhakafresh.com",
                address = "House 21, Road 4, Banani, Dhaka"
            )
        )
        val cust2Id = commerceDao.insertCustomer(
            CustomerEntity(
                ownerUserId = farmerId,
                customerCode = "CUS-202",
                name = "Nadia Rahman",
                mobile = "+8801811556677",
                email = "customer@smartfarm.app",
                address = "Apt 5B, Dhanmondi 27, Dhaka"
            )
        )

        commerceDao.insertSale(
            SaleEntity(
                invoiceNumber = "INV-2026-101",
                farmId = farm1Id,
                ownerUserId = farmerId,
                customerId = cust1Id,
                customerName = "Dhaka Fresh Mart",
                customerPhone = "+8801755667788",
                productCategory = "Eggs",
                productName = "Organic Brown Chicken Eggs (Tray 30)",
                inventoryItemId = invEggsId,
                quantity = 25.0,
                unitPrice = 360.0,
                discount = 200.0,
                totalAmount = 8_800.0,
                paidAmount = 7_500.0,
                dueAmount = 1_300.0,
                paymentMethod = "bKash",
                saleDate = now - 5 * DAY_MS
            )
        )
        commerceDao.insertSale(
            SaleEntity(
                invoiceNumber = "INV-2026-102",
                farmId = farm2Id,
                ownerUserId = farmerId,
                customerId = cust2Id,
                customerName = "Nadia Rahman",
                customerPhone = "+8801811556677",
                productCategory = "Milk",
                productName = "Pure Holstein Cow Milk (1L Bottle)",
                inventoryItemId = invMilkId,
                quantity = 40.0,
                unitPrice = 85.0,
                discount = 0.0,
                totalAmount = 3_400.0,
                paidAmount = 3_400.0,
                dueAmount = 0.0,
                paymentMethod = "Nagad",
                saleDate = now - 2 * DAY_MS
            )
        )
        commerceDao.insertSale(
            SaleEntity(
                invoiceNumber = "INV-2026-103",
                farmId = farm2Id,
                ownerUserId = farmerId,
                customerId = cust1Id,
                customerName = "Dhaka Fresh Mart",
                customerPhone = "+8801755667788",
                productCategory = "Goats",
                productName = "Black Bengal Goat (Market Ready)",
                animalRecordId = goatId,
                quantity = 2.0,
                unitPrice = 11_500.0,
                discount = 500.0,
                totalAmount = 22_500.0,
                paidAmount = 22_500.0,
                dueAmount = 0.0,
                paymentMethod = "Cash",
                saleDate = now - 14 * DAY_MS
            )
        )

        // 8. Seed Online Customer Orders
        commerceDao.insertOrder(
            OrderEntity(
                orderNumber = "ORD-8801",
                customerUserId = customerId,
                customerName = "Nadia Rahman",
                customerPhone = "+8801811556677",
                deliveryAddress = "Apt 5B, Road 9, Dhanmondi 27, PS: Dhanmondi, District: Dhaka",
                deliveryDistrict = "Dhaka",
                deliveryPoliceStation = "Dhanmondi",
                deliveryLandmark = "Near Rapa Plaza",
                paymentReference = "BK-880192",
                farmId = farm1Id,
                farmOwnerUserId = farmerId,
                farmName = "Green Valley Poultry",
                itemsSummary = "2x Organic Brown Chicken Eggs (Tray 30)",
                serializedItems = "$invEggsId:2.0:360.0:Organic Brown Chicken Eggs (Tray 30)",
                totalAmount = 720.0,
                paymentMethod = "bKash",
                status = OrderStatus.PENDING.name,
                createdAt = now - DAY_MS / 2
            )
        )
        commerceDao.insertOrder(
            OrderEntity(
                orderNumber = "ORD-8800",
                customerUserId = customerId,
                customerName = "Nadia Rahman",
                customerPhone = "+8801811556677",
                deliveryAddress = "Apt 5B, Road 9, Dhanmondi 27, PS: Dhanmondi, District: Dhaka",
                deliveryDistrict = "Dhaka",
                deliveryPoliceStation = "Dhanmondi",
                deliveryLandmark = "Near Rapa Plaza",
                paymentReference = "COD",
                farmId = farm2Id,
                farmOwnerUserId = farmerId,
                farmName = "Padma Dairy & Cattle",
                itemsSummary = "5x Pure Holstein Cow Milk (1L Bottle)",
                serializedItems = "$invMilkId:5.0:85.0:Pure Holstein Cow Milk (1L Bottle)",
                totalAmount = 425.0,
                paymentMethod = "Cash on Delivery",
                status = OrderStatus.DELIVERED.name,
                createdAt = now - 4 * DAY_MS
            )
        )

        // 9. Seed Expenses & Employees
        val opsDao = db.operationsDao()
        opsDao.insertExpense(
            ExpenseEntity(
                farmId = farm1Id,
                ownerUserId = farmerId,
                category = "Food & Feed",
                amount = 14_250.0,
                description = "5 bags Layer Grower Feed",
                expenseDate = now - 6 * DAY_MS
            )
        )
        opsDao.insertExpense(
            ExpenseEntity(
                farmId = farm2Id,
                ownerUserId = farmerId,
                category = "Vaccination",
                amount = 3_600.0,
                description = "FMD Booster shots for cattle",
                expenseDate = now - 12 * DAY_MS
            )
        )
        opsDao.insertExpense(
            ExpenseEntity(
                farmId = farm2Id,
                ownerUserId = farmerId,
                category = "Electricity",
                amount = 2_450.0,
                description = "Dairy chiller & water pump power bill",
                expenseDate = now - 8 * DAY_MS
            )
        )

        val emp1Id = opsDao.insertEmployee(
            EmployeeEntity(
                farmId = farm1Id,
                ownerUserId = farmerId,
                name = "Karim Mia",
                phone = "+8801722334455",
                designation = "Farm Manager",
                salary = 15_000.0,
                joiningDate = now - 180 * DAY_MS,
                canManageInventory = true,
                canManageSales = true,
                canManageLivestock = true,
                lastAttendanceStatus = AttendanceStatus.PRESENT.name,
                email = "karim@smartfarm.app",
                linkedUserId = empUser1Id
            )
        )
        val emp2Id = opsDao.insertEmployee(
            EmployeeEntity(
                farmId = farm2Id,
                ownerUserId = farmerId,
                name = "Salma Khatun",
                phone = "+8801733445566",
                designation = "Dairy Supervisor",
                salary = 12_500.0,
                joiningDate = now - 200 * DAY_MS,
                canManageInventory = true,
                canManageSales = false,
                canManageLivestock = true,
                lastAttendanceStatus = AttendanceStatus.PRESENT.name,
                email = "salma@smartfarm.app",
                linkedUserId = empUser2Id
            )
        )
        opsDao.insertSalaryPayment(
            SalaryPaymentEntity(
                employeeId = emp1Id,
                farmId = farm1Id,
                ownerUserId = farmerId,
                amount = 15_000.0,
                monthLabel = "October 2026",
                paymentDate = now - 15 * DAY_MS,
                note = "Monthly salary paid via bKash",
                paymentType = EmployeePaymentType.SALARY.key
            )
        )
        opsDao.insertSalaryPayment(
            SalaryPaymentEntity(
                employeeId = emp1Id,
                farmId = farm1Id,
                ownerUserId = farmerId,
                amount = 500.0,
                monthLabel = "Festival & Attendance Bonus",
                paymentDate = now - 8 * DAY_MS,
                note = "Zero late check-ins bonus",
                paymentType = EmployeePaymentType.BONUS.key
            )
        )
        opsDao.insertSalaryPayment(
            SalaryPaymentEntity(
                employeeId = emp2Id,
                farmId = farm2Id,
                ownerUserId = farmerId,
                amount = 350.0,
                monthLabel = "Transport & Chiller Expense",
                paymentDate = now - 3 * DAY_MS,
                note = "Local transport for chilled milk samples",
                paymentType = EmployeePaymentType.EXPENSE.key
            )
        )

        // Seed Morning-to-Night Employee Task Targets & Automated Messages
        opsDao.insertTaskTarget(
            EmployeeTaskTargetEntity(
                ownerUserId = farmerId,
                farmId = farm1Id,
                employeeId = emp1Id,
                employeeName = "Karim Mia",
                timeSlot = EmployeeTaskTimeSlot.MORNING.key,
                title = "Morning Feed & Fresh Water Check",
                description = "Distribute morning layer ration & clean all poultry drinkers in Shed A & B.",
                targetGoalText = "100% sheds fed before 8:30 AM",
                rewardBonusAmount = 10.0,
                status = EmployeeTaskStatus.PENDING.key,
                targetDateMillis = now,
                createdAt = now - 3 * 3600_000L
            )
        )
        opsDao.insertTaskTarget(
            EmployeeTaskTargetEntity(
                ownerUserId = farmerId,
                farmId = farm1Id,
                employeeId = emp1Id,
                employeeName = "Karim Mia",
                timeSlot = EmployeeTaskTimeSlot.AFTERNOON.key,
                title = "Egg Collection & Tray Grading",
                description = "Collect afternoon eggs, separate cracked eggs, and log exact tray stock.",
                targetGoalText = "280+ eggs logged cleanly",
                rewardBonusAmount = 20.0,
                status = EmployeeTaskStatus.PENDING.key,
                targetDateMillis = now,
                createdAt = now - 2 * 3600_000L
            )
        )
        opsDao.insertTaskTarget(
            EmployeeTaskTargetEntity(
                ownerUserId = farmerId,
                farmId = farm1Id,
                employeeId = emp1Id,
                employeeName = "Karim Mia",
                timeSlot = EmployeeTaskTimeSlot.NIGHT.key,
                title = "Night Coop Security & Light Off",
                description = "Verify coop locks, check brooder temperature, and submit night report.",
                targetGoalText = "Night check completed by 9:30 PM",
                rewardBonusAmount = 20.0,
                status = EmployeeTaskStatus.PENDING.key,
                targetDateMillis = now,
                createdAt = now - 3600_000L
            )
        )
        opsDao.insertTaskTarget(
            EmployeeTaskTargetEntity(
                ownerUserId = farmerId,
                farmId = farm2Id,
                employeeId = emp2Id,
                employeeName = "Salma Khatun",
                timeSlot = EmployeeTaskTimeSlot.MORNING.key,
                title = "Morning Milking & Chiller Log",
                description = "Complete morning milking for 12 cows and record milk temperature in chiller.",
                targetGoalText = "All bottles sealed & cooled",
                rewardBonusAmount = 20.0,
                status = EmployeeTaskStatus.PENDING.key,
                targetDateMillis = now,
                createdAt = now - 2 * 3600_000L
            )
        )

        opsDao.insertAutoMessage(
            EmployeeAutoMessageEntity(
                ownerUserId = farmerId,
                farmId = farm1Id,
                employeeId = emp1Id,
                senderUserId = empUser1Id,
                senderName = "Karim Mia",
                senderRole = UserRole.EMPLOYEE.name,
                recipientLabel = "Rahim Uddin (Farm Owner)",
                messageType = AutoMessageType.MORNING_CHECKIN.key,
                subject = "Morning Check-In — Green Valley Poultry",
                messageBody = "Shed A & B opened at 6:15 AM. Water lines clean and birds active.",
                isAutoGenerated = true,
                sentAt = now - 2 * 3600_000L
            )
        )
        opsDao.insertAutoMessage(
            EmployeeAutoMessageEntity(
                ownerUserId = farmerId,
                farmId = farm2Id,
                employeeId = emp2Id,
                senderUserId = empUser2Id,
                senderName = "Salma Khatun",
                senderRole = UserRole.EMPLOYEE.name,
                recipientLabel = "Rahim Uddin (Farm Owner)",
                messageType = AutoMessageType.MIDDAY_UPDATE.key,
                subject = "Midday Dairy Update — Padma Dairy",
                messageBody = "95L milk bottled and chilled at 4°C. Ready for customer dispatch.",
                isAutoGenerated = true,
                sentAt = now - 3600_000L
            )
        )

        // Seed Granular Employee Permissions
        val permDao = db.permissionDao()
        permDao.upsertPermissions(
            AppPermission.entries.map { perm ->
                EmployeePermissionEntity(
                    employeeId = emp1Id,
                    ownerUserId = farmerId,
                    permissionKey = perm.key,
                    granted = PermissionPreset.MANAGER.keys.contains(perm.key)
                )
            }
        )
        permDao.upsertPermissions(
            AppPermission.entries.map { perm ->
                EmployeePermissionEntity(
                    employeeId = emp2Id,
                    ownerUserId = farmerId,
                    permissionKey = perm.key,
                    granted = PermissionPreset.POULTRY_WORKER.keys.contains(perm.key)
                )
            }
        )

        // 10. Seed Subscription Plans, User Subscriptions, Notifications, Support, Audit & Settings
        val subDao = db.subscriptionAndAdminDao()
        val monthlyPlanId = subDao.insertPlan(
            SubscriptionPlanEntity(
                name = "Monthly Plan",
                priceBaseBdt = 50.0,
                durationDays = 30,
                description = "Full multi-farm access for 30 days",
                featuresSummary = "Unlimited Farms • Livestock & Egg Tracking • Sales & PDF Invoices • Online Storefront",
                active = true
            )
        )
        val yearlyPlanId = subDao.insertPlan(
            SubscriptionPlanEntity(
                name = "Yearly Plan",
                priceBaseBdt = 100.0,
                durationDays = 365,
                description = "Best value annual digital farming suite",
                featuresSummary = "Everything in Monthly • Priority Support • 12 Months Uninterrupted Access",
                active = true
            )
        )

        subDao.insertUserSubscription(
            UserSubscriptionEntity(
                userId = farmerId,
                userName = "Rahim Uddin",
                planId = yearlyPlanId,
                planName = "Yearly Plan",
                pricePaidBdt = 100.0,
                currencyCode = "BDT",
                paymentMethod = "bKash",
                transactionRef = "BK-9948120",
                paymentStatus = PaymentStatus.SUCCESSFUL.name,
                startDate = now - 30 * DAY_MS,
                expiryDate = now + 335 * DAY_MS,
                status = "ACTIVE"
            )
        )
        subDao.insertUserSubscription(
            UserSubscriptionEntity(
                userId = farmerId,
                userName = "Rahim Uddin",
                planId = monthlyPlanId,
                planName = "Monthly Plan",
                pricePaidBdt = 50.0,
                currencyCode = "BDT",
                paymentMethod = "Nagad",
                transactionRef = "NG-4412011",
                paymentStatus = PaymentStatus.SUCCESSFUL.name,
                startDate = now - 60 * DAY_MS,
                expiryDate = now - 30 * DAY_MS,
                status = "EXPIRED"
            )
        )

        subDao.insertNotification(
            AppNotificationEntity(
                targetUserId = farmerId,
                title = "New Online Order ORD-8801",
                message = "Nadia Rahman placed an order for 2 trays of Organic Brown Chicken Eggs.",
                category = "ORDER",
                createdAt = now - DAY_MS / 2
            )
        )
        subDao.insertNotification(
            AppNotificationEntity(
                targetUserId = farmerId,
                title = "Low Inventory Alert",
                message = "FMD Cattle Vaccine Vials (6 vials left) is below threshold of 8 vials.",
                category = "INVENTORY",
                createdAt = now - 3600_000L
            )
        )
        subDao.insertNotification(
            AppNotificationEntity(
                targetUserId = farmerId,
                title = "Outstanding Customer Balance",
                message = "Dhaka Fresh Mart has ৳1,300 due on INV-2026-101.",
                category = "DUE",
                createdAt = now - 7200_000L
            )
        )
        subDao.insertNotification(
            AppNotificationEntity(
                targetUserId = customerId,
                title = "Order ORD-8800 Delivered",
                message = "Your order of 5L Pure Holstein Cow Milk has been delivered.",
                category = "ORDER",
                createdAt = now - 4 * DAY_MS
            )
        )

        subDao.insertSupport(
            SupportRequestEntity(
                userId = farmerId,
                userName = "Rahim Uddin",
                subject = "SSLCommerz merchant verification",
                message = "Please enable automated bKash callback for our Gazipur outlet.",
                status = "OPEN",
                createdAt = now - 2 * DAY_MS
            )
        )

        subDao.insertAuditLog(
            AdminAuditLogEntity(
                actorUserId = adminId,
                actorName = "Platform Developer",
                action = "PLAN_INITIALIZED",
                target = "Monthly Plan (50 BDT) & Yearly Plan (100 BDT)",
                details = "Initial subscription plans configured on platform.",
                createdAt = now - 60 * DAY_MS
            )
        )

        // Seed Agriculture Lands, Crops (Rice, Wheat, Corn, Cattle Feed) & Sector Expenses
        val agriDao = db.agriDao()
        val land1Id = agriDao.insertLand(
            AgriLandEntity(
                ownerUserId = farmerId,
                farmId = farm2Id,
                name = "North Paddy Field (Plot A)",
                areaSize = 65.0,
                areaUnit = AgriLandUnit.DECIMAL.key,
                location = "Bhawal North Block",
                soilOrLandType = "Alluvial Loam",
                notes = "Main irrigated rice & wheat land"
            )
        )
        val land2Id = agriDao.insertLand(
            AgriLandEntity(
                ownerUserId = farmerId,
                farmId = farm2Id,
                name = "Riverbank Fodder & Corn Plot",
                areaSize = 45.0,
                areaUnit = AgriLandUnit.DECIMAL.key,
                location = "Sirajganj Riverbank",
                soilOrLandType = "Sandy Loam",
                notes = "Napier cattle fodder, corn & wheat rotation"
            )
        )
        val riceCropId = agriDao.insertCrop(
            AgriCropEntity(
                ownerUserId = farmerId,
                farmId = farm2Id,
                landId = land1Id,
                cropType = AgriCropType.RICE.key,
                title = "BRRI Dhan-28 (Boro Rice)",
                variety = "BRRI-28",
                season = AgriSeason.BORO.key,
                plantingDate = now - 110 * DAY_MS,
                status = AgriCropStatus.HARVESTED.key,
                yieldQuantity = 38.0,
                yieldUnit = "mon",
                marketPricePerUnit = 1_350.0,
                yieldMarketWorth = 51_300.0,
                soldAmount = 51_300.0,
                notes = "Strong yield with balanced potash & urea"
            )
        )
        val fodderCropId = agriDao.insertCrop(
            AgriCropEntity(
                ownerUserId = farmerId,
                farmId = farm2Id,
                landId = land2Id,
                cropType = AgriCropType.CATTLE_FEED.key,
                title = "Pakchong Napier Cattle Feed",
                variety = "Pakchong-1",
                season = AgriSeason.YEAR_ROUND.key,
                plantingDate = now - 75 * DAY_MS,
                status = AgriCropStatus.HARVESTED.key,
                yieldQuantity = 3_200.0,
                yieldUnit = "kg",
                marketPricePerUnit = 10.0,
                yieldMarketWorth = 32_000.0,
                soldAmount = 0.0,
                notes = "Used for Dairy Cow herd (saved feed cost)"
            )
        )
        val cornCropId = agriDao.insertCrop(
            AgriCropEntity(
                ownerUserId = farmerId,
                farmId = farm2Id,
                landId = land2Id,
                cropType = AgriCropType.CORN.key,
                title = "Hybrid Yellow Corn",
                variety = "NK-40",
                season = AgriSeason.RABI.key,
                plantingDate = now - 35 * DAY_MS,
                status = AgriCropStatus.GROWING.key,
                yieldQuantity = 0.0,
                yieldUnit = "mon",
                marketPricePerUnit = 1_150.0,
                yieldMarketWorth = 0.0,
                soldAmount = 0.0,
                notes = "Growing phase — irrigation scheduled"
            )
        )
        val wheatCropId = agriDao.insertCrop(
            AgriCropEntity(
                ownerUserId = farmerId,
                farmId = farm3Id,
                landId = land1Id,
                cropType = AgriCropType.WHEAT.key,
                title = "BARI Gom-33 Wheat",
                variety = "BARI Gom-33",
                season = AgriSeason.RABI.key,
                plantingDate = now - 95 * DAY_MS,
                status = AgriCropStatus.SOLD.key,
                yieldQuantity = 22.0,
                yieldUnit = "mon",
                marketPricePerUnit = 1_500.0,
                yieldMarketWorth = 33_000.0,
                soldAmount = 34_200.0,
                notes = "Sold directly after threshing"
            )
        )
        listOf(
            AgriExpenseEntity(ownerUserId = farmerId, farmId = farm2Id, landId = land1Id, cropId = riceCropId, sector = AgriSector.TILLAGE.key, amount = 4_200.0, description = "Tractor plowing & leveling", expenseDate = now - 108 * DAY_MS),
            AgriExpenseEntity(ownerUserId = farmerId, farmId = farm2Id, landId = land1Id, cropId = riceCropId, sector = AgriSector.SEEDS.key, amount = 2_800.0, description = "BRRI-28 certified seedlings", expenseDate = now - 105 * DAY_MS),
            AgriExpenseEntity(ownerUserId = farmerId, farmId = farm2Id, landId = land1Id, cropId = riceCropId, sector = AgriSector.FERTILIZER.key, amount = 6_400.0, description = "Urea, TSP & MoP basal + top dress", expenseDate = now - 85 * DAY_MS),
            AgriExpenseEntity(ownerUserId = farmerId, farmId = farm2Id, landId = land1Id, cropId = riceCropId, sector = AgriSector.IRRIGATION.key, amount = 5_100.0, description = "Shallow pump irrigation cycles", expenseDate = now - 60 * DAY_MS),
            AgriExpenseEntity(ownerUserId = farmerId, farmId = farm2Id, landId = land1Id, cropId = riceCropId, sector = AgriSector.LABOR.key, amount = 8_500.0, description = "Transplanting, weeding & harvesting labor", expenseDate = now - 20 * DAY_MS),
            AgriExpenseEntity(ownerUserId = farmerId, farmId = farm2Id, landId = land1Id, cropId = wheatCropId, sector = AgriSector.SEEDS.key, amount = 3_100.0, description = "BARI Gom-33 seed", expenseDate = now - 94 * DAY_MS),
            AgriExpenseEntity(ownerUserId = farmerId, farmId = farm2Id, landId = land1Id, cropId = wheatCropId, sector = AgriSector.FERTILIZER.key, amount = 4_900.0, description = "DAP, Urea & Boron", expenseDate = now - 78 * DAY_MS),
            AgriExpenseEntity(ownerUserId = farmerId, farmId = farm2Id, landId = land1Id, cropId = wheatCropId, sector = AgriSector.HARVEST_TRANSPORT.key, amount = 4_400.0, description = "Threshing machine & bag transport", expenseDate = now - 15 * DAY_MS),
            AgriExpenseEntity(ownerUserId = farmerId, farmId = farm2Id, landId = land2Id, cropId = fodderCropId, sector = AgriSector.SEEDS.key, amount = 2_500.0, description = "Napier stem cuttings", expenseDate = now - 74 * DAY_MS),
            AgriExpenseEntity(ownerUserId = farmerId, farmId = farm2Id, landId = land2Id, cropId = fodderCropId, sector = AgriSector.LABOR.key, amount = 4_800.0, description = "Planting & green fodder cutting", expenseDate = now - 25 * DAY_MS),
            AgriExpenseEntity(ownerUserId = farmerId, farmId = farm2Id, landId = land2Id, cropId = fodderCropId, sector = AgriSector.FERTILIZER.key, amount = 3_200.0, description = "Organic cow-dung & urea", expenseDate = now - 45 * DAY_MS),
            AgriExpenseEntity(ownerUserId = farmerId, farmId = farm2Id, landId = land2Id, cropId = cornCropId, sector = AgriSector.SEEDS.key, amount = 2_400.0, description = "Hybrid NK-40 corn seed", expenseDate = now - 34 * DAY_MS),
            AgriExpenseEntity(ownerUserId = farmerId, farmId = farm2Id, landId = land2Id, cropId = cornCropId, sector = AgriSector.PESTICIDE.key, amount = 1_600.0, description = "Fall armyworm spray", expenseDate = now - 18 * DAY_MS),
            AgriExpenseEntity(ownerUserId = farmerId, farmId = farm2Id, landId = land2Id, cropId = null, sector = AgriSector.LAND_RENT.key, amount = 6_000.0, description = "Seasonal plot lease share", expenseDate = now - 80 * DAY_MS)
        ).forEach { agriDao.insertExpense(it) }

        subDao.saveSystemSettings(SystemSettingsEntity())
    }

    suspend fun ensureDefaultEmployeePermissionsIfMissing(db: SmartFarmDatabase) {
        val opsDao = db.operationsDao()
        val permDao = db.permissionDao()
        val userDao = db.userDao()
        val allEmployees: List<EmployeeEntity> = opsDao.observeAllEmployees().first()
        for (emp in allEmployees) {
            val existingPerms = permDao.getPermissionsForEmployee(emp.id)
            if (existingPerms.isEmpty()) {
                val keys = PermissionCatalog.defaultGrantedKeysFromLegacyFlags(emp)
                permDao.upsertPermissions(
                    AppPermission.entries.map { p ->
                        EmployeePermissionEntity(
                            employeeId = emp.id,
                            ownerUserId = emp.ownerUserId,
                            permissionKey = p.key,
                            granted = keys.contains(p.key)
                        )
                    }
                )
            } else {
                val existingByKey = existingPerms.associateBy { it.permissionKey }
                val missing = mutableListOf<EmployeePermissionEntity>()
                if (!existingByKey.containsKey(AppPermission.STAFF_TARGETS_VIEW.key)) {
                    missing += EmployeePermissionEntity(
                        employeeId = emp.id,
                        ownerUserId = emp.ownerUserId,
                        permissionKey = AppPermission.STAFF_TARGETS_VIEW.key,
                        granted = true
                    )
                }
                if (!existingByKey.containsKey(AppPermission.STAFF_HR_MANAGE.key)) {
                    missing += EmployeePermissionEntity(
                        employeeId = emp.id,
                        ownerUserId = emp.ownerUserId,
                        permissionKey = AppPermission.STAFF_HR_MANAGE.key,
                        granted = existingByKey[AppPermission.EMPLOYEES_MANAGE.key]?.granted ?: false
                    )
                }
                if (missing.isNotEmpty()) {
                    permDao.upsertPermissions(missing)
                }
            }
        }
        // Ensure seeded employee users link cleanly if Farmer exists and they don't exist yet
        val farmer = userDao.getUserByEmail("farmer@smartfarm.app") ?: return
        listOf(
            Triple("karim@smartfarm.app", "Karim Mia", "+8801722334455"),
            Triple("salma@smartfarm.app", "Salma Khatun", "+8801733445566")
        ).forEach { (email, name, phone) ->
            if (userDao.getUserByEmail(email) == null) {
                val matchedEmp = allEmployees.firstOrNull {
                    it.ownerUserId == farmer.id && it.name.equals(name, ignoreCase = true)
                }
                if (matchedEmp != null) {
                    val uid = userDao.insert(
                        UserEntity(
                            name = matchedEmp.name,
                            mobile = matchedEmp.phone.ifBlank { phone },
                            email = email,
                            passwordHash = PasswordHasher.hash("employee123"),
                            role = UserRole.EMPLOYEE.name,
                            status = AccountStatus.ACTIVE.name,
                            preferredLanguage = farmer.preferredLanguage,
                            preferredCurrency = farmer.preferredCurrency,
                            employerOwnerId = farmer.id
                        )
                    )
                    opsDao.updateEmployee(matchedEmp.copy(email = email, linkedUserId = uid))
                }
            }
        }
    }
}
