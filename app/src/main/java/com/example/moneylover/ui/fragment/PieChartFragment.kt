package com.example.moneylover.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.moneylover.R
import com.example.moneylover.databinding.LayoutChartPieBinding
import com.example.moneylover.ui.adapter.CategoryChartAdapter
import com.example.moneylover.ui.state.CategoryChartData
import com.example.moneylover.ui.state.ChartPeriod
import com.example.moneylover.ui.state.ChartUiState
import com.example.moneylover.ui.state.TransactionType
import com.example.moneylover.ui.view.DashboardChartView
import com.example.moneylover.ui.viewmodel.ChartViewModel
import com.google.android.material.tabs.TabLayout
import kotlinx.coroutines.launch

/**
 * Fragment hiển thị biểu đồ tròn (Pie Chart)
 * Hiển thị chi tiêu/thu nhập theo từng danh mục dưới dạng biểu đồ tròn
 */
class PieChartFragment : Fragment() {

    private var _binding: LayoutChartPieBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: ChartViewModel by activityViewModels()
    private lateinit var categoryAdapter: CategoryChartAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = LayoutChartPieBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupTypeSelector()
        setupPeriodTabs()
        setupNavigation()
        observeState()
    }

    // Thiết lập RecyclerView cho danh sách danh mục
    private fun setupRecyclerView() {
        categoryAdapter = CategoryChartAdapter()
        binding.rvCategories.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = categoryAdapter
        }
    }

    // Thiết lập nút chọn loại giao dịch (Chi tiêu/Thu nhập)
    private fun setupTypeSelector() {
        binding.layoutTypeSelector.setOnClickListener {
            showTypePopupMenu()
        }
    }

    // Hiển thị popup menu chọn loại giao dịch
    private fun showTypePopupMenu() {
        val popup = PopupMenu(requireContext(), binding.layoutTypeSelector)
        popup.menu.add(0, 0, 0, "Chi tiêu")
        popup.menu.add(0, 1, 1, "Thu nhập")
        
        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                0 -> {
                    binding.txtSelectedType.text = "Chi tiêu"
                    viewModel.setTransactionType(TransactionType.EXPENSE)
                    true
                }
                1 -> {
                    binding.txtSelectedType.text = "Thu nhập"
                    viewModel.setTransactionType(TransactionType.INCOME)
                    true
                }
                else -> false
            }
        }
        popup.show()
    }

    // Thiết lập tabs chọn khoảng thời gian (Tuần/Tháng/Năm)
    private fun setupPeriodTabs() {
        // Thêm tabs
        binding.tabPeriod.addTab(binding.tabPeriod.newTab().setText("Tuần"))
        binding.tabPeriod.addTab(binding.tabPeriod.newTab().setText("Tháng"))
        binding.tabPeriod.addTab(binding.tabPeriod.newTab().setText("Năm"))

        // Style tabs
        for (i in 0 until binding.tabPeriod.tabCount) {
            val tab = binding.tabPeriod.getTabAt(i)
            tab?.let {
                val tabView = (binding.tabPeriod.getChildAt(0) as? android.widget.LinearLayout)?.getChildAt(i) as? android.widget.TextView
                tabView?.apply {
                    textSize = 14f
                    setTextColor(requireContext().getColor(R.color.white))
                    setPadding(24, 8, 24, 8)
                }
            }
        }

        binding.tabPeriod.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                when (tab?.position) {
                    0 -> viewModel.setPeriod(ChartPeriod.WEEK)
                    1 -> viewModel.setPeriod(ChartPeriod.MONTH)
                    2 -> viewModel.setPeriod(ChartPeriod.YEAR)
                }
                updateSelectedTabStyle()
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    // Cập nhật style cho tab đang được chọn
    private fun updateSelectedTabStyle() {
        for (i in 0 until binding.tabPeriod.tabCount) {
            val tab = binding.tabPeriod.getTabAt(i)
            val tabView = (binding.tabPeriod.getChildAt(0) as? android.widget.LinearLayout)?.getChildAt(i) as? android.widget.TextView
            tabView?.apply {
                when (i) {
                    binding.tabPeriod.selectedTabPosition -> {
                        setTextColor(requireContext().getColor(R.color.white))
                        setBackgroundColor(requireContext().getColor(R.color.black))
                    }
                    else -> {
                        setTextColor(requireContext().getColor(R.color.white))
                        setBackgroundColor(requireContext().getColor(android.R.color.transparent))
                    }
                }
                setPadding(24, 8, 24, 8)
            }
        }
    }

    // Thiết lập nút điều hướng (Trước/Sau)
    private fun setupNavigation() {
        binding.btnPrev.setOnClickListener {
            viewModel.navigateToPreviousPeriod()
        }
        
        binding.btnNext.setOnClickListener {
            viewModel.navigateToNextPeriod()
        }
    }

    // Quan sát trạng thái UI từ ViewModel
    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    renderUi(state)
                }
            }
        }
    }

    // Cập nhật UI với trạng thái mới
    private fun renderUi(state: ChartUiState) {
        // Cập nhật text khoảng thời gian
        binding.txtCurrentPeriod.text = state.periodDisplayText
        
        // Cập nhật loại giao dịch
        binding.txtSelectedType.text = state.transactionType.displayName
        
        // Cập nhật biểu đồ tròn
        if (state.categories.isEmpty()) {
            binding.dashboardChart.visibility = View.GONE
            binding.txtNoData.visibility = View.VISIBLE
            binding.rvCategories.visibility = View.GONE
        } else {
            binding.dashboardChart.visibility = View.VISIBLE
            binding.txtNoData.visibility = View.GONE
            binding.rvCategories.visibility = View.VISIBLE
            
            // Cập nhật biểu đồ tròn
            val slices = state.categories.map { category ->
                DashboardChartView.ChartSlice(
                    value = category.amount.toFloat(),
                    color = category.color,
                    label = category.categoryName
                )
            }
            binding.dashboardChart.setData(slices)

            // Cập nhật danh sách danh mục với phần trăm đã điều chỉnh
            val categoryItems = adjustPercentages(state.categories)
            categoryAdapter.submitList(categoryItems)
        }
    }

    // Điều chỉnh percentages để tổng = 100%
    private fun adjustPercentages(categories: List<CategoryChartData>): List<CategoryChartAdapter.CategoryDisplayItem> {
        if (categories.isEmpty()) return emptyList()

        val rawPercents = categories.map { it.percentage }
        val total = rawPercents.sum()

        // Nếu tổng đã ≈ 100, trả về nguyên
        if (kotlin.math.abs(total - 100f) < 0.01f) {
            return categories.mapIndexed { index, category ->
                CategoryChartAdapter.CategoryDisplayItem(
                    name = category.categoryName,
                    amount = category.amount,
                    rawPercentage = category.percentage,
                    color = category.color,
                    displayPercent = rawPercents[index]
                )
            }
        }

        // Điều chỉnh item cuối cùng để tổng = 100
        val lastIndex = categories.size - 1
        return categories.mapIndexed { index, category ->
            if (index == lastIndex) {
                val adjustedPercent = 100f - (total - rawPercents[index])
                CategoryChartAdapter.CategoryDisplayItem(
                    name = category.categoryName,
                    amount = category.amount,
                    rawPercentage = category.percentage,
                    color = category.color,
                    displayPercent = adjustedPercent.coerceAtLeast(0f)
                )
            } else {
                CategoryChartAdapter.CategoryDisplayItem(
                    name = category.categoryName,
                    amount = category.amount,
                    rawPercentage = category.percentage,
                    color = category.color,
                    displayPercent = rawPercents[index]
                )
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
