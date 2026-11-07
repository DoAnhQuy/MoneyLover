package com.example.moneylover.ui.adapter

import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.moneylover.databinding.ItemCategoryChartBinding
import java.text.NumberFormat
import java.util.Locale

/**
 * Adapter cho danh sách danh mục trong biểu đồ
 * Hiển thị tên, số tiền, phần trăm và màu của từng danh mục
 */
class CategoryChartAdapter : RecyclerView.Adapter<CategoryChartAdapter.ViewHolder>() {

    private val items = mutableListOf<CategoryDisplayItem>()
    private val formatter = NumberFormat.getNumberInstance(Locale("vi", "VN"))

    // Model hiển thị cho danh mục
    data class CategoryDisplayItem(
        val name: String,           // Tên danh mục
        val amount: Double,         // Số tiền
        val rawPercentage: Float,   // Phần trăm gốc
        val color: Int,            // Màu sắc
        val displayPercent: Float   // Phần trăm hiển thị
    )

    // Cập nhật danh sách
    fun submitList(newItems: List<CategoryDisplayItem>) {
        items.clear()
        if (newItems.isNotEmpty()) {
            items.addAll(newItems)
        }
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemCategoryChartBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class ViewHolder(
        private val binding: ItemCategoryChartBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: CategoryDisplayItem) {
            binding.txtCategoryName.text = item.name
            binding.txtPercentage.text = String.format(Locale.US, "%.2f%%", item.displayPercent)
            binding.txtAmount.text = formatter.format(item.amount.toLong())

            // Set màu indicator
            val drawable = GradientDrawable()
            drawable.shape = GradientDrawable.OVAL
            drawable.setColor(item.color)
            binding.viewColor.background = drawable
        }
    }
}
