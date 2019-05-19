package com.sys1yagi.loco.sample

import android.app.Application
import android.util.Log
import com.google.gson.Gson
import com.sys1yagi.loco.core.*
import com.sys1yagi.loco.core.internal.SmashedLog
import com.sys1yagi.loco.sample.log.ClickLog
import com.sys1yagi.loco.sample.log.ScreenLog

class SampleApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        Loco.start(
            LocoConfig(
                store = InMemoryStore(),
                smashers = listOf(
                    GsonSmasher(Gson())
                ),
                senders = listOf(
                    LogcatSender()
                )
            ) {
                logToSmasher[GsonSmasher::class] = listOf(
                    ClickLog::class,
                    ScreenLog::class
                )
                logToSender[LogcatSender::class] = listOf(
                    ClickLog::class,
                    ScreenLog::class
                )
            }
        )
    }

    class GsonSmasher(val gson: Gson) : Smasher {
        override fun smash(log: LocoLog): String {
            return gson.toJson(log)
        }
    }

    class LogcatSender : Sender {
        override suspend fun send(logs: List<SmashedLog>): SendingResult {
            logs.forEach {
                Log.d("LogcatSender", it.smashedLog)
            }
            return SendingResult.SUCCESS
        }
    }

    class InMemoryStore : Store {
        val storage = mutableListOf<SmashedLog>()
        override suspend fun store(log: SmashedLog) {
            storage.add(log)
        }

        override suspend fun load(size: Int): List<SmashedLog> {
            return storage.take(size)
        }

        override suspend fun delete(logs: List<SmashedLog>) {
            storage.removeAll(logs)
        }
    }
}
