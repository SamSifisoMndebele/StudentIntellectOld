package com.avidco.studentintellect.models

import android.os.Build
import android.os.Parcel
import android.os.Parcelable
import com.google.firebase.Timestamp
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.ktx.Firebase

data class FileData(
    val name: String = "", //ID
    val materialUrl: String = "",
    val materialSize: Float = 0.0f,
    val solutionsUrl: String? = null,
    val solutionsSize: Float? = null,
    val downloads: Int = 0,
    val uploaderUID: String = Firebase.auth.currentUser?.uid!!,
    val uploaderName: String? = Firebase.auth.currentUser?.displayName,
    val uploadedTime: Timestamp = Timestamp.now(),
    @JvmField val isVerified: Boolean = false,
    val verifiedByUID: String? = null,
    @JvmField val isExportable: Boolean = false,
    val ref : DocumentReference? = null
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readString()!!,
        parcel.readString()!!,
        parcel.readFloat(),
        parcel.readString(),
        parcel.readValue(Float::class.java.classLoader) as? Float,
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
        parcel.readByte() != 0.toByte(),
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU){
            parcel.readParcelable(DocumentReference::class.java.classLoader, DocumentReference::class.java)
        } else {
            @Suppress("DEPRECATION")
            parcel.readParcelable(DocumentReference::class.java.classLoader)
        }
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(name)
        parcel.writeString(materialUrl)
        parcel.writeFloat(materialSize)
        parcel.writeString(solutionsUrl)
        parcel.writeValue(solutionsSize)
        parcel.writeInt(downloads)
        parcel.writeString(uploaderUID)
        parcel.writeString(uploaderName)
        parcel.writeParcelable(uploadedTime, flags)
        parcel.writeByte(if (isVerified) 1 else 0)
        parcel.writeString(verifiedByUID)
        parcel.writeByte(if (isExportable) 1 else 0)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<FileData> {
        override fun createFromParcel(parcel: Parcel): FileData {
            return FileData(parcel)
        }

        override fun newArray(size: Int): Array<FileData?> {
            return arrayOfNulls(size)
        }
    }
}