# Netty 源码分析

https://github.com/netty/netty

## EventLoopGroup 默认线程数

`NettyRuntime.availableProcessors() * 2` CPU可用核数底层 `Runtime.getRuntime().availableProcessors()`

CPU可用核数 * 2

```java
private static final int DEFAULT_EVENT_LOOP_THREADS = Math.max(1, SystemPropertyUtil.getInt("io.netty.eventLoopThreads", NettyRuntime.availableProcessors() * 2));
```

## 官方例子

netty-4.1/example/src/main/java/io/netty/example