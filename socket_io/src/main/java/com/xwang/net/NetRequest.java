package com.xwang.net;

import java.util.Map;

/**
 * Created by xwangly on 2015/12/23.
 * Network request interface.
 */
public interface NetRequest {
    NetRequest setParams(String key, String value);
    NetRequest setParams(String keyValuePair);
    NetRequest setParams(Map<String, String> params);
    NetRequest setSequence(int sequence);
    Map<String, String> getParams();
    byte[] getDatas();
    Object getTag();

    String logMsg();
}
