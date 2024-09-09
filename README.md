## FlowBus

使用 Flow 实现的 EventBus。


### 使用方法：

**1. 引入依赖**

```gradle
// repositories 添加 jitpack
repositories {
    google()
    mavenCentral()
    maven { url 'https://jitpack.io' }
}
```

在使用的模块中引入 `flowbus`：

```gradle
dependencies {
    implementation 'com.github.classops:flowbus:1.0.0'
}
```

**2. 代码中使用**

```kotlin
// on 监听事件
FlowBus.on<Int>("count") {
    Log.d("FlowBus", "on: ${it}")
}

// 在 Fragment/Activity 中使用，传引用可以 自动 解除订阅
FlowBus.on<Int>(this, "count") {
    Log.d("FlowBus", "on: ${it}")
}

// emit 发送事件
FlowBus.emit("count", 1)

// 发送粘性事件
FlowBus.emitSticky("count", 1)
// 发送粘性事件， 需指定 sticky 参数为 true
FlowBus.on<Int>("count", true) {
    
}

// off 取消监听，传指定的监听回调
FlowBus.off("count", listener)

// 取消 指定事件的 所有订阅，慎用！！！
FlowBus.off("count")


```