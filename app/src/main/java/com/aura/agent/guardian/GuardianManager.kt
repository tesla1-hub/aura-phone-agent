package com.aura.agent.guardian

import android.content.Context
import android.content.SharedPreferences

data class GuardianDecision(
    val action: String,
    val riskScore: Int,
    val approved: Boolean
)

class GuardianManager(private val context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("aura_guardian", Context.MODE_PRIVATE)

    fun setGuardianEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_GUARDIAN_ENABLED, enabled).apply()
    }

    fun isGuardianEnabled(): Boolean = prefs.getBoolean(KEY_GUARDIAN_ENABLED, true)

    fun shouldRequestApproval(action: String): Boolean {
        val enabled = isGuardianEnabled()
        val risk = evaluateRisk(action)
        return enabled && risk >= 50
    }

    fun evaluateRisk(action: String): Int {
        return when (action.lowercase()) {
            "call" -> 90
            "sms" -> 85
            "system_setting" -> 95
            "open_app" -> 15
            "tap" -> 10
            "scroll" -> 10
            else -> 30
        }
    }

    fun getRiskProfile(): String {
        return if (isGuardianEnabled()) "Guardian active" else "Guardian off"
    }

    companion object {
        private const val KEY_GUARDIAN_ENABLED = "guardian_enabled"
    }
}
