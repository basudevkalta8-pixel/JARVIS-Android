package com.jarvis.assistant

import android.os.Bundle
import android.content.Context
import android.content.Intent
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { JarvisApp() }
    }
}

data class ChatMessage(
    val text: String,
    val isUser: Boolean
)

private val client = OkHttpClient()
private val blue = Color(0xFF00C8FF)
private val bg = Color(0xFF07111F)

private const val PREFS_NAME = "jarvis_memory"
private const val CHAT_KEY = "saved_chat"
private const val HISTORY_KEY = "saved_history"

// APP LIST
private val appPackages = mapOf(
    "youtube" to "com.google.android.youtube",
    "whatsapp" to "com.whatsapp",
    "chrome" to "com.android.chrome",
    "instagram" to "com.instagram.android",
    "facebook" to "com.facebook.katana",
    "maps" to "com.google.android.apps.maps",
    "camera" to "com.android.camera",
    "gmail" to "com.google.android.gm",
    "telegram" to "org.telegram.messenger",
    "free fire" to "com.dts.freefireth"
)

// PHONE COMMANDS
fun executePhoneCommand(
    question: String,
    controller: PhoneController
): String? {

    val q = question.lowercase(Locale.ROOT).trim()

    // TORCH ON
    if (
        q.contains("torch on") ||
        q.contains("flashlight on") ||
        q.contains("light on") ||
        q.contains("torch chalu") ||
        q.contains("light chalu") ||
        q.contains("टॉर्च चालू")
    ) {
        return controller.torchOn()
    }

    // TORCH OFF
    if (
        q.contains("torch off") ||
        q.contains("flashlight off") ||
        q.contains("light off") ||
        q.contains("torch band") ||
        q.contains("light band") ||
        q.contains("टॉर्च बंद")
    ) {
        return controller.torchOff()
    }

    // SETTINGS
    if (
        q == "settings" ||
        q.contains("open settings") ||
        q.contains("phone settings") ||
        q.contains("settings kholo") ||
        q.contains("सेटिंग खोलो")
    ) {
        return controller.openSettings()
    }

    // TIMER
    val timerRegex = Regex(
        """(?:timer|टाइमर)\s*(?:for\s*)?(\d+)\s*(seconds?|secs?|minutes?|mins?|hours?|hrs?|सेकंड|मिनट)""",
        RegexOption.IGNORE_CASE
    )

    val timerMatch = timerRegex.find(q)

    if (timerMatch != null) {
        val value = timerMatch.groupValues[1].toIntOrNull() ?: return null
        val unit = timerMatch.groupValues[2].lowercase()

        val seconds = when {
            unit.startsWith("h") -> value * 3600
            unit.startsWith("m") || unit.contains("मिनट") -> value * 60
            else -> value
        }

        if (seconds > 0) {
            return controller.setTimer(seconds, "JARVIS Timer")
        }
    }

    // ALARM
    val alarmRegex = Regex(
        """(?:alarm|अलार्म)\s*(?:at|for)?\s*(\d{1,2})(?::(\d{2}))?\s*(am|pm)?""",
        RegexOption.IGNORE_CASE
    )

    val alarmMatch = alarmRegex.find(q)

    if (alarmMatch != null) {
        var hour = alarmMatch.groupValues[1].toIntOrNull() ?: return null
        val minute = alarmMatch.groupValues[2].toIntOrNull() ?: 0
        val ampm = alarmMatch.groupValues[3].lowercase()

        if (hour !in 0..23 || minute !in 0..59) {
            return "Please provide a valid alarm time."
        }

        if (ampm == "pm" && hour in 1..11) hour += 12
        if (ampm == "am" && hour == 12) hour = 0

        return controller.setAlarm(hour, minute)
    }

    // OPEN APPS
    val openWords = listOf(
        "open ",
        "launch ",
        "start ",
        "kholo ",
        "kholo",
        "chalao ",
        "चलाओ",
        "खोलो"
    )

    val isOpenCommand = openWords.any { q.contains(it) }

    if (isOpenCommand) {
        for ((appName, packageName) in appPackages) {
            if (q.contains(appName)) {
                return controller.openApp(packageName)
            }
        }
    }

    // DIRECT APP COMMAND
    for ((appName, packageName) in appPackages) {
        if (q == appName || q == "open $appName") {
            return controller.openApp(packageName)
        }
    }

    return null
}

@Composable
fun JarvisApp() {

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    val prefs = remember {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    val phoneController = remember {
        PhoneController(context)
    }

    var input by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var listening by remember { mutableStateOf(false) }
    var voiceReplyEnabled by remember { mutableStateOf(true) }
    var ttsReady by remember { mutableStateOf(false) }
    var tts by remember { mutableStateOf<TextToSpeech?>(null) }

    // CHAT MEMORY
    val messages = remember {
        mutableStateListOf<ChatMessage>().apply {
            val saved = prefs.getString(CHAT_KEY, null)

            if (saved != null) {
                try {
                    val array = JSONArray(saved)
                    for (i in 0 until array.length()) {
                        val item = array.getJSONObject(i)
                        add(
                            ChatMessage(
                                item.getString("text"),
                                item.getBoolean("isUser")
                            )
                        )
                    }
                } catch (_: Exception) {
                    clear()
                }
            }

            if (isEmpty()) {
                add(
                    ChatMessage(
                        "Hello! I am JARVIS. How can I assist you today?",
                        false
                    )
                )
            }
        }
    }

    val history = remember {
        mutableStateListOf<JSONObject>().apply {
            val saved = prefs.getString(HISTORY_KEY, null)

            if (saved != null) {
                try {
                    val array = JSONArray(saved)
                    for (i in 0 until array.length()) {
                        add(array.getJSONObject(i))
                    }
                } catch (_: Exception) {
                    clear()
                }
            }
        }
    }

    // SAVE MEMORY
    fun saveMemory() {
        val chatArray = JSONArray()
        messages.forEach {
            chatArray.put(
                JSONObject()
                    .put("text", it.text)
                    .put("isUser", it.isUser)
            )
        }

        val historyArray = JSONArray()
        history.forEach { historyArray.put(it) }

        prefs.edit()
            .putString(CHAT_KEY, chatArray.toString())
            .putString(HISTORY_KEY, historyArray.toString())
            .apply()
    }

    // TEXT TO SPEECH
    DisposableEffect(context) {
        val engine = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val result = tts?.setLanguage(Locale("hi", "IN"))
                ttsReady =
                    result != TextToSpeech.LANG_MISSING_DATA &&
                    result != TextToSpeech.LANG_NOT_SUPPORTED
            }
        }

        tts = engine

        onDispose {
            engine.stop()
            engine.shutdown()
        }
    }

    fun speakText(text: String) {
        if (!voiceReplyEnabled || !ttsReady) return

        tts?.speak(
            text,
            TextToSpeech.QUEUE_FLUSH,
            null,
            "JARVIS_REPLY"
        )
    }

    // SEND MESSAGE
    fun sendSpokenMessage(question: String) {
        if (question.isBlank() || loading) return

        messages.add(ChatMessage(question, true))
        saveMemory()
        loading = true

        scope.launch {
            // Phone control first
            val phoneResult = executePhoneCommand(question, phoneController)

            if (phoneResult != null) {
                messages.add(ChatMessage(phoneResult, false))

                history.add(
                    JSONObject()
                        .put("role", "user")
                        .put("content", question)
                )

                history.add(
                    JSONObject()
                        .put("role", "assistant")
                        .put("content", phoneResult)
                )

                saveMemory()
                speakText(phoneResult)
                loading = false

                if (messages.isNotEmpty()) {
                    listState.animateScrollToItem(messages.lastIndex)
                }

                return@launch
            }

            // AI CHAT
            history.add(
                JSONObject()
                    .put("role", "user")
                    .put("content", question)
            )

            val result = askOpenRouter(history.toList())

            if (result.startsWith("ERROR:")) {
                messages.add(
                    ChatMessage(result.removePrefix("ERROR:"), false)
                )

                if (history.isNotEmpty()) {
                    history.removeAt(history.lastIndex)
                }
            } else {
                messages.add(ChatMessage(result, false))

                history.add(
                    JSONObject()
                        .put("role", "assistant")
                        .put("content", result)
                )

                speakText(result)
            }

            saveMemory()
            loading = false

            if (messages.isNotEmpty()) {
                listState.animateScrollToItem(messages.lastIndex)
            }
        }
    }

    // CLEAR CHAT
    fun clearChat() {
        tts?.stop()
        messages.clear()
        history.clear()

        messages.add(
            ChatMessage(
                "Chat cleared. I am ready for a new conversation.",
                false
            )
        )

        saveMemory()
    }

    // SPEECH RECOGNITION
    val speechLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        listening = false

        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val spokenText = result.data
                ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                ?.firstOrNull()

            if (!spokenText.isNullOrBlank()) {
                input = spokenText
                sendSpokenMessage(spokenText)
            }
        }
    }

    // MICROPHONE
    fun startListening() {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            messages.add(
                ChatMessage(
                    "Speech recognition is unavailable on this device.",
                    false
                )
            )
            return
        }

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "hi-IN")
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak to JARVIS")
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }

        listening = true
        speechLauncher.launch(intent)
    }

    // UI
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = bg
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            // HEADER
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "J.A.R.V.I.S",
                    fontSize = 30.sp,
                    color = blue
                )

                Text(
                    "PERSONAL AI ASSISTANT",
                    fontSize = 12.sp,
                    color = Color.LightGray
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = when {
                        listening -> "● LISTENING..."
                        loading -> "● EXECUTING..."
                        else -> "● ONLINE"
                    },
                    color = if (listening || loading) Color.Yellow
                    else Color.Green,
                    fontSize = 13.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedButton(
                    onClick = { clearChat() },
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFFFF6B6B)
                    )
                ) {
                    Text("🗑 Clear Chat")
                }
            }

            HorizontalDivider(color = blue.copy(alpha = 0.3f))

            // CHAT
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(messages) { message ->
                    ChatBubble(message)
                }

                if (loading) {
                    item {
                        Text(
                            "JARVIS is thinking...",
                            color = blue,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }
            }

            // VOICE CONTROL
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Voice Reply",
                    color = Color.White,
                    modifier = Modifier.weight(1f)
                )

                Button(
                    onClick = { tts?.stop() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF8B2635)
                    )
                ) {
                    Text("Stop Voice")
                }

                Spacer(modifier = Modifier.width(8.dp))

                Switch(
                    checked = voiceReplyEnabled,
                    onCheckedChange = {
                        voiceReplyEnabled = it
                        if (!it) tts?.stop()
                    }
                )
            }

            // INPUT
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    modifier = Modifier.weight(1f),
                    placeholder = {
                        Text("Ask JARVIS...", color = Color.Gray)
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = blue,
                        unfocusedBorderColor = Color.Gray,
                        cursorColor = blue
                    ),
                    shape = RoundedCornerShape(20.dp),
                    enabled = !loading
                )

                Spacer(modifier = Modifier.width(6.dp))

                Button(
                    onClick = { startListening() },
                    enabled = !loading && !listening,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF163A54)
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp)
                ) {
                    Text("🎤", fontSize = 20.sp)
                }

                Spacer(modifier = Modifier.width(6.dp))

                Button(
                    onClick = {
                        val question = input.trim()
                        if (question.isNotEmpty()) {
                            input = ""
                            sendSpokenMessage(question)
                        }
                    },
                    enabled = !loading && input.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = blue
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp)
                ) {
                    Text("Send", color = Color.Black)
                }
            }
        }
    }
}

// OPENROUTER AI
suspend fun askOpenRouter(history: List<JSONObject>): String =
    withContext(Dispatchers.IO) {
        try {
            val apiKey = BuildConfig.OPENROUTER_API_KEY

            if (apiKey.isBlank()) {
                return@withContext "ERROR: OpenRouter API key missing."
            }

            val messagesArray = JSONArray()

            messagesArray.put(
                JSONObject()
                    .put("role", "system")
                    .put(
                        "content",
                        "You are JARVIS, an intelligent personal AI assistant. " +
                        "Be friendly, smart, natural and helpful. " +
                        "Reply in Hindi, Hinglish or English according to the user. " +
                        "Keep answers clear and conversational. " +
                        "Use conversation history to understand context. " +
                        "Never pretend to perform actions you cannot perform."
                    )
            )

            history.takeLast(30).forEach {
                messagesArray.put(it)
            }

            val json = JSONObject()
                .put("model", "openrouter/free")
                .put("messages", messagesArray)
                .put("temperature", 0.7)

            val body = json.toString().toRequestBody(
                "application/json".toMediaType()
            )

            val request = Request.Builder()
                .url("https://openrouter.ai/api/v1/chat/completions")
                .addHeader("Authorization", "Bearer $apiKey")
                .addHeader("Content-Type", "application/json")
                .post(body)
                .build()

            client.newCall(request).execute().use { response ->
                val responseTex
