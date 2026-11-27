package com.example

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import org.junit.Assert.assertTrue
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
            "mobile_staff_hr_chip",
            "mobile_guide_chip",
            "mobile_permissions_chip"
        ).forEach { chipTag ->
            composeRule.onNodeWithTag("farmer_overflow_chips_row").performScrollToNode(hasTestTag(chipTag))
            composeRule.onNodeWithTag(chipTag).performClick()
            composeRule.waitForIdle()
        }

        // Verify Staff HR, Targets (+10/+20 reward) & Auto-Message screen renders & opens dialogs cleanly
        composeRule.onNodeWithTag("farmer_overflow_chips_row").performScrollToNode(hasTestTag("mobile_staff_hr_chip"))
        composeRule.onNodeWithTag("mobile_staff_hr_chip").performClick()
        waitForTag("assign_target_fab")
        composeRule.onNodeWithTag("assign_target_fab").performClick()
        waitForTag("target_title_input")
        composeRule.onNodeWithText("Cancel").performClick()
        composeRule.onNodeWithTag("staff_hr_tab_payroll").performClick()
        waitForTag("record_staff_pay_fab")
        composeRule.onNodeWithTag("staff_hr_tab_messages").performClick()
        waitForTag("send_auto_msg_fab")
        composeRule.onNodeWithTag("send_auto_msg_fab").performClick()
        waitForTag("auto_msg_body_input")
        composeRule.onNodeWithText("Cancel").performClick()


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
        waitForTag("admin_system_lazy_column")
        composeRule.onNodeWithTag("admin_system_lazy_column").performScrollToNode(hasTestTag("admin_toggle_customer_demo"))
        composeRule.onNodeWithTag("admin_toggle_customer_demo").performClick()
        composeRule.onNodeWithTag("admin_system_lazy_column").performScrollToNode(hasTestTag("admin_login_banner_title_input"))
        composeRule.onNodeWithTag("admin_login_banner_title_input").performTextInput("Custom Agro Portal")
        composeRule.onNodeWithTag("admin_system_lazy_column").performScrollToNode(hasTestTag("admin_save_login_screen_button"))
        composeRule.onNodeWithTag("admin_save_login_screen_button").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000L) {
            composeRule.onAllNodes(androidx.compose.ui.test.hasText("System & payment settings saved."), useUnmergedTree = true)
                .fetchSemanticsNodes().isNotEmpty()
        }

        // Edit Order Options (turn off bKash & Bank, save custom order note)
        composeRule.onNodeWithTag("admin_system_lazy_column").performScrollToNode(hasTestTag("admin_toggle_order_bkash"))
        composeRule.onNodeWithTag("admin_toggle_order_bkash").performClick()
        composeRule.onNodeWithTag("admin_system_lazy_column").performScrollToNode(hasTestTag("admin_toggle_order_bank"))
        composeRule.onNodeWithTag("admin_toggle_order_bank").performClick()
        composeRule.onNodeWithTag("admin_system_lazy_column").performScrollToNode(hasTestTag("admin_order_note_input"))
        composeRule.onNodeWithTag("admin_order_note_input").performTextInput("Pay via Nagad or COD")
        composeRule.onNodeWithTag("admin_system_lazy_column").performScrollToNode(hasTestTag("admin_save_order_options_button"))
        composeRule.onNodeWithTag("admin_save_order_options_button").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000L) {
            composeRule.onAllNodes(androidx.compose.ui.test.hasText("System & payment settings saved."), useUnmergedTree = true)
                .fetchSemanticsNodes().isNotEmpty()
        }

        composeRule.onNodeWithTag("admin_settings_button").performClick()
        waitForTag("settings_profile_card")
        scrollSettingsTo("settings_admin_security_card")
        composeRule.onNodeWithTag("settings_change_admin_password_button").performClick()
        composeRule.onNodeWithText("Cancel").performClick()
        composeRule.waitUntil(timeoutMillis = 7_000L) {
            composeRule.onAllNodes(androidx.compose.ui.test.hasText("System & payment settings saved."), useUnmergedTree = true)
                .fetchSemanticsNodes().isEmpty()
        }
        scrollSettingsTo("logout_button")
        composeRule.onNodeWithTag("logout_button").performClick()
        waitForTag("logout_confirm_button")
        composeRule.onNodeWithTag("logout_confirm_button").performClick()
        waitForTag("auth_quick_language_toggle")
        composeRule.waitUntil(timeoutMillis = 5_000L) {
            composeRule.onAllNodes(androidx.compose.ui.test.hasText("Custom Agro Portal"), useUnmergedTree = true)
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("auth_banner_title").assertTextEquals("Custom Agro Portal")
        assertTrue(composeRule.onAllNodesWithTag("demo_customer_button", useUnmergedTree = true).fetchSemanticsNodes().isEmpty())
    }
}

