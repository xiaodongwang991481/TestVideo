package com.example.xiaodong.testvideo

import android.os.AsyncTask

class VideoProcessTask(activity: VideoPlayActivity): AsyncTask<Void, Void, Void> {
    override fun doInBackground(vararg params: Void?): Void {
    }

    override fun onPostExecute(result: Void?) {
        super.onPostExecute(result)
    }

    override fun onCancelled() {
        super.onCancelled()
    }
}