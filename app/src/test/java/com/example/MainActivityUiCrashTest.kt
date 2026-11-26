package com.example

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class MainActivityUiCrashTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    private fun waitForTag(tag: String, timeoutMillis: Long = 10_000L) {
        composeRule.waitUntil(timeoutMillis = timeoutMillis) {
            composeRule.onAllNodesWithTag(tag, useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun scrollSettingsTo(tag: String) {
        composeRule.onNodeWithTag("settings_lazy_column").performScrollToNode(hasTestTag(tag))
    }

    @Test
    fun mainActivity_coldLaunch_navigatesFarmerCustomerAndSuperAdminWithoutCrash() {
        // 1. Cold start -> AuthScreen customer portal card
        waitForTag("auth_quick_language_toggle")

        // Switch to Customer Demo -> Marketplace -> Cart -> Orders -> Settings -> Log Out
        composeRule.onNodeWithTag("demo_customer_button").performScrollTo().performClick()
        waitForTag("customer_settings_button")
        composeRule.onNodeWithTag("customer_settings_button").performClick()

        waitForTag("settings_profile_card")
        scrollSettingsTo("settings_developer_contact_card")
        composeRule.onNodeWithTag("settings_developer_contact_card").assertIsDisplayed()
        scrollSettingsTo("settings_faq_toggle")
        composeRule.onNodeWithTag("settings_faq_toggle").performClick()
        scrollSettingsTo("settings_report_problem_button")
        composeRule.onNodeWithTag("settings_report_problem_button").performClick()
        waitForTag("support_subject_input")
        composeRule.onNodeWithText("Cancel").performClick()
        scrollSettingsTo("logout_button")
        composeRule.onNodeWithTag("logout_button").performClick()
        waitForTag("logout_confirm_button")
        composeRule.onNodeWithTag("logout_confirm_button").performClick()

        // 2. Farmer Login mode -> Farmer Demo -> Dashboard -> Overflow Modules -> Settings -> Log Out
        waitForTag("farmer_login_option_button")
        composeRule.onNodeWithTag("farmer_login_option_button").performScrollTo().performClick()
        waitForTag("demo_farmer_button")
        composeRule.onNodeWithTag("demo_farmer_button").performScrollTo().performClick()

        waitForTag("dashboard_settings_button")
        // Open primary bar tabs (Farms, Agriculture) and More overflow chips
        composeRule.onNodeWithText("Farms").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Agriculture").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("More").performClick()
        waitForTag("farmer_overflow_chips_row")
        listOf(
            "mobile_online_products_chip",
            "mobile_ai_ad_studio_chip",
            "mobile_livestock_chip",
            "mobile_guide_chip",
            "mobile_permissions_chip"
        ).forEach { chipTag ->
            composeRule.onNodeWithTag("farmer_overflow_chips_row").performScrollToNode(hasTestTag(chipTag))
            composeRule.onNodeWithTag(chipTag).performClick()
            composeRule.waitForIdle()
        }

        // Open Settings via primary bottom navigation bar
        composeRule.onNodeWithTag("nav_settings").performClick()

        waitForTag("settings_profile_card")
        composeRule.onNodeWithTag("settings_toggle_profile_editor").performClick()
        waitForTag("settings_save_profile_button")
        scrollSettingsTo("settings_preferences_card")
        composeRule.onNodeWithTag("settings_currency_button").performClick()
        composeRule.onNodeWithText("Done").performClick()
        composeRule.onNodeWithTag("settings_lang_bn").performClick()
        composeRule.onNodeWithTag("settings_lang_en").performClick()
        scrollSettingsTo("logout_button")
        composeRule.onNodeWithTag("logout_button").performClick()
        waitForTag("logout_confirm_button")
        composeRule.onNodeWithTag("logout_confirm_button").performClick()

        // 3. Super Admin Login -> All Admin Tabs + Online Shop Moderation + Admin Settings -> Log Out
        waitForTag("auth_quick_language_toggle")
        composeRule.onNodeWithText("Private Developer / Super Admin Access").performScrollTo().performClick()
        waitForTag("admin_login_email_input")
        composeRule.onNodeWithTag("admin_login_email_input").performTextInput("admin@smartfarm.app")
        composeRule.onNodeWithTag("admin_login_password_input").performTextInput("admin@2026")
        composeRule.onNodeWithTag("admin_login_pin_input").performTextInput("2026")
        composeRule.onNodeWithTag("admin_unlock_console_button").performClick()

        waitForTag("admin_settings_button")
        composeRule.onNodeWithText("Users").performClick()
        waitForTag("admin_subtab_shop_moderation")
        composeRule.onNodeWithTag("admin_subtab_shop_moderation").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Permissions").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Plans").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Finance").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("System").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("admin_settings_button").performClick()
        waitForTag("settings_profile_card")
        scrollSettingsTo("settings_admin_security_card")
        composeRule.onNodeWithTag("settings_change_admin_password_button").performClick()
        composeRule.onNodeWithText("Cancel").performClick()
        scrollSettingsTo("logout_button")
        composeRule.onNodeWithTag("logout_button").performClick()
        waitForTag("logout_confirm_button")
        composeRule.onNodeWithTag("logout_confirm_button").performClick()
        waitForTag("auth_quick_language_toggle")
    }
}

