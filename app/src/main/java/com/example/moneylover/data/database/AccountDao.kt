package com.example.moneylover.data.database

import androidx.room.*
import com.example.moneylover.data.model.AccountEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO = Data Access Object
 * Quản lý bảng "accounts" (Tài khoản: ví, ngân hàng, thẻ...)
 */
@Dao
interface AccountDao {
    
    // Thêm/Sửa tài khoản (trùng ID thì thay thế)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(account: AccountEntity): Long
    
    // Cập nhật tài khoản
    @Update
    suspend fun update(account: AccountEntity)
    
    // Xóa tài khoản
    @Delete
    suspend fun delete(account: AccountEntity)
    
    // Lấy tất cả tài khoản (ghim lên đầu trước)
    @Query("SELECT * FROM accounts ORDER BY isPinned DESC, name ASC")
    fun getAllAccounts(): Flow<List<AccountEntity>>
    
    // Lấy tài khoản theo userId (ghim lên đầu trước)
    @Query("SELECT * FROM accounts WHERE userId = :userId ORDER BY isPinned DESC, name ASC")
    fun getAccountsByUserId(userId: Long): Flow<List<AccountEntity>>
    
    // Lấy tất cả tài khoản (1 lần, không realtime)
    @Query("SELECT * FROM accounts ORDER BY isPinned DESC, name ASC")
    suspend fun getAllAccountsSync(): List<AccountEntity>
    
    // Lấy tài khoản theo userId (1 lần)
    @Query("SELECT * FROM accounts WHERE userId = :userId ORDER BY isPinned DESC, name ASC")
    suspend fun getAccountsByUserIdSync(userId: Long): List<AccountEntity>
    
    // Lấy tài khoản theo ID
    @Query("SELECT * FROM accounts WHERE id = :id")
    suspend fun getAccountById(id: Long): AccountEntity?
    
    // Lấy tài khoản theo ID (realtime)
    @Query("SELECT * FROM accounts WHERE id = :id")
    fun getAccountByIdSync(id: Long): AccountEntity?
    
    // Tìm tài khoản theo tên
    @Query("SELECT * FROM accounts WHERE name = :name")
    suspend fun getAccountByName(name: String): AccountEntity?
    
    // Cập nhật số dư
    @Query("UPDATE accounts SET balance = :balance WHERE id = :id")
    suspend fun updateBalance(id: Long, balance: Double)
    
    // Cập nhật trạng thái ghim
    @Query("UPDATE accounts SET isPinned = :isPinned WHERE id = :id")
    suspend fun updatePinned(id: Long, isPinned: Boolean)
    
    // Lấy các tài khoản nợ (thẻ tín dụng, nợ phải trả)
    @Query("SELECT * FROM accounts WHERE type = 'credit' OR type = 'debt' OR isLiability = 1")
    suspend fun getLiabilityAccounts(): List<AccountEntity>
    
    // Xóa tất cả tài khoản
    @Query("DELETE FROM accounts")
    suspend fun deleteAll()
}
