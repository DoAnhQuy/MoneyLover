package com.example.moneylover.ui.adapter

import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.moneylover.databinding.ItemAccountIconBinding

/**
 * Adapter cho grid chọn icon tài khoản trong AddAccountActivity
 * Hiển thị các icon với màu sắc, hỗ trợ chọn một icon
 */
class AccountIconAdapter(
    private val icons: List<Pair<Int, Int>>, // Pair(iconRes, color)
    private var selectedPosition: Int = 0,    // Vị trí đang được chọn
    private val onIconSelected: (Int, Int) -> Unit // Callback: (iconRes, color)
) : RecyclerView.Adapter<AccountIconAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemAccountIconBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(icons[position], position == selectedPosition)
    }

    override fun getItemCount(): Int = icons.size

    // Cập nhật vị trí được chọn
    fun setSelectedPosition(position: Int) {
        val oldPosition = selectedPosition
        selectedPosition = position
        notifyItemChanged(oldPosition)
        notifyItemChanged(position)
    }

    inner class ViewHolder(
        private val binding: ItemAccountIconBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(icon: Pair<Int, Int>, isSelected: Boolean) {
            binding.imgAccountIcon.setImageResource(icon.first)

            // Set màu nền
            val drawable = binding.imgAccountIcon.background as? GradientDrawable
            drawable?.setColor(icon.second)

            // Hiển thị/ẩn dấu check
            binding.imgAccountSelected.visibility = if (isSelected) View.VISIBLE else View.GONE

            binding.root.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    setSelectedPosition(position)
                    onIconSelected(icon.first, icon.second)
                }
            }
        }
    }
}
