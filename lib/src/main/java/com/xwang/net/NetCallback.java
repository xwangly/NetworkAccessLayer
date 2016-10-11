package com.xwang.net;

/**
 * Created by xwangly on 2016/8/29.
 */
public interface NetCallback<NETRESPONSE> {
    void onResponse(NETRESPONSE response);
    void onFailure(NetException e);
}