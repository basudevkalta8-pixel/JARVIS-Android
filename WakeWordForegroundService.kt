package com.stark.jarvis.voice

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.core.app.NotificationCompat
import com.stark.jarvis.MainActivity
import com.stark.jarvis.permissions.PermissionManager

/**
 * Android 15 Foreground Microphone Service for "Hey JARVIS" Wake-Word Detection.
 *
 * Honest Android 15 / Vivo FuntouchOS 15 Architecture:
 * - Android 14/15 prohibits silent background microphone access from closed apps.
 * - Wake-word detection runs ONLY while this visible Foreground Service notification is active
 *   (and RECORD_AUDIO permission is granted).
 * - When "Hey JARVIS" is heard, it brings MainActivity forward or broadcasts the spoken command.
 */
class WakeWordForegroundService : Service() {

    companion object {
        const val CHANNEL_ID = "jarvis_wakeword_channel"
        const val NOTIFICATION_ID = 2001
        const val ACTION_WAKE_WORD_DETECTED = "com.stark.jarvis.WAKE_WORD_DETECTED"
        const val EXTRA_COMMAND_TEXT = "command_text"

        fun start(context: Context) {
            val intent = Intent(context, WakeWordForegroundService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, WakeWordForegroundService::class.java))
        }
    }

    private var recognizer: SpeechRecognizer? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    private var isServiceRunning = false

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!PermissionManager.isGranted(this, Manifest.permission.RECORD_AUDIO)) {
            stopSelf()
            return START_NOT_STICKY
        }

        val notification = buildNotification("Listening for \"Hey JARVIS\" (Active Service)")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        isServiceRunning = true
        mainHandler.post { startListeningLoop() }
        return START_STICKY
    }

    private fun startListeningLoop() {
        if (!isServiceRunning || !SpeechRecognizer.isRecognitionAvailable(this)) return

        recognizer?.destroy()
        recognizer = SpeechRecognizer.createSpeechRecognizer(this).apply {
            setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {}
                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() {}

                override fun onError(error: Int) {
                    if (isServiceRunning) {
                        mainHandler.postDelayed({ startListeningLoop() }, 1200)
                    }
                }

                override fun onResults(results: Bundle?) {
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val spoken = matches?.firstOrNull()?.lowercase().orEmpty()
                    handleTranscript(spoken)

                    if (isServiceRunning) {
                        mainHandler.postDelayed({ startListeningLoop() }, 600)
                    }
                }

                override fun onPartialResults(partialResults: Bundle?) {
                    val partial = partialResults
                        ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        ?.firstOrNull()
                        ?.lowercase()
                        .orEmpty()
                    if (containsWakeWord(partial)) {
                        handleTranscript(partial)
                    }
                }

                override fun onEvent(eventType: Int, params: Bundle?) {}
            })
        }

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-IN")
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        }
        try {
            recognizer?.startListening(intent)
        } catch (_: Exception) {
            if (isServiceRunning) {
                mainHandler.postDelayed({ startListeningLoop() }, 2000)
            }
        }
    }

    private fun containsWakeWord(text: String): Boolean {
        return text.contains("hey jarvis") ||
            text.contains("ok jarvis") ||
            text.contains("hello jarvis") ||
            text.contains("सुनो जार्विस")
    }

    private fun handleTranscript(transcript: String) {
        if (!containsWakeWord(transcript)) return

        val cleanedCommand = transcript
            .replace("hey jarvis", "")
            .replace("ok jarvis", "")
            .replace("hello jarvis", "")
            .replace("सुनो जार्विस", "")
            .trim()

        val broadcast = Intent(ACTION_WAKE_WORD_DETECTED).apply {
            setPackage(packageName)
            putExtra(EXTRA_COMMAND_TEXT, cleanedCommand)
        }
        sendBroadcast(broadcast)

        val launchIntent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            putExtra(EXTRA_COMMAND_TEXT, cleanedCommand)
        }
        startActivity(launchIntent)
    }

    override fun onDestroy() {
        isServiceRunning = false
        mainHandler.removeCallbacksAndMessages(null)
        recognizer?.destroy()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "J.A.R.V.I.S. Wake-Word Listener",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Active microphone listener for 'Hey JARVIS'"
            }
            val nm = getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(statusText: String): Notification {
        val openAppIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("J.A.R.V.I.S. 2.0 Active")
            .setContentText(statusText)
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentIntent(openAppIntent)
            .setOngoing(true)
            .build()
    }
}
