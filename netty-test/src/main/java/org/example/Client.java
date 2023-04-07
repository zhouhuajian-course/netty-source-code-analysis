package org.example;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import lombok.extern.slf4j.Slf4j;

import java.util.Scanner;

@Slf4j
public class Client {
    public static void main(String[] args) throws InterruptedException {
        NioEventLoopGroup group = new NioEventLoopGroup(24);
        try {
            Bootstrap bootstrap = new Bootstrap()
                .group(group)
                .channel(NioSocketChannel.class)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) throws Exception {
                        ChannelPipeline pipeline = ch.pipeline();
                        pipeline.addLast(new ChannelInboundHandlerAdapter() {
                            @Override
                            public void channelActive(ChannelHandlerContext ctx) throws Exception {
                                // 与服务端的Channel准备好时，往服务端写数据
                                // ctx.writeAndFlush(ctx.alloc().buffer().writeBytes("Hello 2".getBytes()));
                                // super.channelActive(ctx);
                            }
                        });
                    }
                });

            ChannelFuture future = bootstrap.connect("127.0.0.1", 8080).sync();

            log.debug("启动输入线程");
            Channel channel = future.channel();
            new Thread(new Runnable() {
                @Override
                public void run() {
                    Scanner scanner = new Scanner(System.in);
                    while (true) {
                        String message = scanner.next();
                        if (message.equals("q")) {
                            channel.close();
                            group.shutdownGracefully();
                            break;
                        }
                        ByteBuf byteBuf = channel.alloc().buffer().writeBytes(message.getBytes());
                        log.debug(message);
                        channel.writeAndFlush(byteBuf);
                    }
                }
            }, "thread-input").start();

            channel.closeFuture().sync();
        } finally {
            group.shutdownGracefully();
        }

    }
}
