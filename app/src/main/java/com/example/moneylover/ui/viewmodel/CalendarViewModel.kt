package com.example.moneylover.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.moneylover.data.database.AppDatabase
import com.example.moneylover.data.model.TransactionEntity
import com.example.moneylover.data.repository.TransactionRepository
import com.example.moneylover.ui.state.CalendarDayUi
import com.example.moneylover.ui.state.CalendarUiState
import com.example.moneylover.ui.state.MonthStatUi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.Calendar

/**
 * ViewModel cho CalendarActivity
 * 
 * Nhiệm vụ:
 * 1. Quản lý trạng thái lịch (ngày/tháng/năm được chọn)
 * 2. Tải và xử lý dữ liệu giao dịch cho lịch
 * 3. Tính toán thống kê theo ngày/tháng
 */
class CalendarViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: TransactionRepository

    init {
        val database = AppDatabase.getDatabase(application)
        repository = TransactionRepository(database.transactionDao())
    }

    private val _uiState = MutableStateFlow(CalendarUiState())
    val uiState: StateFlow<CalendarUiState> = _uiState.asStateFlow()

    // Cache để track ngày có giao dịch
    private var dayHasExpense = mutableSetOf<Int>()
    private var dayHasIncome = mutableSetOf<Int>()

    init {
        // Khởi tạo ngày hiện tại
        val cal = Calendar.getInstance()
        _uiState.value = _uiState.value.copy(
            selectedYear = cal.get(Calendar.YEAR),
            selectedMonth = cal.get(Calendar.MONTH) + 1,
            selectedDay = cal.get(Calendar.DAY_OF_MONTH)
        )
    }

    /**
     * Chọn ngày - cập nhật calendar và load giao dịch ngày đó
     */
    fun selectDay(day: Int) {
        _uiState.value = _uiState.value.copy(selectedDay = day)
        loadTransactionsForSelectedDay()
    }

    /**
     * Chọn tháng - cập nhật calendar
     */
    fun setSelectedMonth(year: Int, month: Int) {
        dayHasExpense.clear()
        dayHasIncome.clear()
        
        _uiState.value = _uiState.value.copy(
            selectedYear = year,
            selectedMonth = month,
            selectedDay = 1,
            calendarDays = emptyList()
        )
        
        loadTransactionsForMonth()
    }

    /**
     * Tải giao dịch cho tháng hiện tại
     */
    fun loadTransactionsForMonth() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            repository.getByMonth(
                _uiState.value.selectedYear,
                _uiState.value.selectedMonth
            ).collectLatest { transactions ->
                // Reset cache
                dayHasExpense.clear()
                dayHasIncome.clear()

                // Đếm ngày có giao dịch
                val calendar = Calendar.getInstance()
                for (t in transactions) {
                    calendar.timeInMillis = t.date
                    val day = calendar.get(Calendar.DAY_OF_MONTH)
                    if (t.type == "EXPENSE") {
                        dayHasExpense.add(day)
                    } else if (t.type == "INCOME") {
                        dayHasIncome.add(day)
                    }
                }

                // Render lịch với dữ liệu mới
                renderCalendar()

                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }

    /**
     * Tải giao dịch cho ngày được chọn
     */
    fun loadTransactionsForSelectedDay() {
        viewModelScope.launch {
            repository.getByDay(
                _uiState.value.selectedYear,
                _uiState.value.selectedMonth,
                _uiState.value.selectedDay
            ).collectLatest { transactions ->
                val totalExpense = transactions.filter { it.type == "EXPENSE" }.sumOf { it.amount }
                val totalIncome = transactions.filter { it.type == "INCOME" }.sumOf { it.amount }
                val balance = totalIncome - totalExpense

                _uiState.value = _uiState.value.copy(
                    dayTransactions = transactions,
                    dayTotalExpense = totalExpense,
                    dayTotalIncome = totalIncome,
                    dayBalance = balance
                )
            }
        }
    }

    /**
     * Render calendar grid
     */
    private fun renderCalendar() {
        val state = _uiState.value
        val cal = Calendar.getInstance()
        cal.set(state.selectedYear, state.selectedMonth - 1, 1)
        
        val firstDayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
        val maxDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH)

        val items = mutableListOf<CalendarDayUi>()
        
        // Thêm ô trống cho các ngày trước ngày 1
        val leading = firstDayOfWeek - Calendar.SUNDAY
        for (i in 0 until leading) {
            items.add(CalendarDayUi(0, false))
        }
        
        // Thêm các ngày trong tháng
        for (d in 1..maxDay) {
            items.add(
                CalendarDayUi(
                    day = d,
                    inMonth = true,
                    isSelected = d == state.selectedDay,
                    hasExpense = dayHasExpense.contains(d),
                    hasIncome = dayHasIncome.contains(d)
                )
            )
        }
        
        // Đảm bảo grid 7 cột
        while (items.size % 7 != 0) {
            items.add(CalendarDayUi(0, false))
        }

        _uiState.value = _uiState.value.copy(calendarDays = items)
    }

    /**
     * Tải thống kê tất cả tháng (cho báo cáo)
     */
    fun loadAllMonthStats() {
        viewModelScope.launch {
            repository.allTransactions.collectLatest { allTransactions ->
                if (allTransactions.isEmpty()) {
                    _uiState.value = _uiState.value.copy(
                        monthStats = emptyList(),
                        totalAllTimeExpense = 0.0,
                        totalAllTimeIncome = 0.0,
                        totalAllTimeBalance = 0.0
                    )
                } else {
                    val statsMap = mutableMapOf<String, MonthStatUi>()

                    for (t in allTransactions) {
                        val cal = Calendar.getInstance()
                        cal.timeInMillis = t.date
                        val m = cal.get(Calendar.MONTH) + 1
                        val y = cal.get(Calendar.YEAR)
                        val key = "$y-$m"

                        val existing = statsMap[key]
                        if (existing != null) {
                            val newExpense = if (t.type == "EXPENSE") existing.totalExpense + t.amount else existing.totalExpense
                            val newIncome = if (t.type == "INCOME") existing.totalIncome + t.amount else existing.totalIncome
                            statsMap[key] = MonthStatUi(m, y, newExpense, newIncome)
                        } else {
                            val expense = if (t.type == "EXPENSE") t.amount else 0.0
                            val income = if (t.type == "INCOME") t.amount else 0.0
                            statsMap[key] = MonthStatUi(m, y, expense, income)
                        }
                    }

                    val sortedStats = statsMap.values.sortedWith(compareBy({ it.year }, { it.month })).reversed()
                    val totalExpense = sortedStats.sumOf { it.totalExpense }
                    val totalIncome = sortedStats.sumOf { it.totalIncome }
                    val totalBalance = totalIncome - totalExpense

                    _uiState.value = _uiState.value.copy(
                        monthStats = sortedStats,
                        totalAllTimeExpense = totalExpense,
                        totalAllTimeIncome = totalIncome,
                        totalAllTimeBalance = totalBalance
                    )
                }
            }
        }
    }

    /**
     * Xóa giao dịch
     */
    fun deleteTransaction(transaction: TransactionEntity) {
        viewModelScope.launch {
            repository.delete(transaction)
            // Reload
            loadTransactionsForMonth()
            loadTransactionsForSelectedDay()
        }
    }

    /**
     * Format số tiền
     */
    fun formatCurrency(amount: Double): String {
        val formatter = java.text.NumberFormat.getNumberInstance(java.util.Locale("vi", "VN"))
        return formatter.format(amount) + "đ"
    }

    /**
     * Lấy title tháng để hiển thị
     */
    fun getMonthTitle(): String {
        val state = _uiState.value
        return "thg ${state.selectedMonth} ${state.selectedYear}"
    }
}
