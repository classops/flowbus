### Flow 实现 EventBus

公共方法：

- FlowBus.on
- FlowBus.off
- FlowBus.emit

#### 任务的取消

通过 `Map` ， 以 `Listener` 为 Key，在任务完成后，移除`Job`保存。


#### 泄露场景用例

1. eventListenerMap 记录所有 Event 关联的 监听器，可能 在结束后（off、），未移除导致泄露

#### 参考API设计

- https://developers.weixin.qq.com/miniprogram/dev/api/route/EventChannel.html
- 