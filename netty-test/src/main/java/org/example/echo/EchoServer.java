package org.example.echo;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.util.CharsetUtil;
import lombok.extern.slf4j.Slf4j;

/**
 * Echoes back any received data from a client
 */
public class EchoServer {

    public static void main(String[] args) throws Exception {
        // 配置服务器
        EventLoopGroup bossGroup = new NioEventLoopGroup(1);
        EventLoopGroup workerGroup = new NioEventLoopGroup();  // 线程数默认CPU核数*2
        // 服务端处理器
        EchoServerHandler serverHandler = new EchoServerHandler();
        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup)
             .channel(NioServerSocketChannel.class)
             .option(ChannelOption.SO_BACKLOG, 100)
             .handler(new LoggingHandler(LogLevel.INFO))
             .childHandler(new ChannelInitializer<SocketChannel>() {
                 @Override
                 protected void initChannel(SocketChannel ch) throws Exception {
                     ChannelPipeline p = ch.pipeline();
                     p.addLast(new LoggingHandler(LogLevel.INFO));
                     p.addLast(serverHandler);
                 }
             });
            // Start the server.
            ChannelFuture f = b.bind(8007).sync();
            // Wait until the server socket is closed
            f.channel().closeFuture().sync();
        } finally {
            // Shut down all event loops to terminate all threads
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }
}

@Slf4j
class EchoServerHandler extends ChannelInboundHandlerAdapter {
    // 当有客户端连接时，先注册客户端，注册往后，触发channelRegistered
    @Override
    public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
        log.debug("Channel 注册");
        super.channelRegistered(ctx);
    }

    // 当客户端注册完后，会激活连接，触发channelActive
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        log.debug("Channel 激活");
        super.channelActive(ctx);
    }

    // 客户端有数据可读时，首先触发channelRead，因为数据分多次读取，所以channelRead可能触发多次
    // 所有数据读取完时，触发channelReadComplete
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {

        log.debug("Channel 读数据");
        log.debug("{}", ((ByteBuf) msg).toString(CharsetUtil.UTF_8));
        ByteBuf buffer = Unpooled.buffer(3);
        buffer.writeBytes(new byte[]{65, 66, 67});
        ctx.write(buffer);
    }

    @Override
    public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {

        log.debug("Channel 已取消注册");
        super.channelUnregistered(ctx);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        log.debug("Channel 非激活状态");
        super.channelInactive(ctx);
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        log.debug("Channel 用户事件触发了");
        super.userEventTriggered(ctx, evt);
    }

    @Override
    public void channelWritabilityChanged(ChannelHandlerContext ctx) throws Exception {
        log.debug("Channel 可写改变了");
        super.channelWritabilityChanged(ctx);
    }

    // 客户端数据读取完时，触发channelReadComplete
    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        log.debug("Channel 所有数据读取完毕");
        ctx.flush();
    }

    // 客户端入站后，异常捕获
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.debug("出现异常，异常被捕获了");
        // Close the connection when an exception is raised
        cause.printStackTrace();
        ctx.close();
    }
}