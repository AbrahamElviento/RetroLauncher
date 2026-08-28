package com.example.service

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.view.accessibility.AccessibilityEvent
import android.widget.Toast

class LauncherAccessibilityService : AccessibilityService() {

    companion object {
        var instance: LauncherAccessibilityService? = null
            private set

        fun isRunning(): Boolean = instance != null

        fun performRecents(): Boolean {
            val service = instance ?: return false
            return service.performGlobalAction(GLOBAL_ACTION_RECENTS)
        }

        fun performNotifications(): Boolean {
            val service = instance ?: return false
            return service.performGlobalAction(GLOBAL_ACTION_NOTIFICATIONS)
        }

        fun performQuickSettings(): Boolean {
            val service = instance ?: return false
            return service.performGlobalAction(GLOBAL_ACTION_QUICK_SETTINGS)
        }

        fun performLockScreen(): Boolean {
            val service = instance ?: return false
            return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                service.performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN)
            } else {
                false
            }
        }

        fun lockScreen(context: Context) {
            if (performLockScreen()) {
                return
            }
            try {
                Toast.makeText(
                    context,
                    "Enable Accessibility Service for Retro Launcher to Lock Screen",
                    Toast.LENGTH_LONG
                ).show()
                val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        fun openRecentApps(context: Context) {
            // 1. Try global action via AccessibilityService (works reliably on Android 8+)
            if (performRecents()) {
                return
            }

            // 2. Try reflection via StatusBarManager or IStatusBarService
            try {
                val statusBarService = context.getSystemService("statusbar")
                val statusBarManager = Class.forName("android.app.StatusBarManager")
                try {
                    val toggleMethod = statusBarManager.getMethod("toggleRecentApps")
                    toggleMethod.isAccessible = true
                    toggleMethod.invoke(statusBarService)
                    return
                } catch (e: Exception) {
                    val showMethod = statusBarManager.getMethod("showRecentApps")
                    showMethod.isAccessible = true
                    showMethod.invoke(statusBarService)
                    return
                }
            } catch (ignored: Exception) {
            }

            // 3. Fallback: Prompt user to enable Accessibility Service
            try {
                Toast.makeText(
                    context,
                    "Enable Accessibility Service for Retro Launcher to open Recent Apps",
                    Toast.LENGTH_LONG
                ).show()
                val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // No event listening required
    }

    override fun onInterrupt() {
        // No interruption handling required
    }

    override fun onDestroy() {
        super.onDestroy()
        if (instance == this) {
            instance = null
        }
    }
}
