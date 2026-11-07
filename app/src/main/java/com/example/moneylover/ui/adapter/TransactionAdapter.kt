package com.example.moneylover.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.moneylover.data.model.TransactionEntity
import com.example.moneylover.databinding.ItemTransactionBinding
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Adapter cho danh sách giao dịch trong MainActivity và CalendarActivity
 * 
 * Trong MVVM, Adapter nhận dữ liệu từ ViewModel thông qua ListAdapter
 * và chỉ có nhiệm vụ hiển thị (View thuần túy)
 */
class TransactionAdapter(
    private val onItemClick: ((TransactionEntity) -> Unit)? = null,
    private val onItemLongClick: ((TransactionEntity) -> Unit)? = null
) : ListAdapter<TransactionEntity, TransactionAdapter.TransactionViewHolder>(TransactionDiffCallback()) {

    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale("vi", "VN"))

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransactionViewHolder {
        val binding = ItemTransactionBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return TransactionViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TransactionViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class TransactionViewHolder(private val binding: ItemTransactionBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(transaction: TransactionEntity) {
            binding.imgIcon.setImageResource(transaction.iconRes)
            binding.txtCategory.text = transaction.categoryName

            val date = Date(transaction.date)
            binding.txtTime.text = timeFormat.format(date)

            if (!transaction.note.isNullOrBlank()) {
                binding.txtNote.visibility = android.view.View.VISIBLE
                binding.txtNote.text = transaction.note
            } else {
                binding.txtNote.visibility = android.view.View.GONE
            }

            val formattedAmount = formatCurrency(transaction.amount)
            val prefix = when (transaction.type) {
                "EXPENSE" -> "-"
                "INCOME" -> "+"
                else -> ""
            }
            binding.txtAmount.text = "$prefix$formattedAmount"

            binding.txtAmount.setTextColor(binding.root.context.getColor(com.example.moneylover.R.color.black))

            binding.root.setOnClickListener {
                onItemClick?.invoke(transaction)
            }

            binding.root.setOnLongClickListener {
                onItemLongClick?.invoke(transaction)
                true
            }
        }

        private fun formatCurrency(amount: Double): String {
            val formatter = NumberFormat.getNumberInstance(Locale("vi", "VN"))
            return formatter.format(amount) + "đ"
        }
    }

    class TransactionDiffCallback : DiffUtil.ItemCallback<TransactionEntity>() {
        override fun areItemsTheSame(oldItem: TransactionEntity, newItem: TransactionEntity): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: TransactionEntity, newItem: TransactionEntity): Boolean {
            return oldItem == newItem
        }
    }
}
