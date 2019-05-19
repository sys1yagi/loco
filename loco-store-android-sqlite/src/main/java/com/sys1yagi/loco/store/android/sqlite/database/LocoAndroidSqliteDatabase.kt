package com.sys1yagi.loco.store.android.sqlite.database

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.text.TextUtils
import android.util.Log
import com.sys1yagi.loco.core.internal.SmashedLog
import com.sys1yagi.loco.store.android.sqlite.internal.ProcessName


class LocoAndroidSqliteDatabase(context: Context) :
    SQLiteOpenHelper(context, databaseName(context), null, DATABASE_VERSION) {

    companion object {
        private val DATABASE_NAME = "loco-android-sqlite.db"

        private val TABLE_NAME = "logs"

        private val COLUMN_NAME_LOG_TYPE = "log_type"

        private val COLUMN_NAME_SMASHER_TYPE = "smasher_type"

        private val COLUMN_NAME_SENDER_TYPE = "sender_type"

        private val COLUMN_NAME_SMASHED_LOG = "smashed_log"

        private val DATABASE_VERSION = 1

        fun databaseName(context: Context): String {
            val processName = ProcessName.getAndroidProcessName(context)
            return if (TextUtils.isEmpty(processName)) {
                DATABASE_NAME
            } else {
                "$processName.$DATABASE_NAME"
            }
        }
    }

    override fun onCreate(db: SQLiteDatabase?) {
        val query = """
            CREATE TABLE IF NOT EXISTS $TABLE_NAME (
                id INTEGER PRIMARY KEY,
                $COLUMN_NAME_LOG_TYPE TEXT,
                $COLUMN_NAME_SMASHER_TYPE TEXT,
                $COLUMN_NAME_SENDER_TYPE TEXT,
                $COLUMN_NAME_SMASHED_LOG TEXT,
            );
        """.trimIndent()
        db?.execSQL(query)
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        Log.e("LocoAndroidSqlite", "unexpected onUpgrade(db, $oldVersion, $newVersion)");
    }

    fun insert(log: SmashedLog) {
        val contentValues = ContentValues()
        contentValues.put("id", log.uniqueId)
        contentValues.put(COLUMN_NAME_LOG_TYPE, log.logTypeName)
        contentValues.put(COLUMN_NAME_SMASHER_TYPE, log.smasherTypeName)
        contentValues.put(COLUMN_NAME_SENDER_TYPE, log.senderTypeName)
        contentValues.put(COLUMN_NAME_SMASHED_LOG, log.smashedLog)
        writableDatabase.insert(TABLE_NAME, null, contentValues)
    }

    fun select(size: Int): List<Record> {
        val query = """
            SELECT * FROM $TABLE_NAME
                ORDER BY id ASC
                LIMIT $size
        """.trimIndent()
        val cursor = readableDatabase.rawQuery(query, arrayOf())

        try {
            return recordsFromCursor(cursor)
        } finally {
            cursor.close()
        }
    }

    fun delete(records: List<Record>) {
        val query = """
            DELETE FROM $TABLE_NAME
            WHERE id IN (${records.map { it.id }.joinToString(",")} )
        """.trimIndent()
        writableDatabase.execSQL(query)
    }

    private fun recordsFromCursor(cursor: Cursor): List<Record> {
        val records = mutableListOf<Record>()
        while (cursor.moveToNext()) {
            val record = buildRecord(cursor)
            records.add(record)
        }
        return records
    }

    private fun buildRecord(cursor: Cursor): Record {
        return Record(
            cursor.getLong(0),
            cursor.getString(1),
            cursor.getString(2),
            cursor.getString(3),
            cursor.getString(4)
        )
    }

    protected fun finalize() {
        close()
    }
}
