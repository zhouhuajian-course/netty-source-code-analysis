package org.example;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.*;

@Slf4j
public class TestJdkExecutorService {
    public static void main(String[] args) throws ExecutionException, InterruptedException {
        ExecutorService service = Executors.newFixedThreadPool(2);
        service.execute(new Runnable() {
            @Override
            public void run() {
                log.debug("执行任务1");
            }
        });
        Future<?> future1 = service.submit(new Runnable() {
            @Override
            public void run() {
                log.debug("执行任务2");
            }
        });
        Future<Integer> future2 = service.submit(new Callable<Integer>() {
            @Override
            public Integer call() throws Exception {
                log.debug("执行任务3");
                return 30;
            }
        });
        // 阻塞 等待结果
        Integer integer = future2.get();
        log.debug("{}", integer);
    }
}
