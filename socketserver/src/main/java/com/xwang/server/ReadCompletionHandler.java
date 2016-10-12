package com.xwang.server;

import java.io.IOException;
import java.nio.channels.CompletionHandler;

import static com.xwang.server.Logger.log;
import static com.xwang.server.Logger.loge;

/**
 * Created by xwangly on 2016/9/28.
 */
public class ReadCompletionHandler implements CompletionHandler<Integer, Readable> {
    private final SocketServer socketServer;

    public ReadCompletionHandler(SocketServer socketServer) {
        this.socketServer = socketServer;
    }

    @Override
    public void completed(Integer result, Readable attachment) {
        log("ReadCompletionHandler read "+result +" bytes " + attachment);
        if (result < 0) {
            log("Endpoint close the channel.");
            try {
                attachment.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return;
        } else if (result == 0) {
            attachment.read();
        } else {
            attachment.updateTimeMillis();
            socketServer.updateReadable(attachment);
            try {
                attachment.digest();
            } catch (IOException e) {
                e.printStackTrace();
            }
            attachment.read();
        }

    }

    @Override
    public void failed(Throwable exc, Readable attachment) {
        loge("ReadCompletionHandler failed " + attachment + " " + exc.getMessage());
//        exc.printStackTrace();
        if (!attachment.isClosed()) {
            attachment.read();
        }
    }
}