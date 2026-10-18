package com.example.domain.planner

import com.example.data.ai.GeminiApiClient
import com.example.domain.model.AIProviderConfig
import com.example.domain.model.ActionStep
import com.example.domain.model.ActionType
import com.example.domain.model.AgentTask
import com.example.domain.model.StepStatus
import org.json.JSONObject

class TaskPlanner(private val geminiClient: GeminiApiClient) {

    /**
     * Analyzes natural language prompt and produces an AgentTask plan or conversational answer.
     */
    suspend fun planTask(prompt: String, config: AIProviderConfig): AgentTask {
        val trimmed = prompt.trim()

        // 1. Check for fast-path local action plans (e.g. Calculator math, App opening)
        val localPlan = tryCreateLocalPlan(trimmed)
        if (localPlan != null) {
            return localPlan
        }

        // 2. If it's a general question or complex request, use Gemini model
        val systemInstruction = """
            You are Nafsi AI, an autonomous Android Voice and Visual Agent.
            The user talks in Bengali, English, or mixed.
            If the user asks a question, answer concisely and helpfully in natural Bengali.
            If the user asks you to perform an Android UI task, return a JSON object with:
            {
              "type": "action_plan",
              "goal": "description of the goal",
              "steps": [
                 {"action": "launch_app", "target": "app_name", "label": "অ্যাপ চালু করছি"},
                 {"action": "tap_element", "target": "button_or_text", "label": "বোতাম টিপছি"},
                 {"action": "enter_text", "target": "text_to_type", "label": "লেখা টাইপ করছি"},
                 {"action": "verify_result", "target": "expected_result", "label": "ফলাফল যাচাই করছি"}
              ]
            }
            If it is purely an informational question or conversation, return:
            {
              "type": "chat",
              "response": "Your spoken response in friendly Bengali"
            }
            Return ONLY valid JSON.
        """.trimIndent()

        val aiResult = geminiClient.generateContent(
            prompt = prompt,
            config = config,
            systemInstruction = systemInstruction
        )

        if (aiResult.isSuccess) {
            val raw = aiResult.getOrNull() ?: ""
            val parsedTask = parseAIPlanResponse(prompt, raw)
            if (parsedTask != null) return parsedTask

            // If not JSON, treat as chat answer
            return AgentTask(
                rawPrompt = prompt,
                goal = "Answer question",
                steps = emptyList(),
                resultMessage = cleanResponseText(raw)
            )
        }

        // Fallback friendly response
        return AgentTask(
            rawPrompt = prompt,
            goal = "Conversation",
            steps = emptyList(),
            resultMessage = "আমি আপনার কথাটি বুঝতে পেরেছি: '$prompt'। অনুগ্রহ করে API কনফিগারেশন চেক করুন অথবা অন্য নির্দেশ দিন।"
        )
    }

    private fun tryCreateLocalPlan(prompt: String): AgentTask? {
        val lower = prompt.lowercase()

        // Match Calculator Math queries: e.g. "দুই যোগ দুই", "2 + 2", "হিসাব করো 5 গুণ 5", "calculate 2 plus 2"
        val mathMatch = extractMathExpression(prompt)
        if (mathMatch != null) {
            val (num1, op, num2, expected) = mathMatch
            val opSymbol = when (op) {
                "plus", "যোগ", "+" -> "+"
                "minus", "বিয়োগ", "-" -> "-"
                "multiply", "গুণ", "*", "x", "×" -> "×"
                "divide", "ভাগ", "/", "÷" -> "÷"
                else -> "+"
            }

            val steps = listOf(
                ActionStep(
                    index = 0,
                    action = ActionType.LAUNCH_APP,
                    target = "calculator",
                    bengaliLabel = "ক্যালকুলেটর চালু করছি..."
                ),
                ActionStep(
                    index = 1,
                    action = ActionType.TAP_ELEMENT,
                    target = num1.toString(),
                    bengaliLabel = "সংখ্যা $num1 চাপছি..."
                ),
                ActionStep(
                    index = 2,
                    action = ActionType.TAP_ELEMENT,
                    target = opSymbol,
                    bengaliLabel = "চিহ্ন $opSymbol চাপছি..."
                ),
                ActionStep(
                    index = 3,
                    action = ActionType.TAP_ELEMENT,
                    target = num2.toString(),
                    bengaliLabel = "সংখ্যা $num2 চাপছি..."
                ),
                ActionStep(
                    index = 4,
                    action = ActionType.TAP_ELEMENT,
                    target = "=",
                    bengaliLabel = "সমান (=) বোতাম চাপছি..."
                ),
                ActionStep(
                    index = 5,
                    action = ActionType.VERIFY_RESULT,
                    target = expected.toString(),
                    bengaliLabel = "ফলাফল $expected যাচাই করছি..."
                )
            )

            return AgentTask(
                rawPrompt = prompt,
                goal = "Calculate $num1 $opSymbol $num2 = $expected in Calculator",
                steps = steps,
                resultMessage = "হিসাব সফল হয়েছে: $num1 $opSymbol $num2 = $expected"
            )
        }

        // Match: Launch App
        if (lower.contains("open ") || lower.contains("launch ") || lower.contains("খোলো") || lower.contains("চালু করো")) {
            val targetApp = when {
                lower.contains("calc") || lower.contains("ক্যালকুলেটর") -> "calculator"
                lower.contains("setting") || lower.contains("সেটিংস") -> "settings"
                lower.contains("note") || lower.contains("নোট") -> "notes"
                lower.contains("chrome") || lower.contains("browser") || lower.contains("ব্রাউজার") -> "chrome"
                lower.contains("clock") || lower.contains("ঘড়ি") -> "clock"
                lower.contains("camera") || lower.contains("ক্যামেরা") -> "camera"
                lower.contains("map") || lower.contains("ম্যাপ") -> "maps"
                else -> null
            }

            if (targetApp != null) {
                return AgentTask(
                    rawPrompt = prompt,
                    goal = "Launch $targetApp",
                    steps = listOf(
                        ActionStep(
                            index = 0,
                            action = ActionType.LAUNCH_APP,
                            target = targetApp,
                            bengaliLabel = "$targetApp অ্যাপ খুলছি..."
                        ),
                        ActionStep(
                            index = 1,
                            action = ActionType.INSPECT_SCREEN,
                            bengaliLabel = "স্ক্রিন পরিদর্শন করছি..."
                        )
                    ),
                    resultMessage = "$targetApp সফলভাবে খোলা হয়েছে।"
                )
            }
        }

        // Match: Screen Inspection / Reading
        if (lower.contains("screen") || lower.contains("স্ক্রিন") || lower.contains("কী লেখা") || lower.contains("দেখা যাচ্ছে")) {
            return AgentTask(
                rawPrompt = prompt,
                goal = "Inspect screen content",
                steps = listOf(
                    ActionStep(
                        index = 0,
                        action = ActionType.INSPECT_SCREEN,
                        bengaliLabel = "স্ক্রিনের তথ্য সংগ্রহ করছি..."
                    )
                ),
                resultMessage = "স্ক্রিন পরিদর্শন সম্পন্ন হয়েছে।"
            )
        }

        return null
    }

    private data class MathExp(val n1: Int, val op: String, val n2: Int, val expected: Int)

    private fun extractMathExpression(prompt: String): MathExp? {
        // Bengali number words to digits
        val bnNumMap = mapOf(
            "এক" to 1, "দুই" to 2, "তিন" to 3, "চার" to 4, "পাঁচ" to 5,
            "ছয়" to 6, "সাত" to 7, "আট" to 8, "নয়" to 9, "দশ" to 10,
            "one" to 1, "two" to 2, "three" to 3, "four" to 4, "five" to 5
        )

        var normalized = prompt.lowercase()
        bnNumMap.forEach { (word, digit) ->
            normalized = normalized.replace(word, digit.toString())
        }

        // Look for digit [op] digit
        val regex = Regex("(\\d+)\\s*(যোগ|বিয়োগ|গুণ|ভাগ|\\+|-|\\*|/|plus|minus|multiply|divide|times)\\s*(\\d+)")
        val match = regex.find(normalized) ?: return null

        val n1 = match.groupValues[1].toIntOrNull() ?: return null
        val opRaw = match.groupValues[2]
        val n2 = match.groupValues[3].toIntOrNull() ?: return null

        val op = when {
            opRaw.contains("যোগ") || opRaw.contains("+") || opRaw.contains("plus") -> "plus"
            opRaw.contains("বিয়োগ") || opRaw.contains("-") || opRaw.contains("minus") -> "minus"
            opRaw.contains("গুণ") || opRaw.contains("*") || opRaw.contains("multiply") || opRaw.contains("times") -> "multiply"
            opRaw.contains("ভাগ") || opRaw.contains("/") || opRaw.contains("divide") -> "divide"
            else -> "plus"
        }

        val expected = when (op) {
            "plus" -> n1 + n2
            "minus" -> n1 - n2
            "multiply" -> n1 * n2
            "divide" -> if (n2 != 0) n1 / n2 else 0
            else -> n1 + n2
        }

        return MathExp(n1, op, n2, expected)
    }

    private fun parseAIPlanResponse(prompt: String, raw: String): AgentTask? {
        try {
            val jsonStr = extractJsonBlock(raw)
            val json = JSONObject(jsonStr)
            val type = json.optString("type", "chat")

            if (type == "action_plan") {
                val goal = json.optString("goal", "Execute actions")
                val stepsArray = json.optJSONArray("steps")
                val steps = mutableListOf<ActionStep>()

                if (stepsArray != null) {
                    for (i in 0 until stepsArray.length()) {
                        val s = stepsArray.getJSONObject(i)
                        val actionName = s.optString("action", "inspect_screen")
                        val target = if (s.has("target")) s.getString("target") else null
                        val label = s.optString("label", "ধাপ ${i + 1}")

                        val actionType = when (actionName) {
                            "launch_app" -> ActionType.LAUNCH_APP
                            "tap_element" -> ActionType.TAP_ELEMENT
                            "tap_coordinate" -> ActionType.TAP_COORDINATE
                            "enter_text" -> ActionType.ENTER_TEXT
                            "press_back" -> ActionType.PRESS_BACK
                            "press_home" -> ActionType.PRESS_HOME
                            "verify_result" -> ActionType.VERIFY_RESULT
                            else -> ActionType.INSPECT_SCREEN
                        }

                        steps.add(
                            ActionStep(
                                index = i,
                                action = actionType,
                                target = target,
                                bengaliLabel = label
                            )
                        )
                    }
                }

                return AgentTask(
                    rawPrompt = prompt,
                    goal = goal,
                    steps = steps,
                    resultMessage = "কাজটি সম্পন্ন করতে প্রস্তুত।"
                )
            } else if (type == "chat") {
                val resp = json.optString("response", "")
                if (resp.isNotBlank()) {
                    return AgentTask(
                        rawPrompt = prompt,
                        goal = "Conversation",
                        steps = emptyList(),
                        resultMessage = resp
                    )
                }
            }
        } catch (_: Exception) {}
        return null
    }

    private fun extractJsonBlock(raw: String): String {
        val trimmed = raw.trim()
        val startIndex = trimmed.indexOf("{")
        val endIndex = trimmed.lastIndexOf("}")
        return if (startIndex != -1 && endIndex != -1 && endIndex > startIndex) {
            trimmed.substring(startIndex, endIndex + 1)
        } else {
            trimmed
        }
    }

    private fun cleanResponseText(raw: String): String {
        return raw.replace(Regex("```(json)?"), "").trim()
    }
}
