package com.example.moneylover

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.moneylover.data.database.AppDatabase
import com.example.moneylover.data.repository.BudgetRepository
import com.example.moneylover.databinding.ActivityMonthlyStatsBinding
import com.example.moneylover.databinding.BottomSheetYearPickerBinding
import com.example.moneylover.ui.adapter.MonthStatAdapter
import com.example.moneylover.ui.adapter.YearPickerAdapter
import com.example.moneylover.ui.state.MonthStatUi
import com.google.android.material.bottomsheet.BottomSheetDialog
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MonthlyStatsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMonthlyStatsBinding
    private lateinit var database: AppDatabase
    private lateinit var budgetRepository: BudgetRepository
    private lateinit var adapter: MonthStatAdapter
    private var selectedYear: Int = 0 // 0 = Tất cả

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMonthlyStatsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        database = AppDatabase.getDatabase(this)
        budgetRepository = BudgetRepository(this)

        setupUI()
        loadData()
    }

    private fun setupUI() {
        // Nút Back - quay về MainActivity
        binding.btnBack.setOnClickListener {
            finish()
        }

        // Dropdown chọn năm
        binding.layoutYearSelector.setOnClickListener {
            showYearPickerDialog()
        }

        // Setup RecyclerView
        adapter = MonthStatAdapter()
        binding.rvMonthStats.layoutManager = LinearLayoutManager(this)
        binding.rvMonthStats.adapter = adapter

        // Update text hiển thị
        updateYearDisplay()
    }

    private fun updateYearDisplay() {
        binding.txtSelectedYear.text = if (selectedYear == 0) "Tất cả" else selectedYear.toString()
    }

    private fun loadData() {
        lifecycleScope.launch {
            val stats = withContext(kotlinx.coroutines.Dispatchers.IO) {
                getMonthStats()
            }
            
            // Update UI
            if (stats.isEmpty()) {
                binding.rvMonthStats.visibility = View.GONE
                binding.layoutEmptyState.visibility = View.VISIBLE
            } else {
                binding.rvMonthStats.visibility = View.VISIBLE
                binding.layoutEmptyState.visibility = View.GONE
                adapter.submitList(stats)
            }
        }
    }

    private suspend fun getMonthStats(): List<MonthStatUi> {
        val transactions = database.transactionDao().getAllTransactionsList()
        
        // Nhóm theo tháng/năm
        val monthGroups = if (selectedYear == 0) {
            transactions.groupBy { tx ->
                val calendar = java.util.Calendar.getInstance()
                calendar.timeInMillis = tx.date
                Pair(calendar.get(java.util.Calendar.MONTH) + 1, calendar.get(java.util.Calendar.YEAR))
            }
        } else {
            transactions.filter { tx ->
                val calendar = java.util.Calendar.getInstance()
                calendar.timeInMillis = tx.date
                calendar.get(java.util.Calendar.YEAR) == selectedYear
            }.groupBy { tx ->
                val calendar = java.util.Calendar.getInstance()
                calendar.timeInMillis = tx.date
                Pair(calendar.get(java.util.Calendar.MONTH) + 1, calendar.get(java.util.Calendar.YEAR))
            }
        }

        return monthGroups.map { (monthYear, txs) ->
            val (month, year) = monthYear
            val expense = txs.filter { it.type == "EXPENSE" }.sumOf { it.amount }
            val income = txs.filter { it.type == "INCOME" }.sumOf { it.amount }
            MonthStatUi(
                month = month,
                year = year,
                totalExpense = expense,
                totalIncome = income,
                budget = budgetRepository.getMonthlyBudget()
            )
        }.sortedByDescending { it.year * 100 + it.month }
    }

    private fun showYearPickerDialog() {
        val dialog = BottomSheetDialog(this)
        val sheetBinding = BottomSheetYearPickerBinding.inflate(layoutInflater)
        
        val currentYear = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)
        val years = mutableListOf<YearPickerAdapter.YearItem>()
        years.add(YearPickerAdapter.YearItem(0, true)) // Tất cả
        for (y in currentYear downTo currentYear - 10) {
            years.add(YearPickerAdapter.YearItem(y, false))
        }
        
        val adapter = YearPickerAdapter(years, selectedYear) { year ->
            selectedYear = year
            updateYearDisplay()
            loadData()
            dialog.dismiss()
        }
        
        sheetBinding.rvYears.layoutManager = LinearLayoutManager(this)
        sheetBinding.rvYears.adapter = adapter
        
        dialog.setContentView(sheetBinding.root)
        dialog.show()
    }
}
