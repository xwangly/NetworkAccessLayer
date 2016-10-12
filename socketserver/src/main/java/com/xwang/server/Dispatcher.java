package com.xwang.server;

/**
 * Created by xwangly on 2016/9/28.
 */
public interface Dispatcher<T> {
    void dispatchRequest(Writeable readable, T request);
}
