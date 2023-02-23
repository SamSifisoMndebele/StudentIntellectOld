package com.avidco.studentintellect.models

import android.os.Build
import android.os.Parcel
import android.os.Parcelable
import com.google.firebase.Timestamp
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.ktx.Firebase

data class FolderData(
    val name: String = "", //ID
    val path: String = "",
    val filesCount: Int = 0,
    val creatorUID: String = Firebase.auth.currentUser?.uid!!,
    val uploaderName: String? = Firebase.auth.currentUser?.displayName,
    val createdTime: Timestamp = Timestamp.now(),
    val verified: Boolean = false,
    val verifiedByUID: String? = null,
    val ref : CollectionReference? = null
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readString()!!,
        parcel.readString()!!,
        parcel.readInt(),
        parcel.readString()!!,
        parcel.readString(),
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU){
            parcel.readParcelable(Timestamp::class.java.classLoader, Timestamp::class.java)!!
        } else {
            @Suppress("DEPRECATION")
            parcel.readParcelable(Timestamp::class.java.classLoader)!!
        },
        parcel.readByte() != 0.toByte(),
        parcel.readString(),
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU){
            parcel.readParcelable(CollectionReference::class.java.classLoader, CollectionReference::class.java)
        } else {
            @Suppress("DEPRECATION")
            parcel.readParcelable(CollectionReference::class.java.classLoader)
        }
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(path)
        parcel.writeString(name)
        parcel.writeInt(filesCount)
        parcel.writeString(creatorUID)
        parcel.writeString(uploaderName)
        parcel.writeParcelable(createdTime, flags)
        parcel.writeByte(if (verified) 1 else 0)
        parcel.writeString(verifiedByUID)
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