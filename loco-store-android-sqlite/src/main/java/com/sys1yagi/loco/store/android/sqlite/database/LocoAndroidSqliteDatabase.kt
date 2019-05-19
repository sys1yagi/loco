package com.sys1yagi.loco.store.android.sqlite.database

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase.CONFLICT_ABORT
import android.text.TextUtils
import android.util.Log
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import com.sys1yagi.loco.core.internal.SmashedLog
import com.sys1yagi.loco.store.android.sqlite.internal.ProcessName

class LocoAndroidSqliteDatabase(context: Context) :
    SupportSQLiteOpenHelper.Callback(DATABASE_VERSION) {

    val openHelper = FrameworkSQLiteOpenHelperFactory()
        .create(
            SupportSQLiteOpenHelper.Configuration
                .builder(context)
                .name(databaseName(context))
                .callback(this)
                .build()
        );

    companion object {
        private const val DATABASE_NAME = "loco-android-sqlite.db"

        private const val TABLE_NAME = "logs"

        private const val COLUMN_NAME_LOG_TYPE = "log_type"

        private const val COLUMN_NAME_SMASHER_TYPE = "smasher_type"

        private const val COLUMN_NAME_SENDER_TYPE = "sender_type"

        private const val COLUMN_NAME_SMASHED_LOG = "smashed_log"

        private const val COLUMN_NAME_CREATED_AT = "created_at"

        private const val DATABASE_VERSION = 1

        fun databaseName(context: Context): String {
            val processName = ProcessName.getAndroidProcessName(context)
            return if (TextUtils.isEmpty(processName)) {
                DATABASE_NAME
            } else {
                "$processName.$DATABASE_NAME"
            }
        }
    }

    override fun onCreate(db: SupportSQLiteDatabase?) {
        val query = """
            CREATE TABLE IF NOT EXISTS $TABLE_NAME (
                id INTEGER PRIMARY KEY,
                $COLUMN_NAME_LOG_TYPE TEXT,
                $COLUMN_NAME_SMASHER_TYPE TEXT,
                $COLUMN_NAME_SENDER_TYPE TEXT,
                $COLUMN_NAME_SMASHED_LOG TEXT,
                $COLUMN_NAME_CREATED_AT INTEGER
            );
        """.trimIndent()
        db?.execSQL(query)
    }

    override fun onUpgrade(db: SupportSQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        Log.e("LocoAndroidSqlite", "unexpected onUpgrade(db, $oldVersion, $newVersion)");
    }

    fun insert(log: SmashedLog) {
        val contentValues = ContentValues()
        contentValues.put("id", log.uniqueId)
        contentValues.put(COLUMN_NAME_LOG_TYPE, log.logTypeName)
        contentValues.put(COLUMN_NAME_SMASHER_TYPE, log.smasherTypeName)
        contentValues.put(COLUMN_NAME_SENDER_TYPE, log.senderTypeName)
        contentValues.put(COLUMN_NAME_SMASHED_LOG, log.smashedLog)
        contentValues.put(COLUMN_NAME_CREATED_AT, System.currentTimeMillis())
        openHelper.writableDatabase.insert(TABLE_NAME, CONFLICT_ABORT, contentValues)
    }

    fun select(size: Int): List<Record> {
        val query = """
            SELECT * FROM $TABLE_NAME
                ORDER BY $COLUMN_NAME_CREATED_AT ASC
                LIMIT $size
        """.trimIndent()
        openHelper.readableDatabase.query(query, arrayOf()).use { cursor ->
            return recordsFromCursor(cursor)
        }
    }

    fun delete(records: List<Record>) {
        val query = """
            DELETE FROM $TABLE_NAME
            WHERE id IN (${records.map { it.id }.joinToString(",")} )
        """.trimIndent()
        openHelper.writableDatabase.execSQL(query)
    }

    fun clear() {
        val query = """
            DELETE FROM $TABLE_NAME
        """.trimIndent()
        openHelper.writableDatabase.execSQL(query)
    }

    fun count(): Int {
        val query = "SELECT COUNT(*) FROM $TABLE_NAME"
        openHelper.readableDatabase.query(query, null).use { cursor ->
            return if (cursor.moveToNext()) {
                cursor.getInt(0)
            } else {
                0
            }
        }
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
        openHelper.close()
    }
}
