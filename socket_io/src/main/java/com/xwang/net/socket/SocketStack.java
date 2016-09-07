package com.xwang.net.socket;

import com.xwang.net.internal.BlockingHashMap;
import com.xwang.net.Engine;
import com.xwang.net.EngineRequest;
import com.xwang.net.NetException;
import com.xwang.net.NetLog;
import com.xwang.net.NetRequest;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @date 2014-7-22
 * Use NC framework to implements CJSocket channel
 * @date 2014-12-18
 * Use volley framework,Socket has one send thread,and it will wait for response, than it can send next request
 * @date 2014-12-22
 * 1.Update socket lets to a common socket.(Can not only use for cjsocket and enosocket)
 * 2.Send thread support multi thread.(Use reentrantlock to control there is only one thread do sending at one time.
 *   After sending ,it will blocking until receive response.When it blocking,other sending thread can sending request.)
 * @date 2014-12-29
 * Socket sending thread use blockingmap.
 *
 * @date 2016-07-11
 * Abstract BlockingHashMap
 * Delete volley code.
 * Abstract socket frame.
 */
public class SocketStack implements Engine<SocketEngineResponse> {
    private String mHost;
    private int mPort;
    private Socket mSocket;
    
    private Receiver mReceiver;
//    private ExecutorService receiverThreadExecutor;

    //Socket lock.Only one thread runs socket send.
    private static ReentrantLock socketLock = new ReentrantLock();
     
    private BlockingHashMap<Integer, SocketEngineResponse> mResponseMap = new BlockingHashMap<>();

    private final SocketHandler mSocketHandler;
    public SocketStack(SocketHandler handler) {
        NetLog.d("SocketStack created");
        this.mSocketHandler = handler;
        mReceiver = new Receiver();
    }


    private class Receiver implements Runnable {

        public void run() {
            Thread.currentThread().setName("Receiver");
            NetLog.d("Receiver begin run ");
            onSocketConnect();
            try {
                SocketEngineResponse response = null;
                try {
                    InputStream is = mSocket.getInputStream();
                    NetLog.d("Start to read message from Socket. I may enter dead cycle till Socket is close or error.");
                    for (;;) {
                        NetLog.d("Begin blocking read message from Socket");
                        try {
                            if (mSocket.isClosed()) throw new IOException("Socket closed.");
                            response = mSocketHandler.parseResponse(mSocket, is);

                            if (response.isPushMsg()) {
                                NetLog.d("received push message.");
                                onPush(response);
                            } else {
                                mResponseMap.put(response.getSerial(), response);
                            }
                        } catch (NetException e) {
                            //ParseError must consumed here,it can not break the receiver thread.
                            NetLog.e(e, "ParseError occur.");
                        }
                        NetLog.d("End read one message.");
                    }
                } catch (IOException ex) {
                    NetLog.e(ex, String.format("'%s:%d' socket colsed", mHost, mPort));
                } catch (Throwable tr) {
                    NetLog.e(tr, "Uncaught exception on reader thread");
                }

                NetLog.d(String.format("Disconnected from '%s:%d' socket", mHost, mPort));
                try {
                    if (mSocket != null && mSocket.isConnected()) {
                        mSocket.close();
                    }
                } catch (IOException ex) {
                }

                mSocket = null;

                //Clear request list on close
                NetLog.d("responseWaitingRequestUseException");
                mResponseMap.interrutpAll();
            } catch (Throwable tr) {
                NetLog.e(tr, "Uncaught exception");
            }
            onSocketDisconnect();
            NetLog.d("Receiver end run ");
        }
    }

    private void disConnect_() throws IOException {
        NetLog.d("To close socket. isConnected?" + isConnected());
        if (mSocket != null) {
            mSocket.close();
            mSocket = null;
        }
    }
    /**
     * 
     */
    private void connect_(String host, int port, int timeout) throws IOException {
        Socket s = null;

        try {
            s = new Socket();
            s.connect(new InetSocketAddress(host, port), timeout);
            s.setKeepAlive(true);
            this.mHost = host;
            this.mPort = port;
            this.mSocket = s;
            new Thread(mReceiver).start();
            NetLog.d(String.format("Connected to '%s:%d' socket", host, port));
        } catch (IOException ex) {
            try {
                if (s != null) {
                    s.close();
                    s = null;
                }
            } catch (IOException ex2) {
                // ignore failure to close after failure to connect
            }

            NetLog.e(ex, String.format("Couldn't connect '%s:%d' ", host, port));
            throw ex;
        }
    }

    private void open(String host, int port, int timeout)  throws IOException {
        if (isConnected()) {
            if (mPort == port && mHost != null && mHost.equals(host)) {
                NetLog.d("connect request. We have already connect this addr ");
            } else {
                disConnect();
                connect_(host, port, timeout);
            }
        } else {
            connect_(host, port, timeout);
        }
    }

    /**
     * Is the socket connected?
     * @return
     */
    public boolean isConnected() {
        boolean isConnected = mSocket != null && mSocket.isConnected();
        return isConnected;
    }

    /**
     * Get current connect address.
     * @return
     */
    public String getHost() {
        if (isConnected()) return mHost;
        return null;
    }
    public int getPort() {
        return mPort;
    }

    public void connect(String host, int port, int timeMS) throws NetException {
        NetLog.d(String.format("To open socket '%s:%d' ", host, port));
        try {
            open(host, port, timeMS);
        } catch (SocketTimeoutException e) {
            throw new NetException(NetException.CONNECT_TIMEOUT, e);
        } catch (IOException e) {
            throw new NetException(NetException.IO_EXCEPTION, e);
        }
    }

    public void disConnect() {
        NetLog.d("To close socket. isConnected?" + isConnected());
        try {
            disConnect_();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * @param request
     * @return
     * @throws NetException
     * 
     */
    @Override
    public final SocketEngineResponse performRequest(EngineRequest request)
            throws NetException {
        long requestStart = System.currentTimeMillis();
        socketLock.lock();
        try {
            Socket s = mSocket;
            if (s == null) {
                NetLog.e("socket == null.");
                throw new NetException(NetException.SOCKET_NOT_CONNECTED);
            }
            if (request.isCanceled()) {
                throw new NetException(NetException.USER_CANCELED);
            }

            request.addMarker("NCSocket begin send");
            sendRequest(s, s.getOutputStream(), request);
            SocketEngineResponse response = null;
            try {
                request.addMarker(String.format("Begin wait response for [%d] ", request.getSequence()));
                // Waiting for the response.This method will blocking current
                // thread,until response come in or timeout.
                response = mResponseMap.get(request.getSequence(), request.getTimeoutMs(), TimeUnit.MILLISECONDS);
                request.addMarker(String.format("End wait response for [%d] ", request.getSequence()));
                if (response == null) {
                    // May be timeout
                    request.addMarker(request.toString() + " time out");
                    throw new NetException(NetException.REQUEST_TIMEOUT);
                } else {
                    response.networkTimeMs = System.currentTimeMillis() - requestStart;
                    return response;
                }
            } catch (InterruptedException e1) {
                request.addMarker("InterruptedException occur.");
                NetLog.e(e1, "InterruptedException occur.");
                // May be Receiver exception occor.
                NetLog.e(request.toString() + " EXCEPTION_NETWORK_RESPONSE");
                throw new NetException(NetException.NETWORK_DISCONNECT);
            }
        } catch (SocketTimeoutException e) {
            throw new NetException(NetException.CONNECT_TIMEOUT, e);
        } catch (IOException e) {
            throw new NetException(NetException.IO_EXCEPTION, e);
        } finally {
            socketLock.unlock();
        }
    }

    @Override
    public void cancelRequest(EngineRequest request) throws NetException {
        //Nothing to do while not support cancel.
        mResponseMap.interrutp(request.getSequence());
    }

    @Override
    public void shutDown() {
        disConnect();
        mResponseMap.interrutpAll();
    }

    /**
     * Send request to the socket
     * @throws IOException
     */
    protected void sendRequest(Socket socket, OutputStream os,
            EngineRequest sr) throws IOException {
        byte[] data = sr.getBody();
        os.write(data);
        os.flush();
    }

    /**
     * Receive push message
     * @param response
     */
    protected void onPush(SocketEngineResponse response) {
        if (mSocketHandler != null) {
            mSocketHandler.onPush(response);
        }
    }
    /**
     * 
     */
    protected void onSocketConnect() {
        if (mSocketHandler != null) {
            mSocketHandler.onSocketConnect(mHost, mPort);
        }
    }

    /**
     * Monitor the socket connection state
     */
    protected void onSocketDisconnect() {
        if (mSocketHandler != null) {
            mSocketHandler.onSocketDisconnect();
        }
    }
}
