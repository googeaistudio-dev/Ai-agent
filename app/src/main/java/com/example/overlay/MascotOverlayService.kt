package com.example.overlay

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import kotlin.math.abs

/**
 * Robust, crash-proof Floating Mascot Overlay Service.
 * Does not rely on fragile WindowManager Compose recomposers, ensuring zero crashes
 * across all Android versions while providing smooth draggable floating mascot companion.
 */
class MascotOverlayService : Service() {

    private var windowManager: WindowManager? = null
    private var overlayContainer: FrameLayout? = null
    private var windowLayoutParams: WindowManager.LayoutParams? = null

    private var initialX = 0
    private var initialY = 0
    private var initialTouchX = 0f
    private var initialTouchY = 0f

    companion object {
        const val CHANNEL_ID = "nafsi_floating_mascot_channel"
        const val NOTIFICATION_ID = 2026

        @Volatile
        var isRunning = false
            private set

        fun start(context: Context) {
            try {
                if (Settings.canDrawOverlays(context)) {
                    val intent = Intent(context, MascotOverlayService::class.java)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        context.startForegroundService(intent)
                    } else {
                        context.startService(intent)
                    }
                }
            } catch (_: Throwable) {}
        }

        fun stop(context: Context) {
            try {
                val intent = Intent(context, MascotOverlayService::class.java)
                context.stopService(intent)
            } catch (_: Throwable) {}
        }
    }

    override fun onCreate() {
        super.onCreate()
        try {
            // CRITICAL: Always initialize notification and call startForeground at line 1 of onCreate
            // to satisfy Android 8+ foreground service requirements and prevent RemoteServiceException.
            createNotificationChannel()
            val notification = buildNotification("Nafsi AI ভাসমান সহকারী সক্রিয় রয়েছে")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
                } else {
                    startForeground(NOTIFICATION_ID, notification)
                }
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }

            isRunning = true

            if (Settings.canDrawOverlays(this)) {
                createFloatingMascotView()
            } else {
                stopSelf()
            }
        } catch (_: Throwable) {
            stopSelf()
        }
    }

    private fun createFloatingMascotView() {
        try {
            windowManager = getSystemService(Context.WINDOW_SERVICE) as? WindowManager ?: return

            val layoutFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            }

            val density = resources.displayMetrics.density
            val sizePx = (72 * density).toInt()

            windowLayoutParams = WindowManager.LayoutParams(
                sizePx,
                sizePx,
                layoutFlag,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or Gravity.START
                x = (40 * density).toInt()
                y = (180 * density).toInt()
            }

            // High-polish floating orb container with glowing neon cyan border
            overlayContainer = FrameLayout(this).apply {
                val glowBorder = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(Color.parseColor("#E60A0E1A"))
                    setStroke((2 * density).toInt(), Color.parseColor("#00F0FF"))
                }
                background = glowBorder
                clipToOutline = true
                elevation = 16f

                val imageView = ImageView(this@MascotOverlayService).apply {
                    setImageResource(R.drawable.nafsi_app_icon_1792287520913)
                    scaleType = ImageView.ScaleType.CENTER_CROP
                    val pad = (6 * density).toInt()
                    setPadding(pad, pad, pad, pad)
                }

                addView(
                    imageView,
                    FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        Gravity.CENTER
                    )
                )

                setOnTouchListener { _, event ->
                    val lp = windowLayoutParams ?: return@setOnTouchListener false
                    when (event.action) {
                        MotionEvent.ACTION_DOWN -> {
                            initialX = lp.x
                            initialY = lp.y
                            initialTouchX = event.rawX
                            initialTouchY = event.rawY
                            true
                        }
                        MotionEvent.ACTION_MOVE -> {
                            lp.x = initialX + (event.rawX - initialTouchX).toInt()
                            lp.y = initialY + (event.rawY - initialTouchY).toInt()
                            try {
                                windowManager?.updateViewLayout(this, lp)
                            } catch (_: Throwable) {}
                            true
                        }
                        MotionEvent.ACTION_UP -> {
                            val diffX = abs(event.rawX - initialTouchX)
                            val diffY = abs(event.rawY - initialTouchY)
                            // Click detected if movement was less than 15 pixels
                            if (diffX < 15 && diffY < 15) {
                                openMainActivity()
                            }
                            true
                        }
                        else -> false
                    }
                }
            }

            windowManager?.addView(overlayContainer, windowLayoutParams)
        } catch (_: Throwable) {
            stopSelf()
        }
    }

    private fun openMainActivity() {
        try {
            val intent = Intent(this, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            }
            startActivity(intent)
        } catch (_: Throwable) {}
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Nafsi AI Assistant Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows status of continuous listening and floating mascot"
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(text: String): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Nafsi AI Agent")
            .setContentText(text)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        try {
            if (overlayContainer != null) {
                windowManager?.removeView(overlayContainer)
                overlayContainer = null
            }
        } catch (_: Throwable) {}
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
