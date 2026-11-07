package com.example.moneylover

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Patterns
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.EditText
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.lifecycle.lifecycleScope
import com.example.moneylover.data.database.AppDatabase
import com.example.moneylover.data.model.UserEntity
import com.example.moneylover.data.repository.UserRepository
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    private lateinit var actvEmail: AutoCompleteTextView
    private lateinit var etPassword: EditText
    private lateinit var ivTogglePassword: ImageView
    private lateinit var tvForgotPassword: TextView
    private lateinit var tvError: TextView
    private lateinit var btnLogin: AppCompatButton
    private lateinit var progressBar: ProgressBar
    private lateinit var tvSignUp: TextView

    private lateinit var userRepository: UserRepository
    private lateinit var prefs: SharedPreferences

    private var isPasswordVisible = false
    private var savedEmails = mutableListOf<String>()

    companion object {
        const val PREFS_NAME = "money_lover_prefs"
        const val KEY_IS_LOGGED_IN = "is_logged_in"
        const val KEY_USER_EMAIL = "user_email"
        const val KEY_USER_NAME = "user_name"
        const val KEY_USER_GENDER = "user_gender"
        const val PREFS_LOGIN_HISTORY = "login_history"
        const val MAX_LOGIN_HISTORY = 10
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)

        if (isLoggedIn()) {
            navigateToMain()
            return
        }

        setContentView(R.layout.activity_login)

        initViews()
        initDatabase()
        setupListeners()
        loadSavedEmails()
    }

    private fun loadSavedEmails() {
        lifecycleScope.launch {
            try {
                // Load từ database
                val allUsers = userRepository.getAllUsers()
                val dbEmails = allUsers.map { it.email }

                // Load từ SharedPreferences (lịch sử đăng nhập)
                val prefsEmails = getLoginHistory()

                // Gộp 2 nguồn, loại bỏ trùng lặp, giữ thứ tự: prefs trước (mới nhất), rồi đến db
                val combinedEmails = (prefsEmails + dbEmails).distinct()

                savedEmails = combinedEmails.toMutableList()

                val adapter = ArrayAdapter(
                    this@LoginActivity,
                    R.layout.item_email_dropdown,
                    savedEmails
                )
                actvEmail.setAdapter(adapter)
            } catch (e: Exception) {
                // Nếu lỗi, thử chỉ load từ SharedPreferences
                try {
                    val prefsEmails = getLoginHistory()
                    savedEmails = prefsEmails.toMutableList()
                    val adapter = ArrayAdapter(
                        this@LoginActivity,
                        R.layout.item_email_dropdown,
                        savedEmails
                    )
                    actvEmail.setAdapter(adapter)
                } catch (e2: Exception) {
                    // Ignore errors
                }
            }
        }
    }

    /**
     * Lấy lịch sử đăng nhập từ SharedPreferences
     */
    private fun getLoginHistory(): List<String> {
        val prefs = getSharedPreferences(PREFS_LOGIN_HISTORY, MODE_PRIVATE)
        val historySet = prefs.getStringSet("emails", emptySet()) ?: emptySet()
        return historySet.toList()
    }

    /**
     * Thêm email vào lịch sử đăng nhập
     */
    private fun addToLoginHistory(email: String) {
        val prefs = getSharedPreferences(PREFS_LOGIN_HISTORY, MODE_PRIVATE)
        val historySet = prefs.getStringSet("emails", emptySet())?.toMutableSet() ?: mutableSetOf()

        // Xóa email cũ nếu có (để đưa lên đầu)
        historySet.remove(email)

        // Thêm vào đầu danh sách (mới nhất)
        val newHistory = mutableListOf(email)
        newHistory.addAll(historySet)

        // Giới hạn số lượng
        val limitedHistory = newHistory.take(MAX_LOGIN_HISTORY).toSet()

        prefs.edit().putStringSet("emails", limitedHistory).apply()
    }

    private fun initViews() {
        actvEmail = findViewById(R.id.actvEmail)
        etPassword = findViewById(R.id.etPassword)
        ivTogglePassword = findViewById(R.id.ivTogglePassword)
        tvForgotPassword = findViewById(R.id.tvForgotPassword)
        tvError = findViewById(R.id.tvError)
        btnLogin = findViewById(R.id.btnLogin)
        progressBar = findViewById(R.id.progressBar)
        tvSignUp = findViewById(R.id.tvSignUp)
    }

    private fun initDatabase() {
        val database = AppDatabase.getDatabase(this)
        userRepository = UserRepository(database.userDao())
    }

    private fun setupListeners() {
        ivTogglePassword.setOnClickListener {
            togglePasswordVisibility()
        }

        tvForgotPassword.setOnClickListener {
            startActivity(Intent(this, ForgotPasswordActivity::class.java))
        }

        tvSignUp.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }

        btnLogin.setOnClickListener {
            performLogin()
        }

        actvEmail.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                tvError.visibility = View.GONE
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        etPassword.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                tvError.visibility = View.GONE
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun togglePasswordVisibility() {
        isPasswordVisible = !isPasswordVisible
        if (isPasswordVisible) {
            etPassword.inputType = android.text.InputType.TYPE_CLASS_TEXT or
                    android.text.InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            ivTogglePassword.setImageResource(R.drawable.ic_auth_visibility_off)
        } else {
            etPassword.inputType = android.text.InputType.TYPE_CLASS_TEXT or
                    android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
            ivTogglePassword.setImageResource(R.drawable.ic_auth_visibility)
        }
        etPassword.setSelection(etPassword.text?.length ?: 0)
    }

    private fun performLogin() {
        val email = actvEmail.text.toString().trim()
        val password = etPassword.text.toString()

        if (!validateInput(email, password)) {
            return
        }

        showLoading(true)

        lifecycleScope.launch {
            try {
                val user = userRepository.login(email, password)
                if (user != null) {
                    saveUserSession(email, user.name)
                    Toast.makeText(
                        this@LoginActivity,
                        getString(R.string.login_success),
                        Toast.LENGTH_SHORT
                    ).show()
                    navigateToMain()
                } else {
                    showError(getString(R.string.login_failed))
                }
            } catch (e: Exception) {
                showError(getString(R.string.login_failed))
            } finally {
                showLoading(false)
            }
        }
    }

    private fun validateInput(email: String, password: String): Boolean {
        if (email.isEmpty()) {
            showError(getString(R.string.email_required))
            return false
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            showError(getString(R.string.invalid_email))
            return false
        }

        if (password.isEmpty()) {
            showError(getString(R.string.password_required))
            return false
        }

        if (password.length < 6) {
            showError(getString(R.string.password_too_short))
            return false
        }

        return true
    }

    private fun showError(message: String) {
        tvError.text = message
        tvError.visibility = View.VISIBLE
    }

    private fun showLoading(show: Boolean) {
        progressBar.visibility = if (show) View.VISIBLE else View.GONE
        btnLogin.isEnabled = !show
        actvEmail.isEnabled = !show
        etPassword.isEnabled = !show
    }

    private fun saveUserSession(email: String, name: String) {
        prefs.edit().apply {
            putBoolean(KEY_IS_LOGGED_IN, true)
            putString(KEY_USER_EMAIL, email)
            putString(KEY_USER_NAME, name)
            apply()
        }

        // Lưu vào lịch sử đăng nhập
        addToLoginHistory(email)
        
        // Khôi phục name và gender từ saved preferences nếu có
        val savedName = prefs.getString("saved_user_name", null)
        val savedGender = prefs.getString("saved_user_gender", null)
        if (savedName != null || savedGender != null) {
            lifecycleScope.launch {
                try {
                    val user = userRepository.getUserByEmail(email)
                    user?.let {
                        val updatedUser = it.copy(
                            name = savedName ?: it.name,
                            gender = savedGender ?: it.gender
                        )
                        userRepository.updateUser(updatedUser)
                    }
                    // Xóa saved preferences sau khi khôi phục
                    prefs.edit().apply {
                        remove("saved_user_name")
                        remove("saved_user_gender")
                        apply()
                    }
                } catch (e: Exception) {
                    // Ignore errors
                }
            }
        }
    }

    private fun isLoggedIn(): Boolean {
        return prefs.getBoolean(KEY_IS_LOGGED_IN, false)
    }

    private fun navigateToMain() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}
