package com.example.moneylover.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entity cho bảng "accounts" (Tài khoản)
 * Lưu danh sách tài khoản (VD: Ví, Ngân hàng, Tiền mặt, Thẻ tín dụng)
 */
@Entity(tableName = "accounts")
data class AccountEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,                  // ID tài khoản (tự tăng)
    val userId: Long = 1,             // Liên kết với User
    val name: String,                  // Tên tài khoản (VD: "Ví Momo", "Ngân hàng Vietcombank")
    val balance: Double = 0.0,        // Số dư hiện tại
    val iconRes: Int,                  // Icon tài khoản (resource ID)
    val createdAt: Long = System.currentTimeMillis(), // Thời gian tạo
    val color: Int,                    // Màu sắc tài khoản
    val isPinned: Boolean = false,    // Tài khoản được ghim lên đầu
    val type: String = "cash",         // Loại: cash, bank, e_wallet, credit, virtual, investment, debt
    val isLiability: Boolean = false  // Là nợ phải trả (thẻ tín dụng, nợ phải trả)
)
