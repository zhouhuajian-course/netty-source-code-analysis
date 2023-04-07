package org.example;


import ch.qos.logback.core.net.server.Client;
import io.netty.channel.DefaultEventLoopGroup;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;

@Slf4j
public class TestNettyFuture {
    public static void main(String[] args) throws ExecutionException, InterruptedException {
        DefaultEventLoopGroup group = new DefaultEventLoopGroup(2);
        Future<Integer> future = group.next().submit(new Callable<Integer>() {
            @Override
            public Integer call() throws Exception {
                log.debug("执行任务");
                return 10;
            }
        });
        // future.get();
        future.addListener(new GenericFutureListener<Future<? super Integer>>() {
            @Override
            public void operationComplete(Future<? super Integer> future) throws Exception {
                Integer integer = (Integer) future.get();
                log.debug("{}", integer);
            }
        });
    }
}
