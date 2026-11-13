package com.stark.jarvis.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.FlashlightOff
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stark.jarvis.actions.PickedContact
import com.stark.jarvis.network.ProposedDeviceAction
import com.stark.jarvis.permissions.JarvisPermissionStatus
import com.stark.jarvis.ui.theme.StarkAmber
import com.stark.jarvis.ui.theme.StarkCyan
import com.stark.jarvis.ui.theme.StarkCyanGlow
import com.stark.jarvis.ui.theme.StarkEmerald
import com.stark.jarvis.ui.theme.StarkHeaderBg
import com.stark.jarvis.ui.theme.StarkNavyBg
import com.stark.jarvis.ui.theme.StarkPanelBg
import com.stark.jarvis.ui.theme.StarkRose
import com.stark.jarvis.ui.theme.StarkTextMuted
import com.stark.jarvis.ui.theme.StarkTextPrimary

data class HudChatMessage(
    val id: String,
    val isUser: Boolean,
    val text: String,
    val timestamp: String,
    val proposedAction: ProposedDeviceAction? = null
)

@Composable
fun JarvisHudScreen(
    messages: List<HudChatMessage>,
    isThinking: Boolean,
    isListening: Boolean,
    isSpeaking: Boolean,
    liveTranscript: String,
    autoVoice: Boolean,
    wakeWordEnabled: Boolean,
    isTorchOn: Boolean,
    selectedLanguage: String,
    backendUrl: String,
    permissionStatuses: List<JarvisPermissionStatus>,
    pendingConfirmationAction: ProposedDeviceAction?,
    pickedContact: PickedContact?,
    onSendMessage: (String) -> Unit,
    onToggleListening: () -> Unit,
    onToggleAutoVoice: () -> Unit,
    onToggleWakeWord: () -> Unit,
    onToggleFlashlight: () -> Unit,
    onSelectLanguage: (String) -> Unit,
    onStopSpeaking: () -> Unit,
    onReplayAudio: (String) -> Unit,
    onPickContact: () -> Unit,
    onClearPickedContact: () -> Unit,
    onConfirmAction: (ProposedDeviceAction) -> Unit,
    onDismissConfirmation: () -> Unit,
    onRequestPermissions: () -> Unit,
    onOpenAppSettings: () -> Unit,
    onUpdateBackendUrl: (String) -> Unit
) {
    var inputText by remember { mutableStateOf("") }
    var showSettingsDialog by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()

    LaunchedEffect(messages.size, isThinking) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(StarkNavyBg)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
    ) {
        // Top HUD Header
        Surface(
            color = StarkHeaderBg,
            border = BorderStroke(1.dp, StarkCyan.copy(alpha = 0.25f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "J.A.R.V.I.S. 2.0",
                        color = StarkCyanGlow,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 16.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "VIVO T2 PRO · ANDROID 15",
                        color = StarkTextMuted.copy(alpha = 0.7f),
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    // Auto Voice Toggle
                    Surface(
                        onClick = onToggleAutoVoice,
                        shape = RoundedCornerShape(8.dp),
                        color = if (autoVoice) StarkCyan.copy(alpha = 0.16f) else Color(0xFF0F172A),
                        border = BorderStroke(1.dp, if (autoVoice) StarkCyan else Color(0xFF334155))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (autoVoice) Icons.Default.VolumeUp else Icons.Default.VolumeOff,
                                contentDescription = "Voice Toggle",
                                tint = if (autoVoice) StarkEmerald else Color.Gray,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (autoVoice) "ON" else "OFF",
                                color = if (autoVoice) StarkEmerald else Color.Gray,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    // Wake Word Toggle
                    Surface(
                        onClick = onToggleWakeWord,
                        shape = RoundedCornerShape(8.dp),
                        color = if (wakeWordEnabled) StarkEmerald.copy(alpha = 0.16f) else Color(0xFF0F172A),
                        border = BorderStroke(1.dp, if (wakeWordEnabled) StarkEmerald else StarkCyan.copy(alpha = 0.3f))
                    ) {
                        Text(
                            text = if (wakeWordEnabled) "HEY JARVIS: ON" else "WAKE",
                            color = if (wakeWordEnabled) StarkEmerald else StarkTextMuted,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                        )
                    }

                    // Permissions & Backend Config Button
                    IconButton(
                        onClick = { showSettingsDialog = true },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Permissions and Settings",
                            tint = StarkCyanGlow
                        )
                    }
                }
            }
        }

        // Arc Reactor Visual Core
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp, bottom = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            ArcReactorCore(
                isListening = isListening,
                isSpeaking = isSpeaking,
                isThinking = isThinking,
                onClick = {
                    if (isSpeaking) onStopSpeaking() else onToggleListening()
                }
            )
        }

        // Native Hardware & Device Quick Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Language selector cycle
            val langLabel = when (selectedLanguage) {
                "hi" -> "Lang: हिन्दी"
                "hinglish" -> "Lang: Hinglish"
                "en" -> "Lang: English"
                else -> "Lang: Auto"
            }
            QuickHardwareButton(
                label = langLabel,
                active = false,
                onClick = {
                    val next = when (selectedLanguage) {
                        "auto" -> "hi"
                        "hi" -> "hinglish"
                        "hinglish" -> "en"
                        else -> "auto"
                    }
                    onSelectLanguage(next)
                }
            )

            // Real Camera2 Torch Toggle
            QuickHardwareButton(
                label = if (isTorchOn) "Torch: ON" else "Torch: OFF",
                active = isTorchOn,
                icon = {
                    Icon(
                        imageVector = if (isTorchOn) Icons.Default.FlashlightOn else Icons.Default.FlashlightOff,
                        contentDescription = "Torch",
                        tint = if (isTorchOn) StarkAmber else StarkCyanGlow,
                        modifier = Modifier.size(14.dp)
                    )
                },
                onClick = onToggleFlashlight
            )

            // Contact Picker
            QuickHardwareButton(
                label = "Pick Contact",
                active = false,
                icon = {
                    Icon(
                        imageVector = Icons.Default.Contacts,
                        contentDescription = "Contacts",
                        tint = StarkCyanGlow,
                        modifier = Modifier.size(14.dp)
                    )
                },
                onClick = onPickContact
            )

            // Quick YouTube / Maps / Alarm prompts
            QuickHardwareButton(label = "Open YouTube", active = false, onClick = { onSendMessage("YouTube kholo") })
            QuickHardwareButton(label = "Open Maps", active = false, onClick = { onSendMessage("Maps open karo") })
            QuickHardwareButton(label = "7 AM Alarm", active = false, onClick = { onSendMessage("Subah 7 baje ka alarm lagao") })
        }

        // Chat Stream
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(messages, key = { it.id }) { msg ->
                ChatBubbleItem(
                    message = msg,
                    isSpeaking = isSpeaking,
                    onReplay = { onReplayAudio(msg.text) },
                    onStop = onStopSpeaking,
                    onTriggerAction = { action -> onConfirmAction(action) }
                )
            }

            if (isThinking) {
                item {
                    Text(
                        text = "J.A.R.V.I.S. processing neural request...",
                        color = StarkAmber,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }
        }

        // Live Voice Transcript Banner
        if (isListening || liveTranscript.isNotBlank()) {
            Surface(
                color = StarkPanelBg,
                border = BorderStroke(1.dp, StarkCyan),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp)
            ) {
                Text(
                    text = liveTranscript.ifBlank { "Listening in English / हिन्दी / Hinglish..." },
                    color = StarkCyanGlow,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(10.dp)
                )
            }
        }

        // Bottom Input Bar
        Surface(
            color = StarkHeaderBg,
            border = BorderStroke(1.dp, StarkCyan.copy(alpha = 0.25f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    onClick = onToggleListening,
                    shape = RoundedCornerShape(12.dp),
                    color = if (isListening) StarkRose.copy(alpha = 0.25f) else StarkCyan.copy(alpha = 0.2f),
                    border = BorderStroke(1.dp, if (isListening) StarkRose else StarkCyan),
                    modifier = Modifier.size(48.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (isListening) Icons.Default.MicOff else Icons.Default.Mic,
                            contentDescription = "Microphone",
                            tint = if (isListening) StarkRose else StarkCyanGlow
                        )
                    }
                }

                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    placeholder = {
                        Text(
                            "Speak or type in Hindi, English, Hinglish...",
                            color = StarkTextMuted.copy(alpha = 0.5f),
                            fontSize = 13.sp
                        )
                    },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = StarkCyan,
                        unfocusedBorderColor = StarkCyan.copy(alpha = 0.35f),
                        focusedTextColor = StarkTextPrimary,
                        unfocusedTextColor = StarkTextPrimary,
                        focusedContainerColor = StarkPanelBg,
                        unfocusedContainerColor = StarkPanelBg
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                )

                Surface(
                    onClick = {
                        val trimmed = inputText.trim()
                        if (trimmed.isNotEmpty()) {
                            onSendMessage(trimmed)
                            inputText = ""
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    color = StarkCyan,
                    modifier = Modifier.size(48.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Send,
                            contentDescription = "Send",
                            tint = Color.Black
                        )
                    }
                }
            }
        }
    }

    // Contact Picked Action Modal (Call or WhatsApp)
    if (pickedContact != null) {
        AlertDialog(
            onDismissRequest = onClearPickedContact,
            containerColor = StarkPanelBg,
            title = { Text(pickedContact.name, color = StarkCyanGlow, fontFamily = FontFamily.Monospace) },
            text = {
                Text(
                    "Phone: ${pickedContact.phoneNumber}\nChoose an action to execute:",
                    color = StarkTextPrimary
                )
            },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            onConfirmAction(
                                ProposedDeviceAction(
                                    actionType = "CALL_PHONE",
                                    actionTitle = "Call ${pickedContact.name}",
                                    phoneNumber = pickedContact.phoneNumber
                                )
                            )
                            onClearPickedContact()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = StarkCyan, contentColor = Color.Black)
                    ) {
                        Icon(Icons.Default.Call, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Dial")
                    }
                    Button(
                        onClick = {
                            onConfirmAction(
                                ProposedDeviceAction(
                                    actionType = "SEND_WHATSAPP",
                                    actionTitle = "WhatsApp ${pickedContact.name}",
                                    phoneNumber = pickedContact.phoneNumber,
                                    messageBody = "Hello ${pickedContact.name}"
                                )
                            )
                            onClearPickedContact()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = StarkEmerald, contentColor = Color.Black)
                    ) {
                        Text("WhatsApp")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = onClearPickedContact) {
                    Text("Cancel", color = StarkTextMuted)
                }
            }
        )
    }

    // Sensitive Action Confirmation Modal
    if (pendingConfirmationAction != null) {
        AlertDialog(
            onDismissRequest = onDismissConfirmation,
            containerColor = StarkPanelBg,
            title = {
                Text(
                    "Confirm Action",
                    color = StarkCyanGlow,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(pendingConfirmationAction.actionTitle, color = StarkTextPrimary, fontWeight = FontWeight.SemiBold)
                    pendingConfirmationAction.explanation?.let {
                        Text(it, color = StarkTextMuted, fontSize = 13.sp)
                    }
                    pendingConfirmationAction.phoneNumber?.let {
                        Text("Target Number: $it", color = StarkAmber, fontSize = 12.sp)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { onConfirmAction(pendingConfirmationAction) },
                    colors = ButtonDefaults.buttonColors(containerColor = StarkCyan, contentColor = Color.Black)
                ) {
                    Text("Approve & Execute")
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissConfirmation) {
                    Text("Decline", color = StarkRose)
                }
            }
        )
    }

    // Permissions & Backend Setup Dialog
    if (showSettingsDialog) {
        var editableUrl by remember(backendUrl) { mutableStateOf(backendUrl) }
        AlertDialog(
            onDismissRequest = { showSettingsDialog = false },
            containerColor = StarkPanelBg,
            title = {
                Text(
                    "Android 15 Permissions & Server",
                    color = StarkCyanGlow,
                    fontSize = 16.sp,
                    fontFamily = FontFamily.Monospace
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Gemini Backend URL (No API key in APK):", color = StarkTextMuted, fontSize = 11.sp)
                    OutlinedTextField(
                        value = editableUrl,
                        onValueChange = { editableUrl = it },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = StarkCyan,
                            unfocusedBorderColor = StarkCyan.copy(alpha = 0.4f),
                            focusedTextColor = StarkTextPrimary,
                            unfocusedTextColor = StarkTextPrimary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Runtime Permissions (Vivo T2 Pro):", color = StarkTextMuted, fontSize = 11.sp)
                    permissionStatuses.forEach { perm ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(perm.title, color = StarkTextPrimary, fontSize = 12.sp)
                            Text(
                                text = if (perm.isGranted) "GRANTED" else "NEEDED",
                                color = if (perm.isGranted) StarkEmerald else StarkAmber,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 6.dp)) {
                        OutlinedButton(onClick = onRequestPermissions) {
                            Text("Grant All", fontSize = 11.sp, color = StarkCyanGlow)
                        }
                        OutlinedButton(onClick = onOpenAppSettings) {
                            Text("App Settings", fontSize = 11.sp, color = StarkTextMuted)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onUpdateBackendUrl(editableUrl)
                        showSettingsDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StarkCyan, contentColor = Color.Black)
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSettingsDialog = false }) {
                    Text("Close", color = StarkTextMuted)
                }
            }
        )
    }
}

@Composable
private fun ArcReactorCore(
    isListening: Boolean,
    isSpeaking: Boolean,
    isThinking: Boolean,
    onClick: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "arc_reactor")
    val outerAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(14000, easing = LinearEasing)),
        label = "outer_spin"
    )
    val innerAngle by infiniteTransition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(tween(9000, easing = LinearEasing)),
        label = "inner_spin"
    )
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.7f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(tween(1200), RepeatMode.Reverse),
        label = "pulse"
    )

    val ringColor = when {
        isThinking -> StarkAmber
        isSpeaking -> StarkEmerald
        isListening -> StarkRose
        else -> StarkCyan
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(124.dp)
                .clip(CircleShape)
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val center = Offset(size.width / 2f, size.height / 2f)
                val radius = size.minDimension / 2f

                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(ringColor.copy(alpha = 0.25f * pulse), Color.Transparent),
                        center = center,
                        radius = radius
                    )
                )

                rotate(outerAngle, pivot = center) {
                    drawCircle(
                        color = ringColor.copy(alpha = 0.8f),
                        radius = radius - 6.dp.toPx(),
                        style = Stroke(
                            width = 2.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(18f, 12f), 0f)
                        )
                    )
                }

                rotate(innerAngle, pivot = center) {
                    drawArc(
                        color = StarkCyanGlow,
                        startAngle = 0f,
                        sweepAngle = 100f,
                        useCenter = false,
                        topLeft = Offset(18.dp.toPx(), 18.dp.toPx()),
                        size = Size(size.width - 36.dp.toPx(), size.height - 36.dp.toPx()),
                        style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                    )
                    drawArc(
                        color = StarkCyanGlow,
                        startAngle = 180f,
                        sweepAngle = 100f,
                        useCenter = false,
                        topLeft = Offset(18.dp.toPx(), 18.dp.toPx()),
                        size = Size(size.width - 36.dp.toPx(), size.height - 36.dp.toPx()),
                        style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                    )
                }

                drawCircle(
                    color = ringColor.copy(alpha = 0.9f),
                    radius = 28.dp.toPx(),
                    style = Stroke(width = 2.dp.toPx())
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "JARVIS",
                    color = StarkCyanGlow,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = when {
                        isThinking -> "THINKING"
                        isSpeaking -> "SPEAKING"
                        isListening -> "LISTENING"
                        else -> "TAP MIC"
                    },
                    color = ringColor,
                    fontSize = 8.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

@Composable
private fun QuickHardwareButton(
    label: String,
    active: Boolean,
    icon: (@Composable () -> Unit)? = null,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = if (active) StarkAmber.copy(alpha = 0.2f) else StarkPanelBg,
        border = BorderStroke(1.dp, if (active) StarkAmber else StarkCyan.copy(alpha = 0.35f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            icon?.invoke()
            Text(
                text = label,
                color = if (active) StarkAmber else StarkTextPrimary,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

@Composable
private fun ChatBubbleItem(
    message: HudChatMessage,
    isSpeaking: Boolean,
    onReplay: () -> Unit,
    onStop: () -> Unit,
    onTriggerAction: (ProposedDeviceAction) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (message.isUser) Alignment.End else Alignment.Start
    ) {
        Text(
            text = if (message.isUser) "YOU · ${message.timestamp}" else "J.A.R.V.I.S. · ${message.timestamp}",
            color = StarkTextMuted.copy(alpha = 0.6f),
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace
        )
        Spacer(modifier = Modifier.height(3.dp))
        Surface(
            color = if (message.isUser) Color(0xFF0C2D48) else StarkPanelBg,
            border = BorderStroke(1.dp, StarkCyan.copy(alpha = if (message.isUser) 0.45f else 0.3f)),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth(0.92f)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = message.text,
                    color = StarkTextPrimary,
                    fontSize = 14.sp
                )

                message.proposedAction?.let { action ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = { onTriggerAction(action) },
                        colors = ButtonDefaults.buttonColors(containerColor = StarkCyan, contentColor = Color.Black),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(action.actionTitle, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                if (!message.isUser) {
                    Row(
                        modifier = Modifier.padding(top = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TextButton(onClick = onReplay) {
                            Icon(Icons.Default.VolumeUp, contentDescription = null, tint = StarkCyanGlow, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Replay", color = StarkCyanGlow, fontSize = 11.sp)
                        }
                        if (isSpeaking) {
                            TextButton(onClick = onStop) {
                                Icon(Icons.Default.Stop, contentDescription = null, tint = StarkRose, modifier = Modifier.size(14.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Stop", color = StarkRose, fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}
