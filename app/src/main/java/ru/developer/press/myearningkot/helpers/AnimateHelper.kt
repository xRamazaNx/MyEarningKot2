package ru.developer.press.myearningkot.helpers

import android.animation.ValueAnimator
import android.graphics.Color
import android.view.View
import android.view.ViewAnimationUtils
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import androidx.core.animation.doOnEnd
import androidx.core.graphics.ColorUtils
import org.jetbrains.anko.backgroundColor
import kotlin.math.hypot

fun View.animateRipple(
    cx: Int = width / 2,
    cy: Int = height / 2,
    finalRadius: Float = hypot(cx.toDouble(), cy.toDouble()).toFloat()
) {
    val oldBackground = background
    backgroundColor = Color.TRANSPARENT
    // create the animator for this view (the start radius is zero)
    val startRadius = finalRadius / 10
    val anim = ViewAnimationUtils.createCircularReveal(
        this,
        cx,
        cy,
        startRadius,
        finalRadius
    )
    // make the view visible and start the animation
    visibility = View.VISIBLE
    anim.interpolator = DecelerateInterpolator()
    anim.duration = 250
    anim.start()
    background = oldBackground
}

fun View.animateColor(
    colorFrom: Int,
    colorTo: Int,
    duration: Long = 325,
    endAnimate: () -> Unit = {}
) {
    val valueAnimator: ValueAnimator = ValueAnimator.ofFloat(0.0f, 1.0f)
    valueAnimator.duration = duration
    valueAnimator.interpolator = AccelerateInterpolator()
    valueAnimator.addUpdateListener {
        val fractionAnim = valueAnimator.animatedValue as Float
        backgroundColor = ColorUtils.blendARGB(colorFrom, colorTo, fractionAnim)
    }
    valueAnimator.doOnEnd {
        endAnimate.invoke()
    }
    valueAnimator.start()
}
