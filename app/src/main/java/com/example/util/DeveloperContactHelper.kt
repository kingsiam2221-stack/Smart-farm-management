package com.example.util

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri

object DeveloperContactHelper {

    fun normalizePhoneForDial(raw: String): String {
        val trimmed = raw.trim()
        if (trimmed.isBlank()) return ""
        val sb = StringBuilder()
        trimmed.forEachIndexed { idx, ch ->
            if (ch.isDigit()) {
                sb.append(ch)
            } else if (ch == '+' && sb.isEmpty() && idx == 0) {
                sb.append(ch)
            }
        }
        val result = sb.toString()
        return if (result.any { it.isDigit() }) result else ""
    }

    fun normalizePhoneForWhatsApp(raw: String): String {
        val digitsOnly = raw.filter { it.isDigit() }
        if (digitsOnly.isBlank()) return ""
        return when {
            // Convert local Bangladesh mobile number (01XXXXXXXXX) to international 8801XXXXXXXXX for wa.me
            digitsOnly.length == 11 && digitsOnly.startsWith("01") -> "88$digitsOnly"
            digitsOnly.startsWith("00") && digitsOnly.length > 4 -> digitsOnly.drop(2)
            else -> digitsOnly
        }
    }

    fun buildDialIntent(phone: String): Intent? {
        val dial = normalizePhoneForDial(phone)
        if (dial.isBlank()) return null
        return Intent(Intent.ACTION_DIAL, Uri.parse("tel:$dial"))
    }

    fun buildWhatsAppIntent(
        context: Context,
        whatsAppNumber: String,
        prefillMessage: String
    ): Intent? {
        val digits = normalizePhoneForWhatsApp(whatsAppNumber)
        if (digits.isBlank()) return null
        val encoded = Uri.encode(prefillMessage)
        val waUri = Uri.parse("https://wa.me/$digits?text=$encoded")

        for (pkg in listOf("com.whatsapp", "com.whatsapp.w4b")) {
            val targeted = Intent(Intent.ACTION_VIEW, waUri).apply {
                setPackage(pkg)
                if (context !is Activity) addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            if (targeted.resolveActivity(context.packageManager) != null) {
                return targeted
            }
        }

        return Intent(Intent.ACTION_VIEW, waUri).apply {
            if (context !is Activity) addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    fun buildEmailIntent(
        email: String,
        subject: String,
        body: String
    ): Intent? {
        val cleanEmail = email.trim()
        if (cleanEmail.isBlank() || !cleanEmail.contains("@")) return null
        return Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:$cleanEmail")).apply {
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, body)
        }
    }

    fun launchCallDeveloper(context: Context, phone: String): Boolean {
        val intent = buildDialIntent(phone) ?: return false
        if (context !is Activity) intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return runCatching {
            context.startActivity(intent)
            true
        }.getOrDefault(false)
    }

    fun launchWhatsAppDeveloper(
        context: Context,
        whatsAppNumber: String,
        prefillMessage: String
    ): Boolean {
        val intent = buildWhatsAppIntent(context, whatsAppNumber, prefillMessage) ?: return false
        return runCatching {
            context.startActivity(intent)
            true
        }.getOrDefault(false)
    }

    fun launchEmailDeveloper(
        context: Context,
        email: String,
        subject: String,
        body: String
    ): Boolean {
        val intent = buildEmailIntent(email, subject, body) ?: return false
        if (context !is Activity) intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return runCatching {
            context.startActivity(intent)
            true
        }.getOrDefault(false)
    }
}
