package org.example.misc;

import io.netty.util.NettyRuntime;

public class AvailableProcessor {
    public static void main(String[] args) {
        // 12
        System.out.println(NettyRuntime.availableProcessors());
        System.out.println(Runtime.getRuntime().availableProcessors());
    }
}
