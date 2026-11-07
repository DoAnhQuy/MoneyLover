package com.example.moneylover

import android.content.Intent
import android.icu.text.NumberFormat
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.GridLayout
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.moneylover.databinding.ActivityCalendarBinding
import com.example.moneylover.databinding.BottomSheetDayDetailBinding
import com.example.moneylover.databinding.BottomSheetDayTransactionsBinding
import com.example.moneylover.databinding.BottomSheetMonthlyDetailBinding
import com.example.moneylover.databinding.BottomSheetMonthPickerBinding
import com.example.moneylover.databinding.BottomSheetTransactionDetailBinding
import com.example.moneylover.data.model.TransactionEntity
import com.example.moneylover.ui.adapter.CalendarDayAdapter
import com.example.moneylover.ui.adapter.MonthStatAdapter
import com.example.moneylover.ui.adapter.createMonthStatAdapter
import com.example.moneylover.ui.adapter.TransactionAdapter
import com.example.moneylover.ui.state.CalendarDayUi
import com.example.moneylover.ui.state.CalendarUiState
import com.example.moneylover.ui.viewmodel.CalendarViewModel
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * CalendarActivity - Màn hình lịch
 * 
 * Trong MVVM:
 * - View chỉ lo hiển thị UI và gọi ViewModel methods
 * - Observe UiState để cập nhật UI tự động
 */
class CalendarActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCalendarBinding
    private lateinit var calendarAdapter: CalendarDayAdapter

    private val viewModel: CalendarViewModel by viewModels()

    private val currencyFormatter = NumberFormat.getNumberInstance(Locale("vi", "VN"))
    private val dateFormatter = SimpleDateFormat("dd/MM/yyyy", Locale("vi", "VN"))
    private val timeFormatter = SimpleDateFormat("HH:mm", Locale.getDefault())
    
    private val REQUEST_EDIT_TRANSACTION = 1002

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCalendarBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.toolbar.setNavigationOnClickListener { finish() }

        setupCalendar()
        setupClickListeners()
        observeUiState()

        viewModel.loadTransactionsForMonth()
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
    private fun renderUi(state: CalendarUiState) {
        binding.txtMonthTitle.text = viewModel.getMonthTitle()
        calendarAdapter.updateItems(state.calendarDays, state.selectedDay)
    }

    /**
     * Setup calendar adapter
     */
    private fun setupCalendar() {
        calendarAdapter = CalendarDayAdapter(
            items = emptyList(),
            selectedDay = 1,
            onDayClick = { dayItem ->
                if (dayItem.inMonth && dayItem.day > 0) {
                    viewModel.selectDay(dayItem.day)
                    showDayTransactions(dayItem.day)
                }
            },
            onDayLongClick = { dayItem ->
                if (dayItem.inMonth && dayItem.day > 0) {
                    viewModel.selectDay(dayItem.day)
                    showDayTransactions(dayItem.day)
                }
            }
        )
        
        binding.rcCalendar.layoutManager = GridLayoutManager(this, 7)
        binding.rcCalendar.adapter = calendarAdapter
    }

    /**
     * Setup click listeners
     */
    private fun setupClickListeners() {
        binding.imgMonthDropdown.setOnClickListener {
            showMonthPickerLocal()
        }
        
        binding.fabAddCalendar.setOnClickListener {
            val state = viewModel.uiState.value
            val intent = Intent(this, AddTransactionActivity::class.java).apply {
                putExtra(AddTransactionActivity.EXTRA_SELECTED_YEAR, state.selectedYear)
                putExtra(AddTransactionActivity.EXTRA_SELECTED_MONTH, state.selectedMonth)
                putExtra(AddTransactionActivity.EXTRA_SELECTED_DAY, state.selectedDay)
            }
            startActivity(intent)
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.loadTransactionsForMonth()
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_EDIT_TRANSACTION && resultCode == RESULT_OK) {
            viewModel.loadTransactionsForMonth()
        }
    }

    /**
     * Hiển thị BottomSheet giao dịch trong ngày
     */
    private fun showDayTransactions(day: Int) {
        val dialog = BottomSheetDialog(this)
        val sheetBinding = BottomSheetDayTransactionsBinding.inflate(layoutInflater)
        
        val state = viewModel.uiState.value
        val dayStr = if (day < 10) "0$day" else "$day"
        val monthStr = if (state.selectedMonth < 10) "0${state.selectedMonth}" else "${state.selectedMonth}"
        sheetBinding.txtTitle.text = "Ngày $dayStr/$monthStr/${state.selectedYear}"
        
        val transactionAdapter = TransactionAdapter(
            onItemClick = { transaction ->
                dialog.dismiss()
                showTransactionDetail(transaction)
            },
            onItemLongClick = { transaction ->
                showTransactionOptions(transaction)
            }
        )
        
        sheetBinding.rvTransactions.layoutManager = LinearLayoutManager(this)
        sheetBinding.rvTransactions.adapter = transactionAdapter
        
        // Observe giao dịch ngày
        lifecycleScope.launch {
            viewModel.uiState.collectLatest { currentState ->
                if (currentState.selectedDay == day) {
                    val transactions = currentState.dayTransactions
                    if (transactions.isEmpty()) {
                        sheetBinding.txtEmptyState.visibility = View.VISIBLE
                        sheetBinding.rvTransactions.visibility = View.GONE
                        sheetBinding.txtTotalExpense.text = "0đ"
                        sheetBinding.txtTotalIncome.text = "0đ"
                        sheetBinding.txtBalance.text = "0đ"
                    } else {
                        sheetBinding.txtEmptyState.visibility = View.GONE
                        sheetBinding.rvTransactions.visibility = View.VISIBLE
                        transactionAdapter.submitList(transactions)
                        
                        sheetBinding.txtTotalExpense.text = viewModel.formatCurrency(currentState.dayTotalExpense)
                        sheetBinding.txtTotalIncome.text = viewModel.formatCurrency(currentState.dayTotalIncome)
                        sheetBinding.txtBalance.text = viewModel.formatCurrency(currentState.dayBalance)
                    }
                }
            }
        }
        
        sheetBinding.btnClose.setOnClickListener { dialog.dismiss() }
        
        sheetBinding.layoutExpense.setOnClickListener {
            dialog.dismiss()
            showMonthlyDetailSheet("EXPENSE")
        }
        
        sheetBinding.layoutIncome.setOnClickListener {
            dialog.dismiss()
            showMonthlyDetailSheet("INCOME")
        }
        
        sheetBinding.layoutBalance.setOnClickListener {
            dialog.dismiss()
            showMonthlyDetailSheet("ALL")
        }
        
        dialog.setContentView(sheetBinding.root)
        dialog.behavior.state = BottomSheetBehavior.STATE_EXPANDED
        dialog.show()
    }
    
    /**
     * Hiển thị chi tiết giao dịch
     */
    private fun showTransactionDetail(transaction: TransactionEntity) {
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
        
        // Hiển thị tài khoản
        val accountName = transaction.fromAccount ?: transaction.toAccount
        if (!accountName.isNullOrBlank()) {
            sheetBinding.layoutAccount.visibility = View.VISIBLE
            sheetBinding.txtAccount.text = accountName
        } else {
            sheetBinding.layoutAccount.visibility = View.GONE
        }
        
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
    private fun openEditTransaction(transaction: TransactionEntity) {
        val state = viewModel.uiState.value
        val intent = Intent(this, AddTransactionActivity::class.java).apply {
            putExtra(AddTransactionActivity.EXTRA_TRANSACTION_ID, transaction.id)
            putExtra(AddTransactionActivity.EXTRA_SELECTED_YEAR, state.selectedYear)
            putExtra(AddTransactionActivity.EXTRA_SELECTED_MONTH, state.selectedMonth)
        }
        startActivityForResult(intent, REQUEST_EDIT_TRANSACTION)
    }
    
    /**
     * Hiển thị popup menu khi long press
     */
    private fun showTransactionOptions(transaction: TransactionEntity) {
        android.widget.PopupMenu(this, binding.root).apply {
            menuInflater.inflate(R.menu.menu_transaction, menu)
            setOnMenuItemClickListener { item ->
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
            show()
        }
    }
    
    /**
     * Xác nhận xóa giao dịch
     */
    private fun confirmDelete(transaction: TransactionEntity) {
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
    private fun showMonthPickerLocal() {
        val dialog = BottomSheetDialog(this)
        val sheetBinding = BottomSheetMonthPickerBinding.inflate(layoutInflater)

        val state = viewModel.uiState.value
        var currentYear = state.selectedYear
        var currentMonth = state.selectedMonth

        sheetBinding.txtYear.text = currentYear.toString()
        sheetBinding.txtTitle.text = "Tháng $currentMonth năm $currentYear"

        fun renderMonthsLocal() {
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
                        renderMonthsLocal()
                    }
                }
                sheetBinding.monthGrid.addView(btn)
            }
        }
        renderMonthsLocal()

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
                sheetBinding.txtTitle.text = "Chi tiết"
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
}
