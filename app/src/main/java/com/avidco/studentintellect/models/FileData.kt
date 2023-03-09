package com.avidco.studentintellect.models

import android.os.Build
import android.os.Parcel
import android.os.Parcelable
import com.google.firebase.Timestamp
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

data class FileData(
    val id: String = "",
    val name: String = "", //ID
    val materialUrl: String = "",
    val materialSize: Double = 0.0,
    val solutionsUrl: String? = null,
    val solutionsSize: Double? = null,
    val downloads: Int = 0,
    val uploaderUID: String = Firebase.auth.currentUser?.uid!!,
    val uploaderName: String = Firebase.auth.currentUser?.displayName?:"",
    val uploadedTime: Timestamp = Timestamp.now(),
    @JvmField val isVerified: Boolean = false,
    val verifiedByUID: String = "",
    @JvmField val isExportable: Boolean = false,
    var updatedTime: Timestamp = Timestamp.now()
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readString()!!,
        parcel.readString()!!,
        parcel.readString()!!,
        parcel.readDouble(),
        parcel.readString(),
        parcel.readValue(Double::class.java.classLoader) as? Double,
        parcel.readInt(),
        parcel.readString()!!,
        parcel.readString()!!,
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU){
            parcel.readParcelable(Timestamp::class.java.classLoader, Timestamp::class.java)!!
        } else {
            @Suppress("DEPRECATION")
            parcel.readParcelable(Timestamp::class.java.classLoader)!!
        },
        parcel.readByte() != 0.toByte(),
        parcel.readString()!!,
        parcel.readByte() != 0.toByte(),
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU){
            parcel.readParcelable(Timestamp::class.java.classLoader, Timestamp::class.java)!!
        } else {
            @Suppress("DEPRECATION")
            parcel.readParcelable(Timestamp::class.java.classLoader)!!
        }
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(id)
        parcel.writeString(name)
        parcel.writeString(materialUrl)
        parcel.writeDouble(materialSize)
        parcel.writeString(solutionsUrl)
        parcel.writeValue(solutionsSize)
        parcel.writeInt(downloads)
        parcel.writeString(uploaderUID)
        parcel.writeString(uploaderName)
        parcel.writeParcelable(uploadedTime, flags)
        parcel.writeByte(if (isVerified) 1 else 0)
        parcel.writeString(verifiedByUID)
        parcel.writeByte(if (isExportable) 1 else 0)
        parcel.writeParcelable(updatedTime, flags)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<FileData> {
        override fun createFromParcel(parcel: Parcel): FileData {
            return FileData(parcel)
        }

        override fun newArray(size: Int): Array<FileData?> {
            return arrayOfNulls(size)
        }
    }
}