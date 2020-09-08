package com.firemaples.dragtest

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import androidx.core.view.ViewCompat
import androidx.customview.widget.ViewDragHelper
import kotlin.math.absoluteValue

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
class DragLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    companion object {
        private val logger: Logger = LoggerFactory.getLogger(DragLayout::class.java)
        private const val defaultHardlyMovementThresholdDp = 50
    }

    private var dragAbleViewId: Int = 0

    /**
     * Currently, [SwipeDirection.UP] and [SwipeDirection.DOWN] are not supported.
     */
    private var supportedDirections: Set<SwipeDirection> = setOf()
    private var hardlyMoveThreshold: Float =
        UiUtils.getPxFromDp(context, defaultHardlyMovementThresholdDp)

    init {
        if (attrs != null) {
            val typedArray = context.obtainStyledAttributes(attrs, R.styleable.DragLayout, 0, 0)

            try {
                dragAbleViewId = typedArray.getResourceId(R.styleable.DragLayout_dragAbleViewId, 0)
                hardlyMoveThreshold = typedArray.getDimensionPixelSize(
                    R.styleable.DragLayout_actionMovement,
                    UiUtils.getPxFromDp(context, defaultHardlyMovementThresholdDp).toInt()
                ).toFloat()
                val supportedDirectionFlags =
                    typedArray.getInt(R.styleable.DragLayout_supportedDirections, 0)
                supportedDirections = SwipeDirection.fromFlags(supportedDirectionFlags)
            } finally {
                typedArray.recycle()
            }
        }
    }

    private val dragAbleView: View by lazy { findViewById(dragAbleViewId) }

    private var viewDragHelper: ViewDragHelper

    private val viewDragHelperCallback: ViewDragHelper.Callback =
        object : ViewDragHelper.Callback() {

            private var originalViewLeft: Int? = null
            private var originalViewTop: Int? = null

            override fun tryCaptureView(child: View, pointerId: Int): Boolean {
                return child == dragAbleView
            }

            override fun onViewCaptured(capturedChild: View, activePointerId: Int) {
                super.onViewCaptured(capturedChild, activePointerId)
                originalViewLeft = capturedChild.left
                originalViewTop = capturedChild.top
            }

            override fun onViewPositionChanged(
                changedView: View,
                left: Int,
                top: Int,
                dx: Int,
                dy: Int
            ) {
                super.onViewPositionChanged(changedView, left, top, dx, dy)
            }

            override fun onViewReleased(releasedChild: View, xvel: Float, yvel: Float) {
                super.onViewReleased(releasedChild, xvel, yvel)

                val originalViewLeft = originalViewLeft ?: return
                val originalViewTop = originalViewTop ?: return

                val dx = releasedChild.left - originalViewLeft
                val dy = releasedChild.top - originalViewTop
                if (dx.absoluteValue >= hardlyMoveThreshold) {
                    onSwiped(if (dx > 0) SwipeDirection.RIGHT else SwipeDirection.LEFT)
                }

                ViewCompat.offsetLeftAndRight(dragAbleView, dx * -1)
                ViewCompat.offsetTopAndBottom(dragAbleView, dy * -1)
            }

            override fun clampViewPositionHorizontal(child: View, left: Int, dx: Int): Int {
//                logger.debug("clampViewPositionHorizontal(), child: $child, left: $left, dx: $dx")

//                val leftBound = paddingLeft
//                val rightBound = width - dragAbleView.width
//
//                logger.debug("Bounds, left: $leftBound, right: $rightBound")
//
//                val newLeft = left.coerceAtLeast(leftBound).coerceAtMost(rightBound)
//
//                return newLeft

//                val originalViewLeft = originalViewLeft ?: return left
//                val moved = left - originalViewLeft
//                val newLeft =
//                    originalViewLeft + if (moved.absoluteValue < hardlyMoveThreshold) {
//                        moved
//                    } else {
//                        (((moved.absoluteValue - hardlyMoveThreshold).pow(0.8f) + hardlyMoveThreshold) * (moved / moved.absoluteValue)).toInt()
//                    }
//                return newLeft

                val originalViewLeft = originalViewLeft ?: return left

                var newLeft = left
                if (supportedDirections.contains(SwipeDirection.RIGHT).not()) {
                    newLeft = newLeft.coerceAtMost(originalViewLeft)
                }
                if (supportedDirections.contains(SwipeDirection.LEFT).not()) {
                    newLeft = newLeft.coerceAtLeast(originalViewLeft)
                }

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

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
//        return super.onTouchEvent(event)
        try {
            viewDragHelper.processTouchEvent(event)
        } catch (e: Exception) {
            logger.warn(e)
        }
        return true
    }

    fun onSwiped(direction: SwipeDirection) {
        logger.debug("onSwiped(), direction: $direction")
    }

    enum class SwipeDirection(private val flag: Int) {
        LEFT(1),
        UP(2),
        RIGHT(4),
        DOWN(8);

        companion object {
            fun fromFlags(flags: Int): Set<SwipeDirection> =
                values().filter { flags and it.flag == it.flag }.toSet()
        }
    }
}