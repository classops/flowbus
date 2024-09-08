package com.github.classops.flowbus

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

/**
 * 订阅
 * 包含 监听器、Dispatcher、粘性 属性
 */
class Subscription<T>(
    val listener: Listener<T>,
    val dispatcher: CoroutineDispatcher = Dispatchers.IO,
    val sticky: Boolean = false,
)