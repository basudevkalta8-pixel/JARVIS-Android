package com.jarvis.assistant

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
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

    val scope = rememberCoroutineScope()
    val messages = remember {
        mutableStateListOf(
            ChatMessage("Hello! I am JARVIS. How can I assist you today?", false)
        )
    }

    val history = remember { mutableStateListOf<JSONObject>() }
    val listState = rememberLazyListState()

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = bg
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            Column(
                modifier = Modifier.fillMaxWidth().padding(20.dp),
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

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    if (loading) "● THINKING..." else "● ONLINE",
                    color = if (loading) Color.Yellow else Color.Green,
                    fontSize = 13.sp
                )
            }

            HorizontalDivider(color = blue.copy(alpha = 0.3f))

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

                Spacer(modifier = Modifier.width(8.dp))

                Button(
                    enabled = !loading && input.isNotBlank(),
                    onClick = {
                        val question = input.trim()
                        if (question.isNotEmpty()) {
                            input = ""
                            messages.add(ChatMessage(question, true))
                            history.add(
                                JSONObject().put("role", "user").put("content", question)
                            )
                            loading = true

                            scope.launch {
                                val result = askOpenRouter(history.toList())

                                if (result.startsWith("ERROR:")) {
                                    messages.add(
                                        ChatMessage(result.removePrefix("ERROR:"), false)
                                    )
                                    history.removeAt(history.lastIndex)
                                } else {
                                    messages.add(ChatMessage(result, false))
                                    history.add(
                                        JSONObject().put("role", "assistant").put("content", result)
                                    )
                                }

                                loading = false

                                if (messages.isNotEmpty()) {
                                    listState.animateScrollToItem(messages.lastIndex)
                                }
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = blue)
                ) {
                    Text("Send", color = Color.Black)
                }
            }
        }
    }
}

suspend fun askOpenRouter(history: List<JSONObject>): String =
    withContext(Dispatchers.IO) {
        try {
            val apiKey = BuildConfig.OPENROUTER_API_KEY

            if (apiKey.isBlank()) {
                return@withContext "ERROR:OpenRouter API key is missing. Check GitHub Secrets and rebuild."
            }

            val messagesArray = JSONArray()

            messagesArray.put(
                JSONObject()
                    .put("role", "system")
                    .put(
                        "content",
                        "You are JARVIS, a helpful personal AI assistant. " +
                        "Be intelligent, friendly, concise, and natural. " +
                        "Reply in the user's language. Understand Hindi, Hinglish, and English. " +
                        "Address the user respectfully and help with questions, explanations, " +
                        "planning, learning, and everyday tasks. Never claim to perform " +
                        "actions you cannot actually do."
                    )
            )

            history.forEach { messagesArray.put(it) }

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
                    return@withContext "ERROR:AI connection failed (${response.code}). $responseText"
                }

                val result = JSONObject(responseText)
                    .getJSONArray("choices")
                    .getJSONObject(0)
                    .getJSONObject("message")
                    .getString("content")

                result.trim().ifEmpty { "I received an empty response, Sir." }
            }
        } catch (e: Exception) {
            "ERROR:Connection error: ${e.message ?: "Unknown error"}"
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
        horizontalAlignment = if (message.isUser) Alignment.End else Alignment.Start
    ) {
        Card(
            modifier = Modifier.widthIn(max = 300.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = bubbleColor)
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
