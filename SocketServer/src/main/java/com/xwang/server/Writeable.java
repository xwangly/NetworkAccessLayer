package com.xwang.server;

import okio.Buffer;

/**
 * Created by xwangly on 2016/9/30.
 */
public interface Writeable {
    void write(Buffer buffer);
}
