package com.stark.jarvis

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.provider.ContactsContract
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.lifecycleScope
import com.stark.jarvis.actions.DeviceActionExecutor
import com.stark.jarvis.actions.PickedContact
import com.stark.jarvis.network.ChatTurn
import com.stark.jarvis.network.JarvisApiClient
import com.stark.jarvis.network.ProposedDeviceAction
import com.stark.jarvis.permissions.JarvisPermissionStatus
import com.stark.jarvis.permissions.PermissionManager
import com.stark.jarvis.ui.HudChatMessage
import com.stark.jarvis.ui.JarvisHudScreen
import com.stark.jarvis.ui.theme.JarvisTheme
import com.stark.jarvis.voice.JarvisVoiceEngine
import com.stark.jarvis.voice.WakeWordForegroundService
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {

    private lateinit var voiceEngine: JarvisVoiceEngine
    private lateinit var actionExecutor: DeviceActionExecutor
    private lateinit var apiClient: JarvisApiClient

    private val messages = mutableStateListOf<HudChatMessage>()
    private var isThinking by mutableStateOf(false)
    private var autoVoice by mutableStateOf(true)
    private var wakeWordEnabled by mutableStateOf(false)
    private var isTorchOn by mutableStateOf(false)
    private var selectedLanguage by mutableStateOf("auto")
    private var backendUrl by mutableStateOf(BuildConfig.DEFAULT_BACKEND_URL)
    private var permissionStatuses by mutableStateOf<List<JarvisPermissionStatus>>(emptyList())
    private var pendingConfirmationAction by mutableStateOf<ProposedDeviceAction?>(null)
    private var pickedContact by mutableStateOf<PickedContact?>(null)

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        permissionStatuses = PermissionManager.getAllStatuses(this)
    }

    private val contactPickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val contactUri = result.data?.data
        if (result.resultCode == RESULT_OK && contactUri != null) {
            val resolved = actionExecutor.readContactFromPickerUri(contactUri)
            if (resolved != null) {
                pickedContact = resolved
            } else {
                Toast.makeText(this, "Selected contact has no phone number.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private val wakeWordReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val cmd = intent?.getStringExtra(WakeWordForegroundService.EXTRA_COMMAND_TEXT).orEmpty()
            if (cmd.isNotBlank()) {
                sendToJarvis(cmd)
            } else {
                voiceEngine.speak("At your service, Sir.", selectedLanguage)
                startVoiceCommand()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val prefs = getSharedPreferences("jarvis_prefs", MODE_PRIVATE)
        backendUrl = prefs.getString("backend_url", BuildConfig.DEFAULT_BACKEND_URL)
            ?: BuildConfig.DEFAULT_BACKEND_URL
        autoVoice = prefs.getBoolean("auto_voice", true)

        voiceEngine = JarvisVoiceEngine(this)
        actionExecutor = DeviceActionExecutor(this)
        apiClient = JarvisApiClient { backendUrl }

        permissionStatuses = PermissionManager.getAllStatuses(this)
        if (permissionStatuses.any { !it.isGranted }) {
            permissionLauncher.launch(PermissionManager.requiredPermissions().toTypedArray())
        }

        messages.add(
            HudChatMessage(
                id = "init-1",
                isUser = false,
                text = "Systems online, Sir. Native Android 15 companion ready for Vivo T2 Pro 5G. Speak in English, हिन्दी, or Hinglish.",
                timestamp = currentTime()
            )
        )

        val filter = IntentFilter(WakeWordForegroundService.ACTION_WAKE_WORD_DETECTED)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(wakeWordReceiver, filter, RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(wakeWordReceiver, filter)
        }

        setContent {
            val isListening by voiceEngine.isListening.collectAsState()
            val isSpeaking by voiceEngine.isSpeaking.collectAsState()
            val liveTranscript by voiceEngine.liveTranscript.collectAsState()

            JarvisTheme {
                JarvisHudScreen(
                    messages = messages,
                    isThinking = isThinking,
                    isListening = isListening,
                    isSpeaking = isSpeaking,
                    liveTranscript = liveTranscript,
                    autoVoice = autoVoice,
                    wakeWordEnabled = wakeWordEnabled,
                    isTorchOn = isTorchOn,
                    selectedLanguage = selectedLanguage,
                    backendUrl = backendUrl,
                    permissionStatuses = permissionStatuses,
                    pendingConfirmationAction = pendingConfirmationAction,
                    pickedContact = pickedContact,
                    onSendMessage = { sendToJarvis(it) },
                    onToggleListening = {
                        if (isListening) voiceEngine.stopListening() else startVoiceCommand()
                    },
                    onToggleAutoVoice = {
                        autoVoice = !autoVoice
                        prefs.edit().putBoolean("auto_voice", autoVoice).apply()
                        if (!autoVoice) voiceEngine.stopSpeaking()
                    },
                    onToggleWakeWord = { toggleWakeWordService() },
                    onToggleFlashlight = { toggleFlashlight(!isTorchOn) },
                    onSelectLanguage = { selectedLanguage = it },
                    onStopSpeaking = { voiceEngine.stopSpeaking() },
                    onReplayAudio = { voiceEngine.speak(it, selectedLanguage) },
                    onPickContact = { launchContactPicker() },
                    onClearPickedContact = { pickedContact = null },
                    onConfirmAction = { action ->
                        pendingConfirmationAction = null
                        executeDeviceAction(action)
                    },
                    onDismissConfirmation = { pendingConfirmationAction = null },
                    onRequestPermissions = {
                        permissionLauncher.launch(PermissionManager.requiredPermissions().toTypedArray())
                    },
                    onOpenAppSettings = { PermissionManager.openAppSettings(this) },
                    onUpdateBackendUrl = { newUrl ->
                        backendUrl = newUrl.trim()
                        prefs.edit().putString("backend_url", backendUrl).apply()
                    }
                )
            }
        }
    }

    private fun launchContactPicker() {
        val pickIntent = Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI)
        if (pickIntent.resolveActivity(packageManager) != null) {
            contactPickerLauncher.launch(pickIntent)
        } else {
            val fallbackIntent = Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI)
            if (fallbackIntent.resolveActivity(packageManager) != null) {
                contactPickerLauncher.launch(fallbackIntent)
            } else {
                Toast.makeText(this, "No Contacts app found on this device.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun startVoiceCommand() {
        if (!PermissionManager.isGranted(this, android.Manifest.permission.RECORD_AUDIO)) {
            permissionLauncher.launch(arrayOf(android.Manifest.permission.RECORD_AUDIO))
            return
        }
        voiceEngine.startListening(
            languageMode = selectedLanguage,
            onFinalResult = { text -> sendToJarvis(text) },
            onError = { err -> Toast.makeText(this, err, Toast.LENGTH_SHORT).show() }
        )
    }

    private fun toggleWakeWordService() {
        if (!PermissionManager.isGranted(this, android.Manifest.permission.RECORD_AUDIO)) {
            permissionLauncher.launch(PermissionManager.requiredPermissions().toTypedArray())
            return
        }
        wakeWordEnabled = !wakeWordEnabled
        if (wakeWordEnabled) {
            WakeWordForegroundService.start(this)
            Toast.makeText(this, "Active Foreground Wake-Word ON ('Hey JARVIS')", Toast.LENGTH_SHORT).show()
        } else {
            WakeWordForegroundService.stop(this)
        }
    }

    private fun toggleFlashlight(enable: Boolean) {
        val result = actionExecutor.setFlashlight(enable)
        isTorchOn = actionExecutor.isTorchOn
        if (autoVoice) voiceEngine.speak(result.message, selectedLanguage)
        if (!result.success) {
            Toast.makeText(this, result.message, Toast.LENGTH_LONG).show()
        }
    }

    private fun sendToJarvis(userText: String) {
        if (userText.isBlank() || isThinking) return

        // Handle direct native hardware flashlight commands cleanly
        val lower = userText.lowercase()
        if (lower.contains("flashlight") || lower.contains("torch")) {
            val turnOn = !lower.contains("off") && !lower.contains("band")
            messages.add(HudChatMessage("u-${System.currentTimeMillis()}", true, userText, currentTime()))
            val res = actionExecutor.setFlashlight(turnOn)
            isTorchOn = actionExecutor.isTorchOn
            messages.add(HudChatMessage("b-${System.currentTimeMillis()}", false, res.message, currentTime()))
            if (autoVoice) voiceEngine.speak(res.message, selectedLanguage)
            return
        }

        messages.add(HudChatMessage("u-${System.currentTimeMillis()}", true, userText, currentTime()))
        isThinking = true

        lifecycleScope.launch {
            val history = messages.map { ChatTurn(if (it.isUser) "user" else "model", it.text) }
            val result = apiClient.sendMessage(userText, history, selectedLanguage)
            isThinking = false

            result.onSuccess { res ->
                var action = res.action
                // Pre-resolve contact number from Android Contacts if user asked to call/WhatsApp a contact name
                if (action != null && action.phoneNumber.isNullOrBlank() && !action.packageNameQuery.isNullOrBlank()
                    && action.actionType in listOf("CALL_PHONE", "SEND_WHATSAPP")
                ) {
                    val found = actionExecutor.findContactByName(action.packageNameQuery)
                    if (found != null) {
                        action = action.copy(
                            phoneNumber = found.phoneNumber,
                            actionTitle = "${action.actionTitle} (${found.name}: ${found.phoneNumber})"
                        )
                    }
                }

                messages.add(
                    HudChatMessage(
                        id = "b-${System.currentTimeMillis()}",
                        isUser = false,
                        text = res.reply,
                        timestamp = currentTime(),
                        proposedAction = action
                    )
                )
                if (autoVoice) voiceEngine.speak(res.reply, res.language)

                // Always prompt user confirmation before sensitive or device actions
                if (action != null && action.actionType in listOf(
                        "CALL_PHONE",
                        "SEND_WHATSAPP",
                        "SET_ALARM",
                        "SET_TIMER",
                        "FLASHLIGHT",
                        "OPEN_APP_INTENT",
                        "OPEN_INSTALLED_APP"
                    )
                ) {
                    pendingConfirmationAction = action
                }
            }.onFailure { err ->
                val errText = "Connection issue with Gemini backend: ${err.message}"
                messages.add(HudChatMessage("e-${System.currentTimeMillis()}", false, errText, currentTime()))
                if (autoVoice) voiceEngine.speak("Unable to reach server, Sir.", "en")
            }
        }
    }

    private fun executeDeviceAction(action: ProposedDeviceAction) {
        val result = when (action.actionType) {
            "FLASHLIGHT" -> {
                val res = actionExecutor.setFlashlight(action.flashlightState ?: !isTorchOn)
                isTorchOn = actionExecutor.isTorchOn
                res
            }
            "SET_ALARM" -> actionExecutor.createAlarm(
                hour = action.alarmHour ?: 7,
                minute = action.alarmMinute ?: 0,
                label = "J.A.R.V.I.S. Alarm"
            )
            "SET_TIMER" -> actionExecutor.createTimer(action.timerSeconds ?: 120)
            "CALL_PHONE" -> {
                val number = action.phoneNumber ?: action.packageNameQuery?.let {
                    actionExecutor.findContactByName(it)?.phoneNumber
                }.orEmpty()
                if (number.isBlank() && action.packageNameQuery != null) {
                    launchContactPicker()
                    com.stark.jarvis.actions.ActionExecutionResult(
                        true,
                        "Opening Contact Picker so you can select ${action.packageNameQuery}."
                    )
                } else {
                    actionExecutor.dialOrCall(number, directCall = false)
                }
            }
            "SEND_WHATSAPP" -> actionExecutor.openWhatsApp(action.phoneNumber, action.messageBody)
            "OPEN_APP_INTENT" -> when (action.appName) {
                "YouTube" -> actionExecutor.openYouTube(action.messageBody)
                "Google Maps" -> actionExecutor.openGoogleMaps(action.messageBody)
                "WhatsApp" -> actionExecutor.openWhatsApp(action.phoneNumber, action.messageBody)
                else -> action.targetUrl?.let { actionExecutor.openUrlInBrowser(it) }
                    ?: actionExecutor.openInstalledAppByName(action.appName.orEmpty())
            }
            "OPEN_INSTALLED_APP" -> actionExecutor.openInstalledAppByName(
                action.packageNameQuery ?: action.appName.orEmpty()
            )
            else -> action.targetUrl?.let { actionExecutor.openUrlInBrowser(it) }
                ?: com.stark.jarvis.actions.ActionExecutionResult(false, "Unsupported action type.")
        }

        if (!result.success) {
            Toast.makeText(this, result.message, Toast.LENGTH_LONG).show()
        }
        if (autoVoice) {
            voiceEngine.speak(result.message, selectedLanguage)
        }
    }

    private fun currentTime(): String {
        return SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
    }

    override fun onDestroy() {
        unregisterReceiver(wakeWordReceiver)
        voiceEngine.release()
        super.onDestroy()
    }
}
