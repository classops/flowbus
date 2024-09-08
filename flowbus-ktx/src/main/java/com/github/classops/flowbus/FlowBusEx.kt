package com.github.classops.flowbus

import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner


fun <T> Fragment.onEvent(event: String, sticky: Boolean = false, listener: (T) -> Unit) {
    FlowBus.on(viewLifecycleOwner, event, sticky, listener)
}

fun <T> LifecycleOwner.onEvent(event: String, sticky: Boolean = false, listener: (T) -> Unit) {
    FlowBus.on(this, event, sticky, listener)
}

fun <T> Lifecycle.onEvent(event: String, sticky: Boolean = false, listener: (T) -> Unit) {
    FlowBus.on(this, event, sticky = sticky, listener)
}