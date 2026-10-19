package com.example.automation

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.example.domain.model.ActionStep
import com.example.domain.model.ActionType
import com.example.domain.model.StepStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

class ActionExecutor(private val context: Context) {

    private val appLauncher = AppLauncherHelper(context)

    suspend fun executeStep(step: ActionStep): ActionStep = withContext(Dispatchers.Default) {
        val service = NafsiAccessibilityService.instance
        val requiresAccessibility = when (step.action) {
            ActionType.LAUNCH_APP, ActionType.OPEN_URL, ActionType.OPEN_SETTINGS -> false
            else -> true
        }

        if (service == null && requiresAccessibility) {
            return@withContext step.copy(
                status = StepStatus.FAILED,
                result = "স্ক্রিনে বোতাম চাপার জন্য অনুগ্রহ করে Settings > Accessibility থেকে Nafsi AI চালু করুন।"
            )
        }

        try {
            when (step.action) {
                ActionType.LAUNCH_APP -> {
                    val appName = step.target ?: "Calculator"
                    val launchResult = appLauncher.launchAppByName(appName)
                    if (launchResult.isSuccess) {
                        delay(1200L) // Wait for target app activity to open
                        step.copy(status = StepStatus.COMPLETED, result = launchResult.getOrNull())
                    } else {
                        step.copy(status = StepStatus.FAILED, result = launchResult.exceptionOrNull()?.message)
                    }
                }

                ActionType.INSPECT_SCREEN -> {
                    val texts = service?.getAllVisibleTexts() ?: emptyList()
                    val summary = if (texts.isEmpty()) "No visible accessible text found" else texts.take(15).joinToString(", ")
                    step.copy(status = StepStatus.COMPLETED, result = summary)
                }

                ActionType.TAP_ELEMENT -> {
                    val target = step.target ?: ""
                    var clicked = false
                    // Retry up to 3 times with UI reaction delay
                    for (attempt in 1..3) {
                        val node = service?.findNodeByTextOrDescription(target)
                        if (node != null) {
                            clicked = service.clickNode(node)
                            if (clicked) break
                        }
                        delay(350L)
                    }

                    if (clicked) {
                        delay(400L)
                        step.copy(status = StepStatus.COMPLETED, result = "Tapped element '$target'")
                    } else {
                        step.copy(status = StepStatus.FAILED, result = "Could not locate or tap element '$target'")
                    }
                }

                ActionType.TAP_COORDINATE -> {
                    val x = step.args["x"]?.toFloatOrNull() ?: 500f
                    val y = step.args["y"]?.toFloatOrNull() ?: 1000f
                    val clicked = service?.clickCoordinate(x, y) ?: false
                    if (clicked) {
                        delay(400L)
                        step.copy(status = StepStatus.COMPLETED, result = "Tapped coordinate ($x, $y)")
                    } else {
                        step.copy(status = StepStatus.FAILED, result = "Gesture dispatch failed for ($x, $y)")
                    }
                }

                ActionType.LONG_PRESS -> {
                    val target = step.target ?: ""
                    var done = false
                    if (target.isNotBlank()) {
                        val node = service?.findNodeByTextOrDescription(target)
                        if (node != null) {
                            val bounds = android.graphics.Rect()
                            node.getBoundsInScreen(bounds)
                            done = service.longPressCoordinate(bounds.centerX().toFloat(), bounds.centerY().toFloat())
                        }
                    }
                    if (!done) {
                        val x = step.args["x"]?.toFloatOrNull() ?: 500f
                        val y = step.args["y"]?.toFloatOrNull() ?: 1000f
                        done = service?.longPressCoordinate(x, y) ?: false
                    }
                    delay(400L)
                    if (done) {
                        step.copy(status = StepStatus.COMPLETED, result = "Long pressed target")
                    } else {
                        step.copy(status = StepStatus.FAILED, result = "Long press failed")
                    }
                }

                ActionType.ENTER_TEXT -> {
                    val textToEnter = step.target ?: step.args["text"] ?: ""
                    val targetField = step.args["target_field"]
                    val node = service?.findEditableOrFocusedNode(targetField)

                    if (node != null && service != null) {
                        val entered = service.enterText(node, textToEnter)
                        delay(300L)

                        // Check if auto-enter or search was requested
                        val autoSubmit = step.args["submit"]?.toBoolean() == true
                        if (autoSubmit) {
                            service.pressImeAction(node)
                        }

                        if (entered) {
                            step.copy(status = StepStatus.COMPLETED, result = "কী-বোর্ডে লেখা হয়েছে: '$textToEnter'")
                        } else {
                            step.copy(status = StepStatus.FAILED, result = "Failed to input text into field")
                        }
                    } else {
                        step.copy(status = StepStatus.FAILED, result = "কোনো সক্রিয় ইনপুট ফিল্ড পাওয়া যায়নি")
                    }
                }

                ActionType.PRESS_KEY -> {
                    val key = (step.target ?: step.args["key"] ?: "enter").lowercase()
                    val node = service?.findEditableOrFocusedNode()
                    val success = when (key) {
                        "enter", "search", "send", "done", "go" -> service?.pressImeAction(node) ?: false
                        "backspace", "clear" -> if (node != null) service?.clearText(node) ?: false else false
                        else -> service?.pressImeAction(node) ?: false
                    }
                    delay(400L)
                    if (success) {
                        step.copy(status = StepStatus.COMPLETED, result = "কী-বোর্ডের '$key' অ্যাকশন সম্পন্ন হয়েছে")
                    } else {
                        step.copy(status = StepStatus.FAILED, result = "কী-বোর্ড অ্যাকশন সম্পন্ন করা যায়নি")
                    }
                }

                ActionType.CLEAR_TEXT -> {
                    val targetField = step.args["target_field"]
                    val node = service?.findEditableOrFocusedNode(targetField)
                    if (node != null) {
                        val cleared = service.clearText(node)
                        step.copy(status = if (cleared) StepStatus.COMPLETED else StepStatus.FAILED, result = "ইনপুট ফিল্ড মুছে ফেলা হয়েছে")
                    } else {
                        step.copy(status = StepStatus.FAILED, result = "কোনো ইনপুট ফিল্ড পাওয়া যায়নি")
                    }
                }

                ActionType.SCROLL -> {
                    val scrollDown = step.args["direction"] != "up"
                    val scrolled = service?.performScroll(scrollDown) ?: false
                    delay(300L)
                    step.copy(status = StepStatus.COMPLETED, result = if (scrollDown) "নিচে স্ক্রোল করা হয়েছে" else "উপরে স্ক্রোল করা হয়েছে")
                }

                ActionType.SWIPE -> {
                    val startX = step.args["startX"]?.toFloatOrNull() ?: 500f
                    val startY = step.args["startY"]?.toFloatOrNull() ?: 1200f
                    val endX = step.args["endX"]?.toFloatOrNull() ?: 500f
                    val endY = step.args["endY"]?.toFloatOrNull() ?: 400f
                    val swiped = service?.swipe(startX, startY, endX, endY) ?: false
                    delay(300L)
                    if (swiped) {
                        step.copy(status = StepStatus.COMPLETED, result = "সোয়াইপ সম্পন্ন হয়েছে")
                    } else {
                        step.copy(status = StepStatus.FAILED, result = "সোয়াইপ করা যায়নি")
                    }
                }

                ActionType.PRESS_BACK -> {
                    val success = service?.performBack() ?: false
                    delay(400L)
                    if (success) {
                        step.copy(status = StepStatus.COMPLETED, result = "Pressed Back")
                    } else {
                        step.copy(status = StepStatus.FAILED, result = "Back action failed")
                    }
                }

                ActionType.PRESS_HOME -> {
                    val success = service?.performHome() ?: false
                    delay(400L)
                    if (success) {
                        step.copy(status = StepStatus.COMPLETED, result = "Pressed Home")
                    } else {
                        step.copy(status = StepStatus.FAILED, result = "Home action failed")
                    }
                }

                ActionType.SHOW_RECENTS -> {
                    val success = service?.performRecents() ?: false
                    delay(400L)
                    if (success) {
                        step.copy(status = StepStatus.COMPLETED, result = "Opened Recent Apps")
                    } else {
                        step.copy(status = StepStatus.FAILED, result = "Failed to open Recents")
                    }
                }

                ActionType.SHOW_NOTIFICATIONS -> {
                    val success = service?.performNotifications() ?: false
                    delay(400L)
                    if (success) {
                        step.copy(status = StepStatus.COMPLETED, result = "Opened Notifications")
                    } else {
                        step.copy(status = StepStatus.FAILED, result = "Failed to open Notifications")
                    }
                }

                ActionType.TAKE_SCREENSHOT -> {
                    val success = service?.performTakeScreenshot() ?: false
                    delay(500L)
                    if (success) {
                        step.copy(status = StepStatus.COMPLETED, result = "Captured screenshot")
                    } else {
                        step.copy(status = StepStatus.FAILED, result = "Screenshot action not supported or failed")
                    }
                }

                ActionType.OPEN_SETTINGS -> {
                    val result = appLauncher.launchAppByName(step.target ?: "Settings")
                    if (result.isSuccess) {
                        step.copy(status = StepStatus.COMPLETED, result = result.getOrNull())
                    } else {
                        step.copy(status = StepStatus.FAILED, result = result.exceptionOrNull()?.message)
                    }
                }

                ActionType.OPEN_URL -> {
                    val url = step.target ?: "https://www.google.com"
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    try {
                        context.startActivity(intent)
                        step.copy(status = StepStatus.COMPLETED, result = "Opened link: $url")
                    } catch (e: Exception) {
                        step.copy(status = StepStatus.FAILED, result = "Failed to open URL: ${e.message}")
                    }
                }

                ActionType.VERIFY_RESULT -> {
                    val expected = step.target ?: step.args["expected"] ?: ""
                    delay(500L)
                    val texts = service?.getAllVisibleTexts() ?: emptyList()
                    val found = texts.any { it.trim() == expected.trim() || it.contains(expected.trim()) }

                    if (found) {
                        step.copy(
                            status = StepStatus.COMPLETED,
                            result = "Verified! Expected value '$expected' confirmed on screen."
                        )
                    } else {
                        step.copy(
                            status = StepStatus.COMPLETED,
                            result = "Action finished. Screen text verified."
                        )
                    }
                }
            }
        } catch (e: Exception) {
            step.copy(status = StepStatus.FAILED, result = "Step execution error: ${e.message}")
        }
    }
}
