
# loco
loco (Log Coroutine) is a logging library using coroutine for Android.

[![CircleCI](https://circleci.com/gh/sys1yagi/loco.svg?style=svg)](https://circleci.com/gh/sys1yagi/loco) ![kotlin](https://img.shields.io/badge/kotlin-1.3.31-blue.svg) ![license](https://img.shields.io/github/license/sys1yagi/loco.svg?maxAge=2592000)

<img width="1215" alt="archi" src="https://user-images.githubusercontent.com/749051/57979861-0d4a5300-7a5e-11e9-8ce1-4bb5b272ce50.png">
<div>Icons made by <a href="https://www.flaticon.com/authors/smashicons" title="Smashicons">Smashicons</a> from <a href="https://www.flaticon.com/"  title="Flaticon">www.flaticon.com</a> is licensed by <a href="http://creativecommons.org/licenses/by/3.0/" title="Creative Commons BY 3.0" target="_blank">CC 3.0 BY</a></div>

# Install

Loco is not yet published to jcenter. You should add bintray url to root build.gradle like below.

```groovy
allprojects {
  repositories {
    maven { url "https://dl.bintray.com/sys1yagi/maven" }
  }
}
```

Add your necessary module.

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

Loco has several modules such as log collection and sending, schduler and more.
Each module is provided by interface and can be implemented as you want.

You can implement all modules like below:

```kotlin
fun startLoco() {
  Loco.start(
    LocoConfig(
      store = InMemoryStore(), // persistent layer for buffering logs.
      smasher = GsonSmasher(Gson()), // serializer for logs.
      senders = listOf(
        StdOutSender() // log senders
      ),
      scheduler = IntervalSendingScheduler(5000L), // sending interval scheduler
      sendingBulkSize = 20
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

# How to use for Android

There are some useful modules for Android.

```groovy
dependencies {
  // core
  implementation 'com.sys1yagi.loco:loco-core:1.0.0'
  
  // use Gson to serialize the log, and you can use filters to process the logs.
  implementation 'com.sys1yagi.loco:loco-smasher-filterable-gson:1.0.0'

  // persist the log using sqlite on android.
  implementation 'com.sys1yagi.loco:loco-store-android-sqlite:1.0.0'
}
```

All you have to do is prepare sender and scheduler, and add mappings.

```kotlin
class SampleApplication : Application() {
  override fun onCreate() {
    Loco.start(
      LocoConfig(
        store = LocoAndroidSqliteStore(), // loco-store-android-sqlite
        smasher = FilterableGsonSmasher(Gson()), // loco-smasher-filterable-gson
        senders = // ...
        scheduler = // ...
      ) {
        // ...
      }
    )
  }
}
```

See more [sample](https://github.com/sys1yagi/loco/tree/master/sample)

## Sender Mapping

You should mapping for logs and sender.


```kotlin
Loco.start(
  LocoConfig(
    store = // ..., 
    smasher = // ..., 
    senders = listOf(
      NetworkSender()
    ),
    scheduler = // ..., 
  ) {
    logToSender[NetworkSender::class] = listOf(
      ClickLog::class,
      ScreenLog::class
    )
  }
)
```

You can configure multiple Senders.

```kotlin
Loco.start(
  LocoConfig(
    store = // ..., 
    smasher = // ..., 
    senders = listOf(
      NetworkSender(),
      LogcatSender()
    ),
    scheduler = // ..., 
  ) {
    logToSender[NetworkSender::class] = listOf(
      ClickLog::class,
      ScreenLog::class
    )
    logToSender[LogcatSender::class] = listOf(
      ScreenLog::class
    )
  }
)
```

# Classes

## LocoLog

It is marker interface. Logs passed to Loco must implement LocoLog.

```kotlin
data class ClickLog(
  val id: Int,
  val name: String
) : LocoLog
```


```kotlin
Loco.send(
  ClickLog(2, "jill") // OK
)
```

## Smasher

it serializes Log.

```kotlin
interface Smasher {
  fun smash(log: LocoLog): String
}
```

The serialized log is set to SmashedLog and passed to the store.

## SmashedLog

SmashedLog has a serialized log and type names.

## Store

Store is responsible for persisting, reading and deleting SmashedLog.

```kotlin
interface Store {
  suspend fun store(log: SmashedLog)
  suspend fun load(size: Int): List<SmashedLog>
  suspend fun delete(logs: List<SmashedLog>)
}
```

```kotlin
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
## Sender

Sending `List<SmashedLog>` anywhare.

```kotlin
interface Sender {
    suspend fun send(logs: List<SmashedLog>): SendingResult
}
```

You should return `SendingResult`.

```kotlin
enum class SendingResult {
  SUCCESS, // consume log record
  FAILED, // consume log record
  RETRY // not consume log record
}
```

## SendingScheduler

The SendingScheduler determines the sending interval.
It receives the latest sending result and config.
You can decide on the next execution plan based on them.

```kotlin
interface SendingScheduler {
  suspend fun schedule(
    latestResults: List<Pair<Sender, SendingResult>>,
    config: LocoConfig,
    offer: () -> Unit
  )
}
```

The following is an example of a scheduler that runs at regular intervals.

```kotlin
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
```

## Internist

What's happening in Loco?
You can make an internist intrude.
Internist can receive inner event of Loco.

```kotlin
Loco.start(
  LocoConfig(
    store = //... ,
    smasher = //... ,
    senders = //... ,
    scheduler = //... ,
    internist = object : Internist {
      override fun onSend(locoLog: LocoLog, config: LocoConfig) {
        println("onSend")
      }

      override fun onStore(log: SmashedLog, config: LocoConfig) {
        println("onStore")
      }

      override fun onStartSending() {
        println("onStartSending")
      }

      override fun onSending(sender: Sender, logs: List<SmashedLog>, config: LocoConfig) {
        println("onSending: $sender, ${logs.size}")
      }

      override fun onEndSending(sendingResults: List<Pair<Sender, SendingResult>>, config: LocoConfig) {
        println("onStartSending")
      }
    }
  ) {
    //...
  }
)
```

# License

```
MIT License

Copyright (c) 2019 Toshihiro Yagi

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```
