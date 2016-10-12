package com.xwang.server;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.locks.ReentrantLock;

import okio.Buffer;

import static com.xwang.server.Logger.log;
import static com.xwang.server.Logger.loge;

/**
 * Created by xwangly on 2016/9/28.
 */
public class ClientSession implements Readable, Writeable, CompletionHandler<Integer, Buffer> {
    private long timeMillis;
    final int index;
    final SocketServer socketServer;
    private final AsynchronousSocketChannel socketChannel;
    private final int READ_BUFFER_SIZE = 1024;
    private final ByteBuffer readByteBuffer = ByteBuffer.allocate(READ_BUFFER_SIZE);
    private final byte[] readByteArray = new byte[READ_BUFFER_SIZE];
    private final Buffer readBuffer = new Buffer();
    private final byte[] header;
    private final int HEADER_LEN;

    private ReentrantLock writeLock = new ReentrantLock();
    //Write params need lock.
    private final int WRITE_BUFFER_SIZE = 1024;
    private final ByteBuffer writeByteBuffer = ByteBuffer.allocate(WRITE_BUFFER_SIZE);
    private final byte[] writeByteArray = new byte[WRITE_BUFFER_SIZE];
    private Queue<Buffer> writeQueue = new LinkedList<>();
    private boolean isWrite = false;

    private final ReadCompletionHandler readCompletionHandler;

    public ClientSession(int index, SocketServer socketServer, AsynchronousSocketChannel channel, ReadCompletionHandler readCompletionHandler) {
        this.index = index;
        this.socketServer = socketServer;
        this.socketChannel = channel;
        this.readCompletionHandler = readCompletionHandler;
        timeMillis = System.currentTimeMillis();
        this.HEADER_LEN = socketServer.getHeaderLength();
        this.header = new byte[HEADER_LEN];
    }

    @Override
    public void read() {
        //Listen read event.
        socketChannel.read(readByteBuffer, this, readCompletionHandler);
    }

    @Override
    public void digest() throws IOException {
        //Only one thread call read at the same time.
        //Because read operation is sequence.
        readByteBuffer.flip();
        //Copy read bytes to buffer.
        int readBytes = readByteBuffer.remaining();
        readByteBuffer.get(readByteArray, 0, readBytes);
        readBuffer.write(readByteArray, 0, readBytes);
        readByteBuffer.clear();

        long read_buffer_size;
        while ((read_buffer_size = readBuffer.size()) > HEADER_LEN) {
            for (int i =0;i< HEADER_LEN;i++) {
                header[i] = readBuffer.getByte(i);
            }
            int bodyLength = socketServer.getBodyLength(header);
            int requestLength = HEADER_LEN + bodyLength;
            if (readBuffer.size() >= requestLength) {
                log("Read a fully request length :"+requestLength+". Parse to request and dispatcher it.");
                //There are fully request.Parse request and dispatch it.
                try {
                    socketServer.parseToRequestAndDispatcher(this, readBuffer, readBuffer.readByteArray(HEADER_LEN));
                } catch (IOException e) {
                    e.printStackTrace();
                }

                //Check weather server has eat the data.
                long eat_size = read_buffer_size - readBuffer.size();

                log("Parse to request has eat " + eat_size + " bytes");
                if (eat_size > requestLength) {
                    //Error. Why server eat so many bytes.
                    throw new IOException("Parse to request can only eat " + requestLength + " but it has eat " + eat_size + " bytes");
                } else if (eat_size < requestLength) {
                    //Server not eat bytes.
                    //We eat it.
                    for (int i = 0;i < requestLength - eat_size;i++) {
                        readBuffer.readByte();
                    }
                }
                //Parse a fully request. It may have a next fully request,so go while.
            } else {
                //There are not a fully request,break.
                break;
            }
        }
    }

    @Override
    public void write(Buffer buffer) {
        if (buffer == null || buffer.size() == 0) return;
        writeLock.lock();
        try {
            log("Write buffer come. isWrite:" + isWrite + " client:" + this);
            if (isWrite) {
                writeQueue.add(buffer);
            } else {
                isWrite = true;
                log("Write a new response to client:" + this);
                writeBuffer(buffer);
            }
        } finally {
            writeLock.unlock();
        }
    }
    private void writeBuffer(Buffer buffer) {
        writeByteBuffer.clear();
        log("Write buffer remain size :" + buffer.size() + " client:" + this);

        int SIZE = buffer.read(writeByteArray, 0 , WRITE_BUFFER_SIZE);
        if (SIZE > 0) {
            writeByteBuffer.put(writeByteArray, 0 ,SIZE);
            writeByteBuffer.flip();
            socketChannel.write(writeByteBuffer, buffer, this);
        }
    }
    @Override
    public long getTimeMillis() {
        return timeMillis;
    }
    @Override
    public boolean isClosed() {
        return !socketChannel.isOpen();
    }
    @Override
    public void close() throws IOException {
        if (socketChannel != null && socketChannel.isOpen()) {
            socketChannel.close();
        }
    }

    @Override
    public void completed(Integer result, Buffer attachment) {
        log("Write complete success "+result + " remain:" + attachment.size() + " bytes client:" + this);
        updateTimeMillis();
        if (attachment.size() > 0) {
            writeBuffer(attachment);
        } else {
            log("Write next buffer client:" + this);
            writeLock.lock();
            try {
                //来查为什么没执行到这里来，remain=0了，为什么不执行这里呢，是因为lock吗？
                log("Write Queue:" + writeQueue.toString()  + " queue client:" + this);
                if (writeQueue.isEmpty()) {
                    isWrite = false;
                } else {
                    Buffer buffer = writeQueue.poll();
                    isWrite = true;
                    log("Write a new response to client:" + this);
                    writeBuffer(buffer);
                }
            } finally {
                writeLock.unlock();
            }
        }
    }

    @Override
    public void failed(Throwable exc, Buffer attachment) {
        loge("Write complete failed for " + attachment);
        exc.printStackTrace();
    }
    public void updateTimeMillis() {
        timeMillis = System.currentTimeMillis();
    }

    @Override
    public String toString() {
        int port = -1;
        try {
            if (socketChannel.isOpen()) {
                InetSocketAddress address = (InetSocketAddress) socketChannel.getRemoteAddress();
                port = address.getPort();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return index + ":port:" + port;
    }
}
