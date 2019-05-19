package com.sys1yagi.loco.sample

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import com.google.gson.Gson
import com.sys1yagi.loco.core.Loco
import com.sys1yagi.loco.core.*
import com.sys1yagi.loco.core.internal.SmashedLog
import com.sys1yagi.loco.sample.log.ClickLog
import com.sys1yagi.loco.sample.log.ScreenLog
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Loco.send(ScreenLog(MainActivity::class.java.simpleName))
        setContentView(R.layout.activity_main)

        var i = 0
        button.setOnClickListener {
            Loco.send(ClickLog("click $i"))
            i++
        }
    }
}
