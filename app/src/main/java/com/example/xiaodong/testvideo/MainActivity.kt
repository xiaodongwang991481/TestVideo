package com.example.xiaodong.testvideo

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import kotlinx.android.synthetic.main.activity_main.*
import android.content.Intent
import android.os.PersistableBundle
import android.util.Log
import android.view.View
import android.widget.AdapterView


class MainActivity : AppCompatActivity() {

    inner class AddCamera : View.OnClickListener {
        override fun onClick(v: View?) {
            this@MainActivity.onButtonClickAdd()
        }
    }

    inner class ShowCamera : AdapterView.OnItemClickListener {
        override fun onItemClick(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
            val camera: Camera? = cameras.getItemAtPosition(position) as? Camera
            camera?.let {
                this@MainActivity.onItemClickShow(camera)
            }
        }
    }

    private var cameraList = ArrayList<Camera>()
    private var camerasAdapter = CameraAdapter(
            this, cameraList
    )
    private val LOGTAG = "mainActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        onRestoreInstanceState(savedInstanceState)
        // Example of a call to a native method
        // sample_text.text = stringFromJNI()
        var address = Integer.toHexString(System.identityHashCode(this))
        Log.i(LOGTAG, "main activiy initialized with state = $savedInstanceState on ${address}")
        add_camera.setOnClickListener(AddCamera())
        cameras.setOnItemClickListener(ShowCamera())
    }

    fun onButtonClickAdd() {
        Log.i(LOGTAG, "add camera")
        val intent = Intent(this, CameraEditActivity::class.java)
        this.startActivityForResult(intent, 1)
    }

    override fun onSaveInstanceState(outState: Bundle?) {
        outState?.let {
            super.onSaveInstanceState(outState)
        }
        Log.i(LOGTAG, "save state")
        outState?.putParcelableArrayList("cameras", cameraList)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle?) {
        savedInstanceState?.let {
            super.onRestoreInstanceState(savedInstanceState)
        }
        Log.i(LOGTAG, "restore state")
        cameraList = savedInstanceState?.getParcelableArrayList("cameras") ?: getInitialCameraList()
        camerasAdapter = CameraAdapter(
                this, cameraList
        )
        cameras.setAdapter(camerasAdapter)
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

    fun updateCamera(camera: Camera) {
        Log.i(LOGTAG,"update camera ${camera.name} = ${camera.source}")
        var found = false
        for (i in cameraList.indices) {
            var existingCamera = cameraList[i]
            if (existingCamera.name == camera.name) {
                existingCamera = camera
                cameraList[i] = existingCamera
                Log.i(LOGTAG, "update existing camera ${existingCamera.name} = ${existingCamera.source}")
                found = true
                break
            }
        }
        if (!found) {
            Log.e(LOGTAG, "camera ${camera.name} is not found in existing cameras")
        }
        camerasAdapter.notifyDataSetChanged()
    }

    fun addCamera(camera: Camera) {
        Log.i(LOGTAG,"add camera ${camera.name} = ${camera.source}")
        cameraList.add(camera)
        camerasAdapter.notifyDataSetChanged()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        data?.let {
            val camera: Camera? = it.getParcelableExtra("camera") as? Camera
            if (camera != null) {
                when (requestCode) {
                    0 -> updateCamera(camera)
                    1 -> addCamera(camera)
                    else -> {
                        Log.e(LOGTAG, "unknown request code $requestCode")
                    }
                }
            } else {
                Log.e(LOGTAG, "camera is null")
            }
        }
    }

    fun onButtonClickEdit(camera: Camera) {
        Log.i(LOGTAG, " edit camera ${camera.name}")
        val intent = Intent(this, CameraEditActivity::class.java)
        intent.putExtra("camera", camera)
        this.startActivityForResult(intent, 0)
    }

    fun onButtonClickDelete(camera: Camera) {
        Log.i(LOGTAG, "delete camera ${camera.name}")
        cameraList.remove(camera)
        camerasAdapter.notifyDataSetChanged()
    }

    fun onItemClickShow(camera: Camera) {
        Log.i(LOGTAG, "show camera ${camera.name}")
    }

    private fun getInitialCameraList() : ArrayList<Camera> {
        var cameraList = ArrayList<Camera>()
        cameraList.add(Camera(name="test1", source="rtsp://"))
        return cameraList
    }
    /**
     * A native method that is implemented by the 'native-lib' native library,
     * which is packaged with this application.
     */
    external fun stringFromJNI(): String

    companion object {

        // Used to load the 'native-lib' library on application startup.
        init {
            // System.loadLibrary("avutil")
            System.loadLibrary("native-lib")
        }
    }
}
