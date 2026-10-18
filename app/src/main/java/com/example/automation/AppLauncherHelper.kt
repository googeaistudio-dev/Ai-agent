package com.example.automation

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.net.Uri
import android.provider.AlarmClock
import android.provider.MediaStore
import android.provider.Settings

class AppLauncherHelper(private val context: Context) {

    fun launchAppByName(nameQuery: String): Result<String> {
        val q = nameQuery.trim().lowercase()

        // 1. Direct System Intent Handlers for System Controls
        when {
            q.contains("wifi") || q.contains("ওয়াইফাই") || q.contains("ওয়াইফাই") -> {
                return launchSystemSettings(Settings.ACTION_WIFI_SETTINGS, "Wi-Fi Settings")
            }
            q.contains("bluetooth") || q.contains("ব্লুটুথ") -> {
                return launchSystemSettings(Settings.ACTION_BLUETOOTH_SETTINGS, "Bluetooth Settings")
            }
            q.contains("display") || q.contains("ডিসপ্লে") || q.contains("উজ্জ্বলতা") || q.contains("brightness") -> {
                return launchSystemSettings(Settings.ACTION_DISPLAY_SETTINGS, "Display Settings")
            }
            q.contains("sound") || q.contains("সাউন্ড") || q.contains("ভলিউম") || q.contains("volume") -> {
                return launchSystemSettings(Settings.ACTION_SOUND_SETTINGS, "Sound Settings")
            }
            q.contains("battery") || q.contains("ব্যাটারি") -> {
                return launchSystemSettings(Intent.ACTION_POWER_USAGE_SUMMARY, "Battery Settings")
            }
            q.contains("app settings") || q.contains("অ্যাপ্লিকেশন") -> {
                return launchSystemSettings(Settings.ACTION_APPLICATION_SETTINGS, "Application Settings")
            }
            q.contains("date") || q.contains("time") || q.contains("তারিখ") -> {
                return launchSystemSettings(Settings.ACTION_DATE_SETTINGS, "Date & Time Settings")
            }
            q.contains("location") || q.contains("জিপিএস") || q.contains("gps") || q.contains("লোকেশন") -> {
                return launchSystemSettings(Settings.ACTION_LOCATION_SOURCE_SETTINGS, "Location Settings")
            }
            q.contains("camera") || q.contains("ক্যামেরা") || q.contains("ছবি তোলো") -> {
                val intent = Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                return tryStartIntent(intent, "Camera")
            }
            q.contains("gallery") || q.contains("গ্যালারি") || q.contains("ফটো") || q.contains("photos") -> {
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    type = "image/*"
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                return tryStartIntent(intent, "Gallery / Photos")
            }
            q.contains("dial") || q.contains("phone") || q.contains("ফোন") || q.contains("কল") -> {
                val intent = Intent(Intent.ACTION_DIAL).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                return tryStartIntent(intent, "Phone Dialer")
            }
            q.contains("alarm") || q.contains("অ্যালার্ম") -> {
                val intent = Intent(AlarmClock.ACTION_SHOW_ALARMS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                return tryStartIntent(intent, "Alarm Clock")
            }
        }

        // 2. Discover installed apps on device
        val pm = context.packageManager
        val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }

        val resolvedActivities = pm.queryIntentActivities(mainIntent, 0)
        val normalizedQuery = normalizeAppName(q)

        var bestMatch: ResolveInfo? = null

        for (info in resolvedActivities) {
            val label = info.loadLabel(pm).toString().lowercase()
            val pkg = info.activityInfo.packageName.lowercase()

            if (label == normalizedQuery || pkg.endsWith(".$normalizedQuery")) {
                bestMatch = info
                break
            }
            if (label.contains(normalizedQuery) || pkg.contains(normalizedQuery)) {
                if (bestMatch == null) {
                    bestMatch = info
                }
            }
        }

        if (bestMatch != null) {
            val launchIntent = pm.getLaunchIntentForPackage(bestMatch.activityInfo.packageName)?.apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            if (launchIntent != null) {
                context.startActivity(launchIntent)
                val appLabel = bestMatch.loadLabel(pm).toString()
                return Result.success("Launched $appLabel (${bestMatch.activityInfo.packageName})")
            }
        }

        // 3. Fallback: Web search if app not found on device
        if (q.contains("search") || q.contains("খোঁজ") || q.contains("সার্চ")) {
            val queryText = q.replace("search", "").replace("সার্চ", "").replace("খোঁজ", "").trim()
            val searchIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?q=${Uri.encode(queryText)}")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            return tryStartIntent(searchIntent, "Google Search for '$queryText'")
        }

        return Result.failure(Exception("Could not find installed application matching '$nameQuery'."))
    }

    private fun launchSystemSettings(action: String, displayName: String): Result<String> {
        return try {
            val intent = Intent(action).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            Result.success("Opened $displayName")
        } catch (e: Exception) {
            Result.failure(Exception("Could not open $displayName: ${e.message}"))
        }
    }

    private fun tryStartIntent(intent: Intent, displayName: String): Result<String> {
        return try {
            context.startActivity(intent)
            Result.success("Opened $displayName")
        } catch (e: Exception) {
            Result.failure(Exception("Could not open $displayName: ${e.message}"))
        }
    }

    private fun normalizeAppName(query: String): String {
        val q = query.trim().lowercase()
        return when {
            q.contains("calc") || q.contains("ক্যালকুলেটর") || q.contains("হিসাব") -> "calculator"
            q.contains("setting") || q.contains("সেটিংস") -> "settings"
            q.contains("youtube") || q.contains("ইউটিউব") || q.contains("গান") -> "youtube"
            q.contains("note") || q.contains("নোট") || q.contains("খাতা") -> "notes"
            q.contains("chrome") || q.contains("browser") || q.contains("ব্রাউজার") || q.contains("নেট") -> "chrome"
            q.contains("clock") || q.contains("ঘড়ি") || q.contains("ঘড়ি") || q.contains("সময়") -> "clock"
            q.contains("camera") || q.contains("ক্যামেরা") || q.contains("ছবি") -> "camera"
            q.contains("map") || q.contains("ম্যাপ") || q.contains("রাস্তা") -> "maps"
            q.contains("whatsapp") || q.contains("হোয়াটসঅ্যাপ") || q.contains("হোয়াটসঅ্যাপ") -> "whatsapp"
            q.contains("facebook") || q.contains("ফেসবুক") -> "facebook"
            q.contains("play") || q.contains("প্লে স্টোর") -> "vending"
            q.contains("message") || q.contains("মেসেজ") || q.contains("বার্তা") || q.contains("sms") -> "messaging"
            q.contains("file") || q.contains("ফাইল") -> "files"
            q.contains("drive") || q.contains("ড্রাইভ") -> "drive"
            q.contains("gmail") || q.contains("মেইল") || q.contains("ইমেইল") -> "gm"
            else -> q
        }
    }
}
