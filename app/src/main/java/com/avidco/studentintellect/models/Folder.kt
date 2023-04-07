package com.avidco.studentintellect.models

import android.os.Build
import android.os.Parcel
import android.os.Parcelable
import com.google.firebase.Timestamp

data class Folder(
    val id: String = "",
    var name: String = "",
    val path: String = "",
    val filesCount: Long = 0,
    var timeUpdated: Timestamp = Timestamp.now(),
    val creator: UserInfo = UserInfo(),
    @JvmField val isVerified: Boolean = false,
    val verifier: UserInfo? = null,
    @JvmField val isDeleted: Boolean = false,
    val deleter: UserInfo? = null
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readString()!!,
        parcel.readString()!!,
        parcel.readString()!!,
        parcel.readLong(),
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU){
            parcel.readParcelable(UserInfo::class.java.classLoader, UserInfo::class.java)
        } else {
            @Suppress("DEPRECATION")
            parcel.readParcelable(UserInfo::class.java.classLoader)
        },
        parcel.readByte() != 0.toByte(),
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU){
            parcel.readParcelable(UserInfo::class.java.classLoader, UserInfo::class.java)
        } else {
            @Suppress("DEPRECATION")
            parcel.readParcelable(UserInfo::class.java.classLoader)
        }
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(id)
        parcel.writeString(name)
        parcel.writeString(path)
        parcel.writeLong(filesCount)
        parcel.writeParcelable(timeUpdated, flags)
        parcel.writeParcelable(creator, flags)
        parcel.writeByte(if (isVerified) 1 else 0)
        parcel.writeParcelable(verifier, flags)
        parcel.writeByte(if (isDeleted) 1 else 0)
        parcel.writeParcelable(deleter, flags)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<Folder> {
        override fun createFromParcel(parcel: Parcel): Folder {
            return Folder(parcel)
        }

        override fun newArray(size: Int): Array<Folder?> {
            return arrayOfNulls(size)
        }
    }
}