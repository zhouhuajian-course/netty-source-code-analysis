package io.netty.example.exception;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.util.CharsetUtil;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.TimeUnit;

/**
 * 客户端
 *
 * @author zhouhuajian
 */
@Slf4j
public class Client {

    public static void main(String[] args) throws Exception {
        NioEventLoopGroup group = new NioEventLoopGroup();
        try {
            Bootstrap bootstrap = new Bootstrap()
                .group(group)
                .channel(NioSocketChannel.class)

                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) throws Exception {
                        ch.pipeline().addLast(new LoggingHandler());
                        // ch.pipeline().addLast(new ReadTimeoutHandler(5000, TimeUnit.MILLISECONDS));
                        ch.pipeline().addLast(new ChannelInboundHandlerAdapter() {
                            @Override
                            public void channelActive(ChannelHandlerContext ctx) throws Exception {
                                ctx.writeAndFlush(ctx.alloc().buffer().writeCharSequence("test", CharsetUtil.UTF_8));
                                // super.channelActive(ctx);
                            }
                        });
                    }
                });
            ChannelFuture future = bootstrap.connect("192.168.1.206", 8080).sync();
            future.channel().closeFuture().sync();

        } finally {
            group.shutdownGracefully();
        }
    }
}
