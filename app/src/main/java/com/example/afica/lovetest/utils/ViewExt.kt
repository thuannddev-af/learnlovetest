package com.example.afica.lovetest.utils

import android.annotation.SuppressLint
import android.view.MotionEvent
import android.view.View
import android.view.animation.DecelerateInterpolator

/**
 * Adds a small press animation while preserving normal click and accessibility behavior.
 * Replaces this view's touch and click listeners.
 */
@SuppressLint("ClickableViewAccessibility")
fun View.setOnClickWithPressEffect(
    pressedScale: Float = 0.94f,
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
    setOnClickListener(onClick)
}
