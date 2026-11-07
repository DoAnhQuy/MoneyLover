package com.example.moneylover.ui.viewmodel

import android.annotation.SuppressLint
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.moneylover.data.database.AppDatabase
import com.example.moneylover.data.model.AccountEntity
import com.example.moneylover.data.model.TransactionEntity
import com.example.moneylover.data.repository.AccountRepository
import com.example.moneylover.data.repository.BudgetRepository
import com.example.moneylover.data.repository.TransactionRepository
import com.example.moneylover.ui.state.MainUiState
import com.example.moneylover.ui.state.MainView
import com.example.moneylover.ui.state.MonthStatUi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.util.Calendar

/**
 * ViewModel cho MainActivity
 * 
 * Nhiệm vụ của ViewModel trong MVVM:
 * 1. Lưu trữ trạng thái UI (UiState)
 * 2. Xử lý logic nghiệp vụ (tính toán, validate)
 * 3. Cung cấp dữ liệu cho View thông qua StateFlow
 * 4. KHÔNG chứa code UI trực tiếp (Activity/Fragment sẽ observe)
 * 
 * ViewModel sống sót qua configuration change (xoay màn hình)
 */
open class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val transactionRepository: TransactionRepository
    private val accountRepository: AccountRepository
    private val budgetRepository: BudgetRepository

    // StateFlow: Luồng dữ liệu có thể observe, tự động cập nhật khi thay đổi
    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    // Cache transactions - GIỮ VĨNH VIỄN khi đã load
    private var cachedTransactions: List<TransactionEntity> = emptyList()
    private var cachedMonthStats: List<MonthStatUi> = emptyList()
    private var cachedTotalAllTimeExpense: Double = 0.0
    private var cachedTotalAllTimeIncome: Double = 0.0
    private var cachedTotalAllTimeBalance: Double = 0.0
    
    // Cache accounts
    private var cachedAccounts: List<AccountEntity> = emptyList()
    private var cachedTotalAssets: Double = 0.0
    private var cachedTotalLiabilities: Double = 0.0
    private var cachedNetAssets: Double = 0.0
    
    // Cache budget
    private var cachedCurrentMonthBudget: Double = 0.0
    
    // Job để quản lý coroutine load
    private var loadJob: Job? = null
    
    // Flag đã load dữ liệu chưa
    private var isDataLoaded = false

    init {
        // Khởi tạo Database và Repository một lần duy nhất
        val database = AppDatabase.getDatabase(application)
        transactionRepository = TransactionRepository(database.transactionDao())
        accountRepository = AccountRepository(database.accountDao())
        budgetRepository = BudgetRepository(application)
        
        // Load ngân sách đã lưu
        cachedCurrentMonthBudget = budgetRepository.getMonthlyBudget()
        
        // Khởi tạo ngày/tháng/năm mặc định
        val cal = Calendar.getInstance()
        _uiState.value = _uiState.value.copy(
            selectedYear = cal.get(Calendar.YEAR),
            selectedMonth = cal.get(Calendar.MONTH) + 1
        )
    }

    /**
     * Tải giao dịch từ Repository theo tháng/năm đã chọn
     * Dùng launch thay vì collectLatest để tránh race condition
     */
    fun loadTransactions(forceReload: Boolean = false) {
        val currentYear = _uiState.value.selectedYear
        val currentMonth = _uiState.value.selectedMonth
        
        // Nếu không force reload và đã load rồi, giữ nguyên dữ liệu
        if (!forceReload && isDataLoaded && cachedTransactions.isNotEmpty()) {
            return
        }
        
        // Cancel job cũ nếu có
        loadJob?.cancel()
        
        loadJob = viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            // Sử dụng first() thay vì collectLatest để tránh race condition
            val transactions = transactionRepository.getByMonth(currentYear, currentMonth).first()
            
            cachedTransactions = transactions

            // Tính toán tổng chi tiêu/thu nhập
            val totalExpense = transactions.filter { it.type == "EXPENSE" }.sumOf { it.amount }
            val totalIncome = transactions.filter { it.type == "INCOME" }.sumOf { it.amount }
            val balance = totalIncome - totalExpense

            _uiState.value = _uiState.value.copy(
                isLoading = false,
                transactions = transactions,
                totalExpense = totalExpense,
                totalIncome = totalIncome,
                balance = balance,
                isEmpty = transactions.isEmpty(),
                currentMonthBudget = cachedCurrentMonthBudget,
                currentMonthSpending = totalExpense,
                currentMonthRemaining = cachedCurrentMonthBudget - totalExpense
            )
            
            isDataLoaded = true
        }
        
        // Load all-time stats cho header
        loadAllTransactionsForStats()
        
        // Load accounts
        loadAccounts()
    }

    /**
     * Tải danh sách tài khoản
     * Chỉ load một lần, không collect liên tục
     */
    private fun loadAccounts() {
        viewModelScope.launch {
            try {
                val accounts = accountRepository.allAccounts.first()
                cachedAccounts = accounts
                
                // Tính tổng tài sản và nợ
                // Nợ phải trả = thẻ tín dụng (balance < 0), loại debt, hoặc tài khoản có isLiability = true
                // Tài sản = tất cả tài khoản KHÔNG phải nợ
                val totalLiabilities = accounts.filter { account ->
                    account.type == "credit" && account.balance < 0 || 
                    account.type == "debt" || 
                    account.isLiability
                }.sumOf { 
                    if (it.type == "debt" || it.isLiability) it.balance 
                    else -it.balance // credit card: -balance (because balance is negative)
                }
                
                // Tài sản = tất cả balance dương không phải nợ
                val totalAssets = accounts.filter { account ->
                    account.balance > 0 && 
                    account.type != "debt" && 
                    !account.isLiability &&
                    !(account.type == "credit" && account.balance < 0)
                }.sumOf { it.balance }
                
                // Tài sản ròng = Tài sản - Nợ phải trả
                val netAssets = totalAssets - totalLiabilities
                
                cachedTotalAssets = totalAssets
                cachedTotalLiabilities = totalLiabilities
                cachedNetAssets = netAssets
                
                _uiState.value = _uiState.value.copy(
                    accounts = accounts,
                    totalAssets = totalAssets,
                    totalLiabilities = totalLiabilities,
                    netAssets = netAssets
                )
            } catch (e: Exception) {
                // Log error but don't crash
            }
        }
    }

    /**
     * Chuyển đổi tháng - cập nhật state
     */
    fun setSelectedMonth(year: Int, month: Int) {
        // Reset flag để load dữ liệu tháng mới
        isDataLoaded = false
        cachedTransactions = emptyList()
        
        _uiState.value = _uiState.value.copy(
            selectedYear = year,
            selectedMonth = month,
            isEmpty = true // Reset để hiển thị loading
        )
        // Force reload vì đang chuyển sang tháng khác
        loadTransactions(forceReload = true)
    }

    /**
     * Chuyển đổi giữa HOME, MONTHLY_STATS và REPORT view
     * KHÔNG reload dữ liệu - giữ nguyên totals hiện tại
     */
    fun setCurrentView(view: MainView) {
        val currentState = _uiState.value

        // Nếu chuyển sang REPORT hoặc MONTHLY_STATS, giữ nguyên dữ liệu tháng hiện tại
        if (view == MainView.REPORT || view == MainView.MONTHLY_STATS) {
            _uiState.value = currentState.copy(
                currentView = view,
                transactions = cachedTransactions,
                totalExpense = if (cachedTransactions.isNotEmpty()) cachedTransactions.filter { it.type == "EXPENSE" }.sumOf { it.amount } else currentState.totalExpense,
                totalIncome = if (cachedTransactions.isNotEmpty()) cachedTransactions.filter { it.type == "INCOME" }.sumOf { it.amount } else currentState.totalIncome,
                balance = if (cachedTransactions.isNotEmpty()) (if (cachedTransactions.isNotEmpty()) cachedTransactions.filter { it.type == "INCOME" }.sumOf { it.amount } else currentState.totalIncome) - (if (cachedTransactions.isNotEmpty()) cachedTransactions.filter { it.type == "EXPENSE" }.sumOf { it.amount } else currentState.totalExpense) else currentState.balance,
                monthStats = cachedMonthStats,
                totalAllTimeExpense = cachedTotalAllTimeExpense,
                totalAllTimeIncome = cachedTotalAllTimeIncome,
                totalAllTimeBalance = cachedTotalAllTimeBalance,
                accounts = cachedAccounts,
                totalAssets = cachedTotalAssets,
                totalLiabilities = cachedTotalLiabilities,
                netAssets = cachedNetAssets,
                currentMonthBudget = cachedCurrentMonthBudget,
                currentMonthSpending = currentState.totalExpense,
                currentMonthRemaining = cachedCurrentMonthBudget - currentState.totalExpense
            )
        }
        // Nếu chuyển về HOME
        else if (currentState.currentView != MainView.HOME && view == MainView.HOME) {
            // Giữ nguyên dữ liệu, KHÔNG reload để tránh reset header
            _uiState.value = currentState.copy(
                currentView = MainView.HOME,
                transactions = cachedTransactions,
                totalExpense = if (cachedTransactions.isNotEmpty()) cachedTransactions.filter { it.type == "EXPENSE" }.sumOf { it.amount } else currentState.totalExpense,
                totalIncome = if (cachedTransactions.isNotEmpty()) cachedTransactions.filter { it.type == "INCOME" }.sumOf { it.amount } else currentState.totalIncome,
                balance = if (cachedTransactions.isNotEmpty()) (if (cachedTransactions.isNotEmpty()) cachedTransactions.filter { it.type == "INCOME" }.sumOf { it.amount } else currentState.totalIncome) - (if (cachedTransactions.isNotEmpty()) cachedTransactions.filter { it.type == "EXPENSE" }.sumOf { it.amount } else currentState.totalExpense) else currentState.balance,
                isEmpty = cachedTransactions.isEmpty()
            )
        }
        // Trường hợp khác (đã ở đúng view)
        else {
            _uiState.value = currentState.copy(currentView = view)
        }
    }

    /**
     * Tải tất cả giao dịch để tính thống kê cho màn hình báo cáo và header
     * Chỉ cập nhật monthStats và all-time totals, KHÔNG reset current month data
     */
    private fun loadAllTransactionsForStats() {
        viewModelScope.launch {
            val allTransactions = transactionRepository.allTransactions.first()
            
            if (allTransactions.isEmpty()) {
                cachedMonthStats = emptyList()
                cachedTotalAllTimeExpense = 0.0
                cachedTotalAllTimeIncome = 0.0
                cachedTotalAllTimeBalance = 0.0
                
                // Chỉ update state nếu đang ở HOME
                if (_uiState.value.currentView == MainView.HOME) {
                    _uiState.value = _uiState.value.copy(
                        monthStats = emptyList(),
                        totalAllTimeExpense = 0.0,
                        totalAllTimeIncome = 0.0,
                        totalAllTimeBalance = 0.0
                    )
                }
            } else {
                // Tính thống kê theo tháng
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

                // Sắp xếp theo năm và tháng giảm dần
                val sortedStats = statsMap.values.sortedWith(compareBy({ it.year }, { it.month })).reversed()

                // Tính tổng
                val totalExpense = sortedStats.sumOf { it.totalExpense }
                val totalIncome = sortedStats.sumOf { it.totalIncome }
                val totalBalance = totalIncome - totalExpense
                
                // Cache lại
                cachedMonthStats = sortedStats
                cachedTotalAllTimeExpense = totalExpense
                cachedTotalAllTimeIncome = totalIncome
                cachedTotalAllTimeBalance = totalBalance
                
                // Update state với dữ liệu đã cache
                _uiState.value = _uiState.value.copy(
                    monthStats = sortedStats,
                    totalAllTimeExpense = totalExpense,
                    totalAllTimeIncome = totalIncome,
                    totalAllTimeBalance = totalBalance
                )
            }
        }
    }

    /**
     * Xóa giao dịch - ViewModel xử lý logic xóa
     */
    fun deleteTransaction(transaction: TransactionEntity) {
        viewModelScope.launch {
            transactionRepository.delete(transaction)
            // Sau khi xóa, load lại dữ liệu
            loadTransactions()
        }
    }

    /**
     * Refresh dữ liệu tháng hiện tại (chỉ gọi khi quay lại từ AddTransaction)
     * Luôn reload để lấy dữ liệu mới nhất
     */
    fun refreshCurrentMonthData() {
        // Force reload khi quay lại từ AddTransaction
        loadTransactions(forceReload = true)
    }

    /**
     * Cập nhật ngân sách tháng hiện tại
     */
    fun setCurrentMonthBudget(budget: Double) {
        cachedCurrentMonthBudget = budget
        budgetRepository.setMonthlyBudget(budget)
        val currentSpending = _uiState.value.currentMonthSpending
        _uiState.value = _uiState.value.copy(
            currentMonthBudget = budget,
            currentMonthRemaining = budget - currentSpending
        )
    }

    /**
     * Reload ngân sách từ repository (gọi khi quay lại từ BudgetActivity)
     */
    fun reloadBudgetFromRepository() {
        val savedBudget = budgetRepository.getMonthlyBudget()
        if (savedBudget != cachedCurrentMonthBudget) {
            cachedCurrentMonthBudget = savedBudget
            val currentSpending = _uiState.value.currentMonthSpending
            _uiState.value = _uiState.value.copy(
                currentMonthBudget = savedBudget,
                currentMonthRemaining = savedBudget - currentSpending
            )
        }
    }

    /**
     * Lấy giao dịch đang được cache
     */
    fun getCachedTransactions(): List<TransactionEntity> = cachedTransactions

    /**
     * Format số tiền thành chuỗi VND
     */
    fun formatCurrency(amount: Double): String {
        val formatter = java.text.NumberFormat.getNumberInstance(java.util.Locale("vi", "VN"))
        return formatter.format(amount) + "đ"
    }

    /**
     * Format số tiền không có đuôi "đ"
     */
    @SuppressLint("DefaultLocale")
    fun formatAmount(amount: String): String {
        return try {
            val value = amount.replace(".", "").replace(",", "").toDouble()
            String.format("%,.0f", value).replace(",", ".")
        } catch (e: Exception) {
            amount
        }
    }

    /**
     * Lấy tất cả thống kê tháng
     */
    fun getAllMonthStats(): List<MonthStatUi> {
        val selectedYear = _uiState.value.statsSelectedYear
        return if (selectedYear == 0) {
            cachedMonthStats
        } else {
            cachedMonthStats.filter { it.year == selectedYear }
        }
    }

    /**
     * Lấy danh sách các năm có dữ liệu
     */
    fun getAvailableYears(): List<Int> {
        return cachedMonthStats.map { it.year }.distinct().sortedDescending()
    }

    /**
     * Đặt năm được chọn cho màn hình thống kê
     */
    fun setSelectedYearForStats(year: Int) {
        _uiState.value = _uiState.value.copy(statsSelectedYear = year)
    }
}
