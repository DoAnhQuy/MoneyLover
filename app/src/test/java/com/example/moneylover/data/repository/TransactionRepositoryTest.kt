package com.example.moneylover.data.repository

import com.example.moneylover.data.database.TransactionDao
import com.example.moneylover.data.model.TransactionEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.kotlin.any
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

/**
 * Unit Test cho TransactionRepository
 * Test các chức năng: CRUD, lọc theo tháng, theo loại, tính tổng
 */
class TransactionRepositoryTest {

    @Mock
    private lateinit var transactionDao: TransactionDao

    private lateinit var repository: TransactionRepository

    private val sampleTransaction = TransactionEntity(
        id = 1,
        userId = 1,
        categoryId = 1,
        categoryName = "Ăn uống",
        iconRes = 1,
        amount = 100000.0,
        type = "EXPENSE",
        date = System.currentTimeMillis(),
        note = "Test transaction"
    )

    private val sampleTransactionList = listOf(
        TransactionEntity(id = 1, categoryId = 1, categoryName = "Ăn uống", iconRes = 1, amount = 50000.0, type = "EXPENSE", date = System.currentTimeMillis()),
        TransactionEntity(id = 2, categoryId = 2, categoryName = "Di chuyển", iconRes = 1, amount = 30000.0, type = "EXPENSE", date = System.currentTimeMillis()),
        TransactionEntity(id = 3, categoryId = 10, categoryName = "Lương", iconRes = 1, amount = 10000000.0, type = "INCOME", date = System.currentTimeMillis())
    )

    @Before
    fun setup() {
        transactionDao = mock(TransactionDao::class.java)
        repository = TransactionRepository(transactionDao)
    }

    // ===== INSERT =====

    @Test
    fun `insert transaction should call dao insert and return id`() = runTest {
        val transaction = sampleTransaction.copy(id = 0)
        whenever(transactionDao.insert(any())).thenReturn(5L)

        val result = repository.insert(transaction)

        assertEquals(5L, result)
        verify(transactionDao).insert(transaction)
    }

    @Test
    fun `insert multiple transactions should return correct ids`() = runTest {
        whenever(transactionDao.insert(any())).thenReturn(1L, 2L, 3L)

        val id1 = repository.insert(sampleTransaction.copy(id = 0))
        val id2 = repository.insert(sampleTransaction.copy(id = 0))
        val id3 = repository.insert(sampleTransaction.copy(id = 0))

        assertEquals(1L, id1)
        assertEquals(2L, id2)
        assertEquals(3L, id3)
    }

    // ===== UPDATE =====

    @Test
    fun `update transaction should call dao update`() = runTest {
        val updatedTransaction = sampleTransaction.copy(amount = 200000.0)

        repository.update(updatedTransaction)

        verify(transactionDao).update(updatedTransaction)
    }

    @Test
    fun `update transaction with different type should work`() = runTest {
        val expenseToIncome = sampleTransaction.copy(type = "INCOME")

        repository.update(expenseToIncome)

        verify(transactionDao).update(expenseToIncome)
    }

    // ===== DELETE =====

    @Test
    fun `delete transaction should call dao delete`() = runTest {
        repository.delete(sampleTransaction)

        verify(transactionDao).delete(sampleTransaction)
    }

    @Test
    fun `deleteAll should call dao deleteAll`() = runTest {
        repository.deleteAll()

        verify(transactionDao).deleteAll()
    }

    // ===== GET BY ID =====

    @Test
    fun `getById should return transaction when exists`() = runTest {
        whenever(transactionDao.getTransactionById(1)).thenReturn(sampleTransaction)

        val result = repository.getById(1)

        assertNotNull(result)
        assertEquals(sampleTransaction.id, result?.id)
        assertEquals(sampleTransaction.amount, result?.amount)
    }

    @Test
    fun `getById should return null when not exists`() = runTest {
        whenever(transactionDao.getTransactionById(999)).thenReturn(null)

        val result = repository.getById(999)

        assertNull(result)
    }

    // ===== GET BY TYPE =====

    @Test
    fun `getByType should return flow of expense transactions`() = runTest {
        val expenseList = sampleTransactionList.filter { it.type == "EXPENSE" }
        whenever(transactionDao.getTransactionsByType("EXPENSE")).thenReturn(flowOf(expenseList))

        val result = repository.getByType("EXPENSE").first()

        assertEquals(2, result.size)
        assertTrue(result.all { it.type == "EXPENSE" })
    }

    @Test
    fun `getByType should return flow of income transactions`() = runTest {
        val incomeList = sampleTransactionList.filter { it.type == "INCOME" }
        whenever(transactionDao.getTransactionsByType("INCOME")).thenReturn(flowOf(incomeList))

        val result = repository.getByType("INCOME").first()

        assertEquals(1, result.size)
        assertTrue(result.all { it.type == "INCOME" })
    }

    @Test
    fun `getByType should return empty list when no transactions`() = runTest {
        whenever(transactionDao.getTransactionsByType("TRANSFER")).thenReturn(flowOf(emptyList()))

        val result = repository.getByType("TRANSFER").first()

        assertTrue(result.isEmpty())
    }

    // ===== GET BY MONTH =====

    @Test
    fun `getByMonth should format month and year correctly`() = runTest {
        whenever(transactionDao.getTransactionsByMonth("2026", "05")).thenReturn(flowOf(sampleTransactionList))

        val result = repository.getByMonth(2026, 5).first()

        verify(transactionDao).getTransactionsByMonth("2026", "05")
        assertEquals(3, result.size)
    }

    @Test
    fun `getByMonth should pad single digit month`() = runTest {
        whenever(transactionDao.getTransactionsByMonth("2026", "01")).thenReturn(flowOf(emptyList()))

        repository.getByMonth(2026, 1)

        verify(transactionDao).getTransactionsByMonth("2026", "01")
    }

    // ===== GET SUM BY TYPE AND MONTH =====

    @Test
    fun `getSumByTypeAndMonth should return sum of expenses`() = runTest {
        whenever(transactionDao.getSumByTypeAndMonth("EXPENSE", "2026", "05")).thenReturn(flowOf(80000.0))

        val result = repository.getSumByTypeAndMonth("EXPENSE", 2026, 5).first()

        assertEquals(80000.0, result)
    }

    @Test
    fun `getSumByTypeAndMonth should return sum of income`() = runTest {
        whenever(transactionDao.getSumByTypeAndMonth("INCOME", "2026", "05")).thenReturn(flowOf(10000000.0))

        val result = repository.getSumByTypeAndMonth("INCOME", 2026, 5).first()

        assertEquals(10000000.0, result)
    }

    @Test
    fun `getSumByTypeAndMonth should return null when no transactions`() = runTest {
        whenever(transactionDao.getSumByTypeAndMonth("EXPENSE", "2026", "01")).thenReturn(flowOf(null))

        val result = repository.getSumByTypeAndMonth("EXPENSE", 2026, 1).first()

        assertNull(result)
    }

    // ===== GET ALL TRANSACTIONS =====

    @Test
    fun `allTransactions should return flow from dao`() = runTest {
        whenever(transactionDao.getAllTransactions()).thenReturn(flowOf(sampleTransactionList))

        val result = repository.allTransactions.first()

        assertEquals(3, result.size)
    }

    @Test
    fun `transactionCount should return count`() = runTest {
        whenever(transactionDao.getTransactionCount()).thenReturn(flowOf(10))

        val result = repository.transactionCount.first()

        assertEquals(10, result)
    }

    @Test
    fun `getAllTransactionsList should return list sync`() = runTest {
        whenever(transactionDao.getAllTransactionsList()).thenReturn(sampleTransactionList)

        val result = repository.getAllTransactionsList()

        assertEquals(3, result.size)
        assertEquals(sampleTransactionList, result)
    }

    @Test
    fun `getAllTransactionsSync should return list sync`() = runTest {
        whenever(transactionDao.getAllTransactionsList()).thenReturn(sampleTransactionList)

        val result = repository.getAllTransactionsSync()

        assertEquals(3, result.size)
    }

    // ===== GET BY DAY =====

    @Test
    fun `getByDay should format date components correctly`() = runTest {
        whenever(transactionDao.getTransactionsByDay("2026", "05", "17")).thenReturn(flowOf(sampleTransactionList))

        val result = repository.getByDay(2026, 5, 17).first()

        verify(transactionDao).getTransactionsByDay("2026", "05", "17")
        assertEquals(3, result.size)
    }

    @Test
    fun `getByDay should pad single digit day`() = runTest {
        whenever(transactionDao.getTransactionsByDay("2026", "05", "01")).thenReturn(flowOf(emptyList()))

        repository.getByDay(2026, 5, 1)

        verify(transactionDao).getTransactionsByDay("2026", "05", "01")
    }

    // ===== EDGE CASES =====

    @Test
    fun `insert transaction with null note should work`() = runTest {
        val transaction = sampleTransaction.copy(note = null)
        whenever(transactionDao.insert(any())).thenReturn(1L)

        val result = repository.insert(transaction)

        assertEquals(1L, result)
    }

    @Test
    fun `insert transaction with zero amount should work`() = runTest {
        val transaction = sampleTransaction.copy(amount = 0.0)
        whenever(transactionDao.insert(any())).thenReturn(1L)

        val result = repository.insert(transaction)

        assertEquals(1L, result)
    }

    @Test
    fun `insert transaction with large amount should work`() = runTest {
        val transaction = sampleTransaction.copy(amount = 9999999999.99)
        whenever(transactionDao.insert(any())).thenReturn(1L)

        val result = repository.insert(transaction)

        assertEquals(1L, result)
    }

    @Test
    fun `insert transaction with TRANSFER type should work`() = runTest {
        val transaction = sampleTransaction.copy(
            type = "TRANSFER",
            fromAccount = "Ví Momo",
            toAccount = "Ngân hàng VCB"
        )
        whenever(transactionDao.insert(any())).thenReturn(1L)

        val result = repository.insert(transaction)

        assertEquals(1L, result)
    }
}
