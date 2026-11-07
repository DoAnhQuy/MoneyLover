package com.example.moneylover

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.moneylover.data.model.AccountEntity
import com.example.moneylover.databinding.ItemAccountBinding
import java.text.NumberFormat
import java.util.Locale

/**
 * Adapter cho danh sách tài khoản
 */
class AccountAdapter(
    private val accounts: List<AccountEntity>,
    private val onItemClick: (AccountEntity) -> Unit,
    private val onMenuClick: (AccountEntity, View) -> Unit,
    private val onPinClick: ((AccountEntity) -> Unit)? = null
) : RecyclerView.Adapter<AccountAdapter.VH>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemAccountBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(accounts[position])
    }

    override fun getItemCount(): Int = accounts.size

    inner class VH(
        private val binding: ItemAccountBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(account: AccountEntity) {
            binding.imgIcon.setImageResource(account.iconRes)
            binding.imgIcon.setBackgroundColor(account.color)
            binding.txtAccountName.text = account.name
            binding.txtAccountBalance.text = formatCurrency(account.balance)

            // Hiển thị icon ghim nếu được ghim
            binding.imgPin.visibility = if (account.isPinned) View.VISIBLE else View.GONE

            binding.root.setOnClickListener {
                onItemClick(account)
            }

            binding.btnMenu.setOnClickListener {
                onMenuClick(account, it)
            }

            binding.imgPin.setOnClickListener {
                onPinClick?.invoke(account)
            }
        }

        private fun formatCurrency(amount: Double): String {
            val formatter = NumberFormat.getNumberInstance(Locale("vi", "VN"))
            return formatter.format(amount) + "đ"
        }
    }
}
