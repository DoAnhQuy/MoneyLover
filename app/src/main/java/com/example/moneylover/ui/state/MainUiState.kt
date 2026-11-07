package com.example.moneylover.ui.state

import com.example.moneylover.data.model.AccountEntity
import com.example.moneylover.data.model.TransactionEntity

/**
 * UiState cho màn hình MainActivity
 * Chứa tất cả trạng thái UI cần hiển thị
 */
data class MainUiState(
    val isLoading: Boolean = false,                    // Đang tải dữ liệu
    val transactions: List<TransactionEntity> = emptyList(), // Danh sách giao dịch
    val totalExpense: Double = 0.0,                   // Tổng chi tiêu tháng hiện tại
    val totalIncome: Double = 0.0,                    // Tổng thu nhập tháng hiện tại
    val balance: Double = 0.0,                        // Số dư tháng hiện tại (thu - chi)
    val selectedYear: Int = 0,                       // Năm đang chọn
    val selectedMonth: Int = 0,                      // Tháng đang chọn
    val isEmpty: Boolean = true,                      // Có dữ liệu không
    val currentView: MainView = MainView.HOME,      // View hiện tại đang hiển thị
    val monthStats: List<MonthStatUi> = emptyList(), // Thống kê các tháng
    val totalAllTimeExpense: Double = 0.0,           // Tổng chi tiêu mọi thời đại
    val totalAllTimeIncome: Double = 0.0,            // Tổng thu nhập mọi thời đại
    val totalAllTimeBalance: Double = 0.0,           // Tổng số dư mọi thời đại
    val statsSelectedYear: Int = 0,                  // Năm được chọn trong thống kê tháng
    
    // Dữ liệu cho tab Tài khoản
    val accounts: List<AccountEntity> = emptyList(), // Danh sách tài khoản
    val totalAssets: Double = 0.0,                   // Tổng tài sản
    val totalLiabilities: Double = 0.0,              // Tổng nợ phải trả
    val netAssets: Double = 0.0,                    // Tài sản ròng (tài sản - nợ)
    
    // Dữ liệu cho tab Phân tích (tháng hiện tại)
    val currentMonthBudget: Double = 0.0,             // Ngân sách tháng hiện tại
    val currentMonthSpending: Double = 0.0,          // Chi tiêu tháng hiện tại
    val currentMonthRemaining: Double = 0.0          // Còn lại trong tháng
)

/**
 * Trạng thái hiển thị của màn hình chính
 */
enum class MainView {
    HOME,       // Trang chủ - hiển thị danh sách giao dịch
    MONTHLY_STATS,  // Thống kê hàng tháng
    REPORT,      // Báo cáo - hiển thị biểu đồ/thống kê
    OVERVIEW
}

/**
 * UI model cho thống kê tháng (dùng chung)
 */
data class MonthStatUi(
    val month: Int,
    val year: Int,
    val totalExpense: Double,
    val totalIncome: Double,
    val budget: Double = 0.0
) {
    val balance: Double
        get() = totalIncome - totalExpense

    val monthName: String
        get() {
            val monthStr = if (month < 10) "0$month" else "$month"
            return "Tháng $monthStr/$year"
        }
}
