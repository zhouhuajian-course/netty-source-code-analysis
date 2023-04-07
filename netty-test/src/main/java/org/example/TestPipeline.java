package org.example;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.util.CharsetUtil;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TestPipeline {

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
                             pipeline.addLast("h1", new ChannelInboundHandlerAdapter() {

                                 @Override
                                 public void channelActive(ChannelHandlerContext ctx) throws Exception {
                                     log.debug("{}", ctx);
                                     super.channelActive(ctx);
                                 }

                                 @Override
                                 public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
                                     // log.debug(((String) msg));
                                     // ByteBuf buffer = Unpooled.buffer(2);
                                     // buffer.writeBytes("Hi".getBytes());
                                     // ctx.write(buffer);
                                     log.debug("1");
                                     String name = ((ByteBuf) msg).toString(CharsetUtil.UTF_8);
                                     super.channelRead(ctx, name);
                                 }

                                 // @Override
                                 // public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
                                 //     ctx.flush();
                                 //     super.channelReadComplete(ctx);
                                 // }
                             });
                             pipeline.addLast("h2", new ChannelInboundHandlerAdapter() {
                                 @Override
                                 public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
                                     log.debug("2");
                                     Student student = new Student(((String) msg));
                                     super.channelRead(ctx, student);
                                 }
                             });
                             pipeline.addLast("h3", new ChannelInboundHandlerAdapter() {
                                 @Override
                                 public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
                                     log.debug("3 {} {}", msg, msg.getClass());
                                     // super.channelRead(ctx, msg);
                                     ctx.write(ch.alloc().buffer().writeBytes("hi".getBytes()));
                                     // ch.write(ch.alloc().buffer().writeBytes("hi".getBytes()));
                                 }

                                 @Override
                                 public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
                                     ctx.flush();
                                     // super.channelReadComplete(ctx);
                                 }
                             });
                             pipeline.addLast("h4", new ChannelOutboundHandlerAdapter() {
                                 @Override
                                 public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
                                     log.debug("4");
                                     super.write(ctx, msg, promise);
                                 }
                             });
                             pipeline.addLast("h5", new ChannelOutboundHandlerAdapter() {
                                 @Override
                                 public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
                                     log.debug("5");
                                     super.write(ctx, msg, promise);
                                 }
                             });
                             pipeline.addLast("h6", new ChannelOutboundHandlerAdapter() {
                                 @Override
                                 public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
                                     log.debug("6");
                                     super.write(ctx, msg, promise);
                                 }
                             });
                         }
                     });
            ChannelFuture future = bootstrap.bind(8080).sync();
            future.channel().closeFuture().sync();
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }

    @Data
    @AllArgsConstructor
    static class Student {
        private String name;
    }
}
