package com.example.xiaodong.testvideo

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.widget.ArrayAdapter
import kotlinx.android.synthetic.main.activity_main.*
import android.content.ClipData.Item
import android.content.Intent
import android.util.Log
import android.view.View
import android.widget.AdapterView


class MainActivity : AppCompatActivity() {

    inner class AddCamera : View.OnClickListener {
        override fun onClick(v: View?) {
            this@MainActivity?.let {
                this@MainActivity.onButtonClickAdd()
            }
        }
    }

    inner class ShowCamera : AdapterView.OnItemClickListener {
        override fun onItemClick(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
            this@MainActivity?.let {
                var camera: Camera? = cameras.getItemAtPosition(position) as? Camera
                camera?.let {
                    this@MainActivity.onItemClickShow(camera)
                }
            }
        }
    }

    private var camera_list = ArrayList<Camera>()
    private val LOG_TAG = "mainActivity"
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Example of a call to a native method
        // sample_text.text = stringFromJNI()
        Log.i(LOG_TAG, "activiy initialized")
        initCameraList()
        Log.i(LOG_TAG, "camera list is set")
        add_camera.setOnClickListener(AddCamera())
        cameras.setOnItemClickListener(ShowCamera())
        cameras.setAdapter(CameraAdapter(
                this, camera_list
        ))
    }

    public fun onButtonClickAdd() {
        Log.i(LOG_TAG, "add camera")
        var intent: Intent = Intent(this, CameraEditActivity::class.java)
        this.startActivityForResult(intent, 1)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
    }

    public fun onButtionClickEdit(camera: Camera) {
        Log.i(LOG_TAG, " edit camera ${camera.name}")
        var intent: Intent = Intent(this, CameraEditActivity::class.java)
        this.startActivityForResult(intent, 0)
    }

    public fun onButtonClickDelete(camera: Camera) {
        Log.i(LOG_TAG, "delete camera ${camera.name}")
    }

    public fun onItemClickShow(camera: Camera) {
        Log.i(LOG_TAG, "show camera ${camera.name}")
    }

    private fun initCameraList() {
        camera_list.add(Camera(name="test1", source="rtsp://"))
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
