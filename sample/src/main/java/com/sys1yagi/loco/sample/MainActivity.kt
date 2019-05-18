package com.sys1yagi.loco.sample

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import com.google.gson.Gson
import com.sys1yagi.loco.android.LocoAndroid
import com.sys1yagi.loco.core.*
import com.sys1yagi.loco.core.internal.SmashedLog

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        LocoAndroid.start(
            LocoConfig(
                store = TestStore(),
                smashers = listOf(
                    JsonSmasher(Gson())
                ),
                senders = listOf(
                    LogcatSender()
                )
            ) {
                logToSmasher[JsonSmasher::class] = listOf(
                    ClickLog::class
                )
            }
        )
        LocoAndroid.send(
            ClickLog(1, "jack")
        )

        setContentView(R.layout.activity_main)
    }

    data class ClickLog(
        val id: Int,
        val name: String
    ) : LocoLog

    class JsonSmasher(val gson: Gson) : Smasher {
        private val filters = mutableListOf<Filter>()
        override fun smash(log: LocoLog): String {
            return gson.toJson(log)
        }

        override fun registerFilter(filter: Filter) {
            filters.add(filter)
        }
    }

    class LogcatSender : Sender {
        override fun send() {
            // no op
        }
    }

    class TestStore : Store {
        override suspend fun store(log: SmashedLog) {
            // no op
        }

        override suspend fun load(size: Int): List<SmashedLog> {
            return emptyList()
        }

        override suspend fun delete(logs: List<SmashedLog>) {
            // no op
        }
    }
}
