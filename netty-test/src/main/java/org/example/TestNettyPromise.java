package org.example;

import io.netty.channel.EventLoop;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.util.concurrent.DefaultPromise;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import io.netty.util.concurrent.Promise;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ExecutionException;

@Slf4j
public class TestNettyPromise {
    public static void main(String[] args) throws ExecutionException, InterruptedException {
        NioEventLoopGroup group = new NioEventLoopGroup(2);
        EventLoop eventLoop = group.next();
        Promise<Integer> promise = new DefaultPromise<Integer>(eventLoop);
        new Thread(new Runnable() {
            @Override
            public void run() {
                log.debug("执行任务");
                try {
                    Thread.sleep(1000);
                    promise.setSuccess(50);
                } catch (InterruptedException e) {
                    promise.setFailure(e);
                }
            }
        }, "thread-0").start();
        // 阻塞
        // Integer integer = promise.get();
        // log.debug("{}", integer);
        // 非阻塞
        promise.addListener(new GenericFutureListener<Future<? super Integer>>() {
            @Override
            public void operationComplete(Future<? super Integer> future) throws Exception {
                Integer integer = promise.get();
                log.debug("{}", integer);
            }
        });
    }
}
