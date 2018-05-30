package com.example.xiaodong.testvideo

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.util.Log
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.PersistableBundle
import android.provider.DocumentsContract
import android.view.View
import android.widget.AdapterView
import kotlinx.android.synthetic.main.activity_camera_dest_edit.*
import kotlinx.android.synthetic.main.activity_camera_edit.*

class CameraDestEditActivity : AppCompatActivity() {

    private var cameraDestProperties: ArrayList<CameraDestProperty>? = null
    private var cameraDestPropertyAdapter: CameraDestPropertyAdapter? = null

    inner class SaveCameraDest: View.OnClickListener {
        override fun onClick(v: View?) {
            this@CameraDestEditActivity.onButtonClickSave()
        }
    }

    inner class AddCameraDestProperty: View.OnClickListener {
        override fun onClick(v: View?) {
            this@CameraDestEditActivity.onButtonClickAddProperty()
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

    fun getDocumentUriPath(documentUri: Uri) : String? {
        val docUri = DocumentsContract.buildDocumentUriUsingTree(
                documentUri,
                DocumentsContract.getTreeDocumentId(documentUri)
        )
        val cursor = contentResolver.query(
                docUri,
                arrayOf(
                        DocumentsContract.Document.COLUMN_DISPLAY_NAME
                ), null, null, null
        )
        val column_index = cursor.getColumnIndexOrThrow(
                DocumentsContract.Document.COLUMN_DISPLAY_NAME
        )
        cursor.moveToFirst()
        return cursor.getString(column_index)
    }

    fun getCameraDestUrl(data: Intent?) : String? {
        val selectedImageDir = data?.getData()
        Log.i(LOG_TAG, "selected image dir: $selectedImageDir")
        selectedImageDir?.let {
            return getDocumentUriPath(selectedImageDir)
        } ?: let {
            Log.e(LOG_TAG, "failed to get dest Uri")
        }
        return null
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
        setContentView(R.layout.activity_camera_dest_edit)
        Log.i(LOG_TAG, "camera dest activity initialized with state = $savedInstanceState")
        if (intent.hasExtra("cameraDest")) {
            var cameraDest: CameraDest = intent.getParcelableExtra("cameraDest")
            Log.i(LOG_TAG, "get camera dest $cameraDest")
            var cameraDestName = cameraDest.name
            var cameraDestUrl = cameraDest.url
            edit_camera_dest_name.setText(cameraDestName)
            edit_camera_dest_name.focusable = View.NOT_FOCUSABLE
            edit_camera_dest_name.setEnabled(false)
            edit_camera_dest_name.setTextColor(Color.GRAY)
            edit_camera_dest_url.setText(cameraDestUrl)
            cameraDestProperties = cameraDest.dest_properties
        } else {
            cameraDestProperties = ArrayList()
        }
        cameraDestPropertyAdapter = CameraDestPropertyAdapter(this, cameraDestProperties!!)
        var header = layoutInflater.inflate(R.layout.camera_dest_property_header, camera_dest_properties, false) as View
        camera_dest_properties.addHeaderView(header)
        var footer = layoutInflater.inflate(R.layout.listview_footer, camera_dest_properties, false) as View
        camera_dest_properties.addFooterView(footer)
        camera_dest_properties.setAdapter(cameraDestPropertyAdapter!!)
        add_camera_dest_property.setOnClickListener(AddCameraDestProperty())
        edit_camera_dest_save.setOnClickListener(SaveCameraDest())
        edit_camera_dest_url.setOnLongClickListener(SelectDirectory())
    }

    override fun onSaveInstanceState(outState: Bundle?) {
        outState?.let {
            super.onSaveInstanceState(outState)
        }
        var cameraDestName = edit_camera_dest_name.text.toString()
        var cameraDestUrl = edit_camera_dest_url.text.toString()
        Log.i(LOG_TAG, "save state camera dest $cameraDestName=$cameraDestUrl")
        outState?.let {
            outState.putString("camera_dest_name", cameraDestName)
            outState.putString("camera_dest_url", cameraDestUrl)
            cameraDestProperties?.let {
                outState.putParcelableArrayList("camera_dest_properties", cameraDestProperties)
            }
        }
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle?) {
        savedInstanceState?.let {
            super.onRestoreInstanceState(savedInstanceState)
        }
        var cameraDestName = savedInstanceState?.getString(
                "camera_dest_name"
        ) ?: ""
        var cameraDestUrl = savedInstanceState?.getString(
                "camera_dest_url"
        ) ?: ""
        Log.i(LOG_TAG, "restore state camera dest $cameraDestName=$cameraDestUrl")
        edit_camera_dest_name.setText(cameraDestName)
        edit_camera_dest_url.setText(cameraDestUrl)
        cameraDestProperties = savedInstanceState?.getParcelableArrayList(
                "camera_dest_properties"
        ) ?: ArrayList()
        cameraDestPropertyAdapter = CameraDestPropertyAdapter(this, cameraDestProperties!!)
        camera_dest_properties.setAdapter(cameraDestPropertyAdapter!!)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            data?.let {
                when (requestCode) {
                    CameraEditActivity.REQUEST_UPLOAD_GALLERY_VIDEO -> {
                        var url = getCameraDestUrl(data)
                        url?.let {
                            edit_camera_dest_url.setText(it)
                        }
                    }
                    else -> {
                        Log.e(LOG_TAG, "unknown request code $requestCode")
                    }
                }
            }
        } else {
            Log.e(LOG_TAG, "result code = $resultCode")
        }
    }

    fun onButtonClickSave() {
        if (edit_camera_dest_name == null || edit_camera_dest_name.text.isNullOrBlank()) {
            Log.e(LOG_TAG, "camera dest name is empty")
            return
        }
        if (edit_camera_dest_url == null || edit_camera_dest_url.text.isNullOrBlank()) {
            Log.e(LOG_TAG, "camera dest url is empty")
            return
        }
        var cameraDestName = edit_camera_dest_name.text.toString()
        var cameraDestUrl = edit_camera_dest_url.text.toString()
        val cameraDest = CameraDest(
                name=cameraDestName,
                url=cameraDestUrl,
                dest_properties=cameraDestProperties!!
        )
        Log.i(LOG_TAG, "set camera dest to $cameraDest")
        val intent = Intent()
        intent.putExtra("cameraDest", cameraDest)
        setResult(RESULT_OK, intent)
        finish()
    }

    fun onButtonClickAddProperty() {
        Log.i(LOG_TAG, "add camera dest property")
        if (
                add_camera_dest_property_name == null ||
                add_camera_dest_property_name.text.isNullOrBlank()
        ) {
            Log.e(LOG_TAG, "camera dest property name is empty")
            return
        }
        if (
                add_camera_dest_property_value == null ||
                add_camera_dest_property_value.text.isNullOrBlank()
        ) {
            Log.e(LOG_TAG, "camera dest property value is empty")
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
        Log.i(LOG_TAG,"add camera dest property $cameraDestProperty")
        if (cameraDestProperty in cameraDestProperties!!) {
            Log.i(LOG_TAG, "camera dest property $cameraDestProperty in camera dest properties $cameraDestProperties")
        } else {
            cameraDestProperties!!.add(cameraDestProperty)
        }
        cameraDestPropertyAdapter!!.notifyDataSetChanged()
    }

    fun onButtonClickDelete(cameraDestProperty: CameraDestProperty) {
        Log.i(LOG_TAG, "delete cameraDestProperty $cameraDestProperty")
        cameraDestProperties!!.remove(cameraDestProperty)
        cameraDestPropertyAdapter!!.notifyDataSetChanged()
    }

    companion object {
        private val LOG_TAG = "CameraDestEditActivity"
        private val REQUEST_UPLOAD_GALLERY_VIDEO =  1
    }
 }
