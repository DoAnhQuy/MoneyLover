package com.example.moneylover.ui.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.moneylover.databinding.ItemMonthStatBinding
import com.example.moneylover.ui.state.MonthStatUi
import java.text.NumberFormat
import java.util.Locale

/**
 * Adapter cho danh sách thống kê tháng trong MonthlyStatsActivity
 * Hiển thị tổng chi tiêu, thu nhập, số dư của từng tháng
 */
class MonthStatAdapter(
    private val onItemClick: ((MonthStatUi) -> Unit)? = null
) : ListAdapter<MonthStatUi, MonthStatAdapter.MonthStatVH>(MonthStatDiff()) {

    private val currencyFormatter = NumberFormat.getNumberInstance(Locale("vi", "VN"))

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MonthStatVH {
        val binding = ItemMonthStatBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MonthStatVH(binding)
    }

    override fun onBindViewHolder(holder: MonthStatVH, position: Int) {
        holder.bind(getItem(position))
    }

    inner class MonthStatVH(private val binding: ItemMonthStatBinding) : RecyclerView.ViewHolder(binding.root) {
        @SuppressLint("SetTextI18n")
        fun bind(item: MonthStatUi) {
            // Hiển thị tên tháng
            val monthNames = listOf("", "Tháng 1", "Tháng 2", "Tháng 3", "Tháng 4",
                "Tháng 5", "Tháng 6", "Tháng 7", "Tháng 8",
                "Tháng 9", "Tháng 10", "Tháng 11", "Tháng 12")
            binding.txtMonthYear.text = "${monthNames[item.month]} ${item.year}"

            // Format số tiền với dấu +/-
            binding.txtExpense.text = "-${formatCurrency(item.totalExpense.toLong())}"
            binding.txtIncome.text = "+${formatCurrency(item.totalIncome.toLong())}"
            binding.txtBalance.text = "${if (item.balance >= 0) "+" else ""}${formatCurrency(item.balance.toLong())}"

            // Set màu cho số dư (xanh dương nếu dương, đỏ nếu âm)
            binding.txtBalance.setTextColor(
                if (item.balance >= 0) binding.root.context.getColor(android.R.color.holo_green_dark)
                else binding.root.context.getColor(android.R.color.holo_red_dark)
            )

            // Cập nhật chiều cao biểu đồ cột
            updateBarChart(item)

            binding.root.setOnClickListener {
                onItemClick?.invoke(item)
            }
        }

        // Format số tiền VND
        private fun formatCurrency(amount: Long): String {
            return String.format("%,d", kotlin.math.abs(amount)).replace(",", ".") + "đ"
        }

        // Cập nhật biểu đồ cột mini
        private fun updateBarChart(item: MonthStatUi) {
            // Nếu có ngân sách, dùng ngân sách làm mốc (100%)
            // Nếu không có ngân sách, dùng max của expense/income/balance
            val maxValue = if (item.budget > 0) {
                item.budget
            } else {
                maxOf(item.totalExpense, item.totalIncome, kotlin.math.abs(item.balance), 1.0)
            }
            
            if (maxValue > 0) {
                val parentHeight = 40 // chiều cao container (dp)

                // Tính chiều cao theo tỷ lệ
                val expenseHeight = (item.totalExpense / maxValue * parentHeight).toInt().coerceAtLeast(4)
                val incomeHeight = (item.totalIncome / maxValue * parentHeight).toInt().coerceAtLeast(4)
                val balanceHeight = (kotlin.math.abs(item.balance) / maxValue * parentHeight).toInt().coerceAtLeast(4)

                // Set chiều cao cho các cột
                binding.barExpense.layoutParams = (binding.barExpense.layoutParams as ViewGroup.LayoutParams).apply {
                    height = (expenseHeight * binding.root.context.resources.displayMetrics.density).toInt()
                }
                binding.barIncome.layoutParams = (binding.barIncome.layoutParams as ViewGroup.LayoutParams).apply {
                    height = (incomeHeight * binding.root.context.resources.displayMetrics.density).toInt()
                }
                binding.barBalance.layoutParams = (binding.barBalance.layoutParams as ViewGroup.LayoutParams).apply {
                    height = (balanceHeight * binding.root.context.resources.displayMetrics.density).toInt()
                }
            }
        }
    }

    // DiffUtil để tối ưu hiệu suất
    class MonthStatDiff : DiffUtil.ItemCallback<MonthStatUi>() {
        override fun areItemsTheSame(oldItem: MonthStatUi, newItem: MonthStatUi): Boolean {
            return oldItem.month == newItem.month && oldItem.year == newItem.year
        }

        override fun areContentsTheSame(oldItem: MonthStatUi, newItem: MonthStatUi): Boolean {
            return oldItem == newItem
        }
    }
}

// Hàm mở rộng để tạo MonthStatAdapter với list data
fun createMonthStatAdapter(
    monthStats: List<MonthStatUi>,
    onItemClick: ((MonthStatUi) -> Unit)? = null
): MonthStatAdapter {
    return MonthStatAdapter(onItemClick).apply {
        submitList(monthStats)
    }
}
