package com.example.moneylover.data.repository

import com.example.moneylover.data.database.AccountDao
import com.example.moneylover.data.model.AccountEntity
import kotlinx.coroutines.flow.Flow

/**
 * Repository cho Account (Tài khoản)
 * Quản lý dữ liệu tài khoản, là cầu nối giữa ViewModel và DAO
 */
class AccountRepository(private val accountDao: AccountDao) {

    // Lấy tất cả tài khoản (realtime)
    val allAccounts: Flow<List<AccountEntity>> = accountDao.getAllAccounts()
    
    // Lấy tài khoản theo userId (realtime)
    fun getAccountsByUserId(userId: Long): Flow<List<AccountEntity>> {
        return accountDao.getAccountsByUserId(userId)
    }
    
    // Lấy tài khoản theo userId (1 lần)
    suspend fun getAccountsByUserIdSync(userId: Long): List<AccountEntity> {
        return accountDao.getAccountsByUserIdSync(userId)
    }

    // Thêm tài khoản mới
    suspend fun insert(account: AccountEntity): Long {
        return accountDao.insert(account)
    }

    // Cập nhật tài khoản
    suspend fun update(account: AccountEntity) {
        accountDao.update(account)
    }

    // Xóa tài khoản
    suspend fun delete(account: AccountEntity) {
        accountDao.delete(account)
    }

    // Lấy tài khoản theo ID
    suspend fun getAccountById(id: Long): AccountEntity? {
        return accountDao.getAccountById(id)
    }
    
    // Lấy tài khoản theo ID (realtime)
    fun getAccountByIdSync(id: Long): AccountEntity? {
        return accountDao.getAccountByIdSync(id)
    }

    // Tìm tài khoản theo tên
    suspend fun getAccountByName(name: String): AccountEntity? {
        return accountDao.getAccountByName(name)
    }

    // Cập nhật số dư tài khoản
    suspend fun updateBalance(id: Long, balance: Double) {
        accountDao.updateBalance(id, balance)
    }

    // Cập nhật trạng thái ghim (hiển thị ở đầu danh sách)
    suspend fun updatePinned(id: Long, isPinned: Boolean) {
        accountDao.updatePinned(id, isPinned)
    }

    // Xóa tất cả tài khoản
    suspend fun deleteAll() {
        accountDao.deleteAll()
    }
}
