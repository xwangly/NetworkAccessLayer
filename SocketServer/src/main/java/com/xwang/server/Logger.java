package com.xwang.server;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Created by xwangly on 2016/9/28.
 */
public class Logger {
    private static BlockingQueue<String> logQueue = new LinkedBlockingQueue();
    private static SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss SSSS");
    static {
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    try {
                        String msg = logQueue.take();
                        if (msg.startsWith("e")) {
                            msg = msg.substring(1);
                            System.err.println(msg);
                        } else {
                            System.out.println(msg);
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();
    }
    public static void log(String msg) {
        String formatmsg = sdf.format(new Date()) + " Thread:" + Thread.currentThread().getName() +" " + msg;
        logQueue.add(formatmsg);
    }
    public static void loge(String msg) {
        String formatmsg = "e"+sdf.format(new Date()) + " Thread:" + Thread.currentThread().getName() +" " + msg;
        logQueue.add(formatmsg);
    }
}
