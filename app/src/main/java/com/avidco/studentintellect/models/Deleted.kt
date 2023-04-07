package com.avidco.studentintellect.models

import com.google.firebase.Timestamp

data class Deleted(
    val deletedIds : Map<String, Timestamp>? = null,
    val isDeletedPermanently: Boolean = true
)