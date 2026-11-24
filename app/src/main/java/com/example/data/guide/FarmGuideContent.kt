package com.example.data.guide

enum class GuideTopic {
    SUMMARY,
    HATCH_DUCK_EGGS,
    RAISE_DUCKS,
    RAISE_CHICKENS,
    VACCINE_SCHEDULE,
    MEDICINE_SCHEDULE
}

enum class GuideSpecies {
    ALL,
    DUCK,
    CHICKEN
}

data class BilingualText(
    val en: String,
    val bn: String
) {
    fun get(isBangla: Boolean): String = if (isBangla) bn else en
}

data class GuideSummaryCardData(
    val topic: GuideTopic,
    val title: BilingualText,
    val highlightMetric: BilingualText,
    val summaryPoints: List<BilingualText>
)

data class GuideStepItem(
    val stepNumber: Int,
    val stageBadge: BilingualText,
    val title: BilingualText,
    val details: BilingualText,
    val keySpec: BilingualText? = null
)

data class VaccineScheduleEntry(
    val species: GuideSpecies,
    val ageTiming: BilingualText,
    val vaccineName: BilingualText,
    val targetDisease: BilingualText,
    val route: BilingualText,
    val doseAndNotes: BilingualText
)

data class MedicineScheduleEntry(
    val species: GuideSpecies,
    val timingOrCondition: BilingualText,
    val medicineOrSupplement: BilingualText,
    val purpose: BilingualText,
    val administrationAndSafety: BilingualText,
    val requiresVetCheck: Boolean = false
)

object FarmGuideContent {

    val topicLabels: Map<GuideTopic, BilingualText> = mapOf(
        GuideTopic.SUMMARY to BilingualText("Summary", "সারাংশ"),
        GuideTopic.HATCH_DUCK_EGGS to BilingualText("Hatch Duck Eggs", "হাঁসের ডিম ফুটানো"),
        GuideTopic.RAISE_DUCKS to BilingualText("Raise Ducks", "হাঁস পালন"),
        GuideTopic.RAISE_CHICKENS to BilingualText("Raise Chickens", "মুরগি পালন"),
        GuideTopic.VACCINE_SCHEDULE to BilingualText("Vaccines", "টিকার সময়সূচি"),
        GuideTopic.MEDICINE_SCHEDULE to BilingualText("Medicines", "ওষুধ ও ভিটামিন")
    )

    val summaryCards: List<GuideSummaryCardData> = listOf(
        GuideSummaryCardData(
            topic = GuideTopic.HATCH_DUCK_EGGS,
            title = BilingualText("Duck Egg Hatching (28 Days)", "হাঁসের ডিম ফুটানো (২৮ দিন)"),
            highlightMetric = BilingualText("37.5°C -> 37.0°C | 55% -> 80% RH", "৩৭.৫°সে -> ৩৭.০°সে | আর্দ্রতা ৫৫% -> ৮০%"),
            summaryPoints = listOf(
                BilingualText(
                    "Turn fertile eggs 3–5 times/day from Day 1 to Day 25; stop turning on Day 26 (lockdown).",
                    "১ম–২৫তম দিন প্রতিদিন ৩–৫ বার ডিম ঘুরান; ২৬তম দিনে ঘুরানো বন্ধ করে লকডাউনে রাখুন।"
                ),
                BilingualText(
                    "From Day 10–25, cool eggs 10–15 mins/day and mist lightly with lukewarm water (37°C).",
                    "১০ম–২৫তম দিন প্রতিদিন ১০–১৫ মিনিট ডিম ঠান্ডা করে হালকা কুসুম গরম পানি (৩৭°সে) স্প্রে করুন।"
                ),
                BilingualText(
                    "Candle eggs on Days 7, 14 & 25 to remove infertile or spoiled eggs; hatch finishes on Day 28.",
                    "৭, ১৪ ও ২৫তম দিনে ক্যান্ডলিং করে অকার্যকর ডিম সরান; ২৮তম দিনে বাচ্চা বের হবে।"
                )
            )
        ),
        GuideSummaryCardData(
            topic = GuideTopic.RAISE_DUCKS,
            title = BilingualText("Raising Ducks Essentials", "হাঁস পালনের মূল নিয়ম"),
            highlightMetric = BilingualText("Brood 32°C -> 25°C | Laying at 20–24 wks", "ব্রুডিং ৩২°সে -> ২৫°সে | ডিম ২০–২৪ সপ্তাহে"),
            summaryPoints = listOf(
                BilingualText(
                    "Use deep drinkers placed over wire mesh so ducks dip nostrils/eyes while keeping litter dry.",
                    "গভীর পাত্রে পানি দিন ও তারজালির ওপর রাখুন যাতে হাঁস চোখ-নাক ধুতে পারে কিন্তু বিছানা ভিজবে না।"
                ),
                BilingualText(
                    "Starter crumble (20–22% protein, 0–3 wks) -> Grower (15–16%, 4–16 wks) -> Layer (17–18% + Calcium).",
                    "স্টার্টার (২০–২২% প্রোটিন, ০–৩ সপ্তাহ) -> গ্রোয়ার (১৫–১৬%, ৪–১৬ সপ্তাহ) -> লেয়ার (১৭–১৮% + ক্যালসিয়াম)।"
                ),
                BilingualText(
                    "Ducks lay mostly between 4:00 AM and 8:00 AM—keep them inside dry laying pens until 9:00 AM.",
                    "হাঁস সাধারণত ভোর ৪টা থেকে সকাল ৮টার মধ্যে ডিম পাড়ে—তাই সকাল ৯টা পর্যন্ত শুকনো ঘরে রাখুন।"
                )
            )
        ),
        GuideSummaryCardData(
            topic = GuideTopic.RAISE_CHICKENS,
            title = BilingualText("Raising Chickens Essentials", "মুরগি পালনের মূল নিয়ম"),
            highlightMetric = BilingualText("Brood 34°C -> 23°C | 1.5–2 sq ft/layer", "ব্রুডিং ৩৪°সে -> ২৩°সে | ১.৫–২ বর্গফুট/মুরগি"),
            summaryPoints = listOf(
                BilingualText(
                    "Disinfect shed & dry rice husk litter 7 days early; keep brooder at 33–35°C for Days 1–3.",
                    "বাচ্চা আনার ৭ দিন আগে ঘর জীবাণুমুক্ত করুন; ১ম–৩ দিন ব্রুডার তাপমাত্রা ৩৩–৩৫°সে রাখুন।"
                ),
                BilingualText(
                    "Broiler finishes in 30–35 days; Layer starts laying eggs at 18–20 weeks (needs 16 hrs daily light).",
                    "ব্রয়লার ৩০–৩৫ দিনে বাজারজাত হয়; লেয়ার ১৮–২০ সপ্তাহে ডিম পাড়ে (দৈনিক ১৬ ঘণ্টা আলো প্রয়োজন)।"
                ),
                BilingualText(
                    "Ensure ammonia-free cross ventilation and clean drinker sanitation twice every day.",
                    "ঘরে পর্যাপ্ত বাতাস চলাচল রাখুন এবং দিনে ২ বার পানির পাত্র পরিষ্কার করুন।"
                )
            )
        ),
        GuideSummaryCardData(
            topic = GuideTopic.VACCINE_SCHEDULE,
            title = BilingualText("Key Vaccine Milestones", "প্রধান টিকা সময়সূচি"),
            highlightMetric = BilingualText("Chicken: Day 5, 12, 21 | Duck: Day 15, 30, 42", "মুরগি: দিন ৫, ১২, ২১ | হাঁস: দিন ১৫, ৩০, ৪২"),
            summaryPoints = listOf(
                BilingualText(
                    "Chicken: Marek's (Day 1) -> BCRDV/Ranikhet (Day 5–7 & 21) -> Gumboro (Day 12–14 & 28) -> Fowl Pox (Wk 6–8) -> RDV (Wk 8–10 & every 6 mos).",
                    "মুরগি: মারেক্স (দিন ১) -> বিসিআরডিভি/রানীক্ষেত (দিন ৫–৭ ও ২১) -> গামবোরো (দিন ১২–১৪ ও ২৮) -> ফাউল পক্স (৬–৮ সপ্তাহ) -> আরডিভি (৮–১০ সপ্তাহ ও প্রতি ৬ মাসে)।"
                ),
                BilingualText(
                    "Duck: Duck Plague 1st dose (Day 15–21) -> Booster (Day 30–35) -> Duck Cholera (Day 42–45) -> Repeat every 6 months.",
                    "হাঁস: ডাক প্লেগ ১ম ডোজ (দিন ১৫–২১) -> বুস্টার (দিন ৩০–৩৫) -> ডাক কলেরা (দিন ৪২–৪৫) -> প্রতি ৬ মাস পর পর পুনরাবৃত্তি।"
                ),
                BilingualText(
                    "Keep vaccines cold (2–8°C) and never vaccinate sick birds; give Vitamin C after vaccination.",
                    "টিকা ২–৮°সে তাপমাত্রায় ঠান্ডা রাখুন, অসুস্থ পাখিকে টিকা দেবেন না এবং টিকার পর ভিটামিন সি দিন।"
                )
            )
        ),
        GuideSummaryCardData(
            topic = GuideTopic.MEDICINE_SCHEDULE,
            title = BilingualText("Medicine & Supplement Timing", "ওষুধ ও ভিটামিনের সময়সূচি"),
            highlightMetric = BilingualText("Stress -> Gut -> Calcium -> Deworming", "ধকল -> হজম -> ক্যালসিয়াম -> কৃমিনাশক"),
            summaryPoints = listOf(
                BilingualText(
                    "Days 1–3: Glucose + Electrolytes + Vitamin C. Weeks 1–2: Multivitamin & Probiotics.",
                    "১ম–৩ দিন: গ্লুকোজ + ইলেকট্রোলাইট + ভিটামিন সি। ১ম–২ সপ্তাহ: মাল্টিভিটামিন ও প্রোবায়োটিক।"
                ),
                BilingualText(
                    "Day 45–60 & every 3 months: Dewormer in morning water, followed by liver tonic.",
                    "৪৫–৬০ দিন ও প্রতি ৩ মাস পর: সকালে কৃমিনাশক ওষুধ এবং পরদিন লিভার টনিক ও ভিটামিন।"
                ),
                BilingualText(
                    "Use Amprolium for bloody droppings or vet-guided antibiotics for CRD; always respect withdrawal days.",
                    "রক্ত আমাশয়ে অ্যাম্প্রোলিয়াম এবং ঠান্ডায় পশুচিকিৎসকের পরামর্শে অ্যান্টিবায়োটিক দিন; ডিম/মাংস বিক্রির বিরতি মানুন।"
                )
            )
        )
    )

    val duckHatchingSteps: List<GuideStepItem> = listOf(
        GuideStepItem(
            stepNumber = 1,
            stageBadge = BilingualText("Before Setting", "ডিম বসানোর আগে"),
            title = BilingualText("Select & Store Fertile Duck Eggs", "উর্বর হাঁসের ডিম বাছাই ও সংরক্ষণ"),
            details = BilingualText(
                "Collect clean, smooth, oval eggs weighing 65–75g from flocks with 1 healthy drake per 5–7 female ducks. Avoid cracked, overly dirty, or double-yolk eggs. Wipe dry spots gently—never soak in cold water. Store pointed-end down at 13–15°C (75% RH) for no longer than 7 days, tilting daily.",
                "সুস্থ পুরুষ হাঁস (৫–৭টি স্ত্রী হাঁসের জন্য ১টি) থাকা ঝাঁক থেকে ৬৫–৭৫ গ্রাম ওজনের পরিষ্কার ও মসৃণ ডিম বাছাই করুন। ফাটা বা অতিরিক্ত ছোট-বড় ডিম বাদ দিন। ঠান্ডা পানিতে ধোবেন না (জীবাণু সুরক্ষা স্তর নষ্ট হয়)। সূচালো দিক নিচে রেখে ১৩–১৫°সে তাপমাত্রায় সর্বোচ্চ ৭ দিন সংরক্ষণ করুন।"
            ),
            keySpec = BilingualText("Ratio: 1 Drake : 6 Ducks | Max storage: 7 days", "অনুপাত: ১ মর্দা : ৬ মাদি | সংরক্ষণ: সর্বোচ্চ ৭ দিন")
        ),
        GuideStepItem(
            stepNumber = 2,
            stageBadge = BilingualText("Days 1–9", "১ম–৯ম দিন"),
            title = BilingualText("Incubator Warmup, Turning & 1st Candling", "ইনকিউবেটর সেটিং, ঘুরানো ও ১ম ক্যান্ডলিং"),
            details = BilingualText(
                "Pre-run the clean, disinfected incubator for 12 hours to stabilize temperature at 37.5°C (99.5°F) and relative humidity at 55–60%. Turn eggs 3 to 5 times every day (an odd number so eggs don't rest on the same side every night). On Day 7, shine a bright candling light in a dark room: fertile eggs show a red network of blood vessels; remove clear infertile eggs.",
                "ডিম দেওয়ার আগে জীবাণুমুক্ত ইনকিউবেটর ১২ ঘণ্টা চালিয়ে তাপমাত্রা ৩৭.৫°সে (৯৯.৫°ফা) ও আর্দ্রতা ৫৫–৬০% স্থির করুন। প্রতিদিন ৩–৫ বার (বিজোড় সংখ্যায়) ডিম ঘুরান। ৭ম দিনে অন্ধকার ঘরে টর্চ/ক্যান্ডলার দিয়ে পরীক্ষা করুন—উর্বর ডিমে মাকড়সার জালের মতো রক্তনালী দেখা যাবে; স্বচ্ছ অনুর্বর ডিম সরিয়ে ফেলুন।"
            ),
            keySpec = BilingualText("Temp: 37.5°C | Humidity: 55–60% | Turn: 3–5x/day", "তাপ: ৩৭.৫°সে | আর্দ্রতা: ৫৫–৬০% | ঘুরানো: ৩–৫ বার/দিন")
        ),
        GuideStepItem(
            stepNumber = 3,
            stageBadge = BilingualText("Days 10–25", "১০ম–২৫তম দিন"),
            title = BilingualText("Daily Cooling & Lukewarm Mist Spray", "প্রতিদিন ঠান্ডা করা ও কুসুম গরম পানি স্প্রে"),
            details = BilingualText(
                "Duck eggs need cooling and extra moisture because their shells are thick and waxy. Once daily from Day 10 to Day 25, open the incubator for 10–15 minutes so eggs cool slightly (simulating the mother duck leaving the nest), then mist lightly with clean lukewarm water (37°C) before closing. Candle again on Day 14 to remove dead embryos ('blood rings').",
                "হাঁসের ডিমের খোসা পুরু ও তৈলাক্ত হওয়ায় আর্দ্রতা ও সামান্য ঠান্ডা হওয়া জরুরি। ১০ম থেকে ২৫তম দিন প্রতিদিন একবার ১০–১৫ মিনিটের জন্য ইনকিউবেটরের ঢাকনা খুলে রাখুন এবং ৩৭°সে কুসুম গরম পানি হালকা স্প্রে করে দিন। ১৪তম দিনে আবার ক্যান্ডলিং করে নষ্ট ভ্রূণ (রক্তের রিং) সরান।"
            ),
            keySpec = BilingualText("Cool: 10–15 mins/day | Mist: 37°C water daily", "ঠান্ডা: ১০–১৫ মি./দিন | স্প্রে: ৩৭°সে কুসুম গরম পানি")
        ),
        GuideStepItem(
            stepNumber = 4,
            stageBadge = BilingualText("Days 26–28", "২৬তম–২৮তম দিন"),
            title = BilingualText("Lockdown, High Humidity & Hatching", "লকডাউন, উচ্চ আর্দ্রতা ও বাচ্চা ফোটা"),
            details = BilingualText(
                "Candle on Day 25 and transfer live eggs (large air cell visible) to the hatching tray. STOP turning eggs completely. Lower temperature slightly to 37.0°C (98.6°F) and boost humidity to 75–80% using warm water trays/wet sponges. Keep the incubator closed during hatching—ducklings often rest 24–36 hours after pipping before zipping out. Let ducklings dry for 12–24 hrs before moving to the brooder.",
                "২৫তম দিনে শেষবার পরীক্ষা করে ডিম ঘুরানো সম্পূর্ণ বন্ধ করুন (লকডাউন)। তাপমাত্রা সামান্য কমিয়ে ৩৭.০°সে করুন এবং পানির ট্রে/ভেজা স্পঞ্জ দিয়ে আর্দ্রতা ৭৫–৮০% এ উন্নীত করুন। ডিম ফাটা শুরু হলে ঢাকনা খুলবেন না—খোসা ফাটার ২৪–৩৬ ঘণ্টা পর বাচ্চা পুরো বের হয়। বাচ্চা পুরো শুকানো ও ঝরঝরে হলে ব্রুডারে নিন।"
            ),
            keySpec = BilingualText("Temp: 37.0°C | Humidity: 75–80% | No turning", "তাপ: ৩৭.০°সে | আর্দ্রতা: ৭৫–৮০% | ঘুরানো বন্ধ")
        ),
        GuideStepItem(
            stepNumber = 5,
            stageBadge = BilingualText("Natural Method", "প্রাকৃতিক পদ্ধতি"),
            title = BilingualText("Using a Broody Hen or Muscovy Duck", "কুচে মুরগি বা চীনাহাঁস দিয়ে ডিম ফুটানো"),
            details = BilingualText(
                "A large broody hen can sit on 8–10 duck eggs (Muscovy ducks can cover 12–15). Place the nest on a slightly damp earthen base lined with dry straw in a quiet dark corner. Give the sitting bird feed and water once daily. In hot/dry weather, sprinkle a little lukewarm water on the eggs during the final 2 weeks.",
                "বড় কুচে দেশি মুরগির নিচে ৮–১০টি (এবং চীনাহাঁসের নিচে ১২–১৫টি) হাঁসের ডিম বসানো যায়। নিরিবিলি অন্ধকার কোণে হালকা ভেজা মাটির ওপর শুকনো খড় দিয়ে বাসা তৈরি করুন। তা দেওয়া পাখিকে দিনে ১ বার খাবার ও পানি দিন এবং শেষ ২ সপ্তাহে আবহাওয়া শুষ্ক হলে ডিমে সামান্য কুসুম গরম পানি ছিটিয়ে দিন।"
            ),
            keySpec = BilingualText("Broody hen: 8–10 eggs | Hatch time: 28 days", "কুচে মুরগি: ৮–১০টি ডিম | সময়: ২৮ দিন")
        )
    )

    val raiseDucksSteps: List<GuideStepItem> = listOf(
        GuideStepItem(
            stepNumber = 1,
            stageBadge = BilingualText("Weeks 1–3", "১ম–৩ সপ্তাহ"),
            title = BilingualText("Brooding Warmth & Bedding Hygiene", "বাচ্চার ব্রুডিং তাপমাত্রা ও শুষ্ক বিছানা"),
            details = BilingualText(
                "Pre-warm the chick guard area before ducklings arrive: 32–33°C in Week 1, 29°C in Week 2, and 25°C in Week 3. Use 3 inches of dry rice husk/wood shavings, and cover it with rough paper or burlap for the first 4 days so ducklings don't eat litter. Remove damp patches daily to prevent pneumonia and footpad sores.",
                "বাচ্চা আনার আগেই ব্রুডার গরম রাখুন: ১ম সপ্তাহ ৩২–৩৩°সে, ২য় সপ্তাহ ২৯°সে এবং ৩য় সপ্তাহ ২৫°সে। ৩ ইঞ্চি শুকনো তুষ/কাঠের গুঁড়ার ওপর প্রথম ৪ দিন খবরের কাগজ বা চট বিছিয়ে দিন যাতে বাচ্চা তুষ না খায়। ভেজা জায়গা প্রতিদিন বদলে দিন।"
            ),
            keySpec = BilingualText("Space: 0.5 -> 1.5 sq ft | Litter: Always dry", "জায়গা: ০.৫ -> ১.৫ বর্গফুট | বিছানা সবসময় শুষ্ক")
        ),
        GuideStepItem(
            stepNumber = 2,
            stageBadge = BilingualText("Water Setup", "পানির ব্যবস্থা"),
            title = BilingualText("Deep Drinkers Over Wire Mesh", "তারজালির ওপর গভীর পানির পাত্র"),
            details = BilingualText(
                "Ducks need to submerge their entire bill and eyes in clean water to wash feed dust out of their nostrils and sinuses—shallow chick drinkers cause eye infections and choking. However, splashing wets the floor. Solution: put drinkers on a 1/2-inch wire mesh platform above a catch tray/drain.",
                "হাঁসের চোখ ও নাকের ছিদ্র পরিষ্কার রাখতে পুরো ঠোঁট ডুবানো যায় এমন গভীর পানির পাত্র প্রয়োজন। কিন্তু মেঝে ভিজলে ঠান্ডা লেগে বাচ্চা মারা যায়। সমাধান: পানির পাত্র ১/২ ইঞ্চি তারজালি বা কাঠের মাচার ওপর রাখুন যাতে বাড়তি পানি নিচে ড্রেনে জমা হয়।"
            ),
            keySpec = BilingualText("Never feed mash without water right beside it", "পানি ছাড়া শুকনো গুঁড়া খাবার দেবেন না")
        ),
        GuideStepItem(
            stepNumber = 3,
            stageBadge = BilingualText("Feeding Plan", "খাদ্য ব্যবস্থাপনা"),
            title = BilingualText("Stage-Wise Duck Nutrition", "বয়স অনুযায়ী হাঁসের খাদ্য তালিকা"),
            details = BilingualText(
                "• Weeks 1–3 (Starter): 20–22% protein crumble or moist mash (50–70g/bird/day by Week 3).\n• Weeks 4–16 (Grower): 15–16% protein + chopped green grass, Azolla, duckweed, or snail meal.\n• Week 17+ (Layer): 17–18% protein (130–160g/day) + 3.5% calcium (oyster shell grit) for strong eggshells.\nNote: Avoid chicken feed containing coccidiostats (some drugs are toxic to ducklings!).",
                "• ১ম–৩ সপ্তাহ (স্টার্টার): ২০–২২% প্রোটিনযুক্ত ক্রাম্বল বা হালকা ভেজানো খাবার।\n• ৪–১৬ সপ্তাহ (গ্রোয়ার): ১৫–১৬% প্রোটিন + কুচানো ঘাস, অ্যাজোলা, ক্ষুদিপানা বা শামুকের গুঁড়া।\n• ১৭+ সপ্তাহ (লেয়ার): ১৭–১৮% প্রোটিন (দৈনিক ১৩০–১৬০ গ্রাম) + ঝিনুকের গুঁড়া (ক্যালসিয়াম)।\nসতর্কতা: কক্সিডিওস্ট্যাট মেশানো মুরগির ফিড হাঁসকে দেবেন না।"
            ),
            keySpec = BilingualText("Peak lay: ~280–300 eggs/yr (Khaki Campbell)", "উৎপাদন: বছরে প্রায় ২৮০–৩০০ ডিম (খাকি ক্যাম্পবেল)")
        ),
        GuideStepItem(
            stepNumber = 4,
            stageBadge = BilingualText("Housing & Pond", "বাসস্থান ও পুকুর"),
            title = BilingualText("Night Shelter & Swimming Care", "নিরাপদ রাতের ঘর ও পুকুর ব্যবস্থাপনা"),
            details = BilingualText(
                "Give 2–3 sq ft per adult laying duck in a predator-proof, well-ventilated house with 14–16 hours of total light for layers. Allow swimming from Week 3+ only in clean ponds during warm sunny hours; young ducklings (<2 wks) lack waterproofing oil and can chill and drown. Keep ducks in the dry shed until 9:00 AM so all morning eggs are laid cleanly in nest boxes.",
                "পূর্ণবয়স্ক হাঁসের জন্য ঘরে ২–৩ বর্গফুট জায়গা ও শিয়াল-বেজি থেকে সুরক্ষিত বেড়া দিন। ৩ সপ্তাহের আগে পুকুরে নামাবেন না (তখন পালকে তৈলাক্ত স্তর থাকে না, ঠান্ডা লেগে ডুবে যেতে পারে)। ভোর ৪টা–৮টায় ডিম পাড়ে বলে সকাল ৯টা পর্যন্ত শুকনো ঘরে খড়ের বাসায় রাখুন।"
            ),
            keySpec = BilingualText("1 nest box per 4 ducks | Lock until 9:00 AM", "প্রতি ৪ হাঁসে ১টি বাসা | সকাল ৯টা পর্যন্ত ঘরে রাখুন")
        )
    )

    val raiseChickensSteps: List<GuideStepItem> = listOf(
        GuideStepItem(
            stepNumber = 1,
            stageBadge = BilingualText("7 Days Before", "বাচ্চা আনার ৭ দিন আগে"),
            title = BilingualText("Shed Sanitation & Brooder Setup", "খামার ঘর জীবাণুমুক্তকরণ ও প্রস্তুতি"),
            details = BilingualText(
                "Scrub floors, feeders, and drinkers with detergent, spray disinfectant (e.g., quaternary ammonium/iodophor), and lime-wash walls 7–10 days before new chicks arrive. Place a disinfectant footbath at the door. Spread 3–4 inches of dry, dust-free rice husk litter and pre-heat the chick guard 12 hours before arrival.",
                "নতুন বাচ্চা আনার ৭–১০ দিন আগে ঘর, ফিডার ও ড্রিংকার সাবান ও জীবাণুনাশক দিয়ে ধুয়ে চুনকাম করুন। প্রবেশপথে ফুটবাথ রাখুন। মেঝেতে ৩–৪ ইঞ্চি শুকনো ধুলামুক্ত তুষ বিছিয়ে বাচ্চা আসার ১২ ঘণ্টা আগেই ব্রুডার গরম রাখুন।"
            ),
            keySpec = BilingualText("All-in All-out cycle reduces disease by >70%", "একসাথে তোলা-বিক্রি করলে রোগ ৭০% কমে")
        ),
        GuideStepItem(
            stepNumber = 2,
            stageBadge = BilingualText("Weeks 1–4", "১ম–৪ সপ্তাহ"),
            title = BilingualText("Precision Brooding Temperature Curve", "সপ্তাহভিত্তিক ব্রুডিং তাপমাত্রা নিয়ন্ত্রণ"),
            details = BilingualText(
                "• Days 1–3: 33–35°C | • Days 4–7: 31–32°C | • Week 2: 28–29°C | • Week 3: 25–26°C | • Week 4+: 21–24°C.\nObserve chick distribution: bunched under heater = cold; pressed against outer guard = too hot; piled on one side = cold draft; scattered evenly and active = perfect!",
                "• ১ম–৩ দিন: ৩৩–৩৫°সে | • ৪–৭ দিন: ৩১–৩২°সে | • ২য় সপ্তাহ: ২৮–২৯°সে | • ৩য় সপ্তাহ: ২৫–২৬°সে | • ৪র্থ+ সপ্তাহ: ২১–২৪°সে।\nবাচ্চার আচরণ দেখুন: বাতির নিচে জড়ো হলে ঠান্ডা; কিনারায় সরে গেলে গরম বেশি; চারদিকে সমান ছড়িয়ে খেলা করলে তাপমাত্রা একদম ঠিক।"
            ),
            keySpec = BilingualText("First 3 hrs: Glucose/Electrolyte water before feed", "প্রথম ৩ ঘণ্টা শুধু গ্লুকোজ ও ইলেকট্রোলাইট পানি দিন")
        ),
        GuideStepItem(
            stepNumber = 3,
            stageBadge = BilingualText("Feed & Water", "খাদ্য ও পানি"),
            title = BilingualText("Broiler vs. Layer Feeding Programs", "ব্রয়লার ও লেয়ার মুরগির খাদ্য পদ্ধতি"),
            details = BilingualText(
                "• Broiler (Market 30–35 days): Starter Crumble (Days 1–14, 22% protein) -> Grower Pellet (Days 15–24, 20%) -> Finisher (Days 25–35, 18.5%).\n• Layer / Sonali / Deshi: Chick Starter (0–6 wks, 20%) -> Grower (7–17 wks, 15%) -> Pre-lay/Layer Mash (18+ wks, 16.5% protein + 3.8% calcium).\nKeep feeders at bird back height to avoid feed wastage.",
                "• ব্রয়লার (৩০–৩৫ দিনে বিক্রি): স্টার্টার (১–১৪ দিন, ২২% প্রোটিন) -> গ্রোয়ার (১৫–২৪ দিন, ২০%) -> ফিনিশার (২৫–৩৫ দিন, ১৮.৫%)।\n• লেয়ার / সোনালী / দেশি: চিক স্টার্টার (০–৬ সপ্তাহ) -> গ্রোয়ার (৭–১৭ সপ্তাহ) -> লেয়ার ফিড (১৮+ সপ্তাহ, ৩.৮% ক্যালসিয়ামসহ)।"
            ),
            keySpec = BilingualText("Adult space: Broiler 1.2 sq ft | Layer 1.8 sq ft", "জায়গা: ব্রয়লার ১.২ বর্গফুট | লেয়ার ১.৮ বর্গফুট")
        ),
        GuideStepItem(
            stepNumber = 4,
            stageBadge = BilingualText("Light & Heat Care", "আলো ও গরম নিয়ন্ত্রণ"),
            title = BilingualText("Lighting Schedule & Summer Heat Protection", "আলোর সময়সূচি ও গ্রীষ্মকালীন সুরক্ষা"),
            details = BilingualText(
                "Give chicks 22–23 hrs light in Week 1, then taper for layers during growing (10–12 hrs) and increase cleanly to 16 hrs/day from Week 18 onward (never shorten light during egg laying!). During summer heat (>33°C), provide cool fresh water + Vitamin C, use roof sprinklers/fans, and feed mostly in cool morning/evening hours.",
                "১ম সপ্তাহে ২২–২৩ ঘণ্টা আলো দিন; বাড়ন্ত লেয়ারে ১০–১২ ঘণ্টা এবং ১৮ সপ্তাহ থেকে ডিম পাড়া পর্যন্ত স্থির ১৬ ঘণ্টা আলো রাখুন (ডিম পাড়ার সময় আলো কখনো কমাবেন না)। গরমে (>৩৩°সে) দুপুরে ঠান্ডা পানিতে ভিটামিন সি ও ইলেকট্রোলাইট দিন এবং সকাল-সন্ধ্যায় খাবার দিন।"
            ),
            keySpec = BilingualText("Stir damp litter daily & remove caked spots", "ভেজা বা জমাট বাঁধা তুষ প্রতিদিন নেড়ে বা সরিয়ে দিন")
        )
    )

    val vaccineSchedules: List<VaccineScheduleEntry> = listOf(
        // Chickens
        VaccineScheduleEntry(
            species = GuideSpecies.CHICKEN,
            ageTiming = BilingualText("Day 1 (Hatchery)", "১ম দিন (হ্যাচারি)"),
            vaccineName = BilingualText("Marek's Disease Vaccine", "মারেক্স ডিজিজ ভ্যাকসিন"),
            targetDisease = BilingualText("Marek's paralysis & tumors", "মারেক্স প্যারালাইসিস ও টিউমার"),
            route = BilingualText("Subcutaneous (neck) 0.2 ml", "চামড়ার নিচে ইনজেকশন (০.২ মি.লি.)"),
            doseAndNotes = BilingualText("Essential for all layer & breeding chicks at day-old.", "লেয়ার ও ব্রিডিং বাচ্চার জন্য ১ম দিনেই হ্যাচারিতে দেওয়া আবশ্যক।")
        ),
        VaccineScheduleEntry(
            species = GuideSpecies.CHICKEN,
            ageTiming = BilingualText("Days 5–7", "৫ম–৭ম দিন"),
            vaccineName = BilingualText("BCRDV (Ranikhet / Newcastle F/Lasota)", "বিসিআরডিভি (রানীক্ষেত - বাচ্চা)"),
            targetDisease = BilingualText("Newcastle Disease (Ranikhet)", "রানীক্ষেত রোগ"),
            route = BilingualText("Eye / Nostril drop (1 drop)", "চোখ বা নাকে ১ ফোঁটা"),
            doseAndNotes = BilingualText("Mix with chilled distilled diluent; keep on ice while vaccinating.", "ঠান্ডা ডিস্টিল্ড পানিতে মিশিয়ে আইসপ্যাকে রেখে ফোঁটা দিন।")
        ),
        VaccineScheduleEntry(
            species = GuideSpecies.CHICKEN,
            ageTiming = BilingualText("Days 12–14", "১২–১৪তম দিন"),
            vaccineName = BilingualText("Gumboro (IBD) Live Vaccine", "গামবোরো (আইবিডি) ভ্যাকসিন"),
            targetDisease = BilingualText("Infectious Bursal Disease", "গামবোরো (রোগ প্রতিরোধ ক্ষমতা ধ্বংস)"),
            route = BilingualText("Eye drop or Chlorine-free drinking water", "চোখে ফোঁটা বা ক্লোরিনমুক্ত পানির সাথে"),
            doseAndNotes = BilingualText("Withhold water 2 hrs prior; add 2g/L skim milk powder to stabilize live virus.", "২ ঘণ্টা আগে পানি বন্ধ রাখুন; লিটারপ্রতি ২ গ্রাম গুঁড়া দুধ মিশিয়ে দিন।")
        ),
        VaccineScheduleEntry(
            species = GuideSpecies.CHICKEN,
            ageTiming = BilingualText("Day 21", "২১তম দিন"),
            vaccineName = BilingualText("BCRDV Booster (Ranikhet)", "বিসিআরডিভি বুস্টার (রানীক্ষেত)"),
            targetDisease = BilingualText("Newcastle Disease Booster", "রানীক্ষেত রোগ প্রতিরোধ জোরদার"),
            route = BilingualText("Eye drop / Drinking water", "চোখে ফোঁটা / খাবার পানি"),
            doseAndNotes = BilingualText("Broilers usually stop vaccines after Day 21–24; layers continue.", "ব্রয়লার মুরগির সাধারণত ২১–২৪ দিনেই টিকা শেষ হয়; লেয়ারে চলবে।")
        ),
        VaccineScheduleEntry(
            species = GuideSpecies.CHICKEN,
            ageTiming = BilingualText("Day 28", "২৮তম দিন"),
            vaccineName = BilingualText("Gumboro (IBD) Booster", "গামবোরো (আইবিডি) বুস্টার"),
            targetDisease = BilingualText("Infectious Bursal Disease Booster", "গামবোরো বুস্টার"),
            route = BilingualText("Drinking water", "পানির সাথে"),
            doseAndNotes = BilingualText("Give Multivitamin + Vitamin C for 3 days afterwards.", "টিকার পর ৩ দিন মাল্টিভিটামিন ও ভিটামিন সি দিন।")
        ),
        VaccineScheduleEntry(
            species = GuideSpecies.CHICKEN,
            ageTiming = BilingualText("Weeks 6–8", "৬–৮ সপ্তাহ"),
            vaccineName = BilingualText("Fowl Pox Vaccine", "ফাউল পক্স (মুরগির বসন্ত) ভ্যাকসিন"),
            targetDisease = BilingualText("Avian Pox (skin & mouth scabs)", "মুরগির বসন্ত রোগ"),
            route = BilingualText("Wing-web prick (double needle)", "ডানার ঝিল্লিতে ফোঁড় (উইং-ওয়েব)"),
            doseAndNotes = BilingualText("Check for small 'take' swelling after 7 days to confirm immunity.", "৭ দিন পর ফোঁড়ার স্থানে সামান্য ফোলা দেখে টিকা সফল হয়েছে বুঝুন।")
        ),
        VaccineScheduleEntry(
            species = GuideSpecies.CHICKEN,
            ageTiming = BilingualText("Weeks 8–10 & Every 6 Mos", "৮–১০ সপ্তাহ ও প্রতি ৬ মাসে"),
            vaccineName = BilingualText("RDV (Adult Ranikhet) + Fowl Cholera", "আরডিভি (বড় রানীক্ষেত) ও ফাউল কলেরা"),
            targetDisease = BilingualText("Adult Newcastle & Pasteurellosis", "বড় মুরগির রানীক্ষেত ও কলেরা"),
            route = BilingualText("Intramuscular (RDV) / Subcutaneous (Cholera)", "মাংসে (আরডিভি) / চামড়ার নিচে (কলেরা)"),
            doseAndNotes = BilingualText("Crucial for layers, Sonali & Deshi hens; repeat every 6 months.", "লেয়ার, সোনালী ও দেশি মুরগির জন্য অত্যন্ত জরুরি; প্রতি ৬ মাসে বুস্টার দিন।")
        ),
        // Ducks
        VaccineScheduleEntry(
            species = GuideSpecies.DUCK,
            ageTiming = BilingualText("Days 15–21", "১৫–২১তম দিন"),
            vaccineName = BilingualText("Duck Plague (DVE) 1st Dose", "ডাক প্লেগ (হাঁসের মহামারি) ১ম ডোজ"),
            targetDisease = BilingualText("Duck Viral Enteritis (Duck Plague)", "ডাক প্লেগ রোগ"),
            route = BilingualText("Subcutaneous / IM (1.0 ml)", "চামড়ার নিচে বা মাংসে (১.০ মি.লি.)"),
            doseAndNotes = BilingualText("Most vital duck vaccine! Vaccinate healthy ducklings only on ice-chilled syringe.", "হাঁসের সবচেয়ে জরুরি টিকা! কেবল সুস্থ বাচ্চাকে ঠান্ডা ভ্যাকসিন দিন।")
        ),
        VaccineScheduleEntry(
            species = GuideSpecies.DUCK,
            ageTiming = BilingualText("Days 30–35", "৩০–৩৫তম দিন"),
            vaccineName = BilingualText("Duck Plague Booster", "ডাক প্লেগ বুস্টার ডোজ"),
            targetDisease = BilingualText("Duck Viral Enteritis Long Immunity", "ডাক প্লেগ দীর্ঘমেয়াদী সুরক্ষা"),
            route = BilingualText("Subcutaneous / IM (1.0 ml)", "চামড়ার নিচে বা মাংসে (১.০ মি.লি.)"),
            doseAndNotes = BilingualText("Builds strong flock immunity before pond access.", "পুকুরে নামানোর আগে শক্তিশালী রোগ প্রতিরোধ ক্ষমতা গড়ে তোলে।")
        ),
        VaccineScheduleEntry(
            species = GuideSpecies.DUCK,
            ageTiming = BilingualText("Days 42–45 (Week 6–7)", "৪২–৪৫তম দিন (৬–৭ সপ্তাহ)"),
            vaccineName = BilingualText("Duck Cholera 1st Dose", "ডাক কলেরা (হাঁসের কলেরা) ১ম ডোজ"),
            targetDisease = BilingualText("Avian Cholera (Pasteurella)", "হাঁসের কলেরা রোগ"),
            route = BilingualText("Subcutaneous neck/thigh (1.0 ml)", "চামড়ার নিচে (১.০ মি.লি.)"),
            doseAndNotes = BilingualText("Follow with booster after 3–4 weeks in high-risk wetland areas.", "জলাভূমি বা ঝুঁকিপূর্ণ এলাকায় ৩–৪ সপ্তাহ পর বুস্টার দিন।")
        ),
        VaccineScheduleEntry(
            species = GuideSpecies.DUCK,
            ageTiming = BilingualText("Week 16 & Every 6 Months", "১৬ সপ্তাহ ও প্রতি ৬ মাসে"),
            vaccineName = BilingualText("Duck Plague + Duck Cholera Booster", "ডাক প্লেগ ও ডাক কলেরা বুস্টার"),
            targetDisease = BilingualText("Pre-lay & Adult Flock Protection", "ডিম পাড়া শুরুর আগে ও নিয়মিত সুরক্ষা"),
            route = BilingualText("IM / SC (separate sides)", "মাংসে / চামড়ার নিচে (দুই পাশে আলাদা)"),
            doseAndNotes = BilingualText("Complete pre-lay boosters 2–3 weeks before egg laying begins.", "ডিম পাড়া শুরুর ২–৩ সপ্তাহ আগেই বুস্টার সম্পন্ন করুন।")
        )
    )

    val medicineSchedules: List<MedicineScheduleEntry> = listOf(
        MedicineScheduleEntry(
            species = GuideSpecies.ALL,
            timingOrCondition = BilingualText("Day 1–3 (Arrival)", "১ম–৩ দিন (বাচ্চা আনার পর)"),
            medicineOrSupplement = BilingualText("Glucose + Electrolytes + Vitamin C", "গ্লুকোজ + ইলেকট্রোলাইট + ভিটামিন সি"),
            purpose = BilingualText("Transport stress recovery & instant hydration", "ভ্রমণের ধকল কাটিয়ে ওঠা ও পানিশূন্যতা রোধ"),
            administrationAndSafety = BilingualText(
                "Mix in lukewarm drinking water for first 3–6 hours before introducing solid crumble feed.",
                "শক্ত খাবার দেওয়ার আগে প্রথম ৩–৬ ঘণ্টা হালকা কুসুম গরম পানির সাথে মিশিয়ে খাওয়ান।"
            )
        ),
        MedicineScheduleEntry(
            species = GuideSpecies.ALL,
            timingOrCondition = BilingualText("Days 4–7 & Post-Vaccination (3 days)", "৪–৭ দিন ও প্রতি টিকার পর ৩ দিন"),
            medicineOrSupplement = BilingualText("Multivitamin (AD3E + B-Complex) + Vitamin C", "মাল্টিভিটামিন (AD3E + B-Complex) ও ভিটামিন সি"),
            purpose = BilingualText("Boosts immunity, growth & lowers vaccine stress", "রোগ প্রতিরোধ ক্ষমতা বাড়ানো ও টিকার ধকল কমানো"),
            administrationAndSafety = BilingualText(
                "Give in morning drinking water for 3–5 days (never mix during live drinking-water vaccine hour).",
                "সকালের খাবার পানিতে ৩–৫ দিন দিন (পানিতে জীবন্ত টিকা দেওয়ার সময় সাথে মেশাবেন না)।"
            )
        ),
        MedicineScheduleEntry(
            species = GuideSpecies.ALL,
            timingOrCondition = BilingualText("Week 2–3 & Monthly (3–5 days)", "২য়–৩ সপ্তাহ ও প্রতি মাসে ৩–৫ দিন"),
            medicineOrSupplement = BilingualText("Probiotics + Liver Tonic + Enzymes", "প্রোবায়োটিক + লিভার টনিক + এনজাইম"),
            purpose = BilingualText("Healthy gut flora, better digestion & FCR", "হজমশক্তি বাড়ানো, লিভার সুস্থ রাখা ও ওজন বৃদ্ধি"),
            administrationAndSafety = BilingualText(
                "Helps prevent loose droppings and ammonia odor; safe for both ducks and chickens.",
                "পাতলা পায়খানা ও গ্যাস কমায়; হাঁস ও মুরগি উভয়ের জন্য নিরাপদ।"
            )
        ),
        MedicineScheduleEntry(
            species = GuideSpecies.ALL,
            timingOrCondition = BilingualText("Week 4–6 & Peak Laying Weeks", "৪–৬ সপ্তাহ ও ডিম পাড়ার সময়"),
            medicineOrSupplement = BilingualText("Calcium + Phosphorus + Vitamin D3", "ক্যালসিয়াম + ফসফরাস + ভিটামিন ডি৩"),
            purpose = BilingualText("Strong bones, leg strength & thick eggshells", "হাড় মজবুত রাখা, পায়ের দুর্বলতা রোধ ও শক্ত ডিমের খোসা"),
            administrationAndSafety = BilingualText(
                "Give 3–5 days/month during growth and whenever soft-shelled eggs appear.",
                "বাড়ন্ত বয়সে মাসে ৩–৫ দিন এবং নরম খোসার ডিম দেখলে সাথে সাথে দিন।"
            )
        ),
        MedicineScheduleEntry(
            species = GuideSpecies.ALL,
            timingOrCondition = BilingualText("Day 45–60 & Every 3 Months", "৪৫–৬০ দিন ও প্রতি ৩ মাস পর"),
            medicineOrSupplement = BilingualText("Dewormer (Levamisole / Albendazole / Piperazine)", "কৃমিনাশক (লিভামিসোল / অ্যালবেনডাজল / পাইপারাজিন)"),
            purpose = BilingualText("Clears roundworms & tapeworms that steal nutrients", "পাকস্থলীর কৃমি দূর করে খাদ্য হজম নিশ্চিত করা"),
            administrationAndSafety = BilingualText(
                "Give early morning after 2 hrs water fast; repeat per vet label and follow with 2 days liver tonic + B-complex.",
                "সকালে ২ ঘণ্টা পানি বন্ধ রেখে খাওয়ান; এরপর ২ দিন লিভার টনিক ও ভিটামিন বি-কমপ্লেক্স দিন। ডিম/মাংস বিরতি মেনে চলুন।"
            ),
            requiresVetCheck = true
        ),
        MedicineScheduleEntry(
            species = GuideSpecies.CHICKEN,
            timingOrCondition = BilingualText("Bloody / Chocolate Droppings (Weeks 2–6)", "রক্ত বা চকোলেট রঙের পায়খানা (২–৬ সপ্তাহ)"),
            medicineOrSupplement = BilingualText("Amprolium / Toltrazuril + Vitamin K", "অ্যাম্প্রোলিয়াম / টলট্রাজুরিল + ভিটামিন কে"),
            purpose = BilingualText("Treats Coccidiosis intestinal bleeding", "কক্সিডিওসিস (রক্ত আমাশয়) চিকিৎসা"),
            administrationAndSafety = BilingualText(
                "Start promptly under vet guidance, replace wet litter immediately, and check duck-safe dosing separately.",
                "দ্রুত পশুচিকিৎসকের পরামর্শে ডোজ শুরু করুন এবং ভেজা তুষ তৎক্ষণাৎ বদলে দিন।"
            ),
            requiresVetCheck = true
        ),
        MedicineScheduleEntry(
            species = GuideSpecies.ALL,
            timingOrCondition = BilingualText("Cold, Sneezing, Swollen Eyes (CRD)", "সর্দি, হাঁচি, চোখ ফোলা ও শ্বাসকষ্ট"),
            medicineOrSupplement = BilingualText("Vet-Prescribed Tylosin / Doxycycline + Bromhexine", "চিকিৎসকের পরামর্শে টাইলোসিন / ডক্সিসাইক্লিন + ব্রোমহেক্সিন"),
            purpose = BilingualText("Clears respiratory infection & airway mucus", "শ্বাসনালীর সংক্রমণ ও কফ পরিষ্কার করা"),
            administrationAndSafety = BilingualText(
                "Isolate sick birds, improve shed airflow without cold drafts, and strictly observe 7–14 day antibiotic withdrawal before selling eggs or meat.",
                "অসুস্থ পাখি আলাদা করুন, পশুচিকিৎসকের সঠিক মাত্রায় ৩–৫ দিন দিন এবং অ্যান্টিবায়োটিক শেষের ৭–১৪ দিন পর ডিম/মাংস বিক্রি করুন।"
            ),
            requiresVetCheck = true
        ),
        MedicineScheduleEntry(
            species = GuideSpecies.DUCK,
            timingOrCondition = BilingualText("Wet Feather / Weakness / Diarrhea", "পালক ভেজা, ঝিমুনি বা সবুজ-সাদা পায়খানা"),
            medicineOrSupplement = BilingualText("Vet-Guided Electrolyte + Broad-Spectrum Therapy", "চিকিৎসকের পরামর্শে ইলেকট্রোলাইট ও উপযুক্ত ওষুধ"),
            purpose = BilingualText("Prevents dehydration and secondary bacterial septicemia", "পানিশূন্যতা রোধ ও ব্যাকটেরিয়াল সংক্রমণ নিয়ন্ত্রণ"),
            administrationAndSafety = BilingualText(
                "Dry the floor immediately, isolate weak ducks in a warm pen, and consult a livestock officer before giving antibiotics.",
                "ঘর শুকনো রাখুন, দুর্বল হাঁস আলাদা উষ্ণ স্থানে রাখুন এবং অ্যান্টিবায়োটিক দেওয়ার আগে প্রাণীসম্পদ কর্মকর্তার পরামর্শ নিন।"
            ),
            requiresVetCheck = true
        )
    )
}
