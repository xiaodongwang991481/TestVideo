package com.example.xiaodong.testvideo

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.PersistableBundle
import android.util.Log
import android.view.View
import android.widget.AdapterView
import kotlinx.android.synthetic.main.activity_camera_edit.*

class CameraEditActivity : AppCompatActivity() {

    private val LOG_TAG = "CameraEditActivity"
    private var cameraDests: ArrayList<CameraDest>? = null
    var groupIndicatorWidth: Int = 50
    var childIndicatorWidth: Int = 50
    private var cameraDestAdapter: CameraDestAdapter? = null

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

    inner class EditCameraDest : AdapterView.OnItemLongClickListener {
        override fun onItemLongClick(parent: AdapterView<*>?, view: View?, position: Int, id: Long): Boolean {
            var cameraDest = edit_camera_dests.getItemAtPosition(position) as CameraDest
            Log.i(LOG_TAG, "item $position long click on $cameraDest")
            this@CameraEditActivity.onButtonClickEdit(cameraDest)
            return true
        }
    }

    override fun onStart() {
        super.onStart()
        Log.i(LOG_TAG, "start activity")
    }

    override fun onStop() {
        super.onStop()
        Log.i(LOG_TAG, "stop activity")
    }

    override fun onResume() {
        super.onResume()
        Log.i(LOG_TAG, "resume activity")
    }

    override fun onPause() {
        super.onPause()
        Log.i(LOG_TAG, "pause activity")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera_edit)
        Log.i(LOG_TAG, "camera edit activity initialized with state = $savedInstanceState")
        if (intent.hasExtra("camera")) {
            val camera: Camera = intent.getParcelableExtra("camera") as Camera
            Log.i(LOG_TAG, "get camera $camera")
            var cameraName = camera.name
            var cameraSource = camera.source
            edit_camera_name.setText(cameraName)
            edit_camera_name.focusable = View.NOT_FOCUSABLE
            edit_camera_name.setEnabled(false)
            edit_camera_name.setTextColor(Color.GRAY)
            edit_camera_source.setText(cameraSource)
            cameraDests = camera.dests
        } else {
            cameraDests = ArrayList()
        }
        cameraDestAdapter = CameraDestAdapter(this, cameraDests!!)
        var header = layoutInflater.inflate(R.layout.camera_dest_header, edit_camera_dests, false) as View
        edit_camera_dests.addHeaderView(header)
        var footer = layoutInflater.inflate(R.layout.listview_footer, edit_camera_dests, false) as View
        edit_camera_dests.addFooterView(footer)
        edit_camera_dests.setAdapter(cameraDestAdapter!!)
        edit_camera_dests.setOnItemLongClickListener(EditCameraDest())
        val groupIndicator = resources.getDrawable(R.drawable.expandible_group_indicator, this.theme)
        val childIndicator = resources.getDrawable(R.drawable.expandible_child_indicator, this.theme)
        groupIndicatorWidth = groupIndicator.intrinsicWidth
        childIndicatorWidth = childIndicator.intrinsicWidth
        Log.i(LOG_TAG, "group indicator width: $groupIndicatorWidth")
        Log.i(LOG_TAG, "child indicator width: $childIndicatorWidth")
        edit_camera_dests.setIndicatorBounds(0, groupIndicatorWidth)
        edit_camera_dests.setGroupIndicator(groupIndicator)
        edit_camera_dests.setChildIndicatorBounds(0, childIndicatorWidth)
        edit_camera_dests.setChildIndicator(childIndicator)
        add_camera_dest.setOnClickListener(AddCameraDest())
        edit_camera_save.setOnClickListener(SaveCamera())
    }

    override fun onSaveInstanceState(outState: Bundle?) {
        outState?.let {
            super.onSaveInstanceState(outState)
        }

        var cameraName = edit_camera_name.text.toString()
        var cameraSource = edit_camera_source.text.toString()
        Log.i(LOG_TAG, "save state camera name = $cameraName, camera source = $cameraSource")
        outState?.let {
            outState.putString("name", cameraName)
            outState.putString("source", cameraSource)
            cameraDests?.let {
                outState.putParcelableArrayList("camera_dests", cameraDests)
            }
        }
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle?) {
        savedInstanceState?.let {
            super.onRestoreInstanceState(savedInstanceState)
        }
        var cameraName = savedInstanceState?.getString("name") ?: ""
        var cameraSource = savedInstanceState?.getString("source") ?: ""
        Log.i(LOG_TAG, "restore state camera name = $cameraName, camera source = $cameraSource")
        edit_camera_name.setText(cameraName)
        edit_camera_source.setText(cameraSource)
        cameraDests = savedInstanceState?.getParcelableArrayList(
                "camera_dests"
        ) ?: ArrayList()
        cameraDestAdapter = CameraDestAdapter(this, cameraDests!!)
        edit_camera_dests.setAdapter(cameraDestAdapter!!)
    }

    fun onButtonClickSave() {
        Log.i(LOG_TAG, "save camera")
        if (edit_camera_name == null || edit_camera_name.text.isNullOrBlank()) {
            Log.e(LOG_TAG, "camera name is empty")
            return
        }
        if (edit_camera_source == null || edit_camera_source.text.isNullOrBlank()) {
            Log.e(LOG_TAG, "camera source is empty")
            return
        }
        var cameraName = edit_camera_name.text.toString()
        var cameraSource = edit_camera_source.text.toString()
        val camera = Camera(
                name=cameraName,
                source=cameraSource,
                dests=cameraDests!!
            )
        Log.i(LOG_TAG, "set camera to $camera")
        val intent = Intent()
        intent.putExtra("camera", camera)
        setResult(RESULT_OK, intent)
        finish()
    }

    fun onButtonClickAddDest() {
        Log.i(LOG_TAG, "add camera dest")
        if (camera_dest_name == null || camera_dest_name.text.isNullOrBlank()) {
            Log.e(LOG_TAG, "camera dest is empty")
            return
        }
        addCameraDest(CameraDest(camera_dest_name.text.toString()))
    }

    fun onButtonClickEdit(cameraDest: CameraDest) {
        Log.i(LOG_TAG, "edit camera dest $cameraDest")
        val intent = Intent(this, CameraDestEditActivity::class.java)
        intent.putExtra("cameraDest", cameraDest)
        this.startActivityForResult(intent, 0)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            data?.let {
                val cameraDest: CameraDest = it.getParcelableExtra("cameraDest") as CameraDest
                when (requestCode) {
                    0 -> updateCameraDest(cameraDest)
                    1 -> addCameraDest(cameraDest)
                    else -> {
                        Log.e(LOG_TAG, "unknown request code $requestCode")
                    }
                }
            }
        } else {
            Log.e(LOG_TAG, "result code = $resultCode")
        }
    }

    fun updateCameraDest(cameraDest: CameraDest) {
        Log.i(LOG_TAG,"update cameraDest $cameraDest")
        var found = false
        var cameraDestsInternal = cameraDests!!
        for (i in cameraDestsInternal.indices) {
            var existingCameraDest = cameraDestsInternal[i]
            if (existingCameraDest.name == cameraDest.name) {
                existingCameraDest = cameraDest
                cameraDestsInternal[i] = existingCameraDest
                Log.i(LOG_TAG, "update existing camera dest $existingCameraDest")
                found = true
                break
            }
        }
        if (!found) {
            Log.e(LOG_TAG, "cameradest ${cameraDest.name} is not found in existing camera dests")
        }
        cameraDestAdapter!!.notifyDataSetChanged()
    }

    fun addCameraDest(cameraDest: CameraDest) {
        Log.i(LOG_TAG,"add camera dest $cameraDest")
        if (cameraDest in cameraDests!!) {
            Log.i(LOG_TAG, "camera dest $cameraDest in camera dest list $cameraDests")
        } else {
            cameraDests!!.add(cameraDest)
        }
        cameraDestAdapter!!.notifyDataSetChanged()
    }

    fun onButtonClickDelete(cameraDest: CameraDest) {
        Log.i(LOG_TAG, "delete camera dest $cameraDest")
        cameraDests!!.remove(cameraDest)
        cameraDestAdapter!!.notifyDataSetChanged()
    }

    fun onButtonClickDeleteProperty(cameraDest: CameraDest, cameraDestProperty: CameraDestProperty) {
        Log.i(
                LOG_TAG,
                "delete camera dest ${cameraDest.name} property $cameraDestProperty"
        )
        cameraDest.dest_properties.remove(cameraDestProperty)
        cameraDestAdapter!!.notifyDataSetChanged()
    }

    companion object {
        fun px2dip(context: Context, pxValue: Float) : Int {
            var scale = context.getResources().getDisplayMetrics().density
            return (pxValue/scale+0.5f).toInt()
        }

        fun dip2px(context: Context, dipValue: Float) : Int {
            var scale = context.getResources().getDisplayMetrics().density
            return (dipValue*scale+0.5f).toInt()
        }
    }
}
