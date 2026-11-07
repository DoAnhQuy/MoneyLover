package com.example.moneylover.data.repository

import android.content.Context
import com.example.moneylover.data.database.UserDao
import com.example.moneylover.data.model.UserEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever

/**
 * Unit Test cho UserRepository
 * Test các chức năng: CRUD, đăng nhập, đăng ký, cập nhật thông tin
 */
class UserRepositoryTest {

    @Mock
    private lateinit var userDao: UserDao

    @Mock
    private lateinit var mockContext: Context

    @Mock
    private lateinit var mockPrefs: android.content.SharedPreferences

    private lateinit var repository: UserRepository

    private val sampleUser = UserEntity(
        id = 1,
        name = "Nguyễn Văn A",
        email = "test@example.com",
        password = "password123",
        gender = "Nam",
        defaultCurrency = "VND"
    )

    @Before
    fun setup() {
        userDao = mock(UserDao::class.java)
        whenever(mockContext.getSharedPreferences(any(), any())).thenReturn(mockPrefs)
        repository = UserRepository(userDao, mockContext)
    }

    // ===== GET USER =====

    @Test
    fun `getUser should return flow of user`() = runTest {
        whenever(userDao.getUser()).thenReturn(flowOf(sampleUser))

        val result = repository.getUser().first()

        assertNotNull(result)
        assertEquals(sampleUser.id, result?.id)
        assertEquals(sampleUser.email, result?.email)
    }

    @Test
    fun `getUser should return null when no user`() = runTest {
        whenever(userDao.getUser()).thenReturn(flowOf(null))

        val result = repository.getUser().first()

        assertNull(result)
    }

    @Test
    fun `getUserSync should return user`() = runTest {
        whenever(userDao.getUserSync()).thenReturn(sampleUser)

        val result = repository.getUserSync()

        assertNotNull(result)
        assertEquals(sampleUser.name, result?.name)
    }

    // ===== GET BY EMAIL =====

    @Test
    fun `getUserByEmail should return user when exists`() = runTest {
        whenever(userDao.getUserByEmail("test@example.com")).thenReturn(sampleUser)

        val result = repository.getUserByEmail("test@example.com")

        assertNotNull(result)
        assertEquals("test@example.com", result?.email)
    }

    @Test
    fun `getUserByEmail should return null when not exists`() = runTest {
        whenever(userDao.getUserByEmail("notexist@example.com")).thenReturn(null)

        val result = repository.getUserByEmail("notexist@example.com")

        assertNull(result)
    }

    // ===== GET ALL USERS =====

    @Test
    fun `getAllUsers should return list of users`() = runTest {
        val userList = listOf(sampleUser, sampleUser.copy(id = 2, email = "user2@example.com"))
        whenever(userDao.getAllUsers()).thenReturn(userList)

        val result = repository.getAllUsers()

        assertEquals(2, result.size)
    }

    // ===== LOGIN =====

    @Test
    fun `login with correct credentials should return user`() = runTest {
        whenever(userDao.login("test@example.com", "password123")).thenReturn(sampleUser)

        val result = repository.login("test@example.com", "password123")

        assertNotNull(result)
        assertEquals(sampleUser.id, result?.id)
    }

    @Test
    fun `login with wrong password should return null`() = runTest {
        whenever(userDao.login("test@example.com", "wrongpassword")).thenReturn(null)

        val result = repository.login("test@example.com", "wrongpassword")

        assertNull(result)
    }

    @Test
    fun `login with non-existent email should return null`() = runTest {
        whenever(userDao.login("notexist@example.com", "password")).thenReturn(null)

        val result = repository.login("notexist@example.com", "password")

        assertNull(result)
    }

    // ===== REGISTER =====

    @Test
    fun `register with new email should succeed`() = runTest {
        whenever(userDao.getUserByEmail("newuser@example.com")).thenReturn(null)
        whenever(userDao.insertUser(any())).thenReturn(1L)

        val result = repository.register(
            name = "New User",
            email = "newuser@example.com",
            password = "password123"
        )

        assertTrue(result.isSuccess)
        assertEquals(1L, result.getOrNull())
    }

    @Test
    fun `register with existing email should fail`() = runTest {
        whenever(userDao.getUserByEmail("test@example.com")).thenReturn(sampleUser)

        val result = repository.register(
            name = "Duplicate User",
            email = "test@example.com",
            password = "password123"
        )

        assertTrue(result.isFailure)
        assertEquals("Email đã tồn tại", result.exceptionOrNull()?.message)
    }

    @Test
    fun `register with gender should save gender`() = runTest {
        whenever(userDao.getUserByEmail("newuser@example.com")).thenReturn(null)
        whenever(userDao.insertUser(any())).thenReturn(1L)

        val result = repository.register(
            name = "New User",
            email = "newuser@example.com",
            password = "password123",
            gender = "Nữ"
        )

        assertTrue(result.isSuccess)
    }

    // ===== UPDATE USER =====

    @Test
    fun `updateUser should call dao updateUser`() = runTest {
        val updatedUser = sampleUser.copy(name = "Updated Name")

        repository.updateUser(updatedUser)

        verify(userDao).updateUser(updatedUser)
    }

    @Test
    fun `updateName should call dao updateName`() = runTest {
        repository.updateName(1, "New Name")

        verify(userDao).updateName(1, "New Name")
    }

    @Test
    fun `updateGender should call dao updateGender`() = runTest {
        repository.updateGender(1, "Nữ")

        verify(userDao).updateGender(1, "Nữ")
    }

    @Test
    fun `updateDefaultCurrency should call dao updateDefaultCurrency`() = runTest {
        repository.updateDefaultCurrency(1, "USD")

        verify(userDao).updateDefaultCurrency(1, "USD")
    }

    @Test
    fun `updatePassword should call dao updatePassword`() = runTest {
        repository.updatePassword(1, "newpassword456")

        verify(userDao).updatePassword(1, "newpassword456")
    }

    // ===== RESET PASSWORD =====

    @Test
    fun `resetPassword with existing email should succeed`() = runTest {
        whenever(userDao.getUserByEmail("test@example.com")).thenReturn(sampleUser)

        val result = repository.resetPassword("test@example.com", "newpassword789")

        assertTrue(result.isSuccess)
        verify(userDao).updatePasswordByEmail("test@example.com", "newpassword789")
    }

    @Test
    fun `resetPassword with non-existent email should fail`() = runTest {
        whenever(userDao.getUserByEmail("notexist@example.com")).thenReturn(null)

        val result = repository.resetPassword("notexist@example.com", "newpassword")

        assertTrue(result.isFailure)
        assertEquals("Không tìm thấy email", result.exceptionOrNull()?.message)
    }

    // ===== DELETE USER =====

    @Test
    fun `deleteUser should call dao deleteAllUsers`() = runTest {
        repository.deleteUser()

        verify(userDao).deleteAllUsers()
    }

    // ===== CREATE USER =====

    @Test
    fun `createUser should insert user and return id`() = runTest {
        whenever(userDao.insertUser(any())).thenReturn(1L)

        val result = repository.createUser("Quick User", "Nam")

        assertEquals(1L, result)
    }

    // ===== EDGE CASES =====

    @Test
    fun `login with empty email should call dao`() = runTest {
        whenever(userDao.login("", "password")).thenReturn(null)

        val result = repository.login("", "password")

        assertNull(result)
    }

    @Test
    fun `register with empty name should succeed`() = runTest {
        whenever(userDao.getUserByEmail("empty@example.com")).thenReturn(null)
        whenever(userDao.insertUser(any())).thenReturn(1L)

        val result = repository.register("", "empty@example.com", "password")

        assertTrue(result.isSuccess)
    }

    @Test
    fun `updateUser with default gender should work`() = runTest {
        val userWithDefaultGender = sampleUser.copy(gender = "Không xác định")

        repository.updateUser(userWithDefaultGender)

        verify(userDao).updateUser(userWithDefaultGender)
    }
}
