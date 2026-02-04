package com.vxncius.authx.logic
import android.content.Context
import android.net.Uri
import com.vxncius.authx.data.VaultItem
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
object CsvHandler {
    fun exportToCsv(context: Context, uri: Uri, items: List<VaultItem>): Boolean {
        return try {
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                OutputStreamWriter(outputStream).use { writer ->
                    writer.write("Title,Username,Website,Password,Notes,TotpSecret,Digits,Period,Algorithm\n")
                    items.forEach { item ->
                        val line = listOf(
                            escapeCsv(item.title),
                            escapeCsv(item.username),
                            escapeCsv(item.websiteUrl),
                            escapeCsv(item.password),
                            escapeCsv(item.notes),
                            escapeCsv(item.totpSecret ?: ""),
                            item.digits.toString(),
                            item.period.toString(),
                            escapeCsv(item.algorithm)
                        ).joinToString(",")
                        writer.write("$line\n")
                    }
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
    fun importFromCsv(context: Context, uri: Uri): List<VaultItem> {
        val items = mutableListOf<VaultItem>()
        try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                BufferedReader(InputStreamReader(inputStream)).use { reader ->
                    val header = reader.readLine()
                    var line: String? = reader.readLine()
                    while (line != null) {
                        val parts = parseCsvLine(line)
                        if (parts.isNotEmpty()) {
                            items.add(
                                VaultItem(
                                    title = parts.getOrNull(0) ?: "Imported Item",
                                    username = parts.getOrNull(2) ?: "",
                                    websiteUrl = parts.getOrNull(1) ?: "",
                                    password = parts.getOrNull(3) ?: "",
                                    notes = parts.getOrNull(4) ?: "",
                                    totpSecret = parts.getOrNull(5)?.ifBlank { null },
                                    digits = parts.getOrNull(6)?.toIntOrNull() ?: 6,
                                    period = parts.getOrNull(7)?.toIntOrNull() ?: 30,
                                    algorithm = parts.getOrNull(8) ?: "SHA1"
                                )
                            )
                        }
                        line = reader.readLine()
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return items
    }
    private fun escapeCsv(value: String): String {
        if (!value.contains(",") && !value.contains("\"") && !value.contains("\n")) {
            return value
        }
        return "\"" + value.replace("\"", "\"\"") + "\""
    }
    private fun parseCsvLine(line: String): List<String> {
        val result = mutableListOf<String>()
        var current = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < line.length) {
            val c = line[i]
            if (c == '\"') {
                if (inQuotes && i + 1 < line.length && line[i + 1] == '\"') {
                    current.append('\"')
                    i++
                } else {
                    inQuotes = !inQuotes
                }
            } else if (c == ',' && !inQuotes) {
                result.add(current.toString())
                current = StringBuilder()
            } else {
                current.append(c)
            }
            i++
        }
        result.add(current.toString())
        return result
    }
}

