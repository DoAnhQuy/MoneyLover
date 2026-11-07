package com.example.moneylover.ui.custom

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import android.view.animation.DecelerateInterpolator

class CircularStatView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var title: String = ""
    private var value: String = ""
    private var progress: Float = 0f
    private var animatedProgress = 0f
    private var animator: ValueAnimator? = null
    private var showValueOnly: Boolean = false

    private val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        color = Color.parseColor("#666666")
    }

    private val valuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        color = Color.parseColor("#333333")
        isFakeBoldText = true
    }

    private val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = Color.parseColor("#E0E0E0")
    }

    private val progressPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }

    private val rect = RectF()

    fun setData(titleText: String, valueText: String, progressPercent: Float = 0f, showValueOnly: Boolean = false) {
        title = titleText
        value = valueText
        this.showValueOnly = showValueOnly
        setProgress(progressPercent)
    }

    fun setTitle(titleText: String) {
        title = titleText
        invalidate()
    }

    fun setValue(valueText: String) {
        value = valueText
        invalidate()
    }

    fun setProgress(percent: Float, animate: Boolean = true) {
        val targetProgress = percent.coerceIn(0f, 100f)
        
        if (animate) {
            animator?.cancel()
            animator = ValueAnimator.ofFloat(animatedProgress, targetProgress).apply {
                duration = 500
                interpolator = DecelerateInterpolator()
                addUpdateListener { animation ->
                    animatedProgress = animation.animatedValue as Float
                    invalidate()
                }
                start()
            }
        } else {
            animatedProgress = targetProgress
            invalidate()
        }
        progress = targetProgress
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        
        val strokeWidth = w * 0.15f
        backgroundPaint.strokeWidth = strokeWidth
        progressPaint.strokeWidth = strokeWidth
        
        val padding = strokeWidth / 2 + 4
        rect.set(padding, padding, w - padding, h - padding)

        titlePaint.textSize = w * 0.16f
        valuePaint.textSize = w * 0.22f
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        val centerX = width / 2f
        val centerY = height / 2f

        if (showValueOnly) {
            // Chế độ hiển thị giá trị lớn (cho Chi tiêu, Thu nhập, Số dư)
            if (value.isNotEmpty()) {
                valuePaint.textSize = height * 0.35f
                canvas.drawText(value, centerX, centerY + valuePaint.textSize / 3, valuePaint)
            }
            if (title.isNotEmpty()) {
                titlePaint.textSize = height * 0.2f
                canvas.drawText(title, centerX, titlePaint.textSize + 4, titlePaint)
            }
        } else {
            // Chế độ hiển thị biểu đồ tròn (cho Ngân sách)
            // Vẽ vòng tròn nền
            canvas.drawArc(rect, 0f, 360f, false, backgroundPaint)
            
            // Vẽ vòng tròn tiến độ
            if (animatedProgress > 0) {
                progressPaint.color = when {
                    animatedProgress <= 50 -> Color.parseColor("#4CAF50")   // Xanh - còn nhiều
                    animatedProgress <= 75 -> Color.parseColor("#8BC34A")  // Xanh nhạt
                    animatedProgress <= 90 -> Color.parseColor("#FFC107")  // Vàng - cảnh báo
                    animatedProgress <= 100 -> Color.parseColor("#FF9800") // Cam
                    else -> Color.parseColor("#F44336")                    // Đỏ - vượt ngân sách
                }
                
                val sweepAngle = (animatedProgress / 100f) * 360f
                canvas.drawArc(rect, -90f, sweepAngle, false, progressPaint)
            }
            
            // Vẽ phần trăm ở giữa
            val percentText = "${animatedProgress.toInt()}%"
            canvas.drawText(percentText, centerX, centerY + valuePaint.textSize / 3, valuePaint)
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        animator?.cancel()
    }
}
