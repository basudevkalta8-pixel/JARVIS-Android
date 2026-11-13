package com.stark.jarvis.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class ChatTurn(
    val role: String, // "user" or "model"
    val text: String
)

data class ProposedDeviceAction(
    val actionType: String,
    val appName: String? = null,
    val actionTitle: String,
    val targetUrl: String? = null,
    val androidIntent: String? = null,
    val phoneNumber: String? = null,
    val messageBody: String? = null,
    val alarmHour: Int? = null,
    val alarmMinute: Int? = null,
    val timerSeconds: Int? = null,
    val flashlightState: Boolean? = null,
    val packageNameQuery: String? = null,
    val explanation: String? = null
)

data class JarvisBackendResponse(
    val reply: String,
    val language: String,
    val action: ProposedDeviceAction?
)

class JarvisApiClient(
    private val baseUrlProvider: () -> String
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    suspend fun sendMessage(
        message: String,
        history: List<ChatTurn>,
        language: String = "auto"
    ): Result<JarvisBackendResponse> = withContext(Dispatchers.IO) {
        try {
            val baseUrl = baseUrlProvider().trimEnd('/')
            val historyArray = JSONArray()
            history.takeLast(10).forEach { turn ->
                historyArray.put(
                    JSONObject().apply {
                        put("role", turn.role)
                        put("text", turn.text)
                    }
                )
            }

            val payload = JSONObject().apply {
                put("message", message)
                put("history", historyArray)
                put("language", language)
                put("clientPlatform", "android-native-kotlin")
            }

            val request = Request.Builder()
                .url("$baseUrl/api/chat")
                .post(payload.toString().toRequestBody(jsonMediaType))
                .addHeader("Accept", "application/json")
                .build()

            client.newCall(request).execute().use { response ->
                val bodyString = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    return@withContext Result.failure(
                        Exception("Backend HTTP ${response.code}: $bodyString")
                    )
                }

                val json = JSONObject(bodyString)
                val reply = json.optString("reply", "All systems nominal, Sir.")
                val detectedLang = json.optString("language", "en")
                val actionObj = json.optJSONObject("action")

                val parsedAction = actionObj?.let { act ->
                    ProposedDeviceAction(
                        actionType = act.optString("actionType", ""),
                        appName = act.optString("appName").takeIf { it.isNotEmpty() },
                        actionTitle = act.optString("actionTitle", "Proposed Action"),
                        targetUrl = act.optString("targetUrl").takeIf { it.isNotEmpty() },
                        androidIntent = act.optString("androidIntent").takeIf { it.isNotEmpty() },
                        phoneNumber = act.optString("phoneNumber").takeIf { it.isNotEmpty() },
                        messageBody = act.optString("messageBody").takeIf { it.isNotEmpty() },
                        alarmHour = if (act.has("alarmHour") && !act.isNull("alarmHour")) act.getInt("alarmHour") else null,
                        alarmMinute = if (act.has("alarmMinute") && !act.isNull("alarmMinute")) act.getInt("alarmMinute") else null,
                        timerSeconds = if (act.has("timerSeconds") && !act.isNull("timerSeconds")) act.getInt("timerSeconds") else null,
                        flashlightState = if (act.has("flashlightState") && !act.isNull("flashlightState")) act.getBoolean("flashlightState") else null,
                        packageNameQuery = act.optString("packageNameQuery").takeIf { it.isNotEmpty() },
                        explanation = act.optString("explanation").takeIf { it.isNotEmpty() }
                    )
                }

                Result.success(
                    JarvisBackendResponse(
                        reply = reply,
                        language = detectedLang,
                        action = parsedAction
                    )
                )
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun checkHealth(): Boolean = withContext(Dispatchers.IO) {
        try {
            val baseUrl = baseUrlProvider().trimEnd('/')
            val request = Request.Builder()
                .url("$baseUrl/api/system-status")
                .get()
                .build()
            client.newCall(request).execute().use { it.isSuccessful }
        } catch (_: Exception) {
            false
        }
    }
}
