package com.firemaples.dragtest

import android.content.Context
import android.graphics.Rect
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import androidx.core.view.ViewCompat
import kotlin.math.absoluteValue
import kotlin.math.atan2
import kotlin.math.pow

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
                    R.styleable.DragLayout_hardlyMovedThreshold,
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
    private var dragging: Boolean = false
    private var originalPosition: FloatArray? = null
    private var viewOriginalPosition: FloatArray? = null
    private var previousFingerPosition: FloatArray? = null

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        return when (ev.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                val rect = Rect()
                dragAbleView.getHitRect(rect)
                if (rect.contains(ev.x.toInt(), ev.y.toInt())) {
                    dragging = true
                }
                true
            }
            MotionEvent.ACTION_UP -> {
                val intercept = dragging
                dragging = false
                return intercept
            }
            else -> dragging
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                if (dragging) {
                    originalPosition = floatArrayOf(event.x, event.y)
                    viewOriginalPosition =
                        floatArrayOf(dragAbleView.left.toFloat(), dragAbleView.top.toFloat())
                }
            }
            MotionEvent.ACTION_UP -> {
                if (dragging) {
                    val viewOriginalPosition = viewOriginalPosition
                    if (viewOriginalPosition != null) {
                        val dx = viewOriginalPosition[0] - dragAbleView.left
                        val dy = viewOriginalPosition[1] - dragAbleView.top
                        ViewCompat.offsetLeftAndRight(dragAbleView, dx.toInt())
                        ViewCompat.offsetTopAndBottom(dragAbleView, dy.toInt())
                    }
                    dragging = false
                }
            }
            MotionEvent.ACTION_MOVE -> {
                val originalPoint = originalPosition
                if (dragging && originalPoint != null) {
                    onFingerMove(originalPoint, floatArrayOf(event.x, event.y))
                }
            }
        }

        return true
    }

    private fun onFingerMove(originalPosition: FloatArray, currentPosition: FloatArray) {
        val viewOriginalPosition = viewOriginalPosition ?: return

        val direction =
            getDirection(
                originalPosition[0],
                originalPosition[1],
                currentPosition[0],
                currentPosition[1]
            )

        if (supportedDirections.contains(direction)) {
            when (direction) {
                SwipeDirection.LEFT, SwipeDirection.RIGHT -> {
                    val moved = currentPosition[0] - originalPosition[0]
//                    val dx = currentPoint[0] - (previousFingerPosition ?: originalPoint)[0]

                    val left =
                        viewOriginalPosition[0] + if (moved.absoluteValue < hardlyMoveThreshold) {
                            moved
                        } else {
                            ((moved.absoluteValue - hardlyMoveThreshold).pow(0.8f) + hardlyMoveThreshold) * (moved / moved.absoluteValue)
                        }

//                    val leftBound = paddingLeft
//                    val rightBound = width - dragAbleView.width - paddingRight
//
//                    val newLeft = left.toInt().coerceAtLeast(leftBound).coerceAtMost(rightBound)

                    val newLeft = left.toInt()

                    val moveX = newLeft - dragAbleView.left

                    ViewCompat.offsetLeftAndRight(dragAbleView, moveX)

                    previousFingerPosition = floatArrayOf(currentPosition[0], currentPosition[1])
                }
                else -> {
                }
            }
        } else {
            logger.warn("The direction $direction is not supported")
        }
    }

    /**
     * Given two points in the plane p1=(x1, x2) and p2=(y1, y1), this method
     * returns the direction that an arrow pointing from p1 to p2 would have.
     * @param x1 the x position of the first point
     * @param y1 the y position of the first point
     * @param x2 the x position of the second point
     * @param y2 the y position of the second point
     * @return the direction
     */
    fun getDirection(x1: Float, y1: Float, x2: Float, y2: Float): SwipeDirection {
        val angle = getAngle(x1, y1, x2, y2)
        return SwipeDirection.fromAngle(angle)
    }

    /**
     *
     * Finds the angle between two points in the plane (x1,y1) and (x2, y2)
     * The angle is measured with 0/360 being the X-axis to the right, angles
     * increase counter clockwise.
     *
     * @param x1 the x position of the first point
     * @param y1 the y position of the first point
     * @param x2 the x position of the second point
     * @param y2 the y position of the second point
     * @return the angle between two points
     */
    private fun getAngle(x1: Float, y1: Float, x2: Float, y2: Float): Double {
        val rad: Double = atan2(y1 - y2, x2 - x1) + Math.PI
        return (rad * 180 / Math.PI + 180) % 360
    }

    enum class SwipeDirection(private val flag: Int) {
        LEFT(1),
        UP(2),
        RIGHT(4),
        DOWN(8);

        companion object {
            fun fromFlags(flags: Int): Set<SwipeDirection> =
                values().filter { flags and it.flag == it.flag }.toSet()

            /**
             * Returns a direction given an angle.
             * Directions are defined as follows:
             *
             * [UP]: [45, 135]
             * [RIGHT]: [0,45] and [315, 360]
             * [DOWN]: [225, 315]
             * [LEFT]: [135, 225]
             *
             * @param angle an angle from 0 to 360 - e
             * @return the direction of an angle
             */
            fun fromAngle(angle: Double): SwipeDirection =
                when {
                    inRange(angle, 45f, 135f) -> UP
                    inRange(angle, 0f, 45f) ||
                            inRange(angle, 315f, 360f) -> RIGHT
                    inRange(angle, 225f, 315f) -> DOWN
                    else -> LEFT
                }

            /**
             * @param angle an angle
             * @param init the initial bound
             * @param end the final bound
             * @return returns true if the given angle is in the interval [init, end).
             */
            private fun inRange(angle: Double, init: Float, end: Float): Boolean =
                angle >= init && angle < end
        }
    }
}