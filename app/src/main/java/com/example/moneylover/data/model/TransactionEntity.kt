package com.example.moneylover.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entity cho bảng "transactions" (Giao dịch)
 * Lưu chi tiết các giao dịch: chi tiêu, thu nhập, chuyển khoản
 */
@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,                  // ID giao dịch (tự tăng)
    val userId: Long = 1,             // Liên kết với User
    val categoryId: Int,              // ID danh mục
    val categoryName: String,         // Tên danh mục (VD: "Ăn uống", "Lương")
    val iconRes: Int,                 // Icon danh mục (resource ID)
    val amount: Double,               // Số tiền giao dịch
    val type: String,                // Loại: "EXPENSE" (chi), "INCOME" (thu), "TRANSFER" (chuyển)
    val fromAccount: String? = null, // Tài khoản nguồn (cho chuyển khoản)
    val toAccount: String? = null,    // Tài khoản đích (cho chuyển khoản)
    val date: Long,                   // Thời gian giao dịch (timestamp milliseconds)
    val note: String? = null,         // Ghi chú giao dịch
    val createdAt: Long = System.currentTimeMillis() // Thời gian tạo
)
