package com.avidco.studentintellect.models

import android.os.Build
import android.os.Parcel
import android.os.Parcelable
import com.google.firebase.Timestamp
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

data class ModuleData(
    val id: String = "",
    val code: String = "",
    val name: String = "",
    val imageUrl: String? = null,
    val timeAdded : Timestamp = Timestamp.now(),
    val adderUID: String = Firebase.auth.currentUser?.uid!!,
    val adderName: String = Firebase.auth.currentUser?.displayName?:"",
    val timeUpdated: Timestamp = Timestamp.now(),
    @JvmField val isVerified: Boolean = false,
    val verifiedByUID: String? = null
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readString()!!,
        parcel.readString()!!,
        parcel.readString()!!,
        parcel.readString(),
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            parcel.readParcelable(Timestamp::class.java.classLoader, Timestamp::class.java)!!
        } else {
            @Suppress("DEPRECATION")
            parcel.readParcelable(Timestamp::class.java.classLoader)!!
        },
        parcel.readString()!!,
        parcel.readString()!!,
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            parcel.readParcelable(Timestamp::class.java.classLoader, Timestamp::class.java)!!
        } else {
            @Suppress("DEPRECATION")
            parcel.readParcelable(Timestamp::class.java.classLoader)!!
        },
        parcel.readByte() != 0.toByte(),
        parcel.readString()
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(id)
        parcel.writeString(code)
        parcel.writeString(name)
        parcel.writeString(imageUrl)
        parcel.writeParcelable(timeAdded, flags)
        parcel.writeString(adderUID)
        parcel.writeString(adderName)
        parcel.writeParcelable(timeUpdated, flags)
        parcel.writeByte(if (isVerified) 1 else 0)
        parcel.writeString(verifiedByUID)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<ModuleData> {
        override fun createFromParcel(parcel: Parcel): ModuleData {
            return ModuleData(parcel)
        }

        override fun newArray(size: Int): Array<ModuleData?> {
            return arrayOfNulls(size)
        }
    }
}