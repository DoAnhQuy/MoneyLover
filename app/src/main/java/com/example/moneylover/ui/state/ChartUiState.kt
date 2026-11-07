package com.example.moneylover.ui.state

import com.example.moneylover.ui.view.LineChartView

// Khoảng thời gian hiển thị biểu đồ
enum class ChartPeriod {
    WEEK,   // Tuần
    MONTH,  // Tháng
    YEAR    // Năm
}

// Loại giao dịch hiển thị trên biểu đồ
enum class TransactionType(val displayName: String) {
    EXPENSE("Chi tiêu"),
    INCOME("Thu nhập")
}

/**
 * UiState cho màn hình biểu đồ (ChartActivity)
 * Chứa dữ liệu và trạng thái hiển thị biểu đồ
 */
data class ChartUiState(
    val currentPeriod: ChartPeriod = ChartPeriod.WEEK,                    // Khoảng thời gian hiện tại
    val transactionType: TransactionType = TransactionType.EXPENSE,       // Loại giao dịch hiển thị
    val startDate: Long = System.currentTimeMillis(),                     // Ngày bắt đầu
    val endDate: Long = System.currentTimeMillis(),                       // Ngày kết thúc
    val totalAmount: Double = 0.0,                                       // Tổng số tiền
    val totalIncome: Double = 0.0,                                        // Tổng thu nhập
    val balance: Double = 0.0,                                            // Số dư
    val budget: Double = 0.0,                                            // Ngân sách
    val categories: List<CategoryChartData> = emptyList(),                 // Dữ liệu theo danh mục
    val isEmpty: Boolean = true,                                         // Có dữ liệu không
    val dateRangeText: String = "",                                      // Text khoảng thời gian
    val periodDisplayText: String = "Tuần này",                          // Text hiển thị khoảng thời gian
    val hasData: Boolean = false,                                       // Có data để hiển thị
    val lineChartData: List<LineChartView.DataPoint> = emptyList()       // Dữ liệu biểu đồ đường
)

/**
 * Dữ liệu danh mục cho biểu đồ tròn
 */
data class CategoryChartData(
    val categoryId: Int,       // ID danh mục
    val categoryName: String,   // Tên danh mục
    val iconRes: Int,          // Icon danh mục
    val amount: Double,        // Số tiền
    val percentage: Float,      // Phần trăm
    val color: Int             // Màu sắc
)
