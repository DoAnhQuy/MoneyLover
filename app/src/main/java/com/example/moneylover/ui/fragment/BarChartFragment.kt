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
import com.example.moneylover.R
import com.example.moneylover.databinding.LayoutChartBarBinding
import com.example.moneylover.ui.state.ChartPeriod
import com.example.moneylover.ui.state.ChartUiState
import com.example.moneylover.ui.state.TransactionType
import com.example.moneylover.ui.view.BarChartView
import com.example.moneylover.ui.viewmodel.ChartViewModel
import com.google.android.material.tabs.TabLayout
import kotlinx.coroutines.launch

/**
 * Fragment hiển thị biểu đồ cột (Bar Chart)
 * So sánh chi tiêu/thu nhập theo từng danh mục
 */
class BarChartFragment : Fragment() {

    private var _binding: LayoutChartBarBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: ChartViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = LayoutChartBarBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupTypeSelector()
        setupPeriodTabs()
        setupNavigation()
        observeState()
    }

    // Thiết lập nút chọn loại giao dịch
    private fun setupTypeSelector() {
        binding.layoutTypeSelector.setOnClickListener {
            showTypePopupMenu()
        }
    }

    // Hiển thị popup menu
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

    // Thiết lập tabs khoảng thời gian
    private fun setupPeriodTabs() {
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

    // Cập nhật style tab đang chọn
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

    // Thiết lập nút điều hướng
    private fun setupNavigation() {
        binding.btnPrev.setOnClickListener {
            viewModel.navigateToPreviousPeriod()
        }
        
        binding.btnNext.setOnClickListener {
            viewModel.navigateToNextPeriod()
        }
    }

    // Quan sát trạng thái UI
    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    renderUi(state)
                }
            }
        }
    }

    // Cập nhật UI
    private fun renderUi(state: ChartUiState) {
        binding.txtCurrentPeriod.text = state.periodDisplayText
        binding.txtSelectedType.text = state.transactionType.displayName

        // Hiển thị/ẩn biểu đồ
        val barData = viewModel.getCategoryBarData()
        if (barData.isEmpty()) {
            binding.barChart.visibility = View.GONE
            binding.txtNoData.visibility = View.VISIBLE
        } else {
            binding.barChart.visibility = View.VISIBLE
            binding.txtNoData.visibility = View.GONE
            binding.barChart.setData(barData)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
