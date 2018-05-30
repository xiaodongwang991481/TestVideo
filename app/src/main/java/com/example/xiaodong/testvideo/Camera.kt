package com.example.xiaodong.testvideo

import android.os.Parcel
import android.os.Parcelable

data class Camera(val name: String, var source: String,
                  var source_properties: ArrayList<CameraSourceProperty> = ArrayList(),
                  var dests: ArrayList<CameraDest> = ArrayList()
) : Parcelable {
    constructor(parcel: Parcel) : this(
            parcel.readString(),
            parcel.readString(),
            ArrayList<CameraSourceProperty>(),
            ArrayList<CameraDest>()) {
        parcel.readTypedList(source_properties, CameraSourceProperty.CREATOR)
        parcel.readTypedList(dests, CameraDest.CREATOR)
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(name)
        parcel.writeString(source)
        parcel.writeTypedList(source_properties)
        parcel.writeTypedList(dests)
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun toString(): String {
        var prefix = source_properties.joinToString(prefix="name=$source[", postfix = "]")
        return dests.joinToString(prefix="$prefix[", postfix = "]")
    }

    override fun equals(other: Any?): Boolean {
        val otherCamera = other as? Camera
        if (otherCamera == null) {
            return false
        } else {
            return name == otherCamera.name
        }
    }

    override fun hashCode(): Int {
        return name.hashCode()
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