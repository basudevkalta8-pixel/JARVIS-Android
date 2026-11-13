package com.stark.jarvis.actions

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.net.Uri
import android.provider.AlarmClock
import android.provider.ContactsContract
import com.stark.jarvis.permissions.PermissionManager

data class ActionExecutionResult(
    val success: Boolean,
    val message: String
)

data class PickedContact(
    val name: String,
    val phoneNumber: String
)

class DeviceActionExecutor(private val context: Context) {

    var isTorchOn: Boolean = false
        private set

    // 1. Control Flashlight using Android Camera2 APIs (No fake state)
    fun setFlashlight(enable: Boolean): ActionExecutionResult {
        return try {
            val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
            var torchCameraId: String? = null

            for (id in cameraManager.cameraIdList) {
                val characteristics = cameraManager.getCameraCharacteristics(id)
                val hasFlash = characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
                val lensFacing = characteristics.get(CameraCharacteristics.LENS_FACING)
                if (hasFlash && lensFacing == CameraCharacteristics.LENS_FACING_BACK) {
                    torchCameraId = id
                    break
                }
            }

            if (torchCameraId == null) {
                return ActionExecutionResult(false, "No rear flash hardware found on this device.")
            }

            cameraManager.setTorchMode(torchCameraId, enable)
            isTorchOn = enable
            ActionExecutionResult(
                true,
                if (enable) "Flashlight turned ON, Sir." else "Flashlight turned OFF, Sir."
            )
        } catch (e: SecurityException) {
            ActionExecutionResult(false, "Camera/Flash permission denied: ${e.message}")
        } catch (e: Exception) {
            ActionExecutionResult(false, "Flashlight error: ${e.message}")
        }
    }

    // 2. Create Alarm using Android AlarmClock Intent (Android 15 supported)
    fun createAlarm(hour: Int, minute: Int, label: String = "J.A.R.V.I.S. Alarm"): ActionExecutionResult {
        return try {
            val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
                putExtra(AlarmClock.EXTRA_HOUR, hour.coerceIn(0, 23))
                putExtra(AlarmClock.EXTRA_MINUTES, minute.coerceIn(0, 59))
                putExtra(AlarmClock.EXTRA_MESSAGE, label)
                putExtra(AlarmClock.EXTRA_SKIP_UI, false) // Opens Clock app for transparent user verification
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            if (intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(intent)
                val formatted = String.format("%02d:%02d", hour.coerceIn(0, 23), minute.coerceIn(0, 59))
                ActionExecutionResult(true, "Opened Clock app to confirm alarm for $formatted.")
            } else {
                ActionExecutionResult(false, "No Clock application available to handle alarm intents.")
            }
        } catch (e: Exception) {
            ActionExecutionResult(false, "Could not create alarm: ${e.message}")
        }
    }

    // 3. Create Timer using Android AlarmClock Intent
    fun createTimer(seconds: Int, label: String = "J.A.R.V.I.S. Timer"): ActionExecutionResult {
        return try {
            val intent = Intent(AlarmClock.ACTION_SET_TIMER).apply {
                putExtra(AlarmClock.EXTRA_LENGTH, seconds.coerceAtLeast(1))
                putExtra(AlarmClock.EXTRA_MESSAGE, label)
                putExtra(AlarmClock.EXTRA_SKIP_UI, false)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            if (intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(intent)
                ActionExecutionResult(true, "Opened Clock timer for $seconds seconds.")
            } else {
                ActionExecutionResult(false, "No Clock application found for timers.")
            }
        } catch (e: Exception) {
            ActionExecutionResult(false, "Timer launch error: ${e.message}")
        }
    }

    // 4. Open YouTube (Native App -> Browser Fallback)
    fun openYouTube(searchQuery: String? = null): ActionExecutionResult {
        val url = if (searchQuery.isNullOrBlank()) {
            "https://www.youtube.com"
        } else {
            "https://www.youtube.com/results?search_query=${Uri.encode(searchQuery)}"
        }

        return try {
            val appIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                setPackage("com.google.android.youtube")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            if (appIntent.resolveActivity(context.packageManager) != null) {
                context.startActivity(appIntent)
                ActionExecutionResult(true, "Launched YouTube Android app.")
            } else {
                openUrlInBrowser(url, "YouTube")
            }
        } catch (e: Exception) {
            openUrlInBrowser(url, "YouTube")
        }
    }

    // 5. Open Google Maps (Native App -> Browser Fallback)
    fun openGoogleMaps(query: String? = null): ActionExecutionResult {
        return try {
            val geoUri = if (query.isNullOrBlank()) {
                Uri.parse("geo:0,0?q=")
            } else {
                Uri.parse("geo:0,0?q=${Uri.encode(query)}")
            }
            val mapIntent = Intent(Intent.ACTION_VIEW, geoUri).apply {
                setPackage("com.google.android.apps.maps")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            if (mapIntent.resolveActivity(context.packageManager) != null) {
                context.startActivity(mapIntent)
                ActionExecutionResult(true, "Launched Google Maps app.")
            } else {
                val webUrl = if (query.isNullOrBlank()) {
                    "https://maps.google.com"
                } else {
                    "https://www.google.com/maps/search/${Uri.encode(query)}"
                }
                openUrlInBrowser(webUrl, "Google Maps")
            }
        } catch (e: Exception) {
            ActionExecutionResult(false, "Unable to open Google Maps: ${e.message}")
        }
    }

    // 6. Open WhatsApp (Directly or with pre-filled message)
    fun openWhatsApp(phoneNumber: String? = null, message: String? = null): ActionExecutionResult {
        return try {
            if (phoneNumber.isNullOrBlank() && message.isNullOrBlank()) {
                val launch = context.packageManager.getLaunchIntentForPackage("com.whatsapp")
                    ?: context.packageManager.getLaunchIntentForPackage("com.whatsapp.w4b")
                if (launch != null) {
                    launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(launch)
                    return ActionExecutionResult(true, "Launched WhatsApp app.")
                }
            }

            val cleanPhone = phoneNumber?.replace(Regex("[^0-9+]"), "") ?: ""
            val uriBuilder = StringBuilder("https://api.whatsapp.com/send?")
            if (cleanPhone.isNotEmpty()) {
                uriBuilder.append("phone=${Uri.encode(cleanPhone)}&")
            }
            if (!message.isNullOrBlank()) {
                uriBuilder.append("text=${Uri.encode(message)}")
            }

            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uriBuilder.toString())).apply {
                setPackage("com.whatsapp")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            if (intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(intent)
                ActionExecutionResult(true, "Opened WhatsApp for confirmation before sending.")
            } else {
                openUrlInBrowser(uriBuilder.toString(), "WhatsApp")
            }
        } catch (e: Exception) {
            ActionExecutionResult(false, "Could not launch WhatsApp: ${e.message}")
        }
    }

    // 7. Open Phone Dialer or Place Confirmed Call
    fun dialOrCall(phoneNumber: String, directCall: Boolean = false): ActionExecutionResult {
        val cleanNumber = phoneNumber.trim()
        if (cleanNumber.isEmpty()) {
            return openDialerOnly()
        }

        return try {
            val useCallAction = directCall && PermissionManager.isGranted(context, Manifest.permission.CALL_PHONE)
            val action = if (useCallAction) Intent.ACTION_CALL else Intent.ACTION_DIAL
            val intent = Intent(action, Uri.parse("tel:$cleanNumber")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            ActionExecutionResult(
                true,
                if (useCallAction) "Placing call to $cleanNumber..." else "Opened Phone Dialer with $cleanNumber."
            )
        } catch (e: Exception) {
            ActionExecutionResult(false, "Dialer launch failed: ${e.message}")
        }
    }

    fun openDialerOnly(): ActionExecutionResult {
        return try {
            val intent = Intent(Intent.ACTION_DIAL).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            ActionExecutionResult(true, "Opened Phone Dialer.")
        } catch (e: Exception) {
            ActionExecutionResult(false, "Cannot open Phone Dialer: ${e.message}")
        }
    }

    // 8. Open Any Installed Android App by Name (Uses Android 15 <queries> package visibility)
    fun openInstalledAppByName(appNameQuery: String): ActionExecutionResult {
        val query = appNameQuery.trim().lowercase()
        if (query.isEmpty()) {
            return ActionExecutionResult(false, "No application name specified.")
        }

        return try {
            val pm = context.packageManager
            val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
            }
            val apps = pm.queryIntentActivities(mainIntent, PackageManager.MATCH_ALL)

            val matched = apps.firstOrNull { resolveInfo ->
                val label = resolveInfo.loadLabel(pm).toString().lowercase()
                label == query || label.contains(query)
            }

            if (matched != null) {
                val pkg = matched.activityInfo.packageName
                val launchIntent = pm.getLaunchIntentForPackage(pkg)
                if (launchIntent != null) {
                    launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(launchIntent)
                    val appLabel = matched.loadLabel(pm).toString()
                    return ActionExecutionResult(true, "Launched $appLabel.")
                }
            }

            ActionExecutionResult(false, "App \"$appNameQuery\" not found among installed launcher apps.")
        } catch (e: Exception) {
            ActionExecutionResult(false, "Failed to search installed apps: ${e.message}")
        }
    }

    // 9. Resolve Contact from Contact Picker URI (supports both Phone.CONTENT_URI and Contacts.CONTENT_URI)
    fun readContactFromPickerUri(contactUri: Uri): PickedContact? {
        return try {
            val phoneProjection = arrayOf(
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.NUMBER
            )
            context.contentResolver.query(contactUri, phoneProjection, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                    val numIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                    if (numIdx >= 0) {
                        val name = if (nameIdx >= 0) cursor.getString(nameIdx) ?: "Unknown" else "Unknown"
                        val number = cursor.getString(numIdx).orEmpty()
                        if (number.isNotBlank()) {
                            return PickedContact(name, number)
                        }
                    }
                }
            }

            // Fallback: resolve Contact._ID -> CommonDataKinds.Phone.CONTENT_URI
            val contactProjection = arrayOf(
                ContactsContract.Contacts._ID,
                ContactsContract.Contacts.DISPLAY_NAME
            )
            context.contentResolver.query(contactUri, contactProjection, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val idIdx = cursor.getColumnIndex(ContactsContract.Contacts._ID)
                    val nameIdx = cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME)
                    if (idIdx >= 0) {
                        val contactId = cursor.getString(idIdx)
                        val displayName = if (nameIdx >= 0) cursor.getString(nameIdx) ?: "Unknown" else "Unknown"
                        context.contentResolver.query(
                            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                            arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER),
                            "${ContactsContract.CommonDataKinds.Phone.CONTACT_ID} = ?",
                            arrayOf(contactId),
                            null
                        )?.use { phoneCursor ->
                            if (phoneCursor.moveToFirst()) {
                                val number = phoneCursor.getString(0).orEmpty()
                                if (number.isNotBlank()) {
                                    return PickedContact(displayName, number)
                                }
                            }
                        }
                    }
                }
            }
            null
        } catch (_: Exception) {
            null
        }
    }

    // 10. Lookup Contact Phone Number by Name (Requires READ_CONTACTS permission)
    fun findContactByName(nameQuery: String): PickedContact? {
        if (!PermissionManager.isGranted(context, Manifest.permission.READ_CONTACTS)) {
            return null
        }
        return try {
            val projection = arrayOf(
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.NUMBER
            )
            val selection = "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} LIKE ?"
            val selectionArgs = arrayOf("%${nameQuery.trim()}%")
            context.contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val name = cursor.getString(0) ?: nameQuery
                    val number = cursor.getString(1) ?: ""
                    if (number.isNotBlank()) {
                        return PickedContact(name, number)
                    }
                }
            }
            null
        } catch (_: Exception) {
            null
        }
    }

    fun openUrlInBrowser(url: String, label: String = "Website"): ActionExecutionResult {
        return try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            ActionExecutionResult(true, "Opened $label in browser.")
        } catch (e: Exception) {
            ActionExecutionResult(false, "Cannot open $label: ${e.message}")
        }
    }
}
