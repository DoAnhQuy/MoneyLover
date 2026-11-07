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
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * Custom View vẽ biểu đồ tròn (Donut Chart) với animation
 * Hiển thị các phần với hiệu ứng donut ở giữa
 */
class DashboardChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    // Model cho một phần biểu đồ
    data class ChartSlice(
        val value: Float,    // Giá trị
        val color: Int,     // Màu sắc
        val label: String   // Nhãn
    )

    private var slices: List<ChartSlice> = emptyList()
    private var animatedProgress = 0f
    private var animator: ValueAnimator? = null

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        textSize = 36f
        textAlign = Paint.Align.CENTER
    }

    private val centerCircle = RectF()
    private var centerX = 0f
    private var centerY = 0f
    private var radius = 0f

    // Thiết lập dữ liệu
    fun setData(sliceList: List<ChartSlice>) {
        this.slices = sliceList
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

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        centerX = w / 2f
        centerY = h / 2f
        val chartSize = min(w, h) * 0.7f
        radius = chartSize / 2f
        centerCircle.set(
            centerX - radius,
            centerY - radius,
            centerX + radius,
            centerY + radius
        )
    }

    // Vẽ biểu đồ tròn donut
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (slices.isEmpty()) {
            // Vẽ trạng thái trống
            paint.color = Color.parseColor("#E0E0E0")
            canvas.drawCircle(centerX, centerY, radius, paint)
            
            textPaint.textSize = 32f
            canvas.drawText("Không có dữ liệu", centerX, centerY, textPaint)
            return
        }

        val total = slices.sumOf { it.value.toDouble() }.toFloat()
        if (total <= 0) {
            paint.color = Color.parseColor("#E0E0E0")
            canvas.drawCircle(centerX, centerY, radius, paint)
            return
        }

        var startAngle = -90f
        val animatedTotal = total * animatedProgress

        // Vẽ các phần biểu đồ
        for (slice in slices) {
            val sweepAngle = (slice.value / total) * 360f * animatedProgress
            paint.color = slice.color
            canvas.drawArc(centerCircle, startAngle, sweepAngle, true, paint)
            startAngle += sweepAngle
        }

        // Vẽ vòng tròn trắng ở giữa (hiệu ứng donut)
        paint.color = Color.WHITE
        val innerRadius = radius * 0.5f
        canvas.drawCircle(centerX, centerY, innerRadius, paint)

        // Vẽ tổng ở giữa
        textPaint.textSize = 40f
        textPaint.color = Color.BLACK
        textPaint.isFakeBoldText = true
        canvas.drawText(formatCurrency(animatedTotal), centerX, centerY + 10f, textPaint)
        
        textPaint.textSize = 28f
        textPaint.isFakeBoldText = false
        textPaint.color = Color.GRAY
        canvas.drawText("Tổng chi", centerX, centerY + 45f, textPaint)
    }

    // Format số tiền VND
    private fun formatCurrency(amount: Float): String {
        val formatter = java.text.NumberFormat.getNumberInstance(java.util.Locale("vi", "VN"))
        return formatter.format(amount.toLong()) + "đ"
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        animator?.cancel()
    }
}
