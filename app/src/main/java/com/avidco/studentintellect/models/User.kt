package com.avidco.studentintellect.models

import android.os.Build
import android.os.Parcel
import android.os.Parcelable
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

data class User (
    val userInfo : UserInfo = UserInfo(),
    val phone: String? = Firebase.auth.currentUser?.phoneNumber,
    val balance : Double = 0.0,
    val imageUrl: String? = null,
    @JvmField val isOnline : Boolean = false,
    val myModulesList: List<Module>? = null
) : Parcelable {
    constructor(parcel: Parcel) : this(
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU){
            parcel.readParcelable(UserInfo::class.java.classLoader, UserInfo::class.java)!!
        } else {
            @Suppress("DEPRECATION")
            parcel.readParcelable(UserInfo::class.java.classLoader)!!
        },
        parcel.readString(),
        parcel.readDouble(),
        parcel.readString(),
        parcel.readByte() != 0.toByte(),
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            parcel.readArrayList(Module::class.java.classLoader, Module::class.java)?.toList()
        } else {
            var modulesList : List<Module>? = null
            @Suppress("DEPRECATION")
            val arrayList = parcel.readArrayList(Module::class.java.classLoader)
            if(!arrayList.isNullOrEmpty()){
                modulesList = mutableListOf()
                arrayList.forEach {
                    modulesList.add(it as Module)
                }
            }
            modulesList
        }
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeParcelable(userInfo, flags)
        parcel.writeString(phone)
        parcel.writeDouble(balance)
        parcel.writeString(imageUrl)
        parcel.writeByte(if (isOnline) 1 else 0)
        parcel.writeList(myModulesList)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<User> {
        override fun createFromParcel(parcel: Parcel): User {
            return User(parcel)
        }

        override fun newArray(size: Int): Array<User?> {
            return arrayOfNulls(size)
        }
    }
}