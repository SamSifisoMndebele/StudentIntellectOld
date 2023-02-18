package com.avidco.studentintellect.models

import android.os.Parcel
import android.os.Parcelable
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

data class UserData(
    val uid: String = Firebase.auth.currentUser?.uid!!,
    val name: String? = Firebase.auth.currentUser?.displayName,
    val email: String = Firebase.auth.currentUser?.email!!,
    val phone: String? = Firebase.auth.currentUser?.phoneNumber,
    val userType : Int = UserType.STUDENT,
    val balance : Float = 0f,
    val myModules : String? = "",
    val termsAccepted : Boolean = false,
    val imageUrl: String? = null
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readString()!!,
        parcel.readString(),
        parcel.readString()!!,
        parcel.readString(),
        parcel.readInt(),
        parcel.readFloat(),
        parcel.readString()!!,
        parcel.readByte() != 0.toByte(),
        parcel.readString()
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(uid)
        parcel.writeString(name)
        parcel.writeString(email)
        parcel.writeString(phone)
        parcel.writeInt(userType)
        parcel.writeFloat(balance)
        parcel.writeString(myModules)
        parcel.writeByte(if (termsAccepted) 1 else 0)
        parcel.writeString(imageUrl)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<UserData> {
        override fun createFromParcel(parcel: Parcel): UserData {
            return UserData(parcel)
        }

        override fun newArray(size: Int): Array<UserData?> {
            return arrayOfNulls(size)
        }
    }
}