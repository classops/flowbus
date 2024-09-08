package com.github.classops.flowbus

import android.util.Log
import androidx.annotation.AnyThread
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

typealias Listener<T> = (T) -> Unit

object FlowBus {

    private val SUBSCRIBE_STATE = Lifecycle.State.RESUMED

    internal const val TAG = "FlowBus"
    private val busScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val flowMap = ConcurrentHashMap<String, MutableSharedFlow<Any>>()

    // 监听器 对应的 Job，通过 监听器 取消任务
    private val listenerJobMap = ConcurrentHashMap<Listener<*>, Job>()

    // 事件 对应的 监听器
    private val eventSubscriptionsMap =
        ConcurrentHashMap<String, CopyOnWriteArrayList<Subscription<*>>>()

    // 粘性事件
    private val stickyEvents: MutableMap<String, Any> = ConcurrentHashMap<String, Any>()

    fun get(event: String): MutableSharedFlow<Any> {
        val flow = flowMap[event]
        return if (flow != null) {
            flow
        } else {
            MutableSharedFlow<Any>().also { flowMap[event] = it }
        }
    }

    /**
     * 对于 添加、移除操作 需要 考虑 线程安全
     */
    @AnyThread
    fun <T> addEventListener(event: String, subscription: Subscription<T>, job: Job) {
        val listenerList =
            eventSubscriptionsMap[event] ?: CopyOnWriteArrayList<Subscription<*>>().also {
                eventSubscriptionsMap[event] = it
            }
        listenerList.addIfAbsent(subscription)
        listenerJobMap[subscription.listener] = job
    }

    @AnyThread
    fun <T> removeEventListener(event: String, listener: Listener<T>) {
        Log.d(TAG, "remove event:${event} listener")
        val listenerList = eventSubscriptionsMap[event]
        // 移除事件的监听器
        if (listenerList != null) {
            val it = listenerList.iterator()
            while (it.hasNext()) {
                val subscription = it.next()
                if (subscription.listener == listener) {
                    it.remove()
                }
            }
        }
        // 取消监听 和 任务
        removeListener(listener)
    }

    @AnyThread
    fun <T> removeListener(listener: Listener<T>) {
        val job = listenerJobMap.remove(listener) ?: return
        Log.d(TAG, "remove listener")
        if (job.isActive) {
            job.cancel()
        }
    }

    fun <T> on(event: String, listener: Listener<T>) {
        on(event, false, listener)
    }

    /**
     * 设置永远监听，仅App全局，谨慎使用
     */
    fun <T> on(event: String, sticky: Boolean, listener: Listener<T>) {
        val subscription = Subscription(listener, sticky = sticky)
        val job = busScope.launch {
            collectEvent(event, subscription)
        }
        // 自动移除
        addEventListener(event, subscription, job)
        job.invokeOnCompletion {
            Log.d(TAG, "job complete")
            removeEventListener(event, listener)
        }
    }

    fun <T> on(lifecycleOwner: LifecycleOwner, event: String, listener: Listener<T>) {
        on(lifecycleOwner, event, false,  listener)
    }

    fun <T> on(
        lifecycleOwner: LifecycleOwner,
        event: String,
        sticky: Boolean = false,
        listener: Listener<T>
    ) {
        val subscription = Subscription(listener, sticky = sticky)
        val job = lifecycleOwner.lifecycleScope.launch {
            lifecycleOwner.repeatOnLifecycle(SUBSCRIBE_STATE) {
                Log.d(TAG, "started, on")
                collectEvent(event, subscription)
            }
        }
        // 添加事件到Map，注意 生命周期 即时移除 listener
        addEventListener(event, subscription, job)
        job.invokeOnCompletion {
            Log.d(TAG, "job complete")
            removeEventListener(event, listener)
        }
    }

    fun <T> on(
        lifecycle: Lifecycle,
        event: String,
        sticky: Boolean = false,
        listener: Listener<T>
    ) {
        // 监听时，直接执行 粘性 事件
        val subscription = Subscription(listener, sticky = sticky)
        val job = busScope.launch {
            lifecycle.repeatOnLifecycle(SUBSCRIBE_STATE) {
                collectEvent(event, subscription)
            }
        }
        // 添加事件到Map，注意 生命周期 即时移除 listener
        addEventListener(event, subscription, job)
        job.invokeOnCompletion {
            Log.d(TAG, "job complete")
            removeEventListener(event, listener)
        }
    }

    fun <T> once(lifecycleOwner: LifecycleOwner, event: String, listener: Listener<T>) {
        once(lifecycleOwner, event, false, listener)
    }

    fun <T> once(
        lifecycleOwner: LifecycleOwner,
        event: String,
        sticky: Boolean,
        listener: Listener<T>
    ) {
        on(lifecycleOwner, event, sticky, OnceListener(this, event, listener))
    }

    fun <T> once(event: String, listener: Listener<T>) {
        on(event, false, OnceListener(this, event, listener))
    }

    fun <T> once(event: String, sticky: Boolean, listener: Listener<T>) {
        on(event, sticky, OnceListener(this, event, listener))
    }

    fun <T> off(event: String, listener: Listener<T>) {
        // 移除 listenerJobMap 和 eventListenerMap 中，对应的 listener
        removeEventListener(event, listener)
    }

    /**
     * 移除事件所有的监听器
     */
    fun off(event: String) {
        // 移除 所有监听器
        val listenerList = eventSubscriptionsMap.remove(event) ?: return
        val it = listenerList.iterator()
        while (it.hasNext()) {
            val subscription = it.next()
            removeListener(subscription.listener)
        }
    }

    fun emit(event: String, data: Any) {
        busScope.launch {
            val flow = get(event)
            flow.emit(data)
        }
    }

    fun emitSticky(event: String, data: Any) {
        stickyEvents[event] = data
        this.emit(event, data)
    }

    fun shutdown() {
        if (busScope.isActive) {
            busScope.cancel()
        }
    }

    @Suppress("UNCHECKED_CAST")
    private suspend fun <T> collectEvent(event: String, subscription: Subscription<T>) {
        // 执行在 调度器
        withContext(subscription.dispatcher) {
            if (subscription.sticky) {
                collectStickyEvent(event, subscription)
            }
            val flow = get(event)
            flow.collect {
                val data = it as? T ?: return@collect
                Log.d(TAG, "collect event: $data")
                subscription.listener(data)
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T> collectStickyEvent(event: String, subscription: Subscription<T>) {
        val data = stickyEvents[event] as? T ?: return

        Log.d(TAG, "collect sticky event: $data")
        subscription.listener(data)
    }

}

internal class OnceListener<T>(
    private val bus: FlowBus,
    private val event: String,
    private val listener: Listener<T>
) : Listener<T> {
    override fun invoke(p1: T) {
        Log.d(FlowBus.TAG, "once invoke")
        try {
            listener.invoke(p1)
        } finally {
            bus.off(event, this)
        }
    }
}