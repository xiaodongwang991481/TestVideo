package com.example.xiaodong.testvideo

import android.os.ParcelFileDescriptor
import java.io.FileDescriptor

class FFmpeg {
    /**
     * A native method that is implemented by the 'native-lib' native library,
     * which is packaged with this application.
     */
    external fun stringFromJNI(): String
    external fun decode(
            source: String, dests: Array<String>?, callback: CallbackFromJNI?,
            sourceProperties: Map<String, String>?,
            destsProperties: Array<Map<String, String>?>?,
            fileDescriptors: Map<String, ParcelFileDescriptor>?,
            base_pts: Long
    ): Boolean

    fun decode2(
            camera: Camera, fileManager: FileManager, callback: CallbackFromJNI?=null,
            copyToDests: Boolean = false,
            base_pts: Long=0
    ): Boolean {
        var source = camera.source
        var sourceProperties: MutableMap<String, String>? = null
        var fileDescriptors: MutableMap<String, ParcelFileDescriptor>? = mutableMapOf()
        if (camera.source_properties.size > 0) {
            sourceProperties = mutableMapOf()
            for (property in camera.source_properties) {
                sourceProperties.put(property.name, property.value)
            }
        }
        if (source.startsWith("content://")) {
            fileDescriptors!!.put(source, fileManager.getFileDescriptior(source))
        }
        var dests: Array<String>? = null
        var destsProperties: Array<Map<String, String>? >? = null
        if (copyToDests) {
            var camera_dests = ArrayList<String>()
            var camera_dests_properties = ArrayList<Map<String, String>? >()
            for (dest in camera.dests) {
                camera_dests.add(dest.url)
                var dest_properties: MutableMap<String, String>? = null
                if (dest.dest_properties.size > 0) {
                    dest_properties = mutableMapOf()
                    for (property in dest.dest_properties) {
                        dest_properties.put(property.name, property.value)
                    }
                }
                camera_dests_properties.add(dest_properties?.toMap())
                if (dest.url.startsWith("content://")) {
                    fileDescriptors!!.put(dest.url, fileManager.getFileDescriptior(dest.url))
                }
            }
            dests = camera_dests.toTypedArray()
            destsProperties = camera_dests_properties.toTypedArray()
        }
        if (fileDescriptors!!.size == 0) {
            fileDescriptors = null
        }
        return decode(
                source, dests, callback, sourceProperties?.toMap(), destsProperties,
                fileDescriptors?.toMap(), base_pts
        )
    }

    companion object {
        @JvmStatic
        external fun initJNI(): Unit

        private val singleton = FFmpeg()

        @Synchronized fun getInstance(): FFmpeg {
            return singleton
        }

        // Used to load the 'native-lib' library on application startup.
        init {
            // System.loadLibrary("avutil")
            System.loadLibrary("native-lib")
            initJNI()
        }
    }
}