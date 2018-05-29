package com.example.xiaodong.testvideo

import android.os.Parcel
import android.os.Parcelable

data class CameraSourceProperty(val name: String, var value: String) : Parcelable {
    constructor(parcel: Parcel) : this(
            parcel.readString(),
            parcel.readString())

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
        val otherCameraSourceProperty: CameraSourceProperty? = other as? CameraSourceProperty
        if (otherCameraSourceProperty == null) {
            return false
        } else {
            return name == otherCameraSourceProperty.name
        }
    }

    override fun hashCode(): Int {
        return name.hashCode()
    }

    companion object CREATOR : Parcelable.Creator<CameraSourceProperty> {
        override fun createFromParcel(parcel: Parcel): CameraSourceProperty {
            return CameraSourceProperty(parcel)
        }

        override fun newArray(size: Int): Array<CameraSourceProperty?> {
            return arrayOfNulls(size)
        }
    }
}