package com.jarvis.assistant

import android.content.Context
import android.content.Intent
import android.hardware.camera2.CameraManager
import android.provider.Settings
import android.provider.AlarmClock

class PhoneController(private val context: Context) {

    // FLASHLIGHT ON
    fun torchOn(): String {
        return try {
            val cameraManager =
                context.getSystemService(Context.CAMERA_SERVICE) as CameraManager

            val cameraId = cameraManager.cameraIdList.firstOrNull()

            if (cameraId != null) {
                cameraManager.setTorchMode(cameraId, true)
                "Flashlight turned ON."
            } else {
                "Flashlight is not available."
            }
        } catch (e: Exception) {
            "Unable to turn flashlight on."
        }
    }

    // FLASHLIGHT OFF
    fun torchOff(): String {
        return try {
            val cameraManager =
                context.getSystemService(Context.CAMERA_SERVICE) as CameraManager

            val cameraId = cameraManager.cameraIdList.firstOrNull()

            if (cameraId != null) {
                cameraManager.setTorchMode(cameraId, false)
                "Flashlight turned OFF."
            } else {
                "Flashlight is not available."
            }
        } catch (e: Exception) {
            "Unable to turn flashlight off."
        }
    }

    // OPEN SETTINGS
    fun openSettings(): String {
        return try {
            val intent = Intent(Settings.ACTION_SETTINGS)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            "Opening phone settings."
        } catch (e: Exception) {
            "Unable to open settings."
        }
    }

    // SET ALARM
    fun setAlarm(hour: Int, minute: Int): String {
        return try {
            val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
                putExtra(AlarmClock.EXTRA_HOUR, hour)
                putExtra(AlarmClock.EXTRA_MINUTES, minute)
                putExtra(AlarmClock.EXTRA_MESSAGE, "JARVIS Alarm")
                putExtra(AlarmClock.EXTRA_SKIP_UI, false)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            context.startActivity(intent)
            "Opening alarm to set ${hour}:${minute.toString().padStart(2, '0')}."
        } catch (e: Exception) {
            "Unable to set alarm."
        }
    }

    // SET TIMER
    fun setTimer(seconds: Int, label: String): String {
        return try {
            val intent = Intent(AlarmClock.ACTION_SET_TIMER).apply {
                putExtra(AlarmClock.EXTRA_LENGTH, seconds)
                putExtra(AlarmClock.EXTRA_MESSAGE, label)
                putExtra(AlarmClock.EXTRA_SKIP_UI, false)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            context.startActivity(intent)
            "Starting timer for $seconds seconds."
        } catch (e: Exception) {
            "Unable to start timer."
        }
    }

    // OPEN APP
    fun openApp(packageName: String): String {
        return try {
            val intent = context.packageManager
                .getLaunchIntentForPackage(packageName)

            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                "Opening app."
            } else {
                "App is not installed."
            }
        } catch (e: Exception) {
            "Unable to open app."
        }
    }
}
