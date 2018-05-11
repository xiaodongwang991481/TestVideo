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
                source=edit_camera_source.text.toString(),
                dests=cameraDests
            )
        Log.i(LOGTAG, "set camera to ${camera.name}=${camera.source}")
        val intent = Intent()
        intent.putExtra("camera", camera)
        setResult(RESULT_OK, intent)
        finish()
    }

    fun onButtonClickAddDest() {
        Log.i(LOGTAG, "add camera dest")
        val intent = Intent(this, CameraDestEditActivity::class.java)
        this.startActivityForResult(intent, 1)
    }

    fun onButtonClickEdit(cameraDest: CameraDest) {
        Log.i(LOGTAG, "edit camera dest ${cameraDest.name}")
        val intent = Intent(this, CameraDestEditActivity::class.java)
        intent.putExtra("cameraDest", cameraDest)
        this.startActivityForResult(intent, 0)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        data?.let {
            val cameraDest: CameraDest? = it.getParcelableExtra("cameraDest") as? CameraDest
            if (cameraDest != null) {
                when (requestCode) {
                    0 -> updateCameraDest(cameraDest)
                    1 -> addCameraDest(cameraDest)
                    else -> {
                        Log.e(LOGTAG, "unknown request code $requestCode")
                    }
                }
            } else {
                Log.e(LOGTAG, "cameraDest is null")
            }
        }
    }

    fun updateCameraDest(cameraDest: CameraDest) {
        Log.i(LOGTAG,"update cameraDest ${cameraDest.name}")
        var found = false
        for (i in cameraDests.indices) {
            var existingCameraDest = cameraDests[i]
            if (existingCameraDest.name == cameraDest.name) {
                existingCameraDest = cameraDest
                cameraDests[i] = existingCameraDest
                Log.i(LOGTAG, "update existing camera dest ${existingCameraDest.name}")
                found = true
                break
            }
        }
        if (!found) {
            Log.e(LOGTAG, "cameradest ${cameraDest.name} is not found in existing camera dests")
        }
        cameraDestAdapter.notifyDataSetChanged()
    }

    fun addCameraDest(cameraDest: CameraDest) {
        Log.i(LOGTAG,"add camera dest ${cameraDest.name}")
        cameraDests.add(cameraDest)
        cameraDestAdapter.notifyDataSetChanged()
    }

    fun onButtonClickDelete(cameraDest: CameraDest) {
        Log.i(LOGTAG, "delete camera dest ${cameraDest.name}")
        cameraDests.remove(cameraDest)
        cameraDestAdapter.notifyDataSetChanged()
    }

    fun onButtonClickDeleteProperty(cameraDest: CameraDest, cameraDestProperty: CameraDestProperty) {
        Log.i(
                LOGTAG,
                "delete camera dest ${cameraDest.name} property ${cameraDestProperty.name}=${cameraDestProperty.value}"
        )
        cameraDest.dest_properties.remove(cameraDestProperty)
        cameraDestAdapter.notifyDataSetChanged()
    }
}
