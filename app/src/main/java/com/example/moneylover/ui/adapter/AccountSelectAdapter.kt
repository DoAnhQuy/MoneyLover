package com.example.moneylover.ui.adapter

import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.moneylover.data.model.AccountEntity
import com.example.moneylover.databinding.ItemSelectAccountBinding
import java.text.NumberFormat
import java.util.Locale

/**
 * Adapter cho việc chọn tài khoản trong bottom sheet
 * Hiển thị danh sách tài khoản để chọn khi thêm giao dịch
 */
class AccountSelectAdapter(
    private val accounts: List<AccountEntity>,           // Danh sách tài khoản
    private var selectedAccountId: Long? = null,         // ID tài khoản đang chọn
    private val onAccountSelected: (AccountEntity) -> Unit // Callback khi chọn
) : RecyclerView.Adapter<AccountSelectAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemSelectAccountBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(accounts[position])
    }

    override fun getItemCount(): Int = accounts.size

    // Cập nhật tài khoản được chọn
    fun setSelectedAccount(accountId: Long?) {
        val oldId = selectedAccountId
        selectedAccountId = accountId
        // Thông báo thay đổi cho item cũ và mới
        accounts.forEachIndexed { index, account ->
            if (account.id == oldId || account.id == accountId) {
                notifyItemChanged(index)
            }
        }
    }

    inner class ViewHolder(
        private val binding: ItemSelectAccountBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(account: AccountEntity) {
            binding.imgAccountIcon.setImageResource(account.iconRes)
            
            // Set màu nền icon
            val drawable = binding.imgAccountIcon.background as? GradientDrawable
            drawable?.setColor(account.color)
            
            binding.txtAccountName.text = account.name
            binding.txtAccountBalance.text = formatCurrency(account.balance)

            // Hiển thị/ẩn dấu check
            binding.imgSelected.visibility = if (account.id == selectedAccountId) View.VISIBLE else View.GONE

            binding.root.setOnClickListener {
                onAccountSelected(account)
            }
        }

        // Format số tiền VND
        private fun formatCurrency(amount: Double): String {
            val formatter = NumberFormat.getNumberInstance(Locale("vi", "VN"))
            return formatter.format(amount) + "đ"
        }
    }
}
