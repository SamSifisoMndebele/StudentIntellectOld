package com.avidco.studentintellect.models

import android.os.Build
import android.os.Parcel
import android.os.Parcelable
import com.google.firebase.Timestamp
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

data class FolderData(
    val name: String = "",
    val url: String = "",
    val size: Float = 0.0f,
    val uploaderUID: String = Firebase.auth.currentUser?.uid!!,
    val uploadedTime: Timestamp? = Timestamp.now(),
    val verified: Boolean = false,
    val verifiedByUID: String? = null,
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readString()!!,
        parcel.readString()!!,
        parcel.readFloat(),
        parcel.readString()!!,
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU){
            parcel.readParcelable(Timestamp::class.java.classLoader, Timestamp::class.java)
        } else {
            @Suppress("DEPRECATION")
            parcel.readParcelable(Timestamp::class.java.classLoader)
        },
        parcel.readByte() != 0.toByte(),
        parcel.readString()
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(name)
        parcel.writeString(url)
        parcel.writeFloat(size)
        parcel.writeString(uploaderUID)
        parcel.writeParcelable(uploadedTime, flags)
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