package com.example.xiaodong.testvideo

class FFmpeg {
    /**
     * A native method that is implemented by the 'native-lib' native library,
     * which is packaged with this application.
     */
    external fun stringFromJNI(): String
    external fun decode(
            source: String, dests: Array<String>?, width: Int, height: Int, callback: CallbackFromJNI,
            decode: Boolean, sync: Boolean): Boolean

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