package com.example.domain.planner

import com.example.data.ai.GeminiApiClient
import com.example.data.ai.OpenAICompatibleClient
import com.example.domain.model.AIProviderConfig
import com.example.domain.model.AIProviderType
import com.example.domain.model.ActionStep
import com.example.domain.model.ActionType
import com.example.domain.model.AgentTask
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TaskPlanner(
    private val geminiClient: GeminiApiClient,
    private val openAIClient: OpenAICompatibleClient
) {

    /**
     * Analyzes natural language prompt and produces an AgentTask plan or conversational answer.
     * Works with both Gemini and OpenAI-compatible providers, without relying on rigid keywords.
     */
    suspend fun planTask(prompt: String, config: AIProviderConfig): AgentTask {
        val trimmed = prompt.trim()

        // 1. If AI API key is configured, use the active AI model (Gemini or OpenAI/OpenRouter)
        // This gives human-like natural comprehension without rigid keywords!
        if (config.apiKey.isNotBlank()) {
            val systemInstruction = """
                You are Nafsi AI (নাফসি), an autonomous Android Voice and Visual Agent.
                The user speaks naturally in Bengali, English, or Banglish.
                You can do ANYTHING on an Android phone just like a human assistant.
                
                Respond politely, intelligently, and warmly in clear Bengali.
                
                If the user asks a question, wants conversation, explanation, advice, translation, story, coding, or recipe:
                Return JSON:
                {
                  "type": "chat",
                  "response": "Your complete, accurate, helpful human-like answer in Bengali"
                }

                If the user asks to open an app, control device settings, search web, do math, or perform actions on Android:
                Return JSON:
                {
                  "type": "action_plan",
                  "goal": "Description of the goal in Bengali",
                  "steps": [
                     {"action": "launch_app", "target": "app_name", "label": "অ্যাপ চালু করছি..."},
                     {"action": "tap_element", "target": "button_or_text", "label": "বোতাম চাপছি..."},
                     {"action": "enter_text", "target": "text_to_type", "label": "লেখা টাইপ করছি..."},
                     {"action": "open_url", "target": "url", "label": "লিংক খুলছি..."},
                     {"action": "press_back", "label": "পিছনে যাচ্ছি..."},
                     {"action": "press_home", "label": "হোমে ফিরে যাচ্ছি..."},
                     {"action": "show_recents", "label": "রিসেন্ট অ্যাপস খুলছি..."},
                     {"action": "show_notifications", "label": "নোটিফিকেশন নামাচ্ছি..."},
                     {"action": "take_screenshot", "label": "স্ক্রিনশট নিচ্ছি..."},
                     {"action": "inspect_screen", "label": "স্ক্রিন দেখছি..."},
                     {"action": "verify_result", "target": "expected_result", "label": "ফলাফল যাচাই করছি..."}
                  ]
                }
                
                Valid action names: launch_app, tap_element, enter_text, press_back, press_home, show_recents, show_notifications, take_screenshot, inspect_screen, verify_result, open_url.
                Output ONLY valid JSON.
            """.trimIndent()

            val aiResult = if (config.providerType == AIProviderType.OPENAI_COMPATIBLE) {
                openAIClient.generateContent(
                    prompt = prompt,
                    config = config,
                    systemInstruction = systemInstruction
                )
            } else {
                geminiClient.generateContent(
                    prompt = prompt,
                    config = config,
                    systemInstruction = systemInstruction
                )
            }

            if (aiResult.isSuccess) {
                val raw = aiResult.getOrNull() ?: ""
                val parsedTask = parseAIPlanResponse(prompt, raw)
                if (parsedTask != null) return parsedTask

                // If text response without JSON wrapper, treat directly as natural chat answer
                val cleanedText = cleanResponseText(raw)
                if (cleanedText.isNotBlank()) {
                    return AgentTask(
                        rawPrompt = prompt,
                        goal = "Human Conversation",
                        steps = emptyList(),
                        resultMessage = cleanedText
                    )
                }
            }
        }

        // 2. Intelligent Natural Language Intent Interpreter (for offline or pre-configured state)
        // Does not rely on rigid keywords — dynamically interprets intent!
        val naturalTask = interpretNaturalIntent(trimmed)
        if (naturalTask != null) {
            return naturalTask
        }

        // 3. Natural Bengali Conversational Engine
        val conversationResponse = getNaturalConversationalResponse(trimmed)
        return AgentTask(
            rawPrompt = prompt,
            goal = "Natural Conversation",
            steps = emptyList(),
            resultMessage = conversationResponse
        )
    }

    private fun interpretNaturalIntent(prompt: String): AgentTask? {
        val lower = prompt.lowercase()

        // 1. Calculator with multi-step math if user explicitly mentions calculator
        if (lower.contains("ক্যালকুলেটর") || lower.contains("calculator")) {
            val steps = mutableListOf<ActionStep>()
            steps.add(ActionStep(index = 0, action = ActionType.LAUNCH_APP, target = "Calculator", bengaliLabel = "ক্যালকুলেটর খুলছি..."))

            // Multi-step math inside calculator if numbers/operators are mentioned
            val op = detectMathExpression(prompt)
            if (op != null) {
                var stepIdx = 1
                for (ch in op.firstNum) {
                    steps.add(ActionStep(index = stepIdx++, action = ActionType.TAP_ELEMENT, target = ch.toString(), bengaliLabel = "'$ch' বোতাম চাপছি..."))
                }
                steps.add(ActionStep(index = stepIdx++, action = ActionType.TAP_ELEMENT, target = op.operatorSymbol, bengaliLabel = "'${op.operatorBengali}' চাপছি..."))
                for (ch in op.secondNum) {
                    steps.add(ActionStep(index = stepIdx++, action = ActionType.TAP_ELEMENT, target = ch.toString(), bengaliLabel = "'$ch' চাপছি..."))
                }
                steps.add(ActionStep(index = stepIdx++, action = ActionType.TAP_ELEMENT, target = "=", bengaliLabel = "সমান বোতাম চাপছি..."))
                steps.add(ActionStep(index = stepIdx, action = ActionType.VERIFY_RESULT, target = op.expectedResult, bengaliLabel = "ফলাফল '${op.expectedResult}' যাচাই করছি..."))
                return AgentTask(
                    rawPrompt = prompt,
                    goal = "Calculate ${op.firstNum} ${op.operatorSymbol} ${op.secondNum} in Calculator",
                    steps = steps,
                    resultMessage = "গণনা সম্পন্ন হয়েছে: ${op.firstNum} ${op.operatorSymbol} ${op.secondNum} = ${op.expectedResult}"
                )
            }

            return AgentTask(
                rawPrompt = prompt,
                goal = "Open Calculator",
                steps = steps,
                resultMessage = "ক্যালকুলেটর খোলা হয়েছে।"
            )
        }

        // 2. Math calculation expressions (e.g. "২৫ যোগ ৪০", "৫০০ টাকার ১০%", "১০ গুণ ২", "২০ / ৪")
        val mathTask = tryEvaluateMath(prompt)
        if (mathTask != null) return mathTask

        // 2. Navigation Actions: Home
        if (lower.contains("home") || lower.contains("হোম")) {
            return AgentTask(
                rawPrompt = prompt,
                goal = "Go to Home Screen",
                steps = listOf(ActionStep(index = 0, action = ActionType.PRESS_HOME, bengaliLabel = "হোম স্ক্রিনে ফিরে যাচ্ছি...")),
                resultMessage = "হোম স্ক্রিনে ফিরে এসেছি।"
            )
        }

        // 3. Navigation Actions: Back
        if (lower.contains("back") || lower.contains("পিছনে") || lower.contains("আগের স্ক্রিন") || lower.contains("ব্যাক")) {
            return AgentTask(
                rawPrompt = prompt,
                goal = "Go Back",
                steps = listOf(ActionStep(index = 0, action = ActionType.PRESS_BACK, bengaliLabel = "পূর্বের স্ক্রিনে ফিরে যাচ্ছি...")),
                resultMessage = "পূর্বের স্ক্রিনে ফিরে এসেছি।"
            )
        }

        // 4. Navigation Actions: Recent Apps
        if (lower.contains("recent") || lower.contains("রিসেন্ট") || lower.contains("সাম্প্রতিক অ্যাপ")) {
            return AgentTask(
                rawPrompt = prompt,
                goal = "Show Recent Apps",
                steps = listOf(ActionStep(index = 0, action = ActionType.SHOW_RECENTS, bengaliLabel = "সাম্প্রতিক অ্যাপ তালিকা খুলছি...")),
                resultMessage = "রিসেন্ট অ্যাপস প্রদর্শন করা হচ্ছে।"
            )
        }

        // 5. Navigation Actions: Notifications
        if (lower.contains("notification") || lower.contains("নোটিফিকেশন") || lower.contains("পর্দা নামাও")) {
            return AgentTask(
                rawPrompt = prompt,
                goal = "Show Notifications",
                steps = listOf(ActionStep(index = 0, action = ActionType.SHOW_NOTIFICATIONS, bengaliLabel = "নোটিফিকেশন প্যানেল নামাচ্ছি...")),
                resultMessage = "নোটিফিকেশন প্যানেল নামানো হয়েছে।"
            )
        }

        // 6. Screenshot
        if (lower.contains("screenshot") || lower.contains("স্ক্রিনশট") || lower.contains("পর্দার ছবি")) {
            return AgentTask(
                rawPrompt = prompt,
                goal = "Take Screenshot",
                steps = listOf(ActionStep(index = 0, action = ActionType.TAKE_SCREENSHOT, bengaliLabel = "স্ক্রিনশট নিচ্ছি...")),
                resultMessage = "স্ক্রিনশট সফলভাবে ধারণ করা হয়েছে।"
            )
        }

        // 7. Screen Inspection / Reading
        if (lower.contains("স্ক্রিনে কী আছে") || lower.contains("পড়ে শোনাও") || lower.contains("কী লেখা") || lower.contains("read screen")) {
            return AgentTask(
                rawPrompt = prompt,
                goal = "Inspect screen content",
                steps = listOf(ActionStep(index = 0, action = ActionType.INSPECT_SCREEN, bengaliLabel = "পর্দার লেখা ও উপাদান সংগ্রহ করছি...")),
                resultMessage = "পর্দার তথ্য পর্যবেক্ষণ সম্পন্ন হয়েছে।"
            )
        }

        // 8. YouTube Video / Music
        if (lower.contains("youtube") || lower.contains("ইউটিউব") || lower.contains("গান চালাও") || lower.contains("ভিডিও দেখাও") || lower.contains("গান শোনাও")) {
            val query = prompt
                .replace("youtube", "", ignoreCase = true)
                .replace("ইউটিউবে", "")
                .replace("ইউটিউব", "")
                .replace("চালিয়ে দাও", "")
                .replace("গান চালাও", "")
                .replace("গান শোনাও", "")
                .replace("ভিডিও দেখাও", "")
                .replace("চালাও", "")
                .replace("খোলো", "")
                .trim()
            val ytUrl = if (query.isNotBlank()) {
                "https://www.youtube.com/results?search_query=${java.net.URLEncoder.encode(query, "UTF-8")}"
            } else {
                "https://www.youtube.com"
            }
            return AgentTask(
                rawPrompt = prompt,
                goal = if (query.isNotBlank()) "Play YouTube: $query" else "Open YouTube",
                steps = listOf(ActionStep(index = 0, action = ActionType.OPEN_URL, target = ytUrl, bengaliLabel = "ইউটিউব খুলছি...")),
                resultMessage = if (query.isNotBlank()) "ইউটিউবে '$query' চালানো হচ্ছে।" else "ইউটিউব চালু করা হয়েছে।"
            )
        }


        // 10. Natural Web Search / General Query
        if (lower.contains("সার্চ") || lower.contains("খোঁজো") || lower.contains("search") || lower.contains("গুগল") || lower.contains("কেমন") || lower.contains("কোথায়") || lower.contains("কবে") || lower.contains("কীভাবে")) {
            val query = prompt
                .replace("search", "", ignoreCase = true)
                .replace("google", "", ignoreCase = true)
                .replace("গুগলে", "")
                .replace("গুগল", "")
                .replace("সার্চ করো", "")
                .replace("খোঁজো", "")
                .trim()
            if (query.isNotBlank()) {
                val searchUrl = "https://www.google.com/search?q=${java.net.URLEncoder.encode(query, "UTF-8")}"
                return AgentTask(
                    rawPrompt = prompt,
                    goal = "Search web for: $query",
                    steps = listOf(ActionStep(index = 0, action = ActionType.OPEN_URL, target = searchUrl, bengaliLabel = "গুগলে অনুসন্ধান করছি...")),
                    resultMessage = "গুগলে '$query' ফলাফল খুঁজে বের করা হয়েছে।"
                )
            }
        }

        // 11. Generic App Launching from any natural language phrasing
        // Examples: "ফেসবুক খোলো", "ক্যামেরা ওপেন করো", "সেটিংস দেখতে চাই", "ঘড়ি চালু করো", "হোয়াটসঅ্যাপে যাও", "নোটস অ্যাপটি খোলো"
        val appName = extractAppNameFromPrompt(prompt)
        if (appName != null) {
            return AgentTask(
                rawPrompt = prompt,
                goal = "Open $appName",
                steps = listOf(ActionStep(index = 0, action = ActionType.LAUNCH_APP, target = appName, bengaliLabel = "$appName চালু করছি...")),
                resultMessage = "$appName সফলভাবে চালু করা হয়েছে।"
            )
        }

        return null
    }

    private fun extractAppNameFromPrompt(prompt: String): String? {
        val lower = prompt.lowercase()
        val launchPhrases = listOf(
            "খোলো", "খোল", "ওপেন করো", "ওপেন", "চালু করো", "চালু", "যাও", "দেখাও",
            "open", "launch", "start", "run", "open up"
        )

        val hasLaunchIntent = launchPhrases.any { lower.contains(it) }
        if (!hasLaunchIntent) return null

        val appAliases = listOf(
            "facebook" to "Facebook",
            "ফেসবুক" to "Facebook",
            "whatsapp" to "WhatsApp",
            "হোয়াটসঅ্যাপ" to "WhatsApp",
            "হোয়াটসঅ্যাপ" to "WhatsApp",
            "camera" to "Camera",
            "ক্যামেরা" to "Camera",
            "ছবি তোলো" to "Camera",
            "settings" to "Settings",
            "সেটিংস" to "Settings",
            "setting" to "Settings",
            "wifi" to "Wi-Fi Settings",
            "ওয়াইফাই" to "Wi-Fi Settings",
            "ওয়াইফাই" to "Wi-Fi Settings",
            "bluetooth" to "Bluetooth Settings",
            "ব্লুটুথ" to "Bluetooth Settings",
            "clock" to "Clock",
            "ঘড়ি" to "Clock",
            "ঘড়ি" to "Clock",
            "alarm" to "Alarm Clock",
            "অ্যালার্ম" to "Alarm Clock",
            "chrome" to "Chrome",
            "ক্রোম" to "Chrome",
            "browser" to "Browser",
            "ব্রাউজার" to "Browser",
            "gallery" to "Gallery",
            "গ্যালারি" to "Gallery",
            "photos" to "Photos",
            "ফটো" to "Photos",
            "notes" to "Notes",
            "নোটস" to "Notes",
            "নোট" to "Notes",
            "play store" to "Play Store",
            "প্লে স্টোর" to "Play Store",
            "maps" to "Maps",
            "ম্যাপস" to "Maps",
            "ম্যাপ" to "Maps",
            "phone" to "Phone",
            "ফোন" to "Phone",
            "dialer" to "Dialer",
            "ডায়ালার" to "Dialer",
            "messages" to "Messages",
            "মেসেজ" to "Messages",
            "sound" to "Sound Settings",
            "সাউন্ড" to "Sound Settings",
            "volume" to "Sound Settings",
            "ভলিউম" to "Sound Settings",
            "display" to "Display Settings",
            "ডিসপ্লে" to "Display Settings",
            "battery" to "Battery Settings",
            "ব্যাটারি" to "Battery Settings"
        )

        for ((alias, canonical) in appAliases) {
            if (lower.contains(alias)) {
                return canonical
            }
        }

        // If user specified an unknown app name after "open" or before "খোলো"
        val words = prompt.split(" ")
        for (w in words) {
            val clean = w.replace("টা", "").replace("টি", "").replace("অ্যাপ", "").trim()
            if (clean.length > 2 && !launchPhrases.contains(clean.lowercase())) {
                return clean
            }
        }

        return null
    }

    private data class MathOperation(
        val firstNum: String,
        val secondNum: String,
        val operatorSymbol: String,
        val operatorBengali: String,
        val expectedResult: String
    )

    private fun detectMathExpression(prompt: String): MathOperation? {
        val lower = prompt.lowercase()
        val numMap = mapOf(
            "শূন্য" to "0", "এক" to "1", "দুই" to "2", "তিন" to "3", "চার" to "4",
            "পাঁচ" to "5", "ছয়" to "6", "সাত" to "7", "আট" to "8", "নয়" to "9", "দশ" to "10",
            "০" to "0", "১" to "1", "২" to "2", "৩" to "3", "৪" to "4",
            "৫" to "5", "৬" to "6", "৭" to "7", "৮" to "8", "৯" to "9"
        )

        var parsed = lower
        for ((word, digit) in numMap) {
            parsed = parsed.replace(word, digit)
        }

        val pattern = Regex("(\\d+)\\s*([+\\-*×/÷]|যোগ|বিয়োগ|বিয়োগ|গুণ|গুন|ভাগ)\\s*(\\d+)")
        val match = pattern.find(parsed) ?: return null

        val num1Str = match.groupValues[1]
        val opRaw = match.groupValues[2]
        val num2Str = match.groupValues[3]

        val n1 = num1Str.toLongOrNull() ?: return null
        val n2 = num2Str.toLongOrNull() ?: return null

        val (symbol, bOp, res) = when {
            opRaw == "+" || opRaw.contains("যোগ") -> Triple("+", "যোগ", (n1 + n2).toString())
            opRaw == "-" || opRaw.contains("বিয়োগ") || opRaw.contains("বিয়োগ") -> Triple("-", "বিয়োগ", (n1 - n2).toString())
            opRaw == "*" || opRaw == "×" || opRaw.contains("গুণ") || opRaw.contains("গুন") -> Triple("×", "গুণ", (n1 * n2).toString())
            opRaw == "/" || opRaw == "÷" || opRaw.contains("ভাগ") -> {
                if (n2 == 0L) return null
                Triple("÷", "ভাগ", (n1 / n2).toString())
            }
            else -> return null
        }

        return MathOperation(num1Str, num2Str, symbol, bOp, res)
    }

    private fun tryEvaluateMath(prompt: String): AgentTask? {
        val op = detectMathExpression(prompt) ?: return null
        return AgentTask(
            rawPrompt = prompt,
            goal = "Calculate ${op.firstNum} ${op.operatorSymbol} ${op.secondNum}",
            steps = emptyList(),
            resultMessage = "${op.firstNum} ${op.operatorSymbol} ${op.secondNum} এর হিসাব অনুযায়ী ফলাফল হলো ${op.expectedResult}।"
        )
    }

    private fun getNaturalConversationalResponse(prompt: String): String {
        val lower = prompt.lowercase()
        return when {
            lower.contains("কেমন আছো") || lower.contains("কেমন আছেন") || lower.contains("how are you") ->
                "আমি খুব ভালো আছি! আপনার স্মার্টফোনে যে কোনো কাজ পরিচালনা করতে বা যে কোনো বিষয়ে সাহায্য করতে প্রস্তুত। আপনার কী প্রয়োজন বলুন?"

            lower.contains("তুমি কে") || lower.contains("তোমার নাম কী") || lower.contains("who are you") ->
                "আমি নাফসি (Nafsi AI) — আপনার অ্যান্ড্রয়েড ফোনের ব্যক্তিগত স্বায়ত্তশাসিত সহকারী। আমি মানুষের মতোই কথা বুঝতে পারি এবং মোবাইলের সমস্ত কাজ পরিচালনা করতে পারি।"

            lower.contains("কী কী করতে পারো") || lower.contains("তোমার কাজ কী") || lower.contains("what can you do") ->
                "আমি আপনার ফোনের অ্যাপস খোলা, বোতাম স্পর্শ করা, গণনা করা, ইউটিউবে গান চালানো, ওয়েবে তথ্য খোঁজা, স্ক্রিনের লেখা পড়া এবং মানুষের মতো যে কোনো বিষয়ে কথা বলা বা পরামর্শ দেওয়া সম্পন্ন করতে পারি।"

            lower.contains("ধন্যবাদ") || lower.contains("থ্যাংক ইউ") || lower.contains("thank you") || lower.contains("thanks") ->
                "আপনাকে অনেক ধন্যবাদ! আপনার কাজে আসতে পেরে আমি অত্যন্ত আনন্দিত। আর কিছু করতে হবে কি?"

            lower.contains("শুভ সকাল") || lower.contains("good morning") ->
                "সুপ্রভাত! আপনার আজকের দিনটি দারুণ ও সফল হোক। আমি কীভাবে আপনাকে সাহায্য করতে পারি?"

            lower.contains("শুভ রাত্রি") || lower.contains("good night") ->
                "শুভ রাত্রি! আপনার ঘুম সুন্দর ও প্রশান্তিময় হোক। আগামীকাল আবার দেখা হবে!"

            lower.contains("কৌতুক") || lower.contains("হাসাও") || lower.contains("joke") ->
                "একটি মজার কৌতুক শুনুন:\nশিক্ষক ছাত্রকে জিজ্ঞেস করলেন: বলতো পৃথিবীর সবচেয়ে দ্রুতগামী জিনিস কী?\nছাত্র বলল: স্যার, বিদ্যুৎ!\nশিক্ষক: কীভাবে?\nছাত্র: সুইচ টেপার সাথে সাথেই লাইট জ্বলে ওঠে!\nশিক্ষক: বাহ! আর কিছু?\nছাত্র: স্যার, মানুষের চিন্তা! কারণ লাইট জ্বালানোর আগেই মন চায় ঘুমাতে যাই!"

            lower.contains("কবিতা") || lower.contains("poem") ->
                "জীবনানন্দ দাশের দুটি বিখ্যাত লাইন মনে করিয়ে দিচ্ছি:\n'সুরঞ্জনা, ওইখানে যেয়ো নাকো তুমি,\nবলো নাকো কথা ওই যুবকের সাথে;\nফিরে এসো সুরঞ্জনা:\nনক্ষত্রের রূপালি আগুন ভরা রাতে...'"

            lower.contains("সময়") || lower.contains("কয়টা বাজে") || lower.contains("time") -> {
                val now = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date())
                "বর্তমানে স্থানীয় ঘড়ির সময় হলো $now।"
            }

            lower.contains("তারিখ") || lower.contains("আজকে কি বার") || lower.contains("date") -> {
                val today = SimpleDateFormat("EEEE, d MMMM yyyy", Locale.getDefault()).format(Date())
                "আজকের তারিখ হলো $today।"
            }

            else ->
                "আমি আপনার কথাটি বুঝতে পেরেছি। আপনি চাইলে আমাকে যেকোনো অ্যাপ চালু করতে, হিসাব করতে, গান চালাতে বা যেকোনো তথ্য জানতে বলতে পারেন। আমি মানুষের মতোই আপনাকে সার্বক্ষণিক সহায়তা করব।"
        }
    }

    private fun parseAIPlanResponse(rawPrompt: String, rawText: String): AgentTask? {
        try {
            val jsonStr = extractJsonString(rawText) ?: return null
            val json = JSONObject(jsonStr)

            val type = json.optString("type", "chat")
            if (type == "chat") {
                val response = json.optString("response", "")
                if (response.isNotBlank()) {
                    return AgentTask(
                        rawPrompt = rawPrompt,
                        goal = "Answer Question",
                        steps = emptyList(),
                        resultMessage = response
                    )
                }
            }

            if (type == "action_plan") {
                val goal = json.optString("goal", "Execute plan")
                val stepsArray = json.optJSONArray("steps") ?: JSONArray()
                val steps = mutableListOf<ActionStep>()

                for (i in 0 until stepsArray.length()) {
                    val stepObj = stepsArray.getJSONObject(i)
                    val actionName = stepObj.optString("action", "launch_app")
                    val target = stepObj.optString("target", "")
                    val label = stepObj.optString("label", "ধাপ সম্পন্ন করছি...")

                    val actionType = mapActionType(actionName)
                    steps.add(
                        ActionStep(
                            index = i,
                            action = actionType,
                            target = target,
                            bengaliLabel = label
                        )
                    )
                }

                if (steps.isNotEmpty()) {
                    return AgentTask(
                        rawPrompt = rawPrompt,
                        goal = goal,
                        steps = steps,
                        resultMessage = "কাজটি সফলভাবে সম্পন্ন হয়েছে।"
                    )
                }
            }
        } catch (_: Exception) {}
        return null
    }

    private fun mapActionType(name: String): ActionType {
        return when (name.lowercase()) {
            "launch_app", "open_app" -> ActionType.LAUNCH_APP
            "tap_element", "click_element" -> ActionType.TAP_ELEMENT
            "tap_coordinate", "click_coordinate" -> ActionType.TAP_COORDINATE
            "enter_text", "type_text" -> ActionType.ENTER_TEXT
            "press_back", "back" -> ActionType.PRESS_BACK
            "press_home", "home" -> ActionType.PRESS_HOME
            "show_recents", "recents" -> ActionType.SHOW_RECENTS
            "show_notifications", "notifications" -> ActionType.SHOW_NOTIFICATIONS
            "take_screenshot", "screenshot" -> ActionType.TAKE_SCREENSHOT
            "inspect_screen" -> ActionType.INSPECT_SCREEN
            "open_url" -> ActionType.OPEN_URL
            "verify_result", "verify" -> ActionType.VERIFY_RESULT
            else -> ActionType.LAUNCH_APP
        }
    }

    private fun extractJsonString(raw: String): String? {
        val trimmed = raw.trim()
        val startIndex = trimmed.indexOf('{')
        val endIndex = trimmed.lastIndexOf('}')
        if (startIndex != -1 && endIndex != -1 && endIndex > startIndex) {
            return trimmed.substring(startIndex, endIndex + 1)
        }
        return null
    }

    private fun cleanResponseText(raw: String): String {
        return raw.replace(Regex("```json[\\s\\S]*?```"), "")
            .replace(Regex("```[\\s\\S]*?```"), "")
            .trim()
    }
}
