package com.avidco.studentintellect.models

import android.os.Build
import android.os.Parcel
import android.os.Parcelable
import com.google.firebase.Timestamp
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

data class FolderData(
    val id: String = "",
    val name: String = "",
    var path: String = "",
    val filesCount: Int = 0,
    val creatorUID: String = Firebase.auth.currentUser?.uid!!,
    val creatorName: String = Firebase.auth.currentUser?.displayName?:"",
    val createdTime: Timestamp = Timestamp.now(),
    @JvmField val isVerified: Boolean = false,
    val verifiedByUID: String = "",
    val updatedTime: Timestamp = Timestamp.now()
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readString()!!,
        parcel.readString()!!,
        parcel.readString()!!,
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
        parcel.writeString(path)
        parcel.writeInt(filesCount)
        parcel.writeString(creatorUID)
        parcel.writeString(creatorName)
        parcel.writeParcelable(createdTime, flags)
        parcel.writeByte(if (isVerified) 1 else 0)
        parcel.writeString(verifiedByUID)
        parcel.writeParcelable(updatedTime, flags)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<FolderData> {
        override fun createFromParcel(parcel: Parcel): FolderData {
            return FolderData(parcel)
        }

        override fun newArray(size: Int): Array<FolderData?> {
            return arrayOfNulls(size)
        }
    }
}