package com.avidco.studentintellect.utils

import android.content.Context
import android.util.AttributeSet
import com.google.android.material.imageview.ShapeableImageView
import kotlin.jvm.internal.Intrinsics

class SquareImageView : ShapeableImageView {
    constructor(context: Context?) : super(context!!) {
        Intrinsics.checkNotNull(context)
    }

    constructor(context: Context?, attrs: AttributeSet?) : super(context!!, attrs) {
        Intrinsics.checkNotNull(context)
    }

    constructor(context: Context?, attrs: AttributeSet?, defStyle: Int) : super(context!!, attrs, defStyle) {
        Intrinsics.checkNotNull(context)
    }

    public override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, widthMeasureSpec)
        val width = measuredWidth
        if (width != measuredHeight) {
            setMeasuredDimension(width, width)
        }
    }
}