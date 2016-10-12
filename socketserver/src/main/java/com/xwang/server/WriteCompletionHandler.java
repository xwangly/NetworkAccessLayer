package com.xwang.server;

import java.nio.channels.CompletionHandler;

import static com.xwang.server.Logger.log;
import static com.xwang.server.Logger.loge;

/**
 * Created by xwangly on 2016/9/28.
 */


public class WriteCompletionHandler implements CompletionHandler<Integer, Readable> {

    @Override
    public void completed(Integer result, Readable attachment) {
        log("WriteCompletionHandler relay success for " + attachment);
    }

    @Override
    public void failed(Throwable exc, Readable attachment) {
        loge("WriteCompletionHandler failed " + attachment);
        exc.printStackTrace();
    }
}