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
import android.content.ContentUris
import android.os.Build
import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.database.Cursor


class FileManager {
    val context: Context

    constructor(context: Context) {
        this.context = context
    }

    fun getUriRealPath(contentUri: Uri): String? {
        // val proj = arrayOf(MediaStore.Images.Media.DATA)
        // val cursor = context.contentResolver.query(contentUri, proj, null, null, null) ?: return null
        // val column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
        // var realPath: String? = null
        // if(cursor.moveToFirst()) {
        //     realPath = cursor.getString(column_index)
        // }
        var realPath = getPath(contentUri)
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

    /**
     * Method for return file path of Gallery image
     *
     * @param context
     * @param uri
     * @return path of the selected image file from gallery
     */
    @TargetApi(Build.VERSION_CODES.KITKAT)
    @SuppressLint("NewApi")
    fun getPath(uri: Uri): String? {
        // check here to KITKAT or new version
        val isKitKat = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT
        // DocumentProvider
        if (isKitKat && DocumentsContract.isDocumentUri(context, uri)) {
            Log.i(LOG_TAG, "uri $uri is documentUri")
            // ExternalStorageProvider
            if (isExternalStorageDocument(uri)) {
                Log.i(LOG_TAG, "uri $uri is external storage document")
                val docId = DocumentsContract.getDocumentId(uri)
                val split = docId.split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                val type = split[0]
                if ("primary".equals(type, ignoreCase = true)) {
                    Log.i(LOG_TAG, "uri type is primary")
                    return (Environment.getExternalStorageDirectory().toString() + "/"
                            + split[1])
                } else {
                    Log.e(LOG_TAG, "did not find the path of external uri")
                    return null
                }
            } else if (isDownloadsDocument(uri)) {
                Log.i(LOG_TAG, "uri $uri is document uri")
                val id = DocumentsContract.getDocumentId(uri)
                val contentUri = ContentUris.withAppendedId(
                        Uri.parse("content://downloads/public_downloads"),
                        java.lang.Long.valueOf(id))
                return getDataColumn(contentUri, null, null)
            } else if (isMediaDocument(uri)) {
                Log.i(LOG_TAG, "uri $uri is media uri")
                val docId = DocumentsContract.getDocumentId(uri)
                val split = docId.split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                val type = split[0]
                Log.i(LOG_TAG, "media type is $type")
                var contentUri: Uri? = null
                if ("image" == type) {
                    contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                } else if ("video" == type) {
                    contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                } else if ("audio" == type) {
                    contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
                }
                if (contentUri != null) {
                    val selection = "_id=?"
                    val selectionArgs = arrayOf(split[1])
                    return getDataColumn(
                            contentUri, selection,
                            selectionArgs
                    )
                } else {
                    return null
                }
            } // MediaProvider
            // DownloadsProvider
        } else if ("content".equals(uri.scheme, ignoreCase = true)) {
            // Return the remote address
            if (isGooglePhotosUri(uri)) {
                return uri.lastPathSegment
            } else {
                return getDataColumn(uri, null, null)
            }
        } else if ("file".equals(uri.scheme, ignoreCase = true)) {
            return uri.path
        }// File
        // MediaStore (and general)
        return null
    }

    /**
     * Get the value of the data column for this Uri. This is <span id="IL_AD2" class="IL_AD">useful</span> for MediaStore Uris, and other file-based
     * ContentProviders.
     *
     * @param uri
     * The Uri to query.
     * @param selection
     * (Optional) Filter used in the query.
     * @param selectionArgs
     * (Optional) Selection arguments used in the query.
     * @return The value of the _data column, which is typically a file path.
     */
    fun getDataColumn(uri: Uri,
                      selection: String?, selectionArgs: Array<String>?
    ): String? {
        var cursor: Cursor? = null
        val column = "_data"
        val projection = arrayOf(column)
        try {
            cursor = context.contentResolver.query(uri, projection,
                    selection, selectionArgs, null)
            if (cursor != null && cursor!!.moveToFirst()) {
                val index = cursor!!.getColumnIndexOrThrow(column)
                return cursor!!.getString(index)
            }
        } finally {
            if (cursor != null)
                cursor.close()
        }
        return null
    }

    /**
     * @param uri
     * The Uri to check.
     * @return Whether the Uri authority is ExternalStorageProvider.
     */
    fun isExternalStorageDocument(uri: Uri): Boolean {
        return "com.android.externalstorage.documents" == uri
                .authority
    }

    /**
     * @param uri
     * The Uri to check.
     * @return Whether the Uri authority is DownloadsProvider.
     */
    fun isDownloadsDocument(uri: Uri): Boolean {
        return "com.android.providers.downloads.documents" == uri
                .authority
    }

    /**
     * @param uri
     * The Uri to check.
     * @return Whether the Uri authority is MediaProvider.
     */
    fun isMediaDocument(uri: Uri): Boolean {
        return "com.android.providers.media.documents" == uri
                .authority
    }

    /**
     * @param uri
     * The Uri to check.
     * @return Whether the Uri authority is Google Photos.
     */
    fun isGooglePhotosUri(uri: Uri): Boolean {
        return "com.google.android.apps.photos.content" == uri
                .authority
    }

    companion object {
        private val LOG_TAG = "FileManager"
    }
}