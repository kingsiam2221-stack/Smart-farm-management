package com.example.data.local

enum class PermissionGroup(val titleEn: String, val titleBn: String) {
    DASHBOARD_AI("Dashboard & AI Advisor", "ড্যাশবোর্ড ও এআই"),
    FARMS("Farms Management", "খামার ব্যবস্থাপনা"),
    LIVESTOCK("Livestock & Birds", "পশু ও পাখি"),
    EGGS("Egg Production", "ডিম উৎপাদন"),
    INVENTORY("Inventory & Stock", "মজুদ / ইনভেন্টরি"),
    EXPENSES("Farm Expenses", "খামারের খরচ"),
    EMPLOYEES("Employees & Payroll", "কর্মচারী ও বেতন"),
    SALES_CRM("Sales, Customers & Orders", "বিক্রয়, গ্রাহক ও অর্ডার"),
    ONLINE_STORE("Online Product Storefront", "অনলাইন পণ্য ও বিক্রি"),
    AGRICULTURE("Agriculture, Crops & Land", "কৃষি, ফসল ও জমির হিসাব"),
    GUIDE("Farming Guide", "পালন গাইড"),
    REPORTS_SYSTEM("Reports, Subscription & Caution", "রিপোর্ট, সাবস্ক্রিপশন ও সতর্কতা"),
    CUSTOM("Custom Added Options", "নতুন যোগকৃত অপশন")
}

enum class AppPermission(
    val key: String,
    val group: PermissionGroup,
    val titleEn: String,
    val titleBn: String,
    val descEn: String,
    val descBn: String
) {
    DASHBOARD_OVERVIEW(
        key = "DASHBOARD_OVERVIEW",
        group = PermissionGroup.DASHBOARD_AI,
        titleEn = "Dashboard & KPI Summary",
        titleBn = "ড্যাশবোর্ড ও মূল পরিসংখ্যান",
        descEn = "View home dashboard cards and farm counts",
        descBn = "হোম ড্যাশবোর্ড ও খামারের মূল তথ্য দেখা"
    ),
    DASHBOARD_CHARTS(
        key = "DASHBOARD_CHARTS",
        group = PermissionGroup.DASHBOARD_AI,
        titleEn = "Dashboard Performance Charts",
        titleBn = "ড্যাশবোর্ড গ্রাফ ও চার্ট",
        descEn = "View sales vs expense and egg trend charts",
        descBn = "আয়-ব্যয় ও ডিম উৎপাদনের চার্ট দেখা"
    ),
    AI_ADVISOR(
        key = "AI_ADVISOR",
        group = PermissionGroup.DASHBOARD_AI,
        titleEn = "Smart Farm AI Advisor",
        titleBn = "স্মার্ট ফার্ম এআই উপদেষ্টা",
        descEn = "Ask Gemini AI Advisor & run farm audits",
        descBn = "এআই উপদেষ্টাকে প্রশ্ন ও ফার্ম অডিট চালানো"
    ),
    AI_AD_STUDIO(
        key = "AI_AD_STUDIO",
        group = PermissionGroup.DASHBOARD_AI,
        titleEn = "AI Product Video Ad Studio",
        titleBn = "AI পণ্য বিজ্ঞাপন ও ভিডিও স্টুডিও",
        descEn = "Create Bengali AI product ad videos & order demos",
        descBn = "পণ্যের ছবি দিয়ে বাংলায় AI বিজ্ঞাপন ও অর্ডার ভিডিও তৈরি"
    ),
    FARMS_VIEW(
        key = "FARMS_VIEW",
        group = PermissionGroup.FARMS,
        titleEn = "View & Switch Farms",
        titleBn = "খামার দেখা ও পরিবর্তন",
        descEn = "Browse farm list and filter by active farm",
        descBn = "খামারের তালিকা দেখা ও সক্রিয় খামার নির্বাচন"
    ),
    FARMS_CREATE_EDIT(
        key = "FARMS_CREATE_EDIT",
        group = PermissionGroup.FARMS,
        titleEn = "Create & Edit Farms",
        titleBn = "খামার তৈরি ও সম্পাদনা",
        descEn = "Add new farms and edit farm details",
        descBn = "নতুন খামার যোগ ও তথ্য পরিবর্তন করা"
    ),
    FARMS_DELETE(
        key = "FARMS_DELETE",
        group = PermissionGroup.FARMS,
        titleEn = "Delete Farms",
        titleBn = "খামার মুছে ফেলা",
        descEn = "Remove farms permanently",
        descBn = "খামার মুছে ফেলার অনুমতি"
    ),
    LIVESTOCK_VIEW(
        key = "LIVESTOCK_VIEW",
        group = PermissionGroup.LIVESTOCK,
        titleEn = "View Animals & Birds",
        titleBn = "পশু ও পাখি দেখা",
        descEn = "View herds, flocks, breeds & charts",
        descBn = "পশুপাখির তালিকা, জাত ও চার্ট দেখা"
    ),
    LIVESTOCK_CREATE_EDIT(
        key = "LIVESTOCK_CREATE_EDIT",
        group = PermissionGroup.LIVESTOCK,
        titleEn = "Add, Edit & Custom Animal Types",
        titleBn = "পশুপাখি যোগ, সম্পাদনা ও কাস্টম ধরন",
        descEn = "Add/edit animal batches and create custom types",
        descBn = "নতুন পশুপাখি ও কাস্টম ধরন যোগ করা"
    ),
    LIVESTOCK_HEALTH_EVENTS(
        key = "LIVESTOCK_HEALTH_EVENTS",
        group = PermissionGroup.LIVESTOCK,
        titleEn = "Log Health, Vaccines & Events",
        titleBn = "স্বাস্থ্য, টিকা ও ঘটনা লেখা",
        descEn = "Record births, deaths, vaccinations & treatments",
        descBn = "জন্ম, মৃত্যু, টিকা ও চিকিৎসার হিসাব রাখা"
    ),
    LIVESTOCK_DELETE(
        key = "LIVESTOCK_DELETE",
        group = PermissionGroup.LIVESTOCK,
        titleEn = "Delete Animal Records",
        titleBn = "পশুপাখির রেকর্ড মুছে ফেলা",
        descEn = "Delete livestock or bird records",
        descBn = "পশুপাখির রেকর্ড মুছে ফেলার অনুমতি"
    ),
    EGGS_VIEW(
        key = "EGGS_VIEW",
        group = PermissionGroup.EGGS,
        titleEn = "View Egg Production",
        titleBn = "ডিম উৎপাদন দেখা",
        descEn = "View daily/weekly/monthly egg logs & stock",
        descBn = "দৈনিক/সাপ্তাহিক/মাসিক ডিম উৎপাদনের হিসাব দেখা"
    ),
    EGGS_RECORD(
        key = "EGGS_RECORD",
        group = PermissionGroup.EGGS,
        titleEn = "Record Daily Eggs",
        titleBn = "দৈনিক ডিম হিসাব যোগ",
        descEn = "Log produced, broken, and sold eggs",
        descBn = "উৎপাদিত, ভাঙা ও বিক্রিত ডিমের হিসাব লেখা"
    ),
    EGGS_DELETE(
        key = "EGGS_DELETE",
        group = PermissionGroup.EGGS,
        titleEn = "Delete Egg Records",
        titleBn = "ডিমের হিসাব মুছে ফেলা",
        descEn = "Remove saved egg production entries",
        descBn = "সংরক্ষিত ডিমের হিসাব মুছে ফেলা"
    ),
    INVENTORY_VIEW(
        key = "INVENTORY_VIEW",
        group = PermissionGroup.INVENTORY,
        titleEn = "View Inventory & Low-Stock Alerts",
        titleBn = "মজুদ ও কম স্টক সতর্কতা দেখা",
        descEn = "View feed, medicine & stock levels",
        descBn = "খাদ্য, ওষুধ ও পণ্যের মজুদ দেখা"
    ),
    INVENTORY_MANAGE(
        key = "INVENTORY_MANAGE",
        group = PermissionGroup.INVENTORY,
        titleEn = "Add Stock & Stock In / Out",
        titleBn = "স্টক যোগ ও ইন/আউট পরিচালনা",
        descEn = "Create stock items and adjust quantities",
        descBn = "নতুন মজুদ যোগ ও স্টক বাড়ানো/কমানো"
    ),
    INVENTORY_DELETE(
        key = "INVENTORY_DELETE",
        group = PermissionGroup.INVENTORY,
        titleEn = "Delete Inventory Items",
        titleBn = "মজুদ পণ্য মুছে ফেলা",
        descEn = "Delete items from farm inventory",
        descBn = "ইনভেন্টরি থেকে পণ্য মুছে ফেলা"
    ),
    EXPENSES_VIEW(
        key = "EXPENSES_VIEW",
        group = PermissionGroup.EXPENSES,
        titleEn = "View Farm Expenses",
        titleBn = "খামারের খরচ দেখা",
        descEn = "View daily/monthly expense entries",
        descBn = "দৈনিক ও মাসিক খরচের তালিকা দেখা"
    ),
    EXPENSES_MANAGE(
        key = "EXPENSES_MANAGE",
        group = PermissionGroup.EXPENSES,
        titleEn = "Add & Delete Expenses",
        titleBn = "খরচ যোগ ও মুছে ফেলা",
        descEn = "Log new expenses or remove expense entries",
        descBn = "নতুন খরচ যোগ ও পুরনো খরচ মুছে ফেলা"
    ),
    EMPLOYEES_VIEW(
        key = "EMPLOYEES_VIEW",
        group = PermissionGroup.EMPLOYEES,
        titleEn = "View Employee Directory",
        titleBn = "কর্মচারী তালিকা দেখা",
        descEn = "View staff list, designations & attendance status",
        descBn = "কর্মচারীদের তালিকা ও উপস্থিতি দেখা"
    ),
    EMPLOYEES_MANAGE(
        key = "EMPLOYEES_MANAGE",
        group = PermissionGroup.EMPLOYEES,
        titleEn = "Manage Staff, Attendance & Salaries",
        titleBn = "কর্মচারী যোগ, উপস্থিতি ও বেতন প্রদান",
        descEn = "Add/delete employees, mark attendance & pay salary",
        descBn = "কর্মচারী যোগ/মুছা, উপস্থিতি ও বেতন পরিশোধ"
    ),
    STAFF_TARGETS_VIEW(
        key = "STAFF_TARGETS_VIEW",
        group = PermissionGroup.EMPLOYEES,
        titleEn = "My Tasks, Targets, Pay & Auto-Messages",
        titleBn = "নিজের কাজ, টার্গেট, বেতন ও অটো-মেসেজ",
        descEn = "View morning-to-night targets, claim task bonus, view pay & send auto updates",
        descBn = "সকাল থেকে রাতের টার্গেট দেখা, বোনাস পাওয়া ও অটোমেটেড বার্তা পাঠানো"
    ),
    STAFF_HR_MANAGE(
        key = "STAFF_HR_MANAGE",
        group = PermissionGroup.EMPLOYEES,
        titleEn = "Manage Salaries, Bonuses, Expenses & Targets",
        titleBn = "বেতন, বোনাস, কর্মচারী খরচ ও টার্গেট পরিচালনা",
        descEn = "Assign daily targets with cash bonus and record salary, bonus & staff expenses",
        descBn = "কর্মচারীর বেতন, বোনাস, খরচ এবং টাকার পুরস্কারসহ দৈনিক টার্গেট নির্ধারণ"
    ),
    SALES_VIEW(
        key = "SALES_VIEW",
        group = PermissionGroup.SALES_CRM,
        titleEn = "View Sales & Invoices",
        titleBn = "বিক্রয় ও ইনভয়েস দেখা",
        descEn = "Browse sales history, paid & due totals",
        descBn = "বিক্রয় ইতিহাস ও বকেয়ার হিসাব দেখা"
    ),
    SALES_CREATE_COLLECT(
        key = "SALES_CREATE_COLLECT",
        group = PermissionGroup.SALES_CRM,
        titleEn = "Create Sales, Collect Due & PDF",
        titleBn = "নতুন বিক্রয়, বকেয়া আদায় ও পিডিএফ",
        descEn = "Create invoices, collect due & export PDF",
        descBn = "নতুন বিক্রয় করা, বকেয়া আদায় ও পিডিএফ ডাউনলোড"
    ),
    SALES_DELETE(
        key = "SALES_DELETE",
        group = PermissionGroup.SALES_CRM,
        titleEn = "Delete Sales Records",
        titleBn = "বিক্রয় রেকর্ড মুছে ফেলা",
        descEn = "Delete recorded sales invoices",
        descBn = "বিক্রয়ের হিসাব মুছে ফেলা"
    ),
    CUSTOMERS_VIEW(
        key = "CUSTOMERS_VIEW",
        group = PermissionGroup.SALES_CRM,
        titleEn = "View Customers",
        titleBn = "গ্রাহক তালিকা দেখা",
        descEn = "View customer profiles & due balances",
        descBn = "গ্রাহকের তথ্য ও বকেয়া দেখা"
    ),
    CUSTOMERS_MANAGE(
        key = "CUSTOMERS_MANAGE",
        group = PermissionGroup.SALES_CRM,
        titleEn = "Add, Edit & Delete Customers",
        titleBn = "গ্রাহক যোগ, সম্পাদনা ও মুছে ফেলা",
        descEn = "Create, update or delete customer records",
        descBn = "গ্রাহক যোগ, তথ্য পরিবর্তন ও মুছে ফেলা"
    ),
    ORDERS_VIEW(
        key = "ORDERS_VIEW",
        group = PermissionGroup.SALES_CRM,
        titleEn = "View Online Orders",
        titleBn = "অনলাইন অর্ডার দেখা",
        descEn = "View incoming customer marketplace orders",
        descBn = "অনলাইন দোকান থেকে আসা অর্ডার দেখা"
    ),
    ORDERS_MANAGE(
        key = "ORDERS_MANAGE",
        group = PermissionGroup.SALES_CRM,
        titleEn = "Update Order Status",
        titleBn = "অর্ডারের অবস্থা পরিবর্তন",
        descEn = "Confirm, process, ship or cancel orders",
        descBn = "অর্ডার নিশ্চিত, প্রস্তুত বা বাতিল করা"
    ),
    ONLINE_PRODUCTS_VIEW(
        key = "ONLINE_PRODUCTS_VIEW",
        group = PermissionGroup.ONLINE_STORE,
        titleEn = "View Online Store Products",
        titleBn = "অনলাইন পণ্য তালিকা দেখা",
        descEn = "View farm & agricultural products listed online",
        descBn = "অনলাইন দোকানে যুক্ত কৃষি ও খামার পণ্য দেখা"
    ),
    ONLINE_PRODUCTS_MANAGE(
        key = "ONLINE_PRODUCTS_MANAGE",
        group = PermissionGroup.ONLINE_STORE,
        titleEn = "Add, Edit & Publish Online Products",
        titleBn = "অনলাইন পণ্য যোগ, সম্পাদনা ও প্রকাশ",
        descEn = "Add rice, wheat, mustard or any agri product for sale",
        descBn = "চাল, গম, সরিষা বা যেকোনো কৃষি পণ্য অনলাইনে বিক্রি করা"
    ),
    AGRICULTURE_VIEW(
        key = "AGRICULTURE_VIEW",
        group = PermissionGroup.AGRICULTURE,
        titleEn = "View Agriculture, Crops & Land Ledger",
        titleBn = "কৃষি, ফসল ও জমির হিসাব দেখা",
        descEn = "View rice, wheat, corn, cattle feed, land costs & profit/loss",
        descBn = "ধান, গম, ভুট্টা, গোখাদ্য, জমির খরচ ও লাভ-ক্ষতি দেখা"
    ),
    AGRICULTURE_MANAGE(
        key = "AGRICULTURE_MANAGE",
        group = PermissionGroup.AGRICULTURE,
        titleEn = "Add & Edit Lands, Crops & Sector Expenses",
        titleBn = "জমি, ফসল ও খাতভিত্তিক খরচ যোগ",
        descEn = "Record land plots, crop harvests, yield worth & sector costs",
        descBn = "নতুন জমি, ফসল, ফলনের মূল্য ও খাতভিত্তিক খরচ সংরক্ষণ"
    ),
    AGRICULTURE_DELETE(
        key = "AGRICULTURE_DELETE",
        group = PermissionGroup.AGRICULTURE,
        titleEn = "Delete Land, Crop & Agri Expense Records",
        titleBn = "কৃষি, জমি ও ফসলের রেকর্ড মুছে ফেলা",
        descEn = "Remove saved land plots, crops or agriculture expense entries",
        descBn = "সংরক্ষিত জমি, ফসল বা কৃষি খরচের হিসাব মুছে ফেলা"
    ),
    FARM_GUIDE_VIEW(
        key = "FARM_GUIDE_VIEW",
        group = PermissionGroup.GUIDE,
        titleEn = "Duck & Chicken Farming Guide",
        titleBn = "হাঁস-মুরগি পালন ও টিকা গাইড",
        descEn = "Access hatching, brooding, vaccine & medicine guide",
        descBn = "ডিম ফুটানো, পালন, টিকা ও ওষুধের গাইড দেখা"
    ),
    REPORTS_VIEW_EXPORT(
        key = "REPORTS_VIEW_EXPORT",
        group = PermissionGroup.REPORTS_SYSTEM,
        titleEn = "Financial Reports & PDF/CSV Export",
        titleBn = "আর্থিক রিপোর্ট ও পিডিএফ/সিএসভি",
        descEn = "View P/L charts and export PDF/CSV reports",
        descBn = "লাভ-ক্ষতির রিপোর্ট দেখা ও পিডিএফ/সিএসভি ডাউনলোড"
    ),
    SUBSCRIPTION_MANAGE(
        key = "SUBSCRIPTION_MANAGE",
        group = PermissionGroup.REPORTS_SYSTEM,
        titleEn = "Subscription Plans & Payments",
        titleBn = "সাবস্ক্রিপশন ও পেমেন্ট",
        descEn = "View active plans and purchase/renew subscription",
        descBn = "সাবস্ক্রিপশন প্ল্যান দেখা ও পেমেন্ট করা"
    ),
    ALERTS_CAUTION_VIEW(
        key = "ALERTS_CAUTION_VIEW",
        group = PermissionGroup.REPORTS_SYSTEM,
        titleEn = "Caution, Alerts & Support",
        titleBn = "সতর্কতা, নোটিফিকেশন ও সাপোর্ট",
        descEn = "View caution alerts, notifications & support tickets",
        descBn = "সতর্কতা বার্তা, নোটিফিকেশন ও সাপোর্ট দেখা"
    ),
    PROFILE_APPEARANCE(
        key = "PROFILE_APPEARANCE",
        group = PermissionGroup.REPORTS_SYSTEM,
        titleEn = "Language, Theme & Terminology",
        titleBn = "ভাষা, থিম ও অ্যাপের নাম পরিবর্তন",
        descEn = "Customize language, currency, theme & app labels",
        descBn = "ভাষা, মুদ্রা, ডার্ক/হোয়াইট মোড ও লেবেল পরিবর্তন"
    );

    companion object {
        fun fromKey(key: String): AppPermission? = entries.firstOrNull { it.key == key }
    }
}

data class PermissionDescriptor(
    val key: String,
    val group: PermissionGroup,
    val titleEn: String,
    val titleBn: String,
    val descEn: String,
    val descBn: String,
    val isCustom: Boolean = false
)

enum class PermissionPreset(val labelEn: String, val labelBn: String, val keys: Set<String>) {
    FULL_ACCESS(
        "Full Access",
        "সকল অনুমতি",
        AppPermission.entries.map { it.key }.toSet()
    ),
    MANAGER(
        "Farm Manager",
        "খামার ম্যানেজার",
        setOf(
            AppPermission.DASHBOARD_OVERVIEW.key,
            AppPermission.DASHBOARD_CHARTS.key,
            AppPermission.AI_ADVISOR.key,
            AppPermission.AI_AD_STUDIO.key,
            AppPermission.FARMS_VIEW.key,
            AppPermission.LIVESTOCK_VIEW.key,
            AppPermission.LIVESTOCK_CREATE_EDIT.key,
            AppPermission.LIVESTOCK_HEALTH_EVENTS.key,
            AppPermission.EGGS_VIEW.key,
            AppPermission.EGGS_RECORD.key,
            AppPermission.INVENTORY_VIEW.key,
            AppPermission.INVENTORY_MANAGE.key,
            AppPermission.EXPENSES_VIEW.key,
            AppPermission.EXPENSES_MANAGE.key,
            AppPermission.EMPLOYEES_VIEW.key,
            AppPermission.STAFF_TARGETS_VIEW.key,
            AppPermission.STAFF_HR_MANAGE.key,
            AppPermission.SALES_VIEW.key,
            AppPermission.SALES_CREATE_COLLECT.key,
            AppPermission.CUSTOMERS_VIEW.key,
            AppPermission.CUSTOMERS_MANAGE.key,
            AppPermission.ORDERS_VIEW.key,
            AppPermission.ORDERS_MANAGE.key,
            AppPermission.ONLINE_PRODUCTS_VIEW.key,
            AppPermission.ONLINE_PRODUCTS_MANAGE.key,
            AppPermission.AGRICULTURE_VIEW.key,
            AppPermission.AGRICULTURE_MANAGE.key,
            AppPermission.FARM_GUIDE_VIEW.key,
            AppPermission.REPORTS_VIEW_EXPORT.key,
            AppPermission.ALERTS_CAUTION_VIEW.key
        )
    ),
    POULTRY_WORKER(
        "Poultry & Duck Staff",
        "হাঁস-মুরগি কর্মী",
        setOf(
            AppPermission.DASHBOARD_OVERVIEW.key,
            AppPermission.FARMS_VIEW.key,
            AppPermission.LIVESTOCK_VIEW.key,
            AppPermission.LIVESTOCK_CREATE_EDIT.key,
            AppPermission.LIVESTOCK_HEALTH_EVENTS.key,
            AppPermission.EGGS_VIEW.key,
            AppPermission.EGGS_RECORD.key,
            AppPermission.INVENTORY_VIEW.key,
            AppPermission.STAFF_TARGETS_VIEW.key,
            AppPermission.FARM_GUIDE_VIEW.key
        )
    ),
    SALES_CASHIER(
        "Sales & Orders",
        "বিক্রয় ও ক্যাশিয়ার",
        setOf(
            AppPermission.DASHBOARD_OVERVIEW.key,
            AppPermission.INVENTORY_VIEW.key,
            AppPermission.STAFF_TARGETS_VIEW.key,
            AppPermission.SALES_VIEW.key,
            AppPermission.SALES_CREATE_COLLECT.key,
            AppPermission.CUSTOMERS_VIEW.key,
            AppPermission.CUSTOMERS_MANAGE.key,
            AppPermission.ORDERS_VIEW.key,
            AppPermission.ORDERS_MANAGE.key,
            AppPermission.ONLINE_PRODUCTS_VIEW.key,
            AppPermission.ONLINE_PRODUCTS_MANAGE.key,
            AppPermission.AI_AD_STUDIO.key
        )
    ),
    VIEW_ONLY(
        "View Only",
        "শুধু দেখা",
        setOf(
            AppPermission.DASHBOARD_OVERVIEW.key,
            AppPermission.FARMS_VIEW.key,
            AppPermission.LIVESTOCK_VIEW.key,
            AppPermission.EGGS_VIEW.key,
            AppPermission.INVENTORY_VIEW.key,
            AppPermission.STAFF_TARGETS_VIEW.key,
            AppPermission.SALES_VIEW.key,
            AppPermission.AGRICULTURE_VIEW.key,
            AppPermission.FARM_GUIDE_VIEW.key
        )
    )
}

data class EmployeeAccessPolicy(
    val isAdminOwner: Boolean = true,
    val grantedKeys: Set<String> = emptySet()
) {
    fun has(permission: AppPermission): Boolean = isAdminOwner || grantedKeys.contains(permission.key)
    fun hasKey(key: String): Boolean = isAdminOwner || grantedKeys.contains(key)
    fun hasAny(vararg permissions: AppPermission): Boolean = isAdminOwner || permissions.any { has(it) }
    fun canManagePermissions(): Boolean = isAdminOwner
}

object PermissionCatalog {
    fun allDescriptors(customOptions: List<CustomPermissionOptionEntity>): List<PermissionDescriptor> {
        val builtIn = AppPermission.entries.map {
            PermissionDescriptor(
                key = it.key,
                group = it.group,
                titleEn = it.titleEn,
                titleBn = it.titleBn,
                descEn = it.descEn,
                descBn = it.descBn,
                isCustom = false
            )
        }
        val custom = customOptions.map {
            PermissionDescriptor(
                key = it.permissionKey,
                group = PermissionGroup.CUSTOM,
                titleEn = it.titleEn,
                titleBn = it.titleBn.ifBlank { it.titleEn },
                descEn = it.descriptionEn.ifBlank { "Custom added module option" },
                descBn = it.descriptionBn.ifBlank { it.descriptionEn.ifBlank { "নতুন যোগকৃত অপশন" } },
                isCustom = true
            )
        }
        return builtIn + custom
    }

    fun defaultGrantedKeysFromLegacyFlags(employee: EmployeeEntity): Set<String> = buildSet {
        add(AppPermission.DASHBOARD_OVERVIEW.key)
        add(AppPermission.FARMS_VIEW.key)
        add(AppPermission.FARM_GUIDE_VIEW.key)
        add(AppPermission.STAFF_TARGETS_VIEW.key)
        if (employee.canManageInventory) {
            add(AppPermission.INVENTORY_VIEW.key)
            add(AppPermission.INVENTORY_MANAGE.key)
        }
        if (employee.canManageLivestock) {
            add(AppPermission.LIVESTOCK_VIEW.key)
            add(AppPermission.LIVESTOCK_CREATE_EDIT.key)
            add(AppPermission.LIVESTOCK_HEALTH_EVENTS.key)
            add(AppPermission.EGGS_VIEW.key)
            add(AppPermission.EGGS_RECORD.key)
        }
        if (employee.canManageSales) {
            add(AppPermission.SALES_VIEW.key)
            add(AppPermission.SALES_CREATE_COLLECT.key)
            add(AppPermission.CUSTOMERS_VIEW.key)
            add(AppPermission.ORDERS_VIEW.key)
        }
    }
}
