package com.example.data.local

enum class SubscriptionFeature(
    val key: String,
    val titleEn: String,
    val titleBn: String,
    val descEn: String,
    val descBn: String
) {
    MULTI_FARM_MANAGEMENT(
        key = "MULTI_FARM_MANAGEMENT",
        titleEn = "Multi-Farm Management (1 to 100+)",
        titleBn = "একাধিক খামার পরিচালনা (১ থেকে ১০০+)",
        descEn = "Create, switch & manage unlimited independent farms",
        descBn = "সীমাহীন স্বাধীন খামার তৈরি, পরিবর্তন ও পরিচালনা"
    ),
    LIVESTOCK_TRACKING(
        key = "LIVESTOCK_TRACKING",
        titleEn = "Livestock, Birds & Health Records",
        titleBn = "পশুপাখি ও স্বাস্থ্য/টিকা রেকর্ড",
        descEn = "Track animals, birds, custom types, vaccines & treatments",
        descBn = "পশু, পাখি, কাস্টম ধরন, টিকা ও চিকিৎসার হিসাব"
    ),
    EGG_PRODUCTION(
        key = "EGG_PRODUCTION",
        titleEn = "Egg Production Tracking",
        titleBn = "দৈনিক ডিম উৎপাদন হিসাব",
        descEn = "Daily egg logs, broken/sold tracking & stock valuation",
        descBn = "দৈনিক ডিম উৎপাদন, ভাঙা/বিক্রি ও মজুদের হিসাব"
    ),
    FARM_GUIDE(
        key = "FARM_GUIDE",
        titleEn = "Duck & Chicken Farming Guide",
        titleBn = "হাঁস-মুরগি পালন ও টিকা গাইড",
        descEn = "Hatching steps, brooding, vaccines & medicine schedule",
        descBn = "হাঁসের ডিম ফুটানো, পালন পদ্ধতি, টিকা ও ওষুধ গাইড"
    ),
    DASHBOARD_CHARTS_AI(
        key = "DASHBOARD_CHARTS_AI",
        titleEn = "Performance Charts & AI Advisor",
        titleBn = "গ্রাফ চার্ট ও স্মার্ট এআই উপদেষ্টা",
        descEn = "Farm analytics charts, live audit & Gemini AI Advisor",
        descBn = "খামার বিশ্লেষণ গ্রাফ ও এআই পরামর্শদাতা"
    ),
    AI_AD_VIDEO_STUDIO(
        key = "AI_AD_VIDEO_STUDIO",
        titleEn = "Google AI Bengali Video Ad Studio",
        titleBn = "Google AI বাংলা পণ্য বিজ্ঞাপন ভিডিও স্টুডিও",
        descEn = "Turn product photos into Bengali video ads + order screen demos",
        descBn = "যেকোনো পণ্যের ছবি থেকে বাংলা বিজ্ঞাপন ভিডিও ও অর্ডার ডেমো তৈরি"
    ),
    INVENTORY_EXPENSES(
        key = "INVENTORY_EXPENSES",
        titleEn = "Inventory Stock & Expense Tracking",
        titleBn = "মজুদ (ইনভেন্টরি) ও খরচ হিসাব",
        descEn = "Full feed, medicine & vaccine stock + farm expense logs",
        descBn = "খাদ্য-ওষুধের মজুদ ও দৈনিক/মাসিক খরচের হিসাব"
    ),
    BUY_SELL_SALES_CRM(
        key = "BUY_SELL_SALES_CRM",
        titleEn = "Buying & Selling, Invoices & Online Orders",
        titleBn = "ক্রয়-বিক্রয়, ইনভয়েস ও অনলাইন অর্ডার",
        descEn = "Create sales, collect dues, manage customers & orders",
        descBn = "বিক্রয় ইনভয়েস, বকেয়া আদায়, গ্রাহক ও অনলাইন অর্ডার"
    ),
    ONLINE_PRODUCT_STORE(
        key = "ONLINE_PRODUCT_STORE",
        titleEn = "Online Product Add & Marketplace Store",
        titleBn = "অনলাইন পণ্য যোগ ও মার্কেটপ্লেস স্টোর",
        descEn = "List rice, wheat, mustard & any agricultural goods online",
        descBn = "চাল, গম, সরিষাসহ যেকোনো কৃষি পণ্য অনলাইনে যোগ ও বিক্রি"
    ),
    AGRICULTURE_CROP_LEDGER(
        key = "AGRICULTURE_CROP_LEDGER",
        titleEn = "Agriculture, Crops & Land Ledger",
        titleBn = "কৃষি, ফসল ও জমির লাভ-ক্ষতি হিসাব",
        descEn = "Track rice, wheat, corn, cattle feed, land costs & yield value",
        descBn = "ধান, গম, ভুট্টা, গোখাদ্য, জমির খরচ, ফলনের মূল্য ও লাভ-ক্ষতি"
    ),
    FINANCIAL_REPORTS_EXPORT(
        key = "FINANCIAL_REPORTS_EXPORT",
        titleEn = "Financial Reports & PDF/CSV Export",
        titleBn = "আর্থিক রিপোর্ট ও পিডিএফ/সিএসভি",
        descEn = "Profit/loss reports and downloadable PDF/CSV statements",
        descBn = "লাভ-ক্ষতির রিপোর্ট এবং পিডিএফ/সিএসভি ডাউনলোড"
    ),
    EMPLOYEES_PERMISSIONS(
        key = "EMPLOYEES_PERMISSIONS",
        titleEn = "Employees, Payroll & Permissions",
        titleBn = "কর্মচারী, বেতন ও পারমিশন নিয়ন্ত্রণ",
        descEn = "Staff attendance, salary payments & granular access rules",
        descBn = "কর্মচারীর উপস্থিতি, বেতন প্রদান ও পারমিশন নিয়ন্ত্রণ"
    );

    companion object {
        val DEFAULT_LOCKED_KEYS: Set<String> = setOf(
            DASHBOARD_CHARTS_AI.key,
            INVENTORY_EXPENSES.key,
            BUY_SELL_SALES_CRM.key,
            FINANCIAL_REPORTS_EXPORT.key,
            EMPLOYEES_PERMISSIONS.key
        )

        val DEFAULT_LOCKED_CSV: String = DEFAULT_LOCKED_KEYS.joinToString(",")

        val ALL_LOCKED_KEYS: Set<String> = entries.map { it.key }.toSet()

        fun fromKey(key: String): SubscriptionFeature? = entries.firstOrNull { it.key == key }
    }
}

data class SubscriptionAccessPolicy(
    val hasActiveSubscription: Boolean = false,
    val lockedFeatureKeys: Set<String> = SubscriptionFeature.DEFAULT_LOCKED_KEYS
) {
    fun requiresSubscription(feature: SubscriptionFeature): Boolean = lockedFeatureKeys.contains(feature.key)
    fun isFree(feature: SubscriptionFeature): Boolean = !requiresSubscription(feature)
    fun canAccess(feature: SubscriptionFeature): Boolean = isFree(feature) || hasActiveSubscription

    companion object {
        fun fromSettingsAndSubs(
            settings: SystemSettingsEntity,
            subs: List<UserSubscriptionEntity>,
            now: Long = System.currentTimeMillis()
        ): SubscriptionAccessPolicy {
            val active = subs.any {
                it.status == "ACTIVE" &&
                    it.paymentStatus == PaymentStatus.SUCCESSFUL.name &&
                    it.expiryDate > now
            }
            return SubscriptionAccessPolicy(
                hasActiveSubscription = active,
                lockedFeatureKeys = settings.parseLockedFeatureKeys()
            )
        }
    }
}

data class DeveloperPaymentMethod(
    val id: String,
    val labelEn: String,
    val labelBn: String,
    val accountTitleEn: String,
    val accountTitleBn: String,
    val primaryNumberOrAccount: String,
    val secondaryDetailsEn: String,
    val secondaryDetailsBn: String,
    val copyValue: String,
    val requiresManualApproval: Boolean
)

fun SystemSettingsEntity.parseLockedFeatureKeys(): Set<String> {
    if (subscriptionLockedFeaturesCsv.isBlank()) return emptySet()
    return subscriptionLockedFeaturesCsv
        .split(",")
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .toSet()
}

fun SystemSettingsEntity.configuredPaymentMethods(): List<DeveloperPaymentMethod> = buildList {
    if (bkashEnabled && bkashNumber.isNotBlank()) {
        val accTypeEn = bkashAccountType.ifBlank { "Personal" }
        val accTypeBn = when {
            accTypeEn.equals("Merchant", ignoreCase = true) -> "মার্চেন্ট"
            accTypeEn.equals("Agent", ignoreCase = true) -> "এজেন্ট"
            else -> "পার্সোনাল"
        }
        add(
            DeveloperPaymentMethod(
                id = "bKash",
                labelEn = "bKash",
                labelBn = "বিকাশ (bKash)",
                accountTitleEn = "bKash ($accTypeEn)",
                accountTitleBn = "বিকাশ ($accTypeBn)",
                primaryNumberOrAccount = bkashNumber.trim(),
                secondaryDetailsEn = "Send Money / Payment to $bkashNumber ($accTypeEn)",
                secondaryDetailsBn = "$bkashNumber নম্বরে ($accTypeBn) পেমেন্ট/সেন্ড মানি করুন",
                copyValue = bkashNumber.trim(),
                requiresManualApproval = requireAdminApprovalForAll
            )
        )
    }
    if (nagadEnabled && nagadNumber.isNotBlank()) {
        val accTypeEn = nagadAccountType.ifBlank { "Personal" }
        val accTypeBn = when {
            accTypeEn.equals("Merchant", ignoreCase = true) -> "মার্চেন্ট"
            accTypeEn.equals("Agent", ignoreCase = true) -> "এজেন্ট"
            else -> "পার্সোনাল"
        }
        add(
            DeveloperPaymentMethod(
                id = "Nagad",
                labelEn = "Nagad",
                labelBn = "নগদ (Nagad)",
                accountTitleEn = "Nagad ($accTypeEn)",
                accountTitleBn = "নগদ ($accTypeBn)",
                primaryNumberOrAccount = nagadNumber.trim(),
                secondaryDetailsEn = "Send Money / Payment to $nagadNumber ($accTypeEn)",
                secondaryDetailsBn = "$nagadNumber নম্বরে ($accTypeBn) পেমেন্ট/সেন্ড মানি করুন",
                copyValue = nagadNumber.trim(),
                requiresManualApproval = requireAdminApprovalForAll
            )
        )
    }
    if (bankEnabled && bankAccountNumber.isNotBlank()) {
        val bankLabel = bankName.ifBlank { "Bank" }
        val holder = bankAccountName.ifBlank { "Smart Farm Management" }
        val branch = bankBranch.ifBlank { "Main Branch" }
        add(
            DeveloperPaymentMethod(
                id = "Bank Account",
                labelEn = "Bank Account",
                labelBn = "ব্যাংক অ্যাকাউন্ট",
                accountTitleEn = bankLabel,
                accountTitleBn = bankLabel,
                primaryNumberOrAccount = bankAccountNumber.trim(),
                secondaryDetailsEn = "A/C Name: $holder • Branch: $branch",
                secondaryDetailsBn = "হিসাবের নাম: $holder • শাখা: $branch",
                copyValue = bankAccountNumber.trim(),
                requiresManualApproval = true
            )
        )
    }
}
