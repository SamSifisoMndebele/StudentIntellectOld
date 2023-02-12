package com.avidco.studentintellect.utils

import android.content.Context
import androidx.activity.result.contract.ActivityResultContracts.OpenDocument
import android.content.Intent

class OpenDocumentContract : OpenDocument() {
    override fun createIntent(context: Context, input: Array<String>): Intent {
        val intent = super.createIntent(context, input)
        intent.addCategory("android.intent.category.OPENABLE")
        intent.action = Intent.ACTION_GET_CONTENT
        intent.type = "application/pdf"
        return intent
    }
}