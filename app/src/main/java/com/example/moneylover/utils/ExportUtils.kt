package com.example.moneylover.utils

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Environment
import androidx.core.content.FileProvider
import com.example.moneylover.data.model.TransactionEntity
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Utils để xuất dữ liệu giao dịch ra file
 * Hỗ trợ xuất JSON, CSV, TXT và chia sẻ file
 */
object ExportUtils {

    // Các formatter cho ngày tháng
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale("vi", "VN"))
    private val fileNameDateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale("vi", "VN"))

    /**
     * Xuất danh sách giao dịch ra file JSON
     */
    fun exportToJson(context: Context, transactions: List<TransactionEntity>): File? {
        return try {
            val jsonArray = JSONArray()
            transactions.forEach { tx ->
                val obj = JSONObject().apply {
                    put("id", tx.id)
                    put("categoryId", tx.categoryId)
                    put("categoryName", tx.categoryName)
                    put("amount", tx.amount)
                    put("type", tx.type)
                    put("fromAccount", tx.fromAccount ?: "")
                    put("toAccount", tx.toAccount ?: "")
                    put("date", dateFormat.format(Date(tx.date)))
                    put("note", tx.note ?: "")
                    put("createdAt", dateFormat.format(Date(tx.createdAt)))
                }
                jsonArray.put(obj)
            }

            val rootObj = JSONObject().apply {
                put("exportDate", dateFormat.format(Date()))
                put("totalTransactions", transactions.size)
                put("transactions", jsonArray)
            }

            val fileName = "MoneyLover_${fileNameDateFormat.format(Date())}.json"
            val file = File(getExportDir(context), fileName)
            FileWriter(file).use { it.write(rootObj.toString(2)) }
            file
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Xuất danh sách giao dịch ra file CSV
     */
    fun exportToCsv(context: Context, transactions: List<TransactionEntity>): File? {
        return try {
            val fileName = "MoneyLover_${fileNameDateFormat.format(Date())}.csv"
            val file = File(getExportDir(context), fileName)
            FileWriter(file).use { writer ->
                // Header
                writer.append("ID,Loại,Ngày,Danh mục,Số tiền,Tài khoản nguồn,Tài khoản đích,Ghi chú\n")

                // Data
                transactions.forEach { tx ->
                    writer.append("${tx.id},")
                    writer.append("${getTypeName(tx.type)},")
                    writer.append("${dateFormat.format(Date(tx.date))},")
                    writer.append("\"${tx.categoryName}\",")
                    writer.append("${tx.amount.toLong()},")
                    writer.append("${tx.fromAccount ?: ""},")
                    writer.append("${tx.toAccount ?: ""},")
                    writer.append("\"${tx.note ?: ""}\"\n")
                }
            }
            file
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Xuất danh sách giao dịch ra file TXT (báo cáo)
     */
    fun exportToTxt(context: Context, transactions: List<TransactionEntity>): File? {
        return try {
            val fileName = "MoneyLover_${fileNameDateFormat.format(Date())}.txt"
            val file = File(getExportDir(context), fileName)
            FileWriter(file).use { writer ->
                writer.appendLine("=".repeat(50))
                writer.appendLine("         BÁO CÁO GIAO DỊCH MONEYLOVER")
                writer.appendLine("=".repeat(50))
                writer.appendLine("Ngày xuất: ${dateFormat.format(Date())}")
                writer.appendLine("Tổng giao dịch: ${transactions.size}")
                writer.appendLine("")

                // Tính tổng thu/chi
                val totalIncome = transactions.filter { it.type == "INCOME" }.sumOf { it.amount }
                val totalExpense = transactions.filter { it.type == "EXPENSE" }.sumOf { it.amount }
                writer.appendLine("Tổng thu nhập: ${formatCurrency(totalIncome)}")
                writer.appendLine("Tổng chi tiêu: ${formatCurrency(totalExpense)}")
                writer.appendLine("Chênh lệch: ${formatCurrency(totalIncome - totalExpense)}")
                writer.appendLine("")
                writer.appendLine("-".repeat(50))

                // Danh sách giao dịch
                transactions.sortedByDescending { it.date }.forEach { tx ->
                    val typeSymbol = when (tx.type) {
                        "INCOME" -> "[+]"
                        "EXPENSE" -> "[-]"
                        "TRANSFER" -> "[→]"
                        else -> "[?]"
                    }
                    writer.appendLine("${dateFormat.format(Date(tx.date))} $typeSymbol ${tx.categoryName}")
                    writer.appendLine("   Số tiền: ${formatCurrency(tx.amount)}")
                    if (tx.fromAccount != null || tx.toAccount != null) {
                        writer.appendLine("   Tài khoản: ${tx.fromAccount ?: ""} → ${tx.toAccount ?: ""}")
                    }
                    if (!tx.note.isNullOrBlank()) {
                        writer.appendLine("   Ghi chú: ${tx.note}")
                    }
                    writer.appendLine("")
                }

                writer.appendLine("=".repeat(50))
                writer.appendLine("              HẾT BÁO CÁO")
                writer.appendLine("=".repeat(50))
            }
            file
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Chia sẻ file qua Intent
     */
    fun shareFile(context: Context, file: File) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = when {
                file.name.endsWith(".json") -> "application/json"
                file.name.endsWith(".csv") -> "text/csv"
                else -> "text/plain"
            }
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Chia sẻ file"))
    }

    // Lấy thư mục lưu file export
    private fun getExportDir(context: Context): File {
        val dir = File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "MoneyLover")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    // Chuyển type code sang tên
    private fun getTypeName(type: String): String = when (type) {
        "INCOME" -> "Thu nhập"
        "EXPENSE" -> "Chi tiêu"
        "TRANSFER" -> "Chuyển khoản"
        else -> type
    }

    // Format số tiền VND
    @SuppressLint("DefaultLocale")
    private fun formatCurrency(amount: Double): String {
        return String.format("%,.0f đ", amount).replace(",", ".")
    }
}
