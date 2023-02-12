package com.avidco.studentintellect.models

import android.os.Parcel
import android.os.Parcelable
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

data class Uploader(
    val uid : String = Firebase.auth.currentUser!!.uid,
    val name : String? = Firebase.auth.currentUser!!.displayName
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readString()!!,
        parcel.readString()
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(uid)
        parcel.writeString(name)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<Uploader> {
        override fun createFromParcel(parcel: Parcel): Uploader {
            return Uploader(parcel)
        }

        override fun newArray(size: Int): Array<Uploader?> {
            return arrayOfNulls(size)
        }
    }
}
