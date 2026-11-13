package com.stark.jarvis.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.Locale
import java.util.UUID

class JarvisVoiceEngine(private val context: Context) {

    private var tts: TextToSpeech? = null
    private var isTtsReady = false
    private var speechRecognizer: SpeechRecognizer? = null

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening

    private val _liveTranscript = MutableStateFlow("")
    val liveTranscript: StateFlow<String> = _liveTranscript

    init {
        initTextToSpeech()
    }

    private fun initTextToSpeech() {
        tts = TextToSpeech(context.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                isTtsReady = true
                tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        _isSpeaking.value = true
                    }

                    override fun onDone(utteranceId: String?) {
                        _isSpeaking.value = false
                    }

                    @Deprecated("Deprecated in Java")
                    override fun onError(utteranceId: String?) {
                        _isSpeaking.value = false
                    }
                })
            }
        }
    }

    fun speak(text: String, languageCode: String = "auto") {
        if (!isTtsReady || text.isBlank()) return

        val cleaned = cleanForSpeech(text)
        if (cleaned.isEmpty()) return

        val targetLocale = when {
            languageCode == "hi" || Regex("[\u0900-\u097F]").containsMatchIn(cleaned) -> Locale("hi", "IN")
            languageCode == "hinglish" || isHinglish(cleaned) -> Locale("en", "IN")
            else -> Locale.UK
        }

        val availability = tts?.isLanguageAvailable(targetLocale) ?: TextToSpeech.LANG_NOT_SUPPORTED
        if (availability >= TextToSpeech.LANG_AVAILABLE) {
            tts?.language = targetLocale
        } else {
            tts?.language = Locale("en", "IN")
        }

        tts?.setPitch(0.96f)
        tts?.setSpeechRate(1.0f)

        val utteranceId = UUID.randomUUID().toString()
        tts?.speak(cleaned, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
    }

    fun stopSpeaking() {
        tts?.stop()
        _isSpeaking.value = false
    }

    fun startListening(
        languageMode: String = "auto",
        onFinalResult: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        stopSpeaking()

        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            onError("Speech recognition service unavailable on this device.")
            return
        }

        speechRecognizer?.destroy()
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
            setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    _isListening.value = true
                    _liveTranscript.value = ""
                }

                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}

                override fun onEndOfSpeech() {
                    _isListening.value = false
                }

                override fun onError(error: Int) {
                    _isListening.value = false
                    val msg = when (error) {
                        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission required."
                        SpeechRecognizer.ERROR_NO_MATCH -> "Did not catch that, Sir. Please try again."
                        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech detected."
                        SpeechRecognizer.ERROR_NETWORK -> "Network required for speech recognition."
                        else -> "Voice sensor error ($error)."
                    }
                    onError(msg)
                }

                override fun onResults(results: Bundle?) {
                    _isListening.value = false
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val text = matches?.firstOrNull()?.trim().orEmpty()
                    _liveTranscript.value = ""
                    if (text.isNotEmpty()) {
                        onFinalResult(text)
                    }
                }

                override fun onPartialResults(partialResults: Bundle?) {
                    val partial = partialResults
                        ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        ?.firstOrNull()
                        .orEmpty()
                    if (partial.isNotEmpty()) {
                        _liveTranscript.value = partial
                    }
                }

                override fun onEvent(eventType: Int, params: Bundle?) {}
            })
        }

        val langTag = when (languageMode) {
            "hi" -> "hi-IN"
            "en" -> "en-US"
            else -> "en-IN" // Best for English + Hinglish mixed commands on Vivo / Android
        }

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, langTag)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        }

        speechRecognizer?.startListening(intent)
    }

    fun stopListening() {
        speechRecognizer?.stopListening()
        _isListening.value = false
    }

    fun release() {
        speechRecognizer?.destroy()
        tts?.shutdown()
    }

    private fun isHinglish(text: String): Boolean {
        val pattern = Regex(
            "\\b(haan|nahi|shukriya|namaste|kya|kaise|karo|kholo|lagao|hoon|hai|hain|mera|aap|bilkul|chalo|sunao)\\b",
            RegexOption.IGNORE_CASE
        )
        return pattern.containsMatchIn(text)
    }

    private fun cleanForSpeech(text: String): String {
        return text
            .replace(Regex("```[\\s\\S]*?```"), "Code block omitted.")
            .replace(Regex("[*_`#]"), "")
            .replace(Regex("https?://\\S+"), "link")
            .trim()
    }
}
