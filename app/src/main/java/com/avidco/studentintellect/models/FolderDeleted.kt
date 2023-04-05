package com.avidco.studentintellect.models

import android.os.Build
import android.os.Parcel
import android.os.Parcelable
import com.google.firebase.Timestamp
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

data class FolderDeleted(
    val id: String = "",
    val isDelete: Boolean = true,
    val isDeletePermanently: Boolean = true,
    val deletedByUID: String = Firebase.auth.currentUser?.uid!!,
    val timeDeleted: Timestamp = Timestamp.now()
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readString()!!,
        parcel.readByte() != 0.toByte(),
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
        parcel.writeByte(if (isDelete) 1 else 0)
        parcel.writeByte(if (isDeletePermanently) 1 else 0)
        parcel.writeString(deletedByUID)
        parcel.writeParcelable(timeDeleted, flags)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<FolderDeleted> {
        override fun createFromParcel(parcel: Parcel): FolderDeleted {
            return FolderDeleted(parcel)
        }

        override fun newArray(size: Int): Array<FolderDeleted?> {
            return arrayOfNulls(size)
        }
    }
}