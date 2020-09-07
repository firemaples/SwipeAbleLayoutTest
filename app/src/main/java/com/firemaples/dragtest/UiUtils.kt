package com.firemaples.dragtest

import android.content.Context
import android.util.TypedValue

class UiUtils {
    companion object {
        fun getPxFromDp(context: Context, dp: Int): Float {
            val r = context.resources
            return TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                dp.toFloat(),
                r.displayMetrics
            )
        }
    }
}