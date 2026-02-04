package com.vxncius.authx.data
import androidx.room.*
import kotlinx.coroutines.flow.Flow
@Dao
interface VaultDao {
    @Query("SELECT * FROM vault_items ORDER BY title ASC")
    fun getAllItems(): Flow<List<VaultItem>>
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: VaultItem)
    @Delete
    suspend fun deleteItem(item: VaultItem)
    @Update
    suspend fun updateItem(item: VaultItem)
}

