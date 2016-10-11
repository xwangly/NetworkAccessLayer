package com.xwang.net.http;

import com.xwang.net.NetRequest;
import com.xwang.net.NetResponseParser;

import java.util.Map;

/**
 * Created by xwangly on 2016/9/5.
 */
public interface HttpRequest extends NetRequest {
    void setUrl(String url);

    void setTimeout(int timeout);

    void setMethod(String method);

    void setEncoding(String encoding);

    void setTag(Object tag);

    String getUrl();

    int getTimeout();

    String getMethod();

    String getBodyContentType();

    Map<String, String> getHeaders();

    <RESPONSE> NetResponseParser<RESPONSE> getParser();
}
