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
            source: String, dests: Array<String>?, bitmapCallback: BitmapCallback?,
            finishCallback: FinishCallback?,
            sourceProperties: Map<String, String>?,
            destsProperties: Array<Map<String, String>?>?,
            fileDescriptors: Map<String, ParcelFileDescriptor>?,
            base_pts: Long=0, sync: Boolean=false
    ): Boolean

    fun decode2(
            camera: Camera, fileManager: FileManager, bitmapCallback: BitmapCallback?=null,
            finishCallback: FinishCallback?=null,
            copyToDests: Boolean = false,
            base_pts: Long=0, sync: Boolean=false
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
        var callbackForBitmap = bitmapCallback
        if (bitmapCallback == null) {
            if (sourceProperties != null) {
                var bitmapCallbackName = sourceProperties.remove("bitmap_callback")
                if(bitmapCallbackName != null) {
                    callbackForBitmap = Class.forName(bitmapCallbackName).asSubclass(
                            BitmapCallback::class.java
                    ).newInstance()
                    callbackForBitmap.init(camera)
                }
            }
        }
        var callbackForFinish = finishCallback
        if (finishCallback == null) {
            if (sourceProperties != null) {
                var finishCallbackName = sourceProperties.remove("finish_callback")
                if (finishCallbackName != null) {
                    callbackForFinish = Class.forName(finishCallbackName).asSubclass(
                            FinishCallback::class.java
                    ).newInstance()
                    callbackForFinish.init(camera)
                }
            }
        }
        return decode(
                source, dests, callbackForBitmap, callbackForFinish,
                sourceProperties?.toMap(), destsProperties,
                fileDescriptors?.toMap(), base_pts, sync
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