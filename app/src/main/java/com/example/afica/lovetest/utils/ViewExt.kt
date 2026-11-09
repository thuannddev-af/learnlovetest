package com.example.afica.lovetest.utils

import android.annotation.SuppressLint
import android.os.SystemClock
import android.view.MotionEvent
import android.view.View
import android.view.animation.DecelerateInterpolator

/** Accepts the first click immediately, then ignores clicks within [intervalMillis].
 * Each view has its own timer. Ignored clicks do not extend the waiting period.
 */
fun View.setOnSingleClickListener(
    intervalMillis: Long = 600L,
    onClick: (View) -> Unit
) {
    require(intervalMillis >= 0L) { "Click interval must not be negative" }
    var lastAcceptedClick: Long? = null
    setOnClickListener { view ->
        val now = SystemClock.elapsedRealtime()
        val previous = lastAcceptedClick
        if (previous == null || now - previous >= intervalMillis) {
            lastAcceptedClick = now
            onClick(view)
        }
    }
}

@SuppressLint("ClickableViewAccessibility")
fun View.setOnClickWithPressEffect(
    pressedScale: Float = 0.94f,
    intervalMillis: Long = 300L,
    onClick: (View) -> Unit
) {
    require(pressedScale > 0f && pressedScale <= 1f)
    val initialScaleX = scaleX
    val initialScaleY = scaleY

    fun animatePress(pressed: Boolean) {
        animate().cancel()
        animate()
            .scaleX(initialScaleX * if (pressed) pressedScale else 1f)
            .scaleY(initialScaleY * if (pressed) pressedScale else 1f)
            .setDuration(if (pressed) 80L else 120L)
            .setInterpolator(DecelerateInterpolator())
            .start()
    }

    setOnTouchListener { view, event ->
        if (view.isEnabled) {
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> animatePress(true)
                MotionEvent.ACTION_MOVE -> animatePress(
                    event.x >= 0 && event.x < view.width &&
                        event.y >= 0 && event.y < view.height
                )
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> animatePress(false)
            }
        }
        // Let View handle pressed state, ripple, scrolling cancellation and performClick().
        false
    }
    setOnSingleClickListener(intervalMillis, onClick)
}
