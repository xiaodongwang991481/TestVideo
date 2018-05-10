package com.example.xiaodong.testvideo

import android.content.Intent
import android.graphics.Color
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import kotlinx.android.synthetic.main.activity_camera_edit.*

class CameraEditActivity : AppCompatActivity() {

    private val LOGTAG = "CameraEditActivity"
    private var cameraDests = ArrayList<CameraDest>()
    private var cameraDestAdapter = CameraDestAdapter(this, cameraDests)

    inner class SaveCamera : View.OnClickListener {
        override fun onClick(v: View?) {
            this@CameraEditActivity.onButtonClickSave()
        }
    }

    inner class AddCameraDest : View.OnClickListener {
        override fun onClick(v: View?) {
            this@CameraEditActivity.onButtonClickAddDest()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera_edit)
        if (intent.hasExtra("camera")) {
            val camera: Camera = intent.getParcelableExtra("camera")
            Log.i(LOGTAG, "get camera ${camera.name}=${camera.source}")
            edit_camera_name.setText(camera.name)
            edit_camera_name.focusable = 0
            edit_camera_name.setEnabled(false)
            edit_camera_name.setTextColor(Color.GRAY)
            edit_camera_source.setText(camera.source)
            add_camera_dest.setOnClickListener(AddCameraDest())
            cameraDests = camera.dests
            cameraDestAdapter = CameraDestAdapter(this, cameraDests)
            edit_camera_dests.setAdapter(cameraDestAdapter)
        }
        edit_camera_save.setOnClickListener(SaveCamera())
    }

    fun onButtonClickSave() {
        Log.i(LOGTAG, "save camera")
        if (edit_camera_name.text.isNullOrBlank()) {
            Log.e(LOGTAG, "camera name is empty")
            return
        }
        if (edit_camera_source.text.isNullOrBlank()) {
            Log.e(LOGTAG, "camera source is empty")
            return
        }
        val camera = Camera(
                name=edit_camera_name.text.toString(),
                source=edit_camera_source.text.toString()
            )
        Log.i(LOGTAG, "set camera to ${camera.name}=${camera.source}")
        val intent = Intent()
        intent.putExtra("camera", camera)
        setResult(RESULT_OK, intent)
        finish()
    }

    fun onButtonClickAddDest() {
        Log.i(LOGTAG, "add camera dest")
        if (camera_dest.text.isNullOrBlank()) {
            Log.e(LOGTAG, "camera dest is empty")
            return
        }
        val cameraDest = CameraDest(camera_dest.text.toString())
        Log.i(LOGTAG, "add camera dest ${cameraDest.name}")
        cameraDests.add(cameraDest)
        cameraDestAdapter.notifyDataSetChanged()
    }

    fun onButtonClickAddProperty(val cameraDest: CameraDest) {
    }

    fun onButtonClickDelete(val cameraDest: CameraDest) {
        cameraDests.remove(cameraDest)
        cameraDestAdapter.notifyDataSetChanged()
    }

    fun onButtonClickDeleteProperty(val cameraDest: CameraDest, val cameraDestProperty: CameraDestProperty) {
        cameraDest.dest_properties.remove(cameraDestProperty)
        cameraDestAdapter.notifyDataSetChanged()
    }
}
