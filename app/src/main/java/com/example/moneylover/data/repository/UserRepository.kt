package com.example.moneylover.data.repository

import android.content.Context
import com.example.moneylover.data.database.UserDao
import com.example.moneylover.data.model.UserEntity
import kotlinx.coroutines.flow.Flow

/**
 * Repository cho User (Người dùng)
 * Quản lý dữ liệu người dùng, là cầu nối giữa ViewModel và DAO
 */
class UserRepository(private val userDao: UserDao, private val context: Context? = null) {

    private val prefs = context?.getSharedPreferences("money_lover_prefs", Context.MODE_PRIVATE)

    // Lấy user đầu tiên (realtime - tự động cập nhật khi data thay đổi)
    fun getUser(): Flow<UserEntity?> = userDao.getUser()

    // Lấy user đầu tiên (1 lần, không realtime)
    suspend fun getUserSync(): UserEntity? = userDao.getUserSync()

    // Tìm user theo email
    suspend fun getUserByEmail(email: String): UserEntity? = userDao.getUserByEmail(email)

    // Lấy tất cả users (dùng cho dropdown đăng nhập)
    suspend fun getAllUsers(): List<UserEntity> = userDao.getAllUsers()

    // Đăng nhập: kiểm tra email + password
    suspend fun login(email: String, password: String): UserEntity? {
        return userDao.login(email, password)
    }

    /**
     * Đăng ký tài khoản mới
     * @param name Tên người dùng
     * @param email Email đăng nhập
     * @param password Mật khẩu
     * @param gender Giới tính
     * @return Result với ID user nếu thành công
     */
    suspend fun register(name: String, email: String, password: String, gender: String = "Không xác định"): Result<Long> {
        val existingUser = userDao.getUserByEmail(email)
        if (existingUser != null) {
            return Result.failure(Exception("Email đã tồn tại"))
        }

        val user = UserEntity(
            name = name,
            email = email,
            password = password,
            gender = gender
        )
        val userId = userDao.insertUser(user)
        
        // Xóa saved preferences sau khi đăng ký thành công
        prefs?.edit()?.apply {
            remove("saved_user_name")
            remove("saved_user_gender")
            apply()
        }
        
        return Result.success(userId)
    }

    // Tạo user mới (không cần email/password)
    suspend fun createUser(name: String, gender: String = "Không xác định"): Long {
        val user = UserEntity(name = name, gender = gender)
        return userDao.insertUser(user)
    }

    // Cập nhật thông tin user
    suspend fun updateUser(user: UserEntity) {
        userDao.updateUser(user)
    }

    // Cập nhật tên user
    suspend fun updateName(userId: Long, name: String) {
        userDao.updateName(userId, name)
    }

    // Cập nhật giới tính user
    suspend fun updateGender(userId: Long, gender: String) {
        userDao.updateGender(userId, gender)
    }

    // Cập nhật đơn vị tiền tệ mặc định
    suspend fun updateDefaultCurrency(userId: Long, currency: String) {
        userDao.updateDefaultCurrency(userId, currency)
    }

    // Cập nhật mật khẩu (theo ID)
    suspend fun updatePassword(userId: Long, password: String) {
        userDao.updatePassword(userId, password)
    }

    /**
     * Đặt lại mật khẩu (quên mật khẩu)
     * @param email Email của tài khoản
     * @param newPassword Mật khẩu mới
     */
    suspend fun resetPassword(email: String, newPassword: String): Result<Unit> {
        val user = userDao.getUserByEmail(email)
            ?: return Result.failure(Exception("Không tìm thấy email"))

        userDao.updatePasswordByEmail(email, newPassword)
        return Result.success(Unit)
    }

    // Xóa tất cả users (đăng xuất)
    suspend fun deleteUser() {
        userDao.deleteAllUsers()
    }
}
