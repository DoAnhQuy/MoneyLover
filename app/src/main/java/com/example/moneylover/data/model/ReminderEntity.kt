package com.example.moneylover.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entity cho bảng "reminders" (Lời nhắc nhở)
 * Lưu các lời nhắc thanh toán hóa đơn, tiết kiệm...
 */
@Entity(tableName = "reminders")
data class ReminderEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,                  // ID lời nhắc (tự tăng)
    val userId: Long = 1,             // Liên kết với User
    val name: String,                 // Tên mục nhắc nhở (VD: "Thanh toán tiền điện")
    val message: String,              // Lời nhắc nhớ (VD: "Nhớ thanh toán hóa đơn điện tháng này")
    val frequency: String,            // Tần suất: "DAILY" (hàng ngày), "WEEKLY" (hàng tuần), "MONTHLY" (hàng tháng)
    val startDate: Long,              // Ngày bắt đầu nhắc nhở (timestamp)
    val timeHour: Int,                // Giờ nhắc (0-23)
    val timeMinute: Int,              // Phút nhắc (0-59)
    val note: String? = null,         // Ghi chú thêm
    val isEnabled: Boolean = true,    // Trạng thái bật/tắt
    val createdAt: Long = System.currentTimeMillis() // Thời gian tạo
)
