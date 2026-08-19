package com.arh.terminal.core.mcp

import com.arh.terminal.core.mcp.bridge.AccessibilityBridge
import com.arh.terminal.core.mcp.bridge.TreeFingerprint
import com.arh.terminal.core.mcp.tools.AndroidToolRegistry
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AndroidToolRegistryBehavioralTest {

    private class FakeDynamicAccessibilityBridge : AccessibilityBridge {
        var currentScreenState: JSONObject = JSONObject()
        val dispatchedGestures = mutableListOf<String>()
        val launchedApps = mutableListOf<String>()
        var customLogs: String = "Log entry A"
        var customNotifications: JSONArray = JSONArray()
        private var clip: String = ""

        override fun getScreenState(): JSONObject = currentScreenState
        override fun findNodes(query: String): JSONArray {
            val arr = JSONArray()
            val nodes = currentScreenState.optJSONArray("nodes") ?: return arr
            for (i in 0 until nodes.length()) {
                val n = nodes.getJSONObject(i)
                if (n.optString("text").contains(query) || n.optString("viewId").contains(query)) {
                    arr.put(n)
                }
            }
            return arr
        }

        override fun getNodeDetails(nodeId: String): JSONObject? {
            val nodes = findNodes(nodeId)
            return if (nodes.length() > 0) nodes.getJSONObject(0) else null
        }

        override fun performTap(x: Int, y: Int): Boolean {
            dispatchedGestures.add("tap:$x,$y")
            return true
        }

        override fun performSwipe(startX: Int, startY: Int, endX: Int, endY: Int, durationMs: Long): Boolean {
            dispatchedGestures.add("swipe:$startX,$startY->$endX,$endY($durationMs)")
            return true
        }

        override fun performKey(keyCode: Int): Boolean = true
        override fun typeText(text: String): Boolean = true
        override fun getClipboard(): String = clip
        override fun setClipboard(text: String): Boolean {
            clip = text
            return true
        }

        override fun openApp(packageName: String): Boolean {
            launchedApps.add(packageName)
            return true
        }

        override fun getDeviceLogs(): String = customLogs
        override fun getNotifications(): JSONArray = customNotifications
    }

    @Test
    fun getScreenStateReturnsDynamicContentNotCannedString() {
        val bridge = FakeDynamicAccessibilityBridge()
        val registry = AndroidToolRegistry(bridge)

        // Frame 1: Home screen with Login button
        bridge.currentScreenState = JSONObject().apply {
            put("status", "ok")
            put("nodes", JSONArray().apply {
                put(JSONObject().apply {
                    put("className", "android.widget.Button")
                    put("text", "Login")
                    put("isClickable", true)
                    put("bounds", "[100,200][300,400]")
                })
            })
        }
        val out1 = registry.executeTool("android_get_screen_state", JSONObject())
        assertTrue(out1.toString().contains("Login"))

        // Frame 2: Dashboard screen with Welcome text
        bridge.currentScreenState = JSONObject().apply {
            put("status", "ok")
            put("nodes", JSONArray().apply {
                put(JSONObject().apply {
                    put("className", "android.widget.TextView")
                    put("text", "Welcome Administrator")
                    put("isClickable", false)
                    put("bounds", "[50,50][400,100]")
                })
            })
        }
        val out2 = registry.executeTool("android_get_screen_state", JSONObject())
        assertTrue(out2.toString().contains("Welcome Administrator"))
        assertNotEquals(out1.toString(), out2.toString()) // Differential check
    }

    @Test
    fun performTapDispatchesExactDynamicCoordinates() {
        val bridge = FakeDynamicAccessibilityBridge()
        val registry = AndroidToolRegistry(bridge)

        val args1 = JSONObject().apply { put("x", 142); put("y", 380) }
        val args2 = JSONObject().apply { put("x", 720); put("y", 1280) }

        registry.executeTool("android_tap", args1)
        registry.executeTool("android_tap", args2)

        assertEquals(2, bridge.dispatchedGestures.size)
        assertEquals("tap:142,380", bridge.dispatchedGestures[0])
        assertEquals("tap:720,1280", bridge.dispatchedGestures[1])
        assertNotEquals(bridge.dispatchedGestures[0], bridge.dispatchedGestures[1])
    }

    @Test
    fun openAppAndNodeDetailsExecuteDynamically() {
        val bridge = FakeDynamicAccessibilityBridge()
        bridge.currentScreenState = JSONObject().apply {
            put("status", "ok")
            put("nodes", JSONArray().apply {
                put(JSONObject().apply {
                    put("viewId", "com.example:id/submit_btn")
                    put("text", "Submit Order")
                })
            })
        }
        val registry = AndroidToolRegistry(bridge)

        val openRes = registry.executeTool("android_open_app", JSONObject().put("package_name", "com.android.chrome"))
        assertTrue(openRes.content[0].text?.contains("com.android.chrome") == true)
        assertEquals(listOf("com.android.chrome"), bridge.launchedApps)

        val nodeRes = registry.executeTool("android_get_node_details", JSONObject().put("node_id", "submit_btn"))
        assertTrue(nodeRes.content[0].text?.contains("Submit Order") == true)
    }

    @Test
    fun treeFingerprintProducesUniqueHashesForDistinctHierarchies() {
        val treeA = JSONObject().apply {
            put("nodes", JSONArray().apply {
                put(JSONObject().apply {
                    put("type", "Button")
                    put("text", "Submit")
                    put("bounds", "[0,0][100,100]")
                })
            })
        }

        val treeB = JSONObject().apply {
            put("nodes", JSONArray().apply {
                put(JSONObject().apply {
                    put("type", "Button")
                    put("text", "Cancel")
                    put("bounds", "[0,0][100,100]")
                })
            })
        }

        val hashA1 = TreeFingerprint.computeFingerprint(treeA)
        val hashA2 = TreeFingerprint.computeFingerprint(treeA)
        val hashB = TreeFingerprint.computeFingerprint(treeB)

        assertEquals(hashA1, hashA2) // Deterministic
        assertNotEquals(hashA1, hashB) // Distinct
    }
}
