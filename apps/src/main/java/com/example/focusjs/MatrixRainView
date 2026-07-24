package com.example.focusjs

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.View
import kotlin.random.Random

class MatrixRainView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    // Larger text size with dense grid spacing
    private val charSize = 34f
    private val charSpacingX = 36f
    private val charSpacingY = 40f

    // Heavy symbol character set
    private val charSet = "!@#$%^&*()_+-=[]{}|;:,.<>?/~`\\0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz"

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = charSize
        typeface = Typeface.MONOSPACE
        style = Paint.Style.FILL
    }

    // Center Floating Text Paint
    private val centerTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 44f
        typeface = Typeface.MONOSPACE
        style = Paint.Style.FILL
        color = Color.parseColor("#D2FF55")
    }

    private var columns = 0
    private var rows = 0

    // Immutable Grid: Characters are defined once and NEVER change position
    private var charGrid = Array(0) { CharArray(0) }

    // Mutable State: Each column tracks the head of its visibility animation
    private var columnHeads = IntArray(0)
    private var streamLengths = IntArray(0)
    private var speeds = IntArray(0)

    // Palette: Yellowish lead, Teal body, Dark Green tail
    private val colorBrightYellow = Color.parseColor("#D2FF55")
    private val colorTealGreen = Color.parseColor("#58C99E")
    private val colorDimGreen = Color.parseColor("#28604C")

    // Exclusion Box for "Focus.js"
    private val focusText = "focus.js"
    private val centerTextBounds = RectF()

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)

        columns = (w / charSpacingX).toInt().coerceAtLeast(1)
        rows = (h / charSpacingY).toInt().coerceAtLeast(1)

        // 1. GENERATE THE FIXED STATIC GRID
        // These characters define the underlying grid and never change.
        charGrid = Array(columns) {
            CharArray(rows) {
                charSet[Random.nextInt(charSet.length)]
            }
        }

        // 2. Initialize the dynamic visibility heads
        columnHeads = IntArray(columns) { Random.nextInt(-rows, rows) }
        streamLengths = IntArray(columns) { Random.nextInt(10, rows + 5) }
        speeds = IntArray(columns) { Random.nextInt(1, 3) }

        // Set bounding box around center "Focus.js" to block visibility updates
        val textWidth = centerTextPaint.measureText(focusText)
        val textHeight = centerTextPaint.textSize
        val centerX = w / 2f
        val centerY = h / 2f

        val paddingX = 30f // Buffer gap
        val paddingY = 30f

        centerTextBounds.set(
            centerX - (textWidth / 2f) - paddingX,
            centerY - (textHeight / 2f) - paddingY,
            centerX + (textWidth / 2f) + paddingX,
            centerY + (textHeight / 2f) + paddingY
        )
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Dark background
        canvas.drawColor(Color.argb(220, 8, 14, 14))

        // --- 1. Draw Matrix Visibility Effect ---
        for (col in 0 until columns) {
            val x = col * charSpacingX + 4f
            val headY = columnHeads[col]
            val length = streamLengths[col]

            for (row in 0 until rows) {
                val distanceToHead = headY - row

                // Determine if this cell's fixed character is visible
                // and what color it should be, based on visibility state.
                if (distanceToHead in 0 until length) {
                    val y = row * charSpacingY + charSize

                    // Skip updating visibility if position hits the center box
                    if (centerTextBounds.contains(x, y)) {
                        continue
                    }

                    // Define the color based on distance to the visibility head
                    when (distanceToHead) {
                        0 -> textPaint.color = colorBrightYellow
                        in 1..5 -> textPaint.color = colorTealGreen
                        else -> textPaint.color = colorDimGreen
                    }

                    // DRAW THE FIXED CHARACTER (from charGrid)
                    val fixedChar = charGrid[col][row].toString()
                    canvas.drawText(fixedChar, x, y, textPaint)
                }
            }

            // Move the visibility head down
            columnHeads[col] += speeds[col]

            // Reset visibility stream when the tail falls off bottom edge
            if (columnHeads[col] - streamLengths[col] > rows) {
                columnHeads[col] = Random.nextInt(-15, 0)
                streamLengths[col] = Random.nextInt(10, rows + 5)
                speeds[col] = Random.nextInt(1, 3)
            }
        }

        // --- 2. Draw Center Text "Focus.js" ---
        val centerX = width / 2f
        val centerY = height / 2f
        val textWidth = centerTextPaint.measureText(focusText)

        canvas.drawText(
            focusText,
            centerX - (textWidth / 2f),
            centerY + (centerTextPaint.textSize / 3f),
            centerTextPaint
        )

        if (visibility == VISIBLE) {
            // Speed of visibility update
            postInvalidateDelayed(150)
        }
    }
}
