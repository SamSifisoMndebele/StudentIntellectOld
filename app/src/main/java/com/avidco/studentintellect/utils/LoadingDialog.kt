package com.avidco.studentintellect.utils

import android.app.Activity
import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.LinearLayout
import android.widget.TextView
import com.airbnb.lottie.LottieAnimationView
import com.avidco.studentintellect.R

class LoadingDialog(private val activity: Activity?) {

    private var dialog: Dialog? = null
    private var loadingView :LinearLayout? = null
    private var loadingAnimationView :LottieAnimationView? = null
    private var doneView :LinearLayout? = null
    private var doneAnimationView : LottieAnimationView? = null
    private var errorView :LinearLayout? = null
    private var errorAnimationView : LottieAnimationView? = null
    private var loadingText: TextView? = null
    private var doneText: TextView? = null
    private var errorText: TextView? = null
    private var shownView = "loadingView"

    init {
        if (activity != null) {
            val dialog = Dialog(activity)
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
            dialog.setContentView(R.layout.custom_loading_dialog)
            dialog.setCancelable(false)
            dialog.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            dialog.window?.setGravity(Gravity.CENTER)

            this.dialog = dialog
            this.loadingView = dialog.findViewById(R.id.loading_view)
            this.loadingAnimationView = dialog.findViewById(R.id.loading_anim)
            this.doneView = dialog.findViewById(R.id.done_view)
            this.doneView?.visibility = View.GONE
            this.doneAnimationView = dialog.findViewById(R.id.done_anim)
            this.errorView = dialog.findViewById(R.id.error_view)
            this.errorView?.visibility = View.GONE
            this.errorAnimationView = dialog.findViewById(R.id.error_anim)
            this.loadingText = dialog.findViewById(R.id.progress_text)
            this.doneText = dialog.findViewById(R.id.done_text)
            this.errorText = dialog.findViewById(R.id.error_text)
        }
    }

    fun setText(text: String) {
        if (activity == null) {
            dismiss()
            return
        }

        when (shownView){
            "loadingView" -> loadingText?.text = text
            "doneView" -> doneText?.text = text
            "errorView" -> errorText?.text = text
        }
    }

    fun show(text: String = "") {
        if (activity == null) {
            dismiss()
            return
        } else {
            loadingView?.visibility = View.VISIBLE
            doneView?.visibility = View.GONE
            errorView?.visibility = View.GONE

            shownView = "loadingView"
        }

        loadingAnimationView?.playAnimation()
        if (dialog?.isShowing != true) {
            dialog?.show()
        }
        loadingText?.text = text
    }

    fun isDone(text: String = "", onSuccess : (dialog : Dialog?) -> Unit) {
        if (activity == null) {
            dialog?.dismiss()
            return
        } else {
            loadingView?.visibility = View.GONE
            doneView?.visibility = View.VISIBLE
            errorView?.visibility = View.GONE

            shownView = "doneView"
        }

        doneAnimationView?.playAnimation()
        doneText?.text = text
        if (dialog?.isShowing != true) {
            dialog?.show()
        }

        Handler(Looper.getMainLooper()).postDelayed({
            dismiss()
            onSuccess(this.dialog)
        },1000)
    }

    fun isError(text: String = "", onFailure : (dialog : Dialog?) -> Unit) {
        if (activity == null) {
            dialog?.dismiss()
            return
        } else {
            loadingView?.visibility = View.GONE
            doneView?.visibility = View.VISIBLE
            errorView?.visibility = View.GONE

            shownView = "loadingView"
        }

        doneAnimationView?.playAnimation()
        errorText?.text = text
        if (dialog?.isShowing != true) {
            dialog?.show()
        }

        Handler(Looper.getMainLooper()).postDelayed({
            dismiss()
            onFailure(this.dialog)
        },2000)
    }

    fun dismiss() {
        dialog?.dismiss()
    }
}