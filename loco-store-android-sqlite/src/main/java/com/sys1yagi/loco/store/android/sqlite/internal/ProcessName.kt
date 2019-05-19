package com.sys1yagi.loco.store.android.sqlite.internal

import android.app.ActivityManager
import android.content.Context

import java.io.BufferedReader
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStreamReader

// http://stackoverflow.com/questions/19631894/is-there-a-way-to-get-current-process-name-in-android
object ProcessName {
    fun getAndroidProcessName(context: Context): String {
        val name = findProcessNameInLinuxWay() ?: findProcessNameInAndroidWay(context)
        return name?.let { extractAndroidProcessName(it) } ?: ""
    }

    private fun extractAndroidProcessName(fullProcessName: String): String {
        val pos = fullProcessName.lastIndexOf(':')
        return if (pos != -1) {
            fullProcessName.substring(pos + 1)
        } else ""
    }

    private fun findProcessNameInLinuxWay(): String? {
        FileInputStream(
            "/proc/" + android.os.Process.myPid() + "/cmdline"
        )
            .bufferedReader()
            .use { bufferedReader ->
                try {
                    val processName = StringBuilder()
                    while (true) {
                        bufferedReader.read().takeIf { it > 0 }?.let { c ->
                            processName.append(c.toChar())
                        } ?: break
                    }
                    return processName.toString()
                } catch (e: IOException) {
                    return null
                }
            }

    }

    private fun findProcessNameInAndroidWay(context: Context): String? {
        val pid = android.os.Process.myPid()
        val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        for (processInfo in manager.runningAppProcesses) {
            if (processInfo.pid == pid) {
                return processInfo.processName
            }
        }
        return null
    }
}
