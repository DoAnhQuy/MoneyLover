package com.example.moneylover.ui.adapter

import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.moneylover.R
import com.example.moneylover.data.model.AccountEntity
import com.example.moneylover.databinding.ItemSelectAccountBinding
import com.example.moneylover.databinding.ItemSelectNoAccountBinding
import java.text.NumberFormat
import java.util.Locale

/**
 * Adapter cho việc chọn tài khoản trong bottom sheet
 * Có thêm tùy chọn "Không liên kết tài khoản"
 */
class AccountSelectWithNoneAdapter(
    private val accounts: List<AccountEntity>,              // Danh sách tài khoản
    private var selectedAccountId: Long? = null,            // ID tài khoản đang chọn
    private val onAccountSelected: (AccountEntity?) -> Unit // Callback: null = không liên kết
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        const val VIEW_TYPE_NORMAL = 0   // Item tài khoản thường
        const val VIEW_TYPE_NONE = 1    // Item "Không liên kết"
    }

    override fun getItemViewType(position: Int): Int {
        return if (position == accounts.size) VIEW_TYPE_NONE else VIEW_TYPE_NORMAL
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_NONE -> {
                val binding = ItemSelectNoAccountBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
                NoneViewHolder(binding)
            }
            else -> {
                val binding = ItemSelectAccountBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
                NormalViewHolder(binding)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is NormalViewHolder -> holder.bind(accounts[position])
            is NoneViewHolder -> holder.bind()
        }
    }

    override fun getItemCount(): Int = accounts.size + 1 // +1 cho item "Không liên kết"

    // Cập nhật tài khoản được chọn
    fun setSelectedAccount(accountId: Long?) {
        val oldId = selectedAccountId
        selectedAccountId = accountId
        notifyDataSetChanged()
    }

    // ViewHolder cho item tài khoản thường
    inner class NormalViewHolder(
        private val binding: ItemSelectAccountBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(account: AccountEntity) {
            binding.imgAccountIcon.setImageResource(account.iconRes)
            
            val drawable = binding.imgAccountIcon.background as? GradientDrawable
            drawable?.setColor(account.color)
            
            binding.txtAccountName.text = account.name
            binding.txtAccountBalance.text = formatCurrency(account.balance)

            binding.imgSelected.visibility = if (account.id == selectedAccountId) View.VISIBLE else View.GONE

            binding.root.setOnClickListener {
                onAccountSelected(account)
            }
        }

        private fun formatCurrency(amount: Double): String {
            val formatter = NumberFormat.getNumberInstance(Locale("vi", "VN"))
            return formatter.format(amount) + "đ"
        }
    }

    // ViewHolder cho item "Không liên kết"
    inner class NoneViewHolder(
        private val binding: ItemSelectNoAccountBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind() {
            // Icon màu xám
            val drawable = binding.imgAccountIcon.background as? GradientDrawable
            drawable?.setColor(0xFF9E9E9E.toInt())

            // Hiển thị check nếu không có tài khoản nào được chọn
            binding.imgSelected.visibility = if (selectedAccountId == null) View.VISIBLE else View.GONE

            binding.root.setOnClickListener {
                onAccountSelected(null)
            }
        }
    }
}
