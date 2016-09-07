package com.xwang.net.internal;

import com.xwang.net.*;

import java.util.concurrent.Executor;

/**
 * Created by xwangly on 2016/8/27.
 */
public class RealCall<REQUEST extends NetRequest,NETRESPONSE,ENGINERESPONSE extends EngineResponse> {
    // Guarded by this.
    private boolean executed;
    volatile boolean canceled;

    /** The application's original request unadulterated by redirects or auth headers. */
    private final Dispatcher mDispatcher;
    private final EngineRequest<REQUEST> originalRequest;
    private final Engine<ENGINERESPONSE> netEngine;
    private final NetResponseParser<NETRESPONSE> parser;

    public RealCall(Engine<ENGINERESPONSE> netEngine, NetResponseParser<NETRESPONSE> parser, Dispatcher dispatcher, EngineRequest<REQUEST> originalRequest) {
        this.netEngine = netEngine;
        this.parser = parser;
        this.mDispatcher = dispatcher;
        this.originalRequest = originalRequest;
    }

    public EngineRequest request() {
        return originalRequest;
    }

    public NETRESPONSE execute() throws NetException {
        synchronized (this) {
            if (executed) throw new IllegalStateException("Already Executed");
            executed = true;
        }
        try {
            mDispatcher.executed(this);
            originalRequest.addMarker("network-execute");
            NETRESPONSE result = getResponse(originalRequest);
            return result;
        } finally {
            originalRequest.finish("done");
            mDispatcher.finished(this);
        }
    }

    Object tag() {
        return originalRequest.getTag();
    }

    public void enqueue(NetCallback<NETRESPONSE> responseCallback, Executor executor) {
        synchronized (this) {
            if (executed) throw new IllegalStateException("Already Executed");
            executed = true;
        }
        originalRequest.addMarker("network-enqueue");
        mDispatcher.enqueue(new AsyncCall(responseCallback, executor));
    }

    public void cancel() {
        canceled = true;
        originalRequest.cancel();
        try {
            netEngine.cancelRequest(originalRequest);
        } catch (NetException e) {
            e.printStackTrace();
        }
//        originalRequest
//        if (engine != null) engine.cancel();
    }

    public synchronized boolean isExecuted() {
        return executed;
    }

    public boolean isCanceled() {
        return canceled;
    }

    /**
     * Returns a string that describes this call. Doesn't include a full URL as that might contain
     * sensitive information.
     */
    private String toLoggableString() {
        String string = canceled ? "canceled call" : "call";
        return string + " to " + originalRequest.host();
    }


    /**
     * Performs the request and returns the response. May return null if this call was canceled.
     */
    NETRESPONSE getResponse(EngineRequest<REQUEST> engineRequest) throws NetException {
        try {
            // If the request was cancelled already, do not perform the
            // network request.
            if (engineRequest.isCanceled()) {
                engineRequest.addMarker("network-discard-cancelled");
                throw new NetException(NetException.USER_CANCELED);
            }

            // Perform the network request.
            ENGINERESPONSE networkResponse = netEngine.performRequest(engineRequest);
            engineRequest.addMarker("network-engine-complete");

            // Parse the response here on the worker thread.
            NETRESPONSE response = parser.parseToNetResponse(networkResponse);
            engineRequest.addMarker("network-parser-complete");

            engineRequest.addMarker("print response [" + engineRequest.getSequence() + "] " + response.toString());
            // If this request has canceled, finish it and don't deliver.

            if (engineRequest.isCanceled()) {
                engineRequest.addMarker("canceled-at-delivery");
                throw new NetException(NetException.USER_CANCELED);
            }
            return response;
        } catch (NetException volleyError) {
            engineRequest.addMarker("network-exception " + volleyError.getErrorMsg());
            throw volleyError;
        } catch (Exception e) {
            engineRequest.addMarker("network-exception " + e.toString());
            e.printStackTrace();
            throw new NetException(NetException.UNKNOW_EXCEPTION, e);
        }
    }
    final class AsyncCall implements Runnable {
        private final NetCallback<NETRESPONSE> responseCallback;
        private final Executor callbaclExecutor;

        /*private*/ AsyncCall(NetCallback<NETRESPONSE> responseCallback, Executor callbaclExecutor) {
            this.responseCallback = responseCallback;
            this.callbaclExecutor = callbaclExecutor;
        }
        public final void run() {
            String oldName = Thread.currentThread().getName();
            Thread.currentThread().setName("AsyncCall" + originalRequest.getSequence());
            try {
                execute();
            } finally {
                Thread.currentThread().setName(oldName);
            }
        }
        String host() {
            return originalRequest.host();
        }

        EngineRequest request() {
            return originalRequest;
        }

        Object tag() {
            return originalRequest.getTag();
        }

        void cancel() {
            RealCall.this.cancel();
        }

        RealCall get() {
            return RealCall.this;
        }

        protected void execute() {
            boolean signalledCallback = false;
            try {
                originalRequest.addMarker("network-execute");
                NETRESPONSE response = getResponse(originalRequest);
                if (canceled) {
                    signalledCallback = true;
                    notifyException(new NetException(NetException.USER_CANCELED));
                } else {
                    signalledCallback = true;
                    notifyResponse(response);
                }
            } catch (NetException e) {
                if (signalledCallback) {
                    // Do not signal the callback twice!
                    NetLog.e(e, "Callback failure for " + toLoggableString());
                } else {
                    notifyException(e);
                }
            } finally {
                originalRequest.finish("done");
                mDispatcher.finished(this);
            }
        }
        private void notifyResponse(final NETRESPONSE response) {
            if (responseCallback != null) {
                if (callbaclExecutor == null) notifyResponse_(response);
                else {
                    callbaclExecutor.execute(new Runnable() {
                        @Override
                        public void run() {
                            notifyResponse_(response);
                        }
                    });
                }
            }
        }
        private void notifyResponse_(NETRESPONSE response) {
            try {
                responseCallback.onResponse(response);
            } catch (Exception e) {
                e.printStackTrace();
                notifyException(new NetException(NetException.NOTIFY_EXCEPTION, e));
            }
        }
        private void notifyException(final NetException e) {
            if (responseCallback != null) {
                if (callbaclExecutor == null) responseCallback.onFailure(e);
                else {
                    callbaclExecutor.execute(new Runnable() {
                        @Override
                        public void run() {
                            responseCallback.onFailure(e);
                        }
                    });
                }

            }
        }
    }
}
