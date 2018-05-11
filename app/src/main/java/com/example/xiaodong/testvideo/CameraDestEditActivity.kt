package com.example.xiaodong.testvideo

import android.content.Intent
import android.graphics.Color
import android.util.Log
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import kotlinx.android.synthetic.main.activity_camera_dest_edit.*
import kotlinx.android.synthetic.main.activity_camera_edit.*

class CameraDestEditActivity : AppCompatActivity() {

    private val LOGTAG = "CameraDestEditActivity"
    private var cameraDestProperties = ArrayList<CameraDestProperty>()
    private var cameraDestPropertyAdapter = CameraDestPropertyAdapter(this, cameraDestProperties)

    inner class SaveCameraDest: View.OnClickListener {
        override fun onClick(v: View?) {
            this@CameraDestEditActivity.onButtonClickSave()
        }
    }

    inner class AddCameraDestProperty: View.OnClickListener {
        override fun onClick(v: View?) {
            this@CameraDestEditActivity.onButtonClickAdd()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera_dest_edit)
        if (intent.hasExtra("cameraDest")) {
            var cameraDest: CameraDest = intent.getParcelableExtra("cameraDest")
            Log.i(LOGTAG, "get camera dest ${cameraDest.name}")
            edit_camera_dest_name.setText(cameraDest.name)
            edit_camera_dest_name.focusable = 0
            edit_camera_dest_name.setEnabled(false)
            edit_camera_dest_name.setTextColor(Color.GRAY)
            add_camera_dest_property.setOnClickListener(AddCameraDestProperty())
        }
        edit_camera_dest_save.setOnClickListener(SaveCameraDest())
    }

    fun onButtonClickSave() {
        Log.i(LOGTAG, "save camera dest")
        if (edit_camera_dest_name.text.isNullOrBlank()) {
            Log.e(LOGTAG, "camera dest name is empty")
            return
        }
        val cameraDest = CameraDest(
                name=edit_camera_dest_name.text.toString(),
                dest_properties = cameraDestProperties
        )
        Log.i(LOGTAG, "set camera dest to ${cameraDest.name}")
        val intent = Intent()
        intent.putExtra("cameraDest", cameraDest)
        setResult(RESULT_OK, intent)
        finish()
    }

    fun onButtonClickAdd() {

    }

    fun onButtonClickDelete(cameraDestProperty: CameraDestProperty) {
        Log.i(LOGTAG, "delete cameraDestProperty ${cameraDestProperty.name}=${cameraDestProperty.value}")
        cameraDestProperties.remove(cameraDestProperty)
        cameraDestPropertyAdapter.notifyDataSetChanged()
    }
 }
