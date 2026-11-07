package com.example.moneylover

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import com.example.moneylover.databinding.ActivityChartBinding
import com.example.moneylover.ui.adapter.ChartPagerAdapter
import com.example.moneylover.ui.state.ChartPeriod
import com.example.moneylover.ui.viewmodel.ChartViewModel
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.tabs.TabLayoutMediator
import android.view.View
import android.widget.TextView

class ChartActivity : AppCompatActivity() {

    private lateinit var binding: ActivityChartBinding
    private val viewModel: ChartViewModel by viewModels()
    private lateinit var pagerAdapter: ChartPagerAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChartBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupViewPager()
        setupClickListeners()
    }

    override fun onResume() {
        super.onResume()
        viewModel.reloadBudgetFromRepository()
    }

    private fun setupViewPager() {
        pagerAdapter = ChartPagerAdapter(this)
        binding.viewPager.adapter = pagerAdapter

        // Connect TabLayout with ViewPager2
        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> "Tròn"
                1 -> "Đường"
                2 -> "Cột"
                else -> ""
            }
        }.attach()

        // Style tabs
        for (i in 0 until binding.tabLayout.tabCount) {
            val tab = binding.tabLayout.getTabAt(i)
            tab?.let {
                val tabView = (binding.tabLayout.getChildAt(0) as? android.widget.LinearLayout)?.getChildAt(i) as? TextView
                tabView?.apply {
                    textSize = 14f
                    setTextColor(getColor(R.color.white))
                    setPadding(8, 16, 8, 16)
                }
            }
        }
    }

    private fun setupClickListeners() {
        // Calendar button - show date picker
        binding.btnCalendar.setOnClickListener {
            showDateRangePicker()
        }
    }

    private fun showDateRangePicker() {
        val dialog = BottomSheetDialog(this)
        val view = layoutInflater.inflate(R.layout.bottom_sheet_date_range_picker, null)
        
        view.findViewById<View>(R.id.tabWeek)?.setOnClickListener {
            viewModel.setPeriod(ChartPeriod.WEEK)
            dialog.dismiss()
        }
        
        view.findViewById<View>(R.id.tabMonth2)?.setOnClickListener {
            viewModel.setPeriod(ChartPeriod.MONTH)
            dialog.dismiss()
        }
        
        view.findViewById<View>(R.id.tabYear2)?.setOnClickListener {
            viewModel.setPeriod(ChartPeriod.YEAR)
            dialog.dismiss()
        }
        
        dialog.setContentView(view)
        dialog.show()
    }
}
