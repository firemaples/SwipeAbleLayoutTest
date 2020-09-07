package com.firemaples.dragtest

import android.content.Context
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import androidx.customview.widget.ViewDragHelper
import kotlin.math.atan2

/**
 * Reference:
 * DragViewHelper
 * https://medium.com/@devwilly/how-to-use-android-viewdraghelper-a2c7539ee62e
 * https://developer.android.com/reference/androidx/customview/widget/ViewDragHelper
 * https://stackoverflow.com/questions/17772532/viewdraghelper-how-to-use-it
 *
 * How to detect swipe direction between left/right and up/down
 * https://stackoverflow.com/a/26387629/2906153
 */
class DragLayout2 : FrameLayout {
    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    )

    companion object {
        private val logger: Logger = LoggerFactory.getLogger(DragLayout2::class.java)
    }

    private val dragableViewId: Int = R.id.dgv1
    private val dragableView: View by lazy { findViewById(dragableViewId) }

    val supportedDirections: MutableSet<DragLayout.SwipeDirection> = mutableSetOf()

    private val viewDragHelper: ViewDragHelper

    private val viewDragHelperCallback: ViewDragHelper.Callback =
        object : ViewDragHelper.Callback() {
            override fun tryCaptureView(child: View, pointerId: Int): Boolean {
                return child == dragableView
            }

            override fun clampViewPositionHorizontal(child: View, left: Int, dx: Int): Int {
                logger.debug("clampViewPositionHorizontal(), child: $child, left: $left, dx: $dx")

                val leftBound = paddingLeft
                val rightBound = width - dragableView.width

                logger.debug("Bounds, left: $leftBound, right: $rightBound")

                val newLeft = left.coerceAtLeast(leftBound).coerceAtMost(rightBound)

                return newLeft
            }
        }

    init {
        viewDragHelper = ViewDragHelper.create(this, 1.0f, viewDragHelperCallback)
    }

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
//        return super.onInterceptTouchEvent(ev)
        return when (ev.actionMasked) {
            MotionEvent.ACTION_CANCEL, MotionEvent.ACTION_UP -> {
                viewDragHelper.cancel()
                false
            }
            else -> viewDragHelper.shouldInterceptTouchEvent(ev)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
//        return super.onTouchEvent(event)
        try {
            viewDragHelper.processTouchEvent(event)
        } catch (e: Exception) {
            logger.warn(e)
        }
        return true
    }
}