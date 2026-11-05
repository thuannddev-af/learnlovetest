package com.example.afica.lovetest.component.customview

import android.animation.ValueAnimator
import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.animation.LinearInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat
import com.example.afica.lovetest.R
import kotlin.math.PI
import kotlin.math.sin

class LoveResultView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : FrameLayout(context, attrs) {
    private val heart = ImageView(context).apply {
        setImageResource(R.drawable.heart1)
        scaleType = ImageView.ScaleType.FIT_CENTER
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
    }
    private val label = TextView(context).apply {
        gravity = Gravity.CENTER
        textSize = 44f
        setTextColor(android.graphics.Color.WHITE)
        typeface = ResourcesCompat.getFont(context, R.font.plusjakartasans_semibold)
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
    }
    private var progressAnimator: ValueAnimator? = null
    private var pulseAnimator: ValueAnimator? = null
    private var displayedPercentage = 0
    var isLoading = false
        private set

    init {
        val inset = (8 * resources.displayMetrics.density).toInt()
        addView(heart, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT).apply {
            setMargins(inset, inset, inset, inset)
        })
        addView(label, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT).apply {
            bottomMargin = (16 * resources.displayMetrics.density).toInt()
        })
    }

    private fun display(value: Int) {
        displayedPercentage = value.coerceIn(0, 100)
        label.text = context.getString(R.string.result_percentage, displayedPercentage)
    }

    fun startLoading(remainingMillis: Long, totalMillis: Long) {
        if (isLoading) return
        stopAnimations()
        isLoading = true
        label.visibility = VISIBLE
        contentDescription = context.getString(R.string.result_calculating)
        val progress = ((1f - remainingMillis.toFloat() / totalMillis) * 100).toInt().coerceIn(0, 100)
        display(progress)
        progressAnimator = ValueAnimator.ofInt(progress, 100).apply {
            duration = remainingMillis.coerceAtLeast(1L)
            interpolator = LinearInterpolator()
            addUpdateListener { display(it.animatedValue as Int) }
            start()
        }
        pulseAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 800L
            repeatCount = ValueAnimator.INFINITE
            interpolator = LinearInterpolator()
            addUpdateListener {
                val phase = it.animatedValue as Float
                val scale = 1f + 0.07f * sin(phase * PI).toFloat()
                heart.scaleX = scale
                heart.scaleY = scale
                label.scaleX = scale
                label.scaleY = scale
            }
            start()
        }
    }

    fun showResult(percentage: Int) {
        stopAnimations()
        label.visibility = VISIBLE
        display(percentage)
        contentDescription = label.text
        accessibilityLiveRegion = ACCESSIBILITY_LIVE_REGION_POLITE
    }

    fun stopAnimations() {
        progressAnimator?.cancel()
        pulseAnimator?.cancel()
        progressAnimator = null
        pulseAnimator = null
        heart.scaleX = 1f
        heart.scaleY = 1f
        label.scaleX = 1f
        label.scaleY = 1f
        isLoading = false
    }

    override fun onDetachedFromWindow() {
        stopAnimations()
        super.onDetachedFromWindow()
    }
}
