package com.shorturl.db

import jetbrains.exodus.entitystore.PersistentEntityStore
import jetbrains.exodus.entitystore.PersistentEntityStores
import jetbrains.exodus.entitystore.StoreTransaction

object XodusDatabase {
    private lateinit var store: PersistentEntityStore

    fun init(dataDir: String) {
        store = PersistentEntityStores.newInstance(dataDir)
    }

    fun close() {
        store.close()
    }

    fun <T> read(block: StoreTransaction.() -> T): T =
        store.computeInReadonlyTransaction { txn -> txn.block() }

    fun write(block: StoreTransaction.() -> Unit) =
        store.executeInTransaction { txn -> txn.block() }

    fun <T> writeReturning(block: StoreTransaction.() -> T): T =
        store.computeInTransaction { txn -> txn.block() }
}
