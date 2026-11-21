package com.example.i18n

import java.text.DecimalFormat
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.roundToLong

enum class AppLanguage(val code: String, val displayName: String, val nativeName: String) {
    EN("en", "English", "English"),
    BN("bn", "Bangla", "বাংলা")
}

enum class CurrencyOption(
    val code: String,
    val symbol: String,
    val countryName: String,
    val rateFromBdt: Double,
    val gateways: List<String>
) {
    BDT("BDT", "৳", "Bangladesh", 1.0, listOf("bKash", "Nagad", "SSLCommerz", "Manual Verification")),
    USD("USD", "$", "United States", 0.0084, listOf("Stripe", "Credit/Debit Card", "Manual Verification")),
    INR("INR", "₹", "India", 0.70, listOf("UPI", "Razorpay", "Stripe", "Manual Verification")),
    GBP("GBP", "£", "United Kingdom", 0.0066, listOf("Stripe", "Card", "Manual Verification")),
    EUR("EUR", "€", "European Union", 0.0078, listOf("Stripe", "SEPA", "Manual Verification")),
    AED("AED", "د.إ", "UAE", 0.031, listOf("Stripe", "Telr", "Manual Verification")),
    SAR("SAR", "﷼", "Saudi Arabia", 0.0315, listOf("Mada", "Stripe", "Manual Verification")),
    MYR("MYR", "RM", "Malaysia", 0.037, listOf("FPX", "Stripe", "Manual Verification")),
    CAD("CAD", "C$", "Canada", 0.0115, listOf("Stripe", "Interac", "Manual Verification")),
    AUD("AUD", "A$", "Australia", 0.0128, listOf("Stripe", "Card", "Manual Verification"));

    companion object {
        fun fromCode(code: String): CurrencyOption =
            entries.firstOrNull { it.code.equals(code, ignoreCase = true) } ?: BDT
    }
}

object CurrencyFormatter {
    fun convertFromBase(amountInBdt: Double, target: CurrencyOption): Double {
        return amountInBdt * target.rateFromBdt
    }

    fun toBaseBdt(amountInTarget: Double, source: CurrencyOption): Double {
        return if (source.rateFromBdt <= 0.0) amountInTarget else amountInTarget / source.rateFromBdt
    }

    fun format(amountInBdt: Double, currency: CurrencyOption, decimals: Int = 2): String {
        val converted = convertFromBase(amountInBdt, currency)
        val pattern = if (decimals == 0 || converted % 1.0 == 0.0 && currency == CurrencyOption.BDT) {
            "#,##0"
        } else {
            "#,##0.00"
        }
        val df = (NumberFormat.getNumberInstance(Locale.US) as DecimalFormat).apply {
            applyPattern(pattern)
        }
        val sign = if (converted < 0) "-" else ""
        return "$sign${currency.symbol}${df.format(kotlin.math.abs(converted))}"
    }

    fun formatCompact(amountInBdt: Double, currency: CurrencyOption): String {
        val converted = convertFromBase(amountInBdt, currency)
        val absVal = kotlin.math.abs(converted)
        val sign = if (converted < 0) "-" else ""
        return when {
            absVal >= 1_000_000 -> "$sign${currency.symbol}${String.format(Locale.US, "%.1fM", absVal / 1_000_000)}"
            absVal >= 1_000 -> "$sign${currency.symbol}${String.format(Locale.US, "%.1fK", absVal / 1_000)}"
            else -> "$sign${currency.symbol}${absVal.roundToLong()}"
        }
    }
}
