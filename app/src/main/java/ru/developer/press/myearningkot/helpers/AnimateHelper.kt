package ru.developer.press.myearningkot.helpers

import android.animation.Animator
import android.animation.ValueAnimator
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.RippleDrawable
import android.view.View
import android.view.ViewAnimationUtils
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import androidx.core.animation.doOnEnd
import androidx.core.graphics.ColorUtils
import org.jetbrains.anko.backgroundColor
import kotlin.math.hypot


fun View.animateClick() {
    val rippleDrawable = RippleDrawable(ColorStateList.valueOf(Color.LTGRAY), background, null)
    rippleDrawable.setHotspot(
        (width / 2).toFloat(),
        (height / 2).toFloat()
    )
    background = rippleDrawable
}

fun View.animateRipple(
    cx: Int = width / 2,
    cy: Int = height / 2,
    startRadius: Float = 0f,
    finalRadius: Float = hypot(cx.toDouble(), cy.toDouble()).toFloat(),
    endAnimate: (Animator) -> Unit = {}
) {
    val oldBackground = background
    visibility = View.VISIBLE
    backgroundColor = Color.TRANSPARENT
    // create the animator for this view (the start radius is zero)
    val anim = ViewAnimationUtils.createCircularReveal(
        this,
        cx,
        cy,
        startRadius,
        finalRadius
    )
    // make the view visible and start the animation
    anim.doOnEnd(endAnimate)
    anim.interpolator = DecelerateInterpolator()
    anim.duration = 500
    anim.start()
    background = oldBackground
}

fun View.animateClickAlpha() {
    val duration: Long = 250
    animate()
        .setDuration(duration)
        .alpha(0.5f)
        .withEndAction {
            animate()
                .alpha(1f)
                .setDuration(duration)
                .start()
        }
        .start()
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
