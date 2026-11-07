package com.example.moneylover

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.PopupMenu
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.moneylover.data.database.AppDatabase
import com.example.moneylover.data.model.AccountEntity
import com.example.moneylover.data.repository.AccountRepository
import com.example.moneylover.databinding.LayoutAccountBinding
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class AccountActivity : AppCompatActivity() {

    private lateinit var binding: LayoutAccountBinding
    private lateinit var accountRepository: AccountRepository
    private lateinit var adapter: AccountAdapter
    private val accounts = mutableListOf<AccountEntity>()

    private val requestAddAccount = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            loadAccounts()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = LayoutAccountBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val database = AppDatabase.getDatabase(applicationContext)
        accountRepository = AccountRepository(database.accountDao())

        setupToolbar()
        setupRecyclerView()
        setupFab()
        loadAccounts()
    }

    private fun setupToolbar() {
        binding.btnBack.setOnClickListener {
            finish()
        }
    }

    private fun setupRecyclerView() {
        adapter = AccountAdapter(
            accounts = accounts,
            onItemClick = { account ->
                openEditAccount(account)
            },
            onMenuClick = { account, anchorView ->
                showAccountMenu(account, anchorView)
            }
        )

        binding.rvAccounts.layoutManager = LinearLayoutManager(this)
        binding.rvAccounts.adapter = adapter
    }

    private fun setupFab() {
        binding.fabAddAccount.setOnClickListener {
            val intent = Intent(this, AddAccountActivity::class.java)
            requestAddAccount.launch(intent)
        }
    }

    private fun loadAccounts() {
        lifecycleScope.launch {
            accountRepository.allAccounts.collectLatest { accountList ->
                val sortedAccounts = accountList.sortedWith(
                    compareByDescending<AccountEntity> { it.isPinned }
                        .thenBy { it.name }
                )
                accounts.clear()
                accounts.addAll(sortedAccounts)
                adapter.notifyDataSetChanged()
            }
        }
    }

    private fun openEditAccount(account: AccountEntity) {
        val intent = Intent(this, AddAccountActivity::class.java).apply {
            putExtra(AddAccountActivity.EXTRA_ACCOUNT_ID, account.id)
            putExtra(AddAccountActivity.EXTRA_ACCOUNT_NAME, account.name)
            putExtra(AddAccountActivity.EXTRA_ACCOUNT_BALANCE, account.balance)
            putExtra(AddAccountActivity.EXTRA_ACCOUNT_ICON, account.iconRes)
            putExtra(AddAccountActivity.EXTRA_ACCOUNT_COLOR, account.color)
        }
        requestAddAccount.launch(intent)
    }

    private fun showAccountMenu(account: AccountEntity, anchorView: View) {
        val popup = PopupMenu(this, anchorView)
        popup.menuInflater.inflate(R.menu.menu_account_item, popup.menu)

        // Ẩn/hiện nút Ghim tùy theo trạng thái
        val pinItem = popup.menu.findItem(R.id.action_pin)
        if (account.isPinned) {
            pinItem.title = "Bỏ ghim"
        } else {
            pinItem.title = "Ghim"
        }

        popup.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_edit -> {
                    openEditAccount(account)
                    true
                }
                R.id.action_delete -> {
                    showDeleteConfirmation(account)
                    true
                }
                R.id.action_pin -> {
                    togglePinAccount(account)
                    true
                }
                R.id.action_detail -> {
                    openAccountDetail(account)
                    true
                }
                else -> false
            }
        }
        popup.show()
    }

    private fun showDeleteConfirmation(account: AccountEntity) {
        AlertDialog.Builder(this)
            .setTitle("Xóa tài khoản")
            .setMessage("Bạn có chắc muốn xóa tài khoản \"${account.name}\"?")
            .setPositiveButton("Xóa") { _, _ ->
                lifecycleScope.launch {
                    accountRepository.delete(account)
                    Toast.makeText(this@AccountActivity, "Đã xóa tài khoản", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Hủy", null)
            .show()
    }

    private fun togglePinAccount(account: AccountEntity) {
        lifecycleScope.launch {
            accountRepository.update(account.copy(isPinned = !account.isPinned))
        }
    }

    private fun openAccountDetail(account: AccountEntity) {
        val intent = Intent(this, AccountDetailActivity::class.java).apply {
            putExtra(AccountDetailActivity.EXTRA_ACCOUNT_ID, account.id)
        }
        startActivity(intent)
    }
}
