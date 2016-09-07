package com.xwang.net.http;

import com.xwang.net.NetException;
import com.xwang.net.NetResponseParser;
import com.xwang.net.internal.EngineResponse;

import java.io.UnsupportedEncodingException;

/**
 * Created by xwangly on 2016/9/6.
 */
public class StringRequest extends DefaultHttpRequest implements NetResponseParser<String>{

    @Override
    public NetResponseParser getParser() {
        return this;
    }

    @Override
    public <ENGINERESPONSE extends EngineResponse> String parseToNetResponse(ENGINERESPONSE engine_response) throws NetException {
        String parsed;
        HttpEngineResponse httpEngineResponse = (HttpEngineResponse) engine_response;
        try {
            parsed = new String(httpEngineResponse.data, HttpHeaderParser.parseCharset(httpEngineResponse.headers));
        } catch (UnsupportedEncodingException e) {
            parsed = new String(httpEngineResponse.data);
        }
        return parsed;
    }
}
