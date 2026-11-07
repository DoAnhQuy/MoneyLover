package com.example.moneylover

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.Toast
import com.example.moneylover.data.model.ReminderEntity

object ReminderScheduler {

    fun scheduleReminder(context: Context, reminder: ReminderEntity) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra(ReminderReceiver.EXTRA_REMINDER_ID, reminder.id)
            putExtra(ReminderReceiver.EXTRA_REMINDER_NAME, reminder.name)
            putExtra(ReminderReceiver.EXTRA_REMINDER_MESSAGE, reminder.message)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            reminder.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Tính thời gian nhắc nhở
        val calendar = java.util.Calendar.getInstance().apply {
            timeInMillis = reminder.startDate
            set(java.util.Calendar.HOUR_OF_DAY, reminder.timeHour)
            set(java.util.Calendar.MINUTE, reminder.timeMinute)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }

        // Nếu thời gian đã qua hôm nay, chuyển sang ngày mai
        if (calendar.timeInMillis <= System.currentTimeMillis()) {
            calendar.add(java.util.Calendar.DAY_OF_MONTH, 1)
        }

        // Kiểm tra quyền exact alarm (Android 12+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!alarmManager.canScheduleExactAlarms()) {
                // Không có quyền, thử dùng inexact alarm
                try {
                    scheduleInexactAlarm(context, reminder, calendar, pendingIntent)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                return
            }
        }

        // Hủy alarm cũ trước
        alarmManager.cancel(pendingIntent)

        try {
            if (reminder.isEnabled) {
                // Sử dụng setExactAndAllowWhileIdle cho độ chính xác cao
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        calendar.timeInMillis,
                        pendingIntent
                    )
                } else {
                    alarmManager.setExact(
                        AlarmManager.RTC_WAKEUP,
                        calendar.timeInMillis,
                        pendingIntent
                    )
                }

                // Schedule lần tiếp theo sau khi notification được hiển thị
                // (ReminderReceiver sẽ tự reschedule cho lần sau)
            }
        } catch (e: SecurityException) {
            // Fallback to inexact alarm
            scheduleInexactAlarm(context, reminder, calendar, pendingIntent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun scheduleInexactAlarm(
        context: Context,
        reminder: ReminderEntity,
        calendar: java.util.Calendar,
        pendingIntent: PendingIntent
    ) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        if (!reminder.isEnabled) {
            return
        }

        // Lên lịch với inexact alarm
        val interval = when (reminder.frequency) {
            "DAILY" -> AlarmManager.INTERVAL_DAY
            "WEEKLY" -> AlarmManager.INTERVAL_DAY * 7
            "MONTHLY" -> AlarmManager.INTERVAL_DAY * 30
            else -> AlarmManager.INTERVAL_DAY
        }

        alarmManager.setInexactRepeating(
            AlarmManager.RTC_WAKEUP,
            calendar.timeInMillis,
            interval,
            pendingIntent
        )
    }

    fun cancelReminder(context: Context, reminderId: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val intent = Intent(context, ReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            reminderId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        alarmManager.cancel(pendingIntent)
    }
}
