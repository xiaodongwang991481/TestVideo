package com.example.xiaodong.testvideo

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.util.Log
import android.os.Environment.getExternalStorageDirectory
import java.text.SimpleDateFormat
import java.util.*


class FileManager {

    companion object {
        private val LOG_TAG = "FileManager"

        fun isExternalStorageDocument(uri: Uri): Boolean {
            return "com.android.externalstorage.documents" == uri.authority
        }

        fun isDownloadsDocument(uri: Uri): Boolean {
            return "com.android.providers.downloads.documents" == uri.authority
        }

        fun isMediaDocument(uri: Uri): Boolean {
            return "com.android.providers.media.documents" == uri.authority
        }

        fun getUriRealPath(context: Context, contentUri: Uri): String? {
            val proj = arrayOf(MediaStore.Images.Media.DATA)
            val cursor = context.contentResolver.query(contentUri, proj, null, null, null) ?: return null
            val column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
            cursor.moveToFirst()
            return cursor.getString(column_index)
        }

        public fun getCameraSource(context: Context, data: Intent?) : String? {
            val selectedImageUri = data?.getData()
            Log.i(LOG_TAG, "selected image uri: $selectedImageUri")
            selectedImageUri?.let {
                val selectedImagePath = getUriRealPath(context, it)
                Log.i(LOG_TAG, "selected image $selectedImagePath from $it")
                return selectedImagePath
            } ?: let {
                Log.e(LOG_TAG, "failed to get source Uri")
            }
            return null
        }

        fun getDocumentUriPath(documentUri: Uri) : String? {
            val docUri = DocumentsContract.buildDocumentUriUsingTree(
                    documentUri,
                    DocumentsContract.getTreeDocumentId(documentUri))
            val docId = DocumentsContract.getDocumentId(docUri)
            val split = docId.split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            Log.i(LOG_TAG, "split[0]=${split[0]} split[1]=${split[1]}")
            val path = Environment.getExternalStorageDirectory().absolutePath + "/" + split[1]
            Log.i(LOG_TAG, "got path=$path")
            return path
        }

        public fun getCameraDestUrl(data: Intent?) : String? {
            val selectedDir = data?.getData()
            Log.i(LOG_TAG, "selected dir: $selectedDir")
            selectedDir?.let {
                var selectedPath = getDocumentUriPath(it)
                Log.i(LOG_TAG, "select directory path $selectedPath from $it")
                val filename = SimpleDateFormat("ddMMyy-hhmmss").format(Date())
                Log.i(LOG_TAG, "generated filename=$filename")
                return selectedPath + "/" + filename + ".mp4"
            } ?: let {
                Log.e(LOG_TAG, "failed to get dest Uri")
            }
            return null
        }
    }
}