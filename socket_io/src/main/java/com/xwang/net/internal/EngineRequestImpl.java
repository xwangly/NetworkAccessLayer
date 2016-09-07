package com.xwang.net.internal;

import com.xwang.net.EngineRequest;
import com.xwang.net.NetLog;
import com.xwang.net.NetRequest;
import com.xwang.net.NetResponseParser;

/**
 * Created by xwangly on 2016/7/12.
 */
public class EngineRequestImpl<REQUEST extends NetRequest> implements EngineRequest<REQUEST> {
    private final REQUEST request;

    /** The retry policy for this request. */
    private int timeoutMS;

    /** An opaque token tagging this request; used for bulk cancellation. */
    private Object mTag;

    /** Whether or not this request has been canceled. */
    private boolean mCanceled = false;

    /** Sequence number of this request, used to enforce FIFO ordering. */
    private Integer mSequence;

    /** An event log tracing the lifetime of this request; for debugging. */
    private final NetLog.MarkerLog mEventLog = NetLog.MarkerLog.ENABLED ? new NetLog.MarkerLog() : null;

    /** URL of this request. */
    private String mUrl;
    /**
     * Creates a new request with the given URL, and error listener.
     * Note that the normal response listener is not provided here as
     * delivery of responses is provided by subclasses, who have a better idea of how to deliver
     * an already-parsed response.
     */
    public EngineRequestImpl(REQUEST request) {
        this.request = request;
    }

    @Override
    public Integer getSequence() {
        return mSequence;
    }

    @Override
    public void setSequence(int i) {
        mSequence = i;
    }

    @Override
    public int getTimeoutMs() {
        return timeoutMS;
    }

    @Override
    public void setTimeoutMS(int timeoutMS) {
        this.timeoutMS = timeoutMS;
    }

    @Override
    public void setTag(Object tag) {
        this.mTag = tag;
    }

    @Override
    public Object getTag() {
        return mTag;
    }

    @Override
    public void setURL(String url) {
        this.mUrl = url;
    }

    @Override
    public String getURL() {
        return mUrl;
    }

    @Override
    public void cancel() {
        mCanceled = true;
    }

    @Override
    public boolean isCanceled() {
        return mCanceled;
    }

    @Override
    public void addMarker(String tag) {
        if (NetLog.MarkerLog.ENABLED) {
            mEventLog.add(tag, Thread.currentThread().getId());
        }
    }

    @Override
    public void finish(String marker) {
        if (NetLog.MarkerLog.ENABLED) {
            final long threadId = Thread.currentThread().getId();
            mEventLog.add(marker, threadId);
            mEventLog.finish(this.toString());
        }
    }

    @Override
    public byte[] getBody() {
        return request.getDatas();
    }

    @Override
    public String host() {
        return mUrl;
    }

    @Override
    public REQUEST getRequest() {
        return request;
    }
}
