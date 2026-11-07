package com.example.moneylover

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.EditText
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.moneylover.data.database.AppDatabase
import com.example.moneylover.databinding.LayoutMeBinding
import com.example.moneylover.ui.state.MeUiState
import com.example.moneylover.ui.viewmodel.MeViewModel
import com.google.android.material.bottomsheet.BottomSheetDialog
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MeActivity : AppCompatActivity() {

    private lateinit var binding: LayoutMeBinding
    private val viewModel: MeViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = LayoutMeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupClickListeners()
        observeUiState()
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshUser()
    }

    private fun observeUiState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collectLatest { state ->
                    renderUi(state)
                }
            }
        }
    }

    private fun renderUi(state: MeUiState) {
        binding.txtUserName.text = if (state.isLoggedIn && state.userName != "Chưa đặt tên") state.userName else "Tên người dùng"
        binding.txtUserId.text = "ID: #${String.format("%06d", state.userId)}"
        binding.txtName.text = state.userName
        binding.txtGender.text = state.userGender
        binding.txtCurrency.text = viewModel.getCurrencyDisplayName(state.userCurrency)
        binding.txtReminderCount.text = "${state.reminderCount} lời nhắc"
    }

    private fun setupClickListeners() {
        // Chỉnh sửa tên
        binding.layoutName.setOnClickListener {
            showEditNameDialog()
        }

        // Chỉnh sửa giới tính
        binding.layoutGender.setOnClickListener {
            showGenderPicker()
        }

        // Tiền tệ mặc định
        binding.layoutCurrency.setOnClickListener {
            showCurrencyPicker()
        }

        // Xuất dữ liệu
        binding.layoutExport.setOnClickListener {
            exportData()
        }

        // Đăng xuất
        binding.layoutLogout.setOnClickListener {
            showLogoutConfirmation()
        }

        // Xóa tài khoản
        binding.layoutDeleteAccount.setOnClickListener {
            showDeleteAccountConfirmation()
        }

        // Lời nhắc nhở
        binding.layoutReminder.setOnClickListener {
            startActivity(Intent(this, ReminderActivity::class.java))
        }

        // Đổi mật khẩu
        binding.layoutChangePassword.setOnClickListener {
            startActivity(Intent(this, ChangePasswordActivity::class.java))
        }
    }

    private fun showEditNameDialog() {
        val editText = EditText(this).apply {
            hint = "Nhập tên của bạn"
            setText(viewModel.uiState.value.userName)
            setPadding(48, 32, 48, 32)
        }

        AlertDialog.Builder(this)
            .setTitle("Chỉnh sửa tên")
            .setView(editText)
            .setPositiveButton("Lưu") { _, _ ->
                val name = editText.text.toString().trim()
                if (name.isNotEmpty()) {
                    viewModel.updateName(name)
                }
            }
            .setNegativeButton("Hủy", null)
            .show()
    }

    private fun showGenderPicker() {
        val genders = arrayOf("Nam", "Nữ", "Khác", "Không xác định")
        val currentGender = viewModel.uiState.value.userGender
        val currentIndex = genders.indexOf(currentGender).coerceAtLeast(0)

        AlertDialog.Builder(this)
            .setTitle("Chọn giới tính")
            .setSingleChoiceItems(genders, currentIndex) { dialog, which ->
                viewModel.updateGender(genders[which])
                dialog.dismiss()
            }
            .setNegativeButton("Hủy", null)
            .show()
    }

    private fun showCurrencyPicker() {
        val currencies = arrayOf(
            "VND" to "Việt Nam Đồng",
            "USD" to "US Dollar",
            "EUR" to "Euro",
            "JPY" to "Japanese Yen",
            "GBP" to "British Pound",
            "KRW" to "Korean Won",
            "CNY" to "Chinese Yuan"
        )

        val currencyNames = currencies.map { "${it.first} - ${it.second}" }.toTypedArray()
        val currentCurrency = viewModel.uiState.value.userCurrency
        val currentIndex = currencies.indexOfFirst { it.first == currentCurrency }.coerceAtLeast(0)

        AlertDialog.Builder(this)
            .setTitle("Chọn tiền tệ mặc định")
            .setSingleChoiceItems(currencyNames, currentIndex) { dialog, which ->
                viewModel.updateDefaultCurrency(currencies[which].first)
                dialog.dismiss()
            }
            .setNegativeButton("Hủy", null)
            .show()
    }

    private fun exportData() {
        val state = viewModel.uiState.value

        if (!state.isLoggedIn) {
            Toast.makeText(this, "Vui lòng tạo hồ sơ trước", Toast.LENGTH_SHORT).show()
            return
        }

        val dialog = BottomSheetDialog(this)
        val view = layoutInflater.inflate(R.layout.bottom_sheet_export, null)
        
        view.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnExportJson).setOnClickListener {
            onExportConfirmed("json")
            dialog.dismiss()
        }
        
        view.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnExportCsv).setOnClickListener {
            onExportConfirmed("csv")
            dialog.dismiss()
        }
        
        view.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnExportTxt).setOnClickListener {
            onExportConfirmed("txt")
            dialog.dismiss()
        }
        
        view.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnCancel).setOnClickListener {
            dialog.dismiss()
        }
        
        dialog.setContentView(view)
        dialog.show()
    }

    fun onExportConfirmed(format: String) {
        try {
            val database = AppDatabase.getDatabase(applicationContext)
            val userDao = database.userDao()
            val transactionDao = database.transactionDao()
            val accountDao = database.accountDao()
            
            val user = kotlinx.coroutines.runBlocking { userDao.getUserSync() }
            val transactions = kotlinx.coroutines.runBlocking { transactionDao.getAllTransactionsSync() }
            val accounts = kotlinx.coroutines.runBlocking { accountDao.getAllAccountsSync() }
            
            val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale("vi", "VN"))
            
            val file: File? = when (format) {
                "json" -> createJsonFile(user, transactions, accounts, dateFormat)
                "csv" -> createCsvFile(user, transactions, accounts, dateFormat)
                "txt" -> createTxtFile(user, transactions, accounts, dateFormat)
                else -> null
            }
            
            if (file != null && file.exists()) {
                showExportSuccessDialog(file)
            } else {
                Toast.makeText(this, "Lỗi khi xuất dữ liệu", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Lỗi khi xuất dữ liệu: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    
    /**
     * Hiển thị dialog thành công với tùy chọn mở/chia sẻ file
     */
    private fun showExportSuccessDialog(file: File) {
        AlertDialog.Builder(this)
            .setTitle("Xuất dữ liệu thành công!")
            .setMessage("File: ${file.name}\n\nBạn muốn làm gì với file này?")
            .setPositiveButton("Chia sẻ") { _, _ ->
                shareFile(file)
            }
            .setNeutralButton("Mở file") { _, _ ->
                openFile(file)
            }
            .setNegativeButton("Đóng", null)
            .show()
    }
    
    /**
     * Chia sẻ file qua các ứng dụng khác
     */
    private fun shareFile(file: File) {
        try {
            val uri = FileProvider.getUriForFile(
                this,
                "${packageName}.fileprovider",
                file
            )
            
            val mimeType = when {
                file.name.endsWith(".json") -> "application/json"
                file.name.endsWith(".csv") -> "text/csv"
                file.name.endsWith(".txt") -> "text/plain"
                else -> "*/*"
            }
            
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Money Lover Export - ${file.name}")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            
            startActivity(Intent.createChooser(shareIntent, "Chia sẻ file"))
        } catch (e: Exception) {
            Toast.makeText(this, "Lỗi khi chia sẻ: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    
    /**
     * Mở file bằng ứng dụng mặc định
     */
    private fun openFile(file: File) {
        try {
            val uri = FileProvider.getUriForFile(
                this,
                "${packageName}.fileprovider",
                file
            )
            
            val mimeType = when {
                file.name.endsWith(".json") -> "application/json"
                file.name.endsWith(".csv") -> "text/csv"
                file.name.endsWith(".txt") -> "text/plain"
                else -> "*/*"
            }
            
            val openIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, mimeType)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            
            startActivity(openIntent)
        } catch (e: Exception) {
            Toast.makeText(this, "Không có ứng dụng nào có thể mở file này", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun createJsonFile(
        user: com.example.moneylover.data.model.UserEntity?,
        transactions: List<com.example.moneylover.data.model.TransactionEntity>,
        accounts: List<com.example.moneylover.data.model.AccountEntity>,
        dateFormat: SimpleDateFormat
    ): File {
        val json = JSONObject()
        
        json.put("exportInfo", JSONObject().apply {
            put("appName", "Money Lover")
            put("exportDate", System.currentTimeMillis())
            put("exportDateStr", dateFormat.format(Date()))
            put("format", "JSON")
        })
        
        json.put("user", JSONObject().apply {
            put("id", user?.id ?: 0)
            put("name", user?.name ?: "Không có")
            put("gender", user?.gender ?: "Không xác định")
            put("defaultCurrency", user?.defaultCurrency ?: "VND")
        })
        
        val transactionsArray = JSONArray()
        transactions.forEach { t ->
            transactionsArray.put(JSONObject().apply {
                put("id", t.id)
                put("categoryId", t.categoryId)
                put("categoryName", t.categoryName)
                put("amount", t.amount)
                put("type", t.type)
                put("fromAccount", t.fromAccount ?: "")
                put("toAccount", t.toAccount ?: "")
                put("date", t.date)
                put("dateStr", dateFormat.format(Date(t.date)))
                put("note", t.note ?: "")
                put("createdAt", t.createdAt)
            })
        }
        json.put("transactions", transactionsArray)
        
        val accountsArray = JSONArray()
        accounts.forEach { a ->
            accountsArray.put(JSONObject().apply {
                put("id", a.id)
                put("name", a.name)
                put("balance", a.balance)
                put("iconRes", a.iconRes)
            })
        }
        json.put("accounts", accountsArray)
        
        json.put("summary", JSONObject().apply {
            put("totalTransactions", transactions.size)
            put("totalAccounts", accounts.size)
        })
        
        val fileName = "moneylover_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())}.json"
        val file = File(getExternalFilesDir(null), fileName)
        FileWriter(file).use { writer -> writer.write(json.toString(2)) }
        
        return file
    }
    
    private fun createCsvFile(
        user: com.example.moneylover.data.model.UserEntity?,
        transactions: List<com.example.moneylover.data.model.TransactionEntity>,
        accounts: List<com.example.moneylover.data.model.AccountEntity>,
        dateFormat: SimpleDateFormat
    ): File {
        val sb = StringBuilder()
        
        sb.appendLine("=== MONEY LOVER EXPORT ===")
        sb.appendLine("Ngày xuất: ${dateFormat.format(Date())}")
        sb.appendLine()
        
        sb.appendLine("--- THÔNG TIN NGƯỜI DÙNG ---")
        sb.appendLine("ID,Tên,Giới tính,Tiền tệ mặc định")
        sb.appendLine("${user?.id ?: ""},${user?.name ?: "Không có"},${user?.gender ?: ""},${user?.defaultCurrency ?: "VND"}")
        sb.appendLine()
        
        sb.appendLine("--- TÀI KHOẢN ---")
        sb.appendLine("ID,Tên,Số dư")
        accounts.forEach { a ->
            sb.appendLine("${a.id},${a.name},${a.balance}")
        }
        sb.appendLine()
        
        sb.appendLine("--- GIAO DỊCH ---")
        sb.appendLine("ID,Danh mục,Loại,Số tiền,Tài khoản nguồn,Tài khoản đích,Ngày,Ghi chú")
        transactions.forEach { t ->
            sb.appendLine("${t.id},${t.categoryName},${t.type},${t.amount},${t.fromAccount ?: ""},${t.toAccount ?: ""},${dateFormat.format(Date(t.date))},${t.note ?: ""}")
        }
        sb.appendLine()
        
        sb.appendLine("Tổng giao dịch: ${transactions.size}")
        
        val fileName = "moneylover_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())}.csv"
        val file = File(getExternalFilesDir(null), fileName)
        FileWriter(file).use { writer -> writer.write(sb.toString()) }
        
        return file
    }
    
    private fun createTxtFile(
        user: com.example.moneylover.data.model.UserEntity?,
        transactions: List<com.example.moneylover.data.model.TransactionEntity>,
        accounts: List<com.example.moneylover.data.model.AccountEntity>,
        dateFormat: SimpleDateFormat
    ): File {
        val sb = StringBuilder()
        
        sb.appendLine("╔══════════════════════════════════════╗")
        sb.appendLine("║       MONEY LOVER - BÁO CÁO          ║")
        sb.appendLine("╚══════════════════════════════════════╝")
        sb.appendLine()
        sb.appendLine("Ngày xuất: ${dateFormat.format(Date())}")
        sb.appendLine()
        
        sb.appendLine("════════ THÔNG TIN NGƯỜI DÙNG ════════")
        sb.appendLine("ID: ${user?.id ?: "Không có"}")
        sb.appendLine("Tên: ${user?.name ?: "Không có"}")
        sb.appendLine("Giới tính: ${user?.gender ?: "Không xác định"}")
        sb.appendLine("Tiền tệ: ${user?.defaultCurrency ?: "VND"}")
        sb.appendLine()
        
        sb.appendLine("════════════ TÀI KHOẢN ═══════════════")
        if (accounts.isEmpty()) {
            sb.appendLine("Chưa có tài khoản")
        } else {
            accounts.forEach { a ->
                sb.appendLine("- ${a.name}: ${a.balance} VND")
            }
        }
        sb.appendLine()
        
        sb.appendLine("════════════ GIAO DỊCH ════════════════")
        if (transactions.isEmpty()) {
            sb.appendLine("Chưa có giao dịch")
        } else {
            transactions.forEach { t ->
                val prefix = if (t.type == "EXPENSE") "-" else "+"
                sb.appendLine("[${dateFormat.format(Date(t.date))}] ${t.categoryName}: $prefix${t.amount} VND")
                if (!t.note.isNullOrBlank()) {
                    sb.appendLine("   Ghi chú: ${t.note}")
                }
            }
        }
        sb.appendLine()
        
        sb.appendLine("══════════════════════════════════════")
        sb.appendLine("Tổng giao dịch: ${transactions.size}")
        sb.appendLine("Tổng tài khoản: ${accounts.size}")
        
        val fileName = "moneylover_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())}.txt"
        val file = File(getExternalFilesDir(null), fileName)
        FileWriter(file).use { writer -> writer.write(sb.toString()) }
        
        return file
    }

    private fun showLogoutConfirmation() {
        AlertDialog.Builder(this)
            .setTitle("Đăng xuất")
            .setMessage("Bạn có chắc muốn đăng xuất không?")
            .setPositiveButton("Đăng xuất") { _, _ ->
                performLogout()
            }
            .setNegativeButton("Hủy", null)
            .show()
    }

    private fun performLogout() {
        try {
            // Xóa trạng thái đăng nhập trong preferences
            val prefs = getSharedPreferences(LoginActivity.PREFS_NAME, 0)
            prefs.edit().apply {
                putBoolean(LoginActivity.KEY_IS_LOGGED_IN, false)
                remove(LoginActivity.KEY_USER_EMAIL)
                remove(LoginActivity.KEY_USER_NAME)
                apply()
            }
            
            Toast.makeText(this@MeActivity, "Đã đăng xuất", Toast.LENGTH_SHORT).show()
            
            // Chuyển về màn hình đăng nhập
            val intent = Intent(this@MeActivity, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finishAffinity()
            
        } catch (e: Exception) {
            Toast.makeText(this@MeActivity, "Lỗi đăng xuất: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showDeleteAccountConfirmation() {
        AlertDialog.Builder(this)
            .setTitle("Xóa tài khoản")
            .setMessage("Bạn có chắc muốn xóa tài khoản? Tất cả dữ liệu sẽ bị mất vĩnh viễn.")
            .setPositiveButton("Xóa") { _, _ ->
                viewModel.deleteAccount {
                    Toast.makeText(this, "Đã xóa tài khoản", Toast.LENGTH_SHORT).show()
                    setResult(Activity.RESULT_OK)
                    finish()
                }
            }
            .setNegativeButton("Hủy", null)
            .show()
    }
}
