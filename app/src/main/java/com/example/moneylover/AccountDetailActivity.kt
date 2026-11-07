package com.example.moneylover

import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.moneylover.data.database.AppDatabase
import com.example.moneylover.data.model.AccountEntity
import com.example.moneylover.data.model.TransactionEntity
import com.example.moneylover.data.repository.AccountRepository
import com.example.moneylover.data.repository.TransactionRepository
import com.example.moneylover.databinding.ActivityAccountDetailBinding
import com.example.moneylover.ui.adapter.TransactionAdapter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.NumberFormat
import java.util.Locale

class AccountDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAccountDetailBinding
    private lateinit var accountRepository: AccountRepository
    private lateinit var transactionRepository: TransactionRepository
    private lateinit var transactionAdapter: TransactionAdapter

    private var account: AccountEntity? = null
    private val transactions = mutableListOf<TransactionEntity>()

    companion object {
        const val EXTRA_ACCOUNT_ID = "extra_account_id"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAccountDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val database = AppDatabase.getDatabase(applicationContext)
        accountRepository = AccountRepository(database.accountDao())
        transactionRepository = TransactionRepository(database.transactionDao())

        setupToolbar()
        setupRecyclerView()
        
        val accountId = intent.getLongExtra(EXTRA_ACCOUNT_ID, 0)
        if (accountId > 0) {
            loadAccountAndTransactions(accountId)
        } else {
            finish()
        }
    }

    private fun setupToolbar() {
        binding.btnBack.setOnClickListener { finish() }
    }

    private fun setupRecyclerView() {
        transactionAdapter = TransactionAdapter(
            onItemClick = { transaction ->
                showTransactionDetail(transaction)
            }
        )

        binding.rvTransactions.layoutManager = LinearLayoutManager(this)
        binding.rvTransactions.adapter = transactionAdapter
    }

    private fun loadAccountAndTransactions(accountId: Long) {
        lifecycleScope.launch {
            // Load thông tin tài khoản từ database
            account = withContext(Dispatchers.IO) {
                accountRepository.getAccountById(accountId)
            }
            
            account?.let { acc ->
                withContext(Dispatchers.Main) {
                    binding.txtAccountName.text = acc.name
                    binding.txtAccountBalance.text = formatCurrency(acc.balance)
                    
                    binding.imgAccountIcon.setImageResource(acc.iconRes)
                    val drawable = binding.imgAccountIcon.background as? GradientDrawable
                    drawable?.setColor(acc.color)
                }
                
                // Load giao dịch
                val allTransactions = withContext(Dispatchers.IO) {
                    transactionRepository.getAllTransactionsList()
                }
                val filteredTransactions = allTransactions.filter { 
                    it.fromAccount == acc.name || it.toAccount == acc.name
                }
                
                transactions.clear()
                transactions.addAll(filteredTransactions.sortedByDescending { it.date })
                
                withContext(Dispatchers.Main) {
                    transactionAdapter.submitList(transactions.toList())
                    
                    if (transactions.isEmpty()) {
                        binding.rvTransactions.visibility = View.GONE
                        binding.txtEmpty.visibility = View.VISIBLE
                    } else {
                        binding.rvTransactions.visibility = View.VISIBLE
                        binding.txtEmpty.visibility = View.GONE
                    }
                }
            } ?: run {
                finish()
            }
        }
    }

    private fun showTransactionDetail(transaction: TransactionEntity) {
        val dialog = com.google.android.material.bottomsheet.BottomSheetDialog(this)
        val sheetBinding = com.example.moneylover.databinding.BottomSheetTransactionDetailBinding.inflate(layoutInflater)

        val date = java.util.Date(transaction.date)
        val dateFormatter = java.text.SimpleDateFormat("dd/MM/yyyy", Locale("vi", "VN"))
        val timeFormatter = java.text.SimpleDateFormat("HH:mm", Locale("vi", "VN"))

        val typeText = when (transaction.type) {
            "EXPENSE" -> "Chi tiêu"
            "INCOME" -> "Thu nhập"
            else -> "Chuyển khoản"
        }

        sheetBinding.imgIcon.setImageResource(transaction.iconRes)
        sheetBinding.txtCategory.text = transaction.categoryName
        sheetBinding.txtType.text = typeText
        sheetBinding.txtAmount.text = "${if (transaction.type == "EXPENSE") "-" else "+"}${formatCurrency(transaction.amount)}"
        sheetBinding.txtDate.text = dateFormatter.format(date)
        sheetBinding.txtTime.text = timeFormatter.format(date)

        // Hiển thị tài khoản cho giao dịch chuyển khoản
        if (transaction.type == "TRANSFER") {
            sheetBinding.layoutAccount.visibility = View.VISIBLE
            val accountText = when {
                !transaction.fromAccount.isNullOrBlank() && !transaction.toAccount.isNullOrBlank() -> 
                    "${transaction.fromAccount} → ${transaction.toAccount}"
                !transaction.fromAccount.isNullOrBlank() -> transaction.fromAccount
                else -> transaction.toAccount
            }
            sheetBinding.txtAccount.text = accountText
        } else {
            // Chi tiêu/Thu nhập - hiển thị tài khoản đã chọn
            val accountName = transaction.fromAccount
            if (!accountName.isNullOrBlank()) {
                sheetBinding.layoutAccount.visibility = View.VISIBLE
                sheetBinding.txtAccount.text = accountName
            } else {
                sheetBinding.layoutAccount.visibility = View.GONE
            }
        }

        if (!transaction.note.isNullOrBlank()) {
            sheetBinding.layoutNote.visibility = View.VISIBLE
            sheetBinding.txtNote.text = transaction.note
        } else {
            sheetBinding.layoutNote.visibility = View.GONE
        }

        sheetBinding.btnEdit.visibility = View.GONE
        sheetBinding.btnDelete.visibility = View.GONE

        dialog.setContentView(sheetBinding.root)
        dialog.behavior.apply {
            skipCollapsed = true
            state = com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED
        }
        dialog.show()
    }

    private fun formatCurrency(amount: Double): String {
        val formatter = NumberFormat.getNumberInstance(Locale("vi", "VN"))
        return formatter.format(amount) + "đ"
    }
}
