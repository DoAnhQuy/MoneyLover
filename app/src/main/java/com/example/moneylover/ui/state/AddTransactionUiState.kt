package com.example.moneylover.ui.state

import com.example.moneylover.TransactionCategory
import com.example.moneylover.data.model.AccountEntity

/**
 * UiState cho màn hình AddTransactionActivity (Thêm/Sửa giao dịch)
 * Chứa tất cả trạng thái UI cần thiết
 */
data class AddTransactionUiState(
    val isLoading: Boolean = false,                            // Đang tải dữ liệu
    val currentType: TransactionTypeState = TransactionTypeState.EXPENSE, // Loại giao dịch hiện tại
    val selectedCategory: TransactionCategory? = null,        // Danh mục đã chọn
    val amount: String = "0",                                  // Số tiền (string để format)
    val selectedDate: Long = System.currentTimeMillis(),       // Ngày đã chọn
    val note: String = "",                                     // Ghi chú
    val isEditMode: Boolean = false,                          // Đang ở chế độ sửa
    val editingTransactionId: Long? = null,                   // ID giao dịch đang sửa
    val expenseCategories: List<TransactionCategory> = emptyList(), // Danh mục chi tiêu
    val incomeCategories: List<TransactionCategory> = emptyList(),  // Danh mục thu nhập
    val isSaving: Boolean = false,                             // Đang lưu giao dịch
    val saveSuccess: Boolean = false,                         // Lưu thành công
    val errorMessage: String? = null,                         // Thông báo lỗi
    val selectedYear: Int = 0,                                // Năm đã chọn
    val selectedMonth: Int = 0,                              // Tháng đã chọn
    val selectedDay: Int = 0,                                 // Ngày đã chọn
    // Tài khoản đã chọn (cho chi tiêu/thu nhập)
    val selectedAccount: AccountEntity? = null,
    // Tài khoản nguồn/đích (cho chuyển khoản)
    val fromAccount: AccountEntity? = null,
    val toAccount: AccountEntity? = null,
    val availableAccounts: List<AccountEntity> = emptyList()   // Danh sách tài khoản khả dụng
)

/**
 * Loại giao dịch (tương ứng với TransactionType enum)
 */
enum class TransactionTypeState {
    EXPENSE,    // Chi tiêu
    INCOME,     // Thu nhập
    TRANSFER    // Chuyển khoản
}
