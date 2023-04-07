package com.avidco.studentintellect.models

import android.os.Build
import android.os.Parcel
import android.os.Parcelable
import com.google.firebase.Timestamp

data class PdfFile(
    val id: String = "",
    var name: String = "",
    var materialUrl: String = "",
    var materialSize: Double = 0.0,
    var solutionsUrl: String? = null,
    var solutionsSize: Double? = null,
    val downloads: Long = 0,
    var timeUpdated: Timestamp = Timestamp.now(),
    val uploader: UserInfo = UserInfo(),
    @JvmField val isExportable: Boolean = false,
    @JvmField val isVerified: Boolean = false,
    val verifier: UserInfo? = null,
    @JvmField val isDeleted: Boolean = false,
    val deleter: UserInfo? = null
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readString()!!,
        parcel.readString()!!,
        parcel.readString()!!,
        parcel.readDouble(),
        parcel.readString(),
        parcel.readValue(Double::class.java.classLoader) as? Double,
        parcel.readLong(),
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            parcel.readParcelable(Timestamp::class.java.classLoader, Timestamp::class.java)!!
        } else {
            @Suppress("DEPRECATION")
            parcel.readParcelable(Timestamp::class.java.classLoader)!!
        },
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            parcel.readParcelable(UserInfo::class.java.classLoader, UserInfo::class.java)!!
        } else {
            @Suppress("DEPRECATION")
            parcel.readParcelable(UserInfo::class.java.classLoader)!!
        },
        parcel.readByte() != 0.toByte(),
        parcel.readByte() != 0.toByte(),
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            parcel.readParcelable(UserInfo::class.java.classLoader, UserInfo::class.java)
        } else {
            @Suppress("DEPRECATION")
            parcel.readParcelable(UserInfo::class.java.classLoader)
        },
        parcel.readByte() != 0.toByte(),
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            parcel.readParcelable(UserInfo::class.java.classLoader, UserInfo::class.java)
        } else {
            @Suppress("DEPRECATION")
            parcel.readParcelable(UserInfo::class.java.classLoader)
        }
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(id)
        parcel.writeString(name)
        parcel.writeString(materialUrl)
        parcel.writeDouble(materialSize)
        parcel.writeString(solutionsUrl)
        parcel.writeValue(solutionsSize)
        parcel.writeLong(downloads)
        parcel.writeParcelable(timeUpdated, flags)
        parcel.writeParcelable(uploader, flags)
        parcel.writeByte(if (isExportable) 1 else 0)
        parcel.writeByte(if (isVerified) 1 else 0)
        parcel.writeParcelable(verifier, flags)
        parcel.writeByte(if (isDeleted) 1 else 0)
        parcel.writeParcelable(deleter, flags)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<PdfFile> {
        override fun createFromParcel(parcel: Parcel): PdfFile {
            return PdfFile(parcel)
        }

        override fun newArray(size: Int): Array<PdfFile?> {
            return arrayOfNulls(size)
        }
    }
}