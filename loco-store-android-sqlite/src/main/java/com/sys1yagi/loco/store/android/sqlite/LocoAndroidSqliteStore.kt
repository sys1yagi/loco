package com.sys1yagi.loco.store.android.sqlite

import android.content.Context
import com.sys1yagi.loco.core.Store
import com.sys1yagi.loco.core.internal.SmashedLog
import com.sys1yagi.loco.store.android.sqlite.database.LocoAndroidSqliteDatabase
import com.sys1yagi.loco.store.android.sqlite.database.Record
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class LocoAndroidSqliteStore(context: Context) : Store {

    private val database = LocoAndroidSqliteDatabase(context)

    override suspend fun store(log: SmashedLog) {
        withContext(Dispatchers.IO) {
            database.insert(log)
        }
    }

    override suspend fun load(size: Int): List<SmashedLog> = withContext(Dispatchers.IO) {
        database.select(size).map {
            SmashedLog(
                it.logTypeName,
                it.smasherTypeName,
                it.senderTypeName,
                it.smashedLog,
                it.id
            )
        }
    }

    override suspend fun delete(logs: List<SmashedLog>) {
        withContext(Dispatchers.IO) {
            database.delete(logs.map {
                Record(
                    it.uniqueId,
                    it.logTypeName,
                    it.smasherTypeName,
                    it.senderTypeName,
                    it.smashedLog
                )
            })
        }
    }

    suspend fun count(): Int =
        withContext(Dispatchers.IO) {
            database.count()
        }

    suspend fun clear() {
        withContext(Dispatchers.IO) {
            database.clear()
        }
    }
}
