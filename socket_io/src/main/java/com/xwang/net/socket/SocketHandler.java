package com.xwang.net.socket;

import com.xwang.net.NetException;

import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;

/**
 * @date 2015-2-16
 * 
 */
public interface SocketHandler {
    SocketEngineResponse parseResponse(Socket socket, InputStream is) throws IOException, NetException;

    /**
     * Receive push message
     * 
     * @param response
     */
    void onPush(SocketEngineResponse response);

    /**
     * Monitor the socket connection state
     * 
     * @param host
     * @param port
     */
    void onSocketConnect(String host, int port);
    /**
     * 
     */
    void onSocketDisconnect();

}
