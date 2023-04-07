package org.example;

import io.netty.util.NettyRuntime;

public class TestAvailableProcessor {
    public static void main(String[] args) {
        // 12
        System.out.println(NettyRuntime.availableProcessors());
        System.out.println(Runtime.getRuntime().availableProcessors());
    }
}
