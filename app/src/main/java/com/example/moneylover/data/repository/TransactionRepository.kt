package com.example.moneylover.data.repository

import android.annotation.SuppressLint
import com.example.moneylover.data.database.TransactionDao
import com.example.moneylover.data.model.TransactionEntity
import kotlinx.coroutines.flow.Flow

/**
 * Repository cho Transaction (Giao dịch)
 * Quản lý dữ liệu giao dịch, là cầu nối giữa ViewModel và DAO
 * 
 * Repository là "Single Source of Truth" - nơi duy nhất chứa logic truy cập dữ liệu
 */
class TransactionRepository(private val transactionDao: TransactionDao) {
    
    // Lấy tất cả giao dịch (Flow tự động cập nhật khi data thay đổi)
    val allTransactions: Flow<List<TransactionEntity>> = transactionDao.getAllTransactions()
    
    // Đếm số giao dịch (realtime)
    val transactionCount: Flow<Int> = transactionDao.getTransactionCount()
    
    // Thêm giao dịch mới
    suspend fun insert(transaction: TransactionEntity): Long {
        return transactionDao.insert(transaction)
    }
    
    // Cập nhật giao dịch
    suspend fun update(transaction: TransactionEntity) {
        transactionDao.update(transaction)
    }
    
    // Xóa giao dịch
    suspend fun delete(transaction: TransactionEntity) {
        transactionDao.delete(transaction)
    }
    
    // Lấy giao dịch theo ID
    suspend fun getById(id: Long): TransactionEntity? {
        return transactionDao.getTransactionById(id)
    }
    
    // Lấy giao dịch theo tháng và năm (realtime)
    @SuppressLint("DefaultLocale")
    fun getByMonth(year: Int, month: Int): Flow<List<TransactionEntity>> {
        val monthStr = String.format("%02d", month)
        val yearStr = year.toString()
        return transactionDao.getTransactionsByMonth(yearStr, monthStr)
    }
    
    // Lấy giao dịch theo loại: EXPENSE/INCOME/TRANSFER (realtime)
    fun getByType(type: String): Flow<List<TransactionEntity>> {
        return transactionDao.getTransactionsByType(type)
    }
    
    // Tính tổng chi tiêu/thu nhập trong tháng (realtime)
    @SuppressLint("DefaultLocale")
    fun getSumByTypeAndMonth(type: String, year: Int, month: Int): Flow<Double?> {
        val monthStr = String.format("%02d", month)
        val yearStr = year.toString()
        return transactionDao.getSumByTypeAndMonth(type, yearStr, monthStr)
    }
    
    // Xóa tất cả giao dịch
    suspend fun deleteAll() {
        transactionDao.deleteAll()
    }
    
    // Lấy giao dịch theo ngày cụ thể (realtime)
    @SuppressLint("DefaultLocale")
    fun getByDay(year: Int, month: Int, day: Int): Flow<List<TransactionEntity>> {
        val yearStr = year.toString()
        val monthStr = String.format("%02d", month)
        val dayStr = String.format("%02d", day)
        return transactionDao.getTransactionsByDay(yearStr, monthStr, dayStr)
    }
    
    // Lấy tất cả giao dịch (1 lần, không realtime) - dùng cho biểu đồ
    suspend fun getAllTransactionsList(): List<TransactionEntity> {
        return transactionDao.getAllTransactionsList()
    }

    // Lấy tất cả giao dịch đồng bộ (cho màn hình chi tiết tài khoản)
    suspend fun getAllTransactionsSync(): List<TransactionEntity> {
        return transactionDao.getAllTransactionsList()
    }
}
