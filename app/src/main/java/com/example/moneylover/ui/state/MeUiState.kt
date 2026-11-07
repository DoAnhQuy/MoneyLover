package com.example.moneylover.ui.state

/**
 * UiState cho màn hình MeActivity (Trang cá nhân)
 * Chứa thông tin người dùng hiện tại
 */
data class MeUiState(
    val userId: Long = 0,                    // ID người dùng
    val userName: String = "Chưa đặt tên",   // Tên người dùng
    val userGender: String = "Không xác định", // Giới tính
    val userCurrency: String = "VND",        // Đơn vị tiền tệ
    val isLoggedIn: Boolean = false,         // Đã đăng nhập chưa
    val reminderCount: Int = 0               // Số lời nhắc nhở
)
