package com.example.moneylover.ui.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import android.animation.ObjectAnimator

class BudgetProgressView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var budget: Double = 0.0
    private var spending: Double = 0.0
    private var progress: Float = 0f  // 0-100 (% còn lại)
    private var animatedProgress = 0f

    private val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 16f
        strokeCap = Paint.Cap.ROUND
        color = Color.parseColor("#E0E0E0")
    }

    private val progressPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 16f
        strokeCap = Paint.Cap.ROUND
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        color = Color.parseColor("#333333")
        isFakeBoldText = true
    }

    private val arcRect = RectF()
    private var animator: ObjectAnimator? = null

    private val normalColor = Color.parseColor("#4CAF50")    // Xanh - còn nhiều
    private val warningColor = Color.parseColor("#FFC107")   // Vàng - cảnh báo
    private val dangerColor = Color.parseColor("#F44336")    // Đỏ - sắp hết

    /**
     * Thiết lập dữ liệu ngân sách - hiển thị % CÒN LẠI
     */
    fun setBudgetData(budget: Double, spending: Double) {
        this.budget = budget
        this.spending = spending

        when {
            budget <= 0 || (budget == 0.0 && spending == 0.0) -> {
                progress = 0f
            }
            spending > budget -> {
                progress = 100f // Vượt quá - full đỏ
            }
            else -> {
                // % còn lại
                val remaining = budget - spending
                progress = ((remaining / budget) * 100).toFloat().coerceAtMost(100f)
            }
        }
        animateProgress()
    }

    fun setProgress(percent: Float) {
        this.progress = percent.coerceIn(0f, 100f)
        animateProgress()
    }

    private fun animateProgress() {
        animator?.cancel()
        animator = ObjectAnimator.ofFloat(this, "animatedProgress", 0f, progress).apply {
            duration = 800
            start()
        }
    }

    @Suppress("unused")
    fun setAnimatedProgress(value: Float) {
        animatedProgress = value
        invalidate()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        textPaint.textSize = w * 0.22f
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val centerX = width / 2f
        val centerY = height / 2f
        val radius = (java.lang.Math.min(width.toFloat(), height.toFloat()) / 2f) - 20f

        arcRect.set(
            centerX - radius,
            centerY - radius,
            centerX + radius,
            centerY + radius
        )

        // Draw background arc (full vòng)
        canvas.drawArc(arcRect, 0f, 360f, false, backgroundPaint)

        // Draw progress arc (% còn lại)
        if (animatedProgress > 0) {
            val sweepAngle = (animatedProgress / 100f) * 360f

            // Set color based on remaining %
            progressPaint.color = when {
                animatedProgress > 20 -> normalColor  // Xanh - còn nhiều
                animatedProgress > 10 -> warningColor // Vàng - cảnh báo
                else -> dangerColor                   // Đỏ - sắp hết
            }

            canvas.drawArc(arcRect, -90f, sweepAngle, false, progressPaint)
        }

        // Vẽ text % ở giữa
        if (progress > 0) {
            val textY = centerY + textPaint.textSize / 3
            val percentText = "${animatedProgress.toInt()}%"
            canvas.drawText(percentText, centerX, textY, textPaint)
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        animator?.cancel()
    }
}
