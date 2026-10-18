package com.example.domain.planner

import com.example.data.ai.GeminiApiClient
import com.example.domain.model.AIProviderConfig
import com.example.domain.model.ActionStep
import com.example.domain.model.ActionType
import com.example.domain.model.AgentTask
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TaskPlanner(private val geminiClient: GeminiApiClient) {

    /**
     * Analyzes natural language prompt and produces an AgentTask plan or conversational answer.
     */
    suspend fun planTask(prompt: String, config: AIProviderConfig): AgentTask {
        val trimmed = prompt.trim()

        // 1. Fast-path local system actions and common commands
        val localPlan = tryCreateLocalPlan(trimmed)
        if (localPlan != null) {
            return localPlan
        }

        // 2. If Gemini API key is configured, use Gemini model (with Google Search Grounding)
        if (config.apiKey.isNotBlank()) {
            val systemInstruction = """
                You are Nafsi AI (নাফসি), a highly intelligent autonomous Android Voice and Visual Agent.
                The user communicates in Bengali, English, or mixed (Banglish).
                
                Respond naturally, politely, and informatively in clear Bengali.
                
                If the user asks a question, wants advice, explanation, information, translation, story, coding, or recipe:
                Return JSON:
                {
                  "type": "chat",
                  "response": "Your complete, accurate, helpful answer in Bengali"
                }

                If the user asks you to interact with an Android application or perform multi-step automation:
                Return JSON:
                {
                  "type": "action_plan",
                  "goal": "Description of the goal in Bengali",
                  "steps": [
                     {"action": "launch_app", "target": "app_name", "label": "অ্যাপ চালু করছি..."},
                     {"action": "tap_element", "target": "button_or_text", "label": "বোতাম চাপছি..."},
                     {"action": "enter_text", "target": "text_to_type", "label": "লেখা টাইপ করছি..."},
                     {"action": "verify_result", "target": "expected_result", "label": "ফলাফল যাচাই করছি..."}
                  ]
                }
                
                Valid action names: launch_app, tap_element, enter_text, press_back, press_home, show_recents, show_notifications, take_screenshot, inspect_screen, verify_result, open_url.
                Output ONLY valid JSON.
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

                // If text response without JSON wrapper, treat directly as chat answer
                return AgentTask(
                    rawPrompt = prompt,
                    goal = "Answer question",
                    steps = emptyList(),
                    resultMessage = cleanResponseText(raw)
                )
            }
        }

        // 3. Built-in Offline Conversational Knowledge fallback (so the app never hits a dead end!)
        val offlineAnswer = getOfflineConversationalResponse(trimmed)
        return AgentTask(
            rawPrompt = prompt,
            goal = "Conversation",
            steps = emptyList(),
            resultMessage = offlineAnswer
        )
    }

    private fun tryCreateLocalPlan(prompt: String): AgentTask? {
        val lower = prompt.lowercase()

        // System Navigation: Home
        if (lower.contains("home") || lower.contains("হোম") || lower.contains("হোমে যাও")) {
            return AgentTask(
                rawPrompt = prompt,
                goal = "Go to Home Screen",
                steps = listOf(
                    ActionStep(
                        index = 0,
                        action = ActionType.PRESS_HOME,
                        bengaliLabel = "হোম স্ক্রিনে ফিরে যাচ্ছি..."
                    )
                ),
                resultMessage = "হোম স্ক্রিনে ফিরে এসেছি।"
            )
        }

        // System Navigation: Back
        if (lower.contains("go back") || lower.contains("পিছনে") || lower.contains("আগের স্ক্রিন") || lower.contains("ব্যাক")) {
            return AgentTask(
                rawPrompt = prompt,
                goal = "Go Back",
                steps = listOf(
                    ActionStep(
                        index = 0,
                        action = ActionType.PRESS_BACK,
                        bengaliLabel = "পূর্বের স্ক্রিনে ফিরে যাচ্ছি..."
                    )
                ),
                resultMessage = "আগের স্ক্রিনে ফিরে যাওয়া হয়েছে।"
            )
        }

        // System Navigation: Recent Apps
        if (lower.contains("recent") || lower.contains("রিসেন্ট") || lower.contains("সাম্প্রতিক অ্যাপ")) {
            return AgentTask(
                rawPrompt = prompt,
                goal = "Show Recent Apps",
                steps = listOf(
                    ActionStep(
                        index = 0,
                        action = ActionType.SHOW_RECENTS,
                        bengaliLabel = "সাম্প্রতিক অ্যাপ তালিকা খুলছি..."
                    )
                ),
                resultMessage = "রিসেন্ট অ্যাপস প্রদর্শন করা হচ্ছে।"
            )
        }

        // System Navigation: Notifications
        if (lower.contains("notification") || lower.contains("নোটিফিকেশন") || lower.contains("পর্দা নামাও")) {
            return AgentTask(
                rawPrompt = prompt,
                goal = "Show Notifications",
                steps = listOf(
                    ActionStep(
                        index = 0,
                        action = ActionType.SHOW_NOTIFICATIONS,
                        bengaliLabel = "নোটিফিকেশন প্যানেল নামাচ্ছি..."
                    )
                ),
                resultMessage = "নোটিফিকেশন প্যানেল খোলা হয়েছে।"
            )
        }

        // Screenshot
        if (lower.contains("screenshot") || lower.contains("স্ক্রিনশট") || lower.contains("স্ক্রিনের ছবি")) {
            return AgentTask(
                rawPrompt = prompt,
                goal = "Take Screenshot",
                steps = listOf(
                    ActionStep(
                        index = 0,
                        action = ActionType.TAKE_SCREENSHOT,
                        bengaliLabel = "স্ক্রিনশট ক্যাপচার করছি..."
                    )
                ),
                resultMessage = "স্ক্রিনশট সফলভাবে সম্পন্ন হয়েছে।"
            )
        }

        // Screen Inspection / Reading
        if (lower.contains("screen") || lower.contains("স্ক্রিন") || lower.contains("কী লেখা") || lower.contains("পড়ো") || lower.contains("পর্দায় কী আছে")) {
            return AgentTask(
                rawPrompt = prompt,
                goal = "Inspect screen content",
                steps = listOf(
                    ActionStep(
                        index = 0,
                        action = ActionType.INSPECT_SCREEN,
                        bengaliLabel = "স্ক্রিনের তথ্য ও টেক্সট সংগ্রহ করছি..."
                    )
                ),
                resultMessage = "স্ক্রিন পরিদর্শন সম্পন্ন হয়েছে।"
            )
        }

        // Google Search / Web query
        if (lower.startsWith("search ") || lower.contains("গুগল সার্চ") || lower.contains("গুগলে খোঁজো") || lower.contains("সার্চ করো")) {
            val query = prompt
                .replace("search", "", ignoreCase = true)
                .replace("google", "", ignoreCase = true)
                .replace("গুগল", "")
                .replace("সার্চ করো", "")
                .replace("খোঁজো", "")
                .replace("search for", "", ignoreCase = true)
                .trim()

            if (query.isNotBlank()) {
                val searchUrl = "https://www.google.com/search?q=${java.net.URLEncoder.encode(query, "UTF-8")}"
                return AgentTask(
                    rawPrompt = prompt,
                    goal = "Search Google for: $query",
                    steps = listOf(
                        ActionStep(
                            index = 0,
                            action = ActionType.OPEN_URL,
                            target = searchUrl,
                            bengaliLabel = "গুগলে '$query' অনুসন্ধান করছি..."
                        )
                    ),
                    resultMessage = "গুগল সার্চ খোলা হয়েছে: '$query'"
                )
            }
        }

        // YouTube Search / Play
        if (lower.contains("youtube") || lower.contains("ইউটিউব") || lower.contains("গান চালাও") || lower.contains("ভিডিও দেখাও")) {
            val query = prompt
                .replace("youtube", "", ignoreCase = true)
                .replace("ইউটিউব", "")
                .replace("গান চালাও", "")
                .replace("ভিডিও দেখাও", "")
                .replace("চালাও", "")
                .replace("খোলো", "")
                .trim()

            val targetUrl = if (query.isNotBlank()) {
                "https://www.youtube.com/results?search_query=${java.net.URLEncoder.encode(query, "UTF-8")}"
            } else {
                "https://www.youtube.com"
            }

            return AgentTask(
                rawPrompt = prompt,
                goal = "Open YouTube: ${if (query.isNotBlank()) query else "Home"}",
                steps = listOf(
                    ActionStep(
                        index = 0,
                        action = ActionType.OPEN_URL,
                        target = targetUrl,
                        bengaliLabel = if (query.isNotBlank()) "ইউটিউবে '$query' বাজাচ্ছি..." else "ইউটিউব অ্যাপ চালু করছি..."
                    )
                ),
                resultMessage = "ইউটিউব খোলা হয়েছে।"
            )
        }

        // Calculator Math queries: e.g. "দুই যোগ দুই", "2 + 2", "হিসাব করো 5 গুণ 5", "calculate 2 plus 2"
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

        // Generic Launch App or System Settings
        if (lower.contains("open ") || lower.contains("launch ") || lower.contains("খোলো") || lower.contains("যাও") || lower.contains("চালু করো")) {
            val targetApp = when {
                lower.contains("calc") || lower.contains("ক্যালকুলেটর") -> "calculator"
                lower.contains("wifi") || lower.contains("ওয়াইফাই") -> "wifi"
                lower.contains("bluetooth") || lower.contains("ব্লুটুথ") -> "bluetooth"
                lower.contains("battery") || lower.contains("ব্যাটারি") -> "battery"
                lower.contains("setting") || lower.contains("সেটিংস") -> "settings"
                lower.contains("camera") || lower.contains("ক্যামেরা") || lower.contains("ছবি") -> "camera"
                lower.contains("gallery") || lower.contains("গ্যালারি") || lower.contains("ফটো") -> "gallery"
                lower.contains("note") || lower.contains("নোট") -> "notes"
                lower.contains("chrome") || lower.contains("browser") || lower.contains("ব্রাউজার") -> "chrome"
                lower.contains("clock") || lower.contains("ঘড়ি") || lower.contains("ঘড়ি") -> "clock"
                lower.contains("alarm") || lower.contains("অ্যালার্ম") -> "alarm"
                lower.contains("map") || lower.contains("ম্যাপ") -> "maps"
                lower.contains("dial") || lower.contains("phone") || lower.contains("ফোন") -> "phone"
                lower.contains("message") || lower.contains("মেসেজ") -> "messaging"
                lower.contains("play") || lower.contains("প্লে স্টোর") -> "vending"
                lower.contains("whatsapp") || lower.contains("হোয়াটসঅ্যাপ") -> "whatsapp"
                lower.contains("facebook") || lower.contains("ফেসবুক") -> "facebook"
                else -> {
                    // Extract app name directly from user text
                    prompt
                        .replace("open", "", ignoreCase = true)
                        .replace("launch", "", ignoreCase = true)
                        .replace("খোলো", "")
                        .replace("চালু করো", "")
                        .trim()
                }
            }

            if (targetApp.isNotBlank()) {
                return AgentTask(
                    rawPrompt = prompt,
                    goal = "Launch $targetApp",
                    steps = listOf(
                        ActionStep(
                            index = 0,
                            action = ActionType.LAUNCH_APP,
                            target = targetApp,
                            bengaliLabel = "$targetApp চালু করছি..."
                        )
                    ),
                    resultMessage = "$targetApp সফলভাবে খোলা হয়েছে।"
                )
            }
        }

        return null
    }

    private data class MathExp(val n1: Int, val op: String, val n2: Int, val expected: Int)

    private fun extractMathExpression(prompt: String): MathExp? {
        val bnNumMap = mapOf(
            "এক" to 1, "দুই" to 2, "তিন" to 3, "চার" to 4, "পাঁচ" to 5,
            "ছয়" to 6, "ছয়" to 6, "সাত" to 7, "আট" to 8, "নয়" to 9, "নয়" to 9, "দশ" to 10,
            "one" to 1, "two" to 2, "three" to 3, "four" to 4, "five" to 5,
            "six" to 6, "seven" to 7, "eight" to 8, "nine" to 9, "ten" to 10
        )

        var normalized = prompt.lowercase()
        bnNumMap.forEach { (word, digit) ->
            normalized = normalized.replace(word, digit.toString())
        }

        val regex = Regex("(\\d+)\\s*(যোগ|বিয়োগ|বিয়োগ|গুণ|ভাগ|\\+|-|\\*|/|plus|minus|multiply|divide|times)\\s*(\\d+)")
        val match = regex.find(normalized) ?: return null

        val n1 = match.groupValues[1].toIntOrNull() ?: return null
        val opRaw = match.groupValues[2]
        val n2 = match.groupValues[3].toIntOrNull() ?: return null

        val op = when {
            opRaw.contains("যোগ") || opRaw.contains("+") || opRaw.contains("plus") -> "plus"
            opRaw.contains("বিয়োগ") || opRaw.contains("বিয়োগ") || opRaw.contains("-") || opRaw.contains("minus") -> "minus"
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
                            "show_recents" -> ActionType.SHOW_RECENTS
                            "show_notifications" -> ActionType.SHOW_NOTIFICATIONS
                            "take_screenshot" -> ActionType.TAKE_SCREENSHOT
                            "open_url" -> ActionType.OPEN_URL
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

    private fun getOfflineConversationalResponse(prompt: String): String {
        val lower = prompt.lowercase()
        return when {
            lower.contains("হ্যালো") || lower.contains("নমস্কার") || lower.contains("hi") || lower.contains("hello") || lower.contains("সালাম") -> {
                "নমস্কার! আমি নাফসি (Nafsi AI)। আমি আপনাকে কীভাবে সাহায্য করতে পারি? আপনি ক্যালকুলেটরে হিসাব করতে, ইউটিউব বা ক্যামেরা খুলতে, অথবা যে কোনো প্রশ্ন করতে পারেন।"
            }
            lower.contains("কেমন আছো") || lower.contains("how are you") -> {
                "আমি ভালো আছি, ধন্যবাদ! আপনার ডিভাইস নিয়ন্ত্রণ করতে এবং যেকোনো কাজের নির্দেশ পালনে আমি প্রস্তুত।"
            }
            lower.contains("নাম কী") || lower.contains("তুমি কে") || lower.contains("who are you") -> {
                "আমি নাফসি (Nafsi AI) — একটি স্বায়ত্তশাসিত অ্যান্ড্রয়েড ভয়েস এবং ভিজ্যুয়াল এজেন্ট। আমি আপনার কথা শুনে স্বয়ংক্রিয়ভাবে ডিভাইসে কাজ করতে পারি।"
            }
            lower.contains("কী করতে পারো") || lower.contains("what can you do") || lower.contains("কাজের তালিকা") -> {
                "আমি ক্যালকুলেটরে হিসাব করতে ও ফলাফল যাচাই করতে পারি, ইউটিউবে গান চালাতে পারি, ক্যামেরা, সেটিংস, ওয়াইফাই, ব্লুটুথ খুলতে পারি, স্ক্রিনশট নিতে পারি, স্ক্রিনে কী লেখা আছে তা পড়ে শোনাতে পারি এবং যেকোনো তথ্যভিত্তিক প্রশ্নের উত্তর দিতে পারি।"
            }
            lower.contains("সময় কত") || lower.contains("time") -> {
                val timeStr = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date())
                "এখন সময় প্রায় $timeStr।"
            }
            lower.contains("তারিখ") || lower.contains("আজকে কী বার") || lower.contains("date") -> {
                val dateStr = SimpleDateFormat("EEEE, d MMMM yyyy", Locale.getDefault()).format(Date())
                "আজকের দিন ও তারিখ: $dateStr।"
            }
            lower.contains("রসিকতা") || lower.contains("কৌতুক") || lower.contains("joke") -> {
                "বলুন তো কোন চাবি দিয়ে কোনো তালা খোলা যায় না?\nউত্তর: কি-বোর্ড (Keyboard)!"
            }
            else -> {
                "আমি আপনার নির্দেশ পেয়েছি: '$prompt'। অনুগ্রহ করে সেটিংস থেকে Gemini API কানেকশন সক্রিয় আছে কি না দেখে নিন, অথবা অন্য কোনো কাজ বলুন।"
            }
        }
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
