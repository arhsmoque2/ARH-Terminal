package com.arh.terminal.mcp

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.graphics.Rect
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.arh.terminal.core.mcp.bridge.AccessibilityBridge
import org.json.JSONArray
import org.json.JSONObject
import java.lang.ref.WeakReference

class McpAccessibilityService : AccessibilityService(), AccessibilityBridge {

    companion object {
        var instance: WeakReference<McpAccessibilityService>? = null
            private set
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = WeakReference(this)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Active event listening for UI settle / window change
    }

    override fun onInterrupt() {
        // Service interrupted
    }

    override fun onDestroy() {
        super.onDestroy()
        if (instance?.get() == this) {
            instance = null
        }
    }

    override fun getScreenState(): JSONObject {
        val root = rootInActiveWindow
        val screenObj = JSONObject()
        val displayMetrics = resources.displayMetrics
        screenObj.put("width", displayMetrics.widthPixels)
        screenObj.put("height", displayMetrics.heightPixels)
        screenObj.put("densityDpi", displayMetrics.densityDpi)

        if (root == null) {
            screenObj.put("status", "no_root_window")
            screenObj.put("nodes", JSONArray())
            return screenObj
        }

        val nodesArray = JSONArray()
        dumpNodeHierarchy(root, nodesArray)
        screenObj.put("status", "ok")
        screenObj.put("nodesCount", nodesArray.length())
        screenObj.put("nodes", nodesArray)
        return screenObj
    }

    override fun findNodes(query: String): JSONArray {
        val root = rootInActiveWindow ?: return JSONArray()
        val results = JSONArray()
        val textMatches = root.findAccessibilityNodeInfosByText(query)
        for (node in textMatches) {
            results.put(nodeToJson(node))
        }
        val idMatches = root.findAccessibilityNodeInfosByViewId(query)
        for (node in idMatches) {
            results.put(nodeToJson(node))
        }
        return results
    }

    override fun performTap(x: Int, y: Int): Boolean {
        val path = Path().apply { moveTo(x.toFloat(), y.toFloat()) }
        val stroke = GestureDescription.StrokeDescription(path, 0L, 100L)
        val gesture = GestureDescription.Builder().addStroke(stroke).build()
        return dispatchGesture(gesture, null, null)
    }

    override fun performSwipe(startX: Int, startY: Int, endX: Int, endY: Int, durationMs: Long): Boolean {
        val path = Path().apply {
            moveTo(startX.toFloat(), startY.toFloat())
            lineTo(endX.toFloat(), endY.toFloat())
        }
        val stroke = GestureDescription.StrokeDescription(path, 0L, durationMs.coerceAtLeast(100L))
        val gesture = GestureDescription.Builder().addStroke(stroke).build()
        return dispatchGesture(gesture, null, null)
    }

    override fun performKey(keyCode: Int): Boolean {
        return when (keyCode) {
            4 -> performGlobalAction(GLOBAL_ACTION_BACK)
            3 -> performGlobalAction(GLOBAL_ACTION_HOME)
            187 -> performGlobalAction(GLOBAL_ACTION_RECENTS)
            else -> false
        }
    }

    override fun typeText(text: String): Boolean {
        val focused = rootInActiveWindow?.findFocus(AccessibilityNodeInfo.FOCUS_INPUT) ?: return false
        val args = android.os.Bundle().apply {
            putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text)
        }
        return focused.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
    }

    override fun getClipboard(): String? {
        val cm = getSystemService(CLIPBOARD_SERVICE) as? android.content.ClipboardManager
        val clip = cm?.primaryClip ?: return null
        return if (clip.itemCount > 0) clip.getItemAt(0).text?.toString() else null
    }

    override fun setClipboard(text: String): Boolean {
        val cm = getSystemService(CLIPBOARD_SERVICE) as? android.content.ClipboardManager ?: return false
        val clip = android.content.ClipData.newPlainText("agent-clipboard", text)
        cm.setPrimaryClip(clip)
        return true
    }

    override fun getNodeDetails(nodeId: String): JSONObject? {
        val root = rootInActiveWindow ?: return null
        val idMatches = root.findAccessibilityNodeInfosByViewId(nodeId)
        if (idMatches.isNotEmpty()) return nodeToJson(idMatches[0])
        val textMatches = root.findAccessibilityNodeInfosByText(nodeId)
        if (textMatches.isNotEmpty()) return nodeToJson(textMatches[0])
        return null
    }

    override fun openApp(packageName: String): Boolean {
        return try {
            val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
            if (launchIntent != null) {
                launchIntent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(launchIntent)
                true
            } else {
                false
            }
        } catch (_: Exception) {
            false
        }
    }

    override fun getDeviceLogs(): String = "AccessibilityService active. UI hierarchy events monitored."

    override fun getNotifications(): JSONArray = JSONArray()

    private fun dumpNodeHierarchy(node: AccessibilityNodeInfo, array: JSONArray) {
        array.put(nodeToJson(node))
        for (i in 0 until node.childCount) {
            node.getChild(i)?.let { child ->
                dumpNodeHierarchy(child, array)
            }
        }
    }

    private fun nodeToJson(node: AccessibilityNodeInfo): JSONObject {
        val bounds = Rect()
        node.getBoundsInScreen(bounds)
        return JSONObject().apply {
            put("className", node.className?.toString() ?: "")
            put("text", node.text?.toString() ?: "")
            put("contentDescription", node.contentDescription?.toString() ?: "")
            put("viewId", node.viewIdResourceName ?: "")
            put("isClickable", node.isClickable)
            put("isEditable", node.isEditable)
            put("bounds", "${bounds.left},${bounds.top},${bounds.right},${bounds.bottom}")
        }
    }
}
