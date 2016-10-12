package com.xwang.server;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.StandardSocketOptions;
import java.nio.channels.AsynchronousChannelGroup;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.util.Comparator;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

import okio.Buffer;

import static com.xwang.server.Logger.log;
import static com.xwang.server.Logger.loge;

/**
 * Created by xwangly on 2016/9/28.
 */
public class SocketServer<T> implements CompletionHandler<AsynchronousSocketChannel, Integer>  {
    private final int port;
    private final long timeout;
    private final RequestParser<T> requestParser;
    private final Dispatcher<T> dispatcher;
    private final int HEADER_LEN;
    private PriorityBlockingQueue<Readable> queue = new PriorityBlockingQueue<>(11, new Comparator<Readable>() {
        @Override
        public int compare(Readable o1, Readable o2) {
            return (int) (o1.getTimeMillis() - o2.getTimeMillis());
        }
    });
    private AsynchronousServerSocketChannel serverSocketChannel;
    private ReadCompletionHandler readCompletionHandler;

    public SocketServer(int port, long timeout, RequestParser<T> parser, Dispatcher<T> dispatcher) {
        this.port = port;
        this.timeout = timeout;
        this.requestParser = parser;
        this.dispatcher = dispatcher;
        HEADER_LEN = requestParser.getHeaderLength();
    }

    public void start() throws IOException, ExecutionException, InterruptedException {
        startWatchDog();
        ThreadFactory threadFactory = new ThreadFactory() {
            private final AtomicInteger threadNumber = new AtomicInteger(1);
            private final String namePrefix = "pool-";
            SecurityManager s = System.getSecurityManager();
            private final ThreadGroup group  = (s != null) ? s.getThreadGroup() :
                    Thread.currentThread().getThreadGroup();
            @Override
            public Thread newThread(Runnable r) {
                Thread t = new Thread(group, r,
                        namePrefix + threadNumber.getAndIncrement(),
                        0);
                if (t.isDaemon())
                    t.setDaemon(false);
                if (t.getPriority() != Thread.NORM_PRIORITY)
                    t.setPriority(Thread.NORM_PRIORITY);
                return t;
            }
        };
        AsynchronousChannelGroup group = AsynchronousChannelGroup.withCachedThreadPool(Executors.newCachedThreadPool(threadFactory), 10);
        serverSocketChannel = AsynchronousServerSocketChannel.open(group);

        serverSocketChannel.setOption(StandardSocketOptions.SO_REUSEADDR, true);
        serverSocketChannel.setOption(StandardSocketOptions.SO_RCVBUF, 16 * 1024);
        serverSocketChannel.bind(new InetSocketAddress(port));
        readCompletionHandler = new ReadCompletionHandler(this);

        accept(0);
    }

    @Override
    public void completed(AsynchronousSocketChannel result, Integer index) {
        try {
            log("New connection:"+index + " url:" + result.getRemoteAddress());
        } catch (IOException e) {
            e.printStackTrace();
        }
        Readable readable =createReadable(index, result);
        addReadable(readable);

        readable.read();
        accept(index + 1);
    }

    @Override
    public void failed(Throwable exc, Integer attachment) {
        loge("Accept failed " + attachment);
        if (serverSocketChannel != null && serverSocketChannel.isOpen()) {
            accept(attachment + 1);
        }
    }

    private void accept(int index) {
        serverSocketChannel.accept(index, this);
    }

    private Readable createReadable(int index, AsynchronousSocketChannel socketChannel) {
        ClientSession session = new ClientSession(index, this, socketChannel, readCompletionHandler);
        return session;
    }
    private void addReadable(Readable readable) {
        queue.add(readable);
    }
    public void updateReadable(Readable readable) {
//        Readable attachment = readable;
//        log("queue befour " + queue);
//        queue.remove(attachment);
//        log("queue remove " + queue);
//        queue.add(attachment);
//        log("queue add " + queue);
    }

    private void startWatchDog() {
        Thread thread = new Thread(new TimeoutWatchDog(queue, timeout));
        thread.setName("Watch ");
        thread.start();
    }

    public int getHeaderLength() {
        return HEADER_LEN;
    }
    public int getBodyLength(byte[] header) {
        return requestParser.getBodyLength(header);
    }

    public void parseToRequestAndDispatcher(Writeable writeable, Buffer readBuffer, byte[] header) throws IOException {
        T request = requestParser.parserToRequest(readBuffer, header);
        dispatcher.dispatchRequest(writeable, request);
    }
}
