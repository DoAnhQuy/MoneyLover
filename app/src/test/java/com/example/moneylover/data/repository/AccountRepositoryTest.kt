package com.example.moneylover.data.repository

import com.example.moneylover.data.database.AccountDao
import com.example.moneylover.data.model.AccountEntity
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
 * Unit Test cho AccountRepository
 * Test các chức năng: CRUD, lọc theo userId, cập nhật số dư, ghim tài khoản
 */
class AccountRepositoryTest {

    @Mock
    private lateinit var accountDao: AccountDao

    private lateinit var repository: AccountRepository

    private val sampleAccount = AccountEntity(
        id = 1,
        userId = 1,
        name = "Ví Momo",
        balance = 500000.0,
        iconRes = 1,
        color = 0xFF2196F3.toInt(),
        type = "e_wallet",
        isPinned = false,
        isLiability = false
    )

    private val sampleAccountList = listOf(
        AccountEntity(id = 1, userId = 1, name = "Ví Momo", balance = 500000.0, iconRes = 1, color = 1, type = "e_wallet"),
        AccountEntity(id = 2, userId = 1, name = "Tiền mặt", balance = 2000000.0, iconRes = 1, color = 2, type = "cash"),
        AccountEntity(id = 3, userId = 1, name = "Vietcombank", balance = 10000000.0, iconRes = 1, color = 3, type = "bank")
    )

    @Before
    fun setup() {
        accountDao = mock(AccountDao::class.java)
        repository = AccountRepository(accountDao)
    }

    // ===== INSERT =====

    @Test
    fun `insert account should call dao insert and return id`() = runTest {
        val account = sampleAccount.copy(id = 0)
        whenever(accountDao.insert(any())).thenReturn(5L)

        val result = repository.insert(account)

        assertEquals(5L, result)
        verify(accountDao).insert(account)
    }

    @Test
    fun `insert multiple accounts should return correct ids`() = runTest {
        whenever(accountDao.insert(any())).thenReturn(1L, 2L, 3L)

        val id1 = repository.insert(sampleAccount.copy(id = 0))
        val id2 = repository.insert(sampleAccount.copy(id = 0))
        val id3 = repository.insert(sampleAccount.copy(id = 0))

        assertEquals(1L, id1)
        assertEquals(2L, id2)
        assertEquals(3L, id3)
    }

    // ===== UPDATE =====

    @Test
    fun `update account should call dao update`() = runTest {
        val updatedAccount = sampleAccount.copy(balance = 1000000.0)

        repository.update(updatedAccount)

        verify(accountDao).update(updatedAccount)
    }

    @Test
    fun `update account type should work`() = runTest {
        val updatedAccount = sampleAccount.copy(type = "bank")

        repository.update(updatedAccount)

        verify(accountDao).update(updatedAccount)
    }

    // ===== DELETE =====

    @Test
    fun `delete account should call dao delete`() = runTest {
        repository.delete(sampleAccount)

        verify(accountDao).delete(sampleAccount)
    }

    @Test
    fun `deleteAll should call dao deleteAll`() = runTest {
        repository.deleteAll()

        verify(accountDao).deleteAll()
    }

    // ===== GET BY ID =====

    @Test
    fun `getAccountById should return account when exists`() = runTest {
        whenever(accountDao.getAccountById(1)).thenReturn(sampleAccount)

        val result = repository.getAccountById(1)

        assertNotNull(result)
        assertEquals(sampleAccount.id, result?.id)
        assertEquals(sampleAccount.name, result?.name)
    }

    @Test
    fun `getAccountById should return null when not exists`() = runTest {
        whenever(accountDao.getAccountById(999)).thenReturn(null)

        val result = repository.getAccountById(999)

        assertNull(result)
    }

    @Test
    fun `getAccountByIdSync should return account`() = runTest {
        whenever(accountDao.getAccountByIdSync(1)).thenReturn(sampleAccount)

        val result = repository.getAccountByIdSync(1)

        assertNotNull(result)
        assertEquals(sampleAccount.id, result?.id)
    }

    // ===== GET BY NAME =====

    @Test
    fun `getAccountByName should return account when exists`() = runTest {
        whenever(accountDao.getAccountByName("Ví Momo")).thenReturn(sampleAccount)

        val result = repository.getAccountByName("Ví Momo")

        assertNotNull(result)
        assertEquals("Ví Momo", result?.name)
    }

    @Test
    fun `getAccountByName should return null when not exists`() = runTest {
        whenever(accountDao.getAccountByName("Không tồn tại")).thenReturn(null)

        val result = repository.getAccountByName("Không tồn tại")

        assertNull(result)
    }

    // ===== GET BY USER ID =====

    @Test
    fun `getAccountsByUserId should return flow of accounts`() = runTest {
        whenever(accountDao.getAccountsByUserId(1)).thenReturn(flowOf(sampleAccountList))

        val result = repository.getAccountsByUserId(1).first()

        assertEquals(3, result.size)
        assertTrue(result.all { it.userId == 1L })
    }

    @Test
    fun `getAccountsByUserId should return empty when no accounts`() = runTest {
        whenever(accountDao.getAccountsByUserId(999)).thenReturn(flowOf(emptyList()))

        val result = repository.getAccountsByUserId(999).first()

        assertTrue(result.isEmpty())
    }

    @Test
    fun `getAccountsByUserIdSync should return list of accounts`() = runTest {
        whenever(accountDao.getAccountsByUserIdSync(1)).thenReturn(sampleAccountList)

        val result = repository.getAccountsByUserIdSync(1)

        assertEquals(3, result.size)
    }

    // ===== GET ALL ACCOUNTS =====

    @Test
    fun `allAccounts should return flow from dao`() = runTest {
        whenever(accountDao.getAllAccounts()).thenReturn(flowOf(sampleAccountList))

        val result = repository.allAccounts.first()

        assertEquals(3, result.size)
    }

    // ===== UPDATE BALANCE =====

    @Test
    fun `updateBalance should call dao updateBalance`() = runTest {
        repository.updateBalance(1, 1000000.0)

        verify(accountDao).updateBalance(1, 1000000.0)
    }

    @Test
    fun `updateBalance with negative amount should work`() = runTest {
        repository.updateBalance(1, -500000.0)

        verify(accountDao).updateBalance(1, -500000.0)
    }

    @Test
    fun `updateBalance with zero should work`() = runTest {
        repository.updateBalance(1, 0.0)

        verify(accountDao).updateBalance(1, 0.0)
    }

    // ===== UPDATE PINNED =====

    @Test
    fun `updatePinned should call dao updatePinned with true`() = runTest {
        repository.updatePinned(1, true)

        verify(accountDao).updatePinned(1, true)
    }

    @Test
    fun `updatePinned should call dao updatePinned with false`() = runTest {
        repository.updatePinned(1, false)

        verify(accountDao).updatePinned(1, false)
    }

    // ===== EDGE CASES =====

    @Test
    fun `insert account with zero balance should work`() = runTest {
        val account = sampleAccount.copy(balance = 0.0)
        whenever(accountDao.insert(any())).thenReturn(1L)

        val result = repository.insert(account)

        assertEquals(1L, result)
    }

    @Test
    fun `insert account with large balance should work`() = runTest {
        val account = sampleAccount.copy(balance = 9999999999.99)
        whenever(accountDao.insert(any())).thenReturn(1L)

        val result = repository.insert(account)

        assertEquals(1L, result)
    }

    @Test
    fun `insert credit card account should work`() = runTest {
        val creditAccount = sampleAccount.copy(
            type = "credit",
            isLiability = true,
            balance = -5000000.0
        )
        whenever(accountDao.insert(any())).thenReturn(1L)

        val result = repository.insert(creditAccount)

        assertEquals(1L, result)
    }

    @Test
    fun `update pinned account should toggle pin status`() = runTest {
        val pinnedAccount = sampleAccount.copy(isPinned = true)

        repository.update(pinnedAccount)

        verify(accountDao).update(pinnedAccount)
    }
}
