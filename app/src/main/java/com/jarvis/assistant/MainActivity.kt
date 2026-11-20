package com.jarvis.assistant

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

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

@Composable
fun JarvisApp() {
    var input by remember { mutableStateOf("") }

    val messages = remember {
        mutableStateListOf(
            ChatMessage("Hello! I am JARVIS. How can I assist you?", false)
        )
    }

    val bg = Color(0xFF07111F)
    val blue = Color(0xFF00C8FF)

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = bg
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {

            // Header
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
                    text = "● ONLINE",
                    color = Color.Green,
                    fontSize = 13.sp
                )
            }

            HorizontalDivider(color = blue.copy(alpha = 0.3f))

            // Chat
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(messages) { message ->
                    ChatBubble(message)
                }
            }

            // Input area
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
                    shape = RoundedCornerShape(20.dp)
                )

                Spacer(modifier = Modifier.width(8.dp))

                Button(
                    onClick = {
                        val question = input.trim()

                        if (question.isNotEmpty()) {
                            messages.add(ChatMessage(question, true))
                            messages.add(
                                ChatMessage(
                                    getJarvisReply(question),
                                    false
                                )
                            )
                            input = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = blue
                    )
                ) {
                    Text("Send", color = Color.Black)
                }
            }
        }
    }
}

@Composable
fun ChatBubble(message: ChatMessage) {
    val bubbleColor = if (message.isUser) {
        Color(0xFF123B58)
    } else {
        Color(0xFF142333)
    }

    val horizontalAlignment = if (message.isUser) {
        Alignment.End
    } else {
        Alignment.Start
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = horizontalAlignment
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
fun getJarvisReply(question: String): String {
    val q = question.lowercase()

    return when {
        q.contains("hello") || q.contains("hi") ->
            "Hello! I am JARVIS. How can I help you?"

        q.contains("your name") ->
            "I am JARVIS, your personal AI assistant."

        q.contains("how are you") ->
            "All systems operational, Sir."

        q.contains("who are you") ->
            "I am JARVIS, your personal AI assistant."

        q.contains("time") ->
            "Time feature will be added soon."

        q.contains("help") ->
            "I am ready to assist you, Sir."

        else ->
            "I received your message: $question. My AI brain is not connected yet."
    }
}
