package com.vxncius.authx.data
import androidx.room.Entity
import androidx.room.PrimaryKey
@Entity(tableName = "vault_items")
data class VaultItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val username: String = "",
    val websiteUrl: String = "",
    val password: String = "",
    val notes: String = "",
    val totpSecret: String? = null,
    val digits: Int = 6,
    val period: Int = 30,
    val algorithm: String = "SHA1",
    val type: String = "LOGIN"
)

