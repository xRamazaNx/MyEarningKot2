package ru.developer.press.myearningkot.helpers

import android.app.AlertDialog
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.RippleDrawable
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.Transformation
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.MainThread
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import org.jetbrains.anko.verticalLayout

fun Context.colorRes(res: Int): Int = ContextCompat.getColor(this, res)

fun View.addClickEffectBackground() = with(TypedValue()) {
    context.theme.resolveAttribute(android.R.attr.selectableItemBackground, this, true)
    setBackgroundResource(resourceId)
}


fun View.addClickEffectRippleBackground() {
    val rippleDrawable = RippleDrawable(ColorStateList.valueOf(Color.GRAY), null, null)
    background = rippleDrawable
}

fun TextView.setFont(fontRes: Int, style: Int = Typeface.NORMAL) {
    tryCatch {
        val tf = ResourcesCompat.getFont(context, fontRes)
        setTypeface(tf, style)
    }
}

fun Context.getDrawableRes(id: Int) = ContextCompat.getDrawable(this, id)


@MainThread
fun Context.showImageTest(text: String, imagePath: String) {
    AlertDialog.Builder(this).apply {
        setView(verticalLayout {
            addView(ImageView(this@showImageTest).apply {
                post {
                    val scaled = BitmapFactory.decodeFile(imagePath)
                    setImageBitmap(scaled)
                }
            })
            addView(TextView(this@showImageTest).apply {
                setText(text)
            })
        }
        )
    }.show()
}

fun View.expand() {
    val matchParentMeasureSpec: Int = View.MeasureSpec.makeMeasureSpec(
        (parent as View).width,
        View.MeasureSpec.EXACTLY
    )
    val wrapContentMeasureSpec: Int =
        View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
    measure(matchParentMeasureSpec, wrapContentMeasureSpec)
    val targetHeight: Int = measuredHeight
    // Older versions of android (pre API 21) cancel animations for views with a height of 0.
    layoutParams.height = 1
    visibility = View.VISIBLE
    val a: Animation = object : Animation() {
        override fun applyTransformation(
            interpolatedTime: Float,
            t: Transformation?
        ) {
            layoutParams.height =
                if (interpolatedTime == 1f) ViewGroup.LayoutParams.WRAP_CONTENT else (targetHeight * interpolatedTime).toInt()
            requestLayout()
        }

        override fun willChangeBounds(): Boolean {
            return true
        }
    }
    // Expansion speed of 1dp/ms
    a.duration = (targetHeight / context.resources.displayMetrics.density).toLong()
    startAnimation(a)
}

fun View.collapse(v: View) {
    val initialHeight: Int = measuredHeight
    val a: Animation = object : Animation() {
        override fun applyTransformation(
            interpolatedTime: Float,
            t: Transformation?
        ) {
            if (interpolatedTime == 1f) {
                visibility = View.GONE
            } else {
                layoutParams.height =
                    initialHeight - (initialHeight * interpolatedTime).toInt()
                requestLayout()
            }
        }

        override fun willChangeBounds(): Boolean {
            return true
        }
    }
    // Collapse speed of 1dp/ms
    a.duration = (initialHeight / context.resources.displayMetrics.density).toLong()
    startAnimation(a)
}