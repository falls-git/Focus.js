package com.example.focusjs

import android.content.Context
import android.text.TextPaint
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ClickableSpan
import android.text.style.ForegroundColorSpan
import android.view.View
import androidx.core.content.ContextCompat

class SyntaxHighlighter(private val context: Context) {

    fun color(text: String, colorRes: Int): SpannableStringBuilder {
        val ssb = SpannableStringBuilder(text)
        val colorInt = ContextCompat.getColor(context, colorRes)
        ssb.setSpan(
            ForegroundColorSpan(colorInt),
            0,
            text.length,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        return ssb
    }

    fun clickableAppString(
        appName: String,
        colorRes: Int,
        onClick: () -> Unit,
        onLongClick: (() -> Unit)? = null
    ): SpannableStringBuilder {
        val fullText = "\"$appName\""
        val ssb = SpannableStringBuilder(fullText)
        val colorInt = ContextCompat.getColor(context, colorRes)

        val clickableSpan = object : ClickableSpan() {
            override fun onClick(widget: View) {
                onClick()
            }

            override fun updateDrawState(ds: TextPaint) {
                super.updateDrawState(ds)
                ds.isUnderlineText = false
                ds.color = colorInt
            }
        }

        ssb.setSpan(
            clickableSpan,
            0,
            fullText.length,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        // Store long-press handler inside a custom tag or span if needed
        if (onLongClick != null) {
            ssb.setSpan(
                LongClickSpan(onLongClick),
                0,
                fullText.length,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }

        return ssb
    }

    class LongClickSpan(val onLongClick: () -> Unit)
}