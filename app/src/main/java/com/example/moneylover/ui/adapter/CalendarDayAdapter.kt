package com.example.moneylover.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.moneylover.databinding.ItemDayBinding
import com.example.moneylover.ui.state.CalendarDayUi

/**
 * Adapter cho lịch ngày trong CalendarActivity
 * 
 * Hiển thị các ngày trong tháng dưới dạng lưới 7 cột (7 ngày/tuần)
 */
class CalendarDayAdapter(
    private var items: List<CalendarDayUi>,
    private var selectedDay: Int,
    private val onDayClick: (CalendarDayUi) -> Unit,
    private val onDayLongClick: (CalendarDayUi) -> Unit = {}
) : RecyclerView.Adapter<CalendarDayAdapter.VH>() {

    private var selDay = selectedDay

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemDayBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(items[position])
    }

    fun updateItems(newItems: List<CalendarDayUi>, newSelected: Int) {
        items = newItems
        selDay = newSelected
        notifyDataSetChanged()
    }

    inner class VH(private val binding: ItemDayBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: CalendarDayUi) {
            if (!item.inMonth || item.day == 0) {
                binding.tvDay.text = ""
                binding.tvDay.background = null
                binding.tvDay.setTextColor(binding.root.context.getColor(android.R.color.black))
            } else {
                binding.tvDay.text = item.day.toString()
                
                val textColor = when {
                    item.hasExpense && item.hasIncome -> android.R.color.holo_purple
                    item.hasExpense -> com.example.moneylover.R.color.black
                    item.hasIncome -> com.example.moneylover.R.color.black
                    else -> android.R.color.black
                }
                binding.tvDay.setTextColor(binding.root.context.getColor(textColor))
                
                if (item.hasExpense || item.hasIncome) {
                    binding.tvDay.setBackgroundResource(com.example.moneylover.R.drawable.day_has_transaction)
                } else {
                    binding.tvDay.background = null
                }
                
                if (item.day == selDay) {
                    binding.tvDay.setBackgroundResource(com.example.moneylover.R.drawable.day_selected)
                    binding.tvDay.setTextColor(binding.root.context.getColor(android.R.color.black))
                }
            }
            
            binding.root.setOnClickListener {
                if (item.inMonth && item.day != 0) {
                    selDay = item.day
                    notifyDataSetChanged()
                    onDayClick(item)
                }
            }
            
            binding.root.setOnLongClickListener {
                if (item.inMonth && item.day != 0) {
                    onDayLongClick(item)
                    true
                } else false
            }
        }
    }
}
