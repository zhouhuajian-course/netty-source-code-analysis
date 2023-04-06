# Netty 4.1.90.Final 源码分析 

https://netty.io/

https://github.com/netty/netty

![img.png](readme/component.png)

## 重要组件

1. ServerBootstrap、BootStrap 启动类，门面类，门面设计模式，这个类是方便用户快速使用Netty的门面类
2. Channel 通道类，如果是服务端，那么Channel是ServerChannel或服务端和客户端的通道，如果是客户端Channel是客户端与服务端的通道
3. Pipeline 每个Channel都有一个流水线，每个流水线有多个处理器，当Channel有事件发生时，每个事件会经过流水线的每一个处理器
4. Handler 流水线里面的事件处理器
5. EventLoopGroup 事件循环组，多线程执行器

## 服务端启动、接受客户端连接、读取消息、响应消息、断开连接源码

调试代码 netty-4.1/example/src/main/java/io/netty/example/simple



## java: 程序包io.netty.util.collection不存在



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
