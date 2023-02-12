package com.avidco.studentintellect.models

data class Address(
    val recipientName: String? = null,
    val recipientPhone: String? = null,
    val street:String = "",
    val complexOrBuilding: String? = null,
    val suburb: String = "",
    val cityOrTown: String = "",
    val postalCode: String = ""
)