
Not yet published to bintray.

# loco
loco (Log Coroutine) is a logging library using coroutine for Android.

[![CircleCI](https://circleci.com/gh/sys1yagi/loco.svg?style=svg)](https://circleci.com/gh/sys1yagi/loco) ![kotlin](https://img.shields.io/badge/kotlin-1.3.31-blue.svg) ![license](https://img.shields.io/github/license/sys1yagi/loco.svg?maxAge=2592000)

# Description

TODO

# Install

```groovy
dependencies {
    // core
    implementation 'com.sys1yagi.loco:loco-core:1.0.0'
  
    // optional, use Gson to serialize the log, and you can use filters to process the logs.
    implementation 'com.sys1yagi.loco:loco-smasher-filterable-gson:1.0.0'

	// optional, persist the log using sqlite on android.
    implementation 'com.sys1yagi.loco:loco-store-android-sqlite:1.0.0'
}
```

# How to use

```kotlin
fun startLoco() {
	Loco.start(
		LocoConfig(
			store = InMemoryStore(), // persistent layer for buffering logs.
			smasher = GsonSmasher(Gson()), // serializer for logs.
			senders = listOf(
				StdOutSender() // log senders
			),
			scheduler = IntervalSendingScheduler(5000L) // sending interval scheduler
		) {
			// mapping logs to sender
			logToSender[StdOutSender::class] = listOf(
				ClickLog::class
			)
		}
	)

	// send logs anytime, anywhere
	Loco.send(
		ClickLog(1, "jack")
	)
}

data class ClickLog(
	val id: Int,
	val name: String
) : LocoLog

class GsonSmasher(val gson: Gson) : Smasher {
	override fun smash(log: LocoLog): String {
		return gson.toJson(log)
	}
}

class IntervalSendingScheduler(val interval: Long) : SendingScheduler {
	override suspend fun schedule(
		latestResults: List<Pair<Sender, SendingResult>>,
		config: LocoConfig,
		offer: () -> Unit
	) {
		delay(interval)
		offer()
	}
}

class StdOutSender : Sender {
	override suspend fun send(logs: List<SmashedLog>): SendingResult {
		logs.forEach {
			println(it.toString())
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
```

# Classes

## LocoLog

TODO

## Smasher

TODO

## SmashedLog

TODO

## Sender

TODO

## Store

TODO

## SendingScheduler

TODO

## Sender

TODO

## Internist

TODO
