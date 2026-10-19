package com.example.automation

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Path
import android.graphics.Rect
import android.os.Build
import android.os.Bundle
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

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
            if (node.isClickable || node.parent?.isClickable == true || node.isEditable) {
                return node
            }
        }

        // Special mathematical and keyboard aliases
        val isSpecialMatch = when (query) {
            "plus", "যোগ", "+" -> text == "+" || desc.contains("plus") || desc.contains("add") || viewId.contains("add") || viewId.contains("plus")
            "minus", "বিয়োগ", "-" -> text == "-" || text == "−" || desc.contains("minus") || desc.contains("subtract") || viewId.contains("sub")
            "multiply", "গুণ", "*", "x", "×" -> text == "×" || text == "*" || text == "x" || desc.contains("multiply") || viewId.contains("mul")
            "divide", "ভাগ", "/", "÷" -> text == "÷" || text == "/" || desc.contains("divide") || viewId.contains("div")
            "equals", "সমান", "=" -> text == "=" || desc.contains("equal") || viewId.contains("eq") || viewId.contains("calc")
            "clear", "মুছুন", "c", "ac" -> text.equals("c", ignoreCase = true) || text.equals("ac", ignoreCase = true) || desc.contains("clear") || viewId.contains("clr")
            "search", "খোঁজো", "অনুসন্ধান" -> desc.contains("search") || viewId.contains("search") || text.contains("search")
            "send", "পাঠাও", "মেসেজ" -> desc.contains("send") || viewId.contains("send") || text.contains("send")
            else -> false
        }

        if (isSpecialMatch) return node

        for (i in 0 until node.childCount) {
            val found = searchNode(node.getChild(i), query)
            if (found != null) return found
        }
        return null
    }

    /**
     * Finds active editable input field or searches for an input node matching query.
     */
    fun findEditableOrFocusedNode(targetQuery: String? = null): AccessibilityNodeInfo? {
        val root = rootInActiveWindow ?: return null

        // 1. If target query provided, try to find that specific element first
        if (!targetQuery.isNullOrBlank()) {
            val targeted = findNodeByTextOrDescription(targetQuery)
            if (targeted != null) return targeted
        }

        // 2. Check currently focused input node
        val focused = root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
        if (focused != null && focused.isEditable) return focused

        // 3. Search for any editable node in active window
        return searchFirstEditableNode(root)
    }

    private fun searchFirstEditableNode(node: AccessibilityNodeInfo?): AccessibilityNodeInfo? {
        if (node == null) return null
        val className = node.className?.toString() ?: ""
        val viewId = node.viewIdResourceName?.lowercase() ?: ""

        if (node.isEditable ||
            className.contains("EditText", ignoreCase = true) ||
            className.contains("AutoCompleteTextView", ignoreCase = true) ||
            viewId.contains("search", ignoreCase = true) ||
            viewId.contains("input", ignoreCase = true) ||
            viewId.contains("query", ignoreCase = true)
        ) {
            return node
        }

        for (i in 0 until node.childCount) {
            val childFound = searchFirstEditableNode(node.getChild(i))
            if (childFound != null) return childFound
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

        // If not directly clickable, tap center coordinate via gesture
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

    fun longPressCoordinate(x: Float, y: Float): Boolean {
        val path = Path().apply {
            moveTo(x, y)
        }
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, 800L))
            .build()
        return dispatchGesture(gesture, null, null)
    }

    fun swipe(startX: Float, startY: Float, endX: Float, endY: Float, durationMs: Long = 300L): Boolean {
        val path = Path().apply {
            moveTo(startX, startY)
            lineTo(endX, endY)
        }
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, durationMs))
            .build()
        return dispatchGesture(gesture, null, null)
    }

    fun performScroll(scrollDown: Boolean): Boolean {
        val root = rootInActiveWindow
        val scrollableNode = findScrollableNode(root)
        if (scrollableNode != null) {
            val action = if (scrollDown) {
                AccessibilityNodeInfo.ACTION_SCROLL_FORWARD
            } else {
                AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD
            }
            if (scrollableNode.performAction(action)) return true
        }

        // Fallback: gesture swipe
        val displayMetrics = resources.displayMetrics
        val width = displayMetrics.widthPixels.toFloat()
        val height = displayMetrics.heightPixels.toFloat()
        val startY = if (scrollDown) height * 0.75f else height * 0.25f
        val endY = if (scrollDown) height * 0.25f else height * 0.75f
        return swipe(width / 2f, startY, width / 2f, endY, 350L)
    }

    private fun findScrollableNode(node: AccessibilityNodeInfo?): AccessibilityNodeInfo? {
        if (node == null) return null
        if (node.isScrollable) return node
        for (i in 0 until node.childCount) {
            val found = findScrollableNode(node.getChild(i))
            if (found != null) return found
        }
        return null
    }

    /**
     * Types text into input field using ACTION_SET_TEXT, with clipboard paste fallback.
     */
    fun enterText(node: AccessibilityNodeInfo, text: String): Boolean {
        // First click/focus the field to open virtual keyboard if needed
        node.performAction(AccessibilityNodeInfo.ACTION_FOCUS)
        clickNode(node)

        // Try direct ACTION_SET_TEXT
        val args = Bundle().apply {
            putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text)
        }
        val directSet = node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
        if (directSet) return true

        // Fallback: Copy to clipboard and paste
        return try {
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("NafsiInput", text)
            clipboard.setPrimaryClip(clip)
            node.performAction(AccessibilityNodeInfo.ACTION_PASTE)
        } catch (_: Throwable) {
            false
        }
    }

    fun clearText(node: AccessibilityNodeInfo): Boolean {
        val args = Bundle().apply {
            putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, "")
        }
        return node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
    }

    fun pressImeAction(node: AccessibilityNodeInfo?): Boolean {
        if (node != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val imeAction = node.actionList?.find { it.id == AccessibilityNodeInfo.AccessibilityAction.ACTION_IME_ENTER.id }
            if (imeAction != null && node.performAction(imeAction.id)) {
                return true
            }
        }

        // Try to find search or send button icon/text in the window
        val searchOrSend = findNodeByTextOrDescription("search")
            ?: findNodeByTextOrDescription("send")
            ?: findNodeByTextOrDescription("খোঁজো")
            ?: findNodeByTextOrDescription("পাঠাও")

        if (searchOrSend != null) {
            return clickNode(searchOrSend)
        }
        return false
    }

    fun performBack(): Boolean = performGlobalAction(GLOBAL_ACTION_BACK)

    fun performHome(): Boolean = performGlobalAction(GLOBAL_ACTION_HOME)

    fun performRecents(): Boolean = performGlobalAction(GLOBAL_ACTION_RECENTS)

    fun performNotifications(): Boolean = performGlobalAction(GLOBAL_ACTION_NOTIFICATIONS)

    fun performQuickSettings(): Boolean = performGlobalAction(GLOBAL_ACTION_QUICK_SETTINGS)

    fun performTakeScreenshot(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            performGlobalAction(GLOBAL_ACTION_TAKE_SCREENSHOT)
        } else false
    }

    fun performLockScreen(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN)
        } else false
    }
}
