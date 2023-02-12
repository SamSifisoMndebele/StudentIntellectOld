package com.avidco.studentintellect.models

data class UserData(
    val uid:String = "",
    val name: String = "",
    val email:String = "",
    val phone: String = "",
    val userType : Int = UserType.STUDENT,
    val balance : Float = 0f,
    val modules : ArrayList<String> = arrayListOf(),
    val termsAccepted : Boolean = false,
    val imageUrl: String? = null
)
