package com.avidco.studentintellect.models

import android.os.Build
import android.os.Parcel
import android.os.Parcelable
import com.google.firebase.Timestamp

data class MaterialData(
    val id: String = "",
    val materialFile: FileData = FileData(),
    val solutionsFile: FileData? = null,
    val downloads: Int = 0,
    val updatedTime: Timestamp? = Timestamp.now(),
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readString()!!,
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU){
            parcel.readParcelable(FileData::class.java.classLoader, FileData::class.java)!!
        } else {
            @Suppress("DEPRECATION")
            parcel.readParcelable(FileData::class.java.classLoader)!!
        },
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU){
            parcel.readParcelable(FileData::class.java.classLoader, FileData::class.java)
        } else {
            @Suppress("DEPRECATION")
            parcel.readParcelable(FileData::class.java.classLoader)
        },
        parcel.readInt(),
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU){
            parcel.readParcelable(Timestamp::class.java.classLoader, Timestamp::class.java)
        } else {
            @Suppress("DEPRECATION")
            parcel.readParcelable(Timestamp::class.java.classLoader)
        }
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(id)
        parcel.writeParcelable(materialFile, flags)
        parcel.writeParcelable(solutionsFile, flags)
        parcel.writeInt(downloads)
        parcel.writeParcelable(updatedTime, flags)
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