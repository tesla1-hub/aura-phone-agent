package com.aura.agent.service

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log

class AuraNotificationListenerService : NotificationListenerService() {

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val packageName = sbn.packageName
        val title = sbn.notification.extras["android.title"]?.toString() ?: "Unknown"
        val text = sbn.notification.extras["android.text"]?.toString() ?: ""
        Log.d("AURA", "Notification from $packageName: $title - $text")
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        Log.d("AURA", "Notification removed from ${sbn.packageName}")
    }
}
