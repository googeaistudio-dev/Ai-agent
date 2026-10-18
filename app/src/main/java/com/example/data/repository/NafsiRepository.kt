package com.example.data.repository

import android.content.Context
import android.graphics.Bitmap
import com.example.automation.ActionExecutor
import com.example.automation.NafsiAccessibilityService
import com.example.data.ai.GeminiApiClient
import com.example.data.ai.ModelDiscoveryService
import com.example.data.ai.OpenAICompatibleClient
import com.example.data.storage.PreferencesManager
import com.example.domain.model.AIProviderConfig
import com.example.domain.model.ActionStep
import com.example.domain.model.AgentTask
import com.example.domain.model.ChatMessage
import com.example.domain.model.DiscoveredModel
import com.example.domain.model.MascotState
import com.example.domain.model.MessageSender
import com.example.domain.model.StepStatus
import com.example.domain.model.TaskStatus
import com.example.domain.planner.TaskPlanner
import com.example.screen.ScreenCaptureManager
import com.example.voice.AndroidSpeechEngine
import com.example.voice.ContinuousListeningManager
import com.example.voice.SpeechSynthesizer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class NafsiRepository(private val context: Context) {

    val preferences = PreferencesManager(context)
    val geminiClient = GeminiApiClient()
    val openAIClient = OpenAICompatibleClient()
    val modelDiscoveryService = ModelDiscoveryService()
    val actionExecutor = ActionExecutor(context)
    val screenCaptureManager = ScreenCaptureManager(context)

    private val speechEngine = AndroidSpeechEngine(context)
    val speechSynthesizer: SpeechSynthesizer = SpeechSynthesizer(context)
    val listeningManager: ContinuousListeningManager = ContinuousListeningManager(context, speechEngine, speechSynthesizer)

    val planner = TaskPlanner(geminiClient, openAIClient)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val _mascotState = MutableStateFlow(MascotState.IDLE)
    val mascotState: StateFlow<MascotState> = _mascotState.asStateFlow()

    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(
        listOf(
            ChatMessage(
                text = "নমস্কার! আমি নাফসি (Nafsi AI)। আপনার স্বায়ত্তশাসিত সহকারী। আপনি বাংলায় বা ইংরেজিতে যে কোনো নির্দেশ দিতে পারেন, যেমন: 'ক্যালকুলেটর খোলো এবং দুই যোগ দুই করো' অথবা স্ক্রিনের বিষয়ে যে কোনো প্রশ্ন করতে পারেন।",
                sender = MessageSender.ASSISTANT
            )
        )
    )
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages.asStateFlow()

    private val _activeTask = MutableStateFlow<AgentTask?>(null)
    val activeTask: StateFlow<AgentTask?> = _activeTask.asStateFlow()

    private val _diagnostics = MutableStateFlow<List<String>>(emptyList())
    val diagnostics: StateFlow<List<String>> = _diagnostics.asStateFlow()

    private val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

    init {
        // Sync voice settings
        val vs = preferences.voiceSettings.value
        speechSynthesizer.speechRate = vs.speechRate
        speechSynthesizer.speechPitch = vs.speechPitch
        speechSynthesizer.isMuted = vs.muteSpokenResponses
        listeningManager.languageCode = vs.language
        listeningManager.echoGuardDelayMs = vs.echoPreventionDelayMs

        speechSynthesizer.onStateChange = { isSpeaking ->
            listeningManager.onSpeechOutputStateChanged(isSpeaking)
            if (isSpeaking) {
                _mascotState.value = MascotState.SPEAKING
            } else if (_mascotState.value == MascotState.SPEAKING) {
                _mascotState.value = if (listeningManager.isContinuousListening.value) MascotState.LISTENING else MascotState.IDLE
            }
        }

        // Wire listening callback to user command execution
        listeningManager.onCommandRecognized = { command ->
            handleUserCommand(command)
        }

        logDiagnostic("Nafsi AI Repository initialized. Continuous listening ready.")
    }

    fun setMascotState(state: MascotState) {
        _mascotState.value = state
    }

    fun handleUserCommand(rawText: String) {
        val trimmed = rawText.trim()
        if (trimmed.isBlank()) return

        // Check voice control toggles directly
        if (trimmed.contains("listening বন্ধ") || trimmed.contains("কথা বন্ধ") || trimmed.contains("stop listening")) {
            listeningManager.stopContinuousListening()
            _mascotState.value = MascotState.PAUSED
            speakFeedback("লিসেনিং বন্ধ করা হয়েছে।")
            return
        }
        if (trimmed.contains("listening চালু") || trimmed.contains("শোনো") || trimmed.contains("start listening")) {
            listeningManager.startContinuousListening()
            _mascotState.value = MascotState.LISTENING
            speakFeedback("লিসেনিং চালু করা হয়েছে। আমি শুনছি।")
            return
        }

        // Add user chat message
        val userMsg = ChatMessage(text = trimmed, sender = MessageSender.USER)
        _chatMessages.value = _chatMessages.value + userMsg
        logDiagnostic("Command received: '$trimmed'")

        scope.launch {
            _mascotState.value = MascotState.THINKING

            val config = preferences.providerConfig.value
            val task = planner.planTask(trimmed, config)
            _activeTask.value = task

            if (task.steps.isNotEmpty()) {
                executeAgentTask(task)
            } else {
                // Conversational response
                val responseText = task.resultMessage ?: "আমি আপনার অনুরোধটি প্রক্রিয়া করতে পেরেছি।"
                _chatMessages.value = _chatMessages.value + ChatMessage(
                    text = responseText,
                    sender = MessageSender.ASSISTANT
                )
                logDiagnostic("Response generated: ${responseText.take(40)}...")
                speakFeedback(responseText)
            }
        }
    }

    private suspend fun executeAgentTask(task: AgentTask) {
        _mascotState.value = MascotState.WORKING
        var updatedTask = task.copy(status = TaskStatus.RUNNING)
        _activeTask.value = updatedTask

        val updatedSteps = updatedTask.steps.toMutableList()
        var hasFailure = false
        var failureReason = ""

        for (i in updatedSteps.indices) {
            val step = updatedSteps[i]
            updatedTask = updatedTask.copy(activeStepIndex = i)
            _activeTask.value = updatedTask

            if (step.bengaliLabel.isNotBlank()) {
                speakFeedback(step.bengaliLabel)
            }

            val executedStep = actionExecutor.executeStep(step)
            updatedSteps[i] = executedStep
            updatedTask = updatedTask.copy(steps = updatedSteps.toList())
            _activeTask.value = updatedTask

            if (executedStep.status == StepStatus.FAILED) {
                hasFailure = true
                failureReason = executedStep.result ?: "ধাপটি ব্যর্থ হয়েছে।"
                logDiagnostic("Step $i failed: $failureReason")
                break
            }
            delay(300L)
        }

        if (hasFailure) {
            _mascotState.value = MascotState.ERROR
            val finalTask = updatedTask.copy(
                status = TaskStatus.FAILED,
                resultMessage = "কাজটি সম্পন্ন করা যায়নি: $failureReason"
            )
            _activeTask.value = finalTask
            _chatMessages.value = _chatMessages.value + ChatMessage(
                text = "কাজটি সম্পন্ন করা যায়নি: $failureReason",
                sender = MessageSender.ASSISTANT,
                task = finalTask,
                error = failureReason
            )
            speakFeedback("দুঃখিত, কাজটি সম্পূর্ণ করা যায়নি। $failureReason")
        } else {
            _mascotState.value = MascotState.SUCCESS
            val successMsg = task.resultMessage ?: "কাজটি সফলভাবে সম্পন্ন হয়েছে এবং ফলাফল যাচাই করা হয়েছে।"
            val finalTask = updatedTask.copy(
                status = TaskStatus.SUCCESS,
                resultMessage = successMsg
            )
            _activeTask.value = finalTask
            _chatMessages.value = _chatMessages.value + ChatMessage(
                text = successMsg,
                sender = MessageSender.ASSISTANT,
                task = finalTask
            )
            logDiagnostic("Task completed successfully: $successMsg")
            speakFeedback(successMsg)
        }

        // Return to idle / listening state after short pause
        scope.launch {
            delay(3500L)
            if (_mascotState.value == MascotState.SUCCESS || _mascotState.value == MascotState.ERROR) {
                _mascotState.value = if (listeningManager.isContinuousListening.value) MascotState.LISTENING else MascotState.IDLE
            }
        }
    }

    fun speakFeedback(text: String) {
        val lang = preferences.voiceSettings.value.language
        speechSynthesizer.speak(text, lang)
    }

    fun cancelActiveTask() {
        _activeTask.value?.let { current ->
            _activeTask.value = current.copy(status = TaskStatus.CANCELLED, resultMessage = "Task cancelled by user.")
            _mascotState.value = MascotState.IDLE
            logDiagnostic("Task cancelled: ${current.goal}")
            speakFeedback("কাজটি বাতিল করা হয়েছে।")
        }
    }

    fun clearChatHistory() {
        _chatMessages.value = emptyList()
        logDiagnostic("Chat history cleared.")
    }

    fun logDiagnostic(entry: String) {
        val timestamp = timeFormat.format(Date())
        val formatted = "[$timestamp] $entry"
        _diagnostics.value = (listOf(formatted) + _diagnostics.value).take(100)
    }

    fun isAccessibilityEnabled(): Boolean = NafsiAccessibilityService.isServiceEnabled

    fun release() {
        listeningManager.release()
        speechSynthesizer.shutdown()
        screenCaptureManager.release()
    }
}
