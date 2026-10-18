package com.example.automation

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.graphics.Rect
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import kotlinx.coroutines.CompletableDeferred

class NafsiAccessibilityService : AccessibilityService() {

    companion object {
        var instance: NafsiAccessibilityService? = null
            private set

        val isServiceEnabled: Boolean
            get() = instance != null
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Track window transitions
    }

    override fun onInterrupt() {
        // Handle interruption
    }

    override fun onDestroy() {
        super.onDestroy()
        if (instance == this) {
            instance = null
        }
    }

    fun getAllVisibleTexts(): List<String> {
        val root = rootInActiveWindow ?: return emptyList()
        val texts = mutableListOf<String>()
        collectTexts(root, texts)
        return texts
    }

    private fun collectTexts(node: AccessibilityNodeInfo?, list: MutableList<String>) {
        if (node == null) return
        val text = node.text?.toString()?.trim()
        val desc = node.contentDescription?.toString()?.trim()
        if (!text.isNullOrBlank()) list.add(text)
        if (!desc.isNullOrBlank() && desc != text) list.add(desc)

        for (i in 0 until node.childCount) {
            collectTexts(node.getChild(i), list)
        }
    }

    fun findNodeByTextOrDescription(query: String): AccessibilityNodeInfo? {
        val root = rootInActiveWindow ?: return null
        return searchNode(root, query.trim().lowercase())
    }

    private fun searchNode(node: AccessibilityNodeInfo?, query: String): AccessibilityNodeInfo? {
        if (node == null) return null

        val text = node.text?.toString()?.trim()?.lowercase() ?: ""
        val desc = node.contentDescription?.toString()?.trim()?.lowercase() ?: ""
        val viewId = node.viewIdResourceName?.lowercase() ?: ""

        // Exact match or contains match
        if (text == query || desc == query) return node
        if (text.contains(query) || desc.contains(query) || viewId.contains(query)) {
            if (node.isClickable || node.parent?.isClickable == true) {
                return node
            }
        }

        // Special mathematical aliases
        val isMathMatch = when (query) {
            "plus", "যোগ", "+" -> text == "+" || desc.contains("plus") || desc.contains("add") || viewId.contains("add") || viewId.contains("plus")
            "minus", "বিয়োগ", "-" -> text == "-" || text == "−" || desc.contains("minus") || desc.contains("subtract") || viewId.contains("sub")
            "multiply", "গুণ", "*", "x", "×" -> text == "×" || text == "*" || text == "x" || desc.contains("multiply") || viewId.contains("mul")
            "divide", "ভাগ", "/", "÷" -> text == "÷" || text == "/" || desc.contains("divide") || viewId.contains("div")
            "equals", "সমান", "=" -> text == "=" || desc.contains("equal") || viewId.contains("eq") || viewId.contains("calc")
            "clear", "মুছুন", "c", "ac" -> text.equals("c", ignoreCase = true) || text.equals("ac", ignoreCase = true) || desc.contains("clear") || viewId.contains("clr")
            else -> false
        }

        if (isMathMatch) return node

        for (i in 0 until node.childCount) {
            val found = searchNode(node.getChild(i), query)
            if (found != null) return found
        }
        return null
    }

    fun clickNode(node: AccessibilityNodeInfo): Boolean {
        var current: AccessibilityNodeInfo? = node
        while (current != null) {
            if (current.isClickable) {
                return current.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            }
            current = current.parent
        }

        // If not clickable directly, click center coordinate via gesture
        val bounds = Rect()
        node.getBoundsInScreen(bounds)
        if (!bounds.isEmpty) {
            return clickCoordinate(bounds.centerX().toFloat(), bounds.centerY().toFloat())
        }
        return false
    }

    fun clickCoordinate(x: Float, y: Float): Boolean {
        val path = Path().apply {
            moveTo(x, y)
        }
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, 80L))
            .build()
        return dispatchGesture(gesture, null, null)
    }

    fun enterText(node: AccessibilityNodeInfo, text: String): Boolean {
        val args = android.os.Bundle().apply {
            putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text)
        }
        return node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
    }

    fun performBack(): Boolean {
        return performGlobalAction(GLOBAL_ACTION_BACK)
    }

    fun performHome(): Boolean {
        return performGlobalAction(GLOBAL_ACTION_HOME)
    }
}
