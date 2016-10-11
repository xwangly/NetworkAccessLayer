package com.xwang.net.internal;

import com.xwang.net.*;

import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by xwangly on 2016/8/31.
 */
public abstract class NetExecutorImpl<REQUEST extends NetRequest, NETRESPONSE, ENGINERESPONSE extends EngineResponse> implements NetExecutor<REQUEST, NETRESPONSE> {
    private Engine<ENGINERESPONSE> engine;
    private Executor callbackExecutor;
    private Dispatcher dispatcher;
    /**
     * Used for generating monotonically-increasing sequence numbers for requests.
     */
    private AtomicInteger mSequenceGenerator = new AtomicInteger();

    public NetExecutorImpl(Executor callbackExecutor) {
        this(null, callbackExecutor);
    }

    public NetExecutorImpl(Engine<ENGINERESPONSE> engine, Executor callbackExecutor) {
        this.engine = engine;
        this.callbackExecutor = callbackExecutor;
        dispatcher = new Dispatcher();
    }

    public void setEngine(Engine<ENGINERESPONSE> engine) {
        this.engine = engine;
    }

    public Engine<ENGINERESPONSE> getEngine() {
        return engine;
    }

    public Executor getCallbackExecutor() {
        return callbackExecutor;
    }

    @Override
    public NETRESPONSE sendRequest(REQUEST request) throws NetException {
        EngineRequest<REQUEST> engineRequest = createEngineRequest(request);
        engineRequest.addMarker("create request [" + engineRequest.getSequence() + "] url:" + engineRequest.getURL());
        engineRequest.addMarker(request.logMsg());
        RealCall<REQUEST, NETRESPONSE, ENGINERESPONSE> realCall = new RealCall<>(engine, getParser(request), dispatcher, engineRequest);
        return realCall.execute();
    }

    @Override
    public void sendRequest(REQUEST request, NetCallback<NETRESPONSE> listener) {
        EngineRequest<REQUEST> engineRequest = createEngineRequest(request);
        engineRequest.addMarker("Create request [" + engineRequest.getSequence() + "] url:" + engineRequest.getURL());
        engineRequest.addMarker(request.logMsg());
        RealCall<REQUEST, NETRESPONSE, ENGINERESPONSE> realCall = new RealCall<>(engine, getParser(request), dispatcher, engineRequest);
        realCall.enqueue(listener, callbackExecutor);
    }

    @Override
    public void cancelRequest(Object tag) {
        for (com.xwang.net.internal.RealCall call : dispatcher.queuedCalls()) {
            if (tag.equals(call.request().getTag())) {
                call.cancel();
                System.out.println("cancel queuedCalls");
            }
        }

        for (com.xwang.net.internal.RealCall call : dispatcher.runningCalls()) {
            if (tag.equals(call.request().getTag())) {
                call.cancel();
                System.out.println("cancel runningCalls");
            }
        }
    }

    @Override
    public void shutDown() {
        dispatcher.cancelAll();
        dispatcher.shutDown();
        engine.shutDown();
    }

    private EngineRequest<REQUEST> createEngineRequest(REQUEST request) {
        EngineRequest<REQUEST> engineRequest = new EngineRequestImpl<>(request);
        engineRequest.setTag(request.getTag());
        engineRequest.setSequence(mSequenceGenerator.incrementAndGet());
        engineRequest.setTimeoutMS(getTimeout(request));
        engineRequest.setURL(getUrl(request));

        request.setSequence(engineRequest.getSequence());

        setRequestParams(engineRequest);

        return engineRequest;
    }

    protected void setRequestParams(EngineRequest<REQUEST> engineRequest) {
        //Empty
    }

    protected abstract NetResponseParser getParser(REQUEST request);
    protected abstract String getUrl(REQUEST request);
    protected abstract int getTimeout(REQUEST request);
}
