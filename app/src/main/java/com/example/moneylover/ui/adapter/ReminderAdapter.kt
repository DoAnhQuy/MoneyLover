package com.example.moneylover.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.moneylover.data.model.ReminderEntity
import com.example.moneylover.databinding.ItemReminderBinding

/**
 * Adapter cho danh sách lời nhắc nhở trong ReminderActivity
 * Hiển thị thông tin lời nhắc với switch bật/tắt
 */
class ReminderAdapter(
    private val onItemClick: (ReminderEntity) -> Unit,           // Click để sửa
    private val onSwitchChanged: (ReminderEntity, Boolean) -> Unit, // Switch thay đổi
    private val onItemLongClick: (ReminderEntity) -> Unit        // Long click để xóa
) : ListAdapter<ReminderEntity, ReminderAdapter.ReminderVH>(ReminderDiff()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReminderVH {
        val binding = ItemReminderBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ReminderVH(binding)
    }

    override fun onBindViewHolder(holder: ReminderVH, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ReminderVH(private val binding: ItemReminderBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(reminder: ReminderEntity) {
            binding.txtName.text = reminder.name
            binding.txtMessage.text = reminder.message
            
            // Format tần suất
            binding.txtFrequency.text = when (reminder.frequency) {
                "DAILY" -> "Hàng ngày"
                "WEEKLY" -> "Hàng tuần"
                "MONTHLY" -> "Hàng tháng"
                else -> reminder.frequency
            }
            
            // Format giờ
            binding.txtTime.text = String.format("%02d:%02d", reminder.timeHour, reminder.timeMinute)
            
            // Trạng thái switch
            binding.switchEnabled.setOnCheckedChangeListener(null)
            binding.switchEnabled.isChecked = reminder.isEnabled
            binding.switchEnabled.setOnCheckedChangeListener { _, isChecked ->
                onSwitchChanged(reminder, isChecked)
            }
            
            binding.root.setOnClickListener {
                onItemClick(reminder)
            }

            binding.root.setOnLongClickListener {
                onItemLongClick(reminder)
                true
            }
        }
    }

    // DiffUtil để tối ưu hiệu suất khi cập nhật list
    class ReminderDiff : DiffUtil.ItemCallback<ReminderEntity>() {
        override fun areItemsTheSame(oldItem: ReminderEntity, newItem: ReminderEntity): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: ReminderEntity, newItem: ReminderEntity): Boolean {
            return oldItem == newItem
        }
    }
}
