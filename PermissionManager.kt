package com.stark.jarvis.permissions

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.ContextCompat

data class JarvisPermissionStatus(
    val permission: String,
    val title: String,
    val purpose: String,
    val isGranted: Boolean
)

object PermissionManager {

    fun requiredPermissions(): List<String> {
        val perms = mutableListOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.CAMERA,
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.CALL_PHONE
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            perms.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        return perms
    }

    fun isGranted(context: Context, permission: String): Boolean {
        return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
    }

    fun getAllStatuses(context: Context): List<JarvisPermissionStatus> {
        val items = mutableListOf(
            JarvisPermissionStatus(
                permission = Manifest.permission.RECORD_AUDIO,
                title = "Microphone",
                purpose = "Voice commands & 'Hey JARVIS' wake-word detection",
                isGranted = isGranted(context, Manifest.permission.RECORD_AUDIO)
            ),
            JarvisPermissionStatus(
                permission = Manifest.permission.CAMERA,
                title = "Camera (Torch Hardware)",
                purpose = "Controls Vivo T2 Pro LED flashlight via Camera2 API",
                isGranted = isGranted(context, Manifest.permission.CAMERA)
            ),
            JarvisPermissionStatus(
                permission = Manifest.permission.READ_CONTACTS,
                title = "Contacts",
                purpose = "Look up phone numbers when calling or messaging contacts",
                isGranted = isGranted(context, Manifest.permission.READ_CONTACTS)
            ),
            JarvisPermissionStatus(
                permission = Manifest.permission.CALL_PHONE,
                title = "Phone Calls",
                purpose = "Place direct calls after your confirmation",
                isGranted = isGranted(context, Manifest.permission.CALL_PHONE)
            )
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            items.add(
                JarvisPermissionStatus(
                    permission = Manifest.permission.POST_NOTIFICATIONS,
                    title = "Notifications (Android 15)",
                    purpose = "Required for active Foreground Wake-Word indicator",
                    isGranted = isGranted(context, Manifest.permission.POST_NOTIFICATIONS)
                )
            )
        }
        return items
    }

    fun openAppSettings(context: Context) {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
}
