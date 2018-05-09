package com.example.xiaodong.testvideo

import android.os.Parcel
import android.os.Parcelable
import java.util.LinkedHashMap

data class Camera(val name: String, val source: String,
                  val dests: LinkedHashMap<String, LinkedHashMap<String, String>> = LinkedHashMap<String, LinkedHashMap<String, String>>()
) : Parcelable {
    constructor(parcel: Parcel) : this(
            parcel.readString(),
            parcel.readString(),
            TODO("dests")) {
    }

    override fun equals(other: Any?): Boolean {
        when(other) {
            is Camera -> return name == other.name
            else -> return false
        }
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(name)
        parcel.writeString(source)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<Camera> {
        override fun createFromParcel(parcel: Parcel): Camera {
            return Camera(parcel)
        }

        override fun newArray(size: Int): Array<Camera?> {
            return arrayOfNulls(size)
        }
    }
}