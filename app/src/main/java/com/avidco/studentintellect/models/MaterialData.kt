package com.avidco.studentintellect.models

import android.os.Build
import android.os.Parcel
import android.os.Parcelable
import com.google.firebase.Timestamp

data class MaterialData(
    val id: String = "",
    val withSolutions: Boolean = false,
    val size: MaterialSize? = MaterialSize(),
    val downloads: Int = 0,
    val uploader: Uploader? = Uploader(),
    val dateAdded: Timestamp? = Timestamp.now(),
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readString()!!,
        parcel.readByte() != 0.toByte(),
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            parcel.readParcelable(MaterialSize::class.java.classLoader, MaterialSize::class.java)
        } else {
            parcel.readParcelable(MaterialSize::class.java.classLoader)
        },
        parcel.readInt(),
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            parcel.readParcelable(Uploader::class.java.classLoader, Uploader::class.java)
        } else {
            parcel.readParcelable(Uploader::class.java.classLoader)
        },
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            parcel.readParcelable(Timestamp::class.java.classLoader, Timestamp::class.java)
        } else {
            parcel.readParcelable(Timestamp::class.java.classLoader)
        }
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(id)
        parcel.writeByte(if (withSolutions) 1 else 0)
        parcel.writeParcelable(size, flags)
        parcel.writeInt(downloads)
        parcel.writeParcelable(uploader, flags)
        parcel.writeParcelable(dateAdded, flags)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<MaterialData> {
        override fun createFromParcel(parcel: Parcel): MaterialData {
            return MaterialData(parcel)
        }

        override fun newArray(size: Int): Array<MaterialData?> {
            return arrayOfNulls(size)
        }
    }
}