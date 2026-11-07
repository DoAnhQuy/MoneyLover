package com.example.moneylover.ui.custom

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View

class BudgetProgressView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    // Dữ liệu ngân sách
    private var budget: Double = 0.0
    private var spending: Double = 0.0
    private var remaining: Double = 0.0
    
    // Trạng thái hiển thị
    private var displayText: String = "--"
    private var displayPercent: Float = 0f
    private var isOverflow: Boolean = false
    private var showProgress: Boolean = false

    private val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = Color.parseColor("#E0E0E0")
    }

    private val progressPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        color = Color.parseColor("#333333")
        isFakeBoldText = true
    }

    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        color = Color.parseColor("#666666")
    }

    private val rect = RectF()

    /**
     * Thiết lập dữ liệu ngân sách để hiển thị
     */
    fun setBudgetData(budget: Double, spending: Double) {
        this.budget = budget
        this.spending = spending
        this.remaining = budget - spending
        
        // Logic hiển thị
        when {
            // Ngân sách = 0 hoặc cả ngân sách và chi tiêu = 0 -> Hiển thị "--"
            budget <= 0 || (budget == 0.0 && spending == 0.0) -> {
                displayText = "--"
                showProgress = false
                isOverflow = false
                displayPercent = 0f
            }
            // Chi tiêu > ngân sách -> Hiển thị "Vượt quá"
            spending > budget -> {
                displayText = "Vượt quá"
                showProgress = true
                isOverflow = true
                displayPercent = ((spending / budget) * 100).toFloat()
                progressPaint.color = Color.parseColor("#F44336") // Đỏ
            }
            // Chi tiêu = ngân sách -> Hiển thị "Hết"
            spending == budget -> {
                displayText = "Hết"
                showProgress = true
                isOverflow = false
                displayPercent = 0f
                progressPaint.color = Color.parseColor("#4CAF50") // Xanh
            }
            // Ngân sách và chi tiêu đều > 0 -> Hiển thị % CÒN LẠI
            else -> {
                val percentRemaining = ((remaining / budget) * 100).toFloat()
                displayPercent = percentRemaining
                showProgress = true
                isOverflow = false
                progressPaint.color = when {
                    percentRemaining > 20 -> Color.parseColor("#4CAF50") // Xanh - còn nhiều
                    percentRemaining > 10 -> Color.parseColor("#FFC107") // Vàng - cảnh báo
                    else -> Color.parseColor("#F44336") // Đỏ - sắp hết
                }
            }
        }
        
        invalidate()
    }

    /**
     * Legacy method - giữ lại để tương thích ngược
     */
    fun setProgress(percent: Float) {
        // Không sử dụng trong logic mới
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        val strokeWidth = w * 0.12f
        backgroundPaint.strokeWidth = strokeWidth
        progressPaint.strokeWidth = strokeWidth
        
        val padding = strokeWidth / 2
        rect.set(padding, padding, w - padding, h - padding)

        textPaint.textSize = w * 0.22f
        labelPaint.textSize = w * 0.10f
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        val centerX = width / 2f
        val centerY = height / 2f

        // Vẽ vòng tròn nền
        canvas.drawArc(rect, 0f, 360f, false, backgroundPaint)
        
        // Vẽ vòng tròn tiến độ (nếu có)
        if (showProgress) {
            if (isOverflow) {
                // Vượt ngân sách - vẽ full vòng đỏ
                canvas.drawArc(rect, 0f, 360f, false, progressPaint)
            } else {
                // Bình thường - vẽ phần CÒN LẠI (không phải phần đã chi)
                val percentRemaining = ((remaining / budget) * 100).toFloat().coerceAtMost(100f)
                val sweepAngle = (percentRemaining / 100f) * 360f
                canvas.drawArc(rect, -90f, sweepAngle, false, progressPaint)
            }
        }
        
        // Vẽ text ở giữa vòng tròn - HIỂN THỊ % CÒN LẠI
        val textCenterY = centerY + textPaint.textSize / 3
        
        if (showProgress && !isOverflow && displayPercent >= 0) {
            // Hiển thị % còn lại
            val percentRemaining = ((remaining / budget) * 100).toFloat().coerceAtMost(100f)
            val percentText = "${percentRemaining.toInt()}%"
            canvas.drawText(percentText, centerX, textCenterY, textPaint)
        } else if (isOverflow) {
            // Hiển thị "Vượt quá" - text nhỏ hơn
            labelPaint.textSize = width * 0.10f
            textPaint.textSize = width * 0.14f
            canvas.drawText("Vượt quá", centerX, textCenterY, textPaint)
            // Reset lại size
            labelPaint.textSize = width * 0.10f
            textPaint.textSize = width * 0.22f
        } else {
            // Hiển thị text trạng thái khác
            canvas.drawText(displayText, centerX, textCenterY, textPaint)
        }
    }
}
