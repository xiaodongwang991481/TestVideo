package com.example.xiaodong.testvideo

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class DBOpenHelper(
        context: Context
) : SQLiteOpenHelper(context, "my.db", null, 1) {
    override fun onCreate(db: SQLiteDatabase?) {
        db?.let {
            db.execSQL("CREATE TABLE if not exists camera(name text primary key, source text not null)")
            db.execSQL(
                    "CREATE TABLE if not exists camera_dest(" +
                            "name text primary key, camera_name text not null, " +
                            " foreign key (camera_name) references camera (name) on delete cascade on update cascade)"
            )
            db.execSQL(
                    "CREATE table if not exists camera_dest_property(" +
                            "name text primary key, value text not null, dest_name text not null, " +
                            "foreign key (dest_name) references camera_dest (name) on delete cascade on update cascade)"
            )
        }
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}