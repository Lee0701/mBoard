package io.github.lee0701.mboard.settings

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.widget.FrameLayout
import io.github.lee0701.mboard.R

class TouchInterceptingFrameLayout(
    context: Context,
    attrs: AttributeSet?,
): FrameLayout(context, attrs) {
    constructor(context: Context, attrs: AttributeSet?, intercept: Boolean): this(context, attrs) {
        this.intercept = intercept
    }

    private var intercept: Boolean = false

    override fun onInterceptTouchEvent(ev: MotionEvent?): Boolean = intercept
}