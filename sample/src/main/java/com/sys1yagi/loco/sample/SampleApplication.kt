package com.sys1yagi.loco.sample

import android.app.Application
import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.sys1yagi.loco.core.Loco
import com.sys1yagi.loco.core.LocoConfig
import com.sys1yagi.loco.core.Sender
import com.sys1yagi.loco.core.SendingResult
import com.sys1yagi.loco.core.SendingScheduler
import com.sys1yagi.loco.core.internal.SmashedLog
import com.sys1yagi.loco.sample.log.ClickLog
import com.sys1yagi.loco.sample.log.ScreenLog
import com.sys1yagi.loco.sender.JsonArraySender
import com.sys1yagi.loco.smasher.FilterableGsonSmasher
import com.sys1yagi.loco.store.android.sqlite.LocoAndroidSqliteStore
import kotlinx.coroutines.delay

class SampleApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        val gson = Gson()
        Loco.start(
            LocoConfig(
                store = LocoAndroidSqliteStore(this),
                smasher = FilterableGsonSmasher(gson),
                senders = mapOf(
                    LogcatSender(gson) to listOf(
                        ClickLog::class,
                        ScreenLog::class
                    )
                ),
                scheduler = IntervalSendingScheduler(5000)
            )
        )
    }

    class LogcatSender(gson: Gson) : JsonArraySender(gson) {
        override suspend fun send(logs: JsonArray): SendingResult {
            logs.forEach {
                Log.d("LogcatSender", it.toString())
            }
            return SendingResult.SUCCESS
        }
    }

    class IntervalSendingScheduler(private val interval: Long) : SendingScheduler {
        override suspend fun schedule(
            latestResults: List<Pair<Sender, SendingResult>>,
            config: LocoConfig,
            offer: () -> Unit
        ) {
            delay(interval)
            offer()
        }
    }
}
