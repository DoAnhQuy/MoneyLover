package com.example.moneylover.ui.adapter

import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.moneylover.R
import com.example.moneylover.TransactionCategory
import com.example.moneylover.databinding.ItemCategoryGridBinding

/**
 * Adapter cho grid categories trong AddTransactionActivity
 *
 * Hiển thị danh sách danh mục giao dịch dưới dạng lưới 4 cột
 * Hỗ trợ highlight category đang được chọn (khi sửa giao dịch)
 */
class CategoryAdapter(
    private val categories: List<TransactionCategory>,
    private val selectedCategoryId: Int? = null,
    private val onCategoryClick: (TransactionCategory) -> Unit
) : RecyclerView.Adapter<CategoryAdapter.CategoryVH>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryVH {
        val binding = ItemCategoryGridBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return CategoryVH(binding)
    }

    override fun onBindViewHolder(holder: CategoryVH, position: Int) {
        val category = categories[position]
        val isSelected = category.id == selectedCategoryId
        holder.bind(category, isSelected)
        holder.itemView.setOnClickListener {
            onCategoryClick(category)
        }
    }

    override fun getItemCount(): Int = categories.size

    class CategoryVH(private val binding: ItemCategoryGridBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(category: TransactionCategory, isSelected: Boolean) {
            binding.ivCategoryIcon.setImageResource(category.iconRes)
            binding.tvCategoryName.text = category.name

            // Highlight category đang được chọn (khi sửa giao dịch)
            if (isSelected) {
                binding.ivCheckmark.visibility = View.VISIBLE
                binding.tvCategoryName.setTextColor(itemView.context.getColor(R.color.primary))
            } else {
                binding.ivCheckmark.visibility = View.GONE
                binding.tvCategoryName.setTextColor(itemView.context.getColor(R.color.black))
            }
        }
    }

    companion object {
        fun createLayoutManager(context: android.content.Context): GridLayoutManager {
            return GridLayoutManager(context, 4).apply {
                orientation = RecyclerView.VERTICAL
            }
        }
    }
}
