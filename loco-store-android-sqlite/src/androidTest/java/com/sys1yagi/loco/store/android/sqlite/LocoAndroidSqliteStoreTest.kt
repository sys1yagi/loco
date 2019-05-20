package com.sys1yagi.loco.store.android.sqlite

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.sys1yagi.loco.core.internal.SmashedLog
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LocoAndroidSqliteStoreTest {

    lateinit var store: LocoAndroidSqliteStore

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        store = LocoAndroidSqliteStore(context)
        runBlocking {
            store.clear()
        }
    }

    @After
    fun tearDown() {
        runBlocking {
            store.clear()
        }
    }

    @ExperimentalCoroutinesApi
    @Test
    fun store() = runBlocking {
        store.store(
            SmashedLog(
                "a",
                "b",
                "c",
                """{"log": 1}"""
            )
        )
        assertThat(store.count()).isEqualTo(1)
    }

    @Test
    fun delete() = runBlocking {
        repeat(100) {
            store.store(
                SmashedLog(
                    "a",
                    "b",
                    "c",
                    """{"log": 1}"""
                )
            )
        }
        val records = store.load(10)
        store.delete(records)
        assertThat(store.count()).isEqualTo(90)
    }
}
