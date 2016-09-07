package com.xwang.net;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Created by xwangly on 2015/12/23.
 * Common network request.
 * Only warp request params.
 */
public abstract class KeyValuePairRequest implements NetRequest {
    protected final HashMap<String, String> bodyParams;

    protected KeyValuePairRequest() {
        bodyParams = new LinkedHashMap<String, String>();
    }

    @Override
    public final NetRequest setParams(String key, String value) {
        if (key == null)
            throw new NullPointerException("params can not be null");
        bodyParams.put(key, value == null ? "" : value.toString());
        return this;
    }
    public final NetRequest setParams(Map<String, String> params) {
        if (params != null && !params.isEmpty()) {
            bodyParams.putAll(params);
        }
        return this;
    }

    @Override
    public final NetRequest setParams(String keyValuePair) {
        if (keyValuePair != null) {
            String[] ss = keyValuePair.split("&");
            if (ss != null && ss.length > 0) {
                String[] sss = null;
                for (String s : ss) {
                    if (s != null) {
                        sss = s.split("=");
                        if (sss != null && sss.length == 2) {
                            bodyParams.put(sss[0], sss[1]);
                        }
                    }
                }
            }
        }
        return this;
    }

    @Override
    public Map<String, String> getParams() {
        return bodyParams;
    }
}
