package com.example.xiaodong.testvideo

import android.Manifest
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import kotlinx.android.synthetic.main.activity_main.*
import android.content.Intent
import android.os.PersistableBundle
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.Manifest.permission
import android.Manifest.permission.SEND_SMS
import android.app.Notification
import android.app.PendingIntent
import android.content.pm.PackageManager
import android.content.Context.NOTIFICATION_SERVICE
import android.app.NotificationManager
import android.app.TaskStackBuilder
import android.content.Context
import android.support.v4.app.NotificationCompat
import android.widget.Toast


class MainActivity : AppCompatActivity() {

    inner class AddCamera : View.OnClickListener {
        override fun onClick(v: View?) {
            this@MainActivity.onButtonClickAdd()
        }
    }

    inner class SaveCameras : View.OnClickListener {
        override fun onClick(v: View?) {
            this@MainActivity.onButtonClickSave()
        }
    }

    inner class ShowCamera : AdapterView.OnItemClickListener {
        override fun onItemClick(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
            val camera: Camera? = cameras.getItemAtPosition(position) as? Camera
            Log.i(LOG_TAG, "item click on $camera")
            this@MainActivity.onItemClickShow(camera!!)
        }
    }

    inner class EditCamera : AdapterView.OnItemLongClickListener {
        override fun onItemLongClick(parent: AdapterView<*>?, view: View?, position: Int, id: Long): Boolean {
            val camera: Camera? = cameras.getItemAtPosition(position) as? Camera
            Log.i(LOG_TAG, "item $position long click on $camera")
            this@MainActivity.onButtonClickEdit(camera!!)
            return true
        }
    }

    inner class StartService : View.OnClickListener {
        override fun onClick(v: View?) {
            val intent = Intent(this@MainActivity, CameraService::class.java)
            Log.i(LOG_TAG, "start service")
            if(startForegroundService(intent) == null) {
                Log.e(LOG_TAG, "failed to start service")
                Toast.makeText(
                        this@MainActivity, "failed to start service",
                        Toast.LENGTH_SHORT
                ).show()
            }

        }
    }

    inner class StopService : View.OnClickListener {
        override fun onClick(v: View?) {
            val intent = Intent(this@MainActivity, CameraService::class.java)
            Log.i(LOG_TAG, "stop service")
            if(!stopService(intent)) {
                Log.e(LOG_TAG, "failed to stop service")
                Toast.makeText(
                        this@MainActivity, "failed to stop service",
                        Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private var dbHelper: DBOpenHelper? = null
    private var cameraList: ArrayList<Camera>? = null
    private var camerasAdapter: CameraAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        requestPermissions()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        // Example of a call to a native method
        // sample_text.text = stringFromJNI()
        Log.i(LOG_TAG, "main activiy initialized with state = $savedInstanceState")
        dbHelper = DBOpenHelper(applicationContext, "my.db", null, 1)
        cameraList = getInitialCameraList()
        camerasAdapter = CameraAdapter(
                this, cameraList!!
        )
        var header = layoutInflater.inflate(R.layout.camera_header, cameras, false)
        cameras.addHeaderView(header)
        var footer = layoutInflater.inflate(R.layout.listview_footer, cameras, false)
        cameras.addFooterView(footer)
        cameras.setAdapter(camerasAdapter!!)
        cameras.smoothScrollToPosition(0, 0)
        add_camera.setOnClickListener(AddCamera())
        cameras.setOnItemClickListener(ShowCamera())
        cameras.setOnItemLongClickListener(EditCamera())
        save_cameras.setOnClickListener(SaveCameras())
        start_service.setOnClickListener(StartService())
        stop_service.setOnClickListener(StopService())
    }

    fun onButtonClickAdd() {
        Log.i(LOG_TAG, "add camera")
        val intent = Intent(this, CameraEditActivity::class.java)
        this.startActivityForResult(intent, REQUEST_ADD_CAMERA)
    }

    fun onButtonClickSave() {
        Log.i(LOG_TAG, "save cameras: $cameraList")
        dbHelper!!.updateAllCameras(cameraList!!)
    }

    override fun onSaveInstanceState(outState: Bundle?) {
        outState?.let {
            super.onSaveInstanceState(outState)
        }
        Log.i(LOG_TAG, "save state")
        cameraList?.let {
            outState?.putParcelableArrayList("cameras", cameraList)
        }
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle?) {
        savedInstanceState?.let {
            super.onRestoreInstanceState(savedInstanceState)
        }
        Log.i(LOG_TAG, "restore state")
        cameraList = savedInstanceState?.getParcelableArrayList("cameras") ?: getInitialCameraList()
        camerasAdapter = CameraAdapter(
                this, cameraList!!
        )
        cameras.setAdapter(camerasAdapter!!)
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

    fun updateCamera(camera: Camera) {
        Log.i(LOG_TAG,"update camera $camera")
        var found = false
        var cameraListInternal = cameraList!!
        for (i in cameraListInternal.indices) {
            var existingCamera = cameraListInternal[i]
            if (existingCamera.name == camera.name) {
                existingCamera = camera
                cameraListInternal[i] = existingCamera
                Log.i(LOG_TAG, "update existing camera $existingCamera")
                found = true
                break
            }
        }
        if (!found) {
            Log.e(LOG_TAG, "camera ${camera.name} is not found in existing cameras")
            Toast.makeText(
                    this, "camera ${camera.name} is not found in existing cameras",
                    Toast.LENGTH_SHORT
            ).show()
        }
        camerasAdapter!!.notifyDataSetChanged()
    }

    fun addCamera(camera: Camera) {
        Log.i(LOG_TAG,"add camera $camera")
        if (camera in cameraList!!) {
            Log.i(LOG_TAG, "camera $camera in camera list $cameraList")
        } else {
            cameraList!!.add(camera)
        }
        camerasAdapter!!.notifyDataSetChanged()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK) {
            data?.let {
                val camera: Camera = it.getParcelableExtra("camera") as Camera
                when (requestCode) {
                    REQUEST_UPDATE_CAMERA -> updateCamera(camera)
                    REQUEST_ADD_CAMERA -> addCamera(camera)
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

    fun onButtonClickEdit(camera: Camera) {
        Log.i(LOG_TAG, " edit camera $camera")
        val intent = Intent(this, CameraEditActivity::class.java)
        intent.putExtra("camera", camera)
        this.startActivityForResult(intent, REQUEST_UPDATE_CAMERA)
    }

    fun onButtonClickDelete(camera: Camera) {
        Log.i(LOG_TAG, "delete camera $camera")
        cameraList!!.remove(camera)
        camerasAdapter!!.notifyDataSetChanged()
    }

    fun onItemClickShow(camera: Camera) {
        Log.i(LOG_TAG, "show camera $camera")
        val intent = Intent(this, VideoPlayActivity::class.java)
        intent.putExtra("camera", camera)
        this.startActivity(intent)
    }

    private fun getInitialCameraList() : ArrayList<Camera> {
        return dbHelper!!.getAllCameras()
    }

    fun requestPermissions() {
        var permissions = ArrayList<String>()
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            for (permission in arrayOf(
                    Manifest.permission.CAMERA,
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.INSTALL_PACKAGES,
                    Manifest.permission.INTERNET,
                    Manifest.permission.CAPTURE_VIDEO_OUTPUT

            )) {
                Log.i("permission", "permission denied to $permission - requesting it")
                permissions.add(permission)

            }
            requestPermissions(permissions.toTypedArray(), PERMISSION_REQUEST_CODE)
        }
    }

    companion object {
        private val PERMISSION_REQUEST_CODE = 1
        private val LOG_TAG = "mainActivity"
        private val REQUEST_UPDATE_CAMERA = 1
        private val REQUEST_ADD_CAMERA = 2
    }

}
