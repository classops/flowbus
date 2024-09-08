package com.classops.flowbus

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import com.classops.flowbus.databinding.ActivityMainBinding
import com.github.classops.flowbus.FlowBus
import com.github.classops.flowbus.onEvent

class MainActivity : AppCompatActivity() {

    private var fragment: HomeFragment? = null

    private val binding: ActivityMainBinding by lazy {
        ActivityMainBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

//        FlowBus.on<Int>("count") {
//            Log.e("MainActivity", "on: ${it}")
//        }
//
//        FlowBus.once<Int>("count") {
//            Log.e("MainActivity", "once: ${it}")
//        }

        this.lifecycle.addObserver(object : LifecycleEventObserver {
            override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
                Log.e("MainActivity", "event: $event, state: ${source.lifecycle.currentState}")
            }

        })

        binding.btnFragment.setOnClickListener {
            fragment = HomeFragment()
            fragment?.let {
                supportFragmentManager.beginTransaction()
                    .add(
                        android.R.id.content,
                        it,
                        "fragment"
                    )
                    .commit()
            }
        }

        binding.btnShowFragment.setOnClickListener {
            fragment?.let {
                supportFragmentManager.beginTransaction()
                    .setMaxLifecycle(it, Lifecycle.State.RESUMED)
                    .show(it)
                    .commit()
            }
        }

        FlowBus.on<Int>(this, "count") {
            Log.e("MainActivity", "life on: ${it}")
        }

        findViewById<Button>(R.id.btnInc).setOnClickListener {
            FlowBus.emit("count", 1)
        }

        findViewById<Button>(R.id.btnOff).setOnClickListener {
            FlowBus.off("count")
        }

        findViewById<Button>(R.id.btnMain).setOnClickListener {
            val intent = Intent(this@MainActivity, SecondActivity::class.java)
            startActivity(intent)
        }

        window.decorView.postDelayed({
            Log.e("MainActivity", "emit count")
//            FlowBus.emit("count", 2)
            FlowBus.emitSticky("count", 2)
        }, 8000L)

//        window.decorView.postDelayed({
//            Log.e("MainActivity", "off count")
//            FlowBus.off("count")
//        }, 5000L)
    }
}