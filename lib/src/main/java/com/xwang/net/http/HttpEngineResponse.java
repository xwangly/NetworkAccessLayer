package com.xwang.net.http;

import com.xwang.net.internal.EngineResponse;

import java.util.Collections;
import java.util.Map;

/**
 * Created by xwangly on 2016/8/31.
 */
public class HttpEngineResponse implements EngineResponse{
    /**
     * Creates a new network response.
     * @param statusCode the HTTP status code
     * @param data Response body
     * @param headers Headers returned with this response, or null for none
     * @param networkTimeMs Round-trip network time to receive network response
     */
    public HttpEngineResponse(int statusCode, byte[] data, Map<String, String> headers, long networkTimeMs) {
        this.statusCode = statusCode;
        this.data = data;
        this.headers = headers;
        this.networkTimeMs = networkTimeMs;
    }

    public HttpEngineResponse(int statusCode, byte[] data, Map<String, String> headers) {
        this(statusCode, data, headers, 0);
    }

    public HttpEngineResponse(byte[] data) {
        this(200, data, Collections.<String, String>emptyMap(), 0);
    }

    public HttpEngineResponse(byte[] data, Map<String, String> headers) {
        this(200, data, headers, 0);
    }
    /** The HTTP status code. */
    public final int statusCode;

    /** Raw data from this response. */
    public final byte[] data;

    /** Response headers. */
    public final Map<String, String> headers;

    /** Network roundtrip time in milliseconds. */
    public long networkTimeMs;
}
