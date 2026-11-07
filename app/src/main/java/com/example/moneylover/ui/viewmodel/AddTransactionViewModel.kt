package com.example.moneylover.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.moneylover.R
import com.example.moneylover.data.database.AppDatabase
import com.example.moneylover.data.model.AccountEntity
import com.example.moneylover.data.model.TransactionEntity
import com.example.moneylover.TransactionCategory
import com.example.moneylover.TransactionType
import com.example.moneylover.utils.NotificationHelper
import com.example.moneylover.data.repository.AccountRepository
import com.example.moneylover.data.repository.TransactionRepository
import com.example.moneylover.ui.state.AddTransactionUiState
import com.example.moneylover.ui.state.TransactionTypeState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Calendar


class AddTransactionViewModel(application: Application) : AndroidViewModel(application) {

    private val transactionRepository: TransactionRepository
    private val accountRepository: AccountRepository

    private val _uiState = MutableStateFlow(AddTransactionUiState())
    val uiState: StateFlow<AddTransactionUiState> = _uiState.asStateFlow()

    init {
        val database = AppDatabase.getDatabase(application)
        transactionRepository = TransactionRepository(database.transactionDao())
        accountRepository = AccountRepository(database.accountDao())
        // Khởi tạo danh sách categories
        _uiState.value = _uiState.value.copy(
            expenseCategories = getExpenseCategories(),
            incomeCategories = getIncomeCategories()
        )
        // Load accounts
        loadAccounts()
    }


    private fun loadAccounts() {
        viewModelScope.launch {
            accountRepository.allAccounts.collect { accounts ->
                _uiState.value = _uiState.value.copy(availableAccounts = accounts)
            }
        }
    }

    /**
     * Thiết lập thông tin ngày được chọn từ Activity cha
     */
    fun setSelectedDate(year: Int, month: Int, day: Int) {
        val now = Calendar.getInstance()
        val cal = Calendar.getInstance()
        cal.set(year, month - 1, day, now.get(Calendar.HOUR_OF_DAY), now.get(Calendar.MINUTE), now.get(Calendar.SECOND))
        
        _uiState.value = _uiState.value.copy(
            selectedYear = year,
            selectedMonth = month,
            selectedDay = day,
            selectedDate = cal.timeInMillis
        )
    }

    /**
     * Bắt đầu chế độ sửa - load dữ liệu giao dịch
     */
    fun startEditMode(transactionId: Long) {
        _uiState.value = _uiState.value.copy(isLoading = true, isEditMode = true, editingTransactionId = transactionId)
        
        viewModelScope.launch {
            val transaction = transactionRepository.getById(transactionId)
            if (transaction != null) {
                val type = when (transaction.type) {
                    "EXPENSE" -> TransactionTypeState.EXPENSE
                    "INCOME" -> TransactionTypeState.INCOME
                    else -> TransactionTypeState.TRANSFER
                }

                val cal = Calendar.getInstance().apply { timeInMillis = transaction.date }
                val year = cal.get(Calendar.YEAR)
                val month = cal.get(Calendar.MONTH) + 1
                val day = cal.get(Calendar.DAY_OF_MONTH)
                val amountStr = transaction.amount.toLong().toString()

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    currentType = type,
                    selectedCategory = TransactionCategory(
                        id = transaction.categoryId,
                        name = transaction.categoryName,
                        iconRes = transaction.iconRes,
                        type = when (type) {
                            TransactionTypeState.EXPENSE -> TransactionType.EXPENSE
                            TransactionTypeState.INCOME -> TransactionType.INCOME
                            TransactionTypeState.TRANSFER -> TransactionType.TRANSFER
                        }
                    ),
                    amount = amountStr,
                    selectedDate = transaction.date,
                    note = transaction.note ?: "",
                    selectedYear = year,
                    selectedMonth = month,
                    selectedDay = day
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Không tìm thấy giao dịch"
                )
            }
        }
    }

    /**
     * Chuyển loại giao dịch (EXPENSE/INCOME/TRANSFER)
     */
    fun setTransactionType(type: TransactionTypeState) {
        if (type == _uiState.value.currentType) return

        _uiState.value = _uiState.value.copy(
            currentType = type,
            selectedCategory = null
        )
    }

    /**
     * Chọn category
     */
    fun selectCategory(category: TransactionCategory) {
        _uiState.value = _uiState.value.copy(selectedCategory = category)
    }

    /**
     * Cập nhật số tiền
     */
    fun setAmount(amount: String) {
        _uiState.value = _uiState.value.copy(amount = amount)
    }

    /**
     * Cập nhật ghi chú
     */
    fun setNote(note: String) {
        _uiState.value = _uiState.value.copy(note = note)
    }

    /**
     * Cập nhật ngày được chọn
     */
    fun setDate(date: Long) {
        _uiState.value = _uiState.value.copy(selectedDate = date)
    }

    /**
     * Định dạng số tiền hiển thị (VD: 1.500.000)
     */
    fun formatAmount(amount: String): String {
        if (amount.isEmpty()) return "0"
        return try {
            val value = amount.replace(".", "").replace(",", "").toDouble()
            String.format("%,.0f", value).replace(",", ".")
        } catch (e: NumberFormatException) {
            amount
        }
    }

    /**
     * Lưu giao dịch mới
     */
    fun saveTransaction(onSuccess: () -> Unit, onError: (String) -> Unit) {
        val state = _uiState.value

        // Validate
        if (state.selectedCategory == null) {
            onError("Vui lòng chọn danh mục")
            return
        }

        val amount = state.amount.replace(".", "").replace(",", "").toDoubleOrNull() ?: 0.0
        if (amount <= 0) {
            onError("Vui lòng nhập số tiền lớn hơn 0")
            return
        }

        _uiState.value = state.copy(isSaving = true)

        val transaction = TransactionEntity(
            categoryId = state.selectedCategory.id,
            categoryName = state.selectedCategory.name,
            iconRes = state.selectedCategory.iconRes,
            amount = amount,
            type = state.selectedCategory.type.name,
            fromAccount = state.selectedAccount?.name, // Lưu tên tài khoản
            date = state.selectedDate,
            note = state.note.ifBlank { null }
        )

        viewModelScope.launch {
            try {
                transactionRepository.insert(transaction)
                
                // Cập nhật số dư tài khoản nếu đã chọn
                state.selectedAccount?.let { account ->
                    val updatedBalance = if (state.currentType == TransactionTypeState.EXPENSE) {
                        account.balance - amount
                    } else {
                        account.balance + amount
                    }
                    val updatedAccount = account.copy(balance = updatedBalance)
                    accountRepository.update(updatedAccount)
                }
                
                _uiState.value = _uiState.value.copy(isSaving = false, saveSuccess = true)
                
                // Hiển thị thông báo như app ngân hàng
                NotificationHelper.showTransactionNotification(
                    getApplication(),
                    transaction.type,
                    transaction.categoryName,
                    transaction.amount
                )
                
                onSuccess()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    errorMessage = "Lỗi khi lưu: ${e.message}"
                )
                onError("Lỗi khi lưu giao dịch")
            }
        }
    }

    /**
     * Cập nhật giao dịch đã tồn tại
     */
    fun updateTransaction(onSuccess: () -> Unit, onError: (String) -> Unit) {
        val state = _uiState.value

        if (state.editingTransactionId == null) {
            onError("Không có giao dịch để cập nhật")
            return
        }

        if (state.selectedCategory == null) {
            onError("Vui lòng chọn danh mục")
            return
        }

        val amount = state.amount.replace(".", "").replace(",", "").toDoubleOrNull() ?: 0.0
        if (amount <= 0) {
            onError("Vui lòng nhập số tiền lớn hơn 0")
            return
        }

        _uiState.value = state.copy(isSaving = true)

        val transaction = TransactionEntity(
            id = state.editingTransactionId,
            categoryId = state.selectedCategory.id,
            categoryName = state.selectedCategory.name,
            iconRes = state.selectedCategory.iconRes,
            amount = amount,
            type = state.selectedCategory.type.name,
            date = state.selectedDate,
            note = state.note.ifBlank { null }
        )

        viewModelScope.launch {
            try {
                transactionRepository.update(transaction)
                _uiState.value = _uiState.value.copy(isSaving = false, saveSuccess = true)
                onSuccess()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    errorMessage = "Lỗi khi cập nhật: ${e.message}"
                )
                onError("Lỗi khi cập nhật giao dịch")
            }
        }
    }

    /**
     * Reset trạng thái save
     */
    fun resetSaveState() {
        _uiState.value = _uiState.value.copy(saveSuccess = false, errorMessage = null)
    }

    /**
     * Xóa thông báo lỗi
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    /**
     * Chọn tài khoản cho Chi tiêu/Thu nhập (có thể null để không liên kết)
     */
    fun selectAccount(account: AccountEntity?) {
        _uiState.value = _uiState.value.copy(selectedAccount = account)
    }

    /**
     * Chọn tài khoản nguồn (cho chuyển khoản)
     */
    fun selectFromAccount(account: AccountEntity) {
        _uiState.value = _uiState.value.copy(fromAccount = account)
    }

    /**
     * Chọn tài khoản đích (cho chuyển khoản)
     */
    fun selectToAccount(account: AccountEntity) {
        _uiState.value = _uiState.value.copy(toAccount = account)
    }

    /**
     * Thêm tài khoản mới
     */
    fun addAccount(account: AccountEntity) {
        viewModelScope.launch {
            accountRepository.insert(account)
        }
    }

    /**
     * Lưu giao dịch chuyển khoản
     */
    fun saveTransferTransaction(onSuccess: () -> Unit, onError: (String) -> Unit) {
        val state = _uiState.value

        // Validate
        if (state.fromAccount == null) {
            onError("Vui lòng chọn tài khoản nguồn")
            return
        }
        if (state.toAccount == null) {
            onError("Vui lòng chọn tài khoản đích")
            return
        }
        if (state.fromAccount.id == state.toAccount.id) {
            onError("Tài khoản nguồn và đích không được trùng nhau")
            return
        }

        val amount = state.amount.replace(".", "").replace(",", "").toDoubleOrNull() ?: 0.0
        if (amount <= 0) {
            onError("Vui lòng nhập số tiền lớn hơn 0")
            return
        }

        // Kiểm tra số dư tài khoản nguồn
        if (state.fromAccount.balance < amount) {
            onError("Số dư tài khoản nguồn không đủ")
            return
        }

        _uiState.value = state.copy(isSaving = true)

        viewModelScope.launch {
            try {
                // Trừ tiền từ tài khoản nguồn
                val updatedFromAccount = state.fromAccount.copy(
                    balance = state.fromAccount.balance - amount
                )
                accountRepository.update(updatedFromAccount)

                // Cộng tiền vào tài khoản đích
                val updatedToAccount = state.toAccount.copy(
                    balance = state.toAccount.balance + amount
                )
                accountRepository.update(updatedToAccount)

                // Lưu giao dịch chuyển khoản
                val transaction = TransactionEntity(
                    categoryId = 0, // Không có category cho transfer
                    categoryName = "Chuyển khoản",
                    iconRes = R.drawable.ic_transfer,
                    amount = amount,
                    type = TransactionType.TRANSFER.name,
                    date = state.selectedDate,
                    note = state.note.ifBlank { "Chuyển từ ${state.fromAccount.name} sang ${state.toAccount.name}" },
                    fromAccount = state.fromAccount.name,
                    toAccount = state.toAccount.name
                )
                transactionRepository.insert(transaction)

                // Hiển thị thông báo như app ngân hàng
                NotificationHelper.showTransactionNotification(
                    getApplication(),
                    TransactionType.TRANSFER.name,
                    "Chuyển khoản",
                    amount
                )

                _uiState.value = _uiState.value.copy(isSaving = false, saveSuccess = true)
                onSuccess()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    errorMessage = "Lỗi khi chuyển khoản: ${e.message}"
                )
                onError("Lỗi khi chuyển khoản")
            }
        }
    }

    /**
     * Reset form về trạng thái ban đầu
     */
    fun resetForm() {
        _uiState.value = AddTransactionUiState(
            expenseCategories = _uiState.value.expenseCategories,
            incomeCategories = _uiState.value.incomeCategories,
            availableAccounts = _uiState.value.availableAccounts
        )
    }

    /**
     * Lấy danh sách categories cho Chi tiêu
     */
    private fun getExpenseCategories(): List<TransactionCategory> = listOf(
        TransactionCategory(1, "Ăn uống", R.drawable.ic_food, TransactionType.EXPENSE),
        TransactionCategory(2, "Di chuyển", R.drawable.ic_transport, TransactionType.EXPENSE),
        TransactionCategory(3, "Mua sắm", R.drawable.ic_shopping, TransactionType.EXPENSE),
        TransactionCategory(4, "Giải trí", R.drawable.ic_entertainment, TransactionType.EXPENSE),
        TransactionCategory(5, "Điện thoại", R.drawable.ic_phone, TransactionType.EXPENSE),
        TransactionCategory(6, "Giáo dục", R.drawable.ic_education, TransactionType.EXPENSE),
        TransactionCategory(7, "Y tế", R.drawable.ic_medical, TransactionType.EXPENSE),
        TransactionCategory(8, "Nhà hàng", R.drawable.ic_restaurant, TransactionType.EXPENSE),
        TransactionCategory(9, "Du lịch", R.drawable.ic_travel, TransactionType.EXPENSE),
        TransactionCategory(10, "Sắc đẹp", R.drawable.ic_beauty, TransactionType.EXPENSE),
        TransactionCategory(11, "Thể thao", R.drawable.ic_sports, TransactionType.EXPENSE),
        TransactionCategory(12, "Khác", R.drawable.ic_other, TransactionType.EXPENSE)
    )

    /**
     * Lấy danh sách categories cho Thu nhập
     */
    private fun getIncomeCategories(): List<TransactionCategory> = listOf(
        TransactionCategory(1, "Lương", R.drawable.ic_salary, TransactionType.INCOME),
        TransactionCategory(2, "Thưởng", R.drawable.ic_bonus, TransactionType.INCOME),
        TransactionCategory(3, "Đầu tư", R.drawable.ic_investment, TransactionType.INCOME),
        TransactionCategory(4, "Kinh doanh", R.drawable.ic_business, TransactionType.INCOME),
        TransactionCategory(5, "Trợ cấp", R.drawable.ic_grant, TransactionType.INCOME),
        TransactionCategory(6, "Thu hồi", R.drawable.ic_refund, TransactionType.INCOME),
        TransactionCategory(7, "Lãi vay", R.drawable.ic_interest, TransactionType.INCOME),
        TransactionCategory(8, "Khác", R.drawable.ic_income_other, TransactionType.INCOME)
    )
}
