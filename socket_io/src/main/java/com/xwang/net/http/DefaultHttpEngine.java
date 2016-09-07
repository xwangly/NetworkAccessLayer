package com.xwang.net.http;

import com.xwang.net.EngineRequest;

import java.net.HttpURLConnection;
import java.util.Map;

/**
 * Created by xwangly on 2016/9/7.
 */
public class DefaultHttpEngine extends HurlEngine<HttpRequest, HttpEngineResponse> {
    @Override
    protected HttpEngineResponse createResponse(int responseCode, String responseMessage, byte[] datas, Map<String, String> header, long networkTimeMs, HttpURLConnection connection, EngineRequest<HttpRequest> request) {
        return new HttpEngineResponse(responseCode, datas, header, networkTimeMs);
    }
}
