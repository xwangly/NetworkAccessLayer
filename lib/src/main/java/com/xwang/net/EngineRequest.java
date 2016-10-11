package com.xwang.net;

/**
 * Created by xwangly on 2016/8/27.
 */
public interface EngineRequest<REQUEST extends NetRequest> {
    void setSequence(int i);

    Integer getSequence();

    void setTimeoutMS(int timeoutMS);

    int getTimeoutMs();

    void setTag(Object tag);

    Object getTag();

    void setURL(String url);

    String getURL();

    void cancel();

    boolean isCanceled();

    void addMarker(String s);

    void finish(String done);

    byte[] getBody();

    String host();

    REQUEST getRequest();
}
