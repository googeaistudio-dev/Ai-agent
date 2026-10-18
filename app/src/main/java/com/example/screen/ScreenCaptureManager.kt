package com.example.screen

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Handler
import android.os.Looper
import android.util.DisplayMetrics
import android.view.WindowManager
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ScreenCaptureManager(private val context: Context) {

    private val projectionManager = context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    private var resultCode: Int = 0
    private var resultData: Intent? = null

    val isPermissionGranted: Boolean
        get() = resultData != null

    fun setConsentResult(code: Int, data: Intent) {
        this.resultCode = code
        this.resultData = data
    }

    fun createScreenCaptureIntent(): Intent {
        return projectionManager.createScreenCaptureIntent()
    }

    suspend fun captureScreenshot(): Bitmap? = withContext(Dispatchers.Default) {
        val data = resultData ?: return@withContext null
        val deferred = CompletableDeferred<Bitmap?>()

        mainHandler.post {
            try {
                val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
                val metrics = DisplayMetrics()
                @Suppress("DEPRECATION")
                wm.defaultDisplay.getRealMetrics(metrics)

                val width = metrics.widthPixels
                val height = metrics.heightPixels
                val density = metrics.densityDpi

                if (imageReader == null || imageReader?.width != width || imageReader?.height != height) {
                    imageReader?.close()
                    imageReader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2)
                }

                if (mediaProjection == null) {
                    mediaProjection = projectionManager.getMediaProjection(resultCode, data.clone() as Intent)
                }

                virtualDisplay?.release()
                virtualDisplay = mediaProjection?.createVirtualDisplay(
                    "NafsiScreenCapture",
                    width,
                    height,
                    density,
                    DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                    imageReader?.surface,
                    null,
                    mainHandler
                )

                // Give 250ms for frame to render on virtual display surface
                mainHandler.postDelayed({
                    try {
                        val image = imageReader?.acquireLatestImage()
                        if (image != null) {
                            val planes = image.planes
                            val buffer = planes[0].buffer
                            val pixelStride = planes[0].pixelStride
                            val rowStride = planes[0].rowStride
                            val rowPadding = rowStride - pixelStride * width

                            val bitmap = Bitmap.createBitmap(
                                width + rowPadding / pixelStride,
                                height,
                                Bitmap.Config.ARGB_8888
                            )
                            bitmap.copyPixelsFromBuffer(buffer)
                            image.close()

                            val cropped = if (rowPadding > 0) {
                                Bitmap.createBitmap(bitmap, 0, 0, width, height)
                            } else {
                                bitmap
                            }
                            deferred.complete(cropped)
                        } else {
                            deferred.complete(null)
                        }
                    } catch (e: Exception) {
                        deferred.complete(null)
                    }
                }, 250L)
            } catch (e: Exception) {
                deferred.complete(null)
            }
        }

        deferred.await()
    }

    fun release() {
        mainHandler.post {
            try {
                virtualDisplay?.release()
                virtualDisplay = null
                imageReader?.close()
                imageReader = null
                mediaProjection?.stop()
                mediaProjection = null
            } catch (_: Exception) {}
        }
    }
}
