package com.example.xiaodong.testvideo

import android.app.Activity
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.view.View
import android.support.v4.app.NavUtils
import android.util.Log
import android.view.MenuItem
import kotlinx.android.synthetic.main.activity_camera_edit.*
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.activity_video_play.*
import android.provider.MediaStore
import android.content.Intent
import android.support.v4.app.ActivityCompat.startActivityForResult
import android.R.attr.data
import android.net.Uri
import android.os.Environment.getExternalStorageDirectory
import android.content.ContentUris
import android.provider.DocumentsContract
import android.os.Build
import android.content.ContentResolver
import android.content.Context
import android.graphics.*
import android.os.Environment
import android.widget.Toast
import kotlinx.android.synthetic.main.cameras_layout.*


/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
class VideoPlayActivity : AppCompatActivity() {
    private val mHideHandler = Handler()
    private val mHidePart2Runnable = Runnable {
        // Delayed removal of status and navigation bar

        // Note that some of these constants are new as of API 16 (Jelly Bean)
        // and API 19 (KitKat). It is safe to use them, as they are inlined
        // at compile-time and do nothing on earlier devices.
        camera_play.systemUiVisibility =
                View.SYSTEM_UI_FLAG_LOW_PROFILE or
                View.SYSTEM_UI_FLAG_FULLSCREEN or
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
    }
    private val mShowPart2Runnable = Runnable {
        // Delayed display of UI elements
        supportActionBar?.show()
        fullscreen_content_controls.visibility = View.VISIBLE
    }
    private var mVisible: Boolean = false
    private val mHideRunnable = Runnable { hide() }
    private var fileManager: FileManager? = null

    inner class SelectVideo : View.OnClickListener {
        override fun onClick(v: View?) {
            val intent = Intent()
            intent.type = "video/*"
            intent.action = Intent.ACTION_OPEN_DOCUMENT
            startActivityForResult(
                    Intent.createChooser(intent, "Choose a file"),
                    REQUEST_TAKE_GALLERY_VIDEO
            )
        }
    }

    private var backgroundTask: VideoProcessTask? = null

    inner class CallbackPlayVideo(camera: Camera) : BitmapCallbackForCamera(camera) {
        override fun bitmapCallback(bitmap: Bitmap?) {
            super.bitmapCallback(bitmap)
            bitmap?.let {
                this@VideoPlayActivity.drawBitmap(bitmap)
            }
        }
    }

    var bitmapCameraCallback: BitmapCallbackForCamera? = null
    var finishCameraCallback: FinishCallbackForCamera? = null
    var camera: Camera? = null
    var last_pts: Long = 0

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == REQUEST_TAKE_GALLERY_VIDEO) {
                var source = fileManager!!.getCameraSource(this, data)
                source?.let {
                    camera!!.source = source
                    stopBackgroundTask()
                    startBackgroundTask()
                }
            } else {
                Log.e(LOG_TAG, "unkown request code: $requestCode")
                Toast.makeText(
                        this, "unkown request code: $requestCode",
                        Toast.LENGTH_SHORT
                ).show()
            }
        } else {
            Log.e(LOG_TAG, "result code = $resultCode")
            Toast.makeText(
                    this, "result code = $resultCode",
                    Toast.LENGTH_SHORT
            ).show()
        }
    }

    fun drawBitmap(bitmap: Bitmap) {
        var canvas = camera_play.holder.lockCanvas()
        canvas?.let {
            var srcRect = Rect(0, 0, bitmap.width, bitmap.height)
            var rectWidth = camera_play.measuredWidth
            var rectHeight = camera_play.measuredHeight
            if (
                    rectWidth.toFloat() / bitmap.width.toFloat() >
                    rectHeight.toFloat() / bitmap.height.toFloat()
            ) {
                rectWidth = rectHeight * bitmap.width / bitmap.height
            } else {
                rectHeight = rectWidth * bitmap.height / bitmap.width
            }
            val left = (camera_play.measuredWidth - rectWidth) / 2
            val top = (camera_play.measuredHeight - rectHeight) / 2
            var destRect = Rect(left, top, left + rectWidth, top + rectHeight)
            it.drawBitmap(bitmap, srcRect, destRect, null)
            camera_play.holder.unlockCanvasAndPost(it)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_video_play)
        Log.i(LOG_TAG, "VideoPlayActivity created")
        if (intent.hasExtra("camera")) {
            camera = intent.getParcelableExtra("camera")
            camera?.let {
                bitmapCameraCallback = CallbackPlayVideo(it)
                finishCameraCallback = FinishCallbackForCamera(it)
            }
            Log.i(LOG_TAG, "get camera $camera")
        }
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        mVisible = true

        // Set up the user interaction to manually show or hide the system UI.
        camera_play.setOnClickListener { toggle() }
        camera_play.holder.setKeepScreenOn(true)

        // Upon interacting with UI controls, delay any scheduled hide()
        // operations to prevent the jarring behavior of controls going away
        // while interacting with the UI.
        select_video.setOnClickListener(SelectVideo())
        fileManager = FileManager(this)
    }

    override fun onSaveInstanceState(outState: Bundle?) {
        outState?.let {
            super.onSaveInstanceState(outState)
        }
        Log.i(LOG_TAG, "save state camera: $camera")
        outState?.let {
            camera?.let {
                outState.putParcelable("camera", camera)
            }
            outState.putLong("last_pts", last_pts)
        }
    }

    fun startBackgroundTask() {
        Log.i(LOG_TAG, "start background task")
        finishCameraCallback?.let {
            it.clearFinished()
        }
        camera?.let {
            cit ->
            fileManager?.let {
                backgroundTask = backgroundTask ?: VideoProcessTask(
                        cit, it, bitmapCameraCallback, finishCameraCallback,
                        last_pts = last_pts, sync = true
                ).apply {
                    execute()
                }
            }
        }
    }

    fun stopBackgroundTask() {
        Log.i(LOG_TAG, "stop background task")
        finishCameraCallback?.let {
            it.setFinished()
        }
        backgroundTask?.apply {
            waitFinish()
        }
        backgroundTask = null
    }

    override fun onStart() {
        super.onStart()
        Log.i(LOG_TAG, "start activity")
        startBackgroundTask()
    }

    override fun onStop() {
        super.onStop()
        Log.i(LOG_TAG, "stop activity")
        stopBackgroundTask()
    }

    override fun onResume() {
        super.onResume()
        Log.i(LOG_TAG, "resume activity")
    }

    override fun onPause() {
        super.onPause()
        Log.i(LOG_TAG, "pause activity")
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle?) {
        savedInstanceState?.let {
            super.onRestoreInstanceState(savedInstanceState)
        }
        camera = savedInstanceState?.getParcelable("camera")
        camera?.let {
            bitmapCameraCallback = BitmapCallbackForCamera(it)
            finishCameraCallback = FinishCallbackForCamera(it)
        }
        last_pts = savedInstanceState?.getLong("last_pts") ?: 0
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)

        // Trigger the initial hide() shortly after the activity has been
        // created, to briefly hint to the user that UI controls
        // are available.
        delayedHide(100)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        if (id == android.R.id.home) {
            // This ID represents the Home or Up button.
            NavUtils.navigateUpFromSameTask(this)
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun toggle() {
        if (mVisible) {
            hide()
        } else {
            show()
        }
    }

    private fun hide() {
        // Hide UI first
        supportActionBar?.hide()
        fullscreen_content_controls.visibility = View.GONE
        mVisible = false

        // Schedule a runnable to remove the status and navigation bar after a delay
        mHideHandler.removeCallbacks(mShowPart2Runnable)
        mHideHandler.postDelayed(mHidePart2Runnable, UI_ANIMATION_DELAY.toLong())
    }

    private fun show() {
        // Show the system bar
        camera_play.systemUiVisibility =
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
        mVisible = true

        // Schedule a runnable to display UI elements after a delay
        mHideHandler.removeCallbacks(mHidePart2Runnable)
        mHideHandler.postDelayed(mShowPart2Runnable, UI_ANIMATION_DELAY.toLong())
    }

    /**
     * Schedules a call to hide() in [delayMillis], canceling any
     * previously scheduled calls.
     */
    private fun delayedHide(delayMillis: Int) {
        mHideHandler.removeCallbacks(mHideRunnable)
        mHideHandler.postDelayed(mHideRunnable, delayMillis.toLong())
    }

    companion object {
        /**
         * Whether or not the system UI should be auto-hidden after
         * [AUTO_HIDE_DELAY_MILLIS] milliseconds.
         */
        private val AUTO_HIDE = true

        /**
         * If [AUTO_HIDE] is set, the number of milliseconds to wait after
         * user interaction before hiding the system UI.
         */
        private val AUTO_HIDE_DELAY_MILLIS = 3000

        /**
         * Some older devices needs a small delay between UI widget updates
         * and a change of the status and navigation bar.
         */
        private val UI_ANIMATION_DELAY = 300

        private val REQUEST_TAKE_GALLERY_VIDEO = 1
        private val LOG_TAG = "VideoPlayActivity"
    }
}
