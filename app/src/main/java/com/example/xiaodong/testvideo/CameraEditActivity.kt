package com.example.xiaodong.testvideo

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.PersistableBundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.AdapterView
import kotlinx.android.synthetic.main.activity_camera_dest_edit.*
import kotlinx.android.synthetic.main.activity_camera_edit.*
import android.support.v4.app.ActivityCompat.startActivityForResult
import android.provider.DocumentsContract
import android.content.ContentResolver
import android.provider.DocumentsContract.Document.COLUMN_MIME_TYPE
import android.widget.Toast
import java.io.File
import java.net.URI


class CameraEditActivity : AppCompatActivity() {

    private var cameraDests: ArrayList<CameraDest>? = null
    private var cameraSourceProperties: ArrayList<CameraSourceProperty>? = null
    var groupIndicatorWidth: Int = 50
    var childIndicatorWidth: Int = 50
    private var cameraDestAdapter: CameraDestAdapter? = null
    private var cameraSourcePropertyAdapter: CameraSourcePropertyAdapter? = null

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

    inner class AddCameraSourceProperty: View.OnClickListener {
        override fun onClick(v: View?) {
            this@CameraEditActivity.onButtonClickAddSourceProperty()
        }
    }

    inner class EditCameraDest : AdapterView.OnItemLongClickListener {
        override fun onItemLongClick(parent: AdapterView<*>?, view: View?, position: Int, id: Long): Boolean {
            var cameraDest = edit_camera_dests.getItemAtPosition(position) as CameraDest
            Log.i(LOG_TAG, "item $position long click on $cameraDest")
            this@CameraEditActivity.onButtonClickEditDest(cameraDest)
            return true
        }
    }

    inner class SelectVideo : View.OnLongClickListener {
        override fun onLongClick(v: View?): Boolean {
            val intent = Intent()
            intent.type = "video/*"
            intent.action = Intent.ACTION_GET_CONTENT
            startActivityForResult(
                    Intent.createChooser(intent, "Choose a file"),
                    REQUEST_TAKE_GALLERY_VIDEO
            )
            return true
        }
    }

    inner class SelectDirectory : View.OnLongClickListener {
        override fun onLongClick(v: View?): Boolean {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
            intent.addCategory(Intent.CATEGORY_DEFAULT)
            startActivityForResult(
                    Intent.createChooser(intent, "Choose directory"),
                    REQUEST_UPLOAD_GALLERY_VIDEO
            )
            return true
        }
    }

    fun onButtonClickAddSourceProperty() {
        Log.i(LOG_TAG, "add camera source property")
        if (
                add_camera_source_property_name == null ||
                add_camera_source_property_name.text.isNullOrBlank()
        ) {
            Log.e(LOG_TAG, "camera source property name is empty")
            return
        }
        if (
                add_camera_source_property_value == null ||
                add_camera_source_property_value.text.isNullOrBlank()
        ) {
            Log.e(LOG_TAG, "camera source property value is empty")
            return
        }
        addCameraSourceProperty(
                CameraSourceProperty(
                        add_camera_source_property_name.text.toString(),
                        add_camera_source_property_value.text.toString()
                )
        )
    }

    fun addCameraSourceProperty(cameraSourceProperty: CameraSourceProperty) {
        Log.i(LOG_TAG,"add camera source property $cameraSourceProperty")
        if (cameraSourceProperty in cameraSourceProperties!!) {
            Log.i(
                    LOG_TAG,
                    "camera source property $cameraSourceProperty " +
                            "is already in camera source properties $cameraSourceProperties"
            )
        } else {
            cameraSourceProperties!!.add(cameraSourceProperty)
        }
        cameraSourcePropertyAdapter!!.notifyDataSetChanged()
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
            cameraSourceProperties = camera.source_properties
            cameraDests = camera.dests
        } else {
            cameraSourceProperties = ArrayList()
            cameraDests = ArrayList()
        }
        cameraSourcePropertyAdapter = CameraSourcePropertyAdapter(this, cameraSourceProperties!!)
        var header = layoutInflater.inflate(
                R.layout.camera_source_property_header, camera_source_properties, false
        ) as View
        camera_source_properties.addHeaderView(header)
        var footer = layoutInflater.inflate(
                R.layout.listview_footer, camera_source_properties, false
        ) as View
        camera_source_properties.addFooterView(footer)
        camera_source_properties.setAdapter(cameraSourcePropertyAdapter!!)
        add_camera_source_property.setOnClickListener(AddCameraSourceProperty())
        cameraDestAdapter = CameraDestAdapter(this, cameraDests!!)
        var dest_header = layoutInflater.inflate(
                R.layout.camera_dest_header, edit_camera_dests, false
        ) as View
        edit_camera_dests.addHeaderView(dest_header)
        var dest_footer = layoutInflater.inflate(
                R.layout.listview_footer, edit_camera_dests, false
        ) as View
        edit_camera_dests.addFooterView(dest_footer)
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
        edit_camera_source.setOnLongClickListener(SelectVideo())
        camera_dest_url.setOnLongClickListener(SelectDirectory())
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
            cameraSourceProperties?.let {
                outState.putParcelableArrayList("camera_source_properties", it)
            }
            cameraDests?.let {
                outState.putParcelableArrayList("camera_dests", it)
            }
        }
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle?) {
        savedInstanceState?.let {
            super.onRestoreInstanceState(savedInstanceState)
        }
        var cameraName = savedInstanceState?.getString("name") ?: ""
        var cameraSource = savedInstanceState?.getString("source") ?: ""
        Log.i(LOG_TAG, "restore state $cameraName=$cameraSource")
        edit_camera_name.setText(cameraName)
        edit_camera_source.setText(cameraSource)
        cameraSourceProperties = savedInstanceState?.getParcelableArrayList(
                "camera_source_properties"
        ) ?: ArrayList()
        cameraDests = savedInstanceState?.getParcelableArrayList(
                "camera_dests"
        ) ?: ArrayList()
        cameraSourcePropertyAdapter = CameraSourcePropertyAdapter(this, cameraSourceProperties!!)
        cameraDestAdapter = CameraDestAdapter(this, cameraDests!!)
        camera_source_properties.setAdapter(cameraSourcePropertyAdapter!!)
        edit_camera_dests.setAdapter(cameraDestAdapter!!)
    }

    fun onButtonClickSave() {
        Log.i(LOG_TAG, "save camera")
        if (edit_camera_name == null || edit_camera_name.text.isNullOrBlank()) {
            Log.e(LOG_TAG, "camera name is empty")
            Toast.makeText(
                    this, "camera name is empty",
                    Toast.LENGTH_SHORT
            ).show()
            return
        }
        if (edit_camera_source == null || edit_camera_source.text.isNullOrBlank()) {
            Log.e(LOG_TAG, "camera source is empty")
            Toast.makeText(
                    this, "camera source is empty",
                    Toast.LENGTH_SHORT
            ).show()
            return
        }
        var cameraName = edit_camera_name.text.toString()
        var cameraSource = edit_camera_source.text.toString()
        val camera = Camera(
                name=cameraName,
                source=cameraSource,
                source_properties=cameraSourceProperties!!,
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
            Log.e(LOG_TAG, "camera dest name is empty")
            Toast.makeText(
                    this, "camera dest name is empty",
                    Toast.LENGTH_SHORT
            ).show()
            return
        }
        if (camera_dest_url == null || camera_dest_url.text.isNullOrBlank()) {
            Log.e(LOG_TAG, "camera dest url is empty")
            Toast.makeText(
                    this, "camera dest url is empty",
                    Toast.LENGTH_SHORT
            ).show()
            return
        }
        addCameraDest(CameraDest(
                name=camera_dest_name.text.toString(),
                url=camera_dest_url.text.toString()
        ))
    }

    fun onButtonClickEditDest(cameraDest: CameraDest) {
        Log.i(LOG_TAG, "edit camera dest $cameraDest")
        val intent = Intent(this, CameraDestEditActivity::class.java)
        intent.putExtra("cameraDest", cameraDest)
        this.startActivityForResult(intent, REQUEST_UPDATE_CAMERA_DEST)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            data?.let {
                var cameraDest: CameraDest? = null
                if (it.hasExtra("cameraDest")) {
                    cameraDest = it.getParcelableExtra(
                            "cameraDest"
                    ) as CameraDest
                }
                when (requestCode) {
                    REQUEST_UPDATE_CAMERA_DEST -> updateCameraDest(cameraDest!!)
                    REQUEST_ADD_CAMERA_DEST -> addCameraDest(cameraDest!!)
                    REQUEST_TAKE_GALLERY_VIDEO -> {
                        var source = FileManager.getCameraSource(this, it)
                        source?.let {
                            edit_camera_source.setText(it)
                        }
                    }
                    REQUEST_UPLOAD_GALLERY_VIDEO -> {
                        var url = FileManager.getCameraDestUrl(this, it)
                        url?.let {
                            camera_dest_url.setText(it)
                        }
                    }
                    else -> {
                        Log.e(LOG_TAG, "unknown request code $requestCode")
                        Toast.makeText(
                                this, "unknown request code $requestCode",
                                Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        } else {
            Log.e(LOG_TAG, "result code = $resultCode")
            Toast.makeText(
                    this, "result code = $resultCode",
                    Toast.LENGTH_SHORT
            ).show()
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
            Toast.makeText(
                    this, "cameradest ${cameraDest.name} is not found in existing camera dests",
                    Toast.LENGTH_SHORT
            ).show()
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

    fun onButtonClickDeleteDest(cameraDest: CameraDest) {
        Log.i(LOG_TAG, "delete camera dest $cameraDest")
        cameraDests!!.remove(cameraDest)
        cameraDestAdapter!!.notifyDataSetChanged()
    }

    fun onButtonClickDeleteDestProperty(cameraDest: CameraDest, cameraDestProperty: CameraDestProperty) {
        Log.i(
                LOG_TAG,
                "delete camera dest ${cameraDest.name} property $cameraDestProperty"
        )
        cameraDest.dest_properties.remove(cameraDestProperty)
        cameraDestAdapter!!.notifyDataSetChanged()
    }

    fun onButtonClickDeleteSourceProperty(cameraSourceProperty: CameraSourceProperty) {
        Log.i(
                LOG_TAG,
                "delete camera source property $cameraSourceProperty"
        )
        cameraSourceProperties!!.remove(cameraSourceProperty)
        cameraSourcePropertyAdapter!!.notifyDataSetChanged()
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
        private val LOG_TAG = "CameraEditActivity"
        private val REQUEST_ADD_CAMERA_DEST = 1
        private var REQUEST_UPDATE_CAMERA_DEST = 2
        private val REQUEST_TAKE_GALLERY_VIDEO = 3
        private val REQUEST_UPLOAD_GALLERY_VIDEO = 4
    }
}
