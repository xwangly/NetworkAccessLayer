package com.xwang.server;

import java.io.IOException;

import okio.Buffer;

/**
 * Created by xwangly on 2016/9/28.
 */
public interface RequestParser<T> {
    int getHeaderLength();
    int getBodyLength(byte[] header);
    T parserToRequest(Buffer buffer, byte[] header) throws IOException;
}
