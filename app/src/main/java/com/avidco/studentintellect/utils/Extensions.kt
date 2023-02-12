package com.avidco.studentintellect.utils

import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import kotlin.math.roundToInt

object Extensions {
    fun Context.shortToast(msg: String?){
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }
    fun Context.longToast(msg: String?){
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
    }

    fun cardXXGen(cardNumber: String):String{
        return "**** **** **** ${cardNumber.takeLast(4)}"
    }

    fun Float.to2DecimalString() : String {
        val number : Int = (this * 100f).roundToInt()
        if (number == 0) return "0.00"
        val wholeNum = number/100
        val decNum = number.toString().padStart(2,'0').takeLast(2)

        return "$wholeNum.$decNum"
    }

    fun Context.hideKeyboard(view: View) {
        val inputMethodManager = getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.hideSoftInputFromWindow(view.windowToken, 0)
    }

    fun View.tempDisable(delayMillis : Long = 1000){
        isEnabled = false
        Handler(Looper.getMainLooper()).postDelayed({
            isEnabled = true
        }, delayMillis)
    }
}