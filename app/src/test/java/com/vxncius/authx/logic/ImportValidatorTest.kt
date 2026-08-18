package com.vxncius.authx.logic

import com.vxncius.authx.data.VaultItem
import org.junit.Assert.assertEquals
import org.junit.Test

class ImportValidatorTest {

    private fun item(
        title: String = "Netflix",
        username: String = "user@example.com",
        websiteUrl: String = "https://netflix.com",
        password: String = "p@ss",
        type: String = "LOGIN"
    ) = VaultItem(
        title = title,
        username = username,
        websiteUrl = websiteUrl,
        password = password,
        type = type
    )

    @Test
    fun `ignora itens ja existentes no cofre`() {
        val existing = listOf(item())
        val imported = listOf(item(), item(title = "Spotify", websiteUrl = "https://spotify.com"))

        val unique = ImportValidator.deduplicate(imported, existing)

        assertEquals(1, unique.size)
        assertEquals("Spotify", unique.single().title)
    }

    @Test
    fun `importa itens novos quando nao ha sobreposicao`() {
        val existing = listOf(item(title = "Netflix"))
        val imported = listOf(item(title = "Spotify"), item(title = "GitHub"))

        val unique = ImportValidator.deduplicate(imported, existing)

        assertEquals(2, unique.size)
    }

    @Test
    fun `remove duplicatas dentro do proprio arquivo importado`() {
        val imported = listOf(item(), item(), item(title = "Spotify"))

        val unique = ImportValidator.deduplicate(imported, emptyList())

        assertEquals(2, unique.size)
    }

    @Test
    fun `comparacao de titulo usuario e site ignora caixa e espacos`() {
        val existing = listOf(item(title = "  Netflix ", username = "User@Example.COM"))
        val imported = listOf(item(title = "netflix", username = "user@example.com"))

        val unique = ImportValidator.deduplicate(imported, existing)

        assertEquals(0, unique.size)
    }

    @Test
    fun `senha diferente nao e considerada duplicata`() {
        val existing = listOf(item(password = "senha-antiga"))
        val imported = listOf(item(password = "senha-nova"))

        val unique = ImportValidator.deduplicate(imported, existing)

        assertEquals(1, unique.size)
    }
}