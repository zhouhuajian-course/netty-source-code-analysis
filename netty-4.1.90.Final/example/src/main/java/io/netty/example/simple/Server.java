package io.netty.example.simple;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.string.StringDecoder;
import lombok.extern.slf4j.Slf4j;

/**
 * 简单服务端
 *
 * @author zhouhuajian
 */
@Slf4j
public class Server {

    public static void main(String[] args) throws Exception {
        EventLoopGroup bossGroup = new NioEventLoopGroup(1);
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(bossGroup, workerGroup)
                     .channel(NioServerSocketChannel.class)
                     .option(ChannelOption.SO_BACKLOG, 100)
                     .childHandler(new ChannelInitializer<SocketChannel>() {
                         @Override
                         protected void initChannel(SocketChannel ch) throws Exception {
                             ChannelPipeline pipeline = ch.pipeline();
                             pipeline.addLast(new StringDecoder());
                             pipeline.addLast(new ChannelInboundHandlerAdapter() {
                                 @Override
                                 public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
                                     super.channelRegistered(ctx);
                                 }

                                 @Override
                                 public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {
                                     super.channelUnregistered(ctx);
                                 }

                                 @Override
                                 public void channelActive(ChannelHandlerContext ctx) throws Exception {
                                     // 16:13:44.850 [nioEventLoopGroup-3-1] DEBUG org.example.simple.Server -- ChannelHandlerContext(Server$1$1#0, [id: 0xca6b3c64, L:/127.0.0.1:8080 - R:/127.0.0.1:52974])
                                     log.info("{}", ctx);
                                     super.channelActive(ctx);
                                 }

                                 @Override
                                 public void channelInactive(ChannelHandlerContext ctx) throws Exception {
                                     super.channelInactive(ctx);
                                 }

                                 @Override
                                 public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
                                     log.info(((String) msg));
                                     ByteBuf buffer = Unpooled.buffer(2);
                                     buffer.writeBytes("Hi".getBytes());
                                     ctx.write(buffer);
                                     super.channelRead(ctx, msg);
                                 }

                                 @Override
                                 public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
                                     ctx.flush();
                                     super.channelReadComplete(ctx);
                                 }

                                 @Override
                                 public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
                                     super.userEventTriggered(ctx, evt);
                                 }

                                 @Override
                                 public void channelWritabilityChanged(ChannelHandlerContext ctx) throws Exception {
                                     super.channelWritabilityChanged(ctx);
                                 }

                                 @Override
                                 public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
                                     super.exceptionCaught(ctx, cause);
                                 }
                             });
                         }
                     });
            ChannelFuture future = bootstrap.bind(8080).sync();
            log.info("服务端启动成功");
            future.channel().closeFuture().sync();
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }
}
