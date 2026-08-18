package com.vxncius.authx.logic

import android.content.Context
import android.net.Uri
import com.vxncius.authx.data.VaultItem
import com.vxncius.authx.logic.AuthxFileCrypto.AuthxFormat
import com.vxncius.authx.logic.AuthxFileCrypto.ImportResult
import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Android I/O wrapper for the `.authx` backup format.
 *
 * Encryption/format handling lives in [AuthxFileCrypto] (pure and JVM-testable).
 * This object only bridges [Context]/[Uri] I/O and keeps the legacy V1 read path.
 *
 * New exports always use the portable V2 format (password-based). The legacy
 * `authx_export_key` Keystore key is only ever *read* to open V1 backups and is
 * never recreated implicitly: if the key is missing the import reports
 * [ImportResult.LegacyKeyMissing] instead of silently generating a new key.
 */
object CsvHandler {
    private const val KEY_ALIAS = "authx_export_key"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val DETECTION_READ_BYTES = 4096

    fun exportToAuthx(
        context: Context,
        uri: Uri,
        items: List<VaultItem>,
        password: CharArray
    ): Boolean {
        return try {
            val plainCsv = AuthxFileCrypto.buildCsv(items).toByteArray(StandardCharsets.UTF_8)
            val encrypted = AuthxFileCrypto.encrypt(plainCsv, password)
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                outputStream.write(encrypted)
                outputStream.flush()
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    fun detectFormat(context: Context, uri: Uri): AuthxFormat {
        val bytes = readPrefix(context, uri, DETECTION_READ_BYTES) ?: return AuthxFormat.INVALID
        return AuthxFileCrypto.detectFormat(bytes)
    }

    fun importFromAuthx(context: Context, uri: Uri, password: CharArray?): ImportResult {
        val bytes = readAllBytes(context, uri) ?: return ImportResult.IoError
        return when (AuthxFileCrypto.detectFormat(bytes)) {
            AuthxFormat.V2 -> importV2(bytes, password)
            AuthxFormat.V1 -> importLegacyV1(bytes)
            AuthxFormat.CSV -> importPlainCsv(bytes)
            AuthxFormat.UNSUPPORTED -> ImportResult.UnsupportedVersion
            AuthxFormat.INVALID -> ImportResult.InvalidFile
        }
    }

    private fun importV2(bytes: ByteArray, password: CharArray?): ImportResult {
        if (password == null) return ImportResult.InvalidFile
        return try {
            val csvBytes = AuthxFileCrypto.decrypt(bytes, password)
            ImportResult.Success(AuthxFileCrypto.parseCsv(String(csvBytes, StandardCharsets.UTF_8)))
        } catch (e: WrongPasswordException) {
            ImportResult.WrongPassword
        } catch (e: InvalidFileException) {
            ImportResult.InvalidFile
        } catch (e: Exception) {
            ImportResult.InvalidFile
        }
    }

    private fun importPlainCsv(bytes: ByteArray): ImportResult {
        return try {
            ImportResult.Success(AuthxFileCrypto.parseCsv(String(bytes, StandardCharsets.UTF_8)))
        } catch (e: Exception) {
            ImportResult.InvalidFile
        }
    }

    private fun importLegacyV1(bytes: ByteArray): ImportResult {
        val magic = AuthxFileCrypto.V1_MAGIC.toByteArray(StandardCharsets.US_ASCII)
        if (bytes.size <= magic.size) return ImportResult.InvalidFile
        val payload = bytes.copyOfRange(magic.size, bytes.size)
        val key = getKeystoreKeyOrNull()
        if (key == null) return ImportResult.LegacyKeyMissing
        return try {
            val csvBytes = decryptV1(payload, key)
            ImportResult.Success(AuthxFileCrypto.parseCsv(String(csvBytes, StandardCharsets.UTF_8)))
        } catch (e: Exception) {
            ImportResult.InvalidFile
        }
    }

    private fun decryptV1(payload: ByteArray, key: SecretKey): ByteArray {
        val ivSize = payload.first().toInt()
        val iv = payload.copyOfRange(1, 1 + ivSize)
        val encrypted = payload.copyOfRange(1 + ivSize, payload.size)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(128, iv))
        return cipher.doFinal(encrypted)
    }

    /**
     * Read-only lookup. Never creates a key: a missing legacy key must surface
     * as [ImportResult.LegacyKeyMissing], not as a silently created one.
     */
    private fun getKeystoreKeyOrNull(): SecretKey? {
        return try {
            val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
            keyStore.getKey(KEY_ALIAS, null) as? SecretKey
        } catch (e: Exception) {
            null
        }
    }

    private fun readAllBytes(context: Context, uri: Uri): ByteArray? {
        return try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                val buffer = ByteArrayOutputStream()
                val chunk = ByteArray(8192)
                var total = 0
                while (true) {
                    val read = input.read(chunk)
                    if (read < 0) break
                    total += read
                    if (total > AuthxFileCrypto.MAX_FILE_SIZE) return null
                    buffer.write(chunk, 0, read)
                }
                buffer.toByteArray()
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun readPrefix(context: Context, uri: Uri, maxBytes: Int): ByteArray? {
        return try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                val buffer = ByteArray(maxBytes)
                var total = 0
                while (total < maxBytes) {
                    val read = input.read(buffer, total, maxBytes - total)
                    if (read < 0) break
                    total += read
                }
                buffer.copyOf(total)
            }
        } catch (e: Exception) {
            null
        }
    }
}