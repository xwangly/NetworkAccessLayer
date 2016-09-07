package com.xwang.net.http;

import com.xwang.net.Engine;
import com.xwang.net.NetResponse;
import com.xwang.net.NetResponseParser;

import java.util.concurrent.Executor;

/**
 * Created by xwangly on 2016/9/6.
 */
public class DefalutHttpExecutor extends HttpExecutor<HttpRequest, NetResponse, HttpEngineResponse> {
    public DefalutHttpExecutor(Executor callbackExecutor) {
        super(callbackExecutor);
    }


    public DefalutHttpExecutor(Executor callbackExecutor, Engine engine) {
        super(callbackExecutor, engine);
    }


    @Override
    protected NetResponseParser getParser(HttpRequest request) {
        return request.getParser();
    }

    @Override
    protected String getUrl(HttpRequest request) {
        return request.getUrl();
    }

    @Override
    protected int getTimeout(HttpRequest request) {
        return request.getTimeout();
    }
}
