package com.xwang.net;

import com.xwang.net.internal.EngineResponse;

/**
 * Created by xwangly on 2016/8/31.
 */
public interface NetResponseParser<NETRESPONSE> {
    <ENGINERESPONSE extends EngineResponse> NETRESPONSE parseToNetResponse(ENGINERESPONSE engine_response) throws NetException;
}
