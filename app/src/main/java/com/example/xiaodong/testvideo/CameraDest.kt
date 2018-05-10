package com.example.xiaodong.testvideo

import android.os.Parcel
import android.os.Parcelable

data class CameraDest(
        val name: String,
        val dest_properties: ArrayList<CameraDestProperty> = ArrayList<CameraDestProperty>()
) : Parcelable {
    constructor(parcel: Parcel) : this(
            parcel.readString(),
            ArrayList<CameraDestProperty>()) {
        parcel.readTypedList(dest_properties, CameraDestProperty.CREATOR)
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(name)
        parcel.writeTypedList(dest_properties)
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun equals(other: Any?): Boolean {
        val otherCameraDest = other as? CameraDest
        if (otherCameraDest.name == null) {
            return false
        } else {
            return name == otherCameraDest.name
        }
    }

    override fun hashCode(): Int {
        return name.hashCode()
    }

    companion object CREATOR : Parcelable.Creator<CameraDest> {
        override fun createFromParcel(parcel: Parcel): CameraDest {
            return CameraDest(parcel)
        }

        override fun newArray(size: Int): Array<CameraDest?> {
            return arrayOfNulls(size)
        }
    }
}