package com.jarvis.assistant

import android.os.Bundle
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

        setContent {
            JarvisApp()
        }
    }
}

data class ChatMessage(
    val text: String,
    val isUser: Boolean
)

private val client = OkHttpClient()

private val blue = Color(0xFF00C8FF)
private val bg = Color(0xFF07111F)

@Composable
fun JarvisApp() {

    var input by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var listening by remember { mutableStateOf(false) }
    var voiceReplyEnabled by remember { mutableStateOf(true) }

    val scope = rememberCoroutineScope()

    val messages = remember {
        mutableStateListOf(
            ChatMessage(
                "Hello! I am JARVIS. How can I assist you today?",
                false
            )
        )
    }

    val history = remember {
        mutableStateListOf<JSONObject>()
    }

    val listState = rememberLazyListState()

    // Text to Speech
    var tts by remember {
        mutableStateOf<TextToSpeech?>(null)
    }

    var ttsReady by remember {
        mutableStateOf(false)
    }

    DisposableEffect(Unit) {
        val engine = TextToSpeech(
            androidx.compose.ui.platform.LocalContext.current
        ) { status ->
            if (status == TextToSpeech.SUCCESS) {
                ttsReady = true
            }
        }

        tts = engine

        onDispose {
            engine.stop()
            engine.shutdown()
        }
    }

    // Speech Recognition
    val context = androidx.compose.ui.platform.LocalContext.current

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
            }
        }
    }

    fun startListening() {

        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            messages.add(
                ChatMessage(
                    "Speech recognition is not available on this phone.",
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

            putExtra(
                RecognizerIntent.EXTRA_PROMPT,
                "Speak to JARVIS"
            )

            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
        }

        listening = true
        speechLauncher.launch(intent)
    }

    fun speakText(text: String) {

        if (voiceReplyEnabled && ttsReady) {

            val engine = tts ?: return

            val hindi = engine.setLanguage(Locale("hi", "IN"))

            if (hindi == TextToSpeech.LANG_MISSING_DATA ||
                hindi == TextToSpeech.LANG_NOT_SUPPORTED
            ) {
                engine.setLanguage(Locale.US)
            }

            engine.speak(
                text,
                TextToSpeech.QUEUE_FLUSH,
                null,
                "JARVIS_REPLY"
            )
        }
    }

    fun sendMessage(question: String) {

        if (question.isBlank() || loading) return

        messages.add(ChatMessage(question, true))

        history.add(
            JSONObject()
                .put("role", "user")
                .put("content", question)
        )

        loading = true

        scope.launch {

            val result = askOpenRouter(history.toList())

            if (result.startsWith("ERROR:")) {

                messages.add(
                    ChatMessage(
                        result.removePrefix("ERROR:"),
                        false
                    )
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

            loading = false

            if (messages.isNotEmpty()) {
                listState.animateScrollToItem(messages.lastIndex)
            }
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = bg
    ) {

        Column(
            modifier = Modifier.fillMaxSize()
        ) {

            // HEADER
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                Text(
                    text = "J.A.R.V.I.S",
                    fontSize = 30.sp,
                    color = blue
                )

                Text(
                    text = "PERSONAL AI ASSISTANT",
                    fontSize = 12.sp,
                    color = Color.LightGray
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = when {
                        listening -> "● LISTENING..."
                        loading -> "● THINKING..."
                        else -> "● ONLINE"
                    },
                    color = when {
                        listening -> Color.Yellow
                        loading -> Color.Yellow
                        else -> Color.Green
                    },
                    fontSize = 13.sp
                )
            }

            HorizontalDivider(
                color = blue.copy(alpha = 0.3f)
            )

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

            // VOICE REPLY SWITCH
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

                Switch(
                    checked = voiceReplyEnabled,
                    onCheckedChange = {
                        voiceReplyEnabled = it

                        if (!it) {
                            tts?.stop()
                        }
                    }
                )
            }

            // INPUT AREA
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
                        Text(
                            "Ask JARVIS...",
                            color = Color.Gray
                        )
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

                // MIC BUTTON
                Button(
                    onClick = { startListening() },
                    enabled = !loading && !listening,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF163A54)
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp)
                ) {
                    Text(
                        "🎤",
                        fontSize = 20.sp
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                // SEND BUTTON
                Button(
                    onClick = {
                        val question = input.trim()
                        if (question.isNotEmpty()) {
                            input = ""
                            sendMessage(question)
                        }
                    },
                    enabled = !loading && input.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = blue
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp)
                ) {
                    Text(
                        "Send",
                        color = Color.Black
                    )
                }
            }
        }
    }
}

suspend fun askOpenRouter(
    history: List<JSONObject>
): String = withContext(Dispatchers.IO) {

    try {

        val apiKey = BuildConfig.OPENROUTER_API_KEY

        if (apiKey.isBlank()) {
            return@withContext "ERROR:OpenRouter API key missing."
        }

        val messagesArray = JSONArray()

        messagesArray.put(
            JSONObject()
                .put("role", "system")
                .put(
                    "content",
                    "You are JARVIS, an intelligent personal AI assistant. " +
                    "Be friendly, smart, natural and helpful. " +
                    "Reply in Hindi, Hinglish or English depending on the user. " +
                    "Keep answers clear and conversational. " +
                    "Never pretend to perform actions you cannot perform."
                )
        )

        history.forEach {
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

            val responseText = response.body?.string().orEmpty()

            if (!response.isSuccessful) {
                return@withContext "ERROR:AI server error ${response.code}"
            }

            val result = JSONObject(responseText)
                .getJSONArray("choices")
                .getJSONObject(0)
                .getJSONObject("message")
                .getString("content")

            result.trim().ifEmpty {
                "I received an empty response."
            }
        }

    } catch (e: Exception) {

        "ERROR:Connection failed. Please check internet."
    }
}

@Composable
fun ChatBubble(message: ChatMessage) {

    val bubbleColor = if (message.isUser) {
        Color(0xFF123B58)
    } else {
        Color(0xFF142333)
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (message.isUser) {
            Alignment.End
        } else {
            Alignment.Start
        }
    ) {

        Card(
            modifier = Modifier.widthIn(max = 300.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = bubbleColor
            )
        ) {

            Text(
                text = message.text,
                color = Color.White,
                fontSize = 15.sp,
                modifier = Modifier.padding(14.dp)
            )
        }
    }
}
