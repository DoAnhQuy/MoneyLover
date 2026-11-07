package com.example.moneylover.data.database

import androidx.room.*
import com.example.moneylover.data.model.UserEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO = Data Access Object
 * Quản lý bảng "users" (Người dùng)
 */
@Dao
interface UserDao {
    // Lấy user đầu tiên (realtime)
    @Query("SELECT * FROM users LIMIT 1")
    fun getUser(): Flow<UserEntity?>

    // Lấy user đầu tiên (1 lần, không realtime)
    @Query("SELECT * FROM users LIMIT 1")
    suspend fun getUserSync(): UserEntity?

    // Tìm user theo email
    @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
    suspend fun getUserByEmail(email: String): UserEntity?

    // Đăng nhập: kiểm tra email + password
    @Query("SELECT * FROM users WHERE email = :email AND password = :password LIMIT 1")
    suspend fun login(email: String, password: String): UserEntity?

    // Lấy tất cả users (1 lần)
    @Query("SELECT * FROM users")
    suspend fun getAllUsers(): List<UserEntity>

    // Thêm/Sửa user (trùng ID thì thay thế)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity): Long

    // Cập nhật user
    @Update
    suspend fun updateUser(user: UserEntity)

    // Xóa user
    @Delete
    suspend fun deleteUser(user: UserEntity)

    // Xóa tất cả users
    @Query("DELETE FROM users")
    suspend fun deleteAllUsers()

    // Cập nhật tên user
    @Query("UPDATE users SET name = :name WHERE id = :userId")
    suspend fun updateName(userId: Long, name: String)

    // Cập nhật giới tính user
    @Query("UPDATE users SET gender = :gender WHERE id = :userId")
    suspend fun updateGender(userId: Long, gender: String)

    // Cập nhật đơn vị tiền tệ mặc định
    @Query("UPDATE users SET defaultCurrency = :currency WHERE id = :userId")
    suspend fun updateDefaultCurrency(userId: Long, currency: String)

    // Cập nhật mật khẩu (theo ID)
    @Query("UPDATE users SET password = :password WHERE id = :userId")
    suspend fun updatePassword(userId: Long, password: String)

    // Cập nhật mật khẩu (theo email)
    @Query("UPDATE users SET password = :password WHERE email = :email")
    suspend fun updatePasswordByEmail(email: String, password: String)
}
