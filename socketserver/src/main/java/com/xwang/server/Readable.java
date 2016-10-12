package com.xwang.server;

import java.io.Closeable;
import java.io.IOException;

/**
 * Created by xwangly on 2016/9/28.
 */
public interface Readable extends Closeable,TimeoutAble {
    void read();
    void digest() throws IOException;
    boolean isClosed();
}
