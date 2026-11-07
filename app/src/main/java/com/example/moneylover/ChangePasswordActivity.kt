package com.example.moneylover

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.lifecycle.lifecycleScope
import com.example.moneylover.data.database.AppDatabase
import com.example.moneylover.data.repository.UserRepository
import kotlinx.coroutines.launch

class ChangePasswordActivity : AppCompatActivity() {

    private lateinit var ivBack: ImageView
    private lateinit var etCurrentPassword: EditText
    private lateinit var etNewPassword: EditText
    private lateinit var etConfirmPassword: EditText
    private lateinit var ivToggleCurrentPassword: ImageView
    private lateinit var ivToggleNewPassword: ImageView
    private lateinit var ivToggleConfirmPassword: ImageView
    private lateinit var tvError: TextView
    private lateinit var btnChangePassword: AppCompatButton
    private lateinit var progressBar: ProgressBar

    private lateinit var userRepository: UserRepository

    private var isCurrentPasswordVisible = false
    private var isNewPasswordVisible = false
    private var isConfirmPasswordVisible = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_change_password)

        initViews()
        initDatabase()
        setupListeners()
    }

    private fun initViews() {
        ivBack = findViewById(R.id.ivBack)
        etCurrentPassword = findViewById(R.id.etCurrentPassword)
        etNewPassword = findViewById(R.id.etNewPassword)
        etConfirmPassword = findViewById(R.id.etConfirmPassword)
        ivToggleCurrentPassword = findViewById(R.id.ivToggleCurrentPassword)
        ivToggleNewPassword = findViewById(R.id.ivToggleNewPassword)
        ivToggleConfirmPassword = findViewById(R.id.ivToggleConfirmPassword)
        tvError = findViewById(R.id.tvError)
        btnChangePassword = findViewById(R.id.btnChangePassword)
        progressBar = findViewById(R.id.progressBar)
    }

    private fun initDatabase() {
        val database = AppDatabase.getDatabase(this)
        userRepository = UserRepository(database.userDao())
    }

    private fun setupListeners() {
        ivBack.setOnClickListener {
            finish()
        }

        ivToggleCurrentPassword.setOnClickListener {
            togglePasswordVisibility(etCurrentPassword, ivToggleCurrentPassword, isCurrentPasswordVisible)
            isCurrentPasswordVisible = !isCurrentPasswordVisible
        }

        ivToggleNewPassword.setOnClickListener {
            togglePasswordVisibility(etNewPassword, ivToggleNewPassword, isNewPasswordVisible)
            isNewPasswordVisible = !isNewPasswordVisible
        }

        ivToggleConfirmPassword.setOnClickListener {
            togglePasswordVisibility(etConfirmPassword, ivToggleConfirmPassword, isConfirmPasswordVisible)
            isConfirmPasswordVisible = !isConfirmPasswordVisible
        }

        btnChangePassword.setOnClickListener {
            changePassword()
        }
    }

    private fun togglePasswordVisibility(editText: EditText, imageView: ImageView, isVisible: Boolean) {
        if (isVisible) {
            editText.inputType = android.text.InputType.TYPE_CLASS_TEXT or
                    android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
            imageView.setImageResource(R.drawable.ic_auth_visibility)
        } else {
            editText.inputType = android.text.InputType.TYPE_CLASS_TEXT or
                    android.text.InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            imageView.setImageResource(R.drawable.ic_auth_visibility_off)
        }
        editText.setSelection(editText.text?.length ?: 0)
    }

    private fun changePassword() {
        val currentPassword = etCurrentPassword.text.toString()
        val newPassword = etNewPassword.text.toString()
        val confirmPassword = etConfirmPassword.text.toString()

        // Validate current password
        if (currentPassword.isEmpty()) {
            showError("Vui lòng nhập mật khẩu hiện tại")
            return
        }

        // Validate new password
        if (newPassword.isEmpty()) {
            showError("Vui lòng nhập mật khẩu mới")
            return
        }

        if (newPassword.length < 6) {
            showError("Mật khẩu phải có ít nhất 6 ký tự")
            return
        }

        if (newPassword != confirmPassword) {
            showError("Mật khẩu mới không khớp")
            return
        }

        showLoading(true)

        lifecycleScope.launch {
            try {
                // Get current logged in user from preferences
                val prefs = getSharedPreferences(LoginActivity.PREFS_NAME, MODE_PRIVATE)
                val email = prefs.getString(LoginActivity.KEY_USER_EMAIL, null)

                if (email == null) {
                    showError("Không tìm thấy thông tin người dùng")
                    showLoading(false)
                    return@launch
                }

                // Verify current password
                val user = userRepository.getUserByEmail(email)
                if (user == null) {
                    showError("Không tìm thấy người dùng")
                    showLoading(false)
                    return@launch
                }

                if (user.password != currentPassword) {
                    showError("Mật khẩu hiện tại không đúng")
                    showLoading(false)
                    return@launch
                }

                // Update password
                userRepository.resetPassword(email, newPassword)

                Toast.makeText(
                    this@ChangePasswordActivity,
                    "Đổi mật khẩu thành công!",
                    Toast.LENGTH_SHORT
                ).show()

                finish()

            } catch (e: Exception) {
                showError("Có lỗi xảy ra: ${e.message}")
            } finally {
                showLoading(false)
            }
        }
    }

    private fun showError(message: String) {
        tvError.text = message
        tvError.visibility = View.VISIBLE
    }

    private fun showLoading(show: Boolean) {
        progressBar.visibility = if (show) View.VISIBLE else View.GONE
        btnChangePassword.isEnabled = !show
        etCurrentPassword.isEnabled = !show
        etNewPassword.isEnabled = !show
        etConfirmPassword.isEnabled = !show
    }
}
