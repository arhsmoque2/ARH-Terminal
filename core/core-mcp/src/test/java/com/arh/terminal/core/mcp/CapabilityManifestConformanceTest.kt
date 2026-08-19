package com.arh.terminal.core.mcp

import com.arh.terminal.core.mcp.bridge.DefaultAccessibilityBridge
import com.arh.terminal.core.mcp.tools.AndroidToolRegistry
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class CapabilityManifestConformanceTest {

    private val bridge = DefaultAccessibilityBridge()
    private val registry = AndroidToolRegistry(bridge)

    @Test
    fun registeredToolsMatchCapabilitiesManifest() {
        val manifestFile = File("../../capabilities.json")
        val manifestJson = if (manifestFile.exists()) {
            JSONObject(manifestFile.readText(Charsets.UTF_8))
        } else {
            // Fallback lookup if working directory is root
            JSONObject(File("capabilities.json").readText(Charsets.UTF_8))
        }

        val toolsArray = manifestJson.getJSONArray("tools")
        val declaredNames = mutableSetOf<String>()
        for (i in 0 until toolsArray.length()) {
            declaredNames.add(toolsArray.getJSONObject(i).getString("name"))
        }

        val registeredTools = registry.listTools().map { it.name }.toSet()

        assertEquals("Registered tool count must equal declared manifest count", declaredNames.size, registeredTools.size)
        assertEquals("Registered tool names must strictly match declared manifest", declaredNames, registeredTools)
    }

    @Test
    fun differentialExecutionOutputsVaryDynamically() {
        val tap1 = registry.executeTool("android_tap", JSONObject().put("x", 100).put("y", 200))
        val tap2 = registry.executeTool("android_tap", JSONObject().put("x", 500).put("y", 600))

        assertNotNull(tap1.content[0].text)
        assertNotNull(tap2.content[0].text)
        // Ensure differential inputs produce distinct output strings
        assertTrue(tap1.content[0].text != tap2.content[0].text)
    }
}
