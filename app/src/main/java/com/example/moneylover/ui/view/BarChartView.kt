package com.example.moneylover.ui.view

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import android.view.animation.DecelerateInterpolator
import kotlin.math.max

/**
 * Custom View vẽ biểu đồ cột (Bar Chart)
 * So sánh chi tiêu và thu nhập theo từng danh mục
 */
class BarChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    // Model cho một cột
    data class BarData(
        val label: String,     // Nhãn
        val expense: Float,   // Chi tiêu
        val income: Float,    // Thu nhập
        val color: Int       // Màu sắc
    )

    private var barData: List<BarData> = emptyList()
    private var animatedProgress = 0f
    private var animator: ValueAnimator? = null

    // Paint cho cột chi tiêu
    private val expensePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    // Paint cho cột thu nhập
    private val incomePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.parseColor("#4CAF50")
    }

    // Paint cho text nhãn
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.GRAY
        textSize = 24f
        textAlign = Paint.Align.CENTER
    }

    // Paint cho giá trị
    private val valuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        textSize = 20f
        textAlign = Paint.Align.CENTER
    }

    // Paint cho lưới
    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#E0E0E0")
        style = Paint.Style.STROKE
        strokeWidth = 1f
    }

    // Paint cho legend
    private val legendPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 22f
    }

    // Padding
    private val paddingLeft = 80f
    private val paddingRight = 20f
    private val paddingTop = 20f
    private val paddingBottom = 80f

    // Thiết lập dữ liệu
    fun setData(bars: List<BarData>) {
        this.barData = bars
        animateChart()
    }

    // Animation vẽ biểu đồ
    private fun animateChart() {
        animator?.cancel()
        animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 800
            interpolator = DecelerateInterpolator()
            addUpdateListener { animation ->
                animatedProgress = animation.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    // Vẽ biểu đồ cột
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Vẽ legend
        val legendY = 30f
        legendPaint.color = Color.parseColor("#E53935")
        canvas.drawRect(20f, legendY - 10f, 40f, legendY + 5f, legendPaint)
        legendPaint.color = Color.GRAY
        legendPaint.textAlign = Paint.Align.LEFT
        canvas.drawText("Chi tiêu", 50f, legendY + 5f, legendPaint)

        legendPaint.color = Color.parseColor("#4CAF50")
        canvas.drawRect(140f, legendY - 10f, 160f, legendY + 5f, legendPaint)
        legendPaint.color = Color.GRAY
        canvas.drawText("Thu nhập", 170f, legendY + 5f, legendPaint)

        if (barData.isEmpty()) {
            textPaint.textSize = 32f
            canvas.drawText("Không có dữ liệu", width / 2f, height / 2f, textPaint)
            return
        }

        val chartWidth = width - paddingLeft - paddingRight
        val chartHeight = height - paddingTop - paddingBottom
        val legendHeight = 60f
        val effectiveHeight = chartHeight - legendHeight

        val maxValue = barData.flatMap { listOf(it.expense, it.income) }.maxOrNull() ?: 1f
        val valueRange = max(maxValue, 1f)

        // Vẽ lưới
        val gridLines = 4
        for (i in 0..gridLines) {
            val y = paddingTop + (effectiveHeight / gridLines) * i
            canvas.drawLine(paddingLeft, y, width - paddingRight, y, gridPaint)
            
            // Vẽ giá trị trên lưới
            val gridValue = maxValue * (1 - i.toFloat() / gridLines)
            valuePaint.textAlign = Paint.Align.RIGHT
            valuePaint.color = Color.GRAY
            canvas.drawText(formatCurrency(gridValue), paddingLeft - 10f, y + 6f, valuePaint)
        }

        // Vẽ các cột
        val barGroupWidth = chartWidth / barData.size
        val barWidth = barGroupWidth * 0.35f
        val barSpacing = barGroupWidth * 0.05f

        for (i in barData.indices) {
            val data = barData[i]
            val groupCenterX = paddingLeft + barGroupWidth * i + barGroupWidth / 2
            
            // Cột chi tiêu
            val expenseHeight = (data.expense / valueRange) * effectiveHeight * animatedProgress
            val expenseLeft = groupCenterX - barWidth - barSpacing / 2
            val expenseRight = groupCenterX - barSpacing / 2
            val expenseTop = paddingTop + effectiveHeight - expenseHeight
            val expenseRect = RectF(expenseLeft, expenseTop, expenseRight, paddingTop + effectiveHeight)
            
            expensePaint.color = Color.parseColor("#E53935")
            canvas.drawRoundRect(expenseRect, 8f, 8f, expensePaint)

            // Cột thu nhập
            val incomeHeight = (data.income / valueRange) * effectiveHeight * animatedProgress
            val incomeLeft = groupCenterX + barSpacing / 2
            val incomeRight = groupCenterX + barWidth + barSpacing / 2
            val incomeTop = paddingTop + effectiveHeight - incomeHeight
            val incomeRect = RectF(incomeLeft, incomeTop, incomeRight, paddingTop + effectiveHeight)
            
            canvas.drawRoundRect(incomeRect, 8f, 8f, incomePaint)

            // Nhãn
            textPaint.textAlign = Paint.Align.CENTER
            textPaint.color = Color.GRAY
            val labelY = height - 25f
            val truncatedLabel = if (data.label.length > 10) data.label.take(8) + "..." else data.label
            canvas.drawText(truncatedLabel, groupCenterX, labelY, textPaint)
        }
    }

    // Format số tiền
    private fun formatCurrency(amount: Float): String {
        val formatter = java.text.NumberFormat.getNumberInstance(java.util.Locale("vi", "VN"))
        return formatter.format(amount.toLong())
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        animator?.cancel()
    }
}
