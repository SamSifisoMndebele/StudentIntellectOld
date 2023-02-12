package com.avidco.studentintellect.models

import com.google.firebase.Timestamp

data class ModuleData(
    val code: String = "",//doc ID
    val name: String = "",
    val imageUrl: String? = null,
    val dateAdded : Timestamp = Timestamp.now()
)