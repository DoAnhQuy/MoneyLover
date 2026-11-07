package com.example.moneylover.ui.adapter

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.moneylover.ui.fragment.BarChartFragment
import com.example.moneylover.ui.fragment.LineChartFragment
import com.example.moneylover.ui.fragment.PieChartFragment

/**
 * Adapter cho ViewPager2 trong ChartActivity
 * Chuyển đổi giữa 3 loại biểu đồ: Tròn, Đường, Cột
 */
class ChartPagerAdapter(activity: FragmentActivity) : FragmentStateAdapter(activity) {

    override fun getItemCount(): Int = 3

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> PieChartFragment()    // Biểu đồ tròn
            1 -> LineChartFragment()   // Biểu đồ đường
            2 -> BarChartFragment()    // Biểu đồ cột
            else -> PieChartFragment()
        }
    }
}
