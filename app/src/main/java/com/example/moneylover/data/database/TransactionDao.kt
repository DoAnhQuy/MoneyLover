package com.example.moneylover.data.database

import androidx.room.*
import com.example.moneylover.data.model.TransactionEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO = Data Access Object
 * Quản lý bảng "transactions" (Giao dịch: chi tiêu, thu nhập, chuyển khoản)
 */
@Dao
interface TransactionDao {
    
    // Thêm/Sửa giao dịch (trùng ID thì thay thế)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(transaction: TransactionEntity): Long
    
    // Cập nhật giao dịch
    @Update
    suspend fun update(transaction: TransactionEntity)
    
    // Xóa giao dịch
    @Delete
    suspend fun delete(transaction: TransactionEntity)
    
    // Lấy tất cả giao dịch (mới nhất trước, realtime)
    @Query("SELECT * FROM transactions ORDER BY date DESC")
    fun getAllTransactions(): Flow<List<TransactionEntity>>
    
    // Lấy giao dịch theo tháng/năm (realtime)
    @Query("""
        SELECT * FROM transactions 
        WHERE strftime('%Y', date / 1000, 'unixepoch', 'localtime') = :year
        AND strftime('%m', date / 1000, 'unixepoch', 'localtime') = :month
        ORDER BY date DESC
    """)
    fun getTransactionsByMonth(year: String, month: String): Flow<List<TransactionEntity>>
    
    // Lấy giao dịch theo loại: EXPENSE/INCOME/TRANSFER (realtime)
    @Query("SELECT * FROM transactions WHERE type = :type ORDER BY date DESC")
    fun getTransactionsByType(type: String): Flow<List<TransactionEntity>>
    
    // Lấy 1 giao dịch theo ID
    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun getTransactionById(id: Long): TransactionEntity?
    
    // Xóa tất cả giao dịch
    @Query("DELETE FROM transactions")
    suspend fun deleteAll()
    
    // Đếm số giao dịch (realtime)
    @Query("SELECT COUNT(*) FROM transactions")
    fun getTransactionCount(): Flow<Int>
    
    // Tính tổng tiền theo loại/tháng (realtime)
    @Query("""
        SELECT SUM(amount) FROM transactions 
        WHERE type = :type
        AND strftime('%Y', date / 1000, 'unixepoch', 'localtime') = :year
        AND strftime('%m', date / 1000, 'unixepoch', 'localtime') = :month
    """)
    fun getSumByTypeAndMonth(type: String, year: String, month: String): Flow<Double?>
    
    // Lấy giao dịch theo ngày cụ thể (realtime)
    @Query("""
        SELECT * FROM transactions 
        WHERE strftime('%Y', date / 1000, 'unixepoch', 'localtime') = :year
        AND strftime('%m', date / 1000, 'unixepoch', 'localtime') = :month
        AND strftime('%d', date / 1000, 'unixepoch', 'localtime') = :day
        ORDER BY date DESC
    """)
    fun getTransactionsByDay(year: String, month: String, day: String): Flow<List<TransactionEntity>>
    
    // Lấy tất cả giao dịch (1 lần, không realtime)
    @Query("SELECT * FROM transactions ORDER BY date DESC")
    suspend fun getAllTransactionsSync(): List<TransactionEntity>
    
    // Alias: lấy tất cả giao dịch (1 lần)
    @Query("SELECT * FROM transactions ORDER BY date DESC")
    suspend fun getAllTransactionsList(): List<TransactionEntity>
    
    // Lấy tổng chi tiêu theo ngày trong khoảng thời gian (1 lần)
    @Query("""
        SELECT SUM(amount) FROM transactions 
        WHERE type = 'EXPENSE'
        AND date >= :startDate AND date <= :endDate
        GROUP BY strftime('%Y-%m-%d', date / 1000, 'unixepoch', 'localtime')
        ORDER BY date ASC
    """)
    suspend fun getDailyExpenseSum(startDate: Long, endDate: Long): List<Double>
    
    // Lấy tổng chi tiêu theo danh mục/tháng (1 lần)
    @Query("""
        SELECT COALESCE(SUM(amount), 0) FROM transactions 
        WHERE type = 'EXPENSE'
        AND categoryId = :categoryId
        AND strftime('%Y', date / 1000, 'unixepoch', 'localtime') = :year
        AND strftime('%m', date / 1000, 'unixepoch', 'localtime') = :month
    """)
    suspend fun getExpenseSumByCategoryAndMonth(categoryId: Int, year: String, month: String): Double
    
    // Lấy tổng chi tiêu theo danh mục/tháng (realtime)
    @Query("""
        SELECT COALESCE(SUM(amount), 0) FROM transactions 
        WHERE type = 'EXPENSE'
        AND categoryId = :categoryId
        AND strftime('%Y', date / 1000, 'unixepoch', 'localtime') = :year
        AND strftime('%m', date / 1000, 'unixepoch', 'localtime') = :month
    """)
    fun getExpenseSumByCategoryAndMonthFlow(categoryId: Int, year: String, month: String): Flow<Double>
    
    // Lấy tổng chi tiêu tất cả danh mục trong tháng (1 lần)
    @Query("""
        SELECT COALESCE(SUM(amount), 0) FROM transactions 
        WHERE type = 'EXPENSE'
        AND strftime('%Y', date / 1000, 'unixepoch', 'localtime') = :year
        AND strftime('%m', date / 1000, 'unixepoch', 'localtime') = :month
    """)
    suspend fun getTotalExpenseByMonth(year: String, month: String): Double
}
