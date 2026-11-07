# CÂU HỎI VÀ ĐÁP ÁN BẢO VỆ ĐỒ ÁN TỐT NGHIỆP
## Money Lover - Ứng dụng Quản lý Tài chính Cá nhân

---

## PHẦN 1: CÂU HỎI VỀ CODE & KỸ THUẬT

### 1. Tại sao chọn MVVM?

**Đáp án:**

MVVM (Model-View-ViewModel) là kiến trúc phù hợp với Android vì:

| So sánh | MVC | MVP | MVVM |
|---------|-----|-----|------|
| View | Tự xử lý logic UI | Implement Interface | Observe data binding |
| Controller/Presenter | Xử lý tương tác | Implement Interface | ViewModel không có reference đến View |
| Model | Data | Data | Data + Business Logic |
| Binding | Thủ công | Thủ công | **Tự động 2 chiều** |

**Ưu điểm của MVVM:**
- ✅ **Separation of Concerns**: UI tách biệt với logic nghiệp vụ
- ✅ **Lifecycle-aware**: ViewModel sống sót qua configuration change (xoay màn hình)
- ✅ **Testability**: ViewModel không có View reference → dễ test
- ✅ **Data Binding**: Tự động cập nhật UI khi data thay đổi

```kotlin
// Ví dụ: ViewModel lưu trữ StateFlow, View observe
class MainViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()
}

// View observe và cập nhật UI
lifecycleScope.launch {
    viewModel.uiState.collect { state ->
        // Cập nhật UI tự động
    }
}
```

---

### 2. Vai trò Repository trong MVVM

**Đáp án:**

```
┌─────────────┐     ┌─────────────┐     ┌─────────────┐     ┌─────────────┐
│   View      │────▶│  ViewModel  │────▶│ Repository  │────▶│    DAO      │
│ (Activity)  │◀────│             │◀────│             │◀────│  (Room)     │
└─────────────┘     └─────────────┘     └─────────────┘     └─────────────┘
```

**Repository đóng vai trò:**
1. **Single Source of Truth**: Là nơi duy nhất chứa logic truy cập dữ liệu
2. **Abstraction**: Che giấu chi tiết implementation (local DB, remote API)
3. **Data Transformation**: Xử lý data trước khi trả về ViewModel
4. **Testability**: Dễ dàng mock Repository khi test ViewModel

```kotlin
class TransactionRepository(private val transactionDao: TransactionDao) {

    // Xử lý format data trước khi trả về
    fun getByMonth(year: Int, month: Int): Flow<List<TransactionEntity>> {
        val monthStr = String.format("%02d", month)  // Thêm xử lý ở đây
        return transactionDao.getTransactionsByMonth(yearStr, monthStr)
    }
}
```

---

### 3. StateFlow vs LiveData vs Flow

**Đáp án:**

| Tiêu chí | Flow | StateFlow | LiveData |
|----------|------|-----------|----------|
| **Thread** | Cold/Hot | Hot | Hot |
| **Lifecycle-aware** | Thủ công | Thủ công | **Tự động** |
| **Initial value** | Không có | **Có thể có** | Có |
| **Null value** | Được | Được | Được |
| **Platform** | Kotlin only | Kotlin only | Android only |
| **Performance** | Tốt | Tốt | Tốt |

**Tại sao chọn StateFlow:**
```kotlin
// StateFlow: Luôn có giá trị mới nhất
private val _uiState = MutableStateFlow(MainUiState())
val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

// Cập nhật state
_uiState.value = _uiState.value.copy(isLoading = true)
```

**Trong ViewModel dùng StateFlow**, trong Repository dùng Flow để emit nhiều giá trị.

---

### 4. Khi nào dùng suspend function vs Flow?

**Đáp án:**

```kotlin
// SUSPEND FUNCTION: Khi cần lấy 1 kết quả DUY NHẤT
suspend fun getTransactionById(id: Long): TransactionEntity? {
    return transactionDao.getTransactionById(id)
}

// FLOW: Khi cần STREAM dữ liệu, tự động cập nhật khi data thay đổi
fun getAllTransactions(): Flow<List<TransactionEntity>> {
    return transactionDao.getAllTransactions()  // Mỗi khi DB thay đổi → emit list mới
}

// Ví dụ sử dụng:
viewModelScope.launch {
    // Flow: collect nhiều lần
    repository.getAllTransactions().collect { transactions ->
        adapter.submitList(transactions)  // Cập nhật UI khi data thay đổi
    }
}
```

---

### 5. Room Database hoạt động như thế nào?

**Đáp án:**

```
┌──────────────────────────────────────────────────────────────┐
│                     ROOM DATABASE                            │
├──────────────────────────────────────────────────────────────┤
│                                                              │
│  @Entity        @Dao              @Database                 │
│ ┌────────┐    ┌────────┐      ┌─────────────┐              │
│ │ Table  │◀───│ Query  │◀─────│   DAO       │              │
│ │        │───▶│ Insert │─────▶│             │              │
│ │        │◀───│ Delete │◀─────│             │              │
│ └────────┘    └────────┘      └─────────────┘              │
│     │                               │                        │
│     │         @TypeConverters        │                        │
│     └───────────────────────────────┘                        │
│                                                              │
│              Room Database (SQLite wrapper)                  │
└──────────────────────────────────────────────────────────────┘
```

**Room biên dịch annotation thành code:**
- `@Entity` → Tạo bảng trong SQLite
- `@Dao` → Tạo interface với các method query
- `@Database` → Tạo `AppDatabase` class

---

### 6. Migration trong Room

**Đáp án:**

```kotlin
@Database(
    entities = [TransactionEntity::class, AccountEntity::class, ...],
    version = 11,  // Tăng version khi thay đổi schema
    exportSchema = true  // Xuất file schema ra
)
abstract class AppDatabase : RoomDatabase() {

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val builder = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "money_lover_db"
                )

                // Hoặc thêm migration từ version cũ
                .addMigrations(
                    object : Migration(10, 11) {
                        override fun migrate(database: SupportSQLiteDatabase) {
                            // Thêm column mới, tạo bảng mới...
                            database.execSQL("ALTER TABLE accounts ADD COLUMN type TEXT")
                        }
                    }
                )

                builder.build().also { INSTANCE = it }
            }
        }
    }
}
```

---

### 7. Room Thread-safe?

**Đáp án:**

Room đã thread-safe nội bộ. Nhưng cần tuân thủ rules:

```kotlin
// ✅ ĐÚNG: Gọi từ coroutine (background thread)
suspend fun addTransaction() {
    withContext(Dispatchers.IO) {
        transactionDao.insert(transaction)
    }
}

// ✅ ĐÚNG: ViewModelScope tự động cancel khi ViewModel bị destroy
viewModelScope.launch {
    repository.insert(transaction)
}

// ❌ SAI: Gọi suspend function từ main thread mà không có Dispatcher.IO
fun onButtonClick() {
    transactionDao.insert(transaction)  // Crash!
}
```

---

### 8. Coroutine vs Thread truyền thống

**Đáp án:**

```kotlin
// THREAD TRUYỀN THỐNG: Tốn resource, khó quản lý
Thread {
    // Xử lý 1
    Thread.sleep(1000)
    // Xử lý 2
}.start()

// COROUTINE: Nhẹ, có thể tạo hàng ngàn coroutine
viewModelScope.launch(Dispatchers.IO) {
    // Xử lý 1 - tự động suspend khi await
    val result1 = async { apiCall1() }
    val result2 = async { apiCall2() }

    // Chờ cả 2 hoàn thành
    val combined = result1.await() + result2.await()
}
```

**Ưu điểm Coroutine:**
- ✅ Nhẹ hơn Thread (1 Thread có thể chứa hàng ngàn coroutine)
- ✅ Structured Concurrency: Tự động cancel khi lifecycle kết thúc
- ✅ Suspend thay vì blocking

---

### 9. launch vs async vs runBlocking

**Đáp án:**

```kotlin
// LAUNCH: Fire and forget - không return kết quả
viewModelScope.launch {
    repository.delete(transaction)  // Không quan tâm kết quả
}

// ASYNC: Trả về Deferred<T> - cần lấy kết quả
viewModelScope.launch {
    val deferred1 = async { api.getUser() }
    val deferred2 = async { api.getPosts() }
    val user = deferred1.await()
    val posts = deferred2.await()
}

// RUNBLOCKING: Chặn thread hiện tại - CHỈ DÙNG TRONG TEST
fun test() = runBlocking {
    val result = repository.getTransactionById(1)  // Chờ xong mới chạy tiếp
}
```

---

### 10. ViewModelScope vs lifecycleScope

**Đáp án:**

```kotlin
// ViewModelScope: Tự động cancel khi ViewModel bị destroy
class MyViewModel : ViewModel() {
    init {
        viewModelScope.launch {
            // Auto-canceled khi ViewModel cleared
        }
    }
}

// Activity/Fragment: lifecycleScope tự động cancel khi destroy
class MainActivity : AppCompatActivity() {
    override fun onCreate() {
        lifecycleScope.launch {
            // Auto-canceled khi Activity destroyed
        }
    }
}

// Khi nào dùng:
// - ViewModel → viewModelScope (chỉ sống trong ViewModel)
// - Activity/Fragment → lifecycleScope (theo lifecycle)
```

---

## PHẦN 2: CÂU HỎI VỀ NGHIỆP VỤ

### 11. 3 loại giao dịch (EXPENSE, INCOME, TRANSFER)

**Đáp án:**

| Loại | Mô tả | Ví dụ | fromAccount | toAccount |
|------|-------|-------|-------------|-----------|
| **EXPENSE** | Chi tiêu | Mua đồ ăn | null | "Ví Momo" |
| **INCOME** | Thu nhập | Nhận lương | null | "Ngân hàng" |
| **TRANSFER** | Chuyển khoản | Chuyển tiền | "Ví Momo" | "Ngân hàng" |

```kotlin
// Entity lưu trữ:
val type: String,              // "EXPENSE", "INCOME", "TRANSFER"
val fromAccount: String? = null,  // Tài khoản nguồn (TRANSFER)
val toAccount: String? = null,    // Tài khoản đích (EXPENSE, INCOME, TRANSFER)
```

---

### 12. Xử lý chuyển khoản (TRANSFER)

**Đáp án:**

Khi chuyển khoản từ tài khoản A sang tài khoản B:
1. Tạo 1 transaction với type = "TRANSFER"
2. Cập nhật số dư:
   - Tài khoản A: balance -= amount
   - Tài khoản B: balance += amount

```kotlin
// Trong AddTransactionActivity khi type = TRANSFER
suspend fun handleTransfer(fromAccount: Account, toAccount: Account, amount: Double) {
    // Cập nhật số dư 2 tài khoản
    val fromNewBalance = fromAccount.balance - amount
    val toNewBalance = toAccount.balance + amount

    accountRepository.updateBalance(fromAccount.id, fromNewBalance)
    accountRepository.updateBalance(toAccount.id, toNewBalance)
}
```

---

### 13. Xóa giao dịch có tự cập nhật số dư không?

**Đáp án:**

**HIỆN TẠI: KHÔNG tự động cập nhật số dư.**

```kotlin
// MainViewModel.deleteTransaction
fun deleteTransaction(transaction: TransactionEntity) {
    viewModelScope.launch {
        transactionRepository.delete(transaction)
        loadTransactions()  // Chỉ reload list, KHÔNG cập nhật số dư
    }
}
```

**Cần cải thiện:**
```kotlin
suspend fun deleteTransaction(transaction: TransactionEntity) {
    // Trừ lại số dư tài khoản
    when (transaction.type) {
        "EXPENSE" -> {
            val account = accountRepository.getAccountByName(transaction.toAccount!!)
            accountRepository.updateBalance(account.id, account.balance + transaction.amount)
        }
        "INCOME" -> {
            val account = accountRepository.getAccountByName(transaction.toAccount!!)
            accountRepository.updateBalance(account.id, account.balance - transaction.amount)
        }
        "TRANSFER" -> {
            // Cập nhật cả 2 tài khoản
            val fromAccount = accountRepository.getAccountByName(transaction.fromAccount!!)
            val toAccount = accountRepository.getAccountByName(transaction.toAccount!!)
            accountRepository.updateBalance(fromAccount.id, fromAccount.balance + transaction.amount)
            accountRepository.updateBalance(toAccount.id, toAccount.balance - transaction.amount)
        }
    }
    transactionRepository.delete(transaction)
}
```

---

### 14. Phân biệt các loại tài khoản

**Đáp án:**

| Type | Mô tả | Số dư | Ví dụ |
|------|-------|-------|-------|
| **cash** | Tiền mặt | Dương | Ví tiền |
| **bank** | Ngân hàng | Dương | Vietcombank |
| **e_wallet** | Ví điện tử | Dương | Momo, ZaloPay |
| **credit** | Thẻ tín dụng | **Âm** | Credit card |
| **debt** | Công nợ | Dương | Tiền nợ |
| **virtual** | Tài khoản ảo | Dương | Quỹ tiết kiệm |
| **investment** | Đầu tư | Dương | Chứng khoán |

```kotlin
// Tính tổng tài sản trong MainViewModel
val totalAssets = accounts.filter { account ->
    account.balance > 0 &&
    account.type != "debt" &&
    !account.isLiability &&
    !(account.type == "credit" && account.balance < 0)
}.sumOf { it.balance }
```

---

### 15. isLiability và tính tổng tài sản

**Đáp án:**

```kotlin
// Nợ phải trả = credit balance âm, debt, hoặc isLiability = true
val totalLiabilities = accounts.filter { account ->
    account.type == "credit" && account.balance < 0 ||
    account.type == "debt" ||
    account.isLiability
}.sumOf {
    if (it.type == "debt" || it.isLiability) it.balance
    else -it.balance // credit: -balance (vì balance đã âm)
}

// Tài sản ròng = Tài sản - Nợ
val netAssets = totalAssets - totalLiabilities
```

---

### 16. Ngân sách dùng SharedPreferences

**Đáp án:**

```kotlin
// BudgetRepository sử dụng SharedPreferences
class BudgetRepository(context: Context) {
    private val prefs = context.getSharedPreferences("budget_prefs", Context.MODE_PRIVATE)

    fun getMonthlyBudget(): Double {
        return prefs.getFloat("monthly_budget", 0f).toDouble()
    }

    fun setMonthlyBudget(amount: Double) {
        prefs.edit().putFloat("monthly_budget", amount.toFloat()).apply()
    }
}
```

**Tại sao SharedPreferences thay vì Room:**

| Tiêu chí | SharedPreferences | Room |
|----------|------------------|------|
| Dữ liệu nhỏ | ✅ Phù hợp | Overkill |
| Tốc độ đọc | ✅ Nhanh | Chậm hơn |
| Query phức tạp | ❌ Không hỗ trợ | ✅ SQL |
| Type safety | ❌ String/Int | ✅ Type-safe |

→ Chỉ lưu 1 giá trị budget → SharedPreferences là đủ.

---

### 17. Tính % ngân sách

**Đáp án:**

```kotlin
// Tính phần trăm sử dụng
val budget = currentMonthBudget  // 5,000,000
val spending = totalExpense        // 3,500,000
val percentage = if (budget > 0) (spending / budget) * 100 else 0.0

// Cảnh báo
when {
    percentage >= 100 -> showWarning("Đã vượt ngân sách!")
    percentage >= 80 -> showWarning("Ngân sách còn dưới 20%")
    else -> "Bình thường"
}
```

---

### 18. AlarmManager exact vs inexact

**Đáp án:**

```kotlin
// EXACT ALARM: Chạy đúng thời điểm (dùng cho reminder quan trọng)
if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
    if (alarmManager.canScheduleExactAlarms()) {
        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            triggerTime,
            pendingIntent
        )
    }
}

// INEXACT ALARM: Android tự tối ưu thời gian (tiết kiệm pin)
alarmManager.setAndAllowWhileIdle(
    AlarmManager.RTC_WAKEUP,
    triggerTime,
    pendingIntent
)
```

| Loại | Độ chính xác | Pin | Use case |
|------|-------------|-----|----------|
| **Exact** | ✅ Đúng giờ | ⚡ Tốn pin | Reminder thanh toán hóa đơn |
| **Inexact** | ⏰ Gần đúng | 🔋 Tiết kiệm | Thống kê hàng ngày |

---

### 19. BootReceiver xử lý restart

**Đáp án:**

```kotlin
// Khi thiết bị khởi động, hệ thống gửi BOOT_COMPLETED
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            // Reschedule tất cả reminders
            val reminders = database.reminderDao().getAllRemindersSync()
            reminders.forEach { reminder ->
                if (reminder.isEnabled) {
                    ReminderScheduler.schedule(context, reminder)
                }
            }
        }
    }
}
```

```xml
<!-- AndroidManifest.xml -->
<receiver android:name=".BootReceiver"
    android:enabled="true"
    android:exported="true">
    <intent-filter>
        <action android:name="android.intent.action.BOOT_COMPLETED"/>
    </intent-filter>
</receiver>
```

---

## PHẦN 3: CÂU HỎI VỀ BÁO CÁO

### 20. Sơ đồ ERD

**Đáp án:**

```
┌─────────────────┐       ┌─────────────────┐       ┌─────────────────┐
│      users      │       │    accounts     │       │  transactions   │
├─────────────────┤       ├─────────────────┤       ├─────────────────┤
│ id (PK)        │       │ id (PK)         │       │ id (PK)         │
│ name           │       │ user_id (FK)    │◀──────│ user_id (FK)    │
│ email          │◀──────│ name            │       │ category_id     │
│ password       │       │ balance         │       │ amount          │
│ gender         │       │ type            │       │ type            │
│ default_currency│       │ icon_res        │       │ from_account    │
└─────────────────┘       │ is_pinned      │       │ to_account      │
        │                 │ is_liability   │       │ date            │
        │                 │ color          │       │ note            │
        │                 └─────────────────┘       │ created_at      │
        │                         │                         │                 └─────────────────┘
        │                         │                         │
        │                         │                         ▼
        ▼                         ▼                 ┌─────────────────┐
┌─────────────────┐       ┌─────────────────┐       │   sms_transactions│
│   reminders     │       │   categories    │
├─────────────────┤       │ (hardcoded)     │
│ id (PK)         │       ├─────────────────┤
│ user_id (FK)    │◀──────│ id              │
│ title           │       │ name            │
│ amount          │       │ icon_res        │
│ frequency       │       │ type            │
│ is_enabled      │       │                 │
│ next_trigger    │       │                 │
└─────────────────┘       │                 │
                          └─────────────────┘
```

---

### 21. Hardcode userId = 1

**Đáp án:**

**Vấn đề:** Hiện tại app chỉ hỗ trợ 1 user, userId luôn = 1.

```kotlin
// TransactionEntity
val userId: Long = 1  // Hardcode!

// AccountEntity
val userId: Long = 1  // Hardcode!
```

**Để hỗ trợ multi-user:**

```kotlin
// 1. Lưu userId hiện tại vào SharedPreferences khi login
prefs.edit().putLong("current_user_id", userId).apply()

// 2. Lấy userId từ preferences khi tạo transaction
val currentUserId = prefs.getLong("current_user_id", 1)

// 3. Truy vấn theo userId
@Query("SELECT * FROM transactions WHERE userId = :userId ORDER BY date DESC")
fun getTransactionsByUser(userId: Long): Flow<List<TransactionEntity>>
```

---

### 22. Index trong database

**Đáp án:**

```kotlin
@Entity(
    tableName = "transactions",
    indices = [
        Index(value = ["date"]),           // Index cho lọc theo ngày
        Index(value = ["type"]),            // Index cho lọc theo loại
        Index(value = ["userId", "date"])   // Composite index
    ]
)
data class TransactionEntity(...)
```

**Tại sao cần Index:**
- ✅ Tăng tốc truy vấn WHERE, ORDER BY
- ❌ Chậm khi INSERT/UPDATE (phải cập nhật index)
- ⚖️ Chỉ nên index các cột thường xuyên query

---

### 23. 3 loại biểu đồ

**Đáp án:**

| Biểu đồ | Use case | Ví dụ |
|---------|----------|-------|
| **Pie/Donut** | Tỷ lệ phần trăm | Chi tiêu theo danh mục: 40% ăn uống, 30% di chuyển... |
| **Line** | Xu hướng theo thời gian | Chi tiêu 6 tháng gần nhất tăng/giảm thế nào |
| **Bar** | So sánh giá trị | So sánh chi tiêu các tháng trong năm |

```kotlin
// PieChartFragment: Hiển thị % theo category
// LineChartFragment: Hiển thị trend theo thời gian
// BarChartFragment: So sánh giá trị giữa các period
```

---

### 24. Custom View PieChartView

**Đáp án:**

```kotlin
class PieChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private var data: List<PieSlice> = emptyList()

    fun setData(slices: List<PieSlice>) {
        data = slices
        invalidate()  // Yêu cầu vẽ lại
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val centerX = width / 2f
        val centerY = height / 2f
        val radius = minOf(centerX, centerY)

        var startAngle = -90f  // Bắt đầu từ 12 giờ

        data.forEach { slice ->
            val sweepAngle = slice.percentage * 3.6f  // Convert % → degrees

            paint.color = slice.color
            canvas.drawArc(
                centerX - radius,
                centerY - radius,
                centerX + radius,
                centerY + radius,
                startAngle,
                sweepAngle,
                true,
                paint
            )

            startAngle += sweepAngle
        }
    }
}
```

---

### 25. Lưu trữ mật khẩu

**Đáp án:**

**HIỆN TẠI: Lưu plaintext (không an toàn)**

```kotlin
// UserEntity
val password: String  // Lưu trực tiếp!

// Login
suspend fun login(email: String, password: String): UserEntity? {
    return userDao.login(email, password)  // So sánh plaintext
}
```

**CẢI TIẾN: Hash password**

```kotlin
// Sử dụng BCrypt hoặc SHA-256
val hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt())

// Khi đăng nhập
val isValid = BCrypt.checkpw(inputPassword, storedHash)
```

**NÊN DÙNG: Firebase Authentication**

```kotlin
// Không lưu password local, dùng Firebase
FirebaseAuth.getInstance().signInWithEmailAndPassword(email, password)
    .addOnSuccessListener { /* login success */ }
```

---

### 26. Unit Test là gì?

**Đáp án:**

```kotlin
// Unit Test: Test từng "đơn vị" code (function, class)
class TransactionRepositoryTest {

    @Mock
    private lateinit var transactionDao: TransactionDao

    private lateinit var repository: TransactionRepository

    @Test
    fun `insert transaction should return id`() = runTest {
        // Given
        whenever(transactionDao.insert(any())).thenReturn(5L)

        // When
        val result = repository.insert(sampleTransaction)

        // Then
        assertEquals(5L, result)
        verify(transactionDao).insert(any())
    }
}
```

**Test đảm bảo:**
- ✅ Function hoạt động đúng
- ✅ Refactor không phá vỡ logic
- ✅ Tài liệu cho developer

---

### 27. Mockito hoạt động thế nào?

**Đáp án:**

```kotlin
// Mockito tạo "fake object" thay thế dependency thật
@Mock
private lateinit var transactionDao: TransactionDao

// Khi gọi transactionDao.insert(any()) → trả về giá trị ta định nghĩa
whenever(transactionDao.insert(any())).thenReturn(5L)

// Verify: Kiểm tra method có được gọi không
verify(transactionDao).insert(any())
```

---

### 28. Unit Test vs Integration Test

**Đáp án:**

| Tiêu chí | Unit Test | Integration Test |
|----------|-----------|------------------|
| **Phạm vi** | 1 class/function | Nhiều class kết hợp |
| **Database** | Mock | Thật (Test DB) |
| **Tốc độ** | ⚡ Nhanh | 🐢 Chậm hơn |
| **Độ tin cậy** | Cao | Phụ thuộc môi trường |

```kotlin
// UNIT TEST: Chỉ test Repository, DAO bị mock
@Test
fun testRepository() {
    val mockDao = mock(TransactionDao::class.java)
    val repo = TransactionRepository(mockDao)
    // Test repo logic
}

// INTEGRATION TEST: Test Repository + DAO thật
@Test
fun testRepositoryWithRealDao() {
    val db = Room.inMemoryDatabaseBuilder()
        .build()
    val repo = TransactionRepository(db.transactionDao())
    // Test với database thật
}
```

---

## PHẦN 4: TÌNH HUỐNG

### Tình huống 1: Bug số tiền lớn

**Đáp án:**

```kotlin
// Có thể crash nếu:
// 1. Format số quá dài trong TextView
// 2. Parse Double overflow

// Fix:
fun formatAmount(amount: Double): String {
    return try {
        val formatter = NumberFormat.getNumberInstance(Locale("vi", "VN"))
        formatter.maximumFractionDigits = 0
        formatter.format(amount) + "đ"
    } catch (e: Exception) {
        "Số tiền không hợp lệ"
    }
}

// Validate input
fun validateAmount(input: String): Boolean {
    val amount = input.replace(".", "").replace(",", "").toDoubleOrNull()
    return amount != null && amount > 0 && amount <= 999_999_999_999
}
```

---

### Tình huống 2: Firebase Sync

**Đáp án:**

```
┌─────────────┐         ┌─────────────┐         ┌─────────────┐
│   LOCAL     │◀───────▶│   FIREBASE  │◀───────▶│  OTHER      │
│   (Room)    │  sync   │   (Cloud)   │  sync   │  DEVICES    │
└─────────────┘         └─────────────┘         └─────────────┘
```

```kotlin
// Kiến trúc mới với Firebase
class FirebaseSyncManager {

    // Khi có thay đổi local → đẩy lên Firebase
    private val syncDown = Firebase.database.reference

    // Khi có thay đổi Firebase → cập nhật local
    private val syncUp = Firebase.database.reference
        .child("users")
        .child(userId)
        .child("transactions")
        .addChildEventListener { /* merge to Room */ }
}
```

---

### Tình huống 3: Performance với 10,000 transactions

**Đáp án:**

**Nguyên nhân:**
1. Load tất cả transactions cùng lúc
2. RecyclerView không dùng DiffUtil
3. Không phân trang (pagination)

**Giải pháp:**

```kotlin
// 1. Phân trang với Paging 3
@Query("SELECT * FROM transactions ORDER BY date DESC LIMIT :limit OFFSET :offset")
suspend fun getTransactionsPaged(limit: Int, offset: Int): List<TransactionEntity>

// 2. DiffUtil cho RecyclerView
class TransactionAdapter : ListAdapter<TransactionEntity, ...>(DiffCallback()) {

    class DiffCallback : DiffUtil.ItemCallback<TransactionEntity>() {
        override fun areItemsTheSame(old, new) = old.id == new.id
        override fun areContentsTheSame(old, new) = old == new
    }
}

// 3. Load theo tháng thay vì tất cả
@Query("""
    SELECT * FROM transactions
    WHERE date >= :startDate AND date <= :endDate
    ORDER BY date DESC
""")
suspend fun getTransactionsByDateRange(startDate: Long, endDate: Long): List<TransactionEntity>
```

---

### Tình huống 4: Multi-user

**Đáp án:**

```
┌─────────────────────────────────────────────────────┐
│                  MULTI-USER DESIGN                  │
├─────────────────────────────────────────────────────┤
│                                                      │
│   SharedPreferences: userId hiện tại               │
│   ┌─────────────────────────────────────────────┐   │
│   │ current_user_id = 5                          │   │
│   │ Remember me token = "abc123..."             │   │
│   └─────────────────────────────────────────────┘   │
│                                                      │
│   Room queries theo userId:                         │
│   @Query("SELECT * FROM transactions WHERE userId = :userId")
│   fun getTransactionsByUser(userId: Long): Flow     │
│                                                      │
│   SharedPreferences cho session:                    │
│   - Lưu trạng thái login                           │
│   - Lưu userId hiện tại                            │
│   - Auto logout khi hết session                     │
│                                                      │
└─────────────────────────────────────────────────────┘
```

---

## GỢI Ý CHUẨN BỊ

| Thứ tự ưu tiên | Câu hỏi nên ôn kỹ |
|----------------|-------------------|
| 🔴 Quan trọng | MVVM, Room, Coroutines |
| 🟡 Khá quan trọng | AlarmManager, SharedPreferences vs Room |
| 🟢 Ít quan trọng | Custom View, Export data |

---

**Chúc các bạn bảo vệ đồ án thành công! 🎓**
