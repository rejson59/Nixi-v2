package com.example.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.example.NixiApplication

class NixiAccessibilityService : AccessibilityService() {

    companion object {
        var instance: NixiAccessibilityService? = null
            private set
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        val app = application as? NixiApplication
        app?.storageManager?.logAction(
            "ACCESSIBILITY",
            "Tryb ręczny dostępny",
            "Usługa ułatwień dostępu NIXI aktywna. Gotowa do kontroli ekranu."
        )
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Monitored for screen element interactions
    }

    override fun onInterrupt() {}

    override fun onDestroy() {
        super.onDestroy()
        instance = null
    }

    fun clickAt(x: Float, y: Float) {
        val clickPath = Path().apply {
            moveTo(x, y)
        }
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(clickPath, 0, 80))
            .build()
        dispatchGesture(gesture, null, null)
    }

    fun findAndClickNodeByText(text: String): Boolean {
        val rootNode = rootInActiveWindow ?: return false
        val matchingNodes = rootNode.findAccessibilityNodeInfosByText(text)
        for (node in matchingNodes) {
            if (node.isClickable) {
                return node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            }
            var parent = node.parent
            while (parent != null) {
                if (parent.isClickable) {
                    return parent.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                }
                parent = parent.parent
            }
        }
        return false
    }
}
