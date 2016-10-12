package com.xwang.net;

/**
 * Created by xwangly on 2016/8/29.
 * Execute user request.It provide synchronized and asynchronized send method and it can cancel request.
 */
public interface NetExecutor<REQUEST extends NetRequest, RESPONSE> {
    RESPONSE sendRequest(REQUEST request) throws NetException;

    void sendRequest(REQUEST request, NetCallback<RESPONSE> listener);

    void cancelRequest(Object tag);

    void shutDown();
}
