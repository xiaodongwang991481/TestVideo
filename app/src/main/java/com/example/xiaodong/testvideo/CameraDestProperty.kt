package com.example.xiaodong.testvideo

import android.os.Parcel
import android.os.Parcelable

data class CameraDestProperty(val name: String, val value: String) : Parcelable {
    constructor(parcel: Parcel) : this(
            parcel.readString(),
            parcel.readString()) {
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(name)
        parcel.writeString(value)
    }

    override fun toString(): String {
        return "$name=$value"
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun equals(other: Any?): Boolean {
        val otherCameraDestProperty: CameraDestProperty? = other as? CameraDestProperty
        if (otherCameraDestProperty == null) {
            return false
        } else {
            return name == otherCameraDestProperty.name
        }
    }

    override fun hashCode(): Int {
        return name.hashCode()
    }

    companion object CREATOR : Parcelable.Creator<CameraDestProperty> {
        override fun createFromParcel(parcel: Parcel): CameraDestProperty {
            return CameraDestProperty(parcel)
        }

        override fun newArray(size: Int): Array<CameraDestProperty?> {
            return arrayOfNulls(size)
        }
    }

}