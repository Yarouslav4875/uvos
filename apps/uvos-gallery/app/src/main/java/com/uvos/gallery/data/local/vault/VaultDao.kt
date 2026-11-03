package com.uvos.gallery.data.local.vault

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface VaultDao {
    @Query("SELECT * FROM vault_items ORDER BY encrypted_timestamp DESC")
    fun getAllItems(): Flow<List<VaultEntity>>

    @Query("SELECT * FROM vault_items WHERE id = :id LIMIT 1")
    suspend fun getItemById(id: String): VaultEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: VaultEntity)

    @Delete
    suspend fun deleteItem(item: VaultEntity)

    @Query("DELETE FROM vault_items WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("SELECT COUNT(*) FROM vault_items")
    suspend fun getCount(): Int
}
