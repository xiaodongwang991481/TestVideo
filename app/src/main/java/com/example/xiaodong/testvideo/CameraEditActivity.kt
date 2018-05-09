package com.example.xiaodong.testvideo

import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import kotlinx.android.synthetic.main.activity_camera_edit.*

class CameraEditActivity : AppCompatActivity() {

    private val LOG_TAG = "CameraEditActivity"

    inner class SaveCamera : View.OnClickListener {
        override fun onClick(v: View?) {
            this@CameraEditActivity.onButtonClickSave()
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera_edit)
        if (intent.hasExtra("camera")) {
            var camera: Camera = intent.getParcelableExtra("camera")
            edit_camera_name.setText(camera.name)
            edit_camera_name.focusable = 0
            edit_camera_source.setText(camera.source)
        }
    }

    public fun onButtonClickSave() {
        Log.i(LOG_TAG, "save camera")
        if (edit_camera_name.text.isNullOrBlank()) {
            Log.e(LOG_TAG, "camera name is empty")
            return
        }
        if (edit_camera_source.text.isNullOrBlank()) {
            Log.e(LOG_TAG, "camera source is empty")
            return
        }
        var camera: Camera = Camera(
                name=edit_camera_name.text.toString(),
                source=edit_camera_source.text.toString()
            )
        var intent: Intent = Intent()
        intent.putExtra("camera", camera)
        setResult(RESULT_OK, intent)
    }
}
