package com.example.moneylover.data.database

import androidx.room.*
import com.example.moneylover.data.model.ReminderEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO = Data Access Object
 * Quản lý bảng "reminders" (Lời nhắc nhở)
 */
@Dao
interface ReminderDao {
    
    // Thêm/Sửa lời nhắc (trùng ID thì thay thế)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(reminder: ReminderEntity): Long
    
    // Cập nhật lời nhắc
    @Update
    suspend fun update(reminder: ReminderEntity)
    
    // Xóa lời nhắc
    @Delete
    suspend fun delete(reminder: ReminderEntity)
    
    // Xóa lời nhắc theo ID
    @Query("DELETE FROM reminders WHERE id = :id")
    suspend fun deleteById(id: Long)
    
    // Lấy tất cả lời nhắc (mới nhất trước)
    @Query("SELECT * FROM reminders ORDER BY createdAt DESC")
    fun getAllReminders(): Flow<List<ReminderEntity>>
    
    // Lấy lời nhắc theo ID
    @Query("SELECT * FROM reminders WHERE id = :id")
    suspend fun getReminderById(id: Long): ReminderEntity?
    
    // Lấy lời nhắc đang bật (sắp xếp theo giờ)
    @Query("SELECT * FROM reminders WHERE isEnabled = 1 ORDER BY timeHour, timeMinute")
    fun getEnabledReminders(): Flow<List<ReminderEntity>>
    
    // Bật/Tắt lời nhắc
    @Query("UPDATE reminders SET isEnabled = :enabled WHERE id = :id")
    suspend fun setEnabled(id: Long, enabled: Boolean)
    
    // Đếm số lời nhắc (realtime)
    @Query("SELECT COUNT(*) FROM reminders")
    fun getReminderCount(): Flow<Int>
}
