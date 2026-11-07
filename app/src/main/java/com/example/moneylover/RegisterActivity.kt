package com.example.moneylover

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
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

class RegisterActivity : AppCompatActivity() {

    private lateinit var ivBack: ImageView
    private lateinit var etName: EditText
    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var etConfirmPassword: EditText
    private lateinit var ivTogglePassword: ImageView
    private lateinit var ivToggleConfirmPassword: ImageView
    private lateinit var tvError: TextView
    private lateinit var btnRegister: AppCompatButton
    private lateinit var progressBar: ProgressBar
    private lateinit var tvLogin: TextView

    private lateinit var userRepository: UserRepository
    private lateinit var prefs: SharedPreferences

    private var isPasswordVisible = false
    private var isConfirmPasswordVisible = false

    companion object {
        const val PREFS_NAME = "money_lover_prefs"
        const val KEY_IS_LOGGED_IN = "is_logged_in"
        const val KEY_USER_EMAIL = "user_email"
        const val KEY_USER_NAME = "user_name"
        const val KEY_USER_GENDER = "user_gender"
        const val KEY_SAVED_NAME = "saved_user_name"
        const val KEY_SAVED_GENDER = "saved_user_gender"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        initViews()
        initDatabase()
        setupListeners()
        
        // Khôi phục name và gender đã lưu
        restoreSavedProfile()
    }
    
    private fun restoreSavedProfile() {
        val savedName = prefs.getString("saved_user_name", null)
        val savedGender = prefs.getString("saved_user_gender", null)
        if (savedName != null) {
            etName.setText(savedName)
        }
    }

    private fun initViews() {
        ivBack = findViewById(R.id.ivBack)
        etName = findViewById(R.id.etName)
        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)
        etConfirmPassword = findViewById(R.id.etConfirmPassword)
        ivTogglePassword = findViewById(R.id.ivTogglePassword)
        ivToggleConfirmPassword = findViewById(R.id.ivToggleConfirmPassword)
        tvError = findViewById(R.id.tvError)
        btnRegister = findViewById(R.id.btnRegister)
        progressBar = findViewById(R.id.progressBar)
        tvLogin = findViewById(R.id.tvLogin)
        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
    }

    private fun initDatabase() {
        val database = AppDatabase.getDatabase(this)
        userRepository = UserRepository(database.userDao(), this)
    }

    private fun setupListeners() {
        ivBack.setOnClickListener {
            finish()
        }

        ivTogglePassword.setOnClickListener {
            togglePasswordVisibility(etPassword, ivTogglePassword)
        }

        ivToggleConfirmPassword.setOnClickListener {
            togglePasswordVisibility(etConfirmPassword, ivToggleConfirmPassword)
        }

        tvLogin.setOnClickListener {
            finish()
        }

        btnRegister.setOnClickListener {
            performRegistration()
        }

        setupTextWatchers()
    }

    private fun setupTextWatchers() {
        val textWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                tvError.visibility = View.GONE
            }
            override fun afterTextChanged(s: Editable?) {}
        }

        etName.addTextChangedListener(textWatcher)
        etEmail.addTextChangedListener(textWatcher)
        etPassword.addTextChangedListener(textWatcher)
        etConfirmPassword.addTextChangedListener(textWatcher)
    }

    private fun togglePasswordVisibility(editText: EditText, imageView: ImageView) {
        val isVisible = if (editText == etPassword) isPasswordVisible else isConfirmPasswordVisible

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

        if (editText == etPassword) {
            isPasswordVisible = !isPasswordVisible
        } else {
            isConfirmPasswordVisible = !isConfirmPasswordVisible
        }
    }

    private fun performRegistration() {
        val name = etName.text.toString().trim()
        val email = etEmail.text.toString().trim()
        val password = etPassword.text.toString()
        val confirmPassword = etConfirmPassword.text.toString()

        if (!validateInput(name, email, password, confirmPassword)) {
            return
        }

        showLoading(true)

        lifecycleScope.launch {
            try {
                // Lấy gender đã lưu, mặc định là "Không xác định"
                val savedGender = prefs.getString("saved_user_gender", "Không xác định") ?: "Không xác định"
                
                val result = userRepository.register(name, email, password, savedGender)
                result.onSuccess {
                    saveUserSession(email, name)
                    Toast.makeText(
                        this@RegisterActivity,
                        getString(R.string.registration_success),
                        Toast.LENGTH_SHORT
                    ).show()
                    navigateToMain()
                }.onFailure { exception ->
                    val message = exception.message ?: getString(R.string.registration_failed)
                    if (message.contains("Email", ignoreCase = true)) {
                        showError(getString(R.string.email_already_exists))
                    } else {
                        showError(message)
                    }
                }
            } catch (e: Exception) {
                showError(getString(R.string.registration_failed))
            } finally {
                showLoading(false)
            }
        }
    }

    private fun validateInput(
        name: String,
        email: String,
        password: String,
        confirmPassword: String
    ): Boolean {
        if (name.isEmpty()) {
            showError(getString(R.string.name_required))
            return false
        }

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

        if (password != confirmPassword) {
            showError(getString(R.string.passwords_not_match))
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
        btnRegister.isEnabled = !show
        etName.isEnabled = !show
        etEmail.isEnabled = !show
        etPassword.isEnabled = !show
        etConfirmPassword.isEnabled = !show
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
    }

    /**
     * Thêm email vào lịch sử đăng nhập
     */
    private fun addToLoginHistory(email: String) {
        val prefsHistory = getSharedPreferences(LoginActivity.PREFS_LOGIN_HISTORY, MODE_PRIVATE)
        val historySet = prefsHistory.getStringSet("emails", emptySet())?.toMutableSet() ?: mutableSetOf()

        historySet.remove(email)
        historySet.add(email)

        prefsHistory.edit().putStringSet("emails", historySet).apply()
    }

    private fun navigateToMain() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}
