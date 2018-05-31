package com.example.xiaodong.testvideo

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log

class DBOpenHelper(
        context: Context?, name: String, factory: SQLiteDatabase.CursorFactory?, version: Int
) : SQLiteOpenHelper(context, name, factory, version) {

    override fun onCreate(db: SQLiteDatabase?) {
        db?.let {
            db.execSQL(
                    "CREATE TABLE if not exists camera(name text primary key, source text not null)"
            )
            db.execSQL(
                    "CREATE TABLE if not exists camera_dest(" +
                            "name text not null, url text not null, camera_name text not null, " +
                            "primary key (name, camera_name), " +
                            " foreign key (camera_name) references camera (name) on delete cascade on update cascade)"
            )
            db.execSQL(
                    "CREATE TABLE if not exists camera_source_property(" +
                            "name text not null, value text not null, camera_name text not null, " +
                            "primary key(name, camera_name), foreign key (camera_name) " +
                            "references camera (name) on delete cascade on update cascade)"

            )
            db.execSQL(
                    "CREATE table if not exists camera_dest_property(" +
                            "name text not null, value text not null, dest_name text not null, camera_name text not null, " +
                            "primary key (name, dest_name, camera_name), " +
                            "foreign key (dest_name, camera_name) " +
                            "references camera_dest (name, camera_name) on delete cascade on update cascade)"
            )
        }
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        db?.let {
            db.execSQL("drop table if exists camera_dest_property")
            db.execSQL("drop table if exists camera_dest")
            db.execSQL("drop table if exists camera_source_property")
            db.execSQL("drop table if exists camera")
        }
        onCreate(db)
    }

    fun getAllCameras(): ArrayList<Camera> {
        var cameras = ArrayList<Camera>()
        var db = readableDatabase
        db.beginTransactionNonExclusive()
        try {
            var cursor = db.query(
                    "camera", null, null,
                    null, null, null, null
            )
            try {
                while (cursor.moveToNext()) {
                    var name = cursor.getString(cursor.getColumnIndex("name"))
                    var source = cursor.getString(cursor.getColumnIndex("source"))
                    var sourcePropertyCursor = db.query(
                            "camera_source_property", null, "camera_name=?",
                            arrayOf(name), null, null, null
                    )
                    var sourceProperties = ArrayList<CameraSourceProperty>()
                    try {
                        while (sourcePropertyCursor.moveToNext()) {
                            var sourcePropertyName = sourcePropertyCursor.getString(
                                    sourcePropertyCursor.getColumnIndex("name")
                            )
                            var sourcePropertyValue = sourcePropertyCursor.getString(
                                    sourcePropertyCursor.getColumnIndex("value")
                            )
                            sourceProperties.add(CameraSourceProperty(
                                    name=sourcePropertyName,
                                    value=sourcePropertyValue
                            ))
                        }
                    } catch (e: Exception) {
                        Log.e(LOG_TAG, e.message)
                        throw e
                    } finally {
                        sourcePropertyCursor.close()
                    }
                    var destCursor = db.query(
                            "camera_dest", null, "camera_name=?",
                            arrayOf(name), null, null, null
                    )
                    var dests = ArrayList<CameraDest>()
                    try {
                        while (destCursor.moveToNext()) {
                            var destName = destCursor.getString(destCursor.getColumnIndex("name"))
                            var destUrl =  destCursor.getString(destCursor.getColumnIndex("url"))
                            var destPropertyCursor = db.query(
                                    "camera_dest_property", null,
                                    "dest_name=? and camera_name=?",
                                    arrayOf(destName, name), null, null, null
                            )
                            var destProperties = ArrayList<CameraDestProperty>()
                            try {
                                while (destPropertyCursor.moveToNext()) {
                                    var destPropertyName = destPropertyCursor.getString(
                                            destPropertyCursor.getColumnIndex("name")
                                    )
                                    var destPropertyValue = destPropertyCursor.getString(
                                            destPropertyCursor.getColumnIndex("value")
                                    )
                                    destProperties.add(CameraDestProperty(
                                            name = destPropertyName,
                                            value = destPropertyValue
                                    ))
                                }
                            } catch (e: Exception) {
                                Log.e(LOG_TAG, e.message)
                                throw e
                            } finally {
                                destPropertyCursor.close()
                            }
                            dests.add(CameraDest(
                                    name=destName,
                                    url=destUrl,
                                    dest_properties=destProperties
                            ))
                        }
                    } catch (e: Exception) {
                        Log.e(LOG_TAG, e.message)
                        throw e
                    } finally {
                        destCursor.close()
                    }
                    cameras.add(Camera(
                            name = name, source = source,
                            source_properties = sourceProperties,
                            dests = dests))
                }
            } catch (e: Exception) {
                Log.e(LOG_TAG, e.message)
                throw e
            }
            finally {
                cursor.close()
            }
            db.setTransactionSuccessful()
        } catch (e: Exception) {
            Log.e(LOG_TAG, e.message)
        } finally {
            db.endTransaction()

            db.close()
        }
        return cameras
    }

    fun updateAllCameras(cameras: ArrayList<Camera>) {
        var db = writableDatabase
        db.beginTransaction()
        try {
            db.delete("camera_dest_property", null, null)
            db.delete("camera_dest", null, null)
            db.delete("camera_source_property", null, null)
            db.delete("camera", null, null)
            for (camera in cameras) {
                var cameraContent = ContentValues()
                cameraContent.put("name", camera.name)
                cameraContent.put("source", camera.source)
                db.insert("camera", null, cameraContent)
                for (cameraSourceProeprty in camera.source_properties) {
                    var cameraSourcePropertyContent = ContentValues()
                    cameraSourcePropertyContent.put("name", cameraSourceProeprty.name)
                    cameraSourcePropertyContent.put("value", cameraSourceProeprty.value)
                    cameraSourcePropertyContent.put("camera_name", camera.name)
                    db.insert("camera_source_property", null, cameraSourcePropertyContent)
                }
                for (cameraDest in camera.dests) {
                    var cameraDestContent = ContentValues()
                    cameraDestContent.put("name", cameraDest.name)
                    cameraDestContent.put("url", cameraDest.url)
                    cameraDestContent.put("camera_name", camera.name)
                    db.insert("camera_dest", null, cameraDestContent)
                    for (cameraDestProperty in cameraDest.dest_properties) {
                        var cameraDestPropertyContent = ContentValues()
                        cameraDestPropertyContent.put("name", cameraDestProperty.name)
                        cameraDestPropertyContent.put("value", cameraDestProperty.value)
                        cameraDestPropertyContent.put("dest_name", cameraDest.name)
                        cameraDestPropertyContent.put("camera_name", camera.name)
                        db.insert("camera_dest_property", null, cameraDestPropertyContent)
                    }
                }
            }
            db.setTransactionSuccessful()
        } catch(e: Exception) {
            Log.e(LOG_TAG, e.message)
        } finally {
            db.endTransaction()
            db.close()
        }
    }

    companion object {
        private val LOG_TAG = "DBOpenHelper"
    }
}