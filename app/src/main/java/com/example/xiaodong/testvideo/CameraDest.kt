package com.example.xiaodong.testvideo

import android.os.Parcel
import android.os.Parcelable

data class CameraDest(
        val name: String,
        val url: String,
        var dest_properties: ArrayList<CameraDestProperty> = ArrayList()
) : Parcelable {
    constructor(parcel: Parcel) : this(
            parcel.readString(),
            parcel.readString(),
            ArrayList<CameraDestProperty>()) {
        parcel.readTypedList(dest_properties, CameraDestProperty.CREATOR)
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(name)
        parcel.writeString(url)
        parcel.writeTypedList(dest_properties)
    }

    override fun toString(): String {
        return dest_properties.joinToString(prefix="$name=$url[", postfix="]")
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun equals(other: Any?): Boolean {
        val otherCameraDest = other as? CameraDest
        if (otherCameraDest == null) {
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