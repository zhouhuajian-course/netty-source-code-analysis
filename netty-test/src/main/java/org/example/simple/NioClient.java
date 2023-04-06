package org.example.simple;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;

@Slf4j
public class NioClient {

    public static void main(String[] args) throws IOException {
        SocketChannel channel = SocketChannel.open();
        channel.connect(new InetSocketAddress("127.0.0.1", 8080));
        Charset charset = Charset.forName("UTF-8");
        channel.write(charset.encode("Hello"));
        ByteBuffer buffer = ByteBuffer.allocate(1024);
        channel.read(buffer);
        buffer.flip();
        log.debug(String.valueOf(charset.decode(buffer)));
        System.in.read();
        channel.close();
    }
}
