package com.example.navigation

/**
 * Type-safe, declarative Navigation Definitions for DentBridge (Kotlin & Jetpack Compose).
 * Maps directly to the multi-tenant dashboard workflow requirements.
 */
sealed class Screen(val route: String) {
    // Authentication Journey
    object Splash : Screen("splash")
    object Login : Screen("login")
    object Register : Screen("register")
    object ForgotPassword : Screen("forgot_password")

    // Role-Specific Entry Gateways
    object DentistDashboard : Screen("dashboard_dentist")
    object LabAdminDashboard : Screen("dashboard_lab_admin")
    object TechnicianDashboard : Screen("dashboard_technician")
    object ClinicAdminDashboard : Screen("dashboard_clinic_admin")

    // Shared Collaborative Features
    object CaseDetails : Screen("case_details/{caseId}") {
        fun createRoute(caseId: String) = "case_details/$caseId"
    }
    object ChatChannel : Screen("chat/{channelId}") {
        fun createRoute(channelId: String) = "chat/$channelId"
    }
    object InteractiveBlueprint : Screen("blueprint")
    object UserProfileSettings : Screen("settings")
}
