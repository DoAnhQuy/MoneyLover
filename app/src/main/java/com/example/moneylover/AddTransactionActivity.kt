package com.example.moneylover

import android.content.Intent
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.moneylover.R
import com.example.moneylover.data.database.AppDatabase
import com.example.moneylover.data.model.AccountEntity
import com.example.moneylover.data.model.TransactionEntity
import com.example.moneylover.databinding.ActivityAddTransactionBinding
import com.example.moneylover.databinding.BottomSheetAddAccountBinding
import com.example.moneylover.databinding.BottomSheetSelectAccountBinding
import com.example.moneylover.ui.adapter.AccountIconAdapter
import com.example.moneylover.ui.adapter.AccountSelectAdapter
import com.example.moneylover.ui.adapter.AccountSelectWithNoneAdapter
import com.example.moneylover.ui.adapter.CategoryAdapter
import com.example.moneylover.ui.state.TransactionTypeState
import com.example.moneylover.ui.viewmodel.AddTransactionViewModel
import com.example.moneylover.TransactionCategory
import com.example.moneylover.utils.NotificationHelper
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * AddTransactionActivity - Màn hình thêm/sửa giao dịch
 * 
 * Trong MVVM:
 * - View chỉ lo hiển thị UI và gọi ViewModel methods
 * - KHÔNG chứa logic xử lý database
 * - Observe UiState để cập nhật UI tự động
 */
class AddTransactionActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddTransactionBinding
    private lateinit var categoryAdapter: CategoryAdapter

    // ViewModel: quản lý trạng thái và logic nghiệp vụ
    private val viewModel: AddTransactionViewModel by viewModels()
    
    // Activity result launcher for AddAccountActivity
    private val addAccountLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            // Account added successfully, ViewModel will auto-refresh via Flow
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddTransactionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Lấy thông tin ngày được chọn
        val selectedYear = intent.getIntExtra(EXTRA_SELECTED_YEAR, java.util.Calendar.getInstance().get(java.util.Calendar.YEAR))
        val selectedMonth = intent.getIntExtra(EXTRA_SELECTED_MONTH, java.util.Calendar.getInstance().get(java.util.Calendar.MONTH) + 1)
        val selectedDay = intent.getIntExtra(EXTRA_SELECTED_DAY, java.util.Calendar.getInstance().get(java.util.Calendar.DAY_OF_MONTH))
        viewModel.setSelectedDate(selectedYear, selectedMonth, selectedDay)

        // Kiểm tra chế độ sửa
        val transactionId = intent.getLongExtra(EXTRA_TRANSACTION_ID, -1)
        if (transactionId != -1L) {
            viewModel.startEditMode(transactionId)
        }

        setupUI()
        observeUiState()

        // Load categories ban đầu (sau observeUiState để đảm bảo state đã sẵn sàng)
        // Nếu là chế độ sửa, load sẽ được gọi lại trong renderUi khi state thay đổi
        if (transactionId == -1L) {
            // Chế độ thêm mới - load expense categories mặc định
            loadExpenseCategories()
        }
    }

    companion object {
        const val EXTRA_TRANSACTION_ID = "extra_transaction_id"
        const val EXTRA_SELECTED_YEAR = "extra_selected_year"
        const val EXTRA_SELECTED_MONTH = "extra_selected_month"
        const val EXTRA_SELECTED_DAY = "extra_selected_day"
        const val RESULT_DATE = "result_date"
    }

    /**
     * Observe UiState từ ViewModel
     */
    private fun observeUiState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collectLatest { state ->
                    renderUi(state)
                }
            }
        }
    }

    /**
     * Render UI dựa trên state
     */
    private fun renderUi(state: com.example.moneylover.ui.state.AddTransactionUiState) {
        // Tiêu đề
        binding.txtTitle.text = if (state.isEditMode) "Sửa giao dịch" else "Thêm giao dịch"

        // Số tiền
        val displayAmount = if (state.amount == "0" || state.amount.isEmpty()) "0" else viewModel.formatAmount(state.amount)
        binding.txtAmount.text = displayAmount

        // Tabs
        updateTabSelection(state.currentType)

        // Categories - load đúng loại khi edit
        updateCategoriesVisibility(state.currentType)
        if (state.currentType == TransactionTypeState.INCOME) {
            loadIncomeCategories()
        } else if (state.currentType == TransactionTypeState.EXPENSE) {
            loadExpenseCategories()
        }

        // Hiển thị số tiền (hiện khi đang sửa HOẶC đã chọn category)
        binding.layoutAmount.visibility = if (state.isEditMode || state.selectedCategory != null) View.VISIBLE else View.GONE

        // Cập nhật icon tài khoản trên nút
        updateAccountButton(state)

        // Transfer layout
        binding.layoutTransfer.visibility = if (state.currentType == TransactionTypeState.TRANSFER) View.VISIBLE else View.GONE
        
        // Numpad visibility - hiển thị khi đã chọn đủ 2 tài khoản
        val canTransfer = state.fromAccount != null && state.toAccount != null
        binding.numpadLayout.root.visibility = if (state.currentType == TransactionTypeState.TRANSFER && canTransfer) View.VISIBLE else View.GONE
        
        // Hiển thị ô nhập số tiền cho transfer
        binding.layoutTransferAmount.visibility = if (state.currentType == TransactionTypeState.TRANSFER && canTransfer) View.VISIBLE else View.GONE
        val transferDisplayAmount = if (state.amount == "0" || state.amount.isEmpty()) "0" else viewModel.formatAmount(state.amount)
        binding.txtTransferAmount.text = transferDisplayAmount
        
        // Hiển thị ghi chú khi đã nhập số tiền
        val hasAmount = state.amount.isNotEmpty() && state.amount != "0" && state.amount != ""
        binding.layoutTransferNote.visibility = if (state.currentType == TransactionTypeState.TRANSFER && hasAmount) View.VISIBLE else View.GONE
        
        // Update selected accounts display
        updateAccountCardsTransfer(state)
    }
    
    /**
     * Cập nhật icon tài khoản trên nút chọn
     */
    private fun updateAccountButton(state: com.example.moneylover.ui.state.AddTransactionUiState) {
        if (state.selectedAccount != null) {
            binding.btnSelectAccount.setImageResource(state.selectedAccount.iconRes)
            val drawable = binding.btnSelectAccount.background as? GradientDrawable
            drawable?.setColor(state.selectedAccount.color)
        } else {
            binding.btnSelectAccount.setImageResource(R.drawable.ic_wallet)
            val drawable = binding.btnSelectAccount.background as? GradientDrawable
            drawable?.setColor(getColor(R.color.report_yellow))
        }
    }
    
    /**
     * Cập nhật hiển thị các ô tài khoản đã chọn (cho chuyển khoản)
     */
    private fun updateAccountCardsTransfer(state: com.example.moneylover.ui.state.AddTransactionUiState) {
        // From account
        if (state.fromAccount != null) {
            binding.imgFromAccountIcon.setImageResource(state.fromAccount.iconRes)
            binding.imgFromAccountIcon.setBackgroundColor(state.fromAccount.color)
            binding.txtFromAccountName.text = state.fromAccount.name
            binding.txtFromAccountName.setTextColor(getColor(R.color.black))
            binding.imgFromAdd.visibility = View.GONE
            binding.txtFromLabel.visibility = View.GONE
        } else {
            binding.imgFromAccountIcon.setImageResource(android.R.drawable.ic_input_add)
            binding.imgFromAccountIcon.setBackgroundColor(getColor(android.R.color.transparent))
            binding.txtFromAccountName.text = "Chọn tài khoản"
            binding.txtFromAccountName.setTextColor(getColor(R.color.report_yellow))
            binding.imgFromAdd.visibility = View.VISIBLE
            binding.txtFromLabel.visibility = View.VISIBLE
        }
        
        // To account
        if (state.toAccount != null) {
            binding.imgToAccountIcon.setImageResource(state.toAccount.iconRes)
            binding.imgToAccountIcon.setBackgroundColor(state.toAccount.color)
            binding.txtToAccountName.text = state.toAccount.name
            binding.txtToAccountName.setTextColor(getColor(R.color.black))
            binding.imgToAdd.visibility = View.GONE
            binding.txtToLabel.visibility = View.GONE
        } else {
            binding.imgToAccountIcon.setImageResource(android.R.drawable.ic_input_add)
            binding.imgToAccountIcon.setBackgroundColor(getColor(android.R.color.transparent))
            binding.txtToAccountName.text = "Chọn tài khoản"
            binding.txtToAccountName.setTextColor(getColor(R.color.report_yellow))
            binding.imgToAdd.visibility = View.VISIBLE
            binding.txtToLabel.visibility = View.VISIBLE
        }
    }

    /**
     * Cập nhật selection của tabs
     */
    private fun updateTabSelection(type: TransactionTypeState) {
        binding.tabExpense.setBackgroundResource(if (type == TransactionTypeState.EXPENSE) R.drawable.bg_segment_selected else android.R.color.transparent)
        binding.tabIncome.setBackgroundResource(if (type == TransactionTypeState.INCOME) R.drawable.bg_segment_selected else android.R.color.transparent)
        binding.tabTransfer.setBackgroundResource(if (type == TransactionTypeState.TRANSFER) R.drawable.bg_segment_selected else android.R.color.transparent)
    }

    /**
     * Cập nhật visibility của categories
     */
    private fun updateCategoriesVisibility(type: TransactionTypeState) {
        val showCategories = type != TransactionTypeState.TRANSFER
        binding.rvCategories.visibility = if (showCategories) View.VISIBLE else View.GONE
    }

    /**
     * Cấu hình UI ban đầu
     */
    private fun setupUI() {
        binding.btnClose.setOnClickListener { finish() }

        // Click vào nút chọn tài khoản (Chi tiêu/Thu nhập)
        binding.btnSelectAccount.setOnClickListener {
            showAccountSelectionSheetForExpense()
        }

        // Click vào ô nhập số tiền (cho Chi tiêu/Thu nhập)
        binding.layoutAmount.setOnClickListener {
            showCalculatorSheet(viewModel.uiState.value.amount) { amount, note ->
                viewModel.setAmount(amount)
                viewModel.setNote(note)

                val state = viewModel.uiState.value
                if (state.isEditMode && state.selectedCategory != null) {
                    viewModel.updateTransaction(
                        onSuccess = { finish() },
                        onError = { msg -> Toast.makeText(this, msg, Toast.LENGTH_SHORT).show() }
                    )
                } else if (state.selectedCategory != null) {
                    viewModel.saveTransaction(
                        onSuccess = {
                            val resultIntent = intent.apply {
                                putExtra(RESULT_DATE, state.selectedDate)
                            }
                            setResult(RESULT_OK, resultIntent)
                            finish()
                        },
                        onError = { msg -> Toast.makeText(this, msg, Toast.LENGTH_SHORT).show() }
                    )
                }
            }
        }
        
        // Xử lý bàn phím số cho chuyển khoản
        setupTransferNumpad()

        setupTabs()
    }
    
    /**
     * Setup bàn phím số cho chuyển khoản
     */
    private fun setupTransferNumpad() {
        val digits = listOf(
            R.id.btnCalc0 to "0", R.id.btnCalc1 to "1", R.id.btnCalc2 to "2",
            R.id.btnCalc3 to "3", R.id.btnCalc4 to "4", R.id.btnCalc5 to "5",
            R.id.btnCalc6 to "6", R.id.btnCalc7 to "7", R.id.btnCalc8 to "8",
            R.id.btnCalc9 to "9"
        )
        digits.forEach { (id, digit) ->
            binding.numpadLayout.root.findViewById<android.widget.Button>(id).setOnClickListener {
                val state = viewModel.uiState.value
                if (state.currentType == TransactionTypeState.TRANSFER) {
                    var s = binding.txtTransferAmount.text.toString()
                    if (s == "0") s = ""
                    binding.txtTransferAmount.text = s + digit
                    viewModel.setAmount(binding.txtTransferAmount.text.toString())
                }
            }
        }

        // Nút xóa từng số
        binding.numpadLayout.btnCalcBack.setOnClickListener {
            val state = viewModel.uiState.value
            if (state.currentType == TransactionTypeState.TRANSFER) {
                val s = binding.txtTransferAmount.text.toString()
                if (s.length > 1) {
                    binding.txtTransferAmount.text = s.dropLast(1)
                } else {
                    binding.txtTransferAmount.text = "0"
                }
                viewModel.setAmount(binding.txtTransferAmount.text.toString())
            }
        }

        // Nút xác nhận
        binding.numpadLayout.btnCalcDone.setOnClickListener {
            val state = viewModel.uiState.value
            if (state.currentType == TransactionTypeState.TRANSFER) {
                val amount = binding.txtTransferAmount.text.toString()
                val note = binding.edtTransferNote.text.toString()
                
                if (amount.isEmpty() || amount == "0") {
                    Toast.makeText(this, "Vui lòng nhập số tiền", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                
                viewModel.setAmount(amount)
                viewModel.setNote(note)
                
                viewModel.saveTransferTransaction(
                    onSuccess = {
                        val resultIntent = intent.apply {
                            putExtra(RESULT_DATE, state.selectedDate)
                        }
                        setResult(RESULT_OK, resultIntent)
                        finish()
                    },
                    onError = { msg -> Toast.makeText(this, msg, Toast.LENGTH_SHORT).show() }
                )
            }
        }
    }

    /**
     * Setup các tabs
     */
    private fun setupTabs() {
        binding.tabExpense.setOnClickListener {
            viewModel.setTransactionType(TransactionTypeState.EXPENSE)
            loadExpenseCategories()
        }

        binding.tabIncome.setOnClickListener {
            viewModel.setTransactionType(TransactionTypeState.INCOME)
            loadIncomeCategories()
        }

        binding.tabTransfer.setOnClickListener {
            viewModel.setTransactionType(TransactionTypeState.TRANSFER)
        }

        // Click vào ô "Từ tài khoản"
        binding.cardFromAccount.setOnClickListener {
            showAccountSelectionSheet(true)
        }

        // Click vào ô "Đến tài khoản"
        binding.cardToAccount.setOnClickListener {
            showAccountSelectionSheet(false)
        }
    }
    
    /**
     * Hiển thị bottom sheet thêm tài khoản
     */
    private fun showAddAccountSheet() {
        val dialog = BottomSheetDialog(this, R.style.BottomSheetDialogTheme)
        val sheetBinding = BottomSheetAddAccountBinding.inflate(layoutInflater)

        var selectedIconRes = R.drawable.ic_wallet
        var selectedColor = 0xFFE53935.toInt()

        // Cập nhật icon preview
        val updatePreview = {
            sheetBinding.imgSelectedIcon.setImageResource(selectedIconRes)
            val drawable = sheetBinding.imgSelectedIcon.background as? GradientDrawable
            drawable?.setColor(selectedColor)
        }
        updatePreview()

        // Setup icon grid
        val adapter = AccountIconAdapter(
            icons = listOf(
                R.drawable.ic_money_bag to 0xFFE53935.toInt(),
                R.drawable.ic_salary to 0xFF43A047.toInt(),
                R.drawable.ic_bank to 0xFF1E88E5.toInt(),
                R.drawable.ic_credit_card to 0xFFFB8C00.toInt(),
                R.drawable.ic_savings to 0xFF8E24AA.toInt(),
                R.drawable.ic_business to 0xFF00ACC1.toInt(),
                R.drawable.ic_investment to 0xFFFDD835.toInt(),
                R.drawable.ic_wallet to 0xFF6D4C41.toInt()
            ),
            selectedPosition = 0
        ) { iconRes, color ->
            selectedIconRes = iconRes
            selectedColor = color
            updatePreview()
        }

        sheetBinding.recyclerIcons.layoutManager = GridLayoutManager(this, 4)
        sheetBinding.recyclerIcons.adapter = adapter

        // Nút hủy
        sheetBinding.btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        // Nút lưu
        sheetBinding.btnSave.setOnClickListener {
            val name = sheetBinding.editAccountName.text.toString().trim()
            if (name.isEmpty()) {
                sheetBinding.editAccountName.error = "Vui lòng nhập tên tài khoản"
                return@setOnClickListener
            }

            lifecycleScope.launch {
                val account = AccountEntity(
                    userId = 1,
                    name = name,
                    balance = 0.0,
                    iconRes = selectedIconRes,
                    color = selectedColor
                )
                viewModel.addAccount(account)
                Toast.makeText(this@AddTransactionActivity, "Đã thêm tài khoản", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            }
        }

        dialog.setContentView(sheetBinding.root)
        dialog.behavior.state = BottomSheetBehavior.STATE_EXPANDED
        dialog.show()
    }

    /**
     * Hiển thị bottom sheet chọn tài khoản cho Chi tiêu/Thu nhập
     */
    private fun showAccountSelectionSheetForExpense() {
        val dialog = BottomSheetDialog(this, R.style.BottomSheetDialogTheme)
        val sheetBinding = BottomSheetSelectAccountBinding.inflate(layoutInflater)
        
        val state = viewModel.uiState.value
        val accounts = state.availableAccounts
        
        // Nếu không có tài khoản, vẫn hiển thị tùy chọn "Không liên kết"
        val currentSelection = state.selectedAccount?.id
        
        val adapter = AccountSelectWithNoneAdapter(accounts, currentSelection) { selectedAccount ->
            viewModel.selectAccount(selectedAccount)
            dialog.dismiss()
        }
        
        sheetBinding.rvAccounts.layoutManager = LinearLayoutManager(this)
        sheetBinding.rvAccounts.adapter = adapter
        
        sheetBinding.txtSheetTitle.text = "Chọn tài khoản"
        
        sheetBinding.btnCreateAccount.setOnClickListener {
            dialog.dismiss()
            showAddAccountSheet()
        }
        
        dialog.setContentView(sheetBinding.root)
        dialog.behavior.state = BottomSheetBehavior.STATE_EXPANDED
        dialog.show()
    }

    /**
     * Hiển thị bottom sheet chọn tài khoản
     */
    private fun showAccountSelectionSheet(isFromAccount: Boolean) {
        val dialog = BottomSheetDialog(this, R.style.BottomSheetDialogTheme)
        val sheetBinding = BottomSheetSelectAccountBinding.inflate(layoutInflater)
        
        val state = viewModel.uiState.value
        val accounts = state.availableAccounts
        
        if (accounts.isEmpty()) {
            // Mở bottom sheet thêm tài khoản
            dialog.dismiss()
            showAddAccountSheet()
            return
        }
        
        val currentSelection = if (isFromAccount) state.fromAccount?.id else state.toAccount?.id
        
        val adapter = AccountSelectAdapter(accounts, currentSelection) { selectedAccount ->
            if (isFromAccount) {
                viewModel.selectFromAccount(selectedAccount)
            } else {
                viewModel.selectToAccount(selectedAccount)
            }
            dialog.dismiss()
        }
        
        sheetBinding.rvAccounts.layoutManager = LinearLayoutManager(this)
        sheetBinding.rvAccounts.adapter = adapter
        
        // Tiêu đề bottom sheet
        sheetBinding.txtSheetTitle.text = if (isFromAccount) "Chọn tài khoản nguồn" else "Chọn tài khoản đích"
        
        // Nút Tạo tài khoản mới
        sheetBinding.btnCreateAccount.setOnClickListener {
            dialog.dismiss()
            showAddAccountSheet()
        }
        
        dialog.setContentView(sheetBinding.root)
        dialog.behavior.state = BottomSheetBehavior.STATE_EXPANDED
        dialog.show()
    }

    /**
     * Tải danh sách categories Chi tiêu
     */
    private fun loadExpenseCategories() {
        val state = viewModel.uiState.value
        categoryAdapter = CategoryAdapter(
            selectedCategoryId = state.selectedCategory?.id,
            categories = state.expenseCategories
        ) { category ->
            onCategorySelected(category)
        }
        binding.rvCategories.layoutManager = CategoryAdapter.createLayoutManager(this)
        binding.rvCategories.adapter = categoryAdapter
    }

    /**
     * Tải danh sách categories Thu nhập
     */
    private fun loadIncomeCategories() {
        val state = viewModel.uiState.value
        categoryAdapter = CategoryAdapter(
            categories = state.incomeCategories,
            selectedCategoryId = state.selectedCategory?.id
        ) { category ->
            onCategorySelected(category)
        }
        binding.rvCategories.layoutManager = CategoryAdapter.createLayoutManager(this)
        binding.rvCategories.adapter = categoryAdapter
    }

    /**
     * Xử lý khi chọn category
     */
    private fun onCategorySelected(category: TransactionCategory) {
        viewModel.selectCategory(category)
        showCalculatorSheet(viewModel.uiState.value.amount) { amount, note ->
            viewModel.setAmount(amount)
            viewModel.setNote(note)

            val state = viewModel.uiState.value
            if (state.isEditMode) {
                viewModel.updateTransaction(
                    onSuccess = { finish() },
                    onError = { msg -> Toast.makeText(this, msg, Toast.LENGTH_SHORT).show() }
                )
            } else {
                viewModel.saveTransaction(
                    onSuccess = {
                        val resultIntent = intent.apply {
                            putExtra(RESULT_DATE, state.selectedDate)
                        }
                        setResult(RESULT_OK, resultIntent)
                        finish()
                    },
                    onError = { msg -> Toast.makeText(this, msg, Toast.LENGTH_SHORT).show() }
                )
            }
        }
    }

    /**
     * Hiển thị BottomSheet Calculator với định dạng số có dấu chấm
     */
    private fun showCalculatorSheet(initialValue: String, onDone: (amount: String, note: String) -> Unit) {
        val dialog = BottomSheetDialog(this, R.style.BottomSheetDialogTheme)
        val sheetBinding = com.example.moneylover.databinding.BottomSheetCalculatorBinding.inflate(layoutInflater)
        
        // Hàm format số với dấu chấm phân cách
        fun formatWithDots(numStr: String): String {
            if (numStr.isEmpty()) return "0"
            val clean = numStr.replace(".", "").replace(",", "")
            if (clean.isEmpty() || clean == "0") return "0"
            val sb = StringBuilder()
            val length = clean.length
            for (i in clean.indices) {
                sb.append(clean[i])
                val remaining = length - i - 1
                if (remaining > 0 && remaining % 3 == 0) {
                    sb.append(".")
                }
            }
            return sb.toString()
        }
        
        // Hàm lấy số thuần (không có dấu chấm)
        fun getCleanNumber(): String {
            return sheetBinding.txtCalculatorValue.text.toString().replace(".", "").replace(",", "")
        }
        
        // Parse giá trị ban đầu (bỏ dấu chấm nếu có)
        val cleanInitial = initialValue.replace(".", "").replace(",", "")
        sheetBinding.txtCalculatorValue.text = if (cleanInitial.isEmpty() || cleanInitial == "0") "0" else formatWithDots(cleanInitial)

        val state = viewModel.uiState.value
        if (state.isEditMode && state.note.isNotBlank()) {
            sheetBinding.edtNote.setText(state.note)
        }

        val digits = listOf(
            R.id.btnCalc0 to "0", R.id.btnCalc1 to "1", R.id.btnCalc2 to "2",
            R.id.btnCalc3 to "3", R.id.btnCalc4 to "4", R.id.btnCalc5 to "5",
            R.id.btnCalc6 to "6", R.id.btnCalc7 to "7", R.id.btnCalc8 to "8",
            R.id.btnCalc9 to "9"
        )
        digits.forEach { (id, digit) ->
            sheetBinding.root.findViewById<android.widget.Button>(id).setOnClickListener {
                val clean = getCleanNumber()
                if (clean == "0") {
                    sheetBinding.txtCalculatorValue.text = digit
                } else {
                    sheetBinding.txtCalculatorValue.text = formatWithDots(clean + digit)
                }
            }
        }

        // Nút xóa hết - reset về 0
        sheetBinding.btnCalcClear.setOnClickListener {
            sheetBinding.txtCalculatorValue.text = "0"
        }

        // Nút xóa từng số
        sheetBinding.btnCalcBack.setOnClickListener {
            val clean = getCleanNumber()
            if (clean.length > 1) {
                sheetBinding.txtCalculatorValue.text = formatWithDots(clean.dropLast(1))
            } else {
                sheetBinding.txtCalculatorValue.text = "0"
            }
        }

        sheetBinding.btnCalcDone.setOnClickListener {
            val note = sheetBinding.edtNote.text.toString()
            // Gửi số thuần (không có dấu chấm)
            onDone(getCleanNumber(), note)
            dialog.dismiss()
        }

        dialog.setContentView(sheetBinding.root)
        dialog.behavior.state = BottomSheetBehavior.STATE_EXPANDED
        dialog.show()
    }
}
