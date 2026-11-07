package com.example.moneylover.ui.view

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.View
import android.view.animation.DecelerateInterpolator
import kotlin.math.max

/**
 * Custom View vẽ biểu đồ đường (Line Chart)
 * Hiển thị dữ liệu theo thời gian với animation
 */
class LineChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    // Model cho một điểm dữ liệu
    data class DataPoint(
        val label: String,
        val value: Float,
        val date: Long
    )

    private var dataPoints: List<DataPoint> = emptyList()
    private var animatedProgress = 0f
    private var animator: ValueAnimator? = null

    // Paint cho đường kẻ
    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 4f
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    // Paint cho vùng tô dưới đường
    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    // Paint cho các điểm
    private val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    // Paint cho text nhãn
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.GRAY
        textSize = 24f
        textAlign = Paint.Align.CENTER
    }

    // Paint cho lưới
    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#E0E0E0")
        style = Paint.Style.STROKE
        strokeWidth = 1f
    }

    private val path = Path()
    private val fillPath = Path()

    // Padding cho biểu đồ
    private val paddingLeft = 60f
    private val paddingRight = 20f
    private val paddingTop = 20f
    private val paddingBottom = 50f

    // Thiết lập dữ liệu và chạy animation
    fun setData(points: List<DataPoint>) {
        this.dataPoints = points
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

    // Vẽ biểu đồ
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (dataPoints.isEmpty()) {
            drawEmptyState(canvas)
            return
        }

        val chartWidth = width - paddingLeft - paddingRight
        val chartHeight = height - paddingTop - paddingBottom

        val maxValue = dataPoints.maxOfOrNull { it.value } ?: 1f
        val minValue = 0f
        val valueRange = max(maxValue - minValue, 1f)

        // Vẽ lưới
        val gridLines = 4
        for (i in 0..gridLines) {
            val y = paddingTop + (chartHeight / gridLines) * i
            canvas.drawLine(paddingLeft, y, width - paddingRight, y, gridPaint)
        }

        // Tính toán các điểm
        val points = mutableListOf<Pair<Float, Float>>()
        val stepX = chartWidth / max(dataPoints.size - 1, 1)

        for (i in dataPoints.indices) {
            val x = paddingLeft + stepX * i
            val normalizedValue = (dataPoints[i].value - minValue) / valueRange
            val y = paddingTop + chartHeight * (1 - normalizedValue * animatedProgress)
            points.add(Pair(x, y))
        }

        if (points.size >= 2) {
            // Vẽ vùng tô
            fillPath.reset()
            fillPath.moveTo(points[0].first, paddingTop + chartHeight)
            for (point in points) {
                fillPath.lineTo(point.first, point.second)
            }
            fillPath.lineTo(points.last().first, paddingTop + chartHeight)
            fillPath.close()
            
            fillPaint.color = Color.parseColor("#2000BCD4")
            canvas.drawPath(fillPath, fillPaint)

            // Vẽ đường
            path.reset()
            path.moveTo(points[0].first, points[0].second)
            for (i in 1 until points.size) {
                path.lineTo(points[i].first, points[i].second)
            }
            
            linePaint.color = Color.parseColor("#00BCD4")
            canvas.drawPath(path, linePaint)

            // Vẽ các điểm
            dotPaint.color = Color.parseColor("#00BCD4")
            for (point in points) {
                canvas.drawCircle(point.first, point.second, 6f, dotPaint)
            }
        }

        // Vẽ nhãn
        val labelStep = max(dataPoints.size / 7, 1)
        for (i in dataPoints.indices step labelStep) {
            val x = paddingLeft + stepX * i
            canvas.drawText(dataPoints[i].label, x, height - 15f, textPaint)
        }
    }

    // Vẽ trạng thái trống
    private fun drawEmptyState(canvas: Canvas) {
        val centerX = width / 2f
        val centerY = height / 2f
        textPaint.textSize = 32f
        canvas.drawText("Không có dữ liệu", centerX, centerY, textPaint)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        animator?.cancel()
    }
}
