package com.example.moneylover.ui.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import com.example.moneylover.R

/**
 * Custom View vẽ biểu đồ tròn (Donut Chart)
 * Hiển thị các phần của biểu đồ tròn với animation
 */
class PieChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    // Model cho một phần biểu đồ
    data class PieSlice(
        val value: Float,    // Giá trị
        val color: Int,     // Màu sắc
        val label: String   // Nhãn
    )

    private val slices = mutableListOf<PieSlice>()
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val rect = RectF()
    
    // Danh sách màu mặc định
    private val defaultColors = listOf(
        Color.parseColor("#E53935"), // Đỏ
        Color.parseColor("#43A047"), // Xanh lá
        Color.parseColor("#1E88E5"), // Xanh dương
        Color.parseColor("#FB8C00"), // Cam
        Color.parseColor("#8E24AA"), // Tím
        Color.parseColor("#00ACC1"), // Cyan
        Color.parseColor("#FDD835"), // Vàng
        Color.parseColor("#6D4C41"), // Nâu
        Color.parseColor("#546E7A"), // Xám
        Color.parseColor("#D81B60")  // Hồng
    )

    // Thiết lập dữ liệu (Pair: value, colorIndex)
    fun setData(data: List<Pair<Float, Int>>) {
        slices.clear()
        data.forEachIndexed { index, (value, iconRes) ->
            val color = defaultColors[index % defaultColors.size]
            slices.add(PieSlice(value, color, ""))
        }
        invalidate()
    }

    // Thiết lập dữ liệu với nhãn (Triple: value, colorIndex, label)
    fun setDataWithLabels(data: List<Triple<Float, Int, String>>) {
        slices.clear()
        data.forEachIndexed { index, (value, iconRes, label) ->
            val color = defaultColors[index % defaultColors.size]
            slices.add(PieSlice(value, color, label))
        }
        invalidate()
    }

    // Vẽ biểu đồ tròn
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (slices.isEmpty()) {
            // Vẽ vòng tròn mặc định khi không có dữ liệu
            paint.color = Color.parseColor("#E0E0E0")
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 40f
            val cx = width / 2f
            val cy = height / 2f
            val radius = minOf(cx, cy) - 20f
            canvas.drawCircle(cx, cy, radius, paint)
            return
        }

        val total = slices.sumOf { it.value.toDouble() }.toFloat()
        if (total <= 0) return

        val cx = width / 2f
        val cy = height / 2f
        val radius = minOf(cx, cy) - 20f

        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 40f
        paint.strokeCap = Paint.Cap.BUTT

        var startAngle = -90f

        slices.forEach { slice ->
            val sweepAngle = (slice.value / total) * 360f
            
            paint.color = slice.color
            rect.set(cx - radius, cy - radius, cx + radius, cy + radius)
            canvas.drawArc(rect, startAngle, sweepAngle, false, paint)
            
            startAngle += sweepAngle
        }
    }
}
