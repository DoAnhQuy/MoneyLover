package com.example.moneylover.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.moneylover.databinding.ItemYearPickerBinding

/**
 * Adapter cho việc chọn năm trong thống kê
 * Hiển thị danh sách các năm có dữ liệu
 */
class YearPickerAdapter(
    private val years: List<YearItem>,       // Danh sách năm
    private val selectedYear: Int,           // Năm đang được chọn
    private val onItemClick: (Int) -> Unit   // Callback khi click
) : RecyclerView.Adapter<YearPickerAdapter.YearVH>() {

    // Model cho item năm
    data class YearItem(val year: Int, val isAll: Boolean = false)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): YearVH {
        val binding = ItemYearPickerBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return YearVH(binding)
    }

    override fun onBindViewHolder(holder: YearVH, position: Int) {
        holder.bind(years[position])
    }

    override fun getItemCount(): Int = years.size

    inner class YearVH(private val binding: ItemYearPickerBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: YearItem) {
            // Hiển thị "Tất cả" hoặc số năm
            binding.txtYear.text = if (item.isAll) "Tất cả" else item.year.toString()
            
            // Kiểm tra xem có phải năm đang chọn không
            val isSelected = if (item.isAll) {
                selectedYear == 0
            } else {
                selectedYear == item.year
            }
            
            // Hiển thị/ẩn icon check
            binding.imgSelected.visibility = if (isSelected) View.VISIBLE else View.GONE
            
            binding.layoutItem.setOnClickListener {
                onItemClick(item.year)
            }
        }
    }
}
