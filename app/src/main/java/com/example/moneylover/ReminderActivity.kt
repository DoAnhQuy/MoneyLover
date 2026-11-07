package com.example.moneylover

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.moneylover.data.database.AppDatabase
import com.example.moneylover.data.model.ReminderEntity
import com.example.moneylover.data.repository.ReminderRepository
import com.example.moneylover.databinding.ActivityReminderBinding
import com.example.moneylover.databinding.ActivityReminderEditBinding
import com.example.moneylover.ui.adapter.ReminderAdapter
import com.google.android.material.bottomsheet.BottomSheetDialog
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class ReminderActivity : AppCompatActivity() {

    private lateinit var binding: ActivityReminderBinding
    private lateinit var repository: ReminderRepository
    private lateinit var adapter: ReminderAdapter

    // Edit screen binding
    private var editBinding: ActivityReminderEditBinding? = null
    private var editDialog: BottomSheetDialog? = null
    private var currentReminder: ReminderEntity? = null
    
    // Form data
    private var selectedDate: Calendar = Calendar.getInstance()
    private var selectedFrequency: String = "DAILY"

    private val dateFormatter = SimpleDateFormat("d 'thg' M, yyyy", Locale("vi", "VN"))

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityReminderBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val database = AppDatabase.getDatabase(this)
        repository = ReminderRepository(database.reminderDao())

        setupUI()
        observeReminders()
    }

    private fun setupUI() {
        // Nút quay lại
        binding.btnBack.setOnClickListener {
            finish()
        }

        // Nút thêm mới
        binding.btnAdd.setOnClickListener {
            showAddEditScreen(null)
        }

        // Setup RecyclerView
        adapter = ReminderAdapter(
            onItemClick = { reminder ->
                showAddEditScreen(reminder)
            },
            onSwitchChanged = { reminder, enabled ->
                lifecycleScope.launch {
                    repository.setEnabled(reminder.id, enabled)
                    // Cập nhật alarm
                    val updatedReminder = reminder.copy(isEnabled = enabled)
                    if (enabled) {
                        ReminderScheduler.scheduleReminder(this@ReminderActivity, updatedReminder)
                    } else {
                        ReminderScheduler.cancelReminder(this@ReminderActivity, reminder.id)
                    }
                }
            },
            onItemLongClick = { reminder ->
                showDeleteConfirmation(reminder)
            }
        )

        binding.rvReminders.layoutManager = LinearLayoutManager(this)
        binding.rvReminders.adapter = adapter
    }

    private fun observeReminders() {
        lifecycleScope.launch {
            repository.getAllReminders().collectLatest { reminders ->
                if (reminders.isEmpty()) {
                    binding.rvReminders.visibility = View.GONE
                    binding.layoutEmptyState.visibility = View.VISIBLE
                } else {
                    binding.rvReminders.visibility = View.VISIBLE
                    binding.layoutEmptyState.visibility = View.GONE
                    adapter.submitList(reminders)
                }
            }
        }
    }

    private fun showAddEditScreen(reminder: ReminderEntity?) {
        currentReminder = reminder
        editDialog = BottomSheetDialog(this)
        editBinding = ActivityReminderEditBinding.inflate(layoutInflater)
        editDialog?.setContentView(editBinding!!.root)

        val binding = editBinding!!

        // Setup UI
        if (reminder == null) {
            // Mode: Thêm mới
            binding.txtTitle.text = "Thêm lời nhắc"
            binding.btnSave.text = "Thêm"
            resetForm()
        } else {
            // Mode: Sửa/Xóa
            binding.txtTitle.text = "Sửa lời nhắc"
            binding.btnSave.text = "Sửa"
            populateForm(reminder)
        }

        // Nút Hủy
        binding.btnCancel.setOnClickListener {
            editDialog?.dismiss()
        }

        // Nút Lưu/Thêm/Sửa
        binding.btnSave.setOnClickListener {
            saveReminder()
        }

        // Chọn tần suất
        binding.layoutFrequency.setOnClickListener {
            showFrequencyPicker()
        }

        // Chọn ngày
        binding.layoutStartDate.setOnClickListener {
            showDatePicker()
        }

        // Chọn giờ
        binding.layoutTime.setOnClickListener {
            showTimePicker()
        }

        editDialog?.show()
    }

    private fun resetForm() {
        val binding = editBinding ?: return
        binding.editName.setText("")
        binding.editMessage.setText("")
        binding.editNote.setText("")
        selectedFrequency = "DAILY"
        binding.txtFrequency.text = "Hàng ngày"
        selectedDate = Calendar.getInstance()
        binding.txtStartDate.text = dateFormatter.format(selectedDate.time)
        binding.txtTime.text = "08:00"
    }

    private fun populateForm(reminder: ReminderEntity) {
        val binding = editBinding ?: return
        binding.editName.setText(reminder.name)
        binding.editMessage.setText(reminder.message)
        binding.editNote.setText(reminder.note ?: "")

        selectedFrequency = reminder.frequency
        binding.txtFrequency.text = when (reminder.frequency) {
            "DAILY" -> "Hàng ngày"
            "WEEKLY" -> "Hàng tuần"
            "MONTHLY" -> "Hàng tháng"
            else -> reminder.frequency
        }

        selectedDate.timeInMillis = reminder.startDate
        binding.txtStartDate.text = dateFormatter.format(selectedDate.time)

        binding.txtTime.text = String.format("%02d:%02d", reminder.timeHour, reminder.timeMinute)
        binding.switchEnabled.isChecked = reminder.isEnabled
    }

    private fun showFrequencyPicker() {
        val options = arrayOf("Hàng ngày", "Hàng tuần", "Hàng tháng")
        val values = arrayOf("DAILY", "WEEKLY", "MONTHLY")

        android.app.AlertDialog.Builder(this)
            .setTitle("Chọn tần suất")
            .setItems(options) { _, which ->
                selectedFrequency = values[which]
                editBinding?.txtFrequency?.text = options[which]
            }
            .show()
    }

    private fun showDatePicker() {
        val picker = DatePickerDialog(
            this,
            { _, year, month, day ->
                selectedDate.set(year, month, day)
                editBinding?.txtStartDate?.text = dateFormatter.format(selectedDate.time)
            },
            selectedDate.get(Calendar.YEAR),
            selectedDate.get(Calendar.MONTH),
            selectedDate.get(Calendar.DAY_OF_MONTH)
        )
        picker.show()
    }

    private fun showTimePicker() {
        val hour = selectedDate.get(Calendar.HOUR_OF_DAY)
        val minute = selectedDate.get(Calendar.MINUTE)

        TimePickerDialog(
            this,
            { _, h, m ->
                selectedDate.set(Calendar.HOUR_OF_DAY, h)
                selectedDate.set(Calendar.MINUTE, m)
                editBinding?.txtTime?.text = String.format("%02d:%02d", h, m)
            },
            hour,
            minute,
            true
        ).show()
    }

    private fun showDeleteConfirmation(reminder: ReminderEntity) {
        AlertDialog.Builder(this)
            .setTitle("Xóa lời nhắc")
            .setMessage("Bạn có muốn xóa lời nhắc \"${reminder.name}\" không?")
            .setPositiveButton("Xóa") { _, _ ->
                lifecycleScope.launch {
                    repository.deleteById(reminder.id)
                    ReminderScheduler.cancelReminder(this@ReminderActivity, reminder.id)
                    Toast.makeText(this@ReminderActivity, "Đã xóa lời nhắc", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Hủy", null)
            .show()
    }

    private fun saveReminder() {
        val binding = editBinding ?: return

        val name = binding.editName.text.toString().trim()
        val message = binding.editMessage.text.toString().trim()
        val note = binding.editNote.text.toString().trim()
        val isEnabled = binding.switchEnabled.isChecked

        if (name.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập tên lời nhắc", Toast.LENGTH_SHORT).show()
            return
        }

        if (message.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập lời nhắc nhớ", Toast.LENGTH_SHORT).show()
            return
        }

        val timeHour = selectedDate.get(Calendar.HOUR_OF_DAY)
        val timeMinute = selectedDate.get(Calendar.MINUTE)

        lifecycleScope.launch {
            if (currentReminder == null) {
                // Thêm mới - mặc định bật
                val reminder = ReminderEntity(
                    name = name,
                    message = message,
                    frequency = selectedFrequency,
                    startDate = selectedDate.timeInMillis,
                    timeHour = timeHour,
                    timeMinute = timeMinute,
                    note = note.ifEmpty { null },
                    isEnabled = isEnabled
                )
                val id = repository.insert(reminder)
                // Lên lịch thông báo
                val savedReminder = reminder.copy(id = id)
                ReminderScheduler.scheduleReminder(this@ReminderActivity, savedReminder)
                Toast.makeText(this@ReminderActivity, "Đã thêm lời nhắc", Toast.LENGTH_SHORT).show()
            } else {
                // Sửa
                val reminder = currentReminder!!.copy(
                    name = name,
                    message = message,
                    frequency = selectedFrequency,
                    startDate = selectedDate.timeInMillis,
                    timeHour = timeHour,
                    timeMinute = timeMinute,
                    note = note.ifEmpty { null }
                )
                repository.update(reminder)
                // Cập nhật alarm
                ReminderScheduler.scheduleReminder(this@ReminderActivity, reminder)
                Toast.makeText(this@ReminderActivity, "Đã cập nhật lời nhắc", Toast.LENGTH_SHORT).show()
            }

            editDialog?.dismiss()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        editDialog?.dismiss()
    }
}
