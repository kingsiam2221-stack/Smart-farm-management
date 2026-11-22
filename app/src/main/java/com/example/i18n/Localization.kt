package com.example.i18n

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import com.example.data.local.AnimalEventType
import com.example.data.local.AttendanceStatus
import com.example.data.local.FarmCategory
import com.example.data.local.OrderStatus
import com.example.data.local.PaymentStatus

val LocalAppLanguage = staticCompositionLocalOf { AppLanguage.EN }

@Composable
fun tr(en: String, bn: String): String =
    if (LocalAppLanguage.current == AppLanguage.BN) bn else en

fun trLang(lang: AppLanguage, en: String, bn: String): String =
    if (lang == AppLanguage.BN) bn else en

@Composable
fun isBangla(): Boolean = LocalAppLanguage.current == AppLanguage.BN

object DomainLocalization {
    fun farmCategory(catKey: String, customName: String = "", isBn: Boolean): String {
        if (catKey == FarmCategory.CUSTOM.name && customName.isNotBlank()) return customName
        val cat = FarmCategory.entries.find { it.name == catKey } ?: return customName.ifBlank { catKey }
        if (!isBn) return cat.label
        return when (cat) {
            FarmCategory.POULTRY -> "পোল্ট্রি / মুরগির খামার"
            FarmCategory.DAIRY -> "ডেইরি / দুগ্ধ খামার"
            FarmCategory.CATTLE -> "গবাদি পশু খামার"
            FarmCategory.GOAT -> "ছাগলের খামার"
            FarmCategory.DUCK -> "হাঁসের খামার"
            FarmCategory.BIRD -> "পাখির খামার"
            FarmCategory.QUAIL -> "কোয়েল খামার"
            FarmCategory.MIXED -> "মিশ্র খামার"
            FarmCategory.CUSTOM -> "কাস্টম খামার"
        }
    }

    fun animalCategory(name: String, isBn: Boolean): String {
        if (!isBn) return name
        return when (name.trim().lowercase()) {
            "cow" -> "গরু"
            "buffalo" -> "মহিষ"
            "goat" -> "ছাগল"
            "sheep" -> "ভেড়া"
            "chicken" -> "মুরগি"
            "duck" -> "হাঁস"
            "turkey" -> "টার্কি"
            "pigeon" -> "কবুতর"
            "quail" -> "কোয়েল"
            "parrot" -> "টিয়া / তোতা"
            "other birds" -> "অন্যান্য পাখি"
            else -> name
        }
    }

    fun animalEventType(type: AnimalEventType, isBn: Boolean): String {
        if (!isBn) return type.label
        return when (type) {
            AnimalEventType.BIRTH -> "জন্ম"
            AnimalEventType.PURCHASE -> "ক্রয়"
            AnimalEventType.DEATH -> "মৃত্যু"
            AnimalEventType.TRANSFER -> "স্থানান্তর"
            AnimalEventType.SALE -> "বিক্রয়"
            AnimalEventType.VACCINATION -> "টিকা প্রদান"
            AnimalEventType.FEEDING -> "খাদ্য প্রদান"
            AnimalEventType.MEDICAL -> "চিকিৎসা খরচ"
        }
    }

    fun animalEventKey(raw: String, isBn: Boolean): String {
        val t = AnimalEventType.entries.find { it.name == raw || it.label.equals(raw, ignoreCase = true) }
        return if (t != null) animalEventType(t, isBn) else raw
    }

    fun orderStatus(status: OrderStatus, isBn: Boolean): String {
        if (!isBn) return status.label
        return when (status) {
            OrderStatus.PENDING -> "অপেক্ষমাণ"
            OrderStatus.CONFIRMED -> "নিশ্চিত"
            OrderStatus.PROCESSING -> "প্রস্তুত হচ্ছে"
            OrderStatus.READY_FOR_DELIVERY -> "ডেলিভারির জন্য প্রস্তুত"
            OrderStatus.SHIPPED -> "পাঠানো হয়েছে"
            OrderStatus.DELIVERED -> "ডেলিভারি সম্পন্ন"
            OrderStatus.CANCELLED -> "বাতিল"
        }
    }

    fun paymentStatus(status: PaymentStatus, isBn: Boolean): String {
        if (!isBn) return status.label
        return when (status) {
            PaymentStatus.PENDING -> "অপেক্ষমাণ"
            PaymentStatus.SUCCESSFUL -> "সফল"
            PaymentStatus.FAILED -> "ব্যর্থ"
            PaymentStatus.REFUNDED -> "ফেরত (রিফান্ড)"
        }
    }

    fun attendanceStatus(status: AttendanceStatus, isBn: Boolean): String {
        if (!isBn) return status.label
        return when (status) {
            AttendanceStatus.PRESENT -> "উপস্থিত"
            AttendanceStatus.ABSENT -> "অনুপস্থিত"
            AttendanceStatus.LEAVE -> "ছুটি"
        }
    }

    fun period(periodKey: String, isBn: Boolean): String {
        if (!isBn) return periodKey
        return when (periodKey) {
            "All" -> "সব"
            "Daily" -> "দৈনিক"
            "Weekly" -> "সাপ্তাহিক"
            "Monthly" -> "মাসিক"
            "Yearly" -> "বাৎসরিক"
            else -> periodKey
        }
    }

    fun eggCategory(cat: String, isBn: Boolean): String {
        if (!isBn) return cat
        return when (cat) {
            "Chicken Eggs" -> "মুরগির ডিম"
            "Duck Eggs" -> "হাঁসের ডিম"
            "Quail Eggs" -> "কোয়েলের ডিম"
            "Pigeon Eggs" -> "কবুতরের ডিম"
            "Other Bird Eggs" -> "অন্যান্য পাখির ডিম"
            "Custom" -> "কাস্টম"
            "Custom Eggs" -> "কাস্টম ডিম"
            else -> cat
        }
    }

    fun productCategory(cat: String, isBn: Boolean): String {
        if (!isBn) return cat
        return when (cat) {
            "Eggs" -> "ডিম"
            "Milk" -> "দুধ"
            "Meat" -> "মাংস"
            "Live Animals" -> "জীবিত পশু"
            "Chickens" -> "মুরগি"
            "Ducks" -> "হাঁস"
            "Cows" -> "গরু"
            "Goats" -> "ছাগল"
            "Birds" -> "পাখি"
            "Animal Feed" -> "পশুর খাদ্য"
            "Poultry Feed" -> "মুরগির খাদ্য"
            "Medicines" -> "ওষুধ"
            "Vaccines" -> "টিকা"
            "Farm Equipment" -> "খামার সরঞ্জাম"
            "Other" -> "অন্যান্য"
            else -> animalCategory(cat, true)
        }
    }

    fun expenseCategory(cat: String, isBn: Boolean): String {
        if (!isBn) return cat
        return when (cat) {
            "Animal Purchase" -> "পশু ক্রয়"
            "Bird Purchase" -> "পাখি ক্রয়"
            "Food & Feed" -> "খাদ্য ও ফিড"
            "Medicine" -> "ওষুধ"
            "Vaccination" -> "টিকা প্রদান"
            "Employee Salary" -> "কর্মচারী বেতন"
            "Electricity" -> "বিদ্যুৎ বিল"
            "Water" -> "পানি বিল"
            "Transportation" -> "পরিবহন"
            "Equipment" -> "সরঞ্জাম"
            "Maintenance" -> "রক্ষণাবেক্ষণ"
            "Other" -> "অন্যান্য"
            else -> cat
        }
    }

    fun paymentMethod(method: String, isBn: Boolean): String {
        if (!isBn) return method
        return when (method) {
            "Cash" -> "নগদ (ক্যাশ)"
            "Bank Transfer" -> "ব্যাংক ট্রান্সফার"
            "Cash on Delivery" -> "ক্যাশ অন ডেলিভারি"
            else -> method
        }
    }

    fun healthStatus(status: String, isBn: Boolean): String {
        if (!isBn) return status
        return when (status.trim().lowercase()) {
            "healthy" -> "সুস্থ"
            "sick" -> "অসুস্থ"
            "under treatment" -> "চিকিৎসাধীন"
            "quarantined" -> "কোয়ারেন্টাইন"
            else -> status
        }
    }

    fun gender(gender: String, isBn: Boolean): String {
        if (!isBn) return gender
        return when (gender.trim().lowercase()) {
            "male" -> "পুরুষ / ষাঁড় / মোরগ"
            "female" -> "স্ত্রী / গাভী / মুরগি"
            "mixed" -> "মিশ্র"
            else -> gender
        }
    }

    fun subscriptionStatus(status: String, isBn: Boolean): String {
        if (!isBn) return status
        return when (status.uppercase()) {
            "ACTIVE" -> "সক্রিয়"
            "EXPIRED" -> "মেয়াদোত্তীর্ণ"
            "PENDING" -> "অপেক্ষমাণ"
            else -> status
        }
    }

    fun accountStatus(status: String, isBn: Boolean): String {
        if (!isBn) return status
        return when (status.uppercase()) {
            "ACTIVE" -> "সক্রিয়"
            "INACTIVE" -> "নিষ্ক্রিয়"
            "SUSPENDED" -> "স্থগিত"
            else -> status
        }
    }

    fun userRole(role: String, isBn: Boolean): String {
        if (!isBn) return role
        return when (role.uppercase()) {
            "FARMER" -> "খামারি"
            "CUSTOMER" -> "গ্রাহক"
            "SUPER_ADMIN" -> "সুপার অ্যাডমিন"
            else -> role
        }
    }
}

data class AppStrings(
    val appName: String,
    val dashboard: String,
    val farms: String,
    val livestock: String,
    val eggs: String,
    val operations: String,
    val commerce: String,
    val reports: String,
    val more: String,
    val marketplace: String,
    val cart: String,
    val myOrders: String,
    val profile: String,
    val adminOverview: String,
    val adminUsers: String,
    val adminPlans: String,
    val adminFinance: String,
    val adminSystem: String,
    val welcomeBack: String,
    val allFarms: String,
    val switchFarm: String,
    val totalFarms: String,
    val totalAnimals: String,
    val totalBirds: String,
    val eggsProduced: String,
    val eggsAvailable: String,
    val totalSales: String,
    val totalExpenses: String,
    val totalIncome: String,
    val outstandingPayments: String,
    val netProfitLoss: String,
    val addFarm: String,
    val addAnimal: String,
    val recordEggs: String,
    val newSale: String,
    val addExpense: String,
    val addInventory: String,
    val addEmployee: String,
    val addCustomer: String,
    val subscription: String,
    val notifications: String,
    val language: String,
    val currency: String,
    val themeMode: String,
    val darkMode: String,
    val lightMode: String,
    val logout: String,
    val login: String,
    val signUp: String,
    val forgotPassword: String,
    val resetPassword: String,
    val save: String,
    val cancel: String,
    val delete: String,
    val edit: String,
    val search: String,
    val noDataYet: String,
    val active: String,
    val expired: String,
    val pending: String,
    val exportPdf: String,
    val exportCsv: String,
    val monthlySales: String,
    val monthlyExpenses: String,
    val profitAndLoss: String,
    val eggProductionTrend: String,
    val farmPerformance: String,
    val animalGrowth: String,
    val checkout: String,
    val placeOrder: String,
    val orderHistory: String,
    val superAdminPortal: String
)

object Localization {
    val english = AppStrings(
        appName = "Smart Farm Management",
        dashboard = "Dashboard",
        farms = "Farms",
        livestock = "Livestock",
        eggs = "Eggs",
        operations = "Operations",
        commerce = "Sales & CRM",
        reports = "Reports",
        more = "More",
        marketplace = "Shop",
        cart = "Cart",
        myOrders = "Orders",
        profile = "Profile",
        adminOverview = "Overview",
        adminUsers = "Users",
        adminPlans = "Plans",
        adminFinance = "Finance",
        adminSystem = "System",
        welcomeBack = "Welcome back",
        allFarms = "All Farms",
        switchFarm = "Switch Farm",
        totalFarms = "Total Farms",
        totalAnimals = "Total Animals",
        totalBirds = "Total Birds",
        eggsProduced = "Eggs Produced",
        eggsAvailable = "Eggs Available",
        totalSales = "Total Sales",
        totalExpenses = "Total Expenses",
        totalIncome = "Total Income",
        outstandingPayments = "Due Payments",
        netProfitLoss = "Net Profit / Loss",
        addFarm = "Add Farm",
        addAnimal = "Add Animal / Bird",
        recordEggs = "Record Eggs",
        newSale = "New Sale",
        addExpense = "Add Expense",
        addInventory = "Add Stock",
        addEmployee = "Add Employee",
        addCustomer = "Add Customer",
        subscription = "Subscription",
        notifications = "Notifications",
        language = "Language",
        currency = "Currency & Country",
        themeMode = "Appearance",
        darkMode = "Dark Mode",
        lightMode = "Light Mode",
        logout = "Log Out",
        login = "Sign In",
        signUp = "Create Account",
        forgotPassword = "Forgot Password?",
        resetPassword = "Reset Password",
        save = "Save",
        cancel = "Cancel",
        delete = "Delete",
        edit = "Edit",
        search = "Search...",
        noDataYet = "No records found yet",
        active = "Active",
        expired = "Expired",
        pending = "Pending",
        exportPdf = "Export PDF",
        exportCsv = "Export Excel / CSV",
        monthlySales = "Monthly Sales",
        monthlyExpenses = "Monthly Expenses",
        profitAndLoss = "Profit & Loss",
        eggProductionTrend = "Egg Production",
        farmPerformance = "Farm-wise Performance",
        animalGrowth = "Livestock & Birds Breakdown",
        checkout = "Checkout",
        placeOrder = "Place Order",
        orderHistory = "Order History",
        superAdminPortal = "Super Admin Console"
    )

    val bangla = AppStrings(
        appName = "স্মার্ট ফার্ম ম্যানেজমেন্ট",
        dashboard = "ড্যাশবোর্ড",
        farms = "খামার",
        livestock = "পশু ও পাখি",
        eggs = "ডিম",
        operations = "পরিচালনা",
        commerce = "বিক্রয় ও গ্রাহক",
        reports = "রিপোর্ট",
        more = "আরও",
        marketplace = "বাজার",
        cart = "কার্ট",
        myOrders = "অর্ডার",
        profile = "প্রোফাইল",
        adminOverview = "ওভারভিউ",
        adminUsers = "ব্যবহারকারী",
        adminPlans = "প্ল্যান",
        adminFinance = "আর্থিক",
        adminSystem = "সিস্টেম",
        welcomeBack = "স্বাগতম",
        allFarms = "সকল খামার",
        switchFarm = "খামার পরিবর্তন",
        totalFarms = "মোট খামার",
        totalAnimals = "মোট পশু",
        totalBirds = "মোট পাখি",
        eggsProduced = "মোট ডিম উৎপাদন",
        eggsAvailable = "মজুদ ডিম",
        totalSales = "মোট বিক্রয়",
        totalExpenses = "মোট খরচ",
        totalIncome = "মোট আয়",
        outstandingPayments = "বকেয়া টাকা",
        netProfitLoss = "নিট লাভ / ক্ষতি",
        addFarm = "খামার যোগ করুন",
        addAnimal = "পশু / পাখি যোগ করুন",
        recordEggs = "ডিমের হিসাব লিখুন",
        newSale = "নতুন বিক্রয়",
        addExpense = "খরচ যোগ করুন",
        addInventory = "স্টক যোগ করুন",
        addEmployee = "কর্মচারী যোগ করুন",
        addCustomer = "গ্রাহক যোগ করুন",
        subscription = "সাবস্ক্রিপশন",
        notifications = "নোটিফিকেশন",
        language = "ভাষা",
        currency = "মুদ্রা ও দেশ",
        themeMode = "থিম",
        darkMode = "ডার্ক মোড",
        lightMode = "লাইট মোড",
        logout = "লগ আউট",
        login = "লগইন",
        signUp = "নিবন্ধন করুন",
        forgotPassword = "পাসওয়ার্ড ভুলে গেছেন?",
        resetPassword = "পাসওয়ার্ড রিসেট",
        save = "সংরক্ষণ",
        cancel = "বাতিল",
        delete = "মুছুন",
        edit = "সম্পাদনা",
        search = "খুঁজুন...",
        noDataYet = "এখনও কোনো তথ্য নেই",
        active = "সক্রিয়",
        expired = "মেয়াদোত্তীর্ণ",
        pending = "অপেক্ষমাণ",
        exportPdf = "পিডিএফ ডাউনলোড",
        exportCsv = "এক্সেল / সিএসভি",
        monthlySales = "মাসিক বিক্রয়",
        monthlyExpenses = "মাসিক খরচ",
        profitAndLoss = "লাভ ও ক্ষতি",
        eggProductionTrend = "ডিম উৎপাদন",
        farmPerformance = "খামারভিত্তিক পারফরম্যান্স",
        animalGrowth = "পশু ও পাখির তালিকা",
        checkout = "চেকআউট",
        placeOrder = "অর্ডার নিশ্চিত করুন",
        orderHistory = "অর্ডার ইতিহাস",
        superAdminPortal = "সুপার অ্যাডমিন কনসোল"
    )

    fun strings(language: AppLanguage, overrides: Map<String, String> = emptyMap()): AppStrings {
        val base = if (language == AppLanguage.BN) bangla else english
        if (overrides.isEmpty()) return base
        return base.copy(
            appName = overrides["appName"] ?: base.appName,
            dashboard = overrides["dashboard"] ?: base.dashboard,
            farms = overrides["farms"] ?: base.farms,
            livestock = overrides["livestock"] ?: base.livestock,
            eggs = overrides["eggs"] ?: base.eggs
        )
    }
}
