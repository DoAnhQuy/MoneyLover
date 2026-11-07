package com.example.moneylover

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.moneylover.data.database.AppDatabase
import com.example.moneylover.data.model.ReminderEntity
import com.example.moneylover.data.repository.ReminderRepository
import kotlinx.coroutines.runBlocking
import java.util.Calendar

class ReminderReceiver : BroadcastReceiver() {

    companion object {
        const val CHANNEL_ID = "reminder_channel"
        const val CHANNEL_NAME = "Lời nhắc nhở"
        const val EXTRA_REMINDER_ID = "reminder_id"
        const val EXTRA_REMINDER_NAME = "reminder_name"
        const val EXTRA_REMINDER_MESSAGE = "reminder_message"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val reminderId = intent.getLongExtra(EXTRA_REMINDER_ID, 0)
        val reminderName = intent.getStringExtra(EXTRA_REMINDER_NAME) ?: "Nhắc nhở"
        val reminderMessage = intent.getStringExtra(EXTRA_REMINDER_MESSAGE) ?: "Đã đến lúc!"

        // Hiển thị notification
        showNotification(context, reminderId.toInt(), reminderName, reminderMessage)

        // Reschedule cho lần tiếp theo
        if (reminderId > 0) {
            rescheduleNext(context, reminderId)
        }
    }

    private fun showNotification(context: Context, notificationId: Int, title: String, message: String) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Tạo notification channel cho Android 8.0+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Thông báo lời nhắc nhở"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 500, 200, 500)
            }
            notificationManager.createNotificationChannel(channel)
        }

        // Intent để mở app khi click notification
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .build()

        notificationManager.notify(notificationId, notification)
    }

    private fun rescheduleNext(context: Context, reminderId: Long) {
        try {
            val database = AppDatabase.getDatabase(context)
            val repository = ReminderRepository(database.reminderDao())

            // Lấy reminder từ database
            val reminder = runBlocking {
                repository.getReminderById(reminderId)
            }

            if (reminder != null && reminder.isEnabled) {
                // Tính lại thời gian cho lần tiếp theo
                val nextTime = calculateNextTime(reminder)

                // Cập nhật startDate và reschedule
                val updatedReminder = reminder.copy(startDate = nextTime)
                ReminderScheduler.scheduleReminder(context, updatedReminder)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun calculateNextTime(reminder: ReminderEntity): Long {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, reminder.timeHour)
            set(Calendar.MINUTE, reminder.timeMinute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        when (reminder.frequency) {
            "DAILY" -> calendar.add(Calendar.DAY_OF_MONTH, 1)
            "WEEKLY" -> calendar.add(Calendar.WEEK_OF_YEAR, 1)
            "MONTHLY" -> calendar.add(Calendar.MONTH, 1)
        }

        return calendar.timeInMillis
    }
}
