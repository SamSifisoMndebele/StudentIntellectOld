package com.avidco.studentintellect.utils

import android.content.Context
import androidx.activity.result.contract.ActivityResultContracts.OpenDocument
import android.content.Intent

class OpenPicturesContract : OpenDocument() {
    override fun createIntent(context: Context, input: Array<String>): Intent {
        val intent = super.createIntent(context, input)
        intent.action = Intent.ACTION_PICK
        intent.type = "image/*"
        return intent
    }
}