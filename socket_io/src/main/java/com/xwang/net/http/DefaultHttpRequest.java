package com.xwang.net.http;

import com.xwang.net.KeyValuePairRequest;
import com.xwang.net.NetRequest;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by xwangly on 2016/9/6.
 */
public abstract class DefaultHttpRequest extends KeyValuePairRequest implements HttpRequest {
    private String method;
    private int timeout;
    private String url;
    private String encoding = "utf-8";

    private Object tag;

    private Map<String, String> header = new HashMap<>();


    public void setMethod(String method) {
        this.method = method;
    }

    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public void setEncoding(String encoding) {
        this.encoding = encoding;
    }

    public void setTag(Object tag) {
        this.tag = tag;
    }
    public void addHeader(String key, String value) {
        header.put(key, value);
    }

    @Override
    public String getUrl() {
        return url;
    }

    @Override
    public int getTimeout() {
        return timeout;
    }

    @Override
    public String getMethod() {
        return method;
    }

    @Override
    public NetRequest setSequence(int sequence) {
        return this;
    }

    @Override
    public String getBodyContentType() {
        return "application/x-www-form-urlencoded; charset=" + encoding;
    }

    @Override
    public Map<String, String> getHeaders() {
        return header;
    }

    @Override
    public byte[] getDatas() {
        if (method != null) {
            method = method.toUpperCase();
            if ("PUT".equals(method) || "POST".equals(method) || "PATCH".equals(method)) {
                Map<String, String> params = getParams();
                StringBuilder sbUrl = new StringBuilder();
                for (Map.Entry<String, String> entry : params.entrySet()) {
                    sbUrl.append(entry.getKey());
                    sbUrl.append('=');
                    sbUrl.append(entry.getValue());
                    sbUrl.append('&');
                }
                try {
                    return sbUrl.toString().getBytes(encoding);
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
            }
        }
        return null;
    }

    @Override
    public Object getTag() {
        return tag;
    }

    @Override
    public String logMsg() {
        Map<String, String> params = getParams();
        StringBuilder sbUrl = new StringBuilder();
        for (Map.Entry<String, String> entry : params.entrySet()) {
            //Only encrypt params except for 'tc_'

                sbUrl.append(entry.getKey());
                sbUrl.append('=');
                sbUrl.append(entry.getValue());
                sbUrl.append('\n');

        }
        String logMsg = "\nHttpRequest"+" [%s]\n%s";
        return String.format(logMsg, getUrl(), sbUrl.toString());
    }
}
