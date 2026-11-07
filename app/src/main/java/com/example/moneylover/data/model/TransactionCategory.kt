package com.example.moneylover

/**
 * Model cho Category (Danh mục giao dịch)
 * Dùng trong form thêm/sửa giao dịch
 */
data class TransactionCategory(
    val id: Int,
    val name: String,
    val iconRes: Int,
    val type: TransactionType // EXPENSE, INCOME, TRANSFER
)

/**
 * Enum định nghĩa các loại giao dịch
 */
enum class TransactionType {
    EXPENSE,    // Chi tiêu
    INCOME,     // Thu nhập
    TRANSFER    // Chuyển khoản
}
