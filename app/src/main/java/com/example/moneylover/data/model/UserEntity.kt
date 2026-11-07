package com.example.moneylover.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entity cho bảng "users" (Người dùng)
 * Lưu thông tin tài khoản người dùng
 */
@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,                 // ID người dùng (tự tăng)
    val name: String,                 // Tên người dùng
    val email: String = "",            // Email đăng nhập
    val password: String = "",         // Mật khẩu
    val gender: String = "Không xác định", // Giới tính
    val defaultCurrency: String = "VND",   // Đơn vị tiền tệ mặc định
    val createdAt: Long = System.currentTimeMillis() // Thời gian tạo
)
