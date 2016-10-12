package com.xwang.net.http;

import com.xwang.net.Engine;
import com.xwang.net.EngineRequest;
import com.xwang.net.internal.NetExecutorImpl;
import com.xwang.net.NetRequest;
import com.xwang.net.NetResponse;

import java.util.concurrent.Executor;

/**
 * Created by xwangly on 2016/8/31.
 */
public abstract class HttpExecutor<REQUEST extends NetRequest, RESPONSE extends NetResponse, ENGINE_RESPONSE extends HttpEngineResponse> extends NetExecutorImpl<REQUEST, RESPONSE, ENGINE_RESPONSE> {
    private final String TAG = "HttpExecutor";

    public HttpExecutor(Executor callbackExecutor) {
        super(callbackExecutor);
    }
    public HttpExecutor(Executor callbackExecutor, Engine<ENGINE_RESPONSE> engine) {
        super(callbackExecutor);
        setEngine(engine);
    }

    @Override
    public void setEngine(Engine<ENGINE_RESPONSE> engine) {
        super.setEngine(engine);
    }

    @Override
    protected void setRequestParams(EngineRequest<REQUEST> engineRequest) {
        super.setRequestParams(engineRequest);
    }
}
