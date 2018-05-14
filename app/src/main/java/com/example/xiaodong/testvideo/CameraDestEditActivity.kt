package com.example.xiaodong.testvideo

import android.content.Intent
import android.graphics.Color
import android.util.Log
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import kotlinx.android.synthetic.main.activity_camera_dest_edit.*
import kotlinx.android.synthetic.main.activity_camera_edit.*
import kotlinx.android.synthetic.main.camera_dest_property_layout.*

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

    override fun onStart() {
        super.onStart()
        Log.i(LOGTAG, "start activity")
    }

    override fun onStop() {
        super.onStop()
        Log.i(LOGTAG, "stop activity")
    }

    override fun onResume() {
        super.onResume()
        Log.i(LOGTAG, "resume activity")
    }

    override fun onPause() {
        super.onPause()
        Log.i(LOGTAG, "pause activity")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera_dest_edit)
        Log.i(LOGTAG, "camera dest activity initialized with state = $savedInstanceState")
        if (intent.hasExtra("cameraDest")) {
            var cameraDest: CameraDest = intent.getParcelableExtra("cameraDest")
            Log.i(LOGTAG, "get camera dest $cameraDest")
            var cameraDestName = cameraDest.name
            edit_camera_dest_name.setText(cameraDestName)
            edit_camera_dest_name.focusable = 0
            edit_camera_dest_name.setEnabled(false)
            edit_camera_dest_name.setTextColor(Color.GRAY)
            cameraDestProperties = cameraDest.dest_properties
        }
        camera_dest_properties.setAdapter(cameraDestPropertyAdapter)
        add_camera_dest_property.setOnClickListener(AddCameraDestProperty())
        edit_camera_dest_save.setOnClickListener(SaveCameraDest())
    }

    override fun onSaveInstanceState(outState: Bundle?) {
        outState?.let {
            super.onSaveInstanceState(outState)
        }
        var cameraDestName = edit_camera_dest_name.text.toString()
        Log.i(LOGTAG, "save state camera dest name = $cameraDestName")
        outState?.putString("camera_dest_name", cameraDestName)
        outState?.putParcelableArrayList("camera_dest_properties", cameraDestProperties)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle?) {
        savedInstanceState?.let {
            super.onRestoreInstanceState(savedInstanceState)
        }
        var cameraDestName = savedInstanceState?.getString(
                "camera_dest_name"
        ) ?: ""
        Log.i(LOGTAG, "restore state camera dest name = $cameraDestName")
        edit_camera_dest_name.setText(cameraDestName
        )
        cameraDestProperties = savedInstanceState?.getParcelableArrayList(
                "camera_dest_properties"
        ) ?: ArrayList<CameraDestProperty>()
        cameraDestPropertyAdapter = CameraDestPropertyAdapter(this, cameraDestProperties)
        camera_dest_properties.setAdapter(cameraDestPropertyAdapter)
    }

    fun onButtonClickSave() {
        if (edit_camera_dest_name == null || edit_camera_dest_name.text.isNullOrBlank()) {
            Log.e(LOGTAG, "camera dest name is empty")
            return
        }
        var cameraDestName = edit_camera_dest_name.text.toString()
        val cameraDest = CameraDest(
                name=cameraDestName,
                dest_properties=cameraDestProperties
        )
        Log.i(LOGTAG, "set camera dest to $cameraDest")
        val intent = Intent()
        intent.putExtra("cameraDest", cameraDest)
        setResult(RESULT_OK, intent)
        finish()
    }

    fun onButtonClickAdd() {
        Log.i(LOGTAG, "add camera dest property")
        if (
                add_camera_dest_property_name == null ||
                add_camera_dest_property_name.text.isNullOrBlank()
        ) {
            Log.e(LOGTAG, "camera dest property name is empty")
            return
        }
        if (
                add_camera_dest_property_value == null ||
                add_camera_dest_property_value.text.isNullOrBlank()
        ) {
            Log.e(LOGTAG, "camera dest property value is empty")
            return
        }
        addCameraDestProperty(
                CameraDestProperty(
                        add_camera_dest_property_name.text.toString(),
                        add_camera_dest_property_value.text.toString()
                )
        )
    }

    fun addCameraDestProperty(cameraDestProperty: CameraDestProperty) {
        Log.i(LOGTAG,"add camera dest property $cameraDestProperty")
        cameraDestProperties.add(cameraDestProperty)
        cameraDestPropertyAdapter.notifyDataSetChanged()
    }

    fun onButtonClickDelete(cameraDestProperty: CameraDestProperty) {
        Log.i(LOGTAG, "delete cameraDestProperty $cameraDestProperty")
        cameraDestProperties.remove(cameraDestProperty)
        cameraDestPropertyAdapter.notifyDataSetChanged()
    }
 }
