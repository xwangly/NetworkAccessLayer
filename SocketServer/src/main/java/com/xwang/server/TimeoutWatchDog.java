package com.xwang.server;

import java.io.IOException;
import java.util.Queue;

import static com.xwang.server.Logger.log;

/**
 * Created by xwangly on 2016/9/28.
 */
public class TimeoutWatchDog implements Runnable {
    private final Queue<? extends TimeoutAble> queue;
    private final long timeout;
    private boolean isInterrupt = false;

    public TimeoutWatchDog(Queue<? extends TimeoutAble> queue, long timeout) {
        this.queue = queue;
        this.timeout = timeout;
    }

    private void wait_(long timeMillis) {
        synchronized (this) {
            try {
                this.wait(timeMillis);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
    public void interrupt() {
        isInterrupt = true;
    }

    @Override
    public void run() {
        while (!isInterrupt) {
            TimeoutAble timeoutAble = queue.peek();
            if (timeoutAble != null) {
                log("WatchDog connection " + timeoutAble);
                long t = timeoutAble.getTimeMillis();
                long flow = System.currentTimeMillis() - t;
                if (flow < timeout) {
                    log("WatchDog connection not timeout wait");
                    this.wait_(timeout - flow);
                } else {
                    log("WatchDog connection timeout, close it.");
                    queue.remove(timeoutAble);
                    try {
                        timeoutAble.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            } else {
                log("WatchDog no connection");
                this.wait_(timeout);
            }
        }
    }
}