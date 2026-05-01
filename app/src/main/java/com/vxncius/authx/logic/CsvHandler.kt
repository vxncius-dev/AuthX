package com.vxncius.authx.logic

import android.content.Context
import android.net.Uri
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import com.vxncius.authx.data.VaultItem
import java.io.BufferedReader
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

object CsvHandler {
    private const val KEY_ALIAS = "authx_export_key"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private val encryptedMagic = "AUTHX_ENCRYPTED_V1\n".toByteArray(StandardCharsets.UTF_8)

    fun exportToCsv(context: Context, uri: Uri, items: List<VaultItem>): Boolean {
        return try {
            val plainCsv = buildCsv(items).toByteArray(StandardCharsets.UTF_8)
            val encryptedPayload = encrypt(plainCsv)
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                outputStream.write(encryptedMagic)
                outputStream.write(encryptedPayload)
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun importFromCsv(context: Context, uri: Uri): List<VaultItem> {
        return try {
            val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: return emptyList()
            val csvBytes = if (bytes.startsWith(encryptedMagic)) {
                decrypt(bytes.copyOfRange(encryptedMagic.size, bytes.size))
            } else {
                bytes
            }
            parseCsv(String(csvBytes, StandardCharsets.UTF_8))
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    private fun buildCsv(items: List<VaultItem>): String {
        return buildString {
            append("Title,Username,Website,Password,Notes,TotpSecret,Digits,Period,Algorithm,Type\n")
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
                    escapeCsv(item.algorithm),
                    escapeCsv(item.type)
                ).joinToString(",")
                append(line).append('\n')
            }
        }
    }

    private fun parseCsv(csv: String): List<VaultItem> {
        val items = mutableListOf<VaultItem>()
        BufferedReader(InputStreamReader(ByteArrayInputStream(csv.toByteArray(StandardCharsets.UTF_8)))).use { reader ->
            reader.readLine()
            var line: String? = reader.readLine()
            while (line != null) {
                val parts = parseCsvLine(line)
                if (parts.isNotEmpty()) {
                    items.add(
                        VaultItem(
                            title = parts.getOrNull(0) ?: "Imported Item",
                            username = parts.getOrNull(1) ?: "",
                            websiteUrl = parts.getOrNull(2) ?: "",
                            password = parts.getOrNull(3) ?: "",
                            notes = parts.getOrNull(4) ?: "",
                            totpSecret = parts.getOrNull(5)?.ifBlank { null },
                            digits = parts.getOrNull(6)?.toIntOrNull() ?: 6,
                            period = parts.getOrNull(7)?.toIntOrNull() ?: 30,
                            algorithm = parts.getOrNull(8) ?: "SHA1",
                            type = parts.getOrNull(9)?.ifBlank { "LOGIN" } ?: "LOGIN"
                        )
                    )
                }
                line = reader.readLine()
            }
        }
        return items
    }

    private fun encrypt(plainBytes: ByteArray): ByteArray {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateSecretKey())
        val encrypted = cipher.doFinal(plainBytes)
        return ByteArrayOutputStream().use { output ->
            output.write(cipher.iv.size)
            output.write(cipher.iv)
            output.write(encrypted)
            output.toByteArray()
        }
    }

    private fun decrypt(payload: ByteArray): ByteArray {
        val ivSize = payload.first().toInt()
        val iv = payload.copyOfRange(1, 1 + ivSize)
        val encrypted = payload.copyOfRange(1 + ivSize, payload.size)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, getOrCreateSecretKey(), GCMParameterSpec(128, iv))
        return cipher.doFinal(encrypted)
    }

    private fun getOrCreateSecretKey(): SecretKey {
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (keyStore.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }

        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        val spec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setRandomizedEncryptionRequired(true)
            .build()
        keyGenerator.init(spec)
        return keyGenerator.generateKey()
    }

    private fun ByteArray.startsWith(prefix: ByteArray): Boolean {
        if (size < prefix.size) return false
        return prefix.indices.all { this[it] == prefix[it] }
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
