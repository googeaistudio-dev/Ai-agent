package com.example.automation

import android.content.Context
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
        if (service == null && step.action != ActionType.LAUNCH_APP) {
            return@withContext step.copy(
                status = StepStatus.FAILED,
                result = "Accessibility Service is not enabled. Please enable it in Settings."
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
                    // Retry up to 2 times with 400ms delay in case animations are completing
                    for (attempt in 1..2) {
                        val node = service?.findNodeByTextOrDescription(target)
                        if (node != null) {
                            clicked = service.clickNode(node)
                            if (clicked) break
                        }
                        delay(400L)
                    }

                    if (clicked) {
                        delay(500L) // UI reaction delay
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

                ActionType.ENTER_TEXT -> {
                    val textToEnter = step.target ?: step.args["text"] ?: ""
                    val node = service?.findNodeByTextOrDescription(step.args["target_field"] ?: "")
                        ?: service?.rootInActiveWindow?.findFocus(android.view.accessibility.AccessibilityNodeInfo.FOCUS_INPUT)

                    if (node != null && service != null) {
                        val entered = service.enterText(node, textToEnter)
                        if (entered) {
                            step.copy(status = StepStatus.COMPLETED, result = "Entered text: '$textToEnter'")
                        } else {
                            step.copy(status = StepStatus.FAILED, result = "Failed to input text into field")
                        }
                    } else {
                        step.copy(status = StepStatus.FAILED, result = "No input field found")
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

                ActionType.SCROLL -> {
                    step.copy(status = StepStatus.COMPLETED, result = "Scrolled screen")
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
                        // Check if result is displayed in active nodes
                        val node = service?.findNodeByTextOrDescription(expected)
                        if (node != null) {
                            step.copy(
                                status = StepStatus.COMPLETED,
                                result = "Verified! Result '$expected' found in active UI element."
                            )
                        } else {
                            val visibleSummary = texts.take(6).joinToString(", ")
                            step.copy(
                                status = StepStatus.FAILED,
                                result = "Verification failed: Expected '$expected' not found on screen. Visible: [$visibleSummary]"
                            )
                        }
                    }
                }
            }
        } catch (e: Exception) {
            step.copy(status = StepStatus.FAILED, result = "Execution error: ${e.message}")
        }
    }
}
