package com.classops.flowbus

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.github.classops.flowbus.FlowBus

class SecondActivity : AppCompatActivity() {

    private val globalListener = { it: Int ->
        Log.e("SecondActivity", "second on: ${it}")
        findViewById<TextView>(R.id.tvCount).text = "${it}"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_second)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        findViewById<Button>(R.id.btnInc).setOnClickListener {
            FlowBus.emit("count", 2)
        }

        findViewById<Button>(R.id.btnOff).setOnClickListener {
            FlowBus.off("count")
        }

        findViewById<Button>(R.id.btnShutdown).setOnClickListener {
            FlowBus.shutdown()
        }

        // on 不使用声明周期，可能会导致 内存泄露
        FlowBus.on<Int>("count", globalListener)

        FlowBus.on<Int>(this, "count") {
            Log.e("SecondActivity", "second life on: ${it}")
            findViewById<TextView>(R.id.tvCount).text = "${it}"
        }
    }

    override fun onDestroy() {
        super.onDestroy()
//        FlowBus.off("count", globalListener)
    }
}