package com.aura.agent.data

import android.content.Context
import androidx.core.content.edit

class AuraMemoryStore(context: Context) {
    private val prefs = context.getSharedPreferences("aura_memory", Context.MODE_PRIVATE)

    fun addDecision(action: String, summary: String) {
        val index = prefs.getInt("decision_count", 0)
        prefs.edit {
            putString("decision_${index}", "$action|$summary")
            putInt("decision_count", index + 1)
        }
    }

    fun recentDecisions(limit: Int = 5): List<String> {
        val count = prefs.getInt("decision_count", 0)
        return (0 until count.coerceAtMost(limit)).mapNotNull { idx ->
            prefs.getString("decision_${count - idx - 1}", null)
        }
    }
}
