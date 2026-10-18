package com.example.automation

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo

class AppLauncherHelper(private val context: Context) {

    fun launchAppByName(nameQuery: String): Result<String> {
        val pm = context.packageManager
        val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }

        val resolvedActivities = pm.queryIntentActivities(mainIntent, 0)
        if (resolvedActivities.isEmpty()) {
            return Result.failure(Exception("No launcher applications accessible on this device."))
        }

        val normalizedQuery = normalizeAppName(nameQuery)

        // Find activity matching label or package
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

        return Result.failure(Exception("Could not find installed application matching '$nameQuery'."))
    }

    private fun normalizeAppName(query: String): String {
        val q = query.trim().lowercase()
        return when {
            q.contains("calc") || q.contains("ক্যালকুলেটর") || q.contains("হিসাব") -> "calculator"
            q.contains("setting") || q.contains("সেটিংস") -> "settings"
            q.contains("note") || q.contains("নোট") -> "notes"
            q.contains("chrome") || q.contains("browser") || q.contains("ব্রাউজার") -> "chrome"
            q.contains("clock") || q.contains("ঘড়ি") || q.contains("সময়") -> "clock"
            q.contains("camera") || q.contains("ক্যামেরা") -> "camera"
            q.contains("map") || q.contains("ম্যাপ") -> "maps"
            else -> q
        }
    }
}
