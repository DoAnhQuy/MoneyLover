package com.example.moneylover.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.moneylover.data.model.AccountEntity
import com.example.moneylover.data.model.ReminderEntity
import com.example.moneylover.data.model.TransactionEntity
import com.example.moneylover.data.model.UserEntity

/**
 * Cấu hình Database chính của ứng dụng
 * Sử dụng Room Database để lưu trữ dữ liệu cục bộ
 */
@Database(
    entities = [TransactionEntity::class, AccountEntity::class, UserEntity::class, ReminderEntity::class],
    version = 11,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    // Các DAO để truy cập dữ liệu
    abstract fun transactionDao(): TransactionDao // DAO cho bảng giao dịch
    abstract fun accountDao(): AccountDao         // DAO cho bảng tài khoản
    abstract fun userDao(): UserDao               // DAO cho bảng người dùng
    abstract fun reminderDao(): ReminderDao       // DAO cho bảng lời nhắc

    companion object {
        // Biến singleton để đảm bảo chỉ có 1 instance database
        @Volatile
        private var INSTANCE: AppDatabase? = null

        // Migration từ version 10 -> 11 (xóa bảng sms_transaction cũ)
        private val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("DROP TABLE IF EXISTS sms_transaction")
            }
        }

        /**
         * Lấy instance database (Singleton pattern)
         * @param context Context của ứng dụng
         * @return AppDatabase instance
         */
        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "app_database"
                )
                .addMigrations(MIGRATION_10_11)
                .build()

                INSTANCE = instance
                instance
            }
        }
    }
}
