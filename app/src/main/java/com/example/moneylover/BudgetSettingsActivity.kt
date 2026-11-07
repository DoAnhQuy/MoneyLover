package com.example.moneylover

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.moneylover.data.repository.BudgetRepository
import com.example.moneylover.databinding.ActivityBudgetSettingsBinding
import com.example.moneylover.databinding.ItemBudgetCategoryBinding
import com.google.android.material.bottomsheet.BottomSheetDialog
import androidx.appcompat.app.AlertDialog

/**
 * Màn Cài đặt ngân sách - Hiển thị danh sách các danh mục với số tiền ngân sách
 */
class BudgetSettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityBudgetSettingsBinding
    private lateinit var budgetRepository: BudgetRepository

    // Danh sách các danh mục ngân sách (sẽ được trả về BudgetActivity)
    private val _categories = mutableListOf(
        BudgetCategoryItem(1, "Mua sắm", R.drawable.ic_shopping, 0.0),
        BudgetCategoryItem(2, "Đồ ăn", R.drawable.ic_food, 0.0),
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

    private lateinit var adapter: BudgetCategoryAdapter
    private var monthlyBudget: Double = 0.0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBudgetSettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Khởi tạo repository
        budgetRepository = BudgetRepository(this)

        // Load ngân sách đã lưu
        loadSavedBudgets()
        monthlyBudget = budgetRepository.getMonthlyBudget()

        binding.btnBack.setOnClickListener {
            returnResults()
        }

        adapter = BudgetCategoryAdapter(_categories, monthlyBudget)

        binding.rvCategories.layoutManager = LinearLayoutManager(this)
        binding.rvCategories.adapter = adapter

        // Sửa ngân sách hàng tháng
        adapter.setOnEditMonthlyClickListener {
            if (monthlyBudget > 0) {
                // Đã có tiền → hiện dialog chọn Sửa hoặc Xóa
                AlertDialog.Builder(this)
                    .setTitle("Ngân sách hàng tháng")
                    .setItems(arrayOf("Sửa", "Xóa")) { _, which ->
                        when (which) {
                            0 -> {
                                // Sửa → mở calculator
                                showCalculatorSheet("0") { value ->
                                    val newAmount = value.toDoubleOrNull() ?: 0.0
                                    val totalCategoryBudget = _categories.sumOf { it.budgetAmount }

                                    if (newAmount < totalCategoryBudget && totalCategoryBudget > 0) {
                                        Toast.makeText(
                                            this,
                                            "Tổng ngân sách không thể nhỏ hơn tổng ngân sách của các mục",
                                            Toast.LENGTH_LONG
                                        ).show()
                                    } else {
                                        monthlyBudget = newAmount
                                        budgetRepository.setMonthlyBudget(newAmount)
                                        adapter.updateMonthlyBudget(newAmount)
                                    }
                                }
                            }

                            1 -> {
                                // Xóa → đặt về 0
                                val totalCategoryBudget = _categories.sumOf { it.budgetAmount }
                                if (totalCategoryBudget > 0) {
                                    Toast.makeText(
                                        this,
                                        "Tổng ngân sách không thể nhỏ hơn tổng ngân sách của các mục",
                                        Toast.LENGTH_LONG
                                    ).show()
                                } else {
                                    monthlyBudget = 0.0
                                    budgetRepository.setMonthlyBudget(0.0)
                                    adapter.updateMonthlyBudget(0.0)
                                }
                            }
                        }
                    }
                    .setNegativeButton("Hủy", null)
                    .show()
            } else {
                // Chưa có tiền → mở calculator trực tiếp
                showCalculatorSheet("0") { value ->
                    val newAmount = value.toDoubleOrNull() ?: 0.0
                    val totalCategoryBudget = _categories.sumOf { it.budgetAmount }

                    if (newAmount < totalCategoryBudget && totalCategoryBudget > 0) {
                        Toast.makeText(
                            this,
                            "Tổng ngân sách không thể nhỏ hơn tổng ngân sách của các mục",
                            Toast.LENGTH_LONG
                        ).show()
                    } else {
                        monthlyBudget = newAmount
                        budgetRepository.setMonthlyBudget(newAmount)
                        adapter.updateMonthlyBudget(newAmount)
                    }
                }
            }
        }

        // Xóa ngân sách hàng tháng
        adapter.setOnDeleteMonthlyClickListener {
            val totalCategoryBudget = _categories.sumOf { it.budgetAmount }
            if (totalCategoryBudget > 0) {
                Toast.makeText(
                    this,
                    "Tổng ngân sách không thể nhỏ hơn tổng ngân sách của các mục",
                    Toast.LENGTH_LONG
                ).show()
            } else {
                AlertDialog.Builder(this)
                    .setTitle("Xóa ngân sách")
                    .setMessage("Bạn có chắc muốn xóa ngân sách hàng tháng?")
                    .setPositiveButton("Xóa") { _, _ ->
                        monthlyBudget = 0.0
                        budgetRepository.setMonthlyBudget(0.0)
                        adapter.updateMonthlyBudget(0.0)
                    }
                    .setNegativeButton("Hủy", null)
                    .show()
            }
        }

        adapter.setOnEditClickListener { position ->
            // Validate position trước khi truy cập
            if (position < 0 || position >= _categories.size) return@setOnEditClickListener
            val item = _categories[position]

            if (item.budgetAmount > 0) {
                // Đã có tiền → hiện dialog chọn Sửa hoặc Xóa
                AlertDialog.Builder(this)
                    .setTitle(item.name)
                    .setItems(arrayOf("Sửa", "Xóa")) { _, which ->
                        when (which) {
                            0 -> {
                                // Sửa → mở calculator
                                showCalculatorSheet("0") { value ->
                                    val newAmount = value.toDoubleOrNull() ?: 0.0

                                    // CHỈ cập nhật số tiền, KHÔNG xóa khỏi list
                                    _categories[position] = item.copy(budgetAmount = newAmount)
                                    budgetRepository.saveCategoryBudget(item.id, newAmount)
                                    adapter.notifyItemChanged(position + 1)

                                    // Cập nhật lại tổng ngân sách nếu cần
                                    val totalCategoryBudget = _categories.sumOf { it.budgetAmount }
                                    if (totalCategoryBudget > monthlyBudget) {
                                        Toast.makeText(
                                            this,
                                            "Tổng ngân sách danh mục đã vượt quá tổng ngân sách và tổng ngân sách sẽ được cập nhật tự động!",
                                            Toast.LENGTH_LONG
                                        ).show()
                                        monthlyBudget = totalCategoryBudget
                                        budgetRepository.setMonthlyBudget(totalCategoryBudget)
                                        adapter.updateMonthlyBudget(totalCategoryBudget)
                                    }
                                }
                            }

                            1 -> {
                                // Xóa → CHỈ đặt số tiền về 0, KHÔNG xóa khỏi list
                                _categories[position] = item.copy(budgetAmount = 0.0)
                                budgetRepository.saveCategoryBudget(item.id, 0.0)
                                adapter.notifyItemChanged(position + 1)
                            }
                        }
                    }
                    .setNegativeButton("Hủy", null)
                    .show()
            } else {
                // Chưa có tiền → mở calculator trực tiếp
                showCalculatorSheet("0") { value ->
                    val newAmount = value.toDoubleOrNull() ?: 0.0

                    if (newAmount <= 0) {
                        // Không làm gì nếu số = 0
                        return@showCalculatorSheet
                    }

                    // Cập nhật số tiền
                    _categories[position] = item.copy(budgetAmount = newAmount)
                    budgetRepository.saveCategoryBudget(item.id, newAmount)

                    val totalCategoryBudget = _categories.sumOf { it.budgetAmount }

                    if (totalCategoryBudget > monthlyBudget) {
                        Toast.makeText(
                            this,
                            "Tổng ngân sách danh mục đã vượt quá tổng ngân sách và tổng ngân sách sẽ được cập nhật tự động!",
                            Toast.LENGTH_LONG
                        ).show()
                        monthlyBudget = totalCategoryBudget
                        budgetRepository.setMonthlyBudget(totalCategoryBudget)
                        adapter.updateMonthlyBudget(totalCategoryBudget)
                    }

                    adapter.notifyItemChanged(position + 1)
                }
            }
        }

        adapter.setOnDeleteClickListener { position ->
            // Chỉ đặt số tiền về 0, không xóa dialog xác nhận
            val item = _categories[position]
            budgetRepository.saveCategoryBudget(item.id, 0.0)
            _categories[position] = item.copy(budgetAmount = 0.0)
            adapter.notifyItemChanged(position + 1) // +1 vì position 0 là monthly
        }
    }

    override fun onResume() {
        super.onResume()
        // Load lại dữ liệu từ repository khi quay lại
        loadSavedBudgets()
        monthlyBudget = budgetRepository.getMonthlyBudget()
        adapter.updateMonthlyBudget(monthlyBudget)
    }

    /**
     * Load ngân sách đã lưu từ repository
     */
    private fun loadSavedBudgets() {
        val savedBudgets = budgetRepository.getAllCategoryBudgets()

        // Cập nhật các danh mục đã có
        _categories.forEachIndexed { index, category ->
            val savedAmount = savedBudgets[category.id] ?: 0.0
            _categories[index] = category.copy(budgetAmount = savedAmount)
        }

        // Tự động thêm các danh mục mới nếu có
        val existingIds = _categories.map { it.id }.toSet()
        savedBudgets.forEach { (id, amount) ->
            if (id !in existingIds && amount > 0) {
                // Tìm thông tin danh mục và thêm vào
                val categoryInfo = getCategoryInfo(id)
                if (categoryInfo != null) {
                    _categories.add(BudgetCategoryItem(id, categoryInfo.first, categoryInfo.second, amount))
                }
            }
        }
    }

    /**
     * Lấy thông tin danh mục theo ID
     */
    private fun getCategoryInfo(id: Int): Pair<String, Int>? {
        return when (id) {
            1 -> "Mua sắm" to R.drawable.ic_shopping
            2 -> "Đồ ăn" to R.drawable.ic_food
            3 -> "Điện thoại" to R.drawable.ic_phone
            4 -> "Giải trí" to R.drawable.ic_entertainment
            5 -> "Giáo dục" to R.drawable.ic_education
            6 -> "Sắc đẹp" to R.drawable.ic_beauty
            7 -> "Thể thao" to R.drawable.ic_sports
            8 -> "Di chuyển" to R.drawable.ic_transport
            9 -> "Nhà hàng" to R.drawable.ic_restaurant
            10 -> "Du lịch" to R.drawable.ic_travel
            11 -> "Y tế" to R.drawable.ic_medical
            12 -> "Khác" to R.drawable.ic_other
            else -> null
        }
    }

    private fun showCalculatorSheet(initialValue: String, onDone: (String) -> Unit) {
        val dialog = AlertDialog.Builder(this)
            .create()
        val sheetBinding = com.example.moneylover.databinding.BottomSheetCalculatorBinding.inflate(layoutInflater)
        dialog.setView(sheetBinding.root)

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

        fun getCleanNumber(): String {
            return sheetBinding.txtCalculatorValue.text.toString().replace(".", "").replace(",", "")
        }

        val cleanInitial = initialValue.replace(".", "").replace(",", "")
        sheetBinding.txtCalculatorValue.text = if (cleanInitial.isEmpty() || cleanInitial == "0") "0" else formatWithDots(cleanInitial)

        // Các nút số
        sheetBinding.btnCalc0.setOnClickListener {
            val current = getCleanNumber()
            sheetBinding.txtCalculatorValue.text = if (current == "0") formatWithDots("0") else formatWithDots(current + "0")
        }
        sheetBinding.btnCalc1.setOnClickListener {
            val current = getCleanNumber()
            sheetBinding.txtCalculatorValue.text = if (current == "0") formatWithDots("1") else formatWithDots(current + "1")
        }
        sheetBinding.btnCalc2.setOnClickListener {
            val current = getCleanNumber()
            sheetBinding.txtCalculatorValue.text = if (current == "0") formatWithDots("2") else formatWithDots(current + "2")
        }
        sheetBinding.btnCalc3.setOnClickListener {
            val current = getCleanNumber()
            sheetBinding.txtCalculatorValue.text = if (current == "0") formatWithDots("3") else formatWithDots(current + "3")
        }
        sheetBinding.btnCalc4.setOnClickListener {
            val current = getCleanNumber()
            sheetBinding.txtCalculatorValue.text = if (current == "0") formatWithDots("4") else formatWithDots(current + "4")
        }
        sheetBinding.btnCalc5.setOnClickListener {
            val current = getCleanNumber()
            sheetBinding.txtCalculatorValue.text = if (current == "0") formatWithDots("5") else formatWithDots(current + "5")
        }
        sheetBinding.btnCalc6.setOnClickListener {
            val current = getCleanNumber()
            sheetBinding.txtCalculatorValue.text = if (current == "0") formatWithDots("6") else formatWithDots(current + "6")
        }
        sheetBinding.btnCalc7.setOnClickListener {
            val current = getCleanNumber()
            sheetBinding.txtCalculatorValue.text = if (current == "0") formatWithDots("7") else formatWithDots(current + "7")
        }
        sheetBinding.btnCalc8.setOnClickListener {
            val current = getCleanNumber()
            sheetBinding.txtCalculatorValue.text = if (current == "0") formatWithDots("8") else formatWithDots(current + "8")
        }
        sheetBinding.btnCalc9.setOnClickListener {
            val current = getCleanNumber()
            sheetBinding.txtCalculatorValue.text = if (current == "0") formatWithDots("9") else formatWithDots(current + "9")
        }

        // Nút Clear (C) - xóa hết
        sheetBinding.btnCalcClear.setOnClickListener {
            sheetBinding.txtCalculatorValue.text = "0"
        }

        // Nút Back - xóa 1 số
        sheetBinding.btnCalcBack.setOnClickListener {
            val current = getCleanNumber()
            if (current.length > 1) {
                sheetBinding.txtCalculatorValue.text = formatWithDots(current.dropLast(1))
            } else {
                sheetBinding.txtCalculatorValue.text = "0"
            }
        }

        // Nút Done (Xong)
        sheetBinding.btnCalcDone.setOnClickListener {
            onDone(getCleanNumber())
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun returnResults() {
        // Chỉ trả về các danh mục có budget > 0
        val categoriesWithBudget = _categories.filter { it.budgetAmount > 0 }
        val intent = Intent().apply {
            putExtra(EXTRA_CATEGORIES_DATA, serializeCategories(categoriesWithBudget))
            putExtra(EXTRA_MONTHLY_BUDGET, monthlyBudget)
        }
        setResult(Activity.RESULT_OK, intent)
        finish()
    }

    private fun serializeCategories(categories: List<BudgetCategoryItem>): String {
        return categories.joinToString(";") { item ->
            "${item.id}|${item.name}|${item.iconRes}|${item.budgetAmount}"
        }
    }

    companion object {
        const val EXTRA_CATEGORIES_DATA = "extra_categories_data"
        const val EXTRA_MONTHLY_BUDGET = "extra_monthly_budget"
    }
}

/**
 * Adapter cho danh sách ngân sách (gồm: Ngân sách hàng tháng + các danh mục)
 */
class BudgetCategoryAdapter(
    private val categories: MutableList<BudgetCategoryItem>,
    private var monthlyBudget: Double
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        const val VIEW_TYPE_MONTHLY = 0
        const val VIEW_TYPE_CATEGORY = 1

        fun formatCurrency(amount: Double): String {
            return String.format("%,.0f", amount).replace(",", ".") + "đ"
        }
    }

    private var onEditMonthlyClick: (() -> Unit)? = null
    private var onDeleteMonthlyClick: (() -> Unit)? = null
    private var onEditClick: ((Int) -> Unit)? = null
    private var onDeleteClick: ((Int) -> Unit)? = null

    fun setOnEditMonthlyClickListener(listener: () -> Unit) {
        onEditMonthlyClick = listener
    }

    fun setOnDeleteMonthlyClickListener(listener: () -> Unit) {
        onDeleteMonthlyClick = listener
    }

    fun setOnEditClickListener(listener: (Int) -> Unit) {
        onEditClick = listener
    }

    fun setOnDeleteClickListener(listener: (Int) -> Unit) {
        onDeleteClick = listener
    }

    fun updateMonthlyBudget(budget: Double) {
        monthlyBudget = budget
        notifyItemChanged(0)
    }

    fun updateCategories(newCategories: List<BudgetCategoryItem>) {
        categories.clear()
        categories.addAll(newCategories)
    }

    override fun getItemViewType(position: Int): Int {
        return if (position == 0) VIEW_TYPE_MONTHLY else VIEW_TYPE_CATEGORY
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val binding = ItemBudgetCategoryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return if (viewType == VIEW_TYPE_MONTHLY) {
            MonthlyVH(binding)
        } else {
            CategoryVH(binding)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is MonthlyVH -> holder.bind(monthlyBudget, onEditMonthlyClick, onDeleteMonthlyClick)
            is CategoryVH -> holder.bind(categories[position - 1], onEditClick, onDeleteClick)
        }
    }

    override fun getItemCount(): Int = categories.size + 1

    class MonthlyVH(private val binding: ItemBudgetCategoryBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(budget: Double, onEdit: (() -> Unit)?, onDelete: (() -> Unit)?) {
            if (bindingAdapterPosition != RecyclerView.NO_POSITION && bindingAdapterPosition != 0) {
                return
            }

            binding.txtCategoryName.text = "Ngân sách hàng tháng"
            binding.txtAmount.text = formatCurrency(budget)
            binding.txtAmount.visibility = View.VISIBLE

            binding.btnEdit.visibility = View.VISIBLE
            binding.btnDelete.visibility = View.GONE

            binding.btnEdit.setOnClickListener { onEdit?.invoke() }
            binding.btnDelete.setOnClickListener { onDelete?.invoke() }
        }
    }

    class CategoryVH(private val binding: ItemBudgetCategoryBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: BudgetCategoryItem, onEdit: ((Int) -> Unit)?, onDelete: ((Int) -> Unit)?) {
            binding.txtCategoryName.text = item.name
            binding.txtAmount.text = formatCurrency(item.budgetAmount)
            binding.txtAmount.visibility = View.VISIBLE

            binding.btnEdit.visibility = View.VISIBLE
            binding.btnDelete.visibility = View.GONE

            val position = if (bindingAdapterPosition != RecyclerView.NO_POSITION && bindingAdapterPosition > 0) {
                bindingAdapterPosition - 1
            } else {
                return
            }

            binding.btnEdit.setOnClickListener { onEdit?.invoke(position) }
            binding.btnDelete.setOnClickListener { onDelete?.invoke(position) }
        }
    }
}
