package com.vxncius.authx.logic

import com.vxncius.authx.data.VaultItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets

class AuthxFileCryptoTest {

    private val magic = AuthxFileCrypto.V2_MAGIC
    private val saltLenIndex = magic.length + 1 + 4
    private val ivLenIndex = magic.length + 1 + 4 + 1 + MIN_SALT

    private fun sampleItems(): List<VaultItem> = listOf(
        VaultItem(
            title = "Netflix",
            username = "user@example.com",
            websiteUrl = "https://netflix.com",
            password = "p@ssw0rd, \"complex\"",
            notes = "nota com,\nquebra de linha\n\"aspas\"",
            totpSecret = "JBSWY3DPEHPK3PXP",
            digits = 6,
            period = 30,
            algorithm = "SHA1",
            type = "LOGIN"
        ),
        VaultItem(
            title = "银行 日本語 🎌 café, emoji e vírgulas",
            username = "usuario",
            websiteUrl = "https://example.com",
            password = "P@ss ção 密码 ünïcode",
            notes = "linha com crlf\r\n",
            totpSecret = "GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ",
            digits = 8,
            period = 60,
            algorithm = "SHA256",
            type = "TOTP"
        )
    )

    private fun encryptItems(items: List<VaultItem>, password: String): ByteArray {
        val csv = AuthxFileCrypto.buildCsv(items)
        return AuthxFileCrypto.encrypt(csv.toByteArray(StandardCharsets.UTF_8), password.toCharArray())
    }

    private fun decryptCsv(bytes: ByteArray, password: String): List<VaultItem> {
        val plain = AuthxFileCrypto.decrypt(bytes, password.toCharArray())
        return AuthxFileCrypto.parseCsv(String(plain, StandardCharsets.UTF_8))
    }

    private fun assertItemsEqual(expected: List<VaultItem>, actual: List<VaultItem>) {
        assertEquals(expected.size, actual.size)
        for (i in expected.indices) {
            val e = expected[i]
            val a = actual[i]
            assertEquals("title[$i]", e.title, a.title)
            assertEquals("username[$i]", e.username, a.username)
            assertEquals("websiteUrl[$i]", e.websiteUrl, a.websiteUrl)
            assertEquals("password[$i]", e.password, a.password)
            assertEquals("notes[$i]", e.notes, a.notes)
            assertEquals("totpSecret[$i]", e.totpSecret, a.totpSecret)
            assertEquals("digits[$i]", e.digits, a.digits)
            assertEquals("period[$i]", e.period, a.period)
            assertEquals("algorithm[$i]", e.algorithm, a.algorithm)
            assertEquals("type[$i]", e.type, a.type)
        }
    }

    @Test
    fun exportAndImportWithCorrectPassword() {
        val expected = sampleItems()
        val bytes = encryptItems(expected, "senha-secreta-123")
        assertEquals(AuthxFileCrypto.AuthxFormat.V2, AuthxFileCrypto.detectFormat(bytes))
        assertItemsEqual(expected, decryptCsv(bytes, "senha-secreta-123"))
    }

    @Test
    fun wrongPasswordFailsAsWrongPassword() {
        val bytes = encryptItems(sampleItems(), "senha-correta")
        val exception = runCatching { decryptCsv(bytes, "senha-errada") }.exceptionOrNull()
        assertNotNull(exception)
        assertTrue(exception is WrongPasswordException)
    }

    @Test
    fun samePasswordSameDataProducesDifferentFiles() {
        val items = sampleItems()
        val first = encryptItems(items, "senha")
        val second = encryptItems(items, "senha")
        assertFalse(first.contentEquals(second))
        assertItemsEqual(sampleItems(), decryptCsv(first, "senha"))
        assertItemsEqual(sampleItems(), decryptCsv(second, "senha"))
    }

    @Test
    fun corruptedSaltLengthIsInvalidFile() {
        val bytes = encryptItems(sampleItems(), "senha")
        val corrupted = bytes.copyOf()
        corrupted[saltLenIndex] = 0
        assertEquals(AuthxFileCrypto.AuthxFormat.INVALID, AuthxFileCrypto.detectFormat(corrupted))

        val tooLarge = bytes.copyOf()
        tooLarge[saltLenIndex] = 200.toByte()
        assertEquals(AuthxFileCrypto.AuthxFormat.INVALID, AuthxFileCrypto.detectFormat(tooLarge))
    }

    @Test
    fun truncatedHeaderIsInvalidFile() {
        val bytes = encryptItems(sampleItems(), "senha")
        val truncated = bytes.copyOf(magic.length + 20)
        assertEquals(AuthxFileCrypto.AuthxFormat.INVALID, AuthxFileCrypto.detectFormat(truncated))
    }

    @Test
    fun truncatedCiphertextFailsAuthentication() {
        val bytes = encryptItems(sampleItems(), "senha")
        val truncated = bytes.copyOf(bytes.size - 8)
        assertEquals(AuthxFileCrypto.AuthxFormat.V2, AuthxFileCrypto.detectFormat(truncated))
        val exception = runCatching { decryptCsv(truncated, "senha") }.exceptionOrNull()
        assertNotNull(exception)
        assertTrue(exception is WrongPasswordException)
    }

    @Test
    fun invalidMagicIsNeverTreatedAsV2() {
        val bytes = encryptItems(sampleItems(), "senha")
        val corrupted = bytes.copyOf()
        corrupted[0] = 'X'.code.toByte()
        assertEquals(AuthxFileCrypto.AuthxFormat.CSV, AuthxFileCrypto.detectFormat(corrupted))

        val empty = ByteArray(0)
        assertEquals(AuthxFileCrypto.AuthxFormat.INVALID, AuthxFileCrypto.detectFormat(empty))
    }

    @Test
    fun unsupportedVersionIsDetected() {
        val bytes = "AUTHX_ENCRYPTED_V3\nresto-do-arquivo".toByteArray(StandardCharsets.US_ASCII)
        assertEquals(AuthxFileCrypto.AuthxFormat.UNSUPPORTED, AuthxFileCrypto.detectFormat(bytes))

        val v1 = "AUTHX_ENCRYPTED_V1\n".toByteArray(StandardCharsets.US_ASCII)
        assertEquals(AuthxFileCrypto.AuthxFormat.V1, AuthxFileCrypto.detectFormat(v1))
    }

    @Test
    fun invalidIvLengthIsInvalidFile() {
        val bytes = encryptItems(sampleItems(), "senha")
        val corrupted = bytes.copyOf()
        corrupted[ivLenIndex] = 13
        assertEquals(AuthxFileCrypto.AuthxFormat.INVALID, AuthxFileCrypto.detectFormat(corrupted))
    }

    @Test
    fun emptyCiphertextIsInvalidFile() {
        val headerOnly = buildHeaderOnlyBytes()
        assertEquals(AuthxFileCrypto.AuthxFormat.INVALID, AuthxFileCrypto.detectFormat(headerOnly))
    }

    @Test
    fun ciphertextByteFlipFailsGcmAuthentication() {
        val bytes = encryptItems(sampleItems(), "senha")
        val corrupted = bytes.copyOf()
        val last = corrupted[corrupted.size - 1]
        corrupted[corrupted.size - 1] = (last.toInt() xor 1).toByte()
        val exception = runCatching { decryptCsv(corrupted, "senha") }.exceptionOrNull()
        assertNotNull(exception)
        assertTrue(exception is WrongPasswordException)
    }

    @Test
    fun saltTamperingFailsGcmAuthentication() {
        val bytes = encryptItems(sampleItems(), "senha")
        val corrupted = bytes.copyOf()
        corrupted[saltLenIndex + 1] = (corrupted[saltLenIndex + 1].toInt() xor 0x01).toByte()
        val exception = runCatching { decryptCsv(corrupted, "senha") }.exceptionOrNull()
        assertNotNull(exception)
        assertTrue(exception is WrongPasswordException)
    }

    @Test
    fun complexDataRoundTripsExactly() {
        val expected = sampleItems()
        assertItemsEqual(expected, decryptCsv(encryptItems(expected, "complexa!"), "complexa!"))
    }

    @Test
    fun emptyVaultRoundTrips() {
        val csv = AuthxFileCrypto.buildCsv(emptyList())
        val bytes = AuthxFileCrypto.encrypt(csv.toByteArray(StandardCharsets.UTF_8), "senha".toCharArray())
        val plain = AuthxFileCrypto.decrypt(bytes, "senha".toCharArray())
        assertTrue(AuthxFileCrypto.parseCsv(String(plain, StandardCharsets.UTF_8)).isEmpty())
    }

    @Test
    fun multipleItemsRoundTrip() {
        val many = (1..25).map { i ->
            VaultItem(
                title = "Item $i",
                username = "user$i",
                password = "senha-$i,\"$\"",
                totpSecret = if (i % 2 == 0) "SECRET$i" else null,
                digits = if (i % 2 == 0) 8 else 6,
                period = if (i % 3 == 0) 60 else 30
            )
        }
        assertItemsEqual(many, decryptCsv(encryptItems(many, "senha"), "senha"))
    }

    @Test
    fun repeatedExportImportCycles() {
        repeat(5) {
            val expected = sampleItems()
            val bytes = encryptItems(expected, "senha")
            assertItemsEqual(expected, decryptCsv(bytes, "senha"))
        }
    }

    @Test
    fun plainCsvFallbackStillWorks() {
        val csv = AuthxFileCrypto.buildCsv(sampleItems())
        val bytes = csv.toByteArray(StandardCharsets.UTF_8)
        assertEquals(AuthxFileCrypto.AuthxFormat.CSV, AuthxFileCrypto.detectFormat(bytes))
        assertItemsEqual(sampleItems(), AuthxFileCrypto.parseCsv(csv))
    }

    @Test
    fun csvParserHandlesQuotesCommasNewlinesAndUnicode() {
        val tricky = listOf(
            VaultItem(
                title = "Título \"entre aspas\"",
                username = "a,b,c",
                websiteUrl = "https://x.com",
                password = "p\"a\"s,s\nnova\nlinha",
                notes = "nota1\r\nnota2",
                totpSecret = "JBSWY3DPEHPK3PXP",
                digits = 6,
                period = 30,
                algorithm = "SHA1",
                type = "LOGIN"
            )
        )
        val csv = AuthxFileCrypto.buildCsv(tricky)
        val parsed = AuthxFileCrypto.parseCsv(csv)
        assertItemsEqual(tricky, parsed)
    }

    @Test
    fun csvParserUsesHeaderColumnNames() {
        val csv = """
            name,url,username,password,note
            Netflix,https://netflix.com,user@example.com,p@ss123,minha conta
            Spotify,https://spotify.com,spotify@example.com,s@ss456,musica
        """.trimIndent()
        val parsed = AuthxFileCrypto.parseCsv(csv)
        assertEquals(2, parsed.size)
        val first = parsed[0]
        assertEquals("Netflix", first.title)
        assertEquals("user@example.com", first.username)
        assertEquals("https://netflix.com", first.websiteUrl)
        assertEquals("p@ss123", first.password)
        assertEquals("minha conta", first.notes)
    }

    @Test
    fun csvParserKeepsFirstRowWhenNoHeader() {
        val csv = """
            Netflix,user@example.com,https://netflix.com,p@ss123
            Spotify,spotify@example.com,https://spotify.com,s@ss456
        """.trimIndent()
        val parsed = AuthxFileCrypto.parseCsv(csv)
        assertEquals(2, parsed.size)
        assertEquals("Netflix", parsed[0].title)
        assertEquals("user@example.com", parsed[0].username)
        assertEquals("https://netflix.com", parsed[0].websiteUrl)
        assertEquals("p@ss123", parsed[0].password)
    }

    private fun buildHeaderOnlyBytes(): ByteArray {
        val out = ByteArrayOutputStream()
        out.write(magic.toByteArray(StandardCharsets.US_ASCII))
        out.write(2)
        out.write((600_000 ushr 24) and 0xFF)
        out.write((600_000 ushr 16) and 0xFF)
        out.write((600_000 ushr 8) and 0xFF)
        out.write(600_000 and 0xFF)
        out.write(16)
        out.write(ByteArray(16) { it.toByte() })
        out.write(12)
        out.write(ByteArray(12) { (it + 10).toByte() })
        return out.toByteArray()
    }

    private companion object {
        const val MIN_SALT = 16
    }
}