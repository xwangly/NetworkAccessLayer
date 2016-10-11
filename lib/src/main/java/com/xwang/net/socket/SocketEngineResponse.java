package com.xwang.net.socket;

import com.xwang.net.internal.EngineResponse;

/**
 * Created by xwangly on 2016/8/1.
 */
public class SocketEngineResponse implements EngineResponse{
    public final int serial;
    public final boolean isPushMsg;
    public final byte[] socketHeader;
    /** Raw data from this response. */
    public final byte[] data;

    /** Network roundtrip time in milliseconds. */
    public long networkTimeMs;

    public SocketEngineResponse(int serial, boolean isPush, byte[] header, byte[] data) {
        this.serial = serial;
        this.isPushMsg = isPush;
        this.socketHeader = header;
        this.data = data;
    }

    public boolean isPushMsg() {
        return isPushMsg;
    }

    public int getSerial() {
        return serial;
    }
}
