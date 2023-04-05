package com.avidco.studentintellect.models

import android.os.Parcel
import android.os.Parcelable
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

data class UserInfo(
    val uid: String = Firebase.auth.currentUser?.uid!!,
    val name: String = Firebase.auth.currentUser?.displayName!!,
    val email: String = Firebase.auth.currentUser?.email!!,
    val userType : Int = UserType.STUDENT
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readString()!!,
        parcel.readString()!!,
        parcel.readString()!!,
        parcel.readInt()
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(uid)
        parcel.writeString(name)
        parcel.writeString(email)
        parcel.writeInt(userType)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<UserInfo> {
        override fun createFromParcel(parcel: Parcel): UserInfo {
            return UserInfo(parcel)
        }

        override fun newArray(size: Int): Array<UserInfo?> {
            return arrayOfNulls(size)
        }
    }
}