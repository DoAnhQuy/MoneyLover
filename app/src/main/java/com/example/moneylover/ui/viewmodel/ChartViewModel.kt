package com.example.moneylover.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.moneylover.data.database.AppDatabase
import com.example.moneylover.data.repository.BudgetRepository
import com.example.moneylover.ui.state.CategoryChartData
import com.example.moneylover.ui.state.ChartPeriod
import com.example.moneylover.ui.state.ChartUiState
import com.example.moneylover.ui.state.TransactionType
import com.example.moneylover.ui.view.BarChartView
import com.example.moneylover.ui.view.LineChartView
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * ViewModel cho ChartActivity (Biểu đồ thống kê)
 * 
 * Nhiệm vụ:
 * 1. Quản lý dữ liệu biểu đồ (tròn, đường, cột)
 * 2. Xử lý lọc theo khoảng thời gian (tuần/tháng/năm)
 * 3. Tính toán thống kê theo danh mục
 */
class ChartViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: com.example.moneylover.data.repository.TransactionRepository
    private val budgetRepository: BudgetRepository
    
    // Các formatter cho ngày tháng
    private val dateFormatter = SimpleDateFormat("d 'tháng' M", Locale("vi", "VN"))
    private val dayFormatter = SimpleDateFormat("dd/MM", Locale("vi", "VN"))
    private val weekFormatter = SimpleDateFormat("'Tuần' w, M", Locale("vi", "VN"))
    private val monthYearFormatter = SimpleDateFormat("'Tháng' M 'năm' yyyy", Locale("vi", "VN"))
    private val yearFormatter = SimpleDateFormat("yyyy", Locale("vi", "VN"))

    // StateFlow chứa trạng thái UI
    private val _uiState = MutableStateFlow(ChartUiState())
    val uiState: StateFlow<ChartUiState> = _uiState.asStateFlow()

    // Danh sách màu cho biểu đồ tròn
    private val chartColors = listOf(
        0xFFE53935.toInt(), // Đỏ
        0xFF43A047.toInt(), // Xanh lá
        0xFF1E88E5.toInt(), // Xanh dương
        0xFFFB8C00.toInt(), // Cam
        0xFF8E24AA.toInt(), // Tím
        0xFF00ACC1.toInt(), // Cyan
        0xFFFDD835.toInt(), // Vàng
        0xFF6D4C41.toInt(), // Nâu
        0xFF546E7A.toInt(), // Xám
        0xFFD81B60.toInt()  // Hồng
    )

    init {
        val database = AppDatabase.getDatabase(application)
        repository = com.example.moneylover.data.repository.TransactionRepository(database.transactionDao())
        budgetRepository = BudgetRepository(application)
        
        // Khởi tạo ngày cho khoảng thời gian hiện tại
        val (startDate, endDate) = getDateRange(_uiState.value.currentPeriod)
        _uiState.value = _uiState.value.copy(
            startDate = startDate,
            endDate = endDate
        )
        updatePeriodDisplayText()
        loadData()
    }

    // Đặt khoảng thời gian (tuần/tháng/năm)
    fun setPeriod(period: ChartPeriod) {
        val (startDate, endDate) = getDateRange(period)
        _uiState.value = _uiState.value.copy(
            currentPeriod = period,
            startDate = startDate,
            endDate = endDate
        )
        updatePeriodDisplayText()
        loadData()
    }

    // Đặt loại giao dịch (chi tiêu/thu nhập)
    fun setTransactionType(type: TransactionType) {
        _uiState.value = _uiState.value.copy(transactionType = type)
        loadData()
    }

    // Chuyển sang khoảng thời gian trước
    fun navigateToPreviousPeriod() {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = _uiState.value.startDate
        
        when (_uiState.value.currentPeriod) {
            ChartPeriod.WEEK -> {
                calendar.add(Calendar.WEEK_OF_YEAR, -1)
                calendar.set(Calendar.DAY_OF_WEEK, calendar.firstDayOfWeek)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                
                val endCalendar = calendar.clone() as Calendar
                endCalendar.add(Calendar.DAY_OF_WEEK, 6)
                endCalendar.set(Calendar.HOUR_OF_DAY, 23)
                endCalendar.set(Calendar.MINUTE, 59)
                endCalendar.set(Calendar.SECOND, 59)
                
                _uiState.value = _uiState.value.copy(
                    startDate = calendar.timeInMillis,
                    endDate = endCalendar.timeInMillis
                )
            }
            ChartPeriod.MONTH -> {
                calendar.add(Calendar.MONTH, -1)
                calendar.set(Calendar.DAY_OF_MONTH, 1)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                
                val endCalendar = calendar.clone() as Calendar
                endCalendar.set(Calendar.DAY_OF_MONTH, endCalendar.getActualMaximum(Calendar.DAY_OF_MONTH))
                endCalendar.set(Calendar.HOUR_OF_DAY, 23)
                endCalendar.set(Calendar.MINUTE, 59)
                endCalendar.set(Calendar.SECOND, 59)
                
                _uiState.value = _uiState.value.copy(
                    startDate = calendar.timeInMillis,
                    endDate = endCalendar.timeInMillis
                )
            }
            ChartPeriod.YEAR -> {
                calendar.add(Calendar.YEAR, -1)
                calendar.set(Calendar.DAY_OF_YEAR, 1)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                
                val endCalendar = calendar.clone() as Calendar
                endCalendar.set(Calendar.DAY_OF_YEAR, endCalendar.getActualMaximum(Calendar.DAY_OF_YEAR))
                endCalendar.set(Calendar.HOUR_OF_DAY, 23)
                endCalendar.set(Calendar.MINUTE, 59)
                endCalendar.set(Calendar.SECOND, 59)
                
                _uiState.value = _uiState.value.copy(
                    startDate = calendar.timeInMillis,
                    endDate = endCalendar.timeInMillis
                )
            }
        }
        
        updatePeriodDisplayText()
        loadData()
    }

    // Chuyển sang khoảng thời gian sau
    fun navigateToNextPeriod() {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = _uiState.value.startDate
        
        when (_uiState.value.currentPeriod) {
            ChartPeriod.WEEK -> {
                calendar.add(Calendar.WEEK_OF_YEAR, 1)
                calendar.set(Calendar.DAY_OF_WEEK, calendar.firstDayOfWeek)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                
                val endCalendar = calendar.clone() as Calendar
                endCalendar.add(Calendar.DAY_OF_WEEK, 6)
                endCalendar.set(Calendar.HOUR_OF_DAY, 23)
                endCalendar.set(Calendar.MINUTE, 59)
                endCalendar.set(Calendar.SECOND, 59)
                
                _uiState.value = _uiState.value.copy(
                    startDate = calendar.timeInMillis,
                    endDate = endCalendar.timeInMillis
                )
            }
            ChartPeriod.MONTH -> {
                calendar.add(Calendar.MONTH, 1)
                calendar.set(Calendar.DAY_OF_MONTH, 1)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                
                val endCalendar = calendar.clone() as Calendar
                endCalendar.set(Calendar.DAY_OF_MONTH, endCalendar.getActualMaximum(Calendar.DAY_OF_MONTH))
                endCalendar.set(Calendar.HOUR_OF_DAY, 23)
                endCalendar.set(Calendar.MINUTE, 59)
                endCalendar.set(Calendar.SECOND, 59)
                
                _uiState.value = _uiState.value.copy(
                    startDate = calendar.timeInMillis,
                    endDate = endCalendar.timeInMillis
                )
            }
            ChartPeriod.YEAR -> {
                calendar.add(Calendar.YEAR, 1)
                calendar.set(Calendar.DAY_OF_YEAR, 1)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                
                val endCalendar = calendar.clone() as Calendar
                endCalendar.set(Calendar.DAY_OF_YEAR, endCalendar.getActualMaximum(Calendar.DAY_OF_YEAR))
                endCalendar.set(Calendar.HOUR_OF_DAY, 23)
                endCalendar.set(Calendar.MINUTE, 59)
                endCalendar.set(Calendar.SECOND, 59)
                
                _uiState.value = _uiState.value.copy(
                    startDate = calendar.timeInMillis,
                    endDate = endCalendar.timeInMillis
                )
            }
        }
        
        updatePeriodDisplayText()
        loadData()
    }

    // Cập nhật text hiển thị khoảng thời gian
    private fun updatePeriodDisplayText() {
        val state = _uiState.value
        val displayText = when (state.currentPeriod) {
            ChartPeriod.WEEK -> {
                val calendar = Calendar.getInstance()
                calendar.timeInMillis = state.startDate
                val weekNum = calendar.get(Calendar.WEEK_OF_YEAR)
                val year = calendar.get(Calendar.YEAR)
                "Tuần $weekNum, $year"
            }
            ChartPeriod.MONTH -> {
                val calendar = Calendar.getInstance()
                calendar.timeInMillis = state.startDate
                val month = calendar.get(Calendar.MONTH) + 1
                val year = calendar.get(Calendar.YEAR)
                "Tháng $month, $year"
            }
            ChartPeriod.YEAR -> {
                val calendar = Calendar.getInstance()
                calendar.timeInMillis = state.startDate
                calendar.get(Calendar.YEAR).toString()
            }
        }
        _uiState.value = state.copy(periodDisplayText = displayText)
    }

    // Tải dữ liệu biểu đồ
    fun loadData() {
        viewModelScope.launch {
            val state = _uiState.value
            val startDate = state.startDate
            val endDate = state.endDate
            val rangeText = "${dateFormatter.format(Date(startDate))} - ${dateFormatter.format(Date(endDate))}"
            
            repository.allTransactions.collectLatest { transactions ->
                val transactionType = state.transactionType
                
                // Lọc giao dịch theo khoảng thời gian và loại
                val filteredTransactions = transactions.filter { 
                    it.date in startDate..endDate && it.type == transactionType.name
                }
                
                // Tính tổng
                val totalAmount = filteredTransactions.sumOf { it.amount }
                val totalIncome = transactions
                    .filter { it.date in startDate..endDate && it.type == "INCOME" }
                    .sumOf { it.amount }
                val balance = totalIncome - transactions
                    .filter { it.date in startDate..endDate && it.type == "EXPENSE" }
                    .sumOf { it.amount }
                
                // Lấy ngân sách tháng
                val monthlyBudget = budgetRepository.getMonthlyBudget()
                
                if (filteredTransactions.isEmpty()) {
                    _uiState.value = state.copy(
                        dateRangeText = rangeText,
                        totalAmount = 0.0,
                        totalIncome = 0.0,
                        balance = 0.0,
                        budget = monthlyBudget,
                        categories = emptyList(),
                        isEmpty = true,
                        hasData = false,
                        lineChartData = emptyList()
                    )
                } else {
                    // Tạo dữ liệu biểu đồ đường
                    val lineChartData = createLineChartData(transactions, startDate, endDate, transactionType.name, state.currentPeriod)
                    
                    // Nhóm theo danh mục
                    val categoryMap = filteredTransactions.groupBy { it.categoryName }
                    val categories = categoryMap.entries.mapIndexed { index, (name, txList) ->
                        val amount = txList.sumOf { it.amount }
                        val percentage = if (totalAmount > 0) ((amount / totalAmount) * 100).toFloat() else 0f
                        val iconRes = txList.firstOrNull()?.iconRes ?: 0
                        val colorIndex = index % chartColors.size
                        CategoryChartData(
                            categoryId = txList.firstOrNull()?.categoryId ?: 0,
                            categoryName = name,
                            iconRes = iconRes,
                            amount = amount,
                            percentage = percentage,
                            color = chartColors[colorIndex]
                        )
                    }.sortedByDescending { it.amount }
                    
                    _uiState.value = state.copy(
                        dateRangeText = rangeText,
                        totalAmount = totalAmount,
                        totalIncome = totalIncome,
                        balance = balance,
                        budget = monthlyBudget,
                        categories = categories,
                        isEmpty = false,
                        hasData = true,
                        lineChartData = lineChartData
                    )
                }
            }
        }
    }

    // Tính khoảng thời gian theo loại (tuần/tháng/năm)
    private fun getDateRange(period: ChartPeriod): Pair<Long, Long> {
        val calendar = Calendar.getInstance()
        
        return when (period) {
            ChartPeriod.WEEK -> {
                calendar.set(Calendar.DAY_OF_WEEK, calendar.firstDayOfWeek)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                val startDate = calendar.timeInMillis
                
                val endCalendar = calendar.clone() as Calendar
                endCalendar.add(Calendar.DAY_OF_WEEK, 6)
                endCalendar.set(Calendar.HOUR_OF_DAY, 23)
                endCalendar.set(Calendar.MINUTE, 59)
                endCalendar.set(Calendar.SECOND, 59)
                val endDate = endCalendar.timeInMillis
                
                Pair(startDate, endDate)
            }
            ChartPeriod.MONTH -> {
                calendar.set(Calendar.DAY_OF_MONTH, 1)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                val startDate = calendar.timeInMillis
                
                val endCalendar = calendar.clone() as Calendar
                endCalendar.set(Calendar.DAY_OF_MONTH, endCalendar.getActualMaximum(Calendar.DAY_OF_MONTH))
                endCalendar.set(Calendar.HOUR_OF_DAY, 23)
                endCalendar.set(Calendar.MINUTE, 59)
                endCalendar.set(Calendar.SECOND, 59)
                val endDate = endCalendar.timeInMillis
                
                Pair(startDate, endDate)
            }
            ChartPeriod.YEAR -> {
                calendar.set(Calendar.DAY_OF_YEAR, 1)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                val startDate = calendar.timeInMillis
                
                val endCalendar = calendar.clone() as Calendar
                endCalendar.set(Calendar.DAY_OF_YEAR, endCalendar.getActualMaximum(Calendar.DAY_OF_YEAR))
                endCalendar.set(Calendar.HOUR_OF_DAY, 23)
                endCalendar.set(Calendar.MINUTE, 59)
                endCalendar.set(Calendar.SECOND, 59)
                val endDate = endCalendar.timeInMillis
                
                Pair(startDate, endDate)
            }
        }
    }

    // Format số tiền
    fun formatCurrency(amount: Double): String {
        val formatter = java.text.NumberFormat.getNumberInstance(Locale("vi", "VN"))
        return formatter.format(amount.toLong())
    }

    // Lấy dữ liệu cột cho biểu đồ cột
    fun getCategoryBarData(): List<BarChartView.BarData> {
        val state = _uiState.value
        val categories = state.categories
        
        return categories.take(7).map { category ->
            BarChartView.BarData(
                label = category.categoryName,
                expense = if (state.transactionType == TransactionType.EXPENSE) category.amount.toFloat() else 0f,
                income = if (state.transactionType == TransactionType.INCOME) category.amount.toFloat() else 0f,
                color = category.color
            )
        }
    }
    
    // Lấy ngân sách tháng
    fun getMonthlyBudget(): Double {
        return budgetRepository.getMonthlyBudget()
    }

    // Tải lại ngân sách từ repository (gọi khi quay lại từ BudgetActivity)
    fun reloadBudgetFromRepository() {
        val monthlyBudget = budgetRepository.getMonthlyBudget()
        _uiState.value = _uiState.value.copy(budget = monthlyBudget)
    }
    
    // Tạo dữ liệu biểu đồ đường theo khoảng thời gian
    private fun createLineChartData(
        transactions: List<com.example.moneylover.data.model.TransactionEntity>,
        startDate: Long,
        endDate: Long,
        txType: String,
        period: ChartPeriod
    ): List<LineChartView.DataPoint> {
        val filteredTx = transactions.filter { 
            it.date in startDate..endDate && it.type == txType
        }
        
        return when (period) {
            ChartPeriod.WEEK -> createDailyDataPoints(filteredTx, startDate, 7)
            ChartPeriod.MONTH -> {
                val cal = Calendar.getInstance()
                cal.timeInMillis = startDate
                val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
                createDailyDataPoints(filteredTx, startDate, daysInMonth)
            }
            ChartPeriod.YEAR -> createMonthlyDataPoints(filteredTx, startDate, endDate)
        }
    }
    
    // Tạo dữ liệu điểm theo ngày (cho tuần/tháng)
    private fun createDailyDataPoints(
        transactions: List<com.example.moneylover.data.model.TransactionEntity>,
        startDate: Long,
        numDays: Int
    ): List<LineChartView.DataPoint> {
        val result = mutableListOf<LineChartView.DataPoint>()
        val cal = Calendar.getInstance()
        
        for (i in 0 until numDays) {
            cal.timeInMillis = startDate
            cal.add(Calendar.DAY_OF_MONTH, i)
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            
            val dayStart = cal.timeInMillis
            cal.set(Calendar.HOUR_OF_DAY, 23)
            cal.set(Calendar.MINUTE, 59)
            cal.set(Calendar.SECOND, 59)
            val dayEnd = cal.timeInMillis
            
            val dayTotal = transactions
                .filter { it.date in dayStart..dayEnd }
                .sumOf { it.amount }
            
            val dayLabel = SimpleDateFormat("d/M", Locale("vi", "VN")).format(cal.time)
            
            result.add(LineChartView.DataPoint(
                label = dayLabel,
                value = dayTotal.toFloat(),
                date = dayStart
            ))
        }
        
        return result
    }
    
    // Tạo dữ liệu điểm theo tháng (cho năm)
    private fun createMonthlyDataPoints(
        transactions: List<com.example.moneylover.data.model.TransactionEntity>,
        startDate: Long,
        endDate: Long
    ): List<LineChartView.DataPoint> {
        val result = mutableListOf<LineChartView.DataPoint>()
        val cal = Calendar.getInstance()
        cal.timeInMillis = startDate
        
        for (month in 0..11) {
            cal.timeInMillis = startDate
            cal.add(Calendar.MONTH, month)
            cal.set(Calendar.DAY_OF_MONTH, 1)
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            
            val monthStart = cal.timeInMillis
            
            cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
            cal.set(Calendar.HOUR_OF_DAY, 23)
            cal.set(Calendar.MINUTE, 59)
            cal.set(Calendar.SECOND, 59)
            val monthEnd = cal.timeInMillis
            
            val monthTotal = transactions
                .filter { it.date in monthStart..monthEnd }
                .sumOf { it.amount }
            
            val monthLabel = SimpleDateFormat("M", Locale("vi", "VN")).format(cal.time)
            
            result.add(LineChartView.DataPoint(
                label = monthLabel,
                value = monthTotal.toFloat(),
                date = monthStart
            ))
        }
        
        return result
    }
}
