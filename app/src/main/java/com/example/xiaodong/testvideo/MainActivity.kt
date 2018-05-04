package com.example.xiaodong.testvideo

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.widget.ArrayAdapter
import kotlinx.android.synthetic.main.activity_main.*
import android.content.ClipData.Item
import android.util.Log



class MainActivity : AppCompatActivity() {

    private var camera_list = ArrayList<String>()
    private val LOG_TAG = "mainActivity"
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Example of a call to a native method
        // sample_text.text = stringFromJNI()
        Log.i(LOG_TAG, "activiy initialized")
        initCameraList()
        Log.i(LOG_TAG, "camera list is set")
        cameras.setAdapter(ArrayAdapter<String>(this, R.layout.cameras_layout, camera_list))
    }

    private fun initCameraList() {
        camera_list.add("test1")
        camera_list.add("test2")
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
