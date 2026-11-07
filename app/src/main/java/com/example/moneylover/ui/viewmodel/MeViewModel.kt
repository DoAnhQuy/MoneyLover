package com.example.moneylover.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.moneylover.LoginActivity
import com.example.moneylover.data.database.AppDatabase
import com.example.moneylover.data.repository.ReminderRepository
import com.example.moneylover.data.repository.UserRepository
import com.example.moneylover.ui.state.MeUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * ViewModel cho MeActivity (Trang cá nhân)
 * 
 * Nhiệm vụ:
 * 1. Quản lý thông tin người dùng
 * 2. Xử lý đăng xuất, cập nhật thông tin
 */
class MeViewModel(application: Application) : AndroidViewModel(application) {

    private val userRepository: UserRepository
    private val reminderRepository: ReminderRepository
    private val prefs = application.getSharedPreferences(LoginActivity.PREFS_NAME, 0)

    // StateFlow chứa trạng thái UI
    private val _uiState = MutableStateFlow(MeUiState())
    val uiState: StateFlow<MeUiState> = _uiState.asStateFlow()

    init {
        // Khởi tạo database và repository
        val database = AppDatabase.getDatabase(application)
        userRepository = UserRepository(database.userDao(), application)
        reminderRepository = ReminderRepository(database.reminderDao())
        loadUser()
        loadReminderCount()
    }

    // Tải thông tin user từ database
    private fun loadUser() {
        viewModelScope.launch {
            val isLoggedIn = prefs.getBoolean(LoginActivity.KEY_IS_LOGGED_IN, false)

            if (isLoggedIn) {
                // Luôn đọc user mới nhất từ database khi quay lại
                val user = userRepository.getUserSync()
                if (user != null) {
                    _uiState.value = _uiState.value.copy(
                        userId = user.id,
                        userName = user.name,
                        userGender = user.gender,
                        userCurrency = user.defaultCurrency,
                        isLoggedIn = true
                    )
                    // Cập nhật lại SharedPreferences với thông tin mới nhất
                    prefs.edit()
                        .putString(LoginActivity.KEY_USER_NAME, user.name)
                        .putString(LoginActivity.KEY_USER_EMAIL, user.email)
                        .apply()
                }
            } else {
                _uiState.value = MeUiState()
            }
        }
    }

    // Tải số lượng lời nhắc
    private fun loadReminderCount() {
        viewModelScope.launch {
            reminderRepository.getReminderCount().collectLatest { count ->
                _uiState.value = _uiState.value.copy(
                    reminderCount = count
                )
            }
        }
    }

    // Refresh dữ liệu (khi quay lại màn hình)
    fun refreshUser() {
        loadUser()
        loadReminderCount()
    }

    // Tạo user mới (khi chưa đăng nhập)
    fun createUser(name: String, gender: String = "Không xác định", onSuccess: () -> Unit) {
        viewModelScope.launch {
            val userId = userRepository.createUser(name, gender)
            _uiState.value = _uiState.value.copy(
                userId = userId,
                userName = name,
                userGender = gender,
                userCurrency = "VND",
                isLoggedIn = true
            )
            onSuccess()
        }
    }

    // Cập nhật tên user
    fun updateName(name: String) {
        viewModelScope.launch {
            val currentState = _uiState.value
            if (currentState.isLoggedIn) {
                userRepository.updateName(currentState.userId, name)
                _uiState.value = currentState.copy(userName = name)
                prefs.edit().putString(LoginActivity.KEY_USER_NAME, name).apply()
            } else {
                createUser(name, onSuccess = {})
            }
        }
    }

    // Cập nhật giới tính user
    fun updateGender(gender: String) {
        viewModelScope.launch {
            val currentState = _uiState.value
            if (currentState.isLoggedIn) {
                userRepository.updateGender(currentState.userId, gender)
                _uiState.value = currentState.copy(userGender = gender)
            }
        }
    }

    // Cập nhật đơn vị tiền tệ mặc định
    fun updateDefaultCurrency(currency: String) {
        viewModelScope.launch {
            val currentState = _uiState.value
            if (currentState.isLoggedIn) {
                userRepository.updateDefaultCurrency(currentState.userId, currency)
                _uiState.value = currentState.copy(userCurrency = currency)
            }
        }
    }

    // Đăng xuất
    fun logout(onSuccess: () -> Unit) {
        viewModelScope.launch {
            prefs.edit().apply {
                putBoolean(LoginActivity.KEY_IS_LOGGED_IN, false)
                remove(LoginActivity.KEY_USER_EMAIL)
                remove(LoginActivity.KEY_USER_NAME)
                apply()
            }
            _uiState.value = MeUiState()
            onSuccess()
        }
    }

    // Xóa tài khoản
    fun deleteAccount(onSuccess: () -> Unit) {
        viewModelScope.launch {
            userRepository.deleteUser()
            prefs.edit().apply {
                putBoolean(LoginActivity.KEY_IS_LOGGED_IN, false)
                remove(LoginActivity.KEY_USER_EMAIL)
                remove(LoginActivity.KEY_USER_NAME)
                apply()
            }
            _uiState.value = MeUiState()
            onSuccess()
        }
    }

    // Lấy tên hiển thị của đơn vị tiền tệ
    fun getCurrencyDisplayName(currencyCode: String): String {
        return when (currencyCode) {
            "VND" -> "VND (Việt Nam Đồng)"
            "USD" -> "USD (US Dollar)"
            "EUR" -> "EUR (Euro)"
            "JPY" -> "JPY (Japanese Yen)"
            "GBP" -> "GBP (British Pound)"
            "KRW" -> "KRW (Korean Won)"
            "CNY" -> "CNY (Chinese Yuan)"
            else -> "$currencyCode"
        }
    }
}
