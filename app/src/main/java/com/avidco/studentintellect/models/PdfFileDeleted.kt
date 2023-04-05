package com.avidco.studentintellect.models

import android.os.Build
import android.os.Parcel
import android.os.Parcelable
import com.google.firebase.Timestamp

data class PdfFileDeleted(
    val id: String = "",
    val name: String = "",
    val materialUrl: String = "",
    val materialSize: Double = 0.0,
    val solutionsUrl: String? = null,
    val solutionsSize: Double? = null,
    val downloads: Int = 0,
    val timeUpdated: Timestamp = Timestamp.now(),
    val uploader: UserInfo = UserInfo(),
    @JvmField val isVerified: Boolean = false,
    @JvmField val isExportable: Boolean = false,
    val verifier: UserInfo? = null,
    @JvmField val isDelete: Boolean = false,
    val deleter: UserInfo? = null,
    val timeDeleted: Timestamp? = null
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readString()!!,
        parcel.readString()!!,
        parcel.readString()!!,
        parcel.readDouble(),
        parcel.readString(),
        parcel.readDouble(),
        parcel.readInt(),
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU){
            parcel.readParcelable(Timestamp::class.java.classLoader, Timestamp::class.java)!!
        } else {
            @Suppress("DEPRECATION")
            parcel.readParcelable(Timestamp::class.java.classLoader)!!
        },
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU){
            parcel.readParcelable(UserInfo::class.java.classLoader, UserInfo::class.java)!!
        } else {
            @Suppress("DEPRECATION")
            parcel.readParcelable(UserInfo::class.java.classLoader)!!
        },
        parcel.readByte() != 0.toByte(),
        parcel.readByte() != 0.toByte(),
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU){
            parcel.readParcelable(UserInfo::class.java.classLoader, UserInfo::class.java)!!
        } else {
            @Suppress("DEPRECATION")
            parcel.readParcelable(UserInfo::class.java.classLoader)!!
        },
        parcel.readByte() != 0.toByte(),
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU){
            parcel.readParcelable(UserInfo::class.java.classLoader, UserInfo::class.java)!!
        } else {
            @Suppress("DEPRECATION")
            parcel.readParcelable(UserInfo::class.java.classLoader)!!
        },
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
        parcel.writeParcelable(timeUpdated, flags)
        parcel.writeParcelable(uploader, flags)
        parcel.writeByte(if (isVerified) 1 else 0)
        parcel.writeParcelable(verifier, flags)
        parcel.writeByte(if (isDelete) 1 else 0)
        parcel.writeParcelable(deleter, flags)
        parcel.writeParcelable(timeDeleted, flags)
        parcel.writeByte(if (isExportable) 1 else 0)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<PdfFileDeleted> {
        override fun createFromParcel(parcel: Parcel): PdfFileDeleted {
            return PdfFileDeleted(parcel)
        }

        override fun newArray(size: Int): Array<PdfFileDeleted?> {
            return arrayOfNulls(size)
        }
    }
}