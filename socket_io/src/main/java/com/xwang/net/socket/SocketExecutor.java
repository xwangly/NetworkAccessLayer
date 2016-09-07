package com.xwang.net.socket;

import com.xwang.net.EngineRequest;
import com.xwang.net.NetException;
import com.xwang.net.internal.EngineResponse;
import com.xwang.net.internal.NetExecutorImpl;
import com.xwang.net.NetRequest;
import com.xwang.net.NetResponse;
import com.xwang.net.NetResponseParser;

import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.util.concurrent.Executor;

/**
 * Created by xwangly on 2016/8/31.
 */
public abstract class SocketExecutor<REQUEST extends NetRequest, RESPONSE extends NetResponse> extends NetExecutorImpl<REQUEST, RESPONSE, SocketEngineResponse> implements SocketHandler, NetResponseParser<RESPONSE>{

    private final String TAG = "SocketManager";
    private String host;
    private int port;
    private SocketStack mSocketStack;

    private enum ConnectedState {
        init,
        connecting,
        connected,
        disconnecting,
    }
    private ConnectedState state = ConnectedState.init;
    private String connectingHost;

    private int connectingPort;

    private String url;
    private int timeout = 30 * 1000;

    public SocketExecutor(Executor callbackExecutor) {
        super(callbackExecutor);
        mSocketStack = new SocketStack(this);
        setEngine(mSocketStack);
    }

    @Override
    protected void setRequestParams(EngineRequest<REQUEST> engineRequest) {
        super.setRequestParams(engineRequest);
    }

    @Override
    public void cancelRequest(Object tag) {
        super.cancelRequest(tag);
    }

    @Override
    public final void onPush(SocketEngineResponse response) {
        try {
            RESPONSE netResponse = parseToNetResponse(response);
            onPush(netResponse);
        } catch (NetException e) {
            e.printStackTrace();
        }
    }

    @Override
    public final void onSocketConnect(String host, int port) {
        state = ConnectedState.connected;
        this.host = host;
        this.port = port;
        setUrl("socket://" + host + ":" + port);
    }

    @Override
    public final void onSocketDisconnect() {
        state = ConnectedState.init;
        host = null;
        port = -1;
    }

    public void connect(String host, int port, int timeMS) throws NetException {
        boolean shouldConnect = false;
        if (state == ConnectedState.connecting) {
            if (!isSame(host, port, connectingHost, connectingPort)) shouldConnect = true;
        } else if (state == ConnectedState.connected) {
            if (!isSame(host, port, this.host, this.port)) shouldConnect = true;
        } else {
            shouldConnect = true;
        }
        if (shouldConnect) {
            state = ConnectedState.connecting;
            connectingHost = host;
            connectingPort = port;

            mSocketStack.connect(host, port, timeMS);
        }

    }
    public void disConnect() {
        if (isConnected()) {
            state = ConnectedState.disconnecting;
            mSocketStack.disConnect();
        }
    }
    public boolean isConnected() {
        return state == ConnectedState.connected;
    }

    private void setUrl(String url) {
        this.url = url;
    }

    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    @Override
    protected String getUrl(REQUEST request) {
        return url;
    }

    @Override
    protected int getTimeout(REQUEST request) {
        return timeout;
    }

    private boolean isSame(String thisHost, int thisPort, String otherHost, int otherPort) {
        return thisPort == otherPort && thisHost != null && thisHost.equals(otherHost);
    }

    @Override
    protected NetResponseParser getParser(REQUEST request) {
        return this;
    }

    public int getTimeout() {
        return timeout;
    }

    public String getUrl() {
        return url;
    }

    @Override
    public <ENGINERESPONSE extends EngineResponse> RESPONSE parseToNetResponse(ENGINERESPONSE engine_response) throws NetException {
        return null;
    }

    @Override
    public abstract SocketEngineResponse parseResponse(Socket socket, InputStream is) throws IOException, NetException;

    public abstract void onPush(RESPONSE response);

}
