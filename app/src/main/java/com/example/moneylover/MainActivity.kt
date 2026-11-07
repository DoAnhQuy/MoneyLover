package com.example.moneylover

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.GridLayout
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.ViewFlipper
import android.widget.FrameLayout
import android.view.ViewGroup
import android.view.Gravity
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.moneylover.data.database.AppDatabase
import com.example.moneylover.data.repository.BudgetRepository
import com.example.moneylover.databinding.ActivityMainBinding
import com.example.moneylover.databinding.BottomSheetMonthPickerBinding
import com.example.moneylover.databinding.BottomSheetMonthlyDetailBinding
import com.example.moneylover.databinding.BottomSheetTransactionDetailBinding
import com.example.moneylover.databinding.BottomSheetYearPickerBinding
import com.example.moneylover.databinding.LayoutMonthlyStatsBinding
import com.example.moneylover.ui.adapter.MonthStatAdapter
import com.example.moneylover.ui.adapter.YearPickerAdapter
import com.example.moneylover.ui.adapter.createMonthStatAdapter
import com.example.moneylover.ui.adapter.TransactionAdapter
import com.example.moneylover.ui.state.MainUiState
import com.example.moneylover.ui.state.MainView
import com.example.moneylover.ui.viewmodel.MainViewModel
import com.example.moneylover.utils.NotificationHelper
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var transactionAdapter: TransactionAdapter
    private var reportView: View? = null
    private var monthlyStatsView: View? = null
    private var monthlyStatsBinding: LayoutMonthlyStatsBinding? = null
    private var pendingEditDate: Long? = null

    // Biến kiểm tra thời gian thoát app
    private var pausedTimeMillis: Long = 0
    private val timeoutMinutes = 3 //
    
    private lateinit var prefs: SharedPreferences

    // ViewModel: bằng viewModels() sẽ tự động tạo hoặc lấy instance đã tồn tại
    private val viewModel: MainViewModel by viewModels()
    
    // Direct access to BudgetRepository for real-time budget display
    private val budgetRepository: BudgetRepository by lazy {
        BudgetRepository(this)
    }

    private val currencyFormatter = NumberFormat.getNumberInstance(Locale("vi", "VN"))
    private val dateFormatter = SimpleDateFormat("dd/MM/yyyy", Locale("vi", "VN"))
    private val timeFormatter = SimpleDateFormat("HH:mm", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Khởi tạo SharedPreferences
        prefs = getSharedPreferences(LoginActivity.PREFS_NAME, MODE_PRIVATE)

        // Kiểm tra thời gian thoát app - nếu thoát quá 3 phút thì logout
        val lastPausedTime = prefs.getLong("last_paused_time", 0)
        if (lastPausedTime > 0) {
            val elapsedMinutes = (System.currentTimeMillis() - lastPausedTime) / 60000
            if (elapsedMinutes >= timeoutMinutes) {
                logout()
                return
            }
        }

        // Khởi tạo thông báo
        NotificationHelper.createNotificationChannel(this)

        // Lên lịch tất cả reminders đã bật
        lifecycleScope.launch {
            val database = com.example.moneylover.data.database.AppDatabase.getDatabase(this@MainActivity)
            val repository = com.example.moneylover.data.repository.ReminderRepository(database.reminderDao())
            repository.getEnabledReminders().collectLatest { reminders ->
                reminders.forEach { reminder ->
                    ReminderScheduler.scheduleReminder(this@MainActivity, reminder)
                }
            }
        }

        setupTransactionList()
        setupClickListeners()
        observeUiState()

        viewModel.loadTransactions()
    }

    private val REQUEST_ADD_TRANSACTION = 1001

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_ADD_TRANSACTION && resultCode == Activity.RESULT_OK) {
            // Lấy ngày giao dịch vừa thêm để xử lý chuyển tháng nếu cần
            data?.getLongExtra(AddTransactionActivity.RESULT_DATE, 0)?.let { newDate ->
                if (newDate > 0) {
                    pendingEditDate = newDate
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        
        // Luôn reload ngân sách từ repository khi quay lại
        viewModel.reloadBudgetFromRepository()
        
        // Cập nhật dữ liệu báo cáo nếu đang ở màn hình REPORT
        if (viewModel.uiState.value.currentView == MainView.REPORT) {
            updateReportData()
        }
        
        // Xử lý ngày giao dịch mới (nếu có)
        pendingEditDate?.let { newDate ->
            val cal = Calendar.getInstance()
            cal.timeInMillis = newDate
            val editMonth = cal.get(Calendar.MONTH) + 1
            val editYear = cal.get(Calendar.YEAR)
            
            val state = viewModel.uiState.value
            // Nếu khác tháng hiện tại thì chuyển tháng
            if (editMonth != state.selectedMonth || editYear != state.selectedYear) {
                viewModel.setSelectedMonth(editYear, editMonth)
            }
            pendingEditDate = null
        }

        // Luôn force reload khi quay về trang chủ từ bất kỳ màn hình nào
        viewModel.refreshCurrentMonthData()
    }

    override fun onPause() {
        super.onPause()
        // Lưu thời gian thoát app
        prefs.edit().putLong("last_paused_time", System.currentTimeMillis()).apply()
    }

    /**
     * Đăng xuất và chuyển về màn hình đăng nhập
     */
    private fun logout() {
        prefs.edit().apply {
            putBoolean(LoginActivity.KEY_IS_LOGGED_IN, false)
            remove(LoginActivity.KEY_USER_EMAIL)
            remove(LoginActivity.KEY_USER_NAME)
            remove("last_paused_time")
            apply()
        }
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }


    @SuppressLint("RepeatOnLifecycleWrongUsage")
    private fun observeUiState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collectLatest { state ->
                    renderUi(state)
                }
            }
        }
    }


    private fun renderUi(state: MainUiState) {
        // Tính lại từ transactions để đảm bảo đồng nhất với updateReportData()
        val transactions = state.transactions
        val totalExpense = transactions.filter { it.type == "EXPENSE" }.sumOf { it.amount }
        val totalIncome = transactions.filter { it.type == "INCOME" }.sumOf { it.amount }
        val balance = totalIncome - totalExpense

        // Cập nhật header
        binding.header.txtYearLabel.text = state.selectedYear.toString()
        binding.header.txtMonthLabel.text = "Thg ${state.selectedMonth}"
        binding.header.txtTotalExpense.text = viewModel.formatCurrency(totalExpense)
        binding.header.txtTotalIncome.text = viewModel.formatCurrency(totalIncome)
        binding.header.txtBalance.text = viewModel.formatCurrency(balance)

        // Hiển thị danh sách giao dịch
        if (state.isEmpty) {
            binding.rv.visibility = View.GONE
            (binding.root.findViewById<View>(R.id.layout_empty_state))?.visibility = View.VISIBLE
        } else {
            (binding.root.findViewById<View>(R.id.layout_empty_state))?.visibility = View.GONE
            binding.rv.visibility = View.VISIBLE
            transactionAdapter.submitList(state.transactions)
        }

        // Hiển thị view tương ứng
        when (state.currentView) {
            MainView.HOME -> showHomeScreen()
            MainView.MONTHLY_STATS -> showMonthlyStatsScreen()
            MainView.REPORT -> {
                showReportScreen()
                updateReportData()
            }

            MainView.OVERVIEW -> TODO()
        }
    }

    /**
     * Setup RecyclerView cho danh sách giao dịch
     */
    private fun setupTransactionList() {
        transactionAdapter = TransactionAdapter(
            onItemClick = { transaction ->
                showTransactionDetail(transaction)
            },
            onItemLongClick = { transaction ->
                showTransactionOptions(transaction)
            }
        )
        binding.rv.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = transactionAdapter
        }
    }

    /**
     * Setup click listeners - gọi ViewModel methods khi cần
     */
    private fun setupClickListeners() {
        // Mũi tên và vùng chọn tháng - mở bảng chọn tháng
        binding.header.btnOpenOverview.setOnClickListener {
            showMonthPickerSheet()
        }

        // Vùng chọn tháng - mở bảng chọn tháng
        binding.header.btnPickMonth.setOnClickListener {
            showMonthPickerSheet()
        }

        // Mở Calendar
        binding.header.btnCalendar.setOnClickListener {
            startActivity(Intent(this, CalendarActivity::class.java))
        }

        // Bottom nav
        binding.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_chart -> {
                    startActivity(Intent(this, ChartActivity::class.java))
                    true
                }
                R.id.nav_report -> {
                    viewModel.setCurrentView(MainView.REPORT)
                    true
                }
                R.id.nav_home -> {
                    viewModel.setCurrentView(MainView.HOME)
                    true
                }
                R.id.nav_profile -> {
                    startActivity(Intent(this, MeActivity::class.java))
                    true
                }
                else -> true
            }
        }

        // FAB thêm giao dịch
        binding.fabAdd.setOnClickListener {
            val state = viewModel.uiState.value
            val intent = Intent(this, AddTransactionActivity::class.java).apply {
                putExtra(AddTransactionActivity.EXTRA_SELECTED_YEAR, state.selectedYear)
                putExtra(AddTransactionActivity.EXTRA_SELECTED_MONTH, state.selectedMonth)
            }
            startActivityForResult(intent, REQUEST_ADD_TRANSACTION)
        }

        // Header items
        binding.header.layoutExpense.setOnClickListener {
            showMonthlyDetailSheet("EXPENSE")
        }
        binding.header.layoutIncome.setOnClickListener {
            showMonthlyDetailSheet("INCOME")
        }
        binding.header.layoutBalance.setOnClickListener {
            showMonthlyDetailSheet("ALL")
        }
    }

    /**
     * Hiển thị chi tiết giao dịch
     */
    private fun showTransactionDetail(transaction: com.example.moneylover.data.model.TransactionEntity) {
        val dialog = BottomSheetDialog(this)
        val sheetBinding = BottomSheetTransactionDetailBinding.inflate(layoutInflater)

        val date = Date(transaction.date)
        val typeText = when (transaction.type) {
            "EXPENSE" -> "Chi tiêu"
            "INCOME" -> "Thu nhập"
            else -> "Chuyển khoản"
        }

        sheetBinding.imgIcon.setImageResource(transaction.iconRes)
        sheetBinding.txtCategory.text = transaction.categoryName
        sheetBinding.txtType.text = typeText
        sheetBinding.txtAmount.text = "${if (transaction.type == "EXPENSE") "-" else "+"}${viewModel.formatCurrency(transaction.amount)}"
        sheetBinding.txtAmount.setTextColor(
            if (transaction.type == "EXPENSE") getColor(R.color.black) else getColor(R.color.black)
        )
        sheetBinding.txtDate.text = dateFormatter.format(date)
        sheetBinding.txtTime.text = timeFormatter.format(date)

        if (!transaction.note.isNullOrBlank()) {
            sheetBinding.layoutNote.visibility = View.VISIBLE
            sheetBinding.txtNote.text = transaction.note
        } else {
            sheetBinding.layoutNote.visibility = View.GONE
        }

        sheetBinding.btnEdit.setOnClickListener {
            dialog.dismiss()
            openEditTransaction(transaction)
        }

        sheetBinding.btnDelete.setOnClickListener {
            dialog.dismiss()
            confirmDelete(transaction)
        }

        dialog.setContentView(sheetBinding.root)
        dialog.behavior.apply {
            skipCollapsed = true
            state = BottomSheetBehavior.STATE_EXPANDED
        }
        dialog.show()
    }

    /**
     * Mở màn hình sửa giao dịch
     */
    private fun openEditTransaction(transaction: com.example.moneylover.data.model.TransactionEntity) {
        val state = viewModel.uiState.value
        val intent = Intent(this, AddTransactionActivity::class.java).apply {
            putExtra(AddTransactionActivity.EXTRA_TRANSACTION_ID, transaction.id)
            putExtra(AddTransactionActivity.EXTRA_SELECTED_YEAR, state.selectedYear)
            putExtra(AddTransactionActivity.EXTRA_SELECTED_MONTH, state.selectedMonth)
        }
        startActivityForResult(intent, REQUEST_ADD_TRANSACTION)
    }

    /**
     * Hiển thị popup menu khi long press
     */
    private fun showTransactionOptions(transaction: com.example.moneylover.data.model.TransactionEntity) {
        val popup = PopupMenu(this, binding.rv.findViewHolderForAdapterPosition(
            transactionAdapter.currentList.indexOf(transaction)
        )?.itemView ?: binding.rv)

        popup.menuInflater.inflate(R.menu.menu_transaction, popup.menu)
        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_detail -> {
                    showTransactionDetail(transaction)
                    true
                }
                R.id.action_edit -> {
                    openEditTransaction(transaction)
                    true
                }
                R.id.action_delete -> {
                    confirmDelete(transaction)
                    true
                }
                else -> false
            }
        }
        popup.show()
    }

    /**
     * Xác nhận xóa giao dịch
     */
    private fun confirmDelete(transaction: com.example.moneylover.data.model.TransactionEntity) {
        AlertDialog.Builder(this)
            .setTitle("Xóa giao dịch")
            .setMessage("Bạn có chắc muốn xóa giao dịch này không?")
            .setPositiveButton("Xóa") { _, _ ->
                viewModel.deleteTransaction(transaction)
            }
            .setNegativeButton("Hủy", null)
            .show()
    }

    /**
     * Hiển thị BottomSheet chọn tháng
     */
    @SuppressLint("SetTextI18n")
    private fun showMonthPickerSheet() {
        val dialog = BottomSheetDialog(this)
        val sheetBinding = BottomSheetMonthPickerBinding.inflate(layoutInflater)

        val state = viewModel.uiState.value
        var currentYear = state.selectedYear
        var currentMonth = state.selectedMonth

        sheetBinding.txtYear.text = currentYear.toString()
        sheetBinding.txtTitle.text = "Tháng $currentMonth năm $currentYear"

        fun renderMonths() {
            sheetBinding.monthGrid.removeAllViews()
            for (m in 1..12) {
                val btn = Button(this).apply {
                    text = "T$m"
                    isAllCaps = false
                    setBackgroundResource(
                        if (m == currentMonth) R.drawable.month_btn_selector
                        else android.R.color.transparent
                    )
                    val lp = GridLayout.LayoutParams().apply {
                        width = 0
                        height = ViewGroup.LayoutParams.WRAP_CONTENT
                        columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                        setGravity(Gravity.FILL)
                    }
                    layoutParams = lp
                    setOnClickListener {
                        currentMonth = m
                        sheetBinding.txtTitle.text = "Tháng $currentMonth năm $currentYear"
                        renderMonths()
                    }
                }
                sheetBinding.monthGrid.addView(btn)
            }
        }
        renderMonths()

        sheetBinding.btnPrevYear.setOnClickListener {
            currentYear -= 1
            sheetBinding.txtYear.text = currentYear.toString()
            sheetBinding.txtTitle.text = "Tháng $currentMonth năm $currentYear"
        }

        sheetBinding.btnNextYear.setOnClickListener {
            currentYear += 1
            sheetBinding.txtYear.text = currentYear.toString()
            sheetBinding.txtTitle.text = "Tháng $currentMonth năm $currentYear"
        }

        sheetBinding.btnOk.setOnClickListener {
            viewModel.setSelectedMonth(currentYear, currentMonth)
            dialog.dismiss()
        }

        sheetBinding.btnCancel.setOnClickListener { dialog.dismiss() }

        dialog.setContentView(sheetBinding.root)
        dialog.show()
    }

    /**
     * Hiển thị chi tiết tháng (Chi tiêu / Thu nhập / Số dư)
     */
    private fun showMonthlyDetailSheet(filterType: String) {
        val dialog = BottomSheetDialog(this)
        val sheetBinding = BottomSheetMonthlyDetailBinding.inflate(layoutInflater)
        
        // Đặt tiêu đề theo loại
        when (filterType) {
            "EXPENSE" -> {
                sheetBinding.txtTitle.text = "Chi tiêu"
                sheetBinding.txtTitle.setTextColor(getColor(R.color.black))
            }
            "INCOME" -> {
                sheetBinding.txtTitle.text = "Thu nhập"
                sheetBinding.txtTitle.setTextColor(getColor(R.color.black))
            }
            else -> {
                sheetBinding.txtTitle.text = "Số dư"
                sheetBinding.txtTitle.setTextColor(getColor(R.color.black))
            }
        }
        
        val state = viewModel.uiState.value
        val monthStatAdapter = createMonthStatAdapter(state.monthStats) { monthStat ->
            dialog.dismiss()
            viewModel.setSelectedMonth(monthStat.year, monthStat.month)
        }
        
        sheetBinding.rvMonths.layoutManager = LinearLayoutManager(this)
        sheetBinding.rvMonths.adapter = monthStatAdapter
        
        // Cập nhật tổng
        sheetBinding.txtTotalExpense.text = viewModel.formatCurrency(state.totalAllTimeExpense)
        sheetBinding.txtTotalIncome.text = viewModel.formatCurrency(state.totalAllTimeIncome)
        sheetBinding.txtTotalBalance.text = viewModel.formatCurrency(state.totalAllTimeBalance)
        
        val balanceColor = when {
            state.totalAllTimeBalance > 0 -> R.color.black
            state.totalAllTimeBalance < 0 -> R.color.black
            else -> R.color.black
        }
        sheetBinding.txtTotalBalance.setTextColor(getColor(balanceColor))
        
        sheetBinding.btnBack.setOnClickListener { dialog.dismiss() }
        
        dialog.setContentView(sheetBinding.root)
        dialog.behavior.state = BottomSheetBehavior.STATE_EXPANDED
        dialog.show()
    }

    /**
     * Hiển thị màn hình báo cáo
     */
    private fun showReportScreen() {
        binding.header.root.visibility = View.GONE
        binding.rv.visibility = View.GONE
        (binding.root.findViewById<View>(R.id.layout_empty_state))?.visibility = View.GONE
        binding.fabAdd.visibility = View.GONE
        
        // Ẩn monthlyStatsView nếu đang hiển thị
        monthlyStatsView?.visibility = View.GONE
        
        if (reportView == null) {
            reportView = layoutInflater.inflate(R.layout.layout_report, binding.container, true)
            setupReportTabs()
        }
        reportView?.visibility = View.VISIBLE
        
        // Cập nhật dữ liệu báo cáo
        updateReportData()
    }

    /**
     * Cập nhật dữ liệu cho màn hình báo cáo
     * Luôn tính lại từ cachedTransactions để đảm bảo dữ liệu đồng nhất với HOME
     */
    private fun updateReportData() {
        val state = viewModel.uiState.value
        val view = reportView ?: return
        
        // Tính lại từ cachedTransactions để đảm bảo đồng nhất với HOME
        val transactions = state.transactions
        val totalExpense = transactions.filter { it.type == "EXPENSE" }.sumOf { it.amount }
        val totalIncome = transactions.filter { it.type == "INCOME" }.sumOf { it.amount }
        val balance = totalIncome - totalExpense
        
        // === TAB PHÂN TÍCH ===
        // Thống kê tháng hiện tại - dùng giá trị đã tính lại
        view.findViewById<TextView>(R.id.txtMonthLabel)?.text = "Tháng ${state.selectedMonth}/${state.selectedYear}"
        view.findViewById<TextView>(R.id.txtSpending)?.apply {
            text = viewModel.formatCurrency(totalExpense)
            setTextColor(getColor(R.color.black))
        }
        view.findViewById<TextView>(R.id.txtIncome)?.apply {
            text = viewModel.formatCurrency(totalIncome)
            setTextColor(getColor(R.color.black))
        }
        view.findViewById<TextView>(R.id.txtBalance)?.apply {
            text = viewModel.formatCurrency(balance)
            setTextColor(getColor(R.color.black))
        }
        
        // Ngân sách tháng - đọc trực tiếp từ SharedPreferences
        val budget = budgetRepository.getMonthlyBudget()
        val remaining = budget - totalExpense
        
        // Cập nhật biểu đồ vòng tròn
        view.findViewById<com.example.moneylover.ui.custom.BudgetProgressView>(R.id.budgetProgressView)?.setBudgetData(budget, totalExpense)
        
        view.findViewById<TextView>(R.id.txtBudget)?.text = viewModel.formatCurrency(budget)
        view.findViewById<TextView>(R.id.txtSpendingBudget)?.text = viewModel.formatCurrency(totalExpense)
        
        // Còn lại
        view.findViewById<TextView>(R.id.txtRemain)?.apply {
            text = viewModel.formatCurrency(remaining)
            setTextColor(
                if (remaining < 0) getColor(R.color.black) else getColor(R.color.black)
            )
        }
        
        // === TAB TÀI KHOẢN ===
        view.findViewById<TextView>(R.id.txtNetAssets)?.apply {
            text = viewModel.formatCurrency(state.netAssets)
            setTextColor(
                if (state.netAssets >= 0) getColor(R.color.black) else getColor(R.color.black)
            )
        }
        view.findViewById<TextView>(R.id.txtAssets)?.text = viewModel.formatCurrency(state.totalAssets)
        view.findViewById<TextView>(R.id.txtLiabilities)?.text = viewModel.formatCurrency(state.totalLiabilities)
    }

    /**
     * Hiển thị dialog sửa ngân sách với định dạng số có dấu chấm
     */
    private fun showEditBudgetDialog() {
        val dialog = BottomSheetDialog(this)
        val sheetBinding = com.example.moneylover.databinding.BottomSheetCalculatorBinding.inflate(layoutInflater)
        
        val currentBudget = budgetRepository.getMonthlyBudget()
        
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
            return sheetBinding.txtCalculatorValue.text.toString().replace(".", "").replace(",", "").replace("đ", "")
        }
        
        sheetBinding.txtCalculatorValue.text = if (currentBudget > 0) {
            formatWithDots(currentBudget.toLong().toString())
        } else {
            "0"
        }
        
        val digits = listOf(
            R.id.btnCalc0 to "0", R.id.btnCalc1 to "1", R.id.btnCalc2 to "2", R.id.btnCalc3 to "3",
            R.id.btnCalc4 to "4", R.id.btnCalc5 to "5", R.id.btnCalc6 to "6", R.id.btnCalc7 to "7",
            R.id.btnCalc8 to "8", R.id.btnCalc9 to "9"
        )
        
        digits.forEach { (id, digit) ->
            sheetBinding.root.findViewById<Button>(id).setOnClickListener {
                val clean = getCleanNumber()
                if (clean == "0") {
                    sheetBinding.txtCalculatorValue.text = digit
                } else {
                    sheetBinding.txtCalculatorValue.text = formatWithDots(clean + digit)
                }
            }
        }
        
        // Nút xóa hết - reset về 0
        sheetBinding.btnCalcClear?.setOnClickListener {
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
            val value = getCleanNumber().toDoubleOrNull() ?: 0.0
            viewModel.setCurrentMonthBudget(value)
            dialog.dismiss()
            updateReportData()
        }
        
        dialog.setContentView(sheetBinding.root)
        dialog.show()
    }

    /**
     * Hiển thị màn hình chính
     */
    private fun showHomeScreen() {
        binding.header.root.visibility = View.VISIBLE
        binding.fabAdd.visibility = View.VISIBLE
        reportView?.visibility = View.GONE
        monthlyStatsView?.visibility = View.GONE
    }

    /**
     * Hiển thị màn hình Thống kê hàng tháng
     */
    private fun showMonthlyStatsScreen() {
        binding.header.root.visibility = View.GONE
        binding.rv.visibility = View.GONE
        (binding.root.findViewById<View>(R.id.layout_empty_state))?.visibility = View.GONE
        binding.fabAdd.visibility = View.GONE
        
        if (reportView != null) {
            reportView?.visibility = View.GONE
        }

        val container = findViewById<FrameLayout>(R.id.container)
        
        if (monthlyStatsView == null) {
            monthlyStatsBinding = LayoutMonthlyStatsBinding.inflate(layoutInflater)
            monthlyStatsView = monthlyStatsBinding?.root
            container.addView(monthlyStatsView)
            setupMonthlyStatsScreen()
        }
        
        monthlyStatsView?.visibility = View.VISIBLE
        updateMonthlyStatsData()
    }

    /**
     * Setup màn hình Thống kê hàng tháng
     */
    private fun setupMonthlyStatsScreen() {
        val binding = monthlyStatsBinding ?: return

        // Nút back - quay về báo cáo
        binding.btnBack.setOnClickListener {
            viewModel.setCurrentView(MainView.REPORT)
        }

        // Setup RecyclerView
        val adapter = MonthStatAdapter()
        binding.rvMonthStats.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(this)
        binding.rvMonthStats.adapter = adapter

        // Dropdown chọn năm
        binding.layoutYearSelector.setOnClickListener {
            showYearPickerDialog()
        }
    }

    /**
     * Cập nhật dữ liệu thống kê tháng
     */
    private fun updateMonthlyStatsData() {
        val binding = monthlyStatsBinding ?: return
        val state = viewModel.uiState.value

        // Update selected year text
        binding.txtSelectedYear.text = if (state.statsSelectedYear == 0) "Tất cả" else state.statsSelectedYear.toString()

        // Get month stats from ViewModel
        val allStats = viewModel.getAllMonthStats()
        val adapter = binding.rvMonthStats.adapter as? MonthStatAdapter
        adapter?.submitList(allStats)
        
        // Show/hide empty state
        if (allStats.isEmpty()) {
            binding.rvMonthStats.visibility = View.GONE
            binding.layoutEmptyState.visibility = View.VISIBLE
        } else {
            binding.rvMonthStats.visibility = View.VISIBLE
            binding.layoutEmptyState.visibility = View.GONE
        }
    }

    /**
     * Hiển thị dialog chọn năm dạng bottom sheet
     */
    private fun showYearPickerDialog() {
        val dialog = com.google.android.material.bottomsheet.BottomSheetDialog(this)
        val sheetBinding = BottomSheetYearPickerBinding.inflate(layoutInflater)
        
        val currentYear = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)
        val selectedYear = viewModel.uiState.value.statsSelectedYear
        
        // Tạo danh sách năm: Tất cả -> Năm hiện tại -> các năm trước
        val years = mutableListOf<YearPickerAdapter.YearItem>()
        years.add(YearPickerAdapter.YearItem(0, isAll = true)) // Tất cả
        for (year in currentYear downTo (currentYear - 10)) {
            years.add(YearPickerAdapter.YearItem(year))
        }
        
        val adapter = YearPickerAdapter(years, selectedYear) { year ->
            viewModel.setSelectedYearForStats(year)
            updateMonthlyStatsData()
            dialog.dismiss()
        }
        
        sheetBinding.rvYears.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(this)
        sheetBinding.rvYears.adapter = adapter
        
        dialog.setContentView(sheetBinding.root)
        dialog.show()
    }

    /**
     * Setup tabs cho màn hình báo cáo
     */
    private fun setupReportTabs() {
        val view = reportView ?: return
        val tabAnalysis = view.findViewById<TextView>(R.id.tabAnalysis)
        val tabAccount = view.findViewById<TextView>(R.id.tabAccount)
        val flipper = view.findViewById<ViewFlipper>(R.id.reportFlipper)

        // Nút xem chi tiết ngân sách - cả hàng có thể bấm
        view.findViewById<View>(R.id.layoutMonthlyBudget)?.setOnClickListener {
            startActivity(Intent(this, BudgetActivity::class.java))
        }

        // Mũi tên Thống kê hàng tháng - mở MonthlyStatsActivity riêng biệt
        view.findViewById<View>(R.id.layoutMonthlyStats)?.setOnClickListener {
            startActivity(Intent(this, MonthlyStatsActivity::class.java))
        }

        // Tab Phân tích
        tabAnalysis?.setOnClickListener {
            flipper?.displayedChild = 0
            tabAnalysis.setBackgroundResource(R.drawable.bg_segment_selected)
            tabAnalysis.setTextColor(getColor(R.color.black))
            tabAccount?.setBackgroundResource(android.R.color.transparent)
            tabAccount?.setTextColor(getColor(R.color.white))
            updateReportData()
        }

        // Tab Tài khoản
        tabAccount?.setOnClickListener {
            flipper?.displayedChild = 1
            tabAccount.setBackgroundResource(R.drawable.bg_segment_selected)
            tabAccount.setTextColor(getColor(R.color.black))
            tabAnalysis?.setBackgroundResource(android.R.color.transparent)
            tabAnalysis?.setTextColor(getColor(R.color.white))
            updateReportData()
        }

        // Nút quản lý tài khoản
        view.findViewById<View>(R.id.btnManageAccount)?.setOnClickListener {
            startActivity(Intent(this, AccountActivity::class.java))
        }

        // Nút thêm tài khoản
        view.findViewById<View>(R.id.btnAddAccount)?.setOnClickListener {
            startActivity(Intent(this, AccountActivity::class.java))
        }
    }
}
