package com.avidco.studentintellect.models

import android.os.Parcel
import android.os.Parcelable

data class MaterialSize(
    val materialSize: Float = 0.0f,
    val solutionsSize: Float = 0.0f,
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readFloat(),
        parcel.readFloat()
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeFloat(materialSize)
        parcel.writeFloat(solutionsSize)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<MaterialSize> {
        override fun createFromParcel(parcel: Parcel): MaterialSize {
            return MaterialSize(parcel)
        }

        override fun newArray(size: Int): Array<MaterialSize?> {
            return arrayOfNulls(size)
        }
    }
}