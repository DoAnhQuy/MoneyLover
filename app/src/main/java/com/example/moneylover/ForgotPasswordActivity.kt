package com.example.moneylover

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
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

class ForgotPasswordActivity : AppCompatActivity() {

    private lateinit var ivBack: ImageView
    private lateinit var etEmail: EditText
    private lateinit var etNewPassword: EditText
    private lateinit var etConfirmPassword: EditText
    private lateinit var ivToggleNewPassword: ImageView
    private lateinit var ivToggleConfirmPassword: ImageView
    private lateinit var tvError: TextView
    private lateinit var btnReset: AppCompatButton
    private lateinit var progressBar: ProgressBar
    private lateinit var tvLogin: TextView

    private lateinit var userRepository: UserRepository

    private var isNewPasswordVisible = false
    private var isConfirmPasswordVisible = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_forgot_password)

        initViews()
        initDatabase()
        setupListeners()
    }

    private fun initViews() {
        ivBack = findViewById(R.id.ivBack)
        etEmail = findViewById(R.id.etEmail)
        etNewPassword = findViewById(R.id.etNewPassword)
        etConfirmPassword = findViewById(R.id.etConfirmPassword)
        ivToggleNewPassword = findViewById(R.id.ivToggleNewPassword)
        ivToggleConfirmPassword = findViewById(R.id.ivToggleConfirmPassword)
        tvError = findViewById(R.id.tvError)
        btnReset = findViewById(R.id.btnSend)
        progressBar = findViewById(R.id.progressBar)
        tvLogin = findViewById(R.id.tvLogin)
    }

    private fun initDatabase() {
        val database = AppDatabase.getDatabase(this)
        userRepository = UserRepository(database.userDao())
    }

    private fun setupListeners() {
        ivBack.setOnClickListener {
            finish()
        }

        tvLogin.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }

        ivToggleNewPassword.setOnClickListener {
            togglePasswordVisibility(etNewPassword, ivToggleNewPassword)
        }

        ivToggleConfirmPassword.setOnClickListener {
            togglePasswordVisibility(etConfirmPassword, ivToggleConfirmPassword)
        }

        btnReset.setOnClickListener {
            resetPassword()
        }
    }

    private fun togglePasswordVisibility(editText: EditText, imageView: ImageView) {
        val isVisible = if (editText == etNewPassword) isNewPasswordVisible else isConfirmPasswordVisible

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

        if (editText == etNewPassword) {
            isNewPasswordVisible = !isNewPasswordVisible
        } else {
            isConfirmPasswordVisible = !isConfirmPasswordVisible
        }
    }

    private fun resetPassword() {
        val email = etEmail.text.toString().trim()
        val newPassword = etNewPassword.text.toString()
        val confirmPassword = etConfirmPassword.text.toString()

        // Validate email
        if (email.isEmpty()) {
            showError("Vui lòng nhập email")
            return
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            showError("Email không hợp lệ")
            return
        }

        // Validate password
        if (newPassword.isEmpty()) {
            showError("Vui lòng nhập mật khẩu mới")
            return
        }

        if (newPassword.length < 6) {
            showError("Mật khẩu phải có ít nhất 6 ký tự")
            return
        }

        if (newPassword != confirmPassword) {
            showError("Mật khẩu không khớp")
            return
        }

        showLoading(true)

        lifecycleScope.launch {
            try {
                val user = userRepository.getUserByEmail(email)
                if (user != null) {
                    userRepository.resetPassword(email, newPassword)
                    Toast.makeText(
                        this@ForgotPasswordActivity,
                        "Đổi mật khẩu thành công!",
                        Toast.LENGTH_SHORT
                    ).show()
                    navigateToLogin()
                } else {
                    showError("Email không tồn tại trong hệ thống")
                }
            } catch (e: Exception) {
                showError("Có lỗi xảy ra: ${e.message}")
            } finally {
                showLoading(false)
            }
        }
    }

    private fun navigateToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun showError(message: String) {
        tvError.text = message
        tvError.visibility = View.VISIBLE
    }

    private fun showLoading(show: Boolean) {
        progressBar.visibility = if (show) View.VISIBLE else View.GONE
        btnReset.isEnabled = !show
        etEmail.isEnabled = !show
        etNewPassword.isEnabled = !show
        etConfirmPassword.isEnabled = !show
    }
}
