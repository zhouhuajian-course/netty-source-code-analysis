# Netty 4.1.90.Final 源码分析 

https://netty.io/

https://github.com/netty/netty

![](readme/component.png)

## ByteBuf 分配

1. 池化 非池化 pooled unpooled

池化的好处是，可以避免频繁创建、销毁缓冲区，如果池里面有空闲的，直接拿出来用

2. 直接内存 堆内存 direct heap

直接内存的好处是，不需要受JVM垃圾回收时，移动内存的性能损耗

3. rcv_bytebuf 接受数据的缓冲区，出于性能考虑，统一使用 direct内存，  
   然后接收数据的缓冲区，有adaptive自适应功能，如果之前的数据量比较少，那么会为下一个分配比较小的缓冲区，
   相反，会分配比较大的缓冲区。  
   默认1024 最小64 最大65536

## 修改Netty示例的日志

netty-4.1.90.Final/example/src/main/resources/logback.xml

```xml
<configuration debug="false">
  <appender name="STDOUT" class="ch.qos.logback.core.ConsoleAppender">
    <encoder>
      <pattern>%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n</pattern>
    </encoder>
  </appender>

  <root level="${logLevel:-debug}">
    <appender-ref ref="STDOUT" />
  </root>
</configuration>
```

## 客户端连接服务端，连接成功

----------- 非常重要 -------------

客户端和服务端TCP三次握手后，就会是已连接状态，即使服务端没有或者还没有accept客户端。（已验证）


## 空闲连接 读写超时

```
为了能够及时的将资源释放出来，会检测空闲连接和超时。常见的方法是通过发送信息来测试一个不活跃的链接，通常被称为“心跳”，然后在远端确认它是否还活着。（还有一个方法是比较激进的，简单地断开那些指定的时间间隔的不活跃的链接）。
处理空闲连接是一项常见的任务,Netty 提供了几个 ChannelHandler 实现此目的。表8.4概述。
Table 8.4 ChannelHandlers for idle connections and timeouts
名称	描述
IdleStateHandler	如果连接闲置时间过长，则会触发 IdleStateEvent 事件。在 ChannelInboundHandler 中可以覆盖 userEventTriggered(...) 方法来处理 IdleStateEvent。
ReadTimeoutHandler	在指定的时间间隔内没有接收到入站数据则会抛出 ReadTimeoutException 并关闭 Channel。ReadTimeoutException 可以通过覆盖 ChannelHandler 的 exceptionCaught(…) 方法检测到。
WriteTimeoutHandler	WriteTimeoutException 可以通过覆盖 ChannelHandler 的 exceptionCaught(…) 方法检测到。

Netty为超时控制封装了两个类ReadTimeoutHandler和WriteTimeoutHandler，
ReadTimeoutHandler，用于控制读取数据的时候的超时，如果在设置时间段内都没有数据读取了，那么就引发超时，然后关闭当前的channel；
WriteTimeoutHandler，用于控制数据输出的时候的超时，如果在设置时间段内都没有数据写了，那么就超时。
它们都是IdleStateHandler的子类。
```


## 网络编程常见异常

1. 服务端没启动，客户端连接服务端，  
   java.net.ConnectException: Connection refused: no further information

2. 服务端已启动，客户端连接服务端超时  

> 注: 客户端和服务端三次握手后，就会是已连接状态，即使服务端还没有accept。（已验证）

```text
默认30秒 `private static final int DEFAULT_CONNECT_TIMEOUT = 30000;`

客户端 连接超时100毫秒
bootstrap.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 100)  
bootstrap.connect("www.gnu.org", 80)
异常 
16:34:26.189 [nioEventLoopGroup-2-1] DEBUG i.n.handler.logging.LoggingHandler - [id: 0x24bb2319] REGISTERED
16:34:26.190 [nioEventLoopGroup-2-1] DEBUG i.n.handler.logging.LoggingHandler - [id: 0x24bb2319] CONNECT: www.gnu.org/209.51.188.116:80
16:34:26.291 [nioEventLoopGroup-2-1] DEBUG i.n.handler.logging.LoggingHandler - [id: 0x24bb2319] CLOSE
16:34:26.293 [nioEventLoopGroup-2-1] DEBUG i.n.handler.logging.LoggingHandler - [id: 0x24bb2319] UNREGISTERED
Exception in thread "main" io.netty.channel.ConnectTimeoutException: connection timed out: www.gnu.org/209.51.188.116:80

客户端 连接超时1000毫秒
bootstrap.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 1000)  
连接成功
16:33:43.278 [nioEventLoopGroup-2-1] DEBUG i.n.handler.logging.LoggingHandler - [id: 0xafc9fb6c] REGISTERED
16:33:43.278 [nioEventLoopGroup-2-1] DEBUG i.n.handler.logging.LoggingHandler - [id: 0xafc9fb6c] CONNECT: www.gnu.org/209.51.188.116:80
16:33:43.512 [nioEventLoopGroup-2-1] DEBUG i.n.handler.logging.LoggingHandler - [id: 0xafc9fb6c, L:/192.168.1.102:65387 - R:www.gnu.org/209.51.188.116:80] ACTIVE
```
   
3. 服务端，accept队列已满，连接拒绝

```text
服务端
.option(ChannelOption.SO_BACKLOG, 2)

服务端根据 SelectionKey.OP_ACCEPT find usages，找到该代码，进行断点，默认服务端没有accept客户端  
debug server
```java
if ((readyOps & (SelectionKey.OP_READ | SelectionKey.OP_ACCEPT)) != 0 || readyOps == 0) {
    unsafe.read();
}

客户端 设置 允许多个实例

依次非debug模式 运行第一个 
16:46:32.912 [nioEventLoopGroup-2-1] DEBUG i.n.handler.logging.LoggingHandler - [id: 0xebb258eb] REGISTERED
16:46:32.912 [nioEventLoopGroup-2-1] DEBUG i.n.handler.logging.LoggingHandler - [id: 0xebb258eb] CONNECT: localhost/127.0.0.1:8080
16:46:32.917 [nioEventLoopGroup-2-1] DEBUG i.n.handler.logging.LoggingHandler - [id: 0xebb258eb, L:/127.0.0.1:50553 - R:localhost/127.0.0.1:8080] ACTIVE

第二个
16:48:17.032 [nioEventLoopGroup-2-1] DEBUG i.n.handler.logging.LoggingHandler - [id: 0x7a538be6] REGISTERED
16:48:17.032 [nioEventLoopGroup-2-1] DEBUG i.n.handler.logging.LoggingHandler - [id: 0x7a538be6] CONNECT: localhost/127.0.0.1:8080
16:48:17.036 [nioEventLoopGroup-2-1] DEBUG i.n.handler.logging.LoggingHandler - [id: 0x7a538be6, L:/127.0.0.1:50691 - R:localhost/127.0.0.1:8080] ACTIVE

第三个 Connection refused 连接拒绝 （服务端机器内核维护的一个accept queue，已满，内核会认为应用忙不过来，所以连接拒绝）
16:50:53.501 [nioEventLoopGroup-2-1] DEBUG i.n.handler.logging.LoggingHandler - [id: 0xcadf1b3e] REGISTERED
16:50:53.502 [nioEventLoopGroup-2-1] DEBUG i.n.handler.logging.LoggingHandler - [id: 0xcadf1b3e] CONNECT: localhost/127.0.0.1:8080
16:50:55.506 [nioEventLoopGroup-2-1] DEBUG i.n.handler.logging.LoggingHandler - [id: 0xcadf1b3e] CLOSE
16:50:55.507 [nioEventLoopGroup-2-1] DEBUG i.n.handler.logging.LoggingHandler - [id: 0xcadf1b3e] UNREGISTERED
Exception in thread "main" io.netty.channel.AbstractChannel$AnnotatedConnectException: Connection refused: no further information: localhost/127.0.0.1:8080
Caused by: java.net.ConnectException: Connection refused: no further information

4. 服务端已启动，客户端发送请求，长时间未得到服务端响应，调用超时

io.netty.handler.timeout.ReadTimeoutException: null

客户端 设置5秒读取超时                         
ch.pipeline().addLast(new ReadTimeoutHandler(5000, TimeUnit.MILLISECONDS));

17:14:50.605 [nioEventLoopGroup-2-1] DEBUG i.n.handler.logging.LoggingHandler - [id: 0x29411827] REGISTERED
17:14:50.605 [nioEventLoopGroup-2-1] DEBUG i.n.handler.logging.LoggingHandler - [id: 0x29411827] CONNECT: localhost/127.0.0.1:8080
17:14:50.609 [nioEventLoopGroup-2-1] DEBUG i.n.handler.logging.LoggingHandler - [id: 0x29411827, L:/127.0.0.1:60672 - R:localhost/127.0.0.1:8080] ACTIVE
---- 5秒后 ----
17:14:55.614 [nioEventLoopGroup-2-1] WARN  i.n.channel.DefaultChannelPipeline - An exceptionCaught() event was fired, and it reached at the tail of the pipeline. It usually means the last handler in the pipeline did not handle the exception.
io.netty.handler.timeout.ReadTimeoutException: null
17:14:55.616 [nioEventLoopGroup-2-1] DEBUG i.n.handler.logging.LoggingHandler - [id: 0x29411827, L:/127.0.0.1:60672 - R:localhost/127.0.0.1:8080] CLOSE
17:14:55.617 [nioEventLoopGroup-2-1] DEBUG i.n.handler.logging.LoggingHandler - [id: 0x29411827, L:/127.0.0.1:60672 ! R:localhost/127.0.0.1:8080] INACTIVE
17:14:55.617 [nioEventLoopGroup-2-1] DEBUG i.n.handler.logging.LoggingHandler - [id: 0x29411827, L:/127.0.0.1:60672 ! R:localhost/127.0.0.1:8080] UNREGISTERED

5. 服务端进程打开文件数过多

linux 
$ rz 上传 netty-server.jar
文件打开数 临时改为 18  (服务端启动后需要18个，刚好剩2个)
$ ulimit -n 18
$ ulimit -a 验证
$ java -cp netty-server.jar org.example.exception.Server
$ jps
1686 Jps
1657 Server
$ cat /proc/1657/limits 
Limit                     Soft Limit           Hard Limit           Units     
Max cpu time              unlimited            unlimited            seconds   
Max file size             unlimited            unlimited            bytes     
Max data size             unlimited            unlimited            bytes     
Max stack size            8388608              unlimited            bytes     
Max core file size        0                    unlimited            bytes     
Max resident set          unlimited            unlimited            bytes     
Max processes             3795                 3795                 processes 
Max open files            18                   18                   files     
Max locked memory         65536                65536                bytes     
Max address space         unlimited            unlimited            bytes     
Max file locks            unlimited            unlimited            locks     
Max pending signals       3795                 3795                 signals   
Max msgqueue size         819200               819200               bytes     
Max nice priority         0                    0                    
Max realtime priority     0                    0                    
Max realtime timeout      unlimited            unlimited            us   

$ ll /proc/1657/fd
total 0
lrwx------. 1 root root 64 Apr 12 18:50 0 -> /dev/pts/0
lrwx------. 1 root root 64 Apr 12 18:50 1 -> /dev/pts/0
lrwx------. 1 root root 64 Apr 12 18:50 10 -> anon_inode:[eventpoll]
lr-x------. 1 root root 64 Apr 12 18:50 11 -> pipe:[32869]
l-wx------. 1 root root 64 Apr 12 18:50 12 -> pipe:[32869]
lrwx------. 1 root root 64 Apr 12 18:50 13 -> anon_inode:[eventpoll]
lrwx------. 1 root root 64 Apr 12 18:50 14 -> socket:[32873]
lrwx------. 1 root root 64 Apr 12 18:50 15 -> socket:[32884]
lrwx------. 1 root root 64 Apr 12 18:50 2 -> /dev/pts/0
lr-x------. 1 root root 64 Apr 12 18:50 3 -> /rocketmq/jdk1.8.0_351/jre/lib/rt.jar
lr-x------. 1 root root 64 Apr 12 18:50 4 -> /root/netty-server.jar
lr-x------. 1 root root 64 Apr 12 18:50 5 -> pipe:[32867]
l-wx------. 1 root root 64 Apr 12 18:50 6 -> pipe:[32867]
lrwx------. 1 root root 64 Apr 12 18:50 7 -> anon_inode:[eventpoll]
lr-x------. 1 root root 64 Apr 12 18:50 8 -> pipe:[32868]
l-wx------. 1 root root 64 Apr 12 18:50 9 -> pipe:[32868]
$ ll /proc/1657/fd | wc -l
17
(  -l, --lines print the newline counts 数多少行)
(total 0 减掉 -1)
最大索引为15，此时共打开16个文件，还剩下2个

IDEA 客户端连接服务端 
bootstrap.connect("192.168.1.206", 8080) 去掉ReadTimeoutHandler

1. 第一个客户端

服务端日志
18:52:20.772 [nioEventLoopGroup-3-1] DEBUG i.n.handler.logging.LoggingHandler -- [id: 0x2643646e, L:/192.168.1.206:8080 - R:/192.168.1.102:56603] REGISTERED
18:52:20.772 [nioEventLoopGroup-3-1] DEBUG i.n.handler.logging.LoggingHandler -- [id: 0x2643646e, L:/192.168.1.206:8080 - R:/192.168.1.102:56603] ACTIVE

多了一个 16 -> socket:[33272]

[root@centos /root]# ll /proc/1657/fd
total 0
lrwx------. 1 root root 64 Apr 12 18:50 0 -> /dev/pts/0
lrwx------. 1 root root 64 Apr 12 18:50 1 -> /dev/pts/0
lrwx------. 1 root root 64 Apr 12 18:50 10 -> anon_inode:[eventpoll]
lr-x------. 1 root root 64 Apr 12 18:50 11 -> pipe:[32869]
l-wx------. 1 root root 64 Apr 12 18:50 12 -> pipe:[32869]
lrwx------. 1 root root 64 Apr 12 18:50 13 -> anon_inode:[eventpoll]
lrwx------. 1 root root 64 Apr 12 18:50 14 -> socket:[32873]
lrwx------. 1 root root 64 Apr 12 18:50 15 -> socket:[32884]
lrwx------. 1 root root 64 Apr 12 18:52 16 -> socket:[33272]
lrwx------. 1 root root 64 Apr 12 18:50 2 -> /dev/pts/0
lr-x------. 1 root root 64 Apr 12 18:50 3 -> /rocketmq/jdk1.8.0_351/jre/lib/rt.jar
lr-x------. 1 root root 64 Apr 12 18:50 4 -> /root/netty-server.jar
lr-x------. 1 root root 64 Apr 12 18:50 5 -> pipe:[32867]
l-wx------. 1 root root 64 Apr 12 18:50 6 -> pipe:[32867]
lrwx------. 1 root root 64 Apr 12 18:50 7 -> anon_inode:[eventpoll]
lr-x------. 1 root root 64 Apr 12 18:50 8 -> pipe:[32868]
l-wx------. 1 root root 64 Apr 12 18:50 9 -> pipe:[32868]

2. 启动第二个客户端

出现太多打开文件异常，当貌似还是连上了
java.io.IOException: Too many open files

多了 17 -> socket:[33914]

18:53:23.225 [nioEventLoopGroup-2-1] WARN  i.n.channel.DefaultChannelPipeline -- An exceptionCaught() event was fired, and it reached at the tail of the pipeline. It usually means the last handler in the pipeline did not handle the exception.
java.io.IOException: Too many open files
	at sun.nio.ch.ServerSocketChannelImpl.accept0(Native Method)
	at sun.nio.ch.ServerSocketChannelImpl.accept(ServerSocketChannelImpl.java:424)
	at sun.nio.ch.ServerSocketChannelImpl.accept(ServerSocketChannelImpl.java:252)
	at io.netty.util.internal.SocketUtils$5.run(SocketUtils.java:119)
	at io.netty.util.internal.SocketUtils$5.run(SocketUtils.java:116)
	at java.security.AccessController.doPrivileged(Native Method)
	at io.netty.util.internal.SocketUtils.accept(SocketUtils.java:116)
	at io.netty.channel.socket.nio.NioServerSocketChannel.doReadMessages(NioServerSocketChannel.java:154)
	at io.netty.channel.nio.AbstractNioMessageChannel$NioMessageUnsafe.read(AbstractNioMessageChannel.java:79)
	at io.netty.channel.nio.NioEventLoop.processSelectedKey(NioEventLoop.java:788)
	at io.netty.channel.nio.NioEventLoop.processSelectedKeysOptimized(NioEventLoop.java:724)
	at io.netty.channel.nio.NioEventLoop.processSelectedKeys(NioEventLoop.java:650)
	at io.netty.channel.nio.NioEventLoop.run(NioEventLoop.java:562)
	at io.netty.util.concurrent.SingleThreadEventExecutor$4.run(SingleThreadEventExecutor.java:997)
	at io.netty.util.internal.ThreadExecutorMap$2.run(ThreadExecutorMap.java:74)
	at io.netty.util.concurrent.FastThreadLocalRunnable.run(FastThreadLocalRunnable.java:30)
	at java.lang.Thread.run(Thread.java:750)
18:53:23.229 [nioEventLoopGroup-3-2] DEBUG i.n.handler.logging.LoggingHandler -- [id: 0xdd8abe2a, L:/192.168.1.206:8080 - R:/192.168.1.102:56839] REGISTERED
18:53:23.229 [nioEventLoopGroup-3-2] DEBUG i.n.handler.logging.LoggingHandler -- [id: 0xdd8abe2a, L:/192.168.1.206:8080 - R:/192.168.1.102:56839] ACTIVE

$ ll /proc/1749/fd
total 0
lrwx------. 1 root root 64 Apr 12 18:58 0 -> /dev/pts/0
lrwx------. 1 root root 64 Apr 12 18:58 1 -> /dev/pts/0
lrwx------. 1 root root 64 Apr 12 18:58 10 -> anon_inode:[eventpoll]
lr-x------. 1 root root 64 Apr 12 18:58 11 -> pipe:[33820]
l-wx------. 1 root root 64 Apr 12 18:58 12 -> pipe:[33820]
lrwx------. 1 root root 64 Apr 12 18:58 13 -> anon_inode:[eventpoll]
lrwx------. 1 root root 64 Apr 12 18:58 14 -> socket:[33824]
lrwx------. 1 root root 64 Apr 12 18:58 15 -> socket:[33835]
lrwx------. 1 root root 64 Apr 12 18:58 16 -> socket:[33870]
lrwx------. 1 root root 64 Apr 12 18:58 17 -> socket:[33914]
lrwx------. 1 root root 64 Apr 12 18:58 2 -> /dev/pts/0
lr-x------. 1 root root 64 Apr 12 18:58 3 -> /rocketmq/jdk1.8.0_351/jre/lib/rt.jar
lr-x------. 1 root root 64 Apr 12 18:58 4 -> /root/netty-server.jar
lr-x------. 1 root root 64 Apr 12 18:58 5 -> pipe:[33818]
l-wx------. 1 root root 64 Apr 12 18:58 6 -> pipe:[33818]
lrwx------. 1 root root 64 Apr 12 18:58 7 -> anon_inode:[eventpoll]
lr-x------. 1 root root 64 Apr 12 18:58 8 -> pipe:[33819]
l-wx------. 1 root root 64 Apr 12 18:58 9 -> pipe:[33819]

3. 启动第三个客户端，客户端一切正常，服务端不正常

客户端日志
18:58:49.597 [nioEventLoopGroup-2-1] DEBUG i.n.handler.logging.LoggingHandler - [id: 0x53104b66] REGISTERED
18:58:49.597 [nioEventLoopGroup-2-1] DEBUG i.n.handler.logging.LoggingHandler - [id: 0x53104b66] CONNECT: /192.168.1.206:8080
18:58:49.601 [nioEventLoopGroup-2-1] DEBUG i.n.handler.logging.LoggingHandler - [id: 0x53104b66, L:/192.168.1.102:57378 - R:/192.168.1.206:8080] ACTIVE

服务端日志 约每秒发一条警告 太多打开文件

$ ll /proc/1749/fd
没有增加额外的打开文件描述符，还是18个

$ ll /proc/1749/fd
total 0
lrwx------. 1 root root 64 Apr 12 18:58 0 -> /dev/pts/0
lrwx------. 1 root root 64 Apr 12 18:58 1 -> /dev/pts/0
lrwx------. 1 root root 64 Apr 12 18:58 10 -> anon_inode:[eventpoll]
lr-x------. 1 root root 64 Apr 12 18:58 11 -> pipe:[33820]
l-wx------. 1 root root 64 Apr 12 18:58 12 -> pipe:[33820]
lrwx------. 1 root root 64 Apr 12 18:58 13 -> anon_inode:[eventpoll]
lrwx------. 1 root root 64 Apr 12 18:58 14 -> socket:[33824]
lrwx------. 1 root root 64 Apr 12 18:58 15 -> socket:[33835]
lrwx------. 1 root root 64 Apr 12 18:58 16 -> socket:[33870]
lrwx------. 1 root root 64 Apr 12 18:58 17 -> socket:[33914]
lrwx------. 1 root root 64 Apr 12 18:58 2 -> /dev/pts/0
lr-x------. 1 root root 64 Apr 12 18:58 3 -> /rocketmq/jdk1.8.0_351/jre/lib/rt.jar
lr-x------. 1 root root 64 Apr 12 18:58 4 -> /root/netty-server.jar
lr-x------. 1 root root 64 Apr 12 18:58 5 -> pipe:[33818]
l-wx------. 1 root root 64 Apr 12 18:58 6 -> pipe:[33818]
lrwx------. 1 root root 64 Apr 12 18:58 7 -> anon_inode:[eventpoll]
lr-x------. 1 root root 64 Apr 12 18:58 8 -> pipe:[33819]
l-wx------. 1 root root 64 Apr 12 18:58 9 -> pipe:[33819]


19:00:37.843 [nioEventLoopGroup-2-1] WARN  i.n.channel.DefaultChannelPipeline -- An exceptionCaught() event was fired, and it reached at the tail of the pipeline. It usually means the last handler in the pipeline did not handle the exception.
java.io.IOException: Too many open files
	at sun.nio.ch.ServerSocketChannelImpl.accept0(Native Method)
	at sun.nio.ch.ServerSocketChannelImpl.accept(ServerSocketChannelImpl.java:424)
	at sun.nio.ch.ServerSocketChannelImpl.accept(ServerSocketChannelImpl.java:252)
	at io.netty.util.internal.SocketUtils$5.run(SocketUtils.java:119)
	at io.netty.util.internal.SocketUtils$5.run(SocketUtils.java:116)
	at java.security.AccessController.doPrivileged(Native Method)
	at io.netty.util.internal.SocketUtils.accept(SocketUtils.java:116)
	at io.netty.channel.socket.nio.NioServerSocketChannel.doReadMessages(NioServerSocketChannel.java:154)
	at io.netty.channel.nio.AbstractNioMessageChannel$NioMessageUnsafe.read(AbstractNioMessageChannel.java:79)
	at io.netty.channel.nio.NioEventLoop.processSelectedKey(NioEventLoop.java:788)
	at io.netty.channel.nio.NioEventLoop.processSelectedKeysOptimized(NioEventLoop.java:724)
	at io.netty.channel.nio.NioEventLoop.processSelectedKeys(NioEventLoop.java:650)
	at io.netty.channel.nio.NioEventLoop.run(NioEventLoop.java:562)
	at io.netty.util.concurrent.SingleThreadEventExecutor$4.run(SingleThreadEventExecutor.java:997)
	at io.netty.util.internal.ThreadExecutorMap$2.run(ThreadExecutorMap.java:74)
	at io.netty.util.concurrent.FastThreadLocalRunnable.run(FastThreadLocalRunnable.java:30)
	at java.lang.Thread.run(Thread.java:750)

4. 启动第四个客户端，并往服务端写数据，客户端一切正常

linux没增加文件描述符 $ ll /proc/1749/fd
依然总共18个

客户端日志
19:03:29.579 [nioEventLoopGroup-2-1] DEBUG i.n.handler.logging.LoggingHandler - [id: 0xbcbe2182] REGISTERED
19:03:29.579 [nioEventLoopGroup-2-1] DEBUG i.n.handler.logging.LoggingHandler - [id: 0xbcbe2182] CONNECT: /192.168.1.206:8080
19:03:29.583 [nioEventLoopGroup-2-1] DEBUG i.n.handler.logging.LoggingHandler - [id: 0xbcbe2182, L:/192.168.1.102:57680 - R:/192.168.1.206:8080] ACTIVE
19:03:29.587 [nioEventLoopGroup-2-1] DEBUG io.netty.util.Recycler - -Dio.netty.recycler.maxCapacityPerThread: 4096
19:03:29.587 [nioEventLoopGroup-2-1] DEBUG io.netty.util.Recycler - -Dio.netty.recycler.ratio: 8
19:03:29.587 [nioEventLoopGroup-2-1] DEBUG io.netty.util.Recycler - -Dio.netty.recycler.chunkSize: 32
19:03:29.587 [nioEventLoopGroup-2-1] DEBUG io.netty.util.Recycler - -Dio.netty.recycler.blocking: false
19:03:29.587 [nioEventLoopGroup-2-1] DEBUG io.netty.util.Recycler - -Dio.netty.recycler.batchFastThreadLocalOnly: true
19:03:29.596 [nioEventLoopGroup-2-1] DEBUG io.netty.buffer.AbstractByteBuf - -Dio.netty.buffer.checkAccessible: true
19:03:29.596 [nioEventLoopGroup-2-1] DEBUG io.netty.buffer.AbstractByteBuf - -Dio.netty.buffer.checkBounds: true
19:03:29.597 [nioEventLoopGroup-2-1] DEBUG i.n.util.ResourceLeakDetectorFactory - Loaded default ResourceLeakDetector: io.netty.util.ResourceLeakDetector@55c601cb
19:03:29.603 [nioEventLoopGroup-2-1] DEBUG i.n.handler.logging.LoggingHandler - [id: 0xbcbe2182, L:/192.168.1.102:57680 - R:/192.168.1.206:8080] WRITE: 4
19:03:29.603 [nioEventLoopGroup-2-1] DEBUG i.n.handler.logging.LoggingHandler - [id: 0xbcbe2182, L:/192.168.1.102:57680 - R:/192.168.1.206:8080] FLUSH

```

## IDEA 打包 在 linux上运行

1. pom.xml 增加

```xml
<build>
    <plugins>
      <plugin>
        <groupId>org.apache.maven.plugins</groupId>
        <artifactId>maven-dependency-plugin</artifactId>
        <version>3.0.1</version>
        <executions>
          <execution>
            <id>copy-dependencies</id>
            <phase>package</phase>
            <goals>
              <goal>copy-dependencies</goal>
            </goals>
            <configuration>
              <outputDirectory>${project.build.directory}/lib</outputDirectory>
              <overWriteReleases>false</overWriteReleases>
              <overWriteSnapshots>false</overWriteSnapshots>
              <overWriteIfNewer>true</overWriteIfNewer>
            </configuration>
          </execution>
        </executions>
      </plugin>
    </plugins>
  </build>
```

2. maven - netty-test - package

3. file -> project structure -> artifacts -> add -> empty

4. add -> module output 选 netty-test 

5. add -> external libs 选 target/lib下的所有jar包

6. build -> build artifacts 

7. out/artifacts/...

8. java -cp netty-server.jar org.example.exception.Server

## ulimit 

```shell
ulimit 限制的是单个进程process的资源，并不是单个用户会话，不是单个用户，也不是整个linux
例如 -n	the maximum number of open file descriptors
限制的是单个进制的最大打开文件描述符数，linux为了包含系统，默认每个进程最大1024个打开文件描述符
$ ulimit -a
core file size          (blocks, -c) 0
data seg size           (kbytes, -d) unlimited
scheduling priority             (-e) 0
file size               (blocks, -f) unlimited
pending signals                 (-i) 7183
max locked memory       (kbytes, -l) 64
max memory size         (kbytes, -m) unlimited
open files                      (-n) 1024
pipe size            (512 bytes, -p) 8
POSIX message queues     (bytes, -q) 819200
real-time priority              (-r) 0
stack size              (kbytes, -s) 8192
cpu time               (seconds, -t) unlimited
max user processes              (-u) 7183
virtual memory          (kbytes, -v) unlimited
file locks                      (-x) unlimited

$ cat /proc/$pid/limits
可查看运行中的进程的资源限制
```

未实际验证

```java
class Ulimit{
    public static void main( String[] args ) throws IOException, InterruptedException
    {
        List<FileInputStream> fileList = new ArrayList<FileInputStream>();
        for(int i=0;i<800;i++) {
            File temp = File.createTempFile("ulimit-test", ".txt");
            fileList.add(new FileInputStream(temp));
            System.out.println("file_seq=" + i + " " + temp.getAbsolutePath());  
        }
        // keep it running, so we can inspect it.
        Thread.sleep(Integer.MAX_VALUE);
    }
}

// Exception in thread "main" java.io.IOException: Too many open files
// at java.io.UnixFileSystem.createFileExclusively(Native Method)
// at java.io.File.createTempFile(File.java:2024)
// at java.io.File.createTempFile(File.java:2070)
```

## Inbound入站 和 Outbound 出站

ch.write 从tail处理器，往回传write事件

```text
11:47:52.568 [nioEventLoopGroup-3-1] DEBUG org.example.TestPipeline -- ChannelHandlerContext(h1, [id: 0xa2d8a4c2, L:/127.0.0.1:8080 - R:/127.0.0.1:54145])
11:47:55.164 [nioEventLoopGroup-3-1] DEBUG org.example.TestPipeline -- 1
11:47:55.165 [nioEventLoopGroup-3-1] DEBUG org.example.TestPipeline -- 2
11:47:55.165 [nioEventLoopGroup-3-1] DEBUG org.example.TestPipeline -- 3 TestPipeline.Student(name=zhangsan) class org.example.TestPipeline$Student
11:47:55.166 [nioEventLoopGroup-3-1] DEBUG org.example.TestPipeline -- 6
11:47:55.166 [nioEventLoopGroup-3-1] DEBUG org.example.TestPipeline -- 5
11:47:55.166 [nioEventLoopGroup-3-1] DEBUG org.example.TestPipeline -- 4
```

ctx.write 从当前处理器，往回传write事件

```text
11:49:34.965 [nioEventLoopGroup-3-1] DEBUG org.example.TestPipeline -- ChannelHandlerContext(h1, [id: 0xdb7a686d, L:/127.0.0.1:8080 - R:/127.0.0.1:54519])
11:49:36.958 [nioEventLoopGroup-3-1] DEBUG org.example.TestPipeline -- 1
11:49:36.959 [nioEventLoopGroup-3-1] DEBUG org.example.TestPipeline -- 2
11:49:36.960 [nioEventLoopGroup-3-1] DEBUG org.example.TestPipeline -- 3 TestPipeline.Student(name=zhangsan) class org.example.TestPipeline$Student
```

## Jdk Future 和 Netty Future Promise 区别

future promise 获取异步任务的执行结果，异步任务是指，当前线程把任务提交给另一个线程去完成

一般需要配合 Callable 对象使用，一个future和一个callable对象关联

都可以理解为是装载线程结果的容器

Jdk Future 当前线程同步等待另一个线程执行任务的结果   future.get()  
Netty Future 当前线程同步或异步等待另一个线程执行任务的结果 future.get() future.addListener()  
Netty Promise 当前线程同步或异步等待另一个线程执行任务的结果，跟future不同的是，它可以主动设置另一个线程执行任务的结果

Future 一般被动创建，被动设置结果
Promise 一般主动创建，主动设置结果

注：在异常处理上也会有差别 

## execute 和 submit 区别

execute 只可提交 Runnable 没返回值任务，而且方法调用没有返回值
submit 可提交 Runnable 没返回值任务，也可以提交 Callable 有返回值任务，方法调用返回Future对象  

## 重要类

1. ServerBootstrap、BootStrap 启动类，门面类，门面设计模式，这个类是方便用户快速使用Netty的门面类
2. Channel 通道类，如果是服务端，那么Channel是NioServerSocketChannel，或者时服务端和客户端的通道NioSocketChannel，如果是客户端Channel是客户端与服务端的通道NioSocketChannel
3. Pipeline 每个Channel都有一个流水线，每个流水线有多个处理器，当Channel有事件发生时，每个事件会经过流水线的每一个处理器，
   事件的触发由pipeline对象触发，例如pipeline.fireChannelRegistered();  
   ServerSocketChannel会有一个Pipeline，  
   每个服务端与客户端的SocketChannel也有一个Pipeline  
   pipeline使用链表方式连接所有handler的上下文对象，  
   每个pipeline都有初始的head和tail  
   调用pipeline.addLast()，会往head和tail中间以链表的方式追加  
   始终保证head在链表最开始，tail在链表最后面，  
   也就是始终保证head先执行，tail最后执行，中间的其他Handler按addLast顺序依次执行，最后一个自定义的Handler的Context的next会指向tail  
   head -> h1 -> h2 -> h3 -> h4 -> h5 -> h6 -> tail  
   双向链表  
   入站时，例如有ChannelRead事件，数据会从head一直流向tail，除非中间断开  
   出站时，例如有write事件，数据会从tail一直流向head，除非中间断开  
          如果是channel.write，会从tail一直流向head  
          如果是context.write，会从当前的处理器一直流向head  
4. Handler 流水线里面的事件处理器，分入站、出站
5. EventLoopGroup 事件循环组，里面有多个EventLoop对象，每个EventLoop对象绑定一个线程 类比JDK ExecutorService
6. ByteBuf 字节缓冲区 字节数组

## 服务端启动、接受客户端连接、读取消息、响应消息、断开连接源码

调试代码 netty-4.1.90.Final/example/src/main/java/io/netty/example/simple

1. ServerBootstrap bootstrap = new ServerBootstrap();

服务端创建ServerBootstrap，门面类，方便使用Netty

2. bootstrap.group(bossGroup, workerGroup)

EventLoopGroup bossGroup = new NioEventLoopGroup(1);  接受客户端连接，线程数一个即可
EventLoopGroup workerGroup = new NioEventLoopGroup();

bossGroup里面children只有一个NioEventLoop  
workerGroup里面children有24个NioEventLoop (CPU 12核*2)

![](readme/event-loop-group-children.png)

bootstrap的group设置为bossGroup，一个Acceptor，用来接受客户端连接，并把与客户端Channel相关的事件交于workGroup处理
bootstrap的chindGroup设置为workerGroup，一个客户端工作组，处理客户端Channel的各种事件

3. bootstrap.channel(NioServerSocketChannel.class)

设置服务端Channel由那个类创建，

NioServerSocketChannel的一个父类AbstractNioChannel，有个属性ch，记录者Java的Channel对象，例如java.nio.channels.ServerSocketChannel

java.nio.channels.ServerSocketChannel是SelectableChannel的子类

SelectableChannel也就是可以使用多路复用器Selector的Channel

```java
public abstract class AbstractNioChannel extends AbstractChannel {
    // ...
    private final SelectableChannel ch;
    // ...
}
```

另外它的父类AbstractChannel还有个pipeline属性，每个Channel只有一个pipeline

public abstract class AbstractChannel extends DefaultAttributeMap implements Channel {

```java
public abstract class AbstractChannel extends DefaultAttributeMap implements Channel {
    // ...
    private final DefaultChannelPipeline pipeline;
    // ...
}
```

4. bootstrap.option(ChannelOption.SO_BACKLOG, 100)

设置一些服务端Channel通道选项

有链表的HashMap，键是ChannelOption，值是具体的值

```java
public abstract class AbstractBootstrap<B extends AbstractBootstrap<B, C>, C extends Channel> implements Cloneable {
    // ...
    private final Map<ChannelOption<?>, Object> options = new LinkedHashMap<ChannelOption<?>, Object>();
    // ...
}
```

ChannelOption.SO_BACKLOG对应的是tcp/ip协议, listen函数 中的 backlog 参数，用来初始化服务端可连接队列。

```
// backlog 指定了内核为此套接口排队的最大连接个数；
// 对于给定的监听套接口，内核要维护两个队列: 未连接队列和已连接队列
// backlog 的值即为未连接队列和已连接队列的和。
listen(int socketfd,int backlog)
```

5. bootstrap.childHandler()

ServerBootstrap有childHandler属性，其父类AbstractBootstrap有handler属性

每个ServerBootstrap只有一个handler和一个childHandler

> option和childOption group和childGroup handler和childHandler

handler处理客户端连接的事情，childHandler处理客户端连接后的事情

handler是通道处理器ChannelHandler，需要是ChannelHandler或其子类的实例

```java
public class ServerBootstrap extends AbstractBootstrap<ServerBootstrap, ServerChannel> {
    // ...
    private final Map<ChannelOption<?>, Object> childOptions = new LinkedHashMap<ChannelOption<?>, Object>();
    private final Map<AttributeKey<?>, Object> childAttrs = new ConcurrentHashMap<AttributeKey<?>, Object>();
    private volatile EventLoopGroup childGroup;
    private volatile ChannelHandler childHandler;
    // ...
}
public abstract class AbstractBootstrap<B extends AbstractBootstrap<B, C>, C extends Channel> implements Cloneable {
    // ...
    private final Map<ChannelOption<?>, Object> options = new LinkedHashMap<ChannelOption<?>, Object>();
    private final Map<AttributeKey<?>, Object> attrs = new ConcurrentHashMap<AttributeKey<?>, Object>();
    volatile EventLoopGroup group;
    private volatile ChannelHandler handler;
    // ...
}
```

6.  bootstrap.bind(8080) 

返回 ChannelFuture 对象

绑定端口 8080，使用指定的NioServerSocketChannel创建服务端Channel，初始化服务端Channel

监听客户端连接

```java

AbstactBootstrap.java

    private ChannelFuture doBind(final SocketAddress localAddress) {
        // 初始化 和 注册
        final ChannelFuture regFuture = initAndRegister();
    }
    
    final ChannelFuture initAndRegister() {
        Channel channel = null;
        try {
            // 创建了ServerSocketChannel 使用NioServerSocketChannel
            // 并创建了ServerSocketChannel的Pipeline
            channel = channelFactory.newChannel();
            // 初始化channel
            init(channel);
        } catch (Throwable t) {
        }
    }

AbstractNioChannel.java    
    
    protected DefaultChannelPipeline newChannelPipeline() {
        return new DefaultChannelPipeline(this);
    }
    
DefaultChannelPipeline.java    
    
    protected DefaultChannelPipeline(Channel channel) {
        this.channel = ObjectUtil.checkNotNull(channel, "channel");
        succeededFuture = new SucceededChannelFuture(channel, null);
        voidPromise =  new VoidChannelPromise(channel, true);
        // 尾上下文
        tail = new TailContext(this);
        // 头上下文
        head = new HeadContext(this);
        // 头的下一个是尾
        head.next = tail;
        // 尾的上一个是头
        tail.prev = head;
    }    
    
ServerBootstrap.java    
    
    void init(Channel channel) {
        // 设置通道选项
        setChannelOptions(channel, newOptionsArray(), logger);
        // 设置通道属性
        setAttributes(channel, newAttributesArray());
        // 获取通道的流水线
        ChannelPipeline p = channel.pipeline();
        // 当前子事件循环组
        final EventLoopGroup currentChildGroup = childGroup;
        // 当前子处理器
        final ChannelHandler currentChildHandler = childHandler;
        final Entry<ChannelOption<?>, Object>[] currentChildOptions = newOptionsArray(childOptions);
        final Entry<AttributeKey<?>, Object>[] currentChildAttrs = newAttributesArray(childAttrs);
        // ServerSocketChannel流水线最后增加一个处理器
        // 默认已经有head和tail，各个Handler被封装成Context，各个Context使用链表的方式连接
        // pipeline使用head tail     
        p.addLast(new ChannelInitializer<Channel>() {
            @Override  // 初始化NioServerSocketChannel
            public void initChannel(final Channel ch) {
                // 获取pipeline
                final ChannelPipeline pipeline = ch.pipeline();
                // 获取bootstrap的handler属性
                // 没设置，则为null
                ChannelHandler handler = config.handler();
                if (handler != null) {
                    pipeline.addLast(handler);
                }
                // ch.eventLoop() 获取到 eventLoop属性
                // 调试可知，这个eventLoop就是NioEventLoopGroup bossGroup里面的一个唯一的eventLoop
                // 执行任务
                ch.eventLoop().execute(new Runnable() {
                    @Override
                    public void run() {
                        // 流水线添加最后一个处理器
                        // ServerBootstrapAcceptor 接受客户端连接
                        pipeline.addLast(new ServerBootstrapAcceptor(
                                // NioServerSocketChannle
                                // 子组 子处理器 子选项 子属性
                                ch, currentChildGroup, currentChildHandler, currentChildOptions, currentChildAttrs));
                    }
                });
            }
        });
    }
    
ServerBootstrap.java
    
    ServerBootstrapAcceptor   
    // channelRead事件处理
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
            // 这里的msg是与客户端建立连接后的NioSocketChannel对象
            // msg = {NioSocketChannel@1954} "[id: 0x76a90a12, L:/127.0.0.1:8080 - R:/127.0.0.1:54323]"
            final Channel child = (Channel) msg;
            // 与客户端的channel的流水线添加最后一个 
            // 把之前配置的childHandler添加进去
            child.pipeline().addLast(childHandler);
            // 设置子选项 子属性
            // NioSocketChannel是由NioServerSocketChannel里面进行创建
            // 所以channel有一定的子父关系
            setChannelOptions(child, childOptions, logger);
            setAttributes(child, childAttrs);

            try {
                // 子组注册这个Channel，监听其读事件以及其他事件
                childGroup.register(child).addListener(new ChannelFutureListener() {
                    @Override
                    public void operationComplete(ChannelFuture future) throws Exception {
                        if (!future.isSuccess()) {
                            forceClose(child, future.cause());
                        }
                    }
                });
            } catch (Throwable t) {
                forceClose(child, t);
            }
        }
        
MultithreadEventLoopGroup.java

    public ChannelFuture register(Channel channel) {
        // 下一个EventLoop对象注册Channel
        // next()是负载均衡，例如24个EventLoop，轮询算法平均分配给每个EventLoop
        // register() 与客户端的Channel和Eventloop绑定
        // 每个与客户端的Channel的所有事件都始终由一个Eventloop管理
        return next().register(channel);
    }
    
SingleThreadEventLoop.java

    public ChannelFuture register(Channel channel) {
        return register(new DefaultChannelPromise(channel, this));
    }
    
AbstractChannel.java

    private void register0(ChannelPromise promise) {
            try {
                // check if the channel is still open as it could be closed in the mean time when the register
                // call was outside of the eventLoop
                if (!promise.setUncancellable() || !ensureOpen(promise)) {
                    return;
                }
                // 第一次注册 true
                boolean firstRegistration = neverRegistered;
                // 注册
                doRegister();
                neverRegistered = false;
                // 已注册的 true
                registered = true;

                // Ensure we call handlerAdded(...) before we actually notify the promise. This is needed as the
                // user may already fire events through the pipeline in the ChannelFutureListener.
                pipeline.invokeHandlerAddedIfNeeded();
                
                safeSetSuccess(promise);
                // 流水线 触发 ChannelRegistered 事件
                pipeline.fireChannelRegistered();
                // Only fire a channelActive if the channel has never been registered. This prevents firing
                // multiple channel actives if the channel is deregistered and re-registered.
    
                // 判断Channel是否是打开状态 活跃状态
                //  return isOpen() && javaChannel().socket().isBound();
                if (isActive()) {
                    if (firstRegistration) {
                        // 第一次注册 触发 channelActive事件
                        pipeline.fireChannelActive();
                    } else if (config().isAutoRead()) {
                        // This channel was registered before and autoRead() is set. This means we need to begin read
                        // again so that we process inbound data.
                        //
                        // See https://github.com/netty/netty/issues/4805
                        beginRead();
                    }
                }
            } catch (Throwable t) {
                // Close the channel directly to avoid FD leak.
                closeForcibly();
                closeFuture.setClosed();
                safeSetFailure(promise, t);
            }
        }
        
AbstractNioChannel.java

    protected void doRegister() throws Exception {
        boolean selected = false;
        for (;;) {
            try {
                // javaChannel()是底层java.nio的SocketChannel
                // 注册到一个selector
                selectionKey = javaChannel().register(eventLoop().unwrappedSelector(), 0, this);
                return;
            } catch (CancelledKeyException e) {
                if (!selected) {
                    // Force the Selector to select now as the "canceled" SelectionKey may still be
                    // cached and not removed because no Select.select(..) operation was called yet.
                    eventLoop().selectNow();
                    selected = true;
                } else {
                    // We forced a select operation on the selector before but the SelectionKey is still cached
                    // for whatever reason. JDK bug ?
                    throw e;
                }
            }
        }
    }

DefaultChannelPipeline.java

    public final ChannelPipeline fireChannelRegistered() {
        // 调用ChannelRegisterd head头上下文
        AbstractChannelHandlerContext.invokeChannelRegistered(head);
        return this;
    }

AbstractChannelHandlerContext.java

    static void invokeChannelRegistered(final AbstractChannelHandlerContext next) {
        // 第一次next是head 处理器上下文
        EventExecutor executor = next.executor();
        if (executor.inEventLoop()) {
            next.invokeChannelRegistered();
        } else {
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    next.invokeChannelRegistered();
                }
            });
        }
    }
    
    
    private void invokeChannelRegistered() {
        if (invokeHandler()) {
            try {
                // DON'T CHANGE
                // Duplex handlers implements both out/in interfaces causing a scalability issue
                // see https://bugs.openjdk.org/browse/JDK-8180450
                final ChannelHandler handler = handler();
                final DefaultChannelPipeline.HeadContext headContext = pipeline.head;
                // 第一次是headContext
                // 调用headContext的channelRegistered 
                // 传递处理器上下文对象
                if (handler == headContext) {
                    headContext.channelRegistered(this);
                } else if (handler instanceof ChannelDuplexHandler) {
                    ((ChannelDuplexHandler) handler).channelRegistered(this);
                } else {
                    // 第二次执行这里
                    ((ChannelInboundHandler) handler).channelRegistered(this);
                }
            } catch (Throwable t) {
                // 如果处理过程出现一次，执行invokeExceptionCaught
                invokeExceptionCaught(t);
            }
        } else {
            fireChannelRegistered();
        }
    }
```

全局搜索，可以快速定位到底层

SelectionKey.OP_ACCEPT  
SelectionKey.OP_READ

## pipeline.addLast()

与客户端的SocketChannel的pipeline，  
默认
head={DefaultChannelPipeline$HeadContext@1952}
    prev=null
    next={DefaultChannelPipeline$HeadContext@1953}
tail={DefaultChannelPipeline$TailContext@1953} ChannelHandlerContext(DefaultChannelPipeline$TailContext#0, [id: 0x12d4d4a2, L:/127.0.0.1:8080 - R:/127.0.0.1:57803])
    prev={DefaultChannelPipeline$HeadContext@1952}
    next=null
底层会默认addLast(childHandler)
head={DefaultChannelPipeline$HeadContext@1952}
    prev=null
    // 这是childHander的上下文
    next={DefaultChannelHandlerContext@1959}
        prev={DefaultChannelPipeline$HeadContext@1952}
        next={DefaultChannelPipeline$TailContext@1953}
tail={DefaultChannelPipeline$TailContext@1953} ChannelHandlerContext(DefaultChannelPipeline$TailContext#0, [id: 0x12d4d4a2, L:/127.0.0.1:8080 - R:/127.0.0.1:57803])
    prev={DefaultChannelHandlerContext@1959}
    next=null
如果再一次addLast()
head={DefaultChannelPipeline$HeadContext@1952}
    prev=null
    // 这是childHander的上下文
    next={DefaultChannelHandlerContext@1959}
        prev={DefaultChannelPipeline$HeadContext@1952}
        // 这是新加的Handler上下文
        next={DefaultChannelHandlerContext@2026}  
            prev={DefaultChannelHandlerContext@1959}
            next={DefaultChannelPipeline$TailContext@1953}
tail={DefaultChannelPipeline$TailContext@1953} ChannelHandlerContext(DefaultChannelPipeline$TailContext#0, [id: 0x12d4d4a2, L:/127.0.0.1:8080 - R:/127.0.0.1:57803])
    prev={DefaultChannelHandlerContext@2026}  
    next=null

说明始终能保证head最先执行 tail最后执行，其他handler会以链表的方式，往中间追加
## java: 程序包io.netty.util.collection不存在

1. Maven -> Netty -> Common -> Lifecycle -> compile 编译 Common 模块

2. 出现 invalid newline character (expected: CRLF) [Newline]

修改父模块 pom.xml

netty-parent -> pom.xml -> check-style -> configuration

```xml
<!--两个地方-->
<configuration>
    <skip>true</skip>
    ...
</configuration>
```

3. Maven -> Netty -> Common -> Lifecycle -> compile 编译 Common 模块

```text
...
[INFO] --- forbiddenapis:2.2:check (check-forbidden-apis) @ netty-common ---
[INFO] Scanning for classes to check...
[INFO] Reading API signatures: E:\java-project\netty-source-code-analysis\netty-4.1.90.Final\common\target\dev-tools\forbidden\signatures.txt
[INFO] Loading classes to check...
[INFO] Scanning classes for violations...
[INFO] Scanned 415 (and 267 related) class file(s) for forbidden API invocations (in 1.84s), 0 error(s).
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] Total time:  14.139 s
[INFO] Finished at: 2023-04-06T16:51:12+08:00
[INFO] ------------------------------------------------------------------------

Process finished with exit code 0
```

## ChannelInboundHandlerAdapter

**基本概念**

Channel：可以理解为一个连接，每一个客户端连到服务器，都会有一个与之对应的Channel。
ChannelHandler：用来处理Channel中的各种事件。
ChannelInboundHandlerAdapter：入站ChannelHandler，即从客户端进入服务器的各种事件。
ChannelHandlerContext：每个处理事件的方法都有这个参数，可用于执行与当前Channel相关的各种操作。

```java
public class EchoServerHandler extends ChannelInboundHandlerAdapter {
    public void channelRegistered(ChannelHandlerContext ctx) {
        System.out.println("注册");
    }

    public void channelActive(ChannelHandlerContext ctx) {
        System.out.println("激活");
    }

    public void channelInactive(ChannelHandlerContext ctx) {
        System.out.println("断开");
    }

    public void channelUnregistered(ChannelHandlerContext ctx) {
        System.out.println("注销");
    }

    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        System.out.println("读取消息");
    }

    public void channelReadComplete(ChannelHandlerContext ctx) {
        System.out.println("消息读取完成");
    }

    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) {
        System.out.println("用户事件");
    }

    public void channelWritabilityChanged(ChannelHandlerContext ctx) {
        System.out.println("可写状态变更为" + ctx.channel().isWritable());
    }

    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        System.out.println("发生异常");
    }
}    
```

**入站事件介绍**

注册和激活：当客户端连接时，首先会触发注册，进行一些初始化的工作，然后激活连接，就可以收发消息了。  
断开和注销：当客户端断开时，反向操作，先断开，再注销。  
读取消息：当收到客户端消息时，首先读取，然后触发读取完成。  
发生异常：不多解释了。  
用户事件：由用户触发的各种非常规事件，根据evt的类型来判断不同的事件类型，从而进行不同的处理。  
可写状态变更：收到消息后，要回复消息，会先把回复内容写到缓冲区。而缓冲区大小是有一定限制的，当达到上限以后，可写状态就会变为否，不能再写。等缓冲区的内容被冲刷掉后，缓冲区又有了空间，可写状态又会变为是。  

_From Internet_

```text
18:44:52.833 [nioEventLoopGroup-3-1] DEBUG org.example.echo.EchoServerHandler -- Channel 注册
18:44:52.839 [nioEventLoopGroup-3-1] DEBUG org.example.echo.EchoServerHandler -- Channel 激活
18:44:52.864 [nioEventLoopGroup-3-1] DEBUG org.example.echo.EchoServerHandler -- Channel 读数据
18:44:52.864 [nioEventLoopGroup-3-1] DEBUG org.example.echo.EchoServerHandler -- Channel 所有数据读取完毕
```

## Netty中很多方法都是异步的

例如 bind connect close ...

异步是指实际的操作，不是由调用方法的线程去操作，而是交给另一个线程操作。

## Future Promise

类名以 Future Promise 结尾的，一般与异步方法配合使用，  
是一种提供访问异步操作结果的机制，可以在线程之间传递数据和异常信息。

## EventLoopGroup 默认线程数

`NettyRuntime.availableProcessors() * 2` CPU可用核数底层 `Runtime.getRuntime().availableProcessors()`

CPU可用核数 * 2

```java
private static final int DEFAULT_EVENT_LOOP_THREADS = Math.max(1, SystemPropertyUtil.getInt("io.netty.eventLoopThreads", NettyRuntime.availableProcessors() * 2));
```

> 计算密集型 推荐 线程数 CPU可用核数  
> IO密集型 推荐 线程数 CPU可用核数*2  

> 相关算法 线程数 = CPU核心数 * (1 + IO耗时 / CPU耗时)

## 官方例子

netty-4.1/example/src/main/java/io/netty/example

## other

inbound adjective /ˈɪn.baʊnd/  travelling towards a particular point  到达的；入境的；归航的；回程的  
outbound adjective /ˈaʊt.baʊnd/ travelling away from a particular point 外驶的；向外的；离港的；离开某地的
