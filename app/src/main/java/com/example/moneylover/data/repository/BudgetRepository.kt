package com.example.moneylover.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.moneylover.BudgetCategoryItem
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * Repository cho Budget (Ngân sách)
 * Quản lý dữ liệu ngân sách sử dụng SharedPreferences
 */
class BudgetRepository(context: Context) {
    
    private val prefs: SharedPreferences = context.getSharedPreferences("budget_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()
    
    companion object {
        private const val KEY_MONTHLY_BUDGET = "monthly_budget"           // Key cho ngân sách tháng
        private const val KEY_CATEGORY_BUDGETS = "category_budgets"        // Key cho danh sách ngân sách theo danh mục
        private const val KEY_CATEGORY_BUDGET_PREFIX = "category_budget_"   // Prefix cho ngân sách từng danh mục
        private const val KEY_SKIPPED_CATEGORIES = "skipped_categories"     // Key cho danh mục đã bỏ qua
    }
    
    // Lấy ngân sách tháng
    fun getMonthlyBudget(): Double {
        return prefs.getFloat(KEY_MONTHLY_BUDGET, 0f).toDouble()
    }
    
    // Lưu ngân sách tháng
    fun setMonthlyBudget(amount: Double) {
        prefs.edit().putFloat(KEY_MONTHLY_BUDGET, amount.toFloat()).apply()
    }
    
    // Lưu ngân sách theo danh mục
    fun saveCategoryBudget(categoryId: Int, amount: Double) {
        prefs.edit().putFloat("$KEY_CATEGORY_BUDGET_PREFIX$categoryId", amount.toFloat()).apply()
    }
    
    // Lấy ngân sách theo danh mục
    fun getCategoryBudget(categoryId: Int): Double {
        return prefs.getFloat("$KEY_CATEGORY_BUDGET_PREFIX$categoryId", 0f).toDouble()
    }
    
    // Lưu tất cả ngân sách theo danh mục
    fun saveAllCategoryBudgets(categories: List<BudgetCategoryItem>) {
        categories.forEach { category ->
            prefs.edit().putFloat("$KEY_CATEGORY_BUDGET_PREFIX${category.id}", category.budgetAmount.toFloat()).apply()
        }
    }
    
    // Lấy tất cả ngân sách theo danh mục đã lưu
    fun getAllCategoryBudgets(): Map<Int, Double> {
        val result = mutableMapOf<Int, Double>()
        // Danh sách các ID danh mục có thể có
        val categoryIds = listOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12)
        categoryIds.forEach { id ->
            val amount = prefs.getFloat("$KEY_CATEGORY_BUDGET_PREFIX$id", 0f).toDouble()
            if (amount > 0) {
                result[id] = amount
            }
        }
        return result
    }
    
    // Xóa ngân sách của một danh mục
    fun removeCategoryBudget(categoryId: Int) {
        prefs.edit().remove("$KEY_CATEGORY_BUDGET_PREFIX$categoryId").apply()
    }
    
    // Xóa tất cả ngân sách theo danh mục
    fun clearAllCategoryBudgets() {
        val categoryIds = listOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12)
        val editor = prefs.edit()
        categoryIds.forEach { id ->
            editor.remove("$KEY_CATEGORY_BUDGET_PREFIX$id")
        }
        editor.apply()
    }

    // Lưu danh sách các danh mục đã bỏ qua
    fun saveSkippedCategories(skippedIds: Set<Int>) {
        val json = gson.toJson(skippedIds)
        prefs.edit().putString(KEY_SKIPPED_CATEGORIES, json).apply()
    }

    // Lấy danh sách các danh mục đã bỏ qua
    fun getSkippedCategories(): Set<Int> {
        val json = prefs.getString(KEY_SKIPPED_CATEGORIES, null)
        return if (json != null) {
            val type = object : TypeToken<Set<Int>>() {}.type
            gson.fromJson(json, type)
        } else {
            emptySet()
        }
    }

    // Xóa tất cả các danh mục đã bỏ qua
    fun clearSkippedCategories() {
        prefs.edit().remove(KEY_SKIPPED_CATEGORIES).apply()
    }
}
