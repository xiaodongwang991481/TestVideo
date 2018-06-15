package com.example.xiaodong.testvideo

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.util.Log
import android.os.Environment.getExternalStorageDirectory
import android.os.ParcelFileDescriptor
import android.provider.OpenableColumns
import android.widget.Toast
import java.io.FileDescriptor
import java.text.SimpleDateFormat
import java.util.*


class FileManager {
    val context: Context

    constructor(context: Context) {
        this.context = context
    }

    fun getUriRealPath(contentUri: Uri): String? {
        val proj = arrayOf(MediaStore.Images.Media.DATA)
        val cursor = context.contentResolver.query(contentUri, proj, null, null, null) ?: return null
        val column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
        var realPath: String? = null
        if(cursor.moveToFirst()) {
            realPath = cursor.getString(column_index)
        }
        if (realPath == null) {
            Log.i(LOG_TAG, "real path not find in database")
            realPath = contentUri.toString()
        }
        return realPath
    }

    fun getFileDescriptior(contentPath: String) : ParcelFileDescriptor {
        val contentUri = Uri.parse(contentPath)
        return context.contentResolver.openFileDescriptor(contentUri, "r")
    }

    fun getFileDisplayName(contentPath: String): String? {
        val contentUri = Uri.parse(contentPath)
        val cursor = context.contentResolver.query(
                contentUri, null, null,
                null, null, null
        )
        var displayName: String? = null
        try {
            if (cursor != null && cursor.moveToFirst()) {
                displayName = cursor.getString(
                        cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                )
            }
        } catch (e: Exception) {
            Log.e(LOG_TAG, "failed to open cursor")
            Toast.makeText(context, "failed to open cursor", Toast.LENGTH_SHORT).show()
            throw e
        } finally {
            cursor.close()
        }
        return displayName
    }

    public fun getCameraSource(context: Context, data: Intent?) : String? {
        val selectedImageUri = data?.getData()
        Log.i(LOG_TAG, "selected image uri: $selectedImageUri")
        if (selectedImageUri != null) {
            val selectedImagePath = getUriRealPath(selectedImageUri)
            val takeFlags = data.flags and (
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
            context.contentResolver.takePersistableUriPermission(selectedImageUri, takeFlags)

            Log.i(LOG_TAG, "selected image $selectedImagePath from $selectedImagePath")
            return selectedImagePath
        } else {
            Log.e(LOG_TAG, "failed to get source Uri")
            Toast.makeText(
                    context, "failed to get source uri",
                    Toast.LENGTH_SHORT
            ).show()
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

    public fun getCameraDestUrl(context: Context, data: Intent?) : String? {
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
            Toast.makeText(
                    context, "failed to get dest uri",
                    Toast.LENGTH_SHORT
            ).show()
        }
        return null
    }

    companion object {
        private val LOG_TAG = "FileManager"
    }
}