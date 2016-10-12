package com.xwang.server;

import java.io.IOException;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;

import static com.xwang.server.Logger.log;
import static com.xwang.server.Logger.loge;

/**
 * Created by xwangly on 2016/9/28.
 */
public class AcceptCompletionHandler implements CompletionHandler<AsynchronousSocketChannel, Integer> {
    private final AsynchronousServerSocketChannel serverSocketChannel;
    private final SocketServer socketServer;

    public AcceptCompletionHandler(AsynchronousServerSocketChannel serverSocketChannel,  SocketServer socketServer) {
        this.serverSocketChannel = serverSocketChannel;
        this.socketServer = socketServer;
    }

    @Override
    public void completed(AsynchronousSocketChannel result, Integer index) {
        try {
            log("New connection:"+index + " url:" + result.getRemoteAddress());
        } catch (IOException e) {
            e.printStackTrace();
        }
        Readable readable =socketServer.createReadable(index, result);
        socketServer.addReadable(readable);

        readable.read();
        accept(index + 1);
    }

    @Override
    public void failed(Throwable exc, Integer attachment) {
        loge("Accept failed " + attachment);
        if (serverSocketChannel != null && serverSocketChannel.isOpen()) {
            accept(attachment + 1);
        }
    }

    public void accept(int index) {
        serverSocketChannel.accept(index, this);
    }
}