package com.jarvis.assistant

import android.content.Context
import android.content.Intent
import android.hardware.camera2.CameraManager
import android.provider.Settings
import android.provider.AlarmClock
import android.media.AudioManager
import android.content.Intent.ACTION_SEARCH
import android.app.SearchManager
import android.net.Uri
import android.os.BatteryManager
import android.content.Context.BATTERY_SERVICE

class PhoneController(private val context: Context) {

    // FLASHLIGHT ON
    fun torchOn(): String {
        return try {
            val manager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
            val id = manager.cameraIdList.firstOrNull()
                ?: return "Flashlight unavailable."
            manager.setTorchMode(id, true)
            "Flashlight turned ON."
        } catch (e: Exception) {
            "Unable to turn flashlight on."
        }
    }

    // FLASHLIGHT OFF
    fun torchOff(): String {
        return try {
            val manager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
            val id = manager.cameraIdList.firstOrNull()
                ?: return "Flashlight unavailable."
            manager.setTorchMode(id, false)
            "Flashlight turned OFF."
        } catch (e: Exception) {
            "Unable to turn flashlight off."
        }
    }

    // BATTERY STATUS
    fun getBattery(): String {
        val battery = context.getSystemService(BATTERY_SERVICE) as BatteryManager
        val level = battery.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        return "Your phone battery is $level percent."
    }

    // VOLUME UP
    fun volumeUp(): String {
        val audio = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        audio.adjustStreamVolume(
            AudioManager.STREAM_MUSIC,
            AudioManager.ADJUST_RAISE,
            AudioManager.FLAG_SHOW_UI
        )
        return "Volume increased."
    }

    // VOLUME DOWN
    fun volumeDown(): String {
        val audio = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        audio.adjustStreamVolume(
            AudioManager.STREAM_MUSIC,
            AudioManager.ADJUST_LOWER,
            AudioManager.FLAG_SHOW_UI
        )
        return "Volume decreased."
    }

    // MUTE
    fun muteVolume(): String {
        val audio = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        audio.adjustStreamVolume(
            AudioManager.STREAM_MUSIC,
            AudioManager.ADJUST_MUTE,
            AudioManager.FLAG_SHOW_UI
        )
        return "Volume muted."
    }

    // GOOGLE SEARCH
    fun googleSearch(query: String): String {
        return try {
            val intent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse("https://www.google.com/search?q=${Uri.encode(query)}")
            )
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            "Searching Google for $query."
        } catch (e: Exception) {
            "Unable to search Google."
        }
    }

    // WIFI SETTINGS
    fun openWifiSettings(): String {
        return openSystemSettings(Settings.ACTION_WIFI_SETTINGS, "Wi-Fi settings")
    }

    // BLUETOOTH SETTINGS
    fun openBluetoothSettings(): String {
        return openSystemSettings(Settings.ACTION_BLUETOOTH_SETTINGS, "Bluetooth settings")
    }

    private fun openSystemSettings(action: String, label: String): String {
        return try {
            val intent = Intent(action)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            "Opening $label."
        } catch (e: Exception) {
            "Unable to open $label."
        }
    }

    // GENERAL SETTINGS
    fun openSettings(): String {
        return openSystemSettings(Settings.ACTION_SETTINGS, "phone settings")
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
            "Opening alarm."
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
            val intent = context.packageManager.getLaunchIntentForPackage(packageName)
                ?: return "App is not installed."
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            "Opening app."
        } catch (e: Exception) {
            "Unable to open app."
        }
    }
}
