package com.vxncius.authx.logic

import com.vxncius.authx.data.VaultItem

/**
 * Validador de importação de senhas (backups `.authx` e CSVs).
 *
 * Evita duplicação ao carregar mais de um arquivo que compartilhe entradas:
 * um item só é considerado novo se não existir uma entrada idêntica já
 * cadastrada no cofre (ou repetida dentro do próprio arquivo importado).
 * A duplicação é comparada por título, usuário, site, senha e tipo.
 */
object ImportValidator {

    fun deduplicate(imported: List<VaultItem>, existing: List<VaultItem>): List<VaultItem> {
        val seen = HashSet<String>()
        existing.forEach { seen.add(keyOf(it)) }
        val unique = ArrayList<VaultItem>()
        for (item in imported) {
            if (seen.add(keyOf(item))) unique.add(item)
        }
        return unique
    }

    private fun keyOf(item: VaultItem): String = buildString {
        append(item.title.trim().lowercase())
        append('\u0000')
        append(item.username.trim().lowercase())
        append('\u0000')
        append(item.websiteUrl.trim().lowercase())
        append('\u0000')
        append(item.password)
        append('\u0000')
        append(item.type)
    }
}
