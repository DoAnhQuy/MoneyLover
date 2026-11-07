package com.example.moneylover.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.moneylover.MainActivity
import com.example.moneylover.R

/**
 * Helper để quản lý thông báo
 * Tạo và hiển thị thông báo giao dịch
 */
object NotificationHelper {

    private const val CHANNEL_ID = "money_lover_transactions"
    private const val CHANNEL_NAME = "Giao dịch"
    private const val CHANNEL_DESC = "Thông báo giao dịch"

    // Tạo notification channel (Android 8.0+)
    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = CHANNEL_DESC
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 250, 250, 250)
            }
            val manager = context.getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    // Hiển thị thông báo giao dịch
    fun showTransactionNotification(
        context: Context,
        type: String, // "INCOME", "EXPENSE", "TRANSFER"
        categoryName: String,
        amount: Double
    ) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Tạo nội dung thông báo theo loại giao dịch
        val (icon, title, content, color) = when (type) {
            "INCOME" -> Quad(
                android.R.drawable.ic_menu_upload,
                "Thu nhập",
                "+${formatCurrency(amount)} vào $categoryName",
                0xFF4CAF50.toInt()
            )
            "EXPENSE" -> Quad(
                android.R.drawable.ic_menu_delete,
                "Chi tiêu",
                "-${formatCurrency(amount)} từ $categoryName",
                0xFFF44336.toInt()
            )
            "TRANSFER" -> Quad(
                android.R.drawable.ic_menu_share,
                "Chuyển khoản",
                "Chuyển ${formatCurrency(amount)}",
                0xFF2196F3.toInt()
            )
            else -> Quad(
                android.R.drawable.ic_dialog_info,
                "Giao dịch",
                "${formatCurrency(amount)} - $categoryName",
                0xFF9E9E9E.toInt()
            )
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(icon)
            .setContentTitle(title)
            .setContentText(content)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setColor(color)
            .build()

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(System.currentTimeMillis().toInt(), notification)
    }

    // Format số tiền VND
    private fun formatCurrency(amount: Double): String {
        return String.format("%,.0fđ", amount).replace(",", ".")
    }

    // Data class cho 4 giá trị
    private data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
}
