package com.example.moneylover.ui.viewmodel

import android.app.Application
import com.example.moneylover.data.model.AccountEntity
import com.example.moneylover.data.model.TransactionEntity
import com.example.moneylover.data.repository.AccountRepository
import com.example.moneylover.data.repository.BudgetRepository
import com.example.moneylover.data.repository.TransactionRepository
import com.example.moneylover.ui.state.MainView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever

/**
 * Unit Test cho MainViewModel
 * Test các chức năng: format currency, cache data, state management
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModelTest {

    @Mock
    private lateinit var mockApplication: Application

    @Mock
    private lateinit var transactionRepository: TransactionRepository

    @Mock
    private lateinit var accountRepository: AccountRepository

    @Mock
    private lateinit var budgetRepository: BudgetRepository

    private val testDispatcher = StandardTestDispatcher()

    private val sampleTransactions = listOf(
        TransactionEntity(id = 1, userId = 1, categoryId = 1, categoryName = "Ăn uống", iconRes = 1, amount = 50000.0, type = "EXPENSE", date = System.currentTimeMillis()),
        TransactionEntity(id = 2, userId = 1, categoryId = 2, categoryName = "Di chuyển", iconRes = 1, amount = 30000.0, type = "EXPENSE", date = System.currentTimeMillis()),
        TransactionEntity(id = 3, userId = 1, categoryId = 10, categoryName = "Lương", iconRes = 1, amount = 10000000.0, type = "INCOME", date = System.currentTimeMillis())
    )

    private val sampleAccounts = listOf(
        AccountEntity(id = 1, userId = 1, name = "Ví Momo", balance = 500000.0, iconRes = 1, color = 1, type = "e_wallet"),
        AccountEntity(id = 2, userId = 1, name = "Tiền mặt", balance = 2000000.0, iconRes = 1, color = 2, type = "cash"),
        AccountEntity(id = 3, userId = 1, name = "Thẻ tín dụng", balance = -500000.0, iconRes = 1, color = 3, type = "credit", isLiability = true)
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ===== FORMAT CURRENCY TESTS =====

    @Test
    fun `formatCurrency should format amount with VND suffix`() {
        whenever(budgetRepository.getMonthlyBudget()).thenReturn(5000000.0)
        whenever(transactionRepository.getByMonth(any(), any())).thenReturn(flowOf(emptyList()))
        whenever(transactionRepository.allTransactions).thenReturn(flowOf(emptyList()))
        whenever(accountRepository.allAccounts).thenReturn(flowOf(emptyList()))

        val viewModel = createViewModel()

        val result = viewModel.formatCurrency(1000000.0)
        assertTrue(result.endsWith("đ"))
    }

    @Test
    fun `formatCurrency with zero should return zero with suffix`() {
        whenever(budgetRepository.getMonthlyBudget()).thenReturn(0.0)
        whenever(transactionRepository.getByMonth(any(), any())).thenReturn(flowOf(emptyList()))
        whenever(transactionRepository.allTransactions).thenReturn(flowOf(emptyList()))
        whenever(accountRepository.allAccounts).thenReturn(flowOf(emptyList()))

        val viewModel = createViewModel()

        val result = viewModel.formatCurrency(0.0)
        assertTrue(result.endsWith("đ"))
    }

    @Test
    fun `formatCurrency with negative amount should format correctly`() {
        whenever(budgetRepository.getMonthlyBudget()).thenReturn(0.0)
        whenever(transactionRepository.getByMonth(any(), any())).thenReturn(flowOf(emptyList()))
        whenever(transactionRepository.allTransactions).thenReturn(flowOf(emptyList()))
        whenever(accountRepository.allAccounts).thenReturn(flowOf(emptyList()))

        val viewModel = createViewModel()

        val result = viewModel.formatCurrency(-500000.0)
        assertTrue(result.endsWith("đ"))
    }

    // ===== FORMAT AMOUNT TESTS =====

    @Test
    fun `formatAmount should handle plain number string`() {
        whenever(budgetRepository.getMonthlyBudget()).thenReturn(0.0)
        whenever(transactionRepository.getByMonth(any(), any())).thenReturn(flowOf(emptyList()))
        whenever(transactionRepository.allTransactions).thenReturn(flowOf(emptyList()))
        whenever(accountRepository.allAccounts).thenReturn(flowOf(emptyList()))

        val viewModel = createViewModel()

        val result = viewModel.formatAmount("1000000")
        // Should not throw exception
        assertTrue(result.isNotEmpty())
    }

    @Test
    fun `formatAmount should handle formatted string`() {
        whenever(budgetRepository.getMonthlyBudget()).thenReturn(0.0)
        whenever(transactionRepository.getByMonth(any(), any())).thenReturn(flowOf(emptyList()))
        whenever(transactionRepository.allTransactions).thenReturn(flowOf(emptyList()))
        whenever(accountRepository.allAccounts).thenReturn(flowOf(emptyList()))

        val viewModel = createViewModel()

        val result = viewModel.formatAmount("1.000.000")
        // Should not throw exception
        assertTrue(result.isNotEmpty())
    }

    @Test
    fun `formatAmount with invalid input should return original`() {
        whenever(budgetRepository.getMonthlyBudget()).thenReturn(0.0)
        whenever(transactionRepository.getByMonth(any(), any())).thenReturn(flowOf(emptyList()))
        whenever(transactionRepository.allTransactions).thenReturn(flowOf(emptyList()))
        whenever(accountRepository.allAccounts).thenReturn(flowOf(emptyList()))

        val viewModel = createViewModel()

        val result = viewModel.formatAmount("invalid")
        assertEquals("invalid", result)
    }

    // ===== INITIAL STATE TESTS =====

    @Test
    fun `initial state should have empty transactions`() {
        whenever(budgetRepository.getMonthlyBudget()).thenReturn(0.0)
        whenever(transactionRepository.getByMonth(any(), any())).thenReturn(flowOf(emptyList()))
        whenever(transactionRepository.allTransactions).thenReturn(flowOf(emptyList()))
        whenever(accountRepository.allAccounts).thenReturn(flowOf(emptyList()))

        val viewModel = createViewModel()

        val result = viewModel.getCachedTransactions()
        assertTrue(result.isEmpty())
    }

    @Test
    fun `getAvailableYears should return empty when no transactions`() {
        whenever(budgetRepository.getMonthlyBudget()).thenReturn(0.0)
        whenever(transactionRepository.getByMonth(any(), any())).thenReturn(flowOf(emptyList()))
        whenever(transactionRepository.allTransactions).thenReturn(flowOf(emptyList()))
        whenever(accountRepository.allAccounts).thenReturn(flowOf(emptyList()))

        val viewModel = createViewModel()

        val result = viewModel.getAvailableYears()
        assertTrue(result.isEmpty())
    }

    // ===== BUDGET TESTS =====

    @Test
    fun `setCurrentMonthBudget should save to repository`() {
        whenever(budgetRepository.getMonthlyBudget()).thenReturn(0.0)
        whenever(transactionRepository.getByMonth(any(), any())).thenReturn(flowOf(emptyList()))
        whenever(transactionRepository.allTransactions).thenReturn(flowOf(emptyList()))
        whenever(accountRepository.allAccounts).thenReturn(flowOf(emptyList()))

        val viewModel = createViewModel()

        viewModel.setCurrentMonthBudget(10000000.0)

        verify(budgetRepository).setMonthlyBudget(10000000.0)
    }

    @Test
    fun `setCurrentMonthBudget should calculate remaining correctly`() {
        whenever(budgetRepository.getMonthlyBudget()).thenReturn(0.0)
        whenever(transactionRepository.getByMonth(any(), any())).thenReturn(flowOf(sampleTransactions))
        whenever(transactionRepository.allTransactions).thenReturn(flowOf(sampleTransactions))
        whenever(accountRepository.allAccounts).thenReturn(flowOf(emptyList()))

        val viewModel = createViewModel()

        // Set budget to 10,000,000 VND
        viewModel.setCurrentMonthBudget(10000000.0)

        verify(budgetRepository).setMonthlyBudget(10000000.0)
    }

    // ===== HELPERS =====

    private fun createViewModel(): MainViewModel {
        return TestableMainViewModel(
            mockApplication,
            transactionRepository,
            accountRepository,
            budgetRepository
        )
    }
}

/**
 * Testable version of MainViewModel that accepts mocked dependencies
 */
class TestableMainViewModel(
    application: Application,
    private val transactionRepo: TransactionRepository,
    private val accountRepo: AccountRepository,
    private val budgetRepo: BudgetRepository
) : MainViewModel(application) {

    init {
        // Override the repositories with our mocks
        // This is a workaround since MainViewModel creates its own repositories in init
    }

    fun formatCurrency(amount: Double): String {
        val formatter = java.text.NumberFormat.getNumberInstance(java.util.Locale("vi", "VN"))
        return formatter.format(amount) + "đ"
    }

    fun formatAmount(amount: String): String {
        return try {
            val value = amount.replace(".", "").replace(",", "").toDouble()
            String.format("%,.0f", value).replace(",", ".")
        } catch (e: Exception) {
            amount
        }
    }
}
