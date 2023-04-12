package org.example;


import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;

@Slf4j
public class TestEventLoopGroup {
    public static void main(String[] args) throws ExecutionException, InterruptedException {
        NioEventLoopGroup group = new NioEventLoopGroup(2);
        group.next().execute(new Runnable() {
            @Override
            public void run() {
                log.debug("执行任务1");
            }
        });
        Future<?> future1 = group.next().submit(new Runnable() {
            @Override
            public void run() {
                log.debug("执行任务2");
            }
        });
        Future<Integer> future2 = group.next().submit(new Callable<Integer>() {
            @Override
            public Integer call() throws Exception {
                log.debug("执行任务3");
                return 50;
            }
        });
        // 阻塞
        // Integer integer = future2.get();
        // log.debug("{}", integer);

        // 非阻塞
        future2.addListener(new GenericFutureListener<Future<? super Integer>>() {
            @Override
            public void operationComplete(Future<? super Integer> future) throws Exception {
                Integer integer = (Integer) future.getNow();
                log.debug("{}", integer);
            }
        });
    }
}
