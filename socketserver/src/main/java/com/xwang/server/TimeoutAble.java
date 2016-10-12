package com.xwang.server;

import java.io.Closeable;

/**
 * Created by xwangly on 2016/9/28.
 */
public interface TimeoutAble extends Closeable{
    long getTimeMillis();
    void updateTimeMillis();
}
