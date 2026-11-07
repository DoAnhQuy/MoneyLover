package com.example.moneylover

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.GridLayout
import android.view.Gravity
import android.graphics.Color
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.example.moneylover.CategoryBudgetAdapter.Companion.formatCurrency
import com.example.moneylover.data.database.AppDatabase
import com.example.moneylover.data.repository.BudgetRepository
import com.example.moneylover.databinding.ActivityBudgetBinding
import com.example.moneylover.databinding.ItemBudgetCategoryChartBinding
import com.example.moneylover.databinding.ItemBudgetMonthlyBinding
import androidx.appcompat.app.AlertDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar

/**
 * Data class cho danh mục ngân sách
 */
data class BudgetCategoryItem(
    val id: Int,
    val name: String,
    val iconRes: Int,
    var budgetAmount: Double = 0.0,
    var isSkipped: Boolean = false
)

class BudgetActivity : AppCompatActivity() {

    private lateinit var binding: ActivityBudgetBinding
    private lateinit var database: AppDatabase
    private lateinit var budgetRepository: BudgetRepository
    private lateinit var categoryAdapter: CategoryBudgetAdapter
    private var selectedMonth: Int = 0
    private var selectedYear: Int = 0

    // Job để hủy coroutine cũ khi onResume được gọi nhiều lần
    private var loadCategorySpendingJob: Job? = null

    // Danh sách các danh mục ngân sách đã cài đặt
    private val budgetCategoryItems = mutableListOf<BudgetCategoryItem>()

    // Map lưu chi tiêu theo category ID
    private val categorySpendingMap = mutableMapOf<Int, Double>()

    // Biến lưu ngân sách hàng tháng
    private var monthlyBudget: Double = 0.0

    // Tổng chi tiêu TẤT CẢ giao dịch trong tháng (dùng cho card Ngân sách hàng tháng)
    private var totalMonthlyExpense: Double = 0.0

    private val budgetCategories = mutableListOf(
        BudgetCategoryItem(1, "Mua sắm", R.drawable.ic_shopping, 0.0),
        BudgetCategoryItem(2, "Đồ ăn", R.drawable.ic_food, 0.0),
        BudgetCategoryItem(3, "Điện thoại", R.drawable.ic_phone, 0.0),
        BudgetCategoryItem(4, "Giải trí", R.drawable.ic_entertainment, 0.0),
        BudgetCategoryItem(5, "Giáo dục", R.drawable.ic_education, 0.0),
        BudgetCategoryItem(6, "Sắc đẹp", R.drawable.ic_beauty, 0.0),
        BudgetCategoryItem(7, "Thể thao", R.drawable.ic_sports, 0.0),
        BudgetCategoryItem(8, "Di chuyển", R.drawable.ic_transport, 0.0),
        BudgetCategoryItem(9, "Nhà hàng", R.drawable.ic_restaurant, 0.0),
        BudgetCategoryItem(10, "Du lịch", R.drawable.ic_travel, 0.0),
        BudgetCategoryItem(11, "Y tế", R.drawable.ic_medical, 0.0),
        BudgetCategoryItem(12, "Khác", R.drawable.ic_other, 0.0)
    )

    // Activity Result Launcher để nhận kết quả từ BudgetSettingsActivity
    private val budgetSettingsLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.let { data ->
                // Nhận và lưu ngân sách hàng tháng
                val newMonthlyBudget = data.getDoubleExtra(BudgetSettingsActivity.EXTRA_MONTHLY_BUDGET, 0.0)
                budgetRepository.setMonthlyBudget(newMonthlyBudget)
                monthlyBudget = newMonthlyBudget
                categoryAdapter.updateMonthlyBudget(newMonthlyBudget)
                
                // Nhận và lưu ngân sách các danh mục
                data.getStringExtra(BudgetSettingsActivity.EXTRA_CATEGORIES_DATA)?.let { categoryData ->
                    val parsedCategories = parseCategories(categoryData)
                    // Lưu tất cả (kể cả budget=0 để xóa)
                    budgetRepository.saveAllCategoryBudgets(parsedCategories)
                    // Chỉ hiển thị những cái có budget > 0
                    budgetCategoryItems.clear()
                    budgetCategoryItems.addAll(parsedCategories.filter { it.budgetAmount > 0 })
                }
                
                // Cập nhật UI
                categoryAdapter.notifyDataSetChanged()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBudgetBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Khởi tạo database và repository
        database = AppDatabase.getDatabase(this)
        budgetRepository = BudgetRepository(this)

        val cal = Calendar.getInstance()
        if (selectedMonth == 0) {
            selectedMonth = cal.get(Calendar.MONTH) + 1
            selectedYear = cal.get(Calendar.YEAR)
        }

        setupRecyclerView()
        monthlyBudget = budgetRepository.getMonthlyBudget()
        loadSavedBudgets()
        updateTimeLabel()
        loadCategorySpending()

        binding.btnBack.setOnClickListener { finish() }

        binding.btnTimePeriod.setOnClickListener {
            showMonthYearPicker { month, year ->
                selectedMonth = month
                selectedYear = year
                updateTimeLabel()
                loadCategorySpending()
            }
        }

        binding.btnSetupBudget.setOnClickListener {
            val intent = Intent(this, BudgetSettingsActivity::class.java)
            budgetSettingsLauncher.launch(intent)
        }
    }

    private fun setupRecyclerView() {
        categoryAdapter = CategoryBudgetAdapter(
            items = budgetCategoryItems,
            spendingMap = categorySpendingMap,
            monthlyBudget = monthlyBudget,
            totalExpense = totalMonthlyExpense,
            onMonthlyItemClick = {
                showCalculatorSheet("0") { value ->
                    val newBudget = value.toDoubleOrNull() ?: 0.0
                    val totalCategoryBudget = budgetCategoryItems.sumOf { it.budgetAmount }

                    if (newBudget < totalCategoryBudget && totalCategoryBudget > 0) {
                        android.widget.Toast.makeText(
                            this,
                            "Tổng ngân sách không thể nhỏ hơn tổng ngân sách của các danh mục (${formatCurrency(totalCategoryBudget)})",
                            android.widget.Toast.LENGTH_LONG
                        ).show()
                        return@showCalculatorSheet
                    }

                    monthlyBudget = newBudget
                    budgetRepository.setMonthlyBudget(newBudget)
                    categoryAdapter.updateMonthlyBudget(newBudget)
                }
            },
            onEditCategoryClick = { position ->
                val item = budgetCategoryItems[position]
                showCalculatorSheet("0") { value ->
                    val newAmount = value.toDoubleOrNull() ?: 0.0
                    budgetRepository.saveCategoryBudget(item.id, newAmount)
                    
                    if (newAmount <= 0) {
                        // Ẩn danh mục khỏi danh sách hiển thị
                        budgetCategoryItems.removeAt(position)
                    } else {
                        budgetCategoryItems[position] = item.copy(budgetAmount = newAmount, isSkipped = false)
                    }
                    categoryAdapter.notifyDataSetChanged()
                }
            },
            onDeleteClick = { position ->
                showDeleteCategoryDialog(position)
            }
        )

        binding.rvCategoryCards.apply {
            layoutManager = androidx.recyclerview.widget.LinearLayoutManager(this@BudgetActivity)
            adapter = categoryAdapter
        }
    }

    /**
     * Load ngân sách đã lưu từ repository
     */
    private fun loadSavedBudgets() {
        val savedBudgets = budgetRepository.getAllCategoryBudgets()
        val skippedCategories = budgetRepository.getSkippedCategories()
        
        // Tạo bản sao để tránh ConcurrentModificationException
        val currentItems = budgetCategoryItems.toList()
        
        savedBudgets.forEach { (categoryId, amount) ->
            // Chỉ thêm nếu chưa có trong danh sách
            if (currentItems.any { it.id == categoryId }) return@forEach
            
            val category = budgetCategories.find { it.id == categoryId }
            if (category != null && amount > 0) {
                val isSkipped = skippedCategories.contains(categoryId)
                budgetCategoryItems.add(category.copy(budgetAmount = amount, isSkipped = isSkipped))
            }
        }
    }

    /**
     * Load chi tiêu theo danh mục từ database
     */
    @SuppressLint("NotifyDataSetChanged")
    private fun loadCategorySpending() {
        // Hủy coroutine cũ nếu đang chạy để tránh ConcurrentModificationException
        loadCategorySpendingJob?.cancel()
        
        // Đảm bảo monthlyBudget được cập nhật từ repository
        monthlyBudget = budgetRepository.getMonthlyBudget()
        
        loadCategorySpendingJob = lifecycleScope.launch {
            val yearStr = selectedYear.toString()
            val monthStr = String.format("%02d", selectedMonth)
            
            categorySpendingMap.clear()
            
            // Tạo bản sao của danh sách để tránh lỗi khi danh sách thay đổi
            val categoryIds = budgetCategoryItems.map { it.id }
            
            withContext(Dispatchers.IO) {
                categoryIds.forEach { categoryId ->
                    val spending = database.transactionDao()
                        .getExpenseSumByCategoryAndMonth(categoryId, yearStr, monthStr)
                    categorySpendingMap[categoryId] = spending
                }
                
                // Lấy tổng TẤT CẢ chi tiêu trong tháng (dùng cho card Ngân sách hàng tháng)
                totalMonthlyExpense = database.transactionDao()
                    .getTotalExpenseByMonth(yearStr, monthStr)
            }
            
            // Cập nhật UI với dữ liệu mới
            categoryAdapter.updateMonthlyBudget(monthlyBudget)
            categoryAdapter.updateTotalExpense(totalMonthlyExpense)
            categoryAdapter.notifyDataSetChanged()
            updateAllDisplays()
        }
    }

    /**
     * Cập nhật tất cả hiển thị
     */
    private fun updateAllDisplays() {
        categoryAdapter.notifyDataSetChanged()
    }

    /**
     * Parse chuỗi categories từ BudgetSettingsActivity
     */
    private fun parseCategories(data: String): List<BudgetCategoryItem> {
        return data.split(";").mapNotNull { item ->
            val parts = item.split("|")
            if (parts.size >= 4) {
                BudgetCategoryItem(
                    id = parts[0].toIntOrNull() ?: return@mapNotNull null,
                    name = parts[1],
                    iconRes = parts[2].toIntOrNull() ?: return@mapNotNull null,
                    budgetAmount = parts[3].toDoubleOrNull() ?: 0.0
                )
            } else null
        }
    }

    override fun onResume() {
        super.onResume()
        // Cập nhật lại monthlyBudget từ repository
        monthlyBudget = budgetRepository.getMonthlyBudget()
        categoryAdapter.updateMonthlyBudget(monthlyBudget)
        
        // Load lại danh sách từ repository
        loadSavedBudgets()
        loadCategorySpending()
    }

    private fun showDeleteCategoryDialog(position: Int) {
        val item = budgetCategoryItems[position]
        // Chỉ đặt số tiền về 0 và ẩn danh mục
        budgetRepository.saveCategoryBudget(item.id, 0.0)
        budgetCategoryItems.removeAt(position)
        categoryAdapter.notifyDataSetChanged()
        updateAllDisplays()
    }

    private fun updateTimeLabel() {
        binding.txtTimePeriod.text = "thg $selectedMonth $selectedYear"
    }

    private fun currentBudgetValue(): String {
        val savedBudget = budgetRepository.getMonthlyBudget()
        return if (savedBudget > 0) {
            savedBudget.toLong().toString()
        } else {
            ""
        }
    }

    private fun showCalculatorSheet(initialValue: String, onDone: (String) -> Unit) {
        val dialog = AlertDialog.Builder(this)
            .create()
        val sheetBinding = com.example.moneylover.databinding.BottomSheetCalculatorBinding.inflate(layoutInflater)
        dialog.setView(sheetBinding.root)
        
        // Hàm format số với dấu chấm phân cách
        fun formatWithDots(numStr: String): String {
            if (numStr.isEmpty()) return "0"
            val clean = numStr.replace(".", "").replace(",", "").replace("đ", "")
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
        
        // Parse giá trị ban đầu
        val cleanInitial = initialValue.replace(".", "").replace(",", "").replace("đ", "")
        sheetBinding.txtCalculatorValue.text = if (cleanInitial.isEmpty() || cleanInitial == "0") "0" else formatWithDots(cleanInitial)

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
            onDone(getCleanNumber())
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun showMonthYearPicker(onPicked: (month: Int, year: Int) -> Unit) {
        val dialog = AlertDialog.Builder(this)
            .create()
        val sheetBinding = com.example.moneylover.databinding.BottomSheetMonthPickerBinding.inflate(layoutInflater)
        dialog.setView(sheetBinding.root)

        val txtTitle  = sheetBinding.txtTitle
        val txtYear   = sheetBinding.txtYear
        val btnPrev   = sheetBinding.btnPrevYear
        val btnNext   = sheetBinding.btnNextYear
        val monthGrid = sheetBinding.monthGrid

        var currentYear  = selectedYear
        var currentMonth = selectedMonth

        txtYear.text = currentYear.toString()

        fun updateTitle() {
            txtTitle.text = "Tháng $currentMonth năm $currentYear"
        }
        updateTitle()

        fun renderMonths() {
            monthGrid.removeAllViews()
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
                        updateTitle()
                        renderMonths()
                    }
                }
                monthGrid.addView(btn)
            }
        }
        renderMonths()

        btnPrev.setOnClickListener {
            currentYear -= 1
            txtYear.text = currentYear.toString()
            updateTitle()
        }

        btnNext.setOnClickListener {
            currentYear += 1
            txtYear.text = currentYear.toString()
            updateTitle()
        }

        sheetBinding.btnOk.setOnClickListener {
            onPicked(currentMonth, currentYear)
            dialog.dismiss()
        }

        sheetBinding.btnCancel.setOnClickListener { dialog.dismiss() }

        dialog.show()
    }
}

/**
 * Adapter cho danh sách ngân sách (gồm: Ngân sách hàng tháng + danh mục)
 */
class CategoryBudgetAdapter(
    private val items: MutableList<BudgetCategoryItem>,
    private val spendingMap: Map<Int, Double>,
    private var monthlyBudget: Double,
    private var totalExpense: Double, // Tổng TẤT CẢ chi tiêu trong tháng (dùng cho card Ngân sách hàng tháng)
    private val onMonthlyItemClick: () -> Unit,
    private val onEditCategoryClick: (Int) -> Unit,
    private val onDeleteClick: (Int) -> Unit
) : RecyclerView.Adapter<CategoryBudgetAdapter.VH>() {

    companion object {
        const val VIEW_TYPE_MONTHLY = 0
        const val VIEW_TYPE_CATEGORY = 1

        fun formatCurrency(amount: Double): String {
            val absAmount = kotlin.math.abs(amount)
            return if (absAmount > 0) {
                String.format("%,.0f", absAmount).replace(",", ".") + "đ"
            } else {
                "0đ"
            }
        }
    }

    override fun getItemViewType(position: Int): Int {
        return if (position == 0) VIEW_TYPE_MONTHLY else VIEW_TYPE_CATEGORY
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        return when (viewType) {
            VIEW_TYPE_MONTHLY -> {
                val binding = ItemBudgetMonthlyBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                val holder = VHMonthly(binding)
                // Set click trên toàn bộ CardView
                binding.root.setOnClickListener {
                    onMonthlyItemClick()
                }
                holder
            }
            else -> {
                val binding = ItemBudgetCategoryChartBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                VHCategory(binding, onEditCategoryClick, onDeleteClick)
            }
        }
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        when (holder) {
            is VHMonthly -> {
                if (position == 0) {
                    holder.bind(monthlyBudget, totalExpense)
                }
            }
            is VHCategory -> {
                val categoryPosition = position - 1
                if (categoryPosition >= 0 && categoryPosition < items.size) {
                    holder.bind(items[categoryPosition], spendingMap)
                }
            }
        }
    }

    override fun getItemCount(): Int = items.size + 1

    fun updateMonthlyBudget(budget: Double) {
        monthlyBudget = budget
        notifyItemChanged(0)
    }

    fun updateTotalExpense(expense: Double) {
        totalExpense = expense
        notifyItemChanged(0)
    }

    sealed class VH(binding: View) : RecyclerView.ViewHolder(binding)

    class VHMonthly(private val binding: ItemBudgetMonthlyBinding) : VH(binding.root) {
        fun bind(monthlyBudget: Double, totalExpense: Double) {
            // Dùng totalExpense (tổng TẤT CẢ chi tiêu) thay vì spendingMap.values.sum()
            binding.circularView.setBudgetData(monthlyBudget, totalExpense)

            binding.txtCategoryName.text = "Ngân sách hàng tháng"
            binding.txtBudget.text = formatCurrency(monthlyBudget)
            binding.txtSpending.text = formatCurrency(totalExpense)
            
            val remaining = monthlyBudget - totalExpense
            binding.txtRemain.text = if (remaining >= 0) formatCurrency(remaining) else "-" + formatCurrency(-remaining)
        }
    }

    class VHCategory(
        private val binding: ItemBudgetCategoryChartBinding,
        private val onEditClick: (Int) -> Unit,
        @Suppress("UNUSED_PARAMETER") private val onDeleteClick: (Int) -> Unit
    ) : VH(binding.root) {

        fun bind(item: BudgetCategoryItem, spendingMap: Map<Int, Double>) {
            // ===== LUÔN HIỂN THỊ NHƯ "GIẢI TRÍ" =====
            binding.layoutEmpty.visibility = android.view.View.GONE
            binding.layoutFilled.visibility = android.view.View.VISIBLE

            binding.txtCategoryName.text = item.name
            
            // Tính position an toàn
            val actualPosition = if (bindingAdapterPosition != RecyclerView.NO_POSITION && bindingAdapterPosition > 0) {
                bindingAdapterPosition - 1
            } else {
                return // Không bind nếu position không hợp lệ
            }
            
            binding.btnEdit.setOnClickListener { 
                onEditClick(actualPosition)
            }

            val spending = spendingMap[item.id] ?: 0.0
            binding.txtSpending.text = formatCurrency(spending)
            binding.txtBudget.text = formatCurrency(item.budgetAmount)
            
            val remain = item.budgetAmount - spending
            if (remain >= 0) {
                binding.txtRemain.text = formatCurrency(remain)
            } else {
                binding.txtRemain.text = "-" + formatCurrency(-remain)
            }

            // Dùng setBudgetData để hiển thị % CÒN LẠI trong vòng tròn
            binding.circularView.setBudgetData(item.budgetAmount, spending)
        }
    }
}

