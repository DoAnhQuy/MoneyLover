package com.example.moneylover.data.repository

import com.example.moneylover.data.database.ReminderDao
import com.example.moneylover.data.model.ReminderEntity
import kotlinx.coroutines.flow.Flow

/**
 * Repository cho Reminder (Lời nhắc nhở)
 * Quản lý dữ liệu lời nhắc, là cầu nối giữa ViewModel và DAO
 */
class ReminderRepository(private val reminderDao: ReminderDao) {

    // Lấy tất cả lời nhắc (realtime)
    fun getAllReminders(): Flow<List<ReminderEntity>> = reminderDao.getAllReminders()

    // Lấy lời nhắc đang bật (realtime)
    fun getEnabledReminders(): Flow<List<ReminderEntity>> = reminderDao.getEnabledReminders()

    // Đếm số lời nhắc (realtime)
    fun getReminderCount(): Flow<Int> = reminderDao.getReminderCount()

    // Lấy lời nhắc theo ID
    suspend fun getReminderById(id: Long): ReminderEntity? = reminderDao.getReminderById(id)

    // Thêm lời nhắc mới
    suspend fun insert(reminder: ReminderEntity): Long = reminderDao.insert(reminder)

    // Cập nhật lời nhắc
    suspend fun update(reminder: ReminderEntity) = reminderDao.update(reminder)

    // Xóa lời nhắc
    suspend fun delete(reminder: ReminderEntity) = reminderDao.delete(reminder)

    // Xóa lời nhắc theo ID
    suspend fun deleteById(id: Long) = reminderDao.deleteById(id)

    // Bật/tắt lời nhắc
    suspend fun setEnabled(id: Long, enabled: Boolean) = reminderDao.setEnabled(id, enabled)
}
