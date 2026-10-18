# Nafsi AI — Autonomous Android Voice & Visual Agent

**Nafsi AI** (নাফসি) is a native Android autonomous voice and visual agent. It listens continuously to natural human speech, observes the active Android screen, reasons using Gemini models, executes multi-step UI actions across Android applications using an AccessibilityService, and verifies on-screen outcomes before reporting success.

---

## Architecture Subsystems

1. **Continuous Voice Engine (`com.example.voice`)**
   - **Speech Engine (`SpeechEngine.kt`, `AndroidSpeechEngine.kt`)**: Implements resilient session management for uninterrupted listening without wake words.
   - **Speech Synthesis (`SpeechSynthesizer.kt`)**: Multi-lingual Text-to-Speech supporting Bengali (`bn-BD`) and English (`en-US`), with custom speech rates, pitch, and clean speech markdown stripping.
   - **Echo & Self-Trigger Prevention (`ContinuousListeningManager.kt`)**: Coordinates microphone and TTS states, silencing listening during assistant speech and maintaining a 700ms acoustic guard delay.

2. **AI Conversation & Planner (`com.example.domain.planner` & `com.example.data.ai`)**
   - **Gemini REST Client (`GeminiApiClient.kt`)**: Connects to `gemini-3.8-flash`, `gemini-3.8-live`, `gemini-4-argon`, and `gemini-3.1-flash-lite`.
   - **Google Search Grounding**: Integrated using Gemini's native `googleSearch` tool for real-time up-to-date queries.
   - **OpenAI-Compatible Client (`OpenAICompatibleClient.kt`)**: Supports custom model endpoints and dynamic model discovery (`/v1/models`).
   - **Multi-step Structured Planner (`TaskPlanner.kt`)**: Transforms natural language commands (Bengali or English) into verifiable action sequences.

3. **Floating AI Mascot (`com.example.ui.mascot` & `com.example.overlay`)**
   - Chibi futuristic robot companion with glossy dark visor, glowing cyan eyes, crystalline cat-ear fins, and orbital halo rings.
   - **Visual States**:
     - **Idle** (স্বাভাবিক অবস্থা)
     - **Listening** (শুনছে)
     - **Speaking** (কথা বলছে)
     - **Thinking** (ভাবছে)
     - **Working** (কাজ করছে)
     - **Success** (কাজ সম্পন্ন)
     - **Error** (সমস্যা)
     - **Paused** (বিরতি)
   - Interactive drag-to-move, tap-for-radial-menu, and `WindowManager` floating overlay (`TYPE_APPLICATION_OVERLAY`).

4. **Screen Vision (`com.example.screen`)**
   - **Accessibility Tree Inspector**: Instant zero-overhead extraction of visible UI nodes, text, button identifiers, and descriptions.
   - **ScreenCaptureManager**: `MediaProjection` integration for visual screen capture and multimodal analysis when authorized.

5. **Accessibility-based Action Engine (`com.example.automation`)**
   - **NafsiAccessibilityService**: Genuine Android Accessibility Service.
   - **ActionExecutor**: Executes `launch_app`, `tap_element`, `tap_coordinate`, `scroll`, `enter_text`, `press_back`, `press_home`, `inspect_screen`, and `verify_result`.
   - **AppLauncherHelper**: Resolves and launches installed apps by label or package dynamically.

---

## Supported vs Unsupported Actions

### Supported Actions
- Launching any installed launcher application dynamically (e.g. Calculator, Settings, Notes, Chrome, Clock).
- Tapping accessible UI elements by visible text, content description, or view ID.
- Tapping screen coordinates via simulated gesture dispatch.
- Text input into accessible editable fields.
- System navigation (Back and Home).
- Inspecting on-screen text hierarchies.
- Verifying on-screen calculation outcomes (e.g. verifying that `4` appears after `2 + 2 =`).

### Unsupported Actions (By Android Platform Design)
- Bypassing secure windows (FLAG_SECURE screens such as banking apps, password fields).
- Accessing content behind lock screens without user biometric/PIN unlock.
- Programmatic silent activation of Accessibility without explicit user consent in System Settings.

---

## Required Permissions

- `RECORD_AUDIO`: Voice input for continuous speech recognition.
- `SYSTEM_ALERT_WINDOW`: Displaying the floating mascot companion over other apps.
- `FOREGROUND_SERVICE` & `FOREGROUND_SERVICE_MICROPHONE`: Running resilient continuous voice sessions.
- `INTERNET`: Communicating with Gemini API and model providers.
- `BIND_ACCESSIBILITY_SERVICE`: User-enabled UI automation service in Android Accessibility settings.
- `POST_NOTIFICATIONS`: Showing ongoing assistant status notifications.

---

## Building and Testing

Run compilation and unit tests:
```bash
gradle :app:assembleDebug
gradle :app:testDebugUnitTest
```
