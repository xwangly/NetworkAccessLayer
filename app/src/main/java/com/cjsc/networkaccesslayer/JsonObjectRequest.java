package com.cjsc.networkaccesslayer;

import com.xwang.net.NetException;
import com.xwang.net.NetResponseParser;
import com.xwang.net.http.DefaultHttpRequest;
import com.xwang.net.http.HttpEngineResponse;
import com.xwang.net.http.HttpHeaderParser;
import com.xwang.net.internal.EngineResponse;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;

/**
 * Created by xwangly on 2016/9/7.
 */
public class JsonObjectRequest extends DefaultHttpRequest implements NetResponseParser<JSONObject> {
    @Override
    public NetResponseParser getParser() {
        return this;
    }

    @Override
    public <ENGINERESPONSE extends EngineResponse> JSONObject parseToNetResponse(ENGINERESPONSE engine_response) throws NetException {
        String parsed;
        HttpEngineResponse httpEngineResponse = (HttpEngineResponse) engine_response;
        try {
            parsed = new String(httpEngineResponse.data, HttpHeaderParser.parseCharset(httpEngineResponse.headers));
        } catch (UnsupportedEncodingException e) {
            parsed = new String(httpEngineResponse.data);
        }
        try {
            JSONObject jsonObject = new JSONObject(parsed);
            return jsonObject;
        } catch (JSONException e) {
            e.printStackTrace();
            throw new NetException(NetException.PARSE_EXCEPTION, e);
        }
    }
}
