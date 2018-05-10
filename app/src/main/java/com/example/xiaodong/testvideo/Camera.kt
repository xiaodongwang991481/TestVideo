package com.example.xiaodong.testvideo

import android.os.Parcel
import android.os.Parcelable

data class Camera(val name: String, val source: String,
                  val dests: ArrayList<CameraDest> = ArrayList<CameraDest>()
) : Parcelable {
    constructor(parcel: Parcel) : this(
            parcel.readString(),
            parcel.readString(),
            ArrayList<CameraDest>()) {
        parcel.readTypedList(dests, CameraDest.CREATOR)
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(name)
        parcel.writeString(source)
        parcel.writeTypedList(dests)
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