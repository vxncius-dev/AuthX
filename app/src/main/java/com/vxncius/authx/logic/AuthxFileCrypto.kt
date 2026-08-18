package com.vxncius.authx.logic

import com.vxncius.authx.data.VaultItem
import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets
import java.security.SecureRandom
import javax.crypto.AEADBadTagException
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

class InvalidFileException : Exception("Invalid AUTHX file")
class WrongPasswordException : Exception("Wrong password")

/**
 * Pure, JVM-testable implementation of the `.authx` backup format.
 *
 * V2 format (`AUTHX_ENCRYPTED_V2`):
 *
 * MAGIC             "AUTHX_ENCRYPTED_V2\n" (19 bytes)
 * VERSION           1 byte  = 0x02
 * ITERATIONS        4 bytes big-endian PBKDF2 iteration count
 * SALT_LEN          1 byte  (>= 16, <= 64)
 * SALT              SALT_LEN random bytes
 * IV_LEN            1 byte  (= 12)
 * IV                IV_LEN random bytes
 * CIPHERTEXT        AES/GCM/NoPadding output + 128-bit GCM tag
 *
 * The header (VERSION..IV) is authenticated via GCM AAD, so any tampering
 * with the KDF parameters or IV is detected during decryption.
 *
 * No Android Keystore involvement: the file is portable across devices,
 * installs and builds as long as the user knows the password.
 */
object AuthxFileCrypto {
    const val V2_MAGIC = "AUTHX_ENCRYPTED_V2\n"
    const val V1_MAGIC = "AUTHX_ENCRYPTED_V1\n"
    private const val LEGACY_MAGIC_PREFIX = "AUTHX_ENCRYPTED_V"
    private const val VERSION = 2
    const val DEFAULT_PBKDF2_ITERATIONS = 600_000
    private const val KEY_LENGTH_BITS = 256
    private const val TAG_LENGTH_BITS = 128
    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val PBKDF2_ALGORITHM = "PBKDF2WithHmacSHA256"

    private const val MIN_SALT_LENGTH = 16
    private const val MAX_SALT_LENGTH = 64
    private const val IV_LENGTH = 12
    private const val GCM_TAG_LENGTH = TAG_LENGTH_BITS / 8
    private const val MIN_ITERATIONS = 1_000
    private const val MAX_ITERATIONS = 10_000_000
    const val MAX_FILE_SIZE = 64 * 1024 * 1024
    private const val MIN_CIPHERTEXT_LENGTH = GCM_TAG_LENGTH

    private const val CSV_HEADER = "Title,Username,Website,Password,Notes,TotpSecret,Digits,Period,Algorithm,Type"

    enum class AuthxFormat { V2, V1, CSV, UNSUPPORTED, INVALID }

    sealed class ImportResult {
        data class Success(val items: List<VaultItem>) : ImportResult()
        data object WrongPassword : ImportResult()
        data object InvalidFile : ImportResult()
        data object UnsupportedVersion : ImportResult()
        data object IoError : ImportResult()
        data object LegacyKeyMissing : ImportResult()
    }

    fun detectFormat(bytes: ByteArray): AuthxFormat {
        if (bytes.isEmpty()) return AuthxFormat.INVALID
        return when {
            bytes.startsWith(V2_MAGIC) ->
                if (validateV2Header(bytes)) AuthxFormat.V2 else AuthxFormat.INVALID
            bytes.startsWith(V1_MAGIC) -> AuthxFormat.V1
            bytes.startsWith(LEGACY_MAGIC_PREFIX) -> AuthxFormat.UNSUPPORTED
            else -> AuthxFormat.CSV
        }
    }

    fun encrypt(plainBytes: ByteArray, password: CharArray): ByteArray {
        val random = SecureRandom()
        val salt = ByteArray(MIN_SALT_LENGTH).also(random::nextBytes)
        val iv = ByteArray(IV_LENGTH).also(random::nextBytes)
        val key = deriveKey(password, salt, DEFAULT_PBKDF2_ITERATIONS)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(TAG_LENGTH_BITS, iv))
        cipher.updateAAD(headerBytes(DEFAULT_PBKDF2_ITERATIONS, salt, iv))
        val ciphertext = cipher.doFinal(plainBytes)
        return ByteArrayOutputStream().use { out ->
            out.write(V2_MAGIC.toByteArray(StandardCharsets.US_ASCII))
            out.write(headerBytes(DEFAULT_PBKDF2_ITERATIONS, salt, iv))
            out.write(ciphertext)
            out.toByteArray()
        }
    }

    fun decrypt(encrypted: ByteArray, password: CharArray): ByteArray {
        val header = parseV2Header(encrypted) ?: throw InvalidFileException()
        val key = deriveKey(password, header.salt, header.iterations)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        try {
            cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(TAG_LENGTH_BITS, header.iv))
            cipher.updateAAD(header.toHeaderBytes())
            return cipher.doFinal(header.ciphertext)
        } catch (e: AEADBadTagException) {
            throw WrongPasswordException()
        }
    }

    fun buildCsv(items: List<VaultItem>): String {
        return buildString {
            append(CSV_HEADER).append('\n')
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

    fun parseCsv(csv: String): List<VaultItem> {
        val rows = parseCsvRows(csv)
        val items = mutableListOf<VaultItem>()
        if (rows.isEmpty()) return items
        val header = detectHeader(rows[0])
        val start = if (header != null) 1 else 0
        for (index in start until rows.size) {
            val row = rows[index]
            if (row.isEmpty() || row.all { it.isEmpty() }) continue
            items.add(parseRow(row, header))
        }
        return items
    }

    private fun parseRow(row: List<String>, header: Map<String, Int>?): VaultItem {
        if (header != null) {
            fun col(field: String): String {
                val position = header[field] ?: return ""
                return row.getOrNull(position)?.trim() ?: ""
            }
            return VaultItem(
                title = col("title").ifBlank { "Imported Item" },
                username = col("username"),
                websiteUrl = col("websiteUrl"),
                password = col("password"),
                notes = col("notes"),
                totpSecret = col("totpSecret").ifBlank { null },
                digits = col("digits").toIntOrNull() ?: 6,
                period = col("period").toIntOrNull() ?: 30,
                algorithm = col("algorithm").ifBlank { "SHA1" },
                type = col("type").ifBlank { "LOGIN" }
            )
        }
        return VaultItem(
            title = row.getOrNull(0)?.ifBlank { "Imported Item" } ?: "Imported Item",
            username = row.getOrNull(1) ?: "",
            websiteUrl = row.getOrNull(2) ?: "",
            password = row.getOrNull(3) ?: "",
            notes = row.getOrNull(4) ?: "",
            totpSecret = row.getOrNull(5)?.ifBlank { null },
            digits = row.getOrNull(6)?.toIntOrNull() ?: 6,
            period = row.getOrNull(7)?.toIntOrNull() ?: 30,
            algorithm = row.getOrNull(8) ?: "SHA1",
            type = row.getOrNull(9)?.ifBlank { "LOGIN" } ?: "LOGIN"
        )
    }

    /**
     * Detecta um cabeçalho CSV pelos nomes das colunas (case-insensitive).
     * Retorna o mapa campo-canônico -> índice da coluna, ou `null` quando a
     * primeira linha não parece um cabeçalho (arquivo sem header).
     */
    private fun detectHeader(row: List<String>): Map<String, Int>? {
        val aliases = mapOf(
            "title" to listOf("title", "name", "site name", "site"),
            "username" to listOf("username", "user name", "user", "email", "login"),
            "websiteUrl" to listOf("websiteurl", "url", "website", "website url", "web", "domain", "site url"),
            "password" to listOf("password", "pass", "pwd", "senha"),
            "notes" to listOf("notes", "note", "comment", "comments", "description"),
            "totpSecret" to listOf("totpsecret", "totp secret", "totp", "otp secret", "otp", "secret", "2fa"),
            "digits" to listOf("digits", "code length"),
            "period" to listOf("period", "time step", "step"),
            "algorithm" to listOf("algorithm", "algo", "hash"),
            "type" to listOf("type", "category")
        )
        val map = mutableMapOf<String, Int>()
        row.forEachIndexed { index, raw ->
            val normalized = raw.trim().lowercase()
            for ((field, keys) in aliases) {
                if (keys.contains(normalized) && !map.containsKey(field)) {
                    map[field] = index
                    break
                }
            }
        }
        val hasIdentity = map.containsKey("title") || map.containsKey("username")
        return if (map.size >= 2 && hasIdentity) map else null
    }

    private class V2Header(
        val iterations: Int,
        val salt: ByteArray,
        val iv: ByteArray,
        val ciphertext: ByteArray
    ) {
        fun toHeaderBytes(): ByteArray = headerBytes(iterations, salt, iv)
    }

    private fun parseV2Header(bytes: ByteArray): V2Header? {
        if (!validateV2Header(bytes)) return null
        var pos = V2_MAGIC.length
        val version = bytes[pos].toInt() and 0xFF
        pos++
        if (version != VERSION) return null
        val iterations = readInt(bytes, pos)
        pos += 4
        val saltLen = bytes[pos].toInt() and 0xFF
        pos++
        val salt = bytes.copyOfRange(pos, pos + saltLen)
        pos += saltLen
        val ivLen = bytes[pos].toInt() and 0xFF
        pos++
        val iv = bytes.copyOfRange(pos, pos + ivLen)
        pos += ivLen
        val ciphertext = bytes.copyOfRange(pos, bytes.size)
        return V2Header(iterations, salt, iv, ciphertext)
    }

    private fun validateV2Header(bytes: ByteArray): Boolean {
        if (bytes.size < V2_MAGIC.length + 1 + 4 + 1 + MIN_SALT_LENGTH + 1 + IV_LENGTH + GCM_TAG_LENGTH) {
            return false
        }
        var pos = V2_MAGIC.length
        if ((bytes[pos].toInt() and 0xFF) != VERSION) return false
        pos++
        val iterations = readInt(bytes, pos)
        pos += 4
        if (iterations < MIN_ITERATIONS || iterations > MAX_ITERATIONS) return false
        val saltLen = bytes[pos].toInt() and 0xFF
        pos++
        if (saltLen < MIN_SALT_LENGTH || saltLen > MAX_SALT_LENGTH) return false
        if (bytes.size < pos + saltLen + 1 + IV_LENGTH + GCM_TAG_LENGTH) return false
        pos += saltLen
        val ivLen = bytes[pos].toInt() and 0xFF
        pos++
        if (ivLen != IV_LENGTH) return false
        if (bytes.size < pos + ivLen + GCM_TAG_LENGTH) return false
        pos += ivLen
        return bytes.size - pos >= MIN_CIPHERTEXT_LENGTH
    }

    private fun readInt(bytes: ByteArray, pos: Int): Int {
        return ((bytes[pos].toInt() and 0xFF) shl 24) or
            ((bytes[pos + 1].toInt() and 0xFF) shl 16) or
            ((bytes[pos + 2].toInt() and 0xFF) shl 8) or
            (bytes[pos + 3].toInt() and 0xFF)
    }

    private fun headerBytes(iterations: Int, salt: ByteArray, iv: ByteArray): ByteArray {
        return ByteArrayOutputStream().use { out ->
            out.write(VERSION)
            out.write((iterations ushr 24) and 0xFF)
            out.write((iterations ushr 16) and 0xFF)
            out.write((iterations ushr 8) and 0xFF)
            out.write(iterations and 0xFF)
            out.write(salt.size)
            out.write(salt)
            out.write(iv.size)
            out.write(iv)
            out.toByteArray()
        }
    }

    private fun deriveKey(password: CharArray, salt: ByteArray, iterations: Int): SecretKey {
        val spec = PBEKeySpec(password, salt, iterations, KEY_LENGTH_BITS)
        return try {
            val derived = SecretKeyFactory.getInstance(PBKDF2_ALGORITHM).generateSecret(spec)
            SecretKeySpec(derived.encoded, "AES")
        } finally {
            spec.clearPassword()
        }
    }

    private fun parseCsvRows(input: String): List<List<String>> {
        val rows = mutableListOf<List<String>>()
        val row = mutableListOf<String>()
        val field = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < input.length) {
            val c = input[i]
            when {
                inQuotes -> when (c) {
                    '"' -> {
                        if (i + 1 < input.length && input[i + 1] == '"') {
                            field.append('"')
                            i++
                        } else {
                            inQuotes = false
                        }
                    }
                    else -> field.append(c)
                }
                c == '"' -> inQuotes = true
                c == ',' -> {
                    row.add(field.toString())
                    field.setLength(0)
                }
                c == '\r' -> {
                    if (i + 1 < input.length && input[i + 1] == '\n') {
                        row.add(field.toString())
                        field.setLength(0)
                        rows.add(row.toList())
                        row.clear()
                        i++
                    } else {
                        field.append(c)
                    }
                }
                c == '\n' -> {
                    row.add(field.toString())
                    field.setLength(0)
                    rows.add(row.toList())
                    row.clear()
                }
                else -> field.append(c)
            }
            i++
        }
        if (field.isNotEmpty() || row.isNotEmpty()) {
            row.add(field.toString())
            rows.add(row.toList())
        }
        return rows
    }

    private fun escapeCsv(value: String): String {
        if (!value.contains(",") && !value.contains("\"") && !value.contains("\n") && !value.contains("\r")) {
            return value
        }
        return "\"" + value.replace("\"", "\"\"") + "\""
    }

    private fun ByteArray.startsWith(prefix: ByteArray): Boolean {
        if (size < prefix.size) return false
        return prefix.indices.all { this[it] == prefix[it] }
    }

    private fun ByteArray.startsWith(prefix: String): Boolean =
        startsWith(prefix.toByteArray(StandardCharsets.US_ASCII))
}