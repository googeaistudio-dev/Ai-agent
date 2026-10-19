package com.example.domain.model

enum class MascotState(val bengaliLabel: String, val englishLabel: String) {
    IDLE("স্বাভাবিক অবস্থা", "Idle"),
    LISTENING("শুনছে...", "Listening"),
    SPEAKING("কথা বলছে...", "Speaking"),
    THINKING("ভাবছে...", "Thinking"),
    WORKING("কাজ করছে...", "Working"),
    SUCCESS("কাজ সম্পন্ন!", "Success"),
    ERROR("সমস্যা হয়েছে", "Error"),
    PAUSED("বিরতি", "Paused"),
    PERMISSION_REQUIRED("অনুমতি প্রয়োজন", "Permission Required")
}

enum class MessageSender {
    USER,
    ASSISTANT,
    SYSTEM
}

data class ChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val text: String,
    val sender: MessageSender,
    val timestamp: Long = System.currentTimeMillis(),
    val task: AgentTask? = null,
    val isStreaming: Boolean = false,
    val error: String? = null
)

enum class TaskStatus {
    PENDING,
    RUNNING,
    SUCCESS,
    FAILED,
    CANCELLED
}

enum class ActionType(val label: String) {
    LAUNCH_APP("launch_app"),
    TAP_ELEMENT("tap_element"),
    TAP_COORDINATE("tap_coordinate"),
    LONG_PRESS("long_press"),
    SCROLL("scroll"),
    SWIPE("swipe"),
    ENTER_TEXT("enter_text"),
    PRESS_KEY("press_key"),
    CLEAR_TEXT("clear_text"),
    PRESS_BACK("press_back"),
    PRESS_HOME("press_home"),
    SHOW_RECENTS("show_recents"),
    SHOW_NOTIFICATIONS("show_notifications"),
    TAKE_SCREENSHOT("take_screenshot"),
    OPEN_SETTINGS("open_settings"),
    OPEN_URL("open_url"),
    INSPECT_SCREEN("inspect_screen"),
    VERIFY_RESULT("verify_result")
}

enum class StepStatus {
    PENDING,
    EXECUTING,
    COMPLETED,
    FAILED,
    SKIPPED
}

data class ActionStep(
    val index: Int,
    val action: ActionType,
    val target: String? = null,
    val args: Map<String, String> = emptyMap(),
    val status: StepStatus = StepStatus.PENDING,
    val result: String? = null,
    val bengaliLabel: String = ""
)

data class AgentTask(
    val id: String = java.util.UUID.randomUUID().toString(),
    val rawPrompt: String,
    val goal: String,
    val steps: List<ActionStep>,
    val status: TaskStatus = TaskStatus.PENDING,
    val activeStepIndex: Int = 0,
    val resultMessage: String? = null,
    val requiresConfirmation: Boolean = false,
    val confirmationPrompt: String? = null
)

enum class AIProviderType {
    GEMINI_DIRECT,
    OPENAI_COMPATIBLE
}

data class AIProviderConfig(
    val providerType: AIProviderType = AIProviderType.GEMINI_DIRECT,
    val apiKey: String = "",
    val baseUrl: String = "https://generativelanguage.googleapis.com/",
    val modelId: String = "gemini-3.8-flash",
    val visionModelId: String = "gemini-3.8-flash",
    val temperature: Float = 0.4f,
    val maxTokens: Int = 2048,
    val timeoutSeconds: Int = 60,
    val useGoogleSearch: Boolean = true,
    val systemInstruction: String = ""
)

data class DiscoveredModel(
    val id: String,
    val displayName: String,
    val supportsVision: Boolean = true,
    val description: String = ""
)

data class VoiceSettings(
    val continuousListening: Boolean = true,
    val language: String = "bn-BD", // "bn-BD", "en-US", "auto"
    val speechRate: Float = 1.0f,
    val speechPitch: Float = 1.0f,
    val muteSpokenResponses: Boolean = false,
    val echoPreventionDelayMs: Long = 700L
)
