package com.alexvt.assistant.repository

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Bitmap
import android.hardware.display.DisplayManager
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.alexvt.assistant.App.Companion.androidAppContext
import com.alexvt.assistant.AppScope
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import kotlinx.coroutines.delay
import me.tatarka.inject.annotations.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine


@AppScope
@Inject
actual class ExtractableImageTextRepository {

    actual fun isExtractionAvailable(): Boolean =
        true

    actual suspend fun extractFromScreenArea(
        top: Int,
        bottom: Int,
        left: Int,
        right: Int,
        onImageCaptured: () -> Unit,
    ): String {
        // An overlay activity handles screenshot capturing, created / destroyed each time.
        // Time to result may be affected by permission system dialog showing, so result is awaited.
        fullScreenshotBitmapResultOrNull = null
        androidAppContext.startActivity(
            Intent(androidAppContext, ScreenshotPermissionHandlingActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        )
        while (true) {
            delay(100) // not captured yet...
            fullScreenshotBitmapResultOrNull?.run {
                onImageCaptured()
                exceptionOrNull()?.let {
                    Log.e(TAG, "Screenshot taking error", it)
                }
                return getOrNull()?.let { bitmap ->
                    val croppedBitmap = Bitmap.createBitmap(
                        bitmap,
                        left.widthToPx(bitmap),
                        top.heightToPx(bitmap),
                        (right - left).widthToPx(bitmap),
                        (bottom - top).heightToPx(bitmap),
                    )
                    bitmap.recycle()
                    croppedBitmap.runTextRecognition().apply {
                        croppedBitmap.recycle()
                        exceptionOrNull()?.let {
                            Log.e(TAG, "Text recognition error", it)
                        }
                    }.getOrNull() ?: ""
                } ?: "" // error handled above
            }
        }
    }

    private suspend fun Bitmap.runTextRecognition(): Result<String> =
        suspendCoroutine { continuation ->
            val image = InputImage.fromBitmap(this, 0)
            TextRecognition.getClient().process(image)
                .addOnSuccessListener { texts ->
                    continuation.resume(
                        Result.success(texts.textBlocks.joinToString(separator = "\n\n") {
                            it.text
                        })
                    )
                }
                .addOnFailureListener { exception ->
                    continuation.resume(Result.failure(exception))
                }
        }

    private fun Int.widthToPx(maxSizedBitmap: Bitmap): Int {
        val realScreenWidth = androidAppContext.resources.displayMetrics.widthPixels
        val density = androidAppContext.resources.displayMetrics.density
        return (this * density).toInt()
            .coerceIn(0, maxSizedBitmap.width)
            .coerceIn(0, realScreenWidth)
    }

    private fun Int.heightToPx(maxSizedBitmap: Bitmap): Int {
        val realScreenHeight = androidAppContext.resources.displayMetrics.heightPixels
        val density = androidAppContext.resources.displayMetrics.density
        return ((this * density).toInt()
            .coerceIn(0, maxSizedBitmap.height))
            .coerceIn(0, realScreenHeight)
    }

    companion object {
        const val TAG = "AssistantLog"
        var fullScreenshotBitmapResultOrNull: Result<Bitmap>? = null
    }

    class ScreenshotPermissionHandlingActivity : Activity() {

        private companion object {
            private const val REQUEST_CAPTURE = 1

            // permission lasts for the whole process lifespan duration, so it's kept statically
            private var screenshotIntentWithPermissionOrNull: Intent? = null
        }

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)

            // will / won't take screenshot depending on existence of
            // intent with MediaProjection permission
            triggerScreenshotService()

            screenshotIntentWithPermissionOrNull?.run {
                // MediaProjection permission granted already, nothing else to wait for
                finish()
            } ?: run {
                // will obtain intent with MediaProjection permission in onActivityResult,
                // and then take screenshot
                startActivityForResult(
                    (getSystemService(Service.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager)
                        .createScreenCaptureIntent(),
                    REQUEST_CAPTURE
                )
            }
        }

        /**
         * A foreground service is required for MediaProjection-based screenshot capture.
         * onStartCommand of the service will be invoked.
         *
         * If intent with MediaProjection permission was already obtained,
         * MediaProjection for taking actual screenshot can be created from that intent in service.
         *
         * Without MediaProjection permission, foreground service will simply start/run regardless.
         */
        private fun triggerScreenshotService() {
            val screenshotIntentForService = screenshotIntentWithPermissionOrNull?.run {
                setClass(
                    this@ScreenshotPermissionHandlingActivity,
                    ScreenshotService::class.java
                )
                //putExtra("actualWidth", window)
            } ?: Intent(this, ScreenshotService::class.java)
            startService(screenshotIntentForService)
        }

        override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
            if (requestCode == REQUEST_CAPTURE) {
                if (resultCode == RESULT_OK) {
                    screenshotIntentWithPermissionOrNull = data
                    // screenshot couldn't be taken from onCreate, now there's permission to do so
                    triggerScreenshotService()
                } else {
                    screenshotIntentWithPermissionOrNull = null
                    fullScreenshotBitmapResultOrNull = Result.failure(
                        Exception("Permission not granted")
                    )
                    Toast.makeText(this, "Permission not granted", Toast.LENGTH_SHORT)
                        .show()
                }
            }
            finish()
        }
    }

    class ScreenshotService : Service() {

        override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
            val notificationId = 1
            startForeground(notificationId, createNotification())

            intent?.takeIf {
                it.hasExtra("android.media.projection.extra.EXTRA_MEDIA_PROJECTION")
            }?.run {
                val mediaProjectionManager =
                    getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
                val mediaProjection =
                    mediaProjectionManager.getMediaProjection(Activity.RESULT_OK, this)
                getImageOnDisplay(mediaProjection) { result ->
                    fullScreenshotBitmapResultOrNull = result
                }
            }
            return START_STICKY
        }

        @SuppressLint("WrongConstant") // see imageFormat, https://github.com/omerjerk/Screenshotter/issues/1
        private fun getImageOnDisplay(
            mediaProjection: MediaProjection,
            onResult: (Result<Bitmap>) -> Unit,
        ) {
            resources.displayMetrics.run {
                val imageFormat = 0x1
                val maxImages = 1
                val virtualDisplayName = "Screenshot Virtual Display"
                val reader = ImageReader.newInstance(
                    widthPixels, heightPixels + getNavigationBarHeight(), imageFormat, maxImages
                )
                val display = mediaProjection.createVirtualDisplay(
                    virtualDisplayName,
                    widthPixels, heightPixels + getNavigationBarHeight(),
                    densityDpi, DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                    reader.surface, null, null
                )
                reader.setOnImageAvailableListener({
                    onResult(
                        if (display != null) {
                            Result.success(captureImage(reader)).also {
                                display.release()
                            }
                        } else {
                            Result.failure(Exception("Failed to get display screenshot"))
                        }
                    )
                    mediaProjection.stop()
                }, null)
            }
        }

        private fun captureImage(reader: ImageReader): Bitmap =
            resources.displayMetrics.run {
                val image = reader.acquireLatestImage()
                image.planes[0].run {
                    val bitmap = Bitmap.createBitmap(
                        rowStride / pixelStride,
                        heightPixels + getNavigationBarHeight(),
                        Bitmap.Config.ARGB_8888
                    )
                    bitmap.copyPixelsFromBuffer(buffer)
                    image.close()
                    bitmap
                }
            }

        @SuppressLint("DiscouragedApi")
        private fun getNavigationBarHeight(): Int {
            val resName =
                if (resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT) {
                    "navigation_bar_height"
                } else {
                    "navigation_bar_height_landscape"
                }
            val id: Int = resources.getIdentifier(resName, "dimen", "android")
            return if (id > 0) {
                resources.getDimensionPixelSize(id)
            } else {
                0
            }
        }

        private fun createNotification(): Notification {
            val channelId = "AssistantScreenshots"
            val channelName = "Assistant Screenshots"
            val importance = NotificationManager.IMPORTANCE_MIN
            val notificationChannel =
                NotificationChannel(channelId, channelName, importance).apply {
                    description = "Assistant ready to take a screenshot"
                }
            val notificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(notificationChannel)
            val builder = NotificationCompat.Builder(this, channelId)
                .setSmallIcon(android.R.drawable.sym_def_app_icon)
                .setContentTitle("Assistant Screenshots")
                .setContentText("Assistant ready to take a screenshot")
                .setPriority(NotificationCompat.PRIORITY_LOW)
            return builder.build()
        }

        override fun onBind(intent: Intent?): IBinder? =
            null
    }

}
