package com.example.moneylover.ui.state

import com.example.moneylover.data.model.TransactionEntity

/**
 * UiState cho màn hình CalendarActivity (Lịch)
 * Chứa tất cả trạng thái UI cần thiết
 */
data class CalendarUiState(
    val isLoading: Boolean = false,                            // Đang tải dữ liệu
    val selectedYear: Int = 0,                               // Năm đang chọn
    val selectedMonth: Int = 0,                              // Tháng đang chọn
    val selectedDay: Int = 1,                                // Ngày đang chọn
    val calendarDays: List<CalendarDayUi> = emptyList(),      // Danh sách ngày trong tháng
    val dayTransactions: List<TransactionEntity> = emptyList(), // Giao dịch trong ngày đã chọn
    val dayTotalExpense: Double = 0.0,                       // Tổng chi tiêu ngày đã chọn
    val dayTotalIncome: Double = 0.0,                        // Tổng thu nhập ngày đã chọn
    val dayBalance: Double = 0.0,                            // Số dư ngày đã chọn
    val monthStats: List<MonthStatUi> = emptyList(),         // Thống kê các tháng
    val totalAllTimeExpense: Double = 0.0,                   // Tổng chi tiêu mọi thời đại
    val totalAllTimeIncome: Double = 0.0,                    // Tổng thu nhập mọi thời đại
    val totalAllTimeBalance: Double = 0.0                    // Tổng số dư mọi thời đại
)

/**
 * UI model cho ngày trong lịch
 */
data class CalendarDayUi(
    val day: Int,                    // Số ngày (1-31)
    val inMonth: Boolean,            // Có thuộc tháng đang hiển thị không
    val isSelected: Boolean = false, // Có được chọn không
    val hasExpense: Boolean = false, // Có giao dịch chi tiêu không
    val hasIncome: Boolean = false   // Có giao dịch thu nhập không
)
