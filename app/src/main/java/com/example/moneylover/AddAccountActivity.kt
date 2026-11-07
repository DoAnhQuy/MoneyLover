package com.example.moneylover

import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.example.moneylover.data.database.AppDatabase
import com.example.moneylover.data.model.AccountEntity
import com.example.moneylover.data.repository.AccountRepository
import com.example.moneylover.databinding.LayoutAddAccountBinding
import com.example.moneylover.ui.adapter.AccountIconAdapter
import kotlinx.coroutines.launch

class AddAccountActivity : AppCompatActivity() {

    private lateinit var binding: LayoutAddAccountBinding
    private lateinit var accountRepository: AccountRepository
    private var selectedIconRes: Int = R.drawable.ic_wallet
    private var selectedColor: Int = 0xFFE53935.toInt()
    private var selectedType: String = "cash"
    private var editingAccountId: Long? = null

    companion object {
        const val EXTRA_ACCOUNT_ID = "extra_account_id"
        const val EXTRA_ACCOUNT_NAME = "extra_account_name"
        const val EXTRA_ACCOUNT_BALANCE = "extra_account_balance"
        const val EXTRA_ACCOUNT_ICON = "extra_account_icon"
        const val EXTRA_ACCOUNT_COLOR = "extra_account_color"
        const val EXTRA_ACCOUNT_TYPE = "extra_account_type"
    }

    private val accountIcons = listOf(
        R.drawable.ic_wallet to 0xFF6D4C41.toInt(),       // Nâu - Ví
        R.drawable.ic_money_bag to 0xFFE53935.toInt(),   // Đỏ - Túi tiền
        R.drawable.ic_salary to 0xFF43A047.toInt(),       // Xanh lá - Lương
        R.drawable.ic_bank to 0xFF1E88E5.toInt(),         // Xanh dương - Ngân hàng
        R.drawable.ic_credit_card to 0xFFFB8C00.toInt(),  // Cam - Thẻ tín dụng
        R.drawable.ic_savings to 0xFF8E24AA.toInt(),      // Tím - Tiết kiệm
        R.drawable.ic_business to 0xFF00ACC1.toInt(),     // Cyan - Kinh doanh
        R.drawable.ic_investment to 0xFFFDD835.toInt(),   // Vàng - Đầu tư
        R.drawable.ic_home to 0xFF795548.toInt(),         // Nâu đậm - Nhà
        R.drawable.ic_car to 0xFF607D8B.toInt(),          // Xám - Xe
        R.drawable.ic_school to 0xFF3F51B5.toInt(),      // Indigo - Trường học
        R.drawable.ic_medical to 0xFFE91E63.toInt()      // Hồng - Y tế
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = LayoutAddAccountBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val database = AppDatabase.getDatabase(applicationContext)
        accountRepository = AccountRepository(database.accountDao())

        editingAccountId = intent.getLongExtra(EXTRA_ACCOUNT_ID, -1).takeIf { it != -1L }
        
        setupToolbar()
        setupIconPreview()
        setupTypeSelection()
        setupIconRecyclerView()
        setupButtons()
        setupBalanceInput()
        loadAccountData()
    }

    private fun setupToolbar() {
        binding.btnClose.setOnClickListener {
            finish()
        }
        
        if (editingAccountId != null) {
            binding.txtTitle.text = "Sửa tài khoản"
        }
    }

    private fun setupIconPreview() {
        updateIconPreview()
    }

    private fun updateIconPreview() {
        binding.imgSelectedIcon.setImageResource(selectedIconRes)
        
        // Cập nhật màu nền cho hình tròn
        val backgroundDrawable = binding.viewBackgroundColor.background as? GradientDrawable
            ?: GradientDrawable().also { 
                it.shape = GradientDrawable.OVAL
                binding.viewBackgroundColor.background = it
            }
        backgroundDrawable.setColor(selectedColor)
    }

    private fun setupTypeSelection() {
        binding.typeCash.setOnClickListener { selectType("cash") }
        binding.typeBank.setOnClickListener { selectType("bank") }
        binding.typeEWallet.setOnClickListener { selectType("e_wallet") }
        binding.typeCredit.setOnClickListener { selectType("credit") }
        binding.typeVirtual.setOnClickListener { selectType("virtual") }
        binding.typeInvestment.setOnClickListener { selectType("investment") }
        binding.typeDebt.setOnClickListener { selectType("debt") }
    }

    private fun selectType(type: String) {
        selectedType = type
        
        // Reset tất cả
        listOf(
            binding.typeCash to "cash",
            binding.typeBank to "bank",
            binding.typeEWallet to "e_wallet",
            binding.typeCredit to "credit",
            binding.typeVirtual to "virtual",
            binding.typeInvestment to "investment",
            binding.typeDebt to "debt"
        ).forEach { (view, t) ->
            val isSelected = t == type
            view.setBackgroundResource(if (isSelected) R.drawable.bg_type_selected else R.drawable.bg_type_unselected)
            view.alpha = if (isSelected) 1f else 0.6f
        }
    }

    private fun loadAccountData() {
        editingAccountId?.let { id ->
            lifecycleScope.launch {
                val account = accountRepository.getAccountById(id)
                account?.let {
                    binding.editAccountName.setText(it.name)
                    binding.editInitialBalance.setText(it.balance.toString())
                    selectedIconRes = it.iconRes
                    selectedColor = it.color
                    selectedType = it.type
                    updateIconPreview()
                    selectType(it.type)
                    
                    val position = accountIcons.indexOfFirst { pair -> pair.first == it.iconRes }
                    if (position >= 0) {
                        setupIconRecyclerViewWithSelection(position)
                    }
                }
            }
        }
    }

    private fun setupIconRecyclerViewWithSelection(selectedPos: Int) {
        val adapter = AccountIconAdapter(
            icons = accountIcons,
            selectedPosition = selectedPos,
            onIconSelected = { iconRes, color ->
                selectedIconRes = iconRes
                selectedColor = color
                updateIconPreview()
            }
        )

        binding.recyclerIcons.layoutManager = GridLayoutManager(this, 4)
        binding.recyclerIcons.adapter = adapter
    }

    private fun setupIconRecyclerView() {
        val adapter = AccountIconAdapter(
            icons = accountIcons,
            selectedPosition = accountIcons.indexOfFirst { it.first == selectedIconRes }.takeIf { it >= 0 } ?: 0,
            onIconSelected = { iconRes, color ->
                selectedIconRes = iconRes
                selectedColor = color
                updateIconPreview()
            }
        )

        binding.recyclerIcons.layoutManager = GridLayoutManager(this, 4)
        binding.recyclerIcons.adapter = adapter
    }

    private fun setupButtons() {
        binding.btnCancel.setOnClickListener {
            finish()
        }

        binding.btnSave.setOnClickListener {
            saveAccount()
        }
    }

    private fun setupBalanceInput() {
        binding.editInitialBalance.addTextChangedListener(object : TextWatcher {
            private var isFormatting = false

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                if (isFormatting) return
                isFormatting = true

                val input = s?.toString()?.replace(".", "")?.replace(",", "") ?: ""
                
                if (input.isNotEmpty()) {
                    val number = input.toLongOrNull() ?: 0L
                    val formatted = formatCurrency(number)
                    
                    // Chỉ update nếu format khác với text hiện tại
                    if (formatted != s.toString()) {
                        val cursorPosition = binding.editInitialBalance.selectionStart
                        binding.editInitialBalance.setText(formatted)
                        
                        // Đặt con trỏ ở vị trí hợp lý
                        val newPosition = minOf(cursorPosition, formatted.length)
                        binding.editInitialBalance.setSelection(newPosition)
                    }
                }

                isFormatting = false
            }
        })
    }

    private fun formatCurrency(amount: Long): String {
        return String.format("%,d", amount).replace(",", ".")
    }

    private fun saveAccount() {
        val name = binding.editAccountName.text.toString().trim()
        val balanceText = binding.editInitialBalance.text.toString().trim()

        if (name.isEmpty()) {
            binding.editAccountName.error = "Vui lòng nhập tên tài khoản"
            binding.editAccountName.requestFocus()
            return
        }

        val balance = if (balanceText.isNotEmpty()) {
            balanceText.replace(",", "").replace(".", "").toDoubleOrNull() ?: 0.0
        } else {
            0.0
        }

        // Xác định isLiability dựa trên loại tài khoản
        val isLiability = selectedType == "credit" || selectedType == "debt"

        lifecycleScope.launch {
            if (editingAccountId != null) {
                val account = AccountEntity(
                    id = editingAccountId!!,
                    userId = 1,
                    name = name,
                    balance = balance,
                    iconRes = selectedIconRes,
                    color = selectedColor,
                    type = selectedType,
                    isLiability = isLiability
                )
                accountRepository.update(account)
                Toast.makeText(this@AddAccountActivity, "Đã cập nhật tài khoản", Toast.LENGTH_SHORT).show()
            } else {
                val account = AccountEntity(
                    userId = 1,
                    name = name,
                    balance = balance,
                    iconRes = selectedIconRes,
                    color = selectedColor,
                    type = selectedType,
                    isLiability = isLiability
                )
                accountRepository.insert(account)
                Toast.makeText(this@AddAccountActivity, "Đã thêm tài khoản", Toast.LENGTH_SHORT).show()
            }
            setResult(RESULT_OK)
            finish()
        }
    }
}
