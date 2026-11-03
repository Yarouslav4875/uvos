package com.uvos.gallery.data.local.vault

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import net.sqlcipher.database.SupportFactory

@Database(entities = [VaultEntity::class], version = 1, exportSchema = false)
abstract class VaultDatabase : RoomDatabase() {
    abstract fun vaultDao(): VaultDao

    companion object {
        private const val DB_NAME = "uvos_vault_secure.db"

        @Volatile
        private var INSTANCE: VaultDatabase? = null

        fun getInstance(context: Context, passphraseBytes: ByteArray? = null): VaultDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context, passphraseBytes).also { INSTANCE = it }
            }
        }

        fun buildDatabase(context: Context, passphraseBytes: ByteArray? = null): VaultDatabase {
            val builder = Room.databaseBuilder(
                context.applicationContext,
                VaultDatabase::class.java,
                DB_NAME
            )
            if (passphraseBytes != null && passphraseBytes.isNotEmpty()) {
                val factory = SupportFactory(passphraseBytes)
                builder.openHelperFactory(factory)
            }
            return builder
                .fallbackToDestructiveMigration()
                .build()
        }
    }
}
